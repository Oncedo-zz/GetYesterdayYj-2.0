package com.wechat.ferry.service.impl.BaiSheng;

import com.wechat.ferry.core.AppConfig;
import com.wechat.ferry.core.ReportCrawler;
import org.openqa.selenium.WebDriver;

/**
 * 兼容包装类（2.0）。
 * 原 1.0 实现已合并进通用引擎 {@link ReportCrawler}；
 * 本类保留原方法签名，外部若仍调用此类，行为一致但走配置驱动。
 */
public class ZhongDuanYJTongJi_Yes_Yhgz {

    public void ZhongDuanXiaoShouYJ_Yes_Yhgz(WebDriver driver, String targetDate) {
        new ReportCrawler(AppConfig.get()).crawl(driver, "yhgz", targetDate);
    }
}
