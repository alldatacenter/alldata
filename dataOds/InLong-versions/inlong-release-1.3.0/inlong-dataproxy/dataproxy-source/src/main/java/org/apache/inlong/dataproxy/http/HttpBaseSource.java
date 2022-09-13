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

import static org.apache.inlong.dataproxy.consts.ConfigConstants.MAX_MONITOR_CNT;
import com.google.common.base.Preconditions;
import org.apache.flume.ChannelSelector;
import org.apache.flume.Context;
import org.apache.flume.EventDrivenSource;
import org.apache.flume.conf.Configurable;
import org.apache.flume.conf.Configurables;
import org.apache.flume.source.AbstractSource;
import org.apache.inlong.common.metric.MetricRegister;
import org.apache.inlong.common.monitor.MonitorIndex;
import org.apache.inlong.common.monitor.MonitorIndexExt;
import org.apache.inlong.dataproxy.channel.FailoverChannelProcessor;
import org.apache.inlong.dataproxy.consts.ConfigConstants;
import org.apache.inlong.dataproxy.metrics.DataProxyMetricItemSet;
import org.apache.inlong.dataproxy.utils.ConfStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    protected int maxConnections = Integer.MAX_VALUE;
    protected boolean customProcessor = false;
    protected Context context;
    // statistic
    protected MonitorIndex monitorIndex = null;
    protected MonitorIndexExt monitorIndexExt = null;
    private int statIntervalSec = 60;
    private int maxMonitorCnt = 300000;
    // audit
    protected DataProxyMetricItemSet metricItemSet;

    public HttpBaseSource() {
        super();
    }

    @Override
    public synchronized void start() {
        logger.info("{} starting...", this.getName());
        if (customProcessor) {
            ChannelSelector selector = getChannelProcessor().getSelector();
            FailoverChannelProcessor newProcessor = new FailoverChannelProcessor(selector);
            newProcessor.configure(this.context);
            setChannelProcessor(newProcessor);
        }
        if (statIntervalSec > 0) {
            monitorIndex = new MonitorIndex("Source",
                    statIntervalSec, maxMonitorCnt);
            monitorIndexExt = new MonitorIndexExt("DataProxy_monitors#http",
                    statIntervalSec, maxMonitorCnt);
        }
        // register metrics
        this.metricItemSet = new DataProxyMetricItemSet(this.getName());
        MetricRegister.register(metricItemSet);
        super.start();
        logger.info("{} started!", this.getName());
    }

    @Override
    public synchronized void stop() {
        logger.info("{} stopping...", this.getName());
        if (statIntervalSec > 0) {
            try {
                monitorIndex.shutDown();
            } catch (Exception e) {
                logger.warn("Stats runner exception ", e);
            }
        }
        super.stop();
        logger.info("{} stopped!", this.getName());
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

        attr = attr.trim();
        Preconditions.checkArgument(!attr.isEmpty(), "attr is empty");

        messageHandlerName = context.getString(ConfigConstants.MESSAGE_HANDLER_NAME,
                "org.apache.inlong.dataproxy.source.ServerMessageHandler");
        messageHandlerName = messageHandlerName.trim();
        Preconditions.checkArgument(!messageHandlerName.isEmpty(), "messageHandlerName is empty");

        filterEmptyMsg = context.getBoolean(ConfigConstants.FILTER_EMPTY_MSG, false);
        // get statistic interval
        statIntervalSec = context.getInteger(ConfigConstants.STAT_INTERVAL_SEC, 60);
        Preconditions.checkArgument((statIntervalSec >= 0), "statIntervalSec must be >= 0");
        // get max monitor record count
        maxMonitorCnt = context.getInteger(MAX_MONITOR_CNT, 300000);
        Preconditions.checkArgument(maxMonitorCnt >= 0, "maxMonitorCnt must be >= 0");

        customProcessor = context.getBoolean(ConfigConstants.CUSTOM_CHANNEL_PROCESSOR, false);

        try {
            maxConnections = context.getInteger(CONNECTIONS, 5000);
        } catch (NumberFormatException e) {
            logger.warn("BaseSource connections property must specify an integer value {}",
                    context.getString(CONNECTIONS));
        }
    }
}
