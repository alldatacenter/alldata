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

import org.apache.atlas.AtlasClientV2;
import org.apache.atlas.AtlasServiceException;
import org.apache.atlas.model.instance.AtlasClassification;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntityWithExtInfo;
import org.apache.atlas.model.instance.AtlasEntityHeader;
import org.apache.atlas.model.instance.AtlasStruct;
import org.apache.atlas.model.instance.EntityMutationResponse;
import org.apache.atlas.model.instance.EntityMutations;
import org.apache.atlas.type.AtlasTypeUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.Arrays.asList;
import static org.apache.atlas.examples.sampleapp.SampleAppConstants.*;
import static org.apache.atlas.type.AtlasTypeUtil.toAtlasRelatedObjectId;
import static org.apache.atlas.type.AtlasTypeUtil.toAtlasRelatedObjectIds;

public class EntityExample {
    private static final String DATABASE_NAME                = "employee_db_entity";
    private static final String TABLE_NAME                   = "employee_table_entity";
    private static final String PROCESS_NAME                 = "employee_process_entity";
    private static final String METADATA_NAMESPACE_SUFFIX    = "@cl1";
    private static final String MANAGED_TABLE                = "Managed";
    private static final String ATTR_NAME                    = "name";
    private static final String ATTR_DESCRIPTION             = "description";
    private static final String ATTR_QUALIFIED_NAME          = "qualifiedName";
    private static final String REFERENCEABLE_ATTRIBUTE_NAME = ATTR_QUALIFIED_NAME;
    private static final String COLUMN_TIME_ID               = "time_id";
    private static final String COLUMN_CUSTOMER_ID           = "customer_id";
    private static final String COLUMN_COMPANY_ID            = "company_id";

    private final AtlasClientV2     client;
    private       AtlasEntity       dbEntity;
    private       AtlasEntity       tableEntityUS;
    private       AtlasEntity       tableEntityCanada;
    private       AtlasEntityHeader loadProcess;

    EntityExample(AtlasClientV2 client) {
        this.client = client;
    }

    public void createEntities() throws Exception {
        if (dbEntity == null) {
            dbEntity = createDatabaseEntity(DATABASE_NAME);

            SampleApp.log("Created entity: typeName=" + dbEntity.getTypeName() + ", qualifiedName=" + dbEntity.getAttribute(ATTR_QUALIFIED_NAME) + ", guid=" + dbEntity.getGuid());
        }

        if (tableEntityCanada == null) {
            tableEntityCanada = createTableEntity(TABLE_NAME + "_CANADA");

            SampleApp.log("Created entity: typeName=" + tableEntityCanada.getTypeName() + ", qualifiedName=" + tableEntityCanada.getAttribute(ATTR_QUALIFIED_NAME) + ", guid=" + tableEntityCanada.getGuid());
        }

        if (tableEntityUS == null) {
            tableEntityUS = createTableEntity(TABLE_NAME + "_US");

            SampleApp.log("Created entity: typeName=" + tableEntityUS.getTypeName() + ", qualifiedName=" + tableEntityUS.getAttribute(ATTR_QUALIFIED_NAME) + ", guid=" + tableEntityUS.getGuid());
        }

        if (loadProcess == null) {
            loadProcess = createProcessEntity(PROCESS_NAME);

            SampleApp.log("Created entity: typeName=" + loadProcess.getTypeName() + ", qualifiedName=" + loadProcess.getAttribute(ATTR_QUALIFIED_NAME) + ", guid=" + loadProcess.getGuid());
        }
    }

    public AtlasEntity getTableEntity() {
        return tableEntityUS;
    }

    public void getEntityByGuid(String entityGuid) throws Exception {
        AtlasEntityWithExtInfo entity = client.getEntityByGuid(entityGuid);

        if (entity != null) {
            SampleApp.log("Retrieved entity with guid=" + entityGuid);
            SampleApp.log("  " + entity);
        }
    }

    public void deleteEntities() throws Exception {
        client.deleteEntityByGuid(loadProcess.getGuid());

        SampleApp.log("Deleted entity: guid=" + loadProcess.getGuid());

        List<String> entityGuids = Arrays.asList(tableEntityUS.getGuid(), tableEntityCanada.getGuid(), dbEntity.getGuid());

        client.deleteEntitiesByGuids(entityGuids);

        SampleApp.log("Deleted entities:");
        for (String entityGuid : entityGuids) {
            SampleApp.log("  guid=" + entityGuid);
        }
    }

    private AtlasEntity createTableEntity(String tableName) throws Exception {
        return createHiveTable(dbEntity, tableName, MANAGED_TABLE,
                Arrays.asList(createColumn(COLUMN_TIME_ID, "int", "time id"),
                              createColumn(COLUMN_CUSTOMER_ID, "int", "customer id", SampleAppConstants.PII_TAG),
                              createColumn(COLUMN_COMPANY_ID, "double", "company id", SampleAppConstants.FINANCE_TAG)),
                SampleAppConstants.METRIC_TAG);
    }

    private AtlasEntityHeader createProcessEntity(String processName) throws Exception {
        return createProcess(processName, "hive query for monthly avg salary", "user ETL",
                asList(tableEntityUS),
                asList(tableEntityCanada),
                "create table as select ", "plan", "id", "graph", SampleAppConstants.CLASSIFIED_TAG);
    }

    private AtlasEntityHeader createProcess(String name, String description, String user, List<AtlasEntity> inputs, List<AtlasEntity> outputs,
                                            String queryText, String queryPlan, String queryId, String queryGraph, String... classificationNames) throws Exception {

        AtlasEntity entity = new AtlasEntity(SampleAppConstants.PROCESS_TYPE);

        entity.setAttribute(ATTR_NAME, name);
        entity.setAttribute(REFERENCEABLE_ATTRIBUTE_NAME, name + METADATA_NAMESPACE_SUFFIX);
        entity.setAttribute(ATTR_DESCRIPTION, description);
        entity.setAttribute(ATTR_USERNAME, user);
        entity.setAttribute(ATTR_START_TIME, System.currentTimeMillis());
        entity.setAttribute(ATTR_END_TIME, System.currentTimeMillis() + 10000);
        entity.setAttribute(ATTR_QUERY_TEXT, queryText);
        entity.setAttribute(ATTR_QUERY_PLAN, queryPlan);
        entity.setAttribute(ATTR_QUERY_ID, queryId);
        entity.setAttribute(ATTR_QUERY_GRAPH, queryGraph);
        entity.setAttribute(ATTR_OPERATION_TYPE, "testOperation");

        entity.setRelationshipAttribute(ATTR_INPUTS, toAtlasRelatedObjectIds(inputs));
        entity.setRelationshipAttribute(ATTR_OUTPUTS, toAtlasRelatedObjectIds(outputs));

        entity.setClassifications(toAtlasClassifications(classificationNames));

        return createEntity(new AtlasEntityWithExtInfo(entity));
    }

    private AtlasEntity createColumn(String name, String dataType, String comment, String... classificationNames) {
        AtlasEntity ret = new AtlasEntity(SampleAppConstants.COLUMN_TYPE);

        ret.setAttribute(ATTR_NAME, name);
        ret.setAttribute(REFERENCEABLE_ATTRIBUTE_NAME, name + METADATA_NAMESPACE_SUFFIX);
        ret.setAttribute(ATTR_DATA_TYPE, dataType);
        ret.setAttribute(ATTR_COMMENT, comment);

        ret.setClassifications(toAtlasClassifications(classificationNames));

        return ret;
    }

    private List<AtlasClassification> toAtlasClassifications(String[] classificationNames) {
        List<AtlasClassification> ret = new ArrayList<>();

        if (classificationNames != null) {
            for (String classificationName : classificationNames) {
                ret.add(new AtlasClassification(classificationName));
            }
        }

        return ret;
    }

    private AtlasEntityHeader createEntity(AtlasEntityWithExtInfo atlasEntityWithExtInfo) {
        EntityMutationResponse entity;

        try {
            entity = client.createEntity(atlasEntityWithExtInfo);

            if (entity != null && entity.getEntitiesByOperation(EntityMutations.EntityOperation.CREATE) != null) {
                List<AtlasEntityHeader> list = entity.getEntitiesByOperation(EntityMutations.EntityOperation.CREATE);

                if (list.size() > 0) {
                    return entity.getEntitiesByOperation(EntityMutations.EntityOperation.CREATE).get(0);
                }
            }
        } catch (AtlasServiceException e) {
            SampleApp.log("failed in create entity");
            e.printStackTrace();
        }

        return null;
    }

    private AtlasEntity createDatabaseEntity(String dbName) {
        AtlasEntity       hiveDBInstance = createHiveDBInstance(dbName);
        AtlasEntityHeader entityHeader   = createEntity(new AtlasEntityWithExtInfo(hiveDBInstance));

        if (entityHeader != null && entityHeader.getGuid() != null) {
            hiveDBInstance.setGuid(entityHeader.getGuid());
        }

        return hiveDBInstance;
    }

    protected AtlasEntity createHiveDBInstance(String dbName) {
        AtlasEntity entity = new AtlasEntity(SampleAppConstants.DATABASE_TYPE);

        entity.setAttribute(ATTR_NAME, dbName);
        entity.setAttribute(ATTR_DESCRIPTION, "employee database");
        entity.setAttribute(METADATA_NAMESPACE_SUFFIX, "employeeCluster");
        entity.setAttribute(REFERENCEABLE_ATTRIBUTE_NAME, dbName + METADATA_NAMESPACE_SUFFIX);
        entity.setAttribute(ATTR_OWNER, "user");
        entity.setAttribute(ATTR_LOCATION_URI, "/tmp");
        entity.setAttribute(ATTR_CREATE_TIME, 1000);

        return entity;
    }

    private AtlasEntity createHiveTable(AtlasEntity database, String tableName, String tableType, List<AtlasEntity> columns, String... classificationNames) throws Exception {
        AtlasEntityWithExtInfo entityWithExtInfo = new AtlasEntityWithExtInfo();

        AtlasEntity hiveTableInstance = createHiveTable(database, tableName, tableType, classificationNames);
        entityWithExtInfo.setEntity(hiveTableInstance);
        hiveTableInstance.setRelationshipAttribute(ATTR_COLUMNS, toAtlasRelatedObjectIds(columns));

        for (AtlasEntity column : columns) {
            column.setRelationshipAttribute(ATTR_TABLE, toAtlasRelatedObjectId(hiveTableInstance));
            entityWithExtInfo.addReferredEntity(column);
        }

        AtlasEntityHeader createdHeader = createEntity(entityWithExtInfo);

        if (createdHeader != null && createdHeader.getGuid() != null) {
            hiveTableInstance.setGuid(createdHeader.getGuid());
        }

        return hiveTableInstance;
    }

    private AtlasEntity createHiveTable(AtlasEntity database, String tableName, String tableType, String... classificationNames) throws Exception {
        AtlasEntity table = new AtlasEntity(SampleAppConstants.TABLE_TYPE);

        table.setAttribute(ATTR_NAME, tableName);
        table.setAttribute(REFERENCEABLE_ATTRIBUTE_NAME, database.getAttribute(ATTR_NAME) + "." + tableName + METADATA_NAMESPACE_SUFFIX);
        table.setAttribute(ATTR_TABLE_TYPE, tableType);
        table.setRelationshipAttribute(ATTR_DB, AtlasTypeUtil.getAtlasRelatedObjectId(database, TABLE_DATABASE_TYPE));

        table.setAttribute(ATTR_DESCRIPTION, "emp table");
        table.setAttribute(ATTR_LAST_ACCESS_TIME, "2014-07-11T08:00:00.000Z");
        table.setAttribute(ATTR_LEVEL, 2);
        table.setAttribute(ATTR_COMPRESSED, false);
        table.setClassifications(toAtlasClassifications(classificationNames));

        AtlasStruct serde1 = new AtlasStruct(STRUCT_TYPE_SERDE);

        serde1.setAttribute(ATTR_NAME, "serde1");
        serde1.setAttribute(ATTR_SERDE, "serde1");
        table.setAttribute(ATTR_SERDE1, serde1);

        AtlasStruct serde2 = new AtlasStruct(STRUCT_TYPE_SERDE);
        serde2.setAttribute(ATTR_NAME, "serde2");
        serde2.setAttribute(ATTR_SERDE, "serde2");
        table.setAttribute(ATTR_SERDE2, serde2);

        return table;
    }
}