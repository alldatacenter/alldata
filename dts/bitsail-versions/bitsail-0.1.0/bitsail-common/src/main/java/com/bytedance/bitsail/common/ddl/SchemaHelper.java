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

package com.bytedance.bitsail.common.ddl;

import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.exception.CommonErrorCode;
import com.bytedance.bitsail.common.model.ColumnInfo;

import com.google.common.collect.Sets;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

@Slf4j
public class SchemaHelper {

  public static LinkedHashMap<String, String> getLinkedMapColumnsFromConfColumns(List<ColumnInfo> columns, boolean lowerCaseType) {
    if (null == columns || columns.size() == 0) {
      return new LinkedHashMap<>();
    }
    LinkedHashMap<String, String> columnsMap = new LinkedHashMap<>();
    for (ColumnInfo columnInfo : columns) {
      String name = columnInfo.getName();
      String type = columnInfo.getType();
      if (lowerCaseType) {
        type = type.toLowerCase();
      }
      columnsMap.put(name, type);
    }
    return columnsMap;
  }

  public static SchemaIntersectionResponse intersectColumns(List<ColumnInfo> readerColumns, List<ColumnInfo> writerColumns) {
    final Set<String> readerColumnNames = toColumnNamesSet(readerColumns);
    final Set<String> writerColumnNames = toColumnNamesSet(writerColumns);
    final Set<String> intersectionColumnNames = Sets.intersection(readerColumnNames, writerColumnNames);

    if (intersectionColumnNames.size() == 0) {
      throw BitSailException.asBitSailException(CommonErrorCode.PLUGIN_ERROR, "The source columns and sink columns have no intersections, " +
          "and the source columns are " + readerColumns + " the sink columns are " + writerColumns +
          ". Please make sure source columns and sink columns have intersections");
    }

    readerColumns = filterColumnsByNames(readerColumns, intersectionColumnNames);
    writerColumns = filterColumnsByNames(writerColumns, intersectionColumnNames);

    writerColumns = sortColumnsByOtherColumnNames(writerColumns, readerColumns);
    return new SchemaIntersectionResponse(readerColumns, writerColumns);
  }

  /**
   * if cols1.size>cols2.size return cols1 - cols2 else return cols2 - cols1
   *
   * @param cols1
   * @param cols2
   * @return
   */
  public static List<ColumnInfo> getComplementaryColumns(LinkedHashMap<String, String> cols1, LinkedHashMap<String, String> cols2) {
    LinkedHashMap<String, String> temp;
    LinkedHashMap<String, String> cols2Copy = new LinkedHashMap();
    List<ColumnInfo> complementaryColumns = new ArrayList<>();
    if (cols1.size() < cols2.size()) {
      temp = cols1;
      cols1 = cols2;
      cols2 = temp;
    }
    cols2.forEach((x, y) -> cols2Copy.put(convert2ColumnName(x), y));
    Iterator it1 = cols1.entrySet().iterator();
    while (it1.hasNext()) {
      Map.Entry<String, String> entry1 = (Map.Entry<String, String>) it1.next();
      String colName1 = entry1.getKey();
      String colType1 = entry1.getValue();
      if (!cols2Copy.containsKey(convert2ColumnName(colName1))) {
        complementaryColumns.add(getColumn(colName1, colType1));
      }
    }
    return complementaryColumns;
  }

  public static void compareColumnsName(LinkedHashMap<String, String> cols1, LinkedHashMap<String, String> cols2, String cols1Name, String cols2Name) {
    SchemaDifferenceResponse diffColumns = getDifferentColumns(cols1, cols2);
    if (diffColumns.size() > 0) {
      log.warn("Columns mapping warning: " + cols1Name + " columns are not aligned to " + cols2Name + " columns. " +
              "Different " + cols1Name + " columns are:{}, " + cols2Name + " columns are:{}",
          diffColumns.getCols1Diff(), diffColumns.getCols2Diff());
    }
  }

  public static boolean isColumnsSizeEqual(LinkedHashMap<String, String> cols1,
                                           LinkedHashMap<String, String> cols2,
                                           String cols1Name,
                                           String cols2Name) {
    if (cols1.size() != cols2.size()) {
      log.warn("Columns mapping warning: " + cols1Name + " columns and " + cols2Name + " columns size are unequal. " +
              cols1Name + " columns size:{} " + cols2Name + " columns size:{}, complementary Columns set are:{}",
          cols1.size(), cols2.size(), getComplementaryColumns(cols1, cols2));
      return false;
    }
    return true;
  }

  private static boolean compareColumnName(String left, String right) {
    if (null == left || null == right) {
      return false;
    }
    left = convert2ColumnName(left);
    right = convert2ColumnName(right);
    return left.equals(right);
  }

  public static SchemaDifferenceResponse getDifferentColumns(LinkedHashMap<String, String> left, LinkedHashMap<String, String> right) {
    List<ColumnInfo> cols1Diff = new ArrayList<>();
    List<ColumnInfo> cols2Diff = new ArrayList<>();
    if (left.size() != right.size()) {
      log.info("Columns sizes are not equal, cols1 size:{} cols2 size:{},skip different columns check.", left.size(), right.size());
      return new SchemaDifferenceResponse(cols1Diff, cols2Diff);
    }
    Iterator<Map.Entry<String, String>> it1 = left.entrySet().iterator();
    Iterator<Map.Entry<String, String>> it2 = right.entrySet().iterator();
    while (it1.hasNext()) {
      Map.Entry<String, String> entry1 = it1.next();
      Map.Entry<String, String> entry2 = it2.next();
      String colName1 = entry1.getKey();
      String colType1 = entry1.getValue();
      String colName2 = entry2.getKey();
      String colType2 = entry2.getValue();
      if (!compareColumnName(colName1, colName2)) {
        cols1Diff.add(getColumn(colName1, colType1));
        cols2Diff.add(getColumn(colName2, colType2));
      }
    }
    return new SchemaDifferenceResponse(cols1Diff, cols2Diff);
  }

  private static String convert2ColumnName(String colName) {
    String[] split = colName.toLowerCase().replace("_", "").split("\\.");
    colName = split.length > 0 ? split[split.length - 1] : null;
    return colName;
  }

  private static ColumnInfo getColumn(String name, String type) {
    return new ColumnInfo(name, type);
  }

  private static Set<String> toColumnNamesSet(List<ColumnInfo> columns) {
    return columns.stream().map(SchemaHelper::getUpperCaseColName).collect(toSet());
  }

  private static List<ColumnInfo> sortColumnsByOtherColumnNames(List<ColumnInfo> writerCols, List<ColumnInfo> readerCols) {
    // Map<ColumnName, Column>
    final Map<String, ColumnInfo> writerColsMap = writerCols.stream().collect(
        Collectors.toMap(SchemaHelper::getUpperCaseColName, Function.identity()));

    return readerCols.stream().map(
        readerCol -> writerColsMap.get(getUpperCaseColName(readerCol))).collect(toList());
  }

  private static String getUpperCaseColName(ColumnInfo columnInf) {
    return columnInf.getName().toUpperCase();
  }

  private static List<ColumnInfo> filterColumnsByNames(List<ColumnInfo> readerCols, Set<String> intersectionColumnNames) {
    return readerCols.stream()
        .filter(m -> intersectionColumnNames.contains(getUpperCaseColName(m))).collect(Collectors.toList());
  }

  @AllArgsConstructor
  @Getter
  public static class SchemaIntersectionResponse {
    private List<ColumnInfo> readerColumns;
    private List<ColumnInfo> writerColumns;
  }

  @AllArgsConstructor
  @Getter
  public static class SchemaDifferenceResponse {
    private List<ColumnInfo> cols1Diff;
    private List<ColumnInfo> cols2Diff;

    public int size() {
      return cols1Diff.size();
    }
  }
}
