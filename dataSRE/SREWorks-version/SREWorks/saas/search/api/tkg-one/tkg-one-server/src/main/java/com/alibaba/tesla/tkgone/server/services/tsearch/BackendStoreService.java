package com.alibaba.tesla.tkgone.server.services.tsearch;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.tkgone.server.common.Constant;
import com.alibaba.tesla.tkgone.server.common.Tools;
import com.alibaba.tesla.tkgone.server.domain.dto.ConsumerDto;
import com.alibaba.tesla.tkgone.server.services.config.BackendStoreConfigService;
import com.alibaba.tesla.tkgone.server.services.config.Neo4jConfigService;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.ElasticSearchDaemonService;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.ElasticSearchIndicesService;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.ElasticSearchSearchService;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.ElasticSearchUpsertService;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.mapper.PartitionMapper;
import lombok.Data;
import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * @author yangjinghua
 */
@Service
@Log4j
public class BackendStoreService {

    @Autowired
    Neo4jConfigService neo4jConfigService;

    @Autowired
    BackendStoreConfigService njehConfigService;

    @Autowired
    ElasticSearchIndicesService elasticSearchIndicesService;

    @Autowired
    BackendStoreService backendStoreService;

    @Autowired
    ElasticSearchDaemonService elasticSearchDaemonService;

    @Autowired
    ElasticSearchSearchService elasticSearchSearchService;

    @Autowired
    PartitionMapper partitionMapper;

    @Autowired
    ElasticSearchUpsertService elasticSearchUpsertService;

    @Data
    private class NodeAndRelation {

        private List<JSONObject> nodes = new CopyOnWriteArrayList<>();
        private List<List<JSONObject>> relations = new CopyOnWriteArrayList<>();

    }

    private List<String> getNeo4jTypeNodeFields(String type) {
        List<String> typeFields = neo4jConfigService.getNeo4jTypeNodeExtraFields(type);
        typeFields.addAll(Constant.NODE_RELATION_META);
        return typeFields;
    }

    private NodeAndRelation saveBackendStoreData(List<List<JSONObject>> backendStoreData, String partition) {
        NodeAndRelation nodeAndRelation = new NodeAndRelation();

        backendStoreData.parallelStream().forEach(line -> {
            if (line == null) {
                return;
            }
            if (line.size() != 1 && line.size() != 3) {
                log.error("导入数据存在非单实体、或者实体-实体-关系的结构: " + line);
                return;
            }
            for (int i = 0; i < line.size(); i++) {
                line.get(i).put(Constant.UPSERT_TIME_FIELD, Tools.currentTimestamp());
                if (i < 2) {
                    if (line.get(i).getString(Constant.INNER_TYPE) == null) {
                        log.error("__type不存在或值为null实体内容为:" + line.get(i));
                        continue;
                    }
                    if (line.get(i).getString(Constant.INNER_ID) == null) {
                        if (line.get(i).containsKey("query") && line.get(i).containsKey("update")) {
                            JSONObject queryJson = line.get(i).getJSONObject("query");
                            JSONObject updateJson = line.get(i).getJSONObject("update");
                            String type = line.get(i).getString(Constant.INNER_TYPE);
                            try {
                                elasticSearchUpsertService.updateByQuery(type, queryJson, updateJson);
                            } catch (Exception e) {
                                log.error("updateByQuery ERROR: ", e);
                            }
                            break;
                        } else {
                            log.error(String.format("实体的__id不存在或值为null实体内容为: %s; " + "并且不存在query,update的json地段用于配置更新语法",
                                    line.get(i)));
                            continue;
                        }
                    }
                    nodeAndRelation.getNodes().add(line.get(i));
                    String realPartition = partition;
                    if (realPartition == null) {
                        realPartition = getNewestPartition(line.get(i).getString(Constant.INNER_TYPE));
                    }

                    line.get(i).put(Constant.PARTITION_FIELD, realPartition);
                } else {
                    String relationType = line.get(2).getString(Constant.INNER_TYPE);
                    String relationId = line.get(2).getString(Constant.INNER_ID);
                    if (StringUtils.isEmpty(relationId)) {
                        List<String> nodeIds = line.subList(0, 2).stream().map(x -> x.getString(Constant.INNER_ID))
                                .collect(Collectors.toList());
                        line.get(2).put(Constant.INNER_ID, String.join(Constant.RELATION_META_SEPARATOR, nodeIds));
                    }
                    if (StringUtils.isEmpty(relationType)) {
                        List<String> nodeTypes = line.subList(0, 2).stream().map(x -> x.getString(Constant.INNER_TYPE))
                                .collect(Collectors.toList());
                        line.get(2).put(Constant.INNER_TYPE, String.join(Constant.RELATION_META_SEPARATOR, nodeTypes));
                    }

                    List<JSONObject> neo4jRelation = new ArrayList<>();
                    for (int j = 0; j < 3; j++) {
                        String partType = line.get(j).getString(Constant.INNER_TYPE);
                        JSONObject partNeo4jRelation = new JSONObject();
                        neo4jRelation.add(partNeo4jRelation);
                        if (j < 2) {
                            for (String key : getNeo4jTypeNodeFields(partType)) {
                                partNeo4jRelation.put(key, line.get(j).get(key));
                            }
                        } else {
                            partNeo4jRelation.putAll(line.get(j));
                        }
                    }
                    nodeAndRelation.getRelations().add(neo4jRelation);
                }
            }
        });

        return nodeAndRelation;
    }

    public JSONObject importData(String source, List<List<JSONObject>> backendStoreData, String partition) {
        JSONObject ret = new JSONObject();
        NodeAndRelation nodeAndRelation = backendStoreService.saveBackendStoreData(backendStoreData, partition);
        elasticSearchUpsertService.upserts(source, nodeAndRelation.getNodes());
        return ret;
    }

    public JSONObject importData(String source, List<List<JSONObject>> backendStoreData, String partition,
            Long consumeStime, ConsumerDto consumerDto) {
        JSONObject ret = new JSONObject();
        NodeAndRelation nodeAndRelation = backendStoreService.saveBackendStoreData(backendStoreData, partition);
        elasticSearchUpsertService.upserts(source, nodeAndRelation.getNodes(), consumeStime, consumerDto);
        return ret;
    }

    public String getNewestPartition(String index) {
        List<String> partitions = partitionMapper.get(index);
        if (CollectionUtils.isEmpty(partitions)) {
            partitions = Collections.singletonList(Constant.WORLD_START_DATE);
        }
        return partitions.get(0);
    }

}
