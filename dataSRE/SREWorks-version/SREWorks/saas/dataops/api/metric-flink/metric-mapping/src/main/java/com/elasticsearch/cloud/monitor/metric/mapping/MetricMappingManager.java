package com.elasticsearch.cloud.monitor.metric.mapping;

import com.elasticsearch.cloud.monitor.metric.common.blink.utils.Filter;
import com.elasticsearch.cloud.monitor.metric.common.pojo.CommonPojo.EsClusterConf;
import com.elasticsearch.cloud.monitor.metric.mapping.pojo.MetricMappingInfo;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.*;

import static com.elasticsearch.cloud.monitor.metric.mapping.MappingUtil.createHighLevelClient;

/**
 * @author xiaoping
 * @date 2021/6/23
 */

public class MetricMappingManager {
    private static final Log log = LogFactory.getLog(MetricMappingManager.class);
    private EsClusterConf esClusterConf;
    private String metricMappingIndexName;
    private volatile RestHighLevelClient highLevelClient;
    private volatile Map<String, MetricMappingInfo> metricToMappingInfo = Maps.newConcurrentMap();
    private volatile Map<String, Set<Integer>> tenantToNumbers = Maps.newConcurrentMap();
    private Map<String, Long> tenantAccessTimes = Maps.newConcurrentMap();
    private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private ScheduledExecutorService unknownAssignService = Executors.newSingleThreadScheduledExecutor();
    private ExecutorService assignExecutorService = new ThreadPoolExecutor(2, 150, 5, TimeUnit.MINUTES,
        new LinkedBlockingQueue<Runnable>());
    private boolean keepTenantToNumbers;
    private List<String> notKeepTenantNumberPre;
    private Set<MetricMappingInfo> unknownMetric = Sets.newConcurrentHashSet();

    public MetricMappingManager(EsClusterConf esClusterConf, String metricMappingIndexName, boolean syncMappingInfo,
        boolean keepTenantToNumbers, List<String> notKeepTenantNumberPre) {
        this.esClusterConf = esClusterConf;
        this.metricMappingIndexName = metricMappingIndexName;
        this.highLevelClient = createHighLevelClient(esClusterConf);
        this.keepTenantToNumbers = keepTenantToNumbers;
        this.notKeepTenantNumberPre = notKeepTenantNumberPre;

        init(syncMappingInfo);
    }

    private void init(boolean syncMappingInfo) {
        try {
            if (syncMappingInfo) {
                syncMappingInfo();
            }
        } catch (Throwable t) {
            log.error(String.format("MetricMappingManager init syncMappingInfo error %s", t.getMessage()), t);
        }
        executorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                try {
                    log.info("begin to syncMappingInfo");
                    long start = System.currentTimeMillis();
                    syncMappingInfo();
                    log.info(String.format("end syncMappingInfo, took %s seconds",
                        TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - start)));
                } catch (Throwable t) {
                    log.error(String
                            .format("MetricMappingManager scheduleWithFixedDelay syncMappingInfo error %s",
                                t.getMessage()),
                        t);
                }

            }
        }, 0, 5, TimeUnit.MINUTES);
        unknownAssignService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                try {
                    List<MetricMappingInfo> metricMappingInfos = Lists.newArrayList(unknownMetric);
                    unknownMetric.clear();
                    getMapping(metricMappingInfos, true, false);
                } catch (Throwable t) {
                    log.error(String
                        .format("MetricMappingManager scheduleWithFixedDelay assign unknown metric, error %s",
                            t.getMessage()), t);
                }
            }
        }, 0, 10, TimeUnit.SECONDS);
    }

    public void addUnknownMetric(List<MetricMappingInfo> metrics) {
        unknownMetric.addAll(metrics);
    }

    public Map<String, MetricMappingInfo> getMapping(List<MetricMappingInfo> metricList, boolean addIfNotExist,
        boolean wildcard)
        throws IOException, ExecutionException, InterruptedException {
        Map<String, MetricMappingInfo> mappings = Maps.newConcurrentMap();

        Map<String, List<MetricMappingInfo>> tenantToNotMappingMetric = Maps.newConcurrentMap();
        for (MetricMappingInfo mappingInfo : metricList) {
            MetricMappingInfo metricMappingInfo = metricToMappingInfo.get(mappingInfo.getMetric());
            if (metricMappingInfo != null) {
                mappings.put(mappingInfo.getMetric(), metricMappingInfo);
            } else {
                if (wildcard) {
                    for (Entry<String, MetricMappingInfo> entry : metricToMappingInfo.entrySet()) {
                        if (Filter.match(mappingInfo.getMetric(), entry.getKey())) {
                            mappings.put(entry.getValue().getMetric(), entry.getValue());
                        }
                    }
                } else {
                    if (addIfNotExist) {
                        List<MetricMappingInfo> infos = tenantToNotMappingMetric.computeIfAbsent(
                            mappingInfo.getTenant(),
                            t -> new Vector<>());
                        infos.add(mappingInfo);
                    }
                }
            }
        }
        if (tenantToNotMappingMetric.size() > 0) {
            Map<String, MetricMappingInfo> metricMappingInfoMap = assignMetric(tenantToNotMappingMetric);
            mappings.putAll(metricMappingInfoMap);
        }
        for (MetricMappingInfo metricMappingInfo : mappings.values()) {
            tenantAccessTimes.put(metricMappingInfo.getTenant(), System.currentTimeMillis());
        }
        return mappings;
    }

    private synchronized Map<String, MetricMappingInfo> assignMetric(
        Map<String, List<MetricMappingInfo>> tenantToNotMappingMetric)
        throws IOException, ExecutionException, InterruptedException {
        Map<String, MetricMappingInfo> result = Maps.newConcurrentMap();
        List<Future<Map<String, MetricMappingInfo>>> futures = Lists.newArrayList();
        for (Entry<String, List<MetricMappingInfo>> entry : tenantToNotMappingMetric.entrySet()) {
            Future<Map<String, MetricMappingInfo>> future = assignExecutorService.submit(
                new AssignMetricCallable(entry.getKey(), entry.getValue()));
            if (future != null) {
                futures.add(future);
            } else {
                log.error("future submit is null");
            }
        }
        for (Future<Map<String, MetricMappingInfo>> future : futures) {
            try {
                result.putAll(future.get());
            } catch (Throwable t) {
                if (t instanceof NullPointerException) {
                    log.error("future get error" + Throwables.getStackTraceAsString(t));
                } else {
                    log.error("future get error ", t);
                }
            }
        }

        return result;
    }

    private class AssignMetricCallable implements Callable<Map<String, MetricMappingInfo>> {
        private String tenant;
        private List<MetricMappingInfo> metrics;

        public AssignMetricCallable(String tenant,
            List<MetricMappingInfo> metrics) {
            this.tenant = tenant;
            this.metrics = metrics;
        }

        @Override
        public Map<String, MetricMappingInfo> call() throws Exception {
            return assignMetric(tenant, metrics);
        }
    }

    private Map<String, MetricMappingInfo> assignMetric(String tenant, List<MetricMappingInfo> metrics)
        throws IOException {
        Map<String, MetricMappingInfo> result = Maps.newConcurrentMap();
        Integer assignNumber = null;
        boolean needClearNumber = false;
        if (metrics != null) {
            for (MetricMappingInfo mappingInfo : metrics) {
                if (StringUtils.isEmpty(mappingInfo.getMetric())) {
                    continue;
                }
                MetricMappingInfo metricMappingInfo = metricToMappingInfo.get(mappingInfo.getMetric());
                if (metricMappingInfo != null) {
                    result.put(mappingInfo.getMetric(), metricMappingInfo);
                } else {
                    if (StringUtils.isEmpty(mappingInfo.getReferenceMetric())) {
                        if (assignNumber == null) {
                            assignNumber = findAvailableNumber(tenant);
                            needClearNumber = true;
                        }
                    } else {
                        assignNumber = getReferenceNumber(mappingInfo.getReferenceTenant(),
                            mappingInfo.getReferenceMetric());
                        if (assignNumber == null) {
                            log.error(String.format("get reference number of tenant %s metric %s is null",
                                mappingInfo.getReferenceTenant(), mappingInfo.getReferenceMetric()));
                            continue;
                        }
                    }

                    String pk = MappingUtil.md5(mappingInfo.getMetric());
                    boolean success = MappingUtil.addMappingInfo(highLevelClient, metricMappingIndexName, pk, tenant,
                        mappingInfo.getMetric(), assignNumber, true);
                    MetricMappingInfo assignedMetricMappingInfo;
                    if (success) {
                        assignedMetricMappingInfo = new MetricMappingInfo(tenant, mappingInfo.getMetric());
                        assignedMetricMappingInfo.setNumber(assignNumber);
                        assignedMetricMappingInfo.setMetric_pk(true);
                        assignNumber = null;
                    } else {
                        assignedMetricMappingInfo = MappingUtil.getMetricMappingInfo(highLevelClient,
                            metricMappingIndexName, pk);
                        if (assignedMetricMappingInfo == null) {
                            log.error("get pk " + pk + " is null");
                        }
                    }
                    if (assignedMetricMappingInfo != null) {
                        result.put(mappingInfo.getMetric(), assignedMetricMappingInfo);
                        if (needClearNumber) {
                            Set<Integer> numbers = tenantToNumbers.computeIfAbsent(tenant,
                                t -> Sets.newConcurrentHashSet());
                            numbers.add(assignedMetricMappingInfo.getNumber());
                        }
                        metricToMappingInfo.put(mappingInfo.getMetric(), assignedMetricMappingInfo);
                    }
                }
            }
        }
        if (needClearNumber && assignNumber != null) {
            String pk = getTenantNumberPk(tenant, assignNumber);
            MappingUtil.deleteInfo(highLevelClient, metricMappingIndexName, pk);
        }
        return result;
    }

    private String getTenantNumberPk(String tenant, int number) {
        return MappingUtil.md5(tenant + ":" + number);
    }

    private Integer getReferenceNumber(String tenant, String metric) throws IOException {
        Map<String, MetricMappingInfo> mappingInfoMap = assignMetric(tenant,
            Lists.newArrayList(new MetricMappingInfo(tenant, metric)));
        MetricMappingInfo metricMappingInfo = mappingInfoMap.get(metric);
        if (metricMappingInfo != null) {
            return metricMappingInfo.getNumber();
        } else {
            return null;
        }
    }

    private int findAvailableNumber(String tenant) throws IOException {
        int number = 0;
        Set<Integer> numbers = tenantToNumbers.computeIfAbsent(tenant, t -> Sets.newConcurrentHashSet());
        while (true) {
            if (!numbers.contains(number)) {
                String pk = getTenantNumberPk(tenant, number);
                boolean success = false;
                try {
                    success = MappingUtil.addMappingInfo(highLevelClient, metricMappingIndexName, pk, tenant, "",
                        number, false);
                } catch (IOException e) {
                    try {
                        handleException(e);
                        number--;
                    } catch (Throwable throwable) {
                        throw e;
                    }
                }
                if (success) {
                    return number;
                }
            }
            number++;
        }
    }

    public synchronized void syncMappingInfo() {

        List<MetricMappingInfo> infos = null;
        try {
            BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
            boolQueryBuilder.filter(new TermQueryBuilder("metric_pk", true));
            infos = MappingUtil.scanMappingInfo(highLevelClient, metricMappingIndexName, boolQueryBuilder);
            log.info("end to real syncMappingInfo, info size " + infos.size());
        } catch (Throwable e) {
            try {
                handleException(e);
            } catch (Throwable throwable) {
                log.error(String
                        .format("syncMappingInfo of index %s error %s", metricMappingIndexName, throwable.getMessage()),
                    throwable);
            }

        }

        if (infos != null) {
            Map<String, MetricMappingInfo> tempMetricToMappingInfo = Maps.newConcurrentMap();
            Map<String, Set<Integer>> tempTenantToNumbers = Maps.newConcurrentMap();
            for (MetricMappingInfo metricMappingInfo : infos) {
                String metric = metricMappingInfo.getMetric();
                String tenant = metricMappingInfo.getTenant();
                int number = metricMappingInfo.getNumber();
                tempMetricToMappingInfo.put(metric, metricMappingInfo);
                if (needKeepTenantNumber(tenant)) {
                    Set<Integer> numbers = tempTenantToNumbers.computeIfAbsent(tenant,
                        t -> Sets.newConcurrentHashSet());
                    numbers.add(number);
                }
            }
            metricToMappingInfo = tempMetricToMappingInfo;
            tenantToNumbers = tempTenantToNumbers;
        }

        try {
            MappingUtil.updateTenantAccessTime(highLevelClient, tenantAccessTimes, metricMappingIndexName);
            tenantAccessTimes.clear();
        } catch (Throwable e) {
            log.error(String.format("update access time error %s", e.getMessage()), e);
        }
    }

    private boolean needKeepTenantNumber(String tenant) {
        if (!keepTenantToNumbers) {
            return false;
        }
        if (notKeepTenantNumberPre != null) {
            for (String pre : notKeepTenantNumberPre) {
                if (tenant.startsWith(pre)) {
                    return false;
                }
            }
        }
        return true;
    }

    private void handleException(Throwable exception) throws Throwable {
        String stackString = Throwables.getStackTraceAsString(exception);
        if (stackString.contains("I/O reactor status: STOPPED") || stackString.contains("Connection closed")) {
            this.highLevelClient = createHighLevelClient(esClusterConf);
            log.error("es connection stopped, re connect");
        } else {
            throw exception;
        }
    }

}
