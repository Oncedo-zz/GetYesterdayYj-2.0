package com.wechat.ferry.core;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 2.0 配置中心。
 *
 * 加载顺序（后者覆盖前者，深度合并）：
 *   1) classpath:config/sites.yml   —— 内置默认，全部取自 1.0 硬编码值
 *   2) jar 同目录 ./sites.yml        —— 外部覆盖，服务端改版只改这里
 *   3) ${ENV} / ${ENV:默认值}         —— 环境变量占位符，运行时替换
 *
 * 只依赖 SnakeYAML（Spring Boot starter 自带）与 JDK，main 方法可直接用。
 */
public final class AppConfig {

    private static final String CLASSPATH_RESOURCE = "config/sites.yml";
    private static final String EXTERNAL_FILE = "sites.yml";
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^}]+)\\}");

    private static AppConfig instance;

    private final Map<String, Object> root;
    private final String sourceDescription;

    private AppConfig() {
        Map<String, Object> base = loadClasspath();
        Map<String, Object> external = loadExternal();
        if (external != null) {
            deepMerge(base, external);
        }
        this.root = castMap(resolveAll(base));
        this.sourceDescription = external == null
            ? "内置配置 classpath:" + CLASSPATH_RESOURCE
            : "内置配置 + 外部覆盖 " + new File(EXTERNAL_FILE).getAbsolutePath();
    }

    public static synchronized AppConfig get() {
        if (instance == null) {
            instance = new AppConfig();
        }
        return instance;
    }

    /** 强制重新加载（自检 --check 用） */
    public static synchronized AppConfig reload() {
        instance = new AppConfig();
        return instance;
    }

    // ------------------------------------------------------------------
    // 通用取值
    // ------------------------------------------------------------------

    public String str(String path, String def) {
        Object v = get(path);
        return v == null ? def : String.valueOf(v);
    }

    public int integer(String path, int def) {
        Object v = get(path);
        if (v == null) return def;
        try {
            return Integer.parseInt(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public boolean bool(String path, boolean def) {
        Object v = get(path);
        if (v == null) return def;
        return Boolean.parseBoolean(String.valueOf(v).trim());
    }

    public List<String> list(String path) {
        Object v = get(path);
        if (v instanceof List) {
            List<String> out = new ArrayList<String>();
            for (Object o : (List<?>) v) {
                out.add(o == null ? "" : String.valueOf(o));
            }
            return out;
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> map(String path) {
        Object v = get(path);
        return v instanceof Map ? (Map<String, Object>) v : Collections.<String, Object>emptyMap();
    }

    /** 按点分路径取值，如 defaults.wait.afterQueryMs */
    private Object get(String path) {
        Object cur = root;
        for (String key : path.split("\\.")) {
            if (!(cur instanceof Map)) return null;
            cur = ((Map<?, ?>) cur).get(key);
            if (cur == null) return null;
        }
        return cur;
    }

    /** 所有站点 key（保持 yml 中的顺序） */
    @SuppressWarnings("unchecked")
    public List<String> siteKeys() {
        Object sites = root.get("sites");
        if (!(sites instanceof Map)) return Collections.emptyList();
        return new ArrayList<String>(((Map<String, Object>) sites).keySet());
    }

    public Map<String, Object> site(String key) {
        return map("sites." + key);
    }

    public String siteStr(String key, String sub, String def) {
        return str("sites." + key + "." + sub, def);
    }

    public int siteInt(String key, String sub, int def) {
        return integer("sites." + key + "." + sub, def);
    }

    public List<String> siteList(String key, String sub) {
        return list("sites." + key + "." + sub);
    }

    public String sourceDescription() {
        return sourceDescription;
    }

    // ------------------------------------------------------------------
    // 加载与合并
    // ------------------------------------------------------------------

    private static Map<String, Object> loadClasspath() {
        InputStream in = AppConfig.class.getClassLoader().getResourceAsStream(CLASSPATH_RESOURCE);
        if (in == null) {
            throw new IllegalStateException("内置配置缺失：" + CLASSPATH_RESOURCE);
        }
        try {
            Object loaded = new Yaml().load(in);
            return loaded instanceof Map
                ? castMap(loaded)
                : new LinkedHashMap<String, Object>();
        } finally {
            try { in.close(); } catch (Exception ignored) { }
        }
    }

    private static Map<String, Object> loadExternal() {
        File f = new File(EXTERNAL_FILE);
        if (!f.isFile()) return null;
        InputStream in = null;
        try {
            in = new FileInputStream(f);
            Object loaded = new Yaml().load(in);
            return loaded instanceof Map ? castMap(loaded) : null;
        } catch (Exception e) {
            System.err.println("[配置] 外部配置文件读取失败，将使用内置配置：" + e.getMessage());
            return null;
        } finally {
            if (in != null) try { in.close(); } catch (Exception ignored) { }
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object o) {
        return (Map<String, Object>) o;
    }

    /** 深度合并：Map 递归，其他类型（含 List）以覆盖者为准 */
    @SuppressWarnings("unchecked")
    private static void deepMerge(Map<String, Object> target, Map<String, Object> override) {
        for (Map.Entry<String, Object> e : override.entrySet()) {
            String k = e.getKey();
            Object tv = target.get(k);
            Object ov = e.getValue();
            if (tv instanceof Map && ov instanceof Map) {
                deepMerge((Map<String, Object>) tv, (Map<String, Object>) ov);
            } else {
                target.put(k, ov);
            }
        }
    }

    /** 递归替换 ${ENV} 与 ${ENV:默认值} */
    @SuppressWarnings("unchecked")
    private static Object resolveAll(Object v) {
        if (v instanceof String) {
            return resolveString((String) v);
        }
        if (v instanceof List) {
            List<Object> out = new ArrayList<Object>();
            for (Object o : (List<?>) v) out.add(resolveAll(o));
            return out;
        }
        if (v instanceof Map) {
            Map<String, Object> out = new LinkedHashMap<String, Object>();
            for (Map.Entry<String, Object> e : ((Map<String, Object>) v).entrySet()) {
                out.put(e.getKey(), resolveAll(e.getValue()));
            }
            return out;
        }
        return v;
    }

    private static String resolveString(String s) {
        Matcher m = PLACEHOLDER.matcher(s);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String expr = m.group(1);
            int idx = expr.indexOf(':');
            String envName = (idx < 0 ? expr : expr.substring(0, idx)).trim();
            String def = idx < 0 ? "" : expr.substring(idx + 1);
            String val = System.getenv(envName);
            if (val == null || val.isEmpty()) val = System.getProperty(envName);
            if (val == null || val.isEmpty()) val = def;
            m.appendReplacement(sb, Matcher.quoteReplacement(val));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
