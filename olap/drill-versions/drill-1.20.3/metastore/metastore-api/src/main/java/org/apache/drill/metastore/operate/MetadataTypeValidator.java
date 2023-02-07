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
package org.apache.drill.metastore.operate;

import org.apache.drill.metastore.exceptions.MetastoreException;
import org.apache.drill.metastore.metadata.MetadataType;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Provides list of supported metadata types for concrete Metastore component unit
 * and validates if given metadata types are supported.
 */
public interface MetadataTypeValidator {

  /**
   * @return set of supported metadata types for concrete Metastore component unit
   */
  Set<MetadataType> supportedMetadataTypes();

  /**
   * Validates if given metadata types contain at least one metadata type
   * and that all metadata types are supported.
   *
   * @param metadataTypes metadata types to be validated
   * @throws MetastoreException if no metadata types were provided
   *                            or given metadata types contain unsupported types
   */
  default void validate(Set<MetadataType> metadataTypes) {
    if (metadataTypes == null || metadataTypes.isEmpty()) {
      throw new MetastoreException("Metadata type(s) must be indicated");
    }

    Set<MetadataType> supportedMetadataTypes = supportedMetadataTypes();

    Set<MetadataType> unsupportedMetadataTypes = metadataTypes.stream()
      .filter(metadataType -> !supportedMetadataTypes.contains(metadataType))
      .collect(Collectors.toSet());

    if (!unsupportedMetadataTypes.isEmpty()) {
      throw new MetastoreException("Unsupported metadata types are detected: " + unsupportedMetadataTypes);
    }
  }
}
