package com.elasticsearch.cloud.monitor.metric.common.cache;

import com.elasticsearch.cloud.monitor.metric.common.checker.duration.DurationConditionChecker;
import com.elasticsearch.cloud.monitor.metric.common.datapoint.DataPoint;
import com.elasticsearch.cloud.monitor.metric.common.rule.Rule;
import com.elasticsearch.cloud.monitor.metric.common.utils.Pair;
import com.elasticsearch.cloud.monitor.metric.common.utils.TimeUtils;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Slf4j
@Data
public abstract class RuleConditionCache implements RuleDataPointsCache {
    protected int maxCacheSize;
    protected long maxDuration;
    protected Rule rule;
    /**
     * 因为同一个rule下, metricName相同, 但会有多条line, 不同line的tags是不一样的, 所以用流入数据的tags做key
     */
    protected transient Map<Map<String, String>, Pair<TimeSeriesCache, DurationConditionChecker>> stateMap;
    /**
     * 数据精度 granularity
     */
    protected long interval;

    protected RuleConditionCache(final Rule rule, long interval) {
        this.rule = rule;
        this.interval = interval;
        this.maxDuration = rule.getDurationCondition().getCrossSpan();

        Preconditions.checkArgument(maxDuration > 0, "maxDuration(" + maxDuration + ") must > 0");
        Preconditions.checkArgument(maxDuration % interval == 0, "maxDuration(" + maxDuration + ") must be a multiple of " + interval);
        this.maxCacheSize = (int) (maxDuration / interval);
        Preconditions.checkArgument(this.maxCacheSize > 0, "maxCacheSize <= 0");
        this.stateMap = Maps.newConcurrentMap();
    }

    public abstract void recovery(DataPoint dataPoint) throws IOException;

    @Override
    public void put(DataPoint dp) {
        Preconditions.checkArgument(rule.match(dp.getName()), "metric name is different");
        long timestamp = TimeUtils.toMillisecond(dp.getTimestamp());
        if (timestamp % interval != 0) {
            log.error("dp timestamp should be multiple of " + interval + ", : " + dp.toString());
            return;
        }
        //Preconditions.checkArgument(timestamp % interval == 0, "dp timestamp should be multiple of " + interval + ", : " + dp.toString());

        TimeSeriesCache timeSeriesCache = getTimeSeriesCache(dp.getTags());
        timeSeriesCache.put(timestamp, dp.getValue());
    }

    @Override
    public List<Double> get(final long start, final long end, final Map<String, String> queryTags) {
        Preconditions.checkArgument(queryTags != null, "tags should not be null.");
        Preconditions.checkArgument(start <= end, "start(" + start + ") > end(" + end + ")");
        long startMs = TimeUtils.toMillisecond(start);
        long endMs = TimeUtils.toMillisecond(end);

        Preconditions.checkArgument(startMs % interval == 0, "start ms should be multiple of " + interval + ", : " + startMs);
        Preconditions.checkArgument(endMs % interval == 0, "end ms should be multiple of " + interval + ", : " + endMs);

        TimeSeriesCache timeSeriesCache = getTimeSeriesCache(queryTags);
        return timeSeriesCache.get(startMs, endMs);
    }

    private TimeSeriesCache getTimeSeriesCache(final Map<String, String> tags) {
        return getStatePair(tags).getKey();
    }

    public DurationConditionChecker getConditionChecker(final Map<String, String> tags) {
        return getStatePair(tags).getValue();
    }

    private Pair<TimeSeriesCache, DurationConditionChecker> getStatePair(final Map<String, String> tags) {
        Pair<TimeSeriesCache, DurationConditionChecker> cacheAndChecker = stateMap.get(tags);
        if (cacheAndChecker == null) {
            TimeSeriesCache timeSeriesCache = new TimeSeriesCache(maxCacheSize, maxDuration, interval);
            DurationConditionChecker checker = rule.getDurationCondition().getChecker();
            cacheAndChecker = new Pair<>(timeSeriesCache, checker);
            stateMap.put(tags, cacheAndChecker);
        }
        return cacheAndChecker;
    }

    public void compact(final long currentEventTime) {
        List<Map<String, String>> toRemoveKeys = new LinkedList<>();
        for (Map.Entry<Map<String, String>, Pair<TimeSeriesCache, DurationConditionChecker>> entry : stateMap.entrySet()) {
            TimeSeriesCache cache = entry.getValue().getKey();
            if (cache.isAllExpired(currentEventTime)) {
                toRemoveKeys.add(entry.getKey());
            }
        }
        for (Map<String, String> key : toRemoveKeys) {
            stateMap.remove(key);
        }
    }

    public String getCache(Map<String, String> tags) {
        TimeSeriesCache cache = getTimeSeriesCache(tags);
        return cache.toString();
    }
}
