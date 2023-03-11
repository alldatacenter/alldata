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

import com.netease.arctic.ams.server.model.ApiTokens;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;

public interface ApiTokensMapper {
  String TABLE_NAME = "api_tokens";

  @Select("select secret from " +
          TABLE_NAME + " where apikey = #{apikey}")
  String getSecretBykey(String apikey);

  @Insert("insert into " + TABLE_NAME + " (apikey,secret,apply_time) values(#{apiTokens.apikey}," +
          "#{apiTokens.secret},#{apiTokens.applyTime})")
  void insert(ApiTokens apiTokens);

  @Insert("delete from " + TABLE_NAME + " where id = #{id}")
  void delToken(Integer id);

}
