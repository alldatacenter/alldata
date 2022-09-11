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

package org.apache.inlong.audit.send;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import org.apache.inlong.audit.protocol.AuditApi;
import org.apache.inlong.audit.util.AuditConfig;
import org.apache.inlong.audit.util.AuditData;
import org.apache.inlong.audit.util.SenderResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * sender manager
 */
public class SenderManager {
    private static final Logger logger = LoggerFactory.getLogger(SenderManager.class);
    public static final int DEFAULT_SEND_THREADNUM = 2;
    public static final Long MAX_REQUEST_ID = 1000000000L;
    private static final int SEND_INTERVAL_MS = 20;
    public static final int ALL_CONNECT_CHANNEL = -1;
    public static final int DEFAULT_CONNECT_CHANNEL = 2;
    public static final String LF = "\n";
    private SenderGroup sender;
    private int maxConnectChannels = ALL_CONNECT_CHANNEL;
    private SecureRandom sRandom = new SecureRandom(Long.toString(System.currentTimeMillis()).getBytes());
    // IPList
    private HashSet<String> currentIpPorts = new HashSet<String>();
    private AtomicLong requestIdSeq = new AtomicLong(0L);
    private ConcurrentHashMap<Long, AuditData> dataMap = new ConcurrentHashMap<>();
    private AuditConfig auditConfig;

    /**
     * Constructor
     *
     * @param config
     */
    public SenderManager(AuditConfig config) {
        this(config, DEFAULT_CONNECT_CHANNEL);
    }

    /**
     * Constructor
     *
     * @param config
     * @param maxConnectChannels
     */
    public SenderManager(AuditConfig config, int maxConnectChannels) {
        try {
            this.auditConfig = config;
            this.maxConnectChannels = maxConnectChannels;
            SenderHandler clientHandler = new SenderHandler(this);
            this.sender = new SenderGroup(DEFAULT_SEND_THREADNUM, clientHandler);
        } catch (Exception ex) {
            logger.error(ex.getMessage());
        }
    }

    /**
     * update config
     */
    public void setAuditProxy(HashSet<String> ipPortList) {
        if (ipPortList.equals(currentIpPorts) && !this.sender.isHasSendError()) {
            return;
        }
        this.sender.setHasSendError(false);
        this.currentIpPorts = ipPortList;
        int ipSize = ipPortList.size();
        int needNewSize = 0;
        if (this.maxConnectChannels == ALL_CONNECT_CHANNEL || this.maxConnectChannels >= ipSize) {
            needNewSize = ipSize;
        } else {
            needNewSize = maxConnectChannels;
        }
        HashSet<String> updateConfigIpLists = new HashSet<>();
        List<String> availableIpLists = new ArrayList<String>();
        availableIpLists.addAll(ipPortList);
        for (int i = 0; i < needNewSize; i++) {
            int availableIpSize = availableIpLists.size();
            int newIpPortIndex = this.sRandom.nextInt(availableIpSize);
            String ipPort = availableIpLists.remove(newIpPortIndex);
            updateConfigIpLists.add(ipPort);
        }
        if (updateConfigIpLists.size() > 0) {
            this.sender.updateConfig(updateConfigIpLists);
        }
    }

    /**
     * next requestid
     *
     * @return
     */
    public Long nextRequestId() {
        Long requestId = requestIdSeq.getAndIncrement();
        if (requestId > MAX_REQUEST_ID) {
            requestId = 0L;
            requestIdSeq.set(requestId);
        }
        return requestId;
    }

    /**
     * send data
     *
     * @param sdkTime
     * @param baseCommand
     */
    public void send(long sdkTime, AuditApi.BaseCommand baseCommand) {
        AuditData data = new AuditData(sdkTime, baseCommand);
        // Cache first
        this.dataMap.putIfAbsent(baseCommand.getAuditRequest().getRequestId(), data);
        this.sendData(data.getDataByte());
    }

    /**
     * send data
     *
     * @param data
     */
    private void sendData(byte[] data) {
        if (data == null || data.length <= 0) {
            logger.warn("send data is empty!");
            return;
        }
        ByteBuf dataBuf =  ByteBufAllocator.DEFAULT.buffer(data.length);
        dataBuf.writeBytes(data);
        SenderResult result = this.sender.send(dataBuf);
        if (!result.result) {
            this.sender.setHasSendError(true);
        }
    }

    /**
     * Clean up the backlog of unsent message packets
     */
    public void clearBuffer() {
        logger.info("audit failed cache size: {}", this.dataMap.size());
        for (AuditData data : this.dataMap.values()) {
            this.sendData(data.getDataByte());
            sleep(SEND_INTERVAL_MS);
        }
        if (this.dataMap.size() == 0) {
            checkAuditFile();
        }
        if (this.dataMap.size() > auditConfig.getMaxCacheRow()) {
            logger.info("failed cache size: {}>{}", this.dataMap.size(), auditConfig.getMaxCacheRow());
            writeLocalFile();
            this.dataMap.clear();
        }
    }

    /**
     * write local file
     */
    private void writeLocalFile() {
        try {
            if (!checkFilePath()) {
                return;
            }
            File file = new File(auditConfig.getDisasterFile());
            if (!file.exists()) {
                if (!file.createNewFile()) {
                    logger.error("create {} {}", auditConfig.getDisasterFile(), " failed");
                    return;
                }
                logger.info("create {}", auditConfig.getDisasterFile());
            }
            if (file.length() > auditConfig.getMaxFileSize()) {
                file.delete();
                return;
            }
            FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fos);
            objectOutputStream.writeObject(dataMap);
            objectOutputStream.close();
            fos.close();
        } catch (IOException ioException) {
            logger.error(ioException.getMessage());
        }
    }

    /**
     * check file path
     *
     * @return
     */
    private boolean checkFilePath() {
        File file = new File(auditConfig.getFilePath());
        if (!file.exists()) {
            if (!file.mkdirs()) {
                return false;
            }
            logger.info("create {}", auditConfig.getFilePath());
        }
        return true;
    }

    /**
     * check audit file
     */
    private void checkAuditFile() {
        try {
            File file = new File(auditConfig.getDisasterFile());
            if (!file.exists()) {
                return;
            }
            FileInputStream inputStream = new FileInputStream(auditConfig.getDisasterFile());
            ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
            ConcurrentHashMap<Long, AuditData> fileData =
                    (ConcurrentHashMap<Long, AuditData>) objectInputStream.readObject();
            for (Map.Entry<Long, AuditData> entry : fileData.entrySet()) {
                if (this.dataMap.size() < (auditConfig.getMaxCacheRow() / 2)) {
                    this.dataMap.putIfAbsent(entry.getKey(), entry.getValue());
                }
                this.sendData(entry.getValue().getDataByte());
                sleep(SEND_INTERVAL_MS);
            }
            objectInputStream.close();
            inputStream.close();
            file.delete();
        } catch (IOException | ClassNotFoundException ioException) {
            logger.error(ioException.getMessage());
        }
    }

    /**
     * get data map szie
     */
    public int getDataMapSize() {
        return this.dataMap.size();
    }

    /**
     * processing return package
     *
     * @param ctx ctx
     * @param msg msg
     */
    public void onMessageReceived(ChannelHandlerContext ctx, byte[] msg) {
        try {
            //Analyze abnormal events
            byte[] readBytes = msg;
            AuditApi.BaseCommand baseCommand = AuditApi.BaseCommand.parseFrom(readBytes);
            // Parse request id
            Long requestId = baseCommand.getAuditReply().getRequestId();
            AuditData data = this.dataMap.get(requestId);
            if (data == null) {
                logger.error("can not find the requestid onMessageReceived:" + requestId);
                return;
            }
            logger.info("audit-proxy response code: {}", baseCommand.getAuditReply().getRspCode().toString());
            if (AuditApi.AuditReply.RSP_CODE.SUCCESS.equals(baseCommand.getAuditReply().getRspCode())) {
                this.dataMap.remove(requestId);
                return;
            }
            int resendTimes = data.increaseResendTimes();
            if (resendTimes < org.apache.inlong.audit.send.SenderGroup.MAX_SEND_TIMES) {
                this.sendData(data.getDataByte());
            }
        } catch (Throwable ex) {
            logger.error(ex.getMessage());
            this.sender.setHasSendError(true);
        }
    }

    /**
     * Handle the packet return exception
     *
     * @param ctx
     * @param e
     */
    public void onExceptionCaught(ChannelHandlerContext ctx, Throwable e) {
        logger.error(e.getCause().getMessage());
        try {
            this.sender.setHasSendError(true);
        } catch (Throwable ex) {
            logger.error(ex.getMessage());
        }
    }

    /**
     * sleep
     *
     * @param millisecond
     */
    private void sleep(int millisecond) {
        try {
            Thread.sleep(millisecond);
        } catch (Throwable e) {
            logger.error(e.getMessage());
        }
    }

    /***
     * set audit config
     * @param config
     */
    public void setAuditConfig(AuditConfig config) {
        auditConfig = config;
    }
}
