package com.wechat.ferry.core;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * 多候选定位器。
 *
 * 服务端把 placeholder 文案、按钮文字、DOM 结构改了之后，
 * 只要在 sites.yml 里给该元素加一条新候选选择器即可，无需改 Java 代码。
 *
 * 全部候选都失败时抛 StepException，明确告诉你卡在哪一步、试过哪些选择器。
 */
public final class ElementLocator {

    private ElementLocator() {}

    /**
     * 依次尝试候选选择器，返回第一个可见元素。
     * @param candidates 形如 "css=input[x]" / "xpath=//div" / "id=xxx" / "class=xxx" / "text=报表"
     */
    public static WebElement find(WebDriver driver, String site, String date, String step,
                                  List<String> candidates, Duration timeout) {
        if (candidates == null || candidates.isEmpty()) {
            throw new StepException(site, date, step, "未配置任何候选选择器");
        }
        WebDriverWait wait = new WebDriverWait(driver, timeout);
        StringBuilder tried = new StringBuilder();

        for (int i = 0; i < candidates.size(); i++) {
            String raw = candidates.get(i);
            By by = parse(raw);
            tried.append("  ").append(i + 1).append(") ").append(raw).append("\n");
            try {
                WebElement el = wait.until(ExpectedConditions.visibilityOfElementLocated(by));
                StepContext.step(site, date, step + " [命中候选" + (i + 1) + "] " + raw);
                return el;
            } catch (TimeoutException ignored) {
                // 该候选未命中，尝试下一个
            }
        }
        throw new StepException(site, date, step,
            "所有候选选择器均未命中，已尝试：\n" + tried + "当前页面标题=" + safeTitle(driver));
    }

    /** 点击元素：可点击状态 + 失败明确报错 */
    public static void click(WebDriver driver, String site, String date, String step,
                             List<String> candidates, Duration timeout) {
        WebElement el = find(driver, site, date, step, candidates, timeout);
        try {
            el.click();
        } catch (StaleElementReferenceException e) {
            // DOM 刷新导致元素过期，重新定位一次再点
            WebElement fresh = find(driver, site, date, step + "(重试)", candidates, timeout);
            fresh.click();
        }
    }

    /** 等待元素消失（用于等 loading 遮罩） */
    public static void waitGone(WebDriver driver, String site, String date, String step,
                                List<String> candidates, Duration timeout) {
        if (candidates == null || candidates.isEmpty()) return;
        WebDriverWait wait = new WebDriverWait(driver, timeout);
        for (String raw : candidates) {
            try {
                wait.until(ExpectedConditions.invisibilityOfElementLocated(parse(raw)));
                return;
            } catch (TimeoutException ignored) {
                // 当前候选仍然存在，尝试下一个（可能配错了）
            }
        }
        StepContext.warn(site, date, step, "等待元素消失超时（非致命，继续执行）");
    }

    /** 在容器内查找一批元素（不等待，立即返回） */
    public static List<WebElement> findAll(SearchContext context, String raw) {
        return context.findElements(parse(raw));
    }

    /** 解析 "css=..." / "xpath=..." / "id=..." / "class=..." / "tag=..." / "text=..." */
    public static By parse(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new IllegalArgumentException("空选择器");
        }
        String s = raw.trim();
        int eq = s.indexOf('=');
        if (eq <= 0) {
            throw new IllegalArgumentException("选择器格式错误（应为 类型=表达式）：" + raw);
        }
        String type = s.substring(0, eq).trim().toLowerCase();
        String expr = s.substring(eq + 1);
        if (expr.isEmpty()) {
            throw new IllegalArgumentException("选择器表达式为空：" + raw);
        }
        if ("css".equals(type)) return By.cssSelector(expr);
        if ("xpath".equals(type)) return By.xpath(expr);
        if ("id".equals(type)) return By.id(expr);
        if ("class".equals(type)) return By.className(expr);
        if ("tag".equals(type)) return By.tagName(expr);
        if ("name".equals(type)) return By.name(expr);
        if ("text".equals(type)) return By.xpath("//*[normalize-space(text())='" + esc(expr) + "']");
        throw new IllegalArgumentException("不支持的选择器类型：" + type + "（" + raw + "）");
    }

    /** 安全读取元素文本，兼容 stale 与 JS 渲染的空 innerText */
    public static String text(WebDriver driver, WebElement el) {
        try {
            Object v = ((JavascriptExecutor) driver).executeScript(
                "return arguments[0].textContent && arguments[0].textContent.trim() " +
                "? arguments[0].textContent.trim() : (arguments[0].innerText||'').trim();", el);
            return v == null ? "" : String.valueOf(v);
        } catch (Exception e) {
            try { return el.getText(); } catch (Exception ignored) { return ""; }
        }
    }

    private static String esc(String s) {
        return s.replace("'", "\\'");
    }

    private static String safeTitle(WebDriver driver) {
        try { return driver.getTitle(); } catch (Exception e) { return "<无法读取>"; }
    }
}
