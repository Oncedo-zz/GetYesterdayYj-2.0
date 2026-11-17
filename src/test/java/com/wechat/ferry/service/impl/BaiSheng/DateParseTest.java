package com.wechat.ferry.service.impl.BaiSheng;

import org.junit.jupiter.api.Test;

import java.util.Calendar;

import static org.junit.jupiter.api.Assertions.*;

/** 日期解析测试：确保 1.0 支持的四种格式全部兼容 */
public class DateParseTest {

    @Test
    public void isoDate() {
        assertEquals("2026-09-06", GetYesterdayYj.parseDateInput("2026-09-06"));
    }

    @Test
    public void compactDate() {
        assertEquals("2026-09-06", GetYesterdayYj.parseDateInput("20260906"));
    }

    @Test
    public void monthDay() {
        int year = Calendar.getInstance().get(Calendar.YEAR);
        assertEquals(year + "-09-06", GetYesterdayYj.parseDateInput("09-06"));
    }

    @Test
    public void monthDayCompact() {
        int year = Calendar.getInstance().get(Calendar.YEAR);
        assertEquals(year + "-09-06", GetYesterdayYj.parseDateInput("0906"));
    }

    @Test
    public void invalid() {
        assertNull(GetYesterdayYj.parseDateInput("abc"));
        assertNull(GetYesterdayYj.parseDateInput("2026/09/06"));
        assertNull(GetYesterdayYj.parseDateInput(null));
    }
}
