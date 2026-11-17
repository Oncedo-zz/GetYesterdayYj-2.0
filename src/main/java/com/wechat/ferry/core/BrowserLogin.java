package com.wechat.ferry.core;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.File;
import java.time.Duration;
import java.util.List;

/**
 * 浏览器驱动与登录（2.0）。
 *
 * 与 1.0 的关键区别：
 *   1) 登录超时/失败直接抛 StepException，绝不再吞掉异常后返回"未登录的 driver"继续空转。
 *   2) 所有路径、参数、选择器来自 sites.yml。
 *   3) 每个步骤都有 StepContext 日志。
 *
 * 保留 get_driver / LogIn / safeQuit 三个原方法签名，兼容旧调用方。
 */
public final class BrowserLogin {

    private final AppConfig cfg = AppConfig.get();

    public WebDriver get_driver() {
        String driverPath = cfg.str("defaults.driver.chromeDriverPath",
            "C:\\Plugs\\chrome-win64\\chromedriver.exe");
        System.setProperty("webdriver.chrome.driver", driverPath);

        File driverFile = new File(driverPath);
        if (!driverFile.isFile()) {
            throw new IllegalStateException("ChromeDriver 不存在：" + driverPath +
                "。请在 sites.yml 的 defaults.driver.chromeDriverPath 中修正路径。");
        }

        ChromeOptions options = new ChromeOptions();

        // 指定 Chrome 二进制（如 Chrome for Testing 138，专供 MV2 插件使用）。
        // 未配置则用系统默认 Chrome，保持向后兼容。
        String binaryPath = cfg.str("defaults.driver.browserBinaryPath", "");
        if (!binaryPath.isEmpty()) {
            File binaryFile = new File(binaryPath);
            if (!binaryFile.isFile()) {
                throw new IllegalStateException("Chrome 二进制不存在：" + binaryPath +
                    "。请在 sites.yml 的 defaults.driver.browserBinaryPath 中修正路径。");
            }
            options.setBinary(binaryFile);
            StepContext.check("Chrome 二进制", binaryPath);
        }

        List<String> args = cfg.list("defaults.driver.arguments");
        for (String arg : args) {
            options.addArguments(arg);
        }

        String extension = cfg.str("defaults.driver.extensionPath", "");
        if (!extension.isEmpty()) {
            options.addArguments("--load-extension=" + extension);
        }

        String logDir = System.getProperty("java.io.tmpdir");
        String logName = cfg.str("defaults.driver.logFileName", "chromedriver.log");
        System.setProperty("webdriver.chrome.logfile", new File(logDir, logName).getAbsolutePath());

        try {
            return new ChromeDriver(options);
        } catch (Exception e) {
            throw new IllegalStateException("启动 ChromeDriver 失败：" + e.getMessage(), e);
        }
    }

    /** 登录并返回 driver。失败抛异常，不返回无效 driver。 */
    public WebDriver LogIn(WebDriver driver, String siteKey, String url,
                           String username, String password) {
        String display = cfg.siteStr(siteKey, "displayName", siteKey);
        Duration timeout = Duration.ofSeconds(cfg.integer("defaults.wait.loginTimeoutSeconds", 10));

        try {
            StepContext.step(display, "-", "打开登录页 " + url);
            driver.get(url);
            Waits.waitPageReady(driver, timeout);
            Waits.pause(cfg.integer("defaults.wait.afterPageLoadMs", 2000), "登录页加载");
            StepContext.check("页面标题", driver.getTitle());

            StepContext.step(display, "-", "输入账号");
            WebElement userField = ElementLocator.find(driver, display, "-", "定位账号输入框",
                cfg.siteList(siteKey, "login.username"), timeout);
            userField.clear();
            userField.sendKeys(maskCheck(username));

            StepContext.step(display, "-", "输入密码");
            WebElement passField = ElementLocator.find(driver, display, "-", "定位密码输入框",
                cfg.siteList(siteKey, "login.password"), timeout);
            passField.clear();
            passField.sendKeys(maskCheck(password));

            Waits.pause(cfg.integer("defaults.wait.beforeSubmitMs", 1000), "提交前稳定");

            StepContext.step(display, "-", "点击登录按钮");
            ElementLocator.click(driver, display, "-", "点击登录按钮",
                cfg.siteList(siteKey, "login.submit"), timeout);
            Waits.pause(cfg.integer("defaults.wait.afterSubmitMs", 1000), "登录后跳转");

            // 校验登录结果：配置了 successIndicator 就检查，没配则用 URL 变化兜底判断
            verifyLogin(driver, siteKey, display, url, timeout);
            Waits.pause(cfg.integer("defaults.wait.afterLoginMs", 1000), "登录态稳定");
            StepContext.ok(display, "-", "登录成功：" + url);
            return driver;

        } catch (StepException e) {
            throw e;
        } catch (Exception e) {
            throw new StepException(display, "-", "登录流程",
                "登录异常：" + e.getMessage() + "。当前URL=" + safeUrl(driver), e);
        }
    }

    private void verifyLogin(WebDriver driver, String siteKey, String display,
                             String loginUrl, Duration timeout) {
        List<String> indicators = cfg.siteList(siteKey, "login.successIndicator");
        if (indicators.isEmpty()) {
            // 未配置成功标志：只做基本健全性检查（页面仍在登录页 = 登录失败）
            String now = driver.getCurrentUrl();
            if (now != null && now.equals(loginUrl)) {
                StepContext.warn(display, "-", "登录校验",
                    "URL 未变化，可能登录失败；如需严格校验请在 sites.yml 配置 login.successIndicator");
            }
            return;
        }
        // 配置了标志则必须命中，否则判定失败
        ElementLocator.find(driver, display, "-", "校验登录成功标志", indicators, timeout);
    }

    public void safeQuit(WebDriver driver) {
        if (driver == null) return;
        try {
            driver.quit();
            System.out.println("[系统] 浏览器已安全关闭");
        } catch (Exception e) {
            System.out.println("[系统] 浏览器关闭异常（可忽略）：" + e.getMessage());
        }
    }

    /** 账号为空时立即报错，避免拿 null 去 sendKeys */
    private String maskCheck(String v) {
        if (v == null || v.isEmpty()) {
            throw new IllegalStateException("账号或密码为空，请检查环境变量 JTNRB 是否按格式设置" +
                "（4 段逗号分隔：jtnrb账号,密码,yhgz账号,密码）");
        }
        return v;
    }

    private String safeUrl(WebDriver d) {
        try { return d.getCurrentUrl(); } catch (Exception e) { return "<无法读取>"; }
    }
}
