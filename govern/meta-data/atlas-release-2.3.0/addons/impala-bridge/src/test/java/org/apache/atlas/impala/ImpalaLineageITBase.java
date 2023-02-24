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

import static org.apache.atlas.impala.hook.events.BaseImpalaEvent.ATTRIBUTE_QUALIFIED_NAME;
import static org.apache.atlas.impala.hook.events.BaseImpalaEvent.ATTRIBUTE_QUERY_TEXT;
import static org.apache.atlas.impala.hook.events.BaseImpalaEvent.ATTRIBUTE_RECENT_QUERIES;
import static org.apache.atlas.impala.hook.events.BaseImpalaEvent.HIVE_TYPE_DB;
import static org.apache.atlas.impala.hook.events.BaseImpalaEvent.HIVE_TYPE_TABLE;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.atlas.ApplicationProperties;
import org.apache.atlas.AtlasClientV2;
import org.apache.atlas.impala.hook.AtlasImpalaHookContext;
import org.apache.atlas.impala.hook.ImpalaLineageHook;
import org.apache.atlas.impala.hook.events.BaseImpalaEvent;
import org.apache.atlas.impala.model.ImpalaDataType;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasObjectId;
import org.apache.atlas.utils.AuthenticationUtil;
import org.apache.atlas.utils.ParamChecker;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.ql.Driver;
import org.apache.hadoop.hive.ql.processors.CommandProcessorResponse;
import org.apache.hadoop.hive.ql.session.SessionState;
import org.testng.annotations.BeforeClass;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

public class ImpalaLineageITBase {
    private static final Logger LOG = LoggerFactory.getLogger(ImpalaLineageITBase.class);

    public static final String DEFAULT_DB = "default";
    public static final String SEP = ":".intern();
    public static final String IO_SEP = "->".intern();
    protected static final String DGI_URL = "http://localhost:21000/";
    protected static final String CLUSTER_NAME = "primary";
    protected static final String PART_FILE = "2015-01-01";
    protected static final String INPUTS = "inputs";
    protected static final String OUTPUTS = "outputs";
    protected static AtlasClientV2 atlasClientV2;

    private static final String REFERENCEABLE_ATTRIBUTE_NAME = "qualifiedName";
    private static final String ATTR_NAME = "name";

    // to push entity creation/update to HMS, so HMS hook can push the metadata notification
    // to Atlas, then the lineage notification from this tool can be created at Atlas
    protected static Driver              driverWithoutContext;
    protected static SessionState        ss;
    protected static HiveConf            conf;


    @BeforeClass
    public void setUp() throws Exception {
        //Set-up hive session
        conf = new HiveConf();
        conf.setClassLoader(Thread.currentThread().getContextClassLoader());
        HiveConf conf = new HiveConf();
        SessionState ss = new SessionState(conf);
        ss = SessionState.start(ss);
        SessionState.setCurrentSessionState(ss);
        driverWithoutContext = new Driver(conf);

        Configuration configuration = ApplicationProperties.get();

        String[] atlasEndPoint = configuration.getStringArray(ImpalaLineageHook.ATLAS_ENDPOINT);
        if (atlasEndPoint == null || atlasEndPoint.length == 0) {
            atlasEndPoint = new String[]{DGI_URL};
        }

        if (!AuthenticationUtil.isKerberosAuthenticationEnabled()) {
            atlasClientV2 = new AtlasClientV2(atlasEndPoint, new String[]{"admin", "admin"});
        } else {
            atlasClientV2 = new AtlasClientV2(atlasEndPoint);
        }

    }

    // return guid of the entity
    protected String assertEntityIsRegistered(final String typeName, final String property, final String value,
        final AssertPredicate assertPredicate) throws Exception {
        waitFor(100000, new Predicate() {
            @Override
            public void evaluate() throws Exception {
                AtlasEntity.AtlasEntityWithExtInfo atlasEntityWithExtInfo = atlasClientV2.getEntityByAttribute(typeName, Collections
                    .singletonMap(property,value));
                AtlasEntity entity = atlasEntityWithExtInfo.getEntity();
                assertNotNull(entity);
                if (assertPredicate != null) {
                    assertPredicate.assertOnEntity(entity);
                }
            }
        });
        AtlasEntity.AtlasEntityWithExtInfo atlasEntityWithExtInfo = atlasClientV2.getEntityByAttribute(typeName, Collections.singletonMap(property,value));
        AtlasEntity entity = atlasEntityWithExtInfo.getEntity();
        return (String) entity.getGuid();
    }

    protected String assertEntityIsRegistered(final String typeName, List<String> processQFNames,
        final AssertPredicates assertPredicates) throws Exception {
        List<Map<String, String>> attributesList = new ArrayList<>();

        for (String processName : processQFNames) {
            attributesList.add(Collections.singletonMap(ATTRIBUTE_QUALIFIED_NAME, processName));
        }

        return waitForWithReturn(80000, new PredicateWithReturn() {
            @Override
            public String evaluate() throws Exception {
                AtlasEntity.AtlasEntitiesWithExtInfo atlasEntitiesWithExtInfo = atlasClientV2.getEntitiesByAttribute(typeName, attributesList);
                List<AtlasEntity> entities = atlasEntitiesWithExtInfo.getEntities();
                assertNotNull(entities);
                if (assertPredicates != null) {
                    return assertPredicates.assertOnEntities(entities);
                }

                return null;
            }
        });
    }

    protected String assertEntityIsRegisteredViaGuid(String guid,
        final AssertPredicate assertPredicate) throws Exception {
        waitFor(80000, new Predicate() {
            @Override
            public void evaluate() throws Exception {
                AtlasEntity.AtlasEntityWithExtInfo atlasEntityWithExtInfo = atlasClientV2.getEntityByGuid(guid);
                AtlasEntity entity = atlasEntityWithExtInfo.getEntity();
                assertNotNull(entity);
                if (assertPredicate != null) {
                    assertPredicate.assertOnEntity(entity);
                }

            }
        });
        AtlasEntity.AtlasEntityWithExtInfo atlasEntityWithExtInfo = atlasClientV2.getEntityByGuid(guid);
        AtlasEntity entity = atlasEntityWithExtInfo.getEntity();
        return (String) entity.getGuid();
    }


    protected String assertProcessIsRegistered(List<String> processQFNames, String queryString) throws Exception {
        try {
            Thread.sleep(5000);

            LOG.debug("Searching for process with query {}", queryString);

            return assertEntityIsRegistered(ImpalaDataType.IMPALA_PROCESS.getName(), processQFNames, new AssertPredicates() {
                @Override
                public String assertOnEntities(final List<AtlasEntity> entities) throws Exception {
                    for (AtlasEntity entity : entities) {
                        List<String> recentQueries = (List<String>) entity
                            .getAttribute(ATTRIBUTE_RECENT_QUERIES);

                        if (queryString.equalsIgnoreCase(recentQueries.get(0)))
                            return entity.getGuid();

                    }

                    throw new IllegalStateException("Not found entity with matching query");
                }
            });
        } catch(Exception e) {
            LOG.error("Exception : ", e);
            throw e;
        }
    }

    protected String assertProcessIsRegistered(String processQFName, String queryString) throws Exception {
        try {
            Thread.sleep(5000);

            LOG.debug("Searching for process with qualified name {} and query {}", processQFName, queryString);

            return assertEntityIsRegistered(ImpalaDataType.IMPALA_PROCESS.getName(), ATTRIBUTE_QUALIFIED_NAME, processQFName, new AssertPredicate() {
                @Override
                public void assertOnEntity(final AtlasEntity entity) throws Exception {
                    List<String> recentQueries = (List<String>) entity.getAttribute(ATTRIBUTE_RECENT_QUERIES);

                    Assert.assertEquals(recentQueries.get(0), lower(queryString));
                }
            });
        } catch(Exception e) {
            LOG.error("Exception : ", e);
            throw e;
        }
    }

    private String assertProcessExecutionIsRegistered(AtlasEntity impalaProcess, final String queryString) throws Exception {
        try {
            Thread.sleep(5000);

            String guid = "";
            List<AtlasObjectId> processExecutions = toAtlasObjectIdList(impalaProcess.getRelationshipAttribute(
                BaseImpalaEvent.ATTRIBUTE_PROCESS_EXECUTIONS));
            for (AtlasObjectId processExecution : processExecutions) {
                AtlasEntity.AtlasEntityWithExtInfo atlasEntityWithExtInfo = atlasClientV2.
                    getEntityByGuid(processExecution.getGuid());

                AtlasEntity entity = atlasEntityWithExtInfo.getEntity();
                if (String.valueOf(entity.getAttribute(ATTRIBUTE_QUERY_TEXT)).equals(queryString.toLowerCase().trim())) {
                    guid = entity.getGuid();
                    break;
                }
            }

            return assertEntityIsRegisteredViaGuid(guid, new AssertPredicate() {
                @Override
                public void assertOnEntity(final AtlasEntity entity) throws Exception {
                    String queryText = (String) entity.getAttribute(ATTRIBUTE_QUERY_TEXT);
                    Assert.assertEquals(queryText, queryString.toLowerCase().trim());
                }
            });
        } catch(Exception e) {
            LOG.error("Exception : ", e);
            throw e;
        }
    }

    protected AtlasObjectId toAtlasObjectId(Object obj) {
        final AtlasObjectId ret;

        if (obj instanceof AtlasObjectId) {
            ret = (AtlasObjectId) obj;
        } else if (obj instanceof Map) {
            ret = new AtlasObjectId((Map) obj);
        } else if (obj != null) {
            ret = new AtlasObjectId(obj.toString()); // guid
        } else {
            ret = null;
        }

        return ret;
    }

    protected List<AtlasObjectId> toAtlasObjectIdList(Object obj) {
        final List<AtlasObjectId> ret;

        if (obj instanceof Collection) {
            Collection coll = (Collection) obj;

            ret = new ArrayList<>(coll.size());

            for (Object item : coll) {
                AtlasObjectId objId = toAtlasObjectId(item);

                if (objId != null) {
                    ret.add(objId);
                }
            }
        } else {
            AtlasObjectId objId = toAtlasObjectId(obj);

            if (objId != null) {
                ret = new ArrayList<>(1);

                ret.add(objId);
            } else {
                ret = null;
            }
        }

        return ret;
    }


    protected String assertDatabaseIsRegistered(String dbName) throws Exception {
        return assertDatabaseIsRegistered(dbName, null);
    }

    protected String assertDatabaseIsRegistered(String dbName, AssertPredicate assertPredicate) throws Exception {
        LOG.debug("Searching for database: {}", dbName);

        String dbQualifiedName = dbName + AtlasImpalaHookContext.QNAME_SEP_METADATA_NAMESPACE +
            CLUSTER_NAME;

        dbQualifiedName = dbQualifiedName.toLowerCase();

        return assertEntityIsRegistered(HIVE_TYPE_DB, REFERENCEABLE_ATTRIBUTE_NAME, dbQualifiedName, assertPredicate);
    }

    protected String assertTableIsRegistered(String dbName, String tableName) throws Exception {
        return assertTableIsRegistered(dbName, tableName, null, false);
    }

    protected String assertTableIsRegistered(String fullTableName) throws Exception {
        return assertTableIsRegistered(fullTableName, null, false);
    }

    protected String assertTableIsRegistered(String dbName, String tableName, AssertPredicate assertPredicate, boolean isTemporary) throws Exception {
        LOG.debug("Searching for table {}.{}", dbName, tableName);

        String fullTableName = dbName + AtlasImpalaHookContext.QNAME_SEP_ENTITY_NAME + tableName;

        return assertTableIsRegistered(fullTableName, assertPredicate, isTemporary);
    }

    protected String assertTableIsRegistered(String fullTableName, AssertPredicate assertPredicate, boolean isTemporary) throws Exception {
        LOG.debug("Searching for table {}", fullTableName);

        String tableQualifiedName = (fullTableName + AtlasImpalaHookContext.QNAME_SEP_METADATA_NAMESPACE).toLowerCase() +
            CLUSTER_NAME;

        return assertEntityIsRegistered(HIVE_TYPE_TABLE, REFERENCEABLE_ATTRIBUTE_NAME, tableQualifiedName,
            assertPredicate);
    }

    protected String createDatabase() throws Exception {
        String dbName = dbName();

        return createDatabase(dbName);
    }

    protected  String createDatabase(String dbName) throws Exception {
        runCommandWithDelay("CREATE DATABASE IF NOT EXISTS " + dbName, 3000);

        return dbName;
    }

    protected String createTable(String dbName, String columnsString) throws Exception {
        return createTable(dbName, columnsString, false);
    }

    protected String createTable(String dbName, String columnsString, boolean isPartitioned) throws Exception {
        String tableName = tableName();
        return createTable(dbName, tableName, columnsString, isPartitioned);
    }

    protected String createTable(String dbName, String tableName, String columnsString, boolean isPartitioned) throws Exception {
        runCommandWithDelay("CREATE TABLE IF NOT EXISTS " + dbName + "." + tableName + " " + columnsString + " comment 'table comment' " + (isPartitioned ? " partitioned by(dt string)" : ""), 3000);

        return dbName + "." + tableName;
    }

    protected AtlasEntity validateProcess(String processQFName, String queryString) throws Exception {
        String      processId     = assertProcessIsRegistered(processQFName, queryString);
        AtlasEntity processEntity = atlasClientV2.getEntityByGuid(processId).getEntity();

        return processEntity;
    }

    protected AtlasEntity validateProcess(List<String> processQFNames, String queryString) throws Exception {
        String      processId     = assertProcessIsRegistered(processQFNames, queryString);
        AtlasEntity processEntity = atlasClientV2.getEntityByGuid(processId).getEntity();

        return processEntity;
    }

    protected AtlasEntity validateProcessExecution(AtlasEntity impalaProcess, String queryString) throws Exception {
        String      processExecutionId     = assertProcessExecutionIsRegistered(impalaProcess, queryString);
        AtlasEntity processExecutionEntity = atlasClientV2.getEntityByGuid(processExecutionId).getEntity();
        return processExecutionEntity;
    }

    protected int numberOfProcessExecutions(AtlasEntity impalaProcess) {
        return toAtlasObjectIdList(impalaProcess.getRelationshipAttribute(
            BaseImpalaEvent.ATTRIBUTE_PROCESS_EXECUTIONS)).size();
    }

    public interface AssertPredicate {
        void assertOnEntity(AtlasEntity entity) throws Exception;
    }

    public interface AssertPredicates {
        String assertOnEntities(List<AtlasEntity> entities) throws Exception;
    }

    public interface PredicateWithReturn {
        /**
         * Perform a predicate evaluation.
         *
         * @return the boolean result of the evaluation.
         * @throws Exception thrown if the predicate evaluation could not evaluate.
         */
        String evaluate() throws Exception;
    }

    public interface Predicate {
        /**
         * Perform a predicate evaluation.
         *
         * @return the boolean result of the evaluation.
         * @throws Exception thrown if the predicate evaluation could not evaluate.
         */
        void evaluate() throws Exception;
    }

    /**
     * Wait for a condition, expressed via a {@link Predicate} to become true.
     *
     * @param timeout maximum time in milliseconds to wait for the predicate to become true.
     * @param predicate predicate waiting on.
     */
    protected void waitFor(int timeout, Predicate predicate) throws Exception {
        ParamChecker.notNull(predicate, "predicate");
        long mustEnd = System.currentTimeMillis() + timeout;

        while (true) {
            try {
                predicate.evaluate();
                return;
            } catch(Error | Exception e) {
                if (System.currentTimeMillis() >= mustEnd) {
                    fail("Assertions failed. Failing after waiting for timeout " + timeout + " msecs", e);
                }
                LOG.debug("Waiting up to {} msec as assertion failed", mustEnd - System.currentTimeMillis(), e);
                Thread.sleep(5000);
            }
        }
    }

    /**
     * Wait for a condition, expressed via a {@link Predicate} to become true.
     *
     * @param timeout maximum time in milliseconds to wait for the predicate to become true.
     * @param predicate predicate waiting on.
     */
    protected String waitForWithReturn(int timeout, PredicateWithReturn predicate) throws Exception {
        ParamChecker.notNull(predicate, "predicate");
        long mustEnd = System.currentTimeMillis() + timeout;

        while (true) {
            try {
                return predicate.evaluate();
            } catch(Error | Exception e) {
                if (System.currentTimeMillis() >= mustEnd) {
                    fail("Assertions failed. Failing after waiting for timeout " + timeout + " msecs", e);
                }
                LOG.debug("Waiting up to {} msec as assertion failed", mustEnd - System.currentTimeMillis(), e);
                Thread.sleep(5000);
            }
        }
    }

    public static String lower(String str) {
        if (StringUtils.isEmpty(str)) {
            return null;
        }
        return str.toLowerCase().trim();
    }

    protected void runCommand(String cmd) throws Exception {
        runCommandWithDelay(cmd, 0);
    }

    protected void runCommandWithDelay(String cmd, int sleepMs) throws Exception {
        runCommandWithDelay(driverWithoutContext, cmd, sleepMs);
    }

    protected void runCommandWithDelay(Driver driver, String cmd, int sleepMs) throws Exception {
        LOG.debug("Running command '{}'", cmd);
        CommandProcessorResponse response = driver.run(cmd);
        assertEquals(response.getResponseCode(), 0);
        if (sleepMs != 0) {
            Thread.sleep(sleepMs);
        }
    }

    protected String random() {
        return RandomStringUtils.randomAlphanumeric(10);
    }

    protected String tableName() {
        return "table_" + random();
    }
    protected String dbName() {return "db_" + random();}
}
