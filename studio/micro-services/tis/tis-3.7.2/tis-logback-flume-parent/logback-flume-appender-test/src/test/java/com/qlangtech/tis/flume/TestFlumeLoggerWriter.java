package com.qlangtech.tis.flume;

import com.qlangtech.tis.manage.common.Config;
import com.qlangtech.tis.manage.common.TISCollectionUtils;
import com.qlangtech.tis.order.center.IParamContext;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-04-20 11:35
 **/
public class TestFlumeLoggerWriter {
    private static final Logger logger = LoggerFactory.getLogger(TestFlumeLoggerWriter.class);

    @BeforeClass
    public static void preClass() {
        //  System.setProperty(Config.SYSTEM_KEY_LOGBACK_PATH_KEY, Config.SYSTEM_KEY_LOGBACK_PATH_VALUE);
    }

    @Test
    public void testLoggerWrite() throws Exception {
        Config.setTest(false);
        MDC.put(IParamContext.KEY_TASK_ID, String.valueOf(999));
        MDC.put(TISCollectionUtils.KEY_COLLECTION, "baisui");
        int i = 0;
        while (true) {
            logger.info("i am so hot:" + (i++));
            Thread.sleep(1000l);
            System.out.println("send turn:" + i);
        }
    }
}
