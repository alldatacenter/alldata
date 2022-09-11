/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sdk.dataproxy.pb;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.flume.Context;
import org.apache.flume.Transaction;
import org.apache.flume.conf.Configurable;
import org.apache.flume.lifecycle.LifecycleState;
import org.apache.inlong.common.util.NetworkUtils;
import org.apache.inlong.sdk.commons.protocol.SdkEvent;
import org.apache.inlong.sdk.dataproxy.MessageSender;
import org.apache.inlong.sdk.dataproxy.SendMessageCallback;
import org.apache.inlong.sdk.dataproxy.SendResult;
import org.apache.inlong.sdk.dataproxy.network.ProxysdkException;
import org.apache.inlong.sdk.dataproxy.pb.channel.BufferQueueChannel;
import org.apache.inlong.sdk.dataproxy.pb.context.CallbackProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PbProtocolMessageSender
 */
public class PbProtocolMessageSender implements MessageSender, Configurable {

    public static final Logger LOG = LoggerFactory.getLogger(PbProtocolMessageSender.class);

    private String name;
    private String localIp;
    private LifecycleState lifecycleState;
    private Context context;
    private BufferQueueChannel channel;
    private ProxySdkSink sink;

    /**
     * Constructor
     * 
     * @param name
     */
    public PbProtocolMessageSender(String name) {
        lifecycleState = LifecycleState.IDLE;
        this.name = (name == null)
                ? PbProtocolMessageSender.class.getSimpleName() + "-" + Thread.currentThread().getName()
                : name;
        this.localIp = NetworkUtils.getLocalIp();
    }

    /**
     * start
     */
    public void start() {
        lifecycleState = LifecycleState.START;
        this.channel.start();
        this.sink.start();
    }

    /**
     * close
     */
    @Override
    public void close() {
        lifecycleState = LifecycleState.STOP;
        this.sink.stop();
        this.channel.stop();
    }

    /**
     * configure
     * 
     * @param context
     */
    @Override
    public void configure(Context context) {
        this.context = context;
        this.channel = new BufferQueueChannel();
        this.channel.setName(name + "-channel");
        this.channel.configure(context);
        this.sink = new ProxySdkSink();
        this.sink.setName(name + "-sink");
        this.sink.configure(context);
        this.sink.setChannel(channel);
    }

    /**
     * put
     * 
     * @param event
     */
    private void put(CallbackProfile event) {
        Transaction tx = channel.getTransaction();
        tx.begin();
        try {
            channel.put(event);
            tx.commit();
        } catch (Exception e) {
            LOG.error("Put event failed:{}", e);
            try {
                tx.rollback();
            } catch (Throwable ex) {
                LOG.error("Channel take transaction rollback exception:" + name, ex);
            }
            throw e;
        } finally {
            tx.close();
        }
    }

    /**
     * putAll
     * 
     * @param events
     */
    private void putAll(List<CallbackProfile> events) {
        Transaction tx = channel.getTransaction();
        tx.begin();
        try {
            events.forEach((event) -> {
                channel.put(event);
            });
            tx.commit();
        } catch (Exception e) {
            LOG.error("Put event failed:{}", e);
            try {
                tx.rollback();
            } catch (Throwable ex) {
                LOG.error("Channel take transaction rollback exception:" + name, ex);
            }
            throw e;
        } finally {
            tx.close();
        }
    }

    /**
     * get lifecycleState
     * 
     * @return the lifecycleState
     */
    public LifecycleState getLifecycleState() {
        return lifecycleState;
    }

    /**
     * get context
     * 
     * @return the context
     */
    public Context getContext() {
        return context;
    }

    /**
     * sendMessage
     * 
     * @param      body
     * @param      attributes
     * @param      msgUUID
     * @param      timeout
     * @param      timeUnit
     * @return                SendResult
     * @deprecated
     */
    @Override
    public SendResult sendMessage(byte[] body, String attributes, String msgUUID, long timeout, TimeUnit timeUnit) {
        return SendResult.INVALID_ATTRIBUTES;
    }

    /**
     * sendMessage
     * 
     * @param  body
     * @param  groupId
     * @param  streamId
     * @param  dt
     * @param  msgUUID
     * @param  timeout
     * @param  timeUnit
     * @return          SendResult
     */
    @Override
    public SendResult sendMessage(byte[] body, String groupId, String streamId, long dt, String msgUUID, long timeout,
            TimeUnit timeUnit) {
        return this.sendMessage(body, groupId, streamId, dt, msgUUID, timeout, timeUnit, null);
    }

    /**
     * sendMessage
     * 
     * @param  body
     * @param  groupId
     * @param  streamId
     * @param  dt
     * @param  msgUUID
     * @param  timeout
     * @param  timeUnit
     * @param  extraAttrMap
     * @return              SendResult
     */
    @Override
    public SendResult sendMessage(byte[] body, String groupId, String streamId, long dt, String msgUUID, long timeout,
            TimeUnit timeUnit, Map<String, String> extraAttrMap) {
        // prepare
        SdkEvent sdkEvent = new SdkEvent();
        sdkEvent.setInlongGroupId(groupId);
        sdkEvent.setInlongStreamId(streamId);
        sdkEvent.setBody(body);
        sdkEvent.setMsgTime(dt);
        sdkEvent.setSourceIp(localIp);
        if (extraAttrMap != null) {
            sdkEvent.setHeaders(extraAttrMap);
        }
        // callback
        CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<SendResult> refResult = new AtomicReference<>();
        SendMessageCallback callback = new SendMessageCallback() {

            @Override
            public void onMessageAck(SendResult result) {
                refResult.set(SendResult.OK);
                latch.countDown();
            }

            @Override
            public void onException(Throwable e) {
                LOG.error(e.getMessage(), e);
                refResult.set(SendResult.CONNECTION_BREAK);
                latch.countDown();
            }
        };
        CallbackProfile profile = new CallbackProfile(sdkEvent, callback);
        this.put(profile);
        // wait
        try {
            boolean success = latch.await(timeout, timeUnit);
            if (!success) {
                refResult.set(SendResult.TIMEOUT);
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            refResult.set(SendResult.UNKOWN_ERROR);
        }
        return refResult.get();
    }

    /**
     * sendMessage
     * 
     * @param  bodyList
     * @param  groupId
     * @param  streamId
     * @param  dt
     * @param  msgUUID
     * @param  timeout
     * @param  timeUnit
     * @return          SendResult
     */
    @Override
    public SendResult sendMessage(List<byte[]> bodyList, String groupId, String streamId, long dt, String msgUUID,
            long timeout, TimeUnit timeUnit) {
        return this.sendMessage(bodyList, groupId, streamId, dt, msgUUID, timeout, timeUnit, null);
    }

    /**
     * sendMessage
     * 
     * @param  bodyList
     * @param  groupId
     * @param  streamId
     * @param  dt
     * @param  msgUUID
     * @param  timeout
     * @param  timeUnit
     * @param  extraAttrMap
     * @return              SendResult
     */
    @Override
    public SendResult sendMessage(List<byte[]> bodyList, String groupId, String streamId, long dt, String msgUUID,
            long timeout, TimeUnit timeUnit, Map<String, String> extraAttrMap) {
        final AtomicReference<SendResult> refResult = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(bodyList.size());
        // prepare
        List<CallbackProfile> events = new ArrayList<>(bodyList.size());
        for (byte[] body : bodyList) {
            SdkEvent sdkEvent = new SdkEvent();
            sdkEvent.setInlongGroupId(groupId);
            sdkEvent.setInlongStreamId(streamId);
            sdkEvent.setBody(body);
            sdkEvent.setMsgTime(dt);
            sdkEvent.setSourceIp(localIp);
            if (extraAttrMap != null) {
                sdkEvent.setHeaders(extraAttrMap);
            }
            // callback
            SendMessageCallback callback = new SendMessageCallback() {

                @Override
                public void onMessageAck(SendResult result) {
                    refResult.set(SendResult.OK);
                    latch.countDown();
                }

                @Override
                public void onException(Throwable e) {
                    LOG.error(e.getMessage(), e);
                    refResult.set(SendResult.CONNECTION_BREAK);
                    latch.countDown();
                }
            };
            CallbackProfile profile = new CallbackProfile(sdkEvent, callback);
            events.add(profile);
        }
        this.putAll(events);
        // wait
        try {
            boolean success = latch.await(timeout, timeUnit);
            if (!success) {
                refResult.set(SendResult.TIMEOUT);
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            refResult.set(SendResult.UNKOWN_ERROR);
        }
        return refResult.get();
    }

    /**
     * asyncSendMessage
     * 
     * @param      callback
     * @param      body
     * @param      attributes
     * @param      msgUUID
     * @param      timeout
     * @param      timeUnit
     * @throws     ProxysdkException
     * @deprecated
     */
    @Override
    public void asyncSendMessage(SendMessageCallback callback, byte[] body, String attributes, String msgUUID,
            long timeout, TimeUnit timeUnit) throws ProxysdkException {
        throw new ProxysdkException("Not support");
    }

    /**
     * asyncSendMessage
     * 
     * @param  callback
     * @param  body
     * @param  groupId
     * @param  streamId
     * @param  dt
     * @param  msgUUID
     * @param  timeout
     * @param  timeUnit
     * @param  extraAttrMap
     * @throws ProxysdkException
     */
    @Override
    public void asyncSendMessage(SendMessageCallback callback, byte[] body, String groupId, String streamId, long dt,
            String msgUUID, long timeout, TimeUnit timeUnit, Map<String, String> extraAttrMap)
            throws ProxysdkException {
        SdkEvent sdkEvent = new SdkEvent();
        sdkEvent.setInlongGroupId(groupId);
        sdkEvent.setInlongStreamId(streamId);
        sdkEvent.setBody(body);
        sdkEvent.setMsgTime(dt);
        sdkEvent.setSourceIp(localIp);
        if (extraAttrMap != null) {
            sdkEvent.setHeaders(extraAttrMap);
        }
        CallbackProfile profile = new CallbackProfile(sdkEvent, callback);
        this.put(profile);
    }

    /**
     * asyncSendMessage
     * 
     * @param  callback
     * @param  body
     * @param  groupId
     * @param  streamId
     * @param  dt
     * @param  msgUUID
     * @param  timeout
     * @param  timeUnit
     * @throws ProxysdkException
     */
    @Override
    public void asyncSendMessage(SendMessageCallback callback, byte[] body, String groupId, String streamId, long dt,
            String msgUUID, long timeout, TimeUnit timeUnit) throws ProxysdkException {
        this.asyncSendMessage(callback, body, groupId, streamId, dt, msgUUID, timeout, timeUnit, null);
    }

    /**
     * asyncSendMessage
     * 
     * @param  callback
     * @param  bodyList
     * @param  groupId
     * @param  streamId
     * @param  dt
     * @param  msgUUID
     * @param  timeout
     * @param  timeUnit
     * @throws ProxysdkException
     */
    @Override
    public void asyncSendMessage(SendMessageCallback callback, List<byte[]> bodyList, String groupId, String streamId,
            long dt, String msgUUID, long timeout, TimeUnit timeUnit) throws ProxysdkException {
        this.asyncSendMessage(callback, bodyList, groupId, streamId, dt, msgUUID, timeout, timeUnit, null);
    }

    /**
     * asyncSendMessage
     * 
     * @param  callback
     * @param  bodyList
     * @param  groupId
     * @param  streamId
     * @param  dt
     * @param  msgUUID
     * @param  timeout
     * @param  timeUnit
     * @param  extraAttrMap
     * @throws ProxysdkException
     */
    @Override
    public void asyncSendMessage(SendMessageCallback callback, List<byte[]> bodyList, String groupId, String streamId,
            long dt, String msgUUID, long timeout, TimeUnit timeUnit, Map<String, String> extraAttrMap)
            throws ProxysdkException {
        List<CallbackProfile> events = new ArrayList<>(bodyList.size());
        for (byte[] body : bodyList) {
            SdkEvent sdkEvent = new SdkEvent();
            sdkEvent.setInlongGroupId(groupId);
            sdkEvent.setInlongStreamId(streamId);
            sdkEvent.setBody(body);
            sdkEvent.setMsgTime(dt);
            sdkEvent.setSourceIp(localIp);
            if (extraAttrMap != null) {
                sdkEvent.setHeaders(extraAttrMap);
            }
            CallbackProfile profile = new CallbackProfile(sdkEvent, callback);
            events.add(profile);
        }
        this.putAll(events);
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
    public void asyncSendMessage(String inlongGroupId, String inlongStreamId, byte[] body, SendMessageCallback callback)
            throws ProxysdkException {
        this.asyncSendMessage(callback, body, inlongGroupId, inlongStreamId, System.currentTimeMillis(), null, 0L, null,
                null);
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
        this.asyncSendMessage(callback, bodyList, inlongGroupId, inlongStreamId, System.currentTimeMillis(), null, 0L,
                null, null);
    }

}
