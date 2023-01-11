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

package org.apache.ranger.plugin.contextenricher;

import org.apache.commons.lang.StringUtils;
import org.apache.ranger.admin.client.RangerAdminClient;
import org.apache.ranger.authorization.hadoop.config.RangerPluginConfig;
import org.apache.ranger.plugin.policyengine.RangerPluginContext;
import org.apache.ranger.plugin.util.RangerUserStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.ClosedByInterruptException;
import java.util.Map;

public class RangerAdminUserStoreRetriever extends RangerUserStoreRetriever {
    private static final Logger LOG = LoggerFactory.getLogger(RangerAdminUserStoreRetriever.class);

    private RangerAdminClient adminClient;

    @Override
    public void init(Map<String, String> options) {

        if (StringUtils.isNotBlank(serviceName) && serviceDef != null && StringUtils.isNotBlank(appId)) {
            RangerPluginConfig pluginConfig = super.pluginConfig;

            if (pluginConfig == null) {
                pluginConfig = new RangerPluginConfig(serviceDef.getName(), serviceName, appId, null, null, null);
            }

            RangerPluginContext pluginContext = getPluginContext();
            RangerAdminClient	rangerAdmin  = pluginContext.getAdminClient();
            this.adminClient                 = (rangerAdmin != null) ? rangerAdmin : pluginContext.createAdminClient(pluginConfig);

        } else {
            LOG.error("FATAL: Cannot find service/serviceDef to use for retrieving userstore. Will NOT be able to retrieve userstore.");
        }
    }

    @Override
    public RangerUserStore retrieveUserStoreInfo(long lastKnownVersion, long lastActivationTimeInMillis) throws Exception {

        RangerUserStore rangerUserStore = null;

        if (adminClient != null) {
            try {
                rangerUserStore = adminClient.getUserStoreIfUpdated(lastKnownVersion, lastActivationTimeInMillis);
            } catch (ClosedByInterruptException closedByInterruptException) {
                LOG.error("UserStore-retriever thread was interrupted while blocked on I/O");
                throw new InterruptedException();
            } catch (Exception e) {
                LOG.error("UserStore-retriever encounterd exception, exception=", e);
                LOG.error("Returning null userstore info");
            }
        }
        return rangerUserStore;
    }

}

