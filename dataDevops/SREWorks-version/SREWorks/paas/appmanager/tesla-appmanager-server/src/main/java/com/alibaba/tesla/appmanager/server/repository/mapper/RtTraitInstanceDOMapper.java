package com.alibaba.tesla.appmanager.server.repository.mapper;

import com.alibaba.tesla.appmanager.server.repository.domain.RtTraitInstanceDO;
import com.alibaba.tesla.appmanager.server.repository.domain.RtTraitInstanceDOExample;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RtTraitInstanceDOMapper {
    long countByExample(RtTraitInstanceDOExample example);

    int deleteByExample(RtTraitInstanceDOExample example);

    int deleteByPrimaryKey(Long id);

    int insertSelective(RtTraitInstanceDO record);

    List<RtTraitInstanceDO> selectByExample(RtTraitInstanceDOExample example);

    RtTraitInstanceDO selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") RtTraitInstanceDO record, @Param("example") RtTraitInstanceDOExample example);
}