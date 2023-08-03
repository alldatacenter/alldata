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

import java.util.Iterator;
import java.util.List;

public class SimpleResultSet implements TerminalSession.ResultSet {

  List<String> columns;
  Iterator<Object[]> it;
  Object[] current;

  public SimpleResultSet(List<String> columns, List<Object[]> rows) {
    this.columns = columns;
    it = rows.iterator();
  }

  @Override
  public List<String> columns() {
    return this.columns;
  }

  @Override
  public boolean next() {
    if (it.hasNext()) {
      current = it.next();
      return true;
    }

    return false;
  }

  @Override
  public Object[] rowData() {
    return current;
  }

  @Override
  public void close() {

  }
}
