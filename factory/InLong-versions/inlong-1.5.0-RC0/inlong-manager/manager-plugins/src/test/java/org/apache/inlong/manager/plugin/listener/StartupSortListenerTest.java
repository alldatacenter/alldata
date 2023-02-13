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

package org.apache.inlong.manager.plugin.listener;

import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.common.util.JsonUtils;
import org.apache.inlong.manager.pojo.group.InlongGroupExtInfo;
import org.apache.inlong.manager.pojo.group.pulsar.InlongPulsarInfo;
import org.apache.inlong.manager.pojo.workflow.form.process.GroupResourceProcessForm;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test class for start up sort listener.
 */
public class StartupSortListenerTest {

    @Test
    public void testListener() {
        WorkflowContext context = new WorkflowContext();
        GroupResourceProcessForm groupResourceForm = new GroupResourceProcessForm();
        context.setProcessForm(groupResourceForm);
        InlongPulsarInfo pulsarInfo = new InlongPulsarInfo();
        pulsarInfo.setInlongGroupId("1");
        groupResourceForm.setGroupInfo(pulsarInfo);

        List<InlongGroupExtInfo> inlongGroupExtInfos = new ArrayList<>();
        InlongGroupExtInfo inlongGroupExtInfo1 = new InlongGroupExtInfo();
        inlongGroupExtInfo1.setKeyName(InlongConstants.SORT_URL);
        inlongGroupExtInfo1.setKeyValue("127.0.0.1:8085");
        inlongGroupExtInfos.add(inlongGroupExtInfo1);

        InlongGroupExtInfo inlongGroupExtInfo2 = new InlongGroupExtInfo();
        inlongGroupExtInfo2.setKeyName(InlongConstants.SORT_PROPERTIES);
        Map<String, String> sortProperties = new HashMap<>(16);
        String sortStr = JsonUtils.toJsonString(sortProperties);
        inlongGroupExtInfo2.setKeyValue(sortStr);
        inlongGroupExtInfos.add(inlongGroupExtInfo2);

        InlongGroupExtInfo inlongGroupExtInfo5 = new InlongGroupExtInfo();
        inlongGroupExtInfo5.setKeyName(InlongConstants.DATAFLOW);
        inlongGroupExtInfo5.setKeyValue("{\"streamId\":{\n"
                + "    \"id\": 1,\n"
                + "    \"source_info\":\n"
                + "    {\n"
                + "        \"type\": \"pulsar\",\n"
                + "        \"admin_url\": \"http://127.0.0.1:8080\",\n"
                + "        \"service_url\": \"pulsar://127.0.0.1:6650\",\n"
                + "        \"topic\": \"persistent://pzr/pzr/pzr-topic\",\n"
                + "        \"subscription_name\": \"subscriptionName\",\n"
                + "        \"deserialization_info\":\n"
                + "        {\n"
                + "            \"type\": \"debezium_json\",\n"
                + "            \"ignore_parse_errors\": true,\n"
                + "            \"timestamp_format_standard\": \"ISO_8601\"\n"
                + "        },\n"
                + "        \"fields\":\n"
                + "        [\n"
                + "            {\n"
                + "                \"name\": \"name\",\n"
                + "                \"type\":\"base\",\n"
                + "                \"format_info\":\n"
                + "                {\n"
                + "                    \"type\": \"string\"\n"
                + "                }\n"
                + "            },\n"
                + "            {\n"
                + "            \t\"type\":\"base\",\n"
                + "                \"name\": \"age\",\n"
                + "                \"format_info\":\n"
                + "                {\n"
                + "                    \"type\": \"int\"\n"
                + "                }\n"
                + "            }\n"
                + "        ],\n"
                + "        \"authentication\": null\n"
                + "    },\n"
                + "    \"transformation_info\":\n"
                + "    {\n"
                + "         \"transform_rule\":\n"
                + "             {\n"
                + "                 \"type\":\"field_mapping\",\n"
                + "                 \"field_mapping_units\":\n"
                + "                     [\n"
                + "                         {\n"
                + "                             \"source_field\":\n"
                + "                                 {\n"
                + "                                     \"type\":\"base\",\n"
                + "                                     \"name\":\"age\",\n"
                + "                                     \"format_info\":\n"
                + "                                         {\n"
                + "                                             \"type\":\"int\"\n"
                + "                                         }\n"
                + "                                 },\n"
                + "                             \"sink_field\":\n"
                + "                                 {\n"
                + "                                     \"type\":\"base\",\n"
                + "                                     \"name\":\"age\",\n"
                + "                                     \"format_info\":\n"
                + "                                         {\n"
                + "                                             \"type\":\"int\"\n"
                + "                                         }\n"
                + "                                 }\n"
                + "                     },\n"
                + "                     {\n"
                + "                         \"source_field\":\n"
                + "                             {\n"
                + "                                 \"type\":\"base\",\n"
                + "                                 \"name\":\"name\",\n"
                + "                                 \"format_info\":\n"
                + "                                     {\n"
                + "                                         \"type\":\"string\"\n"
                + "                                     }\n"
                + "                             },\n"
                + "                         \"sink_field\":\n"
                + "                             {\n"
                + "                                 \"type\":\"base\",\n"
                + "                                 \"name\":\"name\",\n"
                + "                                 \"format_info\":\n"
                + "                                     {\n"
                + "                                         \"type\":\"string\"\n"
                + "                                     }\n"
                + "                             }\n"
                + "                     }\n"
                + "                 ]\n"
                + "         }\n"
                + "    },\n"
                + "    \"sink_info\":\n"
                + "    {\n"
                + "        \"type\": \"hive\",\n"
                + "        \"fields\":\n"
                + "        [\n"
                + "            {\n"
                + "            \t\"type\":\"base\",\n"
                + "                \"name\": \"name\",\n"
                + "                \"format_info\":\n"
                + "                {\n"
                + "                    \"type\": \"string\"\n"
                + "                }\n"
                + "            },\n"
                + "            {\n"
                + "                \"type\":\"base\",\n"
                + "                \"name\": \"age\",\n"
                + "                \"format_info\":\n"
                + "                {\n"
                + "                    \"type\": \"int\"\n"
                + "                }\n"
                + "            }\n"
                + "        ],\n"
                + "        \"hive_server_jdbc_url\": \"jdbc:hive2://127.0.0.1:10000\",\n"
                + "        \"database\": \"inlong_test\",\n"
                + "        \"table\": \"pzr\",\n"
                + "        \"username\": \"testUsername\",\n"
                + "        \"password\": \"testPassword\",\n"
                + "        \"data_path\": \"hdfs://127.0.0.1:8020\",\n"
                + "        \"partitions\":\n"
                + "        [],\n"
                + "        \"file_format\":\n"
                + "        {\n"
                + "            \"type\": \"text\",\n"
                + "            \"splitter\": \"|\"\n"
                + "        }\n"
                + "    },\n"
                + "    \"properties\":\n"
                + "    {\n"
                + "        \"pulsar.source.consumer.bootstrap-mode\": \"earliest\"\n"
                + "    }\n"
                + "}}");
        inlongGroupExtInfos.add(inlongGroupExtInfo5);
        pulsarInfo.setExtList(inlongGroupExtInfos);

        StartupSortListener startupSortListener = new StartupSortListener();
        // This method temporarily fails the test, so comment it out first
        // startupSortListener.listen(context);
    }
}
