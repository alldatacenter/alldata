package com.alibaba.tdata.aisp.server.service;

import java.util.List;

import com.alibaba.tdata.aisp.server.common.dto.SceneConfigDto;
import com.alibaba.tdata.aisp.server.controller.param.SceneCleanModelParam;
import com.alibaba.tdata.aisp.server.controller.param.SceneCreateParam;
import com.alibaba.tdata.aisp.server.controller.param.SceneQueryParam;
import com.alibaba.tdata.aisp.server.controller.param.SceneUpdateParam;
import com.alibaba.tdata.aisp.server.controller.param.SceneUpsertModelParam;

/**
 * @InterfaceName:SceneService
 * @Author:dyj
 * @DATE: 2021-11-15
 * @Description:
 **/
public interface SceneService {
    /**
     * @param param
     * @return
     */
    public boolean create(SceneCreateParam param);

    /**
     * @param param
     * @return
     */
    boolean upsertModel(SceneUpsertModelParam param);

    /**
     * @param param
     * @return
     */
    boolean cleanModel(SceneCleanModelParam param);

    /**
     * @param sceneCode
     * @return
     */
    boolean delete(String sceneCode);

    /**
     * @param param
     * @param empId
     * @return
     */
    List<SceneConfigDto> list(SceneQueryParam param, String empId);

    /**
     * @param param
     * @return
     */
    boolean update(SceneUpdateParam param);

    SceneConfigDto queryById(String sceneCode);
}
