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
package com.qlangtech.tis.plugin.ds.tidb;

import com.alibaba.citrus.turbine.Context;
import com.pingcap.com.google.common.collect.Lists;
import com.pingcap.com.google.common.collect.Maps;
import com.pingcap.tikv.TiConfiguration;
import com.pingcap.tikv.TiSession;
import com.pingcap.tikv.catalog.Catalog;
import com.pingcap.tikv.meta.TiDAGRequest;
import com.pingcap.tikv.meta.TiDBInfo;
import com.pingcap.tikv.meta.TiTableInfo;
import com.pingcap.tikv.util.RangeSplitter;
import com.qlangtech.tis.annotation.Public;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import com.qlangtech.tis.plugin.annotation.Validator;
import com.qlangtech.tis.plugin.ds.*;
import com.qlangtech.tis.runtime.module.misc.IControlMsgHandler;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.sql.Types;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

//import com.pingcap.tikv.types.DataType;

/**
 * 针对PingCap TiKV作为数据源实现
 *
 * @author: baisui 百岁
 * @create: 2020-11-24 10:55
 **/
@Public
public class TiKVDataSourceFactory extends DataSourceFactory {

    private transient static final Logger logger = LoggerFactory.getLogger(TiKVDataSourceFactory.class);

    @FormField(identity = true, ordinal = 0, type = FormFieldType.INPUTTEXT, validate = {Validator.require, Validator.identity})
    public String name;

    @FormField(ordinal = 2, type = FormFieldType.INPUTTEXT, validate = {Validator.require, Validator.host})
    public String pdAddrs;

    @FormField(identity = false, ordinal = 1, type = FormFieldType.INPUTTEXT, validate = {Validator.require, Validator.db_col_name})
    public String dbName;

    static {
//        public class TiSession implements AutoCloseable {
//            private static final Logger logger = LoggerFactory.getLogger(com.pingcap.tikv.TiSession.class);
//            private static final Map<String, com.pingcap.tikv.TiSession> sessionCachedMap = new HashMap<>();

        try {
            Field sessionCachedMap = TiSession.class.getDeclaredField("sessionCachedMap");//.getDeclaredField();
            sessionCachedMap.setAccessible(true);

            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(sessionCachedMap, sessionCachedMap.getModifiers() & ~java.lang.reflect.Modifier.FINAL);

            sessionCachedMap.set(null, new HashMap<String, com.pingcap.tikv.TiSession>() {
                @Override
                public boolean containsKey(Object key) {
                    // 将缓存作用废掉
                    return false;
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String identityValue() {
        return this.name;
    }

    /**
     * 是否要对date或者timestamp进行格式化
     */
    //@FormField(ordinal = 2, type = FormFieldType.ENUM, validate = {Validator.require, Validator.identity})
    public final boolean datetimeFormat = false;


    public DataDumpers getDataDumpers(TISTable table, Optional<Long> regionId) {
// target cols
        final List<ColumnMetaData> reflectCols = table.getReflectCols();
        if (CollectionUtils.isEmpty(reflectCols)) {
            throw new IllegalStateException("param reflectCols can not be null");
        }

        final AtomicReference<TiTableInfoWrapper> tabRef = new AtomicReference<>();


        final List<TiPartition> parts = this.openTiDB((session, c, db) -> {

            TiTableInfo tiTable = c.getTable(db, table.getTableName());
            Objects.requireNonNull(tiTable, "table:" + table.getTableName() + " can not find relevant table in TiDB");
            tabRef.set(new TiTableInfoWrapper(tiTable));
            TiDAGRequest dagRequest = getTiDAGRequest(reflectCols, session, tiTable);
            List<Long> prunedPhysicalIds = dagRequest.getPrunedPhysicalIds();
            return prunedPhysicalIds.stream().flatMap((prunedPhysicalId)
                    -> createPartitions(prunedPhysicalId, session, dagRequest.copyReqWithPhysicalId(prunedPhysicalId), regionId).stream())
                    .collect(Collectors.toList());
        });

        int[] index = new int[1];
        final int splitCount = parts.size();
        Objects.requireNonNull(tabRef.get(), "instacne of TiTableInfo can not be null");
        Iterator<IDataSourceDumper> dumpers = new Iterator<IDataSourceDumper>() {
            @Override
            public boolean hasNext() {
                return index[0] < splitCount;
            }

            @Override
            public IDataSourceDumper next() {
                return new TiKVDataSourceDumper(TiKVDataSourceFactory.this, parts.get(index[0]++), tabRef.get(), reflectCols);
            }
        };
        return new DataDumpers(splitCount, dumpers);
    }

    @Override
    public DataDumpers getDataDumpers(TISTable table) {
        return getDataDumpers(table, Optional.empty());
    }


    public TiDAGRequest getTiDAGRequest(List<ColumnMetaData> reflectCols, TiSession session, TiTableInfo tiTable) {
        return TiDAGRequest.Builder
                .newBuilder()
                .setFullTableScan(tiTable)
                .addRequiredCols(reflectCols.stream().map((col) -> col.getKey()).collect(Collectors.toList()))
                .setStartTs(session.getTimestamp())
                .build(TiDAGRequest.PushDownType.NORMAL);
    }

    public List<TiPartition> createPartitions(Long physicalId, TiSession session, TiDAGRequest dagRequest, Optional<Long> regionId) {

        final List<TiPartition> partitions = Lists.newArrayList();

        List<RangeSplitter.RegionTask> keyWithRegionTasks = RangeSplitter
                .newSplitter(session.getRegionManager())
                .splitRangeByRegion(dagRequest.getRangesByPhysicalId(physicalId), dagRequest.getStoreType());

        Map<String, List<RangeSplitter.RegionTask>> hostTasksMap = Maps.newHashMap();
        List<RangeSplitter.RegionTask> tasks = null;
        for (RangeSplitter.RegionTask task : keyWithRegionTasks) {
            if (regionId.isPresent() && regionId.get() != task.getRegion().getId()) {
                // 在task中需要对region进行过滤，每一个task执行一个region的dump任务
                continue;
            }
            tasks = hostTasksMap.get(task.getHost());
            if (tasks == null) {
                tasks = Lists.newArrayList();
                hostTasksMap.put(task.getHost(), tasks);
            }
            tasks.add(task);
        }
        int index = 0;
        for (List<RangeSplitter.RegionTask> tks : hostTasksMap.values()) {
            partitions.add(new TiPartition(index++, tks));
        }
        return partitions;
    }

    @Override
    public List<String> getTablesInDB() {
        return this.openTiDB((s, c, d) -> {
            List<TiTableInfo> tabs = c.listTables(d);
            // either view or sequence shall be filter
            return tabs.stream().filter((tbl) -> (tbl != null && !(tbl.isView() || tbl.isSequence())))
                    .map((tt) -> tt.getName()).collect(Collectors.toList());
        });
    }

    <T> T openTiDB(IVistTiDB<T> vistTiDB) {
        TiSession session = null;
        try {
            session = getTiSession();
            try (Catalog cat = session.getCatalog()) {
                TiDBInfo db = cat.getDatabase(dbName);
                return vistTiDB.visit(session, cat, db);
            }
        } finally {
            try {
                session.close();
            } catch (Throwable e) {}
        }
    }

    public TiSession getTiSession() {
        TiConfiguration conf = TiConfiguration.createDefault(this.pdAddrs);
        return TiSession.getInstance(conf);
    }


    @Override
    public List<ColumnMetaData> getTableMetadata(EntityName table) {
        return this.openTiDB((session, c, db) -> {
            TiTableInfo table1 = c.getTable(db, table.getTableName());
            int[] index = new int[1];
            if (table1 == null) {
                throw new IllegalStateException("table:" + table + " can not find relevant table in db:" + db.getName());
            }
            return table1.getColumns().stream().map((col) -> {
                // ref: com.pingcap.tikv.types.MySQLType
                ColumnMetaData cmd
                        = new ColumnMetaData(index[0]++, col.getName(), map2JdbcType(col.getName()
                        , col.getType()), col.isPrimaryKey(), !col.getType().isNotNull());
                cmd.setSchemaFieldType(typeMap(col.getType()));
                return cmd;
            }).collect(Collectors.toList());
        });
    }

    private com.qlangtech.tis.plugin.ds.DataType map2JdbcType(String keyName, com.pingcap.tikv.types.DataType type) {
        int colSize = (int) Long.min(Integer.MAX_VALUE, type.getLength());
        DataType tisType = new DataType(jdbcType(keyName, type), type.getName(), colSize);
        // type.getType()
        tisType.setDecimalDigits(type.getDecimal());
        return tisType;
    }

    private int jdbcType(String keyName, com.pingcap.tikv.types.DataType type) {
        switch (type.getType()) {
            case TypeDecimal:
                return Types.DECIMAL;
            case TypeTiny:
            case TypeShort:
                return Types.TINYINT;
            case TypeLong:
                return Types.BIGINT;
            case TypeFloat:
                return Types.FLOAT;
            case TypeDouble:
                return Types.DOUBLE;
            case TypeNull:
                return Types.NULL;
            case TypeInt24:
            case TypeLonglong:
                return Types.INTEGER;
            case TypeTimestamp:
            case TypeDatetime:
                //return datetimeFormat ? Types.TIMESTAMP : Types.BIGINT;
                return Types.TIMESTAMP;
            case TypeDate:
            case TypeNewDate:
                // return datetimeFormat ? Types.DATE : Types.INTEGER;
                return Types.DATE;
            case TypeDuration:
            case TypeYear:
            case TypeBit:
            case TypeJSON:
                return Types.VARCHAR;
            case TypeNewDecimal:
                return Types.DECIMAL;
            case TypeEnum:
            case TypeSet:
            case TypeGeometry:
                return Types.VARCHAR;
            case TypeTinyBlob:
            case TypeMediumBlob:
            case TypeLongBlob:
            case TypeBlob:
            case TypeVarString:
            case TypeString:
            case TypeVarchar:
                return Types.VARCHAR;
            default:
                throw new RuntimeException("illegal type:" + type);
        }
    }

    private ReservedFieldType typeMap(com.pingcap.tikv.types.DataType dtype) {

        switch (dtype.getType()) {
            case TypeDecimal:
                return new ReservedFieldType(ReflectSchemaFieldType.FLOAT);
            case TypeTiny:
            case TypeShort:
                return new ReservedFieldType(ReflectSchemaFieldType.INT);
            case TypeLong:
                return new ReservedFieldType(ReflectSchemaFieldType.LONG);
            case TypeFloat:
                return new ReservedFieldType(ReflectSchemaFieldType.FLOAT);
            case TypeDouble:
                return new ReservedFieldType(ReflectSchemaFieldType.DOUBLE);
            case TypeNull:
                return new ReservedFieldType(ReflectSchemaFieldType.STRING);
            case TypeTimestamp:
            case TypeDatetime:
                return new ReservedFieldType(
                        this.datetimeFormat ?
                                ReflectSchemaFieldType.TIMESTAMP
                                : ReflectSchemaFieldType.STRING);
            case TypeDate:
            case TypeNewDate:
                return new ReservedFieldType(
                        this.datetimeFormat ?
                                ReflectSchemaFieldType.DATE
                                : ReflectSchemaFieldType.STRING);
            // return this.datetimeFormat ?
            case TypeLonglong:
            case TypeInt24:
                // TypeDuration is just MySQL time type.
                // MySQL uses the 'HHH:MM:SS' format, which is larger than 24 hours.
                return new ReservedFieldType(ReflectSchemaFieldType.LONG);
            case TypeDuration:
            case TypeYear:
            case TypeBit:
            case TypeJSON:
                return new ReservedFieldType(ReflectSchemaFieldType.STRING);
            case TypeNewDecimal:
                return new ReservedFieldType(ReflectSchemaFieldType.FLOAT);
            case TypeEnum:
            case TypeSet:
            case TypeGeometry:
                return new ReservedFieldType(ReflectSchemaFieldType.STRING);
            case TypeTinyBlob:
            case TypeMediumBlob:
            case TypeLongBlob:
            case TypeBlob:
            case TypeVarString:
            case TypeString:
            case TypeVarchar:
                return new ReservedFieldType(ReflectSchemaFieldType.STRING, true);
            default:
                throw new RuntimeException("illegal type:" + dtype);
        }
    }


    @TISExtension
    public static class DefaultDescriptor extends DataSourceFactory.BaseDataSourceFactoryDescriptor<TiKVDataSourceFactory> {

        @Override
        protected String getDataSourceName() {
            return "TiDB";
        }

        @Override
        protected boolean supportFacade() {
            return true;
        }

        @Override
        protected List<String> facadeSourceTypes() {
            return Collections.singletonList(DS_TYPE_MYSQL);
        }

        protected boolean validateDSFactory(IControlMsgHandler msgHandler, Context context, TiKVDataSourceFactory sourceFactory) {
            try {
//                ParseDescribable<DataSourceFactory> tikv = this.newInstance((IPluginContext) msgHandler, postFormVals.rawFormData, Optional.empty());
//                DataSourceFactory sourceFactory = tikv.instance;
                List<String> tables = sourceFactory.getTablesInDB();
                if (tables.size() < 1) {
                    msgHandler.addErrorMessage(context, "TiKV库'" + sourceFactory.dbName + "'中的没有数据表");
                    return false;
                }
            } catch (Exception e) {
                msgHandler.addErrorMessage(context, "请检查配置是否正确,错误:" + e.getMessage());
                logger.warn(e.getMessage(), e);
                // throw new RuntimeException(e);
                return false;
            }
            return true;
        }
    }
}
