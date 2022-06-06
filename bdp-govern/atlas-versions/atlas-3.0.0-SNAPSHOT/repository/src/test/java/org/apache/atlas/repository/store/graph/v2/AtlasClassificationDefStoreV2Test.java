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
package org.apache.atlas.repository.store.graph.v2;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import com.google.inject.Inject;
import org.apache.atlas.AtlasErrorCode;
import org.apache.atlas.TestModules;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.repository.Constants;
import org.apache.atlas.repository.graphdb.AtlasEdge;
import org.apache.atlas.repository.graphdb.AtlasEdgeDirection;
import org.apache.atlas.repository.graphdb.AtlasVertex;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import java.util.ArrayList;

/**
 * Tests for AtlasClassificationDefStoreV2
 */
@Guice(modules = TestModules.TestOnlyModule.class)
public class AtlasClassificationDefStoreV2Test {
  private AtlasClassificationDefStoreV2 classificationDefStore;

  @Inject
  AtlasTypeRegistry atlasTypeRegistry;

  @Inject
  AtlasTypeDefGraphStoreV2 typeDefStore;

  @BeforeClass
  public void setUp() {
    classificationDefStore = new AtlasClassificationDefStoreV2(typeDefStore, atlasTypeRegistry);
  }

  @DataProvider
  public Object[][] traitRegexString() {
    // Test unicode regex for classification
    return new Object[][] {
      {"test1", true},
      {"\u597D", true},
      {"\u597D\u597D", true},
      {"\u597D \u597D", true},
      {"\u597D_\u597D", true},
      {"\u597D.\u597D", true},
      {"class1.attr1", true},
      {"1test", false},
      {"_test1", false},
    };
  }

  @Test(dataProvider = "traitRegexString")
  public void testIsValidName(String data, boolean expected) {
    assertEquals(classificationDefStore.isValidName(data), expected);
  }

  @Test
  public void testDeleteReferencedTraitFail() {
    AtlasVertex typeVertex = mock(AtlasVertex.class);
    when(typeVertex.getProperty(Constants.TYPENAME_PROPERTY_KEY, String.class)).thenReturn("Tag11");
    when(typeVertex.getEdges(AtlasEdgeDirection.IN)).thenReturn(() -> {
      ArrayList<AtlasEdge> list = new ArrayList<>();
      list.add(mock(AtlasEdge.class));
      return list.iterator();
    });
    try {
      classificationDefStore.deleteByName("Tag11", typeVertex );
    } catch (AtlasBaseException abe) {
      assertEquals(abe.getMessage(), AtlasErrorCode.TYPE_HAS_REFERENCES.getFormattedErrorMessage("Tag11"));
      assertEquals(abe.getAtlasErrorCode() , AtlasErrorCode.TYPE_HAS_REFERENCES);
    }
  }
}