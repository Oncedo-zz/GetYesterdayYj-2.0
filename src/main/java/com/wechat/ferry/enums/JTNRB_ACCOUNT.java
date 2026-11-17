package com.wechat.ferry.enums;

/**
 * 账号凭据：从环境变量按逗号切分（格式与 1.0 完全一致：
 * jtnrb账号,jtnrb密码,yhgz账号,yhgz密码）。
 *
 * 2.0 新增通用 get(siteKey, index)，使 ServiceImpl 无需为每个站点写一套方法；
 * 原四个方法保留，兼容旧调用方。
 */
public class JTNRB_ACCOUNT {

    private static final String ENV = "JTNRB";

    public String getJtnrbUser() { return get("jtnrb", 0); }
    public String getJtnrbPwd()  { return get("jtnrb", 1); }
    public String getYhgzUser()  { return get("yhgz", 2); }
    public String getYhgzPwd()   { return get("yhgz", 3); }

    /** 按站点配置的下标取凭据。siteKey 仅用于错误提示，下标来自 sites.yml */
    public String get(String siteKey, int index) {
        String raw = System.getenv(ENV);
        if (raw == null || raw.trim().isEmpty()) {
            throw new IllegalStateException("环境变量 " + ENV + " 未设置。" +
                "请按格式设置：" + ENV + "=jtnrb账号,jtnrb密码,yhgz账号,yhgz密码");
        }
        String[] parts = raw.split(",", -1);
        if (index < 0 || index >= parts.length || parts[index].trim().isEmpty()) {
            throw new IllegalStateException("环境变量 " + ENV + " 缺少第 " + (index + 1) +
                " 段（站点 " + siteKey + "），当前共 " + parts.length + " 段。" +
                "正确格式：jtnrb账号,jtnrb密码,yhgz账号,yhgz密码");
        }
        return parts[index].trim();
    }
}
