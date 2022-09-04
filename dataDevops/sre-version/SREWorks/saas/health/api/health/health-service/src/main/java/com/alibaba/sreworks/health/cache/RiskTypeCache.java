package com.alibaba.sreworks.health.cache;

import com.alibaba.sreworks.health.domain.RiskType;
import com.alibaba.sreworks.health.domain.RiskTypeExample;
import com.alibaba.sreworks.health.domain.RiskTypeMapper;
import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 风险类型cache
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/11/03 16:30
 */
@Getter
public class RiskTypeCache extends HealthDomainCache<RiskTypeMapper> {

    Map<Integer, RiskType> riskTypeMap = new HashMap<>();

    @Override
    public void reconstructCache(RiskTypeMapper reconstructor) {
        List<RiskType> riskTypes = reconstructor.selectByExample(new RiskTypeExample());
        riskTypeMap = riskTypes.stream().collect(Collectors.toMap(RiskType::getId, riskType -> riskType));
    }
}
