/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.seatunnel.datasource.plugin.kafka;

import org.apache.seatunnel.api.configuration.util.OptionRule;
import org.apache.seatunnel.datasource.plugin.api.model.TableField;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Map;

// todo: use test container to test
@Slf4j
@Disabled
public class KafkaDataSourceChannelTest {

    private static final KafkaDataSourceChannel KAFKA_DATA_SOURCE_CHANNEL =
            new KafkaDataSourceChannel();

    private static final String KAFKA_PLUGIN_NAME = "kafka";
    private static final String BOOTSTRAP_SERVER = "localhost:9092";

    private static final Map<String, String> REQUEST_PARAMS =
            new ImmutableMap.Builder<String, String>()
                    .put(KafkaOptionRule.BOOTSTRAP_SERVERS.key(), BOOTSTRAP_SERVER)
                    .build();

    @Test
    public void getDataSourceOptions() {
        OptionRule dataSourceMetadataFieldsByDataSourceName =
                KAFKA_DATA_SOURCE_CHANNEL.getDataSourceOptions(KAFKA_PLUGIN_NAME);
        Assertions.assertEquals(
                1, dataSourceMetadataFieldsByDataSourceName.getRequiredOptions().size());
    }

    @Test
    public void getDatasourceMetadataFieldsByDataSourceName() {
        OptionRule datasourceMetadataFieldsByDataSourceName =
                KAFKA_DATA_SOURCE_CHANNEL.getDatasourceMetadataFieldsByDataSourceName(
                        KAFKA_PLUGIN_NAME);
        Assertions.assertEquals(
                2, datasourceMetadataFieldsByDataSourceName.getOptionalOptions().size());
    }

    //    @Test
    //    public void getTables() {
    //        List<String> tables =
    //                KAFKA_DATA_SOURCE_CHANNEL.getTables(KAFKA_PLUGIN_NAME, REQUEST_PARAMS, null);
    //        log.info("{}", tables);
    //        Assertions.assertNotNull(tables);
    //    }

    @Test
    public void getDatabases() {
        List<String> databases =
                KAFKA_DATA_SOURCE_CHANNEL.getDatabases(KAFKA_PLUGIN_NAME, REQUEST_PARAMS);
        log.info("{}", databases);
        Assertions.assertNotNull(databases);
    }

    @Test
    public void checkDataSourceConnectivity() {
        boolean dataSourceConnectivity =
                KAFKA_DATA_SOURCE_CHANNEL.checkDataSourceConnectivity(
                        KAFKA_PLUGIN_NAME, REQUEST_PARAMS);
        Assertions.assertTrue(dataSourceConnectivity);
    }

    @Test
    public void getTableFields() {
        List<TableField> tableFields =
                KAFKA_DATA_SOURCE_CHANNEL.getTableFields(KAFKA_PLUGIN_NAME, REQUEST_PARAMS, "", "");
        log.info("{}", tableFields);
        Assertions.assertTrue(tableFields.isEmpty());
    }

    @Test
    public void testGetTableFields() {
        Map<String, List<TableField>> tableFields =
                KAFKA_DATA_SOURCE_CHANNEL.getTableFields(
                        KAFKA_PLUGIN_NAME, REQUEST_PARAMS, "", Collections.emptyList());
        log.info("{}", tableFields);
        Assertions.assertTrue(tableFields.isEmpty());
    }
}
