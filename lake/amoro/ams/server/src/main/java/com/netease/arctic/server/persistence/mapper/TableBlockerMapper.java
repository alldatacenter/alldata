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

package com.netease.arctic.server.persistence.mapper;

import com.netease.arctic.server.persistence.converter.List2StringConverter;
import com.netease.arctic.server.persistence.converter.Long2TsConverter;
import com.netease.arctic.server.persistence.converter.Map2StringConverter;
import com.netease.arctic.server.table.ServerTableIdentifier;
import com.netease.arctic.server.table.blocker.TableBlocker;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface TableBlockerMapper {
  String TABLE_NAME = "table_blocker";

  @Select("SELECT blocker_id,catalog_name,db_name,table_name,operations,create_time," +
      "expiration_time,properties FROM " + TABLE_NAME + " " +
      "WHERE catalog_name = #{tableIdentifier.catalog} AND db_name = #{tableIdentifier.database} " +
      "AND table_name = #{tableIdentifier.tableName} " +
      "AND expiration_time > #{now, typeHandler=com.netease.arctic.server.persistence.converter.Long2TsConverter}")
  @Results({
      @Result(property = "blockerId", column = "blocker_id"),
      @Result(property = "tableIdentifier.catalog", column = "catalog_name"),
      @Result(property = "tableIdentifier.database", column = "db_name"),
      @Result(property = "tableIdentifier.tableName", column = "table_name"),
      @Result(property = "operations", column = "operations",
          typeHandler = List2StringConverter.class),
      @Result(property = "createTime", column = "create_time",
          typeHandler = Long2TsConverter.class),
      @Result(property = "expirationTime", column = "expiration_time",
          typeHandler = Long2TsConverter.class),
      @Result(property = "properties", column = "properties",
          typeHandler = Map2StringConverter.class)
  })
  List<TableBlocker> selectBlockers(@Param("tableIdentifier") ServerTableIdentifier tableIdentifier,
                                    @Param("now") long now);

  @Select("SELECT blocker_id,catalog_name,db_name,table_name,operations,create_time," +
      "expiration_time,properties FROM " + TABLE_NAME + " " +
      "WHERE blocker_id = #{blockerId} " +
      "AND expiration_time > #{now, typeHandler=com.netease.arctic.server.persistence.converter.Long2TsConverter}")
  @Results({
      @Result(property = "blockerId", column = "blocker_id"),
      @Result(property = "tableIdentifier.catalog", column = "catalog_name"),
      @Result(property = "tableIdentifier.database", column = "db_name"),
      @Result(property = "tableIdentifier.tableName", column = "table_name"),
      @Result(property = "operations", column = "operations",
          typeHandler = List2StringConverter.class),
      @Result(property = "createTime", column = "create_time",
          typeHandler = Long2TsConverter.class),
      @Result(property = "expirationTime", column = "expiration_time",
          typeHandler = Long2TsConverter.class),
      @Result(property = "properties", column = "properties",
          typeHandler = Map2StringConverter.class)
  })
  TableBlocker selectBlocker(@Param("blockerId") long blockerId, @Param("now") long now);

  @Insert("INSERT INTO " + TABLE_NAME + " (catalog_name,db_name,table_name,operations,create_time," +
      "expiration_time,properties) VALUES (" +
      "#{blocker.tableIdentifier.catalog}," +
      "#{blocker.tableIdentifier.database}," +
      "#{blocker.tableIdentifier.tableName}," +
      "#{blocker.operations,typeHandler=com.netease.arctic.server.persistence.converter.List2StringConverter}," +
      "#{blocker.createTime,typeHandler=com.netease.arctic.server.persistence.converter.Long2TsConverter}," +
      "#{blocker.expirationTime,typeHandler=com.netease.arctic.server.persistence.converter.Long2TsConverter}," +
      "#{blocker.properties,typeHandler=com.netease.arctic.server.persistence.converter.Map2StringConverter}" +
      ")")
  @Options(useGeneratedKeys = true, keyProperty = "blocker.blockerId")
  void insertBlocker(@Param("blocker") TableBlocker blocker);

  @Update("UPDATE " + TABLE_NAME + " SET " +
      "expiration_time = #{expirationTime, " +
      "typeHandler=com.netease.arctic.server.persistence.converter.Long2TsConverter} " +
      "WHERE blocker_id = #{blockerId}")
  void updateBlockerExpirationTime(@Param("blockerId") long blockerId, @Param("expirationTime") long expirationTime);

  @Delete("DELETE FROM " + TABLE_NAME + " " +
      "WHERE blocker_id = #{blockerId}")
  void deleteBlocker(@Param("blockerId") long blockerId);

  @Delete("DELETE FROM " + TABLE_NAME + " " +
      "WHERE catalog_name = #{tableIdentifier.catalog} AND db_name = #{tableIdentifier.database} " +
      "AND table_name = #{tableIdentifier.tableName} " +
      "AND expiration_time <= #{now, typeHandler=com.netease.arctic.server.persistence.converter.Long2TsConverter}")
  int deleteExpiredBlockers(@Param("tableIdentifier") ServerTableIdentifier tableIdentifier, @Param("now") long now);

  @Delete("DELETE FROM " + TABLE_NAME + " " +
      "WHERE catalog_name = #{tableIdentifier.catalog} AND db_name = #{tableIdentifier.database} " +
      "AND table_name = #{tableIdentifier.tableName}")
  int deleteBlockers(@Param("tableIdentifier") ServerTableIdentifier tableIdentifier);
}
