package com.alibaba.tesla.appmanager.meta.helm.repository;

import com.alibaba.tesla.appmanager.meta.helm.repository.condition.HelmMetaQueryCondition;
import com.alibaba.tesla.appmanager.meta.helm.repository.domain.HelmMetaDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface HelmMetaRepository {

    long countByCondition(HelmMetaQueryCondition condition);

    int deleteByCondition(HelmMetaQueryCondition condition);

    int deleteByPrimaryKey(Long id);

    int insert(HelmMetaDO record);

    List<HelmMetaDO> selectByCondition(HelmMetaQueryCondition condition);

    HelmMetaDO selectByPrimaryKey(Long id);

    int updateByCondition(@Param("record") HelmMetaDO record, @Param("condition") HelmMetaQueryCondition condition);

    int updateByPrimaryKey(HelmMetaDO record);
}