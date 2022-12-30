package com.alibaba.tesla.appmanager.server.repository.mapper;

import com.alibaba.tesla.appmanager.server.repository.domain.ClusterDO;
import com.alibaba.tesla.appmanager.server.repository.domain.ClusterDOExample;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ClusterDOMapper {
    long countByExample(ClusterDOExample example);

    int deleteByExample(ClusterDOExample example);

    int deleteByPrimaryKey(Long id);

    int insert(ClusterDO record);

    int insertSelective(ClusterDO record);

    List<ClusterDO> selectByExample(ClusterDOExample example);

    ClusterDO selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") ClusterDO record, @Param("example") ClusterDOExample example);

    int updateByExample(@Param("record") ClusterDO record, @Param("example") ClusterDOExample example);

    int updateByPrimaryKeySelective(ClusterDO record);

    int updateByPrimaryKey(ClusterDO record);
}