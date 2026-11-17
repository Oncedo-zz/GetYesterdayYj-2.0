package com.wechat.ferry.service.impl.BaiSheng;

import com.wechat.ferry.core.BrowserLogin;
import org.openqa.selenium.WebDriver;

/**
 * 兼容包装类（2.0）。原 Login 逻辑已重构为 {@link BrowserLogin}。
 * 保留原类名与原方法签名（get_driver / LogIn(WebDriver,String,String,String) / safeQuit）。
 *
 * 注意：原 LogIn(WebDriver, url, user, pwd) 中 url 参数在新版仍被使用；
 * 站点 key 通过 URL 反查配置，找不到时回退到 jtnrb 的选择器集合（两者一致）。
 */
public class Login {

    private final BrowserLogin delegate = new BrowserLogin();

    public WebDriver get_driver() {
        return delegate.get_driver();
    }

    public WebDriver LogIn(WebDriver driver, String url, String username, String password) {
        return delegate.LogIn(driver, resolveSiteKey(url), url, username, password);
    }

    public void safeQuit(WebDriver driver) {
        delegate.safeQuit(driver);
    }

    private String resolveSiteKey(String url) {
        if (url == null) return "jtnrb";
        String u = url.toLowerCase();
        if (u.contains("yghz")) return "yhgz";
        if (u.contains("jtnrb")) return "jtnrb";
        return "jtnrb";
    }
}
