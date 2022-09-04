package com.alibaba.sreworks.common.util;

import java.util.Random;

/**
 * @author jinghua.yjh
 */
public class SleepUtil {

    public static void randomSleep(int millis) {
        sleep(new Random().nextInt(millis));
    }

    public static void sleep(long millisecond) {
        try {
            Thread.sleep(millisecond);
        } catch (InterruptedException ignored) {
        }
    }

}
