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
package org.apache.ranger.services.nifi;

import org.apache.ranger.plugin.service.RangerBaseService;
import org.apache.ranger.plugin.service.ResourceLookupContext;
import org.apache.ranger.services.nifi.client.NiFiClient;
import org.apache.ranger.services.nifi.client.NiFiConnectionMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.List;

/**
 * RangerService for Apache NiFi.
 */
public class RangerServiceNiFi extends RangerBaseService {

    private static final Logger LOG = LoggerFactory.getLogger(RangerServiceNiFi.class);

    @Override
    public HashMap<String, Object> validateConfig() throws Exception {
        HashMap<String, Object> ret = new HashMap<>();
        String serviceName = getServiceName();

        if (LOG.isDebugEnabled()) {
            LOG.debug("==> RangerServiceNiFi.validateConfig Service: (" + serviceName + " )");
        }

        if (configs != null) {
            try {
                ret = NiFiConnectionMgr.connectionTest(serviceName, configs);
            } catch (Exception e) {
                LOG.error("<== RangerServiceNiFi.validateConfig Error:", e);
                throw e;
            }
        } else {
            throw new IllegalStateException("No Configuration found");
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== RangerServiceNiFi.validateConfig Response : (" + ret + " )");
        }

        return ret;
    }

    @Override
    public List<String> lookupResource(ResourceLookupContext context) throws Exception {
        final NiFiClient client = NiFiConnectionMgr.getNiFiClient(serviceName, configs);
        return client.getResources(context);
    }

}
