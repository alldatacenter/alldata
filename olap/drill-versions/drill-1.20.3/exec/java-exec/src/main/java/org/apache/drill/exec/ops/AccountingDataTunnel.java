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
package org.apache.drill.exec.ops;

import org.apache.drill.exec.proto.BitData;
import org.apache.drill.exec.record.FragmentWritableBatch;
import org.apache.drill.exec.rpc.RpcOutcomeListener;
import org.apache.drill.exec.rpc.data.DataTunnel;
import org.apache.drill.exec.testing.ControlsInjector;
import org.apache.drill.exec.testing.ExecutionControls;
import org.apache.drill.exec.work.filter.RuntimeFilterWritable;


/**
 * Wrapper around a {@link org.apache.drill.exec.rpc.data.DataTunnel} that tracks the status of batches sent to
 * to other Drillbits.
 */
public class AccountingDataTunnel {
  private final DataTunnel tunnel;
  private final SendingAccountor sendingAccountor;
  private final RpcOutcomeListener<BitData.AckWithCredit> statusHandler;

  public AccountingDataTunnel(DataTunnel tunnel, SendingAccountor sendingAccountor, RpcOutcomeListener<BitData.AckWithCredit> statusHandler) {
    this.tunnel = tunnel;
    this.sendingAccountor = sendingAccountor;
    this.statusHandler = statusHandler;
  }

  public void sendRecordBatch(FragmentWritableBatch batch) {
    sendingAccountor.increment();
    tunnel.sendRecordBatch(statusHandler, batch);
  }

  public void sendRuntimeFilter(RuntimeFilterWritable batch) {
    sendingAccountor.increment();
    tunnel.sendRuntimeFilter(statusHandler, batch);
  }

  /**
   * See {@link DataTunnel#setTestInjectionControls(ControlsInjector, ExecutionControls, Logger)}.
   */
  public void setTestInjectionControls(final ControlsInjector testInjector,
      final ExecutionControls testControls, final org.slf4j.Logger testLogger) {
    tunnel.setTestInjectionControls(testInjector, testControls, testLogger);
  }
}
