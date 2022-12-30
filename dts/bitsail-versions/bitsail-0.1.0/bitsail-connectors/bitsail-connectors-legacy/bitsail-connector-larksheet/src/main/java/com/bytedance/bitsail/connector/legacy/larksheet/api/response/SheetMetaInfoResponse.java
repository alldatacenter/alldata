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

package com.bytedance.bitsail.connector.legacy.larksheet.api.response;

import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.connector.legacy.larksheet.error.LarkSheetFormatErrorCode;
import com.bytedance.bitsail.connector.legacy.larksheet.meta.SheetMeta;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

@lombok.Data
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
public class SheetMetaInfoResponse extends OpenApiBaseResponse {

  private Data data;

  @lombok.Data
  @AllArgsConstructor
  @NoArgsConstructor
  public static class Data {

    /**
     * Identifier of sheet.
     */
    private String spreadsheetToken;

    /**
     * Properties of sheet.
     */
    private Properties properties;

    /**
     * Meta data of sheet.
     */
    private List<SheetMeta> sheets;

  }

  @lombok.Data
  @AllArgsConstructor
  @NoArgsConstructor
  public static class Properties {
    /**
     * Sheet title
     */
    private String title;

    /**
     * User id of sheet owner
     */
    private Long ownerUser;

    /**
     * Number of sheets included
     */
    private int sheetCount;

    /**
     * Revision
     */
    private int revision;

  }

  /**
   * Get meta data of target sheet based on sheet id.
   *
   * @param sheetId Id of target sheet. If `sheetId` is null, get meta from the first sheet.
   * @return Meta data.
   */
  public SheetMeta getSheet(String sheetId) {
    SheetMeta goalSheet = null;
    boolean useDefault = StringUtils.isBlank(sheetId);

    for (SheetMeta sheetMeta : data.sheets) {
      if (useDefault && sheetMeta.getIndex() == 0) {
        goalSheet = sheetMeta;
        break;
      }

      if (StringUtils.equals(sheetId, sheetMeta.getSheetId())) {
        goalSheet = sheetMeta;
        break;
      }
    }
    if (goalSheet == null) {
      throw new BitSailException(LarkSheetFormatErrorCode.SHEET_NOT_FOUND,
          String.format("cannot find sheet, sheet_id:[%s]", sheetId));
    }
    return goalSheet;
  }

}

