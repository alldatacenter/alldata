package com.alibaba.tesla.appmanager.server.repository.mapper;

import com.alibaba.tesla.appmanager.server.repository.domain.CustomAddonInstanceDO;
import com.alibaba.tesla.appmanager.server.repository.domain.CustomAddonInstanceDOExample;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.RowBounds;

import java.util.List;

@Mapper
public interface CustomAddonInstanceDOMapper {
    long countByExample(CustomAddonInstanceDOExample example);

    int deleteByExample(CustomAddonInstanceDOExample example);

    int deleteByPrimaryKey(Long id);

    int insert(CustomAddonInstanceDO record);

    int insertSelective(CustomAddonInstanceDO record);

    int insertOrUpdateSelective(CustomAddonInstanceDO record);

    List<CustomAddonInstanceDO> selectByExampleWithBLOBsWithRowbounds(CustomAddonInstanceDOExample example, RowBounds rowBounds);

    List<CustomAddonInstanceDO> selectByExampleWithBLOBs(CustomAddonInstanceDOExample example);

    List<CustomAddonInstanceDO> selectByExampleWithRowbounds(CustomAddonInstanceDOExample example, RowBounds rowBounds);

    List<CustomAddonInstanceDO> selectByExample(CustomAddonInstanceDOExample example);

    CustomAddonInstanceDO selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") CustomAddonInstanceDO record, @Param("example") CustomAddonInstanceDOExample example);

    int updateByExampleWithBLOBs(@Param("record") CustomAddonInstanceDO record, @Param("example") CustomAddonInstanceDOExample example);

    int updateByExample(@Param("record") CustomAddonInstanceDO record, @Param("example") CustomAddonInstanceDOExample example);

    int updateByPrimaryKeySelective(CustomAddonInstanceDO record);

    int updateByPrimaryKeyWithBLOBs(CustomAddonInstanceDO record);

    int updateByPrimaryKey(CustomAddonInstanceDO record);
}