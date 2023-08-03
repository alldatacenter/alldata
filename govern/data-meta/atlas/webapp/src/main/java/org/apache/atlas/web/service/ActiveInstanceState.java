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

import org.apache.atlas.ApplicationProperties;
import org.apache.atlas.AtlasErrorCode;
import org.apache.atlas.AtlasException;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.ha.HAConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessReadWriteLock;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

/**
 * An object that encapsulates storing and retrieving state related to an Active Atlas server.
 *
 * The current implementation uses Zookeeper to store and read this state from. It does this
 * under a read-write lock implemented using Curator's {@link InterProcessReadWriteLock} to
 * provide for safety across multiple processes.
 */
@Component
public class ActiveInstanceState {

    private final Configuration configuration;
    private final CuratorFactory curatorFactory;

    public static final String APACHE_ATLAS_ACTIVE_SERVER_INFO = "/active_server_info";

    private static final Logger LOG = LoggerFactory.getLogger(ActiveInstanceState.class);

    /**
     * Create a new instance of {@link ActiveInstanceState}.
     * @param curatorFactory an instance of {@link CuratorFactory} to get the {@link InterProcessReadWriteLock}
     * @throws AtlasException
     */
    @Inject
    public ActiveInstanceState(CuratorFactory curatorFactory) throws AtlasException {
        this(ApplicationProperties.get(), curatorFactory);
    }

    /**
     * Create a new instance of {@link ActiveInstanceState}.
     * @param configuration an instance of {@link Configuration} created from Atlas configuration
     * @param curatorFactory an instance of {@link CuratorFactory} to get the {@link InterProcessReadWriteLock}
     * @throws AtlasException
     */
    public ActiveInstanceState(Configuration configuration, CuratorFactory curatorFactory) {
        this.configuration = configuration;
        this.curatorFactory = curatorFactory;
    }

    /**
     * Update state of the active server instance.
     *
     * This method writes this instance's Server Address to a shared node in Zookeeper.
     * This information is used by other passive instances to locate the current active server.
     * @throws Exception
     * @param serverId ID of this server instance
     */
    public void update(String serverId) throws AtlasBaseException {
        try {
            CuratorFramework client = curatorFactory.clientInstance();
            HAConfiguration.ZookeeperProperties zookeeperProperties =
                    HAConfiguration.getZookeeperProperties(configuration);
            String atlasServerAddress = HAConfiguration.getBoundAddressForId(configuration, serverId);

            List<ACL> acls = new ArrayList<ACL>();
            ACL parsedACL = AtlasZookeeperSecurityProperties.parseAcl(zookeeperProperties.getAcl(),
                    ZooDefs.Ids.OPEN_ACL_UNSAFE.get(0));
            acls.add(parsedACL);

            //adding world read permission
            if (StringUtils.isNotEmpty(zookeeperProperties.getAcl())) {
                ACL worldReadPermissionACL = new ACL(ZooDefs.Perms.READ, new Id("world", "anyone"));
                acls.add(worldReadPermissionACL);
            }

            Stat serverInfo = client.checkExists().forPath(getZnodePath(zookeeperProperties));
            if (serverInfo == null) {
                client.create().
                        withMode(CreateMode.EPHEMERAL).
                        withACL(acls).
                        forPath(getZnodePath(zookeeperProperties));
            }
            client.setData().forPath(getZnodePath(zookeeperProperties),
                    atlasServerAddress.getBytes(Charset.forName("UTF-8")));
        } catch (Exception e) {
            throw new AtlasBaseException(AtlasErrorCode.CURATOR_FRAMEWORK_UPDATE, e, "forPath: getZnodePath");
        }
    }

    private String getZnodePath(HAConfiguration.ZookeeperProperties zookeeperProperties) {
        return zookeeperProperties.getZkRoot()+APACHE_ATLAS_ACTIVE_SERVER_INFO;
    }

    /**
     * Retrieve state of the active server instance.
     *
     * This method reads the active server location from the shared node in Zookeeper.
     * @return the active server's address and port of form http://host-or-ip:port
     */
    public String getActiveServerAddress() {
        CuratorFramework client = curatorFactory.clientInstance();
        String serverAddress = null;
        try {
            HAConfiguration.ZookeeperProperties zookeeperProperties =
                    HAConfiguration.getZookeeperProperties(configuration);
            byte[] bytes = client.getData().forPath(getZnodePath(zookeeperProperties));
            serverAddress = new String(bytes, Charset.forName("UTF-8"));
        } catch (Exception e) {
            LOG.error("Error getting active server address", e);
        }
        return serverAddress;
    }

}
