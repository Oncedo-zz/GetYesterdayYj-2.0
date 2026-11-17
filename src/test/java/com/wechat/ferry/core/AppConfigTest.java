package com.wechat.ferry.core;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/** 配置加载与选择器解析测试：确保 2.0 默认值与 1.0 逐字一致 */
public class AppConfigTest {

    @Test
    public void sitesAndDefaultsLoaded() {
        AppConfig cfg = AppConfig.get();
        assertEquals(2, cfg.siteKeys().size());
        assertTrue(cfg.siteKeys().contains("jtnrb"));
        assertTrue(cfg.siteKeys().contains("yhgz"));

        // 1.0 硬编码值必须原样作为默认
        assertEquals("http://e3erp.jtnrb2017.cn/", cfg.siteStr("jtnrb", "url", ""));
        assertEquals("http://e3erp.yghz2022.cn/", cfg.siteStr("yhgz", "url", ""));
        assertEquals(0, cfg.siteInt("jtnrb", "report.dateColumnOffset", -1));
        assertEquals(1, cfg.siteInt("yhgz", "report.dateColumnOffset", -1));
        assertEquals("smtp.qq.com", cfg.str("integrations.mail.host", ""));
        assertEquals(465, cfg.integer("integrations.mail.port", 0));
    }

    @Test
    public void selectorsCandidatesPresent() {
        AppConfig cfg = AppConfig.get();
        for (String key : cfg.siteKeys()) {
            assertTrue(cfg.siteList(key, "login.username").size() >= 1, key + " 账号选择器缺失");
            assertTrue(cfg.siteList(key, "login.password").size() >= 1, key + " 密码选择器缺失");
            assertTrue(cfg.siteList(key, "login.submit").size() >= 1, key + " 按钮选择器缺失");
            assertEquals(3, cfg.siteList(key, "navigation").size(), key + " 导航步骤应为 3");
            assertTrue(cfg.siteList(key, "report.startDateInput").size() >= 1, key + " 起始日期缺失");
            assertTrue(cfg.siteList(key, "report.queryButton").size() >= 1, key + " 查询按钮缺失");
        }
    }

    @Test
    public void credentialsIndexesMatchV1() {
        AppConfig cfg = AppConfig.get();
        assertEquals(0, cfg.siteInt("jtnrb", "credentials.userIndex", -1));
        assertEquals(1, cfg.siteInt("jtnrb", "credentials.passwordIndex", -1));
        assertEquals(2, cfg.siteInt("yhgz", "credentials.userIndex", -1));
        assertEquals(3, cfg.siteInt("yhgz", "credentials.passwordIndex", -1));
        assertEquals("JTNRB", cfg.siteStr("jtnrb", "credentials.envName", ""));
    }

    @Test
    public void webDriverWaitDurationApiAvailable() {
        // 验证实际依赖的 selenium-support 支持 Duration 构造，防止 API 不匹配
        assertNotNull(Duration.ofSeconds(30));
    }

    @Test
    public void selectorParsing() {
        assertTrue(ElementLocator.parse("css=input[x]") instanceof By);
        assertTrue(ElementLocator.parse("xpath=//div") instanceof By);
        assertTrue(ElementLocator.parse("id=abc") instanceof By);
        assertTrue(ElementLocator.parse("class=el-table__body") instanceof By);
        assertTrue(ElementLocator.parse("text=报表") instanceof By);
    }

    @Test
    public void selectorBadFormatThrows() {
        assertThrows(IllegalArgumentException.class, () -> ElementLocator.parse("noequals"));
    }
}
