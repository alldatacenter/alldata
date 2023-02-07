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
package org.apache.drill.exec.physical.impl.materialize;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.drill.exec.ops.OperatorStats;
import org.apache.drill.exec.physical.impl.ScreenCreator.ScreenRoot.Metric;
import org.apache.drill.exec.proto.UserBitShared.QueryData;
import org.apache.drill.exec.proto.UserBitShared.QueryId;
import org.apache.drill.exec.proto.UserBitShared.RecordBatchDef;
import org.apache.drill.exec.proto.UserBitShared.SerializedField;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.VectorWrapper;

/**
 * Packages a batch from the Screen operator to send to its
 * user connection. In the original Drill, that connection was always a
 * network connection, and so the outgoing batch is serialized to a set
 * of buffers ready to send. However, the REST server runs in the same process.
 * The original REST query implementation serialized the data to buffers, then
 * copied the data to a big buffer to be deserialized, causing significant memory
 * pressure. This version allows the user connection to elect for serialization,
 * or just to access the original source batch.
 */
public interface QueryDataPackage {

  QueryId queryId();
  QueryWritableBatch toWritableBatch();
  VectorContainer batch();
  List<SerializedField> fields();

  /**
   * Package that contains only a query ID. Send for a query that
   * finishes with no data. The results are null: no data, no schema.
   */
  public static class EmptyResultsPackage implements QueryDataPackage {

    private final QueryId queryId;

    public EmptyResultsPackage(QueryId queryId) {
      this.queryId = queryId;
    }

    @Override
    public QueryId queryId() { return queryId; }

    /**
     * Creates a message that sends only the query ID to the
     * client.
     */
    @Override
    public QueryWritableBatch toWritableBatch() {
      QueryData header = QueryData.newBuilder()
        .setQueryId(queryId)
        .setRowCount(0)
        .setDef(RecordBatchDef.getDefaultInstance())
        .build();
      return new QueryWritableBatch(header);
    }

    @Override
    public VectorContainer batch() { return null; }

    @Override
    public List<SerializedField> fields() {
      return Collections.emptyList();
    }
  }

  /**
   * Represents a batch of data with a schema.
   */
  public static class DataPackage implements QueryDataPackage {
    private final RecordMaterializer materializer;
    private final OperatorStats stats;

    public DataPackage(RecordMaterializer materializer, OperatorStats stats) {
      this.materializer = materializer;
      this.stats = stats;
    }

    @Override
    public QueryId queryId() { return materializer.queryId(); }

    @Override
    public QueryWritableBatch toWritableBatch() {
      QueryWritableBatch batch = materializer.convertNext();
      stats.addLongStat(Metric.BYTES_SENT, batch.getByteCount());
      return batch;
    }

    @Override
    public VectorContainer batch() {
      return materializer.incoming();
    }

    @Override
    public List<SerializedField> fields() {
      List<SerializedField> metadata = new ArrayList<>();
      for (VectorWrapper<?> vw : batch()) {
        metadata.add(vw.getValueVector().getMetadata());
      }
      return metadata;
    }
  }
}
