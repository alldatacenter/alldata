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

package org.apache.inlong.audit.channel;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FailoverChannelSelector extends AbstractChannelSelector {

    private static final Logger LOG = LoggerFactory.getLogger(FailoverChannelSelector.class);

    private static final String MASTER_CHANNEL = "master";

    private int masterIndex = 0;

    private int slaveIndex = 0;

    private final List<Channel> masterChannels = new ArrayList<Channel>();
    private final List<Channel> slaveChannels = new ArrayList<Channel>();

    @Override
    public List<Channel> getRequiredChannels(Event event) {
        List<Channel> retChannels = new ArrayList<Channel>();
        if (masterChannels.size() > 0) {
            retChannels.add(masterChannels.get(masterIndex));
            masterIndex = (masterIndex + 1) % masterChannels.size();
        } else {
            LOG.warn("masterChannels size is zero!");
        }

        return retChannels;
    }

    @Override
    public List<Channel> getOptionalChannels(Event event) {
        List<Channel> retChannels = new ArrayList<Channel>();
        if (slaveChannels.size() > 0) {
            retChannels.add(slaveChannels.get(slaveIndex));
            slaveIndex = (slaveIndex + 1) % slaveChannels.size();
        } else {
            LOG.warn("slaveChannels size is zero!");
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
        String masters = context.getString(MASTER_CHANNEL);
        if (StringUtils.isEmpty(masters)) {
            throw new FlumeException("master channel is null!");
        }
        List<String> masterList = splitChannelName(masters);

        for (Map.Entry<String, Channel> entry : getChannelNameMap().entrySet()) {
            String channelName = entry.getKey();
            Channel channel = entry.getValue();
            if (masterList.contains(channelName)) {
                this.masterChannels.add(channel);
            } else {
                this.slaveChannels.add(channel);
            }
        }
        LOG.info("masters:" + this.masterChannels);
        LOG.info("slaves:" + this.slaveChannels);
    }
}
