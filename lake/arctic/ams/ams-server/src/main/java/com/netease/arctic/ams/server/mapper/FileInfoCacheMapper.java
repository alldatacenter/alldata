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

import com.netease.arctic.ams.api.DataFileInfo;
import com.netease.arctic.ams.api.TableIdentifier;
import com.netease.arctic.ams.server.model.AMSDataFileInfo;
import com.netease.arctic.ams.server.model.CacheFileInfo;
import com.netease.arctic.ams.server.model.PartitionBaseInfo;
import com.netease.arctic.ams.server.model.PartitionFileBaseInfo;
import com.netease.arctic.ams.server.mybatis.Long2TsConvertor;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface FileInfoCacheMapper {
  String TABLE_NAME = "file_info_cache";

  @Insert("insert into " + TABLE_NAME + " (table_identifier, add_snapshot_id, parent_snapshot_id, delete_snapshot_id," +
      " inner_table, file_path, primary_key_md5, file_type, producer, file_size, file_mask, file_index, spec_id, " +
      "record_count,action, partition_name, commit_time, add_snapshot_sequence) values(" +
      "#{cacheFileInfo.tableIdentifier, typeHandler=com.netease.arctic.ams.server.mybatis" +
      ".TableIdentifier2StringConverter}, " +
      "#{cacheFileInfo.addSnapshotId}, #{cacheFileInfo.parentSnapshotId}, #{cacheFileInfo" +
      ".deleteSnapshotId}, #{cacheFileInfo.innerTable}, #{cacheFileInfo.filePath}, #{cacheFileInfo.primaryKeyMd5}, " +
      "#{cacheFileInfo.fileType}, #{cacheFileInfo.producer}, " +
      "#{cacheFileInfo.fileSize}, #{cacheFileInfo.fileMask}, #{cacheFileInfo.fileIndex}, #{cacheFileInfo.specId}, " +
      "#{cacheFileInfo.recordCount}, #{cacheFileInfo.action}, #{cacheFileInfo.partitionName}, #{cacheFileInfo" +
      ".commitTime,typeHandler=com.netease.arctic.ams.server.mybatis.Long2TsConvertor}, " +
      "#{cacheFileInfo.addSnapshotSequence})")
  void insertCache(@Param("cacheFileInfo") CacheFileInfo cacheFileInfo);

  @Update("update " + TABLE_NAME + " set delete_snapshot_id = #{cache.deleteSnapshotId} where primary_key_md5 = " +
      "#{cache.primaryKeyMd5}")
  void updateCache(@Param("cache") CacheFileInfo cache);

  @Select("select file_path, partition_name, file_type, file_size, commit_time, case delete_snapshot_id when " +
          "#{transactionId} then 'remove' else 'add' end as operation from " + TABLE_NAME +
          " where table_identifier = #{tableIdentifier, typeHandler=com.netease.arctic.ams.server.mybatis" +
          ".TableIdentifier2StringConverter} and (add_snapshot_id = #{transactionId} or delete_snapshot_id = " +
          "#{transactionId}) order by commit_time desc")
  @Results({
          @Result(column = "file_path", property = "path"),
          @Result(column = "partition_name", property = "partition"),
          @Result(column = "file_type", property = "type"),
          @Result(column = "file_size", property = "fileSize"),
          @Result(column = "operation", property = "operation"),
          @Result(column = "commit_time", property = "commitTime",
                  typeHandler = Long2TsConvertor.class),
          @Result(column = "add_snapshot_sequence", property = "sequence")
  })
  List<AMSDataFileInfo> getDatafilesInfo(
          @Param("tableIdentifier") TableIdentifier tableIdentifier,
          @Param("transactionId") Long transactionId);

  @Select("select file_path, file_type, file_size, file_mask, file_index, record_count, spec_id, partition_name," +
      " commit_time, add_snapshot_sequence from " + TABLE_NAME + " where table_identifier = #{tableIdentifier," +
      " typeHandler=com.netease.arctic.ams.server.mybatis.TableIdentifier2StringConverter} and" +
      " inner_table = #{innerTable} and delete_snapshot_id is null")
  @Results({
          @Result(column = "file_path", property = "path"),
          @Result(column = "file_type", property = "type"),
          @Result(column = "file_size", property = "size"),
          @Result(column = "file_mask", property = "mask"),
          @Result(column = "file_index", property = "index"),
          @Result(column = "record_count", property = "recordCount"),
          @Result(column = "spec_id", property = "specId"),
          @Result(column = "partition_name", property = "partition"),
          @Result(column = "commit_time", property = "commitTime",
                  typeHandler = Long2TsConvertor.class),
          @Result(column = "add_snapshot_sequence", property = "sequence")
  })
  List<DataFileInfo> getOptimizeDatafiles(
          @Param("tableIdentifier") TableIdentifier tableIdentifier,
          @Param("innerTable") String innerTable);

  /**
   * Get files with snapshot, like Time Travel.
   * These files should:
   * - file's add_snapshot_sequence <= this_snapshot_sequence
   * - is not deleted or is deleted by older snapshot (file's delete_snapshot_sequence > this_snapshot_sequence)
   *
   * @param tableIdentifier -
   * @param innerTable      -
   * @param sequence        - sequence of this snapshot
   * @return files of this snapshot
   */
  @Select("select file_path, file_type, file_size, file_mask, file_index, record_count, spec_id, partition_name," +
      " commit_time, add_snapshot_sequence from " + TABLE_NAME + " where table_identifier = #{tableIdentifier," +
      " typeHandler=com.netease.arctic.ams.server.mybatis.TableIdentifier2StringConverter} and" +
      " inner_table = #{innerTable} and add_snapshot_sequence <= #{sequence} and (delete_snapshot_id is null or" +
      " delete_snapshot_id in (select snapshot_id from snapshot_info_cache where table_identifier =" +
      " #{tableIdentifier,typeHandler=com.netease.arctic.ams.server.mybatis.TableIdentifier2StringConverter} and" +
      " inner_table = #{innerTable} and snapshot_sequence > #{sequence}))")
  @Results({
      @Result(column = "file_path", property = "path"),
      @Result(column = "file_type", property = "type"),
      @Result(column = "file_size", property = "size"),
      @Result(column = "file_mask", property = "mask"),
      @Result(column = "file_index", property = "index"),
      @Result(column = "record_count", property = "recordCount"),
      @Result(column = "spec_id", property = "specId"),
      @Result(column = "partition_name", property = "partition"),
      @Result(column = "commit_time", property = "commitTime",
          typeHandler = Long2TsConvertor.class),
      @Result(column = "add_snapshot_sequence", property = "sequence")
  })
  List<DataFileInfo> getOptimizeDatafilesWithSnapshot(
      @Param("tableIdentifier") TableIdentifier tableIdentifier,
      @Param("innerTable") String innerTable, @Param("sequence") long sequence);

  @Delete("delete from " + TABLE_NAME + " where delete_snapshot_id is not null and commit_time <  #{expiredTime, " +
      "typeHandler=com.netease.arctic.ams.server.mybatis.Long2TsConvertor}")
  void expireCache(@Param("expiredTime") long expiredTime);

  @Delete("delete from " + TABLE_NAME + " where table_identifier = #{tableIdentifier, typeHandler=com.netease.arctic" +
      ".ams.server.mybatis.TableIdentifier2StringConverter}")
  void deleteTableCache(@Param("tableIdentifier") TableIdentifier tableIdentifier);

  @Delete("delete from " + TABLE_NAME + " where table_identifier = #{tableIdentifier, typeHandler=com.netease.arctic" +
      ".ams.server.mybatis.TableIdentifier2StringConverter} and inner_table = #{innerTable}")
  void deleteInnerTableCache(@Param("tableIdentifier") TableIdentifier tableIdentifier,
      @Param("innerTable") String innerTable);

  @Select("select partition_name, count(1) as file_count, sum(file_size) as size," +
          "max(commit_time) as lastCommitTime from " + TABLE_NAME +
          " where table_identifier = #{tableIdentifier, typeHandler=com.netease.arctic" +
          ".ams.server.mybatis.TableIdentifier2StringConverter} and delete_snapshot_id is null group by " +
          "partition_name order by partition_name desc")
  @Results({
          @Result(column = "partition_name", property = "partition"),
          @Result(column = "file_count", property = "fileCount"),
          @Result(column = "size", property = "fileSize"),
          @Result(column = "lastCommitTime", property = "lastCommitTime")
  })
  List<PartitionBaseInfo> getPartitionBaseInfoList(
          @Param("tableIdentifier") TableIdentifier tableIdentifier);


  @Select("<script>" +
          "select add_snapshot_id, partition_name, file_path, partition_name, " +
          "file_type, file_size, commit_time from " + TABLE_NAME +
          " where table_identifier = #{tableIdentifier, typeHandler=com.netease.arctic.ams.server.mybatis" +
          ".TableIdentifier2StringConverter} and delete_snapshot_id is null " +
          "<if test='partition!=null'> and partition_name = #{partition}</if>" +
          " order by commit_time desc </script>"
  )
  @Results({
          @Result(column = "add_snapshot_id", property = "commitId"),
          @Result(column = "partition_name", property = "partitionName"),
          @Result(column = "file_path", property = "path"),
          @Result(column = "partition_name", property = "partitionName"),
          @Result(column = "file_type", property = "fileType"),
          @Result(column = "file_size", property = "fileSize"),
          @Result(column = "commit_time", property = "commitTime", typeHandler = Long2TsConvertor.class)
  })
  List<PartitionFileBaseInfo> getPartitionFileList(
          @Param("tableIdentifier") TableIdentifier tableIdentifier, @Param("partition") String partition);

  @Select("select file_path, file_type, file_size, file_mask, file_index, record_count, spec_id, partition_name, " +
      "commit_time, add_snapshot_sequence from " + TABLE_NAME + " where table_identifier = #{tableIdentifier, " +
      "typeHandler=com.netease.arctic.ams.server.mybatis.TableIdentifier2StringConverter} and " +
      "commit_time <= #{ttl, typeHandler=com.netease.arctic.ams.server.mybatis.Long2TsConvertor} " +
      "and inner_table = #{innerTable} and " +
      "delete_snapshot_id is null  ")
  @Results({
      @Result(column = "file_path", property = "path"),
      @Result(column = "file_type", property = "type"),
      @Result(column = "file_size", property = "size"),
      @Result(column = "file_mask", property = "mask"),
      @Result(column = "file_index", property = "index"),
      @Result(column = "record_count", property = "recordCount"),
      @Result(column = "spec_id", property = "specId"),
      @Result(column = "partition_name", property = "partition"),
      @Result(column = "commit_time", property = "commitTime",
          typeHandler = Long2TsConvertor.class),
      @Result(column = "add_snapshot_sequence", property = "sequence")
  })
  List<DataFileInfo> getChangeTableTTLDataFiles(@Param("tableIdentifier") TableIdentifier tableIdentifier,
                                                @Param("innerTable") String innerTable,
                                                @Param("ttl") long ttl);
}