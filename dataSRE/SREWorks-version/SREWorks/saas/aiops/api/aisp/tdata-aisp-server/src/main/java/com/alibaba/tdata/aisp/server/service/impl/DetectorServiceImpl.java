package com.alibaba.tdata.aisp.server.service.impl;

import java.io.IOException;
import java.util.List;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tdata.aisp.server.common.condition.DetectorQueryCondition;
import com.alibaba.tdata.aisp.server.common.exception.PlatformInternalException;
import com.alibaba.tdata.aisp.server.common.properties.EnvProperties;
import com.alibaba.tdata.aisp.server.common.utils.ClassUtil;
import com.alibaba.tdata.aisp.server.common.utils.DetectorUtil;
import com.alibaba.tdata.aisp.server.common.utils.RequestUtil;
import com.alibaba.tdata.aisp.server.controller.param.DetectorGitRegisterParam;
import com.alibaba.tdata.aisp.server.controller.param.DetectorQueryParam;
import com.alibaba.tdata.aisp.server.controller.param.DetectorRegisterParam;
import com.alibaba.tdata.aisp.server.controller.param.DetectorUpdateParam;
import com.alibaba.tdata.aisp.server.repository.DetectorRepository;
import com.alibaba.tdata.aisp.server.repository.domain.DetectorConfigDO;
import com.alibaba.tdata.aisp.server.service.DetectorService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @ClassName: DetectorServiceImpl
 * @Author: dyj
 * @DATE: 2021-11-15
 * @Description:
 **/
@Slf4j
@Service
public class DetectorServiceImpl implements DetectorService {
    @Autowired
    private DetectorRepository detectorRepository;
    @Autowired
    private EnvProperties envProperties;

    /**
     * @param param
     * @return
     */
    @Override
    public boolean register(DetectorRegisterParam param) {
        DetectorConfigDO detectorConfigDO = new DetectorConfigDO();
        detectorConfigDO.setDetectorCode(param.getDetectorCode());
        detectorConfigDO.setDetectorUrl(param.getDetectorUrl());
        detectorConfigDO.setComment(param.getComment());
        detectorRepository.insert(detectorConfigDO);
        return true;
    }

    @Override
    public boolean registerGit(DetectorGitRegisterParam param) {

        return false;

    }

    /**
     * @param detectorCode
     * @return
     */
    @Override
    public DetectorConfigDO queryById(String detectorCode) {
        return detectorRepository.queryById(detectorCode);
    }

    @Override
    public String getDoc(String detectorCode) {
        DetectorConfigDO configDO = detectorRepository.queryById(detectorCode);
        String host = DetectorUtil.buildUrl(envProperties.getStageId(), configDO.getDetectorUrl());
        String url = "http://".concat(host).concat("/doc");
        String docStr;
        try {
            docStr = RequestUtil.post(url, null, "",null);
        } catch (IOException e) {
            log.error("action=getDoc|| can not get doc from detector:{}", detectorCode, e);
            throw new PlatformInternalException("action=getDoc|| can not get doc from detector:"+ detectorCode + " message:" + e.getMessage(), e);
        }
        return JSONObject.parseObject(docStr, String.class);
    }

    /**
     * @param detectorCode
     * @return
     */
    @Override
    public boolean delete(String detectorCode) {
        detectorRepository.delete(detectorCode);
        return true;
    }

    @Override
    public int updateById(DetectorUpdateParam param) {
        DetectorConfigDO configDO = detectorRepository.queryById(param.getDetectorCode());
        assert configDO!=null;
        ClassUtil.copy(param, configDO);
        return detectorRepository.updateById(configDO);
    }

    @Override
    public List<DetectorConfigDO> list(DetectorQueryParam param) {
        DetectorQueryCondition condition = DetectorQueryCondition.builder().detectorCode(param.getDetectorCode()).build();
        return detectorRepository.queryWithRowBounds(condition, param.getPageSize(),
            (param.getPage() - 1) * param.getPageSize());
    }

    @Override
    public JSONObject getInput(String detectorCode) {
        DetectorConfigDO configDO = detectorRepository.queryById(detectorCode);
        String host = DetectorUtil.buildUrl(envProperties.getStageId(), configDO.getDetectorUrl());
        String url = "http://".concat(host).concat("/input");
        String docStr;
        try {
            docStr = RequestUtil.post(url, null, "", null);
        } catch (IOException e) {
            log.error("action=getInput|| can not get input from detector:{}", detectorCode, e);
            throw new PlatformInternalException("action=getInput|| can not get input from detector:"+ detectorCode, e.getCause());
        }
        return JSONObject.parseObject(docStr, JSONObject.class);
    }
}
