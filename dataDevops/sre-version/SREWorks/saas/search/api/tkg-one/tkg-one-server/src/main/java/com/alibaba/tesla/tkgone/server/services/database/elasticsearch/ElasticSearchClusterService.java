package com.alibaba.tesla.tkgone.server.services.database.elasticsearch;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.tesla.tkgone.server.domain.dto.BackendStoreDTO;
import com.alibaba.tesla.tkgone.server.services.config.ElasticSearchConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;

/**
 * @author jialiang.tjl
 */
@Service
public class ElasticSearchClusterService extends ElasticSearchBasic {

    @Autowired
    ElasticSearchConfigService elasticSearchConfigService;

    public JSONArray getHealth() throws Exception {
        List<BackendStoreDTO> backendStoreDTOs = getAllBackendStores();
        return getOrPostByBackendStores("/_cluster/health?level=cluster", backendStoreDTOs, null, null,
                RequestMethod.GET, true);
    }

}
