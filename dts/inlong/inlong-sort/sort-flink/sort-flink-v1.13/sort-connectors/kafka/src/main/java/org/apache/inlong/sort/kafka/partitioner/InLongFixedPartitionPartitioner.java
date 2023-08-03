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

package org.apache.inlong.sort.kafka.partitioner;

import org.apache.inlong.sort.base.format.AbstractDynamicSchemaFormat;
import org.apache.inlong.sort.base.format.DynamicSchemaFormatFactory;

import lombok.Setter;
import org.apache.flink.streaming.connectors.kafka.partitioner.FlinkKafkaPartitioner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Fixed partitions according to the given library name and table name rules.
 * */
public class InLongFixedPartitionPartitioner<T> extends FlinkKafkaPartitioner<T> {

    private static final Logger LOG = LoggerFactory.getLogger(InLongFixedPartitionPartitioner.class);

    private final Map<String, String> patternPartitionMap;
    private final Map<String, Pattern> regexPatternMap;
    @Setter
    private AbstractDynamicSchemaFormat dynamicSchemaFormat;

    /**
     * The format used to deserialization the raw data(bytes array)
     */
    @Setter
    private String sinkMultipleFormat;

    private final String DEFAULT_PARTITION = "DEFAULT_PARTITION";

    private String databasePattern;
    private String tablePattern;

    private final static String DELIMITER1 = "&";
    private final static String DELIMITER2 = "_";
    private final static String DELIMITER3 = ":";
    private final static String DELIMITER4 = ",";

    private final int defaultPartitionId;

    public InLongFixedPartitionPartitioner(String patternPartitionMapConfig, String partitionPattern) {
        this.patternPartitionMap = configToMap(patternPartitionMapConfig);
        this.regexPatternMap = new HashMap<>();
        this.databasePattern = partitionPattern.split(DELIMITER2)[0];
        this.tablePattern = partitionPattern.split(DELIMITER2)[1];
        this.defaultPartitionId = Integer.parseInt(patternPartitionMap.getOrDefault(DEFAULT_PARTITION, "0"));
    }

    @Override
    public void open(int parallelInstanceId, int parallelInstances) {
        super.open(parallelInstanceId, parallelInstances);
        dynamicSchemaFormat = DynamicSchemaFormatFactory.getFormat(sinkMultipleFormat);
    }

    /***
     * Partition mapping is performed according to the library name and table name rules,
     * and no matching is written to the default partition.
     * */
    @Override
    public int partition(T record, byte[] key, byte[] value, String targetTopic, int[] partitions) {
        try {
            for (Map.Entry<String, String> entry : patternPartitionMap.entrySet()) {
                if (DEFAULT_PARTITION.equals(entry.getKey())) {
                    continue;
                }
                String databaseName = dynamicSchemaFormat.parse(value, databasePattern);
                String tableName = dynamicSchemaFormat.parse(value, tablePattern);
                List<String> regexList = Arrays.asList(entry.getKey().split(DELIMITER1));
                String databaseNameRegex = regexList.get(0);
                String tableNameRegex = regexList.get(1);

                if (match(databaseName, databaseNameRegex) && match(tableName, tableNameRegex)) {
                    return Integer.parseInt(entry.getValue());
                }
            }
        } catch (Exception e) {
            LOG.warn("Extract partition failed", e);
        }
        return defaultPartitionId;
    }

    private boolean match(String name, String nameRegex) {
        if (nameRegex.equals("*")) {
            return true;
        }
        return regexPatternMap.computeIfAbsent(nameRegex, regex -> Pattern.compile(regex))
                .matcher(name)
                .matches();
    }

    private Map<String, String> configToMap(String patternPartitionMapStr) {
        if (patternPartitionMapStr == null) {
            return Collections.emptyMap();
        }

        Map<String, String> patternPartitionMap = new LinkedHashMap<>();
        for (String entry : patternPartitionMapStr.split(DELIMITER4)) {
            String pattern = entry.split(DELIMITER3)[0];
            String partition = entry.split(DELIMITER3)[1];
            patternPartitionMap.put(pattern, partition);
        }
        return patternPartitionMap;
    }
}
