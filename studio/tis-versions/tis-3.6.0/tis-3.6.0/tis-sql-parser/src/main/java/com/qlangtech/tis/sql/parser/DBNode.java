/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.tis.sql.parser;

import com.google.common.collect.Maps;
import com.qlangtech.tis.TIS;
import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.manage.common.incr.StreamContextConstant;
import com.qlangtech.tis.offline.DbScope;
import com.qlangtech.tis.plugin.ds.DataSourceFactoryPluginStore;
import com.qlangtech.tis.plugin.ds.FacadeDataSource;
import com.qlangtech.tis.plugin.ds.PostedDSProp;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class DBNode implements IDBNodeMeta {
    // getDaoDir(), db.getDbName()
    private static final Logger logger = LoggerFactory.getLogger(DBNode.class);

    private final String dbName;

    private final int dbId;

    // private File daoDir;
    private long timestampVer;

    private static final Yaml yaml;

    static {
        yaml = new Yaml(new Constructor());
    }


    public static void dump(List<DBNode> nodes, File f) throws Exception {
        try (Writer writer = new OutputStreamWriter(FileUtils.openOutputStream(f), TisUTF8.get()) {
        }) {
            yaml.dump(nodes.stream().map((r) -> {
                Map<String, Object> row = Maps.newHashMap();
                row.put("dbname", r.getDbName());
                row.put("dbid", r.getDbId());
                row.put("timestamp", new Long(r.getTimestampVer()));
                return row;
            }).collect(Collectors.toList()), writer);
        }
    }

    /**
     * @param collection
     * @param timestamp
     * @return
     * @throws Exception
     */
    public static void registerDependencyDbsFacadeConfig(String collection, long timestamp, DefaultListableBeanFactory factory) {
        try {

            Map<String, DataSourceFactoryPluginStore> dbConfigsMap = null;

            try (InputStream input = FileUtils.openInputStream(StreamContextConstant.getDbDependencyConfigMetaFile(collection, timestamp))) {
                // 这样可以去重
                dbConfigsMap
                        = DBNode.load(input).stream().collect(Collectors.toMap(
                        (db) -> db.getDbName(),
                        (db) -> TIS.getDataBasePluginStore(new PostedDSProp(db.getDbName(), DbScope.FACADE))));

                FacadeDataSource ds = null;
                for (Map.Entry<String, DataSourceFactoryPluginStore> entry : dbConfigsMap.entrySet()) {
                    ds = entry.getValue().createFacadeDataSource();
                    factory.registerSingleton(entry.getKey() + "Datasource", ds.dataSource);
                }

            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static List<DBNode> load(InputStream inputstrea) throws Exception {
        try (InputStreamReader reader = new InputStreamReader(inputstrea, TisUTF8.get()) {
        }) {
            Object o = yaml.load(reader);
            List<Map<String, Object>> configMap = (List) o;
            return configMap.stream().map((r) -> {
                String dbname = (String) r.get("dbname");
                Integer dbid = (Integer) r.get("dbid");
                DBNode db = new DBNode(dbname, dbid);
                db.setTimestampVer(Long.parseLong(String.valueOf(r.get("timestamp"))));
                return db;
            }).collect(Collectors.toList());
        }
    }

    public DBNode(String dbName, int dbId) {
        if (StringUtils.isBlank(dbName)) {
            throw new IllegalArgumentException("param dbName can not be null");
        }
        if (dbId < 1) {
            throw new IllegalArgumentException("param dbId can not be null");
        }
        this.dbName = dbName;
        this.dbId = dbId;
    }

    // getDaoDir(), db.getDbName()
    @Override
    public File getDaoDir() {
        return StreamContextConstant.getDAORootDir(this.getDbName(), this.getTimestampVer());
        // if (this.daoDir == null || !this.daoDir.exists()) {
        // throw new IllegalStateException("dao dir is not exist,dir:" + this.daoDir);
        // }
        // return daoDir;
    }

    // public void setDaoDir(File daoDir) {
    // this.daoDir = daoDir;
    // }
    public long getTimestampVer() {
        return timestampVer;
    }

    public DBNode setTimestampVer(long timestampVer) {
        this.timestampVer = timestampVer;
        return this;
    }

    @Override
    public String getDbName() {
        return dbName;
    }

    public int getDbId() {
        return dbId;
    }

    @Override
    public String toString() {
        return "{" + "dbName='" + dbName + '\'' + ", dbId=" + dbId + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        DBNode dbNode = (DBNode) o;
        return dbId == dbNode.dbId;
    }

    @Override
    public int hashCode() {
        return this.dbId;
    }

}
