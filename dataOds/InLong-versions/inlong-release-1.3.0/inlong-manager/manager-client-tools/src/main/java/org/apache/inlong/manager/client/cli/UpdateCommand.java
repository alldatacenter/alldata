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
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.client.api.InlongClient;
import org.apache.inlong.manager.client.api.InlongGroup;
import org.apache.inlong.manager.client.cli.util.ClientUtils;
import org.apache.inlong.manager.pojo.sort.BaseSortConf;

import java.io.File;
import java.util.List;

/**
 * The update command used to change the fields of inlong groups.
 * Please refer to the document for parameters
 */
@Parameters(commandDescription = "Update resource by json file")
public class UpdateCommand extends AbstractCommand {

    @Parameter()
    private List<String> params;

    public UpdateCommand() {
        super("update");
        jcommander.addCommand("group", new UpdateCommand.UpdateGroup());
    }

    @Parameters(commandDescription = "Update group by json file")
    private static class UpdateGroup extends AbstractCommandRunner {

        @Parameter()
        private List<String> params;

        @Parameter(names = {"--group", "-g"}, required = true, description = "inlong group id")
        private String inlongGroupId;

        @Parameter(names = {"-c", "--config"},
                required = true, description = "json file")
        private File file;

        @Override
        void run() {
            try {
                InlongClient inlongClient = ClientUtils.getClient();
                InlongGroup group = inlongClient.getGroup(inlongGroupId);
                String fileContent = ClientUtils.readFile(file);
                if (StringUtils.isBlank(fileContent)) {
                    System.out.println("Update group failed: file was empty!");
                    return;
                }
                // first extract groupconfig from the file passed in
                BaseSortConf sortConf = objectMapper.readValue(fileContent, BaseSortConf.class);
                group.update(sortConf);
                System.out.println("update group success");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
