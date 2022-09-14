package com.alibaba.tesla.appmanager.server.repository.mapper;

import com.alibaba.tesla.appmanager.server.repository.domain.AddonInstanceDO;
import com.alibaba.tesla.appmanager.server.repository.domain.AddonInstanceDOExample;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AddonInstanceDOMapper {
    long countByExample(AddonInstanceDOExample example);

    int deleteByExample(AddonInstanceDOExample example);

    int insertSelective(AddonInstanceDO record);

    List<AddonInstanceDO> selectByExample(AddonInstanceDOExample example);

    int updateByExampleSelective(@Param("record") AddonInstanceDO record, @Param("example") AddonInstanceDOExample example);

    int updateByPrimaryKeySelective(AddonInstanceDO record);
}