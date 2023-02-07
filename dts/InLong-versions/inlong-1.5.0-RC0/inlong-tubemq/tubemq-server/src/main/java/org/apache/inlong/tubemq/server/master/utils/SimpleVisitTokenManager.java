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

package org.apache.inlong.tubemq.server.master.utils;

import java.util.concurrent.atomic.AtomicLong;
import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.corebase.TokenConstants;
import org.apache.inlong.tubemq.corebase.daemon.AbstractDaemonService;
import org.apache.inlong.tubemq.server.master.MasterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleVisitTokenManager extends AbstractDaemonService {

    private static final Logger logger = LoggerFactory.getLogger(SimpleVisitTokenManager.class);

    private final MasterConfig masterConfig;
    private final AtomicLong validVisitAuthorized = new AtomicLong(0);
    private final AtomicLong freshVisitAuthorized = new AtomicLong(0);
    private String brokerVisitTokens = "";

    public SimpleVisitTokenManager(final MasterConfig masterConfig) {
        super("[VisitToken Manager]", (masterConfig.getVisitTokenValidPeriodMs() * 4) / 5);
        this.masterConfig = masterConfig;
        buildVisitTokens(true,
                new StringBuilder(TBaseConstants.BUILDER_DEFAULT_SIZE));
        super.start();
    }

    public long getCurVisitToken() {
        return validVisitAuthorized.get();
    }

    public long getFreshVisitToken() {
        return freshVisitAuthorized.get();
    }

    public String getBrokerVisitTokens() {
        return brokerVisitTokens;
    }

    @Override
    protected void loopProcess(StringBuilder strBuff) {
        try {
            buildVisitTokens(false, strBuff);
        } catch (Throwable t) {
            logger.error("[VisitToken Manager] Daemon generator thread throw error ", t);
        }
    }

    public void close(long waitTimeMs) {
        if (super.stop()) {
            return;
        }
        logger.info("[VisitToken Manager] VisitToken Manager service stopped!");
    }

    private void buildVisitTokens(boolean initial, StringBuilder strBuff) {
        if (initial) {
            freshVisitAuthorized.set(System.currentTimeMillis());
            validVisitAuthorized.set(freshVisitAuthorized.get());
        } else {
            validVisitAuthorized.set(freshVisitAuthorized.getAndSet(System.currentTimeMillis()));
        }
        brokerVisitTokens = strBuff.append(validVisitAuthorized.get())
                .append(TokenConstants.ARRAY_SEP).append(freshVisitAuthorized.get()).toString();
        strBuff.delete(0, strBuff.length());
    }
}
