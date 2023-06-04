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

package com.bytedance.bitsail.connector.legacy.streamingfile.common.tools;

import lombok.Getter;

public class FileNameUtils {

  public static String newFileName(int version,
                                   long jobId,
                                   int taskNumber,
                                   long checkpointId,
                                   long timestamp,
                                   String compressionExtension) {
    if (version == SupportedVersion.version1.getVersionNum()) {
      String fileName = "task_dorado" +
          "_" +
          jobId +
          "_" +
          taskNumber +
          "_" +
          checkpointId +
          "." +
          timestamp;
      if (compressionExtension != null) {
        fileName += compressionExtension;
      }
      return fileName;
    }
    throw new IllegalArgumentException(String.format("Unsupported version id %s", version));
  }

  public enum SupportedVersion {
    version1(1);

    @Getter
    int versionNum;

    SupportedVersion(int i) {
      this.versionNum = i;
    }

    public static SupportedVersion getVersion(int version) {
      for (SupportedVersion supportedVersion : SupportedVersion.values()) {
        if (supportedVersion.getVersionNum() == version) {
          return supportedVersion;
        }
      }
      return version1;
    }

  }
}
