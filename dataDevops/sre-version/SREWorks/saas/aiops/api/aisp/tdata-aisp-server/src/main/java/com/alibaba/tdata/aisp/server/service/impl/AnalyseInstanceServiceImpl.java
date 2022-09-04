package com.alibaba.tdata.aisp.server.service.impl;

import java.util.List;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tdata.aisp.server.common.condition.InstanceQueryCondition;
import com.alibaba.tdata.aisp.server.common.utils.ClassUtil;
import com.alibaba.tdata.aisp.server.common.utils.MessageDigestUtil;
import com.alibaba.tdata.aisp.server.controller.param.AnalyseInstanceBacthUpsertModelParam;
import com.alibaba.tdata.aisp.server.controller.param.AnalyseInstanceCreateParam;
import com.alibaba.tdata.aisp.server.controller.param.AnalyseInstanceFeedbackParam;
import com.alibaba.tdata.aisp.server.controller.param.AnalyseInstanceQueryParam;
import com.alibaba.tdata.aisp.server.controller.param.AnalyseInstanceUpdateParam;
import com.alibaba.tdata.aisp.server.controller.param.AnalyseInstanceUpsertModelParam;
import com.alibaba.tdata.aisp.server.repository.AnalyseInstanceRepository;
import com.alibaba.tdata.aisp.server.repository.domain.InstanceDO;
import com.alibaba.tdata.aisp.server.service.AnalyseInstanceService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * @ClassName: AnalyseInstanceServiceImpl
 * @Author: dyj
 * @DATE: 2021-11-15
 * @Description:
 **/
@Slf4j
@Service
public class AnalyseInstanceServiceImpl implements AnalyseInstanceService {
    @Autowired
    private AnalyseInstanceRepository instanceRepository;

    /**
     * @param param
     * @return
     */
    @Override
    public String create(AnalyseInstanceCreateParam param) {
        assert !StringUtils.isEmpty(param.getDetectorCode());
        assert !StringUtils.isEmpty(param.getSceneCode());
        assert !StringUtils.isEmpty(param.getEntityId());
        InstanceDO instanceDO = new InstanceDO();
        instanceDO.setInstanceCode(MessageDigestUtil.genSHA256(
            param.getSceneCode()
                .concat(param.getDetectorCode())
                .concat(param.getEntityId())));
        instanceDO.setSceneCode(param.getSceneCode());
        instanceDO.setDetectorCode(param.getDetectorCode());
        instanceDO.setEntityId(param.getEntityId());
        instanceRepository.insert(instanceDO);
        return instanceDO.getInstanceCode();
    }

    @Override
    public InstanceDO queryById(String instanceCode) {
        return instanceRepository.queryById(instanceCode);
    }

    @Override
    public List<InstanceDO> query(AnalyseInstanceQueryParam param) {
        InstanceQueryCondition condition = InstanceQueryCondition.builder()
            .instanceCode(param.getInstanceCode())
            .sceneCode(param.getSceneCode())
            .detectorCode(param.getDetectorCode())
            .entityId(param.getEntityId())
            .build();
        return instanceRepository.queryByRowBounds(condition, param.getPageSize(),
            (param.getPage() - 1) * param.getPageSize());
    }

    /**
     * @param condition
     * @return
     */
    @Override
    public List<InstanceDO> queryByCondition(InstanceQueryCondition condition) {
        return instanceRepository.queryByCondition(condition);
    }

    @Override
    public int updateById(AnalyseInstanceUpdateParam param) {
        InstanceDO instanceDO = new InstanceDO();
        ClassUtil.copy(param, instanceDO);
        instanceDO.setInstanceCode(MessageDigestUtil.genSHA256(
            param.getSceneCode()
                .concat(param.getDetectorCode())
                .concat(param.getEntityId())));
        return instanceRepository.updateById(instanceDO);
    }

    @Override
    public int upsertModel(AnalyseInstanceUpsertModelParam param) {
        String instanceCode = MessageDigestUtil.genSHA256(param.getSceneCode()
                .concat(param.getDetectorCode())
                .concat(param.getEntityId()));
        InstanceDO instanceDO = instanceRepository.queryById(instanceCode);
        if (instanceDO==null){
            throw new IllegalArgumentException("action=upsertModel || Can not find instance by param:"+param);
        }
        if (!StringUtils.isEmpty(instanceDO.getModelParam())){
            JSONObject modelParam = JSONObject.parseObject(instanceDO.getModelParam());
            modelParam.putAll(param.getModelParam());
            instanceDO.setModelParam(modelParam.toJSONString());
        } else {
            instanceDO.setModelParam(param.getModelParam().toJSONString());
        }

        return instanceRepository.updateById(instanceDO);
    }

    @Override
    public int batchUpsertModel(AnalyseInstanceBacthUpsertModelParam param) {
        List<InstanceDO> instanceDOList = instanceRepository.queryByCondition(
            InstanceQueryCondition.builder().sceneCode(param.getSceneCode()).detectorCode(
                    param.getDetectorCode())
                .build());
        int num = 0;
        if (!CollectionUtils.isEmpty(instanceDOList)) {
            for (InstanceDO instanceDO : instanceDOList) {
                JSONObject modelParam = JSONObject.parseObject(instanceDO.getModelParam());
                modelParam.putAll(param.getModelParam());
                instanceDO.setModelParam(modelParam.toJSONString());
                instanceRepository.updateById(instanceDO);
                num++;
            }
        }

        return num;
    }

    @Override
    public int feedback(String sceneCode, String detectorCode, AnalyseInstanceFeedbackParam param) {
        String instanceCode = MessageDigestUtil.genSHA256(sceneCode
            .concat(detectorCode)
            .concat(param.getEntityId()));
        InstanceDO instanceDO = instanceRepository.queryById(instanceCode);
        if (instanceDO==null){
            throw new IllegalArgumentException("action=feedback || Can not find instance by param:"+param);
        }
        if (CollectionUtils.isEmpty(param.getFeedback())) {
            return 0;
        }
        JSONObject recentFeedback;
        if (StringUtils.isEmpty(instanceDO.getRecentFeedback())) {
            recentFeedback = new JSONObject();
        } else {
            recentFeedback = JSONObject.parseObject(instanceDO.getRecentFeedback());
        }
        for (String key : param.getFeedback().keySet()) {
            // 每个key保留最近100次提交
            if (recentFeedback.containsKey(key)) {
                JSONArray feedbackItems = recentFeedback.getJSONArray(key);
                feedbackItems.addAll(param.getFeedback().getJSONArray(key));
                if (feedbackItems.size() > 100) {
                    recentFeedback.put(key, feedbackItems.subList(feedbackItems.size()-101, feedbackItems.size()-1));
                } else {
                    recentFeedback.put(key, feedbackItems);
                }
            } else {
                JSONArray feedbackItems = new JSONArray();
                feedbackItems.addAll(param.getFeedback().getJSONArray(key));
                recentFeedback.put(key, feedbackItems);
            }
        }
        instanceDO.setRecentFeedback(recentFeedback.toJSONString());
        return instanceRepository.updateById(instanceDO);
    }

    /**
     * @param instanceCode
     * @return
     */
    @Override
    public boolean delete(String instanceCode) {
        instanceRepository.deleteById(instanceCode);
        return true;
    }
}
