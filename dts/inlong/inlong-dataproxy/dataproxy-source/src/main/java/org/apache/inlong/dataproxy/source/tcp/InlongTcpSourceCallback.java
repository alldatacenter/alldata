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

package org.apache.inlong.dataproxy.source.tcp;

import org.apache.inlong.sdk.commons.protocol.SourceCallback;
import org.apache.inlong.sdk.commons.protocol.ProxySdk.MessagePackHeader;
import org.apache.inlong.sdk.commons.protocol.ProxySdk.ResponseInfo;
import org.apache.inlong.sdk.commons.protocol.ProxySdk.ResultCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

/**
 * InlongTcpEventCallback
 * 
 */
public class InlongTcpSourceCallback implements SourceCallback {

    public static final Logger LOG = LoggerFactory.getLogger(InlongTcpSourceCallback.class);

    private final ChannelHandlerContext ctx;
    private final MessagePackHeader header;
    private final CountDownLatch latch;
    private final AtomicBoolean hasResponsed = new AtomicBoolean(false);

    /**
     * Constructor
     * @param ctx
     * @param header
     */
    public InlongTcpSourceCallback(ChannelHandlerContext ctx, MessagePackHeader header) {
        this.ctx = ctx;
        this.header = header;
        this.latch = new CountDownLatch(1);
    }

    /**
     * callback
     * @param resultCode
     */
    @Override
    public void callback(ResultCode resultCode) {
        // If DataProxy have sent timeout response to SDK, DataProxy do not send success response to SDK again when
        // event is success to save.
        if (this.hasResponsed.getAndSet(true)) {
            return;
        }
        // response
        try {
            ResponseInfo.Builder builder = ResponseInfo.newBuilder();
            builder.setResult(resultCode);
            builder.setPackId(header.getPackId());

            // encode
            byte[] responseBytes = builder.build().toByteArray();
            //
            ByteBuf buffer = Unpooled.wrappedBuffer(responseBytes);
            Channel remoteChannel = ctx.channel();
            if (remoteChannel.isWritable()) {
                remoteChannel.write(buffer);
            } else {
                LOG.warn("the send buffer2 is full, so disconnect it!"
                        + "please check remote client; Connection info:{}",
                        remoteChannel);
                buffer.release();
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        } finally {
            // notice TCP session
            this.latch.countDown();
        }
    }

    /**
     * get hasResponsed
     * @return the hasResponsed
     */
    public AtomicBoolean getHasResponsed() {
        return hasResponsed;
    }

    /**
     * get latch
     * @return the latch
     */
    public CountDownLatch getLatch() {
        return latch;
    }

}
