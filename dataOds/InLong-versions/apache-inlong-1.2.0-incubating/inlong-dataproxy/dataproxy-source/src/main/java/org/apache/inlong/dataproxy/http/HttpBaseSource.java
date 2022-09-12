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

package org.apache.inlong.dataproxy.http;

import com.google.common.base.Preconditions;
import org.apache.flume.ChannelSelector;
import org.apache.flume.Context;
import org.apache.flume.EventDrivenSource;
import org.apache.flume.conf.Configurable;
import org.apache.flume.conf.Configurables;
import org.apache.flume.source.AbstractSource;
import org.apache.inlong.common.monitor.CounterGroup;
import org.apache.inlong.common.monitor.CounterGroupExt;
import org.apache.inlong.common.monitor.StatConstants;
import org.apache.inlong.common.monitor.StatRunner;
import org.apache.inlong.dataproxy.channel.FailoverChannelProcessor;
import org.apache.inlong.dataproxy.consts.ConfigConstants;
import org.apache.inlong.dataproxy.utils.ConfStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class HttpBaseSource extends AbstractSource implements EventDrivenSource, Configurable {

    private static final Logger logger = LoggerFactory.getLogger(HttpBaseSource.class);
    private static final String CONNECTIONS = "connections";
    protected int port;
    protected String host = null;
    protected int maxMsgLength;
    protected String topic;
    protected String attr;
    protected String messageHandlerName;
    protected boolean filterEmptyMsg;
    protected CounterGroup counterGroup;
    protected CounterGroupExt counterGroupExt;
    protected int maxConnections = Integer.MAX_VALUE;
    protected boolean customProcessor = false;
    protected Context context;
    private int statIntervalSec;
    private StatRunner statRunner;
    private Thread statThread;

    public HttpBaseSource() {
        super();
        counterGroup = new CounterGroup();
        counterGroupExt = new CounterGroupExt();
    }

    @Override
    public synchronized void start() {
        if (statIntervalSec > 0) {
            Set<String> monitorNames = new HashSet<>();
            monitorNames.add(StatConstants.EVENT_SUCCESS);
            monitorNames.add(StatConstants.EVENT_DROPPED);
            monitorNames.add(StatConstants.EVENT_EMPTY);
            monitorNames.add(StatConstants.EVENT_OTHEREXP);
            monitorNames.add(StatConstants.EVENT_INVALID);
            statRunner = new StatRunner(getName(), counterGroup, counterGroupExt, statIntervalSec, monitorNames);
            statThread = new Thread(statRunner);
            statThread.setName("Thread-Stat-" + this.getName());
            statThread.start();
        }

        if (customProcessor) {
            ChannelSelector selector = getChannelProcessor().getSelector();
            FailoverChannelProcessor newProcessor = new FailoverChannelProcessor(selector);
            newProcessor.configure(this.context);
            setChannelProcessor(newProcessor);
        }

        super.start();
    }

    @Override
    public synchronized void stop() {
        if (statIntervalSec > 0) {
            try {
                if (statRunner != null) {
                    statRunner.shutDown();
                }
                if (statThread != null) {
                    statThread.interrupt();
                    statThread.join();
                }

            } catch (InterruptedException e) {
                logger.warn("start runner interrupted");
            }
        }

        super.stop();
    }

    /**
     * configure
     */
    public void configure(Context context) {
        this.context = context;
        port = context.getInteger(ConfigConstants.CONFIG_PORT);
        host = context.getString(ConfigConstants.CONFIG_HOST, "0.0.0.0");

        Configurables.ensureRequiredNonNull(context, ConfigConstants.CONFIG_PORT);

        Preconditions.checkArgument(ConfStringUtils.isValidIp(host), "ip config not valid");
        Preconditions.checkArgument(ConfStringUtils.isValidPort(port), "port config not valid");

        maxMsgLength = context.getInteger(ConfigConstants.MAX_MSG_LENGTH, 1024 * 64);
        Preconditions.checkArgument(
                (maxMsgLength >= 4 && maxMsgLength <= ConfigConstants.MSG_MAX_LENGTH_BYTES),
                "maxMsgLength must be >= 4 and <= 65536");

        topic = context.getString(ConfigConstants.TOPIC);
        attr = context.getString(ConfigConstants.ATTR);
        Configurables.ensureRequiredNonNull(context, ConfigConstants.TOPIC, ConfigConstants.ATTR);

        topic = topic.trim();
        Preconditions.checkArgument(!topic.isEmpty(), "topic is empty");
        attr = attr.trim();
        Preconditions.checkArgument(!attr.isEmpty(), "attr is empty");

        messageHandlerName = context.getString(ConfigConstants.MESSAGE_HANDLER_NAME,
                "org.apache.inlong.dataproxy.source.ServerMessageHandler");
        messageHandlerName = messageHandlerName.trim();
        Preconditions.checkArgument(!messageHandlerName.isEmpty(), "messageHandlerName is empty");

        filterEmptyMsg = context.getBoolean(ConfigConstants.FILTER_EMPTY_MSG, false);

        statIntervalSec = context.getInteger(ConfigConstants.STAT_INTERVAL_SEC, 60);
        Preconditions.checkArgument((statIntervalSec >= 0), "statIntervalSec must be >= 0");

        customProcessor = context.getBoolean(ConfigConstants.CUSTOM_CHANNEL_PROCESSOR, false);

        try {
            maxConnections = context.getInteger(CONNECTIONS, 5000);
        } catch (NumberFormatException e) {
            logger.warn("BaseSource connections property must specify an integer value {}",
                    context.getString(CONNECTIONS));
        }
    }
}
