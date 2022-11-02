package com.elasticsearch.cloud.monitor.metric.common.blink.mock;

import org.apache.flink.util.Collector;

import java.util.ArrayList;
import java.util.List;

/**
 * @author xingming.xuxm
 * @Date 2019-12-12
 */
public class FakeListCollector<T> implements Collector<T> {
    public List<T> outs = new ArrayList<>();

    @Override
    public void collect(T t) {
        outs.add(t);
    }

    @Override
    public void close() {
        outs.clear();
    }

    public int size() {
        return outs.size();
    }

    public T get(int index) {
        return outs.get(index);
    }

    public void clear() {
        outs.clear();
    }

}
