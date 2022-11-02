package com.alibaba.tesla.appmanager.server.repository.mapper;

import com.alibaba.tesla.appmanager.server.repository.domain.RtComponentInstanceDO;
import com.alibaba.tesla.appmanager.server.repository.domain.RtComponentInstanceDOExample;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RtComponentInstanceDOMapper {
    long countByExample(RtComponentInstanceDOExample example);

    int deleteByExample(RtComponentInstanceDOExample example);

    int deleteByPrimaryKey(Long id);

    int insertSelective(RtComponentInstanceDO record);

    List<RtComponentInstanceDO> selectByExample(RtComponentInstanceDOExample example);

    RtComponentInstanceDO selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") RtComponentInstanceDO record, @Param("example") RtComponentInstanceDOExample example);

    List<RtComponentInstanceDO> selectByExampleAndOption(
            @Param("example") RtComponentInstanceDOExample example,
            @Param("key") String key,
            @Param("value") String value);
}