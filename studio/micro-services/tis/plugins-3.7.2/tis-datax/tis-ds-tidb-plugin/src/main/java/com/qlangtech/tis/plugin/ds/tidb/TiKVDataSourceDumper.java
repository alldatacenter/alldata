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

import com.qlangtech.tis.plugin.ds.ColumnMetaData;
import com.qlangtech.tis.plugin.ds.IDataSourceDumper;
import org.tikv.common.TiSession;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author: baisui 百岁
 * @create: 2020-12-04 14:31
 **/
public class TiKVDataSourceDumper implements IDataSourceDumper {
    private final TiTableInfoWrapper tab;

    private final TiKVDataSourceFactory dsFactory;
    public final TiPartition partition;
    private final List<ColumnMetaData> targetCols;

    private TiSession tiSession;

    public TiKVDataSourceDumper(TiKVDataSourceFactory dsFactory, TiPartition p
            , TiTableInfoWrapper tab, List<ColumnMetaData> targetCols) {
        this.partition = p;
        this.dsFactory = dsFactory;
        this.targetCols = targetCols;
        this.tab = tab;
    }

    @Override
    public Iterator<Map<String, Object>> startDump() {
        return null;
//        this.tiSession = dsFactory.getTiSession();
//
//        //Catalog cat = this.tiSession.getCatalog();
//        // TiDBInfo db = cat.getDatabase(dbName);
//        // TiTableInfo tiTable = cat.getTable(db, table.getTableName());
//
//        TiDAGRequest dagRequest = dsFactory.getTiDAGRequest(this.targetCols, tiSession, tab.tableInfo);
//
//        Snapshot snapshot = tiSession.createSnapshot(dagRequest.getStartTs());
//
//        // 取得的是列向量
//        Iterator<TiChunk> tiChunkIterator = snapshot.tableReadChunk(dagRequest, this.partition.tasks, 1024);
//
//        return new Iterator<Map<String, Object>>() {
//            TiChunk next = null;
//            int numOfRows = -1;
//            int rowIndex = -1;
//
//            TiColumnVector column = null;
//            ColumnMetaData columnMetaData;
//
//            @Override
//            public boolean hasNext() {
//
//                if (next != null) {
//                    if (rowIndex++ < (numOfRows - 1)) {
//                        return true;
//                    }
//                    next = null;
//                    numOfRows = -1;
//                    rowIndex = -1;
//                }
//
//                boolean hasNext = tiChunkIterator.hasNext();
//                if (hasNext) {
//                    next = tiChunkIterator.next();
//                    if (next == null) {
//                        throw new IllegalStateException("next TiChunk can not be null");
//                    }
//                    rowIndex = 0;
//                    numOfRows = next.numOfRows();
//
//                }
//                return hasNext;
//            }
//
//            @Override
//            public Map<String, Object> next() {
//                Map<String, Object> row = new HashMap<>();
//                MySQLType colType = null;
//                for (int i = 0; i < targetCols.size(); i++) {
//                    column = next.column(i);
//                    if (column.isNullAt(rowIndex)) {
//                        continue;
//                    }
//                    colType = column.dataType().getType();
//                    columnMetaData = targetCols.get(i);
//                    if (colType == MySQLType.TypeVarchar
//                            || colType == MySQLType.TypeString
//                            || colType == MySQLType.TypeBlob) {
//                        row.put(columnMetaData.getKey(), filter(column.getUTF8String(rowIndex)));
//                    } else if (colType == MySQLType.TypeDate || colType == MySQLType.TypeNewDate) {
//                        // FIXME 日期格式化 一个1970年的一个偏移量，按照实际情况估计要重新format一下
//                        // https://cwiki.apache.org/confluence/display/Hive/LanguageManual+Types#LanguageManualTypes-date
//
//                        row.put(columnMetaData.getKey()
//                                ,
////                                       dsFactory.datetimeFormat
////                                        ? DateUtils.formatDate(column.getLong(rowIndex))
////                                        :
//                                column.getLong(rowIndex));
//
//                    } else if (colType == MySQLType.TypeTimestamp || colType == MySQLType.TypeDatetime) {
//                        row.put(columnMetaData.getKey(),
////                                dsFactory.datetimeFormat
////                                        ? DateUtils.formatTimestamp(column.getLong(rowIndex))
////                                        :
//                                column.getLong(rowIndex));
//                    } else {
//                        row.put(columnMetaData.getKey(), column.getUTF8String(rowIndex));
//                    }
//                }
//                return row;
//            }
//        };
    }

    private static String filter(String input) {
        return input;
//        if (input == null) {
//            return input;
//        }
//        StringBuffer filtered = new StringBuffer(input.length());
//        char c;
//        for (int i = 0; i <= input.length() - 1; i++) {
//            c = input.charAt(i);
//            switch (c) {
//                case '\t':
//                    break;
//                case '\r':
//                    break;
//                case '\n':
//                    break;
//                default:
//                    filtered.append(c);
//            }
//        }
//        return (filtered.toString());
    }

    @Override
    public void closeResource() {
        try {
            this.tiSession.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getRowSize() {
        return tab.getRowSize();
    }

    @Override
    public List<ColumnMetaData> getMetaData() {
//        int[] index = new int[1];
//        return tab.getColumns().stream().map((c) -> {
//            return new ColumnMetaData(index[0]++, c.getName(), c.getType().getTypeCode(), false);
//        }).collect(Collectors.toList());
        return this.targetCols;
    }


    @Override
    public String getDbHost() {
        return "partition_" + partition.idx;
    }
}
