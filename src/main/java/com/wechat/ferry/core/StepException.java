package com.wechat.ferry.core;

import java.util.List;

/**
 * 带站点/日期/步骤上下文的失败异常。
 * 2.0 起所有定位失败、登录失败、数据提取失败都抛出本异常，
 * 绝不再出现"吞掉异常后返回无效 driver，导致后续每一步空转 30 秒"的假死。
 */
public class StepException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String site;
    private final String date;
    private final String step;

    public StepException(String site, String date, String step, String message) {
        super("[" + site + "][" + date + "][" + step + "] " + message);
        this.site = site;
        this.date = date;
        this.step = step;
    }

    public StepException(String site, String date, String step, String message, Throwable cause) {
        super("[" + site + "][" + date + "][" + step + "] " + message, cause);
        this.site = site;
        this.date = date;
        this.step = step;
    }

    public String getSite() { return site; }
    public String getDate() { return date; }
    public String getStep() { return step; }
}
