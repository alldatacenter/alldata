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

package com.bytedance.bitsail.connector.legacy.streamingfile.core.sink.schema;

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.schema.FileSystemMetaManager;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.option.FileSystemCommonOptions;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.validator.StreamingFileSystemValidator;

/**
 * Created 2020/11/17.
 */
public class FileSystemSchemaFactory {

  public static FileSystemMetaManager createSchemaManager(BitSailConfiguration jobConf) {
    String formatType = jobConf.get(FileSystemCommonOptions.DUMP_FORMAT_TYPE);
    if (StreamingFileSystemValidator.HIVE_FORMAT_TYPE_VALUE.equalsIgnoreCase(formatType)) {
      return new HiveFileSystemMetaManager(jobConf);
    }
    return new FileSystemMetaManager(jobConf);
  }
}
