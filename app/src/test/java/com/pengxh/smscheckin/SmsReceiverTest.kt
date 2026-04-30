package com.pengxh.smscheckin

import org.json.JSONArray
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class SmsReceiverTest {

    @After
    fun tearDown() {
        SmsReceiver.keywords = mutableListOf("钉钉打卡")
        SmsReceiver.whitelist = emptyList()
        SmsReceiver.delay = 0L
        SmsReceiver.configVersion = 0
    }

    @Test
    fun `parseKeywords valid json returns list`() {
        val result = SmsReceiver.parseKeywords("[\"打卡\",\"签到\"]")
        assertEquals(listOf("打卡", "签到"), result)
    }

    @Test
    fun `parseKeywords invalid json returns default`() {
        val result = SmsReceiver.parseKeywords("{invalid}")
        assertEquals(listOf("钉钉打卡"), result)
    }

    @Test
    fun `parseKeywords empty array returns empty list`() {
        val result = SmsReceiver.parseKeywords("[]")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `keywordsToJson produces valid json`() {
        val json = SmsReceiver.keywordsToJson(listOf("打卡", "考勤"))
        assertEquals("[\"打卡\",\"考勤\"]", json)
        // 验证是合法的 JSONArray
        val arr = JSONArray(json)
        assertEquals(2, arr.length())
        assertEquals("打卡", arr.getString(0))
        assertEquals("考勤", arr.getString(1))
    }

    @Test
    fun `keywordsToJson empty list produces empty array`() {
        val json = SmsReceiver.keywordsToJson(emptyList())
        assertEquals("[]", json)
    }

    @Test
    fun `keywordsToJson roundtrip preserves values`() {
        val original = listOf("钉钉", "打卡", "上班")
        val json = SmsReceiver.keywordsToJson(original)
        val parsed = SmsReceiver.parseKeywords(json)
        assertEquals(original, parsed)
    }

    @Test
    fun `isSenderAllowed empty whitelist allows any sender`() {
        SmsReceiver.whitelist = emptyList()
        assertTrue(SmsReceiver.isSenderAllowed("1069000000"))
        assertTrue(SmsReceiver.isSenderAllowed("+8613800000000"))
        assertTrue(SmsReceiver.isSenderAllowed(""))
    }

    @Test
    fun `isSenderAllowed matches exact sender`() {
        SmsReceiver.whitelist = listOf("1069000000", "10086")
        assertTrue(SmsReceiver.isSenderAllowed("1069000000"))
        assertTrue(SmsReceiver.isSenderAllowed("10086"))
        assertFalse(SmsReceiver.isSenderAllowed("1069111111"))
    }

    @Test
    fun `isSenderAllowed matches substring sender`() {
        SmsReceiver.whitelist = listOf("10690")
        assertTrue(SmsReceiver.isSenderAllowed("1069000000"))
        assertTrue(SmsReceiver.isSenderAllowed("10690"))
        assertFalse(SmsReceiver.isSenderAllowed("10680"))
    }

    @Test
    fun `isSenderAllowed sender contains whitelist entry`() {
        SmsReceiver.whitelist = listOf("alibaba")
        assertTrue(SmsReceiver.isSenderAllowed("sms.alibaba.com"))
    }

    @Test
    fun `keywords getter returns independent copy`() {
        SmsReceiver.keywords = mutableListOf("A", "B")
        val copy = SmsReceiver.keywords
        copy.toMutableList().clear()
        // 不会影响原始数据
        assertEquals(listOf("A", "B"), SmsReceiver.keywords)
    }

    @Test
    fun `whitelist getter returns independent copy`() {
        SmsReceiver.whitelist = mutableListOf("X", "Y")
        val copy = SmsReceiver.whitelist
        copy.toMutableList().clear()
        assertEquals(listOf("X", "Y"), SmsReceiver.whitelist)
    }

    @Test
    fun `notifyConfigChanged increments version`() {
        val before = SmsReceiver.configVersion
        SmsReceiver.notifyConfigChanged()
        SmsReceiver.notifyConfigChanged()
        assertEquals(before + 2, SmsReceiver.configVersion)
    }
}
