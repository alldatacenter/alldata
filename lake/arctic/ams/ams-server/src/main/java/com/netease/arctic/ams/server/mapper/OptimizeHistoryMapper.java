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

package com.netease.arctic.ams.server.mapper;

import com.netease.arctic.ams.server.model.OptimizeHistory;
import com.netease.arctic.ams.server.mybatis.Long2TsConvertor;
import com.netease.arctic.table.TableIdentifier;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import java.sql.Timestamp;
import java.util.List;

public interface OptimizeHistoryMapper {
  String TABLE_NAME = "optimize_history";

  @Select("select history_id, catalog_name, db_name, table_name, optimize_range, optimize_type, " +
      "visible_time, commit_time, plan_time, duration, total_file_cnt_before, " +
      "total_file_size_before, insert_file_cnt_before, insert_file_size_before, " +
      "delete_file_cnt_before, delete_file_size_before, base_file_cnt_before, " +
      "base_file_size_before, pos_delete_file_cnt_before, pos_delete_file_size_before, " +
      "total_file_cnt_after,total_file_size_after,snapshot_id, total_size, " +
      "added_files, removed_files, added_records, removed_records, added_files_size, " +
      "removed_files_size, total_files, total_records, partition_cnt, partitions, " +
      "partition_optimized_sequence from " + TABLE_NAME + " where " +
      "catalog_name = #{tableIdentifier.catalog} and db_name = #{tableIdentifier.database} " +
      "and table_name = #{tableIdentifier.tableName}")
  @Results({
      @Result(property = "recordId", column = "history_id"),
      @Result(column = "optimize_range", property = "optimizeRange"),
      @Result(column = "optimize_type", property = "optimizeType"),
      @Result(column = "visible_time", property = "visibleTime",
          typeHandler = Long2TsConvertor.class),
      @Result(column = "commit_time", property = "commitTime",
          typeHandler = Long2TsConvertor.class),
      @Result(column = "plan_time", property = "planTime",
          typeHandler = Long2TsConvertor.class),
      @Result(column = "duration", property = "duration"),
      @Result(column = "partition_cnt", property = "partitionCnt"),
      @Result(column = "partitions", property = "partitions"),
      @Result(column = "partition_optimized_sequence", property = "partitionOptimizedSequence"),
      @Result(column = "catalog_name", property = "tableIdentifier.catalog"),
      @Result(column = "db_name", property = "tableIdentifier.database"),
      @Result(column = "table_name", property = "tableIdentifier.tableName"),
      @Result(column = "snapshot_id", property = "snapshotInfo.snapshotId"),
      @Result(column = "total_size", property = "snapshotInfo.totalSize"),
      @Result(column = "added_files", property = "snapshotInfo.addedFiles"),
      @Result(column = "removed_files", property = "snapshotInfo.removedFiles"),
      @Result(column = "added_records", property = "snapshotInfo.addedRecords"),
      @Result(column = "removed_records", property = "snapshotInfo.removedRecords"),
      @Result(column = "added_files_size", property = "snapshotInfo.addedFilesSize"),
      @Result(column = "removed_files_size", property = "snapshotInfo.removedFilesSize"),
      @Result(column = "total_files", property = "snapshotInfo.totalFiles"),
      @Result(column = "total_records", property = "snapshotInfo.totalRecords"),
      @Result(column = "total_file_cnt_before", property = "totalFilesStatBeforeOptimize.fileCnt"),
      @Result(column = "total_file_size_before", property = "totalFilesStatBeforeOptimize.totalSize"),
      @Result(column = "insert_file_cnt_before", property = "insertFilesStatBeforeOptimize.fileCnt"),
      @Result(column = "insert_file_size_before", property = "insertFilesStatBeforeOptimize.totalSize"),
      @Result(column = "delete_file_cnt_before", property = "deleteFilesStatBeforeOptimize.fileCnt"),
      @Result(column = "delete_file_size_before", property = "deleteFilesStatBeforeOptimize.totalSize"),
      @Result(column = "base_file_cnt_before", property = "baseFilesStatBeforeOptimize.fileCnt"),
      @Result(column = "base_file_size_before", property = "baseFilesStatBeforeOptimize.totalSize"),
      @Result(column = "pos_delete_file_cnt_before", property = "posDeleteFilesStatBeforeOptimize.fileCnt"),
      @Result(column = "pos_delete_file_size_before", property = "posDeleteFilesStatBeforeOptimize.totalSize"),
      @Result(column = "total_file_cnt_after", property = "totalFilesStatAfterOptimize.fileCnt"),
      @Result(column = "total_file_size_after", property = "totalFilesStatAfterOptimize.totalSize")
  })
  List<OptimizeHistory> selectOptimizeHistory(@Param("tableIdentifier") TableIdentifier tableIdentifier);

  @Select("select max(commit_time) from " + TABLE_NAME + " where " +
      "catalog_name = #{tableIdentifier.catalog} and db_name = #{tableIdentifier.database} " +
      "and table_name = #{tableIdentifier.tableName}")
  Timestamp latestCommitTime(@Param("tableIdentifier") TableIdentifier tableIdentifier);

  @Delete("delete from " + TABLE_NAME + " where catalog_name = #{tableIdentifier.catalog} and " +
      "db_name = #{tableIdentifier.database} and table_name = #{tableIdentifier.tableName}")
  void deleteOptimizeRecord(@Param("tableIdentifier") TableIdentifier tableIdentifier);

  @Insert(
      "insert into " + TABLE_NAME + " (catalog_name, db_name, " +
          "table_name, optimize_range, optimize_type," +
          "visible_time, commit_time, plan_time, duration, total_file_cnt_before, " +
          "total_file_size_before, insert_file_cnt_before, insert_file_size_before, " +
          "delete_file_cnt_before, delete_file_size_before, base_file_cnt_before, " +
          "base_file_size_before, pos_delete_file_cnt_before, pos_delete_file_size_before, " +
          "total_file_cnt_after, total_file_size_after,snapshot_id, total_size," +
          "added_files, removed_files, added_records, removed_records, added_files_size, " +
          "removed_files_size, total_files, total_records, partition_cnt, partitions, " +
          "partition_optimized_sequence) values (" +
          "#{optimizeHistory.tableIdentifier.catalog}, " +
          "#{optimizeHistory.tableIdentifier.database}, " +
          "#{optimizeHistory.tableIdentifier.tableName}, #{optimizeHistory.optimizeRange}," +
          "#{optimizeHistory.optimizeType}, " +
          "#{optimizeHistory.visibleTime, " +
          "typeHandler=com.netease.arctic.ams.server.mybatis.Long2TsConvertor}, " +
          "#{optimizeHistory.commitTime, " +
          "typeHandler=com.netease.arctic.ams.server.mybatis.Long2TsConvertor}, " +
          "#{optimizeHistory.planTime, " +
          "typeHandler=com.netease.arctic.ams.server.mybatis.Long2TsConvertor}, " +
          "#{optimizeHistory.duration}, " +
          "#{optimizeHistory.totalFilesStatBeforeOptimize.fileCnt}, " +
          "#{optimizeHistory.totalFilesStatBeforeOptimize.totalSize}, " +
          "#{optimizeHistory.insertFilesStatBeforeOptimize.fileCnt}, " +
          "#{optimizeHistory.insertFilesStatBeforeOptimize.totalSize}, " +
          "#{optimizeHistory.deleteFilesStatBeforeOptimize.fileCnt}, " +
          "#{optimizeHistory.deleteFilesStatBeforeOptimize.totalSize}, " +
          "#{optimizeHistory.baseFilesStatBeforeOptimize.fileCnt}, " +
          "#{optimizeHistory.baseFilesStatBeforeOptimize.totalSize}, " +
          "#{optimizeHistory.posDeleteFilesStatBeforeOptimize.fileCnt}, " +
          "#{optimizeHistory.posDeleteFilesStatBeforeOptimize.totalSize}, " +
          "#{optimizeHistory.totalFilesStatAfterOptimize.fileCnt}, " +
          "#{optimizeHistory.totalFilesStatAfterOptimize.totalSize}, " +
          "#{optimizeHistory.snapshotInfo.snapshotId}, " +
          "#{optimizeHistory.snapshotInfo.totalSize}," +
          "#{optimizeHistory.snapshotInfo.addedFiles}, " +
          "#{optimizeHistory.snapshotInfo.removedFiles}, " +
          "#{optimizeHistory.snapshotInfo.addedRecords}, " +
          "#{optimizeHistory.snapshotInfo.removedRecords}, " +
          "#{optimizeHistory.snapshotInfo.addedFilesSize}, " +
          "#{optimizeHistory.snapshotInfo.removedFilesSize}, " +
          "#{optimizeHistory.snapshotInfo.totalFiles}, " +
          "#{optimizeHistory.snapshotInfo.totalRecords}, " +
          "#{optimizeHistory.partitionCnt}, #{optimizeHistory.partitions}, " +
          "#{optimizeHistory.partitionOptimizedSequence})")
  void insertOptimizeHistory(@Param("optimizeHistory") OptimizeHistory optimizeHistory);

  @Select("select max(history_id) from " + TABLE_NAME)
  Long maxOptimizeHistoryId();

  @Delete("delete from " + TABLE_NAME + " where " +
      "catalog_name = #{tableIdentifier.catalog} and db_name = #{tableIdentifier.database} " +
      "and table_name = #{tableIdentifier.tableName} " +
      "and commit_time < #{expireTime, typeHandler=com.netease.arctic.ams.server.mybatis.Long2TsConvertor}")
  void expireOptimizeHistory(@Param("tableIdentifier") TableIdentifier tableIdentifier,
                             @Param("expireTime") long expireTime);
}
