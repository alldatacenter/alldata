package com.alibaba.tesla.appmanager.server.repository.mapper;

import com.alibaba.tesla.appmanager.server.repository.domain.UnitDO;
import com.alibaba.tesla.appmanager.server.repository.domain.UnitDOExample;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UnitDOMapper {
    long countByExample(UnitDOExample example);

    int deleteByExample(UnitDOExample example);

    int deleteByPrimaryKey(Long id);

    int insert(UnitDO record);

    int insertSelective(UnitDO record);

    List<UnitDO> selectByExample(UnitDOExample example);

    UnitDO selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") UnitDO record, @Param("example") UnitDOExample example);

    int updateByExample(@Param("record") UnitDO record, @Param("example") UnitDOExample example);

    int updateByPrimaryKeySelective(UnitDO record);

    int updateByPrimaryKey(UnitDO record);
}