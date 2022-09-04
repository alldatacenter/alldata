package com.alibaba.tesla.tkgone.server.services.database.elasticsearch;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.tkgone.server.common.Constant;
import com.alibaba.tesla.tkgone.server.common.DateUtil;
import com.alibaba.tesla.tkgone.server.common.ErrorUtil;
import com.alibaba.tesla.tkgone.server.common.Tools;
import com.alibaba.tesla.tkgone.server.domain.dto.ConsumerDto;
import com.alibaba.tesla.tkgone.server.services.config.ElasticSearchConfigService;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.elasticsearchgrammar.GetQueryGrammar;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.mapper.IndexMapper;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.params.DocumentBulkRequest;
import com.alibaba.tesla.tkgone.server.services.tsearch.BackendStoreService;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMethod;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * @author jialiang.tjl
 */
@EqualsAndHashCode(callSuper = true)
@Service
@Slf4j
@Data
public class ElasticSearchUpsertService extends ElasticSearchBasic implements InitializingBean {
    static private final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Autowired
    ElasticSearchConfigService elasticSearchConfigService;

    @Autowired
    ElasticSearchDocumentService elasticSearchDocumentService;

    @Autowired
    ElasticSearchIndicesService elasticSearchIndicesService;

    @Autowired
    ElasticSearchDeleteByQueryService elasticSearchDeleteByQueryService;

    @Autowired
    ElasticSearchUpsertService elasticSearchUpsertService;

    @Autowired
    GetQueryGrammar getQueryGrammar;

    @Autowired
    private IndexMapper indexMapper;

    @Autowired
    BackendStoreService njehService;

    private InternalStatistics internalStatistics = new InternalStatistics();

    private BlockingQueue<Runnable> blockingQueue = new ArrayBlockingQueue<>(Constant.QUEUE_MAX_SIZE);

    private ThreadPoolExecutor threadPool = Tools.createThreadPoolWithQueue(Constant.ELASTICSEARCH_UPSERT_THREAD_POOL,
            "ESUpserts", blockingQueue);

    private Long lastModifyTimeStampMilli = System.currentTimeMillis();

    private String getUpdateScriptSource(JSONObject updateJson, String parentKeyDotPath) {
        String ctxStartString = "ctx._source";
        StringBuilder updateScriptSourceSb = new StringBuilder();
        for (String key : updateJson.keySet()) {
            String keyDotPath = org.springframework.util.StringUtils.isEmpty(parentKeyDotPath) ? key
                    : parentKeyDotPath + "." + key;
            Object value = JSONObject.toJSON(updateJson.get(key));
            if (value instanceof JSONObject) {
                updateScriptSourceSb.append("if (").append(ctxStartString).append(".").append(keyDotPath)
                        .append(" == null) {").append(ctxStartString).append(".").append(keyDotPath)
                        .append(" = params.").append(keyDotPath).append(";} else {")
                        .append(getUpdateScriptSource((JSONObject) value, keyDotPath)).append("}");
            } else {
                updateScriptSourceSb.append(ctxStartString).append(".").append(keyDotPath).append(" = params['")
                        .append(keyDotPath).append("'];");
            }
        }
        return updateScriptSourceSb.toString();
    }

    public JSONObject updateByQuery(String index, JSONObject queryJson, JSONObject updateJson) throws Exception {
        queryJson = getQueryGrammar.get(queryJson);

        JSONObject scriptJson = new JSONObject();
        scriptJson.put("script", new JSONObject());
        scriptJson.getJSONObject("script").put("params", updateJson);
        scriptJson.getJSONObject("script").put("source", getUpdateScriptSource(updateJson, null));
        queryJson.putAll(scriptJson);

        String uri = String.format("/%s/_update_by_query", index);
        Map<String, String> params = new HashMap<>(0);
        params.put("conflicts", "proceed");
        log.info(String.format("uri: %s, params: %s, postBody: %s", uri, params, queryJson));
        return getOrPost(uri, index, params, JSONObject.toJSONString(queryJson), RequestMethod.POST, true);

    }

    public JSONObject upsert(List<JSONObject> dataList) throws Exception {
        Set<String> set = new HashSet<>();
        List<DocumentBulkRequest> documentBulkRequests = new ArrayList<>();
        for (JSONObject data : dataList) {
            if (!data.keySet().containsAll(Constant.NODE_RELATION_META)) {
                throw new Exception("实体缺失meta信息");
            }

            String index = data.getString(Constant.INNER_TYPE);
            if (!indexMapper.isAliasContains(index)) {
                try {
                    elasticSearchIndicesService.create(index);
                    Tools.sleep(1);
                } catch (Exception e) {
                    log.error("create index ERROR: ", e);
                }
            }
            String realUpsertIndex = elasticSearchConfigService.getElasticsearchRealUpsertIndex(index);
            String id = data.getString(Constant.INNER_ID).trim() + Constant.AFTER_SEPARATOR
                    + data.getOrDefault(Constant.PARTITION_FIELD, Constant.DEFAULT_PARTITION);
            String realId = index + id;
            if (set.contains(realId)) {
                continue;
            } else {
                set.add(index + id);
            }

            documentBulkRequests.add(DocumentBulkRequest.builder().index(realUpsertIndex)
                    .id(id.substring(Math.max(id.length() - 512, 0))).document(adjustJsonObjectToEs(data).getInnerMap())
                    .build());
        }
        return elasticSearchDocumentService.bulkByApi(documentBulkRequests);
    }

    private CounterManager manager = new CounterManager();

    public void upserts(String source, List<JSONObject> dataList) {
        Counter counter = manager.getCounter(source);
        JSONArray highPriorityIndices = JSON.parseArray(elasticSearchConfigService.getHighPriorityIndices());
        long time = System.currentTimeMillis();
        try {
            dataList = deNodes(dataList);
            int each = Constant.ELASTICSEARCH_UPSERTS_SIZE;
            int size = dataList.size();
            int upsertTimes = 0;
            for (int index = 0; index < size; index += each) {
                int eIndex = Math.min(index + each, size);
                log.info(String.format("execute upsert data list begin: %s", dataList.get(0)));
                threadPool.execute(new Upsert(dataList, index, eIndex));
                waitForAWhile(++upsertTimes, dataList.get(0), highPriorityIndices);
            }
        } finally {
            counter.step(dataList.size(), System.currentTimeMillis() - time);
        }
    }

    public void upserts(String source, List<JSONObject> dataList, Long consumeStime, ConsumerDto consumerDto) {
        Counter counter = manager.getCounter(source);
        JSONArray highPriorityIndices = JSON.parseArray(elasticSearchConfigService.getHighPriorityIndices());
        long time = System.currentTimeMillis();
        try {
            dataList = deNodes(dataList);
            int each = Constant.ELASTICSEARCH_UPSERTS_SIZE;
            int size = dataList.size();
            int upsertTimes = 0;
            for (int index = 0; index < size; index += each) {
                int eIndex = Math.min(index + each, size);
                log.info(String.format("execute upsert data list begin: %s", dataList.get(0)));
                threadPool.execute(new Upsert(dataList, index, eIndex, consumeStime, consumerDto));
                waitForAWhile(++upsertTimes, dataList.get(0), highPriorityIndices);
            }
        } finally {
            counter.step(dataList.size(), System.currentTimeMillis() - time);
        }
    }

    private void waitForAWhile(int upsertTimes, JSONObject data, JSONArray highPriorityIndices) {
        int half = 2;
        if (upsertTimes % (Constant.ELASTICSEARCH_UPSERT_THREAD_POOL / half) == 0
                && !highPriorityIndices.contains(data.getString("__type"))) {
            // 单个对象导入超过最大线程数一半时，等待30秒
            log.warn(String.format("wait %ss for %s", Constant.UPSERT_SLEEP_IN_SECONDS,
                    highPriorityIndices.contains(data.getString("__type"))));
            Tools.sleep(Constant.UPSERT_SLEEP_IN_SECONDS);
        }
    }

    class Upsert implements Runnable {

        List<JSONObject> dataList;
        int sIndex;
        int eIndex;
        int size;
        Long consumeStime = System.currentTimeMillis();
        ConsumerDto consumerDto;

        Upsert(List<JSONObject> dataList, int sIndex, int eIndex) {
            this.dataList = dataList;
            this.sIndex = sIndex;
            this.eIndex = eIndex;
            this.size = dataList.size();
        }

        Upsert(List<JSONObject> dataList, int sIndex, int eIndex, Long consumeStime, ConsumerDto consumerDto) {
            this.dataList = dataList;
            this.sIndex = sIndex;
            this.eIndex = eIndex;
            this.size = dataList.size();
            this.consumeStime = consumeStime;
            this.consumerDto = consumerDto;
        }

        @Override
        public void run() {
            try {
                internalStatistics.addSum(eIndex - sIndex);
                internalStatistics.addBatchSum();
                List<JSONObject> partDataList = dataList.subList(sIndex, eIndex);
                JSONObject retJson = elasticSearchUpsertService.upsert(partDataList);
                JSONArray items = retJson.getJSONArray("items");
                Map<String, Integer> summaryMap = new Hashtable<>();
                for (JSONObject item : items.toJavaList(JSONObject.class)) {
                    for (String action : item.keySet()) {
                        String result = item.getJSONObject(action).getString("result");
                        String index = item.getJSONObject(action).getString("_index");
                        int status = item.getJSONObject(action).getIntValue("status");
                        int failed = 1;
                        try {
                            failed = item.getJSONObject(action).getJSONObject("_shards").getIntValue("failed");
                        } catch (Exception ignored) {
                        }
                        if (failed != 0) {
                            log.error("es写入失败: " + JSONObject.toJSONString(item, true));
                        }
                        String key = String.format("%s:%s:%s", index, result, status);
                        summaryMap.put(key, summaryMap.getOrDefault(key, 0) + 1);
                    }
                }
                if (log.isDebugEnabled()) {
                    log.debug("summaryMap: " + JSONObject.toJSONString(summaryMap, true));
                }
            } catch (Exception e) {
                log.error(String.format("elasticsearch upsert错误: %s", e.getMessage()));
                threadPool.execute(new Upsert(dataList, sIndex, eIndex, consumeStime, consumerDto));
            }
            long consumeEtime = System.currentTimeMillis();
            log.info(String.format("execute upsert data list end, cost: %s", consumeEtime - consumeStime));

            if (consumerDto != null) {
                // 有consumer配置时才需要通知用户
                notify(consumeEtime);
            }

            try {
                Thread.sleep(Constant.UPSERT_INTERVAL_IN_MILLIS);
            } catch (InterruptedException e) {
                ErrorUtil.log(log, e, "sleep interrupted");
            }
        }

        private void notify(long consumeEtime) {
            List<String> empIds = new ArrayList<>();
            String notifiers = consumerDto.getNotifiers();
            if (StringUtils.isNotEmpty(notifiers)) {
                JSONArray notifierArray = JSONObject.parseArray(notifiers);
                for (JSONObject notifierObj : notifierArray.toJavaList(JSONObject.class)) {
                    empIds.add(notifierObj.getString("emplId"));
                }
            }
            log.info(String.format("empIds are %s", empIds));
            if (consumeEtime - this.consumeStime > consumerDto.getEffectiveThreshold() * 1000L) {
                log.info(String.format("知识图谱数据同步生效超时： %s", this.dataList.get(0)));
                //if (!empIds.isEmpty()) {
                //    for (String empId : empIds) {
                //        UserMessge userMessage = new UserMessge();
                //        userMessage.setApp("tesla");
                //        userMessage.setComment("");
                //        userMessage.setEmplId(empId);
                //        userMessage.setEncoderUrl(true);
                //        userMessage.setMessage(buildNotifyMessage(consumeEtime));
                //        userMessage.setTitle("知识图谱数据同步生效超时");
                //        userMessage.setUrl("");
                //        innerNotifyService.sendDingTalk(userMessage);
                //    }
                //}
            }
        }

        private String buildNotifyMessage(long consumeEtime) {
            String message = "生效耗时：%sms\n\n生效时间阈值：%ss\n\nconsumer名称：%s\n\n" + "数据生效发起时间：%s\n\n数据生效结束时间：%s";
            long costTime = consumeEtime - this.consumeStime;
            long threshHold = this.consumerDto.getEffectiveThreshold();
            String name = this.consumerDto.getName();
            String startTime = DATE_FORMAT.format(new Date(this.consumeStime));
            String endTime = DATE_FORMAT.format(new Date(consumeEtime));
            return String.format(message, costTime, threshHold, name, startTime, endTime);
        }

    }

    @Data
    public class InternalStatistics {
        long sum = 0;
        long batchSum = 0;
        String startDate;
        String nowDate;
        long queueSize = 0;
        long actionSize = 0;
        int threadPoolCoreSize = 0;
        int consumerOkNum = 0;
        int consumerFailedNum = 0;
        int consumerDisabledNum = 0;

        InternalStatistics() {
            startDate = Tools.currentDataString();
            nowDate = Tools.currentDataString();
        }

        /**
         * 查询自启动起到目前为止的累计处理吞吐量
         *
         * @return
         */
        public double getTps() {
            if (!StringUtils.isEmpty(startDate) && !StringUtils.isEmpty(nowDate) && sum > 0) {
                try {
                    double seconds = (DateUtil.parse(nowDate, DateUtil.PATTERN_YYYYMMDD_HHMMSS).getTime()
                            - DateUtil.parse(startDate, DateUtil.PATTERN_YYYYMMDD_HHMMSS).getTime()) / 1000.0;
                    if (seconds > 0) {
                        return Math.round(sum / seconds * 100) / 100.0;
                    }
                } catch (IllegalArgumentException e) {
                    ErrorUtil.log(log, e, "parse date failed. {}, {}", startDate, nowDate);
                }
            }
            return 0;
        }

        void addSum(long num) {
            sum += num;
        }

        void addBatchSum() {
            addBatchSum(1);
        }

        void addBatchSum(long num) {
            batchSum += num;
        }
    }

    @Override
    public void afterPropertiesSet() {

        Runnable runnable = () -> {
            internalStatistics.setQueueSize(blockingQueue.size());
            internalStatistics.setNowDate(Tools.currentDataString());
            internalStatistics.setActionSize(threadPool.getActiveCount());
            internalStatistics.setThreadPoolCoreSize(threadPool.getCorePoolSize());
        };

        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(runnable, 1, 1, TimeUnit.SECONDS);

    }

    public List<JSONObject> deNodes(List<JSONObject> nodes) {
        Set<String> set = new HashSet<>();
        List<JSONObject> deNodes = new ArrayList<>();
        for (JSONObject node : nodes) {
            if (node != null) {
                String id = node.getString(Constant.INNER_ID);
                if (!set.contains(id)) {
                    deNodes.add(node);
                    set.add(id);
                }
            }
        }
        return deNodes;
    }

    public JSONObject fillNodeMeta(JSONObject node) throws Exception {
        String index = node.getString(Constant.INNER_TYPE);
        node.put(Constant.UPSERT_TIME_FIELD, System.currentTimeMillis() / 1000);
        if (!node.containsKey(Constant.PARTITION_FIELD)) {
            String partition = njehService.getNewestPartition(index);
            node.put(Constant.PARTITION_FIELD, partition);
        }
        if (!node.keySet().containsAll(Constant.NODE_RELATION_META)) {
            throw new Exception("实体缺失meta信息");
        }
        return node;
    }

    public List<JSONObject> fillNodesMeta(List<JSONObject> nodes) throws Exception {
        for (JSONObject node : nodes) {
            fillNodeMeta(node);
        }
        return nodes;
    }

    public void addNodesIndex(List<JSONObject> nodes) throws Exception {
        for (JSONObject node : nodes) {
            String index = node.getString(Constant.INNER_TYPE);
            if (!indexMapper.isAliasContains(index)) {
                elasticSearchIndicesService.create(index);
            }
        }
    }
}
