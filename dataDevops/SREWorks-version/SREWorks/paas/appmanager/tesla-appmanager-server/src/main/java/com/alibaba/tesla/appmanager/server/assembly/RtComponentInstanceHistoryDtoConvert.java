package com.alibaba.tesla.appmanager.server.assembly;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.tesla.appmanager.common.assembly.BaseDtoConvert;
import com.alibaba.tesla.appmanager.domain.dto.RtComponentInstanceHistoryDTO;
import com.alibaba.tesla.appmanager.server.repository.domain.RtComponentInstanceHistoryDO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * 实时组件实例历史 DTO 转换器
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@Service
public class RtComponentInstanceHistoryDtoConvert extends BaseDtoConvert<RtComponentInstanceHistoryDTO, RtComponentInstanceHistoryDO> {

    public RtComponentInstanceHistoryDtoConvert() {
        super(RtComponentInstanceHistoryDTO.class, RtComponentInstanceHistoryDO.class);
    }

    @Override
    public RtComponentInstanceHistoryDTO to(RtComponentInstanceHistoryDO componentInstanceHistoryDO) {
        if (componentInstanceHistoryDO == null) {
            return null;
        }
        RtComponentInstanceHistoryDTO result = new RtComponentInstanceHistoryDTO();
        result.setGmtCreate(componentInstanceHistoryDO.getGmtCreate());
        result.setGmtModified(componentInstanceHistoryDO.getGmtModified());
        result.setComponentInstanceId(componentInstanceHistoryDO.getComponentInstanceId());
        result.setAppInstanceId(componentInstanceHistoryDO.getAppInstanceId());
        result.setVersion(componentInstanceHistoryDO.getVersion());
        result.setStatus(componentInstanceHistoryDO.getStatus());
        if (StringUtils.isNotEmpty(componentInstanceHistoryDO.getConditions())) {
            result.setConditions(JSONArray.parseArray(componentInstanceHistoryDO.getConditions()));
        }
        return result;
    }
}
