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
package org.apache.drill.exec.store.iceberg.format;

import org.apache.commons.lang3.StringUtils;
import org.apache.drill.exec.store.dfs.FileSelection;
import org.apache.drill.exec.store.dfs.FormatLocationTransformer;
import org.apache.iceberg.MetadataTableType;

import java.util.function.Function;

public class IcebergFormatLocationTransformer implements FormatLocationTransformer {
  public static final FormatLocationTransformer INSTANCE = new IcebergFormatLocationTransformer();

  public static final String METADATA_SEPARATOR = "#";

  @Override
  public boolean canTransform(String location) {
    return getMetadataTableType(location) != null;
  }

  private MetadataTableType getMetadataTableType(String location) {
    String metadataType = StringUtils.substringAfterLast(location, METADATA_SEPARATOR);
    return StringUtils.isNotEmpty(metadataType)
      ? MetadataTableType.from(metadataType)
      : null;
  }

  @Override
  public FileSelection transform(String location, Function<String, FileSelection> selectionFactory) {
    MetadataTableType metadataTableType = getMetadataTableType(location);
    location = StringUtils.substringBeforeLast(location, METADATA_SEPARATOR);
    FileSelection fileSelection = selectionFactory.apply(location);
    return fileSelection != null
      ? new IcebergMetadataFileSelection(fileSelection, metadataTableType)
      : null;
  }
}
