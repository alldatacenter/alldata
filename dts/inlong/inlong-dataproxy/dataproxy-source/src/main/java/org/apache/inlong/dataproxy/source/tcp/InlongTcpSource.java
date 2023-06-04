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

import com.google.common.base.Preconditions;

import org.apache.commons.lang3.StringUtils;
import org.apache.flume.Context;
import org.apache.flume.EventDrivenSource;
import org.apache.flume.FlumeException;
import org.apache.flume.conf.Configurable;
import org.apache.inlong.dataproxy.admin.ProxyServiceMBean;
import org.apache.inlong.dataproxy.consts.ConfigConstants;
import org.apache.inlong.dataproxy.source.SimpleTcpSource;
import org.apache.inlong.dataproxy.source.SourceContext;
import org.apache.inlong.sdk.commons.admin.AdminServiceRegister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;

import io.netty.channel.ChannelInitializer;

/**
 * Inlong tcp source
 */
public class InlongTcpSource extends SimpleTcpSource
        implements
            Configurable,
            EventDrivenSource,
            ProxyServiceMBean {

    public static final Logger LOG = LoggerFactory.getLogger(InlongTcpSource.class);

    protected SourceContext sourceContext;

    protected String msgFactoryName;
    protected String messageHandlerName;

    private Configurable pipelineFactoryConfigurable = null;

    /**
     * Constructor
     */
    public InlongTcpSource() {
        super();
    }

    /**
     * start
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public synchronized void startSource() {
        super.startSource();
    }

    /**
     * stop
     */
    @Override
    public synchronized void stop() {
        LOG.info("[STOP SOURCE]{} stopping...", super.toString());
        super.stop();
    }

    /**
     * configure
     * 
     * @param context
     */
    @Override
    public void configure(Context context) {
        try {
            LOG.info("context is {}", context);
            super.configure(context);
            this.sourceContext = new SourceContext(this, allChannels, context);
            // start
            this.sourceContext.start();

            msgFactoryName = context.getString(ConfigConstants.MSG_FACTORY_NAME,
                    InlongTcpChannelPipelineFactory.class.getName()).trim();
            Preconditions.checkArgument(StringUtils.isNotBlank(msgFactoryName),
                    "msgFactoryName is empty");

            messageHandlerName = context.getString(ConfigConstants.MESSAGE_HANDLER_NAME,
                    InlongTcpChannelHandler.class.getName());
            messageHandlerName = messageHandlerName.trim();
            Preconditions.checkArgument(StringUtils.isNotBlank(messageHandlerName),
                    "messageHandlerName is empty");

            if (this.pipelineFactoryConfigurable != null) {
                this.pipelineFactoryConfigurable.configure(context);
            }
            // register
            AdminServiceRegister.register(ProxyServiceMBean.MBEAN_TYPE, this.getName(), this);
        } catch (Throwable t) {
            LOG.error(t.getMessage(), t);
        }
    }

    /**
     * get ChannelInitializerFactory
     *
     * @return ChannelInitializer
     */
    public ChannelInitializer getChannelInitializerFactory() {
        LOG.info(new StringBuffer("load msgFactory=").append(msgFactoryName)
                .append(" and serviceDecoderName=").append(serviceDecoderName).toString());

        ChannelInitializer fac = null;
        try {
            Class<? extends ChannelInitializer> clazz = (Class<? extends ChannelInitializer>) Class
                    .forName(msgFactoryName);

            Constructor ctor = clazz.getConstructor(SourceContext.class, String.class);

            LOG.info("Using channel processor:{}", getChannelProcessor().getClass().getName());
            fac = (ChannelInitializer) ctor.newInstance(sourceContext,
                    this.getProtocolName());
            if (fac instanceof Configurable) {
                this.pipelineFactoryConfigurable = ((Configurable) fac);
                this.pipelineFactoryConfigurable.configure(sourceContext.getParentContext());
            }
        } catch (Exception e) {
            LOG.error(
                    "Inlong Tcp Source start error, fail to construct ChannelPipelineFactory with name {}, ex {}",
                    msgFactoryName, e);
            stop();
            throw new FlumeException(e.getMessage(), e);
        }
        return fac;
    }

    /**
     * getProtocolName
     * 
     * @return
     */
    public String getProtocolName() {
        return "tcp";
    }

    /**
     * stopService
     */
    @Override
    public void stopService() {
        this.sourceContext.setRejectService(true);
    }

    /**
     * recoverService
     */
    @Override
    public void recoverService() {
        this.sourceContext.setRejectService(false);
    }
}
