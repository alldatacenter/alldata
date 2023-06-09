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
package org.apache.drill.exec.physical.impl.svremover;

import org.apache.drill.exec.exception.OutOfMemoryException;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.physical.config.SelectionVectorRemover;
import org.apache.drill.exec.record.AbstractSingleRecordBatch;
import org.apache.drill.exec.record.BatchSchema.SelectionVectorMode;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.WritableBatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemovingRecordBatch extends AbstractSingleRecordBatch<SelectionVectorRemover>{
  private static final Logger logger = LoggerFactory.getLogger(RemovingRecordBatch.class);

  private Copier copier;

  public RemovingRecordBatch(SelectionVectorRemover popConfig, FragmentContext context,
      RecordBatch incoming) throws OutOfMemoryException {
    super(popConfig, context, incoming);
    logger.debug("Created.");
  }

  @Override
  public int getRecordCount() {
    return container.getRecordCount();
  }

  @Override
  protected boolean setupNewSchema() {
    // Don't clear off container just because an OK_NEW_SCHEMA was received from
    // upstream. For cases when there is just
    // change in container type but no actual schema change, RemovingRecordBatch
    // should consume OK_NEW_SCHEMA and
    // send OK to downstream instead. Since the output of RemovingRecordBatch is
    // always going to be a regular container
    // change in incoming container type is not actual schema change.
    container.zeroVectors();
    copier = GenericCopierFactory.createAndSetupCopier(incoming, container, callBack);

    // If there is an actual schema change then below condition will be true and
    // it will send OK_NEW_SCHEMA
    // downstream too
    if (container.isSchemaChanged()) {
      container.buildSchema(SelectionVectorMode.NONE);
      return true;
    }

    return false;
  }

  @Override
  protected IterOutcome doWork() {
    try {
      int rowCount = incoming.getRecordCount();
      if (rowCount == 0) {
        container.setEmpty();
      } else {
        copier.copyRecords(0, rowCount);
      }
    } catch (Exception e) {
      throw new IllegalStateException(e);
    } finally {
      if (incoming.getSchema().getSelectionVectorMode() != SelectionVectorMode.FOUR_BYTE) {
        incoming.getContainer().zeroVectors();
        if (incoming.getSchema().getSelectionVectorMode() == SelectionVectorMode.TWO_BYTE) {
          incoming.getSelectionVector2().clear();
        }
      }
    }

    logger.debug("doWork(): {} records copied out of {}, incoming schema {} ",
      container.getRecordCount(), container.getRecordCount(), incoming.getSchema());
    return getFinalOutcome(false);
  }

  @Override
  public void close() {
    super.close();
  }

  @Override
  public WritableBatch getWritableBatch() {
    return WritableBatch.get(this);
  }

  @Override
  public void dump() {
    logger.error("RemovingRecordBatch[container={}, state={}, copier={}]", container, state, copier);
  }
}
