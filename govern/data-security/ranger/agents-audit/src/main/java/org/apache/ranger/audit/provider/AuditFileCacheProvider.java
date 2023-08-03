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
package org.apache.ranger.audit.provider;

import org.apache.ranger.audit.model.AuditEventBase;
import org.apache.ranger.audit.queue.AuditFileCacheProviderSpool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Properties;

/*
 AuditFileCacheProvider class does the work of stashing the audit logs into Local Filesystem before sending it to the AuditBatchQueue Consumer
*/

public class AuditFileCacheProvider extends BaseAuditHandler {
    private static final Logger    logger = LoggerFactory.getLogger(AuditFileCacheProvider.class);

    AuditFileCacheProviderSpool fileSpooler = null;
    AuditHandler                consumer    = null;

    AuditFileCacheProvider(AuditHandler consumer) {
        this.consumer = consumer;
    }

    public void init(Properties prop, String basePropertyName) {
        String propPrefix = "xasecure.audit.filecache";
        if (basePropertyName != null) {
            propPrefix = basePropertyName;
        }
        super.init(prop, propPrefix);
        //init Consumer
        if (consumer != null) {
            consumer.init(prop, propPrefix);
        }
        //init AuditFileCacheSpooler
        fileSpooler = new AuditFileCacheProviderSpool(consumer);
        fileSpooler.init(prop,propPrefix);
    }
    @Override
    public boolean log(AuditEventBase event) {
        boolean ret = false;
        if ( event != null) {
            fileSpooler.stashLogs(event);
            if ( fileSpooler.isSpoolingSuccessful()) {
                ret = true;
            }
        }
        return ret;
    }

    @Override
    public boolean log(Collection<AuditEventBase> events) {
        boolean ret = true;
        if ( events != null) {
            for (AuditEventBase event : events) {
                ret = log(event);
            }
        }
        return ret;
    }

    @Override
    public void start() {
        // Start the consumer thread
        if (consumer != null) {
            consumer.start();
        }
        if (fileSpooler != null) {
            // start AuditFileSpool thread
            fileSpooler.start();
        }
    }

    @Override
    public void stop() {
        logger.info("Stop called. name=" + getName());
        if (consumer != null) {
            consumer.stop();
        }
    }

    @Override
    public void waitToComplete() {
        logger.info("waitToComplete called. name=" + getName());
        if ( consumer != null) {
            consumer.waitToComplete();
        }
    }

    @Override
    public void waitToComplete(long timeout) {
        logger.info("waitToComplete called. name=" + getName());
        if ( consumer != null) {
            consumer.waitToComplete(timeout);
        }
    }

    @Override
    public void flush() {
        logger.info("waitToComplete. name=" + getName());
        if ( consumer != null) {
            consumer.flush();
        }
    }
}
