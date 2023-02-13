package com.alibaba.tesla.appmanager.server.repository.mapper;

import com.alibaba.tesla.appmanager.server.repository.domain.DeployComponentAttrDO;
import com.alibaba.tesla.appmanager.server.repository.domain.DeployComponentAttrDOExample;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DeployComponentAttrDOMapper {
    long countByExample(DeployComponentAttrDOExample example);

    int deleteByExample(DeployComponentAttrDOExample example);

    int deleteByPrimaryKey(Long id);

    int insert(DeployComponentAttrDO record);

    int insertSelective(DeployComponentAttrDO record);

    List<DeployComponentAttrDO> selectByExample(DeployComponentAttrDOExample example);

    DeployComponentAttrDO selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") DeployComponentAttrDO record, @Param("example") DeployComponentAttrDOExample example);

    int updateByExample(@Param("record") DeployComponentAttrDO record, @Param("example") DeployComponentAttrDOExample example);

    int updateByPrimaryKeySelective(DeployComponentAttrDO record);

    int updateByPrimaryKey(DeployComponentAttrDO record);
}