/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.manager.service.core;

import com.github.pagehelper.PageInfo;
import org.apache.inlong.manager.common.pojo.user.PasswordChangeRequest;
import org.apache.inlong.manager.common.pojo.user.UserDetailListVO;
import org.apache.inlong.manager.common.pojo.user.UserDetailPageRequest;
import org.apache.inlong.manager.common.pojo.user.UserInfo;
import org.apache.inlong.manager.dao.entity.UserEntity;

/**
 * User service interface
 */
public interface UserService {

    /**
     * Get user info by user name
     *
     * @param username username
     * @return user info
     */
    UserEntity getByName(String username);

    /**
     * Get user info by user id
     *
     * @param userId user id
     * @return user info
     */
    UserInfo getById(Integer userId);

    /**
     * Create user
     *
     * @param userInfo user info
     * @return whether succeed
     */
    boolean create(UserInfo userInfo);

    /**
     * Update user info
     *
     * @param userInfo user info
     * @param currentUser current user name
     * @return rows updated
     */
    int update(UserInfo userInfo, String currentUser);

    /**
     * Change password, need to check whether old password is correct
     *
     * @param request request
     * @return rows updated
     */
    Integer updatePassword(PasswordChangeRequest request);

    /**
     * Delete user by user id
     *
     * @param userId user id
     * @param currentUser current user name
     * @return whether succeed
     */
    Boolean delete(Integer userId, String currentUser);

    /**
     * List all users basic info by request condition
     *
     * @param request request
     * @return user info list
     */
    PageInfo<UserDetailListVO> list(UserDetailPageRequest request);

}
