package com.alibaba.tesla.appmanager.server.repository.mapper;

import com.alibaba.tesla.appmanager.server.repository.domain.AppPackageDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TagQueryMapper {

    /**
     * 查询指定 tag 的最大应用版本的应用包列表
     *
     * @param tag   标签
     * @param start 起始
     * @param limit 单次数量
     * @return List of AppPackageDO
     */
    List<AppPackageDO> queryAppPackageWithMaxVersion(
            @Param("tag") String tag,
            @Param("start") Integer start,
            @Param("limit") Integer limit);

    /**
     * 查询指定 tag 的最大应用版本的应用包列表 (计数)
     *
     * @param tag 标签
     * @return count
     */
    Integer countAppPackageWithMaxVersion(@Param("tag") String tag);

    /**
     * 查询指定 tag 的最大应用版本的应用包列表
     *
     * @param tag   标签
     * @param key   Option Key
     * @param value Option Value
     * @param start 起始
     * @param limit 单次数量
     * @return List of AppPackageDO
     */
    List<AppPackageDO> queryAppPackageWithMaxVersionAndOption(
            @Param("tag") String tag,
            @Param("key") String key,
            @Param("value") String value,
            @Param("start") Integer start,
            @Param("limit") Integer limit);

    /**
     * 查询指定 tag 的最大应用版本的应用包列表 (计数)
     *
     * @param tag   标签
     * @param key   Option Key
     * @param value Option Value
     * @return count
     */
    Integer countAppPackageWithMaxVersionAndOption(
            @Param("tag") String tag,
            @Param("key") String key,
            @Param("value") String value);
}