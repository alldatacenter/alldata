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
package org.apache.drill.exec.rpc.control;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.MessageLite;
import com.google.protobuf.Parser;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.DrillBuf;
import org.apache.drill.exec.proto.BitControl.CustomMessage;
import org.apache.drill.exec.proto.BitControl.FinishedReceiver;
import org.apache.drill.exec.proto.BitControl.FragmentStatus;
import org.apache.drill.exec.proto.BitControl.InitializeFragments;
import org.apache.drill.exec.proto.BitControl.RpcType;
import org.apache.drill.exec.proto.ExecProtos.FragmentHandle;
import org.apache.drill.exec.proto.GeneralRPCProtos.Ack;
import org.apache.drill.exec.proto.UserBitShared.QueryId;
import org.apache.drill.exec.proto.UserBitShared.QueryProfile;
import org.apache.drill.exec.rpc.DrillRpcFuture;
import org.apache.drill.exec.rpc.FutureBitCommand;
import org.apache.drill.exec.rpc.ListeningCommand;
import org.apache.drill.exec.rpc.RpcException;
import org.apache.drill.exec.rpc.RpcOutcomeListener;
import org.apache.drill.exec.rpc.control.Controller.CustomSerDe;

import java.util.concurrent.TimeUnit;

public class ControlTunnel {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ControlTunnel.class);

  private final ControlConnectionManager manager;

  public ControlTunnel(ControlConnectionManager manager) {
    this.manager = manager;
  }

  public void sendFragments(RpcOutcomeListener<Ack> outcomeListener, InitializeFragments fragments){
    SendFragment b = new SendFragment(outcomeListener, fragments);
    manager.runCommand(b);
  }

  public void cancelFragment(RpcOutcomeListener<Ack> outcomeListener, FragmentHandle handle){
    final SignalFragment b = new SignalFragment(outcomeListener, handle, RpcType.REQ_CANCEL_FRAGMENT);
    manager.runCommand(b);
  }

  public void unpauseFragment(final RpcOutcomeListener<Ack> outcomeListener, final FragmentHandle handle) {
    final SignalFragment b = new SignalFragment(outcomeListener, handle, RpcType.REQ_UNPAUSE_FRAGMENT);
    manager.runCommand(b);
  }

  public DrillRpcFuture<Ack> requestCancelQuery(QueryId queryId){
    CancelQuery c = new CancelQuery(queryId);
    manager.runCommand(c);
    return c.getFuture();
  }

  public void informReceiverFinished(RpcOutcomeListener<Ack> outcomeListener, FinishedReceiver finishedReceiver){
    ReceiverFinished b = new ReceiverFinished(outcomeListener, finishedReceiver);
    manager.runCommand(b);
  }

  public DrillRpcFuture<Ack> sendFragmentStatus(FragmentStatus status){
    SendFragmentStatus b = new SendFragmentStatus(status);
    manager.runCommand(b);
    return b.getFuture();
  }

  public DrillRpcFuture<QueryProfile> requestQueryProfile(QueryId queryId) {
    RequestProfile b = new RequestProfile(queryId);
    manager.runCommand(b);
    return b.getFuture();
  }

  public static class SendFragmentStatus extends FutureBitCommand<Ack, ControlConnection, RpcType, FragmentStatus> {
    final FragmentStatus status;

    public SendFragmentStatus(FragmentStatus status) {
      super();
      this.status = status;
    }

    @Override
    public void doRpcCall(RpcOutcomeListener<Ack> outcomeListener, ControlConnection connection) {
      connection.sendUnsafe(outcomeListener, getRpcType(), status, Ack.class);
    }

    @Override
    public RpcType getRpcType() {
      return RpcType.REQ_FRAGMENT_STATUS;
    }

    @Override
    public FragmentStatus getMessage() {
      return status;
    }

  }

  public static class ReceiverFinished extends ListeningCommand<Ack, ControlConnection, RpcType, FinishedReceiver> {
    final FinishedReceiver finishedReceiver;

    public ReceiverFinished(RpcOutcomeListener<Ack> listener, FinishedReceiver finishedReceiver) {
      super(listener);
      this.finishedReceiver = finishedReceiver;
    }

    @Override
    public void doRpcCall(RpcOutcomeListener<Ack> outcomeListener, ControlConnection connection) {
      connection.send(outcomeListener, getRpcType(), finishedReceiver, Ack.class);
    }

    @Override
    public RpcType getRpcType() {
      return RpcType.REQ_RECEIVER_FINISHED;
    }

    @Override
    public FinishedReceiver getMessage() {
      return finishedReceiver;
    }
  }

  public static class SignalFragment extends ListeningCommand<Ack, ControlConnection, RpcType, FragmentHandle> {
    final FragmentHandle handle;
    final RpcType type;

    public SignalFragment(RpcOutcomeListener<Ack> listener, FragmentHandle handle, RpcType type) {
      super(listener);
      this.handle = handle;
      this.type = type;
    }

    @Override
    public void doRpcCall(RpcOutcomeListener<Ack> outcomeListener, ControlConnection connection) {
      connection.sendUnsafe(outcomeListener, type, handle, Ack.class);
    }

    @Override
    public RpcType getRpcType() {
      return type;
    }

    @Override
    public FragmentHandle getMessage() {
      return handle;
    }
  }

  public static class SendFragment extends ListeningCommand<Ack, ControlConnection, RpcType, InitializeFragments> {
    final InitializeFragments fragments;

    public SendFragment(RpcOutcomeListener<Ack> listener, InitializeFragments fragments) {
      super(listener);
      this.fragments = fragments;
    }

    @Override
    public void doRpcCall(RpcOutcomeListener<Ack> outcomeListener, ControlConnection connection) {
      connection.send(outcomeListener, getRpcType(), fragments, Ack.class);
    }

    @Override
    public RpcType getRpcType() {
      return RpcType.REQ_INITIALIZE_FRAGMENTS;
    }

    @Override
    public InitializeFragments getMessage() {
      return fragments;
    }
  }

  public static class RequestProfile extends FutureBitCommand<QueryProfile, ControlConnection, RpcType, QueryId> {
    final QueryId queryId;

    public RequestProfile(QueryId queryId) {
      super();
      this.queryId = queryId;
    }

    @Override
    public void doRpcCall(RpcOutcomeListener<QueryProfile> outcomeListener, ControlConnection connection) {
      connection.send(outcomeListener, getRpcType(), queryId, QueryProfile.class);
    }

    @Override
    public RpcType getRpcType() {
      return RpcType.REQ_QUERY_STATUS;
    }

    @Override
    public QueryId getMessage() {
      return queryId;
    }
  }

  public static class CancelQuery extends FutureBitCommand<Ack, ControlConnection, RpcType, QueryId> {
    final QueryId queryId;

    public CancelQuery(QueryId queryId) {
      super();
      this.queryId = queryId;
    }

    @Override
    public void doRpcCall(RpcOutcomeListener<Ack> outcomeListener, ControlConnection connection) {
      connection.send(outcomeListener, getRpcType(), queryId, Ack.class);
    }

    @Override
    public RpcType getRpcType() {
      return RpcType.REQ_QUERY_CANCEL;
    }

    @Override
    public QueryId getMessage() {
      return queryId;
    }
  }

  @SuppressWarnings("unchecked")
  public <SEND extends MessageLite, RECEIVE extends MessageLite> CustomTunnel<SEND, RECEIVE> getCustomTunnel(
      int messageTypeId, Class<SEND> clazz, Parser<RECEIVE> parser) {
    return new CustomTunnel<SEND, RECEIVE>(
        messageTypeId,
        ((CustomSerDe<SEND>) new ProtoSerDe<Message>(null)),
        new ProtoSerDe<RECEIVE>(parser));
  }

  public <SEND, RECEIVE> CustomTunnel<SEND, RECEIVE> getCustomTunnel(
      int messageTypeId, CustomSerDe<SEND> send, CustomSerDe<RECEIVE> receive) {
    return new CustomTunnel<SEND, RECEIVE>(messageTypeId, send, receive);
  }

  public static class CustomMessageSender extends
    ListeningCommand<CustomMessage, ControlConnection, RpcType, CustomMessage> {

    private CustomMessage message;
    private ByteBuf[] dataBodies;

    public CustomMessageSender(RpcOutcomeListener<CustomMessage> listener, CustomMessage message, ByteBuf[] dataBodies) {
      super(listener);
      this.message = message;
      this.dataBodies = dataBodies;
    }

    @Override
    public void doRpcCall(RpcOutcomeListener<CustomMessage> outcomeListener, ControlConnection connection) {
      connection.send(outcomeListener, getRpcType(), message, CustomMessage.class, dataBodies);
    }

    @Override
    public RpcType getRpcType() {
      return RpcType.REQ_CUSTOM;
    }

    @Override
    public CustomMessage getMessage() {
      return message;
    }

    public ByteBuf[] getDataBodies() {
      return dataBodies;
    }

  }

  public static class SyncCustomMessageSender extends
    FutureBitCommand<CustomMessage, ControlConnection, RpcType, CustomMessage> {

    private CustomMessage message;
    private ByteBuf[] dataBodies;

    public SyncCustomMessageSender(CustomMessage message, ByteBuf[] dataBodies) {
      super();
      this.message = message;
      this.dataBodies = dataBodies;
    }

    @Override
    public void doRpcCall(RpcOutcomeListener<CustomMessage> outcomeListener, ControlConnection connection) {
      connection.send(outcomeListener, getRpcType(), message, CustomMessage.class, dataBodies);
    }

    @Override
    public RpcType getRpcType() {
      return RpcType.REQ_CUSTOM;
    }

    @Override
    public CustomMessage getMessage() {
      return message;
    }

    public ByteBuf[] getDataBodies() {
      return dataBodies;
    }
  }

  /**
   * A class used to return a synchronous future when doing custom rpc messages.
   * @param <RECEIVE>
   *          The type of message that will be returned.
   */
  public class CustomFuture<RECEIVE> {

    private final CustomSerDe<RECEIVE> serde;
    private final DrillRpcFuture<CustomMessage> future;

    public CustomFuture(CustomSerDe<RECEIVE> serde, DrillRpcFuture<CustomMessage> future) {
      super();
      this.serde = serde;
      this.future = future;
    }

    public RECEIVE get() throws Exception {
      CustomMessage message = future.checkedGet();
      return serde.deserializeReceived(message.getMessage().toByteArray());
    }

    public RECEIVE get(long timeout, TimeUnit unit) throws Exception, InvalidProtocolBufferException {
      CustomMessage message = future.checkedGet(timeout, unit);
      return serde.deserializeReceived(message.getMessage().toByteArray());
    }

    public DrillBuf getBuffer() throws RpcException {
      return (DrillBuf) future.getBuffer();
    }
  }


  /**
   * A special tunnel that can be used for custom types of messages. Its lifecycle is tied to the underlying
   * ControlTunnel.
   * @param <SEND>
   *          The type of message the control tunnel will be able to send.
   * @param <RECEIVE>
   *          The expected response the control tunnel expects to receive.
   */
  public class CustomTunnel<SEND, RECEIVE> {
    private int messageTypeId;
    private CustomSerDe<SEND> send;
    private CustomSerDe<RECEIVE> receive;

    private CustomTunnel(int messageTypeId, CustomSerDe<SEND> send, CustomSerDe<RECEIVE> receive) {
      super();
      this.messageTypeId = messageTypeId;
      this.send = send;
      this.receive = receive;
    }

    /**
     * Send a message and receive a future for monitoring the outcome.
     * @param messageToSend
     *          The structured message to send.
     * @param dataBodies
     *          One or more optional unstructured messages to append to the structure message.
     * @return The CustomFuture that can be used to wait for the response.
     */
    public CustomFuture<RECEIVE> send(SEND messageToSend, ByteBuf... dataBodies) {
      final CustomMessage customMessage = CustomMessage.newBuilder()
          .setMessage(ByteString.copyFrom(send.serializeToSend(messageToSend)))
          .setType(messageTypeId)
          .build();
      final SyncCustomMessageSender b = new SyncCustomMessageSender(customMessage, dataBodies);
      manager.runCommand(b);
      DrillRpcFuture<CustomMessage> innerFuture = b.getFuture();
      return new CustomFuture<RECEIVE>(receive, innerFuture);
    }

    /**
     * Send a message using a custom listener.
     * @param listener
     *          The listener to inform of the outcome of the sent message.
     * @param messageToSend
     *          The structured message to send.
     * @param dataBodies
     *          One or more optional unstructured messages to append to the structure message.
     */
    public void send(RpcOutcomeListener<RECEIVE> listener, SEND messageToSend, ByteBuf... dataBodies) {
      final CustomMessage customMessage = CustomMessage.newBuilder()
          .setMessage(ByteString.copyFrom(send.serializeToSend(messageToSend)))
          .setType(messageTypeId)
          .build();
      manager.runCommand(new CustomMessageSender(new CustomTunnelListener(listener), customMessage, dataBodies));
    }

    private class CustomTunnelListener implements RpcOutcomeListener<CustomMessage> {
      final RpcOutcomeListener<RECEIVE> innerListener;

      public CustomTunnelListener(RpcOutcomeListener<RECEIVE> innerListener) {
        super();
        this.innerListener = innerListener;
      }

      @Override
      public void failed(RpcException ex) {
        innerListener.failed(ex);
      }

      @Override
      public void success(CustomMessage value, ByteBuf buffer) {
        try {
          RECEIVE message = receive.deserializeReceived(value.getMessage().toByteArray());
          innerListener.success(message, buffer);
        } catch (Exception e) {
          innerListener.failed(new RpcException("Failure while parsing message locally.", e));
        }
      }

      @Override
      public void interrupted(InterruptedException e) {
        innerListener.interrupted(e);
      }
    }
  }

  public static class ProtoSerDe<MSG extends MessageLite> implements CustomSerDe<MSG> {
    private final Parser<MSG> parser;

    ProtoSerDe(Parser<MSG> parser) {
      this.parser = parser;
    }

    @Override
    public byte[] serializeToSend(MSG send) {
      return send.toByteArray();
    }

    @Override
    public MSG deserializeReceived(byte[] bytes) throws Exception {
      return parser.parseFrom(bytes);
    }

  }

  public static class JacksonSerDe<MSG> implements CustomSerDe<MSG> {

    private final ObjectWriter writer;
    private final ObjectReader reader;

    public JacksonSerDe(Class<MSG> clazz) {
      ObjectMapper mapper = new ObjectMapper();
      writer = mapper.writerFor(clazz);
      reader = mapper.readerFor(clazz);
    }

    public JacksonSerDe(Class<MSG> clazz, JsonSerializer<MSG> serializer, JsonDeserializer<MSG> deserializer) {
      ObjectMapper mapper = new ObjectMapper();
      SimpleModule module = new SimpleModule();
      mapper.registerModule(module);
      module.addSerializer(clazz, serializer);
      module.addDeserializer(clazz, deserializer);
      writer = mapper.writerFor(clazz);
      reader = mapper.readerFor(clazz);
    }

    @Override
    public byte[] serializeToSend(MSG send) {
      try {
        return writer.writeValueAsBytes(send);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public MSG deserializeReceived(byte[] bytes) throws Exception {
      return (MSG) reader.readValue(bytes);
    }
  }
}
