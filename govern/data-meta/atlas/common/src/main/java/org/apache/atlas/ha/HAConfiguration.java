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

import org.apache.atlas.security.SecurityProperties;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A wrapper for getting configuration entries related to HighAvailability.
 */
public final class HAConfiguration {


    public static final String ATLAS_SERVER_ZK_ROOT_DEFAULT = "/apache_atlas";

    private HAConfiguration() {
    }

    public static final String ATLAS_SERVER_HA_PREFIX = "atlas.server.ha.";
    public static final String ZOOKEEPER_PREFIX = "zookeeper.";
    public static final String ATLAS_SERVER_HA_ZK_ROOT_KEY = ATLAS_SERVER_HA_PREFIX + ZOOKEEPER_PREFIX + "zkroot";
    public static final String ATLAS_SERVER_HA_ENABLED_KEY = ATLAS_SERVER_HA_PREFIX + "enabled";
    public static final String ATLAS_SERVER_ADDRESS_PREFIX = "atlas.server.address.";
    public static final String ATLAS_SERVER_IDS = "atlas.server.ids";
    public static final String HA_ZOOKEEPER_CONNECT = ATLAS_SERVER_HA_PREFIX + ZOOKEEPER_PREFIX + "connect";
    public static final int DEFAULT_ZOOKEEPER_CONNECT_SLEEPTIME_MILLIS = 1000;
    public static final String HA_ZOOKEEPER_RETRY_SLEEPTIME_MILLIS =
            ATLAS_SERVER_HA_PREFIX + ZOOKEEPER_PREFIX + "retry.sleeptime.ms";
    public static final String HA_ZOOKEEPER_NUM_RETRIES = ATLAS_SERVER_HA_PREFIX + ZOOKEEPER_PREFIX + "num.retries";
    public static final int DEFAULT_ZOOKEEPER_CONNECT_NUM_RETRIES = 3;
    public static final String HA_ZOOKEEPER_SESSION_TIMEOUT_MS =
            ATLAS_SERVER_HA_PREFIX + ZOOKEEPER_PREFIX + "session.timeout.ms";
    public static final int DEFAULT_ZOOKEEPER_SESSION_TIMEOUT_MILLIS = 20000;
    public static final String HA_ZOOKEEPER_ACL = ATLAS_SERVER_HA_PREFIX + ZOOKEEPER_PREFIX + "acl";
    public static final String HA_ZOOKEEPER_AUTH = ATLAS_SERVER_HA_PREFIX + ZOOKEEPER_PREFIX + "auth";

    /**
     * Return whether HA is enabled or not.
     * @param configuration underlying configuration instance
     * @return
     */
    public static boolean isHAEnabled(Configuration configuration) {
        boolean ret = false;

        if (configuration.containsKey(HAConfiguration.ATLAS_SERVER_HA_ENABLED_KEY)) {
            ret = configuration.getBoolean(ATLAS_SERVER_HA_ENABLED_KEY);
        } else {
            String[] ids = configuration.getStringArray(HAConfiguration.ATLAS_SERVER_IDS);

            ret = ids != null && ids.length > 1;
        }

        return ret;
    }

    /**
     * Get the web server address that a server instance with the passed ID is bound to.
     *
     * This method uses the property {@link SecurityProperties#TLS_ENABLED} to determine whether
     * the URL is http or https.
     *
     * @param configuration underlying configuration
     * @param serverId serverId whose host:port property is picked to build the web server address.
     * @return
     */
    public static String getBoundAddressForId(Configuration configuration, String serverId) {
        String hostPort = configuration.getString(ATLAS_SERVER_ADDRESS_PREFIX +serverId);
        boolean isSecure = configuration.getBoolean(SecurityProperties.TLS_ENABLED);
        String protocol = (isSecure) ? "https://" : "http://";
        return protocol + hostPort;
    }

    public static List<String> getServerInstances(Configuration configuration) {
        String[] serverIds = configuration.getStringArray(ATLAS_SERVER_IDS);
        List<String> serverInstances = new ArrayList<>(serverIds.length);
        for (String serverId : serverIds) {
            serverInstances.add(getBoundAddressForId(configuration, serverId));
        }
        return serverInstances;
    }

    /**
     * A collection of Zookeeper specific configuration that is used by High Availability code.
     */
    public static class ZookeeperProperties {
        private String connectString;
        private String zkRoot;
        private int retriesSleepTimeMillis;
        private int numRetries;
        private int sessionTimeout;
        private String acl;
        private String auth;

        public ZookeeperProperties(String connectString, String zkRoot, int retriesSleepTimeMillis, int numRetries,
                                   int sessionTimeout, String acl, String auth) {
            this.connectString = connectString;
            this.zkRoot = zkRoot;
            this.retriesSleepTimeMillis = retriesSleepTimeMillis;
            this.numRetries = numRetries;
            this.sessionTimeout = sessionTimeout;
            this.acl = acl;
            this.auth = auth;
        }

        public String getConnectString() {
            return connectString;
        }

        public int getRetriesSleepTimeMillis() {
            return retriesSleepTimeMillis;
        }

        public int getNumRetries() {
            return numRetries;
        }

        public int getSessionTimeout() {
            return sessionTimeout;
        }

        public String getAcl() {
            return acl;
        }

        public String getAuth() {
            return auth;
        }

        public String getZkRoot() {
            return zkRoot;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ZookeeperProperties that = (ZookeeperProperties) o;
            return retriesSleepTimeMillis == that.retriesSleepTimeMillis &&
                    numRetries == that.numRetries &&
                    sessionTimeout == that.sessionTimeout &&
                    Objects.equals(connectString, that.connectString) &&
                    Objects.equals(zkRoot, that.zkRoot) &&
                    Objects.equals(acl, that.acl) &&
                    Objects.equals(auth, that.auth);
        }

        @Override
        public int hashCode() {
            return Objects.hash(connectString, zkRoot, retriesSleepTimeMillis, numRetries, sessionTimeout, acl, auth);
        }

        public boolean hasAcl() {
            return getAcl()!=null;
        }

        public boolean hasAuth() {
            return getAuth()!=null;
        }
    }

    public static ZookeeperProperties getZookeeperProperties(Configuration configuration) {
        String[] zkServers;
        if (configuration.containsKey(HA_ZOOKEEPER_CONNECT)) {
            zkServers = configuration.getStringArray(HA_ZOOKEEPER_CONNECT);
        } else {
            zkServers = configuration.getStringArray("atlas.kafka." + ZOOKEEPER_PREFIX + "connect");
        }

        String zkRoot = configuration.getString(ATLAS_SERVER_HA_ZK_ROOT_KEY, ATLAS_SERVER_ZK_ROOT_DEFAULT);
        int retriesSleepTimeMillis = configuration.getInt(HA_ZOOKEEPER_RETRY_SLEEPTIME_MILLIS,
                DEFAULT_ZOOKEEPER_CONNECT_SLEEPTIME_MILLIS);

        int numRetries = configuration.getInt(HA_ZOOKEEPER_NUM_RETRIES, DEFAULT_ZOOKEEPER_CONNECT_NUM_RETRIES);

        int sessionTimeout = configuration.getInt(HA_ZOOKEEPER_SESSION_TIMEOUT_MS,
                DEFAULT_ZOOKEEPER_SESSION_TIMEOUT_MILLIS);

        String acl = configuration.getString(HA_ZOOKEEPER_ACL);
        String auth = configuration.getString(HA_ZOOKEEPER_AUTH);

        return new ZookeeperProperties(StringUtils.join(zkServers, ','),
                                       zkRoot,
                                       retriesSleepTimeMillis, numRetries,
                                       sessionTimeout, acl, auth);
    }
}
