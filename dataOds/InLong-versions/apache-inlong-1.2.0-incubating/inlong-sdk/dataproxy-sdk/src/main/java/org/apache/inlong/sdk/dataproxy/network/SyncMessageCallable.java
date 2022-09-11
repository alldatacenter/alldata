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

package org.apache.inlong.sdk.dataproxy.network;

import io.netty.channel.ChannelFuture;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.inlong.sdk.dataproxy.SendResult;
import org.apache.inlong.sdk.dataproxy.codec.EncodeObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SyncMessageCallable implements Callable<SendResult> {
    private static final Logger logger = LoggerFactory
            .getLogger(SyncMessageCallable.class);

    private final NettyClient client;
    private final CountDownLatch awaitLatch = new CountDownLatch(1);
    private final EncodeObject encodeObject;
    private final long timeout;
    private final TimeUnit timeUnit;

    private SendResult message;

    public SyncMessageCallable(NettyClient client, EncodeObject encodeObject,
                               long timeout, TimeUnit timeUnit) {
        this.client = client;
        this.encodeObject = encodeObject;
        this.timeout = timeout;
        this.timeUnit = timeUnit;
    }

    public void update(SendResult message) {
        this.message = message;
        awaitLatch.countDown();
    }

    public SendResult call() throws Exception {
        // TODO Auto-generated method stub
        try {
            ChannelFuture channelFuture = client.write(encodeObject);
            awaitLatch.await(timeout, timeUnit);
        } catch (Exception e) {
            logger.error("SendResult call", e);
            e.printStackTrace();
            return SendResult.UNKOWN_ERROR;
        }
        return message;
    }

    public NettyClient getClient() {
        return client;
    }
}