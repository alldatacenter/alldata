/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ranger.authorization.hadoop.config;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyStore;

public class RangerAdminConfig extends RangerConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(RangerAdminConfig.class);

    private static volatile RangerAdminConfig sInstance = null;
    private final boolean isFipsEnabled;

    public static RangerAdminConfig getInstance() {
        RangerAdminConfig ret = RangerAdminConfig.sInstance;

        if (ret == null) {
            synchronized (RangerAdminConfig.class) {
                ret = RangerAdminConfig.sInstance;

                if (ret == null) {
                    ret = RangerAdminConfig.sInstance = new RangerAdminConfig();
                }
            }
        }

        return ret;
    }

    private RangerAdminConfig() {
        super();
        addAdminResources();
        String storeType = get(RangerConfigConstants.RANGER_KEYSTORE_TYPE, KeyStore.getDefaultType());
        isFipsEnabled = StringUtils.equalsIgnoreCase("bcfks", storeType) ? true : false;
        
    }

    private boolean addAdminResources() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> addAdminResources()");
        }

        String defaultCfg = "ranger-admin-default-site.xml";
        String addlCfg    = "ranger-admin-site.xml";
        String coreCfg    = "core-site.xml";

        boolean ret = true;

        if (!addResourceIfReadable(defaultCfg)) {
            ret = false;
        }

        if (!addResourceIfReadable(addlCfg)) {
            ret = false;
        }

        if (!addResourceIfReadable(coreCfg)){
            ret = false;
        }

        if (! ret) {
            LOG.error("Could not add ranger-admin resources to RangerAdminConfig.");
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== addAdminResources(), result=" + ret);
        }

        return ret;
    }

    public boolean isFipsEnabled() {
        return isFipsEnabled;
    }
}
