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

package org.apache.atlas.web.setup;

import com.google.common.base.Charsets;
import org.apache.atlas.ApplicationProperties;
import org.apache.atlas.AtlasConstants;
import org.apache.atlas.AtlasException;
import org.apache.atlas.ha.AtlasServerIdSelector;
import org.apache.atlas.ha.HAConfiguration;
import org.apache.atlas.setup.SetupException;
import org.apache.atlas.setup.SetupStep;
import org.apache.atlas.web.service.AtlasZookeeperSecurityProperties;
import org.apache.atlas.web.service.CuratorFactory;
import org.apache.commons.configuration.Configuration;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Singleton
@Component
@Conditional(SetupSteps.SetupRequired.class)
public class SetupSteps {
    private static final Logger LOG = LoggerFactory.getLogger(SetupSteps.class);
    public static final String SETUP_IN_PROGRESS_NODE = "/setup_in_progress";

    private final Set<SetupStep> setupSteps;
    private final Configuration configuration;
    private CuratorFactory curatorFactory;

    @Inject
    public SetupSteps(Set<SetupStep> steps, CuratorFactory curatorFactory, Configuration configuration) {
        setupSteps = steps;
        this.curatorFactory = curatorFactory;
        this.configuration = configuration;
    }

    /**
     * Call each registered {@link SetupStep} one after the other.
     * @throws SetupException Thrown with any error during running setup, including Zookeeper interactions, and
     *                          individual failures in the {@link SetupStep}.
     */
    @PostConstruct
    public void runSetup() throws SetupException {
        HAConfiguration.ZookeeperProperties zookeeperProperties = HAConfiguration.getZookeeperProperties(configuration);
        InterProcessMutex lock = curatorFactory.lockInstance(zookeeperProperties.getZkRoot());
        try {
            LOG.info("Trying to acquire lock for running setup.");
            lock.acquire();
            LOG.info("Acquired lock for running setup.");
            handleSetupInProgress(configuration, zookeeperProperties);
            for (SetupStep step : setupSteps) {
                LOG.info("Running setup step: {}", step);
                step.run();
            }
            clearSetupInProgress(zookeeperProperties);
        } catch (SetupException se) {
            LOG.error("Got setup exception while trying to setup", se);
            throw se;
        } catch (Throwable e) {
            LOG.error("Error running setup steps", e);
            throw new SetupException("Error running setup steps", e);
        } finally {
            releaseLock(lock);
            curatorFactory.close();
        }
    }

    private void handleSetupInProgress(Configuration configuration, HAConfiguration.ZookeeperProperties zookeeperProperties) throws SetupException {
        if (setupInProgress(zookeeperProperties)) {
            throw new SetupException("A previous setup run may not have completed cleanly. " +
                    "Ensure setup can run and retry after clearing the zookeeper node at " +
                    lockPath(zookeeperProperties));
        }
        createSetupInProgressNode(configuration, zookeeperProperties);
    }

    private void releaseLock(InterProcessMutex lock) {
        try {
            lock.release();
            LOG.info("Released lock after running setup.");
        } catch (Exception e) {
            LOG.error("Error releasing acquired lock.", e);
        }
    }

    private boolean setupInProgress(HAConfiguration.ZookeeperProperties zookeeperProperties) {
        CuratorFramework client = curatorFactory.clientInstance();
        Stat lockInProgressStat;
        String path = lockPath(zookeeperProperties);
        try {
            lockInProgressStat = client.checkExists().forPath(path);
            return lockInProgressStat != null;
        } catch (Exception e) {
            LOG.error("Error checking if path {} exists.", path, e);
            return true;
        }
    }

    private void clearSetupInProgress(HAConfiguration.ZookeeperProperties zookeeperProperties)
            throws SetupException {
        CuratorFramework client = curatorFactory.clientInstance();
        String path = lockPath(zookeeperProperties);
        try {
            client.delete().forPath(path);
            LOG.info("Deleted lock path after completing setup {}", path);
        } catch (Exception e) {
            throw new SetupException(String.format("SetupSteps.clearSetupInProgress: Failed to get Zookeeper node patH: %s", path), e);
        }
    }

    private String lockPath(HAConfiguration.ZookeeperProperties zookeeperProperties) {
        return zookeeperProperties.getZkRoot()+ SETUP_IN_PROGRESS_NODE;
    }

    private String getServerId(Configuration configuration) {
        String serverId = configuration.getString(AtlasConstants.ATLAS_REST_ADDRESS_KEY,
                AtlasConstants.DEFAULT_ATLAS_REST_ADDRESS);
        try {
            serverId = AtlasServerIdSelector.selectServerId(configuration);
        } catch (AtlasException e) {
            LOG.error("Could not select server id, defaulting to {}", serverId, e);
        }
        return serverId;
    }

    private void createSetupInProgressNode(Configuration configuration,
                                           HAConfiguration.ZookeeperProperties zookeeperProperties)
            throws SetupException {
        String serverId = getServerId(configuration);
        ACL acl = AtlasZookeeperSecurityProperties.parseAcl(zookeeperProperties.getAcl(),
                ZooDefs.Ids.OPEN_ACL_UNSAFE.get(0));
        List<ACL> acls = Arrays.asList(acl);

        CuratorFramework client = curatorFactory.clientInstance();
        try {
            String path = lockPath(zookeeperProperties);
            client.create().withACL(acls).forPath(path, serverId.getBytes(Charsets.UTF_8));
            LOG.info("Created lock node {}", path);
        } catch (Exception e) {
            throw new SetupException("Could not create lock node before running setup.", e);
        }
    }

    static class SetupRequired implements Condition {
        private static final String ATLAS_SERVER_RUN_SETUP_KEY = "atlas.server.run.setup.on.start";

        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            try {
                Configuration configuration = ApplicationProperties.get();
                boolean shouldRunSetup = configuration.getBoolean(ATLAS_SERVER_RUN_SETUP_KEY, false);
                if (shouldRunSetup) {
                    LOG.warn("Running setup per configuration {}.", ATLAS_SERVER_RUN_SETUP_KEY);
                    return true;
                } else {
                    LOG.info("Not running setup per configuration {}.", ATLAS_SERVER_RUN_SETUP_KEY);
                }
            } catch (AtlasException e) {
                LOG.error("Unable to read config to determine if setup is needed. Not running setup.");
            }
            return false;
        }
    }
}
