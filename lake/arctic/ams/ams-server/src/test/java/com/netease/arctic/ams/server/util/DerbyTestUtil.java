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

package com.netease.arctic.ams.server.util;

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
import com.netease.arctic.ams.server.service.IJDBCService;
import com.netease.arctic.ams.server.utils.JDBCSqlSessionFactoryProvider;
import com.netease.arctic.ams.server.utils.SqlSessionFactoryUtil;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.pool2.impl.BaseObjectPoolConfig;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DerbyTestUtil extends IJDBCService {
  public static volatile SqlSessionFactory sqlSessionFactory;
  public static String path = System.getProperty("user.dir") +
      "/src/test/java/com/netease/arctic/ams/server/derby/".replace("/", File.separator);
  public static String db = "mydb1";

  public void createTestTable() throws Exception {
    try (SqlSession sqlSession = getSqlSession(true)) {
      String query = "SELECT TRUE FROM SYS.SYSTABLES WHERE TABLENAME = ?";
      Connection connection = sqlSession.getConnection();
      PreparedStatement ps = connection.prepareStatement(query);
      ps.setString(1, "CATALOG_METADATA");
      ResultSet rs = ps.executeQuery();
      if (!rs.next() || !rs.getBoolean(1)) {
        // Table does NOT exist ... create it
        ScriptRunner runner = new ScriptRunner(connection);
        runner.runScript(new InputStreamReader(new FileInputStream(getClass().getResource("/sql/derby/ams-init.sql")
            .getFile()), "UTF-8"));
      }
    }
  }

  public static SqlSessionFactory get() {
    if (sqlSessionFactory == null) {
      synchronized (JDBCSqlSessionFactoryProvider.class) {
        if (sqlSessionFactory == null) {
          TransactionFactory transactionFactory = new JdbcTransactionFactory();
          BasicDataSource dataSource = new BasicDataSource();
          if (new File(path + db).exists()) {
            dataSource.setUrl("jdbc:derby:" + path + db + ";");
          } else {
            dataSource.setUrl("jdbc:derby:" + path + db + ";create=true");
          }
          dataSource.setDriverClassName("org.apache.derby.jdbc.EmbeddedDriver");
          dataSource.setDefaultAutoCommit(true);
          dataSource.setMaxIdle(8);
          dataSource.setMinIdle(0);
          dataSource.setLogAbandoned(true);
          dataSource.setRemoveAbandonedTimeout(60);
          dataSource.setTimeBetweenEvictionRunsMillis(
              BaseObjectPoolConfig.DEFAULT_TIME_BETWEEN_EVICTION_RUNS.toMillis());
          dataSource.setTestOnBorrow(BaseObjectPoolConfig.DEFAULT_TEST_ON_BORROW);
          dataSource.setTestWhileIdle(BaseObjectPoolConfig.DEFAULT_TEST_WHILE_IDLE);
          dataSource.setMinEvictableIdleTimeMillis(1000);
          dataSource.setNumTestsPerEvictionRun(BaseObjectPoolConfig.DEFAULT_NUM_TESTS_PER_EVICTION_RUN);
          dataSource.setTestOnReturn(BaseObjectPoolConfig.DEFAULT_TEST_ON_RETURN);
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
          configuration.addMapper(DerbyContainerMetadataMapper.class);
          configuration.addMapper(DerbyFileInfoCacheMapper.class);
          configuration.addMapper(DerbyCatalogMetadataMapper.class);
          configuration.addMapper(DerbyTableMetadataMapper.class);
          configuration.addMapper(DerbyOptimizeTasksMapper.class);
          configuration.addMapper(DDLRecordMapper.class);
          configuration.addMapper(PlatformFileInfoMapper.class);
          configuration.addMapper(DerbyPlatformFileInfoMapper.class);
          configuration.addMapper(TableBlockerMapper.class);
          sqlSessionFactory = SqlSessionFactoryUtil.getSqlSessionFactory(configuration);
        }
      }
    }

    return sqlSessionFactory;
  }

  public static void deleteIfExists(String fileString) throws IOException {
    File file = new File(fileString);
    if (file.exists()) {
      if (!file.isFile()) {
        File[] files = file.listFiles();
        if (files != null && files.length > 0) {
          for (File temp : files) {
            deleteIfExists(String.valueOf(temp));
          }
        }
      }
      if (!file.delete()) {
        System.out.println("Delete file failure,path:" + file.getAbsolutePath());
      }
    }
  }
}
