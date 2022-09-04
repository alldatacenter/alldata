package com.alibaba.tdata.aisp.server.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tdata.aisp.server.common.constant.AispResult;
import com.alibaba.tdata.aisp.server.common.constant.ModelLevelEnum;
import com.alibaba.tdata.aisp.server.common.dto.SceneConfigDto;
import com.alibaba.tdata.aisp.server.common.utils.MessageDigestUtil;
import com.alibaba.tdata.aisp.server.controller.param.AnalyseInstanceUpsertModelParam;
import com.alibaba.tdata.aisp.server.controller.param.ModelQueryParam;
import com.alibaba.tdata.aisp.server.controller.param.ModelUpsertParam;
import com.alibaba.tdata.aisp.server.controller.param.SceneQueryParam;
import com.alibaba.tdata.aisp.server.controller.param.SceneUpsertModelParam;
import com.alibaba.tdata.aisp.server.repository.domain.InstanceDO;
import com.alibaba.tdata.aisp.server.service.AnalyseInstanceService;
import com.alibaba.tdata.aisp.server.service.SceneService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import static com.alibaba.tdata.aisp.server.common.factory.AispResponseFactory.buildSuccessResult;

/**
 * @ClassName: ModelController
 * @Author: dyj
 * @DATE: 2022-05-10
 * @Description:
 **/
@Api(tags = "Model接口")
@Slf4j
@RestController
@RequestMapping("/model/")
public class ModelController {

    @Autowired
    private SceneService sceneService;
    @Autowired
    private AnalyseInstanceService instanceService;

    @ApiOperation("查询参数")
    @PostMapping(value = "query")
    @ResponseBody
    public AispResult queryModel(@RequestBody @Validated ModelQueryParam param){
        ModelLevelEnum modelLevelEnum = ModelLevelEnum.fromValue(param.getLevel());
        if (ModelLevelEnum.SCENE.equals(modelLevelEnum)) {
            SceneConfigDto sceneConfigDto = sceneService.queryById(param.getSceneCode());
            JSONObject sceneModelParam = sceneConfigDto.getSceneModelParam();
            if (sceneModelParam==null || !sceneModelParam.containsKey(param.getDetectorCode())) {
                return buildSuccessResult(new JSONObject());
            }
            return buildSuccessResult(sceneModelParam.getJSONObject(param.getDetectorCode()));
        } else if (ModelLevelEnum.ENTITY.equals(modelLevelEnum)) {
            String instanceCode = MessageDigestUtil.genSHA256(
                param.getSceneCode()
                    .concat(param.getDetectorCode())
                    .concat(param.getEntityId()));
            InstanceDO instanceDO = instanceService.queryById(instanceCode);
            if (!StringUtils.isEmpty(instanceDO.getModelParam())) {
                return buildSuccessResult(JSONObject.parseObject(instanceDO.getModelParam()));
            }
            return buildSuccessResult(new JSONObject());
        } else {
            throw new IllegalArgumentException("action=queryModel || can not find model level:"+ modelLevelEnum);
        }
    }

    @ApiOperation("更新参数")
    @PostMapping(value = "upsert")
    @ResponseBody
    public AispResult upsertModel(@RequestBody @Validated ModelUpsertParam param){
        ModelLevelEnum modelLevelEnum = ModelLevelEnum.fromValue(param.getLevel());
        if (ModelLevelEnum.SCENE.equals(modelLevelEnum)) {
            SceneUpsertModelParam sceneModelParam = SceneUpsertModelParam.builder()
                .sceneCode(param.getSceneCode())
                .detectorCode(param.getDetectorCode())
                .sceneModelParam(param.getModelParam())
                .build();
            return buildSuccessResult(sceneService.upsertModel(sceneModelParam));
        } else if (ModelLevelEnum.ENTITY.equals(modelLevelEnum)) {
            AnalyseInstanceUpsertModelParam instanceUpsertModelParam = AnalyseInstanceUpsertModelParam.builder()
                .sceneCode(param.getSceneCode())
                .detectorCode(param.getDetectorCode())
                .entityId(param.getEntityId())
                .modelParam(param.getModelParam())
                .build();
            return buildSuccessResult(instanceService.upsertModel(instanceUpsertModelParam));
        } else {
            throw new IllegalArgumentException("action=upsertModel || can not find model level:"+ modelLevelEnum);
        }
    }
}
