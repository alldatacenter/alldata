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
 *
 * Original Files: apache/flink(https://github.com/apache/flink)
 * Copyright: Copyright 2014-2022 The Apache Software Foundation
 * SPDX-License-Identifier: Apache License 2.0
 *
 * This file may have been modified by ByteDance Ltd. and/or its affiliates.
 */

package com.bytedance.bitsail.connector.hadoop.source.format;

import com.bytedance.bitsail.base.enumerate.ContentType;
import com.bytedance.bitsail.batch.file.parser.BytesParseFactory;
import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.model.ColumnInfo;
import com.bytedance.bitsail.common.type.BitSailTypeInfoConverter;
import com.bytedance.bitsail.connector.hadoop.common.TextInputFormatErrorCode;
import com.bytedance.bitsail.connector.hadoop.option.HadoopReaderOptions;
import com.bytedance.bitsail.flink.core.parser.BytesParser;
import com.bytedance.bitsail.flink.core.typeutils.ColumnFlinkTypeInfoUtil;

import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.ResultTypeQueryable;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.core.fs.FileInputSplit;
import org.apache.flink.types.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import static com.bytedance.bitsail.common.option.ReaderOptions.BaseReaderOptions.CONTENT_TYPE;

/**
 * @desc:
 */
public class TextInputFormat extends DelimitedFileInputFormatPlugin implements
    ResultTypeQueryable<Row> {

  private static final Logger LOG = LoggerFactory.getLogger(TextInputFormat.class);

  private static final long serialVersionUID = 1L;
  /**
   * Code of \r, used to remove \r from a line when the line ends with \r\n.
   */
  private static final byte CARRIAGE_RETURN = (byte) '\r';
  /**
   * Code of \n, used to identify if \n is used as delimiter.
   */
  private static final byte NEW_LINE = (byte) '\n';
  private RowTypeInfo rowTypeInfo;
  private ContentType contentType;
  private BytesParser bytesParser;
  /**
   * The name of the charset to use for decoding.
   */
  private String charsetName = "UTF-8";

  // --------------------------------------------------------------------------------------------

  public String getCharsetName() {
    return charsetName;
  }

  public void setCharsetName(String charsetName) {
    if (charsetName == null) {
      throw new IllegalArgumentException("Charset must not be null.");
    }

    this.charsetName = charsetName;
  }

  // --------------------------------------------------------------------------------------------

  @Override
  public void configure(Configuration parameters) {
    super.configure(parameters);

    if (charsetName == null || !Charset.isSupported(charsetName)) {
      throw new RuntimeException("Unsupported charset: " + charsetName);
    }
  }

  @Override
  public String toString() {
    return "TextInputFormat (" + Arrays.toString(getFilePaths()) + ") - " + this.charsetName;
  }

  @Override
  public void initPlugin() throws Exception {
    // Necessary values
    String[] paths = inputSliceConfig.getNecessaryOption(HadoopReaderOptions.PATH_LIST, TextInputFormatErrorCode.REQUIRED_VALUE).split(",");
    setFilePaths(paths);

    this.contentType = ContentType.valueOf(inputSliceConfig.getNecessaryOption(CONTENT_TYPE, TextInputFormatErrorCode.REQUIRED_VALUE).toUpperCase());
    this.bytesParser = BytesParseFactory.initBytesParser(inputSliceConfig);

    List<ColumnInfo> columnInfos = inputSliceConfig
        .getNecessaryOption(HadoopReaderOptions.COLUMNS, TextInputFormatErrorCode.REQUIRED_VALUE);

    this.rowTypeInfo = ColumnFlinkTypeInfoUtil.getRowTypeInformation(new BitSailTypeInfoConverter(), columnInfos);

    LOG.info("Row type info: " + rowTypeInfo);
  }

  @Override
  public String getType() {
    return "Text";
  }

  @Override
  public void open(FileInputSplit split) throws IOException {
    super.open(split);
  }

  @Override
  public boolean supportsMultiPaths() {
    return true;
  }

  @Override
  public Row buildRow(Row reuse, String mandatoryEncoding) throws BitSailException {
    //Check if \n is used as delimiter and the end of this line is a \r, then remove \r from the line
    if (this.getDelimiter() != null && this.getDelimiter().length == 1
        && this.getDelimiter()[0] == NEW_LINE && currOffset + currLen >= 1
        && currBuffer[currOffset + currLen - 1] == CARRIAGE_RETURN) {
      currLen -= 1;
    }

    try {
      reuse = bytesParser.parse(reuse, currBuffer, currOffset, currLen, charsetName, rowTypeInfo);
    } catch (Exception e) {
      LOG.error("Parse one line error!", e);
      throw BitSailException.asBitSailException(TextInputFormatErrorCode.ILLEGAL_JSON, e);
    }
    return reuse;
  }

  @Override
  public TypeInformation<Row> getProducedType() {
    return rowTypeInfo;
  }

  public static class TextInputFormatBuilder {
    private final TextInputFormat format;

    public TextInputFormatBuilder() {
      format = new TextInputFormat();
    }

    public TextInputFormat initFromConf(BitSailConfiguration commonConf, BitSailConfiguration inputConf) throws Exception {
      format.initFromConf(commonConf, inputConf);
      return format;
    }
  }
}
