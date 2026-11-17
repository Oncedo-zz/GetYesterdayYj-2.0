package com.wechat.ferry.core;

/**
 * 统一的步骤日志。每一分钟卡在哪一步都能一眼看出，避免"假死"。
 * 只依赖 JDK，不依赖日志框架，保证在 main 启动时立即可用。
 */
public final class StepContext {

    private static final String LINE = "------------------------------------------------------------";

    private StepContext() {}

    public static void start(String site, String date) {
        System.out.println(LINE);
        System.out.println("[任务] 站点=" + site + "  日期=" + date);
        System.out.println(LINE);
    }

    public static void step(String site, String date, String step) {
        System.out.println("[" + site + "][" + date + "][步骤] " + step);
    }

    public static void ok(String site, String date, String step) {
        System.out.println("[" + site + "][" + date + "][完成] " + step);
    }

    public static void warn(String site, String date, String step, String detail) {
        System.out.println("[" + site + "][" + date + "][警告] " + step + " -> " + detail);
    }

    public static void fail(String site, String date, String step, String detail) {
        System.err.println("[" + site + "][" + date + "][失败] " + step + " -> " + detail);
    }

    public static void check(String label, String value) {
        System.out.println("  " + label + " : " + value);
    }

    public static void line() {
        System.out.println(LINE);
    }
}
