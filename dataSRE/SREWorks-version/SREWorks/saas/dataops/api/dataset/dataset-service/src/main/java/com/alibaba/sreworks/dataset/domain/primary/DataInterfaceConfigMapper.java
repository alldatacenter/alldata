package com.alibaba.sreworks.dataset.domain.primary;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.RowBounds;

import java.util.List;

public interface DataInterfaceConfigMapper {
    long countByExample(DataInterfaceConfigExample example);

    int deleteByExample(DataInterfaceConfigExample example);

    int deleteByPrimaryKey(Integer id);

    int insert(DataInterfaceConfig record);

    int insertSelective(DataInterfaceConfig record);

    List<DataInterfaceConfig> selectByExampleWithBLOBsWithRowbounds(DataInterfaceConfigExample example, RowBounds rowBounds);

    List<DataInterfaceConfig> selectByExampleWithBLOBs(DataInterfaceConfigExample example);

    List<DataInterfaceConfig> selectByExampleWithRowbounds(DataInterfaceConfigExample example, RowBounds rowBounds);

    List<DataInterfaceConfig> selectByExample(DataInterfaceConfigExample example);

    DataInterfaceConfig selectByPrimaryKey(Integer id);

    int updateByExampleSelective(@Param("record") DataInterfaceConfig record, @Param("example") DataInterfaceConfigExample example);

    int updateByExampleWithBLOBs(@Param("record") DataInterfaceConfig record, @Param("example") DataInterfaceConfigExample example);

    int updateByExample(@Param("record") DataInterfaceConfig record, @Param("example") DataInterfaceConfigExample example);

    int updateByPrimaryKeySelective(DataInterfaceConfig record);

    int updateByPrimaryKeyWithBLOBs(DataInterfaceConfig record);

    int updateByPrimaryKey(DataInterfaceConfig record);
}