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

import org.apache.ranger.plugin.service.ResourceLookupContext;
import org.apache.ranger.plugin.util.TimedEventUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class SchemaRegistryResourceMgr {

    private static final Logger LOG = LoggerFactory.getLogger(SchemaRegistryResourceMgr.class);

    private static final String REGISTRY_SERVICE = "registry-service";
    private static final String SERDE = "serde";
    private static final String SCHEMA_GROUP = "schema-group";
    private static final String SCHEMA_METADATA = "schema-metadata";
    private static final String SCHEMA_BRANCH = "schema-branch";
    private static final String SCHEMA_VERSION = "schema-version";

    private static final List<String> asteriskList = Collections.singletonList("*");

    private static final int LOOKUP_TIMEOUT_SEC = 5;


    public static List<String> getSchemaRegistryResources(String serviceName,
                                                          Map<String, String> configs,
                                                          ResourceLookupContext context,
                                                          AutocompletionAgent registryClient) throws Exception {

        String userInput = context.getUserInput();
        String resource = context.getResourceName();
        Map<String, List<String>> resourceMap = context.getResources();
        List<String> resultList = null;

        if (LOG.isDebugEnabled()) {
            LOG.debug("==> SchemaRegistryResourceMgr.getSchemaRegistryResources()  UserInput: \"" + userInput + "\" resource : " + resource + " resourceMap: " + resourceMap);
        }

        if (userInput != null
                && !userInput.isEmpty()
                && serviceName != null
                && resource != null
                && resourceMap != null
                && !resourceMap.isEmpty()) {
            if (registryClient != null) {
                Callable<List<String>> callableObj = null;
                try {
                    switch (resource.trim().toLowerCase()) {
                        case SCHEMA_GROUP: {
                            List<String> schemaGroupList = resourceMap.get(SCHEMA_GROUP);
                            // get the SchemaGroupList for given Input
                            final String finalSchemaGroupName = userInput;
                            callableObj = ()
                                    -> registryClient.getSchemaGroupList(finalSchemaGroupName, schemaGroupList);
                            break;
                        }
                        case SCHEMA_METADATA: {
                            List<String> schemaGroupList = resourceMap.get(SCHEMA_GROUP);
                            List<String> schemaMeatadataList = resourceMap.get(SCHEMA_METADATA);
                            // get the SchemaMetadataList for the given Input
                            final String finalSchemaName = userInput;
                            callableObj = ()
                                    -> registryClient.getSchemaMetadataList(finalSchemaName,
                                    schemaGroupList,
                                    schemaMeatadataList);
                            break;
                        }
                        case SCHEMA_BRANCH: {
                            List<String> schemaGroupList = resourceMap.get(SCHEMA_GROUP);
                            List<String> schemaMeatadataList = resourceMap.get(SCHEMA_METADATA);
                            List<String> branchList = resourceMap.get(SCHEMA_BRANCH);
                            // get the SchemaBranchList for given Input
                            final String finalBranchName = userInput;
                            callableObj = ()
                                    -> registryClient.getBranchList(finalBranchName,
                                    schemaGroupList,
                                    schemaMeatadataList,
                                    branchList);
                            break;
                        }
                        case SCHEMA_VERSION: case SERDE: case REGISTRY_SERVICE: {
                            return asteriskList;
                        }
                        default:
                            break;
                    }
                } catch (Exception e) {
                    LOG.error("Unable to get Schema Registry resources.", e);
                    throw e;
                }
                if (callableObj != null) {
                    synchronized (registryClient) {
                        resultList = TimedEventUtil.timedTask(callableObj, LOOKUP_TIMEOUT_SEC,
                                TimeUnit.SECONDS);
                    }
                } else {
                    LOG.error("Could not initiate at timedTask");
                }
            }
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== SchemaRegistryResourceMgr.getSchemaRegistryResources() UserInput: "
                    + userInput
                    + " configs: " + configs
                    + "Result :" + resultList );

        }

        return resultList;
    }
}
