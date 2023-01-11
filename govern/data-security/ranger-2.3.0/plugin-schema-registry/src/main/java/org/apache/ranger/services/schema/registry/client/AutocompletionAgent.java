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

import org.apache.ranger.plugin.client.BaseClient;
import org.apache.ranger.services.schema.registry.client.connection.DefaultSchemaRegistryClient;
import org.apache.ranger.services.schema.registry.client.connection.ISchemaRegistryClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The class that is used to get needed information for auto completion feature.
 */
public class AutocompletionAgent {
    private static final Logger LOG = LoggerFactory.getLogger(AutocompletionAgent.class);

    private ISchemaRegistryClient client;
    private String serviceName;

    private static final String errMessage = "You can still save the repository and start creating "
            + "policies, but you would not be able to use autocomplete for "
            + "resource names. Check server logs for more info.";

    private static final String successMsg = "ConnectionTest Successful";


    public AutocompletionAgent(String serviceName, Map<String, String> configs) {
        this(serviceName, new DefaultSchemaRegistryClient(configs));
    }

    public AutocompletionAgent(String serviceName, ISchemaRegistryClient client) {
        this.serviceName = serviceName;
        this.client = client;
    }

    public HashMap<String, Object> connectionTest() {
        HashMap<String, Object> responseData = new HashMap<String, Object>();

        try {
            client.checkConnection();
            // If it doesn't throw exception, then assume the instance is
            // reachable
            BaseClient.generateResponseDataMap(true, successMsg,
                    successMsg, null, null, responseData);
            if(LOG.isDebugEnabled()) {
                LOG.debug("ConnectionTest Successful.");
            }
        } catch (Exception e) {
            LOG.error("Error connecting to SchemaRegistry. schemaRegistryClient=" + this, e);
            BaseClient.generateResponseDataMap(false, errMessage,
                    errMessage, null, null, responseData);
        }

        return responseData;
    }

    public List<String> getSchemaGroupList(String lookupGroupName, List<String> groupList) {
        List<String> res = groupList;
        Collection<String> schemaGroups = client.getSchemaGroups();
        schemaGroups.forEach(gName -> {
            if (!res.contains(gName) && gName.contains(lookupGroupName)) {
                res.add(gName);
            }
        });

        return res;
    }

    public List<String> getSchemaMetadataList(String lookupSchemaMetadataName,
                                              List<String> schemaGroupList,
                                              List<String> schemaMetadataList) {
        List<String> res = schemaMetadataList;

        Collection<String> schemas = client.getSchemaNames(schemaGroupList);
        schemas.forEach(sName -> {
            if (!res.contains(sName) && sName.contains(lookupSchemaMetadataName)) {
                res.add(sName);
            }
        });

        return res;
    }

    public List<String> getBranchList(String lookupBranchName,
                                      List<String> groupList,
                                      List<String> schemaList,
                                      List<String> branchList) {
        List<String> res = branchList;
        List<String> expandedSchemaList = schemaList.stream().flatMap(
                schemaName -> expandSchemaMetadataNameRegex(groupList, schemaName).stream())
                .collect(Collectors.toList());
        expandedSchemaList.forEach(schemaMetadataName -> {
            Collection<String> branches = client.getSchemaBranches(schemaMetadataName);
            branches.forEach(bName -> {
                if (!res.contains(bName) && bName.contains(lookupBranchName)) {
                    res.add(bName);
                }
            });
        });

        return res;
    }

    List<String> expandSchemaMetadataNameRegex(List<String> schemaGroupList, String lookupSchemaMetadataName) {
        List<String> res = new ArrayList<>();

        Collection<String> schemas = client.getSchemaNames(schemaGroupList);
        schemas.forEach(sName -> {
            if (sName.matches(lookupSchemaMetadataName)) {
                res.add(sName);
            }
        });

        return res;
    }

    @Override
    public String toString() {
        return "AutocompletionAgent [serviceName=" + serviceName + "]";
    }

}
