package com.alibaba.tesla.authproxy.service.impl;

import com.alibaba.tesla.authproxy.model.TeslaServiceUserDO;
import com.alibaba.tesla.authproxy.model.example.TeslaServiceUserExample;
import com.alibaba.tesla.authproxy.model.example.TeslaServiceUserExample.Criteria;
import com.alibaba.tesla.authproxy.service.UserService;
import com.alibaba.tesla.authproxy.model.teslamapper.TeslaServiceUserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * @author wb-cdx417019
 */
@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Autowired
    private TeslaServiceUserMapper teslaServiceUserMapper;

    @Override
    public int saveUser(TeslaServiceUserDO teslaServiceUser) {
        TeslaServiceUserExample example = new TeslaServiceUserExample();
        Criteria criteria = example.createCriteria();
        criteria.andUsernameEqualTo(teslaServiceUser.getUsername());
        List<TeslaServiceUserDO> teslaServiceUsers = teslaServiceUserMapper.selectByExample(example);
        if (teslaServiceUsers == null || teslaServiceUsers.size() == 0) {
            return teslaServiceUserMapper.insert(teslaServiceUser);
        } else {
            TeslaServiceUserDO oldTeslaServiceUser = teslaServiceUsers.get(0);
            oldTeslaServiceUser.setAliww(teslaServiceUser.getAliww());
            oldTeslaServiceUser.setBucUserId(teslaServiceUser.getBucUserId());
            oldTeslaServiceUser.setEmail(teslaServiceUser.getEmail());
            oldTeslaServiceUser.setEmployeeId(teslaServiceUser.getEmployeeId());
            oldTeslaServiceUser.setLogintime(new Date());
            oldTeslaServiceUser.setUsername(teslaServiceUser.getUsername());
            oldTeslaServiceUser.setNickname(teslaServiceUser.getNickname());
            oldTeslaServiceUser.setTelephone(teslaServiceUser.getTelephone());
            return teslaServiceUserMapper.updateByPrimaryKey(oldTeslaServiceUser);
        }
    }
}
