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

package org.apache.inlong.manager.client.api.impl;

import org.apache.inlong.manager.pojo.workflow.form.process.ApplyGroupProcessForm;
import org.apache.inlong.manager.common.util.JsonUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit test for {@link InlongGroupImpl}
 */
class InlongGroupImplTest {

    @Test
    void testParseForm() {
        String json = "{\n"
                + "  \"formName\" : \"ApplyGroupProcessForm\",\n"
                + "  \"groupInfo\" : {\n"
                + "    \"mqType\" : \"PULSAR\",\n"
                + "    \"id\" : 5,\n"
                + "    \"inlongGroupId\" : \"test_group008\",\n"
                + "    \"name\" : null,\n"
                + "    \"description\" : null,\n"
                + "    \"mqResource\" : \"test_namespace\",\n"
                + "    \"enableZookeeper\" : 0,\n"
                + "    \"enableCreateResource\" : 1,\n"
                + "    \"lightweight\" : 1,\n"
                + "    \"inlongClusterTag\" : \"default_cluster\",\n"
                + "    \"dailyRecords\" : 10000000,\n"
                + "    \"dailyStorage\" : 10000,\n"
                + "    \"peakRecords\" : 100000,\n"
                + "    \"maxLength\" : 10000,\n"
                + "    \"inCharges\" : \"test_inCharges,admin\",\n"
                + "    \"followers\" : null,\n"
                + "    \"status\" : 101,\n"
                + "    \"creator\" : \"admin\",\n"
                + "    \"modifier\" : \"admin\",\n"
                + "    \"createTime\" : \"2022-06-05 20:44:22\",\n"
                + "    \"modifyTime\" : \"2022-06-05 12:44:28\",\n"
                + "    \"extList\" : [ ],\n"
                + "    \"tenant\" : null,\n"
                + "    \"adminUrl\" : null,\n"
                + "    \"serviceUrl\" : null,\n"
                + "    \"queueModule\" : \"PARALLEL\",\n"
                + "    \"partitionNum\" : 3,\n"
                + "    \"ensemble\" : 3,\n"
                + "    \"writeQuorum\" : 3,\n"
                + "    \"ackQuorum\" : 2,\n"
                + "    \"ttl\" : 24,\n"
                + "    \"ttlUnit\" : \"hours\",\n"
                + "    \"retentionTime\" : 72,\n"
                + "    \"retentionTimeUnit\" : \"hours\",\n"
                + "    \"retentionSize\" : -1,\n"
                + "    \"retentionSizeUnit\" : \"MB\"\n"
                + "  },\n"
                + "  \"streamInfoList\" : [ {\n"
                + "    \"id\" : 5,\n"
                + "    \"inlongGroupId\" : \"test_group008\",\n"
                + "    \"inlongStreamId\" : \"test_stream008\",\n"
                + "    \"name\" : \"test_stream008\",\n"
                + "    \"sinkList\" : [ {\n"
                + "      \"id\" : 5,\n"
                + "      \"inlongGroupId\" : \"test_group008\",\n"
                + "      \"inlongStreamId\" : \"test_stream008\",\n"
                + "      \"sinkType\" : \"HIVE\",\n"
                + "      \"sinkName\" : \"{hive.sink.name}\",\n"
                + "      \"clusterId\" : null,\n"
                + "      \"clusterUrl\" : null\n"
                + "    } ],\n"
                + "    \"modifyTime\" : \"2022-06-05 12:44:25\"\n"
                + "  } ]\n"
                + "}";

        ApplyGroupProcessForm applyGroupProcessForm =
                JsonUtils.parseObject(json, ApplyGroupProcessForm.class);

        assertNotNull(applyGroupProcessForm);
        assertNotNull(applyGroupProcessForm.getGroupInfo());
        assertNotNull(applyGroupProcessForm.getStreamInfoList());
    }

}
