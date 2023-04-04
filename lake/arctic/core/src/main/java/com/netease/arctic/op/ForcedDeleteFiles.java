/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.op;

import org.apache.iceberg.DataFile;
import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.HasTableOperations;
import org.apache.iceberg.PublicMergingSnapshotProducer;
import org.apache.iceberg.Table;
import org.apache.iceberg.TableOperations;

import static org.apache.iceberg.DataOperations.DELETE;

/**
 * Can not confirm the consistency of data, just forced delete {@link DataFile} and {@link DeleteFile}.
 */
public class ForcedDeleteFiles extends PublicMergingSnapshotProducer<ForcedDeleteFiles> {

  protected ForcedDeleteFiles(String tableName, TableOperations ops) {
    super(tableName, ops);
  }

  public static ForcedDeleteFiles of(Table table) {
    if (table instanceof HasTableOperations) {
      return new ForcedDeleteFiles(table.name(), ((HasTableOperations) table).operations());
    }
    throw new IllegalArgumentException("only support HasTableOperations table");
  }

  @Override
  protected ForcedDeleteFiles self() {
    return this;
  }

  @Override
  protected String operation() {
    return DELETE;
  }

  public void delete(DataFile dataFile) {
    super.delete(dataFile);
  }

  public void delete(DeleteFile deleteFile) {
    super.delete(deleteFile);
  }
}
