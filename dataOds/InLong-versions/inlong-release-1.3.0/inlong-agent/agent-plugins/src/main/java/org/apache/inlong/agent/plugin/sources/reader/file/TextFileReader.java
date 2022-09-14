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
 *
 */

package org.apache.inlong.agent.plugin.sources.reader.file;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.apache.inlong.agent.constant.JobConstants.JOB_FILE_LINE_END_PATTERN;
import static org.apache.inlong.agent.constant.JobConstants.JOB_FILE_MONITOR_DEFAULT_STATUS;
import static org.apache.inlong.agent.constant.JobConstants.JOB_FILE_MONITOR_STATUS;

/**
 * Text file reader
 */
public final class TextFileReader extends AbstractFileReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(TextFileReader.class);

    private final Map<File, String> lineStringBuffer = new ConcurrentHashMap<>();

    public TextFileReader(FileReaderOperator fileReaderOperator) {
        super.fileReaderOperator = fileReaderOperator;
        if (fileReaderOperator.jobConf.get(JOB_FILE_MONITOR_STATUS, JOB_FILE_MONITOR_DEFAULT_STATUS)
                .equals(JOB_FILE_MONITOR_DEFAULT_STATUS)) {
            MonitorTextFile.getInstance().monitor(fileReaderOperator, this);
        }
    }

    public void getData() throws IOException {
        List<String> lines = Files.newBufferedReader(fileReaderOperator.file.toPath()).lines().skip(
                fileReaderOperator.position)
                .collect(Collectors.toList());
        LOGGER.info("path is {}, data reads size {}", fileReaderOperator.file.getName(), lines.size());
        List<String> resultLines = new ArrayList<>();
        //TODO line regular expression matching
        if (fileReaderOperator.jobConf.hasKey(JOB_FILE_LINE_END_PATTERN)) {
            Pattern pattern = Pattern.compile(fileReaderOperator.jobConf.get(JOB_FILE_LINE_END_PATTERN));
            lines.forEach(line -> {
                lineStringBuffer.put(fileReaderOperator.file,
                        lineStringBuffer.isEmpty() ? line
                                : lineStringBuffer.get(fileReaderOperator.file).concat(" ").concat(line));
                String data = lineStringBuffer.get(fileReaderOperator.file);
                Matcher matcher = pattern.matcher(data);
                if (matcher.find() && StringUtils.isNoneBlank(matcher.group())) {
                    String[] splitLines = data.split(matcher.group());
                    int length = splitLines.length;
                    for (int i = 0; i < length; i++) {
                        if (i > 0 && i == length - 1 && null != splitLines[i]) {
                            lineStringBuffer.put(fileReaderOperator.file, splitLines[i]);
                            break;
                        }
                        resultLines.add(splitLines[i].trim());
                    }
                    if (1 == length) {
                        lineStringBuffer.remove(fileReaderOperator.file);
                    }
                }
            });
            if (resultLines.isEmpty()) {
                return;
            }
        }
        lines = resultLines.isEmpty() ? lines : resultLines;
        fileReaderOperator.stream = lines.stream();
        fileReaderOperator.position = fileReaderOperator.position + lines.size();
    }

}
