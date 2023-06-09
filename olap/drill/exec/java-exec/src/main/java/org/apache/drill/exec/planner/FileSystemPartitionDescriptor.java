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
package org.apache.drill.exec.planner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.calcite.plan.RelOptTable;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.drill.common.util.GuavaUtils;
import org.apache.drill.shaded.guava.com.google.common.base.Charsets;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

import org.apache.calcite.adapter.enumerable.EnumerableTableScan;
import org.apache.calcite.prepare.RelOptTableImpl;
import org.apache.calcite.rel.core.TableScan;
import org.apache.calcite.util.BitSets;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.physical.base.FileGroupScan;
import org.apache.drill.exec.planner.logical.SelectionBasedTableScan;
import org.apache.drill.exec.planner.logical.DrillRel;
import org.apache.drill.exec.planner.logical.DrillScanRel;
import org.apache.drill.exec.planner.logical.DrillTable;
import org.apache.drill.exec.planner.logical.DrillTranslatableTable;
import org.apache.drill.exec.planner.logical.DynamicDrillTable;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.exec.store.dfs.FileSelection;
import org.apache.drill.exec.store.dfs.FormatSelection;
import org.apache.drill.exec.store.dfs.MetadataContext;
import org.apache.drill.exec.vector.NullableVarCharVector;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.hadoop.fs.Path;


/**
 * Partition descriptor for file system based tables.
 */
public class FileSystemPartitionDescriptor extends AbstractPartitionDescriptor {

  static final int MAX_NESTED_SUBDIRS = 10; // allow up to 10 nested sub-directories

  private final String partitionLabel;
  private final int partitionLabelLength;
  private final Map<String, Integer> partitions = new HashMap<>();
  private final TableScan scanRel;
  private final DrillTable table;

  public FileSystemPartitionDescriptor(PlannerSettings settings, TableScan scanRel) {
    Preconditions.checkArgument(scanRel instanceof DrillScanRel || scanRel instanceof EnumerableTableScan);
    this.partitionLabel = settings.getFsPartitionColumnLabel();
    this.partitionLabelLength = partitionLabel.length();
    this.scanRel = scanRel;
    DrillTable unwrap;
    unwrap = scanRel.getTable().unwrap(DrillTable.class);
    if (unwrap == null) {
      unwrap = scanRel.getTable().unwrap(DrillTranslatableTable.class).getDrillTable();
    }

    table = unwrap;

    for(int i =0; i < 10; i++){
      partitions.put(partitionLabel + i, i);
    }
  }

  @Override
  public int getPartitionHierarchyIndex(String partitionName) {
    String suffix = partitionName.substring(partitionLabelLength); // get the numeric suffix from 'dir<N>'
    return Integer.parseInt(suffix);
  }

  @Override
  public boolean isPartitionName(String name) {
    return partitions.containsKey(name);
  }

  @Override
  public Integer getIdIfValid(String name) {
    return partitions.get(name);
  }

  @Override
  public int getMaxHierarchyLevel() {
    return MAX_NESTED_SUBDIRS;
  }

  public DrillTable getTable() {
    return table;
  }

  @Override
  public void populatePartitionVectors(ValueVector[] vectors, List<PartitionLocation> partitions,
                                       BitSet partitionColumnBitSet, Map<Integer, String> fieldNameMap) {
    int record = 0;
    for (PartitionLocation partitionLocation: partitions) {
      for (int partitionColumnIndex : BitSets.toIter(partitionColumnBitSet)) {
        if (partitionLocation.getPartitionValue(partitionColumnIndex) == null) {
          // set null if dirX does not exist for the location.
          ((NullableVarCharVector) vectors[partitionColumnIndex]).getMutator().setNull(record);
        } else {
          byte[] bytes = (partitionLocation.getPartitionValue(partitionColumnIndex)).getBytes(Charsets.UTF_8);
          ((NullableVarCharVector) vectors[partitionColumnIndex]).getMutator().setSafe(record, bytes, 0, bytes.length);
        }
      }
      record++;
    }

    for (ValueVector v : vectors) {
      if (v == null) {
        continue;
      }
      v.getMutator().setValueCount(partitions.size());
    }
  }

  @Override
  public TypeProtos.MajorType getVectorType(SchemaPath column, PlannerSettings plannerSettings) {
    return Types.optional(TypeProtos.MinorType.VARCHAR);
  }

  public String getName(int index) {
    return partitionLabel + index;
  }

  @Override
  public Path getBaseTableLocation() {
    final FormatSelection origSelection = (FormatSelection) table.getSelection();
    return origSelection.getSelection().getSelectionRoot();
  }

  @Override
  protected void createPartitionSublists() {
    final Pair<Collection<Path>, Boolean> fileLocationsAndStatus = getFileLocationsAndStatus();
    List<PartitionLocation> locations = new LinkedList<>();
    boolean hasDirsOnly = fileLocationsAndStatus.getRight();

    final Path selectionRoot = getBaseTableLocation();

    // map used to map the partition keys (dir0, dir1, ..), to the list of partitions
    // that share the same partition keys.
    // For example,
    //   1990/Q1/1.parquet, 2.parquet
    // would have <1990, Q1> as key, and value as list of partition location for 1.parquet and 2.parquet.
    HashMap<List<String>, List<PartitionLocation>> dirToFileMap = new HashMap<>();

    // Figure out the list of leaf subdirectories. For each leaf subdirectory, find
    // the list of files (DFSFilePartitionLocation) it contains.
    for (Path file: fileLocationsAndStatus.getLeft()) {
      DFSFilePartitionLocation dfsFilePartitionLocation = new DFSFilePartitionLocation(MAX_NESTED_SUBDIRS,
          selectionRoot, file, hasDirsOnly);
      List<String> dirList = Arrays.asList(dfsFilePartitionLocation.getDirs());

      dirToFileMap.putIfAbsent(dirList, new ArrayList<>());
      dirToFileMap.get(dirList).add(dfsFilePartitionLocation);
    }

    // build a list of DFSDirPartitionLocation.
    dirToFileMap.keySet().stream()
        .map(dirs -> new DFSDirPartitionLocation(dirs.toArray(new String[dirs.size()]), dirToFileMap.get(dirs)))
        .forEach(locations::add);

    locationSuperList = Lists.partition(locations, PartitionDescriptor.PARTITION_BATCH_SIZE);
    sublistsCreated = true;
  }

  protected Pair<Collection<Path>, Boolean> getFileLocationsAndStatus() {
    Collection<Path> fileLocations = null;
    boolean isExpandedPartial = false;
    if (scanRel instanceof DrillScanRel) {
      // If a particular GroupScan provides files, get the list of files from there rather than
      // DrillTable because GroupScan would have the updated version of the selection
      DrillScanRel drillScan = (DrillScanRel) scanRel;
      if (drillScan.getGroupScan().hasFiles()) {
        fileLocations = drillScan.getGroupScan().getFiles();
        isExpandedPartial = false;
      } else {
        FileSelection selection = ((FormatSelection) table.getSelection()).getSelection();
        fileLocations = selection.getFiles();
        isExpandedPartial = selection.isExpandedPartial();
      }
    } else if (scanRel instanceof EnumerableTableScan) {
      FileSelection selection = ((FormatSelection) table.getSelection()).getSelection();
      fileLocations = selection.getFiles();
      isExpandedPartial = selection.isExpandedPartial();
    }
    return Pair.of(fileLocations, isExpandedPartial);
  }

  @Override
  public TableScan createTableScan(List<PartitionLocation> newPartitionLocation, Path cacheFileRoot,
      boolean wasAllPartitionsPruned, MetadataContext metaContext) throws Exception {
    List<Path> newFiles = new ArrayList<>();
    for (PartitionLocation location : newPartitionLocation) {
      if (!location.isCompositePartition()) {
        newFiles.add(location.getEntirePartitionLocation());
      } else {
        final Collection<SimplePartitionLocation> subPartitions = location.getPartitionLocationRecursive();
        for (PartitionLocation subPart : subPartitions) {
          newFiles.add(subPart.getEntirePartitionLocation());
        }
      }
    }

    FormatSelection formatSelection = (FormatSelection) table.getSelection();
    FileSelection newFileSelection = new FileSelection(null, newFiles, getBaseTableLocation(),
        cacheFileRoot, wasAllPartitionsPruned, formatSelection.getSelection().getDirStatus());
    newFileSelection.setMetaContext(metaContext);
    RelOptTable relOptTable = scanRel.getTable();

    if (scanRel instanceof DrillScanRel) {
      FileGroupScan newGroupScan =
          ((FileGroupScan) ((DrillScanRel) scanRel).getGroupScan()).clone(newFileSelection);
      return new DrillScanRel(scanRel.getCluster(),
                      scanRel.getTraitSet().plus(DrillRel.DRILL_LOGICAL),
                      relOptTable,
                      newGroupScan,
                      scanRel.getRowType(),
                      ((DrillScanRel) scanRel).getColumns(),
                      true /*filter pushdown*/);
    } else if (scanRel instanceof EnumerableTableScan) {
      FormatSelection newFormatSelection = new FormatSelection(formatSelection.getFormat(), newFileSelection);

      DynamicDrillTable dynamicDrillTable = new DynamicDrillTable(table.getPlugin(), table.getStorageEngineName(),
          table.getUserName(), newFormatSelection, table.getMetadataProviderManager());
      /* Copy statistics from the original relOptTable */
      DrillTranslatableTable newTable = new DrillTranslatableTable(dynamicDrillTable);

      RelOptTableImpl newOptTableImpl = RelOptTableImpl.create(relOptTable.getRelOptSchema(), relOptTable.getRowType(),
          newTable, GuavaUtils.convertToUnshadedImmutableList(relOptTable.getQualifiedName()));

      // return a SelectionBasedTableScan with fileSelection being part of digest of TableScan node.
      return SelectionBasedTableScan.create(scanRel.getCluster(), newOptTableImpl, newFileSelection.toString());
    } else {
      throw new UnsupportedOperationException("Only DrillScanRel and EnumerableTableScan is allowed!");
    }
  }

  @Override
  public TableScan createTableScan(List<PartitionLocation> newPartitionLocation,
      boolean wasAllPartitionsPruned) throws Exception {
    return createTableScan(newPartitionLocation, null, wasAllPartitionsPruned, null);
  }

  @Override
  public boolean supportsMetadataCachePruning() {
    final Object selection = this.table.getSelection();
    return selection instanceof FormatSelection
        && ((FormatSelection)selection).getSelection().getCacheFileRoot() != null;
  }

  private static boolean supportsScan(TableScan scanRel) {
    return scanRel instanceof SelectionBasedTableScan;
  }
}
