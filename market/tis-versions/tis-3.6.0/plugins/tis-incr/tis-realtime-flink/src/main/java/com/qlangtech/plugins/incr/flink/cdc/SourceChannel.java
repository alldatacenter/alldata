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

package com.qlangtech.plugins.incr.flink.cdc;

import com.google.common.collect.Maps;
import com.qlangtech.tis.async.message.client.consumer.AsyncMsg;
import com.qlangtech.tis.async.message.client.consumer.Tab2OutputTag;
import com.qlangtech.tis.datax.TableAliasMapper;
import com.qlangtech.tis.plugin.ds.BasicDataSourceFactory;
import com.qlangtech.tis.plugin.ds.DBConfig;
import com.qlangtech.tis.plugin.ds.ISelectedTab;
import com.qlangtech.tis.realtime.dto.DTOStream;
import com.qlangtech.tis.realtime.ReaderSource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.compress.utils.Lists;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-09-27 15:47
 **/
public class SourceChannel implements AsyncMsg<List<ReaderSource>> {

    private final List<ReaderSource> sourceFunction;
    private Set<String> focusTabs = null;// = Sets.newHashSet();
    private Tab2OutputTag<DTOStream> tab2OutputTag = null;

    @Override
    public Tab2OutputTag<DTOStream> getTab2OutputTag(
            //        Function<String, List<com.qlangtech.plugins.incr.flink.cdc.FlinkCol>> colsCreator
    ) {
        return Objects.requireNonNull(tab2OutputTag);
    }

    public SourceChannel(List<ReaderSource> sourceFunction) {
        if (CollectionUtils.isEmpty(sourceFunction)) {
            throw new IllegalArgumentException("param sourceFunction can not be empty");
        }
        this.sourceFunction = sourceFunction;
    }

    public static List<ReaderSource> getSourceFunction(
            BasicDataSourceFactory dsFactory, List<ISelectedTab> tabs, ReaderSourceCreator sourceFunctionCreator) {
        return getSourceFunction(dsFactory, (tab) -> tab.dbNanme + "." + tab.tab.getName(), tabs, sourceFunctionCreator);
    }

    //https://ververica.github.io/flink-cdc-connectors/master/
    public static List<ReaderSource> getSourceFunction(
            BasicDataSourceFactory dsFactory, Function<DBTable, String> tabnameCreator
            , List<ISelectedTab> tabs, ReaderSourceCreator sourceFunctionCreator) {

        try {
            DBConfig dbConfig = dsFactory.getDbConfig();
            List<ReaderSource> sourceFuncs = Lists.newArrayList();
            Map<String, List<String>> ip2dbs = Maps.newHashMap();
            Map<String, List<ISelectedTab>> db2tabs = Maps.newHashMap();
            dbConfig.vistDbName((config, ip, dbName) -> {
                List<String> dbs = ip2dbs.get(ip);
                if (dbs == null) {
                    dbs = Lists.newArrayList();
                    ip2dbs.put(ip, dbs);
                }
                dbs.add(dbName);
                if (db2tabs.get(dbName) == null) {
                    db2tabs.put(dbName, tabs);
                }
                return false;
            });

            for (Map.Entry<String /**ip*/, List<String>/**dbs*/> entry : ip2dbs.entrySet()) {

                Set<String> tbs = entry.getValue().stream().flatMap(
                        (dbName) -> db2tabs.get(dbName).stream().map((tab) -> {
                            return tabnameCreator.apply(new DBTable(dbName, tab));
                        })).collect(Collectors.toSet());

                Properties debeziumProperties = new Properties();
                debeziumProperties.put("snapshot.locking.mode", "none");// do not use lock

                String dbHost = entry.getKey();
                List<String> dbs = entry.getValue();
                sourceFuncs.addAll(sourceFunctionCreator.create(dbHost, dbs, tbs, debeziumProperties));
            }

            return sourceFuncs;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static class DBTable {
        public final String dbNanme;
        private final ISelectedTab tab;

        public DBTable(String dbNanme, ISelectedTab tab) {
            this.dbNanme = dbNanme;
            this.tab = tab;
        }

        public String getTabName() {
            return tab.getName();
        }
    }

    public interface ReaderSourceCreator {
        List<ReaderSource> create(String dbHost, List<String> dbs, Set<String> tbs, Properties debeziumProperties);
    }

    @Override
    public List<ReaderSource> getSource() throws IOException {
        return this.sourceFunction;
    }


    @Override
    public String getMsgID() {
        return null;
    }

    @Override
    public Set<String> getFocusTabs() {
        return this.focusTabs;
    }

    public void setFocusTabs(List<ISelectedTab> tabs, TableAliasMapper tabAliasMapper, Function<String, DTOStream> dtoStreamCreator) {
        if (CollectionUtils.isEmpty(tabs)) {
            throw new IllegalArgumentException("param tabs can not be null");
        }
        if (tabAliasMapper.isNull()) {
            throw new IllegalArgumentException("param tabAliasMapper can not be null");
        }
        this.focusTabs = tabs.stream().map((t) -> t.getName()).collect(Collectors.toSet());
        this.tab2OutputTag
                = new Tab2OutputTag<>(tabs.stream().collect(
                Collectors.toMap(
                        (tab) -> (tabAliasMapper.getWithCheckNotNull(tab.getName()))
                        , (t) -> dtoStreamCreator.apply(t.getName()))));
    }


    @Override
    public String getTopic() {
        return null;
    }

    @Override
    public String getTag() {
        return null;
    }


}
