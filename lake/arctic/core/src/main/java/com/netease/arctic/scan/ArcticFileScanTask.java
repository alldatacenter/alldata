/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.scan;

import com.netease.arctic.data.DataFileType;
import com.netease.arctic.data.PrimaryKeyedFile;
import org.apache.iceberg.FileScanTask;

/**
 * A scan task over a single data file with some positional delete files
 */
public interface ArcticFileScanTask extends FileScanTask {

  /**
   * The {@link PrimaryKeyedFile} to scan.
   * @return the file to scan
   */
  PrimaryKeyedFile file();

  /**
   * Returns the type of file to scan
   */
  default DataFileType fileType() {
    return file().type();
  }
}
