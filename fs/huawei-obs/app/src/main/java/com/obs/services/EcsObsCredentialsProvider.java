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

package com.obs.services;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import com.obs.log.ILogger;
import com.obs.log.LoggerBuilder;
import com.obs.services.internal.security.EcsSecurityUtils;
import com.obs.services.internal.security.LimitedTimeSecurityKey;
import com.obs.services.internal.security.SecurityKey;
import com.obs.services.internal.security.SecurityKeyBean;
import com.obs.services.internal.utils.JSONChange;
import com.obs.services.model.ISecurityKey;

public class EcsObsCredentialsProvider implements IObsCredentialsProvider {
    private volatile LimitedTimeSecurityKey securityKey;
    private AtomicBoolean getNewKeyFlag = new AtomicBoolean(false);
    private static final ILogger ILOG = LoggerBuilder.getLogger(EcsObsCredentialsProvider.class);

    // default is -1, not retry
    private int maxRetryTimes = -1;

    public EcsObsCredentialsProvider() {
        this.maxRetryTimes = 3;
    }

    public EcsObsCredentialsProvider(int maxRetryTimes) {
        this.maxRetryTimes = maxRetryTimes;
    }

    @Override
    public void setSecurityKey(ISecurityKey securityKey) {
        throw new UnsupportedOperationException("EcsObsCredentialsProvider class does not support this method");
    }

    @Override
    public ISecurityKey getSecurityKey() {
        if (getNewKeyFlag.compareAndSet(false, true)) {
            try {
                if (securityKey == null || securityKey.willSoonExpire()) {
                    refresh(false);
                } else if (securityKey.aboutToExpire()) {
                    refresh(true);
                }
            } finally {
                getNewKeyFlag.set(false);
            }
        } else {
            if (ILOG.isDebugEnabled()) {
                ILOG.debug("some other thread is refreshing.");
            }
        }

        return securityKey;
    }

    /**
     * fefresh
     * 
     * @param ignoreException
     *            ignore exception
     */
    private void refresh(boolean ignoreException) {
        int times = 0;
        do {
            try {
                securityKey = getNewSecurityKey();
            } catch (IOException | RuntimeException e) {
                ILOG.warn("refresh new security key failed. times : " + times + "; maxRetryTimes is : " + maxRetryTimes
                        + "; ignoreException : " + ignoreException, e);

                if (times >= this.maxRetryTimes) {
                    ILOG.error("refresh new security key failed.", e);
                    if (!ignoreException) {
                        throw new IllegalArgumentException(e);
                    }
                }
            }
        } while (times++ < maxRetryTimes && maxRetryTimes > 0);
    }

    private LimitedTimeSecurityKey getNewSecurityKey() throws IOException, IllegalArgumentException {

        String content = EcsSecurityUtils.getSecurityKeyInfoWithDetail();
        SecurityKey securityInfo = (SecurityKey) JSONChange.jsonToObj(new SecurityKey(), content);

        if (securityInfo == null) {
            throw new IllegalArgumentException("Invalid securityKey : " + content);
        }

        Date expiryDate = null;
        SecurityKeyBean bean = securityInfo.getBean();
        try {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
            String strDate = bean.getExpiresDate();
            expiryDate = df.parse(strDate.substring(0, strDate.length() - 4));
        } catch (ParseException e) {
            throw new IllegalArgumentException("Date parse failed :" + e.getMessage());
        }

        StringBuilder strAccess = new StringBuilder();
        String accessKey = bean.getAccessKey();
        int length = accessKey.length();
        strAccess.append(accessKey.substring(0, length / 3));
        strAccess.append("******");
        strAccess.append(accessKey.substring(2 * length / 3, length - 1));
        ILOG.warn("the AccessKey : " + strAccess.toString() + "will expiry at UTC time : " + expiryDate);

        return new LimitedTimeSecurityKey(bean.getAccessKey(), bean.getSecretKey(), bean.getSecurityToken(),
                expiryDate);
    }
}
