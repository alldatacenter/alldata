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

package org.apache.celeborn.common.network;

import static org.apache.celeborn.common.util.JavaUtils.getLocalHost;
import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Throwables;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.apache.celeborn.common.CelebornConf;
import org.apache.celeborn.common.network.buffer.NioManagedBuffer;
import org.apache.celeborn.common.network.client.RpcResponseCallback;
import org.apache.celeborn.common.network.client.TransportClient;
import org.apache.celeborn.common.network.client.TransportClientFactory;
import org.apache.celeborn.common.network.protocol.*;
import org.apache.celeborn.common.network.server.BaseMessageHandler;
import org.apache.celeborn.common.network.server.TransportServer;
import org.apache.celeborn.common.network.util.TransportConf;
import org.apache.celeborn.common.util.JavaUtils;

public class RpcIntegrationSuiteJ {
  static TransportConf conf;
  static TransportServer server;
  static TransportClientFactory clientFactory;
  static BaseMessageHandler handler;
  static List<String> oneWayMsgs;
  static StreamTestHelper testData;

  @BeforeClass
  public static void setUp() throws Exception {
    conf = new TransportConf("shuffle", new CelebornConf());
    testData = new StreamTestHelper();
    handler =
        new BaseMessageHandler() {
          @Override
          public void receive(TransportClient client, RequestMessage message) {
            if (message instanceof RpcRequest) {
              String msg;
              RpcRequest r = (RpcRequest) message;
              RpcResponseCallback callback =
                  new RpcResponseCallback() {
                    @Override
                    public void onSuccess(ByteBuffer response) {
                      client
                          .getChannel()
                          .writeAndFlush(
                              new RpcResponse(r.requestId, new NioManagedBuffer(response)));
                    }

                    @Override
                    public void onFailure(Throwable e) {
                      client
                          .getChannel()
                          .writeAndFlush(
                              new RpcFailure(r.requestId, Throwables.getStackTraceAsString(e)));
                    }
                  };
              try {
                msg = JavaUtils.bytesToString(message.body().nioByteBuffer());
              } catch (Exception e) {
                throw new RuntimeException(e);
              }
              String[] parts = msg.split("/");
              if (parts[0].equals("hello")) {
                callback.onSuccess(JavaUtils.stringToBytes("Hello, " + parts[1] + "!"));
              } else if (parts[0].equals("return error")) {
                callback.onFailure(new RuntimeException("Returned: " + parts[1]));
              } else if (parts[0].equals("throw error")) {
                callback.onFailure(new RuntimeException("Thrown: " + parts[1]));
              }
            } else if (message instanceof OneWayMessage) {
              String msg;
              try {
                msg = JavaUtils.bytesToString(message.body().nioByteBuffer());
              } catch (Exception e) {
                throw new RuntimeException(e);
              }
              oneWayMsgs.add(msg);
            }
          }

          @Override
          public boolean checkRegistered() {
            return true;
          }
        };
    TransportContext context = new TransportContext(conf, handler);
    server = context.createServer();
    clientFactory = context.createClientFactory();
    oneWayMsgs = new ArrayList<>();
  }

  @AfterClass
  public static void tearDown() {
    server.close();
    clientFactory.close();
    testData.cleanup();
  }

  static class RpcResult {
    public Set<String> successMessages;
    public Set<String> errorMessages;
  }

  private RpcResult sendRPC(String... commands) throws Exception {
    TransportClient client = clientFactory.createClient(getLocalHost(), server.getPort());
    final Semaphore sem = new Semaphore(0);

    final RpcResult res = new RpcResult();
    res.successMessages = Collections.synchronizedSet(new HashSet<String>());
    res.errorMessages = Collections.synchronizedSet(new HashSet<String>());

    RpcResponseCallback callback =
        new RpcResponseCallback() {
          @Override
          public void onSuccess(ByteBuffer message) {
            String response = JavaUtils.bytesToString(message);
            res.successMessages.add(response);
            sem.release();
          }

          @Override
          public void onFailure(Throwable e) {
            res.errorMessages.add(e.getMessage());
            sem.release();
          }
        };

    for (String command : commands) {
      client.sendRpc(JavaUtils.stringToBytes(command), callback);
    }

    if (!sem.tryAcquire(commands.length, 5, TimeUnit.SECONDS)) {
      fail("Timeout getting response from the server");
    }
    client.close();
    return res;
  }

  private static class RpcStreamCallback implements RpcResponseCallback {
    final String streamId;
    final RpcResult res;
    final Semaphore sem;

    RpcStreamCallback(String streamId, RpcResult res, Semaphore sem) {
      this.streamId = streamId;
      this.res = res;
      this.sem = sem;
    }

    @Override
    public void onSuccess(ByteBuffer message) {
      res.successMessages.add(streamId);
      sem.release();
    }

    @Override
    public void onFailure(Throwable e) {
      res.errorMessages.add(e.getMessage());
      sem.release();
    }
  }

  @Test
  public void singleRPC() throws Exception {
    RpcResult res = sendRPC("hello/Aaron");
    assertEquals(res.successMessages, Sets.newHashSet("Hello, Aaron!"));
    assertTrue(res.errorMessages.isEmpty());
  }

  @Test
  public void doubleRPC() throws Exception {
    RpcResult res = sendRPC("hello/Aaron", "hello/Reynold");
    assertEquals(res.successMessages, Sets.newHashSet("Hello, Aaron!", "Hello, Reynold!"));
    assertTrue(res.errorMessages.isEmpty());
  }

  @Test
  public void returnErrorRPC() throws Exception {
    RpcResult res = sendRPC("return error/OK");
    assertTrue(res.successMessages.isEmpty());
    assertErrorsContain(res.errorMessages, Sets.newHashSet("Returned: OK"));
  }

  @Test
  public void throwErrorRPC() throws Exception {
    RpcResult res = sendRPC("throw error/uh-oh");
    assertTrue(res.successMessages.isEmpty());
    assertErrorsContain(res.errorMessages, Sets.newHashSet("Thrown: uh-oh"));
  }

  @Test
  public void doubleTrouble() throws Exception {
    RpcResult res = sendRPC("return error/OK", "throw error/uh-oh");
    assertTrue(res.successMessages.isEmpty());
    assertErrorsContain(res.errorMessages, Sets.newHashSet("Returned: OK", "Thrown: uh-oh"));
  }

  @Test
  public void sendSuccessAndFailure() throws Exception {
    RpcResult res = sendRPC("hello/Bob", "throw error/the", "hello/Builder", "return error/!");
    assertEquals(res.successMessages, Sets.newHashSet("Hello, Bob!", "Hello, Builder!"));
    assertErrorsContain(res.errorMessages, Sets.newHashSet("Thrown: the", "Returned: !"));
  }

  @Test
  public void sendOneWayMessage() throws Exception {
    final String message = "no reply";
    TransportClient client = clientFactory.createClient(getLocalHost(), server.getPort());
    try {
      client.send(JavaUtils.stringToBytes(message));
      assertEquals(0, client.getHandler().numOutstandingRequests());

      // Make sure the message arrives.
      long deadline = System.nanoTime() + TimeUnit.NANOSECONDS.convert(10, TimeUnit.SECONDS);
      while (System.nanoTime() < deadline && oneWayMsgs.size() == 0) {
        TimeUnit.MILLISECONDS.sleep(10);
      }

      assertEquals(1, oneWayMsgs.size());
      assertEquals(message, oneWayMsgs.get(0));
    } finally {
      client.close();
    }
  }

  private void assertErrorsContain(Set<String> errors, Set<String> contains) {
    assertEquals(
        "Expected " + contains.size() + " errors, got " + errors.size() + "errors: " + errors,
        contains.size(),
        errors.size());

    Pair<Set<String>, Set<String>> r = checkErrorsContain(errors, contains);
    assertTrue(
        "Could not find error containing " + r.getRight() + "; errors: " + errors,
        r.getRight().isEmpty());

    assertTrue(r.getLeft().isEmpty());
  }

  private void assertErrorAndClosed(RpcResult result, String expectedError) {
    assertTrue("unexpected success: " + result.successMessages, result.successMessages.isEmpty());
    Set<String> errors = result.errorMessages;
    assertEquals("Expected 2 errors, got " + errors.size() + "errors: " + errors, 2, errors.size());

    // We expect 1 additional error due to closed connection and here are possible keywords in the
    // error message.
    Set<String> possibleClosedErrors =
        Sets.newHashSet(
            "closed",
            "Connection reset",
            "java.nio.channels.ClosedChannelException",
            "java.io.IOException: Broken pipe");
    Set<String> containsAndClosed = Sets.newHashSet(expectedError);
    containsAndClosed.addAll(possibleClosedErrors);

    Pair<Set<String>, Set<String>> r = checkErrorsContain(errors, containsAndClosed);

    assertTrue("Got a non-empty set " + r.getLeft(), r.getLeft().isEmpty());

    Set<String> errorsNotFound = r.getRight();
    assertEquals(
        "The size of " + errorsNotFound + " was not " + (possibleClosedErrors.size() - 1),
        possibleClosedErrors.size() - 1,
        errorsNotFound.size());
    for (String err : errorsNotFound) {
      assertTrue("Found a wrong error " + err, containsAndClosed.contains(err));
    }
  }

  private Pair<Set<String>, Set<String>> checkErrorsContain(
      Set<String> errors, Set<String> contains) {
    Set<String> remainingErrors = Sets.newHashSet(errors);
    Set<String> notFound = Sets.newHashSet();
    for (String contain : contains) {
      Iterator<String> it = remainingErrors.iterator();
      boolean foundMatch = false;
      while (it.hasNext()) {
        if (it.next().contains(contain)) {
          it.remove();
          foundMatch = true;
          break;
        }
      }
      if (!foundMatch) {
        notFound.add(contain);
      }
    }
    return new ImmutablePair<>(remainingErrors, notFound);
  }
}
