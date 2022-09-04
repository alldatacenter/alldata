package com.alibaba.tesla.tkgone.server.services.database.elasticsearch;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.tkgone.server.common.Constant;
import com.alibaba.tesla.tkgone.server.common.ErrorUtil;
import com.alibaba.tesla.tkgone.server.services.config.BackendStoreConfigService;
import com.alibaba.tesla.tkgone.server.services.config.ElasticSearchConfigService;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.mapper.IndexMapper;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.mapper.PartitionMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author jialiang.tjl
 */
@Service
@Slf4j
public class ElasticSearchDaemonService extends ElasticSearchBasic implements InitializingBean {

    @Autowired
    ElasticSearchConfigService elasticSearchConfigService;

    @Autowired
    ElasticSearchDocumentService elasticSearchDocumentService;

    @Autowired
    @Setter
    @Getter
    ElasticSearchIndicesService elasticSearchIndicesService;

    @Autowired
    ElasticSearchDeleteByQueryService elasticSearchDeleteByQueryService;

    @Autowired
    BackendStoreConfigService njehConfigService;

    @Autowired
    ElasticSearchSearchService elasticSearchSearchService;

    @Autowired
    ElasticSearchHttpApiBasic elasticSearchHttpApiBasic;

    @Autowired
    private PartitionMapper partitionMapper;

    @Autowired
    @Setter
    @Getter
    private IndexMapper indexMapper;

    @Value("${elasticsearch.index.daemon:true}")
    private Boolean enable;

    @Override
    public void afterPropertiesSet() {
        log.info("ES index daemon enable={}", enable);
        if (enable) {
            ScheduledExecutorService executorService =
                new ScheduledThreadPoolExecutor(6, new ElasticSearchDaemonThreadFactory());
            executorService.scheduleAtFixedRate(this::addPartitions, 1, 60, TimeUnit.SECONDS);
            executorService.scheduleAtFixedRate(this::addIndices, 1, 60, TimeUnit.SECONDS);
        }
    }

    private void addPartitions() {
        for (String aliasIndex : indexMapper.getAliasIndexes()) {
            JSONArray retArray = null;
            JSONObject jsonObject =
                    JSONObject.parseObject(String.format(
                            "{" + "    \"meta\": {" + "        \"aggSize\": 10000,"
                                    + "        \"aggField\": \"%s\"," + "        \"type\": \"%s\""
                                    + "    }," + "    \"sort\": {" + "        \"field\": \"key\","
                                    + "        \"reverse\": true" + "    },"
                                    + "    \"style\": \"options\"" + "}",
                            Constant.PARTITION_FIELD, aliasIndex));

            try {
                retArray = elasticSearchSearchService.aggByKv(jsonObject, Arrays.asList(aliasIndex),
                        false);
            } catch (Exception e) {
                if (e.getMessage().contains("index_not_found_exception")) {
                    log.debug("index not found: " + aliasIndex, e);
                } else {
                    ErrorUtil.log(log, e, String.format("获取%s分区失败", aliasIndex));
                }
            }

            if (retArray != null) {
                List<String> partitions = retArray.toJavaList(JSONObject.class).stream()
                        .map(x -> x.getString("value")).collect(Collectors.toList());
                partitions.removeAll(Arrays.asList(null, ""));
                partitionMapper.set(aliasIndex, partitions);
            }
        }
    }

    private void addIndices() {
        Map<String, Set<String>> indexMap = new HashMap<>(Constant.HASHMAP_INIT_CAPACITY);
        JSONObject aliases;
        try {
            aliases = elasticSearchIndicesService.getAllAlias();
        } catch (Exception e) {
            log.error("get all alias failed!", e);
            return ;
        }
        log.debug(String.format("aliases size %s", aliases.size()));
        for (String host : aliases.keySet()) {
            JSONObject aliasObj = aliases.getJSONObject(host);
            for (String realIndex : aliasObj.keySet()) {
                Set<String> aliasSet = new HashSet<>();
                aliasSet.addAll(
                        aliasObj.getJSONObject(realIndex).getJSONObject("aliases").keySet());
                indexMap.put(realIndex, aliasSet);
            }

        }
        indexMapper.reset(indexMap);
    }

    private class ElasticSearchDaemonThreadFactory implements ThreadFactory {
        private final String workerName;
        private final AtomicInteger threadId = new AtomicInteger(1);

        ElasticSearchDaemonThreadFactory() {
            this.workerName = "ElasticSearchDaemonThreadFactory's -worker-";
        }

        @Override
        public Thread newThread(@Nonnull Runnable r) {
            String threadName = this.workerName + this.threadId.getAndIncrement();
            Thread thread = new Thread(r, threadName);
            log.info(String.format("new thread name is: %s", thread.getName()));
            return thread;
        }
    }

}
