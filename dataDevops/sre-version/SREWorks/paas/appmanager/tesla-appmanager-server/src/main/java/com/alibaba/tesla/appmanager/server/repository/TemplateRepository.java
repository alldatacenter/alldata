package com.alibaba.tesla.appmanager.server.repository;

import com.alibaba.tesla.appmanager.server.repository.condition.TemplateQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.TemplateDO;

import java.util.List;

public interface TemplateRepository {
    long countByCondition(TemplateQueryCondition condition);

    int deleteByCondition(TemplateQueryCondition condition);

    int insert(TemplateDO record);

    List<TemplateDO> selectByCondition(TemplateQueryCondition condition);

    int updateByCondition(TemplateDO record, TemplateQueryCondition condition);
}
