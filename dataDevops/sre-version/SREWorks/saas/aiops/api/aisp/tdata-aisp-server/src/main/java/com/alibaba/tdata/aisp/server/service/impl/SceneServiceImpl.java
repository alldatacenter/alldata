package com.alibaba.tdata.aisp.server.service.impl;

import java.util.List;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tdata.aisp.server.common.condition.SceneQueryCondition;
import com.alibaba.tdata.aisp.server.common.convert.SceneConfigConvert;
import com.alibaba.tdata.aisp.server.common.convert.UserInfoConvert;
import com.alibaba.tdata.aisp.server.common.dto.SceneConfigDto;
import com.alibaba.tdata.aisp.server.common.utils.SceneUtil;
import com.alibaba.tdata.aisp.server.controller.param.SceneCleanModelParam;
import com.alibaba.tdata.aisp.server.controller.param.SceneCreateParam;
import com.alibaba.tdata.aisp.server.controller.param.SceneQueryParam;
import com.alibaba.tdata.aisp.server.controller.param.SceneUpdateParam;
import com.alibaba.tdata.aisp.server.controller.param.SceneUpsertModelParam;
import com.alibaba.tdata.aisp.server.repository.SceneRepository;
import com.alibaba.tdata.aisp.server.repository.domain.SceneConfigDO;
import com.alibaba.tdata.aisp.server.service.SceneService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * @ClassName: SceneServiceImpl
 * @Author: dyj
 * @DATE: 2021-11-15
 * @Description:
 **/
@Slf4j
@Service
public class SceneServiceImpl implements SceneService {
    @Autowired
    private SceneRepository sceneRepository;
    @Autowired
    private SceneConfigConvert sceneConfigConvert;
    @Autowired
    private UserInfoConvert userInfoConvert;

    /**
     * @param param
     * @return
     */
    @Override
    public boolean create(SceneCreateParam param) {
        SceneConfigDO sceneConfigDO = new SceneConfigDO();
        SceneUtil.regularSceneCode(param.getSceneCode());
        sceneConfigDO.setSceneCode(param.getSceneCode());
        sceneConfigDO.setProductName(
            StringUtils.isEmpty(param.getProductList())? null : String.join(",", param.getProductList())
        );
        sceneConfigDO.setSceneName(param.getSceneName());
        sceneConfigDO.setComment(param.getComment());
        sceneConfigDO.setOwners(userInfoConvert.tos(param.getOwnerInfoList()));
        if (param.getDetectorBinder()==null) {
            sceneConfigDO.setDetectorBinder("*");
        } else {
            sceneConfigDO.setDetectorBinder(param.getDetectorBinder());
        }
        sceneRepository.insert(sceneConfigDO);
        return true;
    }

    @Override
    public boolean upsertModel(SceneUpsertModelParam param) {
        SceneConfigDO sceneConfigDO = sceneRepository.queryById(param.getSceneCode());
        if (sceneConfigDO==null){
            throw new IllegalArgumentException("action=upsertModel|| can not find scene of sceneCode:"+ param.getSceneCode());
        }
        if (StringUtils.isEmpty(sceneConfigDO.getSceneModelParam())){
            JSONObject sceneModelParam = new JSONObject();
            sceneModelParam.put(param.getDetectorCode(), param.getSceneModelParam());
            sceneConfigDO.setSceneModelParam(sceneModelParam.toJSONString());
        } else {
            JSONObject sceneModel = JSONObject.parseObject(sceneConfigDO.getSceneModelParam());
            sceneModel.put(param.getDetectorCode(), param.getSceneModelParam());
            sceneConfigDO.setSceneModelParam(sceneModel.toJSONString());
        }
        sceneRepository.updateById(sceneConfigDO);
        return true;
    }

    @Override
    public boolean cleanModel(SceneCleanModelParam param) {
        SceneConfigDO sceneConfigDO = sceneRepository.queryById(param.getSceneCode());
        if (sceneConfigDO==null){
            throw new IllegalArgumentException("action=upsertModel|| can not find scene of sceneCode:"+ param.getSceneCode());
        }
        if (!StringUtils.isEmpty(sceneConfigDO.getSceneModelParam())){
            JSONObject sceneModel = JSONObject.parseObject(sceneConfigDO.getSceneModelParam());
            sceneModel.remove(param.getDetectorCode());
            sceneConfigDO.setSceneModelParam(sceneModel.toJSONString());
        }
        sceneRepository.updateById(sceneConfigDO);
        return true;
    }

    /**
     * @param sceneCode
     * @return
     */
    @Override
    public boolean delete(String sceneCode) {
        sceneRepository.deleteById(sceneCode);
        return true;
    }

    @Override
    public List<SceneConfigDto> list(SceneQueryParam param, String empId) {
        SceneQueryCondition condition = SceneQueryCondition.builder().sceneCode(param.getSceneCode())
            .sceneCodeLike(param.getSceneCodeLike())
            .sceneName(param.getSceneName())
            .sceneNameLike(param.getSceneNameLike())
            .productList(param.getProductList())
            .owners(empId)
            .build();
        List<SceneConfigDO> sceneConfigDOList = sceneRepository.queryByRowBounds(condition, param.getPageSize(),
            (param.getPage() - 1) * param.getPageSize());
        List<SceneConfigDto> dtoList = sceneConfigConvert.froms(sceneConfigDOList);
        return dtoList;
    }

    @Override
    public boolean update(SceneUpdateParam param) {
        SceneConfigDO sceneConfigDO = sceneRepository.queryById(param.getSceneCode());
        if (sceneConfigDO==null){
            throw new IllegalArgumentException("action=update|| can not find scene of sceneCode:"+ param.getSceneCode());
        }
        sceneConfigDO.setSceneName(param.getSceneName());
        sceneConfigDO.setComment(param.getComment());
        sceneConfigDO.setOwners(userInfoConvert.tos(param.getOwnerInfoList()));
        sceneConfigDO.setProductName(
            CollectionUtils.isEmpty(param.getProductList())? null : String.join(",", param.getProductList())
        );
        log.info("action=update|| scene: {}", sceneConfigDO);
        sceneRepository.updateById(sceneConfigDO);
        return true;
    }

    @Override
    public SceneConfigDto queryById(String sceneCode) {
        SceneConfigDO sceneConfigDO = sceneRepository.queryById(sceneCode);
        if (sceneConfigDO==null) {
            throw new IllegalArgumentException("action=queryById|| can not find scene config by code:"+ sceneCode);
        }
        return sceneConfigConvert.from(sceneConfigDO);
    }
}
