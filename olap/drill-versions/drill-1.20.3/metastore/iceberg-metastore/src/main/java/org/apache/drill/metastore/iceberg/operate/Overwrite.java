/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.metastore.iceberg.operate;

import org.apache.iceberg.DataFile;
import org.apache.iceberg.Transaction;
import org.apache.iceberg.expressions.Expression;

/**
 * Iceberg overwrite operation: overwrites data with given data file based on given row filter.
 */
public class Overwrite implements IcebergOperation {

  private final Expression filter;
  private final DataFile dataFile;

  public Overwrite(DataFile dataFile, Expression filter) {
    this.dataFile = dataFile;
    this.filter = filter;
  }

  public Expression filter() {
    return filter;
  }

  public DataFile dataFile() {
    return dataFile;
  }

  @Override
  public void add(Transaction transaction) {
    transaction.newOverwrite()
      .overwriteByRowFilter(filter)
      .addFile(dataFile)
      .validateAddedFilesMatchOverwriteFilter()
      .commit();
  }
}
