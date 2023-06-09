/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.data.file;

import org.apache.iceberg.ContentFile;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.DeleteFile;

public class WrapFileWithSequenceNumberHelper {

  public static ContentFileWithSequence<?> wrap(ContentFile<?> contentFile, long sequenceNumber) {
    if (contentFile instanceof DataFile) {
      if (contentFile instanceof DataFileWithSequence) {
        return (DataFileWithSequence) contentFile;
      } else {
        return new DataFileWithSequence((DataFile) contentFile, sequenceNumber);
      }
    } else if (contentFile instanceof DeleteFile) {
      if (contentFile instanceof DeleteFileWithSequence) {
        return (DeleteFileWithSequence) contentFile;
      } else {
        return new DeleteFileWithSequence((DeleteFile) contentFile, sequenceNumber);
      }
    } else {
      throw new IllegalArgumentException("Only support DataFile or DeleteFile, can not support: " +
          contentFile.getClass().getSimpleName());
    }
  }
}
