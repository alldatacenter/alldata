/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.tools;

import org.apache.atlas.ApplicationProperties;
import org.apache.atlas.AtlasClientV2;
import org.apache.atlas.AtlasException;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntityWithExtInfo;
import org.apache.atlas.repository.graphdb.AtlasVertex;
import org.apache.atlas.repository.graphdb.janus.AtlasJanusGraphDatabase;
import org.apache.atlas.repository.store.graph.v2.AtlasGraphUtilsV2;
import org.apache.atlas.utils.AuthenticationUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.configuration.Configuration;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.schema.JanusGraphIndex;
import org.janusgraph.core.schema.SchemaAction;
import org.janusgraph.core.schema.SchemaStatus;
import org.janusgraph.diskstorage.BackendTransaction;
import org.janusgraph.diskstorage.indexing.IndexEntry;
import org.janusgraph.graphdb.database.IndexSerializer;
import org.janusgraph.graphdb.database.StandardJanusGraph;
import org.janusgraph.graphdb.database.management.ManagementSystem;
import org.janusgraph.graphdb.transaction.StandardJanusGraphTx;
import org.janusgraph.graphdb.types.MixedIndexType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class RepairIndex {
    private static final Logger LOG = LoggerFactory.getLogger(RepairIndex.class);

    private static final int EXIT_CODE_SUCCESS = 0;
    private static final int EXIT_CODE_FAILED = 1;
    private static final int  MAX_TRIES_ON_FAILURE = 3;

    private static final String INDEX_NAME_VERTEX_INDEX = "vertex_index";
    private static final String INDEX_NAME_FULLTEXT_INDEX = "fulltext_index";
    private static final String INDEX_NAME_EDGE_INDEX = "edge_index";
    private static final String DEFAULT_ATLAS_URL = "http://localhost:21000/";
    private static final String APPLICATION_PROPERTY_ATLAS_ENDPOINT = "atlas.rest.address";

    private static JanusGraph graph;
    private static AtlasClientV2 atlasClientV2;
    private static boolean isSelectiveRestore;

    public static void main(String[] args) {
        int exitCode = EXIT_CODE_FAILED;
        LOG.info("Started index repair");

        try {
            CommandLine cmd = getCommandLine(args);
            String guid = cmd.getOptionValue("g");

            if(guid != null && !guid.isEmpty()){
                isSelectiveRestore = true;
                String uid = cmd.getOptionValue("u");
                String pwd = cmd.getOptionValue("p");
                setupAtlasClient(uid, pwd);
            }

            process(guid);

            LOG.info("Completed index repair!");
            exitCode = EXIT_CODE_SUCCESS;
        } catch (Exception e) {
            LOG.error("Failed!", e);
            display("Failed: " + e.getMessage());
        }

        System.exit(exitCode);
    }

    private static void process(String guid) throws Exception {
        RepairIndex repairIndex = new RepairIndex();

        setupGraph();

        if (isSelectiveRestore) {
            repairIndex.restoreSelective(guid);
        }else{
            repairIndex.restoreAll();
        }

        displayCrlf("Repair Index: Done!");
    }

    private static CommandLine getCommandLine(String[] args) throws ParseException {
        Options options = new Options();
        options.addOption("g", "guid", true, "guid for which update index should be executed.");
        options.addOption("u", "user", true, "User name.");
        options.addOption("p", "password", true, "Password name.");

        return new DefaultParser().parse(options, args);
    }

    private static void setupGraph() {
        display("Initializing graph: ");
        graph = AtlasJanusGraphDatabase.getGraphInstance();
        displayCrlf("Graph Initialized!");
    }

    private static String[] getIndexes() {
        return new String[]{ INDEX_NAME_VERTEX_INDEX, INDEX_NAME_EDGE_INDEX, INDEX_NAME_FULLTEXT_INDEX};
    }

    private static void setupAtlasClient(String uid, String pwd) throws AtlasException {
        String[] atlasEndpoint = getAtlasRESTUrl();
        if (atlasEndpoint == null || atlasEndpoint.length == 0) {
            atlasEndpoint = new String[]{DEFAULT_ATLAS_URL};
        }
        atlasClientV2 = getAtlasClientV2(atlasEndpoint, new String[]{uid, pwd});
    }

    private void restoreAll() throws Exception {
        for (String indexName : getIndexes()){
            displayCrlf("Restoring: " + indexName);
            long startTime = System.currentTimeMillis();

            ManagementSystem mgmt = (ManagementSystem) graph.openManagement();
            JanusGraphIndex index = mgmt.getGraphIndex(indexName);
            mgmt.updateIndex(index, SchemaAction.REINDEX).get();
            mgmt.commit();

            ManagementSystem.awaitGraphIndexStatus(graph, indexName).status(SchemaStatus.ENABLED).call();

            display(": Time taken: " + (System.currentTimeMillis() - startTime) + " ms");
            displayCrlf(": Done!");
        }
    }


    private void restoreSelective(String guid) throws Exception  {
        Set<String> referencedGUIDs = new HashSet<>(getEntityAndReferenceGuids(guid));
        displayCrlf("processing referencedGuids => "+ referencedGUIDs);

        StandardJanusGraph janusGraph = (StandardJanusGraph) graph;
        IndexSerializer indexSerializer = janusGraph.getIndexSerializer();

        for (String indexName : getIndexes()){
            displayCrlf("Restoring: " + indexName);
            long startTime = System.currentTimeMillis();
            reindexVertex(indexName, indexSerializer, referencedGUIDs);

            display(": Time taken: " + (System.currentTimeMillis() - startTime) + " ms");
            displayCrlf(": Done!");
        }
    }

    private static void reindexVertex(String indexName, IndexSerializer indexSerializer, Set<String> entityGUIDs) throws Exception {
        Map<String, Map<String, List<IndexEntry>>> documentsPerStore = new java.util.HashMap<>();
        ManagementSystem mgmt = (ManagementSystem) graph.openManagement();
        StandardJanusGraphTx tx = mgmt.getWrappedTx();
        BackendTransaction mutator = tx.getTxHandle();
        JanusGraphIndex index = mgmt.getGraphIndex(indexName);
        MixedIndexType indexType = (MixedIndexType) mgmt.getSchemaVertex(index).asIndexType();

        for (String entityGuid : entityGUIDs){
            for (int attemptCount = 1; attemptCount <= MAX_TRIES_ON_FAILURE; attemptCount++) {
                AtlasVertex vertex = AtlasGraphUtilsV2.findByGuid(entityGuid);
                try {
                    indexSerializer.reindexElement(vertex.getWrappedElement(), indexType, documentsPerStore);
                    break;
                }catch (Exception e){
                    displayCrlf("Exception: " + e.getMessage());
                    displayCrlf("Pausing before retry..");
                    Thread.sleep(2000 * attemptCount);
                }
            }
        }
        mutator.getIndexTransaction(indexType.getBackingIndexName()).restore(documentsPerStore);
    }

    private static Set<String> getEntityAndReferenceGuids(String guid) throws Exception {
        Set<String> set = new HashSet<>();
        set.add(guid);
        AtlasEntityWithExtInfo entity = atlasClientV2.getEntityByGuid(guid);
        Map<String, AtlasEntity> map = entity.getReferredEntities();
        if (map == null || map.isEmpty()) {
            return set;
        }
        set.addAll(map.keySet());
        return set;
    }

    private static void display(String... formatMessage) {
        displayFn(System.out::print, formatMessage);
    }

    private static void displayCrlf(String... formatMessage) {
        displayFn(System.out::println, formatMessage);
    }

    private static void displayFn(Consumer<String> fn, String... formatMessage) {
        if (formatMessage.length == 1) {
            fn.accept(formatMessage[0]);
        } else {
            fn.accept(String.format(formatMessage[0], formatMessage[1]));
        }
    }

    private static String[] getAtlasRESTUrl() {
        Configuration atlasConf = null;
        try {
            atlasConf = ApplicationProperties.get();
            return atlasConf.getStringArray(APPLICATION_PROPERTY_ATLAS_ENDPOINT);
        } catch (AtlasException e) {
            return new String[]{DEFAULT_ATLAS_URL};
        }
    }

    private static AtlasClientV2 getAtlasClientV2(String[] atlasEndpoint, String[] uidPwdFromCommandLine) throws AtlasException {
        AtlasClientV2 atlasClientV2;
        if (!AuthenticationUtil.isKerberosAuthenticationEnabled()) {
            String[] uidPwd = (uidPwdFromCommandLine[0] == null || uidPwdFromCommandLine[1] == null)
                    ? AuthenticationUtil.getBasicAuthenticationInput()
                    : uidPwdFromCommandLine;

            atlasClientV2 = new AtlasClientV2(atlasEndpoint, uidPwd);
        } else {
            atlasClientV2 = new AtlasClientV2(atlasEndpoint);
        }
        return atlasClientV2;
    }
}
