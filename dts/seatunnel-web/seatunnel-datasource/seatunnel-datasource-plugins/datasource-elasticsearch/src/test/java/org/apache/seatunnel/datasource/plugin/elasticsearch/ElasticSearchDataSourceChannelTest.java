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

package org.apache.seatunnel.datasource.plugin.elasticsearch;

import org.apache.seatunnel.datasource.plugin.api.model.TableField;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.Map;

// todo: use testcontainer to create container
@Disabled
class ElasticSearchDataSourceChannelTest {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ElasticSearchDataSourceChannelTest.class);

    private static final ElasticSearchDataSourceChannel ELASTIC_SEARCH_DATA_SOURCE_CHANNEL =
            new ElasticSearchDataSourceChannel();

    private static final String PLUGIN_NAME = "ElasticSearch";

    private static final String DATABASE = "Default";

    private static final Map<String, String> REQUEST_MAP =
            new ImmutableMap.Builder<String, String>()
                    .put(ElasticSearchOptionRule.HOSTS.key(), "[\"http://localhost:9200\"]")
                    .build();

    @Test
    void canAbleGetSchema() {
        Assertions.assertTrue(ELASTIC_SEARCH_DATA_SOURCE_CHANNEL.canAbleGetSchema());
    }

    @Test
    void getDataSourceOptions() {
        Assertions.assertNotNull(
                ELASTIC_SEARCH_DATA_SOURCE_CHANNEL.getDataSourceOptions(PLUGIN_NAME));
    }

    @Test
    void getDatasourceMetadataFieldsByDataSourceName() {
        Assertions.assertNotNull(
                ELASTIC_SEARCH_DATA_SOURCE_CHANNEL.getDatasourceMetadataFieldsByDataSourceName(
                        PLUGIN_NAME));
    }

    //    @Test
    //    void getTables() {
    //        Assertions.assertDoesNotThrow(
    //                () -> {
    //                    List<String> tables =
    //                            ELASTIC_SEARCH_DATA_SOURCE_CHANNEL.getTables(
    //                                    PLUGIN_NAME, REQUEST_MAP, DATABASE);
    //                    LOGGER.info("{}", tables);
    //                });
    //    }

    @Test
    void getDatabases() {
        Assertions.assertLinesMatch(
                Lists.newArrayList("default"),
                ELASTIC_SEARCH_DATA_SOURCE_CHANNEL.getDatabases(PLUGIN_NAME, REQUEST_MAP));
    }

    @Test
    void checkDataSourceConnectivity() {
        Assertions.assertTrue(
                ELASTIC_SEARCH_DATA_SOURCE_CHANNEL.checkDataSourceConnectivity(
                        PLUGIN_NAME, REQUEST_MAP));
    }

    @Test
    void getTableFields() {
        Assertions.assertDoesNotThrow(
                () -> {
                    List<TableField> tableFields =
                            ELASTIC_SEARCH_DATA_SOURCE_CHANNEL.getTableFields(
                                    PLUGIN_NAME, REQUEST_MAP, DATABASE, "");
                    LOGGER.info("{}", tableFields);
                });
    }

    @Test
    void testGetTableFields() {
        Assertions.assertDoesNotThrow(
                () -> {
                    Map<String, List<TableField>> tableFields =
                            ELASTIC_SEARCH_DATA_SOURCE_CHANNEL.getTableFields(
                                    PLUGIN_NAME, REQUEST_MAP, DATABASE, Lists.newArrayList(""));
                    LOGGER.info("{}", tableFields);
                });
    }
}
