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

import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Generate uniq sequential id, reset base id if max number
 * of sequence uuid are satisfied.
 */
public class SequentialID {

    private static final int MAX_ID = 2000000000;
    private static final String IP_HEX = getHex();
    private static SequentialID uniqueSequentialID = null;
    private final AtomicInteger id = new AtomicInteger(1);
    private final ReentrantLock idLock = new ReentrantLock();
    private final AtomicInteger uid = new AtomicInteger(1);
    private final ReentrantLock uidLock = new ReentrantLock();

    private SequentialID() {

    }

    private static String getHex() {
        String localIp = AgentUtils.getLocalIp();
        try {
            InetAddress ia = InetAddress.getByName(localIp);
            byte[] hostBytes = ia.getAddress();
            StringBuilder sb = new StringBuilder();
            for (byte hostByte : hostBytes) {
                sb.append(String.format("%02x", hostByte));
            }
            return sb.toString();
        } catch (Exception e) {
            // ignore it
        }
        return "00000000";
    }

    /**
     * get SequentialID single instance
     */
    public static synchronized SequentialID getInstance() {

        if (uniqueSequentialID == null) {
            uniqueSequentialID = new SequentialID();
        }
        return uniqueSequentialID;
    }

    public int getNextId() {
        idLock.lock();
        try {
            if (id.get() > MAX_ID) {
                id.set(1);
            }
            return id.incrementAndGet();
        } finally {
            idLock.unlock();
        }
    }

    /**
     * get next uuid
     */
    public String getNextUuid() {
        uidLock.lock();
        try {
            if (uid.get() > MAX_ID) {
                uid.set(1);
            }
            return IP_HEX + "-" + String.format("%014x-%08x",
                    System.currentTimeMillis(), uid.incrementAndGet());
        } finally {
            uidLock.unlock();
        }
    }
}
