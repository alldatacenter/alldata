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

package org.apache.inlong.agent.plugin.sources;

import org.apache.inlong.agent.conf.JobProfile;
import org.apache.inlong.agent.plugin.Reader;
import org.apache.inlong.agent.plugin.Source;
import org.apache.inlong.agent.plugin.metrics.SourceJmxMetric;
import org.apache.inlong.agent.plugin.metrics.SourceMetrics;
import org.apache.inlong.agent.plugin.metrics.SourcePrometheusMetrics;
import org.apache.inlong.agent.plugin.sources.reader.BinlogReader;
import org.apache.inlong.agent.utils.AgentUtils;
import org.apache.inlong.agent.utils.ConfigUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * binlog source, split binlog source job into multi readers
 */
public class BinlogSource implements Source {

    private static final Logger LOGGER = LoggerFactory.getLogger(BinlogSource.class);
    private static final String BINLOG_SOURCE_TAG_NAME = "BinlogSourceMetric";
    private static AtomicLong metricsIndex = new AtomicLong(0);
    private final SourceMetrics sourceMetrics;

    public BinlogSource() {
        if (ConfigUtil.isPrometheusEnabled()) {
            this.sourceMetrics = new SourcePrometheusMetrics(AgentUtils.getUniqId(
                    BINLOG_SOURCE_TAG_NAME, metricsIndex.incrementAndGet()));
        } else {
            this.sourceMetrics = new SourceJmxMetric(AgentUtils.getUniqId(
                    BINLOG_SOURCE_TAG_NAME, metricsIndex.incrementAndGet()));
        }
    }

    @Override
    public List<Reader> split(JobProfile conf) {
        Reader binlogReader = new BinlogReader();
        List<Reader> readerList = new ArrayList<>();
        readerList.add(binlogReader);
        sourceMetrics.incSourceSuccessCount();
        return readerList;
    }

}
