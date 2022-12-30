package com.alibaba.tesla.appmanager.server.assembly;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.tesla.appmanager.common.assembly.BaseDtoConvert;
import com.alibaba.tesla.appmanager.domain.dto.RtComponentInstanceDTO;
import com.alibaba.tesla.appmanager.server.repository.domain.RtComponentInstanceDO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * 实时组件实例 DTO 转换器
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@Service
public class RtComponentInstanceDtoConvert extends BaseDtoConvert<RtComponentInstanceDTO, RtComponentInstanceDO> {

    public RtComponentInstanceDtoConvert() {
        super(RtComponentInstanceDTO.class, RtComponentInstanceDO.class);
    }

    @Override
    public RtComponentInstanceDTO to(RtComponentInstanceDO componentInstanceDO) {
        if (componentInstanceDO == null) {
            return null;
        }
        RtComponentInstanceDTO result = new RtComponentInstanceDTO();
        result.setGmtCreate(componentInstanceDO.getGmtCreate());
        result.setGmtModified(componentInstanceDO.getGmtModified());
        result.setComponentInstanceId(componentInstanceDO.getComponentInstanceId());
        result.setAppInstanceId(componentInstanceDO.getAppInstanceId());
        result.setAppId(componentInstanceDO.getAppId());
        result.setComponentType(componentInstanceDO.getComponentType());
        result.setComponentName(componentInstanceDO.getComponentName());
        result.setClusterId(componentInstanceDO.getClusterId());
        result.setNamespaceId(componentInstanceDO.getNamespaceId());
        result.setStageId(componentInstanceDO.getStageId());
        result.setVersion(componentInstanceDO.getVersion());
        result.setStatus(componentInstanceDO.getStatus());
        result.setWatchKind(componentInstanceDO.getWatchKind());
        result.setTimes(componentInstanceDO.getTimes());
        if (StringUtils.isNotEmpty(componentInstanceDO.getConditions())) {
            result.setConditions(JSONArray.parseArray(componentInstanceDO.getConditions()));
        }
        return result;
    }
}
