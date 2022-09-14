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

import org.apache.flume.lifecycle.LifecycleState;
import org.apache.inlong.sdk.commons.protocol.EventUtils;
import org.apache.inlong.sdk.commons.protocol.ProxySdk.INLONG_COMPRESSED_TYPE;
import org.apache.inlong.sdk.commons.protocol.ProxySdk.MessagePack;
import org.apache.inlong.sdk.commons.protocol.SdkEvent;
import org.apache.inlong.sdk.dataproxy.pb.context.SdkProfile;
import org.apache.inlong.sdk.dataproxy.pb.context.SdkSinkContext;
import org.apache.inlong.sdk.dataproxy.pb.dispatch.DispatchProfile;
import org.apache.inlong.sdk.dataproxy.pb.network.IpPort;
import org.apache.inlong.sdk.dataproxy.pb.network.TcpResult;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;

/**
 * SdkChannelWorker
 */
public class SdkChannelWorker extends Thread {

    public static final Logger LOG = LoggerFactory.getLogger(SdkChannelWorker.class);
    public static final int MAX_TRY_TIMES = 3;

    // manager
    private final SdkProxyChannelManager manager;
    private SdkSinkContext context;
    private LifecycleState status;

    // package buffer
    private ChannelBuffer totalBuffer = ChannelBuffers.dynamicBuffer();

    /**
     * Constructor
     * 
     * @param manager
     */
    public SdkChannelWorker(SdkProxyChannelManager manager, int index) {
        super(manager.getProxyClusterId() + "-worker-" + index);
        this.manager = manager;
        this.context = manager.getContext();
        //
        this.status = LifecycleState.IDLE;
    }

    /**
     * run
     */
    @Override
    public void run() {
        status = LifecycleState.START;
        LOG.info("start to TdbankChannelWorker:{},status:{}", this.getName(), status);
        while (status == LifecycleState.START) {
            try {
                this.packAndSend();
            } catch (Throwable t) {
                LOG.error(t.getMessage(), t);
            }
        }
    }

    private void packAndSend() throws InterruptedException {
        DispatchProfile currentRecord = manager.takeDispatchQueue();
        try {
            if (currentRecord == null) {
                Thread.sleep(manager.getContext().getProcessInterval());
                return;
            }

            // prepare
            String inlongGroupId = currentRecord.getInlongGroupId();
            String inlongStreamId = currentRecord.getInlongStreamId();
            INLONG_COMPRESSED_TYPE compressedType = context.getCompressedType();
            List<SdkEvent> events = new ArrayList<>(currentRecord.getEvents().size());
            currentRecord.getEvents().forEach((value) -> {
                events.add(value.getProfile().getEvent());
            });
            // pack
            MessagePack packObject = EventUtils.encodeSdkEvents(inlongGroupId, inlongStreamId, compressedType, events);
            byte[] packBytes = packObject.toByteArray();
            totalBuffer.clear();
            // total length
            totalBuffer.writeInt(packBytes.length + SdkSinkContext.PACK_VERSION_LENGTH);
            // version
            totalBuffer.writeShort(SdkSinkContext.PACK_VERSION);
            // body
            totalBuffer.writeBytes(packObject.toByteArray());

            // archive
            long sdkPackId = manager.nextPackId();
            SdkProfile sdkProfile = new SdkProfile(currentRecord, sdkPackId);
            manager.putWaitCompletedProfile(sdkProfile);

            // send data
            context.addSendMetric(currentRecord, manager.getProxyClusterId());
            TcpResult result = manager.getSender().send(totalBuffer);
            // send fail
            if (!result.result) {
                for (int i = 0; i < MAX_TRY_TIMES; i++) {
                    result = manager.getSender().send(totalBuffer);
                    if (result.result) {
                        break;
                    }
                    Thread.sleep(manager.getContext().getProcessInterval());
                }
            }
            // check result
            if (!this.checkSendResult(currentRecord, result, result.ipPort, sdkProfile)) {
                LOG.info("proxyClusterId:{},packAndSend:{},result:{}", manager.getProxyClusterId(),
                        currentRecord.getCount(),
                        JSON.toJSONString(result));
                return;
            }
            // record ip port
            sdkProfile.setIpPort(result.ipPort);
        } catch (Throwable e) {
            LOG.error(e.getMessage(), e);
            if (currentRecord != null) {
                manager.offerDispatchQueue(currentRecord);
            }
            try {
                Thread.sleep(manager.getContext().getProcessInterval());
            } catch (InterruptedException e1) {
                LOG.error(e1.getMessage(), e1);
            }
        }
    }

    /**
     * checkSendResult
     * 
     * @param  currentRecord
     * @param  result
     * @param  ipPort
     * @param  tProfile
     * @return
     */
    private boolean checkSendResult(DispatchProfile currentRecord, TcpResult result, IpPort ipPort,
            SdkProfile tProfile) {
        if (!result.result) {
            manager.removeWaitCompletedProfile(tProfile);
            manager.offerDispatchQueue(currentRecord);
            try {
                Thread.sleep(manager.getContext().getProcessInterval());
            } catch (InterruptedException e1) {
                LOG.error(e1.getMessage(), e1);
            }
            return false;
        }
        return true;
    }

    /**
     * get status
     * 
     * @return the status
     */
    public LifecycleState getStatus() {
        return status;
    }

    /**
     * get manager
     * 
     * @return the manager
     */
    public SdkProxyChannelManager getManager() {
        return manager;
    }

    /**
     * close
     */
    public void close() {
        this.status = LifecycleState.STOP;
    }
}
