/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sort.hive.util;

import org.apache.inlong.sort.hive.HiveWriterFactory;
import org.apache.inlong.sort.hive.filesystem.HadoopRenameFileCommitter;

import org.apache.flink.table.catalog.ObjectIdentifier;
import org.apache.flink.table.data.RowData;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.ql.exec.FileSinkOperator;
import org.apache.hadoop.hive.ql.exec.FileSinkOperator.RecordWriter;
import org.apache.hadoop.io.Writable;

import java.util.HashMap;
import java.util.function.Function;

public class CacheHolder {

    /**
     * hive writer factory cache
     */
    private static final HashMap<ObjectIdentifier, HiveWriterFactory> factoryMap = new HashMap<>(16);
    /**
     * hive record writer cache
     */
    private static final HashMap<Path, FileSinkOperator.RecordWriter> recordWriterHashMap = new HashMap<>(16);
    /**
     * hive hdfs file committer cache
     */
    private static final HashMap<Path, HadoopRenameFileCommitter> fileCommitterHashMap = new HashMap<>(16);
    /**
     * hive row converter cache
     */
    private static final HashMap<Path, Function<RowData, Writable>> rowConverterHashMap = new HashMap<>(16);
    /**
     * hive schema check time cache
     */
    private static final HashMap<ObjectIdentifier, Long> schemaCheckTimeMap = new HashMap<>(16);
    /**
     * ignore writing hive table after exception as schema policy is STOP_PARTIAL
     */
    private static final HashMap<ObjectIdentifier, Long> ignoreWritingTableMap = new HashMap<>(16);

    public static HashMap<Path, RecordWriter> getRecordWriterHashMap() {
        return recordWriterHashMap;
    }

    public static HashMap<Path, Function<RowData, Writable>> getRowConverterHashMap() {
        return rowConverterHashMap;
    }

    public static HashMap<ObjectIdentifier, HiveWriterFactory> getFactoryMap() {
        return factoryMap;
    }

    public static HashMap<Path, HadoopRenameFileCommitter> getFileCommitterHashMap() {
        return fileCommitterHashMap;
    }

    public static HashMap<ObjectIdentifier, Long> getSchemaCheckTimeMap() {
        return schemaCheckTimeMap;
    }

    public static HashMap<ObjectIdentifier, Long> getIgnoreWritingTableMap() {
        return ignoreWritingTableMap;
    }

}
