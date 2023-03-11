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

import com.netease.arctic.ams.api.OptimizeTaskId;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface InternalTableFilesMapper {
  String TABLE_NAME = "optimize_file";
  public static String FILE_SCAN_TASK_FILE_TYPE = "FILE_SCAN_TASK";

  @Select("select file_content from " + TABLE_NAME + " where" +
      " optimize_type = #{optimizeTaskId.type} and " +
      " trace_id = #{optimizeTaskId.traceId} and " +
      " content_type = #{contentType} and " +
      " is_target = #{isTarget}"
  )
  List<byte[]> selectOptimizeTaskFiles(@Param("optimizeTaskId") OptimizeTaskId optimizeTaskId,
                                       @Param("contentType") String contentType,
                                       @Param("isTarget") int isTarget);

  @Insert("insert into " + TABLE_NAME + " (optimize_type, trace_id, content_type, is_target," +
      " file_content) values(" +
      " #{optimizeTaskId.type}, #{optimizeTaskId.traceId}, #{contentType}, #{isTarget}, #{content})")
  void insertOptimizeTaskFile(@Param("optimizeTaskId") OptimizeTaskId optimizeTaskId,
                              @Param("contentType") String contentType,
                              @Param("isTarget") int isTarget,
                              @Param("content") byte[] content);

  @Delete("delete from " + TABLE_NAME + " where" +
      " optimize_type = #{optimizeTaskId.type} and " +
      " trace_id = #{optimizeTaskId.traceId}"
  )
  void deleteOptimizeTaskFile(@Param("optimizeTaskId") OptimizeTaskId optimizeTaskId);

  @Delete("delete from " + TABLE_NAME + " where" +
      " is_target = 1 and " +
      " optimize_type = #{optimizeTaskId.type} and " +
      " trace_id = #{optimizeTaskId.traceId}"
  )
  void deleteOptimizeTaskTargetFile(@Param("optimizeTaskId") OptimizeTaskId optimizeTaskId);
}
