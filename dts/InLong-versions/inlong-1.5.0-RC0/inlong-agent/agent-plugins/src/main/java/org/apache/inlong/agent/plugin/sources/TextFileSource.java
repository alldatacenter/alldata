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
import org.apache.inlong.agent.constant.DataCollectType;
import org.apache.inlong.agent.constant.JobConstants;
import org.apache.inlong.agent.plugin.Reader;
import org.apache.inlong.agent.plugin.sources.reader.file.FileReaderOperator;
import org.apache.inlong.agent.plugin.sources.reader.file.TriggerFileReader;
import org.apache.inlong.agent.plugin.utils.FileDataUtils;
import org.apache.inlong.agent.plugin.utils.PluginUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.apache.inlong.agent.constant.CommonConstants.POSITION_SUFFIX;
import static org.apache.inlong.agent.constant.JobConstants.DEFAULT_JOB_LINE_FILTER;
import static org.apache.inlong.agent.constant.JobConstants.DEFAULT_JOB_READ_WAIT_TIMEOUT;
import static org.apache.inlong.agent.constant.JobConstants.JOB_LINE_FILTER_PATTERN;
import static org.apache.inlong.agent.constant.JobConstants.JOB_READ_WAIT_TIMEOUT;
import static org.apache.inlong.agent.constant.JobConstants.JOB_TRIGGER;

/**
 * Read text files
 */
public class TextFileSource extends AbstractSource {

    private static final Logger LOGGER = LoggerFactory.getLogger(TextFileSource.class);

    public TextFileSource() {
    }

    @Override
    public List<Reader> split(JobProfile jobConf) {
        super.init(jobConf);
        if (jobConf.hasKey(JOB_TRIGGER)) {
            // trigger as a special reader.
            return Collections.singletonList(new TriggerFileReader());
        }

        Collection<File> allFiles = PluginUtils.findSuitFiles(jobConf);
        allFiles = FileDataUtils.filterFile(allFiles, jobConf);
        String filterPattern = jobConf.get(JOB_LINE_FILTER_PATTERN, DEFAULT_JOB_LINE_FILTER);
        LOGGER.info("file splits size: {}", allFiles.size());
        List<Reader> result = new ArrayList<>();
        for (File file : allFiles) {
            int startPosition = getStartPosition(jobConf, file);
            LOGGER.info("read from history position {} with job profile {}, file absolute path: {}", startPosition,
                    jobConf.getInstanceId(), file.getAbsolutePath());
            FileReaderOperator fileReader = new FileReaderOperator(file, startPosition);
            long waitTimeout = jobConf.getLong(JOB_READ_WAIT_TIMEOUT, DEFAULT_JOB_READ_WAIT_TIMEOUT);
            fileReader.setWaitMillisecond(waitTimeout);
            addValidator(filterPattern, fileReader);
            result.add(fileReader);
        }
        // increment the count of successful sources
        sourceMetric.sourceSuccessCount.incrementAndGet();
        return result;
    }

    private int getStartPosition(JobProfile jobConf, File file) {
        int seekPosition;
        if (jobConf.hasKey(JobConstants.JOB_FILE_CONTENT_COLLECT_TYPE) && DataCollectType.INCREMENT
                .equalsIgnoreCase(jobConf.get(JobConstants.JOB_FILE_CONTENT_COLLECT_TYPE))) {
            try (LineNumberReader lineNumberReader = new LineNumberReader(new FileReader(file.getPath()))) {
                lineNumberReader.skip(Long.MAX_VALUE);
                seekPosition = lineNumberReader.getLineNumber();
                return seekPosition;
            } catch (IOException ex) {
                LOGGER.error("get position error, file absolute path: {}", file.getAbsolutePath());
                throw new RuntimeException(ex);
            }
        }
        seekPosition = jobConf.getInt(file.getAbsolutePath() + POSITION_SUFFIX, 0);
        return seekPosition;
    }

    private void addValidator(String filterPattern, FileReaderOperator fileReader) {
        fileReader.addPatternValidator(filterPattern);
    }
}
