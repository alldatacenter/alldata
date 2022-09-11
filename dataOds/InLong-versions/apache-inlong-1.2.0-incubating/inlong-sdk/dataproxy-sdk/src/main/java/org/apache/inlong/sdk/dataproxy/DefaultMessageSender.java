/*
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

package org.apache.inlong.sdk.dataproxy;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.inlong.sdk.dataproxy.codec.EncodeObject;
import org.apache.inlong.sdk.dataproxy.config.ProxyConfigEntry;
import org.apache.inlong.sdk.dataproxy.config.ProxyConfigManager;
import org.apache.inlong.sdk.dataproxy.network.ProxysdkException;
import org.apache.inlong.sdk.dataproxy.network.Sender;
import org.apache.inlong.sdk.dataproxy.network.SequentialID;
import org.apache.inlong.sdk.dataproxy.network.Utils;
import org.apache.inlong.sdk.dataproxy.threads.IndexCollectThread;
import org.apache.inlong.sdk.dataproxy.threads.ManagerFetcherThread;
import org.apache.inlong.sdk.dataproxy.utils.ProxyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultMessageSender implements MessageSender {
    private static final Logger logger = LoggerFactory.getLogger(DefaultMessageSender.class);
    private static final long DEFAULT_SEND_TIMEOUT = 100;
    private static final TimeUnit DEFAULT_SEND_TIMEUNIT = TimeUnit.MILLISECONDS;
    private final Sender sender;
    private final SequentialID idGenerator;
    private String groupId;
    private int msgtype = ConfigConstants.MSG_TYPE;
    private boolean isCompress = true;
    private boolean isGroupIdTransfer = false;
    private boolean isReport = false;
    private boolean isSupportLF = false;
    private int cpsSize = ConfigConstants.COMPRESS_SIZE;

    private static final ConcurrentHashMap<String, DefaultMessageSender> cacheSender =
            new ConcurrentHashMap<>();

    private final IndexCollectThread indexCol;
    /* Store index <groupId_streamId,cnt>*/
    private final Map<String, Long> storeIndex = new ConcurrentHashMap<String, Long>();

    private static final AtomicBoolean ManagerFetcherThreadStarted = new AtomicBoolean(false);

    private static ManagerFetcherThread managerFetcherThread;

    public boolean isSupportLF() {
        return isSupportLF;
    }

    public void setSupportLF(boolean supportLF) {
        isSupportLF = supportLF;
    }

    public boolean isGroupIdTransfer() {
        return isGroupIdTransfer;
    }

    public void setGroupIdTransfer(boolean isGroupIdTransfer) {
        this.isGroupIdTransfer = isGroupIdTransfer;
    }

    public boolean isReport() {
        return isReport;
    }

    public void setReport(boolean isReport) {
        this.isReport = isReport;
    }

    public int getCpsSize() {
        return cpsSize;
    }

    public void setCpsSize(int cpsSize) {
        this.cpsSize = cpsSize;
    }

    public int getMsgtype() {
        return msgtype;
    }

    public void setMsgtype(int msgtype) {
        this.msgtype = msgtype;
    }

    public boolean isCompress() {
        return isCompress;
    }

    public void setCompress(boolean isCompress) {
        this.isCompress = isCompress;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public DefaultMessageSender(ProxyClientConfig configure) throws Exception {
        this(configure, null);
    }

    public DefaultMessageSender(ProxyClientConfig configure, ThreadFactory selfDefineFactory) throws Exception {
        ProxyUtils.validClientConfig(configure);
        sender = new Sender(configure, selfDefineFactory);
        idGenerator = new SequentialID(Utils.getLocalIp());
        groupId = configure.getGroupId();
        indexCol = new IndexCollectThread(storeIndex);
        indexCol.start();

        if (configure.isEnableSaveManagerVIps()
                && configure.isLocalVisit()
                && ManagerFetcherThreadStarted.compareAndSet(false, true)) {
            managerFetcherThread = new ManagerFetcherThread(configure);
            managerFetcherThread.start();
        }
    }

    /**
     * generate by cluster id
     *
     * @param configure - sender
     * @return - sender
     */
    public static DefaultMessageSender generateSenderByClusterId(
            ProxyClientConfig configure) throws Exception {

        return generateSenderByClusterId(configure, null);
    }

    /**
     *  generate by cluster id
     *
     * @param configure         - sender
     * @param selfDefineFactory - sender factory
     * @return - sender
     */
    public static DefaultMessageSender generateSenderByClusterId(ProxyClientConfig configure,
            ThreadFactory selfDefineFactory) throws Exception {
        ProxyConfigManager proxyConfigManager = new ProxyConfigManager(configure,
                Utils.getLocalIp(), null);
        proxyConfigManager.setGroupId(configure.getGroupId());
        ProxyConfigEntry entry = proxyConfigManager.getGroupIdConfigure();
        DefaultMessageSender sender = cacheSender.get(entry.getClusterId());
        if (sender != null) {
            return sender;
        } else {
            DefaultMessageSender tmpMessageSender =
                    new DefaultMessageSender(configure, selfDefineFactory);
            cacheSender.put(entry.getClusterId(), tmpMessageSender);
            return tmpMessageSender;
        }
    }

    /**
     * finally clean up
     */
    public static void finallyCleanup() {
        for (DefaultMessageSender sender : cacheSender.values()) {
            sender.close();
        }
        cacheSender.clear();
    }

    public String getSDKVersion() {
        return ConfigConstants.PROXY_SDK_VERSION;
    }

    @Deprecated
    public SendResult sendMessage(byte[] body, String attributes, String msgUUID,
                                  long timeout, TimeUnit timeUnit) {
        return sender.syncSendMessage(new EncodeObject(body, attributes,
                idGenerator.getNextId()), msgUUID, timeout, timeUnit);
    }

    public SendResult sendMessage(byte[] body, String groupId, String streamId, long dt, String msgUUID,
                                  long timeout, TimeUnit timeUnit) {
        dt = ProxyUtils.covertZeroDt(dt);
        if (!ProxyUtils.isBodyValid(body) || !ProxyUtils.isDtValid(dt)) {
            return SendResult.INVALID_ATTRIBUTES;
        }
        addIndexCnt(groupId, streamId, 1);

        boolean isCompressEnd = (isCompress && (body.length > cpsSize));

        if (msgtype == 7 || msgtype == 8) {
            EncodeObject encodeObject = new EncodeObject(body, msgtype, isCompressEnd, isReport,
                    isGroupIdTransfer, dt / 1000, idGenerator.getNextInt(), groupId, streamId, "");
            encodeObject.setSupportLF(isSupportLF);
            return sender.syncSendMessage(encodeObject, msgUUID, timeout, timeUnit);
        } else if (msgtype == 3 || msgtype == 5) {
            if (isCompressEnd) {
                return sender.syncSendMessage(new EncodeObject(body, "groupId=" + groupId
                        + "&streamId=" + streamId + "&dt=" + dt + "&cp=snappy",
                        idGenerator.getNextId(), this.getMsgtype(), true, groupId), msgUUID, timeout, timeUnit);
            } else {
                return sender.syncSendMessage(new EncodeObject(body, "groupId=" + groupId
                        + "&streamId=" + streamId + "&dt=" + dt,
                        idGenerator.getNextId(), this.getMsgtype(), false, groupId), msgUUID, timeout, timeUnit);
            }
        }

        return null;
    }

    public SendResult sendMessage(byte[] body, String groupId, String streamId, long dt, String msgUUID,
                                  long timeout, TimeUnit timeUnit, Map<String, String> extraAttrMap) {

        dt = ProxyUtils.covertZeroDt(dt);
        if (!ProxyUtils.isBodyValid(body) || !ProxyUtils.isDtValid(dt) || !ProxyUtils.isAttrKeysValid(extraAttrMap)) {
            return SendResult.INVALID_ATTRIBUTES;
        }
        addIndexCnt(groupId, streamId, 1);

        StringBuilder attrs = ProxyUtils.convertAttrToStr(extraAttrMap);

        boolean isCompressEnd = (isCompress && (body.length > cpsSize));

        if (msgtype == 7 || msgtype == 8) {
            EncodeObject encodeObject = new EncodeObject(body, msgtype, isCompressEnd, isReport,
                    isGroupIdTransfer, dt / 1000,
                    idGenerator.getNextInt(), groupId, streamId, attrs.toString());
            encodeObject.setSupportLF(isSupportLF);
            return sender.syncSendMessage(encodeObject, msgUUID, timeout, timeUnit);
        } else if (msgtype == 3 || msgtype == 5) {
            attrs.append("&groupId=").append(groupId).append("&streamId=").append(streamId).append("&dt=").append(dt);
            if (isCompressEnd) {
                attrs.append("&cp=snappy");
                return sender.syncSendMessage(new EncodeObject(body, attrs.toString(),
                        idGenerator.getNextId(), this.getMsgtype(), true, groupId), msgUUID, timeout, timeUnit);
            } else {
                return sender.syncSendMessage(new EncodeObject(body, attrs.toString(),
                        idGenerator.getNextId(), this.getMsgtype(), false, groupId), msgUUID, timeout, timeUnit);
            }
        }
        return null;

    }

    public SendResult sendMessage(List<byte[]> bodyList, String groupId, String streamId, long dt, String msgUUID,
                                  long timeout, TimeUnit timeUnit) {
        dt = ProxyUtils.covertZeroDt(dt);
        if (!ProxyUtils.isBodyValid(bodyList) || !ProxyUtils.isDtValid(dt)) {
            return SendResult.INVALID_ATTRIBUTES;
        }
        addIndexCnt(groupId, streamId, bodyList.size());

        if (msgtype == 7 || msgtype == 8) {
            EncodeObject encodeObject = new EncodeObject(bodyList, msgtype, isCompress, isReport,
                    isGroupIdTransfer, dt / 1000,
                    idGenerator.getNextInt(), groupId, streamId, "");
            encodeObject.setSupportLF(isSupportLF);
            return sender.syncSendMessage(encodeObject, msgUUID, timeout, timeUnit);
        } else if (msgtype == 3 || msgtype == 5) {
            if (isCompress) {
                return sender.syncSendMessage(new EncodeObject(bodyList, "groupId=" + groupId + "&streamId=" + streamId
                        + "&dt=" + dt + "&cp=snappy" + "&cnt=" + bodyList.size(),
                        idGenerator.getNextId(), this.getMsgtype(), true, groupId), msgUUID, timeout, timeUnit);
            } else {
                return sender.syncSendMessage(new EncodeObject(bodyList, "groupId=" + groupId + "&streamId=" + streamId
                        + "&dt=" + dt + "&cnt=" + bodyList.size(),
                        idGenerator.getNextId(), this.getMsgtype(), false, groupId), msgUUID, timeout, timeUnit);
            }
        }
        return null;
    }

    public SendResult sendMessage(List<byte[]> bodyList, String groupId, String streamId, long dt, String msgUUID,
                                  long timeout, TimeUnit timeUnit, Map<String, String> extraAttrMap) {
        dt = ProxyUtils.covertZeroDt(dt);
        if (!ProxyUtils.isBodyValid(bodyList) || !ProxyUtils.isDtValid(dt)
            || !ProxyUtils.isAttrKeysValid(extraAttrMap)) {
            return SendResult.INVALID_ATTRIBUTES;
        }
        addIndexCnt(groupId, streamId, bodyList.size());
        StringBuilder attrs = ProxyUtils.convertAttrToStr(extraAttrMap);

        if (msgtype == 7 || msgtype == 8) {
            EncodeObject encodeObject = new EncodeObject(bodyList, msgtype, isCompress, isReport,
                    isGroupIdTransfer, dt / 1000,
                    idGenerator.getNextInt(), groupId, streamId, attrs.toString());
            encodeObject.setSupportLF(isSupportLF);
            return sender.syncSendMessage(encodeObject, msgUUID, timeout, timeUnit);
        } else if (msgtype == 3 || msgtype == 5) {
            attrs.append("&groupId=").append(groupId).append("&streamId=").append(streamId)
                    .append("&dt=").append(dt).append("&cnt=").append(bodyList.size());
            if (isCompress) {
                attrs.append("&cp=snappy");
                return sender.syncSendMessage(new EncodeObject(bodyList, attrs.toString(),
                        idGenerator.getNextId(), this.getMsgtype(), true, groupId), msgUUID, timeout, timeUnit);
            } else {
                return sender.syncSendMessage(new EncodeObject(bodyList, attrs.toString(),
                        idGenerator.getNextId(), this.getMsgtype(), false, groupId), msgUUID, timeout, timeUnit);
            }
        }
        return null;
    }

    @Deprecated
    public void asyncSendMessage(SendMessageCallback callback, byte[] body, String attributes, String msgUUID,
                                 long timeout, TimeUnit timeUnit) throws ProxysdkException {
        sender.asyncSendMessage(new EncodeObject(body, attributes, idGenerator.getNextId()),
                callback, msgUUID, timeout, timeUnit);
    }

    public void asyncSendMessage(SendMessageCallback callback, byte[] body,
                                 String groupId, String streamId, long dt, String msgUUID,
                                 long timeout, TimeUnit timeUnit) throws ProxysdkException {
        dt = ProxyUtils.covertZeroDt(dt);
        if (!ProxyUtils.isBodyValid(body) || !ProxyUtils.isDtValid(dt)) {
            throw new ProxysdkException(SendResult.INVALID_ATTRIBUTES.toString());
        }
        addIndexCnt(groupId, streamId, 1);

        boolean isCompressEnd = (isCompress && (body.length > cpsSize));
        if (msgtype == 7 || msgtype == 8) {
            EncodeObject encodeObject = new EncodeObject(body, this.getMsgtype(), isCompressEnd, isReport,
                    isGroupIdTransfer, dt / 1000, idGenerator.getNextInt(),
                    groupId, streamId, "");
            encodeObject.setSupportLF(isSupportLF);
            sender.asyncSendMessage(encodeObject, callback, msgUUID, timeout, timeUnit);
        } else if (msgtype == 3 || msgtype == 5) {
            if (isCompressEnd) {
                sender.asyncSendMessage(new EncodeObject(body, "groupId="
                        + groupId + "&streamId=" + streamId + "&dt=" + dt + "&cp=snappy",
                        idGenerator.getNextId(), this.getMsgtype(), true, groupId),
                        callback, msgUUID, timeout, timeUnit);
            } else {
                sender.asyncSendMessage(
                        new EncodeObject(body, "groupId=" + groupId + "&streamId="
                                + streamId + "&dt=" + dt, idGenerator.getNextId(),
                                this.getMsgtype(), false, groupId), callback,
                        msgUUID, timeout, timeUnit);
            }
        }

    }

    public void asyncSendMessage(SendMessageCallback callback,
                                 byte[] body, String groupId, String streamId, long dt, String msgUUID,
                                 long timeout, TimeUnit timeUnit,
                                 Map<String, String> extraAttrMap) throws ProxysdkException {
        dt = ProxyUtils.covertZeroDt(dt);
        if (!ProxyUtils.isBodyValid(body) || !ProxyUtils.isDtValid(dt) || !ProxyUtils.isAttrKeysValid(extraAttrMap)) {
            throw new ProxysdkException(SendResult.INVALID_ATTRIBUTES.toString());
        }
        addIndexCnt(groupId, streamId, 1);
        StringBuilder attrs = ProxyUtils.convertAttrToStr(extraAttrMap);

        boolean isCompressEnd = (isCompress && (body.length > cpsSize));
        if (msgtype == 7 || msgtype == 8) {
            EncodeObject encodeObject = new EncodeObject(body, this.getMsgtype(), isCompressEnd,
                    isReport, isGroupIdTransfer, dt / 1000, idGenerator.getNextInt(),
                    groupId, streamId, attrs.toString());
            encodeObject.setSupportLF(isSupportLF);
            sender.asyncSendMessage(encodeObject, callback, msgUUID, timeout, timeUnit);
        } else if (msgtype == 3 || msgtype == 5) {
            attrs.append("&groupId=").append(groupId).append("&streamId=").append(streamId).append("&dt=").append(dt);
            if (isCompressEnd) {
                attrs.append("&cp=snappy");
                sender.asyncSendMessage(new EncodeObject(body, attrs.toString(),
                                idGenerator.getNextId(), this.getMsgtype(), true, groupId),
                        callback, msgUUID, timeout, timeUnit);
            } else {
                sender.asyncSendMessage(new EncodeObject(body, attrs.toString(), idGenerator.getNextId(),
                                this.getMsgtype(), false, groupId),
                        callback, msgUUID, timeout, timeUnit);
            }
        }
    }

    public void asyncSendMessage(SendMessageCallback callback, List<byte[]> bodyList,
                                 String groupId, String streamId, long dt, String msgUUID,
                                 long timeout, TimeUnit timeUnit) throws ProxysdkException {
        dt = ProxyUtils.covertZeroDt(dt);
        if (!ProxyUtils.isBodyValid(bodyList) || !ProxyUtils.isDtValid(dt)) {
            throw new ProxysdkException(SendResult.INVALID_ATTRIBUTES.toString());
        }
        addIndexCnt(groupId, streamId, bodyList.size());
        if (msgtype == 7 || msgtype == 8) {
            EncodeObject encodeObject = new EncodeObject(bodyList, this.getMsgtype(), isCompress,
                    isReport, isGroupIdTransfer, dt / 1000, idGenerator.getNextInt(),
                    groupId, streamId, "");
            encodeObject.setSupportLF(isSupportLF);
            sender.asyncSendMessage(encodeObject, callback, msgUUID, timeout, timeUnit);
        } else if (msgtype == 3 || msgtype == 5) {
            if (isCompress) {
                sender.asyncSendMessage(
                        new EncodeObject(bodyList, "groupId=" + groupId + "&streamId=" + streamId
                                + "&dt=" + dt + "&cp=snappy" + "&cnt=" + bodyList.size(),
                                idGenerator.getNextId(), this.getMsgtype(),
                                true, groupId), callback, msgUUID, timeout, timeUnit);
            } else {
                sender.asyncSendMessage(
                        new EncodeObject(bodyList, "groupId=" + groupId + "&streamId="
                                + streamId + "&dt=" + dt + "&cnt=" + bodyList.size(),
                                idGenerator.getNextId(), this.getMsgtype(),
                                false, groupId), callback, msgUUID, timeout, timeUnit);
            }
        }
    }

    public void asyncSendMessage(SendMessageCallback callback,
                                 List<byte[]> bodyList, String groupId, String streamId, long dt, String msgUUID,
                                 long timeout, TimeUnit timeUnit,
                                 Map<String, String> extraAttrMap) throws ProxysdkException {
        dt = ProxyUtils.covertZeroDt(dt);
        if (!ProxyUtils.isBodyValid(bodyList) || !ProxyUtils.isDtValid(dt)
            || !ProxyUtils.isAttrKeysValid(extraAttrMap)) {
            throw new ProxysdkException(SendResult.INVALID_ATTRIBUTES.toString());
        }
        addIndexCnt(groupId, streamId, bodyList.size());
        StringBuilder attrs = ProxyUtils.convertAttrToStr(extraAttrMap);

        if (msgtype == 7 || msgtype == 8) {
//            if (!isGroupIdTransfer)
            EncodeObject encodeObject = new EncodeObject(bodyList, this.getMsgtype(),
                    isCompress, isReport, isGroupIdTransfer, dt / 1000, idGenerator.getNextInt(),
                    groupId, streamId, attrs.toString());
            encodeObject.setSupportLF(isSupportLF);
            sender.asyncSendMessage(encodeObject, callback, msgUUID, timeout, timeUnit);
        } else if (msgtype == 3 || msgtype == 5) {
            attrs.append("&groupId=").append(groupId).append("&streamId=").append(streamId)
                    .append("&dt=").append(dt).append("&cnt=").append(bodyList.size());
            if (isCompress) {
                attrs.append("&cp=snappy");
                sender.asyncSendMessage(new EncodeObject(bodyList, attrs.toString(), idGenerator.getNextId(),
                        this.getMsgtype(), true, groupId), callback, msgUUID, timeout, timeUnit);
            } else {
                sender.asyncSendMessage(new EncodeObject(bodyList, attrs.toString(), idGenerator.getNextId(),
                        this.getMsgtype(), false, groupId), callback, msgUUID, timeout, timeUnit);
            }
        }

    }

    /**
     * asyncSendMessage
     * 
     * @param  inlongGroupId
     * @param  inlongStreamId
     * @param  body
     * @param  callback
     * @throws ProxysdkException
     */
    @Override
    public void asyncSendMessage(String inlongGroupId, String inlongStreamId, byte[] body,
            SendMessageCallback callback) throws ProxysdkException {
        this.asyncSendMessage(callback, body, inlongGroupId, inlongStreamId, System.currentTimeMillis(),
                idGenerator.getNextId(), DEFAULT_SEND_TIMEOUT, DEFAULT_SEND_TIMEUNIT);
    }

    /**
     * asyncSendMessage
     * 
     * @param  inlongGroupId
     * @param  inlongStreamId
     * @param  bodyList
     * @param  callback
     * @throws ProxysdkException
     */
    @Override
    public void asyncSendMessage(String inlongGroupId, String inlongStreamId, List<byte[]> bodyList,
            SendMessageCallback callback) throws ProxysdkException {
        this.asyncSendMessage(callback, bodyList, inlongGroupId, inlongStreamId, System.currentTimeMillis(),
                idGenerator.getNextId(), DEFAULT_SEND_TIMEOUT, DEFAULT_SEND_TIMEUNIT);
    }

    private void addIndexCnt(String groupId, String streamId, long cnt) {
        try {
            String key = groupId + "|" + streamId;
            if (storeIndex.containsKey(key)) {
                long sum = storeIndex.get(key);
                storeIndex.put(key, sum + cnt);
            } else {
                storeIndex.put(key, cnt);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    public void asyncsendMessageData(FileCallback callback, List<byte[]> bodyList, String groupId,
                                     String streamId, long dt, int sid, boolean isSupportLF, String msgUUID,
                                     long timeout, TimeUnit timeUnit,
                                     Map<String, String> extraAttrMap) throws ProxysdkException {
        dt = ProxyUtils.covertZeroDt(dt);
        if (!ProxyUtils.isBodyValid(bodyList) || !ProxyUtils.isDtValid(dt)
            || !ProxyUtils.isAttrKeysValid(extraAttrMap)) {
            throw new ProxysdkException(SendResult.INVALID_ATTRIBUTES.toString());
        }
        addIndexCnt(groupId, streamId, bodyList.size());

        StringBuilder attrs = ProxyUtils.convertAttrToStr(extraAttrMap);

        if (msgtype == 7 || msgtype == 8) {
            EncodeObject encodeObject = new EncodeObject(bodyList, msgtype,
                    isCompress, isReport, isGroupIdTransfer,
                    dt / 1000, sid, groupId, streamId, attrs.toString(), "data", "");
            encodeObject.setSupportLF(isSupportLF);
            sender.asyncSendMessageIndex(encodeObject, callback, msgUUID, timeout, timeUnit);
        }
    }

    private void asyncSendMetric(FileCallback callback, byte[] body, String groupId,
                                 String streamId, long dt, int sid, String ip, String msgUUID,
                                 long timeout, TimeUnit timeUnit, String messageKey) throws ProxysdkException {
        dt = ProxyUtils.covertZeroDt(dt);
        if (!ProxyUtils.isBodyValid(body) || !ProxyUtils.isDtValid(dt)) {
            throw new ProxysdkException(SendResult.INVALID_ATTRIBUTES.toString());
        }
        boolean isCompressEnd = false;
        if (msgtype == 7 || msgtype == 8) {
            sender.asyncSendMessageIndex(new EncodeObject(body, msgtype, isCompressEnd,
                    isReport, isGroupIdTransfer, dt / 1000,
                    sid, groupId, streamId, "", messageKey, ip), callback, msgUUID, timeout, timeUnit);
        }
    }

    public void asyncsendMessageProxy(FileCallback callback, byte[] body, String groupId, String streamId,
                                    long dt, int sid, String ip, String msgUUID,
                                    long timeout, TimeUnit timeUnit) throws ProxysdkException {
        asyncSendMetric(callback, body, groupId, streamId, dt, sid, ip, msgUUID, timeout, timeUnit, "minute");
    }

    public void asyncsendMessageFile(FileCallback callback, byte[] body, String groupId,
                                     String streamId, long dt, int sid, String msgUUID,
                                     long timeout, TimeUnit timeUnit) throws ProxysdkException {
        asyncSendMetric(callback, body, groupId, streamId, dt, sid, "", msgUUID, timeout, timeUnit, "file");
    }

    public String sendMessageData(List<byte[]> bodyList, String groupId,
                                  String streamId, long dt, int sid, boolean isSupportLF, String msgUUID,
                                  long timeout, TimeUnit timeUnit, Map<String, String> extraAttrMap) {
        dt = ProxyUtils.covertZeroDt(dt);
        if (!ProxyUtils.isBodyValid(bodyList) || !ProxyUtils.isDtValid(dt)
            || !ProxyUtils.isAttrKeysValid(extraAttrMap)) {
            return SendResult.INVALID_ATTRIBUTES.toString();
        }
        addIndexCnt(groupId, streamId, bodyList.size());

        StringBuilder attrs = ProxyUtils.convertAttrToStr(extraAttrMap);

        if (msgtype == 7 || msgtype == 8) {
            EncodeObject encodeObject = new EncodeObject(bodyList, msgtype, isCompress,
                    isReport, isGroupIdTransfer, dt / 1000,
                    sid, groupId, streamId, attrs.toString(), "data", "");
            encodeObject.setSupportLF(isSupportLF);
            return sender.syncSendMessageIndex(encodeObject, msgUUID, timeout, timeUnit);
        }
        return null;
    }

    private String sendMetric(byte[] body, String groupId, String streamId, long dt, int sid, String ip, String msgUUID,
                              long timeout, TimeUnit timeUnit, String messageKey) {
        dt = ProxyUtils.covertZeroDt(dt);
        if (!ProxyUtils.isBodyValid(body) || !ProxyUtils.isDtValid(dt)) {
            return SendResult.INVALID_ATTRIBUTES.toString();
        }
        if (msgtype == 7 || msgtype == 8) {
            EncodeObject encodeObject = new EncodeObject(body, msgtype, false, isReport,
                    isGroupIdTransfer, dt / 1000, sid, groupId, streamId, "", messageKey, ip);
            return sender.syncSendMessageIndex(encodeObject, msgUUID, timeout, timeUnit);
        }
        return null;
    }

    public String sendMessageProxy(byte[] body, String groupId, String streamId,
                                   long dt, int sid, String ip, String msgUUID,
                                 long timeout, TimeUnit timeUnit) {
        return sendMetric(body, groupId, streamId, dt, sid, ip, msgUUID, timeout, timeUnit, "minute");
    }

    public String sendMessageFile(byte[] body, String groupId, String streamId, long dt, int sid, String msgUUID,
                                  long timeout, TimeUnit timeUnit) {
        return sendMetric(body, groupId, streamId, dt, sid, "", msgUUID, timeout, timeUnit, "file");
    }

    private void shutdownInternalThreads() {
        indexCol.shutDown();
        managerFetcherThread.shutdown();
        ManagerFetcherThreadStarted.set(false);
    }

    public void close() {
        logger.info("ready to close resources, may need five minutes !");
        if (sender.getClusterId() != null) {
            cacheSender.remove(sender.getClusterId());
        }
        sender.close();
        shutdownInternalThreads();
    }
}
