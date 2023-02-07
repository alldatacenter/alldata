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

import org.apache.calcite.rel.core.TableScan;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.physical.base.SchemalessScan;
import org.apache.drill.exec.planner.FileSystemPartitionDescriptor;
import org.apache.drill.exec.planner.PartitionLocation;
import org.apache.drill.exec.planner.logical.DrillRel;
import org.apache.drill.exec.planner.logical.DrillScanRel;
import org.apache.drill.exec.planner.logical.DrillTable;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.exec.store.ColumnExplorer;
import org.apache.drill.exec.store.dfs.DrillFileSystem;
import org.apache.drill.exec.store.dfs.FileSelection;
import org.apache.drill.exec.store.dfs.FormatSelection;
import org.apache.drill.exec.util.DrillFileSystemUtil;
import org.apache.drill.exec.util.ImpersonationUtil;
import org.apache.drill.metastore.MetastoreColumn;
import org.apache.drill.metastore.components.tables.BasicTablesRequests;
import org.apache.drill.metastore.metadata.MetadataInfo;
import org.apache.drill.metastore.metadata.MetadataType;
import org.apache.drill.metastore.metadata.TableInfo;
import org.apache.drill.metastore.statistics.TableStatisticsKind;
import org.apache.drill.shaded.guava.com.google.common.collect.ArrayListMultimap;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.shaded.guava.com.google.common.collect.Multimap;
import org.apache.drill.shaded.guava.com.google.common.collect.Streams;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Implementation of {@link MetadataInfoCollector} for file-based tables.
 */
public class FileMetadataInfoCollector implements MetadataInfoCollector {
  private final List<MetadataInfo> metadataToRemove;

  private final BasicTablesRequests basicRequests;
  private final TableInfo tableInfo;
  private final MetadataType metadataLevel;

  private List<MetadataInfo> allMetaToHandle;
  private List<MetadataInfo> rowGroupsInfo = Collections.emptyList();
  private List<MetadataInfo> filesInfo = Collections.emptyList();
  private Multimap<Integer, MetadataInfo> segmentsInfo = ArrayListMultimap.create();

  private TableScan tableScan;

  private boolean outdated = true;

  public FileMetadataInfoCollector(BasicTablesRequests basicRequests, TableInfo tableInfo, FormatSelection selection,
      PlannerSettings settings, Supplier<TableScan> tableScanSupplier, List<SchemaPath> interestingColumns,
      MetadataType metadataLevel, int segmentColumnsCount) throws IOException {
    this.basicRequests = basicRequests;
    this.tableInfo = tableInfo;
    this.metadataLevel = metadataLevel;
    this.metadataToRemove = new ArrayList<>();
    init(selection, settings, tableScanSupplier, interestingColumns, segmentColumnsCount);
  }

  @Override
  public List<MetadataInfo> getRowGroupsInfo() {
    return rowGroupsInfo;
  }

  @Override
  public List<MetadataInfo> getFilesInfo() {
    return filesInfo;
  }

  @Override
  public Multimap<Integer, MetadataInfo> getSegmentsInfo() {
    return segmentsInfo;
  }

  @Override
  public List<MetadataInfo> getAllMetaToHandle() {
    return allMetaToHandle;
  }

  @Override
  public List<MetadataInfo> getMetadataToRemove() {
    return metadataToRemove;
  }

  @Override
  public TableScan getPrunedScan() {
    return tableScan;
  }

  @Override
  public boolean isOutdated() {
    return outdated;
  }

  private void init(FormatSelection selection, PlannerSettings settings, Supplier<TableScan> tableScanSupplier,
      List<SchemaPath> interestingColumns, int segmentColumnsCount) throws IOException {
    List<SchemaPath> metastoreInterestingColumns =
        Optional.ofNullable(basicRequests.interestingColumnsAndPartitionKeys(tableInfo).interestingColumns())
            .map(metastoreInterestingColumnNames -> metastoreInterestingColumnNames.stream()
                // interesting column names are escaped, so SchemaPath.parseFromString() should be used here
                .map(SchemaPath::parseFromString)
                .collect(Collectors.toList()))
        .orElse(null);

    Map<String, Long> filesNamesLastModifiedTime = basicRequests.filesLastModifiedTime(tableInfo, null, null);

    List<String> newFiles = new ArrayList<>();
    List<String> updatedFiles = new ArrayList<>();
    List<String> removedFiles = new ArrayList<>(filesNamesLastModifiedTime.keySet());
    List<String> allFiles = new ArrayList<>();

    for (FileStatus fileStatus : getFileStatuses(selection)) {
      String path = Path.getPathWithoutSchemeAndAuthority(fileStatus.getPath()).toUri().getPath();
      Long lastModificationTime = filesNamesLastModifiedTime.get(path);
      if (lastModificationTime == null) {
        newFiles.add(path);
      } else if (lastModificationTime < fileStatus.getModificationTime()) {
        updatedFiles.add(path);
      }
      removedFiles.remove(path);
      allFiles.add(path);
    }

    String selectionRoot = selection.getSelection().getSelectionRoot().toUri().getPath();

    if (!Objects.equals(metastoreInterestingColumns, interestingColumns)
        && metastoreInterestingColumns != null &&
        (interestingColumns == null || !metastoreInterestingColumns.containsAll(interestingColumns))
        || TableStatisticsKind.ANALYZE_METADATA_LEVEL.getValue(basicRequests.tableMetadata(tableInfo)).compareTo(metadataLevel) != 0) {
      // do not update table scan and lists of segments / files / row groups,
      // metadata should be recalculated
      tableScan = tableScanSupplier.get();
      metadataToRemove.addAll(getMetadataInfoList(selectionRoot, removedFiles, MetadataType.SEGMENT, 0));
      return;
    }

    // checks whether there are no new, updated and removed files
    if (!newFiles.isEmpty() || !updatedFiles.isEmpty() || !removedFiles.isEmpty()) {
      List<String> scanFiles = new ArrayList<>(newFiles);
      scanFiles.addAll(updatedFiles);

      // updates scan to read updated / new files
      tableScan = getTableScan(settings, tableScanSupplier.get(), scanFiles);

      // iterates from the end;
      // takes deepest updated segments;
      // finds their parents:
      //  - fetches all segments for parent level;
      //  - filters segments to leave parents only;
      // obtains all child segments;
      // filters child segments for filtered parent segments

      int lastSegmentIndex = segmentColumnsCount - 1;

      List<String> scanAndRemovedFiles = new ArrayList<>(scanFiles);
      scanAndRemovedFiles.addAll(removedFiles);

      // 1. Obtain files info for files from the same folder without removed files
      // 2. Get segments for obtained files + segments for removed files
      // 3. Get parent segments
      // 4. Get other segments for the same parent segment
      // 5. Remove segments which have only removed files (matched for removedFileInfo and don't match to filesInfo)
      // 6. Do the same for parent segments

      List<MetadataInfo> allFilesInfo = getMetadataInfoList(selectionRoot, allFiles, MetadataType.FILE, 0);

      // first pass: collect updated segments even without files, they will be removed later
      List<MetadataInfo> leafSegments = getMetadataInfoList(selectionRoot, scanAndRemovedFiles, MetadataType.SEGMENT, lastSegmentIndex);
      List<MetadataInfo> removedFilesMetadata = getMetadataInfoList(selectionRoot, removedFiles, MetadataType.FILE, 0);

      List<MetadataInfo> scanFilesInfo = getMetadataInfoList(selectionRoot, scanAndRemovedFiles, MetadataType.FILE, 0);
      // files from scan + files from the same folder without removed files
      filesInfo = leafSegments.stream()
          .filter(parent -> scanFilesInfo.stream().anyMatch(child -> MetadataIdentifierUtils.isMetadataKeyParent(parent.identifier(), child.identifier())))
          .flatMap(parent ->
              allFilesInfo.stream()
                  .filter(child -> MetadataIdentifierUtils.isMetadataKeyParent(parent.identifier(), child.identifier())))
          .collect(Collectors.toList());

      Multimap<Integer, MetadataInfo> allSegments = populateSegments(removedFiles, allFiles, selectionRoot, lastSegmentIndex, leafSegments, removedFilesMetadata);

      List<MetadataInfo> allRowGroupsInfo = getAllRowGroupsMetadataInfos(allFiles);

      rowGroupsInfo = allRowGroupsInfo.stream()
          .filter(child -> filesInfo.stream()
              .map(MetadataInfo::identifier)
              .anyMatch(parent -> MetadataIdentifierUtils.isMetadataKeyParent(parent, child.identifier())))
          .collect(Collectors.toList());

      List<MetadataInfo> segmentsToUpdate = getMetadataInfoList(selectionRoot, scanAndRemovedFiles, MetadataType.SEGMENT, 0);
      allMetaToHandle = Streams.concat(allSegments.values().stream(), allFilesInfo.stream(), allRowGroupsInfo.stream())
          .filter(child -> segmentsToUpdate.stream().anyMatch(parent -> MetadataIdentifierUtils.isMetadataKeyParent(parent.identifier(), child.identifier())))
          .filter(parent ->
              removedFilesMetadata.stream().noneMatch(child -> MetadataIdentifierUtils.isMetadataKeyParent(parent.identifier(), child.identifier()))
                  || filesInfo.stream().anyMatch(child -> MetadataIdentifierUtils.isMetadataKeyParent(parent.identifier(), child.identifier())))
          .collect(Collectors.toList());

      // removed top-level segments are handled separately since their metadata is not overridden when producing writing to the Metastore
      List<MetadataInfo> removedTopSegments = getMetadataInfoList(selectionRoot, removedFiles, MetadataType.SEGMENT, 0).stream()
          .filter(parent ->
              removedFilesMetadata.stream().anyMatch(child -> MetadataIdentifierUtils.isMetadataKeyParent(parent.identifier(), child.identifier()))
                  && allFilesInfo.stream().noneMatch(child -> MetadataIdentifierUtils.isMetadataKeyParent(parent.identifier(), child.identifier())))
          .collect(Collectors.toList());
      metadataToRemove.addAll(removedTopSegments);

      segmentsToUpdate.stream()
          .filter(segment -> !removedTopSegments.contains(segment))
          .forEach(allMetaToHandle::add);
    } else {
      // table metadata may still be actual
      outdated = false;
    }
  }

  private Multimap<Integer, MetadataInfo> populateSegments(List<String> removedFiles, List<String> allFiles,
      String selectionRoot, int lastSegmentIndex, List<MetadataInfo> leafSegments, List<MetadataInfo> removedFilesMetadata) {
    List<String> presentAndRemovedFiles = new ArrayList<>(allFiles);
    presentAndRemovedFiles.addAll(removedFiles);
    Multimap<Integer, MetadataInfo> allSegments = ArrayListMultimap.create();
    if (lastSegmentIndex > 0) {
      allSegments.putAll(lastSegmentIndex, getMetadataInfoList(selectionRoot, presentAndRemovedFiles, MetadataType.SEGMENT, lastSegmentIndex));
    }

    for (int i = lastSegmentIndex - 1; i >= 0; i--) {
      List<MetadataInfo> currentChildSegments = leafSegments;
      List<MetadataInfo> allParentSegments = getMetadataInfoList(selectionRoot, presentAndRemovedFiles, MetadataType.SEGMENT, i);
      allSegments.putAll(i, allParentSegments);

      // segments, parent for segments from currentChildSegments
      List<MetadataInfo> parentSegments = allParentSegments.stream()
          .filter(parent -> currentChildSegments.stream().anyMatch(child -> MetadataIdentifierUtils.isMetadataKeyParent(parent.identifier(), child.identifier())))
          .collect(Collectors.toList());

      // all segments children for parentSegments segments except empty segments
      List<MetadataInfo> childSegments = allSegments.get(i + 1).stream()
          .filter(child -> parentSegments.stream().anyMatch(parent -> MetadataIdentifierUtils.isMetadataKeyParent(parent.identifier(), child.identifier())))
          .filter(parent ->
              removedFilesMetadata.stream().noneMatch(child -> MetadataIdentifierUtils.isMetadataKeyParent(parent.identifier(), child.identifier()))
                  || filesInfo.stream().anyMatch(child -> MetadataIdentifierUtils.isMetadataKeyParent(parent.identifier(), child.identifier())))
          .collect(Collectors.toList());

      segmentsInfo.putAll(i + 1, childSegments);
      leafSegments = childSegments;
    }
    segmentsInfo.putAll(0, getMetadataInfoList(selectionRoot, presentAndRemovedFiles, MetadataType.SEGMENT, 0).stream()
        .filter(parent ->
            removedFilesMetadata.stream().noneMatch(child -> MetadataIdentifierUtils.isMetadataKeyParent(parent.identifier(), child.identifier()))
                || filesInfo.stream().anyMatch(child -> MetadataIdentifierUtils.isMetadataKeyParent(parent.identifier(), child.identifier())))
        .collect(Collectors.toList()));
    return allSegments;
  }

  private List<MetadataInfo> getAllRowGroupsMetadataInfos(List<String> allFiles) {
    List<String> metadataKeys = filesInfo.stream()
        .map(MetadataInfo::key)
        .distinct()
        .collect(Collectors.toList());

    BasicTablesRequests.RequestMetadata requestMetadata = BasicTablesRequests.RequestMetadata.builder()
        .tableInfo(tableInfo)
        .metadataKeys(metadataKeys)
        .paths(allFiles)
        .metadataType(MetadataType.ROW_GROUP)
        .requestColumns(Arrays.asList(MetastoreColumn.METADATA_KEY, MetastoreColumn.METADATA_IDENTIFIER, MetastoreColumn.METADATA_TYPE))
        .build();

    return basicRequests.request(requestMetadata).stream()
        .map(unit -> MetadataInfo.builder().metadataUnit(unit).build())
        .collect(Collectors.toList());
  }

  private List<FileStatus> getFileStatuses(FormatSelection selection) throws IOException {
    FileSelection fileSelection = selection.getSelection();

    FileSystem rawFs = fileSelection.getSelectionRoot().getFileSystem(new Configuration());
    DrillFileSystem fs = ImpersonationUtil.createFileSystem(ImpersonationUtil.getProcessUserName(), rawFs.getConf());

    return getFileStatuses(fileSelection, fs);
  }

  private TableScan getTableScan(PlannerSettings settings, TableScan scanRel, List<String> scanFiles) {
    FileSystemPartitionDescriptor descriptor =
        new FileSystemPartitionDescriptor(settings, scanRel);

    List<PartitionLocation> newPartitions = Lists.newArrayList(descriptor.iterator()).stream()
        .flatMap(Collection::stream)
        .flatMap(p -> p.getPartitionLocationRecursive().stream())
        .filter(p -> scanFiles.contains(p.getEntirePartitionLocation().toUri().getPath()))
        .collect(Collectors.toList());

    try {
      if (!newPartitions.isEmpty()) {
        return descriptor.createTableScan(newPartitions, false);
      } else {
        DrillTable drillTable = descriptor.getTable();
        SchemalessScan scan = new SchemalessScan(drillTable.getUserName(), ((FormatSelection) descriptor.getTable().getSelection()).getSelection().getSelectionRoot());

        return new DrillScanRel(scanRel.getCluster(),
            scanRel.getTraitSet().plus(DrillRel.DRILL_LOGICAL),
            scanRel.getTable(),
            scan,
            scanRel.getRowType(),
            DrillScanRel.getProjectedColumns(scanRel.getTable(), true),
            true /*filter pushdown*/);
      }
    } catch (Exception e) {
      throw new RuntimeException("Error happened during recreation of pruned scan", e);
    }
  }

  private List<MetadataInfo> getMetadataInfoList(String parent, List<String> locations, MetadataType metadataType, int level) {
    return locations.stream()
        .map(location -> getMetadataInfo(parent, location, metadataType, level))
        .distinct()
        .collect(Collectors.toList());
  }

  private MetadataInfo getMetadataInfo(String parent, String location, MetadataType metadataType, int level) {
    List<String> values = ColumnExplorer.listPartitionValues(new Path(location), new Path(parent), true);

    switch (metadataType) {
      case ROW_GROUP: {
        throw new UnsupportedOperationException("MetadataInfo cannot be obtained for row group using file location only");
      }
      case FILE: {
        String key = values.size() > 1 ? values.iterator().next() : MetadataInfo.DEFAULT_SEGMENT_KEY;
        return MetadataInfo.builder()
            .type(metadataType)
            .key(key)
            .identifier(MetadataIdentifierUtils.getMetadataIdentifierKey(values))
            .build();
      }
      case SEGMENT: {
        String key = values.size() > 1 ? values.iterator().next() : MetadataInfo.DEFAULT_SEGMENT_KEY;
        return MetadataInfo.builder()
            .type(metadataType)
            .key(key)
            .identifier(values.size() > 1 ? MetadataIdentifierUtils.getMetadataIdentifierKey(values.subList(0, level + 1)) :  MetadataInfo.DEFAULT_SEGMENT_KEY)
            .build();
      }
      case TABLE: {
        return MetadataInfo.builder()
            .type(metadataType)
            .key(MetadataInfo.GENERAL_INFO_KEY)
            .build();
      }
      default:
        throw new UnsupportedOperationException(metadataType.name());
    }
  }

  /**
   * Returns list of {@link FileStatus} file statuses obtained from specified {@link FileSelection} file selection.
   * Specified file selection may be expanded fully if it wasn't expanded before.
   *
   * @param fileSelection file selection
   * @param fs            file system
   * @return list of {@link FileStatus} file statuses
   */
  public static List<FileStatus> getFileStatuses(FileSelection fileSelection, DrillFileSystem fs) throws IOException {
    if (!fileSelection.isExpandedFully()) {
      fileSelection = getExpandedFileSelection(fileSelection, fs);
    }
    return fileSelection.getStatuses(fs);
  }

  /**
   * Returns {@link FileSelection} file selection based on specified file selection with expanded file statuses.
   *
   * @param fileSelection file selection
   * @return expanded file selection
   */
  public static FileSelection getExpandedFileSelection(FileSelection fileSelection) throws IOException {
    FileSystem rawFs = fileSelection.getSelectionRoot().getFileSystem(new Configuration());
    FileSystem fs = ImpersonationUtil.createFileSystem(ImpersonationUtil.getProcessUserName(), rawFs.getConf());
    return getExpandedFileSelection(fileSelection, fs);
  }

  private static FileSelection getExpandedFileSelection(FileSelection fileSelection, FileSystem fs) throws IOException {
    List<FileStatus> fileStatuses = DrillFileSystemUtil.listFiles(fs, fileSelection.getSelectionRoot(), true);
    fileSelection = FileSelection.create(fileStatuses, null, fileSelection.getSelectionRoot());
    return fileSelection;
  }
}
