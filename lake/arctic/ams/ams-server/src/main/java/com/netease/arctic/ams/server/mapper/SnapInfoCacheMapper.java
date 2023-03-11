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

import com.netease.arctic.ams.api.TableIdentifier;
import com.netease.arctic.ams.server.model.CacheSnapshotInfo;
import com.netease.arctic.ams.server.model.TransactionsOfTable;
import com.netease.arctic.ams.server.mybatis.Long2TsConvertor;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import java.sql.Timestamp;
import java.util.List;

public interface SnapInfoCacheMapper {
  String TABLE_NAME = "snapshot_info_cache";

  @Insert("insert into " + TABLE_NAME + " (table_identifier, snapshot_id, snapshot_sequence, parent_snapshot_id," +
      " action, inner_table, producer, file_size, file_count, commit_time) values(" +
      "#{cacheFileInfo.tableIdentifier, typeHandler=com.netease.arctic.ams.server.mybatis" +
      ".TableIdentifier2StringConverter}," +
      " #{cacheFileInfo.snapshotId}, #{cacheFileInfo.snapshotSequence}, #{cacheFileInfo.parentSnapshotId}," +
      " #{cacheFileInfo.action}," +
      " #{cacheFileInfo.innerTable}, #{cacheFileInfo.producer}, #{cacheFileInfo.fileSize}, #{cacheFileInfo" +
      ".fileCount}, #{cacheFileInfo.commitTime, typeHandler=com.netease.arctic.ams.server.mybatis.Long2TsConvertor})")
  void insertCache(@Param("cacheFileInfo") CacheSnapshotInfo info);

  @Select("select count(1) >0 " +
      "from " + TABLE_NAME +
      " where inner_table=#{type} and table_identifier=#{tableIdentifier, typeHandler=com.netease" +
      ".arctic.ams.server.mybatis.TableIdentifier2StringConverter} and snapshot_id=#{snapshotId} ")
  Boolean snapshotIsCached(
      @Param("tableIdentifier") TableIdentifier tableIdentifier,
      @Param("type") String tableType, @Param("snapshotId") Long snapshotId);

  @Delete("delete from " + TABLE_NAME + " where commit_time < #{expiredTime, typeHandler=com.netease.arctic.ams" +
      ".server.mybatis.Long2TsConvertor} and table_identifier=#{tableIdentifier, typeHandler=com.netease.arctic.ams" +
      ".server.mybatis.TableIdentifier2StringConverter} and inner_table = #{type} and snapshot_id not in (select " +
      "add_snapshot_id from file_info_cache where delete_snapshot_id is null and table_identifier=#{tableIdentifier, " +
      "typeHandler=com.netease.arctic.ams.server.mybatis.TableIdentifier2StringConverter} and inner_table = #{type})")
  void expireCache(
      @Param("expiredTime") long expiredTime, @Param("tableIdentifier") TableIdentifier tableIdentifier,
      @Param("type") String tableType);

  @Delete("delete from " + TABLE_NAME + " where table_identifier = #{tableIdentifier, typeHandler=com.netease.arctic" +
      ".ams.server.mybatis.TableIdentifier2StringConverter}")
  void deleteTableCache(@Param("tableIdentifier") TableIdentifier tableIdentifier);

  @Delete("delete from " + TABLE_NAME + " where table_identifier = #{tableIdentifier, typeHandler=com.netease.arctic" +
      ".ams.server.mybatis.TableIdentifier2StringConverter} and inner_table = #{innerTable}")
  void deleteInnerTableCache(
      @Param("tableIdentifier") TableIdentifier tableIdentifier,
      @Param("innerTable") String innerTable);

  @Select("select max(commit_time) from " + TABLE_NAME + " where table_identifier = " +
      "#{tableIdentifier, typeHandler=com.netease.arctic.ams.server.mybatis.TableIdentifier2StringConverter} and " +
      "inner_table = #{type}")
  Timestamp getCachedMaxTime(
      @Param("tableIdentifier") TableIdentifier tableIdentifier,
      @Param("type") String tableType);

  @Select(
      "select snapshot_id, file_count as cnt, file_size as size, commit_time from " + TABLE_NAME + " where " +
          "table_identifier = #{tableIdentifier, typeHandler=com.netease.arctic.ams.server.mybatis" +
          ".TableIdentifier2StringConverter} and producer!='OPTIMIZE' order by commit_time desc")
  @Results({
      @Result(column = "snapshot_id", property = "transactionId"),
      @Result(column = "cnt", property = "fileCount"),
      @Result(column = "size", property = "fileSize"),
      @Result(column = "commit_time", property = "commitTime",
          typeHandler = Long2TsConvertor.class)
  })
  List<TransactionsOfTable> getTxExcludeOptimize(@Param("tableIdentifier") TableIdentifier tableIdentifier);
}