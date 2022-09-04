package com.alibaba.tesla.appmanager.server.repository.mapper;

import com.alibaba.tesla.appmanager.server.repository.domain.UserProfileDO;
import org.apache.ibatis.annotations.Param;

public interface UserProfileDOMapper {
    UserProfileDO queryByUserId(@Param("userId") String userId, @Param("namespaceId") String namespaceId, @Param("stageId") String stageId);

    int insert(UserProfileDO record);

    int update(UserProfileDO record);
}