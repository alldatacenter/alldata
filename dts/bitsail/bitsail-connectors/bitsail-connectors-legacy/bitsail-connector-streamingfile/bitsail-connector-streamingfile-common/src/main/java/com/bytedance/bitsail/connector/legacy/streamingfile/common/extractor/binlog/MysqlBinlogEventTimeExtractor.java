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

package com.bytedance.bitsail.connector.legacy.streamingfile.common.extractor.binlog;

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.extractor.AbstractEventTimeExtractor;

import com.alibaba.otter.canal.protocol.CanalEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created 2020-02-24.
 */
public class MysqlBinlogEventTimeExtractor extends AbstractEventTimeExtractor {
  private static final Logger LOG = LoggerFactory.getLogger(MysqlBinlogEventTimeExtractor.class);

  public MysqlBinlogEventTimeExtractor(BitSailConfiguration jobConf) {
    super(jobConf);
  }

  @Override
  public CanalEntry.Entry parse(byte[] record) throws Exception {
    return CanalEntry.Entry.parseFrom(record);
  }

  @Override
  protected long extract(Object record, long defaultTimestamp) throws Exception {
    try {
      return ((CanalEntry.Entry) record).getHeader().getExecuteTime();
    } catch (Exception e) {
      LOG.error("failed to extract event time from record.", e);
      return defaultTimestamp;
    }
  }

  @Override
  public boolean isBinlog() {
    return true;
  }
}
