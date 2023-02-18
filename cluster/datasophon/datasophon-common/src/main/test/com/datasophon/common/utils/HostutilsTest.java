package com.datasophon.common.utils;

import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

/**
 *
 *
 * @author rwj
 * @date 2022/11/22
 */
public class HostutilsTest {


    @Test
    public void testReadHotsFile() {
        HostUtils.read();
        Map<String, String> ipHostMap = (Map<String, String>) CacheUtils.get(Constants.IP_HOST);
        ipHostMap.forEach((k, v) -> System.out.println(k + " --- " + v));
    }

    @Test
    public void testFindIp() {
        HostUtils.read();
        String ip = HostUtils.findIp("ddp1.test.cn");
        Assert.assertEquals("192.168.31.231", ip);
    }


}
