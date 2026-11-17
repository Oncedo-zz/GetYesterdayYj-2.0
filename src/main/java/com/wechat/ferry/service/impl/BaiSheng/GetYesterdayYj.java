package com.wechat.ferry.service.impl.BaiSheng;

import com.wechat.ferry.core.AppConfig;
import com.wechat.ferry.core.StepContext;
import com.wechat.ferry.service.impl.GetYesterdayYjServiceImpl;
import org.json.JSONObject;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 昨日业绩统计工具 2.0 入口。
 *
 * 用法：
 *   java -jar GetYesterdayYj-2.0.jar             无参数：交互输入日期 / 默认昨日，执行所有启用站点
 *   java -jar GetYesterdayYj-2.0.jar 0906        单个日期
 *   java -jar GetYesterdayYj-2.0.jar 0906 0908   日期区间
 *   java -jar GetYesterdayYj-2.0.jar --check     自检：只校验配置与凭据，不启动浏览器、不发请求
 *
 * 与 1.0 的行为差异：
 *   - 站点列表来自 sites.yml（原为硬编码 jtnrb + yhgz 两个方法）
 *   - 邮箱/WPS 密钥来自配置 + 环境变量（原为源码硬编码明文）
 *   - 失败立即抛异常并打印卡在哪一步（原为吞掉异常继续空转）
 */
public class GetYesterdayYj {

    private static final AppConfig CFG = AppConfig.get();
    private static final GetYesterdayYjServiceImpl SERVICE = new GetYesterdayYjServiceImpl();
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd");

    public static void main(String[] args) {
        // ---- 自检模式：不启动浏览器、不发网络请求 ----
        if (args != null && args.length > 0 && "--check".equalsIgnoreCase(args[0])) {
            runCheck();
            return;
        }

        if (args == null || args.length == 0) {
            String targetDate = waitForDateInput(15);
            runForDate(targetDate);
        } else if (args.length == 1) {
            String targetDate = parseDateInput(args[0]);
            if (targetDate == null) {
                printDateHelp(args[0]);
                System.exit(1);
            }
            System.out.println("Using date: " + targetDate);
            runForDate(targetDate);
        } else {
            handleRange(args[0], args[1]);
        }
    }

    /** 单个日期：抓所有站点 + WPS 流程（与 1.0 顺序一致） */
    private static void runForDate(String targetDate) {
        try {
            SERVICE.runAll(targetDate);
            executeWpsOperations(targetDate);
        } catch (RuntimeException e) {
            System.err.println("============================================================");
            System.err.println("[致命] 任务失败：" + e.getMessage());
            System.err.println("[提示] 若为定位失败，请在 jar 同目录的 sites.yml 中补充新选择器后重试，");
            System.err.println("       无需改代码、无需重新打包。");
            System.err.println("============================================================");
            throw e;
        }
    }

    private static void handleRange(String arg0, String arg1) {
        String startDateStr = parseDateInput(arg0);
        String endDateStr = parseDateInput(arg1);
        if (startDateStr == null || endDateStr == null) {
            printDateHelp(null);
            System.exit(1);
        }
        Calendar startCal = Calendar.getInstance();
        Calendar endCal = Calendar.getInstance();
        try {
            startCal.setTime(SDF.parse(startDateStr));
            endCal.setTime(SDF.parse(endDateStr));
        } catch (Exception e) {
            System.out.println("Error: Failed to parse date range");
            System.exit(1);
        }
        if (startCal.after(endCal)) {
            System.out.println("Error: Start date is after end date");
            System.exit(1);
        }

        System.out.println("========================================");
        System.out.println("Date Range: " + startDateStr + " to " + endDateStr);
        System.out.println("========================================");

        Calendar cur = startCal;
        while (!cur.after(endCal)) {
            String dateStr = SDF.format(cur.getTime());
            System.out.println("========================================");
            System.out.println("Processing date: " + dateStr);
            System.out.println("========================================");
            runForDate(dateStr);
            try { Thread.sleep(CFG.integer("defaults.wait.afterDateRangeLoopMs", 5000)); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            cur.add(Calendar.DATE, 1);
        }
        System.out.println("All dates in range completed!");
    }

    private static void printDateHelp(String bad) {
        if (bad != null) System.out.println("Error: Invalid date format: " + bad);
        System.out.println("Supported formats: yyyy-MM-dd, yyyyMMdd, MM-dd, MMdd");
    }

    // ==================================================================
    // 自检模式
    // ==================================================================
    private static void runCheck() {
        StepContext.line();
        System.out.println("GetYesterdayYj 2.0 配置自检（不启动浏览器、不发网络请求）");
        StepContext.line();
        StepContext.check("配置来源", CFG.sourceDescription());

        List<String> keys = CFG.siteKeys();
        StepContext.check("站点数量", String.valueOf(keys.size()));
        for (String key : keys) {
            boolean enabled = CFG.bool("sites." + key + ".enabled", true);
            System.out.println();
            System.out.println("  [站点] " + key + "  名称=" + CFG.siteStr(key, "displayName", key)
                + "  启用=" + enabled);
            StepContext.check("  URL", CFG.siteStr(key, "url", "<未配置>"));
            StepContext.check("  账号环境变量", CFG.siteStr(key, "credentials.envName", "JTNRB")
                + " [user下标=" + CFG.siteInt(key, "credentials.userIndex", -1)
                + ", pwd下标=" + CFG.siteInt(key, "credentials.passwordIndex", -1) + "]");
            StepContext.check("  登录选择器候选数",
                CFG.siteList(key, "login.username").size() + "/"
                    + CFG.siteList(key, "login.password").size() + "/"
                    + CFG.siteList(key, "login.submit").size() + " (账号/密码/按钮)");
            StepContext.check("  导航步骤数", String.valueOf(CFG.siteList(key, "navigation").size()));
            StepContext.check("  输出目录", CFG.siteStr(key, "output.saveDir", "<未配置>"));
            StepContext.check("  日期列偏移", String.valueOf(CFG.siteInt(key, "report.dateColumnOffset", 0)));
        }

        System.out.println();
        System.out.println("  [浏览器]");
        String driverPath = CFG.str("defaults.driver.chromeDriverPath", "");
        StepContext.check("  ChromeDriver", driverPath
            + (new File(driverPath).isFile() ? "  [存在]" : "  [!! 文件不存在 !!]"));
        String binaryPath = CFG.str("defaults.driver.browserBinaryPath", "");
        if (!binaryPath.isEmpty()) {
            StepContext.check("  Chrome 二进制", binaryPath
                + (new File(binaryPath).isFile() ? "  [存在]" : "  [!! 文件不存在 !!]"));
        } else {
            StepContext.check("  Chrome 二进制", "[系统默认] 未指定，将用系统 Chrome（MV2 插件可能加载失败）");
        }
        StepContext.check("  扩展目录", CFG.str("defaults.driver.extensionPath", ""));

        System.out.println();
        System.out.println("  [凭据校验]");
        String jt = System.getenv("JTNRB");
        if (jt == null || jt.trim().isEmpty()) {
            StepContext.check("  JTNRB", "[!! 未设置 !!] 请设置 4 段：账号,密码,账号,密码");
        } else {
            String[] parts = jt.split(",", -1);
            StepContext.check("  JTNRB", "已设置，共 " + parts.length + " 段"
                + (parts.length < 4 ? "  [!! 少于 4 段，可能不完整 !!]" : ""));
        }

        System.out.println();
        System.out.println("  [外部集成]");
        StepContext.check("  WPS Token", mask(CFG.str("integrations.wps.token", "")));
        StepContext.check("  WPS 同步地址", CFG.str("integrations.wps.syncUrl", ""));
        StepContext.check("  邮件 SMTP", CFG.str("integrations.mail.host", "") + ":"
            + CFG.integer("integrations.mail.port", 465));
        StepContext.check("  邮件发件人", CFG.str("integrations.mail.sender", ""));
        StepContext.check("  邮件密码", mask(CFG.str("integrations.mail.password", "")));

        StepContext.line();
        System.out.println("自检结束。若上面各项均正常，可直接运行；");
        System.out.println("服务端改版时：编辑 jar 同目录的 sites.yml，补一条新选择器即可，无需重新打包。");
        StepContext.line();
    }

    private static String mask(String v) {
        if (v == null || v.isEmpty()) return "<空>";
        if (v.length() <= 4) return "****";
        return v.substring(0, 2) + "****" + v.substring(v.length() - 2);
    }

    // ==================================================================
    // 日期输入（与 1.0 逻辑一致）
    // ==================================================================
    private static String waitForDateInput(int timeoutSeconds) {
        final String[] userInput = {null};
        final AtomicBoolean done = new AtomicBoolean(false);

        System.out.print("请在 " + timeoutSeconds + " 秒内输入日期（yyyy-MM-dd / yyyyMMdd / MM-dd / MMdd）: ");
        Thread inputThread = new Thread(() -> {
            try {
                Scanner scanner = new Scanner(System.in);
                if (scanner.hasNextLine()) userInput[0] = scanner.nextLine();
            } catch (Exception ignored) {
            } finally {
                done.set(true);
            }
        });
        Thread countdownThread = new Thread(() -> {
            try {
                for (int i = timeoutSeconds; i > 0; i--) {
                    if (done.get()) break;
                    if (i <= 5) {
                        System.out.print("\n请在 " + i + " 秒内完成输入: ");
                        System.out.flush();
                    }
                    Thread.sleep(1000);
                }
            } catch (InterruptedException ignored) {
            }
        });
        inputThread.setDaemon(true);
        countdownThread.setDaemon(true);
        inputThread.start();
        countdownThread.start();

        try {
            long start = System.currentTimeMillis();
            while (!done.get() && System.currentTimeMillis() - start <= timeoutSeconds * 1000L) {
                Thread.sleep(50);
            }
            countdownThread.interrupt();
            countdownThread.join(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println();

        if (userInput[0] != null && !userInput[0].trim().isEmpty()) {
            String parsed = parseDateInput(userInput[0].trim());
            if (parsed != null) {
                System.out.println("使用输入日期: " + parsed);
                return parsed;
            }
            System.out.println("日期格式错误，使用昨日日期");
        }
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        String yesterday = new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime());
        System.out.println("默认使用昨日日期: " + yesterday);
        return yesterday;
    }

    /** 支持 yyyy-MM-dd / yyyyMMdd / MM-dd / MMdd，供单测直接调用 */
    static String parseDateInput(String input) {
        if (input == null) return null;
        input = input.trim();
        int year = Calendar.getInstance().get(Calendar.YEAR);
        if (input.matches("\\d{4}-\\d{2}-\\d{2}")) return input;
        if (input.matches("\\d{8}")) {
            return input.substring(0, 4) + "-" + input.substring(4, 6) + "-" + input.substring(6, 8);
        }
        if (input.matches("\\d{2}-\\d{2}")) return year + "-" + input;
        if (input.matches("\\d{4}")) {
            return year + "-" + input.substring(0, 2) + "-" + input.substring(2, 4);
        }
        return null;
    }

    // ==================================================================
    // WPS 与邮件（逻辑与 1.0 一致，参数改为配置读取）
    // ==================================================================
    private static void executeWpsOperations(String date) {
        String processName = CFG.str("integrations.wps.processName", "wps.exe");
        try {
            if (isProcessRunning(processName)) {
                System.out.println(processName + " 正在运行，等待上传完成后与 WPS 交互");
                countdown(CFG.integer("integrations.wps.uploadWaitSeconds", 60));
                String status = wpsDone(date);
                sendResultMail(status, date);
            } else {
                System.out.println(processName + " 未运行，正在启动 WPS");
                runProcess(CFG.str("integrations.wps.launcherPath", ""));
                countdown(CFG.integer("integrations.wps.uploadWaitSeconds", 60));
                String status = wpsDone(date);
                sendResultMail(status, date);
            }
            countdown(5);
            wpsDuizhang();
            countdown(10);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void sendResultMail(String wpsStatus, String date) {
        String title, body;
        if ("finished".equals(wpsStatus)) {
            title = "销售对账报表" + date;
            body = "这是街头男人帮" + date + "的销售业绩，请到WPS中对账。本邮件用Java发送。";
            sendMail(CFG.str("integrations.mail.attachmentPath", ""), title, body);
        } else {
            title = "WPS业绩导入错误";
            body = "业绩已导出，但是WPS未获取到业绩，3分钟后会重试一次，用Java发送。";
            countdown(CFG.integer("integrations.wps.retryWaitSeconds", 180));
            String retry = wpsDone(date);
            if (!"finished".equals(retry)) {
                body = "业绩已导出，但是WPS未获取到业绩，请到WPS查看状态，用Java发送。";
            }
            sendMail(null, title, body);
        }
    }

    private static void sendMail(String filePath, String title, String body) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", String.valueOf(CFG.bool("integrations.mail.auth", true)));
        props.put("mail.smtp.ssl.enable", String.valueOf(CFG.bool("integrations.mail.ssl", true)));
        props.put("mail.smtp.host", CFG.str("integrations.mail.host", "smtp.qq.com"));
        props.put("mail.smtp.port", String.valueOf(CFG.integer("integrations.mail.port", 465)));

        final String user = CFG.str("integrations.mail.sender", "");
        final String pass = CFG.str("integrations.mail.password", "");
        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, pass);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(user));
            message.setRecipients(Message.RecipientType.TO,
                InternetAddress.parse(CFG.str("integrations.mail.recipient", "")));
            message.setSubject(title);

            Multipart multipart = new MimeMultipart();
            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setText(body);
            multipart.addBodyPart(textPart);

            if (filePath != null && !filePath.isEmpty() && new File(filePath).isFile()) {
                MimeBodyPart attachmentPart = new MimeBodyPart();
                DataSource source = new FileDataSource(filePath);
                attachmentPart.setDataHandler(new DataHandler(source));
                attachmentPart.setFileName(new File(filePath).getName());
                multipart.addBodyPart(attachmentPart);
            }
            message.setContent(multipart);
            Transport.send(message);
            System.out.println("邮件发送成功");
        } catch (Exception e) {
            System.out.println("邮件发送失败: " + e.getMessage());
        }
    }

    private static void countdown(int seconds) {
        try {
            for (int i = seconds; i > 0; i--) {
                System.out.print("\r倒计时: " + i + "秒");
                TimeUnit.SECONDS.sleep(1);
            }
            System.out.println("\r倒计时 " + seconds + " 秒结束！");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String wpsDone(String dateIn) {
        return callWps(CFG.str("integrations.wps.syncUrl", ""), dateIn);
    }

    private static void wpsDuizhang() {
        System.out.println("准备执行对账更新");
        callWps(CFG.str("integrations.wps.duizhangUrl", ""), "");
        System.out.println("已执行对账更新");
    }

    private static String callWps(String urlStr, String dateIn) {
        String token = CFG.str("integrations.wps.token", "");
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("AirScript-Token", token);
            conn.setDoOutput(true);
            String json = "{\"Context\": {\"argv\": {\"date_in\": \"" + dateIn + "\"}}}";
            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes("UTF-8"));
            }
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                JSONObject obj = new JSONObject(sb.toString());
                return obj.optString("status", "error");
            }
        } catch (Exception e) {
            System.err.println("WPS 交互错误: " + e.getMessage());
            return "error";
        }
    }

    private static void runProcess(String path) {
        try {
            if (path == null || path.isEmpty()) {
                System.out.println("未配置 WPS 启动路径，跳过");
                return;
            }
            Runtime.getRuntime().exec(path);
            System.out.println("启动 " + path + " 成功");
        } catch (IOException e) {
            System.out.println("启动 WPS 失败: " + e.getMessage());
        }
    }

    private static boolean isProcessRunning(String processName) {
        try {
            Process process = Runtime.getRuntime().exec("tasklist");
            Scanner scanner = new Scanner(process.getInputStream());
            while (scanner.hasNextLine()) {
                if (scanner.nextLine().contains(processName)) {
                    scanner.close();
                    return true;
                }
            }
            scanner.close();
        } catch (IOException e) {
            System.err.println("进程检查错误: " + e.getMessage());
        }
        return false;
    }
}
