/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.kafka;

import org.apache.atlas.ApplicationProperties;
import org.apache.atlas.AtlasException;
import org.apache.atlas.notification.LogConfigUtils;
import org.apache.atlas.notification.NotificationInterface;
import org.apache.atlas.notification.spool.AtlasFileSpool;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Provider class for Notification interfaces
 */
public class NotificationProvider {
    private static final Logger LOG = LoggerFactory.getLogger(NotificationProvider.class);

    private static final String CONF_ATLAS_HOOK_SPOOL_ENABLED    = "atlas.hook.spool.enabled";
    private static final String CONF_ATLAS_HOOK_SPOOL_DIR        = "atlas.hook.spool.dir";

    private static final boolean CONF_ATLAS_HOOK_SPOOL_ENABLED_DEFAULT = false;

    private static NotificationInterface notificationProvider;

    public static NotificationInterface get() {
        if (notificationProvider == null) {
            try {
                Configuration     conf     = ApplicationProperties.get();
                KafkaNotification kafka    = new KafkaNotification(conf);
                String            spoolDir = getSpoolDir(conf);

                if (isSpoolingEnabled(conf) && StringUtils.isNotEmpty(spoolDir)) {
                    LOG.info("Notification spooling is enabled: spool directory={}", spoolDir);

                    conf.setProperty(CONF_ATLAS_HOOK_SPOOL_DIR, spoolDir);

                    notificationProvider = new AtlasFileSpool(conf, kafka);
                } else {
                    LOG.info("Notification spooling is not enabled");

                    notificationProvider = kafka;
                }
            } catch (AtlasException e) {
                throw new RuntimeException(e);
            }
        }
        return notificationProvider;
    }

    private static boolean isSpoolingEnabled(Configuration configuration) {
        return configuration.getBoolean(CONF_ATLAS_HOOK_SPOOL_ENABLED, CONF_ATLAS_HOOK_SPOOL_ENABLED_DEFAULT);
    }

    private static String getSpoolDir(Configuration configuration) {
        return configuration.getString(CONF_ATLAS_HOOK_SPOOL_DIR);
    }
}
