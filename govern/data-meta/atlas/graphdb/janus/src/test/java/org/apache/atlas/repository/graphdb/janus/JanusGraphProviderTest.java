/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.repository.graphdb.janus;

import org.apache.atlas.ApplicationProperties;
import org.apache.atlas.AtlasException;
import org.apache.atlas.graph.GraphSandboxUtil;
import org.apache.atlas.repository.graphdb.AtlasGraph;
import org.apache.atlas.runner.LocalSolrRunner;
import org.apache.commons.configuration.Configuration;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import static org.apache.atlas.graph.GraphSandboxUtil.useLocalSolr;


@Test
public class JanusGraphProviderTest {

    private Configuration configuration;
    private AtlasGraph<?, ?> graph;

    @BeforeTest
    public void setUp() throws Exception {
        GraphSandboxUtil.create();

        if (useLocalSolr()) {
            LocalSolrRunner.start();
        }

        //First get Instance
        graph         = new AtlasJanusGraph();
        configuration = ApplicationProperties.getSubsetConfiguration(ApplicationProperties.get(), AtlasJanusGraphDatabase.GRAPH_PREFIX);
    }

    @AfterClass
    public void tearDown() throws Exception {
        try {
            graph.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            graph.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (useLocalSolr()) {
            LocalSolrRunner.stop();
        }
    }

    @Test
    public void testValidate() throws AtlasException {
        try {
            AtlasJanusGraphDatabase.validateIndexBackend(configuration);
        } catch (Exception e) {
            Assert.fail("Unexpected exception ", e);
        }

        //Change backend
        configuration.setProperty(AtlasJanusGraphDatabase.INDEX_BACKEND_CONF, AtlasJanusGraphDatabase.INDEX_BACKEND_LUCENE);
        try {
            AtlasJanusGraphDatabase.validateIndexBackend(configuration);
            Assert.fail("Expected exception");
        } catch (Exception e) {
            Assert.assertEquals(e.getMessage(),
                    "Configured Index Backend lucene differs from earlier configured "
                    + "Index Backend solr. Aborting!");
        }
    }
}
