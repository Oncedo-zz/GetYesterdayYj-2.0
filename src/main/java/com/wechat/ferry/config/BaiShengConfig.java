package com.wechat.ferry.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "baisheng")
public class BaiShengConfig {
    private String jtnrbUrl;

    private String yhgzUrl;
}
