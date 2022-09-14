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

package org.apache.inlong.agent.plugin.sources.reader.file;

import com.google.gson.Gson;
import org.apache.inlong.agent.plugin.utils.FileDataUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.apache.inlong.agent.constant.JobConstants.JOB_FILE_CONTENT_COLLECT_TYPE;
import static org.apache.inlong.agent.constant.JobConstants.JOB_FILE_META_FILTER_BY_LABELS;
import static org.apache.inlong.agent.constant.KubernetesConstants.DATA_CONTENT;
import static org.apache.inlong.agent.constant.KubernetesConstants.DATA_CONTENT_TIME;

/**
 * File reader template
 */
public abstract class AbstractFileReader {

    private static final Gson GSON = new Gson();
    public FileReaderOperator fileReaderOperator;

    public abstract void getData() throws Exception;

    public void mergeData(FileReaderOperator fileReaderOperator) {
        if (null == fileReaderOperator.metadata) {
            return;
        }
        List<String> lines = fileReaderOperator.stream.collect(Collectors.toList());
        if (fileReaderOperator.jobConf.hasKey(JOB_FILE_CONTENT_COLLECT_TYPE)) {
            long timestamp = System.currentTimeMillis();
            boolean isJson = FileDataUtils.isJSON(lines.isEmpty() ? null : lines.get(0));
            if (Objects.nonNull(fileReaderOperator.metadata)) {
                lines = lines.stream().map(data -> {
                    Map<String, String> mergeData = new HashMap<>(fileReaderOperator.metadata);
                    mergeData.put(DATA_CONTENT, FileDataUtils.getK8sJsonLog(data, isJson));
                    mergeData.put(DATA_CONTENT_TIME, String.valueOf(timestamp));
                    return GSON.toJson(mergeData);
                }).collect(Collectors.toList());
            } else if (!fileReaderOperator.jobConf.hasKey(JOB_FILE_META_FILTER_BY_LABELS)) {
                lines = lines.stream().map(data -> {
                    Map<String, String> mergeData = new HashMap<>();
                    mergeData.put(DATA_CONTENT, FileDataUtils.getK8sJsonLog(data, isJson));
                    mergeData.put(DATA_CONTENT_TIME, String.valueOf(timestamp));
                    return GSON.toJson(mergeData);
                }).collect(Collectors.toList());
            }
        }
        fileReaderOperator.stream = lines.stream();
    }

}
