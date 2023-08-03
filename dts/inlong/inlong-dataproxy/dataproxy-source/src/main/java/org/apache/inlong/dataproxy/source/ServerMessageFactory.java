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

package org.apache.inlong.dataproxy.source;

import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.concurrent.TimeUnit;

public class ServerMessageFactory extends ChannelInitializer<SocketChannel> {

    public static final int INLONG_LENGTH_FIELD_OFFSET = 0;
    public static final int INLONG_LENGTH_FIELD_LENGTH = 4;
    public static final int INLONG_LENGTH_ADJUSTMENT = 0;
    public static final int INLONG_INITIAL_BYTES_TO_STRIP = 0;
    public static final boolean DEFAULT_FAIL_FAST = true;
    private static final Logger LOG = LoggerFactory.getLogger(ServerMessageFactory.class);
    private final BaseSource source;

    /**
     * get server factory
     *
     * @param source
     */
    public ServerMessageFactory(BaseSource source) {
        this.source = source;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {

        if (source.getProtocolName()
                .equalsIgnoreCase(SourceConstants.SRC_PROTOCOL_TYPE_TCP)) {
            ch.pipeline().addLast("messageDecoder", new LengthFieldBasedFrameDecoder(
                    source.getMaxMsgLength(), INLONG_LENGTH_FIELD_OFFSET, INLONG_LENGTH_FIELD_LENGTH,
                    INLONG_LENGTH_ADJUSTMENT, INLONG_INITIAL_BYTES_TO_STRIP, DEFAULT_FAIL_FAST));
            ch.pipeline().addLast("readTimeoutHandler",
                    new ReadTimeoutHandler(source.getMaxReadIdleTimeMs(), TimeUnit.MILLISECONDS));
        } else if (source.getProtocolName().equalsIgnoreCase(SourceConstants.SRC_PROTOCOL_TYPE_HTTP)) {
            // add http message codec
            ch.pipeline().addLast("msgCodec", new HttpServerCodec());
            ch.pipeline().addLast("msgAggregator", new HttpObjectAggregator(source.getMaxMsgLength()));
            ch.pipeline().addLast("readTimeoutHandler",
                    new ReadTimeoutHandler(source.getMaxReadIdleTimeMs(), TimeUnit.MILLISECONDS));

        }
        // build message handler
        if (source.getChannelProcessor() != null) {
            try {
                Class<? extends ChannelInboundHandlerAdapter> clazz =
                        (Class<? extends ChannelInboundHandlerAdapter>) Class.forName(source.getMessageHandlerName());
                Constructor<?> ctor = clazz.getConstructor(BaseSource.class);
                ChannelInboundHandlerAdapter messageHandler =
                        (ChannelInboundHandlerAdapter) ctor.newInstance(source);
                ch.pipeline().addLast("messageHandler", messageHandler);
            } catch (Exception e) {
                LOG.error("{} newInstance {} failure!", source.getName(),
                        source.getMessageHandlerName(), e);
            }
        }
    }
}
