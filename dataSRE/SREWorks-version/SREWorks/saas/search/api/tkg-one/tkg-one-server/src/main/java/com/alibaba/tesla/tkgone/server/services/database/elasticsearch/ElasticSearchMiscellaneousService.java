package com.alibaba.tesla.tkgone.server.services.database.elasticsearch;

import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.core.MainResponse;
import org.springframework.stereotype.Service;

/**
 * @author jialiang.tjl
 */
@Service
public class ElasticSearchMiscellaneousService extends ElasticSearchBasic {

    public MainResponse getInfo() throws Exception {
        return getRestHighLevelClient(null).info(RequestOptions.DEFAULT);
    }

    public boolean ping() throws Exception {
        return getRestHighLevelClient(null).ping(RequestOptions.DEFAULT);
    }

}
