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

package org.apache.atlas.web.service;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import org.apache.atlas.ApplicationProperties;
import org.apache.atlas.AtlasException;
import org.apache.atlas.ha.HAConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.curator.framework.AuthInfo;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.ACLProvider;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.zookeeper.data.ACL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Singleton;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * A factory to create objects related to Curator.
 *
 * Allows for stubbing in tests.
 */
@Singleton
@Component
public class CuratorFactory {

    private static final Logger LOG = LoggerFactory.getLogger(CuratorFactory.class);

    public static final String APACHE_ATLAS_LEADER_ELECTOR_PATH = "/leader_elector_path";
    public static final String SASL_SCHEME = "sasl";
    public static final String WORLD_SCHEME = "world";
    public static final String ANYONE_ID = "anyone";
    public static final String AUTH_SCHEME = "auth";
    public static final String DIGEST_SCHEME = "digest";
    public static final String IP_SCHEME = "ip";
    public static final String SETUP_LOCK = "/setup_lock";

    private final Configuration configuration;
    private CuratorFramework curatorFramework;

    /**
     * Initializes the {@link CuratorFramework} that is used for all interaction with Zookeeper.
     * @throws AtlasException
     */
    public CuratorFactory() throws AtlasException {
        this(ApplicationProperties.get());
    }

    public CuratorFactory(Configuration configuration) {
        this.configuration = configuration;
        initializeCuratorFramework();
    }

    @VisibleForTesting
    protected void initializeCuratorFramework() {
        HAConfiguration.ZookeeperProperties zookeeperProperties =
                HAConfiguration.getZookeeperProperties(configuration);
        CuratorFrameworkFactory.Builder builder = getBuilder(zookeeperProperties);
        enhanceBuilderWithSecurityParameters(zookeeperProperties, builder);
        curatorFramework = builder.build();
        curatorFramework.start();
    }

    @VisibleForTesting
    void enhanceBuilderWithSecurityParameters(HAConfiguration.ZookeeperProperties zookeeperProperties,
                                              CuratorFrameworkFactory.Builder builder) {

        ACLProvider aclProvider = getAclProvider(zookeeperProperties);

        AuthInfo authInfo = null;
        if (zookeeperProperties.hasAuth()) {
            authInfo = AtlasZookeeperSecurityProperties.parseAuth(zookeeperProperties.getAuth());
        }

        if (aclProvider != null) {
            LOG.info("Setting up acl provider.");
            builder.aclProvider(aclProvider);
            if (authInfo != null) {
                byte[] auth = authInfo.getAuth();
                LOG.info("Setting up auth provider with scheme: {} and id: {}", authInfo.getScheme(),
                        getIdForLogging(authInfo.getScheme(), new String(auth, Charsets.UTF_8)));
                builder.authorization(authInfo.getScheme(), auth);
            }
        }
    }

    private String getCurrentUser() {
        try {
            return UserGroupInformation.getCurrentUser().getUserName();
        } catch (IOException ioe) {
            return "unknown";
        }
    }

    private ACLProvider getAclProvider(HAConfiguration.ZookeeperProperties zookeeperProperties) {
        ACLProvider aclProvider = null;
        if (zookeeperProperties.hasAcl()) {
            final ACL acl = AtlasZookeeperSecurityProperties.parseAcl(zookeeperProperties.getAcl());
            LOG.info("Setting ACL for id {} with scheme {} and perms {}.",
                    getIdForLogging(acl.getId().getScheme(), acl.getId().getId()),
                    acl.getId().getScheme(), acl.getPerms());
            LOG.info("Current logged in user: {}", getCurrentUser());
            final List<ACL> acls = Arrays.asList(acl);
            aclProvider = new ACLProvider() {
                @Override
                public List<ACL> getDefaultAcl() {
                    return acls;
                }

                @Override
                public List<ACL> getAclForPath(String path) {
                    return acls;
                }
            };
        }
        return aclProvider;
    }

    private String getIdForLogging(String scheme, String id) {
        if (scheme.equalsIgnoreCase(SASL_SCHEME) ||
                scheme.equalsIgnoreCase(IP_SCHEME)) {
            return id;
        } else if (scheme.equalsIgnoreCase(WORLD_SCHEME)) {
            return ANYONE_ID;
        } else if (scheme.equalsIgnoreCase(AUTH_SCHEME) ||
                scheme.equalsIgnoreCase(DIGEST_SCHEME)) {
            return id.split(":")[0];
        }
        return "unknown";
    }

    private CuratorFrameworkFactory.Builder getBuilder(HAConfiguration.ZookeeperProperties zookeeperProperties) {
        return CuratorFrameworkFactory.builder().
                connectString(zookeeperProperties.getConnectString()).
                sessionTimeoutMs(zookeeperProperties.getSessionTimeout()).
                retryPolicy(new ExponentialBackoffRetry(
                        zookeeperProperties.getRetriesSleepTimeMillis(), zookeeperProperties.getNumRetries()));
    }

    /**
     * Cleanup resources related to {@link CuratorFramework}.
     *
     * After this call, no further calls to any curator objects should be done.
     */
    public void close() {
        curatorFramework.close();
    }

    /**
     * Returns a pre-created instance of {@link CuratorFramework}.
     *
     * This method can be called any number of times to access the {@link CuratorFramework} used in the
     * application.
     * @return
     */
    public CuratorFramework clientInstance() {
        return curatorFramework;
    }

    /**
     * Create a new instance {@link LeaderLatch}
     * @param serverId the ID used to register this instance with curator.
     *                 This ID should typically be obtained using
     *                 {@link org.apache.atlas.ha.AtlasServerIdSelector#selectServerId(Configuration)}
     * @param zkRoot the root znode under which the leader latch node is added.
     * @return
     */
    public LeaderLatch leaderLatchInstance(String serverId, String zkRoot) {
        return new LeaderLatch(curatorFramework, zkRoot+APACHE_ATLAS_LEADER_ELECTOR_PATH, serverId);
    }

    public InterProcessMutex lockInstance(String zkRoot) {
        return new InterProcessMutex(curatorFramework, zkRoot+ SETUP_LOCK);
    }
}
