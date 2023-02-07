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

package org.apache.inlong.sort.standalone;

import org.apache.flume.node.Application;
import org.apache.inlong.common.metric.MetricObserver;
import org.apache.inlong.sort.standalone.config.holder.CommonPropertiesHolder;
import org.apache.inlong.sort.standalone.metrics.audit.AuditUtils;
import org.apache.inlong.sort.standalone.utils.InlongLoggerFactory;
import org.slf4j.Logger;

/**
 * Sort Standalone Application
 */
public class SortStandaloneApplication {

    public static final Logger LOGGER = InlongLoggerFactory.getLogger(Application.class);

    /**
     * Main entrance
     */
    public static void main(String[] args) {
        LOGGER.info("start to sort-standalone");
        try {
            SortCluster cluster = new SortCluster();
            Runtime.getRuntime().addShutdownHook(new Thread("sort-standalone-shutdown-hook") {

                @Override
                public void run() {
                    AuditUtils.send();
                    cluster.close();
                }
            });
            // start the cluster
            cluster.start();
            // metrics
            MetricObserver.init(CommonPropertiesHolder.get());
            AuditUtils.initAudit();
            Thread.sleep(5000);
        } catch (Exception e) {
            LOGGER.error("fatal error occurred while running sort-standalone: ", e);
        }
    }
}