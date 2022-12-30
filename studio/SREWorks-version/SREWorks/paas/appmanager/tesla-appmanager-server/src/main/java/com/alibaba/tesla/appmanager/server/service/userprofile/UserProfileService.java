package com.alibaba.tesla.appmanager.server.service.userprofile;

import com.alibaba.tesla.appmanager.server.repository.domain.UserProfileDO;

/**
 * @author QianMo
 * @date 2021/03/15.
 */
public interface UserProfileService {
    /**
     * 根据UserID查询
     *
     * @param userId 查询条件
     * @return UserProfileDO
     */
    UserProfileDO get(String userId, String namespaceId, String stageId);

    /**
     * 更新用户的Profile信息
     *
     * @param userProfileDO
     * @return 影响记录条数
     */
    int save(UserProfileDO userProfileDO);
}
