package com.alibaba.tesla.tkgone.server.services.database.elasticsearch;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.mapper.IndexMapper;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.params.DocumentBulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.*;

/**
 * @author jialiang.tjl
 */
@Service
public class ElasticSearchDocumentService extends ElasticSearchBasic {

    @Autowired
    ElasticSearchDocumentService elasticSearchDocumentService;

    @Autowired
    ElasticSearchIndicesService elasticSearchIndicesService;

    @Autowired
    private IndexMapper indexMapper;

    public String index(String index, String id, Map<String, Object> document) throws Exception {
        if (!indexMapper.isAliasContains(index)) {
            elasticSearchIndicesService.create(index);
        }
        IndexRequest indexRequest = new IndexRequest(index).source(document);
        return getRestHighLevelClient(index).index(indexRequest, RequestOptions.DEFAULT).getResult().getLowercase();
    }

    public Map<String, Object> get(String index, String id) throws Exception {
        GetRequest getRequest = new GetRequest(index, id);
        return getRestHighLevelClient(index).get(getRequest, RequestOptions.DEFAULT).getSource();
    }

    public boolean exists(String index, String id) throws Exception {
        GetRequest getRequest = new GetRequest(index, id);
        getRequest.fetchSourceContext(new FetchSourceContext(false));
        return getRestHighLevelClient(index).exists(getRequest, RequestOptions.DEFAULT);
    }

    public String delete(String index, String id) throws Exception {
        DeleteRequest request = new DeleteRequest(index, id);
        return getRestHighLevelClient(index).delete(request, RequestOptions.DEFAULT).getResult().getLowercase();
    }

    JSONObject bulkByApi(List<DocumentBulkRequest> documentBulkRequests) throws Exception {

        List<String> postBody = new ArrayList<>();
        Set<String> indices = new HashSet<>();
        for (DocumentBulkRequest documentBulkRequest : documentBulkRequests) {
            JSONObject indexJson = new JSONObject();
            indexJson.put("update", new JSONObject());
            indexJson.getJSONObject("update").put("_index", documentBulkRequest.getIndex().trim());
            indexJson.getJSONObject("update").put("_id", documentBulkRequest.getId().trim());
            indexJson.getJSONObject("update").put("retry_on_conflict", 3);
            postBody.add(JSONObject.toJSONString(indexJson));
            JSONObject tmpJson = new JSONObject();
            tmpJson.put("doc", documentBulkRequest.getDocument());
            tmpJson.put("doc_as_upsert", true);
            postBody.add(JSONObject.toJSONString(tmpJson));

            indices.add(documentBulkRequest.getIndex().trim());
        }

        // TODO: 目前trick处理理论上一次bulk只会存取相同index的数据
        return elasticSearchDocumentService.getOrPostByIndices("/_bulk", new ArrayList<>(indices), null,
                String.join("\n", postBody) + "\n", RequestMethod.POST, true).getJSONObject(0);
    }

}
