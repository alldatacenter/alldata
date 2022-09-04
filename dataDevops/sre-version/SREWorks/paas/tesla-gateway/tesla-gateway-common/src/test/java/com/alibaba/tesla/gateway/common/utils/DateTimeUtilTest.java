package com.alibaba.tesla.gateway.common.utils;

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.*;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
public class DateTimeUtilTest {

    @Test
    public void testFormat(){
        Date date = new Date();
        String formart = DateTimeUtil.formart(date.getTime());
        System.out.println(formart);
    }

}