/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ranger.service;

import org.apache.ranger.common.PropertiesUtil;
import org.apache.ranger.common.db.RangerTransactionSynchronizationAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class RangerPluginActivityLogger {

    @Autowired
    RangerTransactionSynchronizationAdapter transactionSynchronizationAdapter;

    private static final Logger LOG = LoggerFactory.getLogger(RangerPluginActivityLogger.class);

    boolean pluginActivityAuditCommitInline = false;

    @PostConstruct
    public void init() {
        pluginActivityAuditCommitInline = PropertiesUtil.getBooleanProperty("ranger.plugin.activity.audit.commit.inline", false);
        LOG.info("ranger.plugin.activity.audit.commit.inline = " + pluginActivityAuditCommitInline);
        if (pluginActivityAuditCommitInline) {
            LOG.info("Will use TransactionManager for committing scheduled work");
        } else {
            LOG.info("Will use separate thread for committing scheduled work");
        }
    }

    public void commitAfterTransactionComplete(Runnable commitWork) {
        transactionSynchronizationAdapter.executeOnTransactionCompletion(commitWork);
    }

}
