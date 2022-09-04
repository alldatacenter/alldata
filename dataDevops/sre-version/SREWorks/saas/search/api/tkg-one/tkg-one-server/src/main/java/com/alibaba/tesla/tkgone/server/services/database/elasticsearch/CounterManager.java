package com.alibaba.tesla.tkgone.server.services.database.elasticsearch;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author junwei.jjw@alibaba-inc.com
 * @date 2019/10/27
 */
public class CounterManager {
    private ConcurrentMap<String, Counter> counters = new ConcurrentHashMap<>();

    public Counter getCounter(String source) {
        Counter counter = counters.get(source);
        if (counter == null) {
            synchronized (this) {
                counter = counters.get(source);
                if (counter == null) {
                    counter = new Counter(source);
                    counters.put(source, counter);
                }
            }
        }
        return counter;
    }
}
