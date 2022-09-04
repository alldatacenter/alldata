package com.alibaba.tesla.authproxy.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.authproxy.ApplicationException;
import com.alibaba.tesla.authproxy.model.mapper.AppMapper;
import com.alibaba.tesla.authproxy.model.mapper.PermissionMetaMapper;
import com.alibaba.tesla.authproxy.model.mapper.PermissionResMapper;
import com.alibaba.tesla.authproxy.model.mapper.UserMapper;
import com.alibaba.tesla.authproxy.model.AppDO;
import com.alibaba.tesla.authproxy.model.PermissionMetaDO;
import com.alibaba.tesla.authproxy.model.PermissionResDO;
import com.alibaba.tesla.authproxy.service.AuthPolicy;
import com.alibaba.tesla.authproxy.service.PermissionMetaService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 权限元数据服务实现
 * @author tandong.td@alibaba-inc.com
 */
@Service
@Slf4j
@Transactional(rollbackForClassName = "*")
public class PermissionMetaServiceImpl implements PermissionMetaService {

    @Autowired
    PermissionMetaMapper permissionMetaMapper;

    @Autowired
    AuthPolicy authPolicy;

    @Autowired
    AppMapper appMapper;

    @Autowired
    PermissionResMapper permissionResMapper;

    @Autowired
    UserMapper userMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackForClassName = "*", isolation = Isolation.SERIALIZABLE)
    @Override
    public void createAndInit(String userId, String appId, PermissionMetaDO permissionMetaDO) {
        log.info("createAndInit permission meta:::userId={}, appId={}, permissionMetaDO={}", userId, appId, JSONObject.toJSONString(permissionMetaDO));
        AppDO appDo = appMapper.getByAppId(appId);
        if(null == appDo){
            throw new ApplicationException(500, "app " + appId + " is not exit in tesla-authproxy.");
        }
        PermissionMetaDO pmeta = permissionMetaMapper.selectEnableOne(permissionMetaDO.getPermissionCode());
        if(null != pmeta){
            throw new ApplicationException(500, "permission meta " + permissionMetaDO.getPermissionCode() + "already exits.");
        }
        permissionMetaMapper.insertSelective(permissionMetaDO);

        PermissionResDO permissionResDo = new PermissionResDO();
        permissionResDo.setResPath(permissionMetaDO.getPermissionCode());
        permissionResDo.setAppId(appId);
        permissionResDo.setPermissionId(authPolicy.getAuthServiceManager().createPermissionId(appId, permissionMetaDO));
        permissionResDo.setMemo(permissionMetaDO.getMemo());
        permissionResMapper.insert(permissionResDo);
        //将权限添加到ACL或OAM
        List<PermissionMetaDO> permissionMetaDOList = new ArrayList<>();
        permissionMetaDOList.add(permissionMetaDO);
        authPolicy.getAuthServiceManager().initPermission(userId, appId, permissionMetaDOList);
    }

    @Override
    public PermissionMetaDO selectOne(String permissionCode) {
        return permissionMetaMapper.selectOne(permissionCode);
    }

    @Override
    public PageInfo<PermissionMetaDO> select(String appId, String serviceCode, int page, int size) {
        PageHelper.startPage(page, size);
        List<PermissionMetaDO> tasks = permissionMetaMapper.selectByAppIdAndServiceCode(appId, serviceCode);
        PageInfo<PermissionMetaDO> pageInfo = new PageInfo<>(tasks);
        return pageInfo;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackForClassName = "*")
    @Override
    public int update(PermissionMetaDO permissionMetaDO) {
        return permissionMetaMapper.updateByPrimaryKeySelective(permissionMetaDO);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackForClassName = "*")
    @Override
    public int insert(PermissionMetaDO permissionMetaDO) {
        return permissionMetaMapper.insertSelective(permissionMetaDO);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackForClassName = "*", isolation = Isolation.SERIALIZABLE)
    @Override
    public void delete(String appId, String userEmployeeId, String permissionCode, boolean delAclOam) {
        PermissionMetaDO permissionMetaDO = permissionMetaMapper.selectOne(permissionCode);
        if(null == permissionMetaDO){
            return;
            //throw new ApplicationException(500, "permission code not exits.");
        }
        permissionMetaDO.setIsEnable(0);
        int ret1 = permissionMetaMapper.updateByPrimaryKeySelective(permissionMetaDO);
        log.info("delete permission meta return {}, appId={}, userId={}, permissionCode={}", ret1, appId, userEmployeeId, permissionCode);
        int ret2 = permissionResMapper.deleteByAppIdAndPermissionId(appId, authPolicy.getAuthServiceManager().createPermissionId(appId, permissionMetaDO));
        log.info("delete permission res return {}, appId={}, userId={}, permissionCode={}", ret2, appId, userEmployeeId, permissionCode);
        if(delAclOam){
            authPolicy.getAuthServiceManager().delPermission(userEmployeeId, appId,
                    authPolicy.getAuthServiceManager().createPermissionId(appId, permissionMetaDO));
        }
    }
}
