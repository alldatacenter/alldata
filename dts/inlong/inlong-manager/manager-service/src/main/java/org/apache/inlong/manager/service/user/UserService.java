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

package org.apache.inlong.manager.service.user;

import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.user.UserInfo;
import org.apache.inlong.manager.pojo.user.UserLoginRequest;
import org.apache.inlong.manager.pojo.user.UserRequest;

/**
 * User service interface
 */
public interface UserService {

    /**
     * Save user info
     *
     * @param request user info request
     * @return user id after saving
     */
    Integer save(UserRequest request, String currentUser);

    /**
     * Get user info by user id
     *
     * @param id user id
     * @return user info
     */
    UserInfo getById(Integer id, String currentUser);

    /**
     * Get user by name
     *
     * @param name username
     * @return user info
     */
    UserInfo getByName(String name);

    /**
     * List all users basic info by request condition
     *
     * @param request request
     * @return user info list
     */
    PageResult<UserInfo> list(UserRequest request);

    /**
     * Update user info
     *
     * @param request user info request
     * @param currentUser current user name
     * @return user id
     */
    Integer update(UserRequest request, String currentUser);

    /**
     * Delete user by id
     *
     * @param userId user id
     * @param currentUser current user name
     * @return whether succeed
     */
    Boolean delete(Integer userId, String currentUser);

    /**
     * Account password login
     */
    void login(UserLoginRequest req);

    /**
     * Check the given user is the admin or is one of the in charges.
     *
     * @param inCharges incharge list
     * @param user current user name
     * @param errMsg error message
     */
    void checkUser(String inCharges, String user, String errMsg);

}
