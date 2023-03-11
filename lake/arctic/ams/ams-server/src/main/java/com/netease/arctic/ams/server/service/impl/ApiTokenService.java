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

package com.netease.arctic.ams.server.service.impl;

import com.netease.arctic.ams.server.mapper.ApiTokensMapper;
import com.netease.arctic.ams.server.model.ApiTokens;
import com.netease.arctic.ams.server.service.IJDBCService;
import org.apache.ibatis.session.SqlSession;

public class ApiTokenService extends IJDBCService {

  public String getSecretByKey(String key) {
    try (SqlSession sqlSession = getSqlSession(true)) {
      ApiTokensMapper  apiTokensMapper = getMapper(sqlSession, ApiTokensMapper.class);
      String secret = apiTokensMapper.getSecretBykey(key);
      if (secret != null) {
        return secret;
      }
    }

    return null;
  }

  public void insertApiToken(ApiTokens  apiToken) {
    try (SqlSession sqlSession = getSqlSession(true)) {
      ApiTokensMapper  apiTokensMapper = getMapper(sqlSession, ApiTokensMapper.class);
      apiTokensMapper.insert(apiToken);
    }
  }

  public void deleteApiToken(Integer  id) {
    try (SqlSession sqlSession = getSqlSession(true)) {
      ApiTokensMapper  apiTokensMapper = getMapper(sqlSession, ApiTokensMapper.class);
      apiTokensMapper.delToken(id);
    }
  }
}
