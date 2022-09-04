package com.alibaba.tesla.appmanager.server.repository;

import com.alibaba.tesla.appmanager.server.repository.condition.ComponentQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.ComponentDO;

import java.util.List;

public interface ComponentRepository {

    long countByCondition(ComponentQueryCondition condition);

    int deleteByCondition(ComponentQueryCondition condition);

    int insert(ComponentDO record);

    List<ComponentDO> selectByCondition(ComponentQueryCondition condition);

    int updateByCondition(ComponentDO record, ComponentQueryCondition condition);
}