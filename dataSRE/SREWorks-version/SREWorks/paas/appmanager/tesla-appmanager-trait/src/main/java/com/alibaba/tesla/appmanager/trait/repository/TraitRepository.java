package com.alibaba.tesla.appmanager.trait.repository;

import com.alibaba.tesla.appmanager.trait.repository.condition.TraitQueryCondition;
import com.alibaba.tesla.appmanager.trait.repository.domain.TraitDO;
import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TraitRepository {
    long countByCondition(TraitQueryCondition condition);

    int deleteByCondition(TraitQueryCondition condition);

    int deleteByPrimaryKey(Long id);

    int insert(TraitDO record);

    List<TraitDO> selectByCondition(TraitQueryCondition condition);

    TraitDO selectByPrimaryKey(Long id);

    int updateByCondition(@Param("record") TraitDO record, @Param("condition") TraitQueryCondition condition);

    int updateByPrimaryKey(TraitDO record);
}