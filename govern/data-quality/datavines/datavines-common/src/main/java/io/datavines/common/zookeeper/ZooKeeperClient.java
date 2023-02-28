/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.datavines.common.zookeeper;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.ACLProvider;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZooKeeperClient {

    private final Logger logger = LoggerFactory.getLogger(ZooKeeperClient.class);

    private volatile CuratorFramework client;

    private ZooKeeperClient(){}

    private static class Singleton{
        static ZooKeeperClient instance = new ZooKeeperClient();
    }

    public static ZooKeeperClient getInstance(){
        return Singleton.instance;
    }

    public ZooKeeperClient buildClient(ZooKeeperConfig zooKeeperConfig) throws InterruptedException, UnsupportedEncodingException {
        logger.info("zookeeper registry center init, server lists is: {}.", zooKeeperConfig.getServerList());
        if(client == null){
            synchronized (ZooKeeperClient.class){
                if(client == null){
                    CuratorFrameworkFactory.Builder builder =
                            CuratorFrameworkFactory.builder()
                                    .ensembleProvider(new DefaultEnsembleProvider(zooKeeperConfig.getServerList()))
                                    .retryPolicy(new ExponentialBackoffRetry(
                                            zooKeeperConfig.getBaseSleepTimeMs(),
                                            zooKeeperConfig.getMaxRetries(),
                                            zooKeeperConfig.getMaxSleepMs()));
                    if (0 != zooKeeperConfig.getSessionTimeoutMs()) {
                        builder.sessionTimeoutMs(zooKeeperConfig.getSessionTimeoutMs());
                    }
                    if (0 != zooKeeperConfig.getConnectionTimeoutMs()) {
                        builder.connectionTimeoutMs(zooKeeperConfig.getConnectionTimeoutMs());
                    }
                    if (StringUtils.isNotBlank(zooKeeperConfig.getDigest())) {
                        builder.authorization("digest", zooKeeperConfig.getDigest().getBytes("UTF-8")).aclProvider(new ACLProvider() {

                            @Override
                            public List<ACL> getDefaultAcl() {
                                return ZooDefs.Ids.CREATOR_ALL_ACL;
                            }

                            @Override
                            public List<ACL> getAclForPath(final String path) {
                                return ZooDefs.Ids.CREATOR_ALL_ACL;
                            }
                        });
                    }
                    client = builder.build();
                    client.start();

                    client.blockUntilConnected();
                }
            }
        }

        return getInstance();
    }

    public String get(final String key) {
        try {
            return new String(client.getData().forPath(key), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            logger.error("get key : {}", key, ex);
        }
        return null;
    }

    public List<String> getChildrenKeys(final String key) {
        List<String> values;
        try {
            values = client.getChildren().forPath(key);
            if(CollectionUtils.isEmpty(values)){
                logger.warn("getChildrenKeys key : {} is empty", key);
            }
            return values;
        } catch (InterruptedException ex){
            logger.error("getChildrenKeys key : {} InterruptedException", key);
            throw new IllegalStateException(ex);
        } catch (Exception ex) {
            logger.error("getChildrenKeys key : {}", key, ex);
            throw new RuntimeException(ex);
        }
    }

    public boolean isExisted(final String key) {
        try {
            return client.checkExists().forPath(key) != null;
        } catch (Exception ex) {
            logger.error("isExisted key : {}", key, ex);
        }
        return false;
    }

    public void persist(final String key, final String value) {
        try {
            if (!isExisted(key)) {
                client.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(key, value.getBytes(StandardCharsets.UTF_8));
            } else {
                update(key, value);
            }
        } catch (Exception ex) {
            logger.error("persist key : "+key+ ",value : "+value, ex);
        }
    }

    public void update(final String key, final String value) {
        try {
            client.inTransaction().check().forPath(key).and().setData().forPath(key, value.getBytes(StandardCharsets.UTF_8)).and().commit();
        } catch (Exception ex) {
            logger.error("update key : "+key+ ",value : "+value, ex);
        }
    }

    public void persistEphemeral(final String key, final String value) {
        try {
            if (isExisted(key)) {
                try {
                    client.delete().deletingChildrenIfNeeded().forPath(key);
                } catch (KeeperException.NoNodeException ignore) {
                    //NOP
                }
            }
            client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(key, value.getBytes(StandardCharsets.UTF_8));
        } catch (final Exception ex) {
            logger.error("persistEphemeral key : "+key+ ",value : "+value, ex);
        }
    }

    public void persistEphemeral(String key, String value, boolean overwrite) {
        try {
            if (overwrite) {
                persistEphemeral(key, value);
            } else {
                if (!isExisted(key)) {
                    client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(key, value.getBytes(StandardCharsets.UTF_8));
                }
            }
        } catch (final Exception ex) {
            logger.error("persistEphemeral key : "+key+ ",value : "+value +",overwrite : "+overwrite, ex);
        }
    }

    public void persistEphemeralSequential(final String key) {
        try {
            client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL_SEQUENTIAL).forPath(key);
        } catch (final Exception ex) {
            logger.error("persistEphemeralSequential key : {}", key, ex);
        }
    }

    public void remove(final String key) {
        try {
            if (isExisted(key)) {
                client.delete().deletingChildrenIfNeeded().forPath(key);
            }
        } catch (KeeperException.NoNodeException ignore) {
            //NOP
        } catch (final Exception ex) {
            logger.error("remove key : {}", key, ex);
        }
    }

    public CuratorFramework getClient() {
        return client;
    }

    public void close(){
        if(client != null){
            client.close();
        }
    }

    public InterProcessMutex blockAcquireMutex(String lockPath) throws Exception {
        InterProcessMutex mutex = new InterProcessMutex(client,lockPath);
        mutex.acquire();
        return mutex;
    }

    public InterProcessMutex blockAcquireMutex(String lockPath, long time) throws Exception {
        InterProcessMutex mutex = new InterProcessMutex(client,lockPath);
        mutex.acquire(time, TimeUnit.SECONDS);
        return mutex;
    }

}