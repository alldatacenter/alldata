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

package org.apache.ranger.common;

import org.apache.ranger.plugin.contextenricher.RangerUserStoreEnricher;
import org.apache.ranger.plugin.policyengine.RangerAccessRequest;
import org.apache.ranger.plugin.util.RangerUserStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RangerAdminUserStoreEnricher extends RangerUserStoreEnricher {
    private static final Logger LOG = LoggerFactory.getLogger(RangerAdminUserStoreEnricher.class);

    @Override
    public void init() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> RangerAdminUserStoreEnricher.init()");
        }

        super.init();

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== RangerAdminUserStoreEnricher.init()");
        }
    }

    @Override
    public void enrich(RangerAccessRequest request) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> RangerAdminUserStoreEnricher.enrich(" + request + ")");
        }

        refreshUserStoreIfNeeded();

        super.enrich(request);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== RangerAdminUserStoreEnricher.enrich(" + request + ")");
        }
    }

    private void refreshUserStoreIfNeeded() {
        RangerUserStore latestUserStore        = RangerUserStoreCache.getInstance().getRangerUserStore();
        Long            latestUserStoreVersion = latestUserStore != null ? latestUserStore.getUserStoreVersion() : null;

        if (latestUserStoreVersion != null && !latestUserStoreVersion.equals(getUserStoreVersion())) {
            synchronized (this) {
                if (!latestUserStoreVersion.equals(getUserStoreVersion())) {
                    LOG.info("RangerAdminUserStoreEnricher.refreshUserStoreIfNeeded(): currentVersion={}, newVersion={}", getUserStoreVersion(), latestUserStoreVersion);

                    setRangerUserStore(latestUserStore);
                }
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("RangerAdminUserStoreEnricher={serviceName=").append(serviceName).append("}");
        return sb.toString();
    }
}
