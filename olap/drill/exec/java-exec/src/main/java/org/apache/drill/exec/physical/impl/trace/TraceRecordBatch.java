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
package org.apache.drill.exec.physical.impl.trace;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.cache.VectorAccessibleSerializable;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.physical.config.Trace;
import org.apache.drill.exec.record.AbstractSingleRecordBatch;
import org.apache.drill.exec.record.BatchSchema.SelectionVectorMode;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.TransferPair;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.record.WritableBatch;
import org.apache.drill.exec.record.selection.SelectionVector2;
import org.apache.drill.exec.util.Utilities;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains value vectors which are exactly the same
 * as the incoming record batch's value vectors. If the incoming
 * record batch has a selection vector (type 2) then TraceRecordBatch
 * will also contain a selection vector.
 * <p>
 * Purpose of this record batch is to dump the data associated with all
 * the value vectors and selection vector to disk.
 * <p>
 * This record batch does not modify any data or schema, it simply
 * consumes the incoming record batch's data, dump to disk and pass the
 * same set of value vectors (and selection vectors) to its parent record
 * batch.
 */
public class TraceRecordBatch extends AbstractSingleRecordBatch<Trace> {
  static final Logger logger = LoggerFactory.getLogger(TraceRecordBatch.class);

  private SelectionVector2 sv;

  private final BufferAllocator localAllocator;

  /* Tag associated with each trace operator */
  final String traceTag;

  /* Location where the log should be dumped */
  private final String logLocation;

  /* File descriptors needed to be able to dump to log file */
  private OutputStream fos;

  public TraceRecordBatch(Trace pop, RecordBatch incoming, FragmentContext context) throws ExecutionSetupException {
    super(pop, context, incoming);
    this.traceTag = pop.traceTag;
    logLocation = context.getConfig().getString(ExecConstants.TRACE_DUMP_DIRECTORY);
    localAllocator = context.getNewChildAllocator("trace", 200, 0, Long.MAX_VALUE);
    String fileName = getFileName();

    // Create the log file we will dump to and initialize the file descriptors
    try {
      Configuration conf = new Configuration();
      conf.set(FileSystem.FS_DEFAULT_NAME_KEY,
          context.getConfig().getString(ExecConstants.TRACE_DUMP_FILESYSTEM));
      FileSystem fs = FileSystem.get(conf);

      // create the file
      fos = fs.create(new Path(fileName));
    } catch (IOException e) {
        throw new ExecutionSetupException("Unable to create file: " + fileName + " check permissions or if directory exists", e);
    }
  }

  @Override
  public int getRecordCount() {
    if (sv == null) {
      return container.getRecordCount();
    } else {
      return sv.getCount();
    }
  }

  /**
   * Invoked for every record batch and it simply dumps the buffers
   * associated with all the value vectors in this record batch to a log file.
   */
  @Override
  protected IterOutcome doWork() {

    boolean incomingHasSv2 = incoming.getSchema().getSelectionVectorMode() == SelectionVectorMode.TWO_BYTE;
    if (incomingHasSv2) {
      sv = incoming.getSelectionVector2();
    } else {
      sv = null;
    }
    WritableBatch batch = WritableBatch.getBatchNoHVWrap(incoming.getContainer().getRecordCount(), incoming, incomingHasSv2);
    VectorAccessibleSerializable wrap = new VectorAccessibleSerializable(batch, sv, oContext.getAllocator());

    try {
      wrap.writeToStreamAndRetain(fos);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    batch.reconstructContainer(localAllocator, container);
    if (incomingHasSv2) {
      sv = wrap.getSv2();
      container.setRecordCount(sv.getBatchActualRecordCount());
    } else {
      container.setRecordCount(batch.getDef().getRecordCount());
    }
    return getFinalOutcome(false);
  }

  @Override
  protected boolean setupNewSchema() {
    // Trace operator does not deal with hyper vectors yet
    if (incoming.getSchema().getSelectionVectorMode() == SelectionVectorMode.FOUR_BYTE) {
      throw new UnsupportedOperationException("Trace operator does not work with hyper vectors");
    }

    // we have a new schema, clear our existing container to load the new value vectors

    container.clear();

    // Add all the value vectors in the container
    for (VectorWrapper<?> vv : incoming) {
      TransferPair tp = vv.getValueVector().getTransferPair(oContext.getAllocator());
      container.add(tp.getTo());
    }
    container.buildSchema(incoming.getSchema().getSelectionVectorMode());
    container.setRecordCount(incoming.getRecordCount());
    return true;
  }

  @Override
  public SelectionVector2 getSelectionVector2() {
    return sv;
  }

  private String getFileName() {
    return Utilities.getFileNameForQueryFragment(incoming.getContext(), logLocation, traceTag);
  }

  @Override
  public void close() {
    // Release the selection vector
    if (sv != null) {
      sv.clear();
    }

    // Close the file descriptors
    try {
      fos.close();
    } catch (IOException e) {
      logger.error("Unable to close file descriptors for file: " + getFileName());
    }
    super.close();
  }

  @Override
  public void dump() {
    logger.error("TraceRecordBatch[filename={}, logLocation={}, selectionVector={}]", getFileName(), logLocation, sv);
  }
}
