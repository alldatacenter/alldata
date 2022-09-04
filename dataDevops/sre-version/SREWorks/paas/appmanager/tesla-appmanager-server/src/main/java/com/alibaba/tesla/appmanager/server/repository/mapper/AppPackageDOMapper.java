package com.alibaba.tesla.appmanager.server.repository.mapper;

import com.alibaba.tesla.appmanager.domain.dto.AppPackageVersionCountDTO;
import com.alibaba.tesla.appmanager.server.repository.domain.AppPackageDO;
import com.alibaba.tesla.appmanager.server.repository.domain.AppPackageDOExample;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AppPackageDOMapper {
    long countByExample(AppPackageDOExample example);

    int deleteByExample(AppPackageDOExample example);

    int deleteByPrimaryKey(Long id);

    int insertSelective(AppPackageDO record);

    List<AppPackageDO> selectByExampleWithBLOBs(AppPackageDOExample example);

    List<AppPackageDO> selectByExample(AppPackageDOExample example);

    int updateByExampleSelective(@Param("record") AppPackageDO record, @Param("example") AppPackageDOExample example);

    int updateByPrimaryKeySelective(AppPackageDO record);

    List<AppPackageDO> selectByTagsWithBLOBs(
            @Param("appId") String appId,
            @Param("tags") List<String> tags,
            @Param("tagSize") Integer tagSize,
            @Param("example") AppPackageDOExample example);

    List<AppPackageDO> selectByTags(
            @Param("appId") String appId,
            @Param("tags") List<String> tags,
            @Param("tagSize") Integer tagSize,
            @Param("example") AppPackageDOExample example);

    List<AppPackageVersionCountDTO> countAppPackageVersion(
            @Param("appIds") List<String> appIds, @Param("tag") String tag);
}