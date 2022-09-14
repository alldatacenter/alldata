package com.alibaba.tesla.appmanager.server.repository.mapper;

import com.alibaba.tesla.appmanager.server.repository.domain.ProductReleaseTaskDO;
import com.alibaba.tesla.appmanager.server.repository.domain.ProductReleaseTaskDOExample;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductReleaseTaskDOMapper {
    long countByExample(ProductReleaseTaskDOExample example);

    int deleteByExample(ProductReleaseTaskDOExample example);

    int deleteByPrimaryKey(Long id);

    int insert(ProductReleaseTaskDO record);

    int insertSelective(ProductReleaseTaskDO record);

    List<ProductReleaseTaskDO> selectByExample(ProductReleaseTaskDOExample example);

    ProductReleaseTaskDO selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") ProductReleaseTaskDO record, @Param("example") ProductReleaseTaskDOExample example);

    int updateByExample(@Param("record") ProductReleaseTaskDO record, @Param("example") ProductReleaseTaskDOExample example);

    int updateByPrimaryKeySelective(ProductReleaseTaskDO record);

    int updateByPrimaryKey(ProductReleaseTaskDO record);

    List<ProductReleaseTaskDO> selectByTags(
            @Param("productId") String productId,
            @Param("releaseId") String releaseId,
            @Param("taskId") String taskId,
            @Param("tags") List<String> tags,
            @Param("tagSize") Integer tagSize,
            @Param("example") ProductReleaseTaskDOExample example
    );
}