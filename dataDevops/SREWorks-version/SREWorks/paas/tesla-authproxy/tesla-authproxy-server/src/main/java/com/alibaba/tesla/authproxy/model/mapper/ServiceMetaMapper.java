package com.alibaba.tesla.authproxy.model.mapper;

import com.alibaba.tesla.authproxy.model.ServiceMetaDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 权限元数据
 * @author tandong.td@alibaba-inc.com
 */
@Mapper
public interface ServiceMetaMapper {

    /**
     * 查询某个应用下已经完成开启或者部分开启的服务元数据
     * 需要关联权限资源表查询
     * @return
     */
    List<ServiceMetaDO> selectWithGrantByAppId(String appId);

    /**
     * 查询TESLA服务元数据
     * @return
     */
    List<ServiceMetaDO> select();

    /**
     * 根据服务标识和权限标识获取唯一的服务元数据记录
     * @param serviceCode
     * @return
     */
    ServiceMetaDO selectOne(String serviceCode);

    /**
     * 根据主键查询权限元数据
     * @param id
     * @return
     */
    ServiceMetaDO selectById(long id);

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
    int insertSelective(ServiceMetaDO record);

    /**
     * 根据主键更新
     * @param record
     * @return
     */
    int updateByPrimaryKeySelective(ServiceMetaDO record);

}
