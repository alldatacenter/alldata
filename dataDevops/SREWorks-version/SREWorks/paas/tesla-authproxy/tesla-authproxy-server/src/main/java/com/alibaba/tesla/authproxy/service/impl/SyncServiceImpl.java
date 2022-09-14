package com.alibaba.tesla.authproxy.service.impl;

import com.alibaba.tesla.authproxy.Constants;
import com.alibaba.tesla.authproxy.model.AppExtDO;
import com.alibaba.tesla.authproxy.model.TeslaServiceExtAppDO;
import com.alibaba.tesla.authproxy.model.TeslaServiceUserDO;
import com.alibaba.tesla.authproxy.model.UserDO;
import com.alibaba.tesla.authproxy.model.example.TeslaServiceExtAppExample;
import com.alibaba.tesla.authproxy.model.example.TeslaServiceUserExample;
import com.alibaba.tesla.authproxy.model.mapper.AppExtMapper;
import com.alibaba.tesla.authproxy.model.teslamapper.TeslaServiceExtAppMapper;
import com.alibaba.tesla.authproxy.model.teslamapper.TeslaServiceUserMapper;
import com.alibaba.tesla.authproxy.model.mapper.UserMapper;
import com.alibaba.tesla.authproxy.service.SyncService;
import com.alibaba.tesla.authproxy.util.UserUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * users表数据与ta_user表数据同步
 *
 * @author cdx
 * @date 2019/10/10
 */
@Slf4j
@Service
public class SyncServiceImpl implements SyncService {

    @Autowired
    private TeslaServiceUserMapper teslaServiceUserMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private TeslaServiceExtAppMapper teslaServiceExtAppMapper;
    @Autowired
    private AppExtMapper appExtMapper;

    @Override
    public void syncAppByExample(TeslaServiceExtAppExample teslaServiceExtAppExample) {
        log.info("start sync apps.");
        List<TeslaServiceExtAppDO> teslaServiceExtAppDOS = teslaServiceExtAppMapper.selectByExample(teslaServiceExtAppExample);
        for (TeslaServiceExtAppDO teslaServiceExtAppDO : teslaServiceExtAppDOS) {
            AppExtDO appExtDo = appExtMapper.getByName(teslaServiceExtAppDO.getName());
            if (appExtDo != null) {
                appExtDo.setExtAppKey(teslaServiceExtAppDO.getSecret());
                appExtDo.setGmtModified(new Date());
                log.info("update app:{}", appExtDo);
                appExtMapper.updateByPrimaryKey(appExtDo);
            } else {
                appExtDo = new AppExtDO();
                appExtDo.setExtAppName(teslaServiceExtAppDO.getName());
                appExtDo.setExtAppKey(teslaServiceExtAppDO.getSecret());
                appExtDo.setGmtCreate(new Date());
                appExtDo.setGmtModified(teslaServiceExtAppDO.getModifytime());
                log.info("insert app:{}", appExtDo);
                appExtMapper.insert(appExtDo);
            }
        }
        if (teslaServiceExtAppDOS != null) {
            log.info("sync app success：{}", teslaServiceExtAppDOS.size());
        } else {
            log.info("sync app success：0");
        }
    }

    @Override
    public void syncUserByExample(TeslaServiceUserExample example) {
        log.info("start sync users.");
        List<TeslaServiceUserDO> teslaServiceUsers = teslaServiceUserMapper.selectByExample(example);
        for (TeslaServiceUserDO teslaServiceUser : teslaServiceUsers) {
            /**
             * 兼容employeeId为空的场景
             */
            if (StringUtils.isBlank(teslaServiceUser.getEmployeeId())) {
                UserDO userDo = userMapper.getByLoginName(teslaServiceUser.getUsername());
                if (userDo != null) {
                    userDo.setSecretKey(teslaServiceUser.getSecretkey());
                    userDo.setGmtModified(new Date());
                    log.info("update user:{}", userDo);
                    userMapper.updateByPrimaryKey(userDo);
                } else {
                    insertTeslaUser(teslaServiceUser);
                }
            } else {
                UserDO userDo = userMapper.getByEmpId(teslaServiceUser.getEmployeeId());
                if (userDo != null) {
                    userDo.setLoginName(teslaServiceUser.getUsername());
                    userDo.setSecretKey(teslaServiceUser.getSecretkey());
                    userDo.setGmtModified(new Date());
                    log.info("update user:{}", userDo);
                    userMapper.updateByPrimaryKey(userDo);
                } else {
                    insertTeslaUser(teslaServiceUser);
                }
            }
        }
        if (teslaServiceUsers != null) {
            log.info("sync user success：{}", teslaServiceUsers.size());
        } else {
            log.info("sync user success：0");
        }
    }

    private void insertTeslaUser(TeslaServiceUserDO teslaServiceUserDO) {
        UserDO userDo = new UserDO();
        userDo.setEmpId(teslaServiceUserDO.getEmployeeId());
        String bucUserId = teslaServiceUserDO.getBucUserId();
        if (!StringUtils.isBlank(bucUserId)) {
            if (bucUserId.endsWith(".0")) {
                bucUserId = bucUserId.substring(0, bucUserId.indexOf("."));
            }
            userDo.setBucId(Long.parseLong(bucUserId));
        }
        userDo.setTenantId(Constants.DEFAULT_TENANT_ID);
        userDo.setLoginName(teslaServiceUserDO.getUsername());
        userDo.setNickName(teslaServiceUserDO.getNickname());
        userDo.setEmail(teslaServiceUserDO.getEmail());
        userDo.setPhone(teslaServiceUserDO.getMobilephone());
        userDo.setAliww(teslaServiceUserDO.getAliww());
        userDo.setGmtCreate(new Date());
        userDo.setIsLocked((byte)(userDo.getIsLocked() == 0 ? 1 : 0));
        userDo.setSecretKey(teslaServiceUserDO.getSecretkey());
        userDo.setUserId(UserUtil.getUserId(userDo));
        log.info("insert user:{}", userDo);
        userMapper.insert(userDo);
    }

}
