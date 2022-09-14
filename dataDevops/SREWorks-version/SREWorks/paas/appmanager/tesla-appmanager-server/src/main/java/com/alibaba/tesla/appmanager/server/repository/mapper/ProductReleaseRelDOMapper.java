package com.alibaba.tesla.appmanager.server.repository.mapper;

import com.alibaba.tesla.appmanager.server.repository.domain.ProductReleaseRelDO;
import com.alibaba.tesla.appmanager.server.repository.domain.ProductReleaseRelDOExample;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductReleaseRelDOMapper {
    long countByExample(ProductReleaseRelDOExample example);

    int deleteByExample(ProductReleaseRelDOExample example);

    int deleteByPrimaryKey(Long id);

    int insert(ProductReleaseRelDO record);

    int insertSelective(ProductReleaseRelDO record);

    List<ProductReleaseRelDO> selectByExample(ProductReleaseRelDOExample example);

    ProductReleaseRelDO selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") ProductReleaseRelDO record, @Param("example") ProductReleaseRelDOExample example);

    int updateByExample(@Param("record") ProductReleaseRelDO record, @Param("example") ProductReleaseRelDOExample example);

    int updateByPrimaryKeySelective(ProductReleaseRelDO record);

    int updateByPrimaryKey(ProductReleaseRelDO record);
}