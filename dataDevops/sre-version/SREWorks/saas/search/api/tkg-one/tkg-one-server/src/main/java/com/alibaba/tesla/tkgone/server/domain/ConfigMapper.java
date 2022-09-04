package com.alibaba.tesla.tkgone.server.domain;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.RowBounds;

import java.util.List;

public interface ConfigMapper {
    long countByExample(ConfigExample example);

    int deleteByExample(ConfigExample example);

    int deleteByPrimaryKey(Long id);

    int insert(Config record);

    int insertSelective(Config record);

    List<Config> selectByExampleWithRowbounds(ConfigExample example, RowBounds rowBounds);

    List<Config> selectByExample(ConfigExample example);

    Config selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") Config record, @Param("example") ConfigExample example);

    int updateByExample(@Param("record") Config record, @Param("example") ConfigExample example);

    int updateByPrimaryKeySelective(Config record);

    int updateByPrimaryKey(Config record);
}