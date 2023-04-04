/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.ams.server.maintainer.command;

import com.google.common.collect.Iterables;
import com.netease.arctic.ams.server.maintainer.RepairUtil;
import com.netease.arctic.catalog.ArcticCatalog;
import com.netease.arctic.catalog.CatalogManager;
import com.netease.arctic.io.ArcticFileIO;
import com.netease.arctic.io.TableTrashManager;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.BaseLocationKind;
import com.netease.arctic.table.ChangeLocationKind;
import com.netease.arctic.table.KeyedTable;
import com.netease.arctic.table.TableIdentifier;
import com.netease.arctic.table.UnkeyedTable;
import org.apache.hadoop.fs.Path;
import org.apache.iceberg.ContentFile;
import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.FileScanTask;
import org.apache.iceberg.ManifestFile;
import org.apache.iceberg.Snapshot;
import org.apache.iceberg.exceptions.NoSuchTableException;
import org.apache.iceberg.exceptions.ValidationException;
import org.apache.iceberg.io.CloseableIterator;
import org.apache.thrift.TException;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TableAvailableAnalyzer {

  private static final Pattern PATTERN = Pattern.compile("Metadata file for version ([0-9]+) is missing");

  private Map<String, Boolean> fileExistCache = new HashMap<>();

  private CatalogManager catalogManager;

  private ArcticCatalog arcticCatalog;

  private TableIdentifier identifier;

  private ArcticFileIO io;

  private int maxFindSnapshotNum = 100;

  private int maxRollbackSnapNum = 10;

  public TableAvailableAnalyzer(
      CatalogManager catalogManager, TableIdentifier identifier, Integer maxFindSnapshotNum,
      Integer maxRollbackSnapNum) {
    this.arcticCatalog = catalogManager.getArcticCatalog(identifier.getCatalog());
    this.catalogManager = catalogManager;
    this.identifier = identifier;
    if (maxFindSnapshotNum != null) {
      this.maxFindSnapshotNum = maxFindSnapshotNum;
    }
    if (maxRollbackSnapNum != null) {
      this.maxRollbackSnapNum = maxRollbackSnapNum;
    }
    this.io = RepairUtil.arcticFileIO(catalogManager.getThriftAddress(), identifier.getCatalog());
  }

  public TableAnalyzeResult analyze() {
    TableAnalyzeResult tableAnalyzeResult = check();
    TableTrashManager tableTrashManager = null;
    try {
      tableTrashManager = RepairUtil.tableTrashManager(catalogManager.getThriftAddress(), identifier);
    } catch (TException e) {
      throw new RuntimeException(e);
    }
    tableAnalyzeResult.setTableTrashManager(tableTrashManager);
    return tableAnalyzeResult;
  }

  @NotNull
  private TableAnalyzeResult check() {
    ArcticTable arcticTable;
    try {
      arcticTable = arcticCatalog.loadTable(identifier);
    } catch (NoSuchTableException e) {
      // Now don't resolve this exception
      return TableAnalyzeResult.tableNotFound(identifier);
    } catch (ValidationException e) {
      Matcher matcher = PATTERN.matcher(e.getMessage());
      Integer version = null;
      while (matcher.find()) {
        version = Integer.parseInt(matcher.group(1));
      }

      //Not version exception
      if (version == null) {
        throw e;
      }

      //Metadata has lost
      RepairTableOperation baseOperation = new RepairTableOperation(catalogManager, identifier,
          BaseLocationKind.INSTANT);
      for (Path path: baseOperation.getMetadataCandidateFiles(baseOperation.findVersion())) {
        if (io.exists(path.toString())) {

          //Change is lose if base is exist
          return TableAnalyzeResult.metadataLose(identifier, version, new RepairTableOperation(catalogManager,
              identifier, ChangeLocationKind.INSTANT), ChangeLocationKind.INSTANT);
        }
      }

      return TableAnalyzeResult.metadataLose(identifier, version, new RepairTableOperation(catalogManager,
          identifier, BaseLocationKind.INSTANT), BaseLocationKind.INSTANT);
    }

    if (arcticTable.isKeyedTable()) {
      KeyedTable keyedTable = arcticTable.asKeyedTable();
      TableAnalyzeResult changeResult = check(keyedTable.changeTable());
      if (!changeResult.isOk()) {
        changeResult.setLocationKind(ChangeLocationKind.INSTANT);
        return changeResult;
      }
      TableAnalyzeResult baseResult = check(keyedTable.baseTable());
      baseResult.setLocationKind(BaseLocationKind.INSTANT);
      return baseResult;
    } else {
      TableAnalyzeResult result = check(arcticTable.asUnkeyedTable());
      return result;
    }
  }

  private TableAnalyzeResult check(UnkeyedTable table) {
    Snapshot currentSnapshot = table.currentSnapshot();
    if (currentSnapshot == null) {
      return TableAnalyzeResult.available(identifier);
    }
    TableAnalyzeResult tableAnalyzeResult = checkSnapshot(table, currentSnapshot);
    if (tableAnalyzeResult.isOk()) {
      return tableAnalyzeResult;
    }

    //find can roll back snapshot
    Iterable<Snapshot> remainSnapshot = Iterables.filter(table.snapshots(), s -> !s.equals(currentSnapshot));
    Iterable<Snapshot> maxFindSnapshot = Iterables.limit(remainSnapshot, maxFindSnapshotNum);
    Iterable<Snapshot> okSnapshot = Iterables.filter(maxFindSnapshot, s -> checkSnapshot(table, s).isOk());
    Iterable<Snapshot> finalOkSnapshot = Iterables.limit(okSnapshot, maxRollbackSnapNum);
    List<Snapshot> rollbackSnapshot = new ArrayList<>();
    Iterables.addAll(rollbackSnapshot, finalOkSnapshot);
    tableAnalyzeResult.setRollbackList(rollbackSnapshot);
    tableAnalyzeResult.setArcticTable(table);
    return tableAnalyzeResult;
  }

  @NotNull
  private TableAnalyzeResult checkSnapshot(UnkeyedTable table, Snapshot currentSnapshot) {

    //check manifestList
    if (!exists(currentSnapshot.manifestListLocation())) {
      return TableAnalyzeResult.manifestListLose(identifier, currentSnapshot, table);
    }

    //check manifest
    List<ManifestFile> manifestFiles = currentSnapshot.allManifests();

    List<ManifestFile> loseManifests =
        manifestFiles.stream().filter(s -> !exists(s.path())).collect(Collectors.toList());
    if (loseManifests.size() != 0) {
      return TableAnalyzeResult.manifestLost(identifier, loseManifests, table);
    }

    //check file
    CloseableIterator<FileScanTask> iterator =
        table.newScan().useSnapshot(currentSnapshot.snapshotId()).planFiles().iterator();
    List<ContentFile> lostFile = new ArrayList<>();
    while (iterator.hasNext()) {
      FileScanTask fileScanTask = iterator.next();
      if (!exists(fileScanTask.file().path().toString())) {
        lostFile.add(fileScanTask.file());
      }
      for (DeleteFile deleteFile: fileScanTask.deletes()) {
        if (!exists(deleteFile.path().toString())) {
          lostFile.add(deleteFile);
        }
      }
    }
    if (lostFile.size() != 0) {
      return TableAnalyzeResult.filesLose(identifier, lostFile, table);
    }

    //table is available
    return TableAnalyzeResult.available(identifier);
  }

  private boolean exists(String path) {
    return fileExistCache.computeIfAbsent(path, p -> io.exists(path));
  }
}
