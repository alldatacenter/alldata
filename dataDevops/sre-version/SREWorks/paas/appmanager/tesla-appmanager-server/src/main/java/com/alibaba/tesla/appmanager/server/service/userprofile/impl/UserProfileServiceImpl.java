package com.alibaba.tesla.appmanager.server.service.userprofile.impl;

import com.alibaba.tesla.appmanager.server.repository.domain.UserProfileDO;
import com.alibaba.tesla.appmanager.server.repository.mapper.UserProfileDOMapper;
import com.alibaba.tesla.appmanager.server.service.userprofile.UserProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author QianMo
 * @date 2021/03/15.
 */
@Service
public class UserProfileServiceImpl implements UserProfileService {

    @Autowired
    private UserProfileDOMapper userProfileDOMapper;

    @Override
    public UserProfileDO get(String userId, String namespaceId, String stageId) {
        return userProfileDOMapper.queryByUserId(userId, namespaceId, stageId);
    }

    @Override
    public int save(UserProfileDO userProfileDO) {
        int count = userProfileDOMapper.update(userProfileDO);
        if (count == 0) {
            count = userProfileDOMapper.insert(userProfileDO);
        }
        return count;
    }
}
