/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.obs.services.model;

import com.obs.services.internal.Constants;

/**
 * Type of the user group
 *
 */
public enum GroupGranteeEnum {
    /**
     * Anonymous user group, indicating all users
     */
    ALL_USERS,

    /**
     * OBS authorized user group, indicating all users who own OBS accounts
     */
    @Deprecated AUTHENTICATED_USERS,

    /**
     * Log delivery group, indicating common users who can configure access logs
     */
    LOG_DELIVERY;

    public String getCode() {
        return this.name();
    }

    public static GroupGranteeEnum getValueFromCode(String code) {
        if ("Everyone".equals(code) || Constants.ALL_USERS_URI.equals(code)) {
            return ALL_USERS;
        } else if (Constants.AUTHENTICATED_USERS_URI.equals(code)) {
            return AUTHENTICATED_USERS;
        } else if (Constants.LOG_DELIVERY_URI.equals(code)) {
            return LOG_DELIVERY;
        }
        return null;
    }
}
