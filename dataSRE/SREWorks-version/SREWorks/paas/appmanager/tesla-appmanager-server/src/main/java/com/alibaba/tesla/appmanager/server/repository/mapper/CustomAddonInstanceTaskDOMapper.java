package com.alibaba.tesla.appmanager.server.repository.mapper;

import com.alibaba.tesla.appmanager.server.repository.domain.CustomAddonInstanceTaskDO;
import com.alibaba.tesla.appmanager.server.repository.domain.CustomAddonInstanceTaskDOExample;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.RowBounds;

import java.util.List;

@Mapper
public interface CustomAddonInstanceTaskDOMapper {
    long countByExample(CustomAddonInstanceTaskDOExample example);

    int deleteByExample(CustomAddonInstanceTaskDOExample example);

    int deleteByPrimaryKey(Long id);

    int insert(CustomAddonInstanceTaskDO record);

    int insertSelective(CustomAddonInstanceTaskDO record);

    int insertOrUpdateSelective(CustomAddonInstanceTaskDO record);

    List<CustomAddonInstanceTaskDO> selectByExampleWithBLOBsWithRowbounds(CustomAddonInstanceTaskDOExample example, RowBounds rowBounds);

    List<CustomAddonInstanceTaskDO> selectByExampleWithBLOBs(CustomAddonInstanceTaskDOExample example);

    List<CustomAddonInstanceTaskDO> selectByExampleWithRowbounds(CustomAddonInstanceTaskDOExample example, RowBounds rowBounds);

    List<CustomAddonInstanceTaskDO> selectByExample(CustomAddonInstanceTaskDOExample example);

    CustomAddonInstanceTaskDO selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") CustomAddonInstanceTaskDO record, @Param("example") CustomAddonInstanceTaskDOExample example);

    int updateByExampleWithBLOBs(@Param("record") CustomAddonInstanceTaskDO record, @Param("example") CustomAddonInstanceTaskDOExample example);

    int updateByExample(@Param("record") CustomAddonInstanceTaskDO record, @Param("example") CustomAddonInstanceTaskDOExample example);

    int updateByPrimaryKeySelective(CustomAddonInstanceTaskDO record);

    int updateByPrimaryKeyWithBLOBs(CustomAddonInstanceTaskDO record);

    int updateByPrimaryKey(CustomAddonInstanceTaskDO record);
}