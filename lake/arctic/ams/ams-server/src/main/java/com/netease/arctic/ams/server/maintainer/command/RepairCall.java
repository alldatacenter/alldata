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

import com.netease.arctic.ams.server.maintainer.MaintainerException;
import com.netease.arctic.catalog.ArcticCatalog;
import com.netease.arctic.catalog.CatalogManager;
import com.netease.arctic.hive.table.SupportHive;
import com.netease.arctic.hive.utils.TableTypeUtil;
import com.netease.arctic.io.TableTrashManager;
import com.netease.arctic.op.ForcedDeleteFiles;
import com.netease.arctic.op.ForcedDeleteManifests;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.TableIdentifier;
import org.apache.hadoop.fs.Path;
import org.apache.iceberg.ContentFile;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.ManageSnapshots;
import org.apache.iceberg.ManifestFile;
import org.apache.iceberg.Table;

import java.util.List;

import static com.netease.arctic.ams.server.maintainer.command.RepairWay.SYNC_METADATA;
import static com.netease.arctic.ams.server.maintainer.command.TableAnalyzeResult.ResultType.FILE_LOSE;
import static com.netease.arctic.ams.server.maintainer.command.TableAnalyzeResult.ResultType.MANIFEST_LOST;


public class RepairCall implements CallCommand {

  /**
   * if null then use table name of context
   */
  private String tablePath;

  private RepairWay way;

  /**
   * snapshot id if way is ROLLBACK
   */
  private Long option;

  private CatalogManager catalogManager;

  public RepairCall(
      String tablePath,
      RepairWay way,
      Long option,
      CatalogManager catalogManager) {
    this.tablePath = tablePath;
    this.way = way;
    this.option = option;
    this.catalogManager = catalogManager;
  }

  @Override
  public String call(Context context) throws FullTableNameException {
    TableIdentifier identifier = fullTableName(context, tablePath);

    TableAnalyzeResult tableAnalyzeResult = context.getTableAvailableResult(identifier);
    if (tableAnalyzeResult == null) {
      throw new MaintainerException("Please do analysis first");
    }
    if (tableAnalyzeResult.isOk()) {
      throw new MaintainerException("Table is Ok. No need to repair");
    }

    if (!tableAnalyzeResult.youCan().contains(way)) {
      throw new MaintainerException(String.format("No %s this option", way));
    }

    TableAnalyzeResult.ResultType resultType = tableAnalyzeResult.getDamageType();

    if (resultType == TableAnalyzeResult.ResultType.TABLE_NOT_FOUND) {
      throw new MaintainerException("Table is not found, If you also have data " +
          "you can recreate the table through hive upgrade");
    }

    if (resultType == TableAnalyzeResult.ResultType.METADATA_LOSE) {
      repairMetadataLose(identifier, tableAnalyzeResult);
      return success(identifier, context);
    }

    ArcticCatalog arcticCatalog = catalogManager.getArcticCatalog(identifier.getCatalog());
    ArcticTable arcticTable = arcticCatalog.loadTable(identifier);

    switch (way) {
      case FIND_BACK: {
        TableTrashManager tableTrashManager = tableAnalyzeResult.getTableTrashManager();
        List<String> loseFiles = tableAnalyzeResult.lostFiles();
        for (String path: loseFiles) {
          if (!tableTrashManager.restoreFileFromTrash(path)) {
            throw new MaintainerException(String.format("Can not find back file %s", path));
          }
        }
        return success(identifier, context);
      }
      case SYNC_METADATA: {
        switch (resultType) {
          case MANIFEST_LOST: {
            syncMetadataForManifestLose(tableAnalyzeResult.getArcticTable(), tableAnalyzeResult.getManifestFiles());
            return success(identifier, context);
          }
          case FILE_LOSE: {
            if (TableTypeUtil.isHive(arcticTable)) {
              SupportHive supportHive = (SupportHive) arcticTable;
              if (supportHive.enableSyncHiveDataToArctic()) {
                arcticCatalog.refresh();
                return success(identifier, context);
              }
            }
            syncMetadataForFileLose(tableAnalyzeResult.getArcticTable(), tableAnalyzeResult.getFiles());
            return success(identifier, context);
          }
          default: {
            throw new MaintainerException(String.format("%s only for %s and %s",
                SYNC_METADATA, MANIFEST_LOST, FILE_LOSE));
          }
        }
      }
      case ROLLBACK: {
        rollback(tableAnalyzeResult.getArcticTable(), option);
        return success(identifier, context);
      }
    }
    return success(identifier, context);
  }

  public String success(TableIdentifier identifier, Context context) {
    context.clean(identifier);
    return ok();
  }

  private void repairMetadataLose(TableIdentifier identifier, TableAnalyzeResult tableAnalyzeResult) {
    ArcticCatalog arcticCatalog = catalogManager.getArcticCatalog(identifier.getCatalog());
    RepairTableOperation repairTableOperation = new RepairTableOperation(catalogManager, identifier,
        tableAnalyzeResult.getLocationKind());

    switch (way) {
      case FIND_BACK : {
        List<Path> metadataCandidateFiles =
            repairTableOperation.getMetadataCandidateFiles(tableAnalyzeResult.getMetadataVersion());
        TableTrashManager tableTrashManager = tableAnalyzeResult.getTableTrashManager();
        for (Path path: metadataCandidateFiles) {
          if (tableTrashManager.restoreFileFromTrash(path.toString())) {
            return;
          }
        }
        throw new MaintainerException(String.format("Can not find back, metadata version is %s",
            tableAnalyzeResult.getMetadataVersion()));
      }
      case ROLLBACK_OR_DROP_TABLE: {
        repairTableOperation.removeVersionHit();
        return;
      }
    }
  }

  public void syncMetadataForFileLose(Table table, List<ContentFile> loseFile) {
    ForcedDeleteFiles forcedDeleteFiles = ForcedDeleteFiles.of(table);
    for (ContentFile contentFile: loseFile) {
      if (contentFile instanceof DataFile) {
        forcedDeleteFiles.delete((DataFile) contentFile);
      } else if (contentFile instanceof DeleteFile) {
        forcedDeleteFiles.delete((DeleteFile) contentFile);
      }
    }
    forcedDeleteFiles.commit();
  }

  public void syncMetadataForManifestLose(Table table, List<ManifestFile> loseManifestFile) {
    ForcedDeleteManifests forcedDeleteManifests = ForcedDeleteManifests.of(table);
    for (ManifestFile manifestFile: loseManifestFile) {
      forcedDeleteManifests.deleteManifest(manifestFile);
    }
    forcedDeleteManifests.commit();
  }

  public void rollback(Table table, long snapshot) {
    ManageSnapshots manageSnapshots = table.manageSnapshots();
    manageSnapshots.rollbackTo(snapshot).commit();
  }
}
