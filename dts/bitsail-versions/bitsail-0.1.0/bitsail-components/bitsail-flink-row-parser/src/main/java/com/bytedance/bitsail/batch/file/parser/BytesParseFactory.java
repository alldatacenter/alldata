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

package com.bytedance.bitsail.batch.file.parser;

import com.bytedance.bitsail.base.enumerate.ContentType;
import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.flink.core.parser.BytesParser;
import com.bytedance.bitsail.parser.error.ParserErrorCode;

import static com.bytedance.bitsail.common.option.ReaderOptions.BaseReaderOptions.CONTENT_TYPE;

/**
 * Created at 2018/12/03.
 */
public class BytesParseFactory {

  /**
   * generate a json/protobuf/csv bytesParser based on its contentType
   */
  public static BytesParser initBytesParser(BitSailConfiguration inputSliceConfig) throws Exception {
    ContentType contentType = ContentType.valueOf(inputSliceConfig.getNecessaryOption(
        CONTENT_TYPE, ParserErrorCode.REQUIRED_VALUE).toUpperCase());

    switch (contentType) {
      case PROTOBUF:
      case BINARY:
        return new PbBytesParser(inputSliceConfig);
      case JSON:
        return new JsonBytesParser(inputSliceConfig);
      case CSV:
        return new CsvBytesParser(inputSliceConfig);
      default:
        throw BitSailException.asBitSailException(ParserErrorCode.UNSUPPORTED_ENCODING, "unsupported parser type: " + contentType);
    }

  }

}

