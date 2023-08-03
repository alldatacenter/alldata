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

import com.netease.arctic.server.dashboard.model.ApiTokens;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ApiTokensMapper {
  String TABLE_NAME = "api_tokens";

  @Select("SELECT secret FROM " + TABLE_NAME + " WHERE apikey = #{apikey}")
  String getSecretByKey(String apikey);

  @Insert("INSERT INTO " + TABLE_NAME + " (apikey, secret, apply_time) VALUES(#{apiTokens.apikey}, " +
          "#{apiTokens.secret}, #{apiTokens.applyTime})")
  void insert(@Param("apiTokens") ApiTokens apiTokens);

  @Insert("DELETE FROM " + TABLE_NAME + " WHERE id = #{id}")
  void delToken(@Param("id") Integer id);

}
