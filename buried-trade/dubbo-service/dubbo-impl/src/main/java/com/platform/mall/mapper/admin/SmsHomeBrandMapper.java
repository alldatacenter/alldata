package com.platform.mall.mapper.admin;

import com.platform.mall.entity.admin.SmsHomeBrand;
import com.platform.mall.entity.admin.SmsHomeBrandExample;

import java.util.List;

import org.apache.ibatis.annotations.Param;

public interface SmsHomeBrandMapper {
    long countByExample(SmsHomeBrandExample example);

    int deleteByExample(SmsHomeBrandExample example);

    int deleteByPrimaryKey(Long id);

    int insert(SmsHomeBrand record);

    int insertSelective(SmsHomeBrand record);

    List<SmsHomeBrand> selectByExample(SmsHomeBrandExample example);

    SmsHomeBrand selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") SmsHomeBrand record, @Param("example") SmsHomeBrandExample example);

    int updateByExample(@Param("record") SmsHomeBrand record, @Param("example") SmsHomeBrandExample example);

    int updateByPrimaryKeySelective(SmsHomeBrand record);

    int updateByPrimaryKey(SmsHomeBrand record);
}
