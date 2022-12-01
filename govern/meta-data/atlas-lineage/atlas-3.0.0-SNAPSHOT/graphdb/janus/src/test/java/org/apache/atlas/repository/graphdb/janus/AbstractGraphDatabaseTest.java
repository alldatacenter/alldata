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

import org.apache.atlas.graph.GraphSandboxUtil;
import org.apache.atlas.repository.Constants;
import org.apache.atlas.repository.graphdb.AtlasCardinality;
import org.apache.atlas.repository.graphdb.AtlasGraph;
import org.apache.atlas.repository.graphdb.AtlasGraphManagement;
import org.apache.atlas.repository.graphdb.AtlasPropertyKey;
import org.apache.atlas.repository.graphdb.AtlasVertex;
import org.apache.atlas.runner.LocalSolrRunner;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.apache.atlas.graph.GraphSandboxUtil.useLocalSolr;

public abstract class AbstractGraphDatabaseTest {

    protected static final String WEIGHT_PROPERTY = "weight";
    protected static final String TRAIT_NAMES = Constants.TRAIT_NAMES_PROPERTY_KEY;
    protected static final String TYPE_PROPERTY_NAME = "__type";
    protected static final String TYPESYSTEM = "TYPESYSTEM";

    private static final String BACKING_INDEX_NAME = "backing";

    private AtlasGraph<?, ?> graph = null;

    @BeforeClass
    public static void createIndices() throws Exception {
        GraphSandboxUtil.create();

        if (useLocalSolr()) {
            LocalSolrRunner.start();
        }

        AtlasJanusGraphDatabase db = new AtlasJanusGraphDatabase();
        AtlasGraphManagement mgmt = db.getGraph().getManagementSystem();

        if (mgmt.getGraphIndex(BACKING_INDEX_NAME) == null) {
            mgmt.createVertexMixedIndex(BACKING_INDEX_NAME, Constants.BACKING_INDEX, Collections.emptyList());
        }
        mgmt.makePropertyKey("age13", Integer.class, AtlasCardinality.SINGLE);

        createIndices(mgmt, "name", String.class, false, AtlasCardinality.SINGLE);
        createIndices(mgmt, WEIGHT_PROPERTY, Integer.class, false, AtlasCardinality.SINGLE);
        createIndices(mgmt, "size15", String.class, false, AtlasCardinality.SINGLE);
        createIndices(mgmt, "typeName", String.class, false, AtlasCardinality.SINGLE);
        createIndices(mgmt, "__type", String.class, false, AtlasCardinality.SINGLE);
        createIndices(mgmt, Constants.GUID_PROPERTY_KEY, String.class, true, AtlasCardinality.SINGLE);
        createIndices(mgmt, Constants.TRAIT_NAMES_PROPERTY_KEY, String.class, false, AtlasCardinality.SET);
        createIndices(mgmt, Constants.SUPER_TYPES_PROPERTY_KEY, String.class, false, AtlasCardinality.SET);
        mgmt.commit();
    }

    @AfterMethod
    public void commitGraph() {
        //force any pending actions to be committed so we can be sure they don't cause errors.
        pushChangesAndFlushCache();
        getGraph().commit();
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        AtlasJanusGraph graph = new AtlasJanusGraph();
        graph.clear();

        if (useLocalSolr()) {
            LocalSolrRunner.stop();
        }
    }

    protected <V, E> void pushChangesAndFlushCache() {
        getGraph().commit();
    }

    private static void createIndices(AtlasGraphManagement management, String propertyName, Class propertyClass,
            boolean isUnique, AtlasCardinality cardinality) {

        if (management.containsPropertyKey(propertyName)) {
            //index was already created
            return;
        }

        AtlasPropertyKey key = management.makePropertyKey(propertyName, propertyClass, cardinality);
        try {
            if (propertyClass != Integer.class) {
                management.addMixedIndex(BACKING_INDEX_NAME, key, false);
            }
        } catch(Throwable t) {
            //ok
            t.printStackTrace();
        }
        try {
            management.createVertexCompositeIndex(propertyName, isUnique, Collections.singletonList(key));

        } catch(Throwable t) {
            //ok
            t.printStackTrace();
        }
    }

    protected final <V, E> AtlasGraph<V, E> getGraph() {
        if (graph == null) {
            graph = new AtlasJanusGraph();
        }
        return (AtlasGraph<V, E>)graph;
    }

    protected AtlasJanusGraph getAtlasJanusGraph() {
        AtlasGraph g = getGraph();
        return (AtlasJanusGraph)g;
    }


    protected List<AtlasVertex> newVertices = new ArrayList<>();

    protected final <V, E> AtlasVertex<V, E> createVertex(AtlasGraph<V, E> theGraph) {
        AtlasVertex<V, E> vertex = theGraph.addVertex();
        newVertices.add(vertex);
        return vertex;
    }

    @AfterMethod
    public void removeVertices() {
        for(AtlasVertex vertex : newVertices) {
            if (vertex.exists()) {
                getGraph().removeVertex(vertex);
            }
        }
        getGraph().commit();
        newVertices.clear();
    }

    protected void runSynchronouslyInNewThread(final Runnable r) throws Throwable {
        RunnableWrapper wrapper = new RunnableWrapper(r);
        Thread th = new Thread(wrapper);
        th.start();
        th.join();
        Throwable ex = wrapper.getExceptionThrown();
        if (ex != null) {
            throw ex;
        }
    }

    private static final class RunnableWrapper implements Runnable {
        private final Runnable r;
        private Throwable exceptionThrown = null;

        private RunnableWrapper(Runnable r) {
            this.r = r;
        }

        @Override
        public void run() {
            try {
                r.run();
            } catch(Throwable e) {
                exceptionThrown = e;
            }

        }

        public Throwable getExceptionThrown() {
            return exceptionThrown;
        }
    }
}
