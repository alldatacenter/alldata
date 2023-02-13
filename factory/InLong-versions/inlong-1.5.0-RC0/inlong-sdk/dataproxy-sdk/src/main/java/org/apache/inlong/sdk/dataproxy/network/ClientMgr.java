/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sdk.dataproxy.network;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.apache.inlong.sdk.dataproxy.ConfigConstants;
import org.apache.inlong.sdk.dataproxy.LoadBalance;
import org.apache.inlong.sdk.dataproxy.ProxyClientConfig;
import org.apache.inlong.sdk.dataproxy.codec.EncodeObject;
import org.apache.inlong.sdk.dataproxy.config.EncryptConfigEntry;
import org.apache.inlong.sdk.dataproxy.config.HostInfo;
import org.apache.inlong.sdk.dataproxy.config.ProxyConfigEntry;
import org.apache.inlong.sdk.dataproxy.config.ProxyConfigManager;
import org.apache.inlong.sdk.dataproxy.utils.ConsistencyHashUtil;
import org.apache.inlong.sdk.dataproxy.utils.EventLoopUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;
import java.util.Collections;
import java.util.Iterator;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ClientMgr {

    private static final Logger logger = LoggerFactory
            .getLogger(ClientMgr.class);
    private static final int[] weight = {
            1, 1, 1, 1, 1,
            2, 2, 2, 2, 2,
            3, 3, 3, 3, 3,
            6, 6, 6, 6, 6,
            12, 12, 12, 12, 12,
            48, 96, 192, 384, 1000};
    private final Map<HostInfo, NettyClient> clientMapData = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<HostInfo, NettyClient> clientMapHB = new ConcurrentHashMap<>();
    // clientMapData + clientMapHB = clientMap
    private final ConcurrentHashMap<HostInfo, NettyClient> clientMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<HostInfo, AtomicLong> lastBadHostMap = new ConcurrentHashMap<>();
    // clientList is the valueSet of clientMapData
    private final ArrayList<NettyClient> clientList = new ArrayList<>();
    private final Map<HostInfo, int[]> channelLoadMapData = new ConcurrentHashMap<>();
    private final Map<HostInfo, int[]> channelLoadMapHB = new ConcurrentHashMap<>();
    /**
     * Lock to protect FSNamesystem.
     */
    private final ReentrantReadWriteLock fsLock = new ReentrantReadWriteLock(true);
    private List<HostInfo> proxyInfoList = new ArrayList<>();
    private Bootstrap bootstrap;
    private int currentIndex = 0;
    private ProxyClientConfig configure;
    private Sender sender;
    private int aliveConnections;
    private int realSize;
    private SendHBThread sendHBThread;
    private ProxyConfigManager ipManager;
    private int groupIdNum = 0;
    private String groupId = "";
    private Map<String, Integer> streamIdMap = new HashMap<String, Integer>();
    // private static final int total_weight = 240;
    private int loadThreshold;
    private int loadCycle = 0;
    private LoadBalance loadBalance;

    public ClientMgr(ProxyClientConfig configure, Sender sender) throws Exception {
        this(configure, sender, null);
    }

    /**
     * Build up the connection between the server and client.
     */
    public ClientMgr(ProxyClientConfig configure, Sender sender, ThreadFactory selfDefineFactory) throws Exception {
        /* Initialize the bootstrap. */
        if (selfDefineFactory == null) {
            selfDefineFactory = new DefaultThreadFactory("agent-client-io",
                    Thread.currentThread().isDaemon());
        }
        EventLoopGroup eventLoopGroup = EventLoopUtil.newEventLoopGroup(configure.getIoThreadNum(),
                configure.isEnableBusyWait(), selfDefineFactory);
        bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup);
        bootstrap.channel(EventLoopUtil.getClientSocketChannelClass(eventLoopGroup));
        bootstrap.option(ChannelOption.SO_RCVBUF, ConfigConstants.DEFAULT_RECEIVE_BUFFER_SIZE);
        bootstrap.option(ChannelOption.SO_SNDBUF, ConfigConstants.DEFAULT_SEND_BUFFER_SIZE);
        if (configure.getNetTag().equals("bobcat")) {
            bootstrap.option(ChannelOption.IP_TOS, 96);
        }
        bootstrap.handler(new ClientPipelineFactory(this, sender));
        /* ready to Start the thread which refreshes the proxy list. */
        ipManager = new ProxyConfigManager(configure, Utils.getLocalIp(), this);
        ipManager.setName("proxyConfigManager");
        if (configure.getGroupId() != null) {
            ipManager.setGroupId(configure.getGroupId());
            groupId = configure.getGroupId();
        }

        /*
         * Request the IP before starting, so that we already have three connections.
         */
        this.configure = configure;
        this.sender = sender;
        this.aliveConnections = configure.getAliveConnections();
        this.loadBalance = configure.getLoadBalance();

        try {
            ipManager.doProxyEntryQueryWork();
        } catch (IOException e) {
            e.printStackTrace();
            logger.info(e.getMessage());
        }
        ipManager.setDaemon(true);
        ipManager.start();

        this.sendHBThread = new SendHBThread();
        this.sendHBThread.setName("SendHBThread");
        this.sendHBThread.start();
    }

    public LoadBalance getLoadBalance() {
        return this.loadBalance;
    }

    public int getLoadThreshold() {
        return loadThreshold;
    }

    public void setLoadThreshold(int loadThreshold) {
        this.loadThreshold = loadThreshold;
    }

    public int getGroupIdNum() {
        return groupIdNum;
    }

    public void setGroupIdNum(int groupIdNum) {
        this.groupIdNum = groupIdNum;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public Map<String, Integer> getStreamIdMap() {
        return streamIdMap;
    }

    public void setStreamIdMap(Map<String, Integer> streamIdMap) {
        this.streamIdMap = streamIdMap;
    }

    public EncryptConfigEntry getEncryptConfigEntry() {
        return this.ipManager.getEncryptConfigEntry(configure.getUserName());
    }

    public List<HostInfo> getProxyInfoList() {
        return proxyInfoList;
    }

    public void setProxyInfoList(List<HostInfo> proxyInfoList) {
        try {
            /* Close and remove old client. */
            writeLock();
            this.proxyInfoList = proxyInfoList;

            if (loadThreshold == 0) {
                if (aliveConnections >= proxyInfoList.size()) {
                    realSize = proxyInfoList.size();
                    aliveConnections = realSize;
                    logger.error("there is no enough proxy to work!");
                } else {
                    realSize = aliveConnections;
                }
            } else {
                if (aliveConnections >= proxyInfoList.size()) {
                    realSize = proxyInfoList.size();
                    aliveConnections = realSize;
                    logger.error("there is no idle proxy to choose for balancing!");
                } else if ((aliveConnections + 4) > proxyInfoList.size()) {
                    realSize = proxyInfoList.size();
                    logger.warn("there is only {} idle proxy to choose for balancing!",
                            proxyInfoList.size() - aliveConnections);
                } else {
                    realSize = aliveConnections + 4;
                }
            }

            List<HostInfo> hostInfos = getRealHosts(proxyInfoList, realSize);

            /* Refresh the current channel connections. */
            updateAllConnection(hostInfos);

            logger.info(
                    "update all connection ,client map size {},client list size {}",
                    clientMapData.size(), clientList.size());

        } catch (Exception e) {
            logger.error(e.getMessage());
        } finally {
            writeUnlock();
        }
    }

    public int getAliveConnections() {
        return aliveConnections;
    }

    public void setAliveConnections(int aliveConnections) {
        this.aliveConnections = aliveConnections;
    }

    public void readLock() {
        this.fsLock.readLock().lock();
    }

    public void readUnlock() {
        this.fsLock.readLock().unlock();
    }

    public void writeLock() {
        this.fsLock.writeLock().lock();
    }

    public void writeLockInterruptibly() throws InterruptedException {
        this.fsLock.writeLock().lockInterruptibly();
    }

    public void writeUnlock() {
        this.fsLock.writeLock().unlock();
    }

    public boolean hasWriteLock() {
        return this.fsLock.isWriteLockedByCurrentThread();
    }

    public boolean hasReadLock() {
        return this.fsLock.getReadHoldCount() > 0;
    }

    public boolean hasReadOrWriteLock() {
        return hasReadLock() || hasWriteLock();
    }

    public ProxyConfigEntry getGroupIdConfigureInfo() throws Exception {
        return ipManager.getGroupIdConfigure();
    }

    /**
     * create conn, as DataConn or HBConn
     *
     * @param host
     * @return
     */
    private boolean initConnection(HostInfo host) {
        NettyClient client = clientMap.get(host);
        if (client != null && client.isActive()) {
            logger.info("this client {} has open!", host.getHostName());
            throw new IllegalStateException(
                    "The channel has already been opened");
        }
        client = new NettyClient(bootstrap, host.getHostName(),
                host.getPortNumber(), configure);
        boolean bSuccess = client.connect();

        if (clientMapData.size() < aliveConnections) {
            // create data channel
            if (bSuccess) {
                clientMapData.put(host, client);
                clientList.add(client);
                clientMap.put(host, client);
                logger.info("build a connection success! {},channel {}", host.getHostName(), client.getChannel());
            } else {
                logger.info("build a connection fail! {}", host.getHostName());
            }
            logger.info("client map size {},client list size {}", clientMapData.size(), clientList.size());
        } else {
            // data channel list is enough, create hb channel
            if (bSuccess) {
                clientMapHB.put(host, client);
                clientMap.put(host, client);
                logger.info("build a HBconnection success! {},channel {}", host.getHostName(), client.getChannel());
            } else {
                logger.info("build a HBconnection fail! {}", host.getHostName());
            }
        }
        return bSuccess;
    }

    public void resetClient(Channel channel) {
        if (channel == null) {
            return;
        }
        logger.info("reset this channel {}", channel);
        for (HostInfo hostInfo : clientMap.keySet()) {
            if (hostInfo == null) {
                continue;
            }
            NettyClient client = clientMap.get(hostInfo);
            if (client != null && client.getChannel() != null
                    && client.getChannel().id().equals(channel.id())) {
                client.reconnect();
                break;
            }
        }
    }

    public void setConnectionFrozen(Channel channel) {
        if (channel == null) {
            return;
        }
        logger.info("set this channel {} frozen", channel);
        for (HostInfo hostInfo : clientMap.keySet()) {
            if (hostInfo == null) {
                continue;
            }
            NettyClient client = clientMap.get(hostInfo);
            if (client != null && client.getChannel() != null
                    && client.getChannel().id().equals(channel.id())) {
                client.setFrozen();
                logger.info("end to froze this channel {}", client.getChannel().toString());
                break;
            }
        }
    }

    public void setConnectionBusy(Channel channel) {
        if (channel == null) {
            return;
        }
        logger.info("set this channel {} busy", channel);
        for (HostInfo hostInfo : clientMap.keySet()) {
            if (hostInfo == null) {
                continue;
            }
            NettyClient client = clientMap.get(hostInfo);
            if (client != null && client.getChannel() != null
                    && client.getChannel().id().equals(channel.id())) {
                client.setBusy();
                break;
            }
        }
    }

    public synchronized NettyClient getClientByRoundRobin() {
        NettyClient client = null;
        if (clientList.isEmpty()) {
            return null;
        }
        int currSize = clientList.size();
        for (int retryTime = 0; retryTime < currSize; retryTime++) {
            currentIndex = (++currentIndex) % currSize;
            client = clientList.get(currentIndex);
            if (client != null && client.isActive()) {
                break;
            }
        }
        if (client == null || !client.isActive()) {
            return null;
        }
        // logger.info("get a client {}", client.getChannel());
        return client;
    }

    public synchronized NettyClient getClientByRandom() {
        NettyClient client;
        if (clientList.isEmpty()) {
            return null;
        }
        int currSize = clientList.size();
        int maxRetry = this.configure.getMaxRetry();
        Random random = new Random(System.currentTimeMillis());
        do {
            int randomId = random.nextInt();
            client = clientList.get(randomId % currSize);
            if (client != null && client.isActive()) {
                break;
            }
            maxRetry--;
        } while (maxRetry > 0);
        if (client == null || !client.isActive()) {
            return null;
        }
        return client;
    }

    // public synchronized NettyClient getClientByLeastConnections() {}

    public synchronized NettyClient getClientByConsistencyHash(String messageId) {
        NettyClient client;
        if (clientList.isEmpty()) {
            return null;
        }
        String hash = ConsistencyHashUtil.hashMurMurHash(messageId);
        HashRing cluster = HashRing.getInstance();
        HostInfo info = cluster.getNode(hash);
        client = this.clientMap.get(info);
        return client;
    }

    public synchronized NettyClient getClientByWeightRoundRobin() {
        NettyClient client = null;
        double maxWeight = Double.MIN_VALUE;
        int clientId = 0;
        if (clientList.isEmpty()) {
            return null;
        }
        int currSize = clientList.size();
        for (int retryTime = 0; retryTime < currSize; retryTime++) {
            currentIndex = (++currentIndex) % currSize;
            client = clientList.get(currentIndex);
            if (client != null && client.isActive() && client.getWeight() > maxWeight) {
                clientId = currentIndex;
            }
        }
        if (client == null || !client.isActive()) {
            return null;
        }
        return clientList.get(clientId);
    }

    // public synchronized NettyClient getClientByWeightLeastConnections(){}

    public synchronized NettyClient getClientByWeightRandom() {
        NettyClient client;
        double maxWeight = Double.MIN_VALUE;
        int clientId = 0;
        if (clientList.isEmpty()) {
            return null;
        }
        int currSize = clientList.size();
        int maxRetry = this.configure.getMaxRetry();
        Random random = new Random(System.currentTimeMillis());
        do {
            int randomId = random.nextInt();
            client = clientList.get(randomId % currSize);
            if (client != null && client.isActive()) {
                clientId = randomId;
                break;
            }
            maxRetry--;
        } while (maxRetry > 0);
        if (client == null || !client.isActive()) {
            return null;
        }
        return clientList.get(clientId);
    }

    public NettyClient getContainProxy(String proxyip) {
        if (proxyip == null) {
            return null;
        }
        for (NettyClient tmpClient : clientList) {
            if (tmpClient != null && tmpClient.getServerIP() != null && tmpClient.getServerIP().equals(proxyip)) {
                return tmpClient;
            }
        }
        return null;
    }

    public void shutDown() {
        // bootstrap.shutdown();

        ipManager.shutDown();

        // connectionCheckThread.shutDown();
        sendHBThread.shutDown();
        closeAllConnection();

    }

    private void closeAllConnection() {
        if (!clientMap.isEmpty()) {
            logger.info("ready to close all connections!");
            for (HostInfo hostInfo : clientMap.keySet()) {
                if (hostInfo == null) {
                    continue;
                }
                NettyClient client = clientMap.get(hostInfo);
                if (client != null && client.isActive()) {
                    sender.waitForAckForChannel(client.getChannel());
                    client.close();
                }
            }
        }
        clientMap.clear();
        clientMapData.clear();
        clientMapHB.clear();

        channelLoadMapData.clear();
        channelLoadMapHB.clear();
        clientList.clear();
        sender.clearCallBack();
    }

    private void updateAllConnection(List<HostInfo> hostInfos) {
        closeAllConnection();
        /* Build new channels */
        for (HostInfo hostInfo : hostInfos) {
            initConnection(hostInfo);
        }
    }

    public void notifyHBAck(Channel channel, short loadvalue) {
        try {
            if (loadvalue == (-1) || loadCycle == 0) {
                return;
            } else {
                for (Map.Entry<HostInfo, NettyClient> entry : clientMapData.entrySet()) {
                    NettyClient client = entry.getValue();
                    HostInfo hostInfo = entry.getKey();
                    if (client != null && client.getChannel() != null
                            && client.getChannel().id().equals(channel.id())) {
                        // logger.info("channel" + channel + "; Load:" + load);
                        if (!channelLoadMapData.containsKey(hostInfo)) {
                            channelLoadMapData.put(hostInfo, new int[ConfigConstants.CYCLE]);
                        }
                        if ((loadCycle - 1) >= 0) {
                            channelLoadMapData.get(hostInfo)[loadCycle - 1] = loadvalue;
                        } else {
                            return;
                        }
                        break;
                    }
                }

                for (Map.Entry<HostInfo, NettyClient> entry : clientMapHB.entrySet()) {
                    NettyClient client = entry.getValue();
                    HostInfo hostInfo = entry.getKey();
                    if (client != null && client.getChannel() != null
                            && client.getChannel().id().equals(channel.id())) {
                        // logger.info("HBchannel" + channel + "; Load:" + load);
                        if (!channelLoadMapHB.containsKey(hostInfo)) {
                            channelLoadMapHB.put(hostInfo, new int[ConfigConstants.CYCLE]);
                        }
                        if ((loadCycle - 1) >= 0) {
                            channelLoadMapHB.get(hostInfo)[loadCycle - 1] = loadvalue;
                        } else {
                            return;
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("{} , {}", e.toString(), e.getStackTrace());
        }
    }

    private void loadDataInfo(Map<HostInfo, Integer> loadData) {
        for (Map.Entry<HostInfo, int[]> entry : channelLoadMapData.entrySet()) {
            HostInfo key = entry.getKey();
            int[] value = entry.getValue();
            int numerator = 0;
            int denominator = 0;
            for (int i = 0; i < value.length; i++) {
                if (value[i] > 0) {
                    numerator = numerator + value[i] * weight[i];
                    denominator = denominator + weight[i];
                }
            }
            int sum = numerator / denominator;
            loadData.put(key, sum);
        }
    }

    private void loadHBInfo(Map<HostInfo, Integer> loadHB) {
        for (Map.Entry<HostInfo, int[]> entry : channelLoadMapHB.entrySet()) {
            HostInfo key = entry.getKey();
            int[] value = entry.getValue();
            int numerator = 0;
            int denominator = 0;
            for (int i = 0; i < value.length; i++) {
                if (value[i] > 0) {
                    numerator = numerator + value[i] * weight[i];
                    denominator = denominator + weight[i];
                }
            }
            int sum = numerator / denominator;
            loadHB.put(key, sum);
        }
    }

    public void notifyHBControl() {
        try {
            writeLock();
            logger.info("check if there is need to start balancing!");

            Map<HostInfo, Integer> loadData = new ConcurrentHashMap<HostInfo, Integer>();
            Map<HostInfo, Integer> loadHB = new ConcurrentHashMap<HostInfo, Integer>();
            loadDataInfo(loadData);
            loadHBInfo(loadHB);

            List<Map.Entry<HostInfo, Integer>> listData = new ArrayList<>(loadData.entrySet());
            Collections.sort(listData, new Comparator<Map.Entry<HostInfo, Integer>>() {

                @Override
                public int compare(Map.Entry<HostInfo, Integer> o1, Map.Entry<HostInfo, Integer> o2) {
                    if (o2.getValue() != null && o1.getValue() != null && o1.getValue() > o2.getValue()) {
                        return -1;
                    } else {
                        return 1;
                    }
                }
            });
            List<Map.Entry<HostInfo, Integer>> listHB = new ArrayList<>(loadHB.entrySet());
            Collections.sort(listHB, new Comparator<Map.Entry<HostInfo, Integer>>() {

                @Override
                public int compare(Map.Entry<HostInfo, Integer> o1, Map.Entry<HostInfo, Integer> o2) {
                    if (o2.getValue() != null && o1.getValue() != null && o2.getValue() > o1.getValue()) {
                        return -1;
                    } else {
                        return 1;
                    }
                }
            });

            logger.info("show info: last compute result!");
            for (Map.Entry<HostInfo, Integer> item : listData) {
                // System.out.println("listData:"+listData.get(i));
                logger.info("Client:" + item.getKey() + ";" + item.getValue());
            }
            for (Map.Entry<HostInfo, Integer> item : listHB) {
                // System.out.println("listHB:"+listHB.get(i));
                logger.info("HBClient:" + item.getKey() + ";" + item.getValue());
            }
            boolean isLoadSwitch = false;

            // int smallSize = listData.size() < listHB.size() ? listData.size() : listHB.size();
            int smallSize = 1;
            for (int i = 0; i < smallSize; i++) {
                if ((listData.get(i).getValue() - listHB.get(i).getValue()) >= this.loadThreshold) {
                    isLoadSwitch = true;
                    HostInfo dataHost = listData.get(i).getKey();
                    HostInfo hbHost = listHB.get(i).getKey();
                    logger.info("balancing client:" + dataHost.getHostName() + ",load: " + listData.get(i).getValue()
                            + "; HBclient:" + hbHost.getHostName() + ",load: " + listHB.get(i).getValue());

                    NettyClient client = clientMapData.get(dataHost);
                    client.setFrozen();
                    sender.waitForAckForChannel(client.getChannel());
                    client.close();

                    clientList.remove(clientMapData.get(dataHost));
                    clientMap.remove(dataHost);
                    clientMapData.remove(dataHost);
                    // channelLoadMapData.remove(dataHost);
                    clientMapData.put(hbHost, clientMapHB.get(hbHost));
                    // channelLoadMapData.put(hbHost,listHB.get(i).getValue());
                    clientList.add(clientMapHB.get(hbHost));
                    clientMapHB.remove(hbHost);
                }
            }

            if (!isLoadSwitch) {
                logger.info("Choose other HBClient because there is no load balancing! ");
            }
            for (Map.Entry<HostInfo, NettyClient> entry : clientMapHB.entrySet()) {
                entry.getValue().close();
                clientMap.remove(entry.getKey());
            }
            clientMapHB.clear();

            int realSize = this.realSize - clientMap.size();
            if (realSize > 0) {
                List<HostInfo> hostInfoList = new ArrayList<>(proxyInfoList);
                hostInfoList.removeAll(clientMap.keySet());
                List<HostInfo> replaceHost = getRealHosts(hostInfoList, realSize);
                for (HostInfo hostInfo : replaceHost) {
                    initConnection(hostInfo);
                }
            }
        } catch (Exception e) {
            logger.error("notifyHBcontrol", e);
        } finally {
            writeUnlock();
        }
    }

    private void sendHeartBeat() {
        // all hbChannels need hb
        for (Map.Entry<HostInfo, NettyClient> clientEntry : clientMapHB.entrySet()) {
            if (clientEntry.getKey() != null && clientEntry.getValue() != null) {
                sendHeartBeat(clientEntry.getKey(), clientEntry.getValue());
            }
        }

        // only idle dataChannels need hb
        for (Map.Entry<HostInfo, NettyClient> clientEntry : clientMapData.entrySet()) {
            if (clientEntry.getKey() == null || clientEntry.getValue() == null) {
                continue;
            }
            if (sender.isIdleClient(clientEntry.getValue())) {
                sendHeartBeat(clientEntry.getKey(), clientEntry.getValue());
            }
        }

    }

    private void sendHeartBeat(HostInfo hostInfo, NettyClient client) {
        if (!client.isActive()) {
            logger.info("client {} is inActive", hostInfo.getReferenceName());
            return;
        }
        logger.debug("active host to send heartbeat! {}", hostInfo.getReferenceName());
        String hbMsg = "heartbeat:" + hostInfo.getHostName();
        EncodeObject encodeObject = new EncodeObject(hbMsg.getBytes(StandardCharsets.UTF_8),
                8, false, false, false, System.currentTimeMillis() / 1000, 1, "", "", "");
        try {
            if (configure.isNeedAuthentication()) {
                encodeObject.setAuth(configure.isNeedAuthentication(),
                        configure.getUserName(), configure.getSecretKey());
            }
            client.write(encodeObject);
        } catch (Throwable e) {
            logger.error("sendHeartBeat to " + hostInfo.getReferenceName()
                    + " exception {}, {}", e.toString(), e.getStackTrace());
        }
    }

    /**
     * fill up client with hb client
     */
    private void fillUpWorkClientWithHBClient() {
        if (clientMapHB.size() > 0) {
            logger.info("fill up work client with HB, clientMapData {}, clientMapHB {}",
                    clientMapData.size(), clientMapHB.size());
        }
        Iterator<Map.Entry<HostInfo, NettyClient>> it = clientMapHB.entrySet().iterator();
        while (it.hasNext() && clientMapData.size() < aliveConnections) {
            Map.Entry<HostInfo, NettyClient> entry = it.next();
            clientMapData.put(entry.getKey(), entry.getValue());
            clientList.add(entry.getValue());
            channelLoadMapHB.remove(entry.getKey());
            it.remove();
        }
    }

    private void fillUpWorkClientWithLastBadClient() {

        int currentRealSize = aliveConnections - clientMapData.size();

        List<HostInfo> pendingBadList = new ArrayList<>();
        for (Map.Entry<HostInfo, AtomicLong> entry : lastBadHostMap.entrySet()) {
            if (pendingBadList.size() < currentRealSize) {
                pendingBadList.add(entry.getKey());
            } else {
                for (int index = 0; index < pendingBadList.size(); index++) {

                    if (entry.getValue().get() < lastBadHostMap
                            .get(pendingBadList.get(index)).get()) {
                        pendingBadList.set(index, entry.getKey());
                    }
                }
            }
        }
        List<HostInfo> replaceHostLists = getRealHosts(pendingBadList, currentRealSize);
        if (replaceHostLists.size() > 0) {
            logger.info("replace bad connection, use last bad list, "
                    + "last bad list {}, client Map data {}",
                    lastBadHostMap.size(), clientMapData.size());
        }
        for (HostInfo hostInfo : replaceHostLists) {

            boolean isSuccess = initConnection(hostInfo);

            if (isSuccess) {
                lastBadHostMap.remove(hostInfo);
            }
        }
    }

    private void fillUpWorkClientWithBadClient(List<HostInfo> badHostLists) {
        if (badHostLists.isEmpty()) {
            logger.warn("badHostLists is empty, current hostList size {}, dataClient size {}, hbClient size {}",
                    proxyInfoList.size(), clientMapData.size(), clientMapHB.size());
            return;
        }
        logger.info("all hosts are bad, dataClient is empty, reuse them, badHostLists size {}, "
                + "proxyInfoList size {}", badHostLists.size(), proxyInfoList.size());

        List<HostInfo> replaceHostLists = getRealHosts(badHostLists, aliveConnections);
        boolean isSuccess = false;
        for (HostInfo hostInfo : replaceHostLists) {
            isSuccess = initConnection(hostInfo);
            if (isSuccess) {
                badHostLists.remove(hostInfo);
            }
        }
    }

    private void removeBadRealClient(List<HostInfo> badHostLists, List<HostInfo> normalHostLists) {
        for (HostInfo hostInfo : clientMapData.keySet()) {
            if (hostInfo == null) {
                continue;
            }
            NettyClient client = clientMapData.get(hostInfo);
            if (client == null || !client.isActive()) {
                logger.info("this host {} is bad! so remove it", hostInfo.getHostName());
                badHostLists.add(hostInfo);
            } else {
                logger.info("this host {} is active! so keep it", hostInfo.getHostName());
                normalHostLists.add(hostInfo);
            }
        }
    }

    private void removeBadHBClients(List<HostInfo> badHostLists, List<HostInfo> normalHostLists) {
        for (HostInfo hostInfo : clientMapHB.keySet()) {
            if (hostInfo == null) {
                continue;
            }
            NettyClient client = clientMapHB.get(hostInfo);
            if (client == null || !client.isActive()) {
                logger.info("this HBhost {} is bad! so remove it", hostInfo.getHostName());
                badHostLists.add(hostInfo);
            } else {
                logger.info("this HBhost {} is active! so keep it", hostInfo.getHostName());
                normalHostLists.add(hostInfo);
            }
        }
    }

    private void removeBadClients(List<HostInfo> badHostLists) {
        for (HostInfo hostInfo : badHostLists) {
            if (hostInfo == null) {
                continue;
            }
            NettyClient client = clientMapData.get(hostInfo);
            if (client != null) {
                sender.waitForAckForChannel(client.getChannel());
                client.close();
                clientMapData.remove(hostInfo);
                clientMap.remove(hostInfo);
                clientList.remove(client);

                channelLoadMapData.remove(hostInfo);
                logger.info("remove this client {}", hostInfo.getHostName());
            }
            client = clientMapHB.get(hostInfo);
            if (client != null) {
                clientMapHB.get(hostInfo).close();
                clientMapHB.remove(hostInfo);
                clientMap.remove(hostInfo);

                channelLoadMapHB.remove(hostInfo);
                logger.info("remove this HBclient {}", hostInfo.getHostName());
            }
        }
    }

    /**
     * 1. check all inactive conn, including dataConn and HBConn
     * 2.1. if there is no any bad conn, do dataConn<->hbConn balance every ConfigConstants.CYCLE, according to load
     * 2.2. close and remove all inactive conn
     * 3. fillUp dataConn and HBConn using remaining hosts(excludes lastBadHostMap)
     * 4. if dataConns are still not full, try to using HBConns (then lastBadHostMap) to fillUp
     * 5. update lastBadHostMap, including increase, remove and update badValue
     */
    public void replaceBadConnectionHB() {
        try {
            writeLock();

            List<HostInfo> badHostLists = new ArrayList<>();
            List<HostInfo> normalHostLists = new ArrayList<>();
            removeBadRealClient(badHostLists, normalHostLists);
            removeBadHBClients(badHostLists, normalHostLists);
            removeBadClients(badHostLists);

            if (badHostLists.size() == 0 && normalHostLists.size() != 0 && clientMapData.size() >= aliveConnections) {
                logger.info("hasn't bad host! so keep it");
                if (loadCycle >= ConfigConstants.CYCLE) {
                    if (loadThreshold == 0) {
                        logger.info("the proxy cluster is being updated!");
                    } else if (clientMapHB.size() != 0 && clientMapData.size() != 0) {
                        notifyHBControl();
                    } else if (this.realSize != clientMap.size()) {
                        logger.info("make the amount of proxy to original value");
                        int realSize = this.realSize - clientMap.size();
                        if (realSize > 0) {
                            List<HostInfo> hostInfoList = new ArrayList<>(proxyInfoList);
                            hostInfoList.removeAll(clientMap.keySet());
                            List<HostInfo> replaceHost = getRealHosts(hostInfoList, realSize);
                            for (HostInfo hostInfo : replaceHost) {
                                initConnection(hostInfo);
                            }
                        }
                    }
                    loadCycle = 0;
                    channelLoadMapData.clear();
                    channelLoadMapHB.clear();
                }
                return;
            } else {
                loadCycle = 0;
                channelLoadMapData.clear();
                channelLoadMapHB.clear();
            }

            List<HostInfo> hostLists = new ArrayList<HostInfo>(this.proxyInfoList);
            hostLists.removeAll(badHostLists);
            hostLists.removeAll(lastBadHostMap.keySet());
            hostLists.removeAll(normalHostLists); // now, all hosts in this hostLists are not built conn

            int realSize = this.realSize - clientMap.size();
            if (realSize > hostLists.size()) {
                realSize = hostLists.size();
            }

            if (realSize != 0) {
                List<HostInfo> replaceHostLists = getRealHosts(hostLists, realSize);
                /* Build new channels. */
                for (HostInfo hostInfo : replaceHostLists) {
                    initConnection(hostInfo);
                }
            }

            if (clientMapData.size() < aliveConnections) {
                fillUpWorkClientWithHBClient();
            }

            if (clientMapData.size() < aliveConnections) {
                fillUpWorkClientWithLastBadClient();
            }

            // when all hosts are bad, reuse them to avoid clientMapData is empty in this round
            if (clientMapData.isEmpty()) {
                fillUpWorkClientWithBadClient(badHostLists);
            }

            for (HostInfo hostInfo : badHostLists) {
                AtomicLong tmpValue = new AtomicLong(0);
                AtomicLong hostValue = lastBadHostMap.putIfAbsent(hostInfo, tmpValue);
                if (hostValue == null) {
                    hostValue = tmpValue;
                }
                hostValue.incrementAndGet();
            }

            for (HostInfo hostInfo : normalHostLists) {
                lastBadHostMap.remove(hostInfo);
            }

            logger.info(
                    "replace bad connection ,client map size {},client list size {}",
                    clientMapData.size(), clientList.size());

        } catch (Exception e) {
            logger.error("replaceBadConnection exception {}, {}", e.toString(), e.getStackTrace());
        } finally {
            writeUnlock();
        }

    }

    private List<HostInfo> getRealHosts(List<HostInfo> hostList, int realSize) {
        if (realSize > hostList.size()) {
            return hostList;
        }
        Collections.shuffle(hostList);
        List<HostInfo> resultHosts = new ArrayList<HostInfo>(realSize);
        for (int i = 0; i < realSize; i++) {
            resultHosts.add(hostList.get(i));
            logger.info("host={}", hostList.get(i));
        }
        return resultHosts;
    }

    public NettyClient getClient(LoadBalance loadBalance, EncodeObject encodeObject) {
        NettyClient client = null;
        switch (loadBalance) {
            case RANDOM:
                client = getClientByRandom();
                break;
            case CONSISTENCY_HASH:
                client = getClientByConsistencyHash(encodeObject.getMessageId());
                break;
            case ROBIN:
                client = getClientByRoundRobin();
                break;
            case WEIGHT_ROBIN:
                client = getClientByWeightRoundRobin();
                break;
            case WEIGHT_RANDOM:
                client = getClientByWeightRandom();
                break;
        }
        return client;
    }

    private class SendHBThread extends Thread {

        private final int[] random = {17, 19, 23, 31, 37};
        private boolean bShutDown = false;

        public SendHBThread() {
            bShutDown = false;
        }

        public void shutDown() {
            logger.info("begin to shut down SendHBThread!");
            bShutDown = true;
        }

        @Override
        public void run() {
            while (!bShutDown) {
                try {
                    loadCycle++;
                    sendHeartBeat();
                    replaceBadConnectionHB();
                    try {
                        int index = (int) (Math.random() * random.length);
                        Thread.sleep((random[index]) * 1000);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        logger.error(e.toString());
                    }
                } catch (Throwable e) {
                    logger.error("SendHBThread throw exception: ", e);
                }
            }
        }
    }

}
