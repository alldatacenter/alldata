package com.alibaba.sreworks.pmdb.domain.metric;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.RowBounds;

import java.util.List;

public interface DataSourceMapper {
    long countByExample(DataSourceExample example);

    int deleteByExample(DataSourceExample example);

    int deleteByPrimaryKey(Integer id);

    int insert(DataSource record);

    int insertSelective(DataSource record);

    List<DataSource> selectByExampleWithBLOBsWithRowbounds(DataSourceExample example, RowBounds rowBounds);

    List<DataSource> selectByExampleWithBLOBs(DataSourceExample example);

    List<DataSource> selectByExampleWithRowbounds(DataSourceExample example, RowBounds rowBounds);

    List<DataSource> selectByExample(DataSourceExample example);

    DataSource selectByPrimaryKey(Integer id);

    int updateByExampleSelective(@Param("record") DataSource record, @Param("example") DataSourceExample example);

    int updateByExampleWithBLOBs(@Param("record") DataSource record, @Param("example") DataSourceExample example);

    int updateByExample(@Param("record") DataSource record, @Param("example") DataSourceExample example);

    int updateByPrimaryKeySelective(DataSource record);

    int updateByPrimaryKeyWithBLOBs(DataSource record);

    int updateByPrimaryKey(DataSource record);
}