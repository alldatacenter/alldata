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

package org.apache.inlong.manager.client.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.FileConverter;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.client.api.InlongClient;
import org.apache.inlong.manager.client.api.InlongGroup;
import org.apache.inlong.manager.client.api.InlongStreamBuilder;
import org.apache.inlong.manager.client.cli.pojo.CreateGroupConf;
import org.apache.inlong.manager.client.cli.util.ClientUtils;

import java.io.File;

/**
 * Create resource by json file.
 */
@Parameters(commandDescription = "Create resource by json file")
public class CreateCommand extends AbstractCommand {

    @Parameter()
    private java.util.List<String> params;

    public CreateCommand() {
        super("create");
        jcommander.addCommand("group", new CreateGroup());
    }

    @Parameters(commandDescription = "Create group by json file")
    private static class CreateGroup extends AbstractCommandRunner {

        @Parameter()
        private java.util.List<String> params;

        @Parameter(names = {"-f", "--file"},
                converter = FileConverter.class,
                description = "json file")
        private File file;

        @Parameter(names = {"-s"}, description = "optional log string to create file")
        private String input;

        @Override
        void run() {
            try {
                String content;
                if (input != null) {
                    content = input;
                } else {
                    content = ClientUtils.readFile(file);
                    if (StringUtils.isBlank(content)) {
                        System.out.println("Create group failed: file was empty!");
                        return;
                    }
                }
                // first extract groupconfig from the file passed in
                CreateGroupConf groupConf = objectMapper.readValue(content, CreateGroupConf.class);
                // get the correspodning inlonggroup, a.k.a the task to execute
                InlongClient inlongClient = ClientUtils.getClient();
                InlongGroup group = inlongClient.forGroup(groupConf.getGroupInfo());
                InlongStreamBuilder streamBuilder = group.createStream(groupConf.getStreamInfo());
                // put in parameters:source and sink,stream fields, then initialize
                streamBuilder.fields(groupConf.getStreamFieldList());
                streamBuilder.source(groupConf.getStreamSource());
                streamBuilder.sink(groupConf.getStreamSink());
                streamBuilder.initOrUpdate();
                // initialize the new stream group
                group.init();
                System.out.println("Create group success!");
            } catch (Exception e) {
                System.out.println("Create group failed!");
                System.out.println(e.getMessage());
            }
        }
    }
}
