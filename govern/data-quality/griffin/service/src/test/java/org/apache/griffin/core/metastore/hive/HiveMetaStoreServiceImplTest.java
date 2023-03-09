/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/

package org.apache.griffin.core.metastore.hive;

import org.apache.griffin.core.config.CacheConfig;
import org.apache.hadoop.hive.metastore.HiveMetaStoreClient;
import org.apache.hadoop.hive.metastore.api.MetaException;
import org.apache.hadoop.hive.metastore.api.NoSuchObjectException;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.thrift.TException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;


@RunWith(SpringRunner.class)
@ContextConfiguration
public class HiveMetaStoreServiceImplTest {

    @Configuration
    @EnableCaching
    public static class HiveMetaStoreServiceConfiguration extends CacheConfig {
        @Bean("hiveMetaStoreServiceImpl")
        public HiveMetaStoreServiceImpl service() {
            return new HiveMetaStoreServiceImpl();
        }

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("hive");
        }
    }

    @MockBean
    private HiveMetaStoreClient client;

    @Autowired
    private HiveMetaStoreService service;

    @Autowired
    private CacheManager cacheManager;

    @Before
    public void setup() {
        cacheManager.getCache("hive").clear();
    }

    @Test
    public void testGetAllDatabasesForNormalRun() throws TException {
        given(client.getAllDatabases()).willReturn(Arrays.asList("default"));
        assertEquals(service.getAllDatabases().iterator().hasNext(), true);
    }

    @Test
    public void testGetAllDatabasesForMetaException() throws TException {
        given(client.getAllDatabases()).willThrow(MetaException.class);
        doNothing().when(client).reconnect();
        assertTrue(service.getAllDatabases() == null);
        verify(client).getAllDatabases();
        verify(client).reconnect();
        // check it's not cached
        service.getAllDatabases();
        verify(client, times(2)).reconnect();
        verify(client, times(2)).getAllDatabases();
    }


    @Test
    public void testGetAllTableNamesForNormalRun() throws MetaException {
        String dbName = "default";
        given(client.getAllTables(dbName)).willReturn(Arrays.asList(dbName));
        assertEquals(service.getAllTableNames(dbName).iterator().hasNext(),
                true);
    }

    @Test
    public void testGetAllTableNamesForMetaException() throws MetaException {
        String dbName = "default";
        given(client.getAllTables(dbName)).willThrow(MetaException.class);
        doNothing().when(client).reconnect();
        assertTrue(service.getAllTableNames(dbName) == null);
        verify(client).reconnect();
        verify(client).getAllTables(dbName);
        // check it's not cached
        service.getAllTableNames(dbName);
        verify(client, times(2)).reconnect();
        verify(client, times(2)).getAllTables(dbName);

    }

    @Test
    public void testGetAllTableByDBNameForNormalRun() throws TException {
        String useDbName = "default";
        String tableName = "table";
        given(client.getAllTables(useDbName)).willReturn(Arrays
                .asList(tableName));
        given(client.getTable(useDbName, tableName)).willReturn(new Table());
        assertEquals(service.getAllTable(useDbName).size(), 1);
    }

    @Test
    public void testGetAllTableByDBNameForMetaException() throws TException {
        String useDbName = "default";
        given(client.getAllTables(useDbName)).willThrow(MetaException.class);
        doNothing().when(client).reconnect();
        assertEquals(0, service.getAllTable(useDbName).size());
        verify(client).reconnect();
        verify(client).getAllTables(useDbName);
        // check it's not cached
        service.getAllTable(useDbName);
        verify(client, times(2)).reconnect();
        verify(client, times(2)).getAllTables(useDbName);
    }

    @Test
    public void testGetAllTableForNormalRun() throws TException {
        String useDbName = "default";
        String tableName = "table";
        List<String> databases = Arrays.asList(useDbName);
        given(client.getAllDatabases()).willReturn(databases);
        given(client.getAllTables(databases.get(0))).willReturn(Arrays
                .asList(tableName));
        given(client.getTable(useDbName, tableName)).willReturn(new Table());
        assertEquals(service.getAllTable().size(), 1);
    }

    @Test
    public void testGetAllTableForMetaException1() throws TException {
        String useDbName = "default";
        List<String> databases = Arrays.asList(useDbName);
        given(client.getAllDatabases()).willReturn(databases);
        given(client.getAllTables(useDbName)).willThrow(MetaException.class);
        doNothing().when(client).reconnect();
        assertEquals(service.getAllTable().get(useDbName).size(), 0);
    }

    @Test
    public void testGetAllTableForMetaException2() throws TException {
        given(client.getAllDatabases()).willThrow(MetaException.class);
        doNothing().when(client).reconnect();
        assertEquals(service.getAllTable().size(), 0);
    }

    @Test
    public void testGetTableForNormalRun() throws Exception {
        String dbName = "default";
        String tableName = "tableName";
        given(client.getTable(dbName, tableName)).willReturn(new Table());
        assertTrue(service.getTable(dbName, tableName) != null);
    }

    @Test
    public void testGetTableForException() throws Exception {
        String dbName = "default";
        String tableName = "tableName";
        given(client.getTable(dbName, tableName)).willThrow(NoSuchObjectException.class);
        doNothing().when(client).reconnect();
        assertTrue(service.getTable(dbName, tableName) == null);
        verify(client).reconnect();
        verify(client).getTable(dbName, tableName);
        // check it's not cached
        service.getTable(dbName, tableName);
        verify(client, times(2)).reconnect();
        verify(client, times(2)).getTable(dbName, tableName);
    }

    @Test
    public void testEvictHiveCache() throws Exception {
        String useDbName = "default";
        String tableName = "tableName";
        List<String> databases = Arrays.asList(useDbName);
        given(client.getAllDatabases()).willReturn(databases);
        given(client.getAllTables(databases.get(0))).willReturn(Arrays
                .asList(tableName));
        given(client.getTable(useDbName, tableName)).willReturn(new Table());
        // populate cache
        assertEquals(service.getAllTable().size(), 1);
        verify(client).getAllDatabases();
        verify(client).getAllTables(useDbName);
        verify(client).getTable(useDbName, tableName);
        // verify cached
        service.getAllTable();
        verifyNoMoreInteractions(client);
        // reset the cache, verify values are cached again
        service.evictHiveCache();
        service.getAllTable().size();
        service.getAllTable().size();
        verify(client, times(2)).getAllDatabases();
        verify(client, times(2)).getAllTables(useDbName);
        verify(client, times(2)).getTable(useDbName, tableName);
    }
}
