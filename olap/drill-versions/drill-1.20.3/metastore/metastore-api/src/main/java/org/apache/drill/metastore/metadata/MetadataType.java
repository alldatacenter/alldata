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
package org.apache.drill.metastore.metadata;

import java.util.stream.Stream;

/**
 * Enum with possible types of metadata.
 */
public enum MetadataType {

  /**
   * Metadata type which helps to indicate that there is no overflow of metadata.
   */
  NONE(0),

  /**
   * Table level metadata type.
   */
  TABLE(1),

  /**
   * Segment level metadata type. It corresponds to the metadata
   * within specific directory for FS tables, or may correspond to partition for hive tables.
   */
  SEGMENT(2),

  /**
   * Drill partition level metadata type. It corresponds to parts of table data which has the same
   * values within specific column, i.e. partitions discovered by Drill.
   */
  PARTITION(3),

  /**
   * File level metadata type.
   */
  FILE(4),

  /**
   * Row group level metadata type. Used for parquet tables.
   */
  ROW_GROUP(5),

  /**
   * Metadata that can be applicable to any type.
   */
  ALL(Integer.MAX_VALUE),

  /**
   * Metadata type which belongs to views.
   */
  VIEW(-1);

  /**
   * Level of this metadata type compared to other metadata types.
   * Metadata type with a greater level includes metadata types with the lower one.
   * <p/>
   * For example: {@link #ROW_GROUP} metadata type includes {@link #FILE} metadata type because
   * {@link #ROW_GROUP} metadata level is 5 and {@link #FILE} is 4.
   */
  private final int metadataLevel;

  MetadataType(int metadataLevel) {
    this.metadataLevel = metadataLevel;
  }

  /**
   * Checks whether this {@link MetadataType} includes the specified one.
   * For example, {@link #SEGMENT} metadata type includes {@link #TABLE} metadata type.
   *
   * @param metadataType metadata type to check
   * @return {@code true} if this {@link MetadataType} includes the specified one, {@code false} otherwise.
   */
  public boolean includes(MetadataType metadataType) {
    return metadataLevel >= metadataType.metadataLevel;
  }

  /**
   * Converts metadata type string representation into {@link MetadataType} instance.
   *
   * @param value metadata
   * @return metadata type instance, null otherwise
   */
  public static MetadataType fromValue(String value) {
    return Stream.of(values())
      .filter(type -> type.name().equalsIgnoreCase(value))
      .findAny()
      .orElse(null);
  }
}
