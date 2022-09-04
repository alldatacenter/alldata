package com.alibaba.tesla.appmanager.server.repository.mapper;

import com.alibaba.tesla.appmanager.server.repository.domain.ComponentHistoryDO;
import com.alibaba.tesla.appmanager.server.repository.domain.ComponentHistoryDOExample;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ComponentHistoryDOMapper {
    long countByExample(ComponentHistoryDOExample example);

    int deleteByExample(ComponentHistoryDOExample example);

    int deleteByPrimaryKey(Long id);

    int insert(ComponentHistoryDO record);

    int insertSelective(ComponentHistoryDO record);

    List<ComponentHistoryDO> selectByExampleWithBLOBs(ComponentHistoryDOExample example);

    List<ComponentHistoryDO> selectByExample(ComponentHistoryDOExample example);

    ComponentHistoryDO selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") ComponentHistoryDO record, @Param("example") ComponentHistoryDOExample example);

    int updateByExampleWithBLOBs(@Param("record") ComponentHistoryDO record, @Param("example") ComponentHistoryDOExample example);

    int updateByExample(@Param("record") ComponentHistoryDO record, @Param("example") ComponentHistoryDOExample example);

    int updateByPrimaryKeySelective(ComponentHistoryDO record);

    int updateByPrimaryKeyWithBLOBs(ComponentHistoryDO record);

    int updateByPrimaryKey(ComponentHistoryDO record);
}