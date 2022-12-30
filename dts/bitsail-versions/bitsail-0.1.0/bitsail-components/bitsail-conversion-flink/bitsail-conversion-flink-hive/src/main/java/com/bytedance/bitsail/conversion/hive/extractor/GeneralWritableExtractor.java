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

import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.column.Column;
import com.bytedance.bitsail.common.exception.CommonErrorCode;
import com.bytedance.bitsail.conversion.hive.BitSailColumnConversion;
import com.bytedance.bitsail.conversion.hive.ConvertToHiveObjectOptions;
import com.bytedance.bitsail.conversion.hive.HiveInspectors;
import com.bytedance.bitsail.conversion.hive.HiveObjectConversion;

import org.apache.commons.collections.CollectionUtils;
import org.apache.flink.types.Row;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspectorFactory;
import org.apache.hadoop.hive.serde2.objectinspector.SettableStructObjectInspector;
import org.apache.hadoop.hive.serde2.typeinfo.TypeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GeneralWritableExtractor extends HiveWritableExtractor {

  public static final Logger LOG = LoggerFactory.getLogger(GeneralWritableExtractor.class);

  private HiveObjectConversion[] hiveConversions;
  private List<TypeInfo> columnTypeList;
  private List<String> columnNameList;

  public GeneralWritableExtractor() {
    super();
  }

  @Override
  public void initConvertOptions(ConvertToHiveObjectOptions options) {
    super.initConvertOptions(options);
    BitSailColumnConversion.init(hiveShim, convertOptions);
  }

  @Override
  public SettableStructObjectInspector createObjectInspector(final String columnNames, final String columnTypes) {
    if (CollectionUtils.isEmpty(columnTypeList) || CollectionUtils.isEmpty(columnNameList)) {
      columnTypeList = getHiveTypeInfos(columnTypes);
      columnNameList = getHiveColumnList(columnNames);
      initHiveConversions();
    }
    List<ObjectInspector> objectInspectors = new ArrayList<>(columnNameList.size());
    for (int i = 0; i < columnNameList.size(); i++) {
      objectInspectors.add(HiveInspectors.getObjectInspector(columnTypeList.get(i)));
    }
    inspector = ObjectInspectorFactory.getStandardStructObjectInspector(columnNameList, objectInspectors);
    return inspector;
  }

  @Override
  public Object createRowObject(Row record) {
    String columnName = "";
    try {
      List<Object> fields = Arrays.asList(new Object[inspector.getAllStructFieldRefs().size()]);
      int hiveIndex;
      Column column;
      Object hiveObject;
      for (int i = 0; i < record.getArity(); i++) {
        column = (Column) record.getField(i);
        columnName = fieldNames[i].toUpperCase();
        hiveIndex = columnMapping.get(columnName);
        if (column.getRawData() == null) {
          //Write null when the data is null.
          hiveObject = BitSailColumnConversion.toHiveNull(column);
        } else {
          hiveObject = hiveConversions[hiveIndex].toHiveObject(column);
        }
        fields.set(hiveIndex, hiveObject);
      }
      return fields;
    } catch (Exception e) {
      throw BitSailException.asBitSailException(CommonErrorCode.CONVERT_NOT_SUPPORT, String.format("column[%s] %s", columnName, e.getMessage()));
    }
  }

  /**
   * init hive conversion for general format hive
   */
  private void initHiveConversions() {
    if (hiveConversions == null) {
      hiveConversions = new HiveObjectConversion[columnTypeList.size()];
      for (int i = 0; i < columnTypeList.size(); i++) {
        ObjectInspector objectInspector = HiveInspectors.getObjectInspector(columnTypeList.get(i));
        hiveConversions[i] = HiveInspectors.getConversion(objectInspector, columnTypeList.get(i), hiveShim);
      }
    }
  }
}
