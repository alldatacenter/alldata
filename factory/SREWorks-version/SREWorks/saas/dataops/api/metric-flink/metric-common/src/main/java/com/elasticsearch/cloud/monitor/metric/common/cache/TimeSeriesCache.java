package com.elasticsearch.cloud.monitor.metric.common.cache;

import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public class TimeSeriesCache {

    private final int maxCacheSize;
    private final long cacheDuration;
    private final long maxAliveTime;
    private final long checkInterval;
    private transient TreeMap<Long, Double> dataPintsMap;

    public TimeSeriesCache(final int maxCacheSize, final long maxAliveTime, final long checkInterval) {
        this.checkInterval = checkInterval;
        this.maxCacheSize = maxCacheSize;
        this.cacheDuration = maxCacheSize * checkInterval;
        /**
         * 允许最大60min打出一个点，进行连续报警判断
         */
        this.maxAliveTime = Math.max(60 * TimeUnit.MINUTES.toMillis(1), maxAliveTime);
        Preconditions.checkArgument(maxAliveTime >= cacheDuration, "maxAliveTime should > cacheDuration");
        dataPintsMap = new TreeMap<>(new TimeDescComparator());
    }

    public boolean isEmpty() {
        return dataPintsMap.isEmpty();
    }

    public void put(long timestamp, double value) {
        if (needCache(timestamp)) {
            removeEldestEntry();
            dataPintsMap.put(timestamp, value);
        }
    }

    private boolean needCache(long timestamp) {
        return dataPintsMap.isEmpty() || timestamp > dataPintsMap.firstKey() - cacheDuration;
    }

    public void removeEldestEntry() {
        if (dataPintsMap.size() >= maxCacheSize) {
            // smallest time
            dataPintsMap.remove(dataPintsMap.lastKey());
        }
    }

    public List<Double> get(final long startMs, final long endMs) {
        List<Double> values = new LinkedList<>();
        if (!dataPintsMap.isEmpty()) {
            long curCacheSmallestTime = dataPintsMap.lastKey();
            // all in cache
            if (startMs >= curCacheSmallestTime) {
                getFromCache(startMs, endMs, values);
            } else {
                getFromCache(curCacheSmallestTime, endMs, values);
            }
        }
        return values;
    }

    private void getFromCache(final long startMs, final long endMs, final List<Double> values) {
        for (long s = startMs; s <= endMs; s += checkInterval) {
            Double value = dataPintsMap.get(s);
            if (value != null) {
                values.add(value);
            }
        }
    }

    public int getDpSize() {
        return dataPintsMap.size();
    }

    public boolean isAllExpired(final long currentEventTime) {
        return dataPintsMap.isEmpty() || currentEventTime - dataPintsMap.firstKey() > maxAliveTime;
    }

    public void invalid(long time) {
        dataPintsMap.remove(time);
    }

    @Override
    public String toString() {
        return dataPintsMap.toString();
    }

    //降序
    private static class TimeDescComparator implements Comparator<Long> {
        @Override
        public int compare(Long o1, Long o2) {
            return (int)(o2 - o1);
        }
    }
}
