package com.alibaba.sreworks.health.api.incident;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.health.api.BasicApi;
import com.alibaba.sreworks.health.domain.req.incident.IncidentTypeCreateReq;
import com.alibaba.sreworks.health.domain.req.incident.IncidentTypeUpdateReq;

import java.util.List;

public interface IncidentTypeService extends BasicApi {

    /**
     * 按照Id查询异常类型
     * @return
     */
    JSONObject getIncidentTypeById(Integer id);

    /**
     * 按照label查询异常类型
     * @return
     */
    JSONObject getIncidentTypeByLabel(String label);

    /**
     * 查询异常类型列表
     * @return
     */
    List<JSONObject> getIncidentTypes();

    /**
     * 异常类型存在
     * @param id
     * @return
     */
    boolean existIncidentType(Integer id);

    /**
     * 异常类型不存在
     * @param id
     * @return
     */
    boolean notExistIncidentType(Integer id);

    /**
     * 新建异常类型
     * @param req
     * @return
     * @throws Exception
     */
    int addIncidentType(IncidentTypeCreateReq req) throws Exception;

    /**
     * 更新异常类型
     * @param req
     * @return
     * @throws Exception
     */
    int updateIncidentType(IncidentTypeUpdateReq req) throws Exception;

    /**
     * 删除异常类型
     * @param id
     * @return
     * @throws Exception
     */
    int deleteIncidentType(Integer id) throws Exception;
}
