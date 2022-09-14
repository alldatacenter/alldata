package com.alibaba.sreworks.warehouse.operator;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.util.TypeUtils;
import com.alibaba.sreworks.warehouse.common.client.ESClient;
import com.alibaba.sreworks.warehouse.common.constant.DwConstant;
import com.alibaba.sreworks.warehouse.common.exception.ESDocumentException;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    public synchronized int upsertJSON(String index, JSONObject document) throws Exception {
        if (!document.containsKey(DwConstant.META_ID)) {
            log.warn(String.format("索引:%s 数据缺失%s字段, 写入失败", index, DwConstant.PRIMARY_FIELD));
            throw new ESDocumentException(String.format("索引:%s 数据缺失%s字段, 写入失败", index, DwConstant.PRIMARY_FIELD));
        }

        UpdateRequest request = new UpdateRequest(index, TypeUtils.castToString(document.remove(DwConstant.META_ID)));
        request.doc(document, XContentType.JSON);
        request.docAsUpsert(true);

        RestHighLevelClient hlClient = esClient.getHighLevelClient();

        try {
            UpdateResponse updateResponse = hlClient.update(request, RequestOptions.DEFAULT);
            log.info(updateResponse.toString());
        } catch (Exception ex) {
            log.error(String.format("索引:%s 数据更新失败, 错误%s", index, ex));
            throw new ESDocumentException(String.format("索引:%s 数据更新失败, 错误%s", index, ex));
        }

        return 1;
    }

    public int upsertBulkJSON(String index, List<JSONObject> documents) throws Exception {
        BulkRequest request = new BulkRequest();

        DocWriteRequest[] updateRequests = documents.parallelStream()
                .filter(document -> document.containsKey(DwConstant.META_ID))
                .map(document -> {
                    UpdateRequest updateRequest = new UpdateRequest(index, TypeUtils.castToString(document.remove(DwConstant.META_ID)));
                    updateRequest.doc(document, XContentType.JSON);
                    updateRequest.docAsUpsert(true);
                    return (DocWriteRequest)updateRequest;
                }).toArray(DocWriteRequest[]::new);
        request.add(updateRequests);

        int diff = documents.size() - request.numberOfActions();
        if (diff > 0) {
            log.warn(String.format("索引:%s %s条数据缺失ID, 更新失败", index, diff));
        }
        if (request.numberOfActions() == 0) {
            return 0;
        }

        RestHighLevelClient hlClient = esClient.getHighLevelClient();
        try {
            log.info(request.numberOfActions()+"-numberOfActions");
            BulkResponse bulkResponse = hlClient.bulk(request, RequestOptions.DEFAULT);
            return bulkResponse.getItems().length;
        } catch (Exception ex) {
            log.error(String.format("索引:%s 数据更新失败, 错误%s", index, ex));
            throw new ESDocumentException(String.format("索引:%s 数据更新失败, 错误%s", index, ex));
        }
    }
}
