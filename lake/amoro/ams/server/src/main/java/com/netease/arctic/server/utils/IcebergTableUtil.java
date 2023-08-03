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

package com.netease.arctic.server.utils;

import com.netease.arctic.IcebergFileEntry;
import com.netease.arctic.ams.api.CatalogMeta;
import com.netease.arctic.ams.api.TableFormat;
import com.netease.arctic.ams.api.TableMeta;
import com.netease.arctic.ams.api.properties.MetaTableProperties;
import com.netease.arctic.scan.TableEntriesScan;
import com.netease.arctic.server.ArcticServiceConstants;
import com.netease.arctic.server.table.BasicTableSnapshot;
import com.netease.arctic.server.table.KeyedTableSnapshot;
import com.netease.arctic.server.table.ServerTableIdentifier;
import com.netease.arctic.server.table.TableRuntime;
import com.netease.arctic.server.table.TableSnapshot;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.TableMetaStore;
import com.netease.arctic.table.UnkeyedTable;
import com.netease.arctic.utils.CatalogUtil;
import com.netease.arctic.utils.TableFileUtil;
import com.netease.arctic.utils.TablePropertyUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.iceberg.CatalogProperties;
import org.apache.iceberg.ContentFile;
import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.FileContent;
import org.apache.iceberg.FileScanTask;
import org.apache.iceberg.Snapshot;
import org.apache.iceberg.TableMetadata;
import org.apache.iceberg.TableMetadataParser;
import org.apache.iceberg.TableProperties;
import org.apache.iceberg.TableScan;
import org.apache.iceberg.exceptions.CommitFailedException;
import org.apache.iceberg.io.CloseableIterable;
import org.apache.iceberg.io.FileIO;
import org.apache.iceberg.io.OutputFile;
import org.apache.iceberg.util.LocationUtil;
import org.apache.iceberg.util.StructLikeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class IcebergTableUtil {

  private static final Logger LOG = LoggerFactory.getLogger(IcebergTableUtil.class);

  public static final String DEFAULT_FILE_IO_IMPL = "org.apache.iceberg.io.ResolvingFileIO";
  public static final String METADATA_FOLDER_NAME = "metadata";
  public static final String PROPERTIES_METADATA_LOCATION = "iceberg.metadata.location";
  public static final String PROPERTIES_PREV_METADATA_LOCATION = "iceberg.metadata.prev-location";

  public static long getSnapshotId(UnkeyedTable internalTable, boolean refresh) {
    Snapshot currentSnapshot = getSnapshot(internalTable, refresh);
    if (currentSnapshot == null) {
      return ArcticServiceConstants.INVALID_SNAPSHOT_ID;
    } else {
      return currentSnapshot.snapshotId();
    }
  }

  public static TableSnapshot getSnapshot(ArcticTable arcticTable, TableRuntime tableRuntime) {
    tableRuntime.refresh(arcticTable);
    if (arcticTable.isUnkeyedTable()) {
      return new BasicTableSnapshot(tableRuntime.getCurrentSnapshotId());
    } else {
      StructLikeMap<Long> partitionOptimizedSequence =
          TablePropertyUtil.getPartitionOptimizedSequence(arcticTable.asKeyedTable());
      StructLikeMap<Long> legacyPartitionMaxTransactionId =
          TablePropertyUtil.getLegacyPartitionMaxTransactionId(arcticTable.asKeyedTable());
      return new KeyedTableSnapshot(tableRuntime.getCurrentSnapshotId(),
          tableRuntime.getCurrentChangeSnapshotId(),
          partitionOptimizedSequence,
          legacyPartitionMaxTransactionId);
    }
  }

  public static Snapshot getSnapshot(UnkeyedTable internalTable, boolean refresh) {
    if (refresh) {
      internalTable.refresh();
    }
    return internalTable.currentSnapshot();
  }

  public static Set<String> getAllContentFilePath(UnkeyedTable internalTable) {
    Set<String> validFilesPath = new HashSet<>();

    TableEntriesScan entriesScan = TableEntriesScan.builder(internalTable)
        .includeFileContent(FileContent.DATA, FileContent.POSITION_DELETES, FileContent.EQUALITY_DELETES)
        .allEntries().build();
    try (CloseableIterable<IcebergFileEntry> entries = entriesScan.entries()) {
      for (IcebergFileEntry entry : entries) {
        validFilesPath.add(TableFileUtil.getUriPath(entry.getFile().path().toString()));
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    return validFilesPath;
  }

  public static Set<DeleteFile> getIndependentFiles(UnkeyedTable internalTable) {
    if (internalTable.currentSnapshot() == null) {
      return Collections.emptySet();
    }
    Set<String> deleteFilesPath = new HashSet<>();
    TableScan tableScan = internalTable.newScan();
    try (CloseableIterable<FileScanTask> fileScanTasks = tableScan.planFiles()) {
      for (FileScanTask fileScanTask : fileScanTasks) {
        for (DeleteFile delete : fileScanTask.deletes()) {
          deleteFilesPath.add(delete.path().toString());
        }
      }
    } catch (IOException e) {
      LOG.error("table scna plan files error", e);
      return Collections.emptySet();
    }

    Set<DeleteFile> independentFiles = new HashSet<>();
    TableEntriesScan entriesScan = TableEntriesScan.builder(internalTable)
        .useSnapshot(internalTable.currentSnapshot().snapshotId())
        .includeFileContent(FileContent.EQUALITY_DELETES, FileContent.POSITION_DELETES)
        .build();

    for (IcebergFileEntry entry : entriesScan.entries()) {
      ContentFile<?> file = entry.getFile();
      String path = file.path().toString();
      if (!deleteFilesPath.contains(path)) {
        independentFiles.add((DeleteFile) file);
      }
    }

    return independentFiles;
  }


  /**
   * create an iceberg file io instance
   * @param meta catalog meta
   * @return iceberg file io
   */
  public static FileIO newIcebergFileIo(CatalogMeta meta) {
    Map<String, String> catalogProperties = meta.getCatalogProperties();
    TableMetaStore store = CatalogUtil.buildMetaStore(meta);
    Configuration conf = store.getConfiguration();
    String ioImpl = catalogProperties.getOrDefault(CatalogProperties.FILE_IO_IMPL, DEFAULT_FILE_IO_IMPL);
    return org.apache.iceberg.CatalogUtil.loadFileIO(ioImpl, catalogProperties, conf);
  }


  /**
   * load iceberg table metadata by given ams table metadata
   *
   * @param io        - iceberg file io
   * @param tableMeta - table metadata
   * @return iceberg table metadata object
   */
  public static TableMetadata loadIcebergTableMetadata(
      FileIO io, com.netease.arctic.server.table.TableMetadata tableMeta) {
    String metadataFileLocation = tableMeta.getProperties().get(PROPERTIES_METADATA_LOCATION);
    if (StringUtils.isBlank(metadataFileLocation)) {
      return null;
    }
    return TableMetadataParser.read(io, metadataFileLocation);
  }


  /**
   * generate metadata file location with version
   *
   * @param meta       - iceberg table metadata
   * @param newVersion - version of table metadata
   * @return - file location
   */
  private static String genNewMetadataFileLocation(TableMetadata meta, long newVersion) {
    String codecName =
        meta.property(
            TableProperties.METADATA_COMPRESSION, TableProperties.METADATA_COMPRESSION_DEFAULT);
    String fileExtension = TableMetadataParser.getFileExtension(codecName);
    return genMetadataFileLocation(
        meta, String.format("v%05d-%s%s", newVersion, UUID.randomUUID(), fileExtension));
  }

  /**
   * generate metadata file location with version
   *
   * @param base       - iceberg table metadata
   * @param current    - new iceberg table metadata
   * @return - file location
   */
  public static String genNewMetadataFileLocation(TableMetadata base, TableMetadata current) {
    long version = 0;
    if (base != null && StringUtils.isNotBlank(base.metadataFileLocation())) {
      version = parseMetadataFileVersion(base.metadataFileLocation());
    }

    return genNewMetadataFileLocation(current, version + 1);
  }

  /**
   * generate the metadata file location with given filename
   *
   * @param metadata - the table metadata
   * @param filename - filename for table metadata
   * @return - table metadata file location
   */
  public static String genMetadataFileLocation(TableMetadata metadata, String filename) {
    String metadataLocation = metadata.properties().get(TableProperties.WRITE_METADATA_LOCATION);

    if (metadataLocation != null) {
      return String.format("%s/%s", LocationUtil.stripTrailingSlash(metadataLocation), filename);
    } else {
      return String.format("%s/%s/%s", metadata.location(), METADATA_FOLDER_NAME, filename);
    }
  }


  /**
   * create iceberg table and return an AMS table metadata object to commit.
   *
   * @param identifier           - table identifier
   * @param catalogMeta          - catalog meta
   * @param icebergTableMetadata - iceberg table metadata object
   * @param metadataFileLocation - iceberg table metadata file location
   * @param io                   - iceberg table file io
   * @return AMS table metadata object
   */
  public static com.netease.arctic.server.table.TableMetadata createTableInternal(
      ServerTableIdentifier identifier,
      CatalogMeta catalogMeta,
      TableMetadata icebergTableMetadata,
      String metadataFileLocation,
      FileIO io
  ) {
    OutputFile outputFile = io.newOutputFile(metadataFileLocation);
    TableMetadataParser.overwrite(icebergTableMetadata, outputFile);
    TableMeta meta = new TableMeta();
    meta.setTableIdentifier(identifier.getIdentifier());
    meta.putToLocations(MetaTableProperties.LOCATION_KEY_TABLE, icebergTableMetadata.location());
    meta.putToLocations(MetaTableProperties.LOCATION_KEY_BASE, icebergTableMetadata.location());

    meta.setFormat(TableFormat.ICEBERG.name());
    meta.putToProperties(PROPERTIES_METADATA_LOCATION, metadataFileLocation);
    return new com.netease.arctic.server.table.TableMetadata(
        identifier, meta, catalogMeta);
  }


  private static long parseMetadataFileVersion(String metadataLocation) {
    int fileNameStart = metadataLocation.lastIndexOf('/') + 1; // if '/' isn't found, this will be 0
    String fileName = metadataLocation.substring(fileNameStart);
    if (fileName.startsWith("v")) {
      fileName = fileName.substring(1);
    }
    int versionEnd = fileName.indexOf('-');
    if (versionEnd < 0) {
      return 0;
    }
    try {
      return Long.parseLong(fileName.substring(0, versionEnd));
    } catch (NumberFormatException e) {
      LOG.warn("Unable to parse version from metadata location: {}", metadataLocation, e);
      return 0;
    }
  }


  /**
   * write iceberg table metadata and apply changes to AMS tableMetadata to commit.
   *
   * @param amsTableMetadata         ams table metadata
   * @param baseIcebergTableMetadata base iceberg table metadata
   * @param icebergTableMetadata     iceberg table metadata to commit
   * @param newMetadataFileLocation  new metadata file location
   * @param io                       iceberg file io
   */
  public static void commitTableInternal(
      com.netease.arctic.server.table.TableMetadata amsTableMetadata,
      TableMetadata baseIcebergTableMetadata,
      TableMetadata icebergTableMetadata,
      String newMetadataFileLocation,
      FileIO io
  ) {
    if (!Objects.equals(icebergTableMetadata.location(), baseIcebergTableMetadata.location())) {
      throw new UnsupportedOperationException("SetLocation is not supported.");
    }
    OutputFile outputFile = io.newOutputFile(newMetadataFileLocation);
    TableMetadataParser.overwrite(icebergTableMetadata, outputFile);

    Map<String, String> properties = amsTableMetadata.getProperties();
    properties.put(PROPERTIES_PREV_METADATA_LOCATION, baseIcebergTableMetadata.metadataFileLocation());
    properties.put(PROPERTIES_METADATA_LOCATION, newMetadataFileLocation);
    amsTableMetadata.setProperties(properties);
  }

  public static void checkCommitSuccess(
      com.netease.arctic.server.table.TableMetadata updatedTableMetadata,
      String metadataFileLocation) {
    String metaLocationInDatabase = updatedTableMetadata.getProperties().get(PROPERTIES_METADATA_LOCATION);
    if (!Objects.equals(metaLocationInDatabase, metadataFileLocation)) {
      throw new CommitFailedException(
          "commit conflict, some other commit happened during this commit. ");
    }
  }
}
