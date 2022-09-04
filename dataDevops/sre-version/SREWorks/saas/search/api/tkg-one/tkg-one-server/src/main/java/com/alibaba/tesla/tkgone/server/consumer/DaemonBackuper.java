package com.alibaba.tesla.tkgone.server.consumer;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.tkgone.server.domain.dto.BackendStoreDTO;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.ElasticSearchHttpApiBasic;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.ElasticSearchIndicesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * @author xueyong.zxy
 */

@Service
public class DaemonBackuper {
    @Autowired
    ElasticSearchHttpApiBasic elasticSearchHttpApiBasic;

    @Autowired
    ElasticSearchIndicesService elasticSearchIndicesService;

    Map<Long, ScheduledFuture<?>> runningIndex = new ConcurrentHashMap<>();

    public void checkIndices() {
        Set<String> alias = elasticSearchIndicesService.getIndexes();
        List<BackendStoreDTO> backendStores = elasticSearchIndicesService
                .getSearchBackendStores(new ArrayList<>(alias));
        for (BackendStoreDTO backendStore : backendStores) {
            Map<String, JSONObject> indices = backendStore.getIndices();
            for (String index : indices.keySet()) {
                
            }
        }
    }

}