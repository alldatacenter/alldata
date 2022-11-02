package com.alibaba.sreworks.health.common.utils;

import org.springframework.util.DigestUtils;

import java.lang.management.ManagementFactory;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntUnaryOperator;

/**
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/10/27 16:42
 */
public class IDGenerator {

    private static int MIN_ASC_SERISE_NUM = 1000;
    private static int MAX_ASC_SERISE_NUM = 9999;
    private static AtomicInteger value = new AtomicInteger(MIN_ASC_SERISE_NUM);

    private static IntUnaryOperator unaryOperator = (x) -> x >= MAX_ASC_SERISE_NUM ? MIN_ASC_SERISE_NUM : x;

    public static String generateInstanceId(String content) {
        // 产生ID时间(13) + 进程ID + 实体值hash(32) + 随机自增序列(4)
        Long nowTs = new Date().getTime();
        String pid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
        String contentMd5Hex = DigestUtils.md5DigestAsHex(content.getBytes());

        int currentValue = value.incrementAndGet();
        if(currentValue >= MAX_ASC_SERISE_NUM) {
            value.updateAndGet(unaryOperator);
            currentValue = value.incrementAndGet();
        }

        return new StringBuilder().append(nowTs).append(pid).append(contentMd5Hex).append(currentValue).toString();
    }
}
