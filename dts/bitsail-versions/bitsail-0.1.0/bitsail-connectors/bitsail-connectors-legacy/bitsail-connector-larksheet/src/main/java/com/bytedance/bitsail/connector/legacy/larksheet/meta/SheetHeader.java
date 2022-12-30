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

package com.bytedance.bitsail.connector.legacy.larksheet.meta;

import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.model.ColumnInfo;
import com.bytedance.bitsail.connector.legacy.larksheet.error.LarkSheetFormatErrorCode;
import com.bytedance.bitsail.connector.legacy.larksheet.util.LarkSheetUtil;

import lombok.Getter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SheetHeader implements Serializable {
  private static final Logger LOG = LoggerFactory.getLogger(SheetHeader.class);

  private static final long serialVersionUID = -6436874549909885424L;

  /**
   * LarkSheet column indices are characters (A,B,C,D), so we need to transform characters into integer indices.
   * <br>key: column name
   * <br>value: column index, start at 0.
   */
  private final Map<String, Integer> columnIndexMap;

  @Getter
  private Integer[] reorderColumnIndex;

  /**
   * Last column index in header.
   */
  private int lastColumnNumber;

  public SheetHeader(List<Object> firstRow, List<ColumnInfo> columns, String sheetToken, String sheetId) {
    this.columnIndexMap = new LinkedHashMap<>(firstRow.size());

    // Find the last non-null column.
    int point = firstRow.size() - 1;
    while (point >= 0) {
      if (firstRow.get(point) == null) {
        point--;
      } else {
        break;
      }
    }
    this.lastColumnNumber = point;
    if (CollectionUtils.isEmpty(columns) || lastColumnNumber < 0) {
      throw new BitSailException(LarkSheetFormatErrorCode.INVALID_SHEET_HEADER,
          String.format("All header cells are empty, or reader columns defined is empty! the sheet token is" +
              " [%s], the sheet Id is [%s]", sheetToken, sheetId));
    }

    // Construct map: sheet header -> index.
    for (int i = 0; i <= lastColumnNumber; i++) {
      String cell = ObjectUtils.toString(firstRow.get(i));
      if (StringUtils.isBlank(cell)) {
        throw new BitSailException(LarkSheetFormatErrorCode.INVALID_SHEET_HEADER,
            String.format("Sheet header [%s] is empty, the sheet token is [%s], the sheet Id is [%s]",
                LarkSheetUtil.numberToSequence(i), sheetToken, sheetId));
      }

      if (columnIndexMap.containsKey(cell)) {
        throw new BitSailException(LarkSheetFormatErrorCode.INVALID_SHEET_HEADER,
            String.format("Sheet header has duplicated cell, [%s] and [%s], the value is [%s],the sheet token is [%s], the sheet Id is [%s]",
                LarkSheetUtil.numberToSequence(columnIndexMap.get(cell)),
                LarkSheetUtil.numberToSequence(i),
                cell,
                sheetToken,
                sheetId
            )
        );
      } else {
        columnIndexMap.put(cell, i);
      }
    }

    alignWith(columns, sheetToken, sheetId);
    LOG.info("sheet header generated! {}", this);
  }

  /**
   * @return Start column. Always return "A".
   */
  public String getStartColumn() {
    return "A";
  }

  /**
   * @return End column index.
   */
  public String getEndColumn() {
    return LarkSheetUtil.numberToSequence(lastColumnNumber + 1);
  }

  /**
   * This method is used for:
   * 1. Compare sheet header from API and columns defined in job conf.
   * 2. Find the last columns that will be used in the task.
   *
   * @param readerColumns Columns info defined in job conf.
   */
  public void alignWith(List<ColumnInfo> readerColumns, String sheetToken, String sheetId) {
    int readerColumnSize = readerColumns.size();

    if (readerColumnSize > columnIndexMap.size()) {
      throw new BitSailException(LarkSheetFormatErrorCode.INVALID_SHEET_HEADER,
          String.format("Your sheet has less header cell: [%d] than job conf defined: [%d] , " +
                  "the sheet token is: [%s], the sheet Id is [%s]",
              columnIndexMap.size(), readerColumns.size(), sheetToken, sheetId));
    }
    this.reorderColumnIndex = new Integer[readerColumnSize];

    // Find the last columns that will be used in the task.
    // This helps filter unused columns when calling `/:range` api.
    int columnNumber = 0;
    for (int i = 0; i < readerColumnSize; i++) {
      String name = readerColumns.get(i).getName();
      if (!columnIndexMap.containsKey(name)) {
        throw new BitSailException(LarkSheetFormatErrorCode.INVALID_SHEET_HEADER,
            String.format("Header cell [%s] is not found, but defined in job conf, " +
                "the sheet token is: [%s], the sheet Id is [%s]",
                name, sheetToken, sheetId));
      }
      reorderColumnIndex[i] = columnIndexMap.get(name);
      columnNumber = Math.max(columnNumber, columnIndexMap.get(name));
    }
    lastColumnNumber = columnNumber;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("Sheet header is: [");
    for (Map.Entry<String, Integer> entry : columnIndexMap.entrySet()) {
      if (entry.getValue() > lastColumnNumber) {
        continue;
      }
      sb.append(entry.getKey());
      sb.append(":");
      sb.append(entry.getValue());
      sb.append(", ");
    }
    sb.append("], reorder column index is:");
    sb.append(Arrays.asList(reorderColumnIndex));
    return sb.toString();
  }
}

