package com.alibaba.tesla.tkgone.backend.domain;

import java.util.List;

import com.alibaba.tesla.tkgone.backend.domain.entity.BackendDO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.RowBounds;

public interface BackendMapper {
    long countByExample(BackendExample example);

    int deleteByExample(BackendExample example);

    int deleteByPrimaryKey(Long id);

    int insert(BackendDO record);

    int insertSelective(BackendDO record);

    List<BackendDO> selectByExampleWithRowbounds(BackendExample example, RowBounds rowBounds);

    List<BackendDO> selectByExample(BackendExample example);

    BackendDO selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") BackendDO record, @Param("example") BackendExample example);

    int updateByExample(@Param("record") BackendDO record, @Param("example") BackendExample example);

    int updateByPrimaryKeySelective(BackendDO record);

    int updateByPrimaryKey(BackendDO record);
}