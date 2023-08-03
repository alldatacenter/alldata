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

import org.apache.inlong.dataproxy.config.ConfigHolder;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * save to list
 */
public class VisitConfigHolder extends ConfigHolder {

    private static final int MIN_NETMASK_BITS = 0;
    private static final int MAX_NETMASK_BITS = 32;
    private static final String MASKIP_NETMASK_SEP = "/";
    private static final String IPV4ADDR_TMP =
            "((?:(?:25[0-5]|2[0-4]\\d|((1\\d{2})|([1-9]?\\d)))\\.){3}(?:25[0-5]|2[0-4]\\d|((1\\d{2})|([1-9]?\\d))))";
    private static final Logger LOG = LoggerFactory.getLogger(VisitConfigHolder.class);
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final boolean isBlackList;
    private final ConcurrentHashMap<String, Long> confHolder = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> ipAddrHolder = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Pair<Integer, Integer>> ipSegmentHolder = new ConcurrentHashMap<>();

    public VisitConfigHolder(boolean isBlackList, String fileName) {
        super(fileName);
        this.isBlackList = isBlackList;
    }

    @Override
    protected boolean loadFromFileToHolder() {
        readWriteLock.writeLock().lock();
        try {
            Map<String, Long> tmpHolder = loadFile();
            if (tmpHolder == null) {
                return false;
            }
            // clear removed records
            boolean added = false;
            boolean removed = false;
            Set<String> tmpKeys = new HashSet<>();
            for (Map.Entry<String, Long> entry : confHolder.entrySet()) {
                if (!tmpHolder.containsKey(entry.getKey())) {
                    tmpKeys.add(entry.getKey());
                }
            }
            for (String tmpKey : tmpKeys) {
                removed = true;
                confHolder.remove(tmpKey);
                if (tmpKey.contains(MASKIP_NETMASK_SEP)) {
                    ipSegmentHolder.remove(tmpKey);
                } else {
                    ipAddrHolder.remove(tmpKey);
                }
            }
            // add records
            String subStr;
            int netBits;
            int cidrIpAddr;
            int hostAddrMask;
            tmpKeys.clear();
            for (Map.Entry<String, Long> entry : tmpHolder.entrySet()) {
                if (entry == null || StringUtils.isBlank(entry.getKey())) {
                    continue;
                }
                if (!confHolder.containsKey(entry.getKey())) {
                    confHolder.put(entry.getKey(), entry.getValue());
                    if (entry.getKey().contains(MASKIP_NETMASK_SEP)) {
                        // build CIDR ip segment info
                        subStr = entry.getKey().replaceAll(".*/", "");
                        try {
                            netBits = Integer.parseInt(subStr);
                        } catch (Throwable e) {
                            tmpKeys.add(entry.getKey());
                            continue;
                        }
                        if (netBits < MIN_NETMASK_BITS || netBits > MAX_NETMASK_BITS) {
                            tmpKeys.add(entry.getKey());
                            continue;
                        }
                        hostAddrMask = 0xFFFFFFFF << (32 - netBits);
                        subStr = entry.getKey().replaceAll("/.*", "");
                        if (!subStr.matches(IPV4ADDR_TMP)) {
                            tmpKeys.add(entry.getKey());
                            continue;
                        }
                        cidrIpAddr = getIPV4IntValue(subStr);
                        ipSegmentHolder.put(entry.getKey(), Pair.of(cidrIpAddr & hostAddrMask, hostAddrMask));
                    } else {
                        if (!entry.getKey().matches(IPV4ADDR_TMP)) {
                            tmpKeys.add(entry.getKey());
                            continue;
                        }
                        ipAddrHolder.put(entry.getKey(), entry.getValue());
                    }
                    added = true;
                }
            }
            if (!tmpKeys.isEmpty()) {
                if (this.isBlackList) {
                    LOG.warn("Load BlackList data error, found error data items: " + tmpKeys);
                } else {
                    LOG.warn("Load WhiteList data error, found error data items: " + tmpKeys);
                }
            }
            return (isBlackList && added) || (!isBlackList && removed);
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    public boolean isContain(String strRemoteIP) {
        if (strRemoteIP == null) {
            return false;
        }
        if (ipAddrHolder.containsKey(strRemoteIP)) {
            return true;
        }
        int remoteId = getIPV4IntValue(strRemoteIP);
        for (Pair<Integer, Integer> subMask : ipSegmentHolder.values()) {
            if (subMask == null || subMask.getLeft() == null || subMask.getRight() == null) {
                continue;
            }
            if ((remoteId & subMask.getRight()) == subMask.getLeft()) {
                return true;
            }
        }
        return false;
    }

    public boolean isEmptyConfig() {
        return ipAddrHolder.isEmpty() && ipSegmentHolder.isEmpty();
    }

    private int getIPV4IntValue(String ipv4Addr) {
        String[] addrItems = ipv4Addr.split("\\.");
        return ((Integer.parseInt(addrItems[0]) << 24)
                | (Integer.parseInt(addrItems[1]) << 16)
                | (Integer.parseInt(addrItems[2]) << 8)
                | Integer.parseInt(addrItems[3]));
    }

    private Map<String, Long> loadFile() {
        String filePath = getFilePath();
        if (StringUtils.isBlank(filePath)) {
            LOG.error("Fail to load " + getFileName() + " as the file path is empty");
            return null;
        }
        FileReader reader = null;
        BufferedReader br = null;
        Map<String, Long> configMap = new HashMap<>();
        try {
            String line;
            reader = new FileReader(filePath);
            br = new BufferedReader(reader);
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (StringUtils.isBlank(line)
                        || line.startsWith("#")
                        || line.startsWith(";")) {
                    continue;
                }
                configMap.put(line, System.currentTimeMillis());
            }
            return configMap;
        } catch (Throwable e) {
            LOG.error("Fail to load " + getFileName() + ", path = {}, and e = {}", filePath, e);
            return null;
        } finally {
            IOUtils.closeQuietly(reader);
            IOUtils.closeQuietly(br);
        }
    }
}
