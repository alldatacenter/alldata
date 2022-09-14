package com.alibaba.sreworks.pmdb.api.datasource;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.pmdb.api.BasicApi;
import com.alibaba.sreworks.pmdb.domain.req.datasource.DataSourceCreateReq;
import com.alibaba.sreworks.pmdb.domain.req.datasource.DataSourceUpdateReq;

import java.util.List;

public interface DataSourceService extends BasicApi {
    /**
     * 根据ID查询
     * @param dataSourceId
     * @return
     */
    JSONObject getDataSourceById(Integer dataSourceId);

    /**
     * 按照App查询数据源
     * @return
     */
    List<JSONObject> getDataSourceByApp(String appId);

    /**
     * 按照App查询数据源 (查询sreworks内置数据源)
     * @return
     */
    List<JSONObject> getDataSourceWithCommonByApp(String appId);

    /**
     * 按照类型查询数据源 (查询sreworks内置数据源)
     * @return
     */
    List<JSONObject> getDataSourceWithCommonByType(String appId, String dataSourceType);

    /**
     * 新建数据源
     * @param req
     */
    int createDataSource(DataSourceCreateReq req);

    /**
     * 更新数据源
     * @param req
     */
    int updateDataSource(DataSourceUpdateReq req) throws Exception;

    /**
     * 删除数据源
     * @param dataSourceId
     * @return
     */
    int deleteDataSourceById(Integer dataSourceId);
}
