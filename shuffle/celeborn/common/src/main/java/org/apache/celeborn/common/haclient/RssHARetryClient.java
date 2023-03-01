/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.celeborn.common.haclient;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nullable;

import scala.Tuple2;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.reflect.ClassTag$;

import com.google.common.util.concurrent.Uninterruptibles;
import com.google.protobuf.GeneratedMessageV3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.celeborn.common.CelebornConf;
import org.apache.celeborn.common.protocol.RpcNameConstants;
import org.apache.celeborn.common.protocol.message.ControlMessages.OneWayMessageResponse$;
import org.apache.celeborn.common.protocol.message.MasterRequestMessage;
import org.apache.celeborn.common.protocol.message.Message;
import org.apache.celeborn.common.rpc.RpcAddress;
import org.apache.celeborn.common.rpc.RpcEndpointRef;
import org.apache.celeborn.common.rpc.RpcEnv;
import org.apache.celeborn.common.rpc.RpcTimeout;
import org.apache.celeborn.common.util.ThreadUtils;

public class RssHARetryClient {
  private static final Logger LOG = LoggerFactory.getLogger(RssHARetryClient.class);

  private final RpcEnv rpcEnv;
  private final String[] masterEndpoints;
  private final int maxRetries;

  private final RpcTimeout rpcTimeout;

  private final AtomicReference<RpcEndpointRef> rpcEndpointRef;
  private final ExecutorService oneWayMessageSender;

  public RssHARetryClient(RpcEnv rpcEnv, CelebornConf conf) {
    this.rpcEnv = rpcEnv;
    this.masterEndpoints = conf.masterEndpoints();
    this.maxRetries = Math.max(masterEndpoints.length, conf.clientMaxTries());
    this.rpcTimeout = conf.haClientRpcAskTimeout();
    this.rpcEndpointRef = new AtomicReference<>();
    this.oneWayMessageSender = ThreadUtils.newDaemonSingleThreadExecutor("One-Way-Message-Sender");
  }

  private static final String SPLITER = "#";
  private static final AtomicLong CALL_ID_COUNTER = new AtomicLong();

  static long nextCallId() {
    return CALL_ID_COUNTER.getAndIncrement() & Long.MAX_VALUE;
  }

  public static Tuple2<String, Long> decodeRequestId(String requestId) {
    if (requestId.contains(SPLITER)) {
      return new Tuple2<>(requestId.split(SPLITER)[0], Long.valueOf(requestId.split(SPLITER)[1]));
    } else {
      return null;
    }
  }

  public static String encodeRequestId(String uuid, long callId) {
    return String.format("%s%s%d", uuid, SPLITER, callId);
  }

  /**
   * For message sent by Master itself like ApplicationLost or WorkerLost, we should set requestId
   * manually.
   *
   * @return
   */
  public static String genRequestId() {
    return encodeRequestId(UUID.randomUUID().toString(), nextCallId());
  }

  public void send(Message message) throws Throwable {
    // Send a one-way message. Because we need to know whether the leader between Masters has
    // switched, we must adopt a synchronous method, but for a one-way message, we don't care
    // whether it can be sent successfully, so we adopt an asynchronous method. Therefore, we
    // choose to use one Thread pool to use synchronization.
    oneWayMessageSender.submit(
        () -> {
          try {
            sendMessageInner(message, OneWayMessageResponse$.class);
          } catch (Throwable e) {
            LOG.warn("Exception occurs while send one-way message.", e);
          }
        });
    LOG.debug("Send one-way message {}.", message);
  }

  public <T> T askSync(Message message, Class<T> clz) throws Throwable {
    return sendMessageInner(message, clz);
  }

  public <T> T askSync(GeneratedMessageV3 message, Class<T> clz) throws Throwable {
    return sendMessageInner(message, clz);
  }

  public void close() {
    ThreadUtils.shutdown(oneWayMessageSender, Duration.apply("800ms"));
  }

  @SuppressWarnings("UnstableApiUsage")
  private <T> T sendMessageInner(Object message, Class<T> clz) throws Throwable {
    Throwable throwable = null;
    int numTries = 0;
    boolean shouldRetry = true;
    if (message instanceof MasterRequestMessage) {
      ((MasterRequestMessage) message)
          .requestId_(encodeRequestId(UUID.randomUUID().toString(), nextCallId()));
    }

    LOG.debug("Send rpc message " + message);
    RpcEndpointRef endpointRef = null;
    // Use AtomicInteger or Integer or any Object which holds an int value is ok, we just need to
    // transfer a object to get the change of the current index of master addresses.
    AtomicInteger currentMasterIdx = new AtomicInteger(0);

    long sleepLimitTime = 2000; // 2s
    while (numTries < maxRetries && shouldRetry) {
      try {
        endpointRef = getOrSetupRpcEndpointRef(currentMasterIdx);
        Future<T> future = endpointRef.ask(message, rpcTimeout, ClassTag$.MODULE$.apply(clz));
        return rpcTimeout.awaitResult(future);
      } catch (Throwable e) {
        throwable = e;
        shouldRetry = shouldRetry(endpointRef, throwable);
        if (shouldRetry) {
          numTries++;

          Uninterruptibles.sleepUninterruptibly(
              Math.min(numTries * 100L, sleepLimitTime), TimeUnit.MILLISECONDS);
        }
      }
    }

    LOG.error("Send rpc with failure, has tried {}, max try {}!", numTries, maxRetries, throwable);
    throw throwable;
  }

  private boolean shouldRetry(@Nullable RpcEndpointRef oldRef, Throwable e) {
    // It will always throw rss exception , so we need to get the cause
    // 'RssException: Exception thrown in awaitResult'
    if (e.getCause() instanceof MasterNotLeaderException) {
      MasterNotLeaderException exception = (MasterNotLeaderException) e.getCause();
      String leaderAddr = exception.getSuggestedLeaderAddress();
      if (!leaderAddr.equals(MasterNotLeaderException.LEADER_NOT_PRESENTED)) {
        setRpcEndpointRef(leaderAddr);
      } else {
        LOG.warn("Master leader is not present currently, please check masters' status!");
      }
      return true;
    } else if (e.getCause() instanceof IOException) {
      resetRpcEndpointRef(oldRef);
      return true;
    }
    return false;
  }

  private void setRpcEndpointRef(String masterEndpoint) {
    // This method should never care newer or old value, we just set the suggest master endpoint.
    // If an error occurs when setting the suggest Master, it means that the Master may be down.
    // At this time, we just set `rpcEndpointRef` to null. Then next time, we will re-select the
    // Master and get the correct leader.
    rpcEndpointRef.set(setupEndpointRef(masterEndpoint));
    LOG.info("Fail over to master {}.", masterEndpoint);
  }

  private void resetRpcEndpointRef(@Nullable RpcEndpointRef oldRef) {
    // Only if current rpcEndPointRef equals to oldRef, we could set it to null.
    if (rpcEndpointRef.compareAndSet(oldRef, null)) {
      LOG.debug("Reset the connection to master {}.", oldRef != null ? oldRef.address() : "null");
    }
  }

  /**
   * This method is used to obtain a non-empty RpcEndpointRef.
   *
   * <p>First, determine whether the global `rpcEndpointRef` is empty, and if it is not empty,
   * return directly.
   *
   * <p>When `rpcEndpointRef` is empty, we need to assign a value to it and return this value; but
   * because it is a multi-threaded environment, we need to ensure that the old value is still empty
   * when setting the value of `rpcEndpointRef`, otherwise we should use the new value of
   * `rpcEndpointRef`. Only if the setting is successful, update `currentIndex` to ensure that all
   * Masters can be used.
   *
   * <p>This method must be the only entry to get RpcEndpointRef, otherwise it is difficult to
   * ensure thread safety.
   *
   * @param currentIndex current attempt master address index.
   * @throws IllegalStateException If after several attempts, the non-empty RpcEndpointRef still
   *     cannot be obtained.
   * @return non-empty RpcEndpointRef.
   */
  private RpcEndpointRef getOrSetupRpcEndpointRef(AtomicInteger currentIndex) {
    RpcEndpointRef endpointRef = rpcEndpointRef.get();
    if (endpointRef == null) {
      int index = currentIndex.get();
      do {
        RpcEndpointRef tempEndpointRef = setupEndpointRef(masterEndpoints[index]);
        if (rpcEndpointRef.compareAndSet(null, tempEndpointRef)) {
          index = (index + 1) % masterEndpoints.length;
        }
        endpointRef = rpcEndpointRef.get();
      } while (endpointRef == null && index != currentIndex.get());

      currentIndex.set(index);

      if (endpointRef == null) {
        throw new IllegalStateException(
            "After trying all the available Master Addresses,"
                + " an usable link still couldn't be created.");
      } else {
        LOG.info("connect to master {}.", endpointRef.address());
      }
    }
    return endpointRef;
  }

  private RpcEndpointRef setupEndpointRef(String endpoint) {
    RpcEndpointRef endpointRef = null;
    try {
      endpointRef =
          rpcEnv.setupEndpointRef(RpcAddress.fromHostAndPort(endpoint), RpcNameConstants.MASTER_EP);
    } catch (Exception e) {
      // Catch all exceptions. Because we don't care whether this exception is IOException or
      // TimeoutException or other exceptions, so we just try to connect to host:port, if fail,
      // we try next address.
      LOG.warn("Connect to {} failed.", endpoint, e);
    }
    return endpointRef;
  }
}
