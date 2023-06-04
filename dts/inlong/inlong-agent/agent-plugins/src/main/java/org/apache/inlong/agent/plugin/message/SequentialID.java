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

package org.apache.inlong.agent.plugin.message;

import org.apache.inlong.agent.utils.AgentUtils;
import org.apache.inlong.common.util.SnowFlake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.apache.inlong.common.util.SnowFlake.MAX_MACHINE_NUM;

/**
 * Generate uniq sequential id, reset base id if max number
 * of sequence uuid are satisfied.
 */
public class SequentialID {

    private static final Logger LOGGER = LoggerFactory.getLogger(SequentialID.class);

    private static SequentialID uniqueSequentialID = null;
    private SnowFlake snowFlake;

    private SequentialID() {
        long machineId = ipStr2Int(AgentUtils.getLocalIp());
        snowFlake = new SnowFlake(machineId);
    }

    private long ipStr2Int(String ip) {
        long result = 0;
        InetAddress ipv;
        try {
            ipv = InetAddress.getByName(ip);
        } catch (UnknownHostException e) {
            LOGGER.error("convert ip to int error", e);
            return AgentUtils.getRandomBySeed(10);
        }
        for (byte b : ipv.getAddress()) {
            result = result << 8 | (b & 0xFF);
        }
        if (result < 0) {
            result = AgentUtils.getRandomBySeed(10);
        }
        if (result > MAX_MACHINE_NUM) {
            result %= MAX_MACHINE_NUM;
        }
        return result;
    }

    /**
     * get SequentialID single instance
     */
    public static synchronized SequentialID getInstance() {

        if (uniqueSequentialID == null) {
            synchronized (SequentialID.class) {
                if (uniqueSequentialID == null) {
                    uniqueSequentialID = new SequentialID();
                }
            }
        }
        return uniqueSequentialID;
    }

    /**
     * get next uuid
     */
    public String getNextUuid() {
        return String.valueOf(snowFlake.nextId());
    }
}
