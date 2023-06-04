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

import com.bytedance.bitsail.connector.legacy.larksheet.util.LarkSheetUtil;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Meta information of a sheet.
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SheetMeta implements Serializable {
  private static final long serialVersionUID = 1800860169827483423L;
  /**
   * Index of sheet.
   */
  private int index;

  /**
   * Number of effective columns in the sheet.
   */
  private int columnCount;

  /**
   * Number of rows in the sheet. The open api has the problem of line expansion and needs to filter empty lines
   */
  private int rowCount;

  /**
   * Id of sheet.
   */
  private String sheetId;

  /**
   * Title of sheet.
   */
  private String title;

  /**
   * Cell merge information of the sheet.
   */
  private Object merges;

  /**
   * Get the maximum range of the first row.
   *
   * @return Top left cell index -> top right cell index.
   */
  public String getMaxHeaderRange() {
    return String.format("A1:%s1", LarkSheetUtil.numberToSequence(columnCount));
  }
}