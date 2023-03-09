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


import java.util.List;
import java.util.Map;

import org.apache.hadoop.hive.metastore.api.Table;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/metadata/hive")
public class HiveMetaStoreController {

    @Autowired
    @Qualifier(value = "metastoreSvc")
    private HiveMetaStoreService hiveMetaStoreService;

    @RequestMapping(value = "/dbs", method = RequestMethod.GET)
    public Iterable<String> getAllDatabases() {
        return hiveMetaStoreService.getAllDatabases();
    }


    @RequestMapping(value = "/tables/names", method = RequestMethod.GET)
    public Iterable<String> getAllTableNames(@RequestParam("db") String dbName) {
        return hiveMetaStoreService.getAllTableNames(dbName);
    }

    @RequestMapping(value = "/tables", method = RequestMethod.GET)
    public List<Table> getAllTables(@RequestParam("db") String dbName) {
        return hiveMetaStoreService.getAllTable(dbName);
    }

    @RequestMapping(value = "/dbs/tables", method = RequestMethod.GET)
    public Map<String, List<Table>> getAllTables() {
        return hiveMetaStoreService.getAllTable();
    }

    @RequestMapping(value = "/dbs/tables/names", method = RequestMethod.GET)
    public Map<String, List<String>> getAllTableNames() {
        return hiveMetaStoreService.getAllTableNames();
    }

    @RequestMapping(value = "/table", method = RequestMethod.GET)
    public Table getTable(@RequestParam("db") String dbName,
                          @RequestParam("table") String tableName) {
        return hiveMetaStoreService.getTable(dbName, tableName);
    }


}
