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

import org.apache.griffin.core.util.URLHelper;
import org.apache.hadoop.hive.metastore.api.Table;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(value = HiveMetaStoreController.class, secure = false)
public class HiveMetaStoreControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    @Qualifier(value = "metastoreSvc")
    private HiveMetaStoreService hiveMetaStoreService;


    @Before
    public void setup() {
    }

    @Test
    public void testGetAllDatabases() throws Exception {
        String dbName = "default";
        given(hiveMetaStoreService.getAllDatabases()).willReturn(Arrays
                .asList(dbName));

        mockMvc.perform(get(URLHelper.API_VERSION_PATH + "/metadata/hive/dbs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[0]", is(dbName)));
    }


    @Test
    public void testGetAllTableNames() throws Exception {
        String dbName = "default";
        String tableName = "table";
        given(hiveMetaStoreService.getAllTableNames(dbName)).willReturn(Arrays
                .asList(tableName));

        mockMvc.perform(get(URLHelper.API_VERSION_PATH +
                "/metadata/hive/tables/names").param("db",
                dbName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[0]", is(tableName)));
    }

    @Test
    public void testGetAllTablesWithDb() throws Exception {
        String dbName = "default";
        given(hiveMetaStoreService.getAllTable(dbName)).willReturn(Arrays
                .asList(new Table()));

        mockMvc.perform(get(URLHelper.API_VERSION_PATH +
                "/metadata/hive/tables").param("db",
                dbName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[0].tableName", is(nullValue())));
    }

    @Test
    public void testGetAllTables() throws Exception {
        Map<String, List<Table>> results = new HashMap<>();
        results.put("table", new ArrayList<>());
        given(hiveMetaStoreService.getAllTable()).willReturn(results);

        mockMvc.perform(get(URLHelper.API_VERSION_PATH +
                "/metadata/hive/dbs/tables"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.table", hasSize(0)));
    }


    @Test
    public void testGetTable() throws Exception {
        String dbName = "default";
        String tableName = "table";
        given(hiveMetaStoreService.getTable(dbName, tableName)).willReturn(
                new Table(tableName, null, null, 0, 0, 0, null, null,
                        null, null, null, null));

        mockMvc.perform(get(URLHelper.API_VERSION_PATH + "/metadata/hive/table")
                .param("db", dbName).param("table",
                        tableName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tableName", is(tableName)));
    }
}
