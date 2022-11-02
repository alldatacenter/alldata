package com.alibaba.sreworks.dataset.api.model;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.dataset.api.BasicApi;

import java.util.List;


/**
 * 数据模型数据接口
 */
public interface ModelDataService extends BasicApi {

    /**
     * 模型单条数据落盘
     * @param modelId
     * @param data
     * @return 实际写入数据条数
     * @throws Exception
     */
    int flushModelData(Integer modelId, JSONObject data) throws Exception;

    /**
     * 模型批量数据落盘
     * @param modelId
     * @param datas
     * @return 实际写入数据条数
     * @throws Exception
     */
    int flushModelDatas(Integer modelId, List<JSONObject> datas) throws Exception;
}
