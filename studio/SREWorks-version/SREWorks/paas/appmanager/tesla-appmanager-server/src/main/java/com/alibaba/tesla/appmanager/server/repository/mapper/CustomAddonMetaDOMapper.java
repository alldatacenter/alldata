package com.alibaba.tesla.appmanager.server.repository.mapper;

import com.alibaba.tesla.appmanager.server.repository.domain.CustomAddonMetaDO;
import com.alibaba.tesla.appmanager.server.repository.domain.CustomAddonMetaDOExample;
import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.RowBounds;

import java.util.List;

@Mapper
public interface CustomAddonMetaDOMapper {
    long countByExample(CustomAddonMetaDOExample example);

    int deleteByExample(CustomAddonMetaDOExample example);

    int deleteByPrimaryKey(Long id);

    int insert(CustomAddonMetaDO record);

    int insertSelective(CustomAddonMetaDO record);

    List<CustomAddonMetaDO> selectByExampleWithBLOBsWithRowbounds(CustomAddonMetaDOExample example, RowBounds rowBounds);

    Page<CustomAddonMetaDO> selectByExampleWithBLOBs(CustomAddonMetaDOExample example);

    List<CustomAddonMetaDO> selectByExampleWithRowbounds(CustomAddonMetaDOExample example, RowBounds rowBounds);

    List<CustomAddonMetaDO> selectByExample(CustomAddonMetaDOExample example);

    CustomAddonMetaDO selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") CustomAddonMetaDO record, @Param("example") CustomAddonMetaDOExample example);

    int updateByExampleWithBLOBs(@Param("record") CustomAddonMetaDO record, @Param("example") CustomAddonMetaDOExample example);

    int updateByExample(@Param("record") CustomAddonMetaDO record, @Param("example") CustomAddonMetaDOExample example);

    int updateByPrimaryKeySelective(CustomAddonMetaDO record);

    int updateByPrimaryKeyWithBLOBs(CustomAddonMetaDO record);

    int updateByPrimaryKey(CustomAddonMetaDO record);
}