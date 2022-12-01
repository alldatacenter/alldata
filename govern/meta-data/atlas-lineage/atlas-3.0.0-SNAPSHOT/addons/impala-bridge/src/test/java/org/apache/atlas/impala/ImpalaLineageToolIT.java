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
package org.apache.atlas.impala;

import static org.apache.atlas.impala.hook.events.BaseImpalaEvent.ATTRIBUTE_QUERY_TEXT;

import java.util.ArrayList;
import java.util.List;
import org.apache.atlas.impala.hook.AtlasImpalaHookContext;
import org.apache.atlas.impala.hook.ImpalaLineageHook;
import org.apache.atlas.impala.hook.events.BaseImpalaEvent;
import org.apache.atlas.impala.model.ImpalaQuery;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasObjectId;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.apache.atlas.impala.hook.events.BaseImpalaEvent.ATTRIBUTE_DDL_QUERIES;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class ImpalaLineageToolIT extends ImpalaLineageITBase {
    public static final long TABLE_CREATE_TIME_SOURCE = 1554750070;
    public static final long TABLE_CREATE_TIME        = 1554750072;
    private static String dir = System.getProperty("user.dir") + "/src/test/resources/";

    /**
     * This tests
     * 1) ImpalaLineageTool can parse one lineage file that contains "create view" command lineage
     * 2) Lineage is sent to Atlas
     * 3) Atlas can get this lineage from Atlas
     */
    @Test
    public void testCreateViewFromFile() {
        // this file contains a single lineage record for "create view".
        // It has table vertex with createTime
        String IMPALA = dir + "impalaCreateView.json";
        String IMPALA_WAL = dir + "WALimpala.wal";

        List<ImpalaQuery> lineageList = new ArrayList<>();
        ImpalaLineageHook impalaLineageHook = new ImpalaLineageHook();

        try {
            // create database and tables to simulate Impala behavior that Impala updates metadata
            // to HMS and HMSHook sends the metadata to Atlas, which has to happen before
            // Atlas can handle lineage notification
            String dbName = "db_1";
            createDatabase(dbName);

            String sourceTableName = "table_1";
            createTable(dbName, sourceTableName,"(id string, count int)", false);

            String targetTableName = "view_1";
            createTable(dbName, targetTableName,"(count int, id string)", false);

            // process lineage record, and send corresponding notification to Atlas
            String[] args = new String[]{"-d", "./", "-p", "impala"};
            ImpalaLineageTool toolInstance = new ImpalaLineageTool(args);
            toolInstance.importHImpalaEntities(impalaLineageHook, IMPALA, IMPALA_WAL);

            // verify the process is saved in Atlas
            // the value is from info in IMPALA_3
            String createTime = new Long((long)(1554750072)*1000).toString();
            String processQFName =
                "db_1.view_1" + AtlasImpalaHookContext.QNAME_SEP_METADATA_NAMESPACE +
                    CLUSTER_NAME + AtlasImpalaHookContext.QNAME_SEP_PROCESS + createTime;

            processQFName = processQFName.toLowerCase();

            String      queryString             = "create view db_1.view_1 as select count, id from db_1.table_1";
            AtlasEntity processEntity1          = validateProcess(processQFName, queryString);
            AtlasEntity processExecutionEntity1 = validateProcessExecution(processEntity1, queryString);
            AtlasObjectId process1              = toAtlasObjectId(processExecutionEntity1.getRelationshipAttribute(
                BaseImpalaEvent.ATTRIBUTE_PROCESS));
            Assert.assertEquals(process1.getGuid(), processEntity1.getGuid());
            Assert.assertEquals(numberOfProcessExecutions(processEntity1), 1);

            String      guid       = assertTableIsRegistered(dbName, targetTableName);
            AtlasEntity entity     = atlasClientV2.getEntityByGuid(guid).getEntity();
            List        ddlQueries = (List) entity.getRelationshipAttribute(ATTRIBUTE_DDL_QUERIES);

            assertNotNull(ddlQueries);
            assertEquals(ddlQueries.size(), 1);
        } catch (Exception e) {
            System.out.print("Appending file error");
        }
    }

    /**
     * This tests is for create view query with extra comment and spaces added in between:
     * 1) ImpalaLineageTool can parse one lineage file that contains " create   view" command lineage
     * 2) Lineage is sent to Atlas
     * 3) Atlas can get this lineage from Atlas
     */
    @Test
    public void testCreateViewWithCommentSpacesFromFile() {
        // this file contains a single lineage record for "create view".
        // It has table vertex with createTime
        String IMPALA = dir + "impalaCreateViewWithCommentSpaces.json";
        String IMPALA_WAL = dir + "WALimpala.wal";

        List<ImpalaQuery> lineageList = new ArrayList<>();
        ImpalaLineageHook impalaLineageHook = new ImpalaLineageHook();

        try {
            // create database and tables to simulate Impala behavior that Impala updates metadata
            // to HMS and HMSHook sends the metadata to Atlas, which has to happen before
            // Atlas can handle lineage notification
            String dbName = "db_8";
            createDatabase(dbName);

            String sourceTableName = "table_1";
            createTable(dbName, sourceTableName,"(id string, count int)", false);

            String targetTableName = "view_1";
            createTable(dbName, targetTableName,"(count int, id string)", false);

            // process lineage record, and send corresponding notification to Atlas
            String[] args = new String[]{"-d", "./", "-p", "impala"};
            ImpalaLineageTool toolInstance = new ImpalaLineageTool(args);
            toolInstance.importHImpalaEntities(impalaLineageHook, IMPALA, IMPALA_WAL);

            // verify the process is saved in Atlas
            // the value is from info in IMPALA_3
            String createTime = new Long((long)(1554750072)*1000).toString();
            String processQFName =
                    "db_8.view_1" + AtlasImpalaHookContext.QNAME_SEP_METADATA_NAMESPACE +
                            CLUSTER_NAME + AtlasImpalaHookContext.QNAME_SEP_PROCESS + createTime;

            processQFName = processQFName.toLowerCase();

            String      queryString             = " create   /* comment1 */ view db_8.view_1 as   select /* comment2 */ count, id from db_8.table_1";
            AtlasEntity processEntity1          = validateProcess(processQFName, queryString);
            AtlasEntity processExecutionEntity1 = validateProcessExecution(processEntity1, queryString);
            AtlasObjectId process1              = toAtlasObjectId(processExecutionEntity1.getRelationshipAttribute(
                    BaseImpalaEvent.ATTRIBUTE_PROCESS));
            Assert.assertEquals(process1.getGuid(), processEntity1.getGuid());
            Assert.assertEquals(numberOfProcessExecutions(processEntity1), 1);

            String      guid       = assertTableIsRegistered(dbName, targetTableName);
            AtlasEntity entity     = atlasClientV2.getEntityByGuid(guid).getEntity();
            List        ddlQueries = (List) entity.getRelationshipAttribute(ATTRIBUTE_DDL_QUERIES);

            assertNotNull(ddlQueries);
            assertEquals(ddlQueries.size(), 1);
        } catch (Exception e) {
            System.out.print("Appending file error");
        }
    }

    /**
     * This tests
     * 1) ImpalaLineageTool can parse one lineage file that contains "create view" command lineage,
     *    but there is no table vertex with createTime.
     * 2) Lineage is sent to Atlas
     * 3) Atlas can get this lineage from Atlas
     */
    @Test
    public void testCreateViewNoCreateTimeFromFile() {
        // this file contains a single lineage record for "create view".
        // there is no table vertex with createTime, which is lineage record generated by Impala
        // originally. The table create time is hard-coded before Impala fixes this issue.
        String IMPALA     = dir + "impalaCreateViewNoCreateTime.json";
        String IMPALA_WAL = dir + "WALimpala.wal";

        List<ImpalaQuery> lineageList       = new ArrayList<>();
        ImpalaLineageHook impalaLineageHook = new ImpalaLineageHook();

        try {
            // create database and tables to simulate Impala behavior that Impala updates metadata
            // to HMS and HMSHook sends the metadata to Atlas, which has to happen before
            // Atlas can handle lineage notification
            String dbName = "db_2";
            createDatabase(dbName);

            String sourceTableName = "table_1";
            createTable(dbName, sourceTableName,"(id string, count int)", false);

            String targetTableName = "view_1";
            createTable(dbName, targetTableName,"(count int, id string)", false);

            // process lineage record, and send corresponding notification to Atlas
            String[] args = new String[]{"-d", "./", "-p", "impala"};
            ImpalaLineageTool toolInstance = new ImpalaLineageTool(args);
            Long beforeCreateTime = System.currentTimeMillis() / BaseImpalaEvent.MILLIS_CONVERT_FACTOR;
            toolInstance.importHImpalaEntities(impalaLineageHook, IMPALA, IMPALA_WAL);
            Long afterCreateTime = System.currentTimeMillis() / BaseImpalaEvent.MILLIS_CONVERT_FACTOR;

            String processQFNameWithoutTime =
                dbName + "." + targetTableName + AtlasImpalaHookContext.QNAME_SEP_METADATA_NAMESPACE +
                    CLUSTER_NAME + AtlasImpalaHookContext.QNAME_SEP_PROCESS;
            processQFNameWithoutTime = processQFNameWithoutTime.toLowerCase();

            List<String> processQFNames = new ArrayList<>();
            String createTime = new Long(beforeCreateTime.longValue()*1000).toString();
            processQFNames.add(processQFNameWithoutTime + createTime);

            if (beforeCreateTime != afterCreateTime) {
                createTime = new Long(afterCreateTime.longValue() * 1000).toString();
                processQFNames.add(processQFNameWithoutTime + createTime);
            }

            // verify the process is saved in Atlas. the value is from info in IMPALA_4.
            // There is no createTime in lineage record, so we don't know the process qualified name
            // And can only verify the process is created for the given query.
            String queryString = "create view " + dbName + "." + targetTableName + " as select count, id from " + dbName + "." + sourceTableName;
            AtlasEntity processEntity1 = validateProcess(processQFNames, queryString);
            AtlasEntity processExecutionEntity1 = validateProcessExecution(processEntity1, queryString);
            AtlasObjectId process1 = toAtlasObjectId(processExecutionEntity1.getRelationshipAttribute(
                BaseImpalaEvent.ATTRIBUTE_PROCESS));
            Assert.assertEquals(process1.getGuid(), processEntity1.getGuid());
            Assert.assertEquals(numberOfProcessExecutions(processEntity1), 1);

            String      guid       = assertTableIsRegistered(dbName, targetTableName);
            AtlasEntity entity     = atlasClientV2.getEntityByGuid(guid).getEntity();
            List        ddlQueries = (List) entity.getRelationshipAttribute(ATTRIBUTE_DDL_QUERIES);

            assertNotNull(ddlQueries);
            assertEquals(ddlQueries.size(), 1);
        } catch (Exception e) {
            System.out.print("Appending file error");
        }
    }

    /**
     * This tests
     * 1) ImpalaLineageTool can parse one lineage file that contains "create table as select" command lineage,
     *    there is table vertex with createTime.
     * 2) Lineage is sent to Atlas
     * 3) Atlas can get this lineage from Atlas
     */
    @Test
    public void testCreateTableAsSelectFromFile() throws Exception {
        String IMPALA = dir + "impalaCreateTableAsSelect.json";
        String IMPALA_WAL = dir + "WALimpala.wal";

        ImpalaLineageHook impalaLineageHook = new ImpalaLineageHook();

        // create database and tables to simulate Impala behavior that Impala updates metadata
        // to HMS and HMSHook sends the metadata to Atlas, which has to happen before
        // Atlas can handle lineage notification
        String dbName = "db_3";
        createDatabase(dbName);

        String sourceTableName = "table_1";
        createTable(dbName, sourceTableName,"(id string, count int)", false);

        String targetTableName = "table_2";
        createTable(dbName, targetTableName,"(count int, id string)", false);

        // process lineage record, and send corresponding notification to Atlas
        String[] args = new String[]{"-d", "./", "-p", "impala"};
        ImpalaLineageTool toolInstance = new ImpalaLineageTool(args);
        toolInstance.importHImpalaEntities(impalaLineageHook, IMPALA, IMPALA_WAL);

        // verify the process is saved in Atlas
        // the value is from info in IMPALA_4.
        String createTime = new Long(TABLE_CREATE_TIME*1000).toString();
        String processQFName =
            dbName + "." + targetTableName + AtlasImpalaHookContext.QNAME_SEP_METADATA_NAMESPACE +
                CLUSTER_NAME + AtlasImpalaHookContext.QNAME_SEP_PROCESS + createTime;

        processQFName = processQFName.toLowerCase();

        String queryString = "create table " + dbName + "." + targetTableName + " as select count, id from " + dbName + "." + sourceTableName;
        AtlasEntity processEntity1 = validateProcess(processQFName, queryString);
        AtlasEntity processExecutionEntity1 = validateProcessExecution(processEntity1, queryString);
        AtlasObjectId process1 = toAtlasObjectId(processExecutionEntity1.getRelationshipAttribute(
            BaseImpalaEvent.ATTRIBUTE_PROCESS));
        Assert.assertEquals(process1.getGuid(), processEntity1.getGuid());
        Assert.assertEquals(numberOfProcessExecutions(processEntity1), 1);

        String      guid       = assertTableIsRegistered(dbName, targetTableName);
        AtlasEntity entity     = atlasClientV2.getEntityByGuid(guid).getEntity();
        List        ddlQueries = (List) entity.getRelationshipAttribute(ATTRIBUTE_DDL_QUERIES);

        assertNotNull(ddlQueries);
        assertEquals(ddlQueries.size(), 1);
    }

    /**
     * This tests is based on extra comment and spaces adding to create table as select query
     * 1) ImpalaLineageTool can parse one lineage file that contains "create   table   as   select" command lineage,
     *    there is table vertex with createTime.
     * 2) Lineage is sent to Atlas
     * 3) Atlas can get this lineage from Atlas
     */
    @Test
    public void testCreateTableAsSelectWithCommentSpacesFromFile() throws Exception {
        String IMPALA = dir + "impalaCreateTableAsSelectWithCommentSpaces.json";
        String IMPALA_WAL = dir + "WALimpala.wal";

        ImpalaLineageHook impalaLineageHook = new ImpalaLineageHook();

        // create database and tables to simulate Impala behavior that Impala updates metadata
        // to HMS and HMSHook sends the metadata to Atlas, which has to happen before
        // Atlas can handle lineage notification
        String dbName = "db_9";
        createDatabase(dbName);

        String sourceTableName = "table_1";
        createTable(dbName, sourceTableName,"(id string, count int)", false);

        String targetTableName = "table_2";
        createTable(dbName, targetTableName,"(count int, id string)", false);

        // process lineage record, and send corresponding notification to Atlas
        String[] args = new String[]{"-d", "./", "-p", "impala"};
        ImpalaLineageTool toolInstance = new ImpalaLineageTool(args);
        toolInstance.importHImpalaEntities(impalaLineageHook, IMPALA, IMPALA_WAL);

        // verify the process is saved in Atlas
        // the value is from info in IMPALA_4.
        String createTime = new Long(TABLE_CREATE_TIME*1000).toString();
        String processQFName =
                dbName + "." + targetTableName + AtlasImpalaHookContext.QNAME_SEP_METADATA_NAMESPACE +
                        CLUSTER_NAME + AtlasImpalaHookContext.QNAME_SEP_PROCESS + createTime;

        processQFName = processQFName.toLowerCase();

        String queryString = "create   /* Test */   table " + dbName + "."
                + targetTableName + "   as /* Test */ select count, id from " + dbName + "." + sourceTableName;
        AtlasEntity processEntity1 = validateProcess(processQFName, queryString);
        AtlasEntity processExecutionEntity1 = validateProcessExecution(processEntity1, queryString);
        AtlasObjectId process1 = toAtlasObjectId(processExecutionEntity1.getRelationshipAttribute(
                BaseImpalaEvent.ATTRIBUTE_PROCESS));
        Assert.assertEquals(process1.getGuid(), processEntity1.getGuid());
        Assert.assertEquals(numberOfProcessExecutions(processEntity1), 1);

        String      guid       = assertTableIsRegistered(dbName, targetTableName);
        AtlasEntity entity     = atlasClientV2.getEntityByGuid(guid).getEntity();
        List        ddlQueries = (List) entity.getRelationshipAttribute(ATTRIBUTE_DDL_QUERIES);

        assertNotNull(ddlQueries);
        assertEquals(ddlQueries.size(), 1);
    }

    /**
     * This tests
     * 1) ImpalaLineageTool can parse one lineage file that contains "alter view as select" command lineage,
     *    there is table vertex with createTime.
     * 2) Lineage is sent to Atlas
     * 3) Atlas can get this lineage from Atlas
     */
    @Test
    public void testAlterViewAsSelectFromFile() throws Exception {
        String IMPALA = dir + "impalaAlterViewAsSelect.json";
        String IMPALA_WAL = dir + "WALimpala.wal";

        ImpalaLineageHook impalaLineageHook = new ImpalaLineageHook();

        // create database and tables to simulate Impala behavior that Impala updates metadata
        // to HMS and HMSHook sends the metadata to Atlas, which has to happen before
        // Atlas can handle lineage notification
        String dbName = "db_4";
        createDatabase(dbName);

        String sourceTableName = "table_1";
        createTable(dbName, sourceTableName,"(id string, count int)", false);

        String targetTableName = "view_1";
        createTable(dbName, targetTableName,"(count int, id string)", false);

        // process lineage record, and send corresponding notification to Atlas
        String[] args = new String[]{"-d", "./", "-p", "impala"};
        ImpalaLineageTool toolInstance = new ImpalaLineageTool(args);
        toolInstance.importHImpalaEntities(impalaLineageHook, IMPALA, IMPALA_WAL);

        // verify the process is saved in Atlas
        // the value is from info in IMPALA_4.
        String createTime = new Long(TABLE_CREATE_TIME*1000).toString();
        String processQFName =
            dbName + "." + targetTableName + AtlasImpalaHookContext.QNAME_SEP_METADATA_NAMESPACE +
                CLUSTER_NAME + AtlasImpalaHookContext.QNAME_SEP_PROCESS + createTime;

        processQFName = processQFName.toLowerCase();

        String queryString = "alter view " + dbName + "." + targetTableName + " as select count, id from " + dbName + "." + sourceTableName;
        AtlasEntity processEntity1 = validateProcess(processQFName, queryString);
        AtlasEntity processExecutionEntity1 = validateProcessExecution(processEntity1, queryString);
        AtlasObjectId process1 = toAtlasObjectId(processExecutionEntity1.getRelationshipAttribute(
            BaseImpalaEvent.ATTRIBUTE_PROCESS));
        Assert.assertEquals(process1.getGuid(), processEntity1.getGuid());
        Assert.assertEquals(numberOfProcessExecutions(processEntity1), 1);

        String      guid       = assertTableIsRegistered(dbName, targetTableName);
        AtlasEntity entity     = atlasClientV2.getEntityByGuid(guid).getEntity();
        List        ddlQueries = (List) entity.getRelationshipAttribute(ATTRIBUTE_DDL_QUERIES);

        assertNotNull(ddlQueries);
        assertEquals(ddlQueries.size(), 1);
    }

    /**
     * This tests is for extra comment and spaces present in alter view as select query
     * 1) ImpalaLineageTool can parse one lineage file that contains "alter view as select" command lineage,
     *    there is table vertex with createTime.
     * 2) Lineage is sent to Atlas
     * 3) Atlas can get this lineage from Atlas
     */
    @Test
    public void testAlterViewAsSelectWithCommentSpacesFromFile() throws Exception {
        String IMPALA = dir + "impalaAlterViewAsSelectWithCommentSpaces.json";
        String IMPALA_WAL = dir + "WALimpala.wal";

        ImpalaLineageHook impalaLineageHook = new ImpalaLineageHook();

        // create database and tables to simulate Impala behavior that Impala updates metadata
        // to HMS and HMSHook sends the metadata to Atlas, which has to happen before
        // Atlas can handle lineage notification
        String dbName = "db_10";
        createDatabase(dbName);

        String sourceTableName = "table_1";
        createTable(dbName, sourceTableName,"(id string, count int)", false);

        String targetTableName = "view_1";
        createTable(dbName, targetTableName,"(count int, id string)", false);

        // process lineage record, and send corresponding notification to Atlas
        String[] args = new String[]{"-d", "./", "-p", "impala"};
        ImpalaLineageTool toolInstance = new ImpalaLineageTool(args);
        toolInstance.importHImpalaEntities(impalaLineageHook, IMPALA, IMPALA_WAL);

        // verify the process is saved in Atlas
        // the value is from info in IMPALA_4.
        String createTime = new Long(TABLE_CREATE_TIME*1000).toString();
        String processQFName =
                dbName + "." + targetTableName + AtlasImpalaHookContext.QNAME_SEP_METADATA_NAMESPACE +
                        CLUSTER_NAME + AtlasImpalaHookContext.QNAME_SEP_PROCESS + createTime;

        processQFName = processQFName.toLowerCase();

        String queryString = "alter   /* comment1 */ view " + dbName + "." + targetTableName
                + " as   select /* comment1 */ count, id from " + dbName + "." + sourceTableName;
        AtlasEntity processEntity1 = validateProcess(processQFName, queryString);
        AtlasEntity processExecutionEntity1 = validateProcessExecution(processEntity1, queryString);
        AtlasObjectId process1 = toAtlasObjectId(processExecutionEntity1.getRelationshipAttribute(
                BaseImpalaEvent.ATTRIBUTE_PROCESS));
        Assert.assertEquals(process1.getGuid(), processEntity1.getGuid());
        Assert.assertEquals(numberOfProcessExecutions(processEntity1), 1);

        String      guid       = assertTableIsRegistered(dbName, targetTableName);
        AtlasEntity entity     = atlasClientV2.getEntityByGuid(guid).getEntity();
        List        ddlQueries = (List) entity.getRelationshipAttribute(ATTRIBUTE_DDL_QUERIES);

        assertNotNull(ddlQueries);
        assertEquals(ddlQueries.size(), 1);
    }

    /**
     * This tests
     * 1) ImpalaLineageTool can parse one lineage file that contains "insert into" command lineage,
     *    there is table vertex with createTime.
     * 2) Lineage is sent to Atlas
     * 3) Atlas can get this lineage from Atlas
     */
    @Test
    public void testInsertIntoAsSelectFromFile() throws Exception {
        String IMPALA = dir + "impalaInsertIntoAsSelect.json";
        String IMPALA_WAL = dir + "WALimpala.wal";

        ImpalaLineageHook impalaLineageHook = new ImpalaLineageHook();

        // create database and tables to simulate Impala behavior that Impala updates metadata
        // to HMS and HMSHook sends the metadata to Atlas, which has to happen before
        // Atlas can handle lineage notification
        String dbName = "db_5";
        createDatabase(dbName);

        String sourceTableName = "table_1";
        createTable(dbName, sourceTableName,"(id string, count int)", false);

        String targetTableName = "table_2";
        createTable(dbName, targetTableName,"(count int, id string, int_col int)", false);

        // process lineage record, and send corresponding notification to Atlas
        String[] args = new String[]{"-d", "./", "-p", "impala"};
        ImpalaLineageTool toolInstance = new ImpalaLineageTool(args);
        toolInstance.importHImpalaEntities(impalaLineageHook, IMPALA, IMPALA_WAL);

        // verify the process is saved in Atlas
        // the value is from info in IMPALA_4.
        String createTime1 = new Long(TABLE_CREATE_TIME_SOURCE*1000).toString();
        String createTime2 = new Long(TABLE_CREATE_TIME*1000).toString();
        String sourceQFName = dbName + "." + sourceTableName + AtlasImpalaHookContext.QNAME_SEP_METADATA_NAMESPACE +
            CLUSTER_NAME + AtlasImpalaHookContext.QNAME_SEP_PROCESS + createTime1;
        String targetQFName = dbName + "." + targetTableName + AtlasImpalaHookContext.QNAME_SEP_METADATA_NAMESPACE +
            CLUSTER_NAME + AtlasImpalaHookContext.QNAME_SEP_PROCESS + createTime2;
        String processQFName = "QUERY:" + sourceQFName.toLowerCase() + "->:INSERT:" + targetQFName.toLowerCase();

        String queryString = "insert into table " + dbName + "." + targetTableName + " (count, id) select count, id from " + dbName + "." + sourceTableName;
        AtlasEntity processEntity1 = validateProcess(processQFName, queryString);
        AtlasEntity processExecutionEntity1 = validateProcessExecution(processEntity1, queryString);
        AtlasObjectId process1 = toAtlasObjectId(processExecutionEntity1.getRelationshipAttribute(
            BaseImpalaEvent.ATTRIBUTE_PROCESS));
        Assert.assertEquals(process1.getGuid(), processEntity1.getGuid());
        Assert.assertEquals(numberOfProcessExecutions(processEntity1), 1);

        String      guid       = assertTableIsRegistered(dbName, targetTableName);
        AtlasEntity entity     = atlasClientV2.getEntityByGuid(guid).getEntity();
        List        ddlQueries = (List) entity.getRelationshipAttribute(ATTRIBUTE_DDL_QUERIES);

        assertNotNull(ddlQueries);
        assertEquals(ddlQueries.size(), 0);
    }

    /**
     * This tests
     * 1) ImpalaLineageTool can parse one lineage file that contains multiple "insert into" command lineages,
     *    there is table vertex with createTime.
     * 2) Lineage is sent to Atlas
     * 3) Atlas can get these lineages from Atlas
     */
    @Test
    public void testMultipleInsertIntoAsSelectFromFile() throws Exception {
        String IMPALA = dir + "impalaMultipleInsertIntoAsSelect1.json";
        String IMPALA_WAL = dir + "WALimpala.wal";

        ImpalaLineageHook impalaLineageHook = new ImpalaLineageHook();

        // create database and tables to simulate Impala behavior that Impala updates metadata
        // to HMS and HMSHook sends the metadata to Atlas, which has to happen before
        // Atlas can handle lineage notification
        String dbName = "db_6";
        createDatabase(dbName);

        String sourceTableName = "table_1";
        createTable(dbName, sourceTableName,"(id string, count int)", false);

        String targetTableName = "table_2";
        createTable(dbName, targetTableName,"(count int, id string, int_col int)", false);

        // process lineage record, and send corresponding notification to Atlas
        String[] args = new String[]{"-d", "./", "-p", "impala"};
        ImpalaLineageTool toolInstance = new ImpalaLineageTool(args);
        toolInstance.importHImpalaEntities(impalaLineageHook, IMPALA, IMPALA_WAL);

        // re-run the same lineage record, should have the same process entity and another process execution entity
        Thread.sleep(5000);
        IMPALA = dir + "impalaMultipleInsertIntoAsSelect2.json";
        toolInstance.importHImpalaEntities(impalaLineageHook, IMPALA, IMPALA_WAL);
        Thread.sleep(5000);

        // verify the process is saved in Atlas
        // the value is from info in IMPALA_4.
        String createTime1 = new Long(TABLE_CREATE_TIME_SOURCE*1000).toString();
        String createTime2 = new Long(TABLE_CREATE_TIME*1000).toString();
        String sourceQFName = dbName + "." + sourceTableName + AtlasImpalaHookContext.QNAME_SEP_METADATA_NAMESPACE +
            CLUSTER_NAME + AtlasImpalaHookContext.QNAME_SEP_PROCESS + createTime1;
        String targetQFName = dbName + "." + targetTableName + AtlasImpalaHookContext.QNAME_SEP_METADATA_NAMESPACE +
            CLUSTER_NAME + AtlasImpalaHookContext.QNAME_SEP_PROCESS + createTime2;
        String processQFName = "QUERY:" + sourceQFName.toLowerCase() + "->:INSERT:" + targetQFName.toLowerCase();

        String queryString = "insert into table " + dbName + "." + targetTableName + " (count, id) select count, id from " + dbName + "." + sourceTableName;
        queryString = queryString.toLowerCase().trim();
        String queryString2 = queryString;

        Thread.sleep(5000);
        AtlasEntity processEntity1 = validateProcess(processQFName, queryString);

        List<AtlasObjectId> processExecutions = toAtlasObjectIdList(processEntity1.getRelationshipAttribute(
            BaseImpalaEvent.ATTRIBUTE_PROCESS_EXECUTIONS));
        Assert.assertEquals(processExecutions.size(), 2);
        for (AtlasObjectId processExecutionId : processExecutions) {
            AtlasEntity.AtlasEntityWithExtInfo atlasEntityWithExtInfo = atlasClientV2.
                getEntityByGuid(processExecutionId.getGuid());

            AtlasEntity processExecutionEntity = atlasEntityWithExtInfo.getEntity();
            String entityQueryText = String.valueOf(processExecutionEntity.getAttribute(ATTRIBUTE_QUERY_TEXT)).toLowerCase().trim();
            if (!(queryString.equalsIgnoreCase(entityQueryText) || queryString2.equalsIgnoreCase(entityQueryText))) {
                String errorMessage = String.format("process query text '%s' does not match expected value of '%s' or '%s'", entityQueryText, queryString, queryString2);
                Assert.assertTrue(false, errorMessage);
            }
        }

        String      guid       = assertTableIsRegistered(dbName, targetTableName);
        AtlasEntity entity     = atlasClientV2.getEntityByGuid(guid).getEntity();
        List        ddlQueries = (List) entity.getRelationshipAttribute(ATTRIBUTE_DDL_QUERIES);

        assertNotNull(ddlQueries);
        assertEquals(ddlQueries.size(), 0);
    }

    /**
     * This tests
     * 1) ImpalaLineageTool can parse one lineage file that contains "create table as select" command lineage,
     *    there is table vertex with createTime. The target vertex's vertexId does not contain db name and table name
     * 2) Lineage is sent to Atlas
     * 3) Atlas can get this lineage from Atlas
     */
    @Test
    public void testCreateTableAsSelectVertexIdNoTableNameFromFile() throws Exception {
        String IMPALA = dir + "impalaCreateTableAsSelectVertexIdNoTableName.json";
        String IMPALA_WAL = dir + "WALimpala.wal";

        ImpalaLineageHook impalaLineageHook = new ImpalaLineageHook();

        // create database and tables to simulate Impala behavior that Impala updates metadata
        // to HMS and HMSHook sends the metadata to Atlas, which has to happen before
        // Atlas can handle lineage notification
        String dbName = "sales_db";
        createDatabase(dbName);

        String sourceTableName = "sales_asia";
        createTable(dbName, sourceTableName,"(id string, name string)", false);

        String targetTableName = "sales_china";
        createTable(dbName, targetTableName,"(id string, name string)", false);

        // process lineage record, and send corresponding notification to Atlas
        String[] args = new String[]{"-d", "./", "-p", "impala"};
        ImpalaLineageTool toolInstance = new ImpalaLineageTool(args);
        toolInstance.importHImpalaEntities(impalaLineageHook, IMPALA, IMPALA_WAL);

        // verify the process is saved in Atlas
        // the value is from info in IMPALA_4.
        String createTime = new Long((long)1560885039*1000).toString();
        String processQFName =
            dbName + "." + targetTableName + AtlasImpalaHookContext.QNAME_SEP_METADATA_NAMESPACE +
                CLUSTER_NAME + AtlasImpalaHookContext.QNAME_SEP_PROCESS + createTime;

        processQFName = processQFName.toLowerCase();

        String queryString = "create table " + targetTableName + " as select * from " + sourceTableName;
        AtlasEntity processEntity1 = validateProcess(processQFName, queryString);
        AtlasEntity processExecutionEntity1 = validateProcessExecution(processEntity1, queryString);
        AtlasObjectId process1 = toAtlasObjectId(processExecutionEntity1.getRelationshipAttribute(
            BaseImpalaEvent.ATTRIBUTE_PROCESS));
        Assert.assertEquals(process1.getGuid(), processEntity1.getGuid());
        Assert.assertEquals(numberOfProcessExecutions(processEntity1), 1);

        String      guid       = assertTableIsRegistered(dbName, targetTableName);
        AtlasEntity entity     = atlasClientV2.getEntityByGuid(guid).getEntity();
        List        ddlQueries = (List) entity.getRelationshipAttribute(ATTRIBUTE_DDL_QUERIES);

        assertNotNull(ddlQueries);
        assertEquals(ddlQueries.size(), 1);
    }
}