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

package com.bytedance.bitsail.connector.legacy.larksheet.source;

import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.column.Column;
import com.bytedance.bitsail.common.column.StringColumn;
import com.bytedance.bitsail.common.model.ColumnInfo;
import com.bytedance.bitsail.connector.legacy.larksheet.api.SheetConfig;
import com.bytedance.bitsail.connector.legacy.larksheet.api.TokenHolder;
import com.bytedance.bitsail.connector.legacy.larksheet.error.LarkSheetFormatErrorCode;
import com.bytedance.bitsail.connector.legacy.larksheet.meta.SheetHeader;
import com.bytedance.bitsail.connector.legacy.larksheet.meta.SheetInfo;
import com.bytedance.bitsail.connector.legacy.larksheet.meta.SheetMeta;
import com.bytedance.bitsail.connector.legacy.larksheet.option.LarkSheetReaderOptions;
import com.bytedance.bitsail.connector.legacy.larksheet.util.LarkSheetUtil;
import com.bytedance.bitsail.flink.core.legacy.connector.InputFormatPlugin;
import com.bytedance.bitsail.flink.core.typeinfo.PrimitiveColumnTypeInfo;

import com.google.common.collect.Maps;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.flink.api.common.io.DefaultInputSplitAssigner;
import org.apache.flink.api.common.io.statistics.BaseStatistics;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.ResultTypeQueryable;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.core.io.InputSplit;
import org.apache.flink.core.io.InputSplitAssigner;
import org.apache.flink.types.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class LarkSheetInputFormat extends InputFormatPlugin<Row, InputSplit>
    implements ResultTypeQueryable<Row> {
  private static final Logger LOG = LoggerFactory.getLogger(LarkSheetInputFormat.class);

  /**
   * Sheet header info.
   */
  private SheetHeader sheetHeader;

  /**
   * Number of rows queried in batch.
   */
  private int batchSize;

  /**
   * Row type info
   */
  private RowTypeInfo rowTypeInfo;

  /**
   * A record batch get from API. Empty rows are filtered.
   */
  private List<List<Object>> recordQueue;

  /**
   * Current record to handle.
   */
  private List<Object> curRecord;

  /**
   * Sheet configurations for request.
   */
  private SheetConfig larkSheetConfig;

  /**
   * Number of lines to skip for each sheet.
   */
  private List<Integer>skipNums;

  /**
   * Parameters used when calling single range api.
   */
  private Map<String, String> rangeParamMap;

  /**
   * the list of sheet information
   */
  private List<SheetInfo> sheetInfoList;

  /**
   * Some features when transforming data.
   */
  public enum Feature implements Serializable {
    FORMULA_SUPPORT("dateTimeRenderOption", "FormattedString");

    String key;
    String value;

    Feature(String defaultKey, String defaultValue) {
      this.key = defaultKey;
      this.value = defaultValue;
    }
  }

  /**
   * Initialize the input format on the client.
   */
  @Override
  public void initPlugin() throws Exception {
    // **** STEP0: Initialize open api related configuration ****
    this.larkSheetConfig = new SheetConfig().configure(inputSliceConfig);

    // **** STEP1: Initialize token ****
    TokenHolder.init(SheetConfig.PRE_DEFINED_SHEET_TOKEN);

    // **** STEP2: Parse sheet url ****
    String sheetUrlList = inputSliceConfig.getNecessaryOption(LarkSheetReaderOptions.SHEET_URL,
        LarkSheetFormatErrorCode.REQUIRED_VALUE);
    List<String> sheetUrls = Arrays.asList(sheetUrlList.split(","));

    // **** STEP3: Initialize sheet info ****
    this.sheetInfoList = LarkSheetUtil.resolveSheetUrls(sheetUrls);

    // **** STEP4: Get and verify sheet header ****
    List<ColumnInfo> readerColumns = inputSliceConfig.getNecessaryOption(LarkSheetReaderOptions.COLUMNS,
        LarkSheetFormatErrorCode.REQUIRED_VALUE);
    this.sheetHeader = LarkSheetUtil.getSheetHeader(this.sheetInfoList, readerColumns);

    // **** STEP5: Other initialization ****
    this.rowTypeInfo = buildRowTypeInfo(readerColumns);
    this.batchSize = inputSliceConfig.get(LarkSheetReaderOptions.BATCH_SIZE);
    this.skipNums = inputSliceConfig.getUnNecessaryOption(LarkSheetReaderOptions.SKIP_NUMS, new ArrayList<>());
    fillLarkParams(inputSliceConfig.get(LarkSheetReaderOptions.LARK_PROPERTIES));
  }

  /**
   * Create splits of size `batchSize`.
   */
  @Override
  public InputSplit[] createSplits(int minNumSplits) throws IOException {
    // Generate sharded array
    InputSplit[] inputSplits = calculateLarkSheetInputSplits(this.sheetInfoList, this.batchSize, this.skipNums);
    setTotalSplitsNum(inputSplits.length);
    LOG.info("LarkSheet Input splits size: {}", inputSplits.length);
    return inputSplits;
  }

  /**
   * Make sure token is generated before open().
   */
  @Override
  public void configure(Configuration parameters) {
    super.configure(parameters);
    this.larkSheetConfig.configure(inputSliceConfig);
    TokenHolder.init(SheetConfig.PRE_DEFINED_SHEET_TOKEN);
  }

  /**
   * Compute range based on split id.<br/>
   * &nbsp;&nbsp;&nbsp;&nbsp; - left_border: 2 + splitNumber * batchSize<br/>
   * &nbsp;&nbsp;&nbsp;&nbsp; - right_border: left_border + batchSize - 1<br/>
   * For example, when batchSize=3, and the sheet has 9 rows (1,2,3,4,5,6,7,8,9).
   * Note that the first row is sheet header, so it will not be counted in the splits.
   * In this case, 3 splits are generated:
   * <br/>&nbsp;&nbsp;&nbsp;&nbsp; split1: [2,3,4]
   * <br/>&nbsp;&nbsp;&nbsp;&nbsp; split2: [5,6,7]
   * <br/>&nbsp;&nbsp;&nbsp;&nbsp; split3: [8,9]<br/>
   */
  @Override
  public void open(InputSplit split) throws IOException {
    LarkSheetInputSplit inputSplit = ((LarkSheetInputSplit) split);
    // According to the current shard, obtain the sheetId, sheetToken, sheetMeta,
    // and row number range corresponding to the shard.
    SheetMeta sheetMeta = inputSplit.getSheetMeta();
    String sheetId = inputSplit.getSheetId();
    String sheetToken = inputSplit.getSheetToken();

    int startRowNumber = inputSplit.getStartRowNumber();
    int endRowNumber = inputSplit.getEndRowNumber();

    List<List<Object>> rows;
    if (startRowNumber > sheetMeta.getRowCount()) {
      LOG.warn("It may indicates there is some wrong with split calculating, please check code!");
      rows = Collections.emptyList();
    } else {
      String range = genSheetRange(startRowNumber, endRowNumber, sheetMeta);
      rows = LarkSheetUtil.getRange(sheetToken, sheetId, range, this.rangeParamMap);
    }

    if (recordQueue == null) {
      this.recordQueue = new LinkedList<>();
    } else {
      recordQueue.clear();
    }

    // filter those empty rows.
    for (List<Object> row : rows) {
      List<Object> reorderRow = reorder(row);
      if (reorderRow.stream().anyMatch(Objects::nonNull)) {
        recordQueue.add(reorderRow);
      }
    }
  }

  /**
   * Build a flink row.
   */
  @Override
  public Row buildRow(Row reuse, String mandatoryEncoding) throws BitSailException {
    if (curRecord == null) {
      throw new BitSailException(LarkSheetFormatErrorCode.INVALID_ROW, "row is null");
    }

    for (int i = 0; i < this.rowTypeInfo.getArity(); i++) {
      Object fieldVal = curRecord.get(i);
      Column column = new StringColumn(Objects.isNull(fieldVal) ? null : String.valueOf(fieldVal));
      reuse.setField(i, column);
    }
    return reuse;
  }

  /**
   * Check if all data in the current split are read.
   */
  @Override
  public boolean isSplitEnd() {
    // Split is over if recordQueue is empty.
    hasNext = CollectionUtils.isNotEmpty(recordQueue);
    if (hasNext) {
      curRecord = recordQueue.remove(0);
    } else {
      curRecord = null;
    }
    return !hasNext;
  }

  @Override
  public String getType() {
    return "LarkSheet";
  }

  @Override
  public BaseStatistics getStatistics(BaseStatistics cachedStatistics) throws IOException {
    return new BaseStatistics() {
      @Override
      public long getTotalInputSize() {
        return SIZE_UNKNOWN;
      }

      @Override
      public long getNumberOfRecords() {
        return sheetInfoList.stream().mapToLong(sheetInfo -> sheetInfo.getSheetMeta().getRowCount()).sum();
      }

      @Override
      public float getAverageRecordWidth() {
        return AVG_RECORD_BYTES_UNKNOWN;
      }
    };
  }

  @Override
  public InputSplitAssigner getInputSplitAssigner(InputSplit[] inputSplits) {
    return new DefaultInputSplitAssigner(inputSplits);
  }

  @Override
  public void close() throws IOException {
    if (this.recordQueue != null) {
      this.recordQueue.clear();
    }
    this.curRecord = null;
    LOG.info("finished process one split!");
  }

  @Override
  public TypeInformation<Row> getProducedType() {
    return rowTypeInfo;
  }

  /**
   * Process all columns as string type.
   *
   * @param readerColumns Columns defined in job configuration.
   */
  private RowTypeInfo buildRowTypeInfo(List<ColumnInfo> readerColumns) {
    int size = readerColumns.size();
    PrimitiveColumnTypeInfo[] typeInfos = new PrimitiveColumnTypeInfo[size];
    String[] names = new String[size];

    for (int i = 0; i < size; i++) {
      typeInfos[i] = PrimitiveColumnTypeInfo.STRING_COLUMN_TYPE_INFO;
      names[i] = readerColumns.get(i).getName();
    }

    RowTypeInfo rowTypeInfo = new RowTypeInfo(typeInfos, names);
    LOG.info("Row type info: {}", this.rowTypeInfo);
    return rowTypeInfo;
  }

  /**
   * Parse features.
   * @param features Features defined in job configuration.
   */
  private void fillLarkParams(Map<String, String> features) {
    if (MapUtils.isEmpty(features)) {
      this.rangeParamMap = Maps.newHashMap();
    } else {
      this.rangeParamMap = features;
    }
    for (LarkSheetInputFormat.Feature feature : LarkSheetInputFormat.Feature.values()) {
      this.rangeParamMap.putIfAbsent(feature.key, feature.value);
    }
  }

  /**
   * Generate shard array
   *
   * @param sheetInfoList A list sheet metadata.
   * @param batchSize Size of a split.
   * @param skipNums Number of rows to skip.
   * @return An array of splits.
   */
  private static InputSplit[] calculateLarkSheetInputSplits(List<SheetInfo> sheetInfoList, int batchSize,
                                                            List<Integer> skipNums) {
    int splitCount = 0;
    if (skipNums.isEmpty()) {
      splitCount = sheetInfoList.stream()
          .mapToInt(sheetInfo -> (int) Math.ceil((double) (sheetInfo.getSheetMeta().getRowCount() - 1) / (double) batchSize))
          .sum();
    } else {
      for (int i = 0; i < sheetInfoList.size(); i++) {
        SheetInfo sheetInfo = sheetInfoList.get(i);
        int skipNum = 0;
        if (skipNums.size() > i) {
          skipNum = Math.max(skipNums.get(i), 0);
        }
        splitCount += (int) Math.ceil((double) Math.max(sheetInfo.getSheetMeta().getRowCount() - 1 - skipNum, 0) / (double) batchSize);
      }
    }
    InputSplit[] larkSheetInputSplits = new LarkSheetInputSplit[splitCount];

    splitCount = 0;
    //generate inputSplits array
    for (int i = 0; i < sheetInfoList.size(); i++) {
      SheetInfo sheetInfo = sheetInfoList.get(i);

      //Get the relevant information of the current sheet Meta and the number of shards
      SheetMeta sheetMeta = sheetInfo.getSheetMeta();
      String sheetToken = sheetInfo.getSheetToken();
      int curCount = 0;
      int skipNum = 0;
      if (skipNums.isEmpty()) {
        curCount = (int) Math.ceil((double) (sheetMeta.getRowCount() - 1) / (double) batchSize);
      } else {
        if (skipNums.size() > i) {
          skipNum = Math.max(skipNums.get(i), 0);
        }
        curCount = (int) Math.ceil((double) Math.max(sheetInfo.getSheetMeta().getRowCount() - 1 - skipNum, 0) / (double) batchSize);
      }

      //Generate the shard of the current sheet Meta
      for (int j = 0; j < curCount; j++) {
        int startRowNumber = 2 + j * batchSize + skipNum;
        int endRowNumber = startRowNumber + batchSize - 1;

        //Initialize shard information
        larkSheetInputSplits[splitCount] = new LarkSheetInputSplit(sheetMeta, sheetToken, sheetMeta.getSheetId(), startRowNumber, endRowNumber);
        splitCount += 1;
      }
    }
    return larkSheetInputSplits;
  }

  /**
   * Compute the range of rows to extract.
   *
   * @param startRowNumber Start row number
   * @param endRowNumber End row number.
   * @return A range expression.
   */
  private String genSheetRange(int startRowNumber, int endRowNumber, SheetMeta sheetMeta) {
    return this.sheetHeader.getStartColumn() +
        startRowNumber +
        ":" +
        this.sheetHeader.getEndColumn() +
        Math.min(endRowNumber, sheetMeta.getRowCount());
  }

  /**
   * Reorder rows extracted from open api.
   * @param originRow Rows from open api.
   * @return A list of ordered rows.
   */
  private List<Object> reorder(List<Object> originRow) {
    Integer[] reorderColumnIndex = this.sheetHeader.getReorderColumnIndex();
    List<Object> row = new ArrayList<>(reorderColumnIndex.length);
    for (Integer columnIndex : reorderColumnIndex) {
      row.add(originRow.get(columnIndex));
    }
    return row;
  }

}
