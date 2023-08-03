package com.netease.arctic.server.table.executor;

import com.netease.arctic.server.ArcticManagementConf;
import com.netease.arctic.server.table.TableManager;
import com.netease.arctic.server.utils.Configurations;

public class AsyncTableExecutors {

  private static final AsyncTableExecutors instance = new AsyncTableExecutors();
  private SnapshotsExpiringExecutor snapshotsExpiringExecutor;
  private TableRuntimeRefreshExecutor tableRefreshingExecutor;
  private OrphanFilesCleaningExecutor orphanFilesCleaningExecutor;
  private BlockerExpiringExecutor blockerExpiringExecutor;
  private OptimizingCommitExecutor optimizingCommitExecutor;
  private OptimizingExpiringExecutor optimizingExpiringExecutor;
  private HiveCommitSyncExecutor hiveCommitSyncExecutor;

  public static AsyncTableExecutors getInstance() {
    return instance;
  }

  public void setup(TableManager tableManager, Configurations conf) {
    if (conf.getBoolean(ArcticManagementConf.EXPIRE_SNAPSHOTS_ENABLED)) {
      this.snapshotsExpiringExecutor = new SnapshotsExpiringExecutor(tableManager,
          conf.getInteger(ArcticManagementConf.EXPIRE_SNAPSHOTS_THREAD_COUNT));
    }
    if (conf.getBoolean(ArcticManagementConf.CLEAN_ORPHAN_FILES_ENABLED)) {
      this.orphanFilesCleaningExecutor = new OrphanFilesCleaningExecutor(tableManager,
          conf.getInteger(ArcticManagementConf.CLEAN_ORPHAN_FILES_THREAD_COUNT));
    }
    this.optimizingCommitExecutor = new OptimizingCommitExecutor(tableManager,
        conf.getInteger(ArcticManagementConf.OPTIMIZING_COMMIT_THREAD_COUNT));
    this.optimizingExpiringExecutor = new OptimizingExpiringExecutor(tableManager);
    this.blockerExpiringExecutor = new BlockerExpiringExecutor(tableManager);
    if (conf.getBoolean(ArcticManagementConf.SYNC_HIVE_TABLES_ENABLED)) {
      this.hiveCommitSyncExecutor = new HiveCommitSyncExecutor(tableManager,
          conf.getInteger(ArcticManagementConf.SYNC_HIVE_TABLES_THREAD_COUNT));
    }
    this.tableRefreshingExecutor = new TableRuntimeRefreshExecutor(tableManager,
        conf.getInteger(ArcticManagementConf.REFRESH_TABLES_THREAD_COUNT),
        conf.getLong(ArcticManagementConf.REFRESH_TABLES_INTERVAL));
  }

  public SnapshotsExpiringExecutor getSnapshotsExpiringExecutor() {
    return snapshotsExpiringExecutor;
  }

  public TableRuntimeRefreshExecutor getTableRefreshingExecutor() {
    return tableRefreshingExecutor;
  }

  public OrphanFilesCleaningExecutor getOrphanFilesCleaningExecutor() {
    return orphanFilesCleaningExecutor;
  }

  public BlockerExpiringExecutor getBlockerExpiringExecutor() {
    return blockerExpiringExecutor;
  }

  public OptimizingCommitExecutor getOptimizingCommitExecutor() {
    return optimizingCommitExecutor;
  }

  public OptimizingExpiringExecutor getOptimizingExpiringExecutor() {
    return optimizingExpiringExecutor;
  }

  public HiveCommitSyncExecutor getHiveCommitSyncExecutor() {
    return hiveCommitSyncExecutor;
  }
}