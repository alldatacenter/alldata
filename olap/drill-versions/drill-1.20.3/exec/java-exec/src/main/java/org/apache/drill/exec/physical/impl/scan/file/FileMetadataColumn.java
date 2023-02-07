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
package org.apache.drill.exec.physical.impl.scan.file;

import org.apache.drill.exec.physical.impl.scan.project.VectorSource;

/**
 * Represents projection column which resolved to a file metadata
 * (AKA "implicit") column such as "filename", "fqn", etc. These
 * columns are "synthetic" added by the scan framework itself
 * rather than "organic" coming from the scanned table.
 */
public class FileMetadataColumn extends MetadataColumn {

  private final FileMetadataColumnDefn defn;

  /**
   * Constructor for resolved column. The column is resolved at the file
   * level once we identify the file to which the column is bound.
   *
   * @param name name of the column as given in the projection list
   * @param defn definition of the metadata column
   * @param fileInfo description of the file used in this scan. Used to
   * populate the value of this column on a per-file basis
   * @param source handle to the a logical batch that holds the vectors
   * to be populated in each row batch
   * @param sourceIndex the location of the vector to populate
   */
  public FileMetadataColumn(String name, FileMetadataColumnDefn defn,
      FileMetadata fileInfo, VectorSource source, int sourceIndex) {
    super(name, defn.dataType(), fileInfo.getImplicitColumnValue(defn.defn), source, sourceIndex);
    this.defn = defn;
  }

  /**
   * Constructor for unresolved column. A column is unresolved at the scan
   * level: we know that this is a file metadata column, but we don't yet
   * know the file to which to bind the column
   *
   * @param name name of the column as given in the projection list
   * @param defn definition of the metadata column
   */

  public FileMetadataColumn(String name, FileMetadataColumnDefn defn) {
    super(name, defn.dataType(), null, null, 0);
    this.defn = defn;
  }

  public FileMetadataColumnDefn defn() { return defn; }

  @Override
  public MetadataColumn resolve(FileMetadata fileInfo, VectorSource source, int sourceIndex) {
    return new FileMetadataColumn(name(), defn, fileInfo, source, sourceIndex);
  }
}
