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
package org.apache.drill.exec.service;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import io.netty.channel.EventLoopGroup;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.drill.common.AutoCloseables;
import org.apache.drill.common.util.DrillVersionInfo;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.exception.DrillbitStartupException;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint.State;
import org.apache.drill.exec.rpc.TransportCheck;
import org.apache.drill.exec.rpc.control.Controller;
import org.apache.drill.exec.rpc.control.ControllerImpl;
import org.apache.drill.exec.rpc.data.DataConnectionCreator;
import org.apache.drill.exec.rpc.user.UserServer;
import org.apache.drill.exec.server.BootStrapContext;
import org.apache.drill.exec.work.WorkManager;

import org.apache.drill.shaded.guava.com.google.common.base.Stopwatch;

public class ServiceEngine implements AutoCloseable {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ServiceEngine.class);

  private final UserServer userServer;
  private final Controller controller;
  private final DataConnectionCreator dataPool;

  private final String hostName;
  private final int intialUserPort;
  private final boolean allowPortHunting;
  private final boolean isDistributedMode;

  private final BufferAllocator userAllocator;
  private final BufferAllocator controlAllocator;
  private final BufferAllocator dataAllocator;

  private int userPort;

  public ServiceEngine(final WorkManager manager, final BootStrapContext context,
                       final boolean allowPortHunting, final boolean isDistributedMode)
      throws DrillbitStartupException {
    userAllocator = newAllocator(context, "rpc:user", "drill.exec.rpc.user.server.memory.reservation",
        "drill.exec.rpc.user.server.memory.maximum");
    controlAllocator = newAllocator(context, "rpc:bit-control",
        "drill.exec.rpc.bit.server.memory.control.reservation", "drill.exec.rpc.bit.server.memory.control.maximum");
    dataAllocator = newAllocator(context, "rpc:bit-data",
        "drill.exec.rpc.bit.server.memory.data.reservation", "drill.exec.rpc.bit.server.memory.data.maximum");

    final EventLoopGroup eventLoopGroup = TransportCheck.createEventLoopGroup(
        context.getConfig().getInt(ExecConstants.USER_SERVER_RPC_THREADS), "UserServer-");
    userServer = new UserServer(context, userAllocator, eventLoopGroup, manager.getUserWorker());
    controller = new ControllerImpl(context, controlAllocator, manager.getControlMessageHandler());
    dataPool = new DataConnectionCreator(context, dataAllocator, manager.getWorkBus(), manager.getBee());

    hostName = context.getHostName();
    intialUserPort = context.getConfig().getInt(ExecConstants.INITIAL_USER_PORT);
    this.allowPortHunting = allowPortHunting;
    this.isDistributedMode = isDistributedMode;
  }

  private static BufferAllocator newAllocator(
      BootStrapContext context, String name, String initReservation, String maxAllocation) {
    return context.getAllocator().newChildAllocator(
        name, context.getConfig().getLong(initReservation), context.getConfig().getLong(maxAllocation));
  }

  public DrillbitEndpoint start() throws DrillbitStartupException, UnknownHostException {
    // loopback address check
    if (isDistributedMode && InetAddress.getByName(hostName).isLoopbackAddress()) {
      throw new DrillbitStartupException("Drillbit is disallowed to bind to loopback address in distributed mode.");
    }

    userPort = userServer.bind(intialUserPort, allowPortHunting);
    DrillbitEndpoint partialEndpoint = DrillbitEndpoint.newBuilder()
        .setAddress(hostName)
        .setUserPort(userPort)
        .setVersion(DrillVersionInfo.getVersion())
        .setState(State.STARTUP)
        .build();

    partialEndpoint = controller.start(partialEndpoint, allowPortHunting);
    return dataPool.start(partialEndpoint, allowPortHunting);
  }

  public int getUserPort() {
    return userPort;
  }

  public DataConnectionCreator getDataConnectionCreator(){
    return dataPool;
  }

  public Controller getController() {
    return controller;
  }

  private void submit(Executor p, final String name, final AutoCloseable c) {
    p.execute(new Runnable() {
      @Override
      public void run() {
        Stopwatch watch = Stopwatch.createStarted();
        try {
          c.close();
        } catch (Exception e) {
          logger.warn("Failure while closing {}.", name, e);
        }
        long elapsed = watch.elapsed(MILLISECONDS);
        if (elapsed > 500) {
          logger.info("closed " + name + " in " + elapsed + " ms");
        }
      }
    });
  }

  @Override
  public void close() throws Exception {
    // this takes time so close them in parallel
    // Ideally though we fix this netty bug: https://github.com/netty/netty/issues/2545
    ExecutorService p = Executors.newFixedThreadPool(2);
    submit(p, "userServer", userServer);
    submit(p, "dataPool", dataPool);
    submit(p, "controller", controller);
    p.shutdown();
    try {
      p.awaitTermination(3, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    AutoCloseables.close(userAllocator, controlAllocator, dataAllocator);

  }
}
