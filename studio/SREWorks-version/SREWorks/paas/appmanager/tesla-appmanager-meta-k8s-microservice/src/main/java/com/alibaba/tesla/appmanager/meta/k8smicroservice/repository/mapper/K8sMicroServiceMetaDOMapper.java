package com.alibaba.tesla.appmanager.meta.k8smicroservice.repository.mapper;

import com.alibaba.tesla.appmanager.meta.k8smicroservice.repository.domain.K8sMicroServiceMetaDO;
import com.alibaba.tesla.appmanager.meta.k8smicroservice.repository.domain.K8sMicroServiceMetaDOExample;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface K8sMicroServiceMetaDOMapper {

    long countByExample(K8sMicroServiceMetaDOExample example);

    int deleteByExample(K8sMicroServiceMetaDOExample example);

    int deleteByPrimaryKey(Long id);

    int insertSelective(K8sMicroServiceMetaDO record);

    List<K8sMicroServiceMetaDO> selectByExampleWithBLOBs(K8sMicroServiceMetaDOExample example);

    List<K8sMicroServiceMetaDO> selectByExample(K8sMicroServiceMetaDOExample example);

    K8sMicroServiceMetaDO selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") K8sMicroServiceMetaDO record, @Param("example") K8sMicroServiceMetaDOExample example);
}