package com.alibaba.sreworks.warehouse.services.domain;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.warehouse.api.domain.DomainService;
import com.alibaba.sreworks.warehouse.common.exception.DomainExistException;
import com.alibaba.sreworks.warehouse.common.exception.DomainRefException;
import com.alibaba.sreworks.warehouse.domain.*;
import com.alibaba.sreworks.warehouse.domain.req.domain.DomainBaseReq;
import com.alibaba.sreworks.warehouse.domain.req.domain.DomainCreateReq;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据域服务类
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/11/19 15:04
 */

@Slf4j
@Service
public class DomainServiceImpl implements DomainService {

    @Autowired
    SwDomainMapper domainMapper;

    @Autowired
    SwEntityMapper entityMapper;

    @Autowired
    SwModelMapper modelMapper;

    @Override
    public JSONObject getDoaminById(Integer id) {
        SwDomain swDomain = domainMapper.selectByPrimaryKey(id);
        return relateModelCount(convertToJSONObject(swDomain));
    }

    @Override
    public JSONObject getDomainByName(String name) {
        SwDomainExample example = new SwDomainExample();
        example.createCriteria().andNameEqualTo(name);
        List<SwDomain> swDomains = domainMapper.selectByExampleWithBLOBs(example);
        if (CollectionUtils.isEmpty(swDomains)) {
            return convertToJSONObject(null);
        }
        return relateModelCount(convertToJSONObject(swDomains.get(0)));
    }

    @Override
    public JSONObject getDomainByAbbreviation(String abbreviation) {
        SwDomainExample example = new SwDomainExample();
        example.createCriteria().andAbbreviationEqualTo(abbreviation);
        List<SwDomain> swDomains = domainMapper.selectByExampleWithBLOBs(example);
        if (CollectionUtils.isEmpty(swDomains)) {
            return convertToJSONObject(null);
        }
        return relateModelCount(convertToJSONObject(swDomains.get(0)));
    }

    @Override
    public List<JSONObject> getDomainBySubject(String subject) {
        SwDomainExample example = new SwDomainExample();
        example.createCriteria().andSubjectEqualTo(subject);
        List<SwDomain> swDomains = domainMapper.selectByExampleWithBLOBs(example);
        return relateModelCounts(convertToJSONObjects(swDomains));
    }

    @Override
    public List<JSONObject> getDomains() {
        List<SwDomain> swDomains = domainMapper.selectByExampleWithBLOBs(new SwDomainExample());
        return relateModelCounts(convertToJSONObjects(swDomains));
    }

    @Override
    public int deleteDomainById(Integer id) throws Exception {
        try {
            domainMapper.deleteByPrimaryKey(id);
        } catch (Exception ex) {
            throw new DomainRefException(String.format("请检查数据域是否被实体或者模型关联, id:%s", id));
        }
        return 0;
    }

    @Override
    public int createDomain(DomainCreateReq req) throws Exception {
        SwDomain swDomain = buildSwDomain(req);
        try {
            domainMapper.insert(swDomain);
        } catch (Exception ex) {
            throw new DomainExistException(String.format("请检查数据域名称或者数据域名称简写是否已存在, 名称:%s, 简写:%s", req.getName(), req.getAbbreviation()));
        }

        return swDomain.getId();
    }

    private JSONObject relateModelCount(JSONObject result) {
        if (CollectionUtils.isEmpty(result)) {
            return result;
        }

        SwModelExample example = new SwModelExample();
        example.createCriteria().andDomainIdEqualTo(result.getInteger("id"));
        long modelCount = modelMapper.countByExample(example);
        result.put("modelCount", modelCount);
        return result;
    }

    private List<JSONObject> relateModelCounts(List<JSONObject> results) {
        if (CollectionUtils.isEmpty(results)) {
            return results;
        }

        Set<Integer> domainIds = results.stream().map(result -> result.getInteger("id")).collect(Collectors.toSet());
        SwModelExample example = new SwModelExample();
        example.createCriteria().andDomainIdIn(new ArrayList<>(domainIds));
        List<SwModel> models = modelMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(models)) {
            return results;
        }

        Map<Integer, Integer> domainModelMap =  new HashMap<>();
        models.forEach(model -> {
            Integer domainId = model.getDomainId();
            if (domainModelMap.containsKey(domainId)) {
                domainModelMap.put(domainId, domainModelMap.get(domainId) + 1);
            } else {
                domainModelMap.put(domainId, 1);
            }
        });

        results.forEach(result -> result.put("modelCount", domainModelMap.getOrDefault(result.getInteger("id"), 0)));
        return results;
    }

    private SwDomain buildSwDomain(DomainBaseReq req) {
        SwDomain swDomain = new SwDomain();
        Date now = new Date();

        swDomain.setGmtCreate(now);
        swDomain.setGmtModified(now);
        swDomain.setName(req.getName());
        swDomain.setAbbreviation(req.getAbbreviation());
        swDomain.setBuildIn(req.getBuildIn());
        swDomain.setSubject(req.getSubject());
        swDomain.setDescription(req.getDescription());
        return swDomain;
    }
}
