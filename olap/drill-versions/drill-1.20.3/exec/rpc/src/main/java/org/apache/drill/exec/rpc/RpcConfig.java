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
package org.apache.drill.exec.rpc;

import java.util.Map;
import java.util.concurrent.Executor;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableMap;
import org.apache.drill.shaded.guava.com.google.common.collect.Maps;
import com.google.protobuf.Internal.EnumLite;
import com.google.protobuf.MessageLite;

public class RpcConfig {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(RpcConfig.class);

  private final String name;
  private final int timeout;
  private final Map<EnumLite, RpcMessageType<?, ?, ?>> sendMap;
  private final Map<Integer, RpcMessageType<?, ?, ?>> receiveMap;
  private final Executor executor;

  private RpcConfig(String name, Map<EnumLite, RpcMessageType<?, ?, ?>> sendMap,
      Map<Integer, RpcMessageType<?, ?, ?>> receiveMap, int timeout, Executor executor) {
    Preconditions.checkNotNull(executor, "Executor must be defined.");
    this.name = name;
    this.timeout = timeout;
    this.sendMap = ImmutableMap.copyOf(sendMap);
    this.receiveMap = ImmutableMap.copyOf(receiveMap);
    this.executor = executor;
  }

  public String getName() {
    return name;
  }

  public int getTimeout() {
    return timeout;
  }

  public boolean hasTimeout() {
    return timeout > 0;
  }

  public Executor getExecutor() {
    return executor;
  }

  public boolean checkReceive(int rpcType, Class<?> receiveClass) {
    if (RpcConstants.EXTRA_DEBUGGING) {
      logger.debug(String.format("Checking reception for rpcType %d and receive class %s.", rpcType, receiveClass));
    }
    RpcMessageType<?,?,?> type = receiveMap.get(rpcType);
    if (type == null) {
      throw new IllegalStateException(String.format("%s: There is no defined RpcMessage type for a Rpc receive type number of %s.", name, rpcType));
    }

    if (receiveClass != type.getRet()) {
      throw new IllegalStateException(String.format("%s: The definition for receive doesn't match implementation code.  The definition is %s however the current receive for this type was of type %s.", name, type, receiveClass.getCanonicalName()));
    }
    return true;
  }

  public boolean checkSend(EnumLite send, Class<?> sendClass, Class<?> receiveClass) {
    if (RpcConstants.EXTRA_DEBUGGING) {
      logger.debug(String.format("Checking send classes for send RpcType %s.  Send Class is %s and Receive class is %s.", send, sendClass, receiveClass));
    }
    RpcMessageType<?,?,?> type = sendMap.get(send);
    if (type == null) {
      throw new IllegalStateException(String.format("%s: There is no defined RpcMessage type for a Rpc send type of %s.", name, send));
    }

    if (type.getSend() != sendClass) {
      throw new IllegalStateException(String.format("%s: The definition for send doesn't match implementation code.  The definition is %s however the current send is trying to send an object of type %s.", name, type, sendClass.getCanonicalName()));
    }
    if (type.getRet() != receiveClass) {
      throw new IllegalStateException(String.format("%s: The definition for send doesn't match implementation code.  The definition is %s however the current send is trying to setup an expected reception of an object of type %s.", name, type, receiveClass.getCanonicalName()));
    }

    return true;
  }

  public boolean checkResponseSend(EnumLite responseType, Class<?> responseClass) {
    if (RpcConstants.EXTRA_DEBUGGING) {
      logger.debug(String.format("Checking responce send of type %s with response class of %s.",  responseType, responseClass));
    }
    RpcMessageType<?,?,?> type = receiveMap.get(responseType.getNumber());
    if (type == null) {
      throw new IllegalStateException(String.format("%s: There is no defined RpcMessage type for a Rpc response of type %s.", name, responseType));
    }
    if (type.getRet() != responseClass) {
      throw new IllegalStateException(String.format("%s: The definition for the response doesn't match implementation code.  The definition is %s however the current response is trying to response with an object of type %s.", name, type, responseClass.getCanonicalName()));
    }

    return true;
  }

  public static class RpcMessageType<SEND extends MessageLite, RECEIVE extends MessageLite, T extends EnumLite>{
    private T sendEnum;
    private Class<SEND> send;
    private T receiveEnum;
    private Class<RECEIVE> ret;
    public RpcMessageType(T sendEnum, Class<SEND> send, T receiveEnum, Class<RECEIVE> ret) {
      super();
      this.sendEnum = sendEnum;
      this.send = send;
      this.receiveEnum = receiveEnum;
      this.ret = ret;
    }
    public Class<SEND> getSend() {
      return send;
    }
    public void setSend(Class<SEND> send) {
      this.send = send;
    }
    public T getSendEnum() {
      return sendEnum;
    }
    public void setSendEnum(T sendEnum) {
      this.sendEnum = sendEnum;
    }
    public Class<RECEIVE> getRet() {
      return ret;
    }
    public void setRet(Class<RECEIVE> ret) {
      this.ret = ret;
    }
    public T getReceiveEnum() {
      return receiveEnum;
    }
    public void setReceiveEnum(T receiveEnum) {
      this.receiveEnum = receiveEnum;
    }
    @Override
    public String toString() {
      return "RpcMessageType [sendEnum=" + sendEnum + ", send=" + send + ", receiveEnum=" + receiveEnum + ", ret="
          + ret + "]";
    }

  }

  public static RpcConfigBuilder newBuilder() {
    return new RpcConfigBuilder();
  }

  public static class RpcConfigBuilder {
    private String name;
    private int timeout = -1;
    private Executor executor;
    private Map<EnumLite, RpcMessageType<?, ?, ?>> sendMap = Maps.newHashMap();
    private Map<Integer, RpcMessageType<?, ?, ?>> receiveMap = Maps.newHashMap();

    private RpcConfigBuilder() {
    }

    public RpcConfigBuilder name(String name) {
      this.name = name;
      return this;
    }

    public RpcConfigBuilder timeout(int timeoutInSeconds) {
      this.timeout = timeoutInSeconds;
      return this;
    }

    public <SEND extends MessageLite, RECEIVE extends MessageLite, T extends EnumLite>  RpcConfigBuilder add(T sendEnum, Class<SEND> send, T receiveEnum, Class<RECEIVE> rec) {
      RpcMessageType<SEND, RECEIVE, T> type = new RpcMessageType<SEND, RECEIVE, T>(sendEnum, send, receiveEnum, rec);
      this.sendMap.put(sendEnum, type);
      this.receiveMap.put(receiveEnum.getNumber(), type);
      return this;
    }

    public RpcConfigBuilder executor(Executor executor) {
      this.executor = executor;
      return this;
    }

    public RpcConfig build() {
      Preconditions.checkArgument(timeout > -1, "Timeout must be a positive number or zero for disabled.");
      Preconditions.checkArgument(name != null, "RpcConfig name must be set.");
      return new RpcConfig(name, sendMap, receiveMap, timeout, executor);
    }

  }

}
