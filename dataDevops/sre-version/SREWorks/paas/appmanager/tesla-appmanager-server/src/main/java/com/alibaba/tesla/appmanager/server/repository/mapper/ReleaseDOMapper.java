package com.alibaba.tesla.appmanager.server.repository.mapper;

import com.alibaba.tesla.appmanager.server.repository.domain.ReleaseDO;
import com.alibaba.tesla.appmanager.server.repository.domain.ReleaseDOExample;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReleaseDOMapper {
    long countByExample(ReleaseDOExample example);

    int deleteByExample(ReleaseDOExample example);

    int deleteByPrimaryKey(Long id);

    int insert(ReleaseDO record);

    int insertSelective(ReleaseDO record);

    List<ReleaseDO> selectByExample(ReleaseDOExample example);

    ReleaseDO selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") ReleaseDO record, @Param("example") ReleaseDOExample example);

    int updateByExample(@Param("record") ReleaseDO record, @Param("example") ReleaseDOExample example);

    int updateByPrimaryKeySelective(ReleaseDO record);

    int updateByPrimaryKey(ReleaseDO record);
}