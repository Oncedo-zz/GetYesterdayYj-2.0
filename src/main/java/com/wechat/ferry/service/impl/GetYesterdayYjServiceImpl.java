package com.wechat.ferry.service.impl;

import com.wechat.ferry.core.AppConfig;
import com.wechat.ferry.core.BrowserLogin;
import com.wechat.ferry.core.ReportCrawler;
import com.wechat.ferry.core.StepContext;
import com.wechat.ferry.enums.JTNRB_ACCOUNT;
import org.openqa.selenium.WebDriver;

/**
 * 2.0 主服务：站点完全由 sites.yml 驱动。
 *
 * 新增一个 ERP 站点 = 在 yml 里加一段 sites.<key>，无需改本文件、无需重新编译。
 * 保留 GetYhgzYj / GetJtnrbYj 两个原方法签名供旧调用方使用。
 */
public class GetYesterdayYjServiceImpl {

    private final AppConfig cfg = AppConfig.get();
    private final JTNRB_ACCOUNT account = new JTNRB_ACCOUNT();
    private final ReportCrawler crawler = new ReportCrawler(cfg);

    /** 遍历配置中所有启用的站点 */
    public void runAll(String targetDate) {
        for (String key : cfg.siteKeys()) {
            if (!cfg.bool("sites." + key + ".enabled", true)) {
                System.out.println("[任务] 站点 " + key + " 已禁用，跳过");
                continue;
            }
            runSite(key, targetDate);
        }
    }

    /** 按站点 key 执行完整流程：登录 -> 抓取 -> 退出 */
    public void runSite(String siteKey, String targetDate) {
        BrowserLogin login = new BrowserLogin();
        WebDriver driver = null;
        try {
            driver = login.get_driver();
            String url = cfg.siteStr(siteKey, "url", "");
            String user = account.get(siteKey, cfg.siteInt(siteKey, "credentials.userIndex", 0));
            String pwd = account.get(siteKey, cfg.siteInt(siteKey, "credentials.passwordIndex", 1));
            login.LogIn(driver, siteKey, url, user, pwd);
            crawler.crawl(driver, siteKey, targetDate);
        } catch (RuntimeException e) {
            String display = cfg.siteStr(siteKey, "displayName", siteKey);
            StepContext.fail(display, targetDate, "站点执行失败", e.getMessage());
            throw e;
        } finally {
            login.safeQuit(driver);
        }
    }

    // ---- 兼容 1.0 的调用签名 ----
    public void GetJtnrbYj(String targetDate) {
        runSite("jtnrb", targetDate);
    }

    public void GetYhgzYj(String targetDate) {
        runSite("yhgz", targetDate);
    }
}
