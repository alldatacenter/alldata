package com.alibaba.tesla.tkgone.backend.domain;

import java.util.List;

import com.alibaba.tesla.tkgone.backend.domain.entity.BackendIndexDO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.RowBounds;

public interface BackendIndexMapper {
    long countByExample(BackendIndexExample example);

    int deleteByExample(BackendIndexExample example);

    int deleteByPrimaryKey(Long id);

    int insert(BackendIndexDO record);

    int insertSelective(BackendIndexDO record);

    List<BackendIndexDO> selectByExampleWithRowbounds(BackendIndexExample example, RowBounds rowBounds);

    List<BackendIndexDO> selectByExample(BackendIndexExample example);

    BackendIndexDO selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") BackendIndexDO record, @Param("example") BackendIndexExample example);

    int updateByExample(@Param("record") BackendIndexDO record, @Param("example") BackendIndexExample example);

    int updateByPrimaryKeySelective(BackendIndexDO record);

    int updateByPrimaryKey(BackendIndexDO record);
}