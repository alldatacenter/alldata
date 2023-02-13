package com.alibaba.sreworks.warehouse.services;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.warehouse.common.constant.DwConstant;
import com.alibaba.sreworks.warehouse.operator.ESDocumentOperator;
import com.alibaba.sreworks.warehouse.operator.ESIndexOperator;
import com.alibaba.sreworks.warehouse.operator.ESLifecycleOperator;
import com.alibaba.sreworks.warehouse.operator.ESSearchOperator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/12/01 17:17
 */
@Slf4j
@Service
public class DwCommonService {
    @Autowired
    ESIndexOperator esIndexOperator;

    @Autowired
    ESDocumentOperator esDocumentOperator;

    @Autowired
    ESSearchOperator esSearchOperator;

    @Autowired
    ESLifecycleOperator esLifecycleOperator;

    protected int doFlushDatas(String alias, String index, String partitionFormat, Integer lifecycle, List<JSONObject> esDatas) throws Exception{
        if (CollectionUtils.isEmpty(esDatas)) {
            return 0;
        }

        AtomicInteger size = new AtomicInteger();
        Map<String, List<JSONObject>> classifiedDatas = classifiedDataByPartition(esDatas);
        classifiedDatas.keySet().parallelStream().forEach(dataPartition -> {
            List<JSONObject> partitionEsDatas = classifiedDatas.get(dataPartition);

            try {
                String indexName = inferIndexNameByPartition(index, partitionFormat, dataPartition);
                log.info(String.format("====[start]flush data, es_index:%s, es_index_alias:%s====", indexName, alias));
                if (!esIndexOperator.existIndex(indexName)) {
                    // 创建索引
                    createTableMeta(indexName, alias, lifecycle);
                }

                int cnt;
                if (esDatas.size() > 1) {
                    cnt = esDocumentOperator.upsertBulkJSON(indexName, partitionEsDatas);
                } else {
                    cnt = esDocumentOperator.upsertJSON(indexName, partitionEsDatas.get(0));
                }
                size.addAndGet(cnt);
                log.info(String.format("====[end]flush data, es_index:%s, es_index_alias:%s====", indexName, alias));
            } catch (Exception ex) {
                log.error(String.format("====flush data, es_index_alias:%s, exception:%s====", alias, ex));
                throw new RuntimeException(ex);
            }
        });
        return size.get();
    }

    protected void createTableMeta(String tableName, String tableAlias, Integer lifecycle) throws Exception {
        // 生命周期管理
        log.info("====create lifecycle policy====");
        String policyName = esLifecycleOperator.createLifecyclePolicy(lifecycle);
        // TODO 按照数据类型做mapping映射
        log.info("====create index====");
        esIndexOperator.createIndexIfNotExist(tableName, tableAlias, policyName);
    }

    protected void updateTableLifecycle(String tableName, String tableAlias, Integer lifecycle) throws Exception {
        // 生命周期管理
        log.info("====create lifecycle policy====");
        String policyName = esLifecycleOperator.createLifecyclePolicy(lifecycle);

        log.info("====update index lifecycle====");
        esIndexOperator.updateIndexLifecyclePolicy(tableName, tableAlias, policyName);
    }

    protected JSONObject statsTable(String tableName, String tableAlias) {
        JSONObject stats = new JSONObject();

        Set<String> indices = new HashSet<>();
        try {
            indices = esIndexOperator.getIndicesByAlias(tableAlias);
            int partitionCount = indices.size();
            stats.put("partitionCount", partitionCount);
        } catch (Exception ex) {
            log.error(String.format("统计模型[table:%s, alias:%s]分区数异常, 详情:%s", tableName, tableAlias, ex.getMessage()));
        }

        try {
            long docCount = esSearchOperator.countDocByIndices(indices);
            stats.put("docCount", docCount);
        } catch (Exception ex) {
            log.error(String.format("统计模型[table:%s, alias:%s]]最新分区实例数异常, 详情:%s",tableName, tableAlias, ex.getMessage()));
            stats.put("docCount", 0);
        }

        return stats;
    }

    protected String inferIndexNameByPartition(String index, String partitionFormat, String dataPartition) throws Exception {
        String prefixIndex = index.substring(1, index.lastIndexOf("_"));

        Date date = new SimpleDateFormat(DwConstant.DATE_FORMAT.getString(partitionFormat)).parse(dataPartition);
        SimpleDateFormat f = new SimpleDateFormat(DwConstant.INDEX_DATE_PATTERN.getString(partitionFormat));
        String datePattern = f.format(date);

        return prefixIndex + "_" + datePattern;
    }

    protected Map<String, List<JSONObject>> classifiedDataByPartition(List<JSONObject> esDatas) {
        Map<String, List<JSONObject>> classifiedDatas = new HashMap<>();
        for(JSONObject esData : esDatas) {
            String partition = esData.getString(DwConstant.PARTITION_DIM);
            classifiedDatas.putIfAbsent(partition, new ArrayList<>());
            List<JSONObject> datas = classifiedDatas.get(partition);
            datas.add(esData);
        }
        return classifiedDatas;
    }
}
