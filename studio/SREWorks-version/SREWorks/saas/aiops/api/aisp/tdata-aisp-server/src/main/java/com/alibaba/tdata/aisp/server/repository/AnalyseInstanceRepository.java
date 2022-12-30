package com.alibaba.tdata.aisp.server.repository;

import java.util.List;

import com.alibaba.tdata.aisp.server.common.condition.InstanceQueryCondition;
import com.alibaba.tdata.aisp.server.repository.domain.InstanceDO;

/**
 * @InterfaceName:AnalyseInstanceRepository
 * @Author:dyj
 * @DATE: 2021-11-15
 * @Description:
 **/
public interface AnalyseInstanceRepository {
    /**
     * @param instanceDO
     * @return
     */
    public int insert(InstanceDO instanceDO);

    /**
     * @param instanceCode
     * @return
     */
    InstanceDO queryById(String instanceCode);

    /**
     * @param condition
     * @return
     */
    public List<InstanceDO> queryByCondition(InstanceQueryCondition condition);

    /**
     * @param instanceCode
     * @return
     */
    public int deleteById(String instanceCode);

    /**
     * @param instanceDO
     * @return
     */
    int updateById(InstanceDO instanceDO);

    /**
     * @param condition
     * @param limit
     * @param skip
     * @return
     */
    List<InstanceDO> queryByRowBounds(InstanceQueryCondition condition, Integer limit, Integer skip);
}
