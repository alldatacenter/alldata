/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.store.dfs;

import org.apache.drill.exec.planner.logical.DrillTable;
import org.apache.drill.exec.store.SchemaConfig;
import org.apache.hadoop.fs.FileStatus;

import java.io.IOException;

public abstract class FormatMatcher {

  public static final int MEDIUM_PRIORITY = 5;
  public static final int HIGH_PRIORITY = 7;

  public abstract boolean supportDirectoryReads();
  public abstract DrillTable isReadable(DrillFileSystem fs,
      FileSelection selection, FileSystemPlugin fsPlugin,
      String storageEngineName, SchemaConfig schemaConfig) throws IOException;
  public abstract boolean isFileReadable(DrillFileSystem fs, FileStatus status) throws IOException;
  public abstract FormatPlugin getFormatPlugin();

  /**
   * Priority of specific format matcher to be applied.
   * Can be used for the case when several formats can match the format,
   * but matchers with some more specific requirements should be applied at first.
   *
   * @return priority of {@link this} format matcher.
   */
  public int priority() {
    return MEDIUM_PRIORITY;
  }

  /**
   * Returns {@link FormatLocationTransformer} instance relevant for {@link this} format matcher.
   *
   * @return {@link FormatLocationTransformer} instance
   */
  public FormatLocationTransformer getFormatLocationTransformer() {
    return null;
  }
}
