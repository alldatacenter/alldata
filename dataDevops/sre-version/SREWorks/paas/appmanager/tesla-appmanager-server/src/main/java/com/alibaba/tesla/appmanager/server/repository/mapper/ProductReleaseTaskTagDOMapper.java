package com.alibaba.tesla.appmanager.server.repository.mapper;

import com.alibaba.tesla.appmanager.server.repository.domain.ProductReleaseTaskTagDO;
import com.alibaba.tesla.appmanager.server.repository.domain.ProductReleaseTaskTagDOExample;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductReleaseTaskTagDOMapper {
    long countByExample(ProductReleaseTaskTagDOExample example);

    int deleteByExample(ProductReleaseTaskTagDOExample example);

    int deleteByPrimaryKey(Long id);

    int insert(ProductReleaseTaskTagDO record);

    int insertSelective(ProductReleaseTaskTagDO record);

    List<ProductReleaseTaskTagDO> selectByExample(ProductReleaseTaskTagDOExample example);

    ProductReleaseTaskTagDO selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") ProductReleaseTaskTagDO record, @Param("example") ProductReleaseTaskTagDOExample example);

    int updateByExample(@Param("record") ProductReleaseTaskTagDO record, @Param("example") ProductReleaseTaskTagDOExample example);

    int updateByPrimaryKeySelective(ProductReleaseTaskTagDO record);

    int updateByPrimaryKey(ProductReleaseTaskTagDO record);
}