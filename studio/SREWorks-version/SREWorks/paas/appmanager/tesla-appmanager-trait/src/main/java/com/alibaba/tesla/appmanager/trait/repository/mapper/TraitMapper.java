package com.alibaba.tesla.appmanager.trait.repository.mapper;

import java.util.List;

import com.alibaba.tesla.appmanager.trait.repository.domain.TraitDO;
import com.alibaba.tesla.appmanager.trait.repository.domain.TraitDOExample;

import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.RowBounds;

public interface TraitMapper {
    long countByExample(TraitDOExample example);

    int deleteByExample(TraitDOExample example);

    int deleteByPrimaryKey(Long id);

    int insert(TraitDO record);

    int insertSelective(TraitDO record);

    List<TraitDO> selectByExampleWithBLOBsWithRowbounds(TraitDOExample example, RowBounds rowBounds);

    Page<TraitDO> selectByExampleWithBLOBs(TraitDOExample example);

    List<TraitDO> selectByExampleWithRowbounds(TraitDOExample example, RowBounds rowBounds);

    Page<TraitDO> selectByExample(TraitDOExample example);

    TraitDO selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") TraitDO record, @Param("example") TraitDOExample example);

    int updateByExampleWithBLOBs(@Param("record") TraitDO record, @Param("example") TraitDOExample example);

    int updateByExample(@Param("record") TraitDO record, @Param("example") TraitDOExample example);

    int updateByPrimaryKeySelective(TraitDO record);

    int updateByPrimaryKeyWithBLOBs(TraitDO record);

    int updateByPrimaryKey(TraitDO record);
}