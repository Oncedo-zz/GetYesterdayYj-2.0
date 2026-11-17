package com.wechat.ferry.service.impl.BaiSheng;//package com.wechat.ferry.service.impl.BaiSheng;
//
//
//// Selenium
//import com.alibaba.fastjson2.JSONObject;
//import com.wechat.ferry.enums.JTNRB_ACCOUNT;
//import com.wechat.ferry.service.impl.Wps;
//import org.openqa.selenium.*;
//import org.openqa.selenium.chrome.ChromeDriver;
//import org.openqa.selenium.chrome.ChromeOptions;
//import org.openqa.selenium.support.ui.ExpectedConditions;
//import org.openqa.selenium.support.ui.WebDriverWait;
//import org.openqa.selenium.interactions.Action;
//import org.openqa.selenium.interactions.Actions;
//
//// Apache POI
//import org.apache.poi.hssf.usermodel.HSSFWorkbook;
//import org.apache.poi.ss.usermodel.Cell;
//import org.apache.poi.ss.usermodel.CellStyle;
//import org.apache.poi.ss.usermodel.Font;
//import org.apache.poi.ss.usermodel.Row;
//import org.apache.poi.ss.usermodel.Sheet;
//import org.apache.poi.ss.usermodel.Workbook;
//
//// JavaMail
//import javax.mail.*;
//import javax.mail.internet.InternetAddress;
//import javax.mail.internet.MimeBodyPart;
//import javax.mail.internet.MimeMessage;
//import javax.mail.internet.MimeMultipart;
//
//// 日期/时间
//import java.io.FileInputStream;
//import java.time.Duration;
//import java.time.Instant;
//import java.time.LocalDate;
//import java.time.ZoneId;
//import java.time.format.DateTimeFormatter;
//import java.util.*;
//
//// 文件/IO
//import java.io.File;
//import java.io.FileOutputStream;
//import java.io.IOException;
//
//// 多线程
//import java.util.NoSuchElementException;
//import java.util.concurrent.ExecutionException;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.Future;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.TimeoutException;
//import java.util.stream.Collectors;
//
//
//public class GetYesterdayYj_NRB_Old {
//
//    // 配置常量
//    private static final String DRIVER_PATH = "C:\\Plugs\\chrome-win64\\chromedriver.exe";
//    private static final String DOWNLOAD_DIR = "C:\\Users\\Administrator\\Desktop\\Performance\\";
//    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//    private static final Duration DEFAULT_WAIT = Duration.ofSeconds(10);
//    private static final DateTimeFormatter DAY_CLICK_FORMATTER =
//        DateTimeFormatter.ofPattern("yyyy,M,d");
//    private static final String jtnrb_url = "http://47.97.230.6/ipos/web/manage/privilege.php";
////    private static final String jtnrb_url = "http://www.baidu.com;";
//
//    private WebDriver driver;
//    private WebDriverWait wait;
//    private LocalDate reportDate;
//
//    // 其他接口中的类 需要实例化
//    private final JTNRB_ACCOUNT jtnrbAccount = new JTNRB_ACCOUNT();
//    private final Wps wps = new Wps();
//
//
//    public static void main(String[] args) {
//        GetYesterdayYj_NRB_Old getYesterdayYjNrbOld = new GetYesterdayYj_NRB_Old();
//        getYesterdayYjNrbOld.getYesterdayYj();
//    }
//
//
//
//    public void getYesterdayYj() {
//        try {
//            // 步骤1: 获取报告日期
//            reportDate = getReportDate();
//
//            // 步骤2: 检查文件是否存在
//            if(checkFileExists()) return;
//
//            // 步骤3: 初始化浏览器
//            setupBrowser();
//
//            // 步骤4: 登录系统
//            login(jtnrbAccount.getJtnrbUser(), jtnrbAccount.getJtnrbPwd());
//
//            // 步骤5: 导航到报表页面
//            navigateToSalesReport();
//
//            // 步骤6: 提取销售对账报表的数据
//            List<Map<String, String>> data_xsdzbb = click_XiaoShouDuiZhangBaoBiao(reportDate);
//
//            // 步骤7: 提取终端业绩统计数据
//            List<Map<String, String>> data_zdyjtj = click_ZhongDuanYeJiTongJi(reportDate);
//
//            System.out.println("销售对账报表：\n" + data_xsdzbb);
//            System.out.println("\n终端业绩统计\n" + data_zdyjtj);
//            // 步骤8：终端业绩提取
////            collectPerformanceData();
//
////            // 步骤8: 生成Excel文件
////            generateExcel();
////
////            // 步骤9: 发送邮件
////            sendEmail();
////
////            // 步骤10: 激活对账更新
////            handleWpsIntegration();
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            if(driver != null) driver.quit();
//        }
//    }
//
//    // 各功能方法实现见下文...
//
//    // 登录方法（抽离的核心功能）
//    private void login(String user, String pass) {
//
//        try {
//            driver.getTitle();
//            System.out.println(jtnrb_url);
//            driver.get(jtnrb_url);
//            System.out.println("已打开系统链接");
//            driver.getTitle();
//
//            WebElement orgnameField = driver.findElement(By.id("orgname"));
//            WebElement usernameField = driver.findElement(By.id("username"));
//            WebElement passwordField = driver.findElement(By.id("password"));
//            WebElement submitButton = driver.findElement(By.id("sub"));
//
//            orgnameField.sendKeys("000");
//            usernameField.sendKeys(user);
//            passwordField.sendKeys(pass);
//            submitButton.click();
//            System.out.println("已点击登录按钮");
//
//            Thread.sleep(1500);
//            System.out.println(driver.getTitle());
//            if (driver.getTitle().equals("- 系统信息")) {
//                System.out.println("登陆账号密码错误，请联系管理员");
//                driver.quit();
//
//                return;
//            }
//
//
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//
//    }
//
//    // 导航到销售报表页面
//    private void navigateToSalesReport() {
//        // 切换到header框架
//        switchToFrame(driver, "frame_header");
//
//        // 操作导航栏
//        clickElement(driver, PageElements.NAV_BAR_ITEM);
//
//        // 返回主框架
//        switchToDefaultContent(driver);
//
//        // 切换到菜单框架
//        switchToFrame(driver, "frame_menu");
//
//        // 操作菜单项
//        clickElement(driver, PageElements.MENU_ITEM);
//
//    }
//
//    private static void switchToFrame(WebDriver driver, String frameId) {
//        new WebDriverWait(driver, DEFAULT_WAIT)
//            .until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(frameId));
//    }
//
//    private static void switchToDefaultContent(WebDriver driver) {
//        driver.switchTo().defaultContent();
//    }
//
//    private static void clickElement(WebDriver driver, By locator) {
//        WebElement element = new WebDriverWait(driver, DEFAULT_WAIT)
//            .until(ExpectedConditions.elementToBeClickable(locator));
//        element.click();
//    }
//
//    private static class PageElements {
//        static final By NAV_BAR_ITEM = By.xpath("//*[@id='nav_bar']/li[6]/a");
//        static final By MENU_ITEM = By.id("60705");
//        static final By SALES_REPORT_LINK = By.xpath("//*[@id='li_100074371']/span[1]/a");
//        static final By ZhongDuanYeJTJ = By.xpath("//*[@id='li_715']/span[1]/a");
//
//    }
//
//    // 日期处理
//    private LocalDate getReportDate() throws ExecutionException, InterruptedException {
//        // 使用ExecutorService实现超时控制
//        ExecutorService executor = Executors.newSingleThreadExecutor();
//        Future<String> future = executor.submit(() -> {
//            System.out.print("请输入日期(例：2024-11-15)（10秒内）：");
//            return new Scanner(System.in).nextLine();
//        });
//
//        try {
//            return LocalDate.parse(future.get(3, TimeUnit.SECONDS), DATE_FORMAT);
//        } catch (TimeoutException e) {
//            System.out.println("\n超时，使用昨日日期");
//            return LocalDate.now().minusDays(1);
//        } finally {
//            executor.shutdownNow();
//        }
//    }
//
//    private boolean checkFileExists() {
//        String filePath = DOWNLOAD_DIR + "销售对账报表" + reportDate.format(DATE_FORMAT) + ".xls";
//        File file = new File(filePath);
//
//        if (file.exists()) {
//            long lastModified = file.lastModified();
//            // 修复：添加Instant和ZoneId的导入
//            LocalDate fileDate = Instant.ofEpochMilli(lastModified)
//                .atZone(ZoneId.systemDefault())
//                .toLocalDate();
//
//            if (fileDate.isEqual(LocalDate.now())) {
//                System.out.println("文件已存在且为今日生成");
//                System.exit(0);
//            }
//        }
//        return false;
//    }
//
//    // 选择 提取销售对账报表数据 开始日期及结束日期
//    private List<Map<String, String>> click_XiaoShouDuiZhangBaoBiao(LocalDate yestereday) {
//        // 进入左侧菜单框架
//        switchToDefaultContent(driver);
//        switchToFrame(driver, "frame_menu");
//
//        // 点击销售对账报表
//        clickElement(driver, PageElements.SALES_REPORT_LINK);
//
//        // 进入详情框架
//        switchToDefaultContent(driver);
//        switchToFrame(driver, "frame_details_1");
//
//        // 选择开始日期
//        driver.findElement(By.xpath("/html/body/div[4]/div/div/form/input[1]")).click();
//        selectYesterdayDate(yestereday);
//
//        // 选择结束日期
//        driver.findElement(By.xpath("/html/body/div[4]/div/div/form/input[2]")).click();
//        selectYesterdayDate(yestereday);
//
//        // 点击 搜索 按钮
//        driver.findElement(By.xpath("/html/body/div[4]/div/div/form/input[10]")).click();
//
//        try {
//            Thread.sleep(1000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//
//        // 将页面调整至显示200个记录
//        WebElement pageSize_element = driver.findElement(By.id("pageSize"));
//        Actions actions = new Actions(driver);
//        // actions.sendKeys(element, "0"); // 如果需要发送键到元素，可以取消注释
//        pageSize_element.sendKeys("0");
//        actions.sendKeys(pageSize_element, Keys.ENTER);
//        Action perform = actions.build();
//        perform.perform();
//
//        System.out.println("已设置好页面所显示的数量为200");
//
//        try {
//            Thread.sleep(5000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//
//        List<Map<String, String>> data = extractTableData();
//
//        System.out.println("销售对账报表的数据已获取完成");
//
//        try {
//            Thread.sleep(2000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//
//        // 返回提取的数据
//        return data;
//    }
//
//    // 选择 提取终端业绩统计数据 开始日期及结束日期
//    private List<Map<String, String>> click_ZhongDuanYeJiTongJi(LocalDate yestereday) {
//        // 进入左侧菜单框架
//        switchToDefaultContent(driver);
//        switchToFrame(driver, "frame_menu");
//
//        // 点击销售对账报表
//        clickElement(driver, PageElements.ZhongDuanYeJTJ);
//
//        // 进入详情框架
//        switchToDefaultContent(driver);
//        switchToFrame(driver, "frame_details_2");
//
//        // 选择开始日期
//        driver.findElement(By.xpath("/html/body/div[4]/div/form[1]/input[1]")).click();
//        selectYesterdayDate(yestereday);
//
//        // 选择结束日期
//        driver.findElement(By.xpath("/html/body/div[4]/div/form/input[2]")).click();
//        selectYesterdayDate(yestereday);
//
//        // 点击 搜索 按钮
//        driver.findElement(By.xpath("/html/body/div[4]/div/form[1]/input[16]")).click();
//
//        try {
//            Thread.sleep(1000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//
//        // 将页面调整至显示200个记录
//        WebElement pageSize_element = driver.findElement(By.id("pageSize"));
//        Actions actions = new Actions(driver);
//        // actions.sendKeys(element, "0"); // 如果需要发送键到元素，可以取消注释
//        pageSize_element.sendKeys("0");
//        actions.sendKeys(pageSize_element, Keys.ENTER);
//        Action perform = actions.build();
//        perform.perform();
//
//        try {
//            Thread.sleep(3000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//
//        System.out.println("已设置好页面所显示的数量为200");
//
//        List<Map<String, String>> data = extractTableData();
//
//        // 返回提取的数据
//        return data;
//    }
//
//    // 选择日期
//    private void selectYesterdayDate(LocalDate yesterday) {
//        try {
//            // 切换到日期选择框架
//            driver.switchTo().frame(0);
//            TimeUnit.SECONDS.sleep(1);
//
//            // 生成目标日期字符串
//            String dayClickText = yesterday.format(DAY_CLICK_FORMATTER);
//            String xpath = String.format(
//                "//td[contains(@onclick, 'day_Click(%s);')]",
//                dayClickText
//            );
//
//            try {
//                WebElement dateElement = new WebDriverWait(driver, DEFAULT_WAIT)
//                    .until(ExpectedConditions.elementToBeClickable(By.xpath(xpath)));
//                dateElement.click();
//                driver.switchTo().parentFrame();
//                TimeUnit.SECONDS.sleep(1);
//            } catch (org.openqa.selenium.TimeoutException e) {
//                handleFallbackSelection(driver, yesterday);
//            }
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//        }
//    }
//
//    private static void handleFallbackSelection(WebDriver driver, LocalDate yesterday) {
//        LocalDate today = LocalDate.now();
//        if (yesterday.equals(today.withDayOfMonth(1).minusDays(1))) {
//            handleFirstDayOfMonth(driver);
//        } else {
//            handleNormalFallback(driver);
//        }
//    }
//
//    private static void handleFirstDayOfMonth(WebDriver driver) {
//        try {
//            // 定位日历导航控件
//            WebElement dpTitle = new WebDriverWait(driver, DEFAULT_WAIT)
//                .until(ExpectedConditions.presenceOfElementLocated(By.id("dpTitle")));
//
//            // 点击上个月按钮
//            WebElement prevMonthButton = dpTitle.findElement(
//                By.cssSelector("div.navImg.NavImgl a"));
//            prevMonthButton.click();
//
//            // 选择最后一天
//            WebElement dayTable = new WebDriverWait(driver, DEFAULT_WAIT)
//                .until(ExpectedConditions.presenceOfElementLocated(
//                    By.cssSelector(".WdayTable")));
//
//            List<WebElement> wwdayElements = dayTable.findElements(
//                By.cssSelector("td.Wwday"));
//
//            if (!wwdayElements.isEmpty()) {
//                wwdayElements.get(wwdayElements.size() - 1).click();
//                System.out.println("点击了最后一天");
//                driver.switchTo().parentFrame();
//            }
//        } catch (org.openqa.selenium.TimeoutException e) {
//            System.out.println("备用方案执行失败");
//        }
//    }
//
//    private static void handleNormalFallback(WebDriver driver) {
//        try {
//            WebElement selectedDay = driver.findElement(By.className("Wselday"));
//            String targetOnclick = selectedDay.getAttribute("onclick");
//
//            List<WebElement> days = driver.findElements(By.tagName("td"));
//            for (int i = 0; i < days.size(); i++) {
//                if (targetOnclick.equals(days.get(i).getAttribute("onclick"))) {
//                    if (i > 0) {
//                        days.get(i - 1).click();
//                        TimeUnit.SECONDS.sleep(2);
//                        driver.switchTo().parentFrame();
//                        break;
//                    }
//                }
//            }
//        } catch (NoSuchElementException | InterruptedException e) {
//            System.out.println("常规备用方案执行失败");
//            Thread.currentThread().interrupt();
//        }
//    }
//
//    // 终端业绩统计采集
////    private void collectPerformanceData() {
////        // 切换到菜单框架
////        switchToFrame(driver, "frame_menu");
////
////        // 点击终端业绩统计
////        clickElement(driver, By.xpath("//*[contains(text(),'终端业绩统计')]"));
////
////        // 切换到详情框架
////        switchToDefaultContent(driver);
////        switchToFrame(driver, "frame_details_2");
////
////        // 日期选择（复用已有方法）
////        selectDate(reportDate);
////
////        // 提取业绩数据
////        List<WebElement> performanceRows = driver.findElements(By.cssSelector(".performance-table tr"));
////        // 数据存储与合并逻辑...
////    }
//
//    // 浏览器初始化
//    private void setupBrowser() {
//        System.setProperty("webdriver.chrome.driver", DRIVER_PATH);
//
//        ChromeOptions options = new ChromeOptions();
//        //options.addArguments("--headless"); // 无头模式
//        options.addArguments("--disable-gpu");
////        options.addArguments("--window-size=1920,1080");
//        options.addArguments("--no-sandbox");          // Linux必加
//        options.addArguments("--disable-dev-shm-usage");
//        options.addArguments("--remote-allow-origins=*"); // Selenium 4需要此参数
//        System.setProperty("webdriver.chrome.logfile", "C:\\logs\\chromedriver.log");
//        options.setBinary("C:\\Users\\Administrator\\AppData\\Local\\Google\\Chrome\\Application\\chrome.exe"); // 完整路径
//
//        driver = new ChromeDriver(options);
//
//        // 最大化窗口
//        driver.manage().window().maximize();
//        System.out.println("窗口已经最大化");
//
//        wait = new WebDriverWait(driver, Duration.ofSeconds(30));
//        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(30));
//        System.out.println("\n\n\n\n\n\ndriver已设置完成\n\n\n\n\n\n");
//    }
//
//    // 表格数据提取
//    public List<Map<String, String>> extractTableData() {
//        List<Map<String, String>> tableData = null;
//        try {
//            // 1. 定位表格容器
//            WebElement tableContainer = wait.until(ExpectedConditions.presenceOfElementLocated(
//                By.cssSelector("div.li-table table.list-div")
//            ));
//
//            // 2. 提取表头数据
////            List<String> headers = extractHeaders(tableContainer);
////            System.out.println("表头: " + headers);
//
//            // 3. 提取表格内容
//            tableData = extractTableBody(tableContainer);
//
//            // 4. 打印结果
////            tableData.forEach(row -> {
////                System.out.println("\n--- 行数据 ---");
////                row.forEach((key, value) ->
////                    System.out.printf("%-15s: %s%n", key, value)
////                );
////            });
//            return tableData;
//        } catch (Exception e) {
//            System.err.println("数据提取失败: " + e.getMessage());
//            return tableData;
//        }
//    }
//
//    // 提取表头方法
//    private List<String> extractHeaders(WebElement table) {
//        return table.findElements(By.cssSelector("thead tr[nowrap] th"))
//            .stream()
//            .map(WebElement::getText)
//            .collect(Collectors.toList());
//    }
//
//    // 提取表格内容方法
//    private List<Map<String, String>> extractTableBody(WebElement table) {
//        List<Map<String, String>> data = new ArrayList<>();
//        List<WebElement> rows = table.findElements(By.cssSelector("tbody tr[id='tr-']"));
//
//        for (WebElement row : rows) {
//            Map<String, String> rowData = new LinkedHashMap<>();
//            List<WebElement> cells = row.findElements(By.cssSelector("td"));
//
//            // 根据实际列索引映射数据
//            if (Objects.equals(driver.findElement(By.className("action-span1")).getText(), "销售对帐报表")) {
//                rowData.put("终端代码", getCellText(cells, 0));
//                rowData.put("终端名称", getCellText(cells, 1));
//                rowData.put("销售日期", getCellText(cells, 2));
//                rowData.put("销售收入", getCellText(cells, 3));
//                rowData.put("订金转销售", getCellText(cells, 4));
//                rowData.put("订金收款", getCellText(cells, 5));
//                rowData.put("订金退款", getCellText(cells, 6));
//                rowData.put("当前流入", getCellText(cells, 7));
//
//                data.add(rowData);
//            } else if (Objects.equals(driver.findElement(By.className("action-span1")).getText(), "终端业绩统计")) {
//                try {
//                    rowData.put("终端名称", getCellText(cells, 5));
//                    rowData.put("销售数量", getCellText(cells, 8));
//                    rowData.put("吊牌金额", getCellText(cells, 9));
//                    rowData.put("平均折扣", getCellText(cells, 10));
//                    rowData.put("平均单价", getCellText(cells, 11));
//                    rowData.put("小票数", getCellText(cells, 18));
//                    rowData.put("客单价", getCellText(cells, 19));
//                    rowData.put("连带率", getCellText(cells, 20));
//
//                    data.add(rowData);
//                } catch (IndexOutOfBoundsException e) {
//                    System.err.println("列数不匹配: " + e.getMessage());
//                }
//            }
//        }
//        return data;
//    }
//
//    // 安全获取单元格内容
//    private String getCellText(List<WebElement> cells, int index) {
//        if (index >= 0 && index < cells.size()) {
//            WebElement cell = cells.get(index);
//            // 优先获取span内容
//            List<WebElement> spans = cell.findElements(By.tagName("span"));
//            return spans.isEmpty() ?
//                cell.getText().trim() :
//                spans.get(0).getText().trim();
//        }
//        return "N/A";
//    }
//
//    // 数据合并
//    private void mergeData(List<List<String>> salesData,
//                           List<List<String>> performanceData) {
//        // 根据终端名称匹配两个数据源
//        salesData.forEach(sales -> {
//            performanceData.stream()
//                .filter(p -> p.get(1).equals(sales.get(1)))
//                .findFirst()
//                .ifPresent(p -> sales.addAll(p.subList(8, 15)));
//        });
//    }
//
//    // Excel生成
//    private void generateExcel(List<List<String>> data) throws IOException {
//        // 使用HSSFWorkbook生成xls格式
//        try (Workbook workbook = new HSSFWorkbook()) {
//            Sheet sheet = workbook.createSheet("销售数据");
//
//            // 创建表头样式（可选）
//            CellStyle headerStyle = workbook.createCellStyle();
//            Font headerFont = workbook.createFont();
//            headerFont.setBold(true);
//            headerStyle.setFont(headerFont);
//
//            // 创建表头
//            Row headerRow = sheet.createRow(0);
//            String[] headers = {"终端代码", "终端名称", "销售日期", "销售收入",
//                "订金转销售", "订金收款", "订金退款", "当前流入",
//                "销售数量", "吊牌金额", "平均折扣", "平均单价",
//                "小票数", "客单价", "连带率"};
//
//            for (int i = 0; i < headers.length; i++) {
//                Cell cell = headerRow.createCell(i);
//                cell.setCellValue(headers[i]);
//                cell.setCellStyle(headerStyle); // 应用样式
//                sheet.setColumnWidth(i, 20 * 256); // 设置列宽（20字符）
//            }
//
//            // 填充数据
//            for (int rowNum = 0; rowNum < data.size(); rowNum++) {
//                Row row = sheet.createRow(rowNum + 1);
//                List<String> rowData = data.get(rowNum);
//
//                for (int colNum = 0; colNum < rowData.size(); colNum++) {
//                    Cell cell = row.createCell(colNum);
//                    cell.setCellValue(rowData.get(colNum));
//                }
//            }
//
//            // 保存为xls文件
//            String filePath = DOWNLOAD_DIR + "销售对账报表" +
//                reportDate.format(DATE_FORMAT) + ".xls";
//            try (FileOutputStream fos = new FileOutputStream(filePath)) {
//                workbook.write(fos);
//                System.out.println("Excel文件已生成: " + filePath);
//            }
//        }
//    }
//
//    //  邮件发送
//    private void sendEmail() throws MessagingException, IOException {
//        Properties props = new Properties();
//        props.put("mail.smtp.host", "smtp.qq.com");
//        props.put("mail.smtp.port", "465");
//        props.put("mail.smtp.ssl.enable", "true");
//        props.put("mail.smtp.auth", "true");
//
//        Session session = Session.getInstance(props, new Authenticator() {
//            protected PasswordAuthentication getPasswordAuthentication() {
//                return new PasswordAuthentication(System.getenv("MAIL_SENDER"), System.getenv("MAIL_PASSWORD"));
//            }
//        });
//
//        MimeMessage message = new MimeMessage(session);
//        message.setFrom(new InternetAddress("zks2007@qq.com"));
//        message.addRecipient(Message.RecipientType.TO, new InternetAddress("373479221@qq.com"));
//        message.setSubject("销售报告 - " + reportDate.format(DATE_FORMAT));
//
//        // 添加正文
//        MimeBodyPart textPart = new MimeBodyPart();
//        textPart.setText("这是自动生成的销售报告，请查收附件。");
//
//        // 添加附件
//        MimeBodyPart attachPart = new MimeBodyPart();
//        attachPart.attachFile(new File(DOWNLOAD_DIR + "销售对账报表" + reportDate.format(DATE_FORMAT) + ".xls"));
//
//        Multipart multipart = new MimeMultipart();
//        multipart.addBodyPart(textPart);
//        multipart.addBodyPart(attachPart);
//
//        message.setContent(multipart);
//        Transport.send(message);
//    }
//
//    private void handleWpsIntegration() {
//        // wps更新url
//        String url_yj_update = "https://www.kdocs.cn/api/v3/ide/file/286603414736/script/V2-3M4nPZdILm9pFSdtCTojnA/sync_task";
//        String url_duizhang = "https://www.kdocs.cn/api/v3/ide/file/286603414736/script/V2-45qNffbTQZWcx7OjL7Dawa/sync_task";
//        JSONObject args = new JSONObject();
//
//        // 业绩更新
//        wps.toWps(url_yj_update, args);
//
//        try {
//            Thread.sleep(2000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//
//        // 对账更新
//        wps.toWps(url_duizhang, args);
//    }
//
//    // 对Excel的各种操作
//    private static final String FILE_PATH = "data.xls";
//    private static final String[] B_COLUMNS_TO_MERGE = {"E", "F", "G"}; // 需要合并的B表列
//
//    private void ExcelMerger() throws Exception {
//        // 1. 读取A表数据
//        List<Map<String, String>> dataA = processSheet("销售对帐报表", 0);
//
//        // 2. 读取B表数据
//        List<Map<String, String>> dataB = processSheet("Sheet1", 4); // E列索引为4
//
//        // 3. 创建合并后的数据集
//        List<Map<String, Object>> mergedData = mergeData(dataA, dataB);
//
//        // 4. 创建C表并写入数据
//        createSheetC(mergedData);
//
//        System.out.println("数据处理完成！");
//    }
//
//    // 处理单个工作表
//    private static List<Map<String, String>> processSheet(String sheetName, int keyColumnIndex) throws Exception {
//        FileInputStream file = new FileInputStream(FILE_PATH);
//        try {
//            Workbook workbook = new HSSFWorkbook(file);
//            Sheet sheet = workbook.getSheet(sheetName);
//            List<Map<String, String>> data = new ArrayList<>();
//
//            // 获取表头（原第三行）
//            Row headerRow = sheet.getRow(2);
//            List<String> headers = new ArrayList<>();
//            for (Cell cell : headerRow) {
//                headers.add(getCellValue(cell));
//            }
//
//            // 处理数据行（从第四行开始）
//            for (int i = 3; i <= sheet.getLastRowNum(); i++) {
//                Row row = sheet.getRow(i);
//                if (row == null) continue;
//
//                Map<String, String> rowData = new HashMap<>();
//                for (int j = 0; j < headers.size(); j++) {
//                    Cell cell = row.getCell(j, Row.RETURN_BLANK_AS_NULL);
//                    rowData.put(headers.get(j), getCellValue(cell));
//                }
//                data.add(rowData);
//            }
//            workbook.close();
//            return data;
//        } finally {
//            file.close();
//        }
//    }
//
//    // 合并数据（与原代码相同）
//    private static List<Map<String, Object>> mergeData(List<Map<String, String>> dataA,
//                                                       List<Map<String, String>> dataB) {
//        Map<String, Map<String, String>> bIndex = new HashMap<>();
//        for (Map<String, String> row : dataB) {
//            String key = row.get("终端代码");
//            bIndex.put(key, row);
//        }
//
//        List<Map<String, Object>> merged = new ArrayList<>();
//        for (Map<String, String> aRow : dataA) {
//            String key = aRow.get("终端代码");
//            Map<String, String> bRow = bIndex.get(key);
//
//            Map<String, Object> mergedRow = new LinkedHashMap<>();
//            mergedRow.putAll(aRow);
//
//            if (bRow != null) {
//                for (String col : B_COLUMNS_TO_MERGE) {
//                    mergedRow.put("B_" + col, bRow.get(col));
//                }
//            }
//            merged.add(mergedRow);
//        }
//        return merged;
//    }
//
//    // 创建C表
//    private static void createSheetC(List<Map<String, Object>> data) throws Exception {
//        Workbook workbook = new HSSFWorkbook();
//        try {
//            Sheet sheetC = workbook.createSheet("销售业绩统计");
//
//            // 创建表头
//            if (!data.isEmpty()) {
//                Row headerRow = sheetC.createRow(0);
//                int colNum = 0;
//                for (String key : data.get(0).keySet()) {
//                    headerRow.createCell(colNum++).setCellValue(key);
//                }
//            }
//
//            // 填充数据
//            int rowNum = 1;
//            for (Map<String, Object> row : data) {
//                Row dataRow = sheetC.createRow(rowNum++);
//                int colNum = 0;
//                for (Object value : row.values()) {
//                    Cell cell = dataRow.createCell(colNum++);
//                    if (value != null) {
//                        cell.setCellValue(value.toString());
//                    }
//                }
//            }
//
//            // 保存文件
//            FileOutputStream out = new FileOutputStream("result.xls");
//            workbook.write(out);
//            out.close();
//        } finally {
//            workbook.close();
//        }
//    }
//
//    // 兼容Java 8的单元格值获取方法
//    private static String getCellValue(Cell cell) {
//        if (cell == null) return "";
//
//        switch (cell.getCellType()) {
//            case Cell.CELL_TYPE_STRING:
//                return cell.getStringCellValue().trim();
//            case Cell.CELL_TYPE_NUMERIC:
//                return String.valueOf(cell.getNumericCellValue());
//            case Cell.CELL_TYPE_BOOLEAN:
//                return String.valueOf(cell.getBooleanCellValue());
//            case Cell.CELL_TYPE_FORMULA:
//                return cell.getCellFormula();
//            default:
//                return "";
//        }
//    }
//}
