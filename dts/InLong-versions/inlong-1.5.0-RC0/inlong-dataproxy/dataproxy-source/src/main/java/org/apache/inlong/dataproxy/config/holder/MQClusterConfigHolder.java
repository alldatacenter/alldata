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

package org.apache.inlong.dataproxy.config.holder;

import com.google.common.base.Splitter;
import org.apache.inlong.dataproxy.config.pojo.MQClusterConfig;
import org.apache.inlong.dataproxy.consts.AttrConstants;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Holder of the MQ cluster config.
 */
public class MQClusterConfigHolder extends PropertiesConfigHolder {

    public static final String URL_STORE_PREFIX = "mq_cluster.index";

    private final MQClusterConfig clusterConfig = new MQClusterConfig();

    public MQClusterConfigHolder(String fileName) {
        super(fileName);
    }

    /**
     * load from file
     */
    @Override
    public void loadFromFileToHolder() {
        super.loadFromFileToHolder();
        Map<String, String> tmpUrl2token = new HashMap<>();
        for (Map.Entry<String, String> entry : getHolder().entrySet()) {
            if (entry.getKey().startsWith(URL_STORE_PREFIX)) {
                List<String> kv = Splitter.on(AttrConstants.KEY_VALUE_SEPARATOR)
                        .trimResults().splitToList(entry.getValue());
                tmpUrl2token.put(kv.get(0), kv.get(1));
            }
        }
        if (!tmpUrl2token.isEmpty()) {
            clusterConfig.setUrl2token(tmpUrl2token);
        }
        // for disaster
        clusterConfig.putAll(getHolder());
    }

    public Map<String, String> getUrl2token() {
        return clusterConfig.getUrl2token();
    }

    public void setUrl2token(Map<String, String> newUrl2Token) {
        clusterConfig.setUrl2token(newUrl2Token);
    }

    public MQClusterConfig getClusterConfig() {
        return clusterConfig;
    }
}
