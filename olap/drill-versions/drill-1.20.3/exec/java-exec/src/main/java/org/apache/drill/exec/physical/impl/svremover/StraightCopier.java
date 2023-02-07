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

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.TransferPair;
import org.apache.drill.exec.record.VectorAccessible;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.vector.SchemaChangeCallBack;

import java.util.List;

public class StraightCopier implements Copier {
    private List<TransferPair> pairs = Lists.newArrayList();
    private RecordBatch incoming;
    private VectorContainer outputContainer;
    private SchemaChangeCallBack callBack;

    public StraightCopier(RecordBatch incomingBatch, VectorContainer outputContainer, SchemaChangeCallBack callBack) {
      this.incoming = incomingBatch;
      this.outputContainer = outputContainer;
      this.callBack = callBack;
    }

    @Override
    public void setup(VectorAccessible incoming, VectorContainer outgoing) {
      for(VectorWrapper<?> vv : incoming){
        TransferPair tp = vv.getValueVector().makeTransferPair(outputContainer.addOrGet(vv.getField(), callBack));
        pairs.add(tp);
      }
    }

    @Override
    public int copyRecords(int index, int recordCount) {
      assert index == 0 && recordCount == incoming.getRecordCount() : "Straight copier cannot split batch";
      for(TransferPair tp : pairs){
        tp.transfer();
      }

      outputContainer.setRecordCount(incoming.getRecordCount());
      return recordCount;
    }

    @Override
    public int appendRecord(int index) {
      throw new UnsupportedOperationException();
    }

    @Override
    public int appendRecords(int index, int recordCount) {
      throw new UnsupportedOperationException();
    }
}
