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

import org.apache.atlas.AtlasServiceException;
import org.apache.atlas.hive.HiveITBase;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntityWithExtInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static org.apache.atlas.hive.hook.events.BaseHiveEvent.ATTRIBUTE_DDL_QUERIES;
import static org.apache.atlas.model.instance.AtlasEntity.Status.ACTIVE;
import static org.apache.atlas.model.instance.AtlasEntity.Status.DELETED;
import static org.testng.AssertJUnit.*;

public class HiveMetastoreHookIT extends HiveITBase {
    private static final Logger LOG = LoggerFactory.getLogger(HiveMetastoreHookIT.class);

    @Test (priority = 1)
    public void testCreateDatabase() throws Exception {
        String dbName = dbName();
        String query  = "CREATE DATABASE " + dbName;

        runCommand(query);
        String dbId          = assertDatabaseIsRegistered(dbName);
        AtlasEntity dbEntity = getAtlasEntity(dbId);

        assertEquals(((List) dbEntity.getRelationshipAttribute(ATTRIBUTE_DDL_QUERIES)).size(), 0);
    }

    @Test (priority = 2)
    public void testAlterDatabase() throws Exception {
        String dbName = dbName();
        String query  = "CREATE DATABASE " + dbName;

        runCommand(query);
        String dbId = assertDatabaseIsRegistered(dbName);

        AtlasEntity dbEntity = getAtlasEntity(dbId);
        assertNotNull(dbEntity);

        // SET DBPROPERTIES
        query = "ALTER DATABASE " + dbName + " SET DBPROPERTIES (\"prop1\"=\"val1\", \"prop2\"=\"val2\")";
        runCommandWithDelay(query);

        dbEntity = getAtlasEntity(dbId);
        Map parameters = (Map) dbEntity.getAttribute("parameters");

        assertEquals(((List) dbEntity.getRelationshipAttribute(ATTRIBUTE_DDL_QUERIES)).size(), 0);
        assertNotNull(parameters);
        assertEquals(2, parameters.size());

        // SET OWNER to 'hive'
        query = "ALTER DATABASE " + dbName + " SET OWNER USER hive";
        runCommandWithDelay(query);

        dbEntity = getAtlasEntity(dbId);


        assertEquals(((List) dbEntity.getRelationshipAttribute(ATTRIBUTE_DDL_QUERIES)).size(), 0);
        assertEquals(dbEntity.getAttribute("owner"), "hive");
        assertEquals(dbEntity.getAttribute("ownerType"), "USER");

        // SET LOCATION
        String hdfsPath = "hdfs://localhost:8020/warehouse/tablespace/managed/dwx/new_db.db";

        query = String.format("ALTER DATABASE %s SET LOCATION \"%s\"", dbName, hdfsPath);
        runCommandWithDelay(query);

        dbEntity = getAtlasEntity(dbId);

        assertEquals(((List) dbEntity.getRelationshipAttribute(ATTRIBUTE_DDL_QUERIES)).size(), 0);

        String location = (String) dbEntity.getAttribute("location");
        assertEquals(location, hdfsPath);
    }

    @Test (priority = 3)
    public void testDropDatabase() throws Exception {
        String dbName = dbName();
        String query  = "CREATE DATABASE " + dbName;

        runCommand(query);
        String dbId = assertDatabaseIsRegistered(dbName);

        AtlasEntity dbEntity = getAtlasEntity(dbId);
        assertNotNull(dbEntity);

        query = "DROP DATABASE " + dbName;
        runCommand(query);
        assertDatabaseIsNotRegistered(dbName);

        dbEntity = getAtlasEntity(dbId);
        assertEquals(dbEntity.getStatus(), DELETED);
    }

    @Test (priority = 4)
    public void testDropDatabaseWithTables() throws Exception {
        String dbName = dbName();
        String query  = "CREATE DATABASE " + dbName;

        runCommandWithDelay(query);
        String dbId = assertDatabaseIsRegistered(dbName);
        assertEquals(getAtlasEntity(dbId).getStatus(), ACTIVE);

        String table1 = tableName();
        runCommandWithDelay("CREATE TABLE " + dbName + "." + table1 + " (name string, age int, dob date)");
        String table1Id = assertTableIsRegistered(dbName, table1);
        assertEquals(getAtlasEntity(table1Id).getStatus(), ACTIVE);

        String table2 = tableName();
        runCommandWithDelay("CREATE TABLE " + dbName + "." + table2 + " (name string, age int, dob date)");
        String table2Id = assertTableIsRegistered(dbName, table2);
        assertEquals(getAtlasEntity(table2Id).getStatus(), ACTIVE);

        query = "DROP DATABASE " + dbName + " CASCADE";
        runCommandWithDelay(query);
        assertDatabaseIsNotRegistered(dbName);

        assertEquals(getAtlasEntity(dbId).getStatus(), DELETED);
        assertEquals(getAtlasEntity(table1Id).getStatus(), DELETED);
        assertEquals(getAtlasEntity(table2Id).getStatus(), DELETED);
    }

    @Test (priority = 5)
    public void testCreateTable() throws Exception {
        String dbName = dbName();
        String query  = "CREATE DATABASE " + dbName;

        runCommand(query);
        String dbId = assertDatabaseIsRegistered(dbName);
        assertEquals(getAtlasEntity(dbId).getStatus(), ACTIVE);

        String tableName      = tableName();
        runCommand("CREATE TABLE " + dbName + "." + tableName + " (name string, age int, dob date)");
        String tblId          = assertTableIsRegistered(dbName, tableName);
        AtlasEntity tblEntity = getAtlasEntity(tblId);

        assertEquals(((List) tblEntity.getRelationshipAttribute(ATTRIBUTE_DDL_QUERIES)).size(), 0);
        assertEquals(getAtlasEntity(tblId).getStatus(), ACTIVE);
    }

    @Test (priority = 6)
    public void testCreateView() throws Exception {
        String dbName = dbName();
        String query  = "CREATE DATABASE " + dbName;

        runCommand(query);
        String dbId = assertDatabaseIsRegistered(dbName);
        assertEquals(getAtlasEntity(dbId).getStatus(), ACTIVE);

        String tableName = tableName();
        runCommand("CREATE TABLE " + dbName + "." + tableName + " (name string, age int, dob date)");
        String tblId = assertTableIsRegistered(dbName, tableName);
        assertEquals(getAtlasEntity(tblId).getStatus(), ACTIVE);

        String viewName = tableName();

        runCommand("CREATE VIEW " + dbName + "." + viewName + " AS SELECT * FROM " + dbName + "." + tableName);

        tblId                 = assertTableIsRegistered(dbName, viewName);
        AtlasEntity tblEntity = getAtlasEntity(tblId);

        assertEquals(((List) tblEntity.getRelationshipAttribute(ATTRIBUTE_DDL_QUERIES)).size(), 0);
        assertEquals(getAtlasEntity(tblId).getStatus(), ACTIVE);
    }

    @Test (priority = 7)
    public void testAlterTableProperties() throws Exception {
        String dbName = dbName();
        String query  = "CREATE DATABASE " + dbName;

        runCommand(query);
        String dbId = assertDatabaseIsRegistered(dbName);
        assertEquals(getAtlasEntity(dbId).getStatus(), ACTIVE);
        assertEquals(((List) getAtlasEntity(dbId).getRelationshipAttribute(ATTRIBUTE_DDL_QUERIES)).size(), 0);

        String tableName = tableName();
        runCommand("CREATE TABLE " + dbName + "." + tableName + " (name string, age int, dob date)");
        String tblId = assertTableIsRegistered(dbName, tableName);
        assertEquals(getAtlasEntity(tblId).getStatus(), ACTIVE);
        assertEquals(((List) getAtlasEntity(tblId).getRelationshipAttribute(ATTRIBUTE_DDL_QUERIES)).size(), 0);

        // SET TBLPROPERTIES
        query = "ALTER TABLE " + dbName + "." + tableName + " SET TBLPROPERTIES (\"prop1\"=\"val1\", \"prop2\"=\"val2\", \"prop3\"=\"val3\")";
        runCommandWithDelay(query);

        query = "ALTER TABLE " + dbName + "." + tableName + " SET TBLPROPERTIES (\"comment\" = \"sample comment\")";
        runCommandWithDelay(query);

        // SET SERDE
        query = "ALTER TABLE " + dbName + "." + tableName + " SET SERDE \"org.apache.hadoop.hive.ql.io.orc.OrcSerde\" WITH SERDEPROPERTIES (\"prop1\"=\"val1\", \"prop2\"=\"val2\")";
        runCommandWithDelay(query);

        // SET SERDEPROPERTIES
        query = "ALTER TABLE " + dbName + "." + tableName + " SET SERDEPROPERTIES (\"prop1\"=\"val1\", \"prop2\"=\"val2\")";
        runCommandWithDelay(query);

        AtlasEntity         tableEntity     = getAtlasEntity(tblId);
        Map<String, Object> tableParameters = (Map) tableEntity.getAttribute("parameters");

        assertEquals(tableParameters.get("comment"), "sample comment");
        assertEquals(tableParameters.get("prop1"), "val1");
        assertEquals(tableParameters.get("prop2"), "val2");
        assertEquals(tableParameters.get("prop3"), "val3");

        AtlasEntity sdEntity   = getAtlasEntity((String) ((Map) tableEntity.getAttribute("sd")).get("guid"));
        Map         serdeInfo  = (Map) sdEntity.getAttribute("serdeInfo");
        Map         serdeAttrs = (Map) serdeInfo.get("attributes");

        assertEquals(serdeAttrs.get("serializationLib"), "org.apache.hadoop.hive.ql.io.orc.OrcSerde");
        assertEquals(((Map) serdeAttrs.get("parameters")).get("prop1"), "val1");
        assertEquals(((Map) serdeAttrs.get("parameters")).get("prop2"), "val2");
        assertEquals(((List) tableEntity.getRelationshipAttribute(ATTRIBUTE_DDL_QUERIES)).size(), 0);
    }

    @Test (priority = 8)
    public void testAlterTableRenameTableName() throws Exception {
        String dbName = dbName();
        String query  = "CREATE DATABASE " + dbName;

        runCommand(query);
        String dbId = assertDatabaseIsRegistered(dbName);
        assertEquals(getAtlasEntity(dbId).getStatus(), ACTIVE);

        String tableName = tableName();
        runCommand("CREATE TABLE " + dbName + "." + tableName + " (name string, age int, dob date)");
        String tblId = assertTableIsRegistered(dbName, tableName);
        assertEquals(getAtlasEntity(tblId).getStatus(), ACTIVE);

        // RENAME TABLE NAME
        String newTableName = tableName + "_new";
        query = "ALTER TABLE " + dbName + "." + tableName + " RENAME TO " + dbName + "." + newTableName;
        runCommandWithDelay(query);

        AtlasEntityWithExtInfo tableEntityWithExtInfo = getAtlasEntityWithExtInfo(tblId);
        AtlasEntity            tableEntity            = tableEntityWithExtInfo.getEntity();

        assertEquals(((List) tableEntity.getRelationshipAttribute(ATTRIBUTE_DDL_QUERIES)).size(), 0);

        // validate table rename in table entity
        assertEquals(newTableName, tableEntity.getAttribute("name"));
        assertTrue(((String) tableEntity.getAttribute("qualifiedName")).contains(newTableName));

        // validate table rename in column and sd entity
        for (AtlasEntity referredEntity : tableEntityWithExtInfo.getReferredEntities().values()) {
            assertTrue(((String) referredEntity.getAttribute("qualifiedName")).contains(newTableName));
        }
    }

    @Test (priority = 9)
    public void testAlterTableRenameColumnName() throws Exception {
        String dbName = dbName();
        String query  = "CREATE DATABASE " + dbName;

        runCommand(query);
        String dbId = assertDatabaseIsRegistered(dbName);
        assertEquals(getAtlasEntity(dbId).getStatus(), ACTIVE);

        String tableName = tableName();
        runCommand("CREATE TABLE " + dbName + "." + tableName + " (col1 int, col2 int, col3 int)");
        String tblId = assertTableIsRegistered(dbName, tableName);
        AtlasEntityWithExtInfo tableEntityWithExtInfo = getAtlasEntityWithExtInfo(tblId);

        assertEquals(tableEntityWithExtInfo.getEntity().getStatus(), ACTIVE);

        String col1Id = getColumnId(tableEntityWithExtInfo, "col1");
        String col2Id = getColumnId(tableEntityWithExtInfo, "col2");

        // RENAME COLUMN NAME
        query = "ALTER TABLE " + dbName + "." + tableName + " CHANGE col1 col11 int";
        runCommandWithDelay(query);

        AtlasEntity col1Entity = getAtlasEntity(col1Id);
        assertEquals(col1Entity.getAttribute("name"), "col11");
        assertTrue(((String) col1Entity.getAttribute("qualifiedName")).contains("col11"));

        // CHANGE COLUMN NAME and DATATYPE
        query = "ALTER TABLE " + dbName + "." + tableName + " CHANGE col2 col22 string";
        runCommandWithDelay(query);

        AtlasEntity col2Entity = getAtlasEntity(col2Id);
        assertEquals(col2Entity.getAttribute("name"), "col22");
        assertEquals(col2Entity.getAttribute("type"), "string");
        assertEquals(((List) getAtlasEntity(tblId).getRelationshipAttribute(ATTRIBUTE_DDL_QUERIES)).size(), 0);
    }

    @Test (priority = 10)
    public void testDropTable() throws Exception {
        String dbName = dbName();
        String query  = "CREATE DATABASE " + dbName;

        runCommand(query);
        String dbId = assertDatabaseIsRegistered(dbName);
        assertEquals(getAtlasEntity(dbId).getStatus(), ACTIVE);

        String tableName = tableName();
        runCommand("CREATE TABLE " + dbName + "." + tableName + " (name string, age int, dob date)");
        String tblId = assertTableIsRegistered(dbName, tableName);
        assertEquals(getAtlasEntity(tblId).getStatus(), ACTIVE);

        query = "DROP TABLE " + dbName + "." + tableName;
        runCommandWithDelay(query);

        assertEquals(getAtlasEntity(tblId).getStatus(), DELETED);
    }

    @Test (priority = 11)
    public void testDropView() throws Exception {
        String dbName = dbName();
        String query  = "CREATE DATABASE " + dbName;

        runCommand(query);
        String dbId = assertDatabaseIsRegistered(dbName);
        assertEquals(getAtlasEntity(dbId).getStatus(), ACTIVE);

        String tableName = tableName();
        runCommand("CREATE TABLE " + dbName + "." + tableName + " (name string, age int, dob date)");
        String tblId = assertTableIsRegistered(dbName, tableName);
        assertEquals(getAtlasEntity(tblId).getStatus(), ACTIVE);

        String viewName = tableName();
        runCommand("CREATE VIEW " + dbName + "." + viewName + " AS SELECT * FROM " + dbName + "." + tableName);
        tblId = assertTableIsRegistered(dbName, viewName);
        assertEquals(getAtlasEntity(tblId).getStatus(), ACTIVE);

        query = "DROP VIEW " + dbName + "." + viewName;
        runCommandWithDelay(query);

        assertEquals(getAtlasEntity(tblId).getStatus(), DELETED);
    }

    private String getColumnId(AtlasEntityWithExtInfo entityWithExtInfo, String columnName) {
        String ret = null;

        for (AtlasEntity entity : entityWithExtInfo.getReferredEntities().values()) {

            if (entity.getTypeName().equals("hive_column") && entity.getAttribute("name").equals(columnName)) {
                ret = entity.getGuid();
                break;
            }
        }

        return ret;
    }

    private AtlasEntity getAtlasEntity(String guid) throws AtlasServiceException {
        return atlasClientV2.getEntityByGuid(guid).getEntity();
    }

    private AtlasEntityWithExtInfo getAtlasEntityWithExtInfo(String guid) throws AtlasServiceException {
        return atlasClientV2.getEntityByGuid(guid);
    }

    protected void runCommand(String cmd) throws Exception {
        runCommandWithDelay(driverWithoutContext, cmd, 0);
    }

    protected void runCommandWithDelay(String cmd) throws Exception {
        int delayTimeInMs = 10000;
        runCommandWithDelay(driverWithoutContext, cmd, delayTimeInMs);
    }
}