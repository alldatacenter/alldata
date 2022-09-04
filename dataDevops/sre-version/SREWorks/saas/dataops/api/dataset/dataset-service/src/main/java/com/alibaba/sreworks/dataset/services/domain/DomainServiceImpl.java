package com.alibaba.sreworks.dataset.services.domain;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.dataset.api.domain.DomainService;
import com.alibaba.sreworks.dataset.api.model.ModelConfigService;
import com.alibaba.sreworks.dataset.common.exception.DomainNotExistException;
import com.alibaba.sreworks.dataset.common.exception.ModelExistException;
import com.alibaba.sreworks.dataset.common.exception.ParamException;
import com.alibaba.sreworks.dataset.common.exception.SubjectNotExistException;
import com.alibaba.sreworks.dataset.domain.primary.*;
import com.alibaba.sreworks.dataset.domain.req.domain.DataDomainBaseReq;
import com.alibaba.sreworks.dataset.domain.req.domain.DataDomainCreateReq;
import com.alibaba.sreworks.dataset.domain.req.domain.DataDomainUpdateReq;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 数据主题
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/14 14:19
 */
@Slf4j
@Service
public class DomainServiceImpl implements DomainService {

    @Autowired
    DataDomainMapper dataDomainMapper;

    @Autowired
    DataSubjectMapper dataSubjectMapper;

    @Autowired
    ModelConfigService modelConfigService;

    @Override
    public JSONObject getDomainById(Integer domainId) {
        DataDomain dataDomain = dataDomainMapper.selectByPrimaryKey(domainId);
        JSONObject result = convertToJSONObject(dataDomain);
        if (result.isEmpty()) {
            return result;
        }

        DataSubject dataSubject = dataSubjectMapper.selectByPrimaryKey(dataDomain.getSubjectId());
        if (dataSubject != null) {
            result.put("subjectName", dataSubject.getName());
        }

        Long modelCount = modelConfigService.countModelsByDomain(domainId);
        result.put("modelCount", modelCount);

        return result;
    }

    @Override
    public List<JSONObject> getDomainsBySubject(Integer subjectId) {
        DataDomainExample example = new DataDomainExample();
        example.createCriteria().andSubjectIdEqualTo(subjectId);
        List<DataDomain> dataDomains = dataDomainMapper.selectByExampleWithBLOBs(example);
        List<JSONObject> results = convertToJSONObjects(dataDomains);
        if (results.isEmpty()) {
            return results;
        }

        DataSubject dataSubject = dataSubjectMapper.selectByPrimaryKey(subjectId);

        List<Integer> dataDomainIds = new ArrayList<>();
        dataDomains.forEach(dataDomain -> dataDomainIds.add(dataDomain.getId()));
        Map<Integer, Long> modelCounts = modelConfigService.countModelsByDomains(dataDomainIds);

        results.forEach(result -> {
            result.put("subjectName", dataSubject.getName());
            result.put("modelCount", modelCounts.getOrDefault(result.getInteger("id"), 0L));
        });

        return results;
    }

    @Override
    public List<JSONObject> getDomains(Integer subjectId) {
        DataDomainExample example = new DataDomainExample();
        if (subjectId != null) {
            example.createCriteria().andSubjectIdEqualTo(subjectId);
        }

        List<DataDomain> dataDomains = dataDomainMapper.selectByExampleWithBLOBs(example);
        List<JSONObject> results = convertToJSONObjects(dataDomains);
        if (results.isEmpty()) {
            return results;
        }

        List<Integer> dataSubjectIds = new ArrayList<>();
        dataDomains.forEach(dataDomain -> dataSubjectIds.add(dataDomain.getSubjectId()));

        DataSubjectExample subjectExample = new DataSubjectExample();
        subjectExample.createCriteria().andIdIn(dataSubjectIds);
        List<DataSubject> dataSubjects = dataSubjectMapper.selectByExample(subjectExample);
        Map<Integer, String> subjectNames = new HashMap<>();
        dataSubjects.forEach(dataSubject -> subjectNames.put(dataSubject.getId(), dataSubject.getName()));

        Map<Integer, Long> modelCounts = modelConfigService.countModels();
        results.forEach(result -> {
            result.put("subjectName", subjectNames.getOrDefault(result.getInteger("subjectId"), ""));
            result.put("modelCount", modelCounts.getOrDefault(result.getInteger("id"), 0L));
        });

        return results;
    }

    @Override
    public Long countDomainsBySubject(Integer subjectId) {
        DataDomainExample example = new DataDomainExample();
        example.createCriteria().andSubjectIdEqualTo(subjectId);
        return dataDomainMapper.countByExample(example);
    }

    @Override
    public Map<Integer, Long> countDomains() {
        DataDomainExample example = new DataDomainExample();
        List<DataDomain> dataDomains = dataDomainMapper.selectByExample(example);
        Map<Integer, Long> result = new HashMap<>();
        dataDomains.forEach(dataDomain -> {
                Integer subjectId = dataDomain.getSubjectId();
                if (result.containsKey(subjectId)) {
                    result.put(subjectId, result.get(subjectId) + 1);
                } else {
                    result.put(subjectId, 1L);
                }
        });
        return result;
    }

    @Override
    public boolean existDomain(Integer domainId) {
        return dataDomainMapper.selectByPrimaryKey(domainId) != null;
    }

    @Override
    public boolean buildInDomain(Integer domainId) {
        DataDomain dataDomain = dataDomainMapper.selectByPrimaryKey(domainId);
        return dataDomain != null && dataDomain.getBuildIn();
    }

    @Override
    public int addDomain(DataDomainCreateReq req) throws Exception {
        DataDomain dataDomain = buildDataDomain(req);
        return dataDomainMapper.insert(dataDomain);
    }

    @Override
    public int updateDomain(DataDomainUpdateReq req) throws Exception {
        if (!existDomain(req.getId())) {
            throw new DomainNotExistException(String.format("数据域不存在,请检查参数,数据域ID:%s", req.getId()));
        }

        if (buildInDomain(req.getId())) {
            throw new ParamException("内置数据域不允许修改");
        }

        DataDomain dataDomain = buildDataDomain(req);
        dataDomain.setId(req.getId());
        dataDomain.setGmtCreate(null);
        return dataDomainMapper.updateByPrimaryKeySelective(dataDomain);
    }

    @Override
    public int deleteDomainById(Integer domainId) throws Exception {
        if (buildInDomain(domainId)) {
            throw new ParamException("内置数据域不允许修改");
        }

        Long modelCount = modelConfigService.countModelsByDomain(domainId);
        if (modelCount > 0) {
            throw new ModelExistException("数据域下存在模型, 需要先清理模型");
        }
        return dataDomainMapper.deleteByPrimaryKey(domainId);
    }

    private DataDomain buildDataDomain(DataDomainBaseReq req) throws Exception {
        DataSubject dataSubject = dataSubjectMapper.selectByPrimaryKey(req.getSubjectId());
        if (dataSubject == null) {
            throw new SubjectNotExistException("数据主题不存在");
        }

        DataDomain dataDomain = new DataDomain();
        Date now = new Date();
        dataDomain.setGmtCreate(now);
        dataDomain.setGmtModified(now);
        dataDomain.setName(req.getName());
        dataDomain.setAbbreviation(req.getAbbreviation());
        dataDomain.setSubjectId(req.getSubjectId());
        dataDomain.setBuildIn(false);
        dataDomain.setDescription(req.getDescription());

        return dataDomain;
    }
}
