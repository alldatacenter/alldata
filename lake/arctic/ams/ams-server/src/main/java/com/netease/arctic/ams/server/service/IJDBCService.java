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

package com.netease.arctic.ams.server.service;

import com.netease.arctic.ams.server.ArcticMetaStore;
import com.netease.arctic.ams.server.config.ArcticMetaStoreConf;
import com.netease.arctic.ams.server.utils.JDBCSqlSessionFactoryProvider;
import org.apache.ibatis.io.ResolverUtil;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.TransactionIsolationLevel;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class IJDBCService {
  SqlSessionFactory sqlSessionFactory;

  final ConcurrentHashMap<Class<?>, Class<?>> mapperIntfMap = new ConcurrentHashMap<>();

  public IJDBCService() {
    this.sqlSessionFactory = JDBCSqlSessionFactoryProvider.get();
  }

  public SqlSessionFactory getSqlSessionFactory() {
    return this.sqlSessionFactory;
  }

  public SqlSession getSqlSession(boolean autoCommit) {
    if (autoCommit) {
      return this.getSqlSessionFactory().openSession(true);
    } else {
      // when openSession with TransactionIsolationLevel, autoCommit is always set to false
      return this.getSqlSessionFactory().openSession(TransactionIsolationLevel.READ_COMMITTED);
    }
  }

  public <T> T getMapper(SqlSession sqlSession, Class<T> type) {
    if (ArcticMetaStore.conf.getString(ArcticMetaStoreConf.DB_TYPE).equals("derby")) {
      if (mapperIntfMap.get(type) == null) {
        String packageName = "com.netease.arctic.ams.server.mapper.derby";
        ResolverUtil<T> resolverUtil = new ResolverUtil<>();
        resolverUtil.find(new ResolverUtil.IsA(type), packageName);
        Set<Class<? extends T>> mapperSet = resolverUtil.getClasses();
        if (mapperSet != null && mapperSet.size() > 1) {
          throw new RuntimeException(String.format("Find multi %s in %s", type.getName(), packageName));
        }

        if (mapperSet == null || mapperSet.size() == 0) {
          mapperIntfMap.put(type, type);
        } else {
          mapperIntfMap.put(type, mapperSet.stream().findFirst().get());
        }
      }
      return sqlSession.getMapper((Class<T>)mapperIntfMap.get(type));
    } else {
      return sqlSession.getMapper(type);
    }
  }
}
