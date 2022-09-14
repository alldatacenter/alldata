package com.alibaba.tesla.appmanager.server.repository;

import com.alibaba.tesla.appmanager.server.repository.condition.EnvQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.EnvDO;

import java.util.Arrays;
import java.util.List;

public interface EnvRepository {
    List<String> PRODUCTION = Arrays.asList("pre", "prod");

    long countByCondition(EnvQueryCondition condition);

    int deleteByCondition(EnvQueryCondition condition);

    int insert(EnvDO record);

    List<EnvDO> selectByCondition(EnvQueryCondition condition);

    int updateByCondition(EnvDO record, EnvQueryCondition condition);
}