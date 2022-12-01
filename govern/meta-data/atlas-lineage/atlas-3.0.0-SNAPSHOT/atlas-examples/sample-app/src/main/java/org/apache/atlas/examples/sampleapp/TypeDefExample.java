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
package org.apache.atlas.examples.sampleapp;

import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.apache.atlas.AtlasClientV2;
import org.apache.atlas.AtlasServiceException;
import org.apache.atlas.model.SearchFilter;
import org.apache.atlas.model.typedef.AtlasBaseTypeDef;
import org.apache.atlas.model.typedef.AtlasBusinessMetadataDef;
import org.apache.atlas.model.typedef.AtlasClassificationDef;
import org.apache.atlas.model.typedef.AtlasEntityDef;
import org.apache.atlas.model.typedef.AtlasEnumDef;
import org.apache.atlas.model.typedef.AtlasRelationshipDef;
import org.apache.atlas.model.typedef.AtlasStructDef;
import org.apache.atlas.model.typedef.AtlasStructDef.AtlasAttributeDef;
import org.apache.atlas.model.typedef.AtlasTypesDef;
import org.apache.atlas.type.AtlasTypeUtil;

import javax.ws.rs.core.MultivaluedMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.atlas.examples.sampleapp.SampleAppConstants.*;
import static org.apache.atlas.model.typedef.AtlasBaseTypeDef.*;
import static org.apache.atlas.model.typedef.AtlasRelationshipDef.RelationshipCategory.AGGREGATION;
import static org.apache.atlas.model.typedef.AtlasRelationshipDef.RelationshipCategory.COMPOSITION;
import static org.apache.atlas.model.typedef.AtlasStructDef.AtlasAttributeDef.Cardinality.SET;
import static org.apache.atlas.model.typedef.AtlasStructDef.AtlasAttributeDef.Cardinality.SINGLE;
import static org.apache.atlas.type.AtlasTypeUtil.createBusinessMetadataDef;
import static org.apache.atlas.type.AtlasTypeUtil.createOptionalAttrDef;
import static org.apache.atlas.type.AtlasTypeUtil.createRelationshipEndDef;
import static org.apache.atlas.type.AtlasTypeUtil.createRelationshipTypeDef;
import static org.apache.atlas.type.AtlasTypeUtil.createTraitTypeDef;

public class TypeDefExample {
    private static final String[] SAMPLE_APP_TYPES = {
            SampleAppConstants.DATABASE_TYPE,
            SampleAppConstants.TABLE_TYPE,
            SampleAppConstants.COLUMN_TYPE,
            SampleAppConstants.PROCESS_TYPE,
            SampleAppConstants.PII_TAG,
            SampleAppConstants.CLASSIFIED_TAG,
            SampleAppConstants.FINANCE_TAG,
            SampleAppConstants.METRIC_TAG
    };

    private final AtlasClientV2 client;
    private       AtlasTypesDef typesDef;

    TypeDefExample(AtlasClientV2 client) {
        this.client = client;
    }

    public void createTypeDefinitions() throws Exception {
        AtlasEntityDef databaseDef  = createDatabaseDef();
        AtlasEntityDef tableDef     = createTableDef();
        AtlasEntityDef columnDef    = createColumnDef();
        AtlasEntityDef processDef   = createProcessDef();
        AtlasStructDef serDeDef     = createSerDeDef();
        AtlasEnumDef   tableTypeDef = createTableTypeEnumDef();

        List<AtlasClassificationDef>   classificationDefs  = createClassificationDefs();
        List<AtlasBusinessMetadataDef> businessMetadataDef = createBusinessMetadataDefs();
        List<AtlasRelationshipDef>     relationshipDefs    = createAtlasRelationshipDef();

        AtlasTypesDef typesDef = new AtlasTypesDef(Collections.singletonList(tableTypeDef),
                                                   Collections.singletonList(serDeDef),
                                                   classificationDefs,
                                                   Arrays.asList(databaseDef, tableDef, columnDef, processDef),
                                                   relationshipDefs,
                                                   businessMetadataDef);

        this.typesDef = batchCreateTypes(typesDef);
    }

    public void printTypeDefinitions() throws AtlasServiceException {
        for (String typeName : SAMPLE_APP_TYPES) {
            MultivaluedMap<String, String> searchParams = new MultivaluedMapImpl();

            searchParams.add(SearchFilter.PARAM_NAME, typeName);

            SearchFilter searchFilter = new SearchFilter(searchParams);

            AtlasTypesDef typesDef = client.getAllTypeDefs(searchFilter);

            assert (!typesDef.isEmpty());

            SampleApp.log("Created type: " + typeName);
        }
    }

    public void removeTypeDefinitions() throws AtlasServiceException {
        if (typesDef != null) {
            client.deleteAtlasTypeDefs(typesDef);

            typesDef = null;

            SampleApp.log("Deleted TypesDef successfully!");
        }
    }

    private AtlasEntityDef createDatabaseDef() {
        return AtlasTypeUtil.createClassTypeDef(SampleAppConstants.DATABASE_TYPE,
                                                Collections.singleton(ENTITY_TYPE_DATASET),
                                                AtlasTypeUtil.createOptionalAttrDef("locationUri", "string"),
                                                AtlasTypeUtil.createOptionalAttrDef(ATTR_CREATE_TIME, "long"),
                                                new AtlasAttributeDef(ATTR_RANDOM_TABLE,
                                                        AtlasBaseTypeDef.getArrayTypeName(SampleAppConstants.TABLE_TYPE),
                                                        true, AtlasAttributeDef.Cardinality.SET));
    }

    private AtlasEntityDef createTableDef() {
        return AtlasTypeUtil.createClassTypeDef(SampleAppConstants.TABLE_TYPE,
                                                Collections.singleton(ENTITY_TYPE_DATASET),
                                                createOptionalAttrDef(ATTR_CREATE_TIME, "long"),
                                                createOptionalAttrDef(ATTR_LAST_ACCESS_TIME, "date"),
                                                createOptionalAttrDef(ATTR_TEMPORARY, "boolean"),
                                                createOptionalAttrDef(ATTR_TABLE_TYPE, ENUM_TABLE_TYPE),
                                                createOptionalAttrDef(ATTR_SERDE1, STRUCT_TYPE_SERDE),
                                                createOptionalAttrDef(ATTR_SERDE2, STRUCT_TYPE_SERDE));
    }

    private AtlasEntityDef createColumnDef() {
        return AtlasTypeUtil.createClassTypeDef(SampleAppConstants.COLUMN_TYPE,
                                                Collections.singleton(ENTITY_TYPE_DATASET),
                                                AtlasTypeUtil.createOptionalAttrDef(ATTR_DATA_TYPE, "string"),
                                                AtlasTypeUtil.createOptionalAttrDef(ATTR_COMMENT, "string"));
    }

    private AtlasEntityDef createProcessDef() {
        return AtlasTypeUtil.createClassTypeDef(SampleAppConstants.PROCESS_TYPE,
                                                Collections.singleton(ENTITY_TYPE_PROCESS),
                                                AtlasTypeUtil.createOptionalAttrDef(ATTR_USERNAME, "string"),
                                                AtlasTypeUtil.createOptionalAttrDef(ATTR_START_TIME, "long"),
                                                AtlasTypeUtil.createOptionalAttrDef(ATTR_END_TIME, "long"),
                                                AtlasTypeUtil.createRequiredAttrDef(ATTR_QUERY_TEXT, "string"),
                                                AtlasTypeUtil.createRequiredAttrDef(ATTR_QUERY_PLAN, "string"),
                                                AtlasTypeUtil.createRequiredAttrDef(ATTR_QUERY_ID, "string"),
                                                AtlasTypeUtil.createRequiredAttrDef(ATTR_QUERY_GRAPH, "string"));
    }

    private AtlasStructDef createSerDeDef() {
        return AtlasTypeUtil.createStructTypeDef(SampleAppConstants.STRUCT_TYPE_SERDE,
                                                 AtlasTypeUtil.createRequiredAttrDef(SampleAppConstants.ATTR_NAME, "string"),
                                                 AtlasTypeUtil.createRequiredAttrDef(ATTR_SERDE, "string"));
    }

    private AtlasEnumDef createTableTypeEnumDef() {
        return new AtlasEnumDef(SampleAppConstants.ENUM_TABLE_TYPE,
                                SampleAppConstants.ATTR_DESCRIPTION,
                                Arrays.asList(new AtlasEnumDef.AtlasEnumElementDef("MANAGED", null, 1),
                                              new AtlasEnumDef.AtlasEnumElementDef("EXTERNAL", null, 2)));
    }

    private List<AtlasClassificationDef> createClassificationDefs() {
        AtlasClassificationDef classification = createTraitTypeDef(SampleAppConstants.CLASSIFIED_TAG, Collections.<String>emptySet(), AtlasTypeUtil.createRequiredAttrDef("tag", "string"));
        AtlasClassificationDef pii            = createTraitTypeDef(SampleAppConstants.PII_TAG, Collections.<String>emptySet());
        AtlasClassificationDef finance        = createTraitTypeDef(SampleAppConstants.FINANCE_TAG, Collections.<String>emptySet());
        AtlasClassificationDef metric         = createTraitTypeDef(SampleAppConstants.METRIC_TAG, Collections.emptySet());

        return Arrays.asList(classification, pii, finance, metric);
    }

    private List<AtlasBusinessMetadataDef> createBusinessMetadataDefs() {
        String description = "description";

        Map<String, String> options = new HashMap<>();

        options.put("maxStrLength", "20");
        options.put("applicableEntityTypes", "[\"" + SampleAppConstants.DATABASE_TYPE + "\",\"" + SampleAppConstants.TABLE_TYPE + "\"]");

        AtlasBusinessMetadataDef bmWithAllTypes = createBusinessMetadataDef(SampleAppConstants.BUSINESS_METADATA_TYPE,
                                                                            description,
                                                                            "1.0",
                                                                            createOptionalAttrDef(ATTR_ATTR1, ATLAS_TYPE_BOOLEAN, options, description),
                                                                            createOptionalAttrDef(ATTR_ATTR2, ATLAS_TYPE_BYTE, options, description),
                                                                            createOptionalAttrDef(ATTR_ATTR8, ATLAS_TYPE_STRING, options, description));

        AtlasBusinessMetadataDef bmWithAllTypesMV = createBusinessMetadataDef(SampleAppConstants.BUSINESS_METADATA_TYPE_MV,
                                                                              description,
                                                                              "1.0",
                                                                              createOptionalAttrDef(ATTR_ATTR11, "array<boolean>", options, description),
                                                                              createOptionalAttrDef(ATTR_ATTR18, "array<string>", options, description));

        return Arrays.asList(bmWithAllTypes, bmWithAllTypesMV);
    }

    private List<AtlasRelationshipDef> createAtlasRelationshipDef() {
        AtlasRelationshipDef dbTablesDef = createRelationshipTypeDef(SampleAppConstants.TABLE_DATABASE_TYPE, SampleAppConstants.TABLE_DATABASE_TYPE,
                                                                     "1.0", AGGREGATION, AtlasRelationshipDef.PropagateTags.NONE,
                                                                     createRelationshipEndDef(SampleAppConstants.TABLE_TYPE, "db", SINGLE, false),
                                                                     createRelationshipEndDef(SampleAppConstants.DATABASE_TYPE, "tables", SET, true));

        AtlasRelationshipDef tableColumnsDef = createRelationshipTypeDef(SampleAppConstants.TABLE_COLUMNS_TYPE, SampleAppConstants.TABLE_COLUMNS_TYPE,
                                                                         "1.0", COMPOSITION, AtlasRelationshipDef.PropagateTags.NONE,
                                                                         createRelationshipEndDef(SampleAppConstants.TABLE_TYPE, "columns", SET, true),
                                                                         createRelationshipEndDef(SampleAppConstants.COLUMN_TYPE, "table", SINGLE, false));

        return Arrays.asList(dbTablesDef, tableColumnsDef);
    }

    private AtlasTypesDef batchCreateTypes(AtlasTypesDef typesDef) throws AtlasServiceException {
        AtlasTypesDef typesToCreate = new AtlasTypesDef();

        for (AtlasEnumDef enumDef : typesDef.getEnumDefs()) {
            if (client.typeWithNameExists(enumDef.getName())) {
                SampleApp.log(enumDef.getName() + ": type already exists. Skipping");
            } else {
                typesToCreate.getEnumDefs().add(enumDef);
            }
        }

        for (AtlasStructDef structDef : typesDef.getStructDefs()) {
            if (client.typeWithNameExists(structDef.getName())) {
                SampleApp.log(structDef.getName() + ": type already exists. Skipping");
            } else {
                typesToCreate.getStructDefs().add(structDef);
            }
        }

        for (AtlasEntityDef entityDef : typesDef.getEntityDefs()) {
            if (client.typeWithNameExists(entityDef.getName())) {
                SampleApp.log(entityDef.getName() + ": type already exists. Skipping");
            } else {
                typesToCreate.getEntityDefs().add(entityDef);
            }
        }

        for (AtlasClassificationDef classificationDef : typesDef.getClassificationDefs()) {
            if (client.typeWithNameExists(classificationDef.getName())) {
                SampleApp.log(classificationDef.getName() + ": type already exists. Skipping");
            } else {
                typesToCreate.getClassificationDefs().add(classificationDef);
            }
        }

        for (AtlasRelationshipDef relationshipDef : typesDef.getRelationshipDefs()) {
            if (client.typeWithNameExists(relationshipDef.getName())) {
                SampleApp.log(relationshipDef.getName() + ": type already exists. Skipping");
            } else {
                typesToCreate.getRelationshipDefs().add(relationshipDef);
            }
        }

        for (AtlasBusinessMetadataDef businessMetadataDef : typesDef.getBusinessMetadataDefs()) {
            if (client.typeWithNameExists(businessMetadataDef.getName())) {
                SampleApp.log(businessMetadataDef.getName() + ": type already exists. Skipping");
            } else {
                typesToCreate.getBusinessMetadataDefs().add(businessMetadataDef);
            }
        }

        return client.createAtlasTypeDefs(typesToCreate);
    }
}