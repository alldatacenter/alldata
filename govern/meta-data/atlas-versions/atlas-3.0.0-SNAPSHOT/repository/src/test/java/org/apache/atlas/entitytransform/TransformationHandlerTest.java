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
package org.apache.atlas.entitytransform;

import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.impexp.AtlasExportRequest;
import org.apache.atlas.model.impexp.AttributeTransform;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasObjectId;
import org.apache.atlas.model.typedef.AtlasEntityDef;
import org.apache.atlas.model.typedef.AtlasTypesDef;
import org.apache.atlas.type.AtlasType;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.atlas.entitytransform.TransformationConstants.HDFS_PATH;
import static org.apache.atlas.entitytransform.TransformationConstants.HIVE_COLUMN;
import static org.apache.atlas.entitytransform.TransformationConstants.HIVE_DATABASE;
import static org.apache.atlas.entitytransform.TransformationConstants.HIVE_STORAGE_DESCRIPTOR;
import static org.apache.atlas.entitytransform.TransformationConstants.QUALIFIED_NAME_ATTRIBUTE;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.apache.atlas.entitytransform.TransformationConstants.HIVE_TABLE;

public class TransformationHandlerTest {
    private static final Logger LOG = LoggerFactory.getLogger(TransformationHandlerTest.class);

    private static final String TYPENAME_REFERENCEABLE = "Referenceable";
    private static final String TYPENAME_ASSET         = "Asset";
    private static final String TYPENAME_NON_ASSET     = "non_asset";

    private static final String[] CLUSTER_NAMES  = new String[] { "cl1", "prod" };
    private static final String[] DATABASE_NAMES = new String[] { "hr", "sales", "engg" };
    private static final String[] TABLE_NAMES    = new String[] { "employees", "products", "invoice" };
    private static final String[] COLUMN_NAMES   = new String[] { "name", "age", "dob" };
    private final String ATTR_NAME_QUALIFIED_NAME = "qualifiedName";

    @Test
    public void testHdfsClusterRenameHandler() {
        // Rename clusterName from cl1 to cl2
        AttributeTransform p1 = new AttributeTransform(Collections.singletonMap("hdfs_path.clusterName", "EQUALS: cl1"),
                                                       Collections.singletonMap("hdfs_path.clusterName", "SET: cl2"));

        List<BaseEntityHandler> handlers = initializeHandlers(Collections.singletonList(p1));

        for (AtlasEntity hdfsPath : getHdfsPathEntities()) {
            String  qualifiedName = (String) hdfsPath.getAttribute(ATTR_NAME_QUALIFIED_NAME);
            boolean endsWithCl1   = qualifiedName.endsWith("@cl1");

            applyTransforms(hdfsPath, handlers);

            String transformedValue = (String) hdfsPath.getAttribute(ATTR_NAME_QUALIFIED_NAME);

            if (endsWithCl1) {
                assertTrue(transformedValue.endsWith("@cl2"), transformedValue + ": expected to end with @cl2");
            } else {
                assertEquals(qualifiedName, transformedValue, "not expected to change");
            }
        }
    }

    @Test
    public void testHdfsClusterNameToggleCaseHandler() {
        // Change clusterName to Upper case
        AttributeTransform p1 = new AttributeTransform(Collections.singletonMap("hdfs_path.clusterName", "EQUALS: cl1"),
                                                       Collections.singletonMap("hdfs_path.clusterName", "TO_UPPER:"));

        List<BaseEntityHandler> handlers = initializeHandlers(Collections.singletonList(p1));

        List<AtlasEntity> hdfsPaths = getHdfsPathEntities();

        for (AtlasEntity hdfsPath : hdfsPaths) {
            String  qualifiedName = (String) hdfsPath.getAttribute(ATTR_NAME_QUALIFIED_NAME);
            boolean endsWithCl1   = qualifiedName.endsWith("@cl1");

            applyTransforms(hdfsPath, handlers);

            String transformedValue = (String) hdfsPath.getAttribute(ATTR_NAME_QUALIFIED_NAME);

            if (endsWithCl1) {
                assertTrue(transformedValue.endsWith("@CL1"), transformedValue + ": expected to end with @CL1");
            } else {
                assertEquals(qualifiedName, transformedValue, "not expected to change");
            }
        }

        // Change clusterName back to lower case
        AttributeTransform p2 = new AttributeTransform(Collections.singletonMap("hdfs_path.clusterName", "EQUALS: CL1"),
                                                       Collections.singletonMap("hdfs_path.clusterName", "TO_LOWER:"));

        handlers = initializeHandlers(Collections.singletonList(p2));

        for (AtlasEntity hdfsPath : hdfsPaths) {
            String  qualifiedName = (String) hdfsPath.getAttribute(ATTR_NAME_QUALIFIED_NAME);
            boolean endsWithCL1   = qualifiedName.endsWith("@CL1");

            applyTransforms(hdfsPath, handlers);

            String transformedValue = (String) hdfsPath.getAttribute(ATTR_NAME_QUALIFIED_NAME);

            if (endsWithCL1) {
                assertTrue(transformedValue.endsWith("@cl1"), transformedValue + ": expected to end with @cl1");
            } else {
                assertEquals(qualifiedName, transformedValue, "not expected to change");
            }
        }
    }

    @Test
    public void testHiveTableClearAttributeHandler() {
        // clear replicatedTo attribute for hive_table entities
        AttributeTransform p1 = new AttributeTransform(Collections.singletonMap("hive_table.replicatedTo", "HAS_VALUE:"),
                                                       Collections.singletonMap("hive_table.replicatedTo", "CLEAR:"));

        List<BaseEntityHandler> handlers = initializeHandlers(Collections.singletonList(p1));

        List<AtlasEntity> entities = getAllEntities();

        for (AtlasEntity entity : entities) {
            String  replicatedTo = (String) entity.getAttribute("replicatedTo");

            if (entity.getTypeName() == HIVE_TABLE) {
                assertTrue(StringUtils.isNotEmpty(replicatedTo));
            }

            applyTransforms(entity, handlers);

            String transformedValue = (String) entity.getAttribute("replicatedTo");

            if (entity.getTypeName() == HIVE_TABLE) {
                assertTrue(StringUtils.isEmpty(transformedValue));
            }
        }
    }

    @Test
    public void testEntityClearAttributesActionWithNoCondition() {
        // clear replicatedFrom attribute for hive_table entities without any condition
        Map<String, String> actions = new HashMap<String, String>() {{  put("Referenceable.replicatedTo", "CLEAR:");
                                                                        put("Referenceable.replicatedFrom", "CLEAR:"); }};

        AttributeTransform transform = new AttributeTransform(null, actions);

        List<BaseEntityHandler> handlers = initializeHandlers(Collections.singletonList(transform));


        List<AtlasEntity> entities = getAllEntities();

        for (AtlasEntity entity : entities) {
            String replicatedTo   = (String) entity.getAttribute("replicatedTo");
            String replicatedFrom = (String) entity.getAttribute("replicatedFrom");

            if (entity.getTypeName() == HIVE_TABLE) {
                assertTrue(StringUtils.isNotEmpty(replicatedTo));
                assertTrue(StringUtils.isNotEmpty(replicatedFrom));
            }

            applyTransforms(entity, handlers);

            replicatedTo   = (String) entity.getAttribute("replicatedTo");
            replicatedFrom = (String) entity.getAttribute("replicatedFrom");

            if (entity.getTypeName() == HIVE_TABLE) {
                assertTrue(StringUtils.isEmpty(replicatedTo));
                assertTrue(StringUtils.isEmpty(replicatedFrom));
            }
        }
    }

    @Test
    public void testEntityClearAttributesActionWithNoTypeNameAndNoCondition() {
        // clear replicatedFrom attribute for hive_table entities without any condition
        Map<String, String> actions = new HashMap<String, String>() {{  put("replicatedTo", "CLEAR:");
                                                                        put("replicatedFrom", "CLEAR:"); }};

        AttributeTransform transform = new AttributeTransform(null, actions);

        List<BaseEntityHandler> handlers = initializeHandlers(Collections.singletonList(transform));

        List<AtlasEntity> entities = getAllEntities();

        for (AtlasEntity entity : entities) {
            String replicatedTo   = (String) entity.getAttribute("replicatedTo");
            String replicatedFrom = (String) entity.getAttribute("replicatedFrom");

            if (entity.getTypeName() == HIVE_TABLE) {
                assertTrue(StringUtils.isNotEmpty(replicatedTo));
                assertTrue(StringUtils.isNotEmpty(replicatedFrom));
            }

            applyTransforms(entity, handlers);

            replicatedTo   = (String) entity.getAttribute("replicatedTo");
            replicatedFrom = (String) entity.getAttribute("replicatedFrom");

            if (entity.getTypeName() == HIVE_TABLE) {
                assertTrue(StringUtils.isEmpty(replicatedTo));
                assertTrue(StringUtils.isEmpty(replicatedFrom));
            }
        }
    }

    @Test
    public void testHdfsPathNameReplacePrefixHandler() {
        // Prefix replace hdfs_path name from /aa/bb/ to /xx/yy/
        AttributeTransform p1 = new AttributeTransform(Collections.singletonMap("hdfs_path.name", "STARTS_WITH: /aa/bb/"),
                                                       Collections.singletonMap("hdfs_path.name", "REPLACE_PREFIX: = :/aa/bb/=/xx/yy/"));

        List<BaseEntityHandler> handlers = initializeHandlers(Collections.singletonList(p1));

        for (AtlasEntity hdfsPath : getHdfsPathEntities()) {
            String  name              = (String) hdfsPath.getAttribute("name");
            boolean startsWith_aa_bb_ = name.startsWith("/aa/bb/");

            applyTransforms(hdfsPath, handlers);

            String transformedValue = (String) hdfsPath.getAttribute("name");

            if (startsWith_aa_bb_) {
                assertTrue(transformedValue.startsWith("/xx/yy/"), transformedValue + ": expected to start with /xx/yy/");
            } else {
                assertEquals(name, transformedValue, "not expected to change");
            }
        }
    }

    @Test
    public void testHiveDatabaseClusterRenameHandler() {
        // replace clusterName: from cl1 to cl1_backup
        AttributeTransform p1 = new AttributeTransform(Collections.singletonMap("hive_db.clusterName", "EQUALS: cl1"),
                                                       Collections.singletonMap("hive_db.clusterName", "SET: cl1_backup"));

        List<BaseEntityHandler> handlers = initializeHandlers(Collections.singletonList(p1));

        for (AtlasEntity entity : getAllEntities()) {
            String  qualifiedName = (String) entity.getAttribute(ATTR_NAME_QUALIFIED_NAME);
            boolean isHdfsPath    = StringUtils.equals(entity.getTypeName(), HDFS_PATH);
            boolean endsWithCl1   = qualifiedName.endsWith("@cl1");
            boolean containsCl1   = qualifiedName.contains("@cl1"); // for stroage_desc

            applyTransforms(entity, handlers);

            String transformedValue = (String) entity.getAttribute(ATTR_NAME_QUALIFIED_NAME);

            if (!isHdfsPath && endsWithCl1) {
                assertTrue(transformedValue.endsWith("@cl1_backup"), transformedValue + ": expected to end with @cl1_backup");
            } else if (!isHdfsPath && containsCl1) {
                assertTrue(transformedValue.contains("@cl1_backup"), transformedValue + ": expected to contains @cl1_backup");
            } else {
                assertEquals(qualifiedName, transformedValue, "not expected to change");
            }
        }
    }

    @Test
    public void testHiveDatabaseNameRenameHandler() {
        // replace dbName: from hr to hr_backup
        AttributeTransform p = new AttributeTransform(Collections.singletonMap("hive_db.name", "EQUALS: hr"),
                                                      Collections.singletonMap("hive_db.name", "SET: hr_backup"));

        List<BaseEntityHandler> handlers = initializeHandlers(Collections.singletonList(p));

        for (AtlasEntity entity : getAllEntities()) {
            String  qualifiedName   = (String) entity.getAttribute(ATTR_NAME_QUALIFIED_NAME);
            boolean startsWithHrDot = qualifiedName.startsWith("hr."); // for tables, columns
            boolean startsWithHrAt  = qualifiedName.startsWith("hr@"); // for databases

            applyTransforms(entity, handlers);

            if (startsWithHrDot) {
                assertTrue(((String) entity.getAttribute(ATTR_NAME_QUALIFIED_NAME)).startsWith("hr_backup."));
            } else if (startsWithHrAt) {
                assertTrue(((String) entity.getAttribute(ATTR_NAME_QUALIFIED_NAME)).startsWith("hr_backup@"));
            } else {
                assertEquals(qualifiedName, (String) entity.getAttribute(ATTR_NAME_QUALIFIED_NAME), "not expected to change");
            }
        }
    }

    @Test
    public void testHiveTableNameRenameHandler() {
        // replace tableName: from hr.employees to hr.employees_backup
        AttributeTransform p = new AttributeTransform();
        p.addCondition("hive_db.name", "EQUALS: hr");
        p.addCondition("hive_table.name", "EQUALS: employees");
        p.addAction("hive_table.name", "SET: employees_backup");

        List<BaseEntityHandler> handlers = initializeHandlers(Collections.singletonList(p));

        for (AtlasEntity entity : getAllEntities()) {
            String  qualifiedName            = (String) entity.getAttribute(ATTR_NAME_QUALIFIED_NAME);
            boolean startsWithHrEmployeesDot = qualifiedName.startsWith("hr.employees."); // for columns
            boolean startsWithHrEmployeesAt  = qualifiedName.startsWith("hr.employees@"); // for tables

            applyTransforms(entity, handlers);

            if (startsWithHrEmployeesDot) {
                assertTrue(((String) entity.getAttribute(ATTR_NAME_QUALIFIED_NAME)).startsWith("hr.employees_backup."));
            } else if (startsWithHrEmployeesAt) {
                assertTrue(((String) entity.getAttribute(ATTR_NAME_QUALIFIED_NAME)).startsWith("hr.employees_backup@"));
            } else {
                assertEquals(qualifiedName, (String) entity.getAttribute(ATTR_NAME_QUALIFIED_NAME), "not expected to change");
            }
        }
    }

    @Test
    public void testHiveColumnNameRenameHandler() {
        // replace columnName: from hr.employees.age to hr.employees.age_backup
        AttributeTransform p = new AttributeTransform();
        p.addCondition("hive_db.name", "EQUALS: hr");
        p.addCondition("hive_table.name", "EQUALS: employees");
        p.addCondition("hive_column.name", "EQUALS: age");
        p.addAction("hive_column.name", "SET: age_backup");

        List<BaseEntityHandler> handlers = initializeHandlers(Collections.singletonList(p));

        for (AtlasEntity entity : getAllEntities()) {
            String  qualifiedName              = (String) entity.getAttribute(ATTR_NAME_QUALIFIED_NAME);
            boolean startsWithHrEmployeesAgeAt = qualifiedName.startsWith("hr.employees.age@");

            applyTransforms(entity, handlers);

            if (startsWithHrEmployeesAgeAt) {
                assertTrue(((String) entity.getAttribute(ATTR_NAME_QUALIFIED_NAME)).startsWith("hr.employees.age_backup@"));
            } else {
                assertEquals(qualifiedName, (String) entity.getAttribute(ATTR_NAME_QUALIFIED_NAME), "not expected to change");
            }
        }
    }

    @Test
    public void verifyAddClassification() {
        AtlasEntityTransformer transformer = new AtlasEntityTransformer(Collections.singletonMap("hive_db.qualifiedName", "EQUALS: hr@cl1"),
                                                                        Collections.singletonMap("Referenceable.", "ADD_CLASSIFICATION: replicated"),
                                                                        getTransformerContext());

        List<BaseEntityHandler> handlers = Collections.singletonList(new BaseEntityHandler(Collections.singletonList(transformer)));

        assertApplyTransform(handlers);
    }

    @Test
    public void verifyAddClassificationUsingScope() {
        AtlasExportRequest exportRequest = new AtlasExportRequest();

        exportRequest.setItemsToExport(Collections.singletonList(new AtlasObjectId("hive_db", Collections.singletonMap(ATTR_NAME_QUALIFIED_NAME, "hr@cl1"))));

        AtlasEntityTransformer transformer = new AtlasEntityTransformer(Collections.singletonMap("Referenceable.", "topLevel: "),
                                                                        Collections.singletonMap("Referenceable", "ADD_CLASSIFICATION: replicated"),
                                                                        new TransformerContext(getTypeRegistry(), null, exportRequest));

        List<BaseEntityHandler> handlers = Collections.singletonList(new BaseEntityHandler(Collections.singletonList(transformer)));

        assertApplyTransform(handlers);
    }

    @Test
    public void verifyEntityTypeInAttributeName() {
        AttributeTransform p = new AttributeTransform();
        p.addAction("Asset.name", "SET: renamed");

        List<BaseEntityHandler> handlers = initializeHandlers(Collections.singletonList(p));

        AtlasEntity assetEntity    = new AtlasEntity(TYPENAME_ASSET, "name", "originalName");
        AtlasEntity assetSubEntity = new AtlasEntity(HIVE_DATABASE, "name", "originalName");
        AtlasEntity nonAssetEntity = new AtlasEntity(TYPENAME_NON_ASSET, "name", "originalName");

        applyTransforms(assetEntity, handlers);
        applyTransforms(assetSubEntity, handlers);
        applyTransforms(nonAssetEntity, handlers);

        assertEquals((String) assetEntity.getAttribute("name"), "renamed", "Asset.name expected to be updated for Asset entity");
        assertEquals((String) assetSubEntity.getAttribute("name"), "renamed", "Asset.name expected to be updated for Asset sub-type entity");
        assertEquals((String) nonAssetEntity.getAttribute("name"), "originalName", "Asset.name expected to be not updated for non-Asset type entity");
    }

    @Test
    public void verifyNoEntityTypeInAttributeName() {
        AttributeTransform p = new AttributeTransform();
        p.addAction("name", "SET: renamed");

        List<BaseEntityHandler> handlers = initializeHandlers(Collections.singletonList(p));

        AtlasEntity assetEntity    = new AtlasEntity(TYPENAME_ASSET, "name", "originalName");
        AtlasEntity assetSubEntity = new AtlasEntity(HIVE_DATABASE, "name", "originalName");
        AtlasEntity nonAssetEntity = new AtlasEntity(TYPENAME_NON_ASSET, "name", "originalName");

        applyTransforms(assetEntity, handlers);
        applyTransforms(assetSubEntity, handlers);
        applyTransforms(nonAssetEntity, handlers);

        assertEquals((String) assetEntity.getAttribute("name"), "renamed", "name expected to be updated for Asset entity");
        assertEquals((String) assetSubEntity.getAttribute("name"), "renamed", "name expected to be updated for Asset sub-type entity");
        assertEquals((String) nonAssetEntity.getAttribute("name"), "renamed", "name expected to be not updated for non-Asset type entity");
    }

    @Test
    public void qualifiedNameDifferentFromName() throws IOException {
        final String expectedQualifiedName = "test_partition_bootstrap_target.temptable_temp-5e93084a-f2d1-46f7-8f8f-30ffb859d2be@mycluster1";

        AtlasEntity.AtlasEntityWithExtInfo entityWithExtInfo = readObjectFromJson("entity1", AtlasEntity.AtlasEntityWithExtInfo.class);
        List<BaseEntityHandler> handlers = BaseEntityHandler.fromJson(getStringFromFile("transform1"), null);

        assertNotNull(handlers);
        assertEquals(handlers.size(), 4);
        assertNotNull(entityWithExtInfo);

        applyTransforms(entityWithExtInfo.getEntity(), handlers);
        assertEquals(entityWithExtInfo.getEntity().getAttribute(ATTR_NAME_QUALIFIED_NAME), expectedQualifiedName);
    }

    @Test
    public void emptyQualifiedNameHandling() throws IOException {
        AtlasEntity.AtlasEntityWithExtInfo entityWithExtInfo = readObjectFromJson("entity1", AtlasEntity.AtlasEntityWithExtInfo.class);
        entityWithExtInfo.getEntity().setAttribute(QUALIFIED_NAME_ATTRIBUTE, "");
        List<BaseEntityHandler> handlers = BaseEntityHandler.fromJson("[{\"conditions\":{\"hive_db.clusterName\":\"EQUALS: mycluster0\"},\"action\":{\"hive_db.clusterName\":\"SET: mycluster1\"}}]", null);

        assertNotNull(handlers);
        assertEquals(handlers.size(), 4);
        assertNotNull(entityWithExtInfo);

        applyTransforms(entityWithExtInfo.getEntity(), handlers);
        assertEquals(entityWithExtInfo.getEntity().getAttribute(ATTR_NAME_QUALIFIED_NAME), "");
    }

    private void assertApplyTransform(List<BaseEntityHandler> handlers) {
        for (AtlasEntity entity : getAllEntities()) {
            applyTransforms(entity, handlers);

            if(entity.getTypeName().equals("hive_db") && entity.getAttribute(ATTR_NAME_QUALIFIED_NAME).equals("hr@cl1")) {
                assertNotNull(entity.getClassifications());
            } else{
                assertNull(entity.getClassifications());
            }
        }
    }

    private List<BaseEntityHandler> initializeHandlers(List<AttributeTransform> params) {
        return BaseEntityHandler.createEntityHandlers(params, getTransformerContext());
    }

    private void applyTransforms(AtlasEntity entity, List<BaseEntityHandler> handlers) {
        for (BaseEntityHandler handler : handlers) {
            handler.transform(entity);
        }
    }

    private TransformerContext getTransformerContext() {
        return new TransformerContext(getTypeRegistry(), null, null);
    }

    private AtlasTypeRegistry getTypeRegistry() {
        AtlasTypeRegistry ret = new AtlasTypeRegistry();

        AtlasEntityDef defReferenceable = new AtlasEntityDef(TYPENAME_REFERENCEABLE);
        AtlasEntityDef defAsset         = new AtlasEntityDef(TYPENAME_ASSET);
        AtlasEntityDef defHdfsPath      = new AtlasEntityDef(HDFS_PATH);
        AtlasEntityDef defHiveDb        = new AtlasEntityDef(HIVE_DATABASE);
        AtlasEntityDef defHiveTable     = new AtlasEntityDef(HIVE_TABLE);
        AtlasEntityDef defHiveColumn    = new AtlasEntityDef(HIVE_COLUMN);
        AtlasEntityDef defHiveStorDesc  = new AtlasEntityDef(HIVE_STORAGE_DESCRIPTOR);
        AtlasEntityDef defNonAsset      = new AtlasEntityDef(TYPENAME_NON_ASSET);

        defAsset.addSuperType(TYPENAME_REFERENCEABLE);
        defHdfsPath.addSuperType(TYPENAME_ASSET);
        defHiveDb.addSuperType(TYPENAME_ASSET);
        defHiveTable.addSuperType(TYPENAME_ASSET);
        defHiveColumn.addSuperType(TYPENAME_ASSET);
        defNonAsset.addSuperType(TYPENAME_REFERENCEABLE);

        AtlasTypesDef typesDef = new AtlasTypesDef();

        typesDef.setEntityDefs(Arrays.asList(defReferenceable, defAsset, defHdfsPath, defHiveDb, defHiveTable, defHiveColumn, defHiveStorDesc, defNonAsset));

        try {
            AtlasTypeRegistry.AtlasTransientTypeRegistry ttr = ret.lockTypeRegistryForUpdate();

            ttr.addTypes(typesDef);

            ret.releaseTypeRegistryForUpdate(ttr, true);
        } catch (AtlasBaseException excp) {
            LOG.warn("failed to initialize type-registry", excp);
        }

        return ret;
    }

    private List<AtlasEntity> getHdfsPathEntities() {
        List<AtlasEntity> ret = new ArrayList<>();

        for (String clusterName : CLUSTER_NAMES) {
            ret.add(getHdfsPathEntity1(clusterName));
            ret.add(getHdfsPathEntity2(clusterName));
        }

        return ret;
    }

    private List<AtlasEntity> getAllEntities() {
        List<AtlasEntity> ret = new ArrayList<>();

        for (String clusterName : CLUSTER_NAMES) {
            ret.add(getHdfsPathEntity1(clusterName));
            ret.add(getHdfsPathEntity2(clusterName));

            for (String databaseName : DATABASE_NAMES) {
                ret.add(getHiveDbEntity(clusterName, databaseName));

                for (String tableName : TABLE_NAMES) {
                    ret.add(getHiveTableEntity(clusterName, databaseName, tableName));
                    ret.add(getHiveStorageDescriptorEntity(clusterName, databaseName, tableName));

                    for (String columnName : COLUMN_NAMES) {
                        ret.add(getHiveColumnEntity(clusterName, databaseName, tableName, columnName));
                    }
                }
            }
        }

        return ret;
    }

    private AtlasEntity getHdfsPathEntity1(String clusterName) {
        AtlasEntity entity = new AtlasEntity(HDFS_PATH);

        entity.setAttribute("name", "/aa/bb/employee");
        entity.setAttribute("path", "hdfs://localhost.localdomain:8020/aa/bb/employee");
        entity.setAttribute(ATTR_NAME_QUALIFIED_NAME, "hdfs://localhost.localdomain:8020/aa/bb/employee@" + clusterName);
        entity.setAttribute("clusterName", clusterName);
        entity.setAttribute("isSymlink", false);
        entity.setAttribute("modifiedTime", 0);
        entity.setAttribute("isFile", false);
        entity.setAttribute("numberOfReplicas", 0);
        entity.setAttribute("createTime", 0);
        entity.setAttribute("fileSize", 0);

        return entity;
    }

    private AtlasEntity getHdfsPathEntity2(String clusterName) {
        AtlasEntity entity = new AtlasEntity(HDFS_PATH);

        entity.setAttribute("name", "/cc/dd/employee");
        entity.setAttribute("path", "hdfs://localhost.localdomain:8020/cc/dd/employee");
        entity.setAttribute(ATTR_NAME_QUALIFIED_NAME, "hdfs://localhost.localdomain:8020/cc/dd/employee@" + clusterName);
        entity.setAttribute("clusterName", clusterName);
        entity.setAttribute("isSymlink", false);
        entity.setAttribute("modifiedTime", 0);
        entity.setAttribute("isFile", false);
        entity.setAttribute("numberOfReplicas", 0);
        entity.setAttribute("createTime", 0);
        entity.setAttribute("fileSize", 0);

        return entity;
    }

    private AtlasEntity getHiveDbEntity(String clusterName, String dbName) {
        AtlasEntity entity = new AtlasEntity(TransformationConstants.HIVE_DATABASE);

        entity.setAttribute("name", dbName);
        entity.setAttribute(ATTR_NAME_QUALIFIED_NAME, dbName + "@" + clusterName);
        entity.setAttribute("location", "hdfs://localhost.localdomain:8020/warehouse/tablespace/managed/hive/" + dbName + ".db");
        entity.setAttribute("clusterName", clusterName);
        entity.setAttribute("owner", "hive");
        entity.setAttribute("ownerType", "USER");

        return entity;
    }

    private AtlasEntity getHiveTableEntity(String clusterName, String dbName, String tableName) {
        String qualifiedName = dbName + "." + tableName + "@" + clusterName;

        AtlasEntity entity = new AtlasEntity(TransformationConstants.HIVE_TABLE);

        entity.setAttribute("name", tableName);
        entity.setAttribute(ATTR_NAME_QUALIFIED_NAME, qualifiedName);
        entity.setAttribute("owner", "hive");
        entity.setAttribute("temporary", false);
        entity.setAttribute("lastAccessTime", "1535656355000");
        entity.setAttribute("tableType", "EXTERNAL_TABLE");
        entity.setAttribute("createTime", "1535656355000");
        entity.setAttribute("retention", 0);
        entity.setAttribute("replicatedTo", "[{\"guid\":\"f378cfa5-c4aa-4699-a733-8f11d2f089cd\",\"typeName\":\"AtlasServer\"},{\"guid\":\"58e42789-ea3e-4eaa-a0c4-d38d8632e548\",\"typeName\":\"AtlasServer\"}]");
        entity.setAttribute("replicatedFrom", "[{\"guid\":\"f378cfa5-c4aa-4699-a733-8f11d2f089cd\",\"typeName\":\"AtlasServer\"},{\"guid\":\"58e42789-ea3e-4eaa-a0c4-d38d8632e548\",\"typeName\":\"AtlasServer\"}]");

        return entity;
    }

    private AtlasEntity getHiveStorageDescriptorEntity(String clusterName, String dbName, String tableName) {
        String qualifiedName = "hdfs://localhost.localdomain:8020/warehouse/tablespace/managed/hive/" + dbName + ".db" + "/" + tableName;

        AtlasEntity entity = new AtlasEntity(TransformationConstants.HIVE_STORAGE_DESCRIPTOR);

        entity.setAttribute(ATTR_NAME_QUALIFIED_NAME, dbName + "." + tableName + "@" + clusterName + "_storage");
        entity.setAttribute("storedAsSubDirectories", false);
        entity.setAttribute("location", qualifiedName);
        entity.setAttribute("compressed", false);
        entity.setAttribute("inputFormat", "org.apache.hadoop.mapred.TextInputFormat");
        entity.setAttribute("outputFormat", "org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat");
        entity.setAttribute("numBuckets", -1);

        return entity;
    }

    private AtlasEntity getHiveColumnEntity(String clusterName, String dbName, String tableName, String columnName) {
        String qualifiedName = dbName + "." + tableName + "." + columnName + "@" + clusterName;

        AtlasEntity entity = new AtlasEntity(TransformationConstants.HIVE_COLUMN);

        entity.setAttribute("owner", "hive");
        entity.setAttribute(ATTR_NAME_QUALIFIED_NAME, qualifiedName);
        entity.setAttribute("name", columnName);
        entity.setAttribute("position", 1);
        entity.setAttribute("type", "string");

        return entity;
    }

    private AtlasEntity getNonAssetEntity() {
        return new AtlasEntity(TYPENAME_NON_ASSET);
    }

    public static <T> T readObjectFromJson(String filename, Class<T> objectClass) throws IOException {
        String json = getStringFromFile(filename);
        return AtlasType.fromJson(json, objectClass);
    }

    private static String getStringFromFile(String filename) throws IOException {
        final String userDir = System.getProperty("user.dir");
        return FileUtils.readFileToString(new File(getTestJsonPath(userDir, filename)));
    }

    private static String getTestJsonPath(String startPath, String fileName) {
        return startPath + "/src/test/resources/json/" + fileName + ".json";
    }
}