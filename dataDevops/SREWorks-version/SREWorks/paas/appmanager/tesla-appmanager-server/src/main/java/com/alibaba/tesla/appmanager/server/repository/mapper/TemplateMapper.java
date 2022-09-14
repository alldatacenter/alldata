package com.alibaba.tesla.appmanager.server.repository.mapper;

import com.alibaba.tesla.appmanager.server.repository.domain.TemplateDO;
import com.alibaba.tesla.appmanager.server.repository.domain.TemplateDOExample;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TemplateMapper {
    long countByExample(TemplateDOExample example);

    int deleteByExample(TemplateDOExample example);

    int insertSelective(TemplateDO record);

    List<TemplateDO> selectByExample(TemplateDOExample example);

    int updateByExampleSelective(@Param("record") TemplateDO record, @Param("example") TemplateDOExample example);

    int updateByExample(@Param("record") TemplateDO record, @Param("example") TemplateDOExample example);
}