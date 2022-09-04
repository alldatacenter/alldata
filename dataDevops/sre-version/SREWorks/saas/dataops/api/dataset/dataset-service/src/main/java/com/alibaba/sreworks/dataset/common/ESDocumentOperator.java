package com.alibaba.sreworks.dataset.common;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.util.TypeUtils;
import com.alibaba.sreworks.dataset.common.constant.Constant;
import com.alibaba.sreworks.dataset.common.exception.ESDocumentException;
import com.alibaba.sreworks.dataset.connection.ESClient;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * ES文档操作服务类
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/11/10 20:52
 */
@Service
@Slf4j
public class ESDocumentOperator {

    @Autowired
    ESClient esClient;

    public synchronized int upsertJSON(String index, String datasourceId, JSONObject document) throws Exception {
        if (!document.containsKey(Constant.META_ID)) {
            log.warn(String.format("索引:%s 数据缺失%s字段, 写入失败", index, Constant.ID));
            throw new ESDocumentException(String.format("索引:%s 数据缺失%s字段, 写入失败", index, Constant.ID));
        }

        UpdateRequest request = new UpdateRequest(index, TypeUtils.castToString(document.remove(Constant.META_ID)));
        request.doc(document, XContentType.JSON);
        request.docAsUpsert(true);

        RestHighLevelClient hlClient = esClient.getHighLevelClient(datasourceId);

        try {
            UpdateResponse updateResponse = hlClient.update(request, RequestOptions.DEFAULT);
            log.info(updateResponse.toString());
        } catch (Exception ex) {
            log.error(String.format("索引:%s 数据更新失败, 错误%s", index, ex));
            throw new ESDocumentException(String.format("索引:%s 数据更新失败, 错误%s", index, ex));
        }

        return 1;
    }

    public int upsertBulkJSON(String index, String datasourceId, List<JSONObject> documents) throws Exception {
        BulkRequest request = new BulkRequest();

//        List<UpdateRequest> updateRequests = documents.parallelStream().filter(
//                document -> document.containsKey(Constant.META_ID)
//        ).map(document -> {
//            UpdateRequest updateRequest = new UpdateRequest(index, TypeUtils.castToString(document.remove(Constant.META_ID)));
//            updateRequest.doc(document, XContentType.JSON);
//            updateRequest.docAsUpsert(true);
//            return (DocWriteRequest)updateRequest;
//        }).collect(Collectors.toList());

        List<JSONObject> illegalDocuments = new ArrayList<>();
        documents.forEach(document -> {
            if (document.containsKey(Constant.META_ID)) {
                UpdateRequest updateRequest = new UpdateRequest(index, TypeUtils.castToString(document.remove(Constant.META_ID)));
                updateRequest.doc(document, XContentType.JSON);
                updateRequest.docAsUpsert(true);
                request.add(updateRequest);
            } else {
                illegalDocuments.add(document);
            }
        });
        if (!CollectionUtils.isEmpty(illegalDocuments)) {
            log.warn(String.format("索引:%s %s条数据缺失ID, 更新失败", index, illegalDocuments.size()));
        }

        RestHighLevelClient hlClient = esClient.getHighLevelClient(datasourceId);
        try {
            if (request.numberOfActions() == 0) {
                return 0;
            }
            log.info(request.numberOfActions()+"-numberOfActions");
            BulkResponse bulkResponse = hlClient.bulk(request, RequestOptions.DEFAULT);
            log.info(bulkResponse.getItems().length+"-getItems");
            return bulkResponse.getItems().length;
        } catch (Exception ex) {
            log.error(String.format("索引:%s 数据更新失败, 错误%s", index, ex));
            throw new ESDocumentException(String.format("索引:%s 数据更新失败, 错误%s", index, ex));
        }
    }
}
