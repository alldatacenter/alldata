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

package com.netease.arctic.ams.server.controller;

import com.netease.arctic.ams.server.controller.response.OkResponse;
import com.netease.arctic.ams.server.model.LatestSessionInfo;
import com.netease.arctic.ams.server.model.SessionInfo;
import com.netease.arctic.ams.server.model.SqlExample;
import com.netease.arctic.ams.server.model.SqlResult;
import com.netease.arctic.ams.server.service.ServiceContainer;
import com.netease.arctic.ams.server.terminal.TerminalManager;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * terminal controller .
 */
public class TerminalController {
  private static final Logger LOG =
          LoggerFactory.getLogger(TerminalController.class);

  /** get sql example list */
  public static void getExamples(Context ctx) {
    List<String> examples = Arrays.stream(SqlExample.values()).map(SqlExample::getName).collect(Collectors.toList());
    ctx.json(OkResponse.of(examples));
  }

  /** get sql examples*/
  public static void getSqlExamples(Context ctx) {
    String exampleName = ctx.pathParam("exampleName");

    for (SqlExample example : SqlExample.values()) {
      if (example.getName().equals(exampleName)) {
        ctx.json(OkResponse.of(example.getSql()));
        return;
      }
    }
    throw new IllegalArgumentException("can not get example name : " + exampleName);
  }

  /** execute some sql*/
  public static void executeScript(Context ctx) {
    String catalog = ctx.pathParam("catalog");
    Map<String, String> bodyParams = ctx.bodyAsClass(Map.class);
    String sql = bodyParams.get("sql");
    String terminalId = ctx.cookie("JSESSIONID");
    TerminalManager manager = ServiceContainer.getTerminalManager();
    String sessionId = manager.executeScript(terminalId, catalog, sql);

    ctx.json(OkResponse.of(new SessionInfo(sessionId)));
  }

  /** get execute logs of some session */
  public static void getLogs(Context ctx) {
    String sessionId = ctx.pathParamAsClass("sessionId", String.class).get();
    TerminalManager manager = ServiceContainer.getTerminalManager();
    ctx.json(OkResponse.of(manager.getExecutionLog(sessionId)));
  }

  /** get execute result of some session*/
  public static void getSqlResult(Context ctx) {
    String sessionId = ctx.pathParamAsClass("sessionId", String.class).get();
    TerminalManager manager = ServiceContainer.getTerminalManager();
    List<SqlResult> results = manager.getExecutionResults(sessionId);
    ctx.json(OkResponse.of(results));
  }

  /** stop some sql*/
  public static void stopSql(Context ctx) {
    String sessionId = ctx.pathParamAsClass("sessionId", String.class).get();
    TerminalManager manager = ServiceContainer.getTerminalManager();
    manager.cancelExecution(sessionId);
    ctx.json(OkResponse.ok());
  }

  /** get latest sql info **/
  public static void getLatestInfo(Context ctx) {
    String terminalId = ctx.cookie("JSESSIONID");
    TerminalManager manager = ServiceContainer.getTerminalManager();
    LatestSessionInfo sessionInfo = manager.getLastSessionInfo(terminalId);
    ctx.json(OkResponse.of(sessionInfo));
  }
}
