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

package org.apache.atlas.hive.hook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Joiner;
import com.sun.jersey.api.client.ClientResponse;
import org.apache.atlas.AtlasClient;
import org.apache.atlas.AtlasServiceException;
import org.apache.atlas.hive.HiveITBase;
import org.apache.atlas.hive.bridge.HiveMetaStoreBridge;
import org.apache.atlas.hive.hook.events.BaseHiveEvent;
import org.apache.atlas.hive.model.HiveDataTypes;
import org.apache.atlas.model.instance.*;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntityWithExtInfo;
import org.apache.atlas.model.lineage.AtlasLineageInfo;
import org.apache.atlas.model.typedef.AtlasClassificationDef;
import org.apache.atlas.model.typedef.AtlasEntityDef;
import org.apache.atlas.model.typedef.AtlasTypesDef;
import org.apache.atlas.type.AtlasTypeUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.TableType;
import org.apache.hadoop.hive.metastore.api.hive_metastoreConstants;
import org.apache.hadoop.hive.ql.Driver;
import org.apache.hadoop.hive.ql.hooks.Entity;
import org.apache.hadoop.hive.ql.hooks.ReadEntity;
import org.apache.hadoop.hive.ql.hooks.WriteEntity;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.ql.metadata.Table;
import org.apache.hadoop.hive.ql.plan.HiveOperation;
import org.apache.hadoop.hive.ql.session.SessionState;
import org.apache.hadoop.security.UserGroupInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.text.ParseException;
import java.util.*;

import static org.apache.atlas.AtlasClient.NAME;
import static org.apache.atlas.hive.hook.events.BaseHiveEvent.*;
import static org.testng.Assert.*;
import static org.testng.AssertJUnit.assertEquals;

public class HiveHookIT extends HiveITBase {
    private static final Logger LOG = LoggerFactory.getLogger(HiveHookIT.class);

    private static final String PART_FILE      = "2015-01-01";
    private static final String PATH_TYPE_NAME = "Path";

    private Driver driverWithNoHook;

    @BeforeClass
    public void setUp() throws Exception {
        //  initialize 'driverWithNoHook' with HiveServer2 hook and HiveMetastore hook disabled
        HiveConf conf = new HiveConf();
        conf.set("hive.exec.post.hooks", "");
        conf.set("hive.metastore.event.listeners", "");

        SessionState ss = new SessionState(conf);
        ss = SessionState.start(ss);
        SessionState.setCurrentSessionState(ss);

        // Initialize 'driverWithNoHook'  with HS2 hook disabled and HMS hook disabled.
        driverWithNoHook = new Driver(conf);

        super.setUp();
    }

    @Test
    public void testCreateDatabase() throws Exception {
        String dbName = "db" + random();

        runCommand("create database " + dbName + " WITH DBPROPERTIES ('p1'='v1', 'p2'='v2')");

        String      dbId       = assertDatabaseIsRegistered(dbName);
        AtlasEntity dbEntity   = atlasClientV2.getEntityByGuid(dbId).getEntity();
        Map         params     = (Map) dbEntity.getAttribute(ATTRIBUTE_PARAMETERS);
        List        ddlQueries = (List) dbEntity.getRelationshipAttribute(ATTRIBUTE_DDL_QUERIES);

        Assert.assertNotNull(ddlQueries);
        Assert.assertEquals(ddlQueries.size(),1);

        Assert.assertNotNull(params);
        Assert.assertEquals(params.size(), 2);
        Assert.assertEquals(params.get("p1"), "v1");

        //There should be just one entity per dbname
        runCommandWithDelay("drop database " + dbName, 3000);
        assertDatabaseIsNotRegistered(dbName);

        runCommandWithDelay("create database " + dbName, 3000);
        dbId = assertDatabaseIsRegistered(dbName);

        //assert on qualified name
        dbEntity = atlasClientV2.getEntityByGuid(dbId).getEntity();

        Assert.assertEquals(dbEntity.getAttribute(ATTRIBUTE_QUALIFIED_NAME) , dbName.toLowerCase() + "@" + CLUSTER_NAME);
    }

    @Test
    public void testPathEntityDefAvailable() throws Exception {
        //Check if Path entity definition created or not
        AtlasEntityDef pathEntityDef = atlasClientV2.getEntityDefByName("Path");
        assertNotNull(pathEntityDef);
    }

    @Test
    public void testCreateDatabaseWithLocation() throws Exception {
        String dbName = dbName();
        String query  = "CREATE DATABASE " + dbName;

        runCommand(query);
        String dbId = assertDatabaseIsRegistered(dbName);

        //HDFS Location
        String hdfsLocation = "hdfs://localhost:8020/warehouse/tablespace/external/hive/reports.db";
        alterDatabaseLocation(dbName, hdfsLocation);
        assertDatabaseLocationRelationship(dbId);
    }

    //alter database location
    public void alterDatabaseLocation(String dbName, String location) throws Exception {
        int timeDelay = 5000;
        String query = String.format("ALTER DATABASE %s SET LOCATION \"%s\"", dbName, location);
        runCommandWithDelay(query, timeDelay);
    }

    public void assertDatabaseLocationRelationship(String dbId) throws Exception {
        AtlasEntity    dbEntity      = atlasClientV2.getEntityByGuid(dbId).getEntity();
        AtlasEntityDef pathEntityDef = getPathEntityDefWithAllSubTypes();

        assertTrue(dbEntity.hasAttribute(ATTRIBUTE_LOCATION));

        assertNotNull(dbEntity.getAttribute(ATTRIBUTE_LOCATION));

        assertNotNull(dbEntity.getRelationshipAttribute(ATTRIBUTE_LOCATION_PATH));

        AtlasObjectId locationEntityObject = toAtlasObjectId(dbEntity.getRelationshipAttribute(ATTRIBUTE_LOCATION_PATH));
        assertTrue(pathEntityDef.getSubTypes().contains(locationEntityObject.getTypeName()));
    }

    public AtlasEntityDef getPathEntityDefWithAllSubTypes() throws Exception {
        Set<String>     possiblePathSubTypes = new HashSet<>(Arrays.asList("fs_path", "hdfs_path", "aws_s3_pseudo_dir", "aws_s3_v2_directory", "adls_gen2_directory"));
        AtlasEntityDef  pathEntityDef        = atlasClientV2.getEntityDefByName(PATH_TYPE_NAME);

        if(pathEntityDef == null) {
            pathEntityDef = new AtlasEntityDef(PATH_TYPE_NAME);
        }

        pathEntityDef.setSubTypes(possiblePathSubTypes);

        return pathEntityDef;
    }

    @Test
    public void testCreateTable() throws Exception {
        String tableName = tableName();
        String dbName    = createDatabase();
        String colName   = columnName();

        runCommand("create table " + dbName + "." + tableName + "(" + colName + " int, name string)");

        String      tableId   = assertTableIsRegistered(dbName, tableName);
        String      colId     = assertColumnIsRegistered(HiveMetaStoreBridge.getColumnQualifiedName(HiveMetaStoreBridge.getTableQualifiedName(CLUSTER_NAME, dbName, tableName), colName)); //there is only one instance of column registered
        AtlasEntity colEntity = atlasClientV2.getEntityByGuid(colId).getEntity();
        AtlasEntity tblEntity = atlasClientV2.getEntityByGuid(tableId).getEntity();

        Assert.assertEquals(colEntity.getAttribute(ATTRIBUTE_QUALIFIED_NAME), String.format("%s.%s.%s@%s", dbName.toLowerCase(), tableName.toLowerCase(), colName.toLowerCase(), CLUSTER_NAME));
        Assert.assertNotNull(colEntity.getAttribute(ATTRIBUTE_TABLE));

        Assert.assertNotNull(tblEntity.getRelationshipAttribute(ATTRIBUTE_DDL_QUERIES));
        Assert.assertEquals(((List)tblEntity.getRelationshipAttribute(ATTRIBUTE_DDL_QUERIES)).size(), 1);

        AtlasObjectId tblObjId = toAtlasObjectId(colEntity.getAttribute(ATTRIBUTE_TABLE));

        Assert.assertEquals(tblObjId.getGuid(), tableId);

        //assert that column.owner = table.owner
        AtlasEntity tblEntity1 = atlasClientV2.getEntityByGuid(tableId).getEntity();
        AtlasEntity colEntity1 = atlasClientV2.getEntityByGuid(colId).getEntity();

        assertEquals(tblEntity1.getAttribute(ATTRIBUTE_OWNER), colEntity1.getAttribute(ATTRIBUTE_OWNER));

        //create table where db is not registered
        tableName = createTable();
        tableId   = assertTableIsRegistered(DEFAULT_DB, tableName);

        AtlasEntity tblEntity2 = atlasClientV2.getEntityByGuid(tableId).getEntity();

        Assert.assertEquals(tblEntity2.getAttribute(ATTRIBUTE_TABLE_TYPE), TableType.MANAGED_TABLE.name());
        Assert.assertEquals(tblEntity2.getAttribute(ATTRIBUTE_COMMENT), "table comment");

        String entityName = HiveMetaStoreBridge.getTableQualifiedName(CLUSTER_NAME, DEFAULT_DB, tableName);

        Assert.assertEquals(tblEntity2.getAttribute(AtlasClient.NAME), tableName.toLowerCase());
        Assert.assertEquals(tblEntity2.getAttribute(ATTRIBUTE_QUALIFIED_NAME), entityName);

        Table t          = hiveMetaStoreBridge.getHiveClient().getTable(DEFAULT_DB, tableName);
        long  createTime = Long.parseLong(t.getMetadata().getProperty(hive_metastoreConstants.DDL_TIME)) * MILLIS_CONVERT_FACTOR;

        verifyTimestamps(tblEntity2, ATTRIBUTE_CREATE_TIME, createTime);
        verifyTimestamps(tblEntity2, ATTRIBUTE_LAST_ACCESS_TIME, createTime);

        final AtlasObjectId sdEntity = toAtlasObjectId(tblEntity2.getAttribute(ATTRIBUTE_STORAGEDESC));

        Assert.assertNotNull(sdEntity);

        // Assert.assertEquals(((Id) sdRef.getAttribute(HiveMetaStoreBridge.TABLE))._getId(), tableId);

        //Create table where database doesn't exist, will create database instance as well
        assertDatabaseIsRegistered(DEFAULT_DB);
    }


    private void verifyTimestamps(AtlasEntity ref, String property, long expectedTime) throws ParseException {
        //Verify timestamps.
        Object createTime = ref.getAttribute(property);

        Assert.assertNotNull(createTime);

        if (expectedTime > 0) {
            Assert.assertEquals(expectedTime, createTime);
        }
    }

    private void verifyTimestamps(AtlasEntity ref, String property) throws ParseException {
        verifyTimestamps(ref, property, 0);
    }

    //ATLAS-1321: Disable problematic tests. Need to revisit and fix them later
    @Test(enabled = false)
    public void testCreateExternalTable() throws Exception {
        String tableName     = tableName();
        String colName       = columnName();
        String pFile         = createTestDFSPath("parentPath");
        String query         = String.format("create EXTERNAL table %s.%s(%s, %s) location '%s'", DEFAULT_DB , tableName , colName + " int", "name string",  pFile);

        runCommand(query);

        String tblId         = assertTableIsRegistered(DEFAULT_DB, tableName, null, true);
        AtlasEntity tblEnity = atlasClientV2.getEntityByGuid(tblId).getEntity();
        List ddlList         = (List) tblEnity.getRelationshipAttribute(ATTRIBUTE_DDL_QUERIES);

        assertNotNull(ddlList);
        assertEquals(ddlList.size(), 1);

        String processId = assertEntityIsRegistered(HiveDataTypes.HIVE_PROCESS.getName(), ATTRIBUTE_QUALIFIED_NAME, getTableProcessQualifiedName(DEFAULT_DB, tableName), null);

        AtlasEntity processsEntity = atlasClientV2.getEntityByGuid(processId).getEntity();

        assertEquals(processsEntity.getAttribute("userName"), UserGroupInformation.getCurrentUser().getShortUserName());

        verifyTimestamps(processsEntity, "startTime");
        verifyTimestamps(processsEntity, "endTime");

        validateHDFSPaths(processsEntity, INPUTS, pFile);
    }

    private Set<ReadEntity> getInputs(String inputName, Entity.Type entityType) throws HiveException {
        final ReadEntity entity;

        if (Entity.Type.DFS_DIR.equals(entityType) || Entity.Type.LOCAL_DIR.equals(entityType)) {
            entity = new TestReadEntity(lower(new Path(inputName).toString()), entityType);
        } else {
            entity = new TestReadEntity(getQualifiedTblName(inputName), entityType);
        }

        if (entityType == Entity.Type.TABLE) {
            entity.setT(hiveMetaStoreBridge.getHiveClient().getTable(DEFAULT_DB, inputName));
        }

        return new LinkedHashSet<ReadEntity>() {{ add(entity); }};
    }

    private Set<WriteEntity> getOutputs(String inputName, Entity.Type entityType) throws HiveException {
        final WriteEntity entity;

        if (Entity.Type.DFS_DIR.equals(entityType) || Entity.Type.LOCAL_DIR.equals(entityType)) {
            entity = new TestWriteEntity(lower(new Path(inputName).toString()), entityType);
        } else {
            entity = new TestWriteEntity(getQualifiedTblName(inputName), entityType);
        }

        if (entityType == Entity.Type.TABLE) {
            entity.setT(hiveMetaStoreBridge.getHiveClient().getTable(DEFAULT_DB, inputName));
        }

        return new LinkedHashSet<WriteEntity>() {{ add(entity); }};
    }

    private void validateOutputTables(AtlasEntity processEntity, Set<WriteEntity> expectedTables) throws Exception {
        validateTables(toAtlasObjectIdList(processEntity.getAttribute(ATTRIBUTE_OUTPUTS)), expectedTables);
    }

    private void validateInputTables(AtlasEntity processEntity, Set<ReadEntity> expectedTables) throws Exception {
        validateTables(toAtlasObjectIdList(processEntity.getAttribute(ATTRIBUTE_INPUTS)), expectedTables);
    }

    private void validateTables(List<AtlasObjectId> tableIds, Set<? extends Entity> expectedTables) throws Exception {
        if (tableIds == null) {
            Assert.assertTrue(CollectionUtils.isEmpty(expectedTables));
        } else if (expectedTables == null) {
            Assert.assertTrue(CollectionUtils.isEmpty(tableIds));
        } else {
            Assert.assertEquals(tableIds.size(), expectedTables.size());

            List<String> entityQualifiedNames = new ArrayList<>(tableIds.size());
            List<String> expectedTableNames   = new ArrayList<>(expectedTables.size());

            for (AtlasObjectId tableId : tableIds) {
                AtlasEntity atlasEntity = atlasClientV2.getEntityByGuid(tableId.getGuid()).getEntity();

                entityQualifiedNames.add((String) atlasEntity.getAttribute(ATTRIBUTE_QUALIFIED_NAME));
            }

            for (Iterator<? extends Entity> iterator = expectedTables.iterator(); iterator.hasNext(); ) {
                Entity hiveEntity = iterator.next();

                expectedTableNames.add(hiveEntity.getName());
            }

            for (String entityQualifiedName : entityQualifiedNames) {
                boolean found = false;

                for (String expectedTableName : expectedTableNames) {
                    if (entityQualifiedName.startsWith(expectedTableName)) {
                        found = true;

                        break;
                    }
                }

                assertTrue(found, "Table name '" + entityQualifiedName + "' does not start with any name in the expected list " + expectedTableNames);
            }
        }
    }

    private String assertColumnIsRegistered(String colName) throws Exception {
        return assertColumnIsRegistered(colName, null);
    }

    private String assertColumnIsRegistered(String colName, AssertPredicate assertPredicate) throws Exception {
        LOG.debug("Searching for column {}", colName);

        return assertEntityIsRegistered(HiveDataTypes.HIVE_COLUMN.getName(), ATTRIBUTE_QUALIFIED_NAME, colName, assertPredicate);
    }

    private String assertSDIsRegistered(String sdQFName, AssertPredicate assertPredicate) throws Exception {
        LOG.debug("Searching for sd {}", sdQFName.toLowerCase());

        return assertEntityIsRegistered(HiveDataTypes.HIVE_STORAGEDESC.getName(), ATTRIBUTE_QUALIFIED_NAME, sdQFName.toLowerCase(), assertPredicate);
    }

    private void assertColumnIsNotRegistered(String colName) throws Exception {
        LOG.debug("Searching for column {}", colName);

        assertEntityIsNotRegistered(HiveDataTypes.HIVE_COLUMN.getName(), ATTRIBUTE_QUALIFIED_NAME, colName);
    }

    @Test
    public void testCTAS() throws Exception {
        String tableName     = createTable();
        String ctasTableName = "table" + random();
        String query         = "create table " + ctasTableName + " as select * from " + tableName;

        runCommand(query);

        final Set<ReadEntity> readEntities = getInputs(tableName, Entity.Type.TABLE);
        final Set<WriteEntity> writeEntities = getOutputs(ctasTableName, Entity.Type.TABLE);

        HiveEventContext hiveEventContext = constructEvent(query, HiveOperation.CREATETABLE_AS_SELECT, readEntities,
                writeEntities);
        AtlasEntity processEntity1 = validateProcess(hiveEventContext);
        AtlasEntity processExecutionEntity1 = validateProcessExecution(processEntity1, hiveEventContext);
        AtlasObjectId process = toAtlasObjectId(processExecutionEntity1.getRelationshipAttribute(
                BaseHiveEvent.ATTRIBUTE_PROCESS));
        Assert.assertEquals(process.getGuid(), processEntity1.getGuid());

        Assert.assertEquals(numberOfProcessExecutions(processEntity1), 1);
        assertTableIsRegistered(DEFAULT_DB, ctasTableName);
    }

    private HiveEventContext constructEvent(String query, HiveOperation op, Set<ReadEntity> inputs, Set<WriteEntity> outputs) {
        HiveEventContext event = new HiveEventContext();

        event.setQueryStr(query);
        event.setOperation(op);
        event.setInputs(inputs);
        event.setOutputs(outputs);

        return event;
    }

    @Test
    public void testEmptyStringAsValue() throws Exception{
        String tableName = tableName();
        String command   = "create table " + tableName + "(id int, name string) row format delimited lines terminated by '\n' null defined as ''";

        runCommandWithDelay(command, 3000);

        assertTableIsRegistered(DEFAULT_DB, tableName);
    }

    @Test
    public void testDropAndRecreateCTASOutput() throws Exception {
        String tableName     = createTable();
        String ctasTableName = "table" + random();
        String query         = "create table " + ctasTableName + " as select * from " + tableName;

        runCommand(query);

        assertTableIsRegistered(DEFAULT_DB, ctasTableName);

        Set<ReadEntity>  inputs  = getInputs(tableName, Entity.Type.TABLE);
        Set<WriteEntity> outputs =  getOutputs(ctasTableName, Entity.Type.TABLE);

        HiveEventContext hiveEventContext = constructEvent(query, HiveOperation.CREATETABLE_AS_SELECT, inputs, outputs);
        AtlasEntity processEntity1 = validateProcess(hiveEventContext);
        AtlasEntity processExecutionEntity1 = validateProcessExecution(processEntity1, hiveEventContext);
        AtlasObjectId process = toAtlasObjectId(processExecutionEntity1.getRelationshipAttribute(
                BaseHiveEvent.ATTRIBUTE_PROCESS));
        Assert.assertEquals(process.getGuid(), processEntity1.getGuid());

        String           dropQuery         = String.format("drop table %s ", ctasTableName);

        runCommandWithDelay(dropQuery, 5000);

        assertTableIsNotRegistered(DEFAULT_DB, ctasTableName);

        runCommand(query);

        String tblId          = assertTableIsRegistered(DEFAULT_DB, ctasTableName);
        AtlasEntity tblEntity = atlasClientV2.getEntityByGuid(tblId).getEntity();
        List ddlList          = (List) tblEntity.getRelationshipAttribute(ATTRIBUTE_DDL_QUERIES);

        assertNotNull(ddlList);
        assertEquals(ddlList.size(), 1);

        outputs =  getOutputs(ctasTableName, Entity.Type.TABLE);

        AtlasEntity processEntity2 = validateProcess(hiveEventContext);
        AtlasEntity processExecutionEntity2 = validateProcessExecution(processEntity2, hiveEventContext);
        AtlasObjectId process2 = toAtlasObjectId(processExecutionEntity2.getRelationshipAttribute(
                BaseHiveEvent.ATTRIBUTE_PROCESS));
        Assert.assertEquals(process2.getGuid(), processEntity2.getGuid());

        assertNotEquals(processEntity1.getGuid(), processEntity2.getGuid());
        Assert.assertEquals(numberOfProcessExecutions(processEntity1), 1);
        Assert.assertEquals(numberOfProcessExecutions(processEntity2), 1);

        validateOutputTables(processEntity1, outputs);
    }

    @Test
    public void testCreateView() throws Exception {
        String tableName = createTable();
        String viewName  = tableName();
        String query     = "create view " + viewName + " as select * from " + tableName;

        runCommand(query);

        HiveEventContext hiveEventContext = constructEvent(query, HiveOperation.CREATEVIEW, getInputs(tableName,
                Entity.Type.TABLE), getOutputs(viewName, Entity.Type.TABLE));
        AtlasEntity processEntity1 = validateProcess(hiveEventContext);
        AtlasEntity processExecutionEntity1 = validateProcessExecution(processEntity1, hiveEventContext);
        AtlasObjectId process1 = toAtlasObjectId(processExecutionEntity1.getRelationshipAttribute(
                BaseHiveEvent.ATTRIBUTE_PROCESS));
        Assert.assertEquals(process1.getGuid(), processEntity1.getGuid());
        Assert.assertEquals(numberOfProcessExecutions(processEntity1), 1);
        assertTableIsRegistered(DEFAULT_DB, viewName);

        String viewId          = assertTableIsRegistered(DEFAULT_DB, viewName);
        AtlasEntity viewEntity = atlasClientV2.getEntityByGuid(viewId).getEntity();
        List ddlQueries        = (List) viewEntity.getRelationshipAttribute(ATTRIBUTE_DDL_QUERIES);

        Assert.assertNotNull(ddlQueries);
        Assert.assertEquals(ddlQueries.size(), 1);
    }

    @Test
    public void testAlterViewAsSelect() throws Exception {
        //Create the view from table1
        String table1Name = createTable();
        String viewName   = tableName();
        String query      = "create view " + viewName + " as select * from " + table1Name;

        runCommand(query);

        String table1Id = assertTableIsRegistered(DEFAULT_DB, table1Name);

        HiveEventContext hiveEventContext = constructEvent(query, HiveOperation.CREATEVIEW, getInputs(table1Name,
                Entity.Type.TABLE), getOutputs(viewName, Entity.Type.TABLE));
        String      processId1     = assertProcessIsRegistered(hiveEventContext);
        AtlasEntity processEntity1 = atlasClientV2.getEntityByGuid(processId1).getEntity();
        AtlasEntity processExecutionEntity1 = validateProcessExecution(processEntity1, hiveEventContext);
        AtlasObjectId process1 = toAtlasObjectId(processExecutionEntity1.getRelationshipAttribute(
                BaseHiveEvent.ATTRIBUTE_PROCESS));
        Assert.assertEquals(process1.getGuid(), processEntity1.getGuid());
        Assert.assertEquals(numberOfProcessExecutions(processEntity1), 1);

        String viewId = assertTableIsRegistered(DEFAULT_DB, viewName);

        //Check lineage which includes table1
        String                         datasetName      = HiveMetaStoreBridge.getTableQualifiedName(CLUSTER_NAME, DEFAULT_DB, viewName);
        String                         tableId          = assertTableIsRegistered(DEFAULT_DB, viewName);
        AtlasLineageInfo               inputLineageInfo = atlasClientV2.getLineageInfo(tableId, AtlasLineageInfo.LineageDirection.INPUT, 0);
        Map<String, AtlasEntityHeader> entityMap        = inputLineageInfo.getGuidEntityMap();

        assertTrue(entityMap.containsKey(viewId));
        assertTrue(entityMap.containsKey(table1Id));

        //Alter the view from table2
        String table2Name = createTable();

        query = "alter view " + viewName + " as select * from " + table2Name;

        runCommand(query);

        HiveEventContext hiveEventContext2 = constructEvent(query, HiveOperation.CREATEVIEW, getInputs(table2Name,
                Entity.Type.TABLE), getOutputs(viewName, Entity.Type.TABLE));
        String      processId2     = assertProcessIsRegistered(hiveEventContext2);
        AtlasEntity processEntity2 = atlasClientV2.getEntityByGuid(processId2).getEntity();
        AtlasEntity processExecutionEntity2 = validateProcessExecution(processEntity2, hiveEventContext2);
        AtlasObjectId process2 = toAtlasObjectId(processExecutionEntity2.getRelationshipAttribute(
                BaseHiveEvent.ATTRIBUTE_PROCESS));
        Assert.assertEquals(process2.getGuid(), processEntity2.getGuid());
        Assert.assertEquals(numberOfProcessExecutions(processEntity2), 2);
        Assert.assertEquals(processEntity1.getGuid(), processEntity2.getGuid());

        String table2Id = assertTableIsRegistered(DEFAULT_DB, table2Name);
        String viewId2  = assertTableIsRegistered(DEFAULT_DB, viewName);

        Assert.assertEquals(viewId2, viewId);

        AtlasEntity viewEntity = atlasClientV2.getEntityByGuid(viewId2).getEntity();
        List ddlQueries        = (List) viewEntity.getRelationshipAttribute(ATTRIBUTE_DDL_QUERIES);

        Assert.assertNotNull(ddlQueries);
        Assert.assertEquals(ddlQueries.size(), 2);

        datasetName = HiveMetaStoreBridge.getTableQualifiedName(CLUSTER_NAME, DEFAULT_DB, viewName);

        String                         tableId1          = assertTableIsRegistered(DEFAULT_DB, viewName);
        AtlasLineageInfo               inputLineageInfo1 = atlasClientV2.getLineageInfo(tableId1, AtlasLineageInfo.LineageDirection.INPUT, 0);
        Map<String, AtlasEntityHeader> entityMap1        = inputLineageInfo1.getGuidEntityMap();

        assertTrue(entityMap1.containsKey(viewId));

        //This is through the alter view process
        assertTrue(entityMap1.containsKey(table2Id));

        //This is through the Create view process
        assertTrue(entityMap1.containsKey(table1Id));

        //Outputs dont exist
        AtlasLineageInfo               outputLineageInfo = atlasClientV2.getLineageInfo(tableId1, AtlasLineageInfo.LineageDirection.OUTPUT, 0);
        Map<String, AtlasEntityHeader> entityMap2        = outputLineageInfo.getGuidEntityMap();

        assertEquals(entityMap2.size(),0);
    }

    private String createTestDFSFile(String path) throws Exception {
        return "pfile://" + file(path);
    }

    @Test
    public void testLoadLocalPath() throws Exception {
        String tableName = createTable(false);
        String loadFile  = file("load");
        String query     = "load data local inpath 'file://" + loadFile + "' into table " + tableName;

        String tblId = assertTableIsRegistered(DEFAULT_DB, tableName);

        runCommand(query);

        AtlasEntity tblEntity  = atlasClientV2.getEntityByGuid(tblId).getEntity();
        List ddlQueries        = (List) tblEntity.getRelationshipAttribute(ATTRIBUTE_DDL_QUERIES);

        Assert.assertNotNull(ddlQueries);
        Assert.assertEquals(ddlQueries.size(), 1);

        assertProcessIsRegistered(constructEvent(query, HiveOperation.LOAD, getInputs("file://" + loadFile, Entity.Type.LOCAL_DIR), getOutputs(tableName, Entity.Type.TABLE)));
    }

    @Test
    public void testLoadLocalPathIntoPartition() throws Exception {
        String tableName = createTable(true);
        String loadFile  = file("load");
        String query     = "load data local inpath 'file://" + loadFile + "' into table " + tableName +  " partition(dt = '"+ PART_FILE + "')";

        String tblId = assertTableIsRegistered(DEFAULT_DB, tableName);

        runCommand(query);

        AtlasEntity tblEntity  = atlasClientV2.getEntityByGuid(tblId).getEntity();
        List ddlQueries        = (List) tblEntity.getRelationshipAttribute(ATTRIBUTE_DDL_QUERIES);

        Assert.assertNotNull(ddlQueries);
        Assert.assertEquals(ddlQueries.size(), 1);

        assertProcessIsRegistered(constructEvent(query, HiveOperation.LOAD, null, getOutputs(tableName, Entity.Type.TABLE)));
    }

    @Test
    public void testLoadDFSPathPartitioned() throws Exception {
        String tableName = createTable(true, true, false);

        assertTableIsRegistered(DEFAULT_DB, tableName);

        String loadFile = createTestDFSFile("loadDFSFile");
        String query    = "load data inpath '" + loadFile + "' into table " + tableName + " partition(dt = '"+ PART_FILE + "')";

        runCommand(query);

        Set<WriteEntity> outputs      = getOutputs(tableName, Entity.Type.TABLE);
        Set<ReadEntity>  inputs       = getInputs(loadFile, Entity.Type.DFS_DIR);
        Set<WriteEntity> partitionOps = new LinkedHashSet<>(outputs);

        partitionOps.addAll(getOutputs(DEFAULT_DB + "@" + tableName + "@dt=" + PART_FILE, Entity.Type.PARTITION));

        AtlasEntity processReference = validateProcess(constructEvent(query, HiveOperation.LOAD, inputs, partitionOps), inputs, outputs);

        validateHDFSPaths(processReference, INPUTS, loadFile);
        validateOutputTables(processReference, outputs);

        String loadFile2 = createTestDFSFile("loadDFSFile1");

        query = "load data inpath '" + loadFile2 + "' into table " + tableName + " partition(dt = '"+ PART_FILE + "')";

        runCommand(query);

        Set<ReadEntity> process2Inputs = getInputs(loadFile2, Entity.Type.DFS_DIR);
        Set<ReadEntity> expectedInputs = new LinkedHashSet<>();

        expectedInputs.addAll(process2Inputs);
        expectedInputs.addAll(inputs);

        validateProcess(constructEvent(query, HiveOperation.LOAD, expectedInputs, partitionOps), expectedInputs, outputs);
    }

    private String getQualifiedTblName(String inputTable) {
        String inputtblQlfdName = inputTable;

        if (inputTable != null && !inputTable.contains("@")) {
            inputtblQlfdName = HiveMetaStoreBridge.getTableQualifiedName(CLUSTER_NAME, DEFAULT_DB, inputTable);
        }
        return inputtblQlfdName;
    }

    private AtlasEntity validateProcess(HiveEventContext event, Set<ReadEntity> inputTables, Set<WriteEntity> outputTables) throws Exception {
        String      processId     = assertProcessIsRegistered(event, inputTables, outputTables);
        AtlasEntity processEntity = atlasClientV2.getEntityByGuid(processId).getEntity();

        validateInputTables(processEntity, inputTables);
        validateOutputTables(processEntity, outputTables);

        return processEntity;
    }

    private AtlasEntity validateProcess(HiveEventContext event) throws Exception {
        return validateProcess(event, event.getInputs(), event.getOutputs());
    }

    private AtlasEntity validateProcessExecution(AtlasEntity hiveProcess, HiveEventContext event) throws Exception {
        String      processExecutionId     = assertProcessExecutionIsRegistered(hiveProcess, event);
        AtlasEntity processExecutionEntity = atlasClientV2.getEntityByGuid(processExecutionId).getEntity();
        return processExecutionEntity;
    }

    @Test
    public void testInsertIntoTable() throws Exception {
        String inputTable1Name = createTable();
        String inputTable2Name = createTable();
        String insertTableName = createTable();

        assertTableIsRegistered(DEFAULT_DB, inputTable1Name);
        assertTableIsRegistered(DEFAULT_DB, insertTableName);

        String query = "insert into " + insertTableName + " select t1.id, t1.name from " + inputTable2Name + " as t2, " + inputTable1Name + " as t1 where t1.id=t2.id";

        runCommand(query);

        Set<ReadEntity> inputs = getInputs(inputTable1Name, Entity.Type.TABLE);

        inputs.addAll(getInputs(inputTable2Name, Entity.Type.TABLE));

        Set<WriteEntity> outputs = getOutputs(insertTableName, Entity.Type.TABLE);

        (outputs.iterator().next()).setWriteType(WriteEntity.WriteType.INSERT);

        HiveEventContext event = constructEvent(query, HiveOperation.QUERY, inputs, outputs);

        Set<ReadEntity> expectedInputs = new TreeSet<ReadEntity>(entityComparator) {{
            addAll(inputs);
        }};

        String tblId           = assertTableIsRegistered(DEFAULT_DB, insertTableName);
        AtlasEntity tblEntity  = atlasClientV2.getEntityByGuid(tblId).getEntity();
        List ddlQueries        = (List) tblEntity.getRelationshipAttribute(ATTRIBUTE_DDL_QUERIES);

        Assert.assertNotNull(ddlQueries);
        Assert.assertEquals(ddlQueries.size(), 1);

        AtlasEntity processEntity1 = validateProcess(event, expectedInputs, outputs);

        //Test sorting of tbl names
        SortedSet<String> sortedTblNames = new TreeSet<>();

        sortedTblNames.add(inputTable1Name.toLowerCase());
        sortedTblNames.add(inputTable2Name.toLowerCase());

        //Verify sorted order of inputs in qualified name
        Assert.assertEquals(processEntity1.getAttribute(ATTRIBUTE_QUALIFIED_NAME),
                            Joiner.on(SEP).join("QUERY",
                                    getQualifiedTblName(sortedTblNames.first()),
                                    HiveMetaStoreBridge.getTableCreatedTime(hiveMetaStoreBridge.getHiveClient().getTable(DEFAULT_DB, sortedTblNames.first())),
                                    getQualifiedTblName(sortedTblNames.last()),
                                    HiveMetaStoreBridge.getTableCreatedTime(hiveMetaStoreBridge.getHiveClient().getTable(DEFAULT_DB, sortedTblNames.last())))
                                    + IO_SEP + SEP
                                    + Joiner.on(SEP).
                                    join(WriteEntity.WriteType.INSERT.name(),
                                            getQualifiedTblName(insertTableName),
                                            HiveMetaStoreBridge.getTableCreatedTime(hiveMetaStoreBridge.getHiveClient().getTable(DEFAULT_DB, insertTableName)))
        );

        //Rerun same query. Should result in same process
        runCommandWithDelay(query, 3000);

        AtlasEntity processEntity2 = validateProcess(event, expectedInputs, outputs);
        Assert.assertEquals(numberOfProcessExecutions(processEntity2), 2);
        Assert.assertEquals(processEntity1.getGuid(), processEntity2.getGuid());
    }

    @Test
    public void testInsertIntoTableProcessExecution() throws Exception {
        String inputTable1Name = createTable();
        String inputTable2Name = createTable();
        String insertTableName = createTable();

        assertTableIsRegistered(DEFAULT_DB, inputTable1Name);
        assertTableIsRegistered(DEFAULT_DB, insertTableName);

        String query = "insert into " + insertTableName + " select t1.id, t1.name from " + inputTable2Name + " as t2, " + inputTable1Name + " as t1 where t1.id=t2.id";

        runCommand(query);

        Set<ReadEntity> inputs = getInputs(inputTable1Name, Entity.Type.TABLE);

        inputs.addAll(getInputs(inputTable2Name, Entity.Type.TABLE));

        Set<WriteEntity> outputs = getOutputs(insertTableName, Entity.Type.TABLE);

        (outputs.iterator().next()).setWriteType(WriteEntity.WriteType.INSERT);

        HiveEventContext event = constructEvent(query, HiveOperation.QUERY, inputs, outputs);

        Set<ReadEntity> expectedInputs = new TreeSet<ReadEntity>(entityComparator) {{
            addAll(inputs);
        }};

        assertTableIsRegistered(DEFAULT_DB, insertTableName);

        AtlasEntity processEntity1 = validateProcess(event, expectedInputs, outputs);
        AtlasEntity processExecutionEntity1 = validateProcessExecution(processEntity1, event);
        AtlasObjectId process = toAtlasObjectId(processExecutionEntity1.getRelationshipAttribute(
                BaseHiveEvent.ATTRIBUTE_PROCESS));
        Assert.assertEquals(process.getGuid(), processEntity1.getGuid());

        //Test sorting of tbl names
        SortedSet<String> sortedTblNames = new TreeSet<>();

        sortedTblNames.add(inputTable1Name.toLowerCase());
        sortedTblNames.add(inputTable2Name.toLowerCase());

        //Verify sorted order of inputs in qualified name
        Assert.assertEquals(processEntity1.getAttribute(ATTRIBUTE_QUALIFIED_NAME),
                Joiner.on(SEP).join("QUERY",
                        getQualifiedTblName(sortedTblNames.first()),
                        HiveMetaStoreBridge.getTableCreatedTime(hiveMetaStoreBridge.getHiveClient().getTable(DEFAULT_DB, sortedTblNames.first())),
                        getQualifiedTblName(sortedTblNames.last()),
                        HiveMetaStoreBridge.getTableCreatedTime(hiveMetaStoreBridge.getHiveClient().getTable(DEFAULT_DB, sortedTblNames.last())))
                        + IO_SEP + SEP
                        + Joiner.on(SEP).
                        join(WriteEntity.WriteType.INSERT.name(),
                                getQualifiedTblName(insertTableName),
                                HiveMetaStoreBridge.getTableCreatedTime(hiveMetaStoreBridge.getHiveClient().getTable(DEFAULT_DB, insertTableName)))
        );

        //Rerun same query. Should result in same process
        runCommandWithDelay(query, 3000);

        AtlasEntity processEntity2 = validateProcess(event, expectedInputs, outputs);
        AtlasEntity processExecutionEntity2 = validateProcessExecution(processEntity2, event);
        process = toAtlasObjectId(processExecutionEntity2.getRelationshipAttribute(BaseHiveEvent.ATTRIBUTE_PROCESS));
        Assert.assertEquals(process.getGuid(), processEntity2.getGuid());
        Assert.assertEquals(processEntity1.getGuid(), processEntity2.getGuid());

        String queryWithDifferentPredicate = "insert into " + insertTableName + " select t1.id, t1.name from " +
                inputTable2Name + " as t2, " + inputTable1Name + " as t1 where t1.id=100";
        runCommandWithDelay(queryWithDifferentPredicate, 1000);

        HiveEventContext event3 = constructEvent(queryWithDifferentPredicate, HiveOperation.QUERY, inputs, outputs);
        AtlasEntity processEntity3 = validateProcess(event3, expectedInputs, outputs);
        AtlasEntity processExecutionEntity3 = validateProcessExecution(processEntity3, event3);
        process = toAtlasObjectId(processExecutionEntity3.getRelationshipAttribute(BaseHiveEvent.ATTRIBUTE_PROCESS));
        Assert.assertEquals(process.getGuid(), processEntity3.getGuid());
        Assert.assertEquals(numberOfProcessExecutions(processEntity3), 3);
        Assert.assertEquals(processEntity2.getGuid(), processEntity3.getGuid());
    }

    @Test
    public void testInsertIntoLocalDir() throws Exception {
        String tableName       = createTable();
        String randomLocalPath = mkdir("hiverandom.tmp");
        String query           = "insert overwrite LOCAL DIRECTORY '" + randomLocalPath + "' select id, name from " + tableName;

        runCommand(query);

        HiveEventContext event = constructEvent(query,  HiveOperation.QUERY,
                getInputs(tableName, Entity.Type.TABLE), getOutputs(randomLocalPath, Entity.Type.LOCAL_DIR));
        AtlasEntity hiveProcess = validateProcess(event);
        AtlasEntity hiveProcessExecution = validateProcessExecution(hiveProcess, event);
        AtlasObjectId process = toAtlasObjectId(hiveProcessExecution.getRelationshipAttribute(
                BaseHiveEvent.ATTRIBUTE_PROCESS));
        Assert.assertEquals(process.getGuid(), hiveProcess.getGuid());
        Assert.assertEquals(numberOfProcessExecutions(hiveProcess), 1);

        String tblId          = assertTableIsRegistered(DEFAULT_DB, tableName);

        AtlasEntity tblEntity = atlasClientV2.getEntityByGuid(tblId).getEntity();
        List ddlQueries       = (List) tblEntity.getRelationshipAttribute(ATTRIBUTE_DDL_QUERIES);

        Assert.assertNotNull(ddlQueries);
        Assert.assertEquals(ddlQueries.size(), 1);
    }

    @Test
    public void testUpdateProcess() throws Exception {
        String tableName = createTable();
        String pFile1    = createTestDFSPath("somedfspath1");
        String query     = "insert overwrite DIRECTORY '" + pFile1  + "' select id, name from " + tableName;

        runCommand(query);

        Set<ReadEntity>  inputs  = getInputs(tableName, Entity.Type.TABLE);
        Set<WriteEntity> outputs = getOutputs(pFile1, Entity.Type.DFS_DIR);

        outputs.iterator().next().setWriteType(WriteEntity.WriteType.PATH_WRITE);

        HiveEventContext hiveEventContext = constructEvent(query, HiveOperation.QUERY, inputs, outputs);
        AtlasEntity      processEntity    = validateProcess(hiveEventContext);
        AtlasEntity processExecutionEntity1 = validateProcessExecution(processEntity, hiveEventContext);
        AtlasObjectId process = toAtlasObjectId(processExecutionEntity1.getRelationshipAttribute(
                BaseHiveEvent.ATTRIBUTE_PROCESS));
        Assert.assertEquals(process.getGuid(), processEntity.getGuid());

        validateHDFSPaths(processEntity, OUTPUTS, pFile1);

        assertTableIsRegistered(DEFAULT_DB, tableName);

        validateInputTables(processEntity, inputs);

        //Rerun same query with same HDFS path
        runCommandWithDelay(query, 3000);

        assertTableIsRegistered(DEFAULT_DB, tableName);

        AtlasEntity process2Entity = validateProcess(hiveEventContext);
        AtlasEntity processExecutionEntity2 = validateProcessExecution(processEntity, hiveEventContext);
        AtlasObjectId process2 = toAtlasObjectId(processExecutionEntity2.getRelationshipAttribute(
                BaseHiveEvent.ATTRIBUTE_PROCESS));
        Assert.assertEquals(process2.getGuid(), process2Entity.getGuid());


        validateHDFSPaths(process2Entity, OUTPUTS, pFile1);

        Assert.assertEquals(process2Entity.getGuid(), processEntity.getGuid());

        //Rerun same query with a new HDFS path. Will result in same process since HDFS paths is not part of qualified name for QUERY operations
        String pFile2 = createTestDFSPath("somedfspath2");

        query = "insert overwrite DIRECTORY '" + pFile2  + "' select id, name from " + tableName;

        runCommandWithDelay(query, 3000);

        String tblId          = assertTableIsRegistered(DEFAULT_DB, tableName);

        AtlasEntity tblEntity = atlasClientV2.getEntityByGuid(tblId).getEntity();
        List ddlQueries       = (List) tblEntity.getRelationshipAttribute(ATTRIBUTE_DDL_QUERIES);

        Assert.assertNotNull(ddlQueries);
        Assert.assertEquals(ddlQueries.size(), 1);

        Set<WriteEntity> p3Outputs = new LinkedHashSet<WriteEntity>() {{
            addAll(getOutputs(pFile2, Entity.Type.DFS_DIR));
            addAll(outputs);
        }};

        AtlasEntity process3Entity = validateProcess(constructEvent(query,  HiveOperation.QUERY, inputs, p3Outputs));
        AtlasEntity processExecutionEntity3 = validateProcessExecution(processEntity, hiveEventContext);
        AtlasObjectId process3 = toAtlasObjectId(processExecutionEntity3.getRelationshipAttribute(
                BaseHiveEvent.ATTRIBUTE_PROCESS));
        Assert.assertEquals(process3.getGuid(), process3Entity.getGuid());
        validateHDFSPaths(process3Entity, OUTPUTS, pFile2);

        Assert.assertEquals(numberOfProcessExecutions(process3Entity), 3);
        Assert.assertEquals(process3Entity.getGuid(), processEntity.getGuid());
    }

    @Test
    public void testInsertIntoDFSDirPartitioned() throws Exception {
        //Test with partitioned table
        String tableName = createTable(true);
        String pFile1    = createTestDFSPath("somedfspath1");
        String query     = "insert overwrite DIRECTORY '" + pFile1  + "' select id, name from " + tableName + " where dt = '" + PART_FILE + "'";

        runCommand(query);

        Set<ReadEntity> inputs = getInputs(tableName, Entity.Type.TABLE);
        Set<WriteEntity> outputs = getOutputs(pFile1, Entity.Type.DFS_DIR);

        outputs.iterator().next().setWriteType(WriteEntity.WriteType.PATH_WRITE);

        Set<ReadEntity> partitionIps = new LinkedHashSet<>(inputs);

        partitionIps.addAll(getInputs(DEFAULT_DB + "@" + tableName + "@dt='" + PART_FILE + "'", Entity.Type.PARTITION));

        AtlasEntity processEntity = validateProcess(constructEvent(query,  HiveOperation.QUERY, partitionIps, outputs), inputs, outputs);

        //Rerun same query with different HDFS path. Should not create another process and should update it.

        String pFile2 = createTestDFSPath("somedfspath2");
        query = "insert overwrite DIRECTORY '" + pFile2  + "' select id, name from " + tableName + " where dt = '" + PART_FILE + "'";

        runCommand(query);

        Set<WriteEntity> pFile2Outputs = getOutputs(pFile2, Entity.Type.DFS_DIR);

        pFile2Outputs.iterator().next().setWriteType(WriteEntity.WriteType.PATH_WRITE);

        //Now the process has 2 paths - one older with deleted reference to partition and another with the the latest partition
        Set<WriteEntity> p2Outputs = new LinkedHashSet<WriteEntity>() {{
            addAll(pFile2Outputs);
            addAll(outputs);
        }};

        AtlasEntity process2Entity = validateProcess(constructEvent(query, HiveOperation.QUERY, partitionIps, pFile2Outputs), inputs, p2Outputs);

        validateHDFSPaths(process2Entity, OUTPUTS, pFile2);

        Assert.assertEquals(process2Entity.getGuid(), processEntity.getGuid());
    }

    //Disabling test as temporary table is not captured by hiveHook(https://issues.apache.org/jira/browse/ATLAS-1274)
    @Test(enabled = false)
    public void testInsertIntoTempTable() throws Exception {
        String tableName       = createTable();
        String insertTableName = createTable(false, false, true);

        assertTableIsRegistered(DEFAULT_DB, tableName);
        assertTableIsNotRegistered(DEFAULT_DB, insertTableName, true);

        String query = "insert into " + insertTableName + " select id, name from " + tableName;

        runCommand(query);

        Set<ReadEntity> inputs = getInputs(tableName, Entity.Type.TABLE);
        Set<WriteEntity> outputs = getOutputs(insertTableName, Entity.Type.TABLE);

        outputs.iterator().next().setWriteType(WriteEntity.WriteType.INSERT);

        HiveEventContext event = constructEvent(query,  HiveOperation.QUERY, inputs, outputs);
        AtlasEntity hiveProcess = validateProcess(event);
        AtlasEntity hiveProcessExecution = validateProcessExecution(hiveProcess, event);
        AtlasObjectId process = toAtlasObjectId(hiveProcessExecution.getRelationshipAttribute(
                BaseHiveEvent.ATTRIBUTE_PROCESS));
        Assert.assertEquals(process.getGuid(), hiveProcess.getGuid());
        Assert.assertEquals(numberOfProcessExecutions(hiveProcess), 1);

        assertTableIsRegistered(DEFAULT_DB, tableName);
        assertTableIsRegistered(DEFAULT_DB, insertTableName, null, true);
    }

    @Test
    public void testInsertIntoPartition() throws Exception {
        boolean isPartitionedTable = true;
        String  tableName          = createTable(isPartitionedTable);
        String  insertTableName    = createTable(isPartitionedTable);
        String  query              = "insert into " + insertTableName + " partition(dt = '"+ PART_FILE + "') select id, name from " + tableName + " where dt = '"+ PART_FILE + "'";

        runCommand(query);

        Set<ReadEntity>  inputs  = getInputs(tableName, Entity.Type.TABLE);
        Set<WriteEntity> outputs = getOutputs(insertTableName, Entity.Type.TABLE);

        outputs.iterator().next().setWriteType(WriteEntity.WriteType.INSERT);

        Set<ReadEntity> partitionIps = new LinkedHashSet<ReadEntity>() {
            {
                addAll(inputs);
                add(getPartitionInput());
            }
        };

        Set<WriteEntity> partitionOps = new LinkedHashSet<WriteEntity>() {
            {
                addAll(outputs);
                add(getPartitionOutput());
            }
        };

        HiveEventContext event = constructEvent(query,  HiveOperation.QUERY, partitionIps, partitionOps);
        AtlasEntity hiveProcess = validateProcess(event, inputs, outputs);
        AtlasEntity hiveProcessExecution = validateProcessExecution(hiveProcess, event);
        AtlasObjectId process = toAtlasObjectId(hiveProcessExecution.getRelationshipAttribute(
                BaseHiveEvent.ATTRIBUTE_PROCESS));
        Assert.assertEquals(process.getGuid(), hiveProcess.getGuid());
        Assert.assertEquals(numberOfProcessExecutions(hiveProcess), 1);
        assertTableIsRegistered(DEFAULT_DB, tableName);

        String tblId          = assertTableIsRegistered(DEFAULT_DB, insertTableName);
        AtlasEntity tblEntity = atlasClientV2.getEntityByGuid(tblId).getEntity();
        List ddlQueries       = (List) tblEntity.getRelationshipAttribute(ATTRIBUTE_DDL_QUERIES);

        Assert.assertNotNull(ddlQueries);
        Assert.assertEquals(ddlQueries.size(), 1);

        //TODO -Add update test case
    }

    @Test
    public void testExportImportUnPartitionedTable() throws Exception {
        String tableName = createTable(false);

        String tblId = assertTableIsRegistered(DEFAULT_DB, tableName);

        String filename = "file://" + mkdir("exportUnPartitioned");
        String query    = "export table " + tableName + " to \"" + filename + "\"";

        runCommand(query);

        AtlasEntity tblEntity = atlasClientV2.getEntityByGuid(tblId).getEntity();
        List ddlQueries       = (List) tblEntity.getRelationshipAttribute(ATTRIBUTE_DDL_QUERIES);

        Assert.assertNotNull(ddlQueries);
        Assert.assertEquals(ddlQueries.size(), 1);

        Set<ReadEntity>  inputs        = getInputs(tableName, Entity.Type.TABLE);
        Set<WriteEntity> outputs       = getOutputs(filename, Entity.Type.DFS_DIR);

        HiveEventContext event         = constructEvent(query, HiveOperation.EXPORT, inputs, outputs);
        AtlasEntity      processEntity = validateProcess(event);
        AtlasEntity hiveProcessExecution = validateProcessExecution(processEntity, event);
        AtlasObjectId process = toAtlasObjectId(hiveProcessExecution.getRelationshipAttribute(
                BaseHiveEvent.ATTRIBUTE_PROCESS));
        Assert.assertEquals(process.getGuid(), processEntity.getGuid());
        Assert.assertEquals(numberOfProcessExecutions(processEntity), 1);
        validateHDFSPaths(processEntity, OUTPUTS, filename);
        validateInputTables(processEntity, inputs);

        //Import
        String importTableName = createTable(false);

        String importTblId = assertTableIsRegistered(DEFAULT_DB, importTableName);

        query = "import table " + importTableName + " from '" + filename + "'";

        runCommand(query);

        AtlasEntity importTblEntity = atlasClientV2.getEntityByGuid(importTblId).getEntity();
        List importTblddlQueries    = (List) importTblEntity.getRelationshipAttribute(ATTRIBUTE_DDL_QUERIES);

        Assert.assertNotNull(importTblddlQueries);
        Assert.assertEquals(importTblddlQueries.size(), 1);

        outputs = getOutputs(importTableName, Entity.Type.TABLE);

        HiveEventContext event2         = constructEvent(query, HiveOperation.IMPORT,
                getInputs(filename, Entity.Type.DFS_DIR), outputs);
        AtlasEntity      processEntity2 = validateProcess(event2);
        AtlasEntity hiveProcessExecution2 = validateProcessExecution(processEntity2, event2);
        AtlasObjectId process2 = toAtlasObjectId(hiveProcessExecution2.getRelationshipAttribute(
                BaseHiveEvent.ATTRIBUTE_PROCESS));
        Assert.assertEquals(process2.getGuid(), processEntity2.getGuid());

        Assert.assertEquals(numberOfProcessExecutions(processEntity2), 1);
        Assert.assertNotEquals(processEntity.getGuid(), processEntity2.getGuid());

        //Should create another process
        filename = "file://" + mkdir("export2UnPartitioned");
        query    = "export table " + tableName + " to \"" + filename + "\"";

        runCommand(query);

        AtlasEntity tblEntity2 = atlasClientV2.getEntityByGuid(tblId).getEntity();
        List ddlQueries2       = (List) tblEntity2.getRelationshipAttribute(ATTRIBUTE_DDL_QUERIES);

        Assert.assertNotNull(ddlQueries2);
        Assert.assertEquals(ddlQueries2.size(), 1);

        inputs  = getInputs(tableName, Entity.Type.TABLE);
        outputs = getOutputs(filename, Entity.Type.DFS_DIR);

        HiveEventContext event3            = constructEvent(query, HiveOperation.EXPORT, inputs, outputs);
        AtlasEntity      processEntity3    = validateProcess(event3);
        AtlasEntity hiveProcessExecution3  = validateProcessExecution(processEntity3, event3);
        AtlasObjectId process3 = toAtlasObjectId(hiveProcessExecution3.getRelationshipAttribute(
                BaseHiveEvent.ATTRIBUTE_PROCESS));
        Assert.assertEquals(process3.getGuid(), processEntity3.getGuid());

        Assert.assertEquals(numberOfProcessExecutions(processEntity3), 1);

        // Should be a different process compared to the previous ones
        Assert.assertNotEquals(processEntity.getGuid(), processEntity3.getGuid());
        Assert.assertNotEquals(processEntity2.getGuid(), processEntity3.getGuid());

        //import again shouyld create another process
        query = "import table " + importTableName + " from '" + filename + "'";

        runCommand(query);

        AtlasEntity tblEntity3 = atlasClientV2.getEntityByGuid(importTblId).getEntity();
        List ddlQueries3       = (List) tblEntity3.getRelationshipAttribute(ATTRIBUTE_DDL_QUERIES);

        Assert.assertNotNull(ddlQueries3);
        Assert.assertEquals(ddlQueries3.size(), 1);

        outputs = getOutputs(importTableName, Entity.Type.TABLE);

        HiveEventContext event4 = constructEvent(query, HiveOperation.IMPORT, getInputs(filename,
                Entity.Type.DFS_DIR), outputs);
        AtlasEntity      processEntity4    = validateProcess(event4);
        AtlasEntity hiveProcessExecution4  = validateProcessExecution(processEntity4, event4);
        AtlasObjectId process4 = toAtlasObjectId(hiveProcessExecution4.getRelationshipAttribute(
                BaseHiveEvent.ATTRIBUTE_PROCESS));
        Assert.assertEquals(process4.getGuid(), processEntity4.getGuid());

        Assert.assertEquals(numberOfProcessExecutions(processEntity4), 1);

        // Should be a different process compared to the previous ones
        Assert.assertNotEquals(processEntity.getGuid(), processEntity4.getGuid());
        Assert.assertNotEquals(processEntity2.getGuid(), processEntity4.getGuid());
        Assert.assertNotEquals(processEntity3.getGuid(), processEntity4.getGuid());
    }

    @Test
    public void testExportImportPartitionedTable() throws Exception {
        boolean isPartitionedTable = true;
        String  tableName          = createTable(isPartitionedTable);

        String tblId = assertTableIsRegistered(DEFAULT_DB, tableName);

        //Add a partition
        String partFile = "file://" + mkdir("partition");
        String query    = "alter table " + tableName + " add partition (dt='"+ PART_FILE + "') location '" + partFile + "'";

        runCommand(query);

        AtlasEntity tblEntity = atlasClientV2.getEntityByGuid(tblId).getEntity();
        List ddlQueries       = (List) tblEntity.getRelationshipAttribute(ATTRIBUTE_DDL_QUERIES);

        Assert.assertNotNull(ddlQueries);
        Assert.assertEquals(ddlQueries.size(), 1);

        String filename = "pfile://" + mkdir("export");

        query = "export table " + tableName + " to \"" + filename + "\"";

        runCommand(query);

        AtlasEntity tblEntity2 = atlasClientV2.getEntityByGuid(tblId).getEntity();
        List ddlQueries2       = (List) tblEntity2.getRelationshipAttribute(ATTRIBUTE_DDL_QUERIES);

        Assert.assertNotNull(ddlQueries2);
        Assert.assertEquals(ddlQueries2.size(), 1);

        Set<ReadEntity>  expectedExportInputs = getInputs(tableName, Entity.Type.TABLE);
        Set<WriteEntity> outputs              = getOutputs(filename, Entity.Type.DFS_DIR);
        Set<ReadEntity> partitionIps          = getInputs(DEFAULT_DB + "@" + tableName + "@dt=" + PART_FILE, Entity.Type.PARTITION); //Note that export has only partition as input in this case

        partitionIps.addAll(expectedExportInputs);

        HiveEventContext event1 = constructEvent(query, HiveOperation.EXPORT, partitionIps, outputs);
        AtlasEntity processEntity1 = validateProcess(event1, expectedExportInputs, outputs);
        AtlasEntity hiveProcessExecution1 = validateProcessExecution(processEntity1, event1);
        AtlasObjectId process1 = toAtlasObjectId(hiveProcessExecution1.getRelationshipAttribute(
                BaseHiveEvent.ATTRIBUTE_PROCESS));
        Assert.assertEquals(process1.getGuid(), processEntity1.getGuid());
        Assert.assertEquals(numberOfProcessExecutions(processEntity1), 1);

        validateHDFSPaths(processEntity1, OUTPUTS, filename);

        //Import
        String importTableName = createTable(true);

        String tblId2 = assertTableIsRegistered(DEFAULT_DB, tableName);

        query = "import table " + importTableName + " from '" + filename + "'";

        runCommand(query);

        AtlasEntity tblEntity3 = atlasClientV2.getEntityByGuid(tblId2).getEntity();
        List ddlQueries3       = (List) tblEntity3.getRelationshipAttribute(ATTRIBUTE_DDL_QUERIES);

        Assert.assertNotNull(ddlQueries3);
        Assert.assertEquals(ddlQueries3.size(), 1);

        Set<ReadEntity>  expectedImportInputs = getInputs(filename, Entity.Type.DFS_DIR);
        Set<WriteEntity> importOutputs        = getOutputs(importTableName, Entity.Type.TABLE);
        Set<WriteEntity> partitionOps         = getOutputs(DEFAULT_DB + "@" + importTableName + "@dt=" + PART_FILE, Entity.Type.PARTITION);

        partitionOps.addAll(importOutputs);

        HiveEventContext event2 = constructEvent(query, HiveOperation.IMPORT, expectedImportInputs , partitionOps);
        AtlasEntity processEntity2 = validateProcess(event2, expectedImportInputs, importOutputs);
        AtlasEntity hiveProcessExecution2 = validateProcessExecution(processEntity2, event2);
        AtlasObjectId process2 = toAtlasObjectId(hiveProcessExecution2.getRelationshipAttribute(
                BaseHiveEvent.ATTRIBUTE_PROCESS));
        Assert.assertEquals(process2.getGuid(), processEntity2.getGuid());
        Assert.assertEquals(numberOfProcessExecutions(processEntity2), 1);
        Assert.assertNotEquals(processEntity1.getGuid(), processEntity2.getGuid());

        //Export should update same process
        filename = "pfile://" + mkdir("export2");
        query    = "export table " + tableName + " to \"" + filename + "\"";

        runCommand(query);

        Set<WriteEntity> outputs2  = getOutputs(filename, Entity.Type.DFS_DIR);
        Set<WriteEntity> p3Outputs = new LinkedHashSet<WriteEntity>() {{
            addAll(outputs2);
            addAll(outputs);
        }};

        HiveEventContext event3 = constructEvent(query, HiveOperation.EXPORT, partitionIps, outputs2);

        // this process entity should return same as the processEntity1 since the inputs and outputs are the same,
        // hence the qualifiedName will be the same
        AtlasEntity processEntity3 = validateProcess(event3, expectedExportInputs, p3Outputs);
        AtlasEntity hiveProcessExecution3 = validateProcessExecution(processEntity3, event3);
        AtlasObjectId process3 = toAtlasObjectId(hiveProcessExecution3.getRelationshipAttribute(
                BaseHiveEvent.ATTRIBUTE_PROCESS));
        Assert.assertEquals(process3.getGuid(), processEntity3.getGuid());
        Assert.assertEquals(numberOfProcessExecutions(processEntity3), 2);
        Assert.assertEquals(processEntity1.getGuid(), processEntity3.getGuid());

        query = "alter table " + importTableName + " drop partition (dt='"+ PART_FILE + "')";

        runCommand(query);

        //Import should update same process
        query = "import table " + importTableName + " from '" + filename + "'";

        runCommandWithDelay(query, 3000);

        Set<ReadEntity> importInputs          = getInputs(filename, Entity.Type.DFS_DIR);
        Set<ReadEntity> expectedImport2Inputs = new LinkedHashSet<ReadEntity>() {{
            addAll(importInputs);
            addAll(expectedImportInputs);
        }};

        HiveEventContext event4 = constructEvent(query, HiveOperation.IMPORT, importInputs, partitionOps);

        // This process is going to be same as processEntity2
        AtlasEntity processEntity4 = validateProcess(event4, expectedImport2Inputs, importOutputs);
        AtlasEntity hiveProcessExecution4 = validateProcessExecution(processEntity4, event4);
        AtlasObjectId process4 = toAtlasObjectId(hiveProcessExecution4.getRelationshipAttribute(
                BaseHiveEvent.ATTRIBUTE_PROCESS));
        Assert.assertEquals(process4.getGuid(), processEntity4.getGuid());
        Assert.assertEquals(numberOfProcessExecutions(processEntity4), 2);
        Assert.assertEquals(processEntity2.getGuid(), processEntity4.getGuid());
        Assert.assertNotEquals(processEntity1.getGuid(), processEntity4.getGuid());
    }

    @Test
    public void testIgnoreSelect() throws Exception {
        String tableName = createTable();
        String query     = "select * from " + tableName;

        runCommand(query);

        Set<ReadEntity>  inputs           = getInputs(tableName, Entity.Type.TABLE);
        HiveEventContext hiveEventContext = constructEvent(query, HiveOperation.QUERY, inputs, null);

        assertProcessIsNotRegistered(hiveEventContext);

        //check with uppercase table name
        query = "SELECT * from " + tableName.toUpperCase();

        runCommand(query);

        assertProcessIsNotRegistered(hiveEventContext);
    }

    @Test
    public void testAlterTableRenameAliasRegistered() throws Exception{
        String tableName    = createTable(false);
        String tableGuid    = assertTableIsRegistered(DEFAULT_DB, tableName);
        String newTableName = tableName();
        String query        = String.format("alter table %s rename to %s", tableName, newTableName);

        runCommand(query);

        String newTableGuid = assertTableIsRegistered(DEFAULT_DB, newTableName);

        assertEquals(tableGuid, newTableGuid);

        AtlasEntity         atlasEntity    = atlasClientV2.getEntityByGuid(newTableGuid).getEntity();
        Map<String, Object> valueMap       = atlasEntity.getAttributes();
        Iterable<String>    aliasList      = (Iterable<String>) valueMap.get("aliases");
        String              aliasTableName = aliasList.iterator().next();

        assert tableName.toLowerCase().equals(aliasTableName);
    }

    @Test
    public void testAlterTableRename() throws Exception {
        String      tableName   = createTable(true);
        String      newDBName   = createDatabase();
        String      tableId     = assertTableIsRegistered(DEFAULT_DB, tableName);
        AtlasEntity tableEntity = atlasClientV2.getEntityByGuid(tableId).getEntity();
        String      createTime  = String.valueOf(tableEntity.getAttribute(ATTRIBUTE_CREATE_TIME));

        Assert.assertNotNull(createTime);

        String columnGuid = assertColumnIsRegistered(HiveMetaStoreBridge.getColumnQualifiedName(HiveMetaStoreBridge.getTableQualifiedName(CLUSTER_NAME, DEFAULT_DB, tableName), NAME));
        String sdGuid     = assertSDIsRegistered(HiveMetaStoreBridge.getStorageDescQFName(HiveMetaStoreBridge.getTableQualifiedName(CLUSTER_NAME, DEFAULT_DB, tableName)), null);

        assertDatabaseIsRegistered(newDBName);

        String colTraitDetails     = createTrait(columnGuid); //Add trait to column
        String sdTraitDetails      = createTrait(sdGuid); //Add trait to sd
        String partColumnGuid      = assertColumnIsRegistered(HiveMetaStoreBridge.getColumnQualifiedName(HiveMetaStoreBridge.getTableQualifiedName(CLUSTER_NAME, DEFAULT_DB, tableName), "dt"));
        String partColTraitDetails = createTrait(partColumnGuid); //Add trait to part col keys
        String newTableName        = tableName();
        String query               = String.format("alter table %s rename to %s", DEFAULT_DB + "." + tableName, newDBName + "." + newTableName);

        runCommandWithDelay(query, 3000);

        String newColGuid = assertColumnIsRegistered(HiveMetaStoreBridge.getColumnQualifiedName(HiveMetaStoreBridge.getTableQualifiedName(CLUSTER_NAME, newDBName, newTableName), NAME));

        Assert.assertEquals(newColGuid, columnGuid);

        assertColumnIsNotRegistered(HiveMetaStoreBridge.getColumnQualifiedName(HiveMetaStoreBridge.getTableQualifiedName(CLUSTER_NAME, newDBName, tableName), NAME));

        assertTrait(columnGuid, colTraitDetails);

        String newSdGuid = assertSDIsRegistered(HiveMetaStoreBridge.getStorageDescQFName(HiveMetaStoreBridge.getTableQualifiedName(CLUSTER_NAME, newDBName, newTableName)), null);

        Assert.assertEquals(newSdGuid, sdGuid);
        assertTrait(sdGuid, sdTraitDetails);
        assertTrait(partColumnGuid, partColTraitDetails);
        assertTableIsNotRegistered(DEFAULT_DB, tableName);

        String renamedTableId = assertTableIsRegistered(newDBName, newTableName, new AssertPredicate() {
            @Override
            public void assertOnEntity(final AtlasEntity entity) throws Exception {
                AtlasObjectId sd = toAtlasObjectId(entity.getAttribute(ATTRIBUTE_STORAGEDESC));

                assertNotNull(sd);
            }
        });

        AtlasEntity renamedTableEntity = atlasClientV2.getEntityByGuid(renamedTableId).getEntity();
        List        ddlQueries         = (List) renamedTableEntity.getRelationshipAttribute(ATTRIBUTE_DDL_QUERIES);

        Assert.assertNotNull(ddlQueries);
        Assert.assertEquals(ddlQueries.size(), 2);

    }

    private List<AtlasEntity> getColumns(String dbName, String tableName) throws Exception {
        String                 tableId              = assertTableIsRegistered(dbName, tableName);
        AtlasEntityWithExtInfo tblEntityWithExtInfo = atlasClientV2.getEntityByGuid(tableId);
        AtlasEntity            tableEntity          = tblEntityWithExtInfo.getEntity();

        //with soft delete, the deleted columns are returned as well. So, filter the deleted ones
        List<AtlasObjectId> columns       = toAtlasObjectIdList(tableEntity.getAttribute(ATTRIBUTE_COLUMNS));
        List<AtlasEntity>   activeColumns = new ArrayList<>();

        for (AtlasObjectId col : columns) {
            AtlasEntity columnEntity = tblEntityWithExtInfo.getEntity(col.getGuid());

            if (columnEntity.getStatus() == AtlasEntity.Status.ACTIVE) {
                activeColumns.add(columnEntity);
            }
        }

        return activeColumns;
    }

    private String createTrait(String guid) throws AtlasServiceException {
        //add trait
        //valid type names in v2 must consist of a letter followed by a sequence of letter, number, or _ characters
        String                 traitName = "PII_Trait" + random();
        AtlasClassificationDef piiTrait  =  AtlasTypeUtil.createTraitTypeDef(traitName, Collections.<String>emptySet());

        atlasClientV2.createAtlasTypeDefs(new AtlasTypesDef(Collections.emptyList(), Collections.emptyList(), Collections.singletonList(piiTrait), Collections.emptyList()));
        atlasClientV2.addClassifications(guid, Collections.singletonList(new AtlasClassification(piiTrait.getName())));

        return traitName;
    }

    private void assertTrait(String guid, String traitName) throws AtlasServiceException {
        AtlasClassification.AtlasClassifications classifications = atlasClientV2.getClassifications(guid);

        Assert.assertEquals(classifications.getList().get(0).getTypeName(), traitName);
    }

    @Test
    public void testAlterTableAddColumn() throws Exception {
        String tableName = createTable();
        String column    = columnName();
        String query     = "alter table " + tableName + " add columns (" + column + " string)";

        runCommand(query);

        assertColumnIsRegistered(HiveMetaStoreBridge.getColumnQualifiedName(HiveMetaStoreBridge.getTableQualifiedName(CLUSTER_NAME, DEFAULT_DB, tableName), column));

        //Verify the number of columns present in the table
        List<AtlasEntity> columns = getColumns(DEFAULT_DB, tableName);

        Assert.assertEquals(columns.size(), 3);

        String      tblId      = assertTableIsRegistered(DEFAULT_DB, tableName);
        AtlasEntity tblEntity  = atlasClientV2.getEntityByGuid(tblId).getEntity();
        List        ddlQueries = (List) tblEntity.getRelationshipAttribute(ATTRIBUTE_DDL_QUERIES);

        Assert.assertNotNull(ddlQueries);
        Assert.assertEquals(ddlQueries.size(), 2);

    }

    //ATLAS-1321: Disable problematic tests. Need to revisit and fix them later
    @Test(enabled = false)
    public void testAlterTableDropColumn() throws Exception {
        String tableName  = createTable();
        String colDropped = "id";
        String query      = "alter table " + tableName + " replace columns (name string)";

        runCommand(query);

        assertColumnIsNotRegistered(HiveMetaStoreBridge.getColumnQualifiedName(HiveMetaStoreBridge.getTableQualifiedName(CLUSTER_NAME, DEFAULT_DB, tableName), colDropped));

        //Verify the number of columns present in the table
        List<AtlasEntity> columns = getColumns(DEFAULT_DB, tableName);

        assertEquals(columns.size(), 1);
        assertEquals(columns.get(0).getAttribute(NAME), "name");

        String tblId          = assertTableIsRegistered(DEFAULT_DB, tableName);
        AtlasEntity tblEntity = atlasClientV2.getEntityByGuid(tblId).getEntity();
        List ddlQueries       = (List) tblEntity.getRelationshipAttribute(ATTRIBUTE_DDL_QUERIES);

        Assert.assertNotNull(ddlQueries);
        Assert.assertEquals(ddlQueries.size(), 2);
    }

    @Test
    public void testAlterTableChangeColumn() throws Exception {
        //Change name
        String oldColName = NAME;
        String newColName = "name1";
        String tableName  = createTable();
        String query      = String.format("alter table %s change %s %s string", tableName, oldColName, newColName);

        runCommandWithDelay(query, 3000);

        assertColumnIsNotRegistered(HiveMetaStoreBridge.getColumnQualifiedName(HiveMetaStoreBridge.getTableQualifiedName(CLUSTER_NAME, DEFAULT_DB, tableName), oldColName));
        assertColumnIsRegistered(HiveMetaStoreBridge.getColumnQualifiedName(HiveMetaStoreBridge.getTableQualifiedName(CLUSTER_NAME, DEFAULT_DB, tableName), newColName));

        //Verify the number of columns present in the table
        List<AtlasEntity> columns = getColumns(DEFAULT_DB, tableName);

        Assert.assertEquals(columns.size(), 2);

        String tblId = assertTableIsRegistered(DEFAULT_DB, tableName);
        AtlasEntity tblEntity = atlasClientV2.getEntityByGuid(tblId).getEntity();
        List ddlQueries = (List) tblEntity.getRelationshipAttribute(ATTRIBUTE_DDL_QUERIES);

        Assert.assertNotNull(ddlQueries);
        Assert.assertEquals(ddlQueries.size(), 2);

        //Change column type
        oldColName = "name1";
        newColName = "name2";

        String newColType = "int";

        query = String.format("alter table %s change column %s %s %s", tableName, oldColName, newColName, newColType);

        runCommandWithDelay(query, 3000);

        columns = getColumns(DEFAULT_DB, tableName);

        Assert.assertEquals(columns.size(), 2);

        String newColQualifiedName = HiveMetaStoreBridge.getColumnQualifiedName(HiveMetaStoreBridge.getTableQualifiedName(CLUSTER_NAME, DEFAULT_DB, tableName), newColName);

        assertColumnIsRegistered(newColQualifiedName, new AssertPredicate() {
            @Override
            public void assertOnEntity(AtlasEntity entity) throws Exception {
                assertEquals(entity.getAttribute("type"), "int");
            }
        });

        assertColumnIsNotRegistered(HiveMetaStoreBridge.getColumnQualifiedName(HiveMetaStoreBridge.getTableQualifiedName(CLUSTER_NAME, DEFAULT_DB, tableName), oldColName));

        AtlasEntity tblEntity2  = atlasClientV2.getEntityByGuid(tblId).getEntity();
        List        ddlQueries2 = (List) tblEntity2.getRelationshipAttribute(ATTRIBUTE_DDL_QUERIES);

        Assert.assertNotNull(ddlQueries2);
        Assert.assertEquals(ddlQueries2.size(), 3);

        //Change name and add comment
        oldColName = "name2";
        newColName = "name3";

        String comment = "added comment";

        query = String.format("alter table %s change column %s %s %s COMMENT '%s' after id", tableName, oldColName, newColName, newColType, comment);

        runCommandWithDelay(query, 3000);

        columns = getColumns(DEFAULT_DB, tableName);

        Assert.assertEquals(columns.size(), 2);

        assertColumnIsNotRegistered(HiveMetaStoreBridge.getColumnQualifiedName(HiveMetaStoreBridge.getTableQualifiedName(CLUSTER_NAME, DEFAULT_DB, tableName), oldColName));

        newColQualifiedName = HiveMetaStoreBridge.getColumnQualifiedName(HiveMetaStoreBridge.getTableQualifiedName(CLUSTER_NAME, DEFAULT_DB, tableName), newColName);

        assertColumnIsRegistered(newColQualifiedName, new AssertPredicate() {
            @Override
            public void assertOnEntity(AtlasEntity entity) throws Exception {
                assertEquals(entity.getAttribute(ATTRIBUTE_COMMENT), comment);
            }
        });

        //Change column position
        oldColName = "name3";
        newColName = "name4";
        query      = String.format("alter table %s change column %s %s %s first", tableName, oldColName, newColName, newColType);

        runCommandWithDelay(query, 3000);

        columns = getColumns(DEFAULT_DB, tableName);

        Assert.assertEquals(columns.size(), 2);

        assertColumnIsNotRegistered(HiveMetaStoreBridge.getColumnQualifiedName(HiveMetaStoreBridge.getTableQualifiedName(CLUSTER_NAME, DEFAULT_DB, tableName), oldColName));

        newColQualifiedName = HiveMetaStoreBridge.getColumnQualifiedName(HiveMetaStoreBridge.getTableQualifiedName(CLUSTER_NAME, DEFAULT_DB, tableName), newColName);

        assertColumnIsRegistered(newColQualifiedName);

        String finalNewColName = newColName;

        String tblId3 = assertTableIsRegistered(DEFAULT_DB, tableName, new AssertPredicate() {
                    @Override
                    public void assertOnEntity(AtlasEntity entity) throws Exception {
                        List<AtlasObjectId> columns = toAtlasObjectIdList(entity.getAttribute(ATTRIBUTE_COLUMNS));

                        assertEquals(columns.size(), 2);
                    }
                }
        );

        AtlasEntity tblEntity3  = atlasClientV2.getEntityByGuid(tblId3).getEntity();
        List        ddlQueries3 = (List) tblEntity3.getRelationshipAttribute(ATTRIBUTE_DDL_QUERIES);

        Assert.assertNotNull(ddlQueries3);
        Assert.assertEquals(ddlQueries3.size(), 5);

        //Change col position again
        oldColName = "name4";
        newColName = "name5";
        query      = String.format("alter table %s change column %s %s %s after id", tableName, oldColName, newColName, newColType);

        runCommandWithDelay(query, 3000);

        columns = getColumns(DEFAULT_DB, tableName);

        Assert.assertEquals(columns.size(), 2);

        assertColumnIsNotRegistered(HiveMetaStoreBridge.getColumnQualifiedName(HiveMetaStoreBridge.getTableQualifiedName(CLUSTER_NAME, DEFAULT_DB, tableName), oldColName));

        newColQualifiedName = HiveMetaStoreBridge.getColumnQualifiedName(HiveMetaStoreBridge.getTableQualifiedName(CLUSTER_NAME, DEFAULT_DB, tableName), newColName);

        assertColumnIsRegistered(newColQualifiedName);

        //Check col position
        String finalNewColName2 = newColName;

        String tblId4 = assertTableIsRegistered(DEFAULT_DB, tableName, new AssertPredicate() {
                    @Override
                    public void assertOnEntity(AtlasEntity entity) throws Exception {
                        List<AtlasObjectId> columns = toAtlasObjectIdList(entity.getAttribute(ATTRIBUTE_COLUMNS));

                        assertEquals(columns.size(), 2);
                    }
                }
        );

        AtlasEntity tblEntity4  = atlasClientV2.getEntityByGuid(tblId4).getEntity();
        List        ddlQueries4 = (List) tblEntity4.getRelationshipAttribute(ATTRIBUTE_DDL_QUERIES);

        Assert.assertNotNull(ddlQueries4);
        Assert.assertEquals(ddlQueries4.size(), 6);
    }

    /**
     * Reenabling this test since HIVE-14706 is fixed now and the hive version we are using now sends
     * us the column lineage information
     * @throws Exception
     */
    @Test
    public void testColumnLevelLineage() throws Exception {
        String sourceTable = "table" + random();

        runCommand("create table " + sourceTable + "(a int, b int)");

        String sourceTableGUID = assertTableIsRegistered(DEFAULT_DB, sourceTable);
        String a_guid          = assertColumnIsRegistered(HiveMetaStoreBridge.getColumnQualifiedName(HiveMetaStoreBridge.getTableQualifiedName(CLUSTER_NAME, DEFAULT_DB, sourceTable), "a"));
        String b_guid          = assertColumnIsRegistered(HiveMetaStoreBridge.getColumnQualifiedName(HiveMetaStoreBridge.getTableQualifiedName(CLUSTER_NAME, DEFAULT_DB, sourceTable), "b"));
        String ctasTableName   = "table" + random();
        String query           = "create table " + ctasTableName + " as " + "select sum(a+b) as a, count(*) as b from " + sourceTable;

        runCommand(query);

        String dest_a_guid = assertColumnIsRegistered(HiveMetaStoreBridge.getColumnQualifiedName(HiveMetaStoreBridge.getTableQualifiedName(CLUSTER_NAME, DEFAULT_DB, ctasTableName), "a"));
        String dest_b_guid = assertColumnIsRegistered(HiveMetaStoreBridge.getColumnQualifiedName(HiveMetaStoreBridge.getTableQualifiedName(CLUSTER_NAME, DEFAULT_DB, ctasTableName), "b"));

        Set<ReadEntity>  inputs  = getInputs(sourceTable, Entity.Type.TABLE);
        Set<WriteEntity> outputs = getOutputs(ctasTableName, Entity.Type.TABLE);
        HiveEventContext event   = constructEvent(query, HiveOperation.CREATETABLE_AS_SELECT, inputs, outputs);
        AtlasEntity processEntity1 = validateProcess(event);
        AtlasEntity hiveProcessExecution1 = validateProcessExecution(processEntity1, event);
        AtlasObjectId process1 = toAtlasObjectId(hiveProcessExecution1.getRelationshipAttribute(
                BaseHiveEvent.ATTRIBUTE_PROCESS));
        Assert.assertEquals(process1.getGuid(), processEntity1.getGuid());
        Assert.assertEquals(numberOfProcessExecutions(processEntity1), 1);
        Assert.assertEquals(processEntity1.getGuid(), processEntity1.getGuid());

        assertTableIsRegistered(DEFAULT_DB, ctasTableName);

        String       processQName        = sortEventsAndGetProcessQualifiedName(event);
        List<String> aLineageInputs      = Arrays.asList(a_guid, b_guid);
        String       aLineageProcessName = processQName + ":" + "a";

        LOG.debug("Searching for column lineage process {} ", aLineageProcessName);
        String guid = assertEntityIsRegistered(HiveDataTypes.HIVE_COLUMN_LINEAGE.getName(), ATTRIBUTE_QUALIFIED_NAME, aLineageProcessName, null);

        AtlasEntity         colLineageEntity      = atlasClientV2.getEntityByGuid(guid).getEntity();
        List<AtlasObjectId> processInputs         = toAtlasObjectIdList(colLineageEntity.getAttribute("inputs"));
        List<String>        processInputsAsString = new ArrayList<>();

        for(AtlasObjectId input: processInputs){
            processInputsAsString.add(input.getGuid());
        }

        Collections.sort(processInputsAsString);
        Collections.sort(aLineageInputs);

        Assert.assertEquals(processInputsAsString, aLineageInputs);

        List<String> bLineageInputs      = Arrays.asList(sourceTableGUID);
        String       bLineageProcessName = processQName + ":" + "b";

        LOG.debug("Searching for column lineage process {} ", bLineageProcessName);

        String guid1 = assertEntityIsRegistered(HiveDataTypes.HIVE_COLUMN_LINEAGE.getName(), ATTRIBUTE_QUALIFIED_NAME, bLineageProcessName, null);


        AtlasEntity         colLineageEntity1      = atlasClientV2.getEntityByGuid(guid1).getEntity();
        List<AtlasObjectId> bProcessInputs         = toAtlasObjectIdList(colLineageEntity1.getAttribute("inputs"));
        List<String>        bProcessInputsAsString = new ArrayList<>();

        for(AtlasObjectId input: bProcessInputs){
            bProcessInputsAsString.add(input.getGuid());
        }

        Collections.sort(bProcessInputsAsString);
        Collections.sort(bLineageInputs);

        Assert.assertEquals(bProcessInputsAsString, bLineageInputs);

        //Test lineage API response
        AtlasLineageInfo               atlasLineageInfoInput = atlasClientV2.getLineageInfo(dest_a_guid, AtlasLineageInfo.LineageDirection.INPUT,0);
        Map<String, AtlasEntityHeader> entityMap             = atlasLineageInfoInput.getGuidEntityMap();

        ObjectNode response   = atlasClient.getInputGraphForEntity(dest_a_guid);
        JsonNode   vertices   = response.get("values").get("vertices");
        JsonNode   dest_a_val = vertices.get(dest_a_guid);
        JsonNode   src_a_val  = vertices.get(a_guid);
        JsonNode   src_b_val  = vertices.get(b_guid);

        Assert.assertNotNull(dest_a_val);
        Assert.assertNotNull(src_a_val);
        Assert.assertNotNull(src_b_val);

        ObjectNode b_response  = atlasClient.getInputGraphForEntity(dest_b_guid);
        JsonNode   b_vertices  = b_response.get("values").get("vertices");
        JsonNode   b_val       = b_vertices.get(dest_b_guid);
        JsonNode   src_tbl_val = b_vertices.get(sourceTableGUID);

        Assert.assertNotNull(b_val);
        Assert.assertNotNull(src_tbl_val);
    }

    @Test
    public void testIgnoreTruncateTable() throws Exception {
        String tableName = createTable(false);
        String query     = String.format("truncate table %s", tableName);

        runCommand(query);

        Set<WriteEntity> outputs = getOutputs(tableName, Entity.Type.TABLE);
        HiveEventContext event   = constructEvent(query, HiveOperation.TRUNCATETABLE, null, outputs);

        assertTableIsRegistered(DEFAULT_DB, tableName);
        assertProcessIsNotRegistered(event);
    }

    @Test
    public void testAlterTablePartitionColumnType() throws Exception {
        String tableName = createTable(true, true, false);
        String newType   = "int";
        String query     = String.format("ALTER TABLE %s PARTITION COLUMN (dt %s)", tableName, newType);

        runCommand(query);

        String colQualifiedName = HiveMetaStoreBridge.getColumnQualifiedName(HiveMetaStoreBridge.getTableQualifiedName(CLUSTER_NAME, DEFAULT_DB, tableName), "dt");
        String dtColId          = assertColumnIsRegistered(colQualifiedName, new AssertPredicate() {
            @Override
            public void assertOnEntity(AtlasEntity column) throws Exception {
                Assert.assertEquals(column.getAttribute("type"), newType);
            }
        });

        assertTableIsRegistered(DEFAULT_DB, tableName, new AssertPredicate() {
            @Override
            public void assertOnEntity(AtlasEntity table) throws Exception {
                final List<AtlasObjectId> partitionKeys = toAtlasObjectIdList(table.getAttribute("partitionKeys"));
                Assert.assertEquals(partitionKeys.size(), 1);
                Assert.assertEquals(partitionKeys.get(0).getGuid(), dtColId);

            }
        });
    }

    @Test
    public void testAlterTableWithoutHookConf() throws Exception {
        String tableName     = tableName();
        String createCommand = "create table " + tableName + " (id int, name string)";

        driverWithNoHook.run(createCommand);

        assertTableIsNotRegistered(DEFAULT_DB, tableName);

        String command = "alter table " + tableName + " change id id_new string";

        runCommand(command);

        assertTableIsRegistered(DEFAULT_DB, tableName);

        String tbqn = HiveMetaStoreBridge.getTableQualifiedName(CLUSTER_NAME, DEFAULT_DB, tableName);

        assertColumnIsRegistered(HiveMetaStoreBridge.getColumnQualifiedName(tbqn, "id_new"));
    }

    @Test
    public void testTraitsPreservedOnColumnRename() throws Exception {
        String dbName      = createDatabase();
        String tableName   = tableName();
        String createQuery = String.format("create table %s.%s (id int, name string)", dbName, tableName);

        runCommand(createQuery);

        String tbqn       = HiveMetaStoreBridge.getTableQualifiedName(CLUSTER_NAME, dbName, tableName);
        String guid       = assertColumnIsRegistered(HiveMetaStoreBridge.getColumnQualifiedName(tbqn, "id"));
        String trait      = createTrait(guid);
        String oldColName = "id";
        String newColName = "id_new";
        String query      = String.format("alter table %s.%s change %s %s string", dbName, tableName, oldColName, newColName);

        runCommand(query);

        String guid2 = assertColumnIsRegistered(HiveMetaStoreBridge.getColumnQualifiedName(tbqn, "id_new"));

        assertEquals(guid2, guid);

        assertTrue(atlasClient.getEntity(guid2).getTraitNames().contains(trait));
    }

    @Test
    public void testAlterViewRename() throws Exception {
        String tableName = createTable();
        String viewName  = tableName();
        String newName   = tableName();
        String query     = "create view " + viewName + " as select * from " + tableName;

        runCommandWithDelay(query, 5000);

        query = "alter view " + viewName + " rename to " + newName;

        runCommandWithDelay(query, 5000);

        assertTableIsNotRegistered(DEFAULT_DB, viewName);

        String viewId          = assertTableIsRegistered(DEFAULT_DB, newName);
        AtlasEntity viewEntity = atlasClientV2.getEntityByGuid(viewId).getEntity();
        List ddlQueries        = (List) viewEntity.getRelationshipAttribute(ATTRIBUTE_DDL_QUERIES);

        Assert.assertNotNull(ddlQueries);
        Assert.assertEquals(ddlQueries.size(), 2);
    }

    @Test
    public void testAlterTableLocation() throws Exception {
        //Its an external table, so the HDFS location should also be registered as an entity
        String tableName = createTable(true, true, false);
        String testPath  = createTestDFSPath("testBaseDir");
        String query     = "alter table " + tableName + " set location '" + testPath + "'";

        runCommandWithDelay(query, 8000);

        String tblId = assertTableIsRegistered(DEFAULT_DB, tableName, new AssertPredicate() {
            @Override
            public void assertOnEntity(AtlasEntity tableRef) throws Exception {
                AtlasObjectId sd = toAtlasObjectId(tableRef.getAttribute(ATTRIBUTE_STORAGEDESC));

                assertNotNull(sd);
            }
        });

        AtlasEntity tblEntity           = atlasClientV2.getEntityByGuid(tblId).getEntity();
        List        ddlQueries          = (List) tblEntity.getRelationshipAttribute(ATTRIBUTE_DDL_QUERIES);

        Assert.assertNotNull(ddlQueries);
        Assert.assertEquals(ddlQueries.size(), 2);

        String      processQualifiedName = getTableProcessQualifiedName(DEFAULT_DB, tableName);
        String      processId            = assertEntityIsRegistered(HiveDataTypes.HIVE_PROCESS.getName(), ATTRIBUTE_QUALIFIED_NAME, processQualifiedName, null);
        AtlasEntity processEntity        = atlasClientV2.getEntityByGuid(processId).getEntity();
        Assert.assertEquals(numberOfProcessExecutions(processEntity), 2);
        //validateProcessExecution(processEntity, event);
        validateHDFSPaths(processEntity, INPUTS, testPath);
    }

    @Test
    public void testAlterTableFileFormat() throws Exception {
        String tableName  = createTable();
        String testFormat = "orc";
        String query      = "alter table " + tableName + " set FILEFORMAT " + testFormat;

        runCommand(query);

        assertTableIsRegistered(DEFAULT_DB, tableName, new AssertPredicate() {
            @Override
            public void assertOnEntity(AtlasEntity tableRef) throws Exception {
                AtlasObjectId sdObjectId = toAtlasObjectId(tableRef.getAttribute(ATTRIBUTE_STORAGEDESC));
                AtlasEntity   sdEntity   = atlasClientV2.getEntityByGuid(sdObjectId.getGuid()).getEntity();

                Assert.assertEquals(sdEntity.getAttribute(ATTRIBUTE_INPUT_FORMAT), "org.apache.hadoop.hive.ql.io.orc.OrcInputFormat");
                Assert.assertEquals(sdEntity.getAttribute(ATTRIBUTE_OUTPUT_FORMAT), "org.apache.hadoop.hive.ql.io.orc.OrcOutputFormat");
                Assert.assertNotNull(sdEntity.getAttribute(ATTRIBUTE_SERDE_INFO));

                AtlasStruct serdeInfo = toAtlasStruct(sdEntity.getAttribute(ATTRIBUTE_SERDE_INFO));

                Assert.assertEquals(serdeInfo.getAttribute(ATTRIBUTE_SERIALIZATION_LIB), "org.apache.hadoop.hive.ql.io.orc.OrcSerde");
                Assert.assertNotNull(serdeInfo.getAttribute(ATTRIBUTE_PARAMETERS));
                Assert.assertEquals(((Map<String, String>) serdeInfo.getAttribute(ATTRIBUTE_PARAMETERS)).get("serialization.format"), "1");
            }
        });


        /**
         * Hive 'alter table stored as' is not supported - See https://issues.apache.org/jira/browse/HIVE-9576
         * query = "alter table " + tableName + " STORED AS " + testFormat.toUpperCase();
         * runCommand(query);

         * tableRef = atlasClientV1.getEntity(tableId);
         * sdRef = (AtlasEntity)tableRef.getAttribute(HiveMetaStoreBridge.STORAGE_DESC);
         * Assert.assertEquals(sdRef.getAttribute(HiveMetaStoreBridge.STORAGE_DESC_INPUT_FMT), "org.apache.hadoop.hive.ql.io.orc.OrcInputFormat");
         * Assert.assertEquals(sdRef.getAttribute(HiveMetaStoreBridge.STORAGE_DESC_OUTPUT_FMT), "org.apache.hadoop.hive.ql.io.orc.OrcOutputFormat");
         * Assert.assertEquals(((Map) sdRef.getAttribute(HiveMetaStoreBridge.PARAMETERS)).getAttribute("orc.compress"), "ZLIB");
         */
    }

    @Test
    public void testAlterTableBucketingClusterSort() throws Exception {
        String       tableName = createTable();
        List<String> cols      = Collections.singletonList("id");

        runBucketSortQuery(tableName, 5, cols, cols);

        cols = Arrays.asList("id", NAME);

        runBucketSortQuery(tableName, 2, cols, cols);
    }

    private void runBucketSortQuery(String tableName, final int numBuckets,  final List<String> bucketCols, final List<String> sortCols) throws Exception {
        String fmtQuery = "alter table %s CLUSTERED BY (%s) SORTED BY (%s) INTO %s BUCKETS";
        String query    = String.format(fmtQuery, tableName, stripListBrackets(bucketCols.toString()), stripListBrackets(sortCols.toString()), numBuckets);

        runCommand(query);

        assertTableIsRegistered(DEFAULT_DB, tableName, new AssertPredicate() {
            @Override
            public void assertOnEntity(AtlasEntity entity) throws Exception {
                verifyBucketSortingProperties(entity, numBuckets, bucketCols, sortCols);
            }
        });
    }

    private String stripListBrackets(String listElements) {
        return StringUtils.strip(StringUtils.strip(listElements, "["), "]");
    }

    private void verifyBucketSortingProperties(AtlasEntity tableRef, int numBuckets, List<String> bucketColNames, List<String>  sortcolNames) throws Exception {
        AtlasObjectId sdObjectId = toAtlasObjectId(tableRef.getAttribute(ATTRIBUTE_STORAGEDESC));
        AtlasEntity   sdEntity   = atlasClientV2.getEntityByGuid(sdObjectId.getGuid()).getEntity();

        Assert.assertEquals((sdEntity.getAttribute(ATTRIBUTE_NUM_BUCKETS)), numBuckets);
        Assert.assertEquals(sdEntity.getAttribute(ATTRIBUTE_BUCKET_COLS), bucketColNames);

        List<AtlasStruct> hiveOrderStructList = toAtlasStructList(sdEntity.getAttribute(ATTRIBUTE_SORT_COLS));

        Assert.assertNotNull(hiveOrderStructList);
        Assert.assertEquals(hiveOrderStructList.size(), sortcolNames.size());

        for (int i = 0; i < sortcolNames.size(); i++) {
            AtlasStruct hiveOrderStruct = hiveOrderStructList.get(i);

            Assert.assertNotNull(hiveOrderStruct);
            Assert.assertEquals(hiveOrderStruct.getAttribute("col"), sortcolNames.get(i));
            Assert.assertEquals(hiveOrderStruct.getAttribute("order"), 1);
        }
    }

    @Test
    public void testAlterTableSerde() throws Exception {
        //SERDE PROPERTIES
        String              tableName     = createTable();
        Map<String, String> expectedProps = new HashMap<String, String>() {{
            put("key1", "value1");
        }};

        runSerdePropsQuery(tableName, expectedProps);

        expectedProps.put("key2", "value2");

        //Add another property
        runSerdePropsQuery(tableName, expectedProps);
    }

    @Test
    public void testDropTable() throws Exception {
        //Test Deletion of tables and its corrresponding columns
        String tableName = createTable(true, true, false);

        assertTableIsRegistered(DEFAULT_DB, tableName);
        assertColumnIsRegistered(HiveMetaStoreBridge.getColumnQualifiedName(HiveMetaStoreBridge.getTableQualifiedName(CLUSTER_NAME, DEFAULT_DB, tableName), "id"));
        assertColumnIsRegistered(HiveMetaStoreBridge.getColumnQualifiedName(HiveMetaStoreBridge.getTableQualifiedName(CLUSTER_NAME, DEFAULT_DB, tableName), NAME));

        String query = String.format("drop table %s ", tableName);

        runCommandWithDelay(query, 3000);

        assertColumnIsNotRegistered(HiveMetaStoreBridge.getColumnQualifiedName(HiveMetaStoreBridge.getTableQualifiedName(CLUSTER_NAME, DEFAULT_DB, tableName), "id"));
        assertColumnIsNotRegistered(HiveMetaStoreBridge.getColumnQualifiedName(HiveMetaStoreBridge.getTableQualifiedName(CLUSTER_NAME, DEFAULT_DB, tableName), NAME));
        assertTableIsNotRegistered(DEFAULT_DB, tableName);
    }

    private WriteEntity getPartitionOutput() {
        TestWriteEntity partEntity = new TestWriteEntity(PART_FILE, Entity.Type.PARTITION);

        return partEntity;
    }

    private ReadEntity getPartitionInput() {
        ReadEntity partEntity = new TestReadEntity(PART_FILE, Entity.Type.PARTITION);

        return partEntity;
    }

    @Test
    public void testDropDatabaseWithCascade() throws Exception {
        //Test Deletion of database and its corresponding tables
        String dbName = "db" + random();

        runCommand("create database " + dbName + " WITH DBPROPERTIES ('p1'='v1')");

        int      numTables  = 10;
        String[] tableNames = new String[numTables];

        for(int i = 0; i < numTables; i++) {
            tableNames[i] = createTable(true, true, false);
        }

        String query = String.format("drop database %s cascade", dbName);

        runCommand(query);

        //Verify columns are not registered for one of the tables
        assertColumnIsNotRegistered(HiveMetaStoreBridge.getColumnQualifiedName(HiveMetaStoreBridge.getTableQualifiedName(CLUSTER_NAME, dbName, tableNames[0]), "id"));
        assertColumnIsNotRegistered(HiveMetaStoreBridge.getColumnQualifiedName(HiveMetaStoreBridge.getTableQualifiedName(CLUSTER_NAME, dbName, tableNames[0]), NAME));

        for(int i = 0; i < numTables; i++) {
            assertTableIsNotRegistered(dbName, tableNames[i]);
        }

        assertDatabaseIsNotRegistered(dbName);
    }

    @Test
    public void testDropDatabaseWithoutCascade() throws Exception {
        //Test Deletion of database and its corresponding tables
        String dbName = "db" + random();

        runCommand("create database " + dbName + " WITH DBPROPERTIES ('p1'='v1')");

        int      numTables = 5;
        String[] tableNames = new String[numTables];

        for(int i = 0; i < numTables; i++) {
            tableNames[i] = createTable(true, true, false);

            String query = String.format("drop table %s", tableNames[i]);

            runCommand(query);

            assertTableIsNotRegistered(dbName, tableNames[i]);
        }

        String query = String.format("drop database %s", dbName);

        runCommand(query);

        String dbQualifiedName = HiveMetaStoreBridge.getDBQualifiedName(CLUSTER_NAME, dbName);

        Thread.sleep(10000);

        try {
            atlasClientV2.getEntityByAttribute(HiveDataTypes.HIVE_DB.getName(), Collections.singletonMap(ATTRIBUTE_QUALIFIED_NAME, dbQualifiedName));
        } catch (AtlasServiceException e) {
            if (e.getStatus() == ClientResponse.Status.NOT_FOUND) {
                return;
            }
        }

        fail(String.format("Entity was not supposed to exist for typeName = %s, attributeName = %s, attributeValue = %s", HiveDataTypes.HIVE_DB.getName(), ATTRIBUTE_QUALIFIED_NAME, dbQualifiedName));
    }

    @Test
    public void testDropNonExistingDB() throws Exception {
        //Test Deletion of a non existing DB
        String dbName = "nonexistingdb";

        assertDatabaseIsNotRegistered(dbName);

        String query = String.format("drop database if exists %s cascade", dbName);

        runCommand(query);

        //Should have no effect
        assertDatabaseIsNotRegistered(dbName);
    }

    @Test
    public void testDropNonExistingTable() throws Exception {
        //Test Deletion of a non existing table
        String tableName = "nonexistingtable";

        assertTableIsNotRegistered(DEFAULT_DB, tableName);

        String query = String.format("drop table if exists %s", tableName);

        runCommand(query);

        //Should have no effect
        assertTableIsNotRegistered(DEFAULT_DB, tableName);
    }

    @Test
    public void testDropView() throws Exception {
        //Test Deletion of tables and its corrresponding columns
        String tableName = createTable(true, true, false);
        String viewName  = tableName();
        String query     = "create view " + viewName + " as select * from " + tableName;

        runCommandWithDelay(query, 3000);

        assertTableIsRegistered(DEFAULT_DB, viewName);
        assertColumnIsRegistered(HiveMetaStoreBridge.getColumnQualifiedName(HiveMetaStoreBridge.getTableQualifiedName(CLUSTER_NAME, DEFAULT_DB, viewName), "id"));
        assertColumnIsRegistered(HiveMetaStoreBridge.getColumnQualifiedName(HiveMetaStoreBridge.getTableQualifiedName(CLUSTER_NAME, DEFAULT_DB, viewName), NAME));

        query = String.format("drop view %s ", viewName);

        runCommandWithDelay(query, 3000);
        assertColumnIsNotRegistered(HiveMetaStoreBridge.getColumnQualifiedName(HiveMetaStoreBridge.getTableQualifiedName(CLUSTER_NAME, DEFAULT_DB, viewName), "id"));
        assertColumnIsNotRegistered(HiveMetaStoreBridge.getColumnQualifiedName(HiveMetaStoreBridge.getTableQualifiedName(CLUSTER_NAME, DEFAULT_DB, viewName), NAME));
        assertTableIsNotRegistered(DEFAULT_DB, viewName);
    }

    private void runSerdePropsQuery(String tableName, Map<String, String> expectedProps) throws Exception {
        String serdeLib        = "org.apache.hadoop.hive.serde2.lazy.LazySimpleSerDe";
        String serializedProps = getSerializedProps(expectedProps);
        String query           = String.format("alter table %s set SERDE '%s' WITH SERDEPROPERTIES (%s)", tableName, serdeLib, serializedProps);

        runCommand(query);

        verifyTableSdProperties(tableName, serdeLib, expectedProps);
    }

    private String getSerializedProps(Map<String, String> expectedProps) {
        StringBuilder sb = new StringBuilder();

        for(String expectedPropKey : expectedProps.keySet()) {
            if(sb.length() > 0) {
                sb.append(",");
            }

            sb.append("'").append(expectedPropKey).append("'");
            sb.append("=");
            sb.append("'").append(expectedProps.get(expectedPropKey)).append("'");
        }

        return sb.toString();
    }

    @Test
    public void testAlterDBOwner() throws Exception {
        String dbName = createDatabase();

        assertDatabaseIsRegistered(dbName);

        String owner    = "testOwner";
        String fmtQuery = "alter database %s set OWNER %s %s";
        String query    = String.format(fmtQuery, dbName, "USER", owner);

        runCommandWithDelay(query, 3000);

        assertDatabaseIsRegistered(dbName, new AssertPredicate() {
            @Override
            public void assertOnEntity(AtlasEntity entity) {
                assertEquals(entity.getAttribute(AtlasClient.OWNER), owner);
            }
        });
    }

    @Test
    public void testAlterDBProperties() throws Exception {
        String dbName   = createDatabase();
        String fmtQuery = "alter database %s %s DBPROPERTIES (%s)";

        testAlterProperties(Entity.Type.DATABASE, dbName, fmtQuery);
    }

    @Test
    public void testAlterTableProperties() throws Exception {
        String tableName = createTable();
        String fmtQuery  = "alter table %s %s TBLPROPERTIES (%s)";

        testAlterProperties(Entity.Type.TABLE, tableName, fmtQuery);
    }

    private void testAlterProperties(Entity.Type entityType, String entityName, String fmtQuery) throws Exception {
        String              SET_OP        = "set";
        String              UNSET_OP      = "unset";
        Map<String, String> expectedProps = new HashMap<String, String>() {{
            put("testPropKey1", "testPropValue1");
            put("comment", "test comment");
        }};

        String query = String.format(fmtQuery, entityName, SET_OP, getSerializedProps(expectedProps));

        runCommandWithDelay(query, 3000);

        verifyEntityProperties(entityType, entityName, expectedProps, false);

        expectedProps.put("testPropKey2", "testPropValue2");
        //Add another property

        query = String.format(fmtQuery, entityName, SET_OP, getSerializedProps(expectedProps));

        runCommandWithDelay(query, 3000);

        verifyEntityProperties(entityType, entityName, expectedProps, false);

        if (entityType != Entity.Type.DATABASE) {
            //Database unset properties doesnt work - alter database %s unset DBPROPERTIES doesnt work
            //Unset all the props
            StringBuilder sb = new StringBuilder("'");

            query = String.format(fmtQuery, entityName, UNSET_OP, Joiner.on("','").skipNulls().appendTo(sb, expectedProps.keySet()).append('\''));

            runCommandWithDelay(query, 3000);

            verifyEntityProperties(entityType, entityName, expectedProps, true);
        }
    }

    @Test
    public void testAlterViewProperties() throws Exception {
        String tableName = createTable();
        String viewName  = tableName();
        String query     = "create view " + viewName + " as select * from " + tableName;

        runCommand(query);

        String fmtQuery = "alter view %s %s TBLPROPERTIES (%s)";

        testAlterProperties(Entity.Type.TABLE, viewName, fmtQuery);
    }

    private void verifyEntityProperties(Entity.Type type, String entityName, final Map<String, String> expectedProps, final boolean checkIfNotExists) throws Exception {
        switch(type) {
            case TABLE:
                assertTableIsRegistered(DEFAULT_DB, entityName, new AssertPredicate() {
                    @Override
                    public void assertOnEntity(AtlasEntity entity) throws Exception {
                        verifyProperties(entity, expectedProps, checkIfNotExists);
                    }
                });
                break;
            case DATABASE:
                assertDatabaseIsRegistered(entityName, new AssertPredicate() {
                    @Override
                    public void assertOnEntity(AtlasEntity entity) throws Exception {
                        verifyProperties(entity, expectedProps, checkIfNotExists);
                    }
                });
                break;
        }
    }

    private void verifyTableSdProperties(String tableName, final String serdeLib, final Map<String, String> expectedProps) throws Exception {
        assertTableIsRegistered(DEFAULT_DB, tableName, new AssertPredicate() {
            @Override
            public void assertOnEntity(AtlasEntity tableRef) throws Exception {
                AtlasObjectId sdEntity = toAtlasObjectId(tableRef.getAttribute(ATTRIBUTE_STORAGEDESC));

                assertNotNull(sdEntity);
            }
        });
    }


    private void verifyProperties(AtlasStruct referenceable, Map<String, String> expectedProps, boolean checkIfNotExists) {
        Map<String, String> parameters = (Map<String, String>) referenceable.getAttribute(ATTRIBUTE_PARAMETERS);

        if (!checkIfNotExists) {
            //Check if properties exist
            Assert.assertNotNull(parameters);
            for (String propKey : expectedProps.keySet()) {
                Assert.assertEquals(parameters.get(propKey), expectedProps.get(propKey));
            }
        } else {
            //Check if properties dont exist
            if (expectedProps != null && parameters != null) {
                for (String propKey : expectedProps.keySet()) {
                    Assert.assertFalse(parameters.containsKey(propKey));
                }
            }
        }
    }

    private String sortEventsAndGetProcessQualifiedName(final HiveEventContext event) throws HiveException{
        SortedSet<ReadEntity>  sortedHiveInputs  = event.getInputs() == null ? null : new TreeSet<ReadEntity>(entityComparator);
        SortedSet<WriteEntity> sortedHiveOutputs = event.getOutputs() == null ? null : new TreeSet<WriteEntity>(entityComparator);

        if (event.getInputs() != null) {
            sortedHiveInputs.addAll(event.getInputs());
        }

        if (event.getOutputs() != null) {
            sortedHiveOutputs.addAll(event.getOutputs());
        }

        return getProcessQualifiedName(hiveMetaStoreBridge, event, sortedHiveInputs, sortedHiveOutputs, getSortedProcessDataSets(event.getInputs()), getSortedProcessDataSets(event.getOutputs()));
    }

    private String assertProcessIsRegistered(final HiveEventContext event) throws Exception {
        try {
            String processQFName = sortEventsAndGetProcessQualifiedName(event);

            LOG.debug("Searching for process with query {}", processQFName);

            return assertEntityIsRegistered(HiveDataTypes.HIVE_PROCESS.getName(), ATTRIBUTE_QUALIFIED_NAME, processQFName, new AssertPredicate() {
                @Override
                public void assertOnEntity(final AtlasEntity entity) throws Exception {
                    List<String> recentQueries = (List<String>) entity.getAttribute(ATTRIBUTE_RECENT_QUERIES);
                    Assert.assertEquals(recentQueries.get(0), lower(event.getQueryStr()));
                }
            });
        } catch (Exception e) {
            LOG.error("Exception : ", e);
            throw e;
        }
    }

    private String assertProcessIsRegistered(final HiveEventContext event, final Set<ReadEntity> inputTbls, final Set<WriteEntity> outputTbls) throws Exception {
        try {
            SortedSet<ReadEntity>  sortedHiveInputs  = event.getInputs() == null ? null : new TreeSet<ReadEntity>(entityComparator);
            SortedSet<WriteEntity> sortedHiveOutputs = event.getOutputs() == null ? null : new TreeSet<WriteEntity>(entityComparator);

            if (event.getInputs() != null) {
                sortedHiveInputs.addAll(event.getInputs());
            }

            if (event.getOutputs() != null) {
                sortedHiveOutputs.addAll(event.getOutputs());
            }

            String processQFName = getProcessQualifiedName(hiveMetaStoreBridge, event, sortedHiveInputs, sortedHiveOutputs, getSortedProcessDataSets(inputTbls), getSortedProcessDataSets(outputTbls));

            LOG.debug("Searching for process with query {}", processQFName);

            return assertEntityIsRegistered(HiveDataTypes.HIVE_PROCESS.getName(), ATTRIBUTE_QUALIFIED_NAME, processQFName, new AssertPredicate() {
                @Override
                public void assertOnEntity(final AtlasEntity entity) throws Exception {
                    List<String> recentQueries = (List<String>) entity.getAttribute(BaseHiveEvent.ATTRIBUTE_RECENT_QUERIES);

                    Assert.assertEquals(recentQueries.get(0), lower(event.getQueryStr()));
                }
            });
        } catch(Exception e) {
            LOG.error("Exception : ", e);
            throw e;
        }
    }

    private String assertProcessExecutionIsRegistered(AtlasEntity hiveProcess, final HiveEventContext event) throws Exception {
        try {
            String guid = "";
            List<AtlasObjectId> processExecutions = toAtlasObjectIdList(hiveProcess.getRelationshipAttribute(
                    BaseHiveEvent.ATTRIBUTE_PROCESS_EXECUTIONS));
            for (AtlasObjectId processExecution : processExecutions) {
                AtlasEntity.AtlasEntityWithExtInfo atlasEntityWithExtInfo = atlasClientV2.
                        getEntityByGuid(processExecution.getGuid());
                AtlasEntity entity = atlasEntityWithExtInfo.getEntity();
                if (String.valueOf(entity.getAttribute(ATTRIBUTE_QUERY_TEXT)).equals(event.getQueryStr().toLowerCase().trim())) {
                    guid = entity.getGuid();
                }
            }

            return assertEntityIsRegisteredViaGuid(guid, new AssertPredicate() {
                @Override
                public void assertOnEntity(final AtlasEntity entity) throws Exception {
                    String queryText = (String) entity.getAttribute(ATTRIBUTE_QUERY_TEXT);
                    Assert.assertEquals(queryText, event.getQueryStr().toLowerCase().trim());
                }
            });
        } catch(Exception e) {
            LOG.error("Exception : ", e);
            throw e;
        }
    }


    private String getDSTypeName(Entity entity) {
        return Entity.Type.TABLE.equals(entity.getType()) ? HiveDataTypes.HIVE_TABLE.name() : HiveMetaStoreBridge.HDFS_PATH;
    }

    private <T extends Entity> SortedMap<T, AtlasEntity> getSortedProcessDataSets(Set<T> inputTbls) {
        SortedMap<T, AtlasEntity> inputs = new TreeMap<>(entityComparator);

        if (inputTbls != null) {
            for (final T tbl : inputTbls) {
                AtlasEntity inputTableRef = new AtlasEntity(getDSTypeName(tbl), new HashMap<String, Object>() {{
                    put(ATTRIBUTE_QUALIFIED_NAME, tbl.getName());
                }});

                inputs.put(tbl, inputTableRef);
            }
        }
        return inputs;
    }

    private void assertProcessIsNotRegistered(HiveEventContext event) throws Exception {
        try {
            SortedSet<ReadEntity>  sortedHiveInputs  = event.getInputs() == null ? null : new TreeSet<ReadEntity>(entityComparator);
            SortedSet<WriteEntity> sortedHiveOutputs = event.getOutputs() == null ? null : new TreeSet<WriteEntity>(entityComparator);

            if (event.getInputs() != null) {
                sortedHiveInputs.addAll(event.getInputs());
            }

            if (event.getOutputs() != null) {
                sortedHiveOutputs.addAll(event.getOutputs());
            }

            String processQFName = getProcessQualifiedName(hiveMetaStoreBridge, event, sortedHiveInputs, sortedHiveOutputs, getSortedProcessDataSets(event.getInputs()), getSortedProcessDataSets(event.getOutputs()));

            LOG.debug("Searching for process with query {}", processQFName);

            assertEntityIsNotRegistered(HiveDataTypes.HIVE_PROCESS.getName(), ATTRIBUTE_QUALIFIED_NAME, processQFName);
        } catch(Exception e) {
            LOG.error("Exception : ", e);
        }
    }

    private void assertTableIsNotRegistered(String dbName, String tableName, boolean isTemporaryTable) throws Exception {
        LOG.debug("Searching for table {}.{}", dbName, tableName);

        String tableQualifiedName = HiveMetaStoreBridge.getTableQualifiedName(CLUSTER_NAME, dbName, tableName, isTemporaryTable);

        assertEntityIsNotRegistered(HiveDataTypes.HIVE_TABLE.getName(), ATTRIBUTE_QUALIFIED_NAME, tableQualifiedName);
    }

    private void assertTableIsNotRegistered(String dbName, String tableName) throws Exception {
        LOG.debug("Searching for table {}.{}", dbName, tableName);

        String tableQualifiedName = HiveMetaStoreBridge.getTableQualifiedName(CLUSTER_NAME, dbName, tableName, false);

        assertEntityIsNotRegistered(HiveDataTypes.HIVE_TABLE.getName(), ATTRIBUTE_QUALIFIED_NAME, tableQualifiedName);
    }

    private String assertTableIsRegistered(String dbName, String tableName, AssertPredicate assertPredicate) throws Exception {
        return assertTableIsRegistered(dbName, tableName, assertPredicate, false);
    }

    @Test
    public void testLineage() throws Exception {
        String table1 = createTable(false);
        String db2    = createDatabase();
        String table2 = tableName();
        String query  = String.format("create table %s.%s as select * from %s", db2, table2, table1);

        runCommand(query);

        String                         table1Id     = assertTableIsRegistered(DEFAULT_DB, table1);
        String                         table2Id     = assertTableIsRegistered(db2, table2);
        AtlasLineageInfo               inputLineage = atlasClientV2.getLineageInfo(table2Id, AtlasLineageInfo.LineageDirection.INPUT, 0);
        Map<String, AtlasEntityHeader> entityMap    = inputLineage.getGuidEntityMap();

        assertTrue(entityMap.containsKey(table1Id));
        assertTrue(entityMap.containsKey(table2Id));

        AtlasLineageInfo               inputLineage1 = atlasClientV2.getLineageInfo(table1Id, AtlasLineageInfo.LineageDirection.OUTPUT, 0);
        Map<String, AtlasEntityHeader> entityMap1    = inputLineage1.getGuidEntityMap();

        assertTrue(entityMap1.containsKey(table1Id));
        assertTrue(entityMap1.containsKey(table2Id));
    }

    //For ATLAS-448
    @Test
    public void testNoopOperation() throws Exception {
        runCommand("show compactions");
        runCommand("show transactions");
    }

    private String createDatabase() throws Exception {
        String dbName = dbName();

        runCommand("create database " + dbName);

        return dbName;
    }

    private String columnName() {
        return "col" + random();
    }

    private String createTable() throws Exception {
        return createTable(false);
    }

    private String createTable(boolean isPartitioned) throws Exception {
        String tableName = tableName();

        runCommand("create table " + tableName + "(id int, name string) comment 'table comment' " + (isPartitioned ? " partitioned by(dt string)" : ""));

        return tableName;
    }

    private String createTable(boolean isExternal, boolean isPartitioned, boolean isTemporary) throws Exception {
        String tableName = tableName();

        String location = "";
        if (isExternal) {
            location = " location '" +  createTestDFSPath("someTestPath") + "'";
        }

        runCommandWithDelay("create " + (isExternal ? " EXTERNAL " : "") + (isTemporary ? "TEMPORARY " : "") + "table " + tableName + "(id int, name string) comment 'table comment' " + (isPartitioned ? " partitioned by(dt string)" : "") + location, 3000);

        return tableName;
    }

    // ReadEntity class doesn't offer a constructor that takes (name, type). A hack to get the tests going!
    private static class TestReadEntity extends ReadEntity {
        private final String      name;
        private final Entity.Type type;

        public TestReadEntity(String name, Entity.Type type) {
            this.name = name;
            this.type = type;
        }

        @Override
        public String getName() { return name; }

        @Override
        public Entity.Type getType() { return type; }
    }

    // WriteEntity class doesn't offer a constructor that takes (name, type). A hack to get the tests going!
    private static class TestWriteEntity extends WriteEntity {
        private final String      name;
        private final Entity.Type type;

        public TestWriteEntity(String name, Entity.Type type) {
            this.name = name;
            this.type = type;
        }

        @Override
        public String getName() { return name; }

        @Override
        public Entity.Type getType() { return type; }
    }

    private int numberOfProcessExecutions(AtlasEntity hiveProcess) {
        return toAtlasObjectIdList(hiveProcess.getRelationshipAttribute(
                BaseHiveEvent.ATTRIBUTE_PROCESS_EXECUTIONS)).size();
    }
}
