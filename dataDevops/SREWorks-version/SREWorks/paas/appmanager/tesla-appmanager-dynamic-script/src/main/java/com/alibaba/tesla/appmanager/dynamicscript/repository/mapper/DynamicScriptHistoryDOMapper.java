package com.alibaba.tesla.appmanager.dynamicscript.repository.mapper;

import com.alibaba.tesla.appmanager.dynamicscript.repository.domain.DynamicScriptHistoryDO;
import com.alibaba.tesla.appmanager.dynamicscript.repository.domain.DynamicScriptHistoryDOExample;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DynamicScriptHistoryDOMapper {
    long countByExample(DynamicScriptHistoryDOExample example);

    int deleteByExample(DynamicScriptHistoryDOExample example);

    int deleteByPrimaryKey(Long id);

    int insert(DynamicScriptHistoryDO record);

    int insertSelective(DynamicScriptHistoryDO record);

    List<DynamicScriptHistoryDO> selectByExampleWithBLOBs(DynamicScriptHistoryDOExample example);

    List<DynamicScriptHistoryDO> selectByExample(DynamicScriptHistoryDOExample example);

    DynamicScriptHistoryDO selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") DynamicScriptHistoryDO record, @Param("example") DynamicScriptHistoryDOExample example);

    int updateByExampleWithBLOBs(@Param("record") DynamicScriptHistoryDO record, @Param("example") DynamicScriptHistoryDOExample example);

    int updateByExample(@Param("record") DynamicScriptHistoryDO record, @Param("example") DynamicScriptHistoryDOExample example);

    int updateByPrimaryKeySelective(DynamicScriptHistoryDO record);

    int updateByPrimaryKeyWithBLOBs(DynamicScriptHistoryDO record);

    int updateByPrimaryKey(DynamicScriptHistoryDO record);
}