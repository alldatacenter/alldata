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

package org.apache.inlong.agent.plugin.utils;

import org.apache.commons.io.FileUtils;
import org.apache.inlong.common.metric.MetricRegister;
import org.powermock.api.mockito.PowerMockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;

public class TestUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestUtils.class);
    private static final String RECORD = "This is the test line for file\n";

    public static String getTestTriggerProfile() {
        return "{\n"
                + "  \"job\": {\n"
                + "    \"fileJob\": {\n"
                + "      \"additionStr\": \"m=15&file=test\",\n"
                + "      \"trigger\": \"org.apache.inlong.agent.plugin.trigger.DirectoryTrigger\",\n"
                + "      \"dir\": {\n"
                + "        \"path\": \"\",\n"
                + "        \"patterns\": \"/AgentBaseTestsHelper/"
                + "org.apache.tubemq.inlong.plugin.fetcher.TestTdmFetcher/test*.dat\"\n"
                + "      },\n"
                + "      \"thread\" : {\n"
                + "\"running\": {\n"
                + "\"core\": \"4\"\n"
                + "}\n"
                + "} \n"
                + "    },\n"
                + "    \"id\": 1,\n"
                + "    \"op\": \"0\",\n"
                + "    \"ip\": \"127.0.0.1\",\n"
                + "    \"groupId\": \"groupId\",\n"
                + "    \"streamId\": \"streamId\",\n"
                + "    \"name\": \"fileAgentTest\",\n"
                + "    \"source\": \"org.apache.inlong.agent.plugin.sources.TextFileSource\",\n"
                + "    \"sink\": \"org.apache.inlong.agent.plugin.sinks.MockSink\",\n"
                + "    \"channel\": \"org.apache.inlong.agent.plugin.channel.MemoryChannel\",\n"
                + "    \"standalone\": true,\n"
                + "    \"deliveryTime\": \"1231313\",\n"
                + "    \"splitter\": \"&\"\n"
                + "  }\n"
                + "  }";
    }

    public static void createHugeFiles(String fileName, String rootDir, String record) throws Exception {
        final Path hugeFile = Paths.get(rootDir, fileName);
        FileWriter writer = new FileWriter(hugeFile.toFile());
        for (int i = 0; i < 1024; i++) {
            writer.write(record);
        }
        writer.flush();
        writer.close();
    }

    public static void createMultipleLineFiles(String fileName, String rootDir,
            String record, int lineNum) throws Exception {
        final Path hugeFile = Paths.get(rootDir, fileName);
        List<String> beforeList = new ArrayList<>();
        for (int i = 0; i < lineNum; i++) {
            beforeList.add(String.format("%s_%d", record, i));
        }
        Files.write(hugeFile, beforeList, StandardOpenOption.CREATE);
    }

    public static void mockMetricRegister() throws Exception {
        PowerMockito.mockStatic(MetricRegister.class);
        PowerMockito.doNothing().when(MetricRegister.class, "register", any());
    }

    public static void createFile(String fileName) throws Exception {
        FileWriter writer = new FileWriter(fileName);
        for (int i = 0; i < 1; i++) {
            writer.write(RECORD);
        }
        writer.flush();
        writer.close();
    }

    public static void deleteFile(String fileName) throws Exception {
        try {
            FileUtils.delete(Paths.get(fileName).toFile());
        } catch (Exception ignored) {
            LOGGER.warn("deleteFile error ", ignored);
        }
    }

    public static void write(String fileName, StringBuffer records) throws IOException {
        File file = new File(fileName);
        if (!file.exists()) {
            file.createNewFile();
        }

        FileWriter writer = new FileWriter(file, true);
        writer.write(records.toString());
        writer.flush();
        writer.close();
    }
}
