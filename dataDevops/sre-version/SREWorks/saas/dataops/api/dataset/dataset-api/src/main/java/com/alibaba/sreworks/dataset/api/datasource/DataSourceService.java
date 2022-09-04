package com.alibaba.sreworks.dataset.api.datasource;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.dataset.api.BasicApi;


public interface DataSourceService extends BasicApi {
    /**
     * 根据ID查询
     * @param dataSourceId
     * @return
     */
    JSONObject getDataSourceById(String dataSourceId);
}
