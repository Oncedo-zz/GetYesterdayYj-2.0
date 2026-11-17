package com.wechat.ferry.core;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * 条件等待工具：用"等数据出现"替代固定 Thread.sleep，既更快也更可靠。
 * 原 1.0 里大量 sleep(1000/3000)，服务慢一点就取空表；2.0 改为轮询表格行。
 */
public final class Waits {

    private Waits() {}

    /** 等待至少 minRows 行数据出现，返回实际行数 */
    public static int waitForRows(WebDriver driver, WebElement pane, String rowsSelector,
                                  int minRows, Duration timeout) {
        WebDriverWait wait = new WebDriverWait(driver, timeout);
        final By rows = ElementLocator.parse(rowsSelector);
        try {
            wait.until(d -> {
                try {
                    List<WebElement> found = pane.findElements(rows);
                    return found.size() >= minRows ? found : null;
                } catch (Exception e) {
                    return null;
                }
            });
            return pane.findElements(rows).size();
        } catch (org.openqa.selenium.TimeoutException e) {
            throw new StepException("-", "-", "等待数据行",
                "等待至少 " + minRows + " 行数据超时（" + timeout.getSeconds() + "秒），" +
                "可能是日期无数据或表格结构已变更，请检查 sites.yml 的 report.rows 配置", e);
        }
    }

    /** 等待页面加载完成（document.readyState == complete） */
    public static void waitPageReady(WebDriver driver, Duration timeout) {
        WebDriverWait wait = new WebDriverWait(driver, timeout);
        try {
            wait.until(d -> "complete".equals(
                ((JavascriptExecutor) d).executeScript("return document.readyState")));
        } catch (org.openqa.selenium.TimeoutException e) {
            StepContext.warn("-", "-", "页面加载", "readyState 超时，继续执行");
        }
    }

    /** 短暂休眠，仅在等待动画等非数据场景使用，且打印原因便于排查 */
    public static void pause(long millis, String reason) {
        if (millis <= 0) return;
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (reason != null && !reason.isEmpty()) {
            System.out.println("       (等待 " + millis + "ms：" + reason + ")");
        }
    }
}
