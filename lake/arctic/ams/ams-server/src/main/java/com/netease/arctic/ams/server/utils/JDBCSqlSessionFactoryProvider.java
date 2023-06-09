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

package com.netease.arctic.ams.server.utils;

import com.netease.arctic.ams.server.ArcticMetaStore;
import com.netease.arctic.ams.server.config.ArcticMetaStoreConf;
import com.netease.arctic.ams.server.mapper.ApiTokensMapper;
import com.netease.arctic.ams.server.mapper.CatalogMetadataMapper;
import com.netease.arctic.ams.server.mapper.ContainerMetadataMapper;
import com.netease.arctic.ams.server.mapper.DDLRecordMapper;
import com.netease.arctic.ams.server.mapper.DatabaseMetadataMapper;
import com.netease.arctic.ams.server.mapper.FileInfoCacheMapper;
import com.netease.arctic.ams.server.mapper.InternalTableFilesMapper;
import com.netease.arctic.ams.server.mapper.OptimizeHistoryMapper;
import com.netease.arctic.ams.server.mapper.OptimizeQueueMapper;
import com.netease.arctic.ams.server.mapper.OptimizeTaskRuntimesMapper;
import com.netease.arctic.ams.server.mapper.OptimizeTasksMapper;
import com.netease.arctic.ams.server.mapper.OptimizerGroupMapper;
import com.netease.arctic.ams.server.mapper.OptimizerMapper;
import com.netease.arctic.ams.server.mapper.PlatformFileInfoMapper;
import com.netease.arctic.ams.server.mapper.SnapInfoCacheMapper;
import com.netease.arctic.ams.server.mapper.TableBlockerMapper;
import com.netease.arctic.ams.server.mapper.TableMetadataMapper;
import com.netease.arctic.ams.server.mapper.TableOptimizeRuntimeMapper;
import com.netease.arctic.ams.server.mapper.TaskHistoryMapper;
import com.netease.arctic.ams.server.mapper.derby.DerbyCatalogMetadataMapper;
import com.netease.arctic.ams.server.mapper.derby.DerbyContainerMetadataMapper;
import com.netease.arctic.ams.server.mapper.derby.DerbyFileInfoCacheMapper;
import com.netease.arctic.ams.server.mapper.derby.DerbyOptimizeTasksMapper;
import com.netease.arctic.ams.server.mapper.derby.DerbyPlatformFileInfoMapper;
import com.netease.arctic.ams.server.mapper.derby.DerbyTableMetadataMapper;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.pool2.impl.BaseObjectPoolConfig;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

import java.time.Duration;

public class JDBCSqlSessionFactoryProvider {

  private static volatile SqlSessionFactory sqlSessionFactory;

  public static SqlSessionFactory get() {
    if (sqlSessionFactory == null) {
      synchronized (JDBCSqlSessionFactoryProvider.class) {
        if (sqlSessionFactory == null) {
          BasicDataSource dataSource = new BasicDataSource();
          if (ArcticMetaStore.conf.getString(ArcticMetaStoreConf.DB_TYPE).equals("derby")) {
            dataSource.setUrl(ArcticMetaStore.conf.getString(ArcticMetaStoreConf.MYBATIS_CONNECTION_URL));
            dataSource.setDriverClassName(
                ArcticMetaStore.conf.getString(ArcticMetaStoreConf.MYBATIS_CONNECTION_DRIVER_CLASS_NAME));
          } else {
            dataSource.setUsername(ArcticMetaStore.conf.getString(ArcticMetaStoreConf.MYBATIS_CONNECTION_USER_NAME));
            dataSource.setPassword(ArcticMetaStore.conf.getString(ArcticMetaStoreConf.MYBATIS_CONNECTION_PASSWORD));
            dataSource.setUrl(ArcticMetaStore.conf.getString(ArcticMetaStoreConf.MYBATIS_CONNECTION_URL));
            dataSource.setDriverClassName(
                ArcticMetaStore.conf.getString(ArcticMetaStoreConf.MYBATIS_CONNECTION_DRIVER_CLASS_NAME));
          }
          dataSource.setDefaultAutoCommit(true);
          dataSource.setMaxTotal(20);
          dataSource.setMaxIdle(16);
          dataSource.setMinIdle(0);
          dataSource.setMaxWaitMillis(1000L);
          dataSource.setLogAbandoned(true);
          dataSource.setRemoveAbandonedOnBorrow(true);
          dataSource.setRemoveAbandonedTimeout(60);
          dataSource.setTimeBetweenEvictionRunsMillis(Duration.ofMillis(10 * 60 * 1000L).toMillis());
          dataSource.setTestOnBorrow(BaseObjectPoolConfig.DEFAULT_TEST_ON_BORROW);
          dataSource.setTestWhileIdle(BaseObjectPoolConfig.DEFAULT_TEST_WHILE_IDLE);
          dataSource.setMinEvictableIdleTimeMillis(1000);
          dataSource.setNumTestsPerEvictionRun(BaseObjectPoolConfig.DEFAULT_NUM_TESTS_PER_EVICTION_RUN);
          dataSource.setTestOnReturn(BaseObjectPoolConfig.DEFAULT_TEST_ON_RETURN);
          dataSource.setSoftMinEvictableIdleTimeMillis(
              BaseObjectPoolConfig.DEFAULT_SOFT_MIN_EVICTABLE_IDLE_TIME.toMillis());
          dataSource.setLifo(BaseObjectPoolConfig.DEFAULT_LIFO);
          TransactionFactory transactionFactory = new JdbcTransactionFactory();
          Environment environment = new Environment("develop", transactionFactory, dataSource);
          Configuration configuration = new Configuration(environment);
          configuration.addMapper(TableMetadataMapper.class);
          configuration.addMapper(OptimizeQueueMapper.class);
          configuration.addMapper(InternalTableFilesMapper.class);
          configuration.addMapper(OptimizeTaskRuntimesMapper.class);
          configuration.addMapper(OptimizeTasksMapper.class);
          configuration.addMapper(TableOptimizeRuntimeMapper.class);
          configuration.addMapper(OptimizeHistoryMapper.class);
          configuration.addMapper(CatalogMetadataMapper.class);
          configuration.addMapper(FileInfoCacheMapper.class);
          configuration.addMapper(TaskHistoryMapper.class);
          configuration.addMapper(SnapInfoCacheMapper.class);
          configuration.addMapper(DatabaseMetadataMapper.class);
          configuration.addMapper(OptimizerMapper.class);
          configuration.addMapper(ContainerMetadataMapper.class);
          configuration.addMapper(OptimizerGroupMapper.class);
          configuration.addMapper(ApiTokensMapper.class);
          configuration.addMapper(DDLRecordMapper.class);
          configuration.addMapper(PlatformFileInfoMapper.class);
          configuration.addMapper(TableBlockerMapper.class);
          if (ArcticMetaStore.conf.getString(ArcticMetaStoreConf.DB_TYPE).equals("derby")) {
            configuration.addMapper(DerbyContainerMetadataMapper.class);
            configuration.addMapper(DerbyFileInfoCacheMapper.class);
            configuration.addMapper(DerbyCatalogMetadataMapper.class);
            configuration.addMapper(DerbyTableMetadataMapper.class);
            configuration.addMapper(DerbyOptimizeTasksMapper.class);
            configuration.addMapper(DerbyPlatformFileInfoMapper.class);
          }
          sqlSessionFactory = SqlSessionFactoryUtil.getSqlSessionFactory(configuration);
        }
      }
    }

    return sqlSessionFactory;
  }
}
