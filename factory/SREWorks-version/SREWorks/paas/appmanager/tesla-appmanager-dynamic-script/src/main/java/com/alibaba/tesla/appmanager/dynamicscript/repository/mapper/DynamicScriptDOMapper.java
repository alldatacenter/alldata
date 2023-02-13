package com.alibaba.tesla.appmanager.dynamicscript.repository.mapper;

import com.alibaba.tesla.appmanager.dynamicscript.repository.domain.DynamicScriptDO;
import com.alibaba.tesla.appmanager.dynamicscript.repository.domain.DynamicScriptDOExample;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DynamicScriptDOMapper {
    long countByExample(DynamicScriptDOExample example);

    int deleteByExample(DynamicScriptDOExample example);

    int deleteByPrimaryKey(Long id);

    int insert(DynamicScriptDO record);

    int insertSelective(DynamicScriptDO record);

    List<DynamicScriptDO> selectByExampleWithBLOBs(DynamicScriptDOExample example);

    List<DynamicScriptDO> selectByExample(DynamicScriptDOExample example);

    DynamicScriptDO selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") DynamicScriptDO record, @Param("example") DynamicScriptDOExample example);

    int updateByExampleWithBLOBs(@Param("record") DynamicScriptDO record, @Param("example") DynamicScriptDOExample example);

    int updateByExample(@Param("record") DynamicScriptDO record, @Param("example") DynamicScriptDOExample example);

    int updateByPrimaryKeySelective(DynamicScriptDO record);

    int updateByPrimaryKeyWithBLOBs(DynamicScriptDO record);

    int updateByPrimaryKey(DynamicScriptDO record);
}