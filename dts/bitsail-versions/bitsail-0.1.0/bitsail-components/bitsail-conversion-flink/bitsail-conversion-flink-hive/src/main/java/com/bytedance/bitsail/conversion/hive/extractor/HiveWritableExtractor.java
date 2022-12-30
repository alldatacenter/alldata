/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytedance.bitsail.conversion.hive.extractor;

import com.bytedance.bitsail.conversion.hive.ConvertToHiveObjectOptions;

import com.bytedance.bitsail.shaded.hive.client.HiveMetaClientUtil;
import com.bytedance.bitsail.shaded.hive.shim.HiveShim;

import lombok.Getter;
import lombok.Setter;
import org.apache.flink.types.Row;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.SettableStructObjectInspector;
import org.apache.hadoop.hive.serde2.typeinfo.TypeInfo;
import org.apache.hadoop.hive.serde2.typeinfo.TypeInfoUtils;
import org.apache.hadoop.io.ArrayWritable;
import org.apache.hadoop.io.Writable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Setter
@Getter
public abstract class HiveWritableExtractor {
  public static final Logger LOG = LoggerFactory.getLogger(HiveWritableExtractor.class);

  protected SettableStructObjectInspector inspector;
  protected ConvertToHiveObjectOptions convertOptions;
  protected String[] fieldNames;
  protected Map<String, Integer> columnMapping;
  protected Map<String, String> inspectorBasicTypeMap;
  protected List<ObjectInspector> objectInspectors;   // used by parquet in streaming

  protected HiveShim hiveShim;

  public HiveWritableExtractor() {
    this.hiveShim = HiveMetaClientUtil.getHiveShim();
  }

  /**
   * parse type info list from columns type string
   *
   * @param columnsTypeStr
   * @return
   */
  public static List<TypeInfo> getHiveTypeInfos(final String columnsTypeStr) {
    List<TypeInfo> columnTypes;

    if (columnsTypeStr.length() == 0) {
      columnTypes = new ArrayList<>();
    } else {
      columnTypes = TypeInfoUtils.getTypeInfosFromTypeString(columnsTypeStr);
    }
    return columnTypes;
  }

  /**
   * parse column name list from string
   *
   * @param columnNamesStr
   * @return
   */
  static List<String> getHiveColumnList(final String columnNamesStr) {
    List<String> columnNames;
    if (columnNamesStr.length() == 0) {
      columnNames = new ArrayList<>();
    } else {
      columnNames = Arrays.asList(columnNamesStr.split(","));
    }
    return columnNames;
  }

  public abstract SettableStructObjectInspector createObjectInspector(final String columnNames, final String columnTypes);

  public abstract Object createRowObject(Row record);

  public void initConvertOptions(ConvertToHiveObjectOptions convertOptions) {
    this.convertOptions = convertOptions;
  }

  protected ArrayWritable createArrayWritable(Writable... values) {
    return new ArrayWritable(Writable.class, values);
  }
}

