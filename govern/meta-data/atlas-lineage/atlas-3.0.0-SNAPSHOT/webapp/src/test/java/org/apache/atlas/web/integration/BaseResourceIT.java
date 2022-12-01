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

package org.apache.atlas.web.integration;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.base.Preconditions;
import org.apache.atlas.ApplicationProperties;
import org.apache.atlas.AtlasClient;
import org.apache.atlas.AtlasClientV2;
import org.apache.atlas.AtlasServiceException;
import org.apache.atlas.kafka.AtlasKafkaMessage;
import org.apache.atlas.kafka.KafkaNotification;
import org.apache.atlas.model.instance.AtlasClassification;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntityWithExtInfo;
import org.apache.atlas.model.instance.AtlasEntityHeader;
import org.apache.atlas.model.instance.AtlasStruct;
import org.apache.atlas.model.instance.EntityMutationResponse;
import org.apache.atlas.model.instance.EntityMutations;
import org.apache.atlas.model.typedef.AtlasBaseTypeDef;
import org.apache.atlas.model.typedef.AtlasBusinessMetadataDef;
import org.apache.atlas.model.typedef.AtlasClassificationDef;
import org.apache.atlas.model.typedef.AtlasEntityDef;
import org.apache.atlas.model.typedef.AtlasEnumDef;
import org.apache.atlas.model.typedef.AtlasRelationshipDef;
import org.apache.atlas.model.typedef.AtlasStructDef;
import org.apache.atlas.model.typedef.AtlasStructDef.AtlasAttributeDef;
import org.apache.atlas.model.typedef.AtlasStructDef.AtlasAttributeDef.Cardinality;
import org.apache.atlas.model.typedef.AtlasStructDef.AtlasConstraintDef;
import org.apache.atlas.model.typedef.AtlasTypesDef;
import org.apache.atlas.notification.NotificationConsumer;
import org.apache.atlas.notification.NotificationInterface;
import org.apache.atlas.type.AtlasType;
import org.apache.atlas.type.AtlasTypeUtil;
import org.apache.atlas.utils.AuthenticationUtil;
import org.apache.atlas.utils.ParamChecker;
import org.apache.atlas.v1.model.instance.Id;
import org.apache.atlas.v1.model.instance.Referenceable;
import org.apache.atlas.v1.model.instance.Struct;
import org.apache.atlas.v1.model.notification.EntityNotificationV1;
import org.apache.atlas.v1.model.typedef.AttributeDefinition;
import org.apache.atlas.v1.model.typedef.ClassTypeDefinition;
import org.apache.atlas.v1.model.typedef.EnumTypeDefinition;
import org.apache.atlas.v1.model.typedef.EnumTypeDefinition.EnumValue;
import org.apache.atlas.v1.model.typedef.Multiplicity;
import org.apache.atlas.v1.model.typedef.StructTypeDefinition;
import org.apache.atlas.v1.model.typedef.TraitTypeDefinition;
import org.apache.atlas.v1.model.typedef.TypesDef;
import org.apache.atlas.v1.typesystem.types.utils.TypesUtil;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.atlas.model.typedef.AtlasStructDef.AtlasConstraintDef.CONSTRAINT_PARAM_ATTRIBUTE;
import static org.apache.atlas.model.typedef.AtlasStructDef.AtlasConstraintDef.CONSTRAINT_TYPE_INVERSE_REF;
import static org.apache.atlas.model.typedef.AtlasStructDef.AtlasConstraintDef.CONSTRAINT_TYPE_OWNED_REF;
import static org.apache.atlas.type.AtlasTypeUtil.createBusinessMetadataDef;
import static org.apache.atlas.type.AtlasTypeUtil.createOptionalAttrDef;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Base class for integration tests.
 * Sets up the web resource and has helper methods to created type and entity.
 */
public abstract class BaseResourceIT {
    public static final Logger LOG = LoggerFactory.getLogger(BaseResourceIT.class);

    public static final String ATLAS_REST_ADDRESS = "atlas.rest.address";
    public static final String NAME               = "name";
    public static final String QUALIFIED_NAME     = "qualifiedName";
    public static final String CLUSTER_NAME       = "clusterName";
    public static final String DESCRIPTION        = "description";
    public static final String PII_TAG            = "pii_Tag";
    public static final String PHI_TAG            = "phi_Tag";
    public static final String PCI_TAG            = "pci_Tag";
    public static final String SOX_TAG            = "sox_Tag";
    public static final String SEC_TAG            = "sec_Tag";
    public static final String FINANCE_TAG        = "finance_Tag";
    public static final String CLASSIFICATION     = "classification";

    protected static final int MAX_WAIT_TIME = 60000;

    // All service clients
    protected AtlasClient   atlasClientV1;
    protected AtlasClientV2 atlasClientV2;
    protected String[]      atlasUrls;


    protected NotificationInterface notificationInterface = null;
    protected KafkaNotification     kafkaNotification     = null;

    @BeforeClass
    public void setUp() throws Exception {
        //set high timeouts so that tests do not fail due to read timeouts while you
        //are stepping through the code in a debugger
        ApplicationProperties.get().setProperty("atlas.client.readTimeoutMSecs", "100000000");
        ApplicationProperties.get().setProperty("atlas.client.connectTimeoutMSecs", "100000000");


        Configuration configuration = ApplicationProperties.get();

        atlasUrls = configuration.getStringArray(ATLAS_REST_ADDRESS);

        if (atlasUrls == null || atlasUrls.length == 0) {
            atlasUrls = new String[] { "http://localhost:21000/" };
        }

        if (!AuthenticationUtil.isKerberosAuthenticationEnabled()) {
            atlasClientV1 = new AtlasClient(atlasUrls, new String[]{"admin", "admin"});
            atlasClientV2 = new AtlasClientV2(atlasUrls, new String[]{"admin", "admin"});
        } else {
            atlasClientV1 = new AtlasClient(atlasUrls);
            atlasClientV2 = new AtlasClientV2(atlasUrls);
        }
    }

    protected void batchCreateTypes(AtlasTypesDef typesDef) throws AtlasServiceException {
        AtlasTypesDef toCreate = new AtlasTypesDef();

        for (AtlasEnumDef enumDef : typesDef.getEnumDefs()) {
            if (atlasClientV2.typeWithNameExists(enumDef.getName())) {
                LOG.warn("Type with name {} already exists. Skipping", enumDef.getName());
            } else {
                toCreate.getEnumDefs().add(enumDef);
            }
        }

        for (AtlasStructDef structDef : typesDef.getStructDefs()) {
            if (atlasClientV2.typeWithNameExists(structDef.getName())) {
                LOG.warn("Type with name {} already exists. Skipping", structDef.getName());
            } else {
                toCreate.getStructDefs().add(structDef);
            }
        }

        for (AtlasEntityDef entityDef : typesDef.getEntityDefs()) {
            if (atlasClientV2.typeWithNameExists(entityDef.getName())) {
                LOG.warn("Type with name {} already exists. Skipping", entityDef.getName());
            } else  {
                toCreate.getEntityDefs().add(entityDef);
            }
        }

        for (AtlasClassificationDef classificationDef : typesDef.getClassificationDefs()) {
            if (atlasClientV2.typeWithNameExists(classificationDef.getName())) {
                LOG.warn("Type with name {} already exists. Skipping", classificationDef.getName());
            } else  {
                toCreate.getClassificationDefs().add(classificationDef);
            }
        }

        for (AtlasRelationshipDef relationshipDef : typesDef.getRelationshipDefs()) {
            if (atlasClientV2.typeWithNameExists(relationshipDef.getName())) {
                LOG.warn("Type with name {} already exists. Skipping", relationshipDef.getName());
            } else {
                toCreate.getRelationshipDefs().add(relationshipDef);
            }
        }

        for (AtlasBusinessMetadataDef businessMetadataDef : typesDef.getBusinessMetadataDefs()) {
            if (atlasClientV2.typeWithNameExists(businessMetadataDef.getName())) {
                LOG.warn("Type with name {} already exists. Skipping", businessMetadataDef.getName());
            } else {
                toCreate.getBusinessMetadataDefs().add(businessMetadataDef);
            }
        }

        atlasClientV2.createAtlasTypeDefs(toCreate);
    }

    protected void createType(AtlasTypesDef typesDef) throws AtlasServiceException {
        // Since the bulk create bails out on a single failure, this has to be done as a workaround
        batchCreateTypes(typesDef);
    }

    protected List<String> createType(TypesDef typesDef) throws Exception {
        List<EnumTypeDefinition>   enumTypes   = new ArrayList<>();
        List<StructTypeDefinition> structTypes = new ArrayList<>();
        List<TraitTypeDefinition>  traitTypes  = new ArrayList<>();
        List<ClassTypeDefinition>  classTypes  = new ArrayList<>();

        for (EnumTypeDefinition enumTypeDefinition : typesDef.getEnumTypes()) {
            if (atlasClientV2.typeWithNameExists(enumTypeDefinition.getName())) {
                LOG.warn("Type with name {} already exists. Skipping", enumTypeDefinition.getName());
            } else {
                enumTypes.add(enumTypeDefinition);
            }
        }

        for (StructTypeDefinition structTypeDefinition : typesDef.getStructTypes()) {
            if (atlasClientV2.typeWithNameExists(structTypeDefinition.getTypeName())) {
                LOG.warn("Type with name {} already exists. Skipping", structTypeDefinition.getTypeName());
            } else {
                structTypes.add(structTypeDefinition);
            }
        }

        for (TraitTypeDefinition hierarchicalTypeDefinition : typesDef.getTraitTypes()) {
            if (atlasClientV2.typeWithNameExists(hierarchicalTypeDefinition.getTypeName())) {
                LOG.warn("Type with name {} already exists. Skipping", hierarchicalTypeDefinition.getTypeName());
            } else {
                traitTypes.add(hierarchicalTypeDefinition);
            }
        }

        for (ClassTypeDefinition hierarchicalTypeDefinition : typesDef.getClassTypes()) {
            if (atlasClientV2.typeWithNameExists(hierarchicalTypeDefinition.getTypeName())) {
                LOG.warn("Type with name {} already exists. Skipping", hierarchicalTypeDefinition.getTypeName());
            } else {
                classTypes.add(hierarchicalTypeDefinition);
            }
        }

        TypesDef toCreate = new TypesDef(enumTypes, structTypes, traitTypes, classTypes);

        return atlasClientV1.createType(toCreate);
    }

    protected List<String> createType(String typesAsJSON) throws Exception {
        return createType(AtlasType.fromV1Json(typesAsJSON, TypesDef.class));
    }

    protected Id createInstance(Referenceable referenceable) throws Exception {
        String typeName = referenceable.getTypeName();

        System.out.println("creating instance of type " + typeName);

        List<String> guids = atlasClientV1.createEntity(referenceable);

        System.out.println("created instance for type " + typeName + ", guid: " + guids);

        // return the reference to created instance with guid
        if (guids.size() > 0) {
            return new Id(guids.get(guids.size() - 1), 0, referenceable.getTypeName());
        }

        return null;
    }

    protected TypesDef getTypesDef(List<EnumTypeDefinition>   enums,
                                   List<StructTypeDefinition> structs,
                                   List<TraitTypeDefinition>  traits,
                                   List<ClassTypeDefinition>  classes){
        enums   = (enums != null) ? enums : Collections.<EnumTypeDefinition>emptyList();
        structs = (structs != null) ? structs : Collections.<StructTypeDefinition>emptyList();
        traits  = (traits != null) ? traits : Collections.<TraitTypeDefinition>emptyList();
        classes = (classes != null) ? classes : Collections.<ClassTypeDefinition>emptyList();

        return new TypesDef(enums, structs, traits, classes);
    }

    protected AtlasEntityHeader modifyEntity(AtlasEntity atlasEntity, boolean update) {
        EntityMutationResponse entity = null;

        try {
            if (!update) {
                entity = atlasClientV2.createEntity(new AtlasEntityWithExtInfo(atlasEntity));

                assertNotNull(entity);
                assertNotNull(entity.getEntitiesByOperation(EntityMutations.EntityOperation.CREATE));
                assertTrue(entity.getEntitiesByOperation(EntityMutations.EntityOperation.CREATE).size() > 0);

                return entity.getEntitiesByOperation(EntityMutations.EntityOperation.CREATE).get(0);
            } else {
                entity = atlasClientV2.updateEntity(new AtlasEntityWithExtInfo(atlasEntity));

                assertNotNull(entity);
                assertNotNull(entity.getEntitiesByOperation(EntityMutations.EntityOperation.UPDATE));
                assertTrue(entity.getEntitiesByOperation(EntityMutations.EntityOperation.UPDATE).size() > 0);

                return entity.getEntitiesByOperation(EntityMutations.EntityOperation.UPDATE).get(0);
            }

        } catch (AtlasServiceException e) {
            LOG.error("Entity {} failed", update ? "update" : "creation", entity);
        }

        return null;
    }

    protected AtlasEntityHeader createEntity(AtlasEntity atlasEntity) {
        return modifyEntity(atlasEntity, false);
    }

    protected AtlasEntityHeader updateEntity(AtlasEntity atlasEntity) {
        return modifyEntity(atlasEntity, true);
    }

    protected static final String DATABASE_TYPE_V2     = "hive_db_v2";
    protected static final String HIVE_TABLE_TYPE_V2   = "hive_table_v2";
    protected static final String COLUMN_TYPE_V2       = "hive_column_v2";
    protected static final String HIVE_PROCESS_TYPE_V2 = "hive_process_v2";

    protected static final String DATABASE_TYPE     = "hive_db_v1";
    protected static final String HIVE_TABLE_TYPE   = "hive_table_v1";
    protected static final String COLUMN_TYPE       = "hive_column_v1";
    protected static final String HIVE_PROCESS_TYPE = "hive_process_v1";

    protected static final String DATABASE_TYPE_BUILTIN     = "hive_db";
    protected static final String HIVE_TABLE_TYPE_BUILTIN   = "hive_table";
    protected static final String COLUMN_TYPE_BUILTIN       = "hive_column";
    protected static final String HIVE_PROCESS_TYPE_BUILTIN = "hive_process";

    protected void createTypeDefinitionsV1() throws Exception {
        ClassTypeDefinition dbClsDef = TypesUtil
                .createClassTypeDef(DATABASE_TYPE, null, null,
                        TypesUtil.createUniqueRequiredAttrDef(NAME, AtlasBaseTypeDef.ATLAS_TYPE_STRING),
                        TypesUtil.createRequiredAttrDef(DESCRIPTION, AtlasBaseTypeDef.ATLAS_TYPE_STRING),
                        attrDef("locationUri", AtlasBaseTypeDef.ATLAS_TYPE_STRING),
                        attrDef("owner", AtlasBaseTypeDef.ATLAS_TYPE_STRING),
                        attrDef("createTime", AtlasBaseTypeDef.ATLAS_TYPE_LONG),
                        new AttributeDefinition("tables", AtlasBaseTypeDef.getArrayTypeName(HIVE_TABLE_TYPE),
                                Multiplicity.OPTIONAL, false, "db")
                );

        ClassTypeDefinition columnClsDef = TypesUtil
                .createClassTypeDef(COLUMN_TYPE, null, null, attrDef(NAME, AtlasBaseTypeDef.ATLAS_TYPE_STRING),
                        attrDef("dataType", AtlasBaseTypeDef.ATLAS_TYPE_STRING), attrDef("comment", AtlasBaseTypeDef.ATLAS_TYPE_STRING));

        StructTypeDefinition structTypeDefinition = new StructTypeDefinition("serdeType", null,
                Arrays.asList(new AttributeDefinition[]{TypesUtil.createRequiredAttrDef(NAME, AtlasBaseTypeDef.ATLAS_TYPE_STRING),
                        TypesUtil.createRequiredAttrDef("serde", AtlasBaseTypeDef.ATLAS_TYPE_STRING)}));

        EnumValue values[] = {new EnumValue("MANAGED", 1), new EnumValue("EXTERNAL", 2),};

        EnumTypeDefinition enumTypeDefinition = new EnumTypeDefinition("tableType", null, null, Arrays.asList(values));

        ClassTypeDefinition tblClsDef = TypesUtil
                .createClassTypeDef(HIVE_TABLE_TYPE, null, Collections.singleton("DataSet"),
                        attrDef("createTime", AtlasBaseTypeDef.ATLAS_TYPE_LONG),
                        attrDef("lastAccessTime", AtlasBaseTypeDef.ATLAS_TYPE_DATE),
                        attrDef("temporary", AtlasBaseTypeDef.ATLAS_TYPE_BOOLEAN),
                        new AttributeDefinition("db", DATABASE_TYPE, Multiplicity.OPTIONAL, true, "tables"),
                        new AttributeDefinition("columns", AtlasBaseTypeDef.getArrayTypeName(COLUMN_TYPE),
                                Multiplicity.OPTIONAL, true, null),
                        new AttributeDefinition("tableType", "tableType", Multiplicity.OPTIONAL, false, null),
                        new AttributeDefinition("serde1", "serdeType", Multiplicity.OPTIONAL, false, null),
                        new AttributeDefinition("serde2", "serdeType", Multiplicity.OPTIONAL, false, null));

        ClassTypeDefinition loadProcessClsDef = TypesUtil
                .createClassTypeDef(HIVE_PROCESS_TYPE, null, Collections.singleton("Process"),
                        attrDef("userName", AtlasBaseTypeDef.ATLAS_TYPE_STRING),
                        attrDef("startTime", AtlasBaseTypeDef.ATLAS_TYPE_LONG),
                        attrDef("endTime", AtlasBaseTypeDef.ATLAS_TYPE_LONG),
                        attrDef("queryText", AtlasBaseTypeDef.ATLAS_TYPE_STRING, Multiplicity.REQUIRED),
                        attrDef("queryPlan", AtlasBaseTypeDef.ATLAS_TYPE_STRING, Multiplicity.REQUIRED),
                        attrDef("queryId", AtlasBaseTypeDef.ATLAS_TYPE_STRING, Multiplicity.REQUIRED),
                        attrDef("queryGraph", AtlasBaseTypeDef.ATLAS_TYPE_STRING, Multiplicity.REQUIRED));

        TraitTypeDefinition classificationTrait = TypesUtil
                .createTraitTypeDef("classification", null, Collections.<String>emptySet(),
                        TypesUtil.createRequiredAttrDef("tag", AtlasBaseTypeDef.ATLAS_TYPE_STRING));

        TraitTypeDefinition piiTrait       = TypesUtil.createTraitTypeDef(PII_TAG, null, Collections.<String>emptySet());
        TraitTypeDefinition phiTrait       = TypesUtil.createTraitTypeDef(PHI_TAG, null, Collections.<String>emptySet());
        TraitTypeDefinition pciTrait       = TypesUtil.createTraitTypeDef(PCI_TAG, null, Collections.<String>emptySet());
        TraitTypeDefinition soxTrait       = TypesUtil.createTraitTypeDef(SOX_TAG, null, Collections.<String>emptySet());
        TraitTypeDefinition secTrait       = TypesUtil.createTraitTypeDef(SEC_TAG, null, Collections.<String>emptySet());
        TraitTypeDefinition financeTrait   = TypesUtil.createTraitTypeDef(FINANCE_TAG, null, Collections.<String>emptySet());
        TraitTypeDefinition factTrait      = TypesUtil.createTraitTypeDef("Fact" + randomString(), null, Collections.<String>emptySet());
        TraitTypeDefinition etlTrait       = TypesUtil.createTraitTypeDef("ETL" + randomString(), null, Collections.<String>emptySet());
        TraitTypeDefinition dimensionTrait = TypesUtil.createTraitTypeDef("Dimension" + randomString(), null, Collections.<String>emptySet());
        TraitTypeDefinition metricTrait    = TypesUtil.createTraitTypeDef("Metric" + randomString(), null, Collections.<String>emptySet());

        createType(getTypesDef(Collections.singletonList(enumTypeDefinition),
                               Collections.singletonList(structTypeDefinition),
                               Arrays.asList(classificationTrait, piiTrait, phiTrait, pciTrait, soxTrait, secTrait, financeTrait, factTrait, etlTrait, dimensionTrait, metricTrait),
                               Arrays.asList(dbClsDef, columnClsDef, tblClsDef, loadProcessClsDef)));
    }

    protected void createTypeDefinitionsV2() throws Exception {
        AtlasConstraintDef isCompositeSourceConstraint = new AtlasConstraintDef(CONSTRAINT_TYPE_OWNED_REF);
        AtlasConstraintDef isCompositeTargetConstraint = new AtlasConstraintDef(CONSTRAINT_TYPE_INVERSE_REF, Collections.<String, Object>singletonMap(CONSTRAINT_PARAM_ATTRIBUTE, "randomTable"));

        AtlasEntityDef dbClsTypeDef = AtlasTypeUtil.createClassTypeDef(
                DATABASE_TYPE_V2,
                null,
                AtlasTypeUtil.createUniqueRequiredAttrDef(NAME, "string"),
                AtlasTypeUtil.createRequiredAttrDef(DESCRIPTION, "string"),
                AtlasTypeUtil.createOptionalAttrDef("locationUri", "string"),
                AtlasTypeUtil.createOptionalAttrDef("owner", "string"),
                AtlasTypeUtil.createOptionalAttrDef("createTime", "long"),

                //there is a serializ
                new AtlasAttributeDef("randomTable",
                        AtlasBaseTypeDef.getArrayTypeName(HIVE_TABLE_TYPE_V2),
                        true,
                        Cardinality.SET,
                        0, -1, false, true, false, Collections.singletonList(isCompositeSourceConstraint))
        );

        AtlasEntityDef columnClsDef = AtlasTypeUtil
                .createClassTypeDef(COLUMN_TYPE_V2, null,
                        AtlasTypeUtil.createOptionalAttrDef(NAME, "string"),
                        AtlasTypeUtil.createOptionalAttrDef("dataType", "string"),
                        AtlasTypeUtil.createOptionalAttrDef("comment", "string"));

        AtlasStructDef structTypeDef = AtlasTypeUtil.createStructTypeDef("serdeType",
                AtlasTypeUtil.createRequiredAttrDef(NAME, "string"),
                AtlasTypeUtil.createRequiredAttrDef("serde", "string")
        );

        AtlasEnumDef enumDef = new AtlasEnumDef("tableType", DESCRIPTION, Arrays.asList(
                new AtlasEnumDef.AtlasEnumElementDef("MANAGED", null, 1),
                new AtlasEnumDef.AtlasEnumElementDef("EXTERNAL", null, 2)
        ));

        AtlasEntityDef tblClsDef = AtlasTypeUtil
                .createClassTypeDef(HIVE_TABLE_TYPE_V2,
                        Collections.singleton("DataSet"),
                        AtlasTypeUtil.createOptionalAttrDef("createTime", "long"),
                        AtlasTypeUtil.createOptionalAttrDef("lastAccessTime", "date"),
                        AtlasTypeUtil.createOptionalAttrDef("temporary", "boolean"),
                        new AtlasAttributeDef("db",
                                DATABASE_TYPE_V2,
                                true,
                                Cardinality.SINGLE,
                                0, 1, false, true, false, Collections.singletonList(isCompositeTargetConstraint)),

                        //some tests don't set the columns field or set it to null...
                        AtlasTypeUtil.createOptionalAttrDef("columns", AtlasBaseTypeDef.getArrayTypeName(COLUMN_TYPE_V2)),
                        AtlasTypeUtil.createOptionalAttrDef("tableType", "tableType"),
                        AtlasTypeUtil.createOptionalAttrDef("serde1", "serdeType"),
                        AtlasTypeUtil.createOptionalAttrDef("serde2", "serdeType"));

        AtlasEntityDef loadProcessClsDef = AtlasTypeUtil
                .createClassTypeDef(HIVE_PROCESS_TYPE_V2,
                        Collections.singleton("Process"),
                        AtlasTypeUtil.createOptionalAttrDef("userName", "string"),
                        AtlasTypeUtil.createOptionalAttrDef("startTime", "long"),
                        AtlasTypeUtil.createOptionalAttrDef("endTime", "long"),
                        AtlasTypeUtil.createRequiredAttrDef("queryText", "string"),
                        AtlasTypeUtil.createRequiredAttrDef("queryPlan", "string"),
                        AtlasTypeUtil.createRequiredAttrDef("queryId", "string"),
                        AtlasTypeUtil.createRequiredAttrDef("queryGraph", "string"));

        AtlasClassificationDef classificationTrait = AtlasTypeUtil
                .createTraitTypeDef("classification", Collections.<String>emptySet(),
                        AtlasTypeUtil.createRequiredAttrDef("tag", "string"));
        AtlasClassificationDef piiTrait     = AtlasTypeUtil.createTraitTypeDef(PII_TAG, Collections.<String>emptySet());
        AtlasClassificationDef phiTrait     = AtlasTypeUtil.createTraitTypeDef(PHI_TAG, Collections.<String>emptySet());
        AtlasClassificationDef pciTrait     = AtlasTypeUtil.createTraitTypeDef(PCI_TAG, Collections.<String>emptySet());
        AtlasClassificationDef soxTrait     = AtlasTypeUtil.createTraitTypeDef(SOX_TAG, Collections.<String>emptySet());
        AtlasClassificationDef secTrait     = AtlasTypeUtil.createTraitTypeDef(SEC_TAG, Collections.<String>emptySet());
        AtlasClassificationDef financeTrait = AtlasTypeUtil.createTraitTypeDef(FINANCE_TAG, Collections.<String>emptySet());

        //bussinessmetadata
        String _description = "_description";
        Map<String, String> options = new HashMap<>();
        options.put("maxStrLength", "20");
        AtlasBusinessMetadataDef bmNoApplicableTypes = createBusinessMetadataDef("bmNoApplicableTypes", _description, "1.0",
                createOptionalAttrDef("attr0", "string", options, _description));


        AtlasBusinessMetadataDef bmNoAttributes = createBusinessMetadataDef("bmNoAttributes", _description, "1.0", null);

        options.put("applicableEntityTypes", "[\"" + DATABASE_TYPE_V2 + "\",\"" + HIVE_TABLE_TYPE_V2 + "\"]");

        AtlasBusinessMetadataDef bmWithAllTypes = createBusinessMetadataDef("bmWithAllTypes", _description, "1.0",
                createOptionalAttrDef("attr1", AtlasBusinessMetadataDef.ATLAS_TYPE_BOOLEAN, options, _description),
                createOptionalAttrDef("attr2", AtlasBusinessMetadataDef.ATLAS_TYPE_BYTE, options, _description),
                createOptionalAttrDef("attr8", AtlasBusinessMetadataDef.ATLAS_TYPE_STRING, options, _description));

        AtlasBusinessMetadataDef bmWithAllTypesMV = createBusinessMetadataDef("bmWithAllTypesMV", _description, "1.0",
                createOptionalAttrDef("attr11", "array<boolean>", options, _description),
                createOptionalAttrDef("attr18", "array<string>", options, _description));

        AtlasTypesDef typesDef = new AtlasTypesDef(Collections.singletonList(enumDef),
                Collections.singletonList(structTypeDef),
                Arrays.asList(classificationTrait, piiTrait, phiTrait, pciTrait, soxTrait, secTrait, financeTrait),
                Arrays.asList(dbClsTypeDef, columnClsDef, tblClsDef, loadProcessClsDef),
                new ArrayList<>(),
                Arrays.asList(bmNoApplicableTypes, bmNoAttributes, bmWithAllTypes, bmWithAllTypesMV));
        batchCreateTypes(typesDef);
    }

    AttributeDefinition attrDef(String name, String dT) {
        return attrDef(name, dT, Multiplicity.OPTIONAL, false, null);
    }

    AttributeDefinition attrDef(String name, String dT, Multiplicity m) {
        return attrDef(name, dT, m, false, null);
    }

    AttributeDefinition attrDef(String name, String dT, Multiplicity m, boolean isComposite, String reverseAttributeName) {
        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(dT);

        return new AttributeDefinition(name, dT, m, isComposite, reverseAttributeName);
    }

    protected String randomString() {
        //names cannot start with a digit
        return RandomStringUtils.randomAlphabetic(1) + RandomStringUtils.randomAlphanumeric(9);
    }

    protected Referenceable createHiveTableInstanceBuiltIn(String dbName, String tableName, Id dbId) throws Exception {
        Map<String, Object> values = new HashMap<>();
        values.put(NAME, dbName);
        values.put(DESCRIPTION, "foo database");
        values.put(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, dbName);
        values.put("owner", "user1");
        values.put(CLUSTER_NAME, "cl1");
        values.put("parameters", Collections.EMPTY_MAP);
        values.put("location", "/tmp");

        Referenceable databaseInstance = new Referenceable(dbId._getId(), dbId.getTypeName(), values);
        Referenceable tableInstance    = new Referenceable(HIVE_TABLE_TYPE_BUILTIN, CLASSIFICATION, PII_TAG, PHI_TAG, PCI_TAG, SOX_TAG, SEC_TAG, FINANCE_TAG);
        tableInstance.set(NAME, tableName);
        tableInstance.set(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, tableName);
        tableInstance.set("db", databaseInstance);
        tableInstance.set(DESCRIPTION, "bar table");
        tableInstance.set("lastAccessTime", "2014-07-11T08:00:00.000Z");
        tableInstance.set("type", "managed");
        tableInstance.set("level", 2);
        tableInstance.set("tableType", 1); // enum
        tableInstance.set("compressed", false);

        Struct traitInstance = (Struct) tableInstance.getTrait("classification");
        traitInstance.set("tag", "foundation_etl");

        Struct serde1Instance = new Struct("serdeType");
        serde1Instance.set(NAME, "serde1");
        serde1Instance.set("serde", "serde1");
        tableInstance.set("serde1", serde1Instance);

        Struct serde2Instance = new Struct("serdeType");
        serde2Instance.set(NAME, "serde2");
        serde2Instance.set("serde", "serde2");
        tableInstance.set("serde2", serde2Instance);

        List<String> traits = tableInstance.getTraitNames();
        Assert.assertEquals(traits.size(), 7);

        return tableInstance;
    }

    protected AtlasEntity createHiveTableInstanceV2(AtlasEntity databaseInstance, String tableName) throws Exception {
        AtlasEntity tableInstance = new AtlasEntity(HIVE_TABLE_TYPE_V2);
        tableInstance.setClassifications(
                Arrays.asList(new AtlasClassification(CLASSIFICATION),
                        new AtlasClassification(PII_TAG),
                        new AtlasClassification(PHI_TAG),
                        new AtlasClassification(PCI_TAG),
                        new AtlasClassification(SOX_TAG),
                        new AtlasClassification(SEC_TAG),
                        new AtlasClassification(FINANCE_TAG))
        );

        tableInstance.setAttribute(NAME, tableName);
        tableInstance.setAttribute(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, tableName);
        tableInstance.setAttribute("db", AtlasTypeUtil.getAtlasObjectId(databaseInstance));
        tableInstance.setAttribute(DESCRIPTION, "bar table");
        tableInstance.setAttribute("lastAccessTime", "2014-07-11T08:00:00.000Z");
        tableInstance.setAttribute("type", "managed");
        tableInstance.setAttribute("level", 2);
        tableInstance.setAttribute("tableType", "MANAGED"); // enum
        tableInstance.setAttribute("compressed", false);

        AtlasClassification classification = tableInstance.getClassifications().get(0);
        classification.setAttribute("tag", "foundation_etl");

        AtlasStruct serde1Instance = new AtlasStruct("serdeType");
        serde1Instance.setAttribute(NAME, "serde1");
        serde1Instance.setAttribute("serde", "serde1");
        tableInstance.setAttribute("serde1", serde1Instance);

        AtlasStruct serde2Instance = new AtlasStruct("serdeType");
        serde2Instance.setAttribute(NAME, "serde2");
        serde2Instance.setAttribute("serde", "serde2");
        tableInstance.setAttribute("serde2", serde2Instance);

        List<AtlasClassification> traits = tableInstance.getClassifications();
        Assert.assertEquals(traits.size(), 7);

        return tableInstance;
    }

    protected Referenceable createHiveDBInstanceBuiltIn(String dbName) {
        Referenceable databaseInstance = new Referenceable(DATABASE_TYPE_BUILTIN);

        databaseInstance.set(NAME, dbName);
        databaseInstance.set(QUALIFIED_NAME, dbName);
        databaseInstance.set(CLUSTER_NAME, randomString());
        databaseInstance.set(DESCRIPTION, "foo database");

        return databaseInstance;
    }

    protected Referenceable createHiveDBInstanceV1(String dbName) {
        Referenceable databaseInstance = new Referenceable(DATABASE_TYPE);

        databaseInstance.set(NAME, dbName);
        databaseInstance.set(DESCRIPTION, "foo database");
        databaseInstance.set(CLUSTER_NAME, "fooCluster");

        return databaseInstance;
    }

    protected AtlasEntity createHiveDBInstanceV2(String dbName) {
        AtlasEntity atlasEntity = new AtlasEntity(DATABASE_TYPE_V2);

        atlasEntity.setAttribute(NAME, dbName);
        atlasEntity.setAttribute(DESCRIPTION, "foo database");
        atlasEntity.setAttribute(CLUSTER_NAME, "fooCluster");
        atlasEntity.setAttribute("owner", "user1");
        atlasEntity.setAttribute("locationUri", "/tmp");
        atlasEntity.setAttribute("createTime",1000);

        return atlasEntity;
    }

    protected  AtlasEntity createEntity(String typeName, String name) {
        AtlasEntity atlasEntity = new AtlasEntity(typeName);

        atlasEntity.setAttribute("name", name);
        atlasEntity.setAttribute("qualifiedName", name);
        atlasEntity.setAttribute("clusterName", randomString());

        return atlasEntity;
    }

    public interface Predicate {

        /**
         * Perform a predicate evaluation.
         *
         * @return the boolean result of the evaluation.
         * @throws Exception thrown if the predicate evaluation could not evaluate.
         */
        boolean evaluate() throws Exception;
    }

    public interface NotificationPredicate {

        /**
         * Perform a predicate evaluation.
         *
         * @return the boolean result of the evaluation.
         * @throws Exception thrown if the predicate evaluation could not evaluate.
         */
        boolean evaluate(EntityNotificationV1 notification) throws Exception;
    }

    /**
     * Wait for a condition, expressed via a {@link Predicate} to become true.
     *
     * @param timeout maximum time in milliseconds to wait for the predicate to become true.
     * @param predicate predicate waiting on.
     */
    protected void waitFor(int timeout, Predicate predicate) throws Exception {
        ParamChecker.notNull(predicate, "predicate");

        long    mustEnd = System.currentTimeMillis() + timeout;
        boolean eval;

        while (!(eval = predicate.evaluate()) && System.currentTimeMillis() < mustEnd) {
            LOG.info("Waiting up to {} msec", mustEnd - System.currentTimeMillis());

            Thread.sleep(100);
        }

        if (!eval) {
            throw new Exception("Waiting timed out after " + timeout + " msec");
        }
    }

    protected EntityNotificationV1 waitForNotification(final NotificationConsumer<EntityNotificationV1> consumer, int maxWait,
                                                       final NotificationPredicate predicate) throws Exception {
        final TypesUtil.Pair<EntityNotificationV1, String> pair           = TypesUtil.Pair.of(null, null);
        final long                                         maxCurrentTime = System.currentTimeMillis() + maxWait;

        waitFor(maxWait, new Predicate() {
            @Override
            public boolean evaluate() throws Exception {
                try {
                    while (System.currentTimeMillis() < maxCurrentTime) {
                        List<AtlasKafkaMessage<EntityNotificationV1>> messageList = consumer.receive();

                        if(messageList.size() > 0) {
                            EntityNotificationV1 notification = messageList.get(0).getMessage();

                            if (predicate.evaluate(notification)) {
                                pair.left = notification;

                                return true;
                            }
                        } else {
                            LOG.info( System.currentTimeMillis()+ " messageList no records" +maxCurrentTime );
                        }
                    }
                } catch(Exception e) {
                    LOG.error(" waitForNotification", e);
                    //ignore
                }

                return false;
            }
        });

        return pair.left;
    }

    protected NotificationPredicate newNotificationPredicate(final EntityNotificationV1.OperationType operationType,
                                                             final String typeName, final String guid) {
        return new NotificationPredicate() {
            @Override
            public boolean evaluate(EntityNotificationV1 notification) throws Exception {
                return notification != null &&
                        notification.getOperationType() == operationType &&
                        notification.getEntity().getTypeName().equals(typeName) &&
                        notification.getEntity().getId()._getId().equals(guid);
            }
        };
    }

    protected ArrayNode searchByDSL(String dslQuery) throws AtlasServiceException {
        return atlasClientV1.searchByDSL(dslQuery, 10, 0);
    }

    protected void initNotificationService() throws Exception {
        Configuration applicationProperties = ApplicationProperties.get();

        applicationProperties.setProperty("atlas.kafka.data", "target/" + RandomStringUtils.randomAlphanumeric(5));

        kafkaNotification     = new KafkaNotification(applicationProperties);
        notificationInterface = kafkaNotification;

        kafkaNotification.start();
        Thread.sleep(2000);
    }

    protected void cleanUpNotificationService() {
        if (kafkaNotification != null) {
            kafkaNotification.close();
            kafkaNotification.stop();
        }

    }
}
