package com.alibaba.tesla.authproxy.service;

import com.alibaba.tesla.authproxy.model.ServiceMetaDO;
import com.github.pagehelper.PageInfo;

import java.util.List;

/**
 * <p>Description: 权限元数据服务接口 <／p>
 * <p>Copyright: Copyright (c) 2017<／p>
 * <p>Company: alibaba <／p>
 *
 * @author tandong.td@alibaba-inc.com
 * @version 1.0
 * @date 2018年11月13日
 */
public interface ServiceMetaService {

    /**
     * 查询某个应用下所有已经完全开启或部分开启权限的服务元数据
     * @param appId
     * @return
     */
    List<ServiceMetaDO> selectWithGrant(String appId);

    /**
     * 分页查询服务元数据,返回数据包含该服务下的所有权限个数已经开启的权限个数
     * @param appId
     * @param page
     * @param size
     * @return
     */
    PageInfo<ServiceMetaDO> select(String appId, int page, int size);

    /**
     * 查询唯一的权限元数据
     * @param serviceCode
     * @return
     */
    ServiceMetaDO selectOne(String serviceCode);


    /**
     * @param serviceMetaDO
     * @return
     */
    int update(ServiceMetaDO serviceMetaDO);

    /**
     * @param serviceMetaDO
     * @return
     */
    int insert(ServiceMetaDO serviceMetaDO);

    /**
     * @param id
     * @return
     */
    int delete(long id);
}
