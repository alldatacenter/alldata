package com.alibaba.sreworks.health.api.risk;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.health.api.BasicApi;
import com.alibaba.sreworks.health.domain.req.risk.RiskTypeCreateReq;
import com.alibaba.sreworks.health.domain.req.risk.RiskTypeUpdateReq;

import java.util.List;

public interface RiskTypeService extends BasicApi {

    /**
     * 按照Id查询风险类型
     * @return
     */
    JSONObject getRiskTypeById(Integer id);


    /**
     * 查询风险类型列表
     * @return
     */
    List<JSONObject> getRiskTypes();

    /**
     * 风险类型存在
     * @param id
     * @return
     */
    boolean existRiskType(Integer id);

    /**
     * 风险类型不存在
     * @param id
     * @return
     */
    boolean notExistRiskType(Integer id);

    /**
     * 新建风险类型
     * @param req
     * @return
     */
    int addRiskType(RiskTypeCreateReq req);

    /**
     * 更新风险类型
     * @param req
     * @return
     * @throws Exception
     */
    int updateRiskType(RiskTypeUpdateReq req) throws Exception;

    /**
     * 删除风险类型
     * @param id
     * @return
     * @throws Exception
     */
    int deleteRiskType(Integer id) throws Exception;
}
