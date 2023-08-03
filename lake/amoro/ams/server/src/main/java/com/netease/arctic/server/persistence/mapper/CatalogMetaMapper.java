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

import com.netease.arctic.ams.api.CatalogMeta;
import com.netease.arctic.server.persistence.converter.Map2StringConverter;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface CatalogMetaMapper {
  String TABLE_NAME = "catalog_metadata";

  @Select("SELECT catalog_name, catalog_metastore, storage_configs, auth_configs, catalog_properties FROM " +
      TABLE_NAME)
  @Results({
      @Result(property = "catalogName", column = "catalog_name"),
      @Result(property = "catalogType", column = "catalog_metastore"),
      @Result(property = "storageConfigs", column = "storage_configs", typeHandler = Map2StringConverter.class),
      @Result(property = "authConfigs", column = "auth_configs", typeHandler = Map2StringConverter.class),
      @Result(property = "catalogProperties", column = "catalog_properties", typeHandler = Map2StringConverter.class)
  })
  List<CatalogMeta> getCatalogs();

  @Select("SELECT catalog_name, catalog_metastore, storage_configs, auth_configs, catalog_properties FROM " +
      TABLE_NAME + " WHERE catalog_name = #{catalogName}")
  @Results({
      @Result(property = "catalogName", column = "catalog_name"),
      @Result(property = "catalogType", column = "catalog_type"),
      @Result(property = "storageConfigs", column = "storage_configs", typeHandler = Map2StringConverter.class),
      @Result(property = "authConfigs", column = "auth_configs", typeHandler = Map2StringConverter.class),
      @Result(property = "catalogProperties", column = "catalog_properties", typeHandler = Map2StringConverter.class)
  })
  List<CatalogMeta> getCatalog(@Param("catalogName") String catalogName);

  @Insert("INSERT INTO " + TABLE_NAME +
      " (catalog_name, catalog_metastore, storage_configs, auth_configs, catalog_properties)" +
      " VALUES (#{catalogMeta.catalogName}, #{catalogMeta.catalogType}," +
      " #{catalogMeta.storageConfigs," +
      " typeHandler=com.netease.arctic.server.persistence.converter.Map2StringConverter}," +
      " #{catalogMeta.authConfigs, typeHandler=com.netease.arctic.server.persistence.converter.Map2StringConverter}," +
      " #{catalogMeta.catalogProperties," +
      " typeHandler=com.netease.arctic.server.persistence.converter.Map2StringConverter})")
  void insertCatalog(@Param("catalogMeta") CatalogMeta catalogMeta);

  @Delete("DELETE FROM " + TABLE_NAME + " WHERE catalog_name = #{catalogName} AND database_count = 0" +
      " AND table_count = 0")
  int deleteCatalog(@Param("catalogName") String catalogName);

  @Update("UPDATE " + TABLE_NAME + " SET catalog_metastore = #{catalogMeta.catalogType}," +
      " storage_configs = #{catalogMeta.storageConfigs," +
      " typeHandler=com.netease.arctic.server.persistence.converter.Map2StringConverter}," +
      " auth_configs=#{catalogMeta.authConfigs," +
      " typeHandler=com.netease.arctic.server.persistence.converter.Map2StringConverter}," +
      " catalog_properties=#{catalogMeta.catalogProperties," +
      " typeHandler=com.netease.arctic.server.persistence.converter.Map2StringConverter}" +
      " WHERE catalog_name = #{catalogMeta.catalogName}")
  Integer updateCatalog(@Param("catalogMeta") CatalogMeta catalogMeta);

  @Select("SELECT table_count FROM " + TABLE_NAME + " WHERE catalog_name = #{catalogName}")
  Integer selectTableCount(@Param("catalogName") String catalogName);

  @Update("UPDATE " + TABLE_NAME + " SET table_count = table_count + #{tableCount} WHERE catalog_name = #{catalogName}")
  Integer incTableCount(@Param("tableCount") Integer tableCount, @Param("catalogName") String catalogName);

  @Update("UPDATE " + TABLE_NAME + " SET database_count = database_count + #{databaseCount}" +
      " WHERE catalog_name = #{catalogName}")
  Integer incDatabaseCount(@Param("databaseCount") Integer databaseCount, @Param("catalogName") String catalogName);

  @Update("UPDATE " + TABLE_NAME + " SET table_count = table_count - #{tableCount} WHERE catalog_name = #{catalogName}")
  Integer decTableCount(@Param("tableCount") Integer tableCount, @Param("catalogName") String catalogName);

  @Update("UPDATE " + TABLE_NAME + " SET database_count = database_count - #{databaseCount}" +
      " WHERE catalog_name = #{catalogName}")
  Integer decDatabaseCount(@Param("databaseCount") Integer databaseCount, @Param("catalogName") String catalogName);
}
