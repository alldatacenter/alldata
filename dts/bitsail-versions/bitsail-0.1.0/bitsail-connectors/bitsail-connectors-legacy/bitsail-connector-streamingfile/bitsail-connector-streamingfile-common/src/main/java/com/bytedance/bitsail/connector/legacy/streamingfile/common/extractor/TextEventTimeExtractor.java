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
import com.bytedance.bitsail.common.util.JsonVisitor;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.option.FileSystemCommonOptions;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.option.FileSystemParseOptions;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.option.FileSystemSinkOptions;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.validator.StreamingFileSystemValidator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.msgpack.jackson.dataformat.MessagePackFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created 2020-02-25.
 */
public class TextEventTimeExtractor extends AbstractEventTimeExtractor {
  private static final Logger LOG = LoggerFactory.getLogger(TextEventTimeExtractor.class);

  private final String dumpType;
  private final boolean isCaseInsensitive;
  private List<FieldPathUtils.PathInfo> pathInfos;

  private transient ObjectMapper mapper;

  public TextEventTimeExtractor(BitSailConfiguration jobConf) {
    super(jobConf);
    dumpType = jobConf.get(FileSystemSinkOptions.HDFS_DUMP_TYPE);
    isCaseInsensitive = jobConf.get(FileSystemParseOptions.CASE_INSENSITIVE);
    if (isEventTime) {
      String fields = Preconditions.checkNotNull(jobConf.get(FileSystemCommonOptions.ArchiveOptions.EVENT_TIME_FIELDS));
      pathInfos = Arrays.stream(StringUtils
              .split(fields, ","))
          .map(column -> {
            if (isCaseInsensitive) {
              return StringUtils.lowerCase(column);
            } else {
              return column;
            }
          })
          .map(FieldPathUtils::getPathInfo).collect(Collectors.toList());
    }
  }

  @Override
  protected void initialize() {
    if (Objects.isNull(mapper)) {
      if (StreamingFileSystemValidator.HDFS_DUMP_TYPE_MSGPACK.equalsIgnoreCase(dumpType)) {
        this.mapper = new ObjectMapper(new MessagePackFactory());
      } else {
        this.mapper = new ObjectMapper();
      }
    }
  }

  @Override
  public JsonNode parse(byte[] record) throws Exception {
    initialize();
    return readDependOnDumpType(record);
  }

  @Override
  public String getField(Object record, FieldPathUtils.PathInfo pathInfo, String defaultValue) {
    JsonNode nestValue = JsonVisitor.getPathValue((JsonNode) record, pathInfo, isCaseInsensitive);
    if (Objects.isNull(nestValue) || nestValue.isObject() || nestValue.isArray()) {
      return defaultValue;
    }
    return nestValue.asText();
  }

  /**
   * Extract event time from record.
   * According to the origin dump service strategy, text format record such as JSON,MSGPACK
   * only search first layer within offer event time keys.
   * Message pack format will decode by messagePackMapper.
   *
   * @param record           every single record.
   * @param defaultTimestamp if not found in record,which will be return.
   * @return record's event time.
   */
  @Override
  protected long extract(Object record, long defaultTimestamp) {
    try {
      for (FieldPathUtils.PathInfo pathInfo : pathInfos) {
        JsonNode keyNode = JsonVisitor.getPathValue((JsonNode) record, pathInfo, isCaseInsensitive);
        if (Objects.nonNull(keyNode)) {
          return timeToMs(getEventTime(keyNode, defaultTimestamp));
        }
      }
    } catch (Exception e) {
      LOG.error("parse record failed.", e);
      return defaultTimestamp;
    }
    return defaultTimestamp;
  }

  private JsonNode readDependOnDumpType(byte[] record) throws IOException {
    return mapper.readTree(record);
  }

  @Override
  public List<FieldPathUtils.PathInfo> getEventTimeFields() {
    return pathInfos;
  }
}
