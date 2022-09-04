package com.alibaba.sreworks.dataset.domain.primary;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.RowBounds;

import java.util.List;

public interface InterfaceConfigMapper {
    long countByExample(InterfaceConfigExample example);

    int deleteByExample(InterfaceConfigExample example);

    int deleteByPrimaryKey(Integer id);

    int insert(InterfaceConfig record);

    int insertSelective(InterfaceConfig record);

    List<InterfaceConfig> selectByExampleWithBLOBsWithRowbounds(InterfaceConfigExample example, RowBounds rowBounds);

    List<InterfaceConfig> selectByExampleWithBLOBs(InterfaceConfigExample example);

    List<InterfaceConfig> selectByExampleWithRowbounds(InterfaceConfigExample example, RowBounds rowBounds);

    List<InterfaceConfig> selectByExample(InterfaceConfigExample example);

    InterfaceConfig selectByPrimaryKey(Integer id);

    int updateByExampleSelective(@Param("record") InterfaceConfig record, @Param("example") InterfaceConfigExample example);

    int updateByExampleWithBLOBs(@Param("record") InterfaceConfig record, @Param("example") InterfaceConfigExample example);

    int updateByExample(@Param("record") InterfaceConfig record, @Param("example") InterfaceConfigExample example);

    int updateByPrimaryKeySelective(InterfaceConfig record);

    int updateByPrimaryKeyWithBLOBs(InterfaceConfig record);

    int updateByPrimaryKey(InterfaceConfig record);
}