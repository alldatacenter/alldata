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

import com.google.gson.Gson;
import org.apache.inlong.agent.conf.JobProfile;
import org.apache.inlong.agent.constant.SnapshotModeConstants;
import org.apache.inlong.agent.plugin.sources.reader.BinlogReader;
import org.apache.inlong.agent.pojo.DebeziumFormat;
import org.junit.Assert;
import org.junit.Test;

import static org.apache.inlong.agent.constant.CommonConstants.PROXY_INLONG_GROUP_ID;
import static org.apache.inlong.agent.constant.CommonConstants.PROXY_INLONG_STREAM_ID;

public class TestBinlogReader {

    private static Gson gson = new Gson();

    @Test
    public void testDebeziumFormat() {
        String debeziumJson = "{\n"
                + "    \"before\": null,\n"
                + "    \"after\": {\n"
                + "      \"id\": 1004,\n"
                + "      \"first_name\": \"Anne\",\n"
                + "      \"last_name\": \"Kretchmar\",\n"
                + "      \"email\": \"annek@noanswer.org\"\n"
                + "    },\n"
                + "    \"source\": {\n"
                + "      \"version\": \"1.8.1.Final\",\n"
                + "      \"name\": \"dbserver1\",\n"
                + "      \"server_id\": 0,\n"
                + "      \"ts_sec\": 0,\n"
                + "      \"gtid\": null,\n"
                + "      \"file\": \"mysql-bin.000003\",\n"
                + "      \"pos\": 154,\n"
                + "      \"row\": 0,\n"
                + "      \"snapshot\": true,\n"
                + "      \"thread\": null,\n"
                + "      \"db\": \"inventory\",\n"
                + "      \"table\": \"customers\"\n"
                + "    },\n"
                + "    \"op\": \"r\",\n"
                + "    \"ts_ms\": 1486500577691\n"
                + "  }";
        DebeziumFormat debeziumFormat = gson
                .fromJson(debeziumJson, DebeziumFormat.class);
        Assert.assertEquals("customers", debeziumFormat.getSource().getTable());
    }

    // @Test
    public void binlogStartSpacialTest() throws Exception {
        JobProfile jobProfile = new JobProfile();
        jobProfile.set(BinlogReader.JOB_DATABASE_USER, "root");
        jobProfile.set(BinlogReader.JOB_DATABASE_PASSWORD, "123456");
        jobProfile.set(BinlogReader.JOB_DATABASE_HOSTNAME, "");
        jobProfile.set(BinlogReader.JOB_DATABASE_PORT, "3307");
        jobProfile.set(BinlogReader.JOB_DATABASE_WHITELIST, "etl");
        jobProfile.set(BinlogReader.JOB_DATABASE_OFFSET_SPECIFIC_OFFSET_FILE, "mysql-bin.000001");
        jobProfile.set(BinlogReader.JOB_DATABASE_OFFSET_SPECIFIC_OFFSET_POS, "5641");
        jobProfile.set(BinlogReader.JOB_DATABASE_SNAPSHOT_MODE, SnapshotModeConstants.SPECIFIC_OFFSETS);
        // jobProfile.set(BinlogReader.JOB_DATABASE_STORE_HISTORY_FILENAME, "");
        jobProfile.set(BinlogReader.JOB_DATABASE_STORE_OFFSET_INTERVAL_MS, String.valueOf(6000L));
        jobProfile.set("job.instance.id", "_1");
        jobProfile.set(PROXY_INLONG_GROUP_ID, "groupid");
        jobProfile.set(PROXY_INLONG_STREAM_ID, "streamid");
        BinlogReader binlogReader = new BinlogReader();
        binlogReader.init(jobProfile);
        while (true) {

        }
    }
}
