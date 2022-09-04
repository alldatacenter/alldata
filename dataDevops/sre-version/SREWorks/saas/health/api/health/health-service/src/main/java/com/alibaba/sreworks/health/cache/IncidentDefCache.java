package com.alibaba.sreworks.health.cache;

import com.alibaba.sreworks.health.common.constant.Constant;
import com.alibaba.sreworks.health.domain.CommonDefinition;
import com.alibaba.sreworks.health.domain.CommonDefinitionExample;
import com.alibaba.sreworks.health.domain.CommonDefinitionMapper;
import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 异常定义cache
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/11/03 16:30
 */
@Getter
public class IncidentDefCache extends HealthDomainCache<CommonDefinitionMapper> {

    Map<Integer, CommonDefinition> commonDefinitionMap = new HashMap<>();

    @Override
    public void reconstructCache(CommonDefinitionMapper reconstructor) {
        CommonDefinitionExample example = new CommonDefinitionExample();
        example.createCriteria().andCategoryEqualTo(Constant.INCIDENT);
        List<CommonDefinition> commonDefinitions = reconstructor.selectByExample(example);
        commonDefinitionMap = commonDefinitions.stream().collect(Collectors.toMap(CommonDefinition::getId, commonDefinition -> commonDefinition));
    }
}
