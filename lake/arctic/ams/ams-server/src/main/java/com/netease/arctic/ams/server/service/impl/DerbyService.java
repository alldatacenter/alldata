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

import com.netease.arctic.ams.server.service.IJDBCService;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DerbyService extends IJDBCService {
  public static final Logger LOG = LoggerFactory.getLogger(DerbyService.class);

  public void createTable() throws Exception {
    try (SqlSession sqlSession = getSqlSession(true)) {
      Connection connection = sqlSession.getConnection();
      String query = "SELECT TRUE FROM SYS.SYSTABLES WHERE TABLENAME = ?";
      PreparedStatement ps = connection.prepareStatement(query);
      ps.setString(1, "CATALOG_METADATA");
      ResultSet rs = ps.executeQuery();
      if (!rs.next() || !rs.getBoolean(1)) {
        // Table does NOT exist ... create it
        ScriptRunner runner = new ScriptRunner(connection);
        runner.runScript(new InputStreamReader(new FileInputStream(getDerbyInitSqlDir()), "UTF-8"));
      }
    } catch (Exception e) {
      LOG.error("create derby table error", e);
      throw e;
    }
  }

  private static String getDerbyInitSqlDir() {
    String derbyInitSqlDir = System.getProperty("derby.init.sql.dir");
    if (derbyInitSqlDir == null) {
      return System.getProperty("user.dir") + "/conf/derby/ams-init.sql".replace("/", File.separator);
    } else {
      return derbyInitSqlDir + "/ams-init.sql".replace("/", File.separator);
    }
  }
}

