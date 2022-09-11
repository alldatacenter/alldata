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
import org.apache.inlong.agent.plugin.sources.reader.TextFileReader;
import org.apache.inlong.agent.plugin.utils.PluginUtils;
import org.apache.inlong.agent.utils.AgentUtils;
import org.apache.inlong.agent.utils.ConfigUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.apache.inlong.agent.constant.CommonConstants.POSITION_SUFFIX;
import static org.apache.inlong.agent.constant.JobConstants.DEFAULT_JOB_LINE_FILTER;
import static org.apache.inlong.agent.constant.JobConstants.DEFAULT_JOB_READ_WAIT_TIMEOUT;
import static org.apache.inlong.agent.constant.JobConstants.JOB_LINE_FILTER_PATTERN;
import static org.apache.inlong.agent.constant.JobConstants.JOB_READ_WAIT_TIMEOUT;

/**
 * Read text files
 */
public class TextFileSource implements Source {

    private static final Logger LOGGER = LoggerFactory.getLogger(TextFileSource.class);

    private static final String TEXT_FILE_SOURCE_TAG_NAME = "AgentTextFileSourceMetric";

    // path + suffix
    public static final String MD5_SUFFIX = ".md5";

    private final SourceMetrics sourceMetrics;
    private static AtomicLong metricsIndex = new AtomicLong(0);

    public TextFileSource() {
        if (ConfigUtil.isPrometheusEnabled()) {
            this.sourceMetrics = new SourcePrometheusMetrics(AgentUtils.getUniqId(
                    TEXT_FILE_SOURCE_TAG_NAME,  metricsIndex.incrementAndGet()));
        } else {
            this.sourceMetrics = new SourceJmxMetric(AgentUtils.getUniqId(
                    TEXT_FILE_SOURCE_TAG_NAME,  metricsIndex.incrementAndGet()));
        }
    }

    @Override
    public List<Reader> split(JobProfile jobConf) {
        Collection<File> allFiles = PluginUtils.findSuitFiles(jobConf);
        List<Reader> result = new ArrayList<>();
        String filterPattern = jobConf.get(JOB_LINE_FILTER_PATTERN, DEFAULT_JOB_LINE_FILTER);
        for (File file : allFiles) {
            int seekPosition = jobConf.getInt(file.getAbsolutePath() + POSITION_SUFFIX, 0);
            LOGGER.info("read from history position {} with job profile {}, file absolute path: {}", seekPosition,
                    jobConf.getInstanceId(), file.getAbsolutePath());
            String md5 = jobConf.get(file.getAbsolutePath() + MD5_SUFFIX, "");
            TextFileReader textFileReader = new TextFileReader(file, seekPosition);
            long waitTimeout = jobConf.getLong(JOB_READ_WAIT_TIMEOUT, DEFAULT_JOB_READ_WAIT_TIMEOUT);
            textFileReader.setWaitMillisecs(waitTimeout);
            addValidator(filterPattern, textFileReader);
            result.add(textFileReader);
        }
        // increment the count of successful sources
        sourceMetrics.incSourceSuccessCount();
        return result;
    }

    private void addValidator(String filterPattern, TextFileReader textFileReader) {
        textFileReader.addPatternValidator(filterPattern);
    }
}
