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
package org.apache.drill.exec.store.parquet.metadata;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.hadoop.fs.Path;

import java.util.List;

public class ParquetTableMetadataDirs {

  @JsonProperty
  List<Path> directories;

  public ParquetTableMetadataDirs() {
    // default constructor needed for deserialization
  }

  public ParquetTableMetadataDirs(List<Path> directories) {
    this.directories = directories;
  }

  @JsonIgnore
  public List<Path> getDirectories() {
    return directories;
  }

  /** If directories list contains relative paths, update it to absolute ones
   * @param baseDir base parent directory
   */
  @JsonIgnore public void updateRelativePaths(String baseDir) {
    this.directories = MetadataPathUtils.convertToAbsolutePaths(directories, baseDir);
  }
}
