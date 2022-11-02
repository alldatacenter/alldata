package com.alibaba.tesla.appmanager.server.provider.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.api.provider.UserProfileProvider;
import com.alibaba.tesla.appmanager.domain.dto.UserProfileDTO;
import com.alibaba.tesla.appmanager.server.repository.domain.UserProfileDO;
import com.alibaba.tesla.appmanager.server.service.userprofile.UserProfileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * 应用元信息接口
 *
 * @author qianmo.zm@alibaba-inc.com
 */
@Slf4j
@Service
public class UserProfileProviderImpl implements UserProfileProvider {

    @Autowired
    private UserProfileService userProfileService;

    @Override
    public UserProfileDTO queryProfile(String userId, String namespaceId, String stageId) {
        UserProfileDO userProfileDO = userProfileService.get(userId, namespaceId, stageId);
        JSONObject profile = new JSONObject();
        if (Objects.nonNull(userProfileDO)) {
            profile = userProfileDO.getJsonByProfile();
        }

        return UserProfileDTO.builder().userId(userId).profile(profile).build();
    }

    @Override
    public int save(UserProfileDTO userProfileDTO) {
        return userProfileService.save(UserProfileDO.builder().userId(userProfileDTO.getUserId())
                .profile(userProfileDTO.getProfile().toJSONString())
                .namespaceId(userProfileDTO.getNamespaceId())
                .stageId(userProfileDTO.getStageId()).build());
    }
}
