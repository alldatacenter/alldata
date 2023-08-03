/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *  *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.server.terminal;

import com.google.common.collect.Lists;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * result of execution a script.
 */
public class ExecutionResult {
  static final SimpleDateFormat patten = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

  List<String> logs = Lists.newArrayList();
  List<StatementResult> results = Lists.newArrayList();

  public synchronized void appendLog(String log) {
    String date = patten.format(new Date());
    this.logs.add(date + " " + log);
  }

  public synchronized void appendLogs(Collection<String> logs) {
    this.logs.addAll(logs);
  }

  public synchronized void appendResult(StatementResult result) {
    this.results.add(result);
  }

  public synchronized List<String> getLogs() {
    return Lists.newArrayList(logs);
  }

  public synchronized List<StatementResult> getResults() {
    return Lists.newArrayList(results);
  }
}
