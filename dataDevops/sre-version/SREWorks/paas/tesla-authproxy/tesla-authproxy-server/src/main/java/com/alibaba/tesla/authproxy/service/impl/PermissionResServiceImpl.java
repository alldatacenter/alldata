package com.alibaba.tesla.authproxy.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.authproxy.ApplicationException;
import com.alibaba.tesla.authproxy.model.mapper.AppMapper;
import com.alibaba.tesla.authproxy.model.mapper.PermissionMetaMapper;
import com.alibaba.tesla.authproxy.model.mapper.PermissionResMapper;
import com.alibaba.tesla.authproxy.model.AppDO;
import com.alibaba.tesla.authproxy.model.PermissionMetaDO;
import com.alibaba.tesla.authproxy.model.PermissionResDO;
import com.alibaba.tesla.authproxy.service.AuthPolicy;
import com.alibaba.tesla.authproxy.service.PermissionResService;
import com.alibaba.tesla.common.utils.TeslaResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>Description: 应用信息服务实现 <／p>
 * <p>Copyright: Copyright (c) 2017<／p>
 * <p>Company: alibaba <／p>
 *
 * @author tandong.td@alibaba-inc.com
 * @version 1.0
 * @date 2017年5月3日
 */
@Service
@Slf4j
@Transactional(rollbackForClassName = "*")
public class PermissionResServiceImpl implements PermissionResService {

    @Autowired
    AuthPolicy authPolicy;

    @Autowired
    PermissionResMapper permissionResMapper;

    @Autowired
    PermissionMetaMapper permissionMetaMapper;

    @Autowired
    AppMapper appMapper;

    @Override
    public PermissionResDO getByResPath(String appId, String resPath) throws ApplicationException {
        try {
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("appId", appId);
            params.put("resPath", resPath);
            return permissionResMapper.getByAppAndResPath(params);
        } catch (Exception e) {
            log.error("查询权限资源关系失败", e);
            throw new ApplicationException(TeslaResult.FAILURE, "error.permissionres.get");
        }
    }

    @Override
    public PermissionResDO getByAppAndPath(String appId, String resPath) throws ApplicationException {
        try {
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("appId", appId);
            params.put("resPath", resPath);
            return permissionResMapper.getByAppAndPath(params);
        } catch (Exception e) {
            log.error("查询权限资源关系失败", e);
            throw new ApplicationException(TeslaResult.FAILURE, "error.permissionres.get");
        }
    }

    @Override
    public int insert(PermissionResDO permissionResDo) throws ApplicationException {
        try {
            return permissionResMapper.insert(permissionResDo);
        } catch (Exception e) {
            log.error("添加权限资源关系失败", e);
            throw new ApplicationException(TeslaResult.FAILURE, "error.permissionres.insert");
        }
    }

    @Override
    public int update(PermissionResDO permissionResDo) throws ApplicationException {
        try {
            return permissionResMapper.updateByPrimaryKey(permissionResDo);
        } catch (Exception e) {
            log.error("更新权限资源关系失败", e);
            throw new ApplicationException(TeslaResult.FAILURE, "error.permissionres.update");
        }
    }

    @Override
    public int delete(long id) throws ApplicationException {
        try {
            return permissionResMapper.delete(id);
        } catch (Exception e) {
            log.error("删除权限资源关系失败", e);
            throw new ApplicationException(TeslaResult.FAILURE, "error.permissionres.delete");
        }
    }

    @Override
    public int batchInsert(List<PermissionResDO> permissionResDOS) throws ApplicationException {
        try {
            return permissionResMapper.batchInsert(permissionResDOS);
        } catch (Exception e) {
            log.error("批量添加权限资源关系失败", e);
            throw new ApplicationException(TeslaResult.FAILURE, "error.permissionres.batchInsert");
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackForClassName = "*", isolation = Isolation.SERIALIZABLE)
    @Override
    public void init(String userId, String appId, String accessKey) throws ApplicationException{

        log.info("init permission meta:::userId={}, appId={}", userId, appId);
        AppDO appDo = appMapper.getByAppId(appId);
        if(null == appDo){
            throw new ApplicationException(500, "app " + appId + " is not exit in tesla-authproxy.");
        }
        /**
         * 查询所有权限元数据
         */
        List<PermissionMetaDO> permissionMetaDOList = permissionMetaMapper.select();
        if(null == permissionMetaDOList || permissionMetaDOList.size() == 0){
            throw new ApplicationException(500, "Permission meta is empty, can't init.");
        }
        /**
         * 将权限元数据插入权限资源关系表
         */
        List<PermissionResDO> permissionResDOS = new ArrayList<>();
        for(PermissionMetaDO permissionMetaDO : permissionMetaDOList){
            PermissionResDO old = permissionResMapper.selectOne(appId, permissionMetaDO.getPermissionCode());
            if(null == old){
                PermissionResDO permissionResDo = new PermissionResDO();
                permissionResDo.setAppId(appId);
                permissionResDo.setResPath(permissionMetaDO.getPermissionCode());
                permissionResDo.setPermissionId(authPolicy.getAuthServiceManager().createPermissionId(appId, permissionMetaDO));
                permissionResDo.setMemo(permissionMetaDO.getMemo());
                permissionResDOS.add(permissionResDo);
            }else{
                permissionResMapper.updateByPrimaryKeySelective(old);
            }
        }

        if(permissionResDOS.size() > 0){
            int count = permissionResMapper.batchInsert(permissionResDOS);
            if(count > 0){
                //将权限添加到ACL
                authPolicy.getAuthServiceManager().initPermission(userId, appId, permissionMetaDOList);
            }else{
                throw new ApplicationException(500, "init local db failed.");
            }
        }else {
            //将权限添加到ACL
            authPolicy.getAuthServiceManager().initPermission(userId, appId, permissionMetaDOList);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackForClassName = "*", isolation = Isolation.SERIALIZABLE)
    @Override
    public void enable(String userEmployeeId, String appId, String serviceCode, List<Long> permissionMetaIds) throws ApplicationException {
        log.info("enable permission meta:::appId={}, serviceCode={}, permissionMetaIds={}", appId, serviceCode, JSONObject.toJSONString(permissionMetaIds));
        AppDO appDo = appMapper.getByAppId(appId);
        if(null == appDo){
            log.error("app {} is not exits.", appId);
            throw new ApplicationException(500, "app " + appId + "is not exits");
        }

        List<PermissionMetaDO> permissionMetaDOS = new ArrayList<>();
        if(null == permissionMetaIds || permissionMetaIds.size() == 0){
            permissionMetaDOS = permissionMetaMapper.selectByServiceCode(serviceCode);
        }else{
            permissionMetaDOS = permissionMetaMapper.selectByIds(permissionMetaIds);
        }
        for(PermissionMetaDO permissionMetaDO : permissionMetaDOS){
            PermissionResDO permissionResDo = permissionResMapper.getByAppIdAndPermissionId(appId, permissionMetaDO.getPermissionCode());
            if(null != permissionResDo){
                log.error("permission code {} already exits in app {}, will update it.", permissionMetaDO.getPermissionCode(), appId);
                permissionResMapper.updateByPrimaryKeySelective(permissionResDo);
                continue;
            }
            PermissionResDO newPermissionRes = new PermissionResDO();
            newPermissionRes.setMemo(permissionMetaDO.getMemo());
            newPermissionRes.setResPath(permissionMetaDO.getPermissionCode());
            newPermissionRes.setPermissionId(authPolicy.getAuthServiceManager().createPermissionId(appId, permissionMetaDO));
            newPermissionRes.setAppId(appId);
            newPermissionRes.setResPath(permissionMetaDO.getPermissionCode());
            permissionResMapper.insert(newPermissionRes);
        }

        /**
         * 开启权限之后同步将权限初始化到ACL或OAM
         */
        try {
            authPolicy.getAuthServiceManager().initPermission(userEmployeeId, appId, permissionMetaDOS);
        } catch (Exception e) {
            log.error("Init permission to oam failed, userEmployeeId={}, appId={}, permissionMetaDOS={}, exception={}",
                userEmployeeId, appId, permissionMetaDOS, ExceptionUtils.getStackTrace(e));
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackForClassName = "*", isolation = Isolation.SERIALIZABLE)
    @Override
    public void disable(String userEmployeeId, String appId, String serviceCode, List<Long> permissionMetaIds) throws ApplicationException {
        log.info("disable permission meta:::appId={}, serviceCode={}, permissionMetaIds={}", appId, serviceCode, JSONObject.toJSONString(permissionMetaIds));
        AppDO appDo = appMapper.getByAppId(appId);
        if(null == appDo){
            log.error("app {} is not exits.", appId);
            throw new ApplicationException(500, "app " + appId + "is not exits");
        }
        List<PermissionMetaDO> permissionMetaDOS = new ArrayList<>();
        if(null == permissionMetaIds || permissionMetaIds.size() == 0){
            permissionMetaDOS = permissionMetaMapper.selectByServiceCode(serviceCode);
        }else{
            permissionMetaDOS = permissionMetaMapper.selectByIds(permissionMetaIds);
        }
        for(PermissionMetaDO permissionMetaDO : permissionMetaDOS){
            PermissionResDO permissionResDo = permissionResMapper.getByAppIdAndPermissionId(appId, permissionMetaDO.getPermissionCode());
            if(null == permissionResDo){
                log.error("permission {} is not exits in app {}, do nothing.", permissionMetaDO.getPermissionCode(), appId);
            }else{
                permissionResMapper.delete(permissionResDo.getId());
            }
        }

        /**
         * 关闭权限之后同步将权限从ACL或OAM中删除
         */
        permissionMetaDOS.parallelStream().forEach(permissionMeta -> {
            authPolicy.getAuthServiceManager().delPermission(userEmployeeId, appId,
                    authPolicy.getAuthServiceManager().createPermissionId(appId, permissionMeta));
        });
    }
}
