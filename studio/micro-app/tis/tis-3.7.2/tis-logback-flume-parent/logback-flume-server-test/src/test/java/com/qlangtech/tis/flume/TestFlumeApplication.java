package com.qlangtech.tis.flume;

import org.junit.Test;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-04-20 11:25
 **/
public class TestFlumeApplication {

    @Test
    public void testApplicaitonLaunch() throws Exception {

        FlumeApplication.startFlume();

        Thread.sleep(1000*1000);
    }
}
