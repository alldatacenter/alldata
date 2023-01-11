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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RangerAuditConfig extends RangerConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(RangerAuditConfig.class);

    private final boolean initSuccess;

    public RangerAuditConfig() {
        this("standalone");
    }

    public RangerAuditConfig(String serviceName) {
        super();

        initSuccess = addAuditResources(serviceName);
    }

    public boolean isInitSuccess() { return initSuccess; }

    private boolean addAuditResources(String serviceName) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> addAuditResources()");
        }

        String defaultCfg = "ranger-" + serviceName + "-audit.xml";

        boolean ret = true;

        if (!addResourceIfReadable(defaultCfg)) {
            LOG.error("Could not add " + defaultCfg + " to RangerAuditConfig.");
            ret = false;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== addAuditResources(), result=" + ret);
        }

        return ret;
    }
}
