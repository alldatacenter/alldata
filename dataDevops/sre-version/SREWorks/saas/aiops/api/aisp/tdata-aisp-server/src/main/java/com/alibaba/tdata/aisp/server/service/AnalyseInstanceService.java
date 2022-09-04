package com.alibaba.tdata.aisp.server.service;

import java.util.List;

import com.alibaba.tdata.aisp.server.common.condition.InstanceQueryCondition;
import com.alibaba.tdata.aisp.server.controller.param.AnalyseInstanceBacthUpsertModelParam;
import com.alibaba.tdata.aisp.server.controller.param.AnalyseInstanceCreateParam;
import com.alibaba.tdata.aisp.server.controller.param.AnalyseInstanceFeedbackParam;
import com.alibaba.tdata.aisp.server.controller.param.AnalyseInstanceQueryParam;
import com.alibaba.tdata.aisp.server.controller.param.AnalyseInstanceUpdateParam;
import com.alibaba.tdata.aisp.server.controller.param.AnalyseInstanceUpsertModelParam;
import com.alibaba.tdata.aisp.server.repository.domain.InstanceDO;

/**
 * @InterfaceName:AnalyseInstanceService
 * @Author:dyj
 * @DATE: 2021-11-15
 * @Description:
 **/
public interface AnalyseInstanceService {
    /**
     * @param param
     * @return
     */
    public String create(AnalyseInstanceCreateParam param);

    /**
     * @param instanceCode
     * @return
     */
    public InstanceDO queryById(String instanceCode);

    /**
     * @param param
     * @return
     */
    public List<InstanceDO> query(AnalyseInstanceQueryParam param);

    /**
     * @param condition
     * @return
     */
    public List<InstanceDO> queryByCondition(InstanceQueryCondition condition);

    /**
     * @param param
     * @return
     */
    public int updateById(AnalyseInstanceUpdateParam param);

    /**
     * @param param
     * @return
     */
    int upsertModel(AnalyseInstanceUpsertModelParam param);

    /**
     * @param param
     * @return
     */
    int batchUpsertModel(AnalyseInstanceBacthUpsertModelParam param);

    /**
     * @param param
     * @return
     */
    int feedback(String sceneCode, String detectorCode, AnalyseInstanceFeedbackParam param);

    /**
     * @param instanceCode
     * @return
     */
    public boolean delete(String instanceCode);
}
