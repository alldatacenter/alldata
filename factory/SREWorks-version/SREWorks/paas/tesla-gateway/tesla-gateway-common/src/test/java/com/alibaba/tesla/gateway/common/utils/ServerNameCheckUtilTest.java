package com.alibaba.tesla.gateway.common.utils;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
public class ServerNameCheckUtilTest {


    @Test
    public void check() {
        String url = "lb://fass.test.1233%alibaba/test_test";
        ServerNameCheckUtil.check(url);
    }
}