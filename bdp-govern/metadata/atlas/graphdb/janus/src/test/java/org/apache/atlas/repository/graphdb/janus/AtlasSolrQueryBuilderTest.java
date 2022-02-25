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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.discovery.SearchParameters;
import org.apache.atlas.model.typedef.AtlasBaseTypeDef;
import org.apache.atlas.model.typedef.AtlasStructDef;
import org.apache.atlas.model.typedef.AtlasTypesDef;
import org.apache.atlas.repository.Constants;
import org.apache.atlas.type.AtlasEntityType;
import org.apache.atlas.type.AtlasStructType;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.when;


public class AtlasSolrQueryBuilderTest {

    @Mock
    private AtlasEntityType hiveTableEntityTypeMock;

    @Mock
    private AtlasEntityType hiveTableEntityTypeMock2;

    @Mock
    private AtlasStructType.AtlasAttribute nameAttributeMock;

    @Mock
    private AtlasStructType.AtlasAttribute commentAttributeMock;

    @Mock
    private AtlasStructType.AtlasAttribute stateAttributeMock;

    @Mock
    private AtlasStructType.AtlasAttribute descrptionAttributeMock;

    @Mock
    private AtlasStructType.AtlasAttribute createdAttributeMock;

    @Mock
    private AtlasStructType.AtlasAttribute startedAttributeMock;

    @Mock
    private AtlasStructType.AtlasAttribute entitypeAttributeMock;

    @Mock
    private AtlasStructType.AtlasAttribute qualifiedNameAttributeMock;

    @Mock
    private AtlasStructDef.AtlasAttributeDef stringAttributeDef;

    @Mock
    private AtlasStructDef.AtlasAttributeDef textAttributeDef;

    @Mock
    private AtlasStructDef.AtlasAttributeDef nonStringAttributeDef;

    private Map<String, String> indexFieldNamesMap = new HashMap<>();


    @BeforeTest
    public void setup() {
        AtlasTypesDef typesDef = new AtlasTypesDef();
        MockitoAnnotations.initMocks(this);
        when(hiveTableEntityTypeMock.getAttribute("name")).thenReturn(nameAttributeMock);
        when(hiveTableEntityTypeMock.getAttribute("comment")).thenReturn(commentAttributeMock);
        when(hiveTableEntityTypeMock.getAttribute("__state")).thenReturn(stateAttributeMock);
        when(hiveTableEntityTypeMock.getAttribute("description")).thenReturn(descrptionAttributeMock);
        when(hiveTableEntityTypeMock.getAttribute("created")).thenReturn(createdAttributeMock);
        when(hiveTableEntityTypeMock.getAttribute("started")).thenReturn(startedAttributeMock);
        when(hiveTableEntityTypeMock.getAttribute("Constants.ENTITY_TYPE_PROPERTY_KEY")).thenReturn(entitypeAttributeMock);
        when(hiveTableEntityTypeMock.getAttribute("qualifiedName")).thenReturn(qualifiedNameAttributeMock);

        when(hiveTableEntityTypeMock.getAttributeDef("name")).thenReturn(stringAttributeDef);
        when(hiveTableEntityTypeMock.getAttributeDef("comment")).thenReturn(stringAttributeDef);
        when(hiveTableEntityTypeMock.getAttributeDef("description")).thenReturn(stringAttributeDef);
        when(hiveTableEntityTypeMock.getAttributeDef("qualifiedName")).thenReturn(textAttributeDef);

        when(nonStringAttributeDef.getTypeName()).thenReturn(AtlasBaseTypeDef.ATLAS_TYPE_INT);
        when(stringAttributeDef.getTypeName()).thenReturn(AtlasBaseTypeDef.ATLAS_TYPE_STRING);
        when(textAttributeDef.getTypeName()).thenReturn(AtlasBaseTypeDef.ATLAS_TYPE_STRING);

        when(stringAttributeDef.getIndexType()).thenReturn(AtlasStructDef.AtlasAttributeDef.IndexType.STRING);

        indexFieldNamesMap.put("name", "name_index");
        indexFieldNamesMap.put("comment", "comment_index");
        indexFieldNamesMap.put("__state", "__state_index");
        indexFieldNamesMap.put("description", "descrption__index");
        indexFieldNamesMap.put("created", "created__index");
        indexFieldNamesMap.put("started", "started__index");
        indexFieldNamesMap.put(Constants.ENTITY_TYPE_PROPERTY_KEY, Constants.ENTITY_TYPE_PROPERTY_KEY + "__index");


        when(hiveTableEntityTypeMock.getTypeName()).thenReturn("hive_table");
        when(hiveTableEntityTypeMock2.getTypeName()).thenReturn("hive_db");

        when(nameAttributeMock.getIndexFieldName()).thenReturn("name_index");
        when(commentAttributeMock.getIndexFieldName()).thenReturn("comment_index");
        when(stateAttributeMock.getIndexFieldName()).thenReturn("__state_index");
        when(descrptionAttributeMock.getIndexFieldName()).thenReturn("descrption__index");
        when(createdAttributeMock.getIndexFieldName()).thenReturn("created__index");
        when(startedAttributeMock.getIndexFieldName()).thenReturn("started__index");
        when(entitypeAttributeMock.getIndexFieldName()).thenReturn(Constants.ENTITY_TYPE_PROPERTY_KEY + "__index");
        when(qualifiedNameAttributeMock.getIndexFieldName()).thenReturn("qualifiedName" + "__index");

    }

    @Test
    public void testGenerateSolrQueryString() throws IOException, AtlasBaseException {
        final String fileName = "src/test/resources/searchparameters2OR.json";
        AtlasSolrQueryBuilder underTest = new AtlasSolrQueryBuilder();

        processSearchParameters(fileName, underTest);

        Assert.assertEquals(underTest.build(), "+t  AND  -__state_index:DELETED AND  +__typeName__index:(hive_table )  AND  ( ( +name_index:t10  ) OR ( +comment_index:*t10*  ) )");
    }

    @Test
    public void testGenerateSolrQueryString2() throws IOException, AtlasBaseException {
        final String fileName = "src/test/resources/searchparameters1OR.json";
        AtlasSolrQueryBuilder underTest = new AtlasSolrQueryBuilder();

        processSearchParameters(fileName, underTest);

        Assert.assertEquals(underTest.build(), "+t  AND  -__state_index:DELETED AND  +__typeName__index:(hive_table )  AND  ( ( +name_index:t10  ) )");
    }

    @Test
    public void testGenerateSolrQueryString3() throws IOException, AtlasBaseException {
        final String fileName = "src/test/resources/searchparameters2AND.json";
        AtlasSolrQueryBuilder underTest = new AtlasSolrQueryBuilder();

        processSearchParameters(fileName, underTest);

        Assert.assertEquals(underTest.build(), "+t  AND  -__state_index:DELETED AND  +__typeName__index:(hive_table )  AND  ( ( +name_index:t10  ) AND ( +comment_index:*t10*  ) )");
    }

    @Test
    public void testGenerateSolrQueryString4() throws IOException, AtlasBaseException {
        final String fileName = "src/test/resources/searchparameters1AND.json";
        AtlasSolrQueryBuilder underTest = new AtlasSolrQueryBuilder();

        processSearchParameters(fileName, underTest);

        Assert.assertEquals(underTest.build(), "+t  AND  -__state_index:DELETED AND  +__typeName__index:(hive_table )  AND  ( ( +name_index:t10  ) )");
    }

    @Test
    public void testGenerateSolrQueryString5() throws IOException, AtlasBaseException {
        final String fileName = "src/test/resources/searchparameters0.json";
        AtlasSolrQueryBuilder underTest = new AtlasSolrQueryBuilder();

        processSearchParameters(fileName, underTest);

        Assert.assertEquals(underTest.build(), "+t  AND  -__state_index:DELETED AND  +__typeName__index:(hive_table )  AND  ( +name_index:t10  )");
    }

    @Test
    public void testGenerateSolrQueryString6() throws IOException, AtlasBaseException {
        final String fileName = "src/test/resources/searchparameters3.json";
        AtlasSolrQueryBuilder underTest = new AtlasSolrQueryBuilder();

        processSearchParameters(fileName, underTest);

        Assert.assertEquals(underTest.build(), "+t10  AND  -__state_index:DELETED AND  +__typeName__index:(hive_table )  AND  ( ( +comment_index:*United\\ States*  ) AND ( +descrption__index:*nothing*  ) AND ( +name_index:*t100*  ) )");
    }

    @Test
    public void testGenerateSolrQueryStringGT() throws IOException, AtlasBaseException {
        final String fileName = "src/test/resources/searchparametersGT.json";
        AtlasSolrQueryBuilder underTest = new AtlasSolrQueryBuilder();

        processSearchParameters(fileName, underTest);

        Assert.assertEquals(underTest.build(), "+t10  AND  -__state_index:DELETED AND  +__typeName__index:(hive_table )  AND  ( ( +created__index:{ 100 TO * ]  ) )");
    }

    @Test
    public void testGenerateSolrQueryStringGTE() throws IOException, AtlasBaseException {
        final String fileName = "src/test/resources/searchparametersGTE.json";
        AtlasSolrQueryBuilder underTest = new AtlasSolrQueryBuilder();

        processSearchParameters(fileName, underTest);

        Assert.assertEquals(underTest.build(), "+t10  AND  -__state_index:DELETED AND  +__typeName__index:(hive_table )  AND  ( ( +created__index:[ 100 TO * ]  ) AND ( +started__index:[ 100 TO * ]  ) )");
    }

    @Test
    public void testGenerateSolrQueryStringLT() throws IOException, AtlasBaseException {
        final String fileName = "src/test/resources/searchparametersLT.json";
        AtlasSolrQueryBuilder underTest = new AtlasSolrQueryBuilder();

        processSearchParameters(fileName, underTest);

        Assert.assertEquals(underTest.build(), "+t10  AND  -__state_index:DELETED AND  +__typeName__index:(hive_table )  AND  ( ( +created__index:[ * TO 100}  ) )");
    }

    @Test
    public void testGenerateSolrQueryStringLE() throws IOException, AtlasBaseException {
        final String fileName = "src/test/resources/searchparametersLTE.json";
        AtlasSolrQueryBuilder underTest = new AtlasSolrQueryBuilder();

        processSearchParameters(fileName, underTest);

        Assert.assertEquals(underTest.build(), "+t10  AND  -__state_index:DELETED AND  +__typeName__index:(hive_table )  AND  ( ( +created__index:[ * TO 100 ]  ) AND ( +started__index:[ * TO 100 ]  ) )");
    }

    @Test
    public void testGenerateSolrQueryStartsWith() throws IOException, AtlasBaseException {
        final String fileName = "src/test/resources/searchparametersStartsWith.json";
        AtlasSolrQueryBuilder underTest = new AtlasSolrQueryBuilder();

        processSearchParameters(fileName, underTest);

        Assert.assertEquals(underTest.build(), " -__state_index:DELETED AND  +__typeName__index:(hive_table )  AND  ( ( +qualifiedName__index:testdb.t1*  ) )");
    }

    @Test
    public void testGenerateSolrQueryString2TypeNames() throws IOException, AtlasBaseException {
        final String fileName = "src/test/resources/searchparameters2Types.json";
        AtlasSolrQueryBuilder underTest = new AtlasSolrQueryBuilder();

        processSearchParametersForMultipleTypeNames(fileName, underTest);

        Assert.assertEquals(underTest.build(), "+t  AND  -__state_index:DELETED AND  +__typeName__index:(hive_table hive_db ) ");
    }



    private void validateOrder(List<String> topTerms, int ... indices) {
        Assert.assertEquals(topTerms.size(), indices.length);
        int i = 0;
        for(String term: topTerms) {
            Assert.assertEquals(Integer.toString(indices[i++]), term);
        }
        Assert.assertEquals(topTerms.size(), indices.length);
    }

    private Map<String, AtlasJanusGraphIndexClient.TermFreq>  generateTerms(int ... termFreqs) {
        int i =0;
        Map<String, AtlasJanusGraphIndexClient.TermFreq> terms = new HashMap<>();
        for(int count: termFreqs) {
            AtlasJanusGraphIndexClient.TermFreq termFreq1 = new AtlasJanusGraphIndexClient.TermFreq(Integer.toString(i++), count);
            terms.put(termFreq1.getTerm(), termFreq1);
        }
        return terms;
    }

    private void processSearchParameters(String fileName, AtlasSolrQueryBuilder underTest) throws IOException, AtlasBaseException {
        ObjectMapper mapper = new ObjectMapper();
        SearchParameters searchParameters =  mapper.readValue(new FileInputStream(fileName), SearchParameters.class);

        Set<AtlasEntityType> hiveTableEntityTypeMocks = new HashSet<>();
        hiveTableEntityTypeMocks.add(hiveTableEntityTypeMock);
        underTest.withEntityTypes(hiveTableEntityTypeMocks)
                .withQueryString(searchParameters.getQuery())
                .withCriteria(searchParameters.getEntityFilters())
                .withExcludedDeletedEntities(searchParameters.getExcludeDeletedEntities())
                .withCommonIndexFieldNames(indexFieldNamesMap);
    }

    private void processSearchParametersForMultipleTypeNames(String fileName, AtlasSolrQueryBuilder underTest) throws IOException, AtlasBaseException {
        ObjectMapper mapper = new ObjectMapper();
        SearchParameters searchParameters =  mapper.readValue(new FileInputStream(fileName), SearchParameters.class);

        Set<AtlasEntityType> hiveTableEntityTypeMocks = new HashSet<>();
        hiveTableEntityTypeMocks.add(hiveTableEntityTypeMock);
        hiveTableEntityTypeMocks.add(hiveTableEntityTypeMock2);
        underTest.withEntityTypes(hiveTableEntityTypeMocks)
                .withQueryString(searchParameters.getQuery())
                .withCriteria(searchParameters.getEntityFilters())
                .withExcludedDeletedEntities(searchParameters.getExcludeDeletedEntities())
                .withCommonIndexFieldNames(indexFieldNamesMap);
    }

}