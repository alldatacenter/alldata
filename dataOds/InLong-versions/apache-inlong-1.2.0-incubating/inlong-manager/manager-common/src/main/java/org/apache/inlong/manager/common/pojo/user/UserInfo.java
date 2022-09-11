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

package org.apache.inlong.manager.common.pojo.user;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.inlong.manager.common.enums.UserTypeEnum;
import org.apache.inlong.manager.common.util.Preconditions;

/**
 * User info, including username, password, etc.
 */
@Data
@ApiModel("User info")
public class UserInfo {

    private Integer id;

    @ApiModelProperty("type: 0 - manager, 1 - operator")
    private Integer type;

    @ApiModelProperty("username")
    private String username;

    @ApiModelProperty("password")
    private String password;

    @ApiModelProperty("valid days")
    private Integer validDays;

    public void checkValid() {
        Preconditions.checkNotEmpty(username, "username should not be empty");
        Preconditions.checkNotEmpty(password, "password should not be empty");
        Preconditions.checkNotNull(validDays, "valid days should not be empty");
        UserTypeEnum userType = UserTypeEnum.parse(type);
        Preconditions.checkNotNull(userType, "user type incorrect");
    }
}
