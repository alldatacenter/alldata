package com.alibaba.tdata.aisp.server.common.convert;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tdata.aisp.server.common.dto.SceneConfigDto;
import com.alibaba.tdata.aisp.server.common.dto.SolutionConfigDto;
import com.alibaba.tdata.aisp.server.repository.domain.SceneConfigDO;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * @ClassName: SceneConfigConvert
 * @Author: dyj
 * @DATE: 2021-11-18
 * @Description:
 **/
@Slf4j
@Component
public class SceneConfigConvert {
    @Autowired
    private UserInfoConvert userInfoConvert;

    public SceneConfigDto from(SceneConfigDO configDO){
        SceneConfigDto configDto = SceneConfigDto.builder().sceneCode(configDO.getSceneCode())
            .sceneName(configDO.getSceneName())
            .productList(StringUtils.isEmpty(configDO.getProductName())?null:configDO.getProductName().split(","))
            .comment(configDO.getComment())
            .gmtCreate(configDO.getGmtCreate().getTime())
            .gmtModified(configDO.getGmtModified().getTime())
            .ownerInfoList(
                userInfoConvert.froms(
                    StringUtils.isEmpty(configDO.getOwners())?null:Arrays.asList(configDO.getOwners().split(","))
                )
            )
            .sceneModelParam(JSONObject.parseObject(configDO.getSceneModelParam()))
            .detectorBinder(configDO.getDetectorBinder())
            .build();
        return configDto;
    }

    public List<SceneConfigDto> froms(List<SceneConfigDO> configDOList) {
        return configDOList.stream().map(this::from).collect(Collectors.toList());
    }
}
