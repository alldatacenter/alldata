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

package org.apache.atlas.hive;

import com.google.common.annotations.VisibleForTesting;
import org.apache.atlas.ApplicationProperties;
import org.apache.atlas.AtlasClient;
import org.apache.atlas.AtlasClientV2;
import org.apache.atlas.AtlasServiceException;
import org.apache.atlas.hive.bridge.ColumnLineageUtils;
import org.apache.atlas.hive.bridge.HiveMetaStoreBridge;
import org.apache.atlas.hive.hook.HiveHookIT;
import org.apache.atlas.hive.model.HiveDataTypes;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasObjectId;
import org.apache.atlas.model.instance.AtlasStruct;
import org.apache.atlas.model.notification.HookNotification;
import org.apache.atlas.utils.AuthenticationUtil;
import org.apache.atlas.utils.ParamChecker;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.ql.Driver;
import org.apache.hadoop.hive.ql.hooks.Entity;
import org.apache.hadoop.hive.ql.hooks.HookContext;
import org.apache.hadoop.hive.ql.hooks.LineageInfo;
import org.apache.hadoop.hive.ql.hooks.ReadEntity;
import org.apache.hadoop.hive.ql.hooks.WriteEntity;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.ql.metadata.Table;
import org.apache.hadoop.hive.ql.plan.HiveOperation;
import org.apache.hadoop.hive.ql.processors.CommandProcessorResponse;
import org.apache.hadoop.hive.ql.session.SessionState;
import org.apache.hadoop.security.UserGroupInformation;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;

import static com.sun.jersey.api.client.ClientResponse.Status.NOT_FOUND;
import static org.apache.atlas.hive.bridge.HiveMetaStoreBridge.HDFS_PATH;
import static org.apache.atlas.hive.hook.events.BaseHiveEvent.ATTRIBUTE_QUALIFIED_NAME;
import static org.apache.atlas.hive.model.HiveDataTypes.HIVE_DB;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class HiveITBase {
    private static final Logger LOG = LoggerFactory.getLogger(HiveITBase.class);

    public    static final String DEFAULT_DB   = "default";
    public    static final String SEP          = ":".intern();
    public    static final String IO_SEP       = "->".intern();
    protected static final String DGI_URL      = "http://localhost:21000/";
    protected static final String CLUSTER_NAME = "primary";
    protected static final String PART_FILE    = "2015-01-01";
    protected static final String INPUTS       = "inputs";
    protected static final String OUTPUTS      = "outputs";


    protected Driver              driver;
    protected AtlasClient         atlasClient;
    protected AtlasClientV2       atlasClientV2;
    protected HiveMetaStoreBridge hiveMetaStoreBridge;
    protected SessionState        ss;
    protected HiveConf            conf;
    protected Driver              driverWithoutContext;

    private static final String REFERENCEABLE_ATTRIBUTE_NAME = "qualifiedName";
    private static final String ATTR_NAME                    = "name";


    @BeforeClass
    public void setUp() throws Exception {
        //Set-up hive session
        conf = new HiveConf();
        conf.setClassLoader(Thread.currentThread().getContextClassLoader());
        conf.set("hive.metastore.event.listeners", "");

        // 'driver' using this configuration will be used for tests in HiveHookIT
        //  HiveHookIT will use this driver to test post-execution hooks in HiveServer2.
        //  initialize 'driver' with HMS hook disabled.
        driver = new Driver(conf);
        ss     = new SessionState(conf);
        ss     = SessionState.start(ss);

        SessionState.setCurrentSessionState(ss);

        Configuration configuration = ApplicationProperties.get();

        String[] atlasEndPoint = configuration.getStringArray(HiveMetaStoreBridge.ATLAS_ENDPOINT);

        if (atlasEndPoint == null || atlasEndPoint.length == 0) {
            atlasEndPoint = new String[] { DGI_URL };
        }

        if (!AuthenticationUtil.isKerberosAuthenticationEnabled()) {
            atlasClientV2 = new AtlasClientV2(atlasEndPoint, new String[]{"admin", "admin"});
            atlasClient   = new AtlasClient(atlasEndPoint, new String[]{"admin", "admin"});
        } else {
            atlasClientV2 = new AtlasClientV2(atlasEndPoint);
            atlasClient   = new AtlasClient(atlasEndPoint);
        }

        hiveMetaStoreBridge = new HiveMetaStoreBridge(configuration, conf, atlasClientV2);

        HiveConf conf = new HiveConf();

        conf.set("hive.exec.post.hooks", "");

        SessionState ss = new SessionState(conf);
        ss = SessionState.start(ss);
        SessionState.setCurrentSessionState(ss);

        // 'driverWithoutContext' using this configuration will be used for tests in HiveMetastoreHookIT
        //  HiveMetastoreHookIT will use this driver to test event listeners in HiveMetastore.
        //  initialize 'driverWithoutContext' with HiveServer2 post execution hook disabled.
        driverWithoutContext = new Driver(conf);
    }

    protected void runCommand(String cmd) throws Exception {
        runCommandWithDelay(cmd, 0);
    }

    protected void runCommand(Driver driver, String cmd) throws Exception {
        runCommandWithDelay(driver, cmd, 0);
    }

    protected void runCommandWithDelay(String cmd, int sleepMs) throws Exception {
        runCommandWithDelay(driver, cmd, sleepMs);
    }

    protected void runCommandWithDelay(Driver driver, String cmd, int sleepMs) throws Exception {
        LOG.debug("Running command '{}'", cmd);

        CommandProcessorResponse response = driver.run(cmd);

        assertEquals(response.getResponseCode(), 0);

        if (sleepMs != 0) {
            Thread.sleep(sleepMs);
        }
    }

    protected String createTestDFSPath(String path) throws Exception {
        return "file://" + mkdir(path);
    }

    protected String file(String tag) throws Exception {
        String filename = System.getProperty("user.dir") + "/target/" + tag + "-data-" + random();
        File file = new File(filename);
        file.createNewFile();
        return file.getAbsolutePath();
    }

    protected String mkdir(String tag) throws Exception {
        String filename = "./target/" + tag + "-data-" + random();
        File file = new File(filename);
        file.mkdirs();
        return file.getAbsolutePath();
    }

    public static String lower(String str) {
        if (StringUtils.isEmpty(str)) {
            return null;
        }
        return str.toLowerCase().trim();
    }

    protected String random() {
        return RandomStringUtils.randomAlphanumeric(10).toLowerCase();
    }

    protected String tableName() {
        return "table_" + random();
    }

    protected String dbName() {
        return "db_" + random();
    }

    protected String assertTableIsRegistered(String dbName, String tableName) throws Exception {
        return assertTableIsRegistered(dbName, tableName, null, false);
    }

    protected String assertTableIsRegistered(String dbName, String tableName, HiveHookIT.AssertPredicate assertPredicate, boolean isTemporary) throws Exception {
        LOG.debug("Searching for table {}.{}", dbName, tableName);
        String tableQualifiedName = HiveMetaStoreBridge.getTableQualifiedName(CLUSTER_NAME, dbName, tableName, isTemporary);
        return assertEntityIsRegistered(HiveDataTypes.HIVE_TABLE.getName(), REFERENCEABLE_ATTRIBUTE_NAME, tableQualifiedName,
                assertPredicate);
    }

    protected String assertEntityIsRegistered(final String typeName, final String property, final String value,
                                              final HiveHookIT.AssertPredicate assertPredicate) throws Exception {
        waitFor(100000, new HiveHookIT.Predicate() {
            @Override
            public void evaluate() throws Exception {
                AtlasEntity.AtlasEntityWithExtInfo atlasEntityWithExtInfo = atlasClientV2.getEntityByAttribute(typeName, Collections.singletonMap(property,value));
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

    protected String assertEntityIsRegisteredViaGuid(String guid,
                                              final HiveHookIT.AssertPredicate assertPredicate) throws Exception {
        waitFor(100000, new HiveHookIT.Predicate() {
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

    protected AtlasEntity assertEntityIsRegistedViaEntity(final String typeName, final String property, final String value,
                                              final HiveHookIT.AssertPredicate assertPredicate) throws Exception {
        waitFor(80000, new HiveHookIT.Predicate() {
            @Override
            public void evaluate() throws Exception {
                AtlasEntity.AtlasEntityWithExtInfo atlasEntityWithExtInfo = atlasClientV2.getEntityByAttribute(typeName, Collections.singletonMap(property,value));
                AtlasEntity entity = atlasEntityWithExtInfo.getEntity();
                assertNotNull(entity);
                if (assertPredicate != null) {
                    assertPredicate.assertOnEntity(entity);
                }
            }
        });
        AtlasEntity.AtlasEntityWithExtInfo atlasEntityWithExtInfo = atlasClientV2.getEntityByAttribute(typeName, Collections.singletonMap(property,value));
        AtlasEntity entity = atlasEntityWithExtInfo.getEntity();
        return entity;
    }

    public interface AssertPredicate {
        void assertOnEntity(AtlasEntity entity) throws Exception;
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

    protected String getTableProcessQualifiedName(String dbName, String tableName) throws Exception {
        return HiveMetaStoreBridge.getTableProcessQualifiedName(CLUSTER_NAME,
                hiveMetaStoreBridge.getHiveClient().getTable(dbName, tableName));
    }

    protected void validateHDFSPaths(AtlasEntity processEntity, String attributeName, String... testPaths) throws Exception {
        List<AtlasObjectId> hdfsPathIds = toAtlasObjectIdList(processEntity.getAttribute(attributeName));

        for (String testPath : testPaths) {
            Path   path           = new Path(testPath);
            String testPathNormed = lower(path.toString());
            String hdfsPathId     = assertHDFSPathIsRegistered(testPathNormed);

            assertHDFSPathIdsContain(hdfsPathIds, hdfsPathId);
        }
    }

    private void assertHDFSPathIdsContain(List<AtlasObjectId> hdfsPathObjectIds, String hdfsPathId) {
        Set<String> hdfsPathGuids = new HashSet<>();

        for (AtlasObjectId hdfsPathObjectId : hdfsPathObjectIds) {
            hdfsPathGuids.add(hdfsPathObjectId.getGuid());
        }

        assertTrue(hdfsPathGuids.contains(hdfsPathId));
    }

    protected String assertHDFSPathIsRegistered(String path) throws Exception {
        LOG.debug("Searching for hdfs path {}", path);
        // ATLAS-2444 HDFS name node federation adds the cluster name to the qualifiedName
        if (path.startsWith("hdfs://")) {
            String pathWithCluster = path + "@" + CLUSTER_NAME;
            return assertEntityIsRegistered(HDFS_PATH, REFERENCEABLE_ATTRIBUTE_NAME, pathWithCluster, null);
        } else {
            return assertEntityIsRegistered(HDFS_PATH, REFERENCEABLE_ATTRIBUTE_NAME, path, null);
        }
    }

    protected String assertDatabaseIsRegistered(String dbName) throws Exception {
        return assertDatabaseIsRegistered(dbName, null);
    }

    protected String assertDatabaseIsRegistered(String dbName, AssertPredicate assertPredicate) throws Exception {
        LOG.debug("Searching for database: {}", dbName);

        String dbQualifiedName = HiveMetaStoreBridge.getDBQualifiedName(CLUSTER_NAME, dbName);

        return assertEntityIsRegistered(HIVE_DB.getName(), REFERENCEABLE_ATTRIBUTE_NAME, dbQualifiedName, assertPredicate);
    }

    public void assertDatabaseIsNotRegistered(String dbName) throws Exception {
        LOG.debug("Searching for database {}", dbName);
        String dbQualifiedName = HiveMetaStoreBridge.getDBQualifiedName(CLUSTER_NAME, dbName);
        assertEntityIsNotRegistered(HIVE_DB.getName(), ATTRIBUTE_QUALIFIED_NAME, dbQualifiedName);
    }

    protected void assertEntityIsNotRegistered(final String typeName, final String property, final String value) throws Exception {
        // wait for sufficient time before checking if entity is not available.
        long waitTime = 10000;
        LOG.debug("Waiting for {} msecs, before asserting entity is not registered.", waitTime);
        Thread.sleep(waitTime);

        try {
            atlasClientV2.getEntityByAttribute(typeName, Collections.singletonMap(property, value));

            fail(String.format("Entity was not supposed to exist for typeName = %s, attributeName = %s, attributeValue = %s", typeName, property, value));
        } catch (AtlasServiceException e) {
            if (e.getStatus() == NOT_FOUND) {
                return;
            }
        }
    }

    protected AtlasEntity getAtlasEntityByType(String type, String id) throws Exception {
        AtlasEntity atlasEntity = null;
        AtlasEntity.AtlasEntityWithExtInfo atlasEntityWithExtInfoForProcess = atlasClientV2.getEntityByAttribute(type,
                Collections.singletonMap(AtlasClient.GUID, id));
        atlasEntity = atlasEntityWithExtInfoForProcess.getEntity();
        return atlasEntity;
    }


    public static class HiveEventContext {
        private Set<ReadEntity> inputs;
        private Set<WriteEntity> outputs;

        private String user;
        private UserGroupInformation ugi;
        private HiveOperation operation;
        private HookContext.HookType hookType;
        private JSONObject jsonPlan;
        private String queryId;
        private String queryStr;
        private Long queryStartTime;

        public Map<String, List<ColumnLineageUtils.HiveColumnLineageInfo>> lineageInfo;

        private List<HookNotification> messages = new ArrayList<>();

        public void setInputs(Set<ReadEntity> inputs) {
            this.inputs = inputs;
        }

        public void setOutputs(Set<WriteEntity> outputs) {
            this.outputs = outputs;
        }

        public void setUser(String user) {
            this.user = user;
        }

        public void setUgi(UserGroupInformation ugi) {
            this.ugi = ugi;
        }

        public void setOperation(HiveOperation operation) {
            this.operation = operation;
        }

        public void setHookType(HookContext.HookType hookType) {
            this.hookType = hookType;
        }

        public void setQueryId(String queryId) {
            this.queryId = queryId;
        }

        public void setQueryStr(String queryStr) {
            this.queryStr = queryStr;
        }

        public void setQueryStartTime(Long queryStartTime) {
            this.queryStartTime = queryStartTime;
        }

        public void setLineageInfo(LineageInfo lineageInfo){
            try {
                this.lineageInfo = ColumnLineageUtils.buildLineageMap(lineageInfo);
                LOG.debug("Column Lineage Map => {} ", this.lineageInfo.entrySet());
            }catch (Throwable e){
                LOG.warn("Column Lineage Map build failed with exception {}", e);
            }
        }

        public Set<ReadEntity> getInputs() {
            return inputs;
        }

        public Set<WriteEntity> getOutputs() {
            return outputs;
        }

        public String getUser() {
            return user;
        }

        public UserGroupInformation getUgi() {
            return ugi;
        }

        public HiveOperation getOperation() {
            return operation;
        }

        public HookContext.HookType getHookType() {
            return hookType;
        }

        public String getQueryId() {
            return queryId;
        }

        public String getQueryStr() {
            return queryStr;
        }

        public Long getQueryStartTime() {
            return queryStartTime;
        }

        public void addMessage(HookNotification message) {
            messages.add(message);
        }

        public List<HookNotification> getMessages() {
            return messages;
        }
    }


    @VisibleForTesting
    protected static String getProcessQualifiedName(HiveMetaStoreBridge dgiBridge, HiveEventContext eventContext,
                                          final SortedSet<ReadEntity> sortedHiveInputs,
                                          final SortedSet<WriteEntity> sortedHiveOutputs,
                                          SortedMap<ReadEntity, AtlasEntity> hiveInputsMap,
                                          SortedMap<WriteEntity, AtlasEntity> hiveOutputsMap) throws HiveException {
        HiveOperation op = eventContext.getOperation();
        if (isCreateOp(eventContext)) {
            Entity entity = getEntityByType(sortedHiveOutputs, Entity.Type.TABLE);

            if (entity != null) {
                Table outTable = entity.getTable();
                //refresh table
                outTable = dgiBridge.getHiveClient().getTable(outTable.getDbName(), outTable.getTableName());
                return HiveMetaStoreBridge.getTableProcessQualifiedName(dgiBridge.getMetadataNamespace(), outTable);
            }
        }

        StringBuilder buffer = new StringBuilder(op.getOperationName());

        boolean ignoreHDFSPathsinQFName = ignoreHDFSPathsinQFName(op, sortedHiveInputs, sortedHiveOutputs);
        if ( ignoreHDFSPathsinQFName && LOG.isDebugEnabled()) {
            LOG.debug("Ignoring HDFS paths in qualifiedName for {} {} ", op, eventContext.getQueryStr());
        }

        addInputs(dgiBridge, op, sortedHiveInputs, buffer, hiveInputsMap, ignoreHDFSPathsinQFName);
        buffer.append(IO_SEP);
        addOutputs(dgiBridge, op, sortedHiveOutputs, buffer, hiveOutputsMap, ignoreHDFSPathsinQFName);
        LOG.info("Setting process qualified name to {}", buffer);
        return buffer.toString();
    }

    protected static Entity getEntityByType(Set<? extends Entity> entities, Entity.Type entityType) {
        for (Entity entity : entities) {
            if (entity.getType() == entityType) {
                return entity;
            }
        }
        return null;
    }


    protected static boolean ignoreHDFSPathsinQFName(final HiveOperation op, final Set<ReadEntity> inputs, final Set<WriteEntity> outputs) {
        switch (op) {
            case LOAD:
            case IMPORT:
                return isPartitionBasedQuery(outputs);
            case EXPORT:
                return isPartitionBasedQuery(inputs);
            case QUERY:
                return true;
        }
        return false;
    }

    protected static boolean isPartitionBasedQuery(Set<? extends Entity> entities) {
        for (Entity entity : entities) {
            if (Entity.Type.PARTITION.equals(entity.getType())) {
                return true;
            }
        }
        return false;
    }

    protected static boolean isCreateOp(HiveEventContext hiveEvent) {
        return HiveOperation.CREATETABLE.equals(hiveEvent.getOperation())
                || HiveOperation.CREATEVIEW.equals(hiveEvent.getOperation())
                || HiveOperation.ALTERVIEW_AS.equals(hiveEvent.getOperation())
                || HiveOperation.ALTERTABLE_LOCATION.equals(hiveEvent.getOperation())
                || HiveOperation.CREATETABLE_AS_SELECT.equals(hiveEvent.getOperation());
    }

    protected static void addInputs(HiveMetaStoreBridge hiveBridge, HiveOperation op, SortedSet<ReadEntity> sortedInputs, StringBuilder buffer, final Map<ReadEntity, AtlasEntity> refs, final boolean ignoreHDFSPathsInQFName) throws HiveException {
        if (refs != null) {
            if (sortedInputs != null) {
                Set<String> dataSetsProcessed = new LinkedHashSet<>();
                for (Entity input : sortedInputs) {

                    if (!dataSetsProcessed.contains(input.getName().toLowerCase())) {
                        //HiveOperation.QUERY type encompasses INSERT, INSERT_OVERWRITE, UPDATE, DELETE, PATH_WRITE operations
                        if (ignoreHDFSPathsInQFName &&
                                (Entity.Type.DFS_DIR.equals(input.getType()) || Entity.Type.LOCAL_DIR.equals(input.getType()))) {
                            LOG.debug("Skipping dfs dir input addition to process qualified name {} ", input.getName());
                        } else if (refs.containsKey(input)) {
                            if ( input.getType() == Entity.Type.PARTITION || input.getType() == Entity.Type.TABLE) {
                                Table inputTable = refreshTable(hiveBridge, input.getTable().getDbName(), input.getTable().getTableName());

                                if (inputTable != null) {
                                    addDataset(buffer, refs.get(input), HiveMetaStoreBridge.getTableCreatedTime(inputTable));
                                }
                            } else {
                                addDataset(buffer, refs.get(input));
                            }
                        }

                        dataSetsProcessed.add(input.getName().toLowerCase());
                    }
                }

            }
        }
    }

    protected static void addDataset(StringBuilder buffer, AtlasEntity ref, final long createTime) {
        addDataset(buffer, ref);
        buffer.append(SEP);
        buffer.append(createTime);
    }

    protected static void addDataset(StringBuilder buffer, AtlasEntity ref) {
        buffer.append(SEP);
        String dataSetQlfdName = (String) ref.getAttribute(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME);
        // '/' breaks query parsing on ATLAS
        buffer.append(dataSetQlfdName.toLowerCase().replaceAll("/", ""));
    }

    protected static void addOutputs(HiveMetaStoreBridge hiveBridge, HiveOperation op, SortedSet<WriteEntity> sortedOutputs, StringBuilder buffer, final Map<WriteEntity, AtlasEntity> refs, final boolean ignoreHDFSPathsInQFName) throws HiveException {
        if (refs != null) {
            Set<String> dataSetsProcessed = new LinkedHashSet<>();
            if (sortedOutputs != null) {
                for (WriteEntity output : sortedOutputs) {
                    final Entity entity = output;
                    if (!dataSetsProcessed.contains(output.getName().toLowerCase())) {
                        if (ignoreHDFSPathsInQFName &&
                                (Entity.Type.DFS_DIR.equals(output.getType()) || Entity.Type.LOCAL_DIR.equals(output.getType()))) {
                            LOG.debug("Skipping dfs dir output addition to process qualified name {} ", output.getName());
                        } else if (refs.containsKey(output)) {
                            //HiveOperation.QUERY type encompasses INSERT, INSERT_OVERWRITE, UPDATE, DELETE, PATH_WRITE operations
                            if (addQueryType(op, (WriteEntity) entity)) {
                                buffer.append(SEP);
                                buffer.append(((WriteEntity) entity).getWriteType().name());
                            }

                            if ( output.getType() == Entity.Type.PARTITION || output.getType() == Entity.Type.TABLE) {
                                Table outputTable = refreshTable(hiveBridge, output.getTable().getDbName(), output.getTable().getTableName());

                                if (outputTable != null) {
                                    addDataset(buffer, refs.get(output), HiveMetaStoreBridge.getTableCreatedTime(outputTable));
                                }
                            } else {
                                addDataset(buffer, refs.get(output));
                            }
                        }

                        dataSetsProcessed.add(output.getName().toLowerCase());
                    }
                }
            }
        }
    }

    protected static Table refreshTable(HiveMetaStoreBridge dgiBridge, String dbName, String tableName) {
        try {
            return dgiBridge.getHiveClient().getTable(dbName, tableName);
        } catch (HiveException excp) { // this might be the case for temp tables
            LOG.warn("failed to get details for table {}.{}. Ignoring. {}: {}", dbName, tableName, excp.getClass().getCanonicalName(), excp.getMessage());
        }

        return null;
    }

    protected static boolean addQueryType(HiveOperation op, WriteEntity entity) {
        if (entity.getWriteType() != null && HiveOperation.QUERY.equals(op)) {
            switch (entity.getWriteType()) {
                case INSERT:
                case INSERT_OVERWRITE:
                case UPDATE:
                case DELETE:
                    return true;
                case PATH_WRITE:
                    //Add query type only for DFS paths and ignore local paths since they are not added as outputs
                    if ( !Entity.Type.LOCAL_DIR.equals(entity.getType())) {
                        return true;
                    }
                    break;
                default:
            }
        }
        return false;
    }


    @VisibleForTesting
    protected static final class EntityComparator implements Comparator<Entity> {
        @Override
        public int compare(Entity o1, Entity o2) {
            String s1 = o1.getName();
            String s2 = o2.getName();
            if (s1 == null || s2 == null){
                s1 = o1.getD().toString();
                s2 = o2.getD().toString();
            }
            return s1.toLowerCase().compareTo(s2.toLowerCase());
        }
    }

    @VisibleForTesting
    protected static final Comparator<Entity> entityComparator = new EntityComparator();

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

    protected AtlasStruct toAtlasStruct(Object obj) {
        final AtlasStruct ret;

        if (obj instanceof AtlasStruct) {
            ret = (AtlasStruct) obj;
        } else if (obj instanceof Map) {
            ret = new AtlasStruct((Map) obj);
        } else {
            ret = null;
        }

        return ret;
    }

    protected List<AtlasStruct> toAtlasStructList(Object obj) {
        final List<AtlasStruct> ret;

        if (obj instanceof Collection) {
            Collection coll = (Collection) obj;

            ret = new ArrayList<>(coll.size());

            for (Object item : coll) {
                AtlasStruct struct = toAtlasStruct(item);

                if (struct != null) {
                    ret.add(struct);
                }
            }
        } else {
            AtlasStruct struct = toAtlasStruct(obj);

            if (struct != null) {
                ret = new ArrayList<>(1);

                ret.add(struct);
            } else {
                ret = null;
            }
        }

        return ret;
    }
}
