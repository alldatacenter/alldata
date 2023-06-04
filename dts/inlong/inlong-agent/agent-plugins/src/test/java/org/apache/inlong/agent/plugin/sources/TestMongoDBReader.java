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

import com.alibaba.fastjson.JSONPath;
import org.apache.inlong.agent.conf.JobProfile;
import org.apache.inlong.agent.constant.JobConstants;
import org.apache.inlong.agent.plugin.Message;
import org.apache.inlong.agent.plugin.sources.reader.MongoDBReader;
import org.apache.inlong.agent.pojo.DebeziumFormat;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import static org.apache.inlong.agent.constant.CommonConstants.PROXY_INLONG_GROUP_ID;
import static org.apache.inlong.agent.constant.CommonConstants.PROXY_INLONG_STREAM_ID;

/**
 * TestMongoDBReader : TestMongoDBReader
 */
public class TestMongoDBReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestMongoDBReader.class);

    /**
     * Change event format verification
     */
    @Test
    public void testDebeziumFormat() {
        String json = "{\n"
                + "    \"payload\": {\n"
                + "        \"after\": \"{\\\"_id\\\":{\\\"$oid\\\":\\\"6304d21d96befa25a3630c5e\\\"}"
                + ",\\\"orderId\\\":\\\"o0011\\\",\\\"userId\\\":\\\"u0012\\\",\\\"items\\\":"
                + "[{\\\"itemId2\\\":\\\"i001\\\",\\\"itemName\\\":\\\"yyy\\\"},"
                + "{\\\"itemId\\\":\\\"i002\\\",\\\"itemName\\\":\\\"yyy2\\\"}]}\",\n"
                + "        \"patch\": null,\n"
                + "        \"filter\": null,\n"
                + "        \"updateDescription\": {\n"
                + "            \"removedFields\": null,\n"
                + "            \"updatedFields\": \"{\\\"items\\\":[{\\\"itemId2\\\":\\\"i001\\\", "
                + "\\\"itemName\\\":\\\"xxx\\\"}, {\\\"itemId\\\":\\\"i002\\\", \\\"itemName\\\":\\\"xxx2\\\"}], "
                + "\\\"userId\\\":\\\"u0012\\\"}\",\n"
                + "            \"truncatedArrays\": null\n"
                + "        },\n"
                + "        \"source\": {\n"
                + "            \"version\": \"1.8.0.Final\",\n"
                + "            \"connector\": \"mongodb\",\n"
                + "            \"name\": \"myrs\",\n"
                + "            \"ts_ms\": 1661332000000,\n"
                + "            \"snapshot\": \"false\",\n"
                + "            \"db\": \"mall\",\n"
                + "            \"sequence\": null,\n"
                + "            \"rs\": \"myrs\",\n"
                + "            \"collection\": \"order\",\n"
                + "            \"ord\": 1,\n"
                + "            \"h\": null,\n"
                + "            \"tord\": null,\n"
                + "            \"stxnid\": null,\n"
                + "            \"lsid\": null,\n"
                + "            \"txnNumber\": null\n"
                + "        },\n"
                + "        \"op\": \"u\",\n"
                + "        \"ts_ms\": 1661332000257,\n"
                + "        \"transaction\": null\n"
                + "    }\n"
                + "}";
        DebeziumFormat debeziumFormat = JSONPath.read(json, "$.payload", DebeziumFormat.class);
        Assert.assertEquals("order", debeziumFormat.getSource().getCollection());
        Assert.assertEquals("false", debeziumFormat.getSource().getSnapshot());
    }

    /**
     * Use local Mongo shard cluster for temporary testing
     */
    @Ignore
    public void readChangeEventFromMongo() {
        JobProfile jobProfile = new JobProfile();
        jobProfile.set("job.mongoJob.hosts", "localhost:37018");
        jobProfile.set("job.mongoJob.user", "mongo");
        jobProfile.set("job.mongoJob.password", "root");
        jobProfile.set("job.mongoJob.name", "myrs");
        jobProfile.set("job.mongoJob.connectMaxAttempts", "3");
        jobProfile.set("job.mongoJob.databaseIncludeList", "mall");
        jobProfile.set("job.mongoJob.collectionIncludeList", "order");
        jobProfile.set("job.mongoJob.snapshotMode", "never");
        jobProfile.set(JobConstants.JOB_INSTANCE_ID, UUID.randomUUID().toString());

        jobProfile.set(PROXY_INLONG_GROUP_ID, UUID.randomUUID().toString());
        jobProfile.set(PROXY_INLONG_STREAM_ID, UUID.randomUUID().toString());
        MongoDBReader mongoReader = new MongoDBReader();
        mongoReader.init(jobProfile);
        while (true) {
            Message message = mongoReader.read();
            if (message != null) {
                LOGGER.info("event content: {}", message);
            }
        }
    }
}
