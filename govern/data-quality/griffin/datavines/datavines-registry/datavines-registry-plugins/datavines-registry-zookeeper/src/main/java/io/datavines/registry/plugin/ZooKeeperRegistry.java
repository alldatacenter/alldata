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
package io.datavines.registry.plugin;

import io.datavines.common.utils.CommonPropertyUtils;
import io.datavines.common.utils.NetUtils;
import io.datavines.common.zookeeper.ZooKeeperClient;
import io.datavines.common.zookeeper.ZooKeeperConfig;
import io.datavines.registry.api.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.utils.CloseableUtils;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class ZooKeeperRegistry implements Registry {

    private static final Logger logger = LoggerFactory.getLogger(ZooKeeperRegistry.class);

    private InterProcessMutex mutex;

    private CuratorFramework client;

    private final Map<String, TreeCache> treeCacheMap = new ConcurrentHashMap<>();

    private Properties properties;

    @Override
    public void init(Properties properties) {
        this.properties = properties;
        ZooKeeperClient zooKeeperClient = null;
        try {
            ZooKeeperConfig zooKeeperConfig = new ZooKeeperConfig();
            zooKeeperConfig.setServerList(properties.getProperty(CommonPropertyUtils.REGISTRY_ZOOKEEPER_SERVER_LIST,
                    CommonPropertyUtils.REGISTRY_ZOOKEEPER_SERVER_LIST_DEFAULT));
            zooKeeperClient = ZooKeeperClient.getInstance().buildClient(zooKeeperConfig);
            client = zooKeeperClient.getClient();
            ServerInfo serverInfo = new ServerInfo(NetUtils.getHost(), Integer.valueOf((String) properties.get("server.port")));
            String serverKey = properties.getProperty(CommonPropertyUtils.SERVERS_KEY, CommonPropertyUtils.SERVERS_KEY_DEFAULT);
            put(serverKey, serverInfo.toString(), true);
            treeCacheMap.put(serverKey, new TreeCache(client, serverKey));
        } catch (Exception exception) {
            logger.error("build zookeeper client error: {0} ", exception);
        }
    }

    @Override
    public boolean acquire(String key, long timeout){

        if (client == null) {
            return false;
        }

        if (mutex == null) {
            mutex = new InterProcessMutex(client,key);
        }

        try {
            return mutex.acquire(timeout, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.warn("acquire lock error: ", e);
            return false;
        }
    }

    @Override
    public boolean release(String key){

        if (client == null) {
            return false;
        }

        try {
            if (mutex != null && mutex.isAcquiredInThisProcess()) {
                mutex.release();
                return true;
            }
            return true;
        } catch (Exception e) {
            logger.warn("acquire lock error: ", e);
            return false;
        }
    }

    @Override
    public void subscribe(String key, SubscribeListener listener) {
        String targetKey = StringUtils.isEmpty(key) ?
                properties.getProperty(CommonPropertyUtils.SERVERS_KEY, CommonPropertyUtils.SERVERS_KEY_DEFAULT) : key;
        TreeCache treeCache = treeCacheMap.computeIfAbsent(targetKey, f -> new TreeCache(client, targetKey));
        treeCache.getListenable().addListener((f, event) -> listener.notify(new EventAdaptor(event, targetKey)));
        try {
            treeCache.start();
        } catch (Exception e) {
            treeCacheMap.remove(key);
            throw new RegistryException("Failed to subscribe listener for key: " + key, e);
        }
    }

    @Override
    public void unSubscribe(String key) {
        CloseableUtils.closeQuietly(treeCacheMap.get(key));
    }

    @Override
    public void addConnectionListener(ConnectionListener connectionListener) {
        client.getConnectionStateListenable().addListener(new ZookeeperConnectionStateListener(connectionListener));
    }

    public boolean exists(String key) {
        try {
            return null != client.checkExists().forPath(key);
        } catch (Exception e) {
            throw new RegistryException("zookeeper check key: \"" + key+ "\" is error: ", e);
        }
    }

    public List<String> children(String key) {
        try {
            List<String> result = client.getChildren().forPath(key);
            result.sort(Comparator.reverseOrder());
            return result;
        } catch (Exception e) {
            throw new RegistryException("zookeeper get children error", e);
        }
    }

    public String get(String key) {
        try {
            return new String(client.getData().forPath(key), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RegistryException("zookeeper get data error: ",  e);
        }
    }

    public void put(String key, String value, boolean deleteOnDisconnect) {
        final CreateMode mode = deleteOnDisconnect ? CreateMode.EPHEMERAL : CreateMode.PERSISTENT;

        try {
            client.create()
                    .orSetData()
                    .creatingParentsIfNeeded()
                    .withMode(mode)
                    .forPath(key, value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RegistryException("Failed to put registry key: " + key, e);
        }
    }

    public void delete(String key) {
        try {
            client.delete()
                    .deletingChildrenIfNeeded()
                    .forPath(key);
        } catch (KeeperException.NoNodeException ignored) {
            logger.warn("Is already deleted or does not exist, key:{}", key);
        }  catch (Exception e) {
            throw new RegistryException("Failed to delete registry key: " + key, e);
        }
    }

    @Override
    public List<ServerInfo> getActiveServerList() {
        List<ServerInfo> serverInfos = new ArrayList<>();
        String serverKey = properties.getProperty(CommonPropertyUtils.SERVERS_KEY, CommonPropertyUtils.SERVERS_KEY_DEFAULT);
        List<String> result = children(serverKey);
        if (CollectionUtils.isNotEmpty(result)){
            result.forEach(value -> {
                String[] values = value.split(":");
                if(values.length == 2){
                    ServerInfo serverInfo = new ServerInfo(values[0],Integer.parseInt(values[1]));
                    serverInfos.add(serverInfo);
                }
            });
        }
        return serverInfos;
    }

    @Override
    public void close() {
        treeCacheMap.values().forEach(CloseableUtils::closeQuietly);
        CloseableUtils.closeQuietly(client);
    }

    final class EventAdaptor extends Event {
        public EventAdaptor(TreeCacheEvent event, String key) {
            this.key(key);

            switch (event.getType()) {
                case NODE_ADDED:
                    type(Type.ADD);
                    break;
                case NODE_UPDATED:
                    type(Type.UPDATE);
                    break;
                case NODE_REMOVED:
                    type(Type.REMOVE);
                    break;
                default:
                    logger.warn("event type no match :{}", event.getType());
                    break;
            }

            final ChildData data = event.getData();
            if (data != null) {
                key(new String(data.getData()));
            } else {
                key(get(key));
            }

        }
    }
}
