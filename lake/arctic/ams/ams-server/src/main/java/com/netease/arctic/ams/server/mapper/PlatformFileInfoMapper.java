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

import com.netease.arctic.ams.server.model.PlatformFileInfo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface PlatformFileInfoMapper {
  String TABLE_NAME = "platform_file_info";

  /**
   * add a file with content encoded by base64
   */
  @Insert("insert into " + TABLE_NAME + "(id,file_name,file_content_b64)" +
          "values(#{fileInfo.fileId},#{fileInfo.fileName},#{fileInfo.fileContent})")
  @Options(useGeneratedKeys = true, keyProperty = "fileInfo.fileId", keyColumn = "id")
  void addFile(@Param("fileInfo") PlatformFileInfo platformFileInfo);


  // get file content encoded by base64 by fileId
  @Select("select file_content_b64 from " + TABLE_NAME + " where id=#{fileId}")
  String getFileById(@Param("fileId") Integer fileId);

  // get fileId by content which is encoded with base64. ** caution: for derby only
  @Select("select id from " + TABLE_NAME + " where file_content_b64=#{content} limit 1")
  Integer getFileId(@Param("content") String content);
}
