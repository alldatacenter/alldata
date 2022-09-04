package com.alibaba.tesla.appmanager.server.repository.mapper;

import com.alibaba.tesla.appmanager.server.repository.domain.AppOptionDO;
import com.alibaba.tesla.appmanager.server.repository.domain.AppOptionDOExample;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AppOptionDOMapper {
    long countByExample(AppOptionDOExample example);

    int deleteByExample(AppOptionDOExample example);

    int deleteByPrimaryKey(Long id);

    int insert(AppOptionDO record);

    int insertSelective(AppOptionDO record);

    List<AppOptionDO> selectByExample(AppOptionDOExample example);

    AppOptionDO selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") AppOptionDO record, @Param("example") AppOptionDOExample example);

    int updateByExample(@Param("record") AppOptionDO record, @Param("example") AppOptionDOExample example);

    int updateByPrimaryKeySelective(AppOptionDO record);

    int updateByPrimaryKey(AppOptionDO record);

    int batchInsert(@Param("list") List<AppOptionDO> list);
}