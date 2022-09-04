package com.alibaba.tesla.authproxy.model.mapper;

import com.alibaba.tesla.authproxy.model.PermissionMetaDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 权限元数据
 * @author tandong.td@alibaba-inc.com
 */
@Mapper
public interface PermissionMetaMapper {

    /**
     * 查询某个应用下已开通服务的权限元数据
     * @param appId
     * @return
     */
    List<PermissionMetaDO> selectByAppIdAndServiceCode(String appId, String serviceCode);

    /**
     * 查询某个服务的所有权限元数据
     * @param serviceCode
     * @return
     */
    List<PermissionMetaDO> selectByServiceCode(String serviceCode);

    /**
     * 根据主键查询唯一的权限元数据
     * @param id
     * @return
     */
    PermissionMetaDO selectByPrimaryKey(long id);

    /**
     * 根据服务标识统计服务下的权限元数据总数
     * @param serviceCode
     * @return
     */
    int countByServiceCode(String serviceCode);

    /**
     * 根据服务标识和权限标识获取唯一的权限元数据记录
     * @param permissionCode
     * @return
     */
    PermissionMetaDO selectOne(String permissionCode);

    /**
     * 根据服务标识和权限标识获取唯一的权限元数据记录,只获取有效的权限元数据
     * @param permissionCode
     * @return
     */
    PermissionMetaDO selectEnableOne(String permissionCode);

    /**
     * 根据主键查询权限元数据
     * @param ids
     * @return
     */
    List<PermissionMetaDO> selectByIds(List<Long> ids);

    /**
     * 根据主键删除
     * @param id
     * @return
     */
    int deleteByPrimaryKey(Long id);

    /**
     * 插入
     * @param record
     * @return
     */
    int insertSelective(PermissionMetaDO record);

    /**
     * 查询
     * @return
     */
    List<PermissionMetaDO> select();

    /**
     * 根据主键更新
     * @param record
     * @return
     */
    int updateByPrimaryKeySelective(PermissionMetaDO record);

}
