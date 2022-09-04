package com.alibaba.tesla.appmanager.server.repository.mapper;

import com.alibaba.tesla.appmanager.server.repository.domain.RtAppInstanceDO;
import com.alibaba.tesla.appmanager.server.repository.domain.RtAppInstanceDOExample;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RtAppInstanceDOMapper {
    long countByExample(RtAppInstanceDOExample example);

    int deleteByExample(RtAppInstanceDOExample example);

    int deleteByPrimaryKey(Long id);

    int insertSelective(RtAppInstanceDO record);

    List<RtAppInstanceDO> selectByExample(RtAppInstanceDOExample example);

    List<RtAppInstanceDO> selectByExampleAndOption(
            @Param("example") RtAppInstanceDOExample example,
            @Param("key") String key,
            @Param("value") String value);

    RtAppInstanceDO selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") RtAppInstanceDO record, @Param("example") RtAppInstanceDOExample example);
}