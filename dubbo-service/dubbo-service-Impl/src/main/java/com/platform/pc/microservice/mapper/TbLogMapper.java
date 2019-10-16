package com.platform.pc.microservice.mapper;

import com.platform.mall.pojo.TbLog;
import com.platform.mall.pojo.TbLogExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface TbLogMapper {
    long countByExample(TbLogExample example);

    int deleteByExample(TbLogExample example);

    int deleteByPrimaryKey(Integer id);

    int insert(TbLog record);

    int insertSelective(TbLog record);

    List<TbLog> selectByExample(TbLogExample example);

    TbLog selectByPrimaryKey(Integer id);

    int updateByExampleSelective(@Param("record") TbLog record, @Param("example") TbLogExample example);

    int updateByExample(@Param("record") TbLog record, @Param("example") TbLogExample example);

    int updateByPrimaryKeySelective(TbLog record);

    int updateByPrimaryKey(TbLog record);

    List<TbLog> selectByMulti(@Param("search") String search,@Param("orderCol") String orderCol, @Param("orderDir") String orderDir);
}
