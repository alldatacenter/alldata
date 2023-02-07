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
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.base.Stopwatch;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.test.BaseDirTestWatcher;
import org.apache.drill.test.ClientFixture;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterFixtureBuilder;
import org.apache.drill.test.ClusterTest;

import io.netty.buffer.ByteBuf;
import org.apache.drill.exec.ops.FragmentContextImpl;
import org.apache.drill.categories.SecurityTest;
import org.apache.drill.common.config.DrillProperties;
import org.apache.drill.common.exceptions.UserRemoteException;
import org.apache.drill.common.scanner.ClassPathScanner;
import org.apache.drill.common.scanner.persistence.ScanResult;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.exception.DrillbitStartupException;
import org.apache.drill.exec.exception.FragmentSetupException;
import org.apache.drill.exec.expr.TypeHelper;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.exec.proto.ExecProtos.FragmentHandle;
import org.apache.drill.exec.proto.UserBitShared;
import org.apache.drill.exec.proto.UserBitShared.QueryId;
import org.apache.drill.exec.record.FragmentWritableBatch;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.RawFragmentBatch;
import org.apache.drill.exec.record.WritableBatch;
import org.apache.drill.exec.rpc.RpcException;
import org.apache.drill.exec.rpc.RpcOutcomeListener;
import org.apache.drill.exec.rpc.control.WorkEventBus;
import org.apache.drill.exec.rpc.security.KerberosHelper;
import org.apache.drill.exec.rpc.user.security.testing.UserAuthenticatorTestImpl;
import org.apache.drill.exec.server.BootStrapContext;
import org.apache.drill.exec.server.options.SystemOptionManager;
import org.apache.drill.exec.vector.Float8Vector;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.work.WorkManager.WorkerBee;
import org.apache.drill.exec.work.fragment.FragmentExecutor;
import org.apache.drill.exec.work.fragment.FragmentManager;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Category(SecurityTest.class)
public class TestBitBitKerberos extends ClusterTest {
  private static KerberosHelper krbHelper;

  private int port = 1234;

  @BeforeClass
  public static void setupTest() throws Exception {
    krbHelper = new KerberosHelper(TestBitBitKerberos.class.getSimpleName(), null);
    krbHelper.setupKdc(BaseDirTestWatcher.createTempDir(dirTestWatcher.getTmpDir()));
    cluster = defaultClusterConfig().build();
  }

  private static ClusterFixtureBuilder defaultClusterConfig() {
    return ClusterFixture.bareBuilder(dirTestWatcher)
      .clusterSize(1)
      .configNonStringProperty(ExecConstants.AUTHENTICATION_MECHANISMS, Lists.newArrayList("kerberos"))
      .configProperty(ExecConstants.BIT_AUTHENTICATION_ENABLED, true)
      .configProperty(ExecConstants.BIT_AUTHENTICATION_MECHANISM, "kerberos")
      .configProperty(ExecConstants.USE_LOGIN_PRINCIPAL, true)
      .configProperty(ExecConstants.SERVICE_PRINCIPAL, krbHelper.SERVER_PRINCIPAL)
      .configProperty(ExecConstants.SERVICE_KEYTAB_LOCATION, krbHelper.serverKeytab.toString());
  }

  private FragmentManager setupFragmentContextAndManager(BufferAllocator allocator) {
    final FragmentContextImpl fcontext = mock(FragmentContextImpl.class);
    when(fcontext.getAllocator()).thenReturn(allocator);
    return new MockFragmentManager(fcontext);
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

    TimingOutcome(AtomicLong max) {
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

  @Test
  public void success() throws Exception {
    final WorkerBee bee = mock(WorkerBee.class);
    final WorkEventBus workBus = mock(WorkEventBus.class);

    final ScanResult result = ClassPathScanner.fromPrescan(cluster.config());
    final BootStrapContext c1 =
      new BootStrapContext(cluster.config(), SystemOptionManager.createDefaultOptionDefinitions(), result);

    final FragmentManager manager = setupFragmentContextAndManager(c1.getAllocator());
    when(workBus.getFragmentManager(Mockito.<FragmentHandle>any())).thenReturn(manager);

    DataConnectionConfig config = new DataConnectionConfig(c1.getAllocator(), c1,
        new DataServerRequestHandler(workBus, bee));
    DataServer server = new DataServer(config);

    port = server.bind(port, true);
    DrillbitEndpoint ep = DrillbitEndpoint.newBuilder().setAddress("localhost").setDataPort(port).build();
    DataConnectionManager connectionManager = new DataConnectionManager(ep, config);
    DataTunnel tunnel = new DataTunnel(connectionManager);
    AtomicLong max = new AtomicLong(0);

    try {
      for (int i = 0; i < 40; i++) {
        long t1 = System.currentTimeMillis();
        tunnel.sendRecordBatch(new TimingOutcome(max),
          new FragmentWritableBatch(false, QueryId.getDefaultInstance(), 1, 1, 1, 1,
            getRandomBatch(c1.getAllocator(), 5000)));
      }
      assertTrue(max.get() > 2700);
      Thread.sleep(5000);
    } catch (Exception | AssertionError e) {
      fail();
    } finally {
      server.close();
      connectionManager.close();
      c1.close();
    }
  }

  @Test
  public void successEncryption() throws Exception {

    final WorkerBee bee = mock(WorkerBee.class);
    final WorkEventBus workBus = mock(WorkEventBus.class);
    try (
      ClusterFixture cluster = defaultClusterConfig()
        .configProperty(ExecConstants.BIT_ENCRYPTION_SASL_ENABLED, true)
        .build();
    ) {

      final ScanResult result = ClassPathScanner.fromPrescan(cluster.config());
      final BootStrapContext c2 =
        new BootStrapContext(cluster.config(), SystemOptionManager.createDefaultOptionDefinitions(), result);

      final FragmentManager manager = setupFragmentContextAndManager(c2.getAllocator());
      when(workBus.getFragmentManager(Mockito.<FragmentHandle>any())).thenReturn(manager);

      final DataConnectionConfig config =
        new DataConnectionConfig(c2.getAllocator(), c2, new DataServerRequestHandler(workBus, bee));
      final DataServer server = new DataServer(config);

      port = server.bind(port, true);
      DrillbitEndpoint ep = DrillbitEndpoint.newBuilder().setAddress("localhost").setDataPort(port).build();
      final DataConnectionManager connectionManager = new DataConnectionManager(ep, config);
      final DataTunnel tunnel = new DataTunnel(connectionManager);
      AtomicLong max = new AtomicLong(0);
      try {
        for (int i = 0; i < 40; i++) {
          long t1 = System.currentTimeMillis();
          tunnel.sendRecordBatch(new TimingOutcome(max),
            new FragmentWritableBatch(false, QueryId.getDefaultInstance(), 1, 1, 1, 1,
              getRandomBatch(c2.getAllocator(), 5000)));
        }
        assertTrue(max.get() > 2700);
        Thread.sleep(5000);
      } finally {
        server.close();
        connectionManager.close();
        c2.close();
      }
    }
  }

  @Test
  public void successEncryptionChunkMode()
    throws Exception {

    final WorkerBee bee = mock(WorkerBee.class);
    final WorkEventBus workBus = mock(WorkEventBus.class);

    try (
      ClusterFixture cluster = defaultClusterConfig()
        .configProperty(ExecConstants.BIT_ENCRYPTION_SASL_ENABLED, true)
        .configProperty(ExecConstants.BIT_ENCRYPTION_SASL_MAX_WRAPPED_SIZE, 100000)
        .build();
    ) {

      final ScanResult result = ClassPathScanner.fromPrescan(cluster.config());
      final BootStrapContext c2 =
        new BootStrapContext(cluster.config(), SystemOptionManager.createDefaultOptionDefinitions(), result);

      final FragmentManager manager = setupFragmentContextAndManager(c2.getAllocator());
      when(workBus.getFragmentManager(Mockito.<FragmentHandle>any())).thenReturn(manager);

      final DataConnectionConfig config = new DataConnectionConfig(c2.getAllocator(), c2,
        new DataServerRequestHandler(workBus, bee));
      final DataServer server = new DataServer(config);

      port = server.bind(port, true);
      final DrillbitEndpoint ep = DrillbitEndpoint.newBuilder().setAddress("localhost").setDataPort(port).build();
      final DataConnectionManager connectionManager = new DataConnectionManager(ep, config);
      final DataTunnel tunnel = new DataTunnel(connectionManager);
      AtomicLong max = new AtomicLong(0);

      try {
        for (int i = 0; i < 40; i++) {
          long t1 = System.currentTimeMillis();
          tunnel.sendRecordBatch(new TimingOutcome(max),
            new FragmentWritableBatch(false, QueryId.getDefaultInstance(), 1, 1, 1, 1,
              getRandomBatch(c2.getAllocator(), 5000)));
        }
        assertTrue(max.get() > 2700);
        Thread.sleep(5000);
      } catch (Exception | AssertionError ex) {
        fail();
      } finally {
        server.close();
        connectionManager.close();
        c2.close();
      }
    }
  }

  @Test
  public void failureEncryptionOnlyPlainMechanism() throws Exception {
    try {
      defaultClusterConfig()
        .configNonStringProperty(ExecConstants.AUTHENTICATION_MECHANISMS, Lists.newArrayList("plain"))
        .configProperty(ExecConstants.BIT_ENCRYPTION_SASL_ENABLED, true)
        .build()
        .close();

      fail();
    } catch(Exception ex) {
      assertTrue(ex.getCause() instanceof DrillbitStartupException);
    }
  }

  /**
   * Test to validate that a query which is running only on local Foreman node runs fine even if the Bit-Bit
   * Auth config is wrong. With DRILL-5721, all the local fragment setup and status update
   * doesn't happen over Control tunnel but instead happens locally. Without the fix in DRILL-5721 these queries will
   * hang.
   *
   * This test only starts up 1 Drillbit so that all fragments are scheduled on Foreman Drillbit node
   * @throws Exception
   */
  @Test
  public void localQuerySuccessWithWrongBitAuthConfig() throws Exception {
    try (
      ClusterFixture cluster = defaultClusterConfig()
        .configProperty(ExecConstants.USER_AUTHENTICATION_ENABLED, true)
        .configProperty(ExecConstants.USER_AUTHENTICATOR_IMPL, UserAuthenticatorTestImpl.TYPE)
        .configNonStringProperty(
          ExecConstants.AUTHENTICATION_MECHANISMS,
          Lists.newArrayList("plain", "kerberos")
        )
        .configProperty(ExecConstants.USE_LOGIN_PRINCIPAL, false)
        .build();
      ClientFixture client = cluster.clientBuilder()
      .property(DrillProperties.SERVICE_PRINCIPAL, krbHelper.SERVER_PRINCIPAL)
      .property(DrillProperties.USER, krbHelper.CLIENT_PRINCIPAL)
      .property(DrillProperties.KEYTAB, krbHelper.clientKeytab.getAbsolutePath())
      .build()
    ) {
      // Run a query using the new client
      client.queryBuilder().sqlResource("queries/tpch/01.sql").run();
    }
  }

  /**
   * Test to validate that query setup fails while scheduling remote fragments when multiple Drillbits are running with
   * wrong Bit-to-Bit Authentication configuration.
   *
   * This test starts up 2 Drillbit so that there are combination of local and remote fragments for query
   * execution. Note: When test runs with wrong config then for control connection Drillbit's uses wrong
   * service principal to talk to another Drillbit, and due to this Kerby server also fails with NullPointerException.
   * But for unit testing this should be fine.
   * @throws Exception
   */
  @Test
  public void queryFailureWithWrongBitAuthConfig() throws Exception {
    try {
      try (
        ClusterFixture cluster = defaultClusterConfig()
          .clusterSize(2)
          .configProperty(ExecConstants.USER_AUTHENTICATION_ENABLED, true)
          .configProperty(ExecConstants.USER_AUTHENTICATOR_IMPL, UserAuthenticatorTestImpl.TYPE)
          .configNonStringProperty(
            ExecConstants.AUTHENTICATION_MECHANISMS,
            Lists.newArrayList("plain", "kerberos")
          )
          .configProperty(ExecConstants.USE_LOGIN_PRINCIPAL, false)
          .build();
        ClientFixture client = cluster.clientBuilder()
        .property(DrillProperties.SERVICE_PRINCIPAL, krbHelper.SERVER_PRINCIPAL)
        .property(DrillProperties.USER, krbHelper.CLIENT_PRINCIPAL)
        .property(DrillProperties.KEYTAB, krbHelper.clientKeytab.getAbsolutePath())
        .build()
      ) {
        client.alterSession("planner.slice_target", 10);
        client.queryBuilder().sqlResource("queries/tpch/01.sql").run();
        fail();
      }
    } catch(Exception ex) {
      assertTrue(ex instanceof UserRemoteException);
      assertTrue(((UserRemoteException)ex).getErrorType() == UserBitShared.DrillPBError.ErrorType.CONNECTION);
    }
  }

  @AfterClass
  public static void cleanTest() throws Exception {
    krbHelper.stopKdc();
  }

  public static class MockFragmentManager implements FragmentManager
  {
    private int v = 0;
    private final FragmentContextImpl fragmentContext;

    public MockFragmentManager(final FragmentContextImpl fragmentContext)
    {
      this.fragmentContext = Preconditions.checkNotNull(fragmentContext);
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
      RawFragmentBatch rfb = batch.newRawFragmentBatch(fragmentContext.getAllocator());
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
      return fragmentContext;
    }

    @Override
    public void receivingFragmentFinished(FragmentHandle handle) {
    }
  }
}
