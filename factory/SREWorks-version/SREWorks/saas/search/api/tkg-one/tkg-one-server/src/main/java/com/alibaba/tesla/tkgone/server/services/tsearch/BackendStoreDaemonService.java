package com.alibaba.tesla.tkgone.server.services.tsearch;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.tkgone.server.common.Constant;
import com.alibaba.tesla.tkgone.server.common.Tools;
import com.alibaba.tesla.tkgone.server.services.config.BackendStoreConfigService;
import com.alibaba.tesla.tkgone.server.services.config.BaseConfigService;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.ElasticSearchDeleteByQueryService;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.mapper.IndexMapper;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.mapper.PartitionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * @author yangjinghua
 */
@Service
@Slf4j
public class BackendStoreDaemonService implements InitializingBean {
    private static final int DELETING_TASK_MAX_NUM = 3;
    private static final int CLEAN_REDUNDANCY_PARTITION_PERIOD = 300;
    private static final int CLEAN_INVALID_DATA_PERIOD = 60;

    @Autowired
    BackendStoreConfigService njehConfigService;

    @Autowired
    ElasticSearchDeleteByQueryService elasticSearchDeleteByQueryService;

    @Autowired
    private PartitionMapper partitionMapper;

    @Autowired
    private IndexMapper indexMapper;

    @Autowired
    private BaseConfigService baseConfigService;

    private String localIp = Tools.getLocalIp();

    private Boolean checkLocalRun() {
        return true;
//        String consumerNodes = baseConfigService.getContentWithOutCache(
//                ConfigDto.builder().name("consumerNodes").build());
//        if (StringUtils.isNotEmpty(consumerNodes)) {
//            for (String consumerIp : consumerNodes.split(",")) {
//                if (consumerIp.equals(localIp)) {
//                    log.info(String.format("run njeh daemon service, local ip is: %s", localIp));
//                    return true;
//                }
//            }
//        }
//        log.info(String.format("local ip is %s, do not run njeh daemon service", localIp));
//        return false;
    }

    @Override
    public void afterPropertiesSet() {
        if (checkLocalRun()) {
            ScheduledExecutorService threadExecutor = new ScheduledThreadPoolExecutor(
                    10, new NJEHDaemonThreadFactory());
            threadExecutor.scheduleAtFixedRate(this::cleanRedundancyPartition, 0,
                    CLEAN_REDUNDANCY_PARTITION_PERIOD, TimeUnit.SECONDS);
            threadExecutor.scheduleAtFixedRate(this::cleanInvalidData, 0, CLEAN_INVALID_DATA_PERIOD,
                    TimeUnit.SECONDS);
        }

    }

    private void cleanRedundancyPartition() {
        for (String index : indexMapper.getAliasIndexes()) {
            List<String> partitions = partitionMapper.get(index);
            partitions.sort(Collections.reverseOrder());
            int partitionNums = njehConfigService.getTypePartitionNums(index);
            if (partitions.size() > partitionNums) {
                String borderPartition = partitions.get(partitionNums);
                try {
                    log.info(String.format("[DELETE_BY_QUERY]delete data from %s where partition lessEqual than %s",
                            index, borderPartition));
                    elasticSearchDeleteByQueryService.deleteInvalidNode(index, Constant.PARTITION_FIELD,
                            borderPartition);
                } catch (Exception e) {
                    log.error("error, ", e);
                }
            }
        }
    }

    private class NJEHDaemonThreadFactory implements ThreadFactory {
        private final String workerName;
        private final AtomicInteger threadId = new AtomicInteger(1);

        NJEHDaemonThreadFactory() {
            this.workerName = "NJEHDaemonThreadFactory's -worker-";
        }

        @Override
        public Thread newThread(@Nonnull Runnable r) {
            String threadName = this.workerName + this.threadId.getAndIncrement();
            Thread thread = new Thread(r, threadName);
            log.info(String.format("new thread name is: %s", thread.getName()));
            return thread;
        }
    }

    private Integer getDeletingTaskNum(JSONObject deletingTasksObj) {
        int deletingTaskNum = 0;
        JSONObject nodes = deletingTasksObj.getJSONObject("nodes");
        if (null != nodes) {
            for (String nodeName : nodes.keySet()) {
                JSONObject nodeObj = nodes.getJSONObject(nodeName);
                deletingTaskNum += nodeObj.getJSONObject("tasks").keySet().size();
            }
        }
        return deletingTaskNum;
    }

    private void cleanInvalidData() {
        try {

            log.info("clean invalid data start:");
            for (String index : indexMapper.getAliasIndexes()) {
                int deletingTaskNum = this.getDeletingTaskNum(elasticSearchDeleteByQueryService.queryDeletingTasks(index));
                if (deletingTaskNum > DELETING_TASK_MAX_NUM) {
                    log.info(String.format("delete by query tasks more than %s: %s", DELETING_TASK_MAX_NUM,
                            deletingTaskNum));
                    break;
                }
                log.info(String.format("clean %s invalid data", index));
                int validData = njehConfigService.getTypeValidTime(index);
                if (validData == Constant.NO_TTL) {
                    continue;
                }
                try {
                    String task = elasticSearchDeleteByQueryService.deleteInvalidNodeNoWait(index,
                            Constant.UPSERT_TIME_FIELD,
                            Long.toString(System.currentTimeMillis() / 1000 - validData));
                    log.info(String.format("clean invalid task: %s", task));
                } catch (Exception e) {
                    log.error("清理过期数据错误: ", e);
                }
            }
            log.info("clean invalid end;");
        } catch (Exception e) {
            log.error("cleanInvalidData", e);
        }
    }

}
