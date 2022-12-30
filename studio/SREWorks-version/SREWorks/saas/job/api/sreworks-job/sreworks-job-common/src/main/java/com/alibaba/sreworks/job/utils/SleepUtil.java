package com.alibaba.sreworks.job.utils;

public class SleepUtil {

    public static void sleep(Long second) {
        try {
            Thread.sleep(1000 * second);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
