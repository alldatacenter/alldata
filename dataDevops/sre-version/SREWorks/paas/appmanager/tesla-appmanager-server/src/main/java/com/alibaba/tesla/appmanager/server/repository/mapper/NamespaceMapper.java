package com.alibaba.tesla.appmanager.server.repository.mapper;

import com.alibaba.tesla.appmanager.server.repository.domain.NamespaceDO;
import com.alibaba.tesla.appmanager.server.repository.domain.NamespaceDOExample;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NamespaceMapper {
    long countByExample(NamespaceDOExample example);

    int deleteByExample(NamespaceDOExample example);

    int insertSelective(NamespaceDO record);

    List<NamespaceDO> selectByExample(NamespaceDOExample example);

    int updateByExampleSelective(@Param("record") NamespaceDO record, @Param("example") NamespaceDOExample example);

    int updateByExample(@Param("record") NamespaceDO record, @Param("example") NamespaceDOExample example);
}