package com.alibaba.sreworks.warehouse.api.domain;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.warehouse.api.BasicApi;
import com.alibaba.sreworks.warehouse.domain.req.domain.DomainCreateReq;

import java.util.List;

/**
 * 数据域接口
 */
public interface DomainService extends BasicApi {

    /**
     * 根据ID查询数据域信息
     * @return
     */
    JSONObject getDoaminById(Integer id);

    /**
     * 根据名称查询数据域信息
     * @return
     */
    JSONObject getDomainByName(String name);

    /**
     * 根据主题查询数据域信息
     * @return
     */
    List<JSONObject> getDomainBySubject(String subject);

    /**
     * 根据简写查询数据域信息
     * @return
     */
    JSONObject getDomainByAbbreviation(String abbreviation);

    /**
     * 查询数据域列表
     * @return
     */
    List<JSONObject> getDomains();

    /**
     * 根据ID删除数据域
     * @param id
     * @return
     */
    int deleteDomainById(Integer id) throws Exception;

    /**
     * 创建数据域
     * @param req
     * @return
     */
    int createDomain(DomainCreateReq req) throws Exception;
}
