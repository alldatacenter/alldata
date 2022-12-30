package com.alibaba.tesla.appmanager.meta.helm.repository.mapper;

import com.alibaba.tesla.appmanager.meta.helm.repository.domain.HelmMetaDO;
import com.alibaba.tesla.appmanager.meta.helm.repository.domain.HelmMetaDOExample;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface HelmMetaDOMapper {
    long countByExample(HelmMetaDOExample example);

    int deleteByExample(HelmMetaDOExample example);

    int deleteByPrimaryKey(Long id);

    int insert(HelmMetaDO record);

    int insertSelective(HelmMetaDO record);

    List<HelmMetaDO> selectByExample(HelmMetaDOExample example);

    HelmMetaDO selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") HelmMetaDO record, @Param("example") HelmMetaDOExample example);

    int updateByExample(@Param("record") HelmMetaDO record, @Param("example") HelmMetaDOExample example);

    int updateByPrimaryKeySelective(HelmMetaDO record);

    int updateByPrimaryKey(HelmMetaDO record);
}