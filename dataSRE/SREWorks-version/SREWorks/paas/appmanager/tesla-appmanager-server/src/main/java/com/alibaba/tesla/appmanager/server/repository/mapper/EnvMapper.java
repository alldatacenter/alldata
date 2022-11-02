package com.alibaba.tesla.appmanager.server.repository.mapper;

import com.alibaba.tesla.appmanager.server.repository.domain.EnvDO;
import com.alibaba.tesla.appmanager.server.repository.domain.EnvDOExample;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EnvMapper {
    long countByExample(EnvDOExample example);

    int deleteByExample(EnvDOExample example);

    int insertSelective(EnvDO record);

    List<EnvDO> selectByExample(EnvDOExample example);

    int updateByExampleSelective(@Param("record") EnvDO record, @Param("example") EnvDOExample example);

    int updateByExample(@Param("record") EnvDO record, @Param("example") EnvDOExample example);
}