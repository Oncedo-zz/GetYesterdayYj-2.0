package com.wechat.ferry.core;

import org.apache.poi.ss.usermodel.*;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 通用抓取引擎（2.0）：合并 1.0 中两份 95% 重复的
 * ZhongDuanYJTongJi_Yes_Jtnrb / ZhongDuanYJTongJi_Yes_Yhgz。
 *
 * 站点差异全部由 sites.yml 驱动：
 *   - 选择器候选列表
 *   - 等待时长
 *   - 输出路径 / 文件名 / sheet 名
 *   - dateColumnOffset（1.0 中 jtnrb=0、yhgz=1 的历史差异，原样保留）
 *
 * 行为与 1.0 逐字一致；唯一区别是失败会明确抛异常，不再静默吞掉。
 */
public final class ReportCrawler {

    private final AppConfig cfg;

    public ReportCrawler(AppConfig cfg) {
        this.cfg = cfg;
    }

    public void crawl(WebDriver driver, String siteKey, String targetDate) {
        Map<String, Object> site = cfg.site(siteKey);
        String display = cfg.siteStr(siteKey, "displayName", siteKey);
        Duration timeout = Duration.ofSeconds(cfg.integer("defaults.wait.elementTimeoutSeconds", 30));

        StepContext.start(display, targetDate);

        // ---- 1. 导航：报表 -> 门店 -> 终端业绩统计 ----
        List<String> nav = cfg.siteList(siteKey, "navigation");
        String[] navNames = {"打开报表菜单", "进入门店", "终端业绩统计"};
        for (int i = 0; i < nav.size(); i++) {
            String name = i < navNames.length ? navNames[i] : ("导航步骤" + (i + 1));
            StepContext.step(display, targetDate, name);
            ElementLocator.click(driver, display, targetDate, name,
                Collections.singletonList(nav.get(i)), timeout);
            Waits.pause(cfg.integer("defaults.wait.afterPageLoadMs", 2000) / 2, name + " 页面响应");
        }

        // ---- 2. 填日期：起始 / 终止（与 1.0 相同，clear + sendKeys + ENTER）----
        String startStep = "输入起始日期 " + targetDate;
        StepContext.step(display, targetDate, startStep);
        WebElement dateStart = ElementLocator.find(driver, display, targetDate, startStep,
            cfg.siteList(siteKey, "report.startDateInput"), timeout);
        dateStart.clear();
        dateStart.sendKeys(targetDate);
        dateStart.sendKeys(Keys.ENTER);
        Waits.pause(cfg.integer("defaults.wait.afterDateInputMs", 1000), "起始日期生效");

        String endStep = "输入终止日期 " + targetDate;
        StepContext.step(display, targetDate, endStep);
        WebElement dateEnd = ElementLocator.find(driver, display, targetDate, endStep,
            cfg.siteList(siteKey, "report.endDateInput"), timeout);
        dateEnd.clear();
        dateEnd.sendKeys(targetDate);
        dateEnd.sendKeys(Keys.ENTER);
        Waits.pause(cfg.integer("defaults.wait.afterDateInputMs", 1000), "终止日期生效");

        // ---- 3. 点击查询 ----
        String queryStep = "点击查询";
        StepContext.step(display, targetDate, queryStep);
        ElementLocator.click(driver, display, targetDate, queryStep,
            cfg.siteList(siteKey, "report.queryButton"), timeout);

        // ---- 4. 等待结果面板与数据行（用条件等待替代固定 sleep 3000）----
        String paneStep = "等待报表结果";
        StepContext.step(display, targetDate, paneStep);
        WebElement pane = ElementLocator.find(driver, display, targetDate, paneStep,
            cfg.siteList(siteKey, "report.pane"), timeout);

        String rowsSel = cfg.siteStr(siteKey, "report.rows", ".//tbody/tr");
        int rows = Waits.waitForRows(driver, pane, rowsSel, 1, timeout);
        StepContext.ok(display, targetDate, "报表返回 " + rows + " 行数据");
        Waits.pause(cfg.integer("defaults.wait.afterPaneMs", 2000), "表格动画稳定");

        // ---- 5. 表头（保留 1.0 的横向滚动逻辑，确保虚拟列渲染出来）----
        List<String> headers = getTableHeaders(driver, siteKey, display, targetDate, timeout);

        // ---- 6. 读取表格数据并写入 Excel ----
        WebElement table = pane.findElement(ElementLocator.parse(
            first(cfg.siteList(siteKey, "report.table"), "class=el-table__body")));
        List<WebElement> rowEls = ElementLocator.findAll(table, rowsSel);

        String saveDir = cfg.siteStr(siteKey, "output.saveDir", "C:\\Users\\Administrator\\Desktop\\Performance\\");
        String pattern = cfg.siteStr(siteKey, "output.fileNamePattern", "销售对账报表{date}.xls");
        String sheetName = cfg.siteStr(siteKey, "output.sheetName", "Table Data");
        int offset = cfg.siteInt(siteKey, "report.dateColumnOffset", 0);
        String cellsSel = cfg.siteStr(siteKey, "report.cells", "td");
        String extraHeader = cfg.siteStr(siteKey, "report.extraHeader", "销售日期");

        Path filePath = Paths.get(saveDir + pattern.replace("{date}", targetDate));
        writeExcel(driver, filePath, sheetName, headers, extraHeader, rowEls, cellsSel, offset,
            targetDate, display);
        StepContext.ok(display, targetDate, "Excel 已写入：" + filePath);
    }

    // ------------------------------------------------------------------
    // 表头提取：等价于 1.0 getTableHeaders，但失败明确抛异常
    // ------------------------------------------------------------------
    private List<String> getTableHeaders(WebDriver driver, String siteKey, String display,
                                         String date, Duration timeout) {
        String step = "读取表头";
        StepContext.step(display, date, step);
        List<String> wrapperSel = cfg.siteList(siteKey, "report.headerWrapper");
        WebElement wrapper = ElementLocator.find(driver, display, date, step, wrapperSel, timeout);

        try {
            // 先滚到最左，再超量滚到右侧，触发虚拟列表头渲染（1.0 原有逻辑）
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollLeft = 0;", wrapper);
            Waits.pause(1000, "表头回到最左");
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollLeft = arguments[0].scrollWidth * 1.5;", wrapper);
            Waits.pause(2000, "表头展开全部列");
        } catch (Exception e) {
            StepContext.warn(display, date, step, "表头滚动失败（非致命）:" + e.getMessage());
        }

        String headerCellSel = cfg.siteStr(siteKey, "report.headerCell",
            ".el-table__header-wrapper th > .cell");
        List<WebElement> cells = driver.findElements(ElementLocator.parse(headerCellSel));
        List<String> result = new ArrayList<String>();
        for (WebElement c : cells) {
            result.add(ElementLocator.text(driver, c));
        }
        if (result.isEmpty()) {
            throw new StepException(display, date, step,
                "未取到任何表头，请检查 sites.yml 的 report.headerCell 配置");
        }
        StepContext.ok(display, date, step + "，共 " + result.size() + " 列");
        return result;
    }

    // ------------------------------------------------------------------
    // Excel 写入：等价于 1.0 的"存在则追加、否则新建写表头"逻辑
    // ------------------------------------------------------------------
    private void writeExcel(WebDriver driver, Path filePath, String sheetName,
                            List<String> headers, String extraHeader, List<WebElement> rowEls,
                            String cellsSel, int dateOffset, String targetDate, String display) {
        File file = filePath.toFile();
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            if (!parent.mkdirs()) {
                throw new StepException(display, targetDate, "创建输出目录", "无法创建 " + parent);
            }
        }

        Workbook workbook;
        Sheet sheet;
        int startRow;
        try {
            if (file.exists()) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    workbook = WorkbookFactory.create(fis);
                }
                sheet = workbook.getSheet(sheetName);
                if (sheet == null) {
                    sheet = workbook.createSheet(sheetName);
                    writeHeader(sheet, headers, extraHeader);
                }
                startRow = sheet.getLastRowNum() + 1;
            } else {
                workbook = new org.apache.poi.hssf.usermodel.HSSFWorkbook();
                sheet = workbook.createSheet(sheetName);
                writeHeader(sheet, headers, extraHeader);
                startRow = 1;
            }

            for (int i = 0; i < rowEls.size(); i++) {
                Row row = sheet.createRow(startRow + i);
                List<WebElement> cells = ElementLocator.findAll(rowEls.get(i), cellsSel);
                for (int j = 0; j < cells.size(); j++) {
                    row.createCell(j).setCellValue(ElementLocator.text(driver, cells.get(j)));
                }
                // 1.0 行为：jtnrb 写 cells.size()，yhgz 写 cells.size()+1
                int dateCol = cells.size() + dateOffset;
                row.createCell(dateCol).setCellValue(targetDate);
            }

            try (FileOutputStream out = new FileOutputStream(file)) {
                workbook.write(out);
            }
        } catch (IOException e) {
            throw new StepException(display, targetDate, "写入 Excel", e.getMessage(), e);
        }
    }

    private void writeHeader(Sheet sheet, List<String> headers, String extraHeader) {
        Row h = sheet.createRow(0);
        for (int i = 0; i < headers.size(); i++) {
            h.createCell(i).setCellValue(headers.get(i));
        }
        h.createCell(headers.size()).setCellValue(extraHeader);
    }

    private static String first(List<String> list, String def) {
        return list.isEmpty() ? def : list.get(0);
    }
}
