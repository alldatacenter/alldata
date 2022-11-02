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

package org.apache.atlas.repository.graphdb.janus.migration;

import org.apache.commons.io.FileUtils;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONMapper;
import org.apache.tinkerpop.gremlin.structure.io.graphson.TypeInfo;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.apache.tinkerpop.shaded.jackson.databind.JsonNode;
import org.apache.tinkerpop.shaded.jackson.databind.ObjectMapper;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.AssertJUnit.assertTrue;

public class BaseUtils {
    private static final String resourcesDirRelativePath = "/src/test/resources/";
    private String resourceDir;

    protected final ElementProcessors emptyRelationshipCache = new ElementProcessors(new HashMap<>(), new HashMap<>(), new HashMap<>());
    protected GraphSONUtility graphSONUtility;

    protected JsonNode getJsonNodeFromFile(String s) {
        File f = new File(getFilePath(s));
        try {
            return getEntityNode(FileUtils.readFileToString(f));
        } catch (IOException e) {
            throw new SkipException("getJsonNodeFromFile: " + s, e);
        }
    }

    protected String getFilePath(String fileName) {
        return Paths.get(resourceDir, fileName).toString();
    }

    @BeforeClass
    public void setup() {
        resourceDir = System.getProperty("user.dir") + resourcesDirRelativePath;
        graphSONUtility = new GraphSONUtility(emptyRelationshipCache);
    }

    protected Object getId(JsonNode node) {
        GraphSONUtility gu = graphSONUtility;
        return gu.getTypedValueFromJsonNode(node.get(GraphSONTokensTP2._ID));
    }


    private JsonNode getEntityNode(String json) throws IOException {
        GraphSONMapper.Builder builder = GraphSONMapper.build();
        final ObjectMapper mapper  = builder.create().createMapper();
        return mapper.readTree(json);
    }

    protected void addVertex(TinkerGraph tg, JsonNode node) {
        GraphSONUtility utility = new GraphSONUtility(emptyRelationshipCache);
        utility.vertexFromJson(tg, node);
    }

    protected void addEdge(TinkerGraph tg, MappedElementCache cache) {
        GraphSONUtility gu = graphSONUtility;

        addVertexToGraph(tg, gu, getDBV(), getTableV(), getCol1(), getCol2());
        addEdgeToGraph(tg, gu, cache, getEdge(), getEdgeCol(), getEdgeCol2());
    }

    protected void addEdgesForMap(TinkerGraph tg, MappedElementCache cache) {
        GraphSONUtility gu = graphSONUtility;

        addVertexToGraph(tg, gu, getDBV(), getTableV(), getCol1(), getCol2());
        addEdgeToGraph(tg, gu, cache, getEdgeCol3(), getEdgeCol4());
    }

    protected Vertex fetchTableVertex(TinkerGraph tg) {
        GraphTraversal query = tg.traversal().V().has("__typeName", "hive_table");
        assertTrue(query.hasNext());

        return (Vertex) query.next();
    }

    protected Map<String, Map<String, List<String>>> getTypePropertyMap(String type, String property, String category) {
        Map<String, Map<String, List<String>>> map = new HashMap<>();
        map.put(type, new HashMap<>());
        map.get(type).put(category, new ArrayList<>());
        map.get(type).get(category).add(property);
        return map;
    }

    protected void addVertexToGraph(TinkerGraph tg, GraphSONUtility gu, JsonNode... nodes) {
        for(JsonNode n : nodes) {
            gu.vertexFromJson(tg, n);
        }
    }

    protected void addEdgeToGraph(TinkerGraph tg, GraphSONUtility gu, MappedElementCache cache, JsonNode... nodes) {

        for(JsonNode n : nodes) {
            gu.edgeFromJson(tg, cache, n);
        }
    }

    public JsonNode getCol1() {
        return getJsonNodeFromFile("col-legacy.json");
    }

    public JsonNode getCol2() {
        return getJsonNodeFromFile("col-2-legacy.json");
    }

    public JsonNode getCol3() {
        return getJsonNodeFromFile("col-3-legacy.json");
    }

    public JsonNode getDbType() {
        return getJsonNodeFromFile("db-type-legacy.json");
    }

    public JsonNode getEdge() {
        return getJsonNodeFromFile("edge-legacy.json");
    }

    public JsonNode getEdgeCol() {
        return getJsonNodeFromFile("edge-legacy-col.json");
    }

    public JsonNode getEdgeCol2() {
        return getJsonNodeFromFile("edge-legacy-col2.json");
    }

    public JsonNode getEdgeCol3() {
        return getJsonNodeFromFile("edge-legacy-col3.json");
    }

    public JsonNode getEdgeCol4() {
        return getJsonNodeFromFile("edge-legacy-col4.json");
    }

    public JsonNode getEdgeTag() {
        return getJsonNodeFromFile("edge-legacy-tag.json");
    }

    public JsonNode getDBV() {
        return getJsonNodeFromFile("db-v-65544.json");
    }

    public JsonNode getTableV() {
        return getJsonNodeFromFile("table-v-147504.json");
    }

    public JsonNode getTagV() {
        return getJsonNodeFromFile("tag-163856752.json");
    }

    public JsonNode getProcessV() {
        return getJsonNodeFromFile("lineage-v-98312.json");
    }

    public JsonNode getEdgeProcess() {
        return getJsonNodeFromFile("edge-legacy-process.json");
    }
}
