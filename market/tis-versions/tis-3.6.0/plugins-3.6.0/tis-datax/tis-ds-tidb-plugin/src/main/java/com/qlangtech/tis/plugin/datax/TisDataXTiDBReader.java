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

package com.qlangtech.tis.plugin.datax;

import com.alibaba.datax.common.element.*;
import com.alibaba.datax.common.plugin.RecordSender;
import com.alibaba.datax.common.plugin.TaskPluginCollector;
import com.alibaba.datax.common.spi.Reader;
import com.alibaba.datax.common.util.Configuration;
import com.pingcap.com.google.common.collect.Lists;
import com.qlangtech.tis.plugin.ds.ColumnMetaData;
import com.qlangtech.tis.plugin.ds.DataDumpers;
import com.qlangtech.tis.plugin.ds.IDataSourceDumper;
import com.qlangtech.tis.plugin.ds.TISTable;
import com.qlangtech.tis.plugin.ds.tidb.DateUtils;
import com.qlangtech.tis.plugin.ds.tidb.TiKVDataSourceDumper;
import com.qlangtech.tis.plugin.ds.tidb.TiKVDataSourceFactory;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import org.apache.commons.lang.StringUtils;

import java.sql.Types;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-06-06 09:09
 **/
public class TisDataXTiDBReader extends Reader {

    private static final String PATH_REGION_ID = "connection[0].regionId";

    private static final List<IDataSourceDumper> getTiKVDataSource(Configuration config, Optional<Long> regionId) {
        List<String> cols = config.getList("column", String.class);
        EntityName tableName = EntityName.parse(config.getString("table"));
        Configuration connection = config.getConfiguration("connection");


        TiKVDataSourceFactory sourceFactory = new TiKVDataSourceFactory();
        sourceFactory.pdAddrs = connection.getString("[0].pdAddrs");
        sourceFactory.dbName = connection.getString("[0].dbName");
        // sourceFactory.datetimeFormat = connection.getBool("datetimeFormat");
        if (StringUtils.isBlank(sourceFactory.pdAddrs) || StringUtils.isBlank(sourceFactory.dbName)) {
            throw new IllegalStateException("param 'pdAddrs' or 'dbName' can not be null,connection:" + connection.toJSON());
        }

        List<ColumnMetaData> tableMetadata = sourceFactory.getTableMetadata(tableName);

        TISTable table = new TISTable();
        table.setTableName(tableName.getTableName());
        table.setReflectCols(tableMetadata.stream()
                .filter((cmeta) -> cols.contains(cmeta.getKey())).collect(Collectors.toList()));
        DataDumpers dataDumpers = sourceFactory.getDataDumpers(table, regionId);
        List<IDataSourceDumper> dumpers = Lists.newArrayList(dataDumpers.dumpers);
        return dumpers;
    }

    public static class Job extends Reader.Job {
        private Configuration originalConfig;

        @Override
        public List<Configuration> split(int mandatoryNumber) {

//            List<String> cols = this.originalConfig.getList("column", String.class);
//            String tableName = this.originalConfig.getString("table");
//            Configuration connection = this.originalConfig.getConfiguration("connection");
//
//
//            TiKVDataSourceFactory sourceFactory = new TiKVDataSourceFactory();
//            sourceFactory.pdAddrs = connection.getString("pdAddrs");
//            sourceFactory.dbName = connection.getString("dbName");
//            sourceFactory.datetimeFormat = connection.getBool("datetimeFormat");
//
//            TISTable table = new TISTable();
//            table.setTableName(tableName);
//            int[] index = {0};
//            table.setReflectCols(cols.stream().map((c) -> {
//                return BasicDataXRdbmsReader.createColumnMetaData(index, c);
//            }).collect(Collectors.toList()));
//
//            DataDumpers dataDumpers = sourceFactory.getDataDumpers(table);
            List<IDataSourceDumper> dumpers = getTiKVDataSource(this.originalConfig, Optional.empty());

            return dumpers.stream().flatMap((dumper) -> {

                TiKVDataSourceDumper tikvDumper = (TiKVDataSourceDumper) dumper;

                return tikvDumper.partition.tasks.stream().map((regTsk) -> {
                    Configuration cloneCfg = this.originalConfig.clone();
                    cloneCfg.set(PATH_REGION_ID, regTsk.getRegion().getId());
                    return cloneCfg;
                });
            }).collect(Collectors.toList());
        }

        @Override
        public void init() {
            this.originalConfig = super.getPluginJobConf();
        }

        @Override
        public void destroy() {

        }
    }


    public static class Task extends Reader.Task {
        private Configuration cfg;

        @Override
        public void init() {
            this.cfg = super.getPluginJobConf();

        }

        @Override
        public void startRead(RecordSender recordSender) {
            List<IDataSourceDumper> dumpers = getTiKVDataSource(this.cfg, Optional.of(this.cfg.getLong(PATH_REGION_ID)));
            Iterator<Map<String, Object>> rowsIt = null;
            Map<String, Object> row = null;
            Record record = null;
            Column column = null;
            List<ColumnMetaData> metaData = null;
            TaskPluginCollector taskPluginCollector = this.getTaskPluginCollector();
            for (IDataSourceDumper dumper : dumpers) {
                try {
                    metaData = dumper.getMetaData();

                    rowsIt = dumper.startDump();
                    while (rowsIt.hasNext()) {
                        try {
                            record = recordSender.createRecord();
                            row = rowsIt.next();

                            for (ColumnMetaData m : metaData) {
                                record.addColumn(createCol(m, row.get(m.getKey())));
                            }
                            recordSender.sendToWriter(record);
                        } catch (Exception e) {
                            taskPluginCollector.collectDirtyRecord(record, e);
                        }
                    }
                } finally {
                    dumper.closeResource();
                }
            }
        }

        private Column createCol(ColumnMetaData m, Object val) {

            if (val == null) {
                return new StringColumn();
            }

            DateColumn d = null;
            switch (m.getType().type) {
                case Types.TINYINT:
                case Types.INTEGER:
                case Types.BIGINT:
                    return new LongColumn((String) val);
                case Types.DECIMAL:
                case Types.FLOAT:
                case Types.DOUBLE:
                    return new DoubleColumn((String) val);
                case Types.TIMESTAMP:
                    d = new DateColumn(((Long) val) / 1000);
                    d.setSubType(DateColumn.DateType.DATETIME);
                    return d;
                case Types.DATE:
                    d = new DateColumn(DateUtils.formatDate((Long) (val)));
                    d.setSubType(DateColumn.DateType.DATE);
                    return d;
//                case Types.NULL:
//                case Types.VARCHAR:
//                    return new StringColumn(val);
                default:
                    return new StringColumn((String) val);
            }

        }


        @Override
        public void destroy() {

        }
    }
}
