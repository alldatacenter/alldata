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

package org.apache.atlas.sqoop.hook;

import org.apache.atlas.ApplicationProperties;
import org.apache.atlas.AtlasClient;
import org.apache.atlas.hive.bridge.HiveMetaStoreBridge;
import org.apache.atlas.hive.model.HiveDataTypes;
import org.apache.atlas.sqoop.model.SqoopDataTypes;
import org.apache.atlas.utils.AuthenticationUtil;
import org.apache.atlas.utils.ParamChecker;
import org.apache.atlas.v1.model.instance.Referenceable;
import org.apache.commons.configuration.Configuration;
import org.apache.sqoop.SqoopJobDataPublisher;
import org.slf4j.Logger;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Properties;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

public class SqoopHookIT {
    public static final Logger LOG = org.slf4j.LoggerFactory.getLogger(SqoopHookIT.class);
    private static final String CLUSTER_NAME = "primary";
    public static final String DEFAULT_DB = "default";
    private static final int MAX_WAIT_TIME = 2000;
    private AtlasClient atlasClient;

    @BeforeClass
    public void setUp() throws Exception {
        //Set-up sqoop session
        Configuration configuration = ApplicationProperties.get();
        if (!AuthenticationUtil.isKerberosAuthenticationEnabled()) {
            atlasClient = new AtlasClient(configuration.getStringArray(HiveMetaStoreBridge.ATLAS_ENDPOINT), new String[]{"admin", "admin"});
        } else {
            atlasClient = new AtlasClient(configuration.getStringArray(HiveMetaStoreBridge.ATLAS_ENDPOINT));
        }
    }

    @Test
    public void testSqoopImport() throws Exception {
        SqoopJobDataPublisher.Data d = new SqoopJobDataPublisher.Data("import", "jdbc:mysql:///localhost/db",
                "mysqluser", "mysql", "myTable", null, "default", "hiveTable", new Properties(),
                System.currentTimeMillis() - 100, System.currentTimeMillis());
        SqoopHook hook = new SqoopHook();
        hook.publish(d);
        Thread.sleep(1000);
        String storeName  = SqoopHook.getSqoopDBStoreName(d);
        assertDBStoreIsRegistered(storeName);
        String name = SqoopHook.getSqoopProcessName(d, CLUSTER_NAME);
        assertSqoopProcessIsRegistered(name);
        assertHiveTableIsRegistered(DEFAULT_DB, "hiveTable");
    }

    @Test
    public void testSqoopExport() throws Exception {
        SqoopJobDataPublisher.Data d = new SqoopJobDataPublisher.Data("export", "jdbc:mysql:///localhost/db",
                "mysqluser", "mysql", "myTable", null, "default", "hiveTable", new Properties(),
                System.currentTimeMillis() - 100, System.currentTimeMillis());
        SqoopHook hook = new SqoopHook();
        hook.publish(d);
        Thread.sleep(1000);
        String storeName  = SqoopHook.getSqoopDBStoreName(d);
        assertDBStoreIsRegistered(storeName);
        String name = SqoopHook.getSqoopProcessName(d, CLUSTER_NAME);
        assertSqoopProcessIsRegistered(name);
        assertHiveTableIsRegistered(DEFAULT_DB, "hiveTable");
    }

    private String assertDBStoreIsRegistered(String storeName) throws Exception {
        LOG.debug("Searching for db store {}",  storeName);
        return assertEntityIsRegistered(SqoopDataTypes.SQOOP_DBDATASTORE.getName(), AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, storeName, null);
    }

    private String assertHiveTableIsRegistered(String dbName, String tableName) throws Exception {
        LOG.debug("Searching for table {}.{}", dbName, tableName);
        return assertEntityIsRegistered(HiveDataTypes.HIVE_TABLE.getName(), AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, HiveMetaStoreBridge.getTableQualifiedName(CLUSTER_NAME, dbName, tableName), null);
    }

    private String assertSqoopProcessIsRegistered(String processName) throws Exception {
        LOG.debug("Searching for sqoop process {}",  processName);
        return assertEntityIsRegistered(SqoopDataTypes.SQOOP_PROCESS.getName(), AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, processName, null);
    }

    protected String assertEntityIsRegistered(final String typeName, final String property, final String value,
                                              final AssertPredicate assertPredicate) throws Exception {
        waitFor(80000, new Predicate() {
            @Override
            public void evaluate() throws Exception {
                Referenceable entity = atlasClient.getEntity(typeName, property, value);
                assertNotNull(entity);
                if (assertPredicate != null) {
                    assertPredicate.assertOnEntity(entity);
                }
            }
        });
        Referenceable entity = atlasClient.getEntity(typeName, property, value);
        return entity.getId()._getId();
    }

    public interface Predicate {
        void evaluate() throws Exception;
    }

    public interface AssertPredicate {
        void assertOnEntity(Referenceable entity) throws Exception;
    }

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
}
