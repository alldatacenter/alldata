package com.alibaba.sreworks.dataset.api.domain;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.dataset.api.BasicApi;
import com.alibaba.sreworks.dataset.domain.req.domain.DataDomainCreateReq;
import com.alibaba.sreworks.dataset.domain.req.domain.DataDomainUpdateReq;

import java.util.List;
import java.util.Map;

/**
 * 数据域接口
 */
public interface DomainService extends BasicApi {

    /**
     * 根据数据域ID查询数据域信息
     * @param domainId 数据域ID
     * @return
     */
    JSONObject getDomainById(Integer domainId);

    /**
     * 根据主题查询数据域信息
     * @param subjectId 主题ID
     * @return
     */
    List<JSONObject> getDomainsBySubject(Integer subjectId);

    /**
     * 查询数据域信息(兼容全量查询)
     * @param subjectId 主题ID
     * @return
     */
    List<JSONObject> getDomains(Integer subjectId);

    /**
     * 查询主题关联的数据域数量
     * @param subjectId
     * @return
     */
    Long countDomainsBySubject(Integer subjectId);


    /**
     * 查询主题关联的数据域数量
     * @return 主题ID，数据域数量
     */
    Map<Integer, Long> countDomains();

    /**
     * 数据域存在
     * @param domainId
     * @return
     */
    boolean existDomain(Integer domainId);

    /**
     * 内置数据域
     * @param domainId
     * @return
     */
    boolean buildInDomain(Integer domainId);

    /**
     * 新增数据域
     * @param req
     * @return
     */
    int addDomain(DataDomainCreateReq req) throws Exception;

    /**
     * 更新数据域
     * @param req
     * @return
     */
    int updateDomain(DataDomainUpdateReq req)throws Exception;

    /**
     * 删除数据域
     * @param domainId
     * @return
     */
    int deleteDomainById(Integer domainId) throws Exception;

}
