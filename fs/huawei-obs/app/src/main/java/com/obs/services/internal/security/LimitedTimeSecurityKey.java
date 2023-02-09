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

package com.obs.services.internal.security;

import java.util.Calendar;
import java.util.Date;

import com.obs.services.internal.utils.ServiceUtils;

public class LimitedTimeSecurityKey extends BasicSecurityKey {
    protected Date expiryDate;
    private static final long EXPIRY_SECONDS = 5 * 60L;
    private static final long WILL_SOON_EXPIRE_SECONDS = 2 * 60L;

    public LimitedTimeSecurityKey(String accessKey, String secretKey, String securityToken) {
        super(accessKey, secretKey, securityToken);
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.securityToken = securityToken;
        this.expiryDate = getUtcTime();
    }

    public LimitedTimeSecurityKey(String accessKey, String secretKey, String securityToken, Date expiryDate) {
        super(accessKey, secretKey, securityToken);
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.securityToken = securityToken;
        this.expiryDate = ServiceUtils.cloneDateIgnoreNull(expiryDate);
    }

    /**
     * about 2~5 minutes
     * 
     * @return
     */
    public boolean aboutToExpire() {
        return (expiryDate.getTime() - getUtcTime().getTime()) >= WILL_SOON_EXPIRE_SECONDS * 1000
                && (expiryDate.getTime() - getUtcTime().getTime()) < EXPIRY_SECONDS * 1000;
    }

    /**
     * less than 2 minutes
     * 
     * @return
     */
    public boolean willSoonExpire() {
        return expiryDate.before(getUtcTime())
                || (expiryDate.getTime() - getUtcTime().getTime()) < WILL_SOON_EXPIRE_SECONDS * 1000;
    }

    public static Date getUtcTime() {
        Calendar calendar = Calendar.getInstance();
        int offset = calendar.get(Calendar.ZONE_OFFSET);
        calendar.add(Calendar.MILLISECOND, -offset);
        return calendar.getTime();
    }

    public Date getExpiryDate() {
        return ServiceUtils.cloneDateIgnoreNull(this.expiryDate);
    }
}
