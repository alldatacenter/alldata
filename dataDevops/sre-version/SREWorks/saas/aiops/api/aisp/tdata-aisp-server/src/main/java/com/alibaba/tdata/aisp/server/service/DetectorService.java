package com.alibaba.tdata.aisp.server.service;

import java.util.List;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tdata.aisp.server.controller.param.DetectorGitRegisterParam;
import com.alibaba.tdata.aisp.server.controller.param.DetectorQueryParam;
import com.alibaba.tdata.aisp.server.controller.param.DetectorRegisterParam;
import com.alibaba.tdata.aisp.server.controller.param.DetectorUpdateParam;
import com.alibaba.tdata.aisp.server.repository.domain.DetectorConfigDO;

/**
 * @InterfaceName:DetectorService
 * @Author:dyj
 * @DATE: 2021-11-15
 * @Description:
 **/
public interface DetectorService {
    /**
     * @param param
     * @return
     */
    public boolean register(DetectorRegisterParam param);

    /**
     * @param param
     * @return
     */
    public boolean registerGit(DetectorGitRegisterParam param);

    /**
     * @param detectorCode
     * @return
     */
    public DetectorConfigDO queryById(String detectorCode);

    /**
     * @param detectorCode
     * @return
     */
    public String getDoc(String detectorCode);;

    /**
     * @param detectorCode
     * @return
     */
    public boolean delete(String detectorCode);

    /**
     * @param param
     * @return
     */
    int updateById(DetectorUpdateParam param);

    /**
     * @param param
     * @return
     */
    List<DetectorConfigDO> list(DetectorQueryParam param);

    /**
     * @param detectorCode
     * @return
     */
    JSONObject getInput(String detectorCode);
}
