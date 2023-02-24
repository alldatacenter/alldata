/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.ha;

import org.apache.atlas.AtlasConstants;
import org.apache.atlas.AtlasException;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.net.NetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class AtlasServerIdSelector {

    private static final Logger LOG = LoggerFactory.getLogger(AtlasServerIdSelector.class);

    /**
     * Return the ID corresponding to this Atlas instance.
     *
     * The match is done by looking for an ID configured in {@link HAConfiguration#ATLAS_SERVER_IDS} key
     * that has a host:port entry for the key {@link HAConfiguration#ATLAS_SERVER_ADDRESS_PREFIX}+ID where
     * the host is a local IP address and port is set in the system property
     * {@link AtlasConstants#SYSTEM_PROPERTY_APP_PORT}.
     *
     * @param configuration
     * @return
     * @throws AtlasException if no ID is found that maps to a local IP Address or port
     */
    public static String selectServerId(Configuration configuration) throws AtlasException {
        // ids are already trimmed by this method
        String[] ids = configuration.getStringArray(HAConfiguration.ATLAS_SERVER_IDS);
        String matchingServerId = null;
        int appPort = Integer.parseInt(System.getProperty(AtlasConstants.SYSTEM_PROPERTY_APP_PORT));
        for (String id : ids) {
            String hostPort = configuration.getString(HAConfiguration.ATLAS_SERVER_ADDRESS_PREFIX +id);
            if (!StringUtils.isEmpty(hostPort)) {
                InetSocketAddress socketAddress;
                try {
                    socketAddress = NetUtils.createSocketAddr(hostPort);
                } catch (Exception e) {
                    LOG.warn("Exception while trying to get socket address for {}", hostPort, e);
                    continue;
                }
                if (!socketAddress.isUnresolved()
                        && NetUtils.isLocalAddress(socketAddress.getAddress())
                        && appPort == socketAddress.getPort()) {
                    LOG.info("Found matched server id {} with host port: {}", id, hostPort);
                    matchingServerId = id;
                    break;
                }
            } else {
                LOG.info("Could not find matching address entry for id: {}", id);
            }
        }
        if (matchingServerId == null) {
            String msg = String.format("Could not find server id for this instance. " +
                            "Unable to find IDs matching any local host and port binding among %s",
                    StringUtils.join(ids, ","));
            throw new AtlasException(msg);
        }
        return matchingServerId;
    }

}
