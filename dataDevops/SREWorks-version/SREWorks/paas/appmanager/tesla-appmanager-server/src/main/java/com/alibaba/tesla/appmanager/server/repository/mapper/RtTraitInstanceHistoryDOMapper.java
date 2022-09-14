package com.alibaba.tesla.appmanager.server.repository.mapper;

import com.alibaba.tesla.appmanager.server.repository.domain.RtTraitInstanceHistoryDO;
import com.alibaba.tesla.appmanager.server.repository.domain.RtTraitInstanceHistoryDOExample;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RtTraitInstanceHistoryDOMapper {
    long countByExample(RtTraitInstanceHistoryDOExample example);

    int deleteByExample(RtTraitInstanceHistoryDOExample example);

    int deleteByPrimaryKey(Long id);

    int insert(RtTraitInstanceHistoryDO record);

    int insertSelective(RtTraitInstanceHistoryDO record);

    List<RtTraitInstanceHistoryDO> selectByExample(RtTraitInstanceHistoryDOExample example);

    RtTraitInstanceHistoryDO selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") RtTraitInstanceHistoryDO record, @Param("example") RtTraitInstanceHistoryDOExample example);

    int updateByExample(@Param("record") RtTraitInstanceHistoryDO record, @Param("example") RtTraitInstanceHistoryDOExample example);

    int updateByPrimaryKeySelective(RtTraitInstanceHistoryDO record);

    int updateByPrimaryKey(RtTraitInstanceHistoryDO record);
}