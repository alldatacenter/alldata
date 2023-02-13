package com.alibaba.tesla.appmanager.server.repository.mapper;

import com.alibaba.tesla.appmanager.server.repository.domain.ProductReleaseAppRelDO;
import com.alibaba.tesla.appmanager.server.repository.domain.ProductReleaseAppRelDOExample;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductReleaseAppRelDOMapper {
    long countByExample(ProductReleaseAppRelDOExample example);

    int deleteByExample(ProductReleaseAppRelDOExample example);

    int deleteByPrimaryKey(Long id);

    int insert(ProductReleaseAppRelDO record);

    int insertSelective(ProductReleaseAppRelDO record);

    List<ProductReleaseAppRelDO> selectByExample(ProductReleaseAppRelDOExample example);

    ProductReleaseAppRelDO selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") ProductReleaseAppRelDO record, @Param("example") ProductReleaseAppRelDOExample example);

    int updateByExample(@Param("record") ProductReleaseAppRelDO record, @Param("example") ProductReleaseAppRelDOExample example);

    int updateByPrimaryKeySelective(ProductReleaseAppRelDO record);

    int updateByPrimaryKey(ProductReleaseAppRelDO record);
}