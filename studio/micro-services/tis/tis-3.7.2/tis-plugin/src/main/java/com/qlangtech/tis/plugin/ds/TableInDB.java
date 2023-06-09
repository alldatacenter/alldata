package com.qlangtech.tis.plugin.ds;

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

import com.google.common.collect.Lists;
import com.qlangtech.tis.datax.DataXJobInfo;
import com.qlangtech.tis.datax.DataXJobSubmit;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-12-17 18:45
 **/
public abstract class TableInDB {
    protected final DBIdentity dbSourceId;

    public TableInDB(DBIdentity dbSourceId) {
        this.dbSourceId = Objects.requireNonNull(dbSourceId, "param id can not be null");
    }

    public final boolean isMatch(DBIdentity queryDBSourceId) {
        return dbSourceId.isEquals(queryDBSourceId);
//        return StringUtils.equals(Objects.requireNonNull(queryDBSourceId.identityValue(), "dbFactoryId can not be null")
//                , this.dbSourceId.identityValue());
    }

    public static TableInDB create(DBIdentity id) {
        return new DftTableInDB(id);
    }

    /**
     * @param jdbcUrl 可以标示是哪个分库的
     * @param tab
     */
    public abstract void add(String jdbcUrl, String tab);

    /**
     * 增量脚本中执行过程中需要通过物理表名得到逻辑表名称
     *
     * @return
     */
    public abstract Function<String, String> getPhysicsTabName2LogicNameConvertor();

    public abstract List<String> getTabs();

    public abstract boolean contains(String tableName);

    public abstract boolean isEmpty();

    public abstract DataXJobInfo createDataXJobInfo(DataXJobSubmit.TableDataXEntity tabEntity);

    public static class DefaultTableNameConvert implements Function<String, String>, Serializable {
        @Override
        public String apply(String physicsTabName) {
            return physicsTabName;
        }
    }

    private static class DftTableInDB extends TableInDB {
        private List<String> tabs = Lists.newArrayList();

        public DftTableInDB(DBIdentity id) {
            super(id);
        }

        @Override
        public void add(String jdbcUrl, String tab) {
            this.tabs.add(tab);
        }


        @Override
        public DataXJobInfo createDataXJobInfo(DataXJobSubmit.TableDataXEntity tabEntity) {
            return DataXJobInfo.create(tabEntity.getFileName(), tabEntity
                    , this.tabs.contains(tabEntity.getSourceTableName())
                            ? Collections.singletonList(tabEntity.getSourceTableName())
                            : Collections.emptyList());
        }

        @Override
        public Function<String, String> getPhysicsTabName2LogicNameConvertor() {
            return new DefaultTableNameConvert();
        }

        @Override
        public List<String> getTabs() {
            return this.tabs;
        }

        @Override
        public boolean contains(String tableName) {
            return this.tabs.contains(tableName);
        }

        @Override
        public boolean isEmpty() {
            return this.tabs.isEmpty();
        }
    }
}
