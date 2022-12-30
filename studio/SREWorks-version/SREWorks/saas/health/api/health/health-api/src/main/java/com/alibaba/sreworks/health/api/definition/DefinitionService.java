package com.alibaba.sreworks.health.api.definition;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.health.api.BasicApi;
import com.alibaba.sreworks.health.domain.req.definition.DefinitionCreateReq;
import com.alibaba.sreworks.health.domain.req.definition.DefinitionUpdateReq;

import java.util.List;

public interface DefinitionService extends BasicApi {

    /**
     * 定义统计信息
     * @return
     */
    JSONObject getDefinitionsStat();

    /**
     * 定义实例统计信息
     * @return
     */
    JSONObject getInstancesStat(String appInstanceId);

    /**
     * 所有实例统计
     * @return
     */
    JSONObject getTotalInstancesInc(String appInstanceId);


    /**
     * 当日实例新增数据
     */
    JSONObject getCurrentDayInstancesInc(String appInstanceId);

    /**
     * 按照ID查询定义
     * @return
     */
    JSONObject getDefinitionById(Integer id);

    /**
     * 按照ID查询定义
     * @return
     */
    List<JSONObject> getDefinitionByIds(List<Integer> ids);

    /**
     * 按照分类查询定义
     * @return
     */
    List<JSONObject> getDefinitionsByCategory(String category);

    /**
     * 按照应用查询定义
     * @return
     */
    List<JSONObject> getDefinitionsByApp(String appId);

    /**
     * 按照应用组件查询定义
     * @return
     */
    List<JSONObject> getDefinitionsByAppComponent(String appId, String appComponentName);

    /**
     * 按照应用+分类查询定义
     * @return
     */
    List<JSONObject> getDefinitions(String appId, String appComponentName, String name, String category);

    /**
     * 事件定义存在
     * @param id
     * @return
     */
    boolean existDefinition(Integer id);

    /**
     * 事件定义不存在
     * @param id
     * @return
     */
    boolean notExistDefinition(Integer id);

    /**
     * 新建事件定义
     * @param req
     * @return
     * @throws Exception
     */
    int addDefinition(DefinitionCreateReq req) throws Exception;

    /**
     * 更新事件定义
     * @param req
     * @return
     * @throws Exception
     */
    int updateDefinition(DefinitionUpdateReq req) throws Exception;

    /**
     * 删除事件定义
     * @param id
     * @return
     * @throws Exception
     */
    int deleteDefinition(Integer id) throws Exception;
}
