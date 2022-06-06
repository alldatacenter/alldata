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

import com.google.inject.Inject;
import org.apache.atlas.ApplicationProperties;
import org.apache.atlas.AtlasErrorCode;
import org.apache.atlas.AtlasException;
import org.apache.atlas.TestModules;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.typedef.AtlasRelationshipDef;
import org.apache.atlas.model.typedef.AtlasRelationshipEndDef;
import org.apache.atlas.model.typedef.AtlasStructDef;
import org.apache.atlas.repository.AtlasTestBase;
import org.apache.atlas.repository.graph.AtlasGraphProvider;
import org.apache.atlas.type.AtlasTypeUtil;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.fail;

/**
 * Tests for AtlasRelationshipStoreV1
 */
@Guice(modules = TestModules.TestOnlyModule.class)
public class AtlasRelationshipDefStoreV2Test extends AtlasTestBase {

    @Inject
    private
    AtlasRelationshipDefStoreV2 relationshipDefStore;

    @DataProvider
    public Object[][] invalidAttributeNameWithReservedKeywords(){
        AtlasRelationshipDef invalidAttrNameType =
            AtlasTypeUtil.createRelationshipTypeDef("Invalid_Attribute_Type", "description","" ,
                    AtlasRelationshipDef.RelationshipCategory.ASSOCIATION,
                    AtlasRelationshipDef.PropagateTags.BOTH,
                    new AtlasRelationshipEndDef("typeA", "attr1", AtlasStructDef.AtlasAttributeDef.Cardinality.SINGLE),
                    new AtlasRelationshipEndDef("typeB", "attr1", AtlasStructDef.AtlasAttributeDef.Cardinality.SINGLE),

                    AtlasTypeUtil.createRequiredAttrDef("order", "string"),
                    AtlasTypeUtil.createRequiredAttrDef("limit", "string"));

        return new Object[][] {{
            invalidAttrNameType
        }};
    }
    @DataProvider
    public Object[][] updateValidProperties(){
        AtlasRelationshipDef existingType =
                AtlasTypeUtil.createRelationshipTypeDef("basicType", "description","0" ,
                        AtlasRelationshipDef.RelationshipCategory.ASSOCIATION,
                        AtlasRelationshipDef.PropagateTags.ONE_TO_TWO,
                        new AtlasRelationshipEndDef("typeC", "attr1", AtlasStructDef.AtlasAttributeDef.Cardinality.SINGLE),
                        new AtlasRelationshipEndDef("typeD", "attr1", AtlasStructDef.AtlasAttributeDef.Cardinality.SINGLE),

                        AtlasTypeUtil.createRequiredAttrDef("aaaa", "string"),
                        AtlasTypeUtil.createRequiredAttrDef("bbbb", "string"));
        AtlasRelationshipDef newType =
                AtlasTypeUtil.createRelationshipTypeDef("basicType",
                        "description1", // updated
                        "1" , // updated
                        AtlasRelationshipDef.RelationshipCategory.ASSOCIATION,
                        AtlasRelationshipDef.PropagateTags.BOTH, // updated
                        new AtlasRelationshipEndDef("typeC", "attr1", AtlasStructDef.AtlasAttributeDef.Cardinality.SINGLE),
                        new AtlasRelationshipEndDef("typeD", "attr1", AtlasStructDef.AtlasAttributeDef.Cardinality.SINGLE),

                        AtlasTypeUtil.createRequiredAttrDef("aaaa", "string"),
                        AtlasTypeUtil.createRequiredAttrDef("bbbb", "string"));


        return new Object[][] {{
                existingType,
                newType
        }};
    }


    @DataProvider
    public Object[][] updateRename(){
        AtlasRelationshipDef existingType =
                AtlasTypeUtil.createRelationshipTypeDef("basicType", "description","" ,
                        AtlasRelationshipDef.RelationshipCategory.ASSOCIATION,
                        AtlasRelationshipDef.PropagateTags.BOTH,
                        new AtlasRelationshipEndDef("typeC", "attr1", AtlasStructDef.AtlasAttributeDef.Cardinality.SINGLE),
                        new AtlasRelationshipEndDef("typeD", "attr1", AtlasStructDef.AtlasAttributeDef.Cardinality.SINGLE),

                        AtlasTypeUtil.createRequiredAttrDef("aaaa", "string"),
                        AtlasTypeUtil.createRequiredAttrDef("bbbb", "string"));
        AtlasRelationshipDef newType =
                AtlasTypeUtil.createRelationshipTypeDef("basicType2", "description","" ,
                        AtlasRelationshipDef.RelationshipCategory.ASSOCIATION,
                        AtlasRelationshipDef.PropagateTags.BOTH,
                        new AtlasRelationshipEndDef("typeC", "attr1", AtlasStructDef.AtlasAttributeDef.Cardinality.SINGLE),
                        new AtlasRelationshipEndDef("typeD", "attr1", AtlasStructDef.AtlasAttributeDef.Cardinality.SINGLE),

                        AtlasTypeUtil.createRequiredAttrDef("aaaa", "string"),
                        AtlasTypeUtil.createRequiredAttrDef("bbbb", "string"));


        return new Object[][] {{
                existingType,
                newType
        }};
    }
    @DataProvider
    public Object[][] updateRelCat(){
        AtlasRelationshipDef existingType =
                AtlasTypeUtil.createRelationshipTypeDef("basicType", "description","" ,
                        AtlasRelationshipDef.RelationshipCategory.ASSOCIATION,
                        AtlasRelationshipDef.PropagateTags.BOTH,
                        new AtlasRelationshipEndDef("typeC", "attr1", AtlasStructDef.AtlasAttributeDef.Cardinality.SINGLE),
                        new AtlasRelationshipEndDef("typeD", "attr1", AtlasStructDef.AtlasAttributeDef.Cardinality.SINGLE),

                        AtlasTypeUtil.createRequiredAttrDef("aaaa", "string"),
                        AtlasTypeUtil.createRequiredAttrDef("bbbb", "string"));
        AtlasRelationshipDef newType =
                AtlasTypeUtil.createRelationshipTypeDef("basicType", "description","" ,
                        AtlasRelationshipDef.RelationshipCategory.AGGREGATION,
                        AtlasRelationshipDef.PropagateTags.BOTH,
                        new AtlasRelationshipEndDef("typeC", "attr1", AtlasStructDef.AtlasAttributeDef.Cardinality.SINGLE),
                        new AtlasRelationshipEndDef("typeD", "attr1", AtlasStructDef.AtlasAttributeDef.Cardinality.SINGLE),

                        AtlasTypeUtil.createRequiredAttrDef("aaaa", "string"),
                        AtlasTypeUtil.createRequiredAttrDef("bbbb", "string"));


        return new Object[][] {{
                existingType,
                newType
        }};
    }
    @DataProvider
    public Object[][] updateEnd1(){
        AtlasRelationshipDef existingType =
                AtlasTypeUtil.createRelationshipTypeDef("basicType", "description","" ,
                        AtlasRelationshipDef.RelationshipCategory.ASSOCIATION,
                        AtlasRelationshipDef.PropagateTags.BOTH,
                        new AtlasRelationshipEndDef("typeC", "attr1", AtlasStructDef.AtlasAttributeDef.Cardinality.SINGLE),
                        new AtlasRelationshipEndDef("typeD", "attr1", AtlasStructDef.AtlasAttributeDef.Cardinality.SINGLE),

                        AtlasTypeUtil.createRequiredAttrDef("aaaa", "string"),
                        AtlasTypeUtil.createRequiredAttrDef("bbbb", "string"));
        AtlasRelationshipDef changeType =
                AtlasTypeUtil.createRelationshipTypeDef("basicType", "description","" ,
                        AtlasRelationshipDef.RelationshipCategory.ASSOCIATION,
                        AtlasRelationshipDef.PropagateTags.BOTH,
                        new AtlasRelationshipEndDef("typeE", "attr1", AtlasStructDef.AtlasAttributeDef.Cardinality.SINGLE),
                        new AtlasRelationshipEndDef("typeD", "attr1", AtlasStructDef.AtlasAttributeDef.Cardinality.SINGLE),

                        AtlasTypeUtil.createRequiredAttrDef("aaaa", "string"),
                        AtlasTypeUtil.createRequiredAttrDef("bbbb", "string"));
        AtlasRelationshipDef changeAttr =
                AtlasTypeUtil.createRelationshipTypeDef("basicType", "description","" ,
                        AtlasRelationshipDef.RelationshipCategory.ASSOCIATION,
                        AtlasRelationshipDef.PropagateTags.BOTH,
                        new AtlasRelationshipEndDef("typeC", "attr2", AtlasStructDef.AtlasAttributeDef.Cardinality.SINGLE),
                        new AtlasRelationshipEndDef("typeD", "attr1", AtlasStructDef.AtlasAttributeDef.Cardinality.SINGLE),

                        AtlasTypeUtil.createRequiredAttrDef("aaaa", "string"),
                        AtlasTypeUtil.createRequiredAttrDef("bbbb", "string"));
        AtlasRelationshipDef changeCardinality =
                AtlasTypeUtil.createRelationshipTypeDef("basicType", "description","" ,
                        AtlasRelationshipDef.RelationshipCategory.ASSOCIATION,
                        AtlasRelationshipDef.PropagateTags.BOTH,
                        new AtlasRelationshipEndDef("typeC", "attr1", AtlasStructDef.AtlasAttributeDef.Cardinality.LIST),
                        new AtlasRelationshipEndDef("typeD", "attr1", AtlasStructDef.AtlasAttributeDef.Cardinality.SINGLE),

                        AtlasTypeUtil.createRequiredAttrDef("aaaa", "string"),
                        AtlasTypeUtil.createRequiredAttrDef("bbbb", "string"));


        return new Object[][]{
                {
                        existingType,
                        changeType
                },
                {
                        existingType,
                        changeAttr
                },
                {
                        existingType,
                        changeCardinality
                }
        };
    }
    @DataProvider
    public Object[][] updateEnd2(){
        AtlasRelationshipDef existingType =
                AtlasTypeUtil.createRelationshipTypeDef("basicType", "description","" ,
                        AtlasRelationshipDef.RelationshipCategory.ASSOCIATION,
                        AtlasRelationshipDef.PropagateTags.BOTH,
                        new AtlasRelationshipEndDef("typeC", "attr1", AtlasStructDef.AtlasAttributeDef.Cardinality.SINGLE),
                        new AtlasRelationshipEndDef("typeD", "attr1", AtlasStructDef.AtlasAttributeDef.Cardinality.SINGLE),

                        AtlasTypeUtil.createRequiredAttrDef("aaaa", "string"),
                        AtlasTypeUtil.createRequiredAttrDef("bbbb", "string"));

        AtlasRelationshipDef changeType =
                AtlasTypeUtil.createRelationshipTypeDef("basicType", "description","" ,
                        AtlasRelationshipDef.RelationshipCategory.ASSOCIATION,
                        AtlasRelationshipDef.PropagateTags.BOTH,
                        new AtlasRelationshipEndDef("typeC", "attr1", AtlasStructDef.AtlasAttributeDef.Cardinality.SINGLE),
                        new AtlasRelationshipEndDef("typeE", "attr1", AtlasStructDef.AtlasAttributeDef.Cardinality.SINGLE),

                        AtlasTypeUtil.createRequiredAttrDef("aaaa", "string"),
                        AtlasTypeUtil.createRequiredAttrDef("bbbb", "string"));
        AtlasRelationshipDef changeAttr =
                AtlasTypeUtil.createRelationshipTypeDef("basicType", "description","" ,
                        AtlasRelationshipDef.RelationshipCategory.ASSOCIATION,
                        AtlasRelationshipDef.PropagateTags.BOTH,
                        new AtlasRelationshipEndDef("typeC", "attr1", AtlasStructDef.AtlasAttributeDef.Cardinality.SINGLE),
                        new AtlasRelationshipEndDef("typeD", "attr2", AtlasStructDef.AtlasAttributeDef.Cardinality.SINGLE),

                        AtlasTypeUtil.createRequiredAttrDef("aaaa", "string"),
                        AtlasTypeUtil.createRequiredAttrDef("bbbb", "string"));
        AtlasRelationshipDef changeCardinality =
                AtlasTypeUtil.createRelationshipTypeDef("basicType", "description","" ,
                        AtlasRelationshipDef.RelationshipCategory.ASSOCIATION,
                        AtlasRelationshipDef.PropagateTags.BOTH,
                        new AtlasRelationshipEndDef("typeC", "attr1", AtlasStructDef.AtlasAttributeDef.Cardinality.SINGLE),
                        new AtlasRelationshipEndDef("typeD", "attr1", AtlasStructDef.AtlasAttributeDef.Cardinality.LIST),

                        AtlasTypeUtil.createRequiredAttrDef("aaaa", "string"),
                        AtlasTypeUtil.createRequiredAttrDef("bbbb", "string"));


        return new Object[][]{
                {
                        existingType,
                        changeType
                },
                {
                        existingType,
                        changeAttr
                },
                {
                        existingType,
                        changeCardinality
                }
        };
    }

    @Test(dataProvider = "invalidAttributeNameWithReservedKeywords")
    public void testCreateTypeWithReservedKeywords(AtlasRelationshipDef atlasRelationshipDef) throws AtlasException {
        try {
            ApplicationProperties.get().setProperty(AtlasAbstractDefStoreV2.ALLOW_RESERVED_KEYWORDS, false);
            relationshipDefStore.create(atlasRelationshipDef, null);
        } catch (AtlasBaseException e) {
            Assert.assertEquals(e.getAtlasErrorCode(), AtlasErrorCode.ATTRIBUTE_NAME_INVALID);
        }
    }

    @Test(dataProvider = "updateValidProperties")
    public void testupdateVertexPreUpdatepropagateTags(AtlasRelationshipDef existingRelationshipDef,AtlasRelationshipDef newRelationshipDef) throws AtlasBaseException {
        AtlasRelationshipDefStoreV2.preUpdateCheck(existingRelationshipDef, newRelationshipDef);
    }

    @Test(dataProvider = "updateRename")
    public void testupdateVertexPreUpdateRename(AtlasRelationshipDef existingRelationshipDef,AtlasRelationshipDef newRelationshipDef)  {

        try {
            AtlasRelationshipDefStoreV2.preUpdateCheck(existingRelationshipDef, newRelationshipDef);
            fail("expected error");
        } catch (AtlasBaseException e) {
            if (!e.getAtlasErrorCode().equals(AtlasErrorCode.RELATIONSHIPDEF_INVALID_NAME_UPDATE)){
                fail("unexpected AtlasErrorCode "+e.getAtlasErrorCode());
            }
        }
    }
    @Test(dataProvider = "updateRelCat")
    public void testupdateVertexPreUpdateRelcat(AtlasRelationshipDef existingRelationshipDef,AtlasRelationshipDef newRelationshipDef)  {

        try {
            AtlasRelationshipDefStoreV2.preUpdateCheck(existingRelationshipDef, newRelationshipDef);
            fail("expected error");
        } catch (AtlasBaseException e) {
            if (!e.getAtlasErrorCode().equals(AtlasErrorCode.RELATIONSHIPDEF_INVALID_CATEGORY_UPDATE)){
                fail("unexpected AtlasErrorCode "+e.getAtlasErrorCode());
            }
        }
    }
    @Test(dataProvider = "updateEnd1")
    public void testupdateVertexPreUpdateEnd1(AtlasRelationshipDef existingRelationshipDef,AtlasRelationshipDef newRelationshipDef)  {

        try {
            AtlasRelationshipDefStoreV2.preUpdateCheck(existingRelationshipDef, newRelationshipDef);
            fail("expected error");
        } catch (AtlasBaseException e) {
            if (!e.getAtlasErrorCode().equals(AtlasErrorCode.RELATIONSHIPDEF_INVALID_END1_UPDATE)){
                fail("unexpected AtlasErrorCode "+e.getAtlasErrorCode());
            }
        }
    }

    @Test(dataProvider = "updateEnd2")
    public void testupdateVertexPreUpdateEnd2(AtlasRelationshipDef existingRelationshipDef,AtlasRelationshipDef newRelationshipDef)  {

        try {
            AtlasRelationshipDefStoreV2.preUpdateCheck(existingRelationshipDef, newRelationshipDef);
            fail("expected error");
        } catch (AtlasBaseException e) {
            if (!e.getAtlasErrorCode().equals(AtlasErrorCode.RELATIONSHIPDEF_INVALID_END2_UPDATE)){
                fail("unexpected AtlasErrorCode "+e.getAtlasErrorCode());
            }
        }
    }

    @BeforeClass
    public void initialize() throws Exception {
        super.initialize();
    }

    @AfterClass
    public void clear() throws Exception {
        AtlasGraphProvider.cleanup();

        super.cleanup();
    }
}
