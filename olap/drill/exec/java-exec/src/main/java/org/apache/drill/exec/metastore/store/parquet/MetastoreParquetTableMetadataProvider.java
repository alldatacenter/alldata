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

import org.apache.drill.exec.exception.MetadataException;
import org.apache.drill.exec.metastore.store.FileSystemMetadataProviderManager;
import org.apache.drill.exec.metastore.store.MetastoreFileTableMetadataProvider;
import org.apache.drill.exec.metastore.MetastoreMetadataProviderManager;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.store.dfs.DrillFileSystem;
import org.apache.drill.exec.store.dfs.FileSelection;
import org.apache.drill.exec.store.dfs.ReadEntryWithPath;
import org.apache.drill.exec.store.parquet.ParquetReaderConfig;
import org.apache.drill.exec.util.DrillFileSystemUtil;
import org.apache.drill.metastore.metadata.RowGroupMetadata;
import org.apache.drill.metastore.metadata.TableMetadataProvider;
import org.apache.drill.shaded.guava.com.google.common.collect.LinkedListMultimap;
import org.apache.drill.shaded.guava.com.google.common.collect.Multimap;
import org.apache.hadoop.fs.Path;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of {@link TableMetadataProvider} which uses Drill Metastore for providing
 * table metadata for parquet tables.
 */
public class MetastoreParquetTableMetadataProvider extends MetastoreFileTableMetadataProvider
    implements ParquetTableMetadataProvider {
  private final List<ReadEntryWithPath> entries;
  // stores builder to provide lazy init for fallback ParquetTableMetadataProvider
  private final ParquetTableMetadataProviderBuilder<?> fallbackBuilder;

  private Multimap<Path, RowGroupMetadata> rowGroups;
  private ParquetMetadataProvider fallback;

  private MetastoreParquetTableMetadataProvider(Builder builder) {
    super(builder);

    this.entries = builder.entries;
    this.fallbackBuilder = builder.fallback;

    ParquetMetadataProvider source = (ParquetTableMetadataProvider) builder.metadataProviderManager().getTableMetadataProvider();
    // store results into metadataProviderManager to be able to use them when creating new instances
    // for the case when source wasn't provided or it contains less row group metadata than the provider
    if (source == null || source.getRowGroupsMeta().size() < getRowGroupsMeta().size()) {
      builder.metadataProviderManager().setTableMetadataProvider(this);
    }
  }

  @Override
  public boolean isUsedMetadataCache() {
    return false;
  }

  @Override
  public Path getSelectionRoot() {
    return getTableMetadata().getLocation();
  }

  @Override
  public List<ReadEntryWithPath> getEntries() {
    return entries;
  }

  @Override
  public List<RowGroupMetadata> getRowGroupsMeta() {
    return new ArrayList<>(getRowGroupsMetadataMap().values());
  }

  @Override
  public List<Path> getLocations() {
    return new ArrayList<>(getFilesMetadataMap().keySet());
  }

  @Override
  public Multimap<Path, RowGroupMetadata> getRowGroupsMetadataMap() {
    throwIfChanged();
    if (rowGroups == null) {
      rowGroups = LinkedListMultimap.create();
      basicTablesRequests.rowGroupsMetadata(tableInfo, null, paths).stream()
          .collect(Collectors.groupingBy(RowGroupMetadata::getPath, Collectors.toList()))
          .forEach((path, rowGroupMetadata) -> rowGroups.putAll(path, rowGroupMetadata));
      if (rowGroups.isEmpty()) {
        if (fallbackToFileMetadata) {
          try {
            rowGroups = getFallbackTableMetadataProvider().getRowGroupsMetadataMap();
          } catch (IOException e) {
            throw MetadataException.of(MetadataException.MetadataExceptionType.FALLBACK_EXCEPTION, e);
          }
        } else {
          throw MetadataException.of(MetadataException.MetadataExceptionType.INCOMPLETE_METADATA);
        }
      }
    }
    return rowGroups;
  }

  @Override
  public Set<Path> getFileSet() {
    throwIfChanged();
    return getFilesMetadataMap().keySet();
  }

  private ParquetMetadataProvider getFallbackTableMetadataProvider() throws IOException {
    if (fallback == null) {
      fallback = fallbackBuilder == null ? null : fallbackBuilder.build();
    }
    return fallback;
  }

  public static class Builder extends MetastoreFileTableMetadataProvider.Builder<Builder>
      implements ParquetTableMetadataProviderBuilder<Builder> {
    private final ParquetTableMetadataProviderBuilder<?> fallback;
    private List<ReadEntryWithPath> entries;

    public Builder(MetastoreMetadataProviderManager source) {
      super(source, new ParquetTableMetadataProviderImpl.Builder(FileSystemMetadataProviderManager.init()));
      this.fallback = (ParquetTableMetadataProviderBuilder<?>) super.fallback;
    }

    @Override
    public Builder withEntries(List<ReadEntryWithPath> entries) {
      this.entries = entries;
      fallback.withEntries(entries);
      return this;
    }

    @Override
    public Builder withSelectionRoot(Path selectionRoot) {
      fallback.withSelectionRoot(selectionRoot);
      return this;
    }

    @Override
    public Builder withCacheFileRoot(Path cacheFileRoot) {
      fallback.withCacheFileRoot(cacheFileRoot);
      return this;
    }

    @Override
    public Builder withReaderConfig(ParquetReaderConfig readerConfig) {
      fallback.withReaderConfig(readerConfig);
      return this;
    }

    @Override
    public Builder withFileSystem(DrillFileSystem fs) {
      fallback.withFileSystem(fs);
      return super.withFileSystem(fs);
    }

    @Override
    public Builder withCorrectCorruptedDates(boolean autoCorrectCorruptedDates) {
      fallback.withCorrectCorruptedDates(autoCorrectCorruptedDates);
      return this;
    }

    @Override
    public Builder withSelection(FileSelection selection) {
      fallback.withSelection(selection);
      return super.withSelection(selection);
    }

    @Override
    public Builder withSchema(TupleMetadata schema) {
      fallback.withSchema(schema);
      return super.withSchema(schema);
    }

    @Override
    public Builder self() {
      return this;
    }

    @Override
    public ParquetTableMetadataProvider build() throws IOException {
      if (entries == null) {
        if (!selection().isExpandedFully()) {
          entries = DrillFileSystemUtil.listFiles(fs(), selection().getSelectionRoot(), true).stream()
              .map(fileStatus -> new ReadEntryWithPath(Path.getPathWithoutSchemeAndAuthority(fileStatus.getPath())))
              .collect(Collectors.toList());
        } else {
          entries = selection().getFiles().stream()
              .map(Path::getPathWithoutSchemeAndAuthority)
              .map(ReadEntryWithPath::new)
              .collect(Collectors.toList());
        }
      }

      paths = entries.stream()
          .map(readEntryWithPath -> readEntryWithPath.getPath().toUri().getPath())
          .collect(Collectors.toList());

      return new MetastoreParquetTableMetadataProvider(this);
    }
  }
}
