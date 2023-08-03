/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ranger.services.schema.registry.client.util;

import org.apache.ranger.services.schema.registry.client.AutocompletionAgent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TestAutocompletionAgent extends AutocompletionAgent {
    public TestAutocompletionAgent(String serviceName, Map<String, String> configs) {
        super(serviceName, configs);
    }

    @Override
    public List<String> getSchemaGroupList(String lookupGroupName, List<String> groupList) {
        List<String> res = new ArrayList<>(groupList);
        res.add("testGroup");

        return res;
    }

    @Override
    public List<String> getSchemaMetadataList(String finalSchemaMetadataName,
                                              List<String> schemaGroupList,
                                              List<String> schemaMetadataList) {
        List<String> res = new ArrayList<>(schemaMetadataList);
        res.add("testSchema");

        return res;
    }

    @Override
    public List<String> getBranchList(String lookupBranchName,
                                      List<String> groups,
                                      List<String> schemaList,
                                      List<String> branchList) {
        List<String> res = new ArrayList<>(branchList);
        res.add("testBranch");

        return res;
    }
}
