package com.alibaba.tesla.appmanager.api.provider;

import com.alibaba.tesla.appmanager.domain.dto.UserProfileDTO;

/**
 * 应用元信息接口
 *
 * @author qianmo.zm@alibaba-inc.com
 */
public interface UserProfileProvider {

    /**
     * 查询用户profile
     */
    UserProfileDTO queryProfile(String userId, String namespaceId, String stageId);

    /**
     * 保存用户profile
     */
    int save(UserProfileDTO userProfileDTO);
}
