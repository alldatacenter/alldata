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
package org.apache.drill.exec.server.rest.stream;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.util.DrillExceptionUtil;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.physical.impl.materialize.QueryDataPackage;
import org.apache.drill.exec.physical.resultSet.PushResultSetReader;
import org.apache.drill.exec.physical.resultSet.impl.PushResultSetReaderImpl;
import org.apache.drill.exec.physical.resultSet.impl.PushResultSetReaderImpl.BatchHolder;
import org.apache.drill.exec.physical.resultSet.util.JsonWriter;
import org.apache.drill.exec.physical.rowSet.RowSetReader;
import org.apache.drill.exec.proto.GeneralRPCProtos.Ack;
import org.apache.drill.exec.proto.UserBitShared.QueryId;
import org.apache.drill.exec.proto.helper.QueryIdHelper;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.rpc.Acks;
import org.apache.drill.exec.rpc.RpcOutcomeListener;
import org.apache.drill.exec.server.rest.BaseWebUserConnection;
import org.apache.drill.exec.server.rest.WebSessionResources;
import org.apache.drill.exec.vector.complex.fn.JsonOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stream the results of a query to a REST client as JSON, following the schema
 * defined by {@code QueryResult} to maintain backward compatibility. That schema
 * is not ideal for streaming, but it is what it is.
 * <p>
 * Streaming requires that we use a low-level JSON writer and handle serialization
 * ourselves. While we could build up, then serialize objects, there is little
 * advantage of doing so, and it would simply increase cost -- the very thing that
 * this design tries to decrease.
 * <p>
 * Does minimal pretty-printing: just inserts newlines in a few places.
 * <p>
 * Enforces the row limit, discarding all rows after the requested limit.
 * Trusts that the query will enforce the limit at the batch level to avoid
 * wasting resources.
 */
public class StreamingHttpConnection extends BaseWebUserConnection {
  private static final Logger logger = LoggerFactory.getLogger(StreamingHttpConnection.class);

  private final CountDownLatch startSignal = new CountDownLatch(1);
  private QueryId queryId;
  private int rowLimit;
  private int batchCount;
  private int rowCount;
  private OutputStream out;
  private JsonWriter writer;
  private BatchHolder batchHolder;
  private PushResultSetReader reader;

  public StreamingHttpConnection(WebSessionResources webSessionResources) {
    super(webSessionResources);
  }

  /**
   * Provide query info once the query starts. Sent from the REST request
   * thread.
   */
  public void onStart(QueryId queryId, int rowLimit) {
    this.queryId = queryId;
    this.rowLimit = rowLimit;
  }

  /**
   * Set the output stream. Sent from the REST request thread in the
   * {@code StreamingOutput} callback once the output stream is
   * available. Unblocks the Screen thread.
   */
  public void outputAvailable(OutputStream out) throws IOException {
    this.out = out;
    writer = new JsonWriter(out, false, false);
    startSignal.countDown();
  }

  /**
   * Called from query thread, specifically from the Screen operator,
   * for each batch.
   */
  @Override
  public void sendData(RpcOutcomeListener<Ack> listener, QueryDataPackage data) {
    VectorContainer batch = data.batch();
    try {
      if (batchCount == 0) {
        batchHolder = new BatchHolder(batch);
        reader = new PushResultSetReaderImpl(batchHolder);
        startSignal.await();
      }
      batchHolder.newBatch();
      RowSetReader batchReader = reader.start();
      emitBatch(batchReader);
      batchCount++;
    } catch (IOException e) {
      throw UserException.dataWriteError(e)
        .addContext("Failed to send JSON results to the REST client")
        .build(logger);
    } catch (InterruptedException e) {
      throw new DrillRuntimeException("Interrupted", e);
    } finally {
      batch.zeroVectors();
      listener.success(Acks.OK, null);
    }
  }

  public void emitBatch(RowSetReader batchReader) throws IOException {
    if (batchCount == 0) {
      emitHeader(batchReader.tupleSchema());
    }
    if (rowLimit == 0 || rowCount < rowLimit) {
      emitRows(batchReader);
    }
  }

  /**
   * Emit the JSON header. Must follow the same structure as
   * {@code QueryResult} to ensure backward compatibility. However the order
   * of fields can vary. In particular, we ensure metadata is sent first.
   * <pre><code>
   * {
   *   "queryId" : "21504178-fd2e-55f8-f1dc-c99dfa707db7",
   *   "columns" : [ "employee_id", "full_name"  ],
   *   "metadata" : [ "BIGINT", "VARCHAR" ],
   *   "attemptedAutoLimit" : 0,
   *    "rows" : [ {
   * </code></pre>
   */
  private void emitHeader(TupleMetadata rowSchema) throws IOException {
    startHeader();
    JsonOutput gen = writer.jsonOutput();
    gen.writeFieldName("columns");
    writeColNames(gen, rowSchema);
    writeNewline(gen);
    gen.writeFieldName("metadata");
    writeColTypes(gen, rowSchema);
    writeNewline(gen);
    gen.writeFieldName("attemptedAutoLimit");
    gen.writeInt(rowLimit);
    writeNewline(gen);
    gen.writeFieldName("rows");
    gen.writeStartArray();
    writeNewline(gen);

    // The array of rows is now open, ready to send batches.
  }

  private void startHeader() throws IOException {
    JsonOutput gen = writer.jsonOutput();
    gen.writeStartObject();
    gen.writeFieldName("queryId");
    gen.writeVarChar(QueryIdHelper.getQueryId(queryId));
    writeNewline(gen);
  }

  public void writeNewline(JsonOutput gen) throws IOException {
    gen.flush();
    out.write('\n');
  }

  private void writeColNames(JsonOutput gen, TupleMetadata rowSchema) throws IOException {
    gen.writeStartArray();
    for (ColumnMetadata col : rowSchema) {
      gen.writeVarChar(col.name());
    }
    gen.writeEndArray();
  }

  private void writeColTypes(JsonOutput gen, TupleMetadata rowSchema) throws IOException {
    gen.writeStartArray();
    for (ColumnMetadata col : rowSchema) {
      gen.writeVarChar(webDataType(col.majorType()));
    }
    gen.writeEndArray();
  }

  private void emitRows(RowSetReader batchReader) throws IOException {
    while (batchReader.next()) {
      writer.writeRow(batchReader);
      writeNewline(writer.jsonOutput());
      if (rowLimit > 0 && ++rowCount >= rowLimit) {
        break;
      }
    }
  }

  /**
   * Called from the REST request, after the query completes,
   * to emit the end of the JSON payload:
   * <pre><code>
   *   } ],
   *   "queryState" : "COMPLETED"
   * }
   * </code></pre>
   * <p>
   * Admittedly the tail is pretty lame, but we need it to maintain
   * backward compatibility.
   * <p>
   * Note that, under the original design, there is no good way to report
   * an error that occurs once the query starts running. Here, we report
   * the query state, which can indicate an error. But, the design of the
   * API does not currently provide the error message itself.
   */
  public void finish() throws IOException {
    JsonOutput gen = writer.jsonOutput();
    if (batchCount == 0) {
      startHeader();
      if (getSession().getOptions().getBoolean(ExecConstants.ENABLE_REST_VERBOSE_ERRORS_KEY)) {
        emitErrorInfo();
      }
    } else {
      gen.writeEndArray();
      writeNewline(gen);
    }
    gen.writeFieldName("queryState");
    gen.writeVarChar(getQueryState());
    writeNewline(gen);
    gen.writeEndObject();
    writeNewline(gen);
  }

  private void emitErrorInfo() throws IOException {
    JsonOutput gen = writer.jsonOutput();
    Throwable exception = DrillExceptionUtil.getThrowable(error.getException());
    if (exception != null) {
      gen.writeFieldName("exception");
      gen.writeVarChar(exception.getClass().getName());
      writeNewline(gen);
      gen.writeFieldName("errorMessage");
      gen.writeVarChar(exception.getMessage());
      writeNewline(gen);
      gen.writeFieldName("stackTrace");
      gen.writeStartArray();
      for (String stackFrame : ExceptionUtils.getStackFrames(exception)) {
        gen.writeVarChar(stackFrame);
      }
      gen.writeEndArray();
    } else {
      gen.writeFieldName("errorMessage");
      gen.writeVarChar(error.getMessage());
    }
    writeNewline(gen);
  }
}
