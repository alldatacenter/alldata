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
package org.apache.drill.exec.rpc.data;

import org.apache.drill.exec.proto.BitData;
import org.apache.drill.shaded.guava.com.google.common.base.Stopwatch;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.scanner.ClassPathScanner;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.ExecTest;
import org.apache.drill.exec.exception.FragmentSetupException;
import org.apache.drill.exec.expr.TypeHelper;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.ops.FragmentContextImpl;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.exec.proto.ExecProtos.FragmentHandle;
import org.apache.drill.exec.proto.UserBitShared.QueryId;
import org.apache.drill.exec.record.FragmentWritableBatch;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.RawFragmentBatch;
import org.apache.drill.exec.record.WritableBatch;
import org.apache.drill.exec.rpc.RpcException;
import org.apache.drill.exec.rpc.RpcOutcomeListener;
import org.apache.drill.exec.rpc.control.WorkEventBus;
import org.apache.drill.exec.server.BootStrapContext;
import org.apache.drill.exec.server.options.SystemOptionManager;
import org.apache.drill.exec.vector.Float8Vector;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.work.WorkManager.WorkerBee;
import org.apache.drill.exec.work.fragment.FragmentExecutor;
import org.apache.drill.exec.work.fragment.FragmentManager;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestBitRpc extends ExecTest {
  @Test
  public void testConnectionBackpressure() throws Exception {
    final WorkerBee bee = mock(WorkerBee.class);
    final WorkEventBus workBus = mock(WorkEventBus.class);
    final DrillConfig config1 = DrillConfig.create();
    final BootStrapContext c = new BootStrapContext(config1, SystemOptionManager.createDefaultOptionDefinitions(), ClassPathScanner.fromPrescan(config1));
    final FragmentContextImpl fcon = mock(FragmentContextImpl.class);
    when(fcon.getAllocator()).thenReturn(c.getAllocator());

    final FragmentManager fman = new MockFragmentManager(c);

    when(workBus.getFragmentManager(any(FragmentHandle.class))).thenReturn(fman);

    int port = 1234;

    DataConnectionConfig config = new DataConnectionConfig(c.getAllocator(), c,
        new DataServerRequestHandler(workBus, bee));
    DataServer server = new DataServer(config);

    port = server.bind(port, true);
    DrillbitEndpoint ep = DrillbitEndpoint.newBuilder().setAddress("localhost").setDataPort(port).build();
    DataConnectionManager manager = new DataConnectionManager(ep, config);
    DataTunnel tunnel = new DataTunnel(manager);
    AtomicLong max = new AtomicLong(0);
    for (int i = 0; i < 40; i++) {
      long t1 = System.currentTimeMillis();
      tunnel.sendRecordBatch(new TimingOutcome(max), new FragmentWritableBatch(false, QueryId.getDefaultInstance(), 1,
          1, 1, 1, getRandomBatch(c.getAllocator(), 5000)));
    }
    assertTrue(max.get() > 2700);
    Thread.sleep(5000);
  }

  @Test
  public void testConnectionBackpressureWithDynamicCredit() throws Exception {
    WorkerBee bee = mock(WorkerBee.class);
    WorkEventBus workBus = mock(WorkEventBus.class);
    DrillConfig config1 = DrillConfig.create();
    BootStrapContext c = new BootStrapContext(config1, SystemOptionManager.createDefaultOptionDefinitions(), ClassPathScanner.fromPrescan(config1));
    FragmentContextImpl fcon = mock(FragmentContextImpl.class);
    when(fcon.getAllocator()).thenReturn(c.getAllocator());

    final FragmentManager fman = new MockFragmentManagerWithDynamicCredit(c);

    when(workBus.getFragmentManager(any(FragmentHandle.class))).thenReturn(fman);

    int port = 1234;

    DataConnectionConfig config = new DataConnectionConfig(c.getAllocator(), c,
            new DataServerRequestHandler(workBus, bee));
    DataServer server = new DataServer(config);

    port = server.bind(port, true);
    DrillbitEndpoint ep = DrillbitEndpoint.newBuilder().setAddress("localhost").setDataPort(port).build();
    DataConnectionManager manager = new DataConnectionManager(ep, config);
    DataTunnel tunnel = new DataTunnel(manager);
    AtomicLong max = new AtomicLong(0);
    for (int i = 0; i < 40; i++) {
      long t1 = System.currentTimeMillis();
      tunnel.sendRecordBatch(new TimingOutcome(max), new FragmentWritableBatch(false, QueryId.getDefaultInstance(), 1,
              1, 1, 1, getRandomBatch(c.getAllocator(), 5000)));
    }
    assertTrue(max.get() > 2700);
    Thread.sleep(5000);
  }

  private static WritableBatch getRandomBatch(BufferAllocator allocator, int records) {
    List<ValueVector> vectors = Lists.newArrayList();
    for (int i = 0; i < 5; i++) {
      Float8Vector v = (Float8Vector) TypeHelper.getNewVector(
          MaterializedField.create("a", Types.required(MinorType.FLOAT8)),
          allocator);
      v.allocateNew(records);
      v.getMutator().generateTestData(records);
      vectors.add(v);
    }
    return WritableBatch.getBatchNoHV(records, vectors, false);
  }

  private class TimingOutcome implements RpcOutcomeListener<BitData.AckWithCredit> {
    private AtomicLong max;
    private Stopwatch watch = Stopwatch.createStarted();

    public TimingOutcome(AtomicLong max) {
      super();
      this.max = max;
    }

    @Override
    public void failed(RpcException ex) {
      ex.printStackTrace();
    }

    @Override
    public void success(BitData.AckWithCredit value, ByteBuf buffer) {
      long micros = watch.elapsed(TimeUnit.MILLISECONDS);
      while (true) {
        long nowMax = max.get();
        if (nowMax < micros) {
          if (max.compareAndSet(nowMax, micros)) {
            break;
          }
        } else {
          break;
        }
      }
    }

    @Override
    public void interrupted(final InterruptedException e) {
      // TODO(We don't have any interrupts in test code)
    }
  }

  public static class MockFragmentManager implements FragmentManager
  {
    private final BootStrapContext c;
    private int v;

    public MockFragmentManager(BootStrapContext c)
    {
      this.c = c;
    }

    @Override
    public boolean handle(IncomingDataBatch batch) throws FragmentSetupException, IOException {
      try {
        v++;
        if (v % 10 == 0) {
          Thread.sleep(3000);
        }
      } catch (InterruptedException e) {

      }
      RawFragmentBatch rfb = batch.newRawFragmentBatch(c.getAllocator());
      rfb.sendOk();
      rfb.release();

      return true;
    }

    @Override
    public FragmentExecutor getRunnable() {
      return null;
    }

    @Override
    public void cancel() {

    }

    @Override
    public boolean isCancelled() {
      return false;
    }

    @Override
    public void unpause() {

    }

    @Override
    public boolean isWaiting() {
      return false;
    }

    @Override
    public FragmentHandle getHandle() {
      return null;
    }

    @Override
    public FragmentContext getFragmentContext() {
      return null;
    }

    @Override
    public void receivingFragmentFinished(FragmentHandle handle) {

    }
  }

  public static class MockFragmentManagerWithDynamicCredit implements FragmentManager {
    private final BootStrapContext c;
    private int v;
    private int times = 0;

    public MockFragmentManagerWithDynamicCredit(BootStrapContext c) {
      this.c = c;
    }

    @Override
    public boolean handle(IncomingDataBatch batch) throws FragmentSetupException, IOException {
      try {
        v++;
        if (v % 10 == 0) {
          Thread.sleep(3000);
        }
      } catch (InterruptedException e) {

      }
      times++;
      RawFragmentBatch rfb = batch.newRawFragmentBatch(c.getAllocator());
      if (times > 3) {
        rfb.sendOk(4);
      } else {
        rfb.sendOk();
      }
      rfb.release();

      return true;
    }

    @Override
    public FragmentExecutor getRunnable() {
      return null;
    }

    @Override
    public void cancel() {

    }

    @Override
    public boolean isCancelled() {
      return false;
    }

    @Override
    public void unpause() {

    }

    @Override
    public boolean isWaiting() {
      return false;
    }

    @Override
    public FragmentHandle getHandle() {
      return null;
    }

    @Override
    public FragmentContext getFragmentContext() {
      return null;
    }

    @Override
    public void receivingFragmentFinished(FragmentHandle handle) {

    }
  }
}
