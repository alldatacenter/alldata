package com.alibaba.tdata.aisp.server.repository;

import java.util.List;

import com.alibaba.tdata.aisp.server.common.condition.SceneQueryCondition;
import com.alibaba.tdata.aisp.server.repository.domain.SceneConfigDO;

/**
 * @InterfaceName:SceneRepository
 * @Author:dyj
 * @DATE: 2021-11-15
 * @Description:
 **/
public interface SceneRepository {
    public int insert(SceneConfigDO configDO);

    public int deleteById(String sceneCode);

    List<SceneConfigDO> queryByRowBounds(SceneQueryCondition condition, Integer limit, Integer skip);

    SceneConfigDO queryById(String sceneCode);

    int updateById(SceneConfigDO sceneConfigDO);
}
