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

package org.apache.inlong.dataproxy.channel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.flume.Channel;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.FlumeException;
import org.apache.flume.channel.AbstractChannelSelector;
import org.apache.inlong.dataproxy.consts.AttributeConstants;
import org.apache.inlong.dataproxy.consts.ConfigConstants;
import org.apache.inlong.dataproxy.utils.MessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FailoverChannelSelector extends AbstractChannelSelector {

    private static final Logger LOG = LoggerFactory.getLogger(FailoverChannelSelector.class);

    private static final String SELECTOR_PROPS = "selector.";
    private static final String MASTER_CHANNEL = "master";
    private static final String TRANSFER_CHANNEL = "transfer";
    private static final String FILE_METRIC_CHANNEL = "fileMetric";
    private static final String SLA_METRIC_CHANNEL = "slaMetric";
    private static final String ORDER_CHANNEL = "order";

    private int masterIndex = 0;
    private int slaveIndex = 0;

    private final List<Channel> masterChannels = new ArrayList<Channel>();
    private final List<Channel> orderChannels = new ArrayList<Channel>();
    private final List<Channel> slaveChannels = new ArrayList<Channel>();
    private final List<Channel> transferChannels = new ArrayList<Channel>();
    private final List<Channel> agentFileMetricChannels = new ArrayList<Channel>();
    private final List<Channel> slaMetricChannels = new ArrayList<Channel>();

    @Override
    public List<Channel> getRequiredChannels(Event event) {
        List<Channel> retChannels = new ArrayList<Channel>();
        if (event.getHeaders().containsKey(ConfigConstants.TRANSFER_KEY)) {
            retChannels.add(transferChannels.get(0));
        } else if (event.getHeaders().containsKey(ConfigConstants.FILE_CHECK_DATA)) {
            retChannels.add(agentFileMetricChannels.get(0));
        } else if (event.getHeaders().containsKey(ConfigConstants.SLA_METRIC_DATA)) {
            retChannels.add(slaMetricChannels.get(0));
        } else if (MessageUtils.isSyncSendForOrder(event.getHeaders()
                .get(AttributeConstants.MESSAGE_SYNC_SEND))) {
            String partitionKey = event.getHeaders().get(AttributeConstants.MESSAGE_PARTITION_KEY);
            if (partitionKey == null) {
                partitionKey = "";
            }
            int channelIndex = Math.abs(partitionKey.hashCode()) % orderChannels.size();
            retChannels.add(orderChannels.get(channelIndex));
        } else {
            retChannels.add(masterChannels.get(masterIndex));
            masterIndex = (masterIndex + 1) % masterChannels.size();
        }
        return retChannels;
    }

    @Override
    public List<Channel> getOptionalChannels(Event event) {
        List<Channel> retChannels = new ArrayList<Channel>();
        if (event.getHeaders().containsKey(ConfigConstants.TRANSFER_KEY)) {
            retChannels.add(transferChannels.get(0));
        } else if (event.getHeaders().containsKey(ConfigConstants.FILE_CHECK_DATA)) {
            retChannels.add(agentFileMetricChannels.get(0));
        } else if (event.getHeaders().containsKey(ConfigConstants.SLA_METRIC_DATA)) {
            retChannels.add(slaMetricChannels.get(1));
        } else {
            retChannels.add(slaveChannels.get(slaveIndex));
            slaveIndex = (slaveIndex + 1) % slaveChannels.size();
        }
        return retChannels;
    }

    /**
     * split channel name into name list.
     *
     * @param channelName - channel name
     * @return - name list
     */
    private List<String> splitChannelName(String channelName) {
        List<String> fileMetricList = new ArrayList<String>();
        if (StringUtils.isEmpty(channelName)) {
            LOG.info("channel name is null!");
        } else {
            fileMetricList = Arrays.asList(channelName.split("\\s+"));
        }
        return fileMetricList;
    }

    @Override
    public void configure(Context context) {
//        LOG.info(context.toString());
        String masters = context.getString(MASTER_CHANNEL);
        String transfer = context.getString(TRANSFER_CHANNEL);
        String fileMertic = context.getString(FILE_METRIC_CHANNEL);
        String slaMetric = context.getString(SLA_METRIC_CHANNEL);
        String orderMetric = context.getString(ORDER_CHANNEL);
        if (StringUtils.isEmpty(masters)) {
            throw new FlumeException("master channel is null!");
        }
        List<String> masterList = splitChannelName(masters);
        List<String> transferList = splitChannelName(transfer);
        List<String> fileMetricList = splitChannelName(fileMertic);
        List<String> slaMetricList = splitChannelName(slaMetric);
        List<String> orderMetricList = splitChannelName(orderMetric);

        for (Map.Entry<String, Channel> entry : getChannelNameMap().entrySet()) {
            String channelName = entry.getKey();
            Channel channel = entry.getValue();
            if (masterList.contains(channelName)) {
                this.masterChannels.add(channel);
            } else if (transferList.contains(channelName)) {
                this.transferChannels.add(channel);
            } else if (fileMetricList.contains(channelName)) {
                this.agentFileMetricChannels.add(channel);
            } else if (slaMetricList.contains(channelName)) {
                this.slaMetricChannels.add(channel);
            } else if (orderMetricList.contains(channelName)) {
                this.orderChannels.add(channel);
            } else {
                this.slaveChannels.add(channel);
            }
        }
        LOG.info("masters:" + this.masterChannels);
        LOG.info("orders:" + this.orderChannels);
        LOG.info("slaves:" + this.slaveChannels);
        LOG.info("transfers:" + this.transferChannels);
        LOG.info("agentFileMetrics:" + this.agentFileMetricChannels);
        LOG.info("slaMetrics:" + this.slaMetricChannels);
    }
}
