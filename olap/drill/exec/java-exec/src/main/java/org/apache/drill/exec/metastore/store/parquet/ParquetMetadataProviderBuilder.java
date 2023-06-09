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
package org.apache.drill.exec.metastore.store.parquet;

import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.store.dfs.FileSelection;
import org.apache.drill.exec.store.dfs.ReadEntryWithPath;
import org.apache.drill.exec.store.parquet.ParquetReaderConfig;
import org.apache.drill.metastore.metadata.TableMetadataProviderBuilder;
import org.apache.hadoop.fs.Path;

import java.io.IOException;
import java.util.List;

/**
 * Base interface for builders of {@link ParquetMetadataProvider}.
 */
public interface ParquetMetadataProviderBuilder<T extends ParquetMetadataProviderBuilder<T>>
    extends TableMetadataProviderBuilder {

  @Override
  T withSchema(TupleMetadata schema);

  T withEntries(List<ReadEntryWithPath> entries);

  T withSelectionRoot(Path selectionRoot);

  T withReaderConfig(ParquetReaderConfig readerConfig);

  T withSelection(FileSelection selection);

  @Override
  ParquetMetadataProvider build() throws IOException;
}
