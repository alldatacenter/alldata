package com.alibaba.tesla.appmanager.server.repository.mapper;

import com.alibaba.tesla.appmanager.server.repository.domain.ProductDO;
import com.alibaba.tesla.appmanager.server.repository.domain.ProductDOExample;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductDOMapper {
    long countByExample(ProductDOExample example);

    int deleteByExample(ProductDOExample example);

    int deleteByPrimaryKey(Long id);

    int insert(ProductDO record);

    int insertSelective(ProductDO record);

    List<ProductDO> selectByExample(ProductDOExample example);

    ProductDO selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") ProductDO record, @Param("example") ProductDOExample example);

    int updateByExample(@Param("record") ProductDO record, @Param("example") ProductDOExample example);

    int updateByPrimaryKeySelective(ProductDO record);

    int updateByPrimaryKey(ProductDO record);
}