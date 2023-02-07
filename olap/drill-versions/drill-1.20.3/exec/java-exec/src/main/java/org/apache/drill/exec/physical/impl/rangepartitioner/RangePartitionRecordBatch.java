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
package org.apache.drill.exec.physical.impl.rangepartitioner;

import java.util.List;

import org.apache.drill.common.expression.FieldReference;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.exception.OutOfMemoryException;
import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.expr.TypeHelper;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.physical.config.RangePartitionSender;
import org.apache.drill.exec.record.AbstractSingleRecordBatch;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.TransferPair;
import org.apache.drill.exec.record.VectorAccessible;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.vector.IntVector;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides the ability to divide up the input rows into a fixed number of
 * separate ranges or 'buckets' based on the values of a set of columns (the
 * range partitioning columns).
 */
public class RangePartitionRecordBatch extends AbstractSingleRecordBatch<RangePartitionSender> {
  static final Logger logger = LoggerFactory.getLogger(RangePartitionRecordBatch.class);

  private final int numPartitions;
  private int recordCount;
  private final IntVector partitionIdVector;
  private final List<TransferPair> transfers;

  public RangePartitionRecordBatch(RangePartitionSender popConfig, RecordBatch incoming, FragmentContext context) throws OutOfMemoryException {
    super(popConfig, context, incoming);
    numPartitions = popConfig.getDestinations().size();

    SchemaPath outputPath = popConfig.getPartitionFunction().getPartitionFieldRef();
    MaterializedField outputField = MaterializedField.create(outputPath.getAsNamePart().getName(), Types.required(TypeProtos.MinorType.INT));
    partitionIdVector = (IntVector) TypeHelper.getNewVector(outputField, oContext.getAllocator());
    transfers = Lists.newArrayList();
  }

  @Override
  public void close() {
    super.close();
    partitionIdVector.clear();
  }

  @Override
  public int getRecordCount() {
    return recordCount;
  }

  @Override
  protected IterOutcome doWork() {
    int num = incoming.getRecordCount();
    if (num > 0) {
      // first setup the partition columns.  This needs to be done whenever we have
      // a new batch (not just a new schema) because the partitioning function needs
      // access to the correct value vector.
      setupPartitionCols(incoming);

      partitionIdVector.allocateNew(num);

      recordCount = projectRecords(num, 0);
      container.setValueCount(recordCount);
    }
    // returning OK here is fine because the base class AbstractSingleRecordBatch
    // is handling the actual return status; thus if there was a new schema, the
    // parent will get an OK_NEW_SCHEMA based on innerNext() in base class.
    return IterOutcome.OK;
  }

  /**
   * Sets up projection that will transfer all of the columns in batch, and also setup
   * the partition column based on which partition a record falls into
   *
   * @throws SchemaChangeException
   * @return True if the new schema differs from old schema, False otherwise
   */
  @Override
  protected boolean setupNewSchema() {
    container.clear();

    for (VectorWrapper<?> vw : incoming) {
      TransferPair tp = vw.getValueVector().getTransferPair(oContext.getAllocator());
      transfers.add(tp);
      container.add(tp.getTo());
    }

    container.add(this.partitionIdVector);
    container.buildSchema(incoming.getSchema().getSelectionVectorMode());
    /*
     * Always return true because we transfer incoming. If upstream sent OK_NEW_SCHEMA,
     * it would also generate new VVs. Since we get them via transfer, we should send
     * OK_NEW_SCHEMA to downstream for it to re-init to the new VVs.
     */
    return true;
  }

  /**
   *  Provide the partition function with the appropriate value vector(s) that
   *  are involved in the range partitioning
   *  @param batch batch of columns
   */
  private void setupPartitionCols(VectorAccessible batch) {
    List<VectorWrapper<?>> partitionCols = Lists.newArrayList();

    for (VectorWrapper<?> vw : batch) {
      if (isPartitioningColumn(vw.getField().getName())) {
        partitionCols.add(vw);
      }
    }

    popConfig.getPartitionFunction().setup(partitionCols);
  }

  private boolean isPartitioningColumn(String name) {
    List<FieldReference> refList = popConfig.getPartitionFunction().getPartitionRefList();
    for (FieldReference f : refList) {
      if (f.getRootSegment().getPath().equals(name)) {
        return true;
      }
    }
    return false;
  }

  private int getPartition(int index) {
    return popConfig.getPartitionFunction().eval(index, numPartitions);
  }

  /**
   * For each incoming record, get the partition id it belongs to by invoking the
   * partitioning function. Set this id in the output partitionIdVector.  For all other
   * incoming value vectors, just do a transfer.
   * @param recordCount number of incoming records
   * @param firstOutputIndex the index of the first output
   * @return the number of records projected
   */
  private final int projectRecords(int recordCount, int firstOutputIndex) {
    int countN = recordCount;
    int counter = 0;
    for (int i = 0; i < countN; i++, firstOutputIndex++) {
      int partition = getPartition(i);
      partitionIdVector.getMutator().setSafe(i, partition);
      counter++;
    }
    for(TransferPair t : transfers){
        t.transfer();
    }
    return counter;
  }

  @Override
  public void dump() {
    logger.error("RangePartitionRecordBatch[container={}, numPartitions={}, recordCount={}, partitionIdVector={}]",
        container, numPartitions, recordCount, partitionIdVector);
  }
}
