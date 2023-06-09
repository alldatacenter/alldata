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
package org.apache.drill.exec.store.parquet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;

import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.proto.UserBitShared;
import org.apache.drill.exec.proto.UserBitShared.QueryResult.QueryState;
import org.apache.drill.exec.record.RecordBatchLoader;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.rpc.ConnectionThrottle;
import org.apache.drill.exec.rpc.RpcException;
import org.apache.drill.exec.rpc.user.QueryDataBatch;
import org.apache.drill.exec.rpc.user.UserResultsListener;
import org.apache.drill.exec.vector.ValueVector;

import org.apache.drill.shaded.guava.com.google.common.base.Strings;
import org.apache.drill.shaded.guava.com.google.common.util.concurrent.SettableFuture;

public class ParquetResultListener implements UserResultsListener {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ParquetResultListener.class);

  private final SettableFuture<Void> future = SettableFuture.create();
  int count = 0;
  int totalRecords;

  private final boolean testValues;
  private final BufferAllocator allocator;

  int batchCounter = 1;
  private final HashMap<String, Integer> valuesChecked = new HashMap<>();
  private final ParquetTestProperties props;

  ParquetResultListener(BufferAllocator allocator, ParquetTestProperties props,
      int numberOfTimesRead, boolean testValues) {
    this.allocator = allocator;
    this.props = props;
    this.totalRecords = props.recordsPerRowGroup * props.numberRowGroups * numberOfTimesRead;
    this.testValues = testValues;
  }

  @Override
  public void submissionFailed(UserException ex) {
    logger.error("Submission failed.", ex);
    future.setException(ex);
  }

  @Override
  public void queryCompleted(QueryState state) {
    checkLastChunk();
  }

  private <T> void assertField(ValueVector valueVector, int index,
      TypeProtos.MinorType expectedMinorType, Object value, String name) {
    assertField(valueVector, index, expectedMinorType, value, name, 0);
  }

  @SuppressWarnings("unchecked")
  private <T> void assertField(ValueVector valueVector, int index,
      TypeProtos.MinorType expectedMinorType, T value, String name, int parentFieldId) {

    if (expectedMinorType == TypeProtos.MinorType.MAP) {
      return;
    }

    final T val;
    try {
      val = (T) valueVector.getAccessor().getObject(index);
    } catch (Throwable ex) {
      throw ex;
    }

    if (val instanceof byte[]) {
      assertTrue(Arrays.equals((byte[]) value, (byte[]) val));
    } else {
      assertEquals(value, val);
    }
  }

  @Override
  synchronized public void dataArrived(QueryDataBatch result, ConnectionThrottle throttle) {
    logger.debug("result arrived in test batch listener.");
    int columnValCounter = 0;
    FieldInfo currentField;
    count += result.getHeader().getRowCount();
    boolean schemaChanged = false;
    final RecordBatchLoader batchLoader = new RecordBatchLoader(allocator);
    schemaChanged = batchLoader.load(result.getHeader().getDef(), result.getData());

    // used to make sure each vector in the batch has the same number of records
    int valueCount = batchLoader.getRecordCount();

    // print headers.
    if (schemaChanged) {
    } // do not believe any change is needed for when the schema changes, with the current mock scan use case

    for (final VectorWrapper<?> vw : batchLoader) {
      final ValueVector vv = vw.getValueVector();
      currentField = props.fields.get(vv.getField().getName());
      if (!valuesChecked.containsKey(vv.getField().getName())) {
        valuesChecked.put(vv.getField().getName(), 0);
        columnValCounter = 0;
      } else {
        columnValCounter = valuesChecked.get(vv.getField().getName());
      }
      printColumnMajor(vv);

      if (testValues) {
        for (int j = 0; j < vv.getAccessor().getValueCount(); j++) {
          assertField(vv, j, currentField.type,
              currentField.values[columnValCounter % 3], currentField.name + "/");
          columnValCounter++;
        }
      } else {
        columnValCounter += vv.getAccessor().getValueCount();
      }

      valuesChecked.remove(vv.getField().getName());
      assertEquals("Mismatched value count for vectors in the same batch.", valueCount, vv.getAccessor().getValueCount());
      valuesChecked.put(vv.getField().getName(), columnValCounter);
    }

    if (ParquetRecordReaderTest.VERBOSE_DEBUG){
      printRowMajor(batchLoader);
    }
    batchCounter++;

    batchLoader.clear();
    result.release();
  }

  private void checkLastChunk() {
    int recordsInBatch = -1;
    // ensure the right number of columns was returned, especially important to ensure selective column read is working
    if (testValues) {
      assertEquals( "Unexpected number of output columns from parquet scan.", props.fields.keySet().size(), valuesChecked.keySet().size() );
    }
    for (final String s : valuesChecked.keySet()) {
      try {
        if (recordsInBatch == -1 ){
          recordsInBatch = valuesChecked.get(s);
        } else {
          assertEquals("Mismatched record counts in vectors.", recordsInBatch, valuesChecked.get(s).intValue());
        }
        assertEquals("Record count incorrect for column: " + s, totalRecords, (long) valuesChecked.get(s));
      } catch (AssertionError e) {
        submissionFailed(UserException.systemError(e).build(logger));
      }
    }

    assertTrue(valuesChecked.keySet().size() > 0);
    future.set(null);
  }

  public void printColumnMajor(ValueVector vv) {
    logger.debug("\n{}", vv.getField().getName());
    final StringBuilder sb = new StringBuilder();

    for (int j = 0; j < vv.getAccessor().getValueCount(); j++) {
      if (logger.isDebugEnabled()) {
        Object o = vv.getAccessor().getObject(j);
        if (o instanceof byte[]) {
          try {
            o = new String((byte[])o, "UTF-8");
          } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
          }
        }
        sb.append(Strings.padStart(o + "", 20, ' ') + " ");
        sb.append(", " + (j % 25 == 0 ? "\n batch:" + batchCounter + " v:" + j + " - " : ""));
      }
    }

    logger.debug(sb.toString());
    logger.debug(Integer.toString(vv.getAccessor().getValueCount()));
  }

  public void printRowMajor(RecordBatchLoader batchLoader) {
    for (int i = 0; i < batchLoader.getRecordCount(); i++) {
      if (i % 50 == 0) {
        final StringBuilder sb = new StringBuilder();

        for (VectorWrapper<?> vw : batchLoader) {
          ValueVector v = vw.getValueVector();
          sb.append(Strings.padStart(v.getField().getName(), 20, ' ') + " ");
        }

        logger.debug(sb.toString());
      }

      final StringBuilder sb = new StringBuilder();

      for (final VectorWrapper<?> vw : batchLoader) {
        final ValueVector v = vw.getValueVector();
        Object o = v.getAccessor().getObject(i);
        if (o instanceof byte[]) {
          try {
            // TODO - in the dictionary read error test there is some data that does not look correct
            // the output of our reader matches the values of the parquet-mr cat/head tools (no full comparison was made,
            // but from a quick check of a few values it looked consistent
            // this might have gotten corrupted by pig somehow, or maybe this is just how the data is supposed ot look
            // TODO - check this!!
//              for (int k = 0; k < ((byte[])o).length; k++ ) {
//                // check that the value at each position is a valid single character ascii value.
//
//                if (((byte[])o)[k] > 128) {
//                }
//              }
            o = new String((byte[])o, "UTF-8");
          } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
          }
        }

        sb.append(Strings.padStart(o + "", 20, ' ') + " ");
      }

      logger.debug(sb.toString());
    }
  }

  public void getResults() throws RpcException {
    try {
      future.get();
    } catch(Throwable t) {
      throw RpcException.mapException(t);
    }
  }

  @Override
  public void queryIdArrived(UserBitShared.QueryId queryId) {
  }
}
