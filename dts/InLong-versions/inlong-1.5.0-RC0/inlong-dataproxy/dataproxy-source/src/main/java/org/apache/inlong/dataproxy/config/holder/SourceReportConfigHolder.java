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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Source report configure holder
 *
 */
public class SourceReportConfigHolder {

    public static final Logger LOG =
            LoggerFactory.getLogger(SourceReportConfigHolder.class);

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Map<String, SourceReportInfo> sourceConfMap = new HashMap<>();
    // Begin: this part can be optimized later
    // after optimizing the implementation of the heartbeat reporting interface
    // between Manager and DataProxy, the report is directly configured according to
    // the SourceReportInfo of each source, instead of splicing report items separately.
    private String ipSet = "";
    private String portSet = "";
    private String protocolTypeSet = "";
    // end

    public SourceReportConfigHolder() {

    }

    public void addSourceInfo(String sourceIp, String sourcePort, String protocolType) {
        if (StringUtils.isEmpty(sourceIp)
                || StringUtils.isEmpty(sourcePort)
                || StringUtils.isEmpty(protocolType)) {
            LOG.warn("[Source Report Holder] found empty parameter!, add values is {}, {}, {}",
                    sourceIp, sourcePort, protocolType);
            return;
        }
        String recordKey = sourceIp + "#" + sourcePort + "#" + protocolType;
        SourceReportInfo sourceReportInfo =
                new SourceReportInfo(sourceIp, sourcePort, protocolType);
        try {
            readWriteLock.writeLock().lock();
            if (sourceConfMap.putIfAbsent(recordKey, sourceReportInfo) == null) {
                if (ipSet.isEmpty()) {
                    ipSet = sourceIp;
                    portSet = sourcePort;
                    protocolTypeSet = protocolType;
                } else {
                    ipSet += "," + sourceIp;
                    portSet += "," + sourcePort;
                    protocolTypeSet += "," + protocolType;
                }
            }
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    public SourceReportInfo getSourceReportInfo() {
        try {
            readWriteLock.readLock().lock();
            return new SourceReportInfo(ipSet, portSet, protocolTypeSet);
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

}
