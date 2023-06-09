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
package org.apache.drill.exec.physical.impl;

import java.util.Iterator;
import java.util.List;

import org.apache.drill.common.DeferredException;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.ops.QueryCancelledException;
import org.apache.drill.exec.ops.RootFragmentContext;
import org.apache.drill.exec.physical.impl.ScreenCreator.ScreenRoot;
import org.apache.drill.exec.proto.ExecProtos.FragmentHandle;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.TypedFieldId;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.record.selection.SelectionVector2;
import org.apache.drill.exec.record.selection.SelectionVector4;
import org.apache.drill.exec.vector.ValueVector;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

@Deprecated
public class SimpleRootExec implements RootExec, Iterable<ValueVector> {
  private final RecordBatch incoming;
  private final ScreenRoot screenRoot;

  public SimpleRootExec(final RootExec e) {
    if (e instanceof ScreenRoot) {
      incoming = ((ScreenRoot)e).getIncoming();
      screenRoot = (ScreenRoot) e;
    } else {
      throw new UnsupportedOperationException();
    }

    screenRoot.getContext().setExecutorState(new DummyExecutorState());
  }

  private class DummyExecutorState implements FragmentContext.ExecutorState {
    final DeferredException ex = new DeferredException();

    @Override
    public boolean shouldContinue() {
      return !isFailed();
    }

    @Override
    public void fail(final Throwable t) {
      ex.addThrowable(t);
    }

    @Override
    public boolean isFailed() {
      return ex.getException() != null;
    }

    @Override
    public Throwable getFailureCause() {
      return ex.getException();
    }

    @Override
    public void checkContinue() {
      if (!shouldContinue()) {
        throw new QueryCancelledException();
      }
    }
  }

  public RootFragmentContext getContext() {
    return screenRoot.getContext();
  }

  public SelectionVector2 getSelectionVector2() {
    return incoming.getSelectionVector2();
  }

  public SelectionVector4 getSelectionVector4() {
    return incoming.getSelectionVector4();
  }

  @SuppressWarnings("unchecked")
  public <T extends ValueVector> T getValueVectorById(final SchemaPath path, final Class<?> vvClass) {
    final TypedFieldId tfid = incoming.getValueVectorId(path);
    return (T) incoming.getValueAccessorById(vvClass, tfid.getFieldIds()).getValueVector();
  }

  @Override
  public boolean next() {
    switch (incoming.next()) {
    case NONE:
      return false;
    default:
      return true;
    }
  }

  @Override
  public void dumpBatches(Throwable t) {
    screenRoot.dumpBatches(t);
  }

  @Override
  public void close() throws Exception {
    screenRoot.close();
  }

  @Override
  public void receivingFragmentFinished(final FragmentHandle handle) {
    //no op
  }

  @Override
  public Iterator<ValueVector> iterator() {
    final List<ValueVector> vv = Lists.newArrayList();
    for (final VectorWrapper<?> vw : incoming) {
      vv.add(vw.getValueVector());
    }
    return vv.iterator();
  }

  public int getRecordCount() {
    return incoming.getRecordCount();
  }

  /// Temporary: for exposing the incoming batch to TestHashTable
  public RecordBatch getIncoming() {
    return incoming;
  }

}
