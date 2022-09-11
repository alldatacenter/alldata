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

package org.apache.inlong.dataproxy.source.tcp;

import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.ReadTimeoutHandler;
import java.lang.reflect.Constructor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.apache.flume.Context;
import org.apache.flume.conf.Configurable;
import org.apache.inlong.dataproxy.consts.ConfigConstants;
import org.apache.inlong.dataproxy.source.SourceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * InlongTcpChannelPipelineFactory
 */
public class InlongTcpChannelPipelineFactory extends ChannelInitializer<SocketChannel>
        implements Configurable {

    public static final Logger LOG = LoggerFactory.getLogger(InlongTcpChannelPipelineFactory.class);
    public static final int DEFAULT_LENGTH_FIELD_OFFSET = 0;
    public static final int DEFAULT_LENGTH_FIELD_LENGTH = 4;
    public static final int DEFAULT_LENGTH_ADJUSTMENT = -4;
    public static final int DEFAULT_INITIAL_BYTES_TO_STRIP = 0;
    public static final boolean DEFAULT_FAIL_FAST = true;
    private static final int DEFAULT_READ_IDLE_TIME = 70 * 60 * 1000;
    private SourceContext sourceContext;
    private String messageHandlerName;
    private String protocolType;

    /**
     * get server factory
     *
     * @param sourceContext
     */
    public InlongTcpChannelPipelineFactory(SourceContext sourceContext, String protocolType) {
        this.sourceContext = sourceContext;
        this.protocolType = protocolType;
    }

    @Override
    protected void initChannel(SocketChannel ch) {

        if (StringUtils.isEmpty(protocolType) || this.protocolType
                .equalsIgnoreCase(ConfigConstants.TCP_PROTOCOL)) {
            ch.pipeline().addLast("messageDecoder", new LengthFieldBasedFrameDecoder(
                    sourceContext.getMaxMsgLength(), DEFAULT_LENGTH_FIELD_OFFSET,
                    DEFAULT_LENGTH_FIELD_LENGTH,
                    DEFAULT_LENGTH_ADJUSTMENT, DEFAULT_INITIAL_BYTES_TO_STRIP, DEFAULT_FAIL_FAST));
            ch.pipeline().addLast("readTimeoutHandler",
                    new ReadTimeoutHandler(DEFAULT_READ_IDLE_TIME, TimeUnit.MILLISECONDS));
        }

        if (sourceContext.getSource().getChannelProcessor() != null) {
            try {
                Class<? extends ChannelInboundHandlerAdapter> clazz =
                        (Class<? extends ChannelInboundHandlerAdapter>) Class
                        .forName(messageHandlerName);

                Constructor<?> ctor = clazz.getConstructor(SourceContext.class);

                ChannelInboundHandlerAdapter messageHandler = (ChannelInboundHandlerAdapter) ctor
                        .newInstance(sourceContext);

                ch.pipeline().addLast("messageHandler", messageHandler);
            } catch (Exception e) {
                LOG.error("SimpleChannelHandler.newInstance  has error:"
                        + sourceContext.getSource().getName(), e);
            }
        }
    }

    @Override
    public void configure(Context context) {
        LOG.info("context is {}", context);
        messageHandlerName = context.getString(ConfigConstants.MESSAGE_HANDLER_NAME,
                InlongTcpChannelHandler.class.getName());
        messageHandlerName = messageHandlerName.trim();
        Preconditions.checkArgument(StringUtils.isNotBlank(messageHandlerName),
                "messageHandlerName is empty");
    }
}
