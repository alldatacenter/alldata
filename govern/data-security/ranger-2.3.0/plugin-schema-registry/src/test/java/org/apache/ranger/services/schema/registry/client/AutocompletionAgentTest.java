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

package org.apache.ranger.services.schema.registry.client;

import org.apache.ranger.services.schema.registry.client.connection.ISchemaRegistryClient;
import org.apache.ranger.services.schema.registry.client.util.DefaultSchemaRegistryClientForTesting;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;


public class AutocompletionAgentTest {


    @Test
    public void connectionTest() {
        ISchemaRegistryClient client = new DefaultSchemaRegistryClientForTesting();

        AutocompletionAgent autocompletionAgent =
                new AutocompletionAgent("schema-registry", client);

        HashMap<String, Object> res = autocompletionAgent.connectionTest();
        assertEquals(res.get("connectivityStatus"), true);
        assertEquals(res.get("message"), "ConnectionTest Successful");
        assertEquals(res.get("description"), "ConnectionTest Successful");
        assertNull(res.get("objectId"));
        assertNull(res.get("fieldName"));


        client = new DefaultSchemaRegistryClientForTesting() {
            public void checkConnection() throws Exception {
                throw new Exception("Cannot connect to the SR server");
            }
        };
        autocompletionAgent =
                new AutocompletionAgent("schema-registry", client);

        res = autocompletionAgent.connectionTest();
        String errMessage = "You can still save the repository and start creating "
                + "policies, but you would not be able to use autocomplete for "
                + "resource names. Check server logs for more info.";
        assertEquals(res.get("connectivityStatus"), false);
        assertThat(res.get("message"), is(errMessage));
        assertThat(res.get("description"), is(errMessage));
        assertNull(res.get("objectId"));
        assertNull(res.get("fieldName"));

    }

    @Test
    public void getSchemaGroupList() {
        ISchemaRegistryClient client = new DefaultSchemaRegistryClientForTesting(){
            public List<String> getSchemaGroups() {
                List<String> groups = new ArrayList<>();
                groups.add("testGroup");
                return groups;
            }
        };

        AutocompletionAgent autocompletionAgent =
                new AutocompletionAgent("schema-registry", client);

        // Empty initialGroups and the list of groups returned by ISchemaRegistryClient
        // doesn't contain any groups that starts with 'tesSome'
        List<String> initialGroups = new ArrayList<>();
        List<String> res = autocompletionAgent.getSchemaGroupList("tesSome", initialGroups);
        assertEquals(0, res.size());

        // Empty initialGroups and the list of groups returned by ISchemaRegistryClient
        // contains a group that starts with 'tes'
        initialGroups = new ArrayList<>();
        res = autocompletionAgent.getSchemaGroupList("tes", initialGroups);
        List<String>  expected = new ArrayList<>();
        expected.add("testGroup");
        assertEquals(1, res.size());
        assertThat(res, is(expected));

        // initialGroups contains one element, list of the groups returned by ISchemaRegistryClient
        // contains the same values that are already present in initialGroups
        initialGroups = new ArrayList<>();
        initialGroups.add("testGroup");
        res = autocompletionAgent.getSchemaGroupList("tes", initialGroups);
        expected = new ArrayList<>();
        expected.add("testGroup");
        assertEquals(1, res.size());
        assertThat(res, is(expected));

        // initialGroups contains one element, list of the groups returned by ISchemaRegistryClient
        // contains one element too, that is not equal to the element in initialGroups
        initialGroups = new ArrayList<>();
        initialGroups.add("testGroup2");
        res = autocompletionAgent.getSchemaGroupList("tes", initialGroups);
        expected = new ArrayList<>();
        expected.add("testGroup2");
        expected.add("testGroup");
        assertEquals(2, res.size());
        assertThat(res, is(expected));

    }

    @Test
    public void getSchemaMetadataList() {
        ISchemaRegistryClient client = new DefaultSchemaRegistryClientForTesting(){

            public List<String> getSchemaNames(List<String> schemaGroup) {
                if(!schemaGroup.contains("Group1")) {
                    return new ArrayList<>();
                }
                List<String> schemas = new ArrayList<>();
                schemas.add("testSchema");
                return schemas;
            }
        };

        AutocompletionAgent autocompletionAgent =
                new AutocompletionAgent("schema-registry", client);

        List<String> groupList = new ArrayList<>();
        groupList.add("Group1");
        groupList.add("Group2");
        List<String> res = autocompletionAgent.getSchemaMetadataList("tes", groupList, new ArrayList<>());
        List<String> expected = new ArrayList<>();
        expected.add("testSchema");
        assertEquals(1, res.size());
        assertThat(res, is(expected));

        res = autocompletionAgent.getSchemaMetadataList("tesSome", groupList, new ArrayList<>());
        assertEquals(0, res.size());
    }

    @Test
    public void getBranchList() {
        ISchemaRegistryClient client = new DefaultSchemaRegistryClientForTesting(){

            public List<String> getSchemaBranches(String schemaMetadataName) {
                if(!schemaMetadataName.equals("Schema1")) {
                    return new ArrayList<>();
                }
                List<String> branches = new ArrayList<>();
                branches.add("testBranch");
                return branches;
            }


            public List<String> getSchemaNames(List<String> schemaGroup) {
                if(!schemaGroup.contains("Group1")) {
                    return new ArrayList<>();
                }
                List<String> schemas = new ArrayList<>();
                schemas.add("Schema1");
                return schemas;
            }
        };

        AutocompletionAgent autocompletionAgent =
                new AutocompletionAgent("schema-registry", client);

        List<String> schemaList = new ArrayList<>();
        schemaList.add("Schema1");
        schemaList.add("Schema2");
        List<String> groups = new ArrayList<>();
        groups.add("Group1");
        List<String> res = autocompletionAgent.getBranchList("tes", groups, schemaList, new ArrayList<>());
        List<String> expected = new ArrayList<>();
        expected.add("testBranch");
        assertEquals(1, res.size());
        assertThat(res, is(expected));

        res = autocompletionAgent.getSchemaMetadataList("tesSome", schemaList, new ArrayList<>());
        assertEquals(0, res.size());
    }
}