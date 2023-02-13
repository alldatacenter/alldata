package com.alibaba.tesla.appmanager.server.repository.mapper;

import com.alibaba.tesla.appmanager.server.repository.domain.ProductReleaseTaskAppPackageTaskRelDO;
import com.alibaba.tesla.appmanager.server.repository.domain.ProductReleaseTaskAppPackageTaskRelDOExample;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductReleaseTaskAppPackageTaskRelDOMapper {
    long countByExample(ProductReleaseTaskAppPackageTaskRelDOExample example);

    int deleteByExample(ProductReleaseTaskAppPackageTaskRelDOExample example);

    int deleteByPrimaryKey(Long id);

    int insert(ProductReleaseTaskAppPackageTaskRelDO record);

    int insertSelective(ProductReleaseTaskAppPackageTaskRelDO record);

    List<ProductReleaseTaskAppPackageTaskRelDO> selectByExample(ProductReleaseTaskAppPackageTaskRelDOExample example);

    ProductReleaseTaskAppPackageTaskRelDO selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") ProductReleaseTaskAppPackageTaskRelDO record, @Param("example") ProductReleaseTaskAppPackageTaskRelDOExample example);

    int updateByExample(@Param("record") ProductReleaseTaskAppPackageTaskRelDO record, @Param("example") ProductReleaseTaskAppPackageTaskRelDOExample example);

    int updateByPrimaryKeySelective(ProductReleaseTaskAppPackageTaskRelDO record);

    int updateByPrimaryKey(ProductReleaseTaskAppPackageTaskRelDO record);
}