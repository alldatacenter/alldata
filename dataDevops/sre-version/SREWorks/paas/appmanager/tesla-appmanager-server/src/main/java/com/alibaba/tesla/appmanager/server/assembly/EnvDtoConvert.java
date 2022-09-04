package com.alibaba.tesla.appmanager.server.assembly;

import com.alibaba.tesla.appmanager.common.assembly.BaseDtoConvert;
import com.alibaba.tesla.appmanager.common.util.JsonUtil;
import com.alibaba.tesla.appmanager.domain.dto.StageDTO;
import com.alibaba.tesla.appmanager.server.repository.domain.EnvDO;
import org.springframework.stereotype.Component;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Component
public class EnvDtoConvert extends BaseDtoConvert<StageDTO, EnvDO> {

    public EnvDtoConvert() {
        super(StageDTO.class, EnvDO.class);
    }

    @Override
    public StageDTO to(EnvDO envDO) {
        if (envDO == null) {
            return null;
        }
        return StageDTO.builder()
                .namespaceId(envDO.getNamespaceId())
                .stageId(envDO.getEnvId())
                .stageName(envDO.getEnvName())
                .stageCreator(envDO.getEnvCreator())
                .stageModifier(envDO.getEnvModifier())
                .stageExt(JsonUtil.toJson(envDO.getEnvExt()))
                .gmtCreate(envDO.getGmtCreate())
                .gmtModified(envDO.getGmtModified())
                .build();
    }

    @Override
    public EnvDO from(StageDTO stageDTO) {
        if (stageDTO == null) {
            return null;
        }
        EnvDO result = new EnvDO();
        result.setNamespaceId(stageDTO.getNamespaceId());
        result.setEnvId(stageDTO.getStageId());
        result.setEnvName(stageDTO.getStageName());
        result.setEnvCreator(stageDTO.getStageCreator());
        result.setEnvModifier(stageDTO.getStageModifier());
        result.setEnvExt(JsonUtil.toJsonString(stageDTO.getStageExt()));
        result.setGmtCreate(stageDTO.getGmtCreate());
        result.setGmtModified(stageDTO.getGmtModified());
        return result;
    }
}
