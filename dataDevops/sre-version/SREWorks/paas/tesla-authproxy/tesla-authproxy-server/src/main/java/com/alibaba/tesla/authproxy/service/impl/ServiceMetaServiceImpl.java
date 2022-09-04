package com.alibaba.tesla.authproxy.service.impl;

import com.alibaba.tesla.authproxy.model.mapper.PermissionMetaMapper;
import com.alibaba.tesla.authproxy.model.mapper.PermissionResMapper;
import com.alibaba.tesla.authproxy.model.mapper.ServiceMetaMapper;
import com.alibaba.tesla.authproxy.model.ServiceMetaDO;
import com.alibaba.tesla.authproxy.service.ServiceMetaService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 服务元数据服务实现
 * @author tandong.td@alibaba-inc.com
 */
@Service
@Slf4j
@Transactional(rollbackForClassName = "*")
public class ServiceMetaServiceImpl implements ServiceMetaService {

    @Autowired
    ServiceMetaMapper serviceMetaMapper;

    @Autowired
    PermissionMetaMapper permissionMetaMapper;

    @Autowired
    PermissionResMapper permissionResMapper;

    @Override
    public List<ServiceMetaDO> selectWithGrant(String appId) {

        List<ServiceMetaDO> serviceMetaDOS = serviceMetaMapper.selectWithGrantByAppId(appId);
        serviceMetaDOS.parallelStream().forEach(serviceMetaDO -> {
            serviceMetaDO.setPermissionMetaCount(permissionMetaMapper.countByServiceCode(serviceMetaDO.getServiceCode()));
            serviceMetaDO.setGrantPermissionCount(permissionResMapper.countByAppIdAndServiceCode(appId, serviceMetaDO.getServiceCode()));
        });
        return serviceMetaDOS;
    }

    @Override
    public PageInfo<ServiceMetaDO> select(String appId, int page, int size) {
        PageHelper.startPage(page, size);
        List<ServiceMetaDO> serviceMetaDOS = serviceMetaMapper.select();
        serviceMetaDOS.parallelStream().forEach(serviceMetaDO -> {
            serviceMetaDO.setPermissionMetaCount(permissionMetaMapper.countByServiceCode(serviceMetaDO.getServiceCode()));
            serviceMetaDO.setGrantPermissionCount(permissionResMapper.countByAppIdAndServiceCode(appId, serviceMetaDO.getServiceCode()));
        });
        PageInfo<ServiceMetaDO> pageInfo = new PageInfo<>(serviceMetaDOS);
        return pageInfo;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackForClassName = "*")
    @Override
    public ServiceMetaDO selectOne(String serviceCode) {
        return serviceMetaMapper.selectOne(serviceCode);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackForClassName = "*")
    @Override
    public int update(ServiceMetaDO serviceMetaDO) {
        return serviceMetaMapper.updateByPrimaryKeySelective(serviceMetaDO);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackForClassName = "*")
    @Override
    public int insert(ServiceMetaDO serviceMetaDO) {
        return serviceMetaMapper.insertSelective(serviceMetaDO);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackForClassName = "*")
    @Override
    public int delete(long id) {
        return serviceMetaMapper.deleteByPrimaryKey(id);
    }
}
