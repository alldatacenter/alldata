package com.alibaba.tdata.aisp.server.repository;

import java.util.List;

import com.alibaba.tdata.aisp.server.common.condition.DetectorQueryCondition;
import com.alibaba.tdata.aisp.server.repository.domain.DetectorConfigDO;

/**
 * @InterfaceName:DetectorRepository
 * @Author:dyj
 * @DATE: 2021-11-15
 * @Description:
 **/
public interface DetectorRepository {
    /**
     * @param configDO
     * @return
     */
    public int insert(DetectorConfigDO configDO);

    /**
     * @param detectorCode
     * @return
     */
    DetectorConfigDO queryById(String detectorCode);

    /**
     * @param detectorCode
     * @return
     */
    public int delete(String detectorCode);

    /**
     * @param configDO
     * @return
     */
    int updateById(DetectorConfigDO configDO);

    /**
     * @param condition
     * @param limit
     * @param skip
     * @return
     */
    List<DetectorConfigDO> queryWithRowBounds(DetectorQueryCondition condition, Integer limit, Integer skip);
}
