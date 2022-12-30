package com.alibaba.tesla.tkgone.server.services.database.elasticsearch;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author junwei.jjw@alibaba-inc.com
 * @date 2019/10/27
 */
@Slf4j
public class Counter {
    public static final int OUTPUT_INTERVAL_IN_MILLIS = 60000;
    private String name;
    private AtomicLong value = new AtomicLong(0);
    private AtomicLong callTotal = new AtomicLong(0);
    private AtomicLong callTimeInMillis = new AtomicLong(0);
    private long lastOutputTime = 0;

    public Counter(String name) {
        this.name = name;
    }

    public void step(long value, long timeInMillis) {
        value = this.value.addAndGet(value);
        long callTotal = this.callTotal.incrementAndGet();
        long callTimeInMillis = this.callTimeInMillis.addAndGet(timeInMillis);

        long now = System.currentTimeMillis();
        if (now - lastOutputTime > OUTPUT_INTERVAL_IN_MILLIS) {
            lastOutputTime = now;
            long callAvgTime = callTotal == 0 ? 0 : callTimeInMillis / callTotal;
            log.info("counter={}\tvalue={}\tcallTotals={}\tcallAvgTime={}ms", name, value, callTotal, callAvgTime);
        }
    }
}
