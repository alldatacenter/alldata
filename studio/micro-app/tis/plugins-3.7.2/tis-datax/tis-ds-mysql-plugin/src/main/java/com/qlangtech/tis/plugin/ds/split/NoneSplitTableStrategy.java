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

package com.qlangtech.tis.plugin.ds.split;

import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.plugin.ds.DBIdentity;
import com.qlangtech.tis.plugin.ds.DataSourceFactory;
import com.qlangtech.tis.plugin.ds.SplitTableStrategy;
import com.qlangtech.tis.plugin.ds.TableInDB;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;

import java.util.Collections;
import java.util.List;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-12-17 21:24
 **/
public class NoneSplitTableStrategy extends SplitTableStrategy {
    @Override
    public TableInDB createTableInDB(DBIdentity dbId) {
        return TableInDB.create(dbId);
    }

    @Override
    public DBPhysicsTable getMatchedPhysicsTable(DataSourceFactory dsFactory, String jdbcUrl, EntityName tabName) {

//        DBConfig dbConfig = dsFactory.getDbConfig();
//        dbConfig.vistDbURL(false, (dbName, dbHost, jdbcUrl) -> {
//
//        });
        return new DBPhysicsTable(jdbcUrl, tabName);
    }

//    @Override
//    public List<String> getAllPhysicsTabs(DataSourceFactory dsFactory, DataXJobSubmit.TableDataXEntity tabEntity) {
//        return Collections.singletonList(tabEntity.getSourceTableName());
//    }


    @Override
    public List<String> getAllPhysicsTabs(DataSourceFactory dsFactory, String jdbcUrl, String sourceTableName) {
        return Collections.singletonList(sourceTableName);
    }

    @TISExtension
    public static class DefatDesc extends Descriptor<SplitTableStrategy> {
        @Override
        public String getDisplayName() {
            return SWITCH_OFF;
        }
    }
}
