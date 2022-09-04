package com.alibaba.sreworks.health.services.instance.risk;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.health.api.risk.RiskTypeService;
import com.alibaba.sreworks.health.common.constant.Constant;
import com.alibaba.sreworks.health.common.exception.RiskTypeNotExistException;
import com.alibaba.sreworks.health.domain.*;
import com.alibaba.sreworks.health.domain.req.risk.RiskTypeBaseReq;
import com.alibaba.sreworks.health.domain.req.risk.RiskTypeCreateReq;
import com.alibaba.sreworks.health.domain.req.risk.RiskTypeUpdateReq;
import com.alibaba.sreworks.health.services.cache.HealthDomainCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 风险类型service
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/11/05 11:47
 */
@Service
@Slf4j
public class RiskTypeServiceImpl implements RiskTypeService {
    @Autowired
    RiskTypeMapper riskTypeMapper;

    @Autowired
    CommonDefinitionMapper definitionMapper;

    @Autowired
    HealthDomainCacheService domainCacheService;

    @Override
    public JSONObject getRiskTypeById(Integer id) {
        return convertToJSONObject(riskTypeMapper.selectByPrimaryKey(id));
    }

    @Override
    public List<JSONObject> getRiskTypes() {
        return convertToJSONObjects(riskTypeMapper.selectByExample(new RiskTypeExample()));
    }

    @Override
    public boolean existRiskType(Integer id) {
        return riskTypeMapper.selectByPrimaryKey(id) != null;
    }

    @Override
    public boolean notExistRiskType(Integer id) {
        return !existRiskType(id);
    }

    @Override
    public int addRiskType(RiskTypeCreateReq req) {
        req.setLastModifier(req.getCreator());
        RiskType riskType = buildRiskType(req);
        int result = riskTypeMapper.insert(riskType);

        domainCacheService.expireKey(Constant.CACHE_RISK_TYPE_KEY);

        return result;
    }

    @Override
    public int updateRiskType(RiskTypeUpdateReq req) throws Exception {
        if (notExistRiskType(req.getId())) {
            throw new RiskTypeNotExistException(String.format("更新风险类型[id:%s]不存在", req.getId()));
        }

        RiskType riskType = buildRiskType(req);
        riskType.setId(req.getId());
        riskType.setGmtCreate(null);
        int result =riskTypeMapper.updateByPrimaryKeySelective(riskType);

        domainCacheService.expireKey(Constant.CACHE_RISK_TYPE_KEY);

        return result;
    }

    @Override
    public int deleteRiskType(Integer id) throws Exception {
        RiskType riskType = riskTypeMapper.selectByPrimaryKey(id);
        if (riskType == null) {
            return 0;
        }

        CommonDefinitionExample definitionExample = new CommonDefinitionExample();
        definitionExample.createCriteria().andCategoryEqualTo(Constant.RISK);
//        List<CommonDefinition> commonDefinitions = definitionMapper.selectByExample(definitionExample);

//        Optional<CommonDefinition> optDefinition = commonDefinitions.stream().filter(definition -> {
//            RiskExConfig exConfig = JSONObject.parseObject(definition.getExConfig(), RiskExConfig.class);
//            return exConfig.getTypeId().equals(id);
//        }).findAny();
//        if (optDefinition.isPresent()) {
//            CommonDefinition usedDefinition = optDefinition.get();
//            throw new RiskTypeDeleteException(String.format("风险类型[id:%s]删除失败, 被风险定义[id:%s, name:%s]引用", id, usedDefinition.getId(), usedDefinition.getName()));
//        }

        int result = riskTypeMapper.deleteByPrimaryKey(id);

        domainCacheService.expireKey(Constant.CACHE_RISK_TYPE_KEY);

        return result;
    }

    private RiskType buildRiskType(RiskTypeBaseReq req) {
        RiskType riskType = new RiskType();
        Date now = new Date();
        riskType.setGmtCreate(now);
        riskType.setGmtModified(now);
        riskType.setLabel(req.getLabel());
        riskType.setName(req.getName());
        riskType.setCreator(req.getCreator());
        riskType.setDescription(req.getDescription());
        riskType.setLastModifier(req.getLastModifier());

        return riskType;
    }
}
