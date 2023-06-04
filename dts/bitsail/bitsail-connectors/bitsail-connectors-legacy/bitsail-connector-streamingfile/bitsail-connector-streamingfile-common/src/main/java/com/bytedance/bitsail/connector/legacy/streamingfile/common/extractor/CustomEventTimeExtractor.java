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

package com.bytedance.bitsail.connector.legacy.streamingfile.common.extractor;

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.util.FieldPathUtils;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.option.FileSystemCommonOptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * @class: CustomEventTimeExtractor
 * @desc:
 **/
public class CustomEventTimeExtractor extends AbstractEventTimeExtractor {
  private static final Logger LOG = LoggerFactory.getLogger(CustomEventTimeExtractor.class);
  private transient EventTimeExtractor extractor;

  public CustomEventTimeExtractor(BitSailConfiguration jobConf) {
    super(jobConf);
  }

  @Override
  public Object parse(byte[] record) throws Exception {
    getCustomExtractor();
    return this.extractor.parse(record);
  }

  @Override
  protected long extract(Object record, long defaultTimestamp) {
    return timeToMs(this.extractor.extract(record));
  }

  @Override
  public String getField(Object record, FieldPathUtils.PathInfo pathInfo, String defaultValue) {
    try {
      return this.extractor.getField(record, pathInfo.getName(), defaultValue);
    } catch (Exception e) {
      throw new UnsupportedOperationException("Unsupported get field action.");
    }
  }

  private void getCustomExtractor() throws Exception {
    if (Objects.isNull(this.extractor)) {
      LOG.info("extractor is null, construct custom extractor");
      this.extractor = (EventTimeExtractor) Class.forName(jobConf.get(FileSystemCommonOptions.ArchiveOptions.CUSTOM_EXTRACTOR_CLASSPATH)).newInstance();
    }
  }
}
