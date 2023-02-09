package com.obs.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.Date;

import org.junit.Test;

import com.obs.services.internal.security.LimitedTimeSecurityKey;

public class LimitedTimeSecurityKeyTest {
    @Test
    public void test_get_set() {
        LimitedTimeSecurityKey limitedTimeSecurityKey = new LimitedTimeSecurityKey("accessKey", "secretKey", "securityToken");

        assertEquals(limitedTimeSecurityKey.getAccessKey(), "accessKey");
        assertEquals(limitedTimeSecurityKey.getSecretKey(), "secretKey");
        assertEquals(limitedTimeSecurityKey.getSecurityToken(), "securityToken");
    }

    @Test
    public void now_is_about_to_expire_false() {
        // 获取当前时间
        Date expiryDate = getUtcTime(0);

        LimitedTimeSecurityKey limitedTimeSecurityKey = new LimitedTimeSecurityKey("accessKey", "secretKey", "securityToken", expiryDate);

        assertFalse(limitedTimeSecurityKey.aboutToExpire());
        assertTrue(limitedTimeSecurityKey.willSoonExpire());
    }

    @Test
    public void before_10s_about_to_expire_false() {
        // 获取当前时间
        Date expiryDate = getUtcTime(10);

        LimitedTimeSecurityKey limitedTimeSecurityKey = new LimitedTimeSecurityKey("accessKey", "secretKey", "securityToken", expiryDate);

        assertFalse(limitedTimeSecurityKey.aboutToExpire());
        assertTrue(limitedTimeSecurityKey.willSoonExpire());
    }

    @Test
    public void before_70s_about_to_expire_false() {
        // 获取当前时间
        Date expiryDate = getUtcTime(130);

        LimitedTimeSecurityKey limitedTimeSecurityKey = new LimitedTimeSecurityKey("accessKey", "secretKey", "securityToken", expiryDate);

        assertTrue(limitedTimeSecurityKey.aboutToExpire());
        assertFalse(limitedTimeSecurityKey.willSoonExpire());
    }

    @Test
    public void before_350s_about_to_expire_false() {
        // 获取当前时间
        Date expiryDate = getUtcTime(350);

        LimitedTimeSecurityKey limitedTimeSecurityKey = new LimitedTimeSecurityKey("accessKey", "secretKey", "securityToken", expiryDate);

        assertFalse(limitedTimeSecurityKey.aboutToExpire());
        assertFalse(limitedTimeSecurityKey.willSoonExpire());
    }

    private Date getUtcTime(int amount) {
        Calendar calendar = Calendar.getInstance();
        int offset = calendar.get(Calendar.ZONE_OFFSET);
        calendar.add(Calendar.MILLISECOND, -offset);

        calendar.add(Calendar.SECOND, amount);

        return calendar.getTime();
    }
}
