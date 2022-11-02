package com.alibaba.tesla.appmanager.server.repository.impl;

import com.alibaba.tesla.appmanager.common.util.DateUtil;
import com.alibaba.tesla.appmanager.server.repository.TemplateRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.TemplateQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.TemplateDO;
import com.alibaba.tesla.appmanager.server.repository.domain.TemplateDOExample;
import com.alibaba.tesla.appmanager.server.repository.mapper.TemplateMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class TemplateRepositoryImpl implements TemplateRepository {

    @Autowired
    private TemplateMapper templateMapper;

    @Override
    public long countByCondition(TemplateQueryCondition condition) {
        return templateMapper.countByExample(buildExample(condition));
    }

    @Override
    public int deleteByCondition(TemplateQueryCondition condition) {
        return templateMapper.deleteByExample(buildExample(condition));
    }

    @Override
    public int insert(TemplateDO record) {
        return templateMapper.insertSelective(insertDate(record));
    }

    @Override
    public List<TemplateDO> selectByCondition(TemplateQueryCondition condition) {
        return templateMapper.selectByExample(buildExample(condition));
    }

    @Override
    public int updateByCondition(TemplateDO record, TemplateQueryCondition condition) {
        return templateMapper.updateByExampleSelective(updateDate(record), buildExample(condition));
    }

    private TemplateDOExample buildExample(TemplateQueryCondition condition) {
        TemplateDOExample example = new TemplateDOExample();
        TemplateDOExample.Criteria criteria = example.createCriteria();
        if (StringUtils.isNotBlank(condition.getTemplateId())) {
            criteria.andTemplateIdEqualTo(condition.getTemplateId());
        }
        if (StringUtils.isNotBlank(condition.getTemplateName())) {
            criteria.andTemplateNameEqualTo(condition.getTemplateName());
        }
        if (StringUtils.isNotBlank(condition.getTemplateType())) {
            criteria.andTemplateTypeEqualTo(condition.getTemplateType());
        }
        return example;
    }

    private TemplateDO insertDate(TemplateDO record) {
        Date now = DateUtil.now();
        record.setGmtCreate(now);
        record.setGmtModified(now);
        return record;
    }

    private TemplateDO updateDate(TemplateDO record) {
        Date now = DateUtil.now();
        record.setGmtModified(now);
        return record;
    }
}
