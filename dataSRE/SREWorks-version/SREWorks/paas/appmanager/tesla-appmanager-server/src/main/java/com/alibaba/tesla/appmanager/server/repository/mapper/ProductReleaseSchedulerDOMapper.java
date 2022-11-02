package com.alibaba.tesla.appmanager.server.repository.mapper;

import com.alibaba.tesla.appmanager.server.repository.domain.ProductReleaseSchedulerDO;
import com.alibaba.tesla.appmanager.server.repository.domain.ProductReleaseSchedulerDOExample;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductReleaseSchedulerDOMapper {
    long countByExample(ProductReleaseSchedulerDOExample example);

    int deleteByExample(ProductReleaseSchedulerDOExample example);

    int deleteByPrimaryKey(Long id);

    int insert(ProductReleaseSchedulerDO record);

    int insertSelective(ProductReleaseSchedulerDO record);

    List<ProductReleaseSchedulerDO> selectByExample(ProductReleaseSchedulerDOExample example);

    ProductReleaseSchedulerDO selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") ProductReleaseSchedulerDO record, @Param("example") ProductReleaseSchedulerDOExample example);

    int updateByExample(@Param("record") ProductReleaseSchedulerDO record, @Param("example") ProductReleaseSchedulerDOExample example);

    int updateByPrimaryKeySelective(ProductReleaseSchedulerDO record);

    int updateByPrimaryKey(ProductReleaseSchedulerDO record);
}