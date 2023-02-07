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
package org.apache.drill.exec.physical.impl.project;

import org.apache.drill.common.expression.FieldReference;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.expr.ValueVectorReadExpression;
import org.apache.drill.exec.expr.ValueVectorWriteExpression;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.TransferPair;
import org.apache.drill.exec.record.TypedFieldId;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.vector.FixedWidthVector;
import org.apache.drill.exec.vector.SchemaChangeCallBack;
import org.apache.drill.exec.vector.UntypedNullHolder;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements callbacks to build the physical vectors for the project
 * record batch.
 */
public class ProjectBatchBuilder implements ProjectionMaterializer.BatchBuilder {
  private final ProjectRecordBatch projectBatch;
  private final VectorContainer container;
  private final SchemaChangeCallBack callBack;
  private final RecordBatch incomingBatch;
  private final List<TransferPair> transfers = new ArrayList<>();

  public ProjectBatchBuilder(ProjectRecordBatch projectBatch, VectorContainer container,
      SchemaChangeCallBack callBack, RecordBatch incomingBatch) {
    this.projectBatch = projectBatch;
    this.container = container;
    this.callBack = callBack;
    this.incomingBatch = incomingBatch;
  }

  public List<TransferPair> transfers() { return transfers; }

  @Override
  public void addTransferField(String name, ValueVector vvIn) {
    FieldReference ref = new FieldReference(name);
    ValueVector vvOut = container.addOrGet(MaterializedField.create(ref.getAsNamePart().getName(),
      vvIn.getField().getType()), callBack);
    projectBatch.memoryManager.addTransferField(vvIn, vvIn.getField().getName(), vvOut.getField().getName());
    transfers.add(vvIn.makeTransferPair(vvOut));
  }

  @Override
  public int addDirectTransfer(FieldReference ref, ValueVectorReadExpression vectorRead) {
    TypedFieldId id = vectorRead.getFieldId();
    ValueVector vvIn = incomingBatch.getValueAccessorById(id.getIntermediateClass(), id.getFieldIds()).getValueVector();
    Preconditions.checkNotNull(incomingBatch);

    ValueVector vvOut =
        container.addOrGet(MaterializedField.create(ref.getLastSegment().getNameSegment().getPath(),
        vectorRead.getMajorType()), callBack);
    TransferPair tp = vvIn.makeTransferPair(vvOut);
    projectBatch.memoryManager.addTransferField(vvIn, TypedFieldId.getPath(id, incomingBatch), vvOut.getField().getName());
    transfers.add(tp);
    return vectorRead.getFieldId().getFieldIds()[0];
  }

  @Override
  public ValueVectorWriteExpression addOutputVector(String name, LogicalExpression expr) {
    MaterializedField outputField = MaterializedField.create(name, expr.getMajorType());
    ValueVector vv = container.addOrGet(outputField, callBack);
    projectBatch.allocationVectors.add(vv);
    TypedFieldId fid = container.getValueVectorId(SchemaPath.getSimplePath(outputField.getName()));
    ValueVectorWriteExpression write = new ValueVectorWriteExpression(fid, expr, true);
    projectBatch.memoryManager.addNewField(vv, write);
    return write;
  }

  @Override
  public void addComplexField(FieldReference ref) {
    initComplexWriters();
    if (projectBatch.complexFieldReferencesList == null) {
      projectBatch.complexFieldReferencesList = Lists.newArrayList();
    } else {
      projectBatch.complexFieldReferencesList.clear();
    }
    // reserve place for complex field in container
    ValueVector lateVv = container.addOrGet(
        MaterializedField.create(ref.getLastSegment().getNameSegment().getPath(), UntypedNullHolder.TYPE),
        callBack);
    projectBatch.allocationVectors.add(lateVv);
    // save the field reference for later for getting schema when input is empty
    projectBatch.complexFieldReferencesList.add(ref);
    projectBatch.memoryManager.addComplexField(null); // this will just add an estimate to the row width
  }

  private void initComplexWriters() {
    // Lazy initialization of the list of complex writers, if not done yet.
    if (projectBatch.complexWriters == null) {
      projectBatch.complexWriters = new ArrayList<>();
    } else {
      projectBatch.complexWriters.clear();
    }
  }

  @Override
  public ValueVectorWriteExpression addEvalVector(String outputName, LogicalExpression expr) {
    MaterializedField outputField = MaterializedField.create(outputName, expr.getMajorType());
    ValueVector ouputVector = container.addOrGet(outputField, callBack);
    projectBatch.allocationVectors.add(ouputVector);
    TypedFieldId fid = container.getValueVectorId(SchemaPath.getSimplePath(outputField.getName()));
    boolean useSetSafe = !(ouputVector instanceof FixedWidthVector);
    ValueVectorWriteExpression write = new ValueVectorWriteExpression(fid, expr, useSetSafe);
    projectBatch.memoryManager.addNewField(ouputVector, write);

    // We cannot do multiple transfers from the same vector. However we still
    // need to instantiate the output vector.
    if (expr instanceof ValueVectorReadExpression) {
      ValueVectorReadExpression vectorRead = (ValueVectorReadExpression) expr;
      if (!vectorRead.hasReadPath()) {
        TypedFieldId id = vectorRead.getFieldId();
        ValueVector vvIn = incomingBatch.getValueAccessorById(id.getIntermediateClass(),
                id.getFieldIds()).getValueVector();
        vvIn.makeTransferPair(ouputVector);
      }
    }
    return write;
  }
}
