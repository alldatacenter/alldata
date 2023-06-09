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

package com.netease.arctic.ams.server.mapper.derby;

import com.netease.arctic.ams.server.mapper.FileInfoCacheMapper;
import com.netease.arctic.ams.server.model.CacheFileInfo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

public interface DerbyFileInfoCacheMapper extends FileInfoCacheMapper {
  String TABLE_NAME = "file_info_cache";

  @Insert("INSERT INTO " + TABLE_NAME +
          " (table_identifier, add_snapshot_id, parent_snapshot_id, delete_snapshot_id, inner_table, file_path, " +
          "primary_key_md5, file_type, producer, file_size, file_mask, file_index, spec_id, record_count, action, " +
          "partition_name, commit_time, add_snapshot_sequence) values(#{cacheFileInfo.tableIdentifier, " +
          "typeHandler=com.netease.arctic.ams.server.mybatis.TableIdentifier2StringConverter}, " +
          "#{cacheFileInfo.addSnapshotId}, #{cacheFileInfo.parentSnapshotId}, #{cacheFileInfo" +
          ".deleteSnapshotId, jdbcType=BIGINT}, #{cacheFileInfo.innerTable, jdbcType=VARCHAR}, " +
          "#{cacheFileInfo.filePath, jdbcType=VARCHAR}, #{cacheFileInfo.primaryKeyMd5, jdbcType=VARCHAR}," +
          "#{cacheFileInfo.fileType, jdbcType=VARCHAR}, #{cacheFileInfo.producer, jdbcType=VARCHAR}, " +
          "#{cacheFileInfo.fileSize}, #{cacheFileInfo.fileMask}, #{cacheFileInfo.fileIndex}, " +
          "#{cacheFileInfo.specId}, #{cacheFileInfo.recordCount}, #{cacheFileInfo.action, jdbcType=VARCHAR}, " +
          "#{cacheFileInfo.partitionName, jdbcType=VARCHAR}, " +
          "#{cacheFileInfo.commitTime, typeHandler=com.netease.arctic.ams.server.mybatis.Long2TsConvertor}, " +
          "#{cacheFileInfo.addSnapshotSequence})"
  )
  void insertCache(@Param("cacheFileInfo") CacheFileInfo cacheFileInfo);
}