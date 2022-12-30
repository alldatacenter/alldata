package com.alibaba.sreworks.health.cache;

import com.alibaba.sreworks.health.domain.IncidentType;
import com.alibaba.sreworks.health.domain.IncidentTypeExample;
import com.alibaba.sreworks.health.domain.IncidentTypeMapper;
import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 异常类型cache
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/11/03 16:30
 */
@Getter
public class IncidentTypeCache extends HealthDomainCache<IncidentTypeMapper> {

    Map<Integer, IncidentType> incidentTypeMap = new HashMap<>();

    @Override
    public void reconstructCache(IncidentTypeMapper reconstructor) {
        List<IncidentType> incidentTypes = reconstructor.selectByExample(new IncidentTypeExample());
        incidentTypeMap = incidentTypes.stream().collect(Collectors.toMap(IncidentType::getId, incidentType -> incidentType));
    }
}
