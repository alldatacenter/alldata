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
package org.apache.drill.exec.store.kudu;

import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.exec.ops.OperatorContext;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.VectorAccessible;
import org.apache.kudu.ColumnSchema;
import org.apache.kudu.Schema;
import org.apache.kudu.Type;
import org.apache.kudu.client.Insert;
import org.apache.kudu.client.KuduClient;
import org.apache.kudu.client.KuduSession;
import org.apache.kudu.client.KuduTable;
import org.apache.kudu.client.CreateTableOptions;
import org.apache.kudu.client.OperationResponse;
import org.apache.kudu.client.SessionConfiguration.FlushMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class KuduRecordWriterImpl extends KuduRecordWriter {
  private static final Logger logger = LoggerFactory.getLogger(KuduRecordWriterImpl.class);

  private static final int FLUSH_FREQUENCY = 100;

  private final KuduClient client;
  private final String name;
  private final OperatorContext context;
  private KuduTable table;
  private final KuduSession session;

  private Insert insert;
  private int recordsSinceFlush;

  public KuduRecordWriterImpl(OperatorContext context, KuduClient client, String name) {
    this.client = client;
    this.name = name;
    this.context = context;
    session = client.newSession();
    session.setFlushMode(FlushMode.MANUAL_FLUSH);
  }

  @Override
  public void init(Map<String, String> writerOptions) throws IOException { }

  @Override
  public void updateSchema(VectorAccessible batch) throws IOException {
    BatchSchema schema = batch.getSchema();
    int i = 0;

    try {
      if (!checkForTable(name)) {
        List<ColumnSchema> columns = new ArrayList<>();
        for (MaterializedField f : schema) {
          columns.add(new ColumnSchema.ColumnSchemaBuilder(f.getName(), getType(f.getType()))
              .nullable(f.getType().getMode() == DataMode.OPTIONAL)
              .key(i == 0).build());
          i++;
        }
        Schema kuduSchema = new Schema(columns);
        table = client.createTable(name, kuduSchema, new CreateTableOptions());
      }
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  private boolean checkForTable(String name) throws Exception {
    return !client.getTablesList(name).getTablesList().isEmpty();
  }

  private Type getType(MajorType t) {

    if(t.getMode() == DataMode.REPEATED){
      throw UserException
      .dataWriteError()
      .message("Kudu does not support array types.")
      .build(logger);
    }

    switch (t.getMinorType()) {
    case BIGINT:
      return Type.INT64;
    case BIT:
      return Type.BOOL;
    case FLOAT4:
      return Type.FLOAT;
    case FLOAT8:
      return Type.DOUBLE;
    case INT:
      return Type.INT32;
    case TIMESTAMP:
      return Type.UNIXTIME_MICROS;
    case VARCHAR:
      return Type.STRING;
    case VARBINARY:
      return Type.BINARY;
    default:
      throw UserException
        .dataWriteError()
          .message("Data type: '%s' not supported in Kudu.", t.getMinorType().name())
          .build(logger);
    }
  }

  @Override
  public void startRecord() throws IOException {
    insert = table.newInsert();
    setUp(insert.getRow());
  }

  @Override
  public void endRecord() throws IOException {
    try {
      session.apply(insert);
      recordsSinceFlush++;
      if (recordsSinceFlush == FLUSH_FREQUENCY) {
        flush();
        recordsSinceFlush = 0;
      }
      insert = null;
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public void abort() throws IOException { }

  private void flush() throws IOException {
    try {
      // context.getStats().startWait();
      List<OperationResponse> responses = session.flush();
      for (OperationResponse response : responses) {
        if (response.hasRowError()) {
          throw new IOException(response.getRowError().toString());
        }
      }
    } catch (Exception e) {
      throw new IOException(e);
    } finally {
      // context.getStats().stopWait();
    }
  }

  @Override
  public void cleanup() throws IOException {
    flush();
  }
}
