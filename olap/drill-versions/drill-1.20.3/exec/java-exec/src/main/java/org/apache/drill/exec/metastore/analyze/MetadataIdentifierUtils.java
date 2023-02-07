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
package org.apache.drill.exec.metastore.analyze;

import org.apache.drill.exec.store.ColumnExplorer;
import org.apache.drill.metastore.metadata.MetadataInfo;
import org.apache.hadoop.fs.Path;

import java.util.ArrayList;
import java.util.List;

public class MetadataIdentifierUtils {
  private static final String METADATA_IDENTIFIER_SEPARATOR = "/";

  /**
   * Returns string representation of metadata identifier using specified metadata identifier values.
   *
   * @param values metadata identifier values
   * @return string representation of metadata identifier
   */
  public static String getMetadataIdentifierKey(List<String> values) {
    return String.join(METADATA_IDENTIFIER_SEPARATOR, values);
  }

  /**
   * Checks whether the specified metadata identifier is a parent for another specified metadata identifier.
   *
   * @param parent parent metadata identifier
   * @param child  child metadata identifier
   * @return {@code true} if specified metadata identifier is a parent for another specified metadata identifier
   */
  public static boolean isMetadataKeyParent(String parent, String child) {
    return child.startsWith(parent + METADATA_IDENTIFIER_SEPARATOR) || parent.equals(MetadataInfo.DEFAULT_SEGMENT_KEY);
  }

  /**
   * Returns file metadata identifier.
   *
   * @param partitionValues partition values
   * @param path            file path
   * @return file metadata identifier
   */
  public static String getFileMetadataIdentifier(List<String> partitionValues, Path path) {
    List<String> identifierValues = new ArrayList<>(partitionValues);
    identifierValues.add(ColumnExplorer.ImplicitFileColumns.FILENAME.getValue(path));
    return getMetadataIdentifierKey(identifierValues);
  }

  /**
   * Returns row group metadata identifier.
   *
   * @param partitionValues partition values
   * @param path            file path
   * @param index           row group index
   * @return row group metadata identifier
   */
  public static String getRowGroupMetadataIdentifier(List<String> partitionValues, Path path, int index) {
    List<String> identifierValues = new ArrayList<>(partitionValues);
    identifierValues.add(ColumnExplorer.ImplicitFileColumns.FILENAME.getValue(path));
    identifierValues.add(Integer.toString(index));
    return getMetadataIdentifierKey(identifierValues);
  }

  /**
   * Returns array with metadata identifier values obtained from specified metadata identifier string.
   *
   * @param metadataIdentifier metadata identifier
   * @return array with metadata identifier values
   */
  public static String[] getValuesFromMetadataIdentifier(String metadataIdentifier) {
    return metadataIdentifier.split(METADATA_IDENTIFIER_SEPARATOR);
  }
}
