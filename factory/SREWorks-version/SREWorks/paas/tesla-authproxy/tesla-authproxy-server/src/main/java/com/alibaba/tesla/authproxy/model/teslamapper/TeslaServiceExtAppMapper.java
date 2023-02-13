package com.alibaba.tesla.authproxy.model.teslamapper;

import com.alibaba.tesla.authproxy.model.TeslaServiceExtAppDO;
import com.alibaba.tesla.authproxy.model.example.TeslaServiceExtAppExample;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.RowBounds;

import java.util.List;

@Mapper
public interface TeslaServiceExtAppMapper {
    long countByExample(TeslaServiceExtAppExample example);

    int deleteByExample(TeslaServiceExtAppExample example);

    int deleteByPrimaryKey(Integer id);

    int insert(TeslaServiceExtAppDO record);

    int insertSelective(TeslaServiceExtAppDO record);

    List<TeslaServiceExtAppDO> selectByExampleWithRowbounds(TeslaServiceExtAppExample example, RowBounds rowBounds);

    List<TeslaServiceExtAppDO> selectByExample(TeslaServiceExtAppExample example);

    TeslaServiceExtAppDO selectByPrimaryKey(Integer id);

    int updateByExampleSelective(@Param("record") TeslaServiceExtAppDO record, @Param("example") TeslaServiceExtAppExample example);

    int updateByExample(@Param("record") TeslaServiceExtAppDO record, @Param("example") TeslaServiceExtAppExample example);

    int updateByPrimaryKeySelective(TeslaServiceExtAppDO record);

    int updateByPrimaryKey(TeslaServiceExtAppDO record);
}