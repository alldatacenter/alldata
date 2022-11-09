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

package com.bytedance.bitsail.dump.datasink.file.parser;
// source: 3.proto
@SuppressWarnings("all")
public final class TestMap {
  private static final com.google.protobuf.Descriptors.Descriptor
      internal_static_collie_log_descriptor;
  private static final
  com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_collie_log_fieldAccessorTable;
  private static final com.google.protobuf.Descriptors.Descriptor
      internal_static_collie_log_TaskRuleMapEntry_descriptor;
  private static final
  com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_collie_log_TaskRuleMapEntry_fieldAccessorTable;
  private static final com.google.protobuf.Descriptors.Descriptor
      internal_static_collie_log_ShortcutEntry_descriptor;
  private static final
  com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_collie_log_ShortcutEntry_fieldAccessorTable;
  private static final com.google.protobuf.Descriptors.Descriptor
      internal_static_rule_descriptor;
  private static final
  com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_rule_fieldAccessorTable;
  private static final com.google.protobuf.Descriptors.Descriptor
      internal_static_req_descriptor;
  private static final
  com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_req_fieldAccessorTable;
  private static final com.google.protobuf.Descriptors.Descriptor
      internal_static_rsp_descriptor;
  private static final
  com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_rsp_fieldAccessorTable;
  private static com.google.protobuf.Descriptors.FileDescriptor
      descriptor;

  static {
    String[] descriptorData = {
        "\n\0073.proto\"\217\002\n\ncollie_log\022\021\n\003req\030\001 \001(\0132\004." +
            "req\022\021\n\003rsp\030\002 \001(\0132\004.rsp\0223\n\rtask_rule_map\030" +
            "\003 \003(\0132\034.collie_log.TaskRuleMapEntry\022\r\n\005t" +
            "s_ns\030\004 \001(\003\022+\n\010shortcut\030\n \003(\0132\031.collie_lo" +
            "g.ShortcutEntry\0329\n\020TaskRuleMapEntry\022\013\n\003k" +
            "ey\030\001 \001(\003\022\024\n\005value\030\002 \001(\0132\005.rule:\0028\001\032/\n\rSh" +
            "ortcutEntry\022\013\n\003key\030\001 \001(\t\022\r\n\005value\030\002 \001(\t:" +
            "\0028\001\"\326\001\n\004rule\022\n\n\002id\030\001 \001(\003\022\016\n\006app_id\030\002 \001(\003" +
            "\022\017\n\007task_id\030\003 \001(\003\022\014\n\004expr\030\n \001(\t\022\020\n\010start" +
            "_at\030\013 \001(\003\022\016\n\006end_at\030\014 \001(\003\022\034\n\006status\030\r \001(" +
            "\0162\014.rule_status\022\026\n\016small_flow_ppm\030\016 \001(\003\022" +
            "\r\n\005mtime\030\017 \001(\t\022\014\n\004name\030\036 \001(\t\022\r\n\005descr\030\037 " +
            "\001(\t\022\017\n\007creator\030  \001(\t\"p\n\003req\022\r\n\005appid\030\001 \001" +
            "(\003\022\024\n\014task_id_list\030\002 \003(\003\022\017\n\007user_id\030\003 \001(" +
            "\003\022\021\n\tdevice_id\030\004 \001(\003\022\016\n\006api_no\030\005 \001(\003\022\020\n\010" +
            "json_str\030\006 \001(\t\"@\n\003rsp\022\023\n\013status_code\030\001 \001" +
            "(\005\022\022\n\nstatus_msg\030\002 \001(\t\022\020\n\010json_str\030\003 \001(\t" +
            "*\232\001\n\013rule_status\022\021\n\rstatus_unused\020\000\022\020\n\014s" +
            "tatus_draft\020\n\022\027\n\023status_online_draft\020\017\022\034" +
            "\n\030status_online_small_flow\020\024\022\033\n\027status_o" +
            "nline_full_flow\020\036\022\022\n\016status_offline\020(B\tB" +
            "\007TestMapb\006proto3"
    };
    com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner =
        new com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner() {
          public com.google.protobuf.ExtensionRegistry assignDescriptors(
              com.google.protobuf.Descriptors.FileDescriptor root) {
            descriptor = root;
            return null;
          }
        };
    com.google.protobuf.Descriptors.FileDescriptor
        .internalBuildGeneratedFileFrom(descriptorData,
            new com.google.protobuf.Descriptors.FileDescriptor[] {
            }, assigner);
    internal_static_collie_log_descriptor =
        getDescriptor().getMessageTypes().get(0);
    internal_static_collie_log_fieldAccessorTable = new
        com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_collie_log_descriptor,
        new String[] {"Req", "Rsp", "TaskRuleMap", "TsNs", "Shortcut",});
    internal_static_collie_log_TaskRuleMapEntry_descriptor =
        internal_static_collie_log_descriptor.getNestedTypes().get(0);
    internal_static_collie_log_TaskRuleMapEntry_fieldAccessorTable = new
        com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_collie_log_TaskRuleMapEntry_descriptor,
        new String[] {"Key", "Value",});
    internal_static_collie_log_ShortcutEntry_descriptor =
        internal_static_collie_log_descriptor.getNestedTypes().get(1);
    internal_static_collie_log_ShortcutEntry_fieldAccessorTable = new
        com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_collie_log_ShortcutEntry_descriptor,
        new String[] {"Key", "Value",});
    internal_static_rule_descriptor =
        getDescriptor().getMessageTypes().get(1);
    internal_static_rule_fieldAccessorTable = new
        com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_rule_descriptor,
        new String[] {"Id", "AppId", "TaskId", "Expr", "StartAt", "EndAt", "Status", "SmallFlowPpm", "Mtime", "Name", "Descr", "Creator",});
    internal_static_req_descriptor =
        getDescriptor().getMessageTypes().get(2);
    internal_static_req_fieldAccessorTable = new
        com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_req_descriptor,
        new String[] {"Appid", "TaskIdList", "UserId", "DeviceId", "ApiNo", "JsonStr",});
    internal_static_rsp_descriptor =
        getDescriptor().getMessageTypes().get(3);
    internal_static_rsp_fieldAccessorTable = new
        com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_rsp_descriptor,
        new String[] {"StatusCode", "StatusMsg", "JsonStr",});
  }
  private TestMap() {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }

  public static com.google.protobuf.Descriptors.FileDescriptor
  getDescriptor() {
    return descriptor;
  }
  /**
   * Protobuf enum {@code rule_status}
   */
  public enum rule_status
      implements com.google.protobuf.ProtocolMessageEnum {
    /**
     * <code>status_unused = 0;</code>
     */
    status_unused(0),
    /**
     * <pre>
     * </pre>
     *
     * <code>status_draft = 10;</code>
     */
    status_draft(10),
    /**
     * <pre>
     * </pre>
     *
     * <code>status_online_draft = 15;</code>
     */
    status_online_draft(15),
    /**
     * <pre>
     * </pre>
     *
     * <code>status_online_small_flow = 20;</code>
     */
    status_online_small_flow(20),
    /**
     * <pre>
     * </pre>
     *
     * <code>status_online_full_flow = 30;</code>
     */
    status_online_full_flow(30),
    /**
     * <pre>
     * </pre>
     *
     * <code>status_offline = 40;</code>
     */
    status_offline(40),
    UNRECOGNIZED(-1),
    ;

    /**
     * <code>status_unused = 0;</code>
     */
    public static final int status_unused_VALUE = 0;
    /**
     * <pre>
     * </pre>
     *
     * <code>status_draft = 10;</code>
     */
    public static final int status_draft_VALUE = 10;
    /**
     * <pre>
     * (  ,  )
     * </pre>
     *
     * <code>status_online_draft = 15;</code>
     */
    public static final int status_online_draft_VALUE = 15;
    /**
     * <pre>
     * : , handler.go#getrule
     * </pre>
     *
     * <code>status_online_small_flow = 20;</code>
     */
    public static final int status_online_small_flow_VALUE = 20;
    /**
     * <pre>
     *
     * </pre>
     *
     * <code>status_online_full_flow = 30;</code>
     */
    public static final int status_online_full_flow_VALUE = 30;
    /**
     * <pre>
     *
     * </pre>
     *
     * <code>status_offline = 40;</code>
     */
    public static final int status_offline_VALUE = 40;
    private static final com.google.protobuf.Internal.EnumLiteMap<
        rule_status> internalValueMap =
        new com.google.protobuf.Internal.EnumLiteMap<rule_status>() {
          public rule_status findValueByNumber(int number) {
            return rule_status.forNumber(number);
          }
        };
    private static final rule_status[] VALUES = values();
    private final int value;

    private rule_status(int value) {
      this.value = value;
    }

    /**
     * @deprecated Use {@link #forNumber(int)} instead.
     */
    @Deprecated
    public static rule_status valueOf(int value) {
      return forNumber(value);
    }

    public static rule_status forNumber(int value) {
      switch (value) {
        case 0:
          return status_unused;
        case 10:
          return status_draft;
        case 15:
          return status_online_draft;
        case 20:
          return status_online_small_flow;
        case 30:
          return status_online_full_flow;
        case 40:
          return status_offline;
        default:
          return null;
      }
    }

    public static com.google.protobuf.Internal.EnumLiteMap<rule_status>
    internalGetValueMap() {
      return internalValueMap;
    }

    public static final com.google.protobuf.Descriptors.EnumDescriptor
    getDescriptor() {
      return TestMap.getDescriptor().getEnumTypes().get(0);
    }

    public static rule_status valueOf(
        com.google.protobuf.Descriptors.EnumValueDescriptor desc) {
      if (desc.getType() != getDescriptor()) {
        throw new IllegalArgumentException(
            "EnumValueDescriptor is not for this type.");
      }
      if (desc.getIndex() == -1) {
        return UNRECOGNIZED;
      }
      return VALUES[desc.getIndex()];
    }

    public final int getNumber() {
      if (this == UNRECOGNIZED) {
        throw new IllegalArgumentException(
            "Can't get the number of an unknown enum value.");
      }
      return value;
    }

    public final com.google.protobuf.Descriptors.EnumValueDescriptor
    getValueDescriptor() {
      return getDescriptor().getValues().get(ordinal());
    }

    public final com.google.protobuf.Descriptors.EnumDescriptor
    getDescriptorForType() {
      return getDescriptor();
    }

    // @@protoc_insertion_point(enum_scope:rule_status)
  }
  public interface collie_logOrBuilder extends
      // @@protoc_insertion_point(interface_extends:collie_log)
      com.google.protobuf.MessageOrBuilder {

    /**
     * <code>.req req = 1;</code>
     */
    boolean hasReq();

    /**
     * <code>.req req = 1;</code>
     */
    req getReq();

    /**
     * <code>.req req = 1;</code>
     */
    reqOrBuilder getReqOrBuilder();

    /**
     * <code>.rsp rsp = 2;</code>
     */
    boolean hasRsp();

    /**
     * <code>.rsp rsp = 2;</code>
     */
    rsp getRsp();

    /**
     * <code>.rsp rsp = 2;</code>
     */
    rspOrBuilder getRspOrBuilder();

    /**
     * <code>map&lt;int64, .rule&gt; task_rule_map = 3;</code>
     */
    int getTaskRuleMapCount();

    /**
     * <code>map&lt;int64, .rule&gt; task_rule_map = 3;</code>
     */
    boolean containsTaskRuleMap(
        long key);

    /**
     * Use {@link #getTaskRuleMapMap()} instead.
     */
    @Deprecated
    java.util.Map<Long, rule>
    getTaskRuleMap();

    /**
     * <code>map&lt;int64, .rule&gt; task_rule_map = 3;</code>
     */
    java.util.Map<Long, rule>
    getTaskRuleMapMap();

    /**
     * <code>map&lt;int64, .rule&gt; task_rule_map = 3;</code>
     */

    rule getTaskRuleMapOrDefault(
        long key,
        rule defaultValue);

    /**
     * <code>map&lt;int64, .rule&gt; task_rule_map = 3;</code>
     */

    rule getTaskRuleMapOrThrow(
        long key);

    /**
     * <pre>
     * log, : (10^-9)
     * </pre>
     *
     * <code>int64 ts_ns = 4;</code>
     */
    long getTsNs();

    /**
     * <pre>
     * , req/rsp,
     * </pre>
     *
     * <code>map&lt;string, string&gt; shortcut = 10;</code>
     */
    int getShortcutCount();

    /**
     * <pre>
     * , req/rsp,
     * </pre>
     *
     * <code>map&lt;string, string&gt; shortcut = 10;</code>
     */
    boolean containsShortcut(
        String key);

    /**
     * Use {@link #getShortcutMap()} instead.
     */
    @Deprecated
    java.util.Map<String, String>
    getShortcut();

    /**
     * <pre>
     * , req/rsp,
     * </pre>
     *
     * <code>map&lt;string, string&gt; shortcut = 10;</code>
     */
    java.util.Map<String, String>
    getShortcutMap();

    /**
     * <pre>
     * , req/rsp,
     * </pre>
     *
     * <code>map&lt;string, string&gt; shortcut = 10;</code>
     */

    String getShortcutOrDefault(
        String key,
        String defaultValue);

    /**
     * <pre>
     * , req/rsp,
     * </pre>
     *
     * <code>map&lt;string, string&gt; shortcut = 10;</code>
     */

    String getShortcutOrThrow(
        String key);
  }
  public interface ruleOrBuilder extends
      // @@protoc_insertion_point(interface_extends:rule)
      com.google.protobuf.MessageOrBuilder {

    /**
     * <pre>
     * id()
     * </pre>
     *
     * <code>int64 id = 1;</code>
     */
    long getId();

    /**
     * <pre>
     * appid
     * </pre>
     *
     * <code>int64 app_id = 2;</code>
     */
    long getAppId();

    /**
     * <pre>
     * id(id)
     * </pre>
     *
     * <code>int64 task_id = 3;</code>
     */
    long getTaskId();

    /**
     * <pre>
     * id()
     * </pre>
     *
     * <code>string expr = 10;</code>
     */
    String getExpr();

    /**
     * <pre>
     * id()
     * </pre>
     *
     * <code>string expr = 10;</code>
     */
    com.google.protobuf.ByteString
    getExprBytes();

    /**
     * <pre>
     * (: )
     * </pre>
     *
     * <code>int64 start_at = 11;</code>
     */
    long getStartAt();

    /**
     * <pre>
     * (: )
     * </pre>
     *
     * <code>int64 end_at = 12;</code>
     */
    long getEndAt();

    /**
     * <code>.rule_status status = 13;</code>
     */
    int getStatusValue();

    /**
     * <code>.rule_status status = 13;</code>
     */
    rule_status getStatus();

    /**
     * <pre>
     * (: 10^-6, 0.0001%), status=status_online_small_flow
     * </pre>
     *
     * <code>int64 small_flow_ppm = 14;</code>
     */
    long getSmallFlowPpm();

    /**
     * <pre>
     * (: 2006-01-02 15:04:05)
     * </pre>
     *
     * <code>string mtime = 15;</code>
     */
    String getMtime();

    /**
     * <pre>
     * (: 2006-01-02 15:04:05)
     * </pre>
     *
     * <code>string mtime = 15;</code>
     */
    com.google.protobuf.ByteString
    getMtimeBytes();

    /**
     * <pre>
     * ()
     * </pre>
     *
     * <code>string name = 30;</code>
     */
    String getName();

    /**
     * <pre>
     * ()
     * </pre>
     *
     * <code>string name = 30;</code>
     */
    com.google.protobuf.ByteString
    getNameBytes();

    /**
     * <code>string descr = 31;</code>
     */
    String getDescr();

    /**
     * <code>string descr = 31;</code>
     */
    com.google.protobuf.ByteString
    getDescrBytes();

    /**
     * <pre>
     *
     * </pre>
     *
     * <code>string creator = 32;</code>
     */
    String getCreator();

    /**
     * <pre>
     *
     * </pre>
     *
     * <code>string creator = 32;</code>
     */
    com.google.protobuf.ByteString
    getCreatorBytes();
  }
  public interface reqOrBuilder extends
      // @@protoc_insertion_point(interface_extends:req)
      com.google.protobuf.MessageOrBuilder {

    /**
     * <code>int64 appid = 1;</code>
     */
    long getAppid();

    /**
     * <code>repeated int64 task_id_list = 2;</code>
     */
    java.util.List<Long> getTaskIdListList();

    /**
     * <code>repeated int64 task_id_list = 2;</code>
     */
    int getTaskIdListCount();

    /**
     * <code>repeated int64 task_id_list = 2;</code>
     */
    long getTaskIdList(int index);

    /**
     * <code>int64 user_id = 3;</code>
     */
    long getUserId();

    /**
     * <code>int64 device_id = 4;</code>
     */
    long getDeviceId();

    /**
     * <pre>
     *
     * </pre>
     *
     * <code>int64 api_no = 5;</code>
     */
    long getApiNo();

    /**
     * <pre>
     * json
     * </pre>
     *
     * <code>string json_str = 6;</code>
     */
    String getJsonStr();

    /**
     * <pre>
     * json
     * </pre>
     *
     * <code>string json_str = 6;</code>
     */
    com.google.protobuf.ByteString
    getJsonStrBytes();
  }
  public interface rspOrBuilder extends
      // @@protoc_insertion_point(interface_extends:rsp)
      com.google.protobuf.MessageOrBuilder {

    /**
     * <code>int32 status_code = 1;</code>
     */
    int getStatusCode();

    /**
     * <code>string status_msg = 2;</code>
     */
    String getStatusMsg();

    /**
     * <code>string status_msg = 2;</code>
     */
    com.google.protobuf.ByteString
    getStatusMsgBytes();

    /**
     * <pre>
     * json
     * </pre>
     *
     * <code>string json_str = 3;</code>
     */
    String getJsonStr();

    /**
     * <pre>
     * json
     * </pre>
     *
     * <code>string json_str = 3;</code>
     */
    com.google.protobuf.ByteString
    getJsonStrBytes();
  }

  /**
   * <pre>
   *
   * </pre>
   * <p>
   * Protobuf type {@code collie_log}
   */
  public static final class collie_log extends
      com.google.protobuf.GeneratedMessageV3 implements
      // @@protoc_insertion_point(message_implements:collie_log)
      collie_logOrBuilder {
    public static final int REQ_FIELD_NUMBER = 1;
    public static final int RSP_FIELD_NUMBER = 2;
    public static final int TASK_RULE_MAP_FIELD_NUMBER = 3;
    public static final int TS_NS_FIELD_NUMBER = 4;
    public static final int SHORTCUT_FIELD_NUMBER = 10;
    private static final long serialVersionUID = 0L;
    // @@protoc_insertion_point(class_scope:collie_log)
    private static final collie_log DEFAULT_INSTANCE;
    private static final com.google.protobuf.Parser<collie_log>
        PARSER = new com.google.protobuf.AbstractParser<collie_log>() {
      @Override
      public collie_log parsePartialFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws com.google.protobuf.InvalidProtocolBufferException {
        return new collie_log(input, extensionRegistry);
      }
    };

    static {
      DEFAULT_INSTANCE = new collie_log();
    }

    private int bitField0_;
    private req req_;
    private rsp rsp_;
    private com.google.protobuf.MapField<
        Long, rule> taskRuleMap_;
    private long tsNs_;
    private com.google.protobuf.MapField<
        String, String> shortcut_;
    private byte memoizedIsInitialized = -1;

    // Use collie_log.newBuilder() to construct.
    private collie_log(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
      super(builder);
    }

    private collie_log() {
      tsNs_ = 0L;
    }

    private collie_log(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      this();
      if (extensionRegistry == null) {
        throw new NullPointerException();
      }
      int mutable_bitField0_ = 0;
      com.google.protobuf.UnknownFieldSet.Builder unknownFields =
          com.google.protobuf.UnknownFieldSet.newBuilder();
      try {
        boolean done = false;
        while (!done) {
          int tag = input.readTag();
          switch (tag) {
            case 0:
              done = true;
              break;
            case 10: {
              req.Builder subBuilder = null;
              if (req_ != null) {
                subBuilder = req_.toBuilder();
              }
              req_ = input.readMessage(req.parser(), extensionRegistry);
              if (subBuilder != null) {
                subBuilder.mergeFrom(req_);
                req_ = subBuilder.buildPartial();
              }

              break;
            }
            case 18: {
              rsp.Builder subBuilder = null;
              if (rsp_ != null) {
                subBuilder = rsp_.toBuilder();
              }
              rsp_ = input.readMessage(rsp.parser(), extensionRegistry);
              if (subBuilder != null) {
                subBuilder.mergeFrom(rsp_);
                rsp_ = subBuilder.buildPartial();
              }

              break;
            }
            case 26: {
              if (!((mutable_bitField0_ & 0x00000004) == 0x00000004)) {
                taskRuleMap_ = com.google.protobuf.MapField.newMapField(
                    TaskRuleMapDefaultEntryHolder.defaultEntry);
                mutable_bitField0_ |= 0x00000004;
              }
              com.google.protobuf.MapEntry<Long, rule>
                  taskRuleMap__ = input.readMessage(
                  TaskRuleMapDefaultEntryHolder.defaultEntry.getParserForType(), extensionRegistry);
              taskRuleMap_.getMutableMap().put(
                  taskRuleMap__.getKey(), taskRuleMap__.getValue());
              break;
            }
            case 32: {

              tsNs_ = input.readInt64();
              break;
            }
            case 82: {
              if (!((mutable_bitField0_ & 0x00000010) == 0x00000010)) {
                shortcut_ = com.google.protobuf.MapField.newMapField(
                    ShortcutDefaultEntryHolder.defaultEntry);
                mutable_bitField0_ |= 0x00000010;
              }
              com.google.protobuf.MapEntry<String, String>
                  shortcut__ = input.readMessage(
                  ShortcutDefaultEntryHolder.defaultEntry.getParserForType(), extensionRegistry);
              shortcut_.getMutableMap().put(
                  shortcut__.getKey(), shortcut__.getValue());
              break;
            }
            default: {
              if (!parseUnknownFieldProto3(
                  input, unknownFields, extensionRegistry, tag)) {
                done = true;
              }
              break;
            }
          }
        }
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        throw e.setUnfinishedMessage(this);
      } catch (java.io.IOException e) {
        throw new com.google.protobuf.InvalidProtocolBufferException(
            e).setUnfinishedMessage(this);
      } finally {
        this.unknownFields = unknownFields.build();
        makeExtensionsImmutable();
      }
    }

    public static final com.google.protobuf.Descriptors.Descriptor
    getDescriptor() {
      return TestMap.internal_static_collie_log_descriptor;
    }

    public static collie_log parseFrom(
        java.nio.ByteBuffer data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static collie_log parseFrom(
        java.nio.ByteBuffer data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static collie_log parseFrom(
        com.google.protobuf.ByteString data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static collie_log parseFrom(
        com.google.protobuf.ByteString data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static collie_log parseFrom(byte[] data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static collie_log parseFrom(
        byte[] data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static collie_log parseFrom(java.io.InputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input);
    }

    public static collie_log parseFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static collie_log parseDelimitedFrom(java.io.InputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseDelimitedWithIOException(PARSER, input);
    }

    public static collie_log parseDelimitedFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
    }

    public static collie_log parseFrom(
        com.google.protobuf.CodedInputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input);
    }

    public static collie_log parseFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static Builder newBuilder() {
      return DEFAULT_INSTANCE.toBuilder();
    }

    public static Builder newBuilder(collie_log prototype) {
      return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }

    public static collie_log getDefaultInstance() {
      return DEFAULT_INSTANCE;
    }

    public static com.google.protobuf.Parser<collie_log> parser() {
      return PARSER;
    }

    @Override
    public final com.google.protobuf.UnknownFieldSet
    getUnknownFields() {
      return this.unknownFields;
    }

    @SuppressWarnings({"rawtypes"})
    @Override
    protected com.google.protobuf.MapField internalGetMapField(
        int number) {
      switch (number) {
        case 3:
          return internalGetTaskRuleMap();
        case 10:
          return internalGetShortcut();
        default:
          throw new RuntimeException(
              "Invalid map field number: " + number);
      }
    }

    @Override
    protected FieldAccessorTable
    internalGetFieldAccessorTable() {
      return TestMap.internal_static_collie_log_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              collie_log.class, Builder.class);
    }

    /**
     * <code>.req req = 1;</code>
     */
    public boolean hasReq() {
      return req_ != null;
    }

    /**
     * <code>.req req = 1;</code>
     */
    public req getReq() {
      return req_ == null ? req.getDefaultInstance() : req_;
    }

    /**
     * <code>.req req = 1;</code>
     */
    public reqOrBuilder getReqOrBuilder() {
      return getReq();
    }

    /**
     * <code>.rsp rsp = 2;</code>
     */
    public boolean hasRsp() {
      return rsp_ != null;
    }

    /**
     * <code>.rsp rsp = 2;</code>
     */
    public rsp getRsp() {
      return rsp_ == null ? rsp.getDefaultInstance() : rsp_;
    }

    /**
     * <code>.rsp rsp = 2;</code>
     */
    public rspOrBuilder getRspOrBuilder() {
      return getRsp();
    }

    private com.google.protobuf.MapField<Long, rule>
    internalGetTaskRuleMap() {
      if (taskRuleMap_ == null) {
        return com.google.protobuf.MapField.emptyMapField(
            TaskRuleMapDefaultEntryHolder.defaultEntry);
      }
      return taskRuleMap_;
    }

    public int getTaskRuleMapCount() {
      return internalGetTaskRuleMap().getMap().size();
    }

    /**
     * <code>map&lt;int64, .rule&gt; task_rule_map = 3;</code>
     */

    public boolean containsTaskRuleMap(
        long key) {

      return internalGetTaskRuleMap().getMap().containsKey(key);
    }

    /**
     * Use {@link #getTaskRuleMapMap()} instead.
     */
    @Deprecated
    public java.util.Map<Long, rule> getTaskRuleMap() {
      return getTaskRuleMapMap();
    }

    /**
     * <code>map&lt;int64, .rule&gt; task_rule_map = 3;</code>
     */

    public java.util.Map<Long, rule> getTaskRuleMapMap() {
      return internalGetTaskRuleMap().getMap();
    }

    /**
     * <code>map&lt;int64, .rule&gt; task_rule_map = 3;</code>
     */

    public rule getTaskRuleMapOrDefault(
        long key,
        rule defaultValue) {

      java.util.Map<Long, rule> map =
          internalGetTaskRuleMap().getMap();
      return map.containsKey(key) ? map.get(key) : defaultValue;
    }

    /**
     * <code>map&lt;int64, .rule&gt; task_rule_map = 3;</code>
     */

    public rule getTaskRuleMapOrThrow(
        long key) {

      java.util.Map<Long, rule> map =
          internalGetTaskRuleMap().getMap();
      if (!map.containsKey(key)) {
        throw new IllegalArgumentException();
      }
      return map.get(key);
    }

    /**
     * <pre>
     * log, : (10^-9)
     * </pre>
     *
     * <code>int64 ts_ns = 4;</code>
     */
    public long getTsNs() {
      return tsNs_;
    }

    private com.google.protobuf.MapField<String, String>
    internalGetShortcut() {
      if (shortcut_ == null) {
        return com.google.protobuf.MapField.emptyMapField(
            ShortcutDefaultEntryHolder.defaultEntry);
      }
      return shortcut_;
    }

    public int getShortcutCount() {
      return internalGetShortcut().getMap().size();
    }

    /**
     * <pre>
     * , req/rsp,
     * </pre>
     *
     * <code>map&lt;string, string&gt; shortcut = 10;</code>
     */

    public boolean containsShortcut(
        String key) {
      if (key == null) {
        throw new NullPointerException();
      }
      return internalGetShortcut().getMap().containsKey(key);
    }

    /**
     * Use {@link #getShortcutMap()} instead.
     */
    @Deprecated
    public java.util.Map<String, String> getShortcut() {
      return getShortcutMap();
    }

    /**
     * <pre>
     * , req/rsp,
     * </pre>
     *
     * <code>map&lt;string, string&gt; shortcut = 10;</code>
     */

    public java.util.Map<String, String> getShortcutMap() {
      return internalGetShortcut().getMap();
    }

    /**
     * <pre>
     * , req/rsp,
     * </pre>
     *
     * <code>map&lt;string, string&gt; shortcut = 10;</code>
     */

    public String getShortcutOrDefault(
        String key,
        String defaultValue) {
      if (key == null) {
        throw new NullPointerException();
      }
      java.util.Map<String, String> map =
          internalGetShortcut().getMap();
      return map.containsKey(key) ? map.get(key) : defaultValue;
    }

    /**
     * <pre>
     * , req/rsp,
     * </pre>
     *
     * <code>map&lt;string, string&gt; shortcut = 10;</code>
     */

    public String getShortcutOrThrow(
        String key) {
      if (key == null) {
        throw new NullPointerException();
      }
      java.util.Map<String, String> map =
          internalGetShortcut().getMap();
      if (!map.containsKey(key)) {
        throw new IllegalArgumentException();
      }
      return map.get(key);
    }

    @Override
    public final boolean isInitialized() {
      byte isInitialized = memoizedIsInitialized;
      if (isInitialized == 1) {
        return true;
      }
      if (isInitialized == 0) {
        return false;
      }

      memoizedIsInitialized = 1;
      return true;
    }

    @Override
    public void writeTo(com.google.protobuf.CodedOutputStream output)
        throws java.io.IOException {
      if (req_ != null) {
        output.writeMessage(1, getReq());
      }
      if (rsp_ != null) {
        output.writeMessage(2, getRsp());
      }
      com.google.protobuf.GeneratedMessageV3
          .serializeLongMapTo(
              output,
              internalGetTaskRuleMap(),
              TaskRuleMapDefaultEntryHolder.defaultEntry,
              3);
      if (tsNs_ != 0L) {
        output.writeInt64(4, tsNs_);
      }
      com.google.protobuf.GeneratedMessageV3
          .serializeStringMapTo(
              output,
              internalGetShortcut(),
              ShortcutDefaultEntryHolder.defaultEntry,
              10);
      unknownFields.writeTo(output);
    }

    @Override
    public int getSerializedSize() {
      int size = memoizedSize;
      if (size != -1) {
        return size;
      }

      size = 0;
      if (req_ != null) {
        size += com.google.protobuf.CodedOutputStream
            .computeMessageSize(1, getReq());
      }
      if (rsp_ != null) {
        size += com.google.protobuf.CodedOutputStream
            .computeMessageSize(2, getRsp());
      }
      for (java.util.Map.Entry<Long, rule> entry
          : internalGetTaskRuleMap().getMap().entrySet()) {
        com.google.protobuf.MapEntry<Long, rule>
            taskRuleMap__ = TaskRuleMapDefaultEntryHolder.defaultEntry.newBuilderForType()
            .setKey(entry.getKey())
            .setValue(entry.getValue())
            .build();
        size += com.google.protobuf.CodedOutputStream
            .computeMessageSize(3, taskRuleMap__);
      }
      if (tsNs_ != 0L) {
        size += com.google.protobuf.CodedOutputStream
            .computeInt64Size(4, tsNs_);
      }
      for (java.util.Map.Entry<String, String> entry
          : internalGetShortcut().getMap().entrySet()) {
        com.google.protobuf.MapEntry<String, String>
            shortcut__ = ShortcutDefaultEntryHolder.defaultEntry.newBuilderForType()
            .setKey(entry.getKey())
            .setValue(entry.getValue())
            .build();
        size += com.google.protobuf.CodedOutputStream
            .computeMessageSize(10, shortcut__);
      }
      size += unknownFields.getSerializedSize();
      memoizedSize = size;
      return size;
    }

    @Override
    public boolean equals(final Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof collie_log)) {
        return super.equals(obj);
      }
      collie_log other = (collie_log) obj;

      boolean result = true;
      result = result && (hasReq() == other.hasReq());
      if (hasReq()) {
        result = result && getReq()
            .equals(other.getReq());
      }
      result = result && (hasRsp() == other.hasRsp());
      if (hasRsp()) {
        result = result && getRsp()
            .equals(other.getRsp());
      }
      result = result && internalGetTaskRuleMap().equals(
          other.internalGetTaskRuleMap());
      result = result && (getTsNs()
          == other.getTsNs());
      result = result && internalGetShortcut().equals(
          other.internalGetShortcut());
      result = result && unknownFields.equals(other.unknownFields);
      return result;
    }

    @Override
    public int hashCode() {
      if (memoizedHashCode != 0) {
        return memoizedHashCode;
      }
      int hash = 41;
      hash = (19 * hash) + getDescriptor().hashCode();
      if (hasReq()) {
        hash = (37 * hash) + REQ_FIELD_NUMBER;
        hash = (53 * hash) + getReq().hashCode();
      }
      if (hasRsp()) {
        hash = (37 * hash) + RSP_FIELD_NUMBER;
        hash = (53 * hash) + getRsp().hashCode();
      }
      if (!internalGetTaskRuleMap().getMap().isEmpty()) {
        hash = (37 * hash) + TASK_RULE_MAP_FIELD_NUMBER;
        hash = (53 * hash) + internalGetTaskRuleMap().hashCode();
      }
      hash = (37 * hash) + TS_NS_FIELD_NUMBER;
      hash = (53 * hash) + com.google.protobuf.Internal.hashLong(
          getTsNs());
      if (!internalGetShortcut().getMap().isEmpty()) {
        hash = (37 * hash) + SHORTCUT_FIELD_NUMBER;
        hash = (53 * hash) + internalGetShortcut().hashCode();
      }
      hash = (29 * hash) + unknownFields.hashCode();
      memoizedHashCode = hash;
      return hash;
    }

    @Override
    public Builder newBuilderForType() {
      return newBuilder();
    }

    @Override
    public Builder toBuilder() {
      return this == DEFAULT_INSTANCE
          ? new Builder() : new Builder().mergeFrom(this);
    }

    @Override
    protected Builder newBuilderForType(
        BuilderParent parent) {
      Builder builder = new Builder(parent);
      return builder;
    }

    @Override
    public com.google.protobuf.Parser<collie_log> getParserForType() {
      return PARSER;
    }

    @Override
    public collie_log getDefaultInstanceForType() {
      return DEFAULT_INSTANCE;
    }

    private static final class TaskRuleMapDefaultEntryHolder {
      static final com.google.protobuf.MapEntry<
          Long, rule> defaultEntry =
          com.google.protobuf.MapEntry
              .<Long, rule>newDefaultInstance(
                  TestMap.internal_static_collie_log_TaskRuleMapEntry_descriptor,
                  com.google.protobuf.WireFormat.FieldType.INT64,
                  0L,
                  com.google.protobuf.WireFormat.FieldType.MESSAGE,
                  rule.getDefaultInstance());
    }

    private static final class ShortcutDefaultEntryHolder {
      static final com.google.protobuf.MapEntry<
          String, String> defaultEntry =
          com.google.protobuf.MapEntry
              .<String, String>newDefaultInstance(
                  TestMap.internal_static_collie_log_ShortcutEntry_descriptor,
                  com.google.protobuf.WireFormat.FieldType.STRING,
                  "",
                  com.google.protobuf.WireFormat.FieldType.STRING,
                  "");
    }

    /**
     * <pre>
     *
     * </pre>
     * <p>
     * Protobuf type {@code collie_log}
     */
    public static final class Builder extends
        com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
        // @@protoc_insertion_point(builder_implements:collie_log)
        collie_logOrBuilder {
      private int bitField0_;
      private req req_ = null;
      private com.google.protobuf.SingleFieldBuilderV3<
          req, req.Builder, reqOrBuilder> reqBuilder_;
      private rsp rsp_ = null;
      private com.google.protobuf.SingleFieldBuilderV3<
          rsp, rsp.Builder, rspOrBuilder> rspBuilder_;
      private com.google.protobuf.MapField<
          Long, rule> taskRuleMap_;
      private long tsNs_;
      private com.google.protobuf.MapField<
          String, String> shortcut_;

      // Construct using TestMap.collie_log.newBuilder()
      private Builder() {
        maybeForceBuilderInitialization();
      }

      private Builder(
          BuilderParent parent) {
        super(parent);
        maybeForceBuilderInitialization();
      }

      public static final com.google.protobuf.Descriptors.Descriptor
      getDescriptor() {
        return TestMap.internal_static_collie_log_descriptor;
      }

      @SuppressWarnings({"rawtypes"})
      protected com.google.protobuf.MapField internalGetMapField(
          int number) {
        switch (number) {
          case 3:
            return internalGetTaskRuleMap();
          case 10:
            return internalGetShortcut();
          default:
            throw new RuntimeException(
                "Invalid map field number: " + number);
        }
      }

      @SuppressWarnings({"rawtypes"})
      protected com.google.protobuf.MapField internalGetMutableMapField(
          int number) {
        switch (number) {
          case 3:
            return internalGetMutableTaskRuleMap();
          case 10:
            return internalGetMutableShortcut();
          default:
            throw new RuntimeException(
                "Invalid map field number: " + number);
        }
      }

      @Override
      protected FieldAccessorTable
      internalGetFieldAccessorTable() {
        return TestMap.internal_static_collie_log_fieldAccessorTable
            .ensureFieldAccessorsInitialized(
                collie_log.class, Builder.class);
      }

      private void maybeForceBuilderInitialization() {
        if (com.google.protobuf.GeneratedMessageV3
            .alwaysUseFieldBuilders) {
        }
      }

      @Override
      public Builder clear() {
        super.clear();
        if (reqBuilder_ == null) {
          req_ = null;
        } else {
          req_ = null;
          reqBuilder_ = null;
        }
        if (rspBuilder_ == null) {
          rsp_ = null;
        } else {
          rsp_ = null;
          rspBuilder_ = null;
        }
        internalGetMutableTaskRuleMap().clear();
        tsNs_ = 0L;

        internalGetMutableShortcut().clear();
        return this;
      }

      @Override
      public com.google.protobuf.Descriptors.Descriptor
      getDescriptorForType() {
        return TestMap.internal_static_collie_log_descriptor;
      }

      @Override
      public collie_log getDefaultInstanceForType() {
        return collie_log.getDefaultInstance();
      }

      @Override
      public collie_log build() {
        collie_log result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }

      @Override
      public collie_log buildPartial() {
        collie_log result = new collie_log(this);
        int from_bitField0_ = bitField0_;
        int to_bitField0_ = 0;
        if (reqBuilder_ == null) {
          result.req_ = req_;
        } else {
          result.req_ = reqBuilder_.build();
        }
        if (rspBuilder_ == null) {
          result.rsp_ = rsp_;
        } else {
          result.rsp_ = rspBuilder_.build();
        }
        result.taskRuleMap_ = internalGetTaskRuleMap();
        result.taskRuleMap_.makeImmutable();
        result.tsNs_ = tsNs_;
        result.shortcut_ = internalGetShortcut();
        result.shortcut_.makeImmutable();
        result.bitField0_ = to_bitField0_;
        onBuilt();
        return result;
      }

      @Override
      public Builder clone() {
        return (Builder) super.clone();
      }

      @Override
      public Builder setField(
          com.google.protobuf.Descriptors.FieldDescriptor field,
          Object value) {
        return (Builder) super.setField(field, value);
      }

      @Override
      public Builder clearField(
          com.google.protobuf.Descriptors.FieldDescriptor field) {
        return (Builder) super.clearField(field);
      }

      @Override
      public Builder clearOneof(
          com.google.protobuf.Descriptors.OneofDescriptor oneof) {
        return (Builder) super.clearOneof(oneof);
      }

      @Override
      public Builder setRepeatedField(
          com.google.protobuf.Descriptors.FieldDescriptor field,
          int index, Object value) {
        return (Builder) super.setRepeatedField(field, index, value);
      }

      @Override
      public Builder addRepeatedField(
          com.google.protobuf.Descriptors.FieldDescriptor field,
          Object value) {
        return (Builder) super.addRepeatedField(field, value);
      }

      @Override
      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof collie_log) {
          return mergeFrom((collie_log) other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }

      public Builder mergeFrom(collie_log other) {
        if (other == collie_log.getDefaultInstance()) {
          return this;
        }
        if (other.hasReq()) {
          mergeReq(other.getReq());
        }
        if (other.hasRsp()) {
          mergeRsp(other.getRsp());
        }
        internalGetMutableTaskRuleMap().mergeFrom(
            other.internalGetTaskRuleMap());
        if (other.getTsNs() != 0L) {
          setTsNs(other.getTsNs());
        }
        internalGetMutableShortcut().mergeFrom(
            other.internalGetShortcut());
        this.mergeUnknownFields(other.unknownFields);
        onChanged();
        return this;
      }

      @Override
      public final boolean isInitialized() {
        return true;
      }

      @Override
      public Builder mergeFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws java.io.IOException {
        collie_log parsedMessage = null;
        try {
          parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          parsedMessage = (collie_log) e.getUnfinishedMessage();
          throw e.unwrapIOException();
        } finally {
          if (parsedMessage != null) {
            mergeFrom(parsedMessage);
          }
        }
        return this;
      }

      /**
       * <code>.req req = 1;</code>
       */
      public boolean hasReq() {
        return reqBuilder_ != null || req_ != null;
      }

      /**
       * <code>.req req = 1;</code>
       */
      public req getReq() {
        if (reqBuilder_ == null) {
          return req_ == null ? req.getDefaultInstance() : req_;
        } else {
          return reqBuilder_.getMessage();
        }
      }

      /**
       * <code>.req req = 1;</code>
       */
      public Builder setReq(req value) {
        if (reqBuilder_ == null) {
          if (value == null) {
            throw new NullPointerException();
          }
          req_ = value;
          onChanged();
        } else {
          reqBuilder_.setMessage(value);
        }

        return this;
      }

      /**
       * <code>.req req = 1;</code>
       */
      public Builder setReq(
          req.Builder builderForValue) {
        if (reqBuilder_ == null) {
          req_ = builderForValue.build();
          onChanged();
        } else {
          reqBuilder_.setMessage(builderForValue.build());
        }

        return this;
      }

      /**
       * <code>.req req = 1;</code>
       */
      public Builder mergeReq(req value) {
        if (reqBuilder_ == null) {
          if (req_ != null) {
            req_ =
                req.newBuilder(req_).mergeFrom(value).buildPartial();
          } else {
            req_ = value;
          }
          onChanged();
        } else {
          reqBuilder_.mergeFrom(value);
        }

        return this;
      }

      /**
       * <code>.req req = 1;</code>
       */
      public Builder clearReq() {
        if (reqBuilder_ == null) {
          req_ = null;
          onChanged();
        } else {
          req_ = null;
          reqBuilder_ = null;
        }

        return this;
      }

      /**
       * <code>.req req = 1;</code>
       */
      public req.Builder getReqBuilder() {

        onChanged();
        return getReqFieldBuilder().getBuilder();
      }

      /**
       * <code>.req req = 1;</code>
       */
      public reqOrBuilder getReqOrBuilder() {
        if (reqBuilder_ != null) {
          return reqBuilder_.getMessageOrBuilder();
        } else {
          return req_ == null ?
              req.getDefaultInstance() : req_;
        }
      }

      /**
       * <code>.req req = 1;</code>
       */
      private com.google.protobuf.SingleFieldBuilderV3<
          req, req.Builder, reqOrBuilder>
      getReqFieldBuilder() {
        if (reqBuilder_ == null) {
          reqBuilder_ = new com.google.protobuf.SingleFieldBuilderV3<
              req, req.Builder, reqOrBuilder>(
              getReq(),
              getParentForChildren(),
              isClean());
          req_ = null;
        }
        return reqBuilder_;
      }

      /**
       * <code>.rsp rsp = 2;</code>
       */
      public boolean hasRsp() {
        return rspBuilder_ != null || rsp_ != null;
      }

      /**
       * <code>.rsp rsp = 2;</code>
       */
      public rsp getRsp() {
        if (rspBuilder_ == null) {
          return rsp_ == null ? rsp.getDefaultInstance() : rsp_;
        } else {
          return rspBuilder_.getMessage();
        }
      }

      /**
       * <code>.rsp rsp = 2;</code>
       */
      public Builder setRsp(rsp value) {
        if (rspBuilder_ == null) {
          if (value == null) {
            throw new NullPointerException();
          }
          rsp_ = value;
          onChanged();
        } else {
          rspBuilder_.setMessage(value);
        }

        return this;
      }

      /**
       * <code>.rsp rsp = 2;</code>
       */
      public Builder setRsp(
          rsp.Builder builderForValue) {
        if (rspBuilder_ == null) {
          rsp_ = builderForValue.build();
          onChanged();
        } else {
          rspBuilder_.setMessage(builderForValue.build());
        }

        return this;
      }

      /**
       * <code>.rsp rsp = 2;</code>
       */
      public Builder mergeRsp(rsp value) {
        if (rspBuilder_ == null) {
          if (rsp_ != null) {
            rsp_ =
                rsp.newBuilder(rsp_).mergeFrom(value).buildPartial();
          } else {
            rsp_ = value;
          }
          onChanged();
        } else {
          rspBuilder_.mergeFrom(value);
        }

        return this;
      }

      /**
       * <code>.rsp rsp = 2;</code>
       */
      public Builder clearRsp() {
        if (rspBuilder_ == null) {
          rsp_ = null;
          onChanged();
        } else {
          rsp_ = null;
          rspBuilder_ = null;
        }

        return this;
      }

      /**
       * <code>.rsp rsp = 2;</code>
       */
      public rsp.Builder getRspBuilder() {

        onChanged();
        return getRspFieldBuilder().getBuilder();
      }

      /**
       * <code>.rsp rsp = 2;</code>
       */
      public rspOrBuilder getRspOrBuilder() {
        if (rspBuilder_ != null) {
          return rspBuilder_.getMessageOrBuilder();
        } else {
          return rsp_ == null ?
              rsp.getDefaultInstance() : rsp_;
        }
      }

      /**
       * <code>.rsp rsp = 2;</code>
       */
      private com.google.protobuf.SingleFieldBuilderV3<
          rsp, rsp.Builder, rspOrBuilder>
      getRspFieldBuilder() {
        if (rspBuilder_ == null) {
          rspBuilder_ = new com.google.protobuf.SingleFieldBuilderV3<
              rsp, rsp.Builder, rspOrBuilder>(
              getRsp(),
              getParentForChildren(),
              isClean());
          rsp_ = null;
        }
        return rspBuilder_;
      }

      private com.google.protobuf.MapField<Long, rule>
      internalGetTaskRuleMap() {
        if (taskRuleMap_ == null) {
          return com.google.protobuf.MapField.emptyMapField(
              TaskRuleMapDefaultEntryHolder.defaultEntry);
        }
        return taskRuleMap_;
      }

      private com.google.protobuf.MapField<Long, rule>
      internalGetMutableTaskRuleMap() {
        onChanged();
        ;
        if (taskRuleMap_ == null) {
          taskRuleMap_ = com.google.protobuf.MapField.newMapField(
              TaskRuleMapDefaultEntryHolder.defaultEntry);
        }
        if (!taskRuleMap_.isMutable()) {
          taskRuleMap_ = taskRuleMap_.copy();
        }
        return taskRuleMap_;
      }

      public int getTaskRuleMapCount() {
        return internalGetTaskRuleMap().getMap().size();
      }

      /**
       * <code>map&lt;int64, .rule&gt; task_rule_map = 3;</code>
       */

      public boolean containsTaskRuleMap(
          long key) {

        return internalGetTaskRuleMap().getMap().containsKey(key);
      }

      /**
       * Use {@link #getTaskRuleMapMap()} instead.
       */
      @Deprecated
      public java.util.Map<Long, rule> getTaskRuleMap() {
        return getTaskRuleMapMap();
      }

      /**
       * <code>map&lt;int64, .rule&gt; task_rule_map = 3;</code>
       */

      public java.util.Map<Long, rule> getTaskRuleMapMap() {
        return internalGetTaskRuleMap().getMap();
      }

      /**
       * <code>map&lt;int64, .rule&gt; task_rule_map = 3;</code>
       */

      public rule getTaskRuleMapOrDefault(
          long key,
          rule defaultValue) {

        java.util.Map<Long, rule> map =
            internalGetTaskRuleMap().getMap();
        return map.containsKey(key) ? map.get(key) : defaultValue;
      }

      /**
       * <code>map&lt;int64, .rule&gt; task_rule_map = 3;</code>
       */

      public rule getTaskRuleMapOrThrow(
          long key) {

        java.util.Map<Long, rule> map =
            internalGetTaskRuleMap().getMap();
        if (!map.containsKey(key)) {
          throw new IllegalArgumentException();
        }
        return map.get(key);
      }

      public Builder clearTaskRuleMap() {
        internalGetMutableTaskRuleMap().getMutableMap()
            .clear();
        return this;
      }

      /**
       * <code>map&lt;int64, .rule&gt; task_rule_map = 3;</code>
       */

      public Builder removeTaskRuleMap(
          long key) {

        internalGetMutableTaskRuleMap().getMutableMap()
            .remove(key);
        return this;
      }

      /**
       * Use alternate mutation accessors instead.
       */
      @Deprecated
      public java.util.Map<Long, rule>
      getMutableTaskRuleMap() {
        return internalGetMutableTaskRuleMap().getMutableMap();
      }

      /**
       * <code>map&lt;int64, .rule&gt; task_rule_map = 3;</code>
       */
      public Builder putTaskRuleMap(
          long key,
          rule value) {

        if (value == null) {
          throw new NullPointerException();
        }
        internalGetMutableTaskRuleMap().getMutableMap()
            .put(key, value);
        return this;
      }

      /**
       * <code>map&lt;int64, .rule&gt; task_rule_map = 3;</code>
       */

      public Builder putAllTaskRuleMap(
          java.util.Map<Long, rule> values) {
        internalGetMutableTaskRuleMap().getMutableMap()
            .putAll(values);
        return this;
      }

      /**
       * <pre>
       * log, : (10^-9)
       * </pre>
       *
       * <code>int64 ts_ns = 4;</code>
       */
      public long getTsNs() {
        return tsNs_;
      }

      /**
       * <pre>
       * log, : (10^-9)
       * </pre>
       *
       * <code>int64 ts_ns = 4;</code>
       */
      public Builder setTsNs(long value) {

        tsNs_ = value;
        onChanged();
        return this;
      }

      /**
       * <pre>
       * log, : (10^-9)
       * </pre>
       *
       * <code>int64 ts_ns = 4;</code>
       */
      public Builder clearTsNs() {

        tsNs_ = 0L;
        onChanged();
        return this;
      }

      private com.google.protobuf.MapField<String, String>
      internalGetShortcut() {
        if (shortcut_ == null) {
          return com.google.protobuf.MapField.emptyMapField(
              ShortcutDefaultEntryHolder.defaultEntry);
        }
        return shortcut_;
      }

      private com.google.protobuf.MapField<String, String>
      internalGetMutableShortcut() {
        onChanged();
        ;
        if (shortcut_ == null) {
          shortcut_ = com.google.protobuf.MapField.newMapField(
              ShortcutDefaultEntryHolder.defaultEntry);
        }
        if (!shortcut_.isMutable()) {
          shortcut_ = shortcut_.copy();
        }
        return shortcut_;
      }

      public int getShortcutCount() {
        return internalGetShortcut().getMap().size();
      }

      /**
       * <pre>
       * , req/rsp,
       * </pre>
       *
       * <code>map&lt;string, string&gt; shortcut = 10;</code>
       */

      public boolean containsShortcut(
          String key) {
        if (key == null) {
          throw new NullPointerException();
        }
        return internalGetShortcut().getMap().containsKey(key);
      }

      /**
       * Use {@link #getShortcutMap()} instead.
       */
      @Deprecated
      public java.util.Map<String, String> getShortcut() {
        return getShortcutMap();
      }

      /**
       * <pre>
       * , req/rsp,
       * </pre>
       *
       * <code>map&lt;string, string&gt; shortcut = 10;</code>
       */

      public java.util.Map<String, String> getShortcutMap() {
        return internalGetShortcut().getMap();
      }

      /**
       * <pre>
       * , req/rsp,
       * </pre>
       *
       * <code>map&lt;string, string&gt; shortcut = 10;</code>
       */

      public String getShortcutOrDefault(
          String key,
          String defaultValue) {
        if (key == null) {
          throw new NullPointerException();
        }
        java.util.Map<String, String> map =
            internalGetShortcut().getMap();
        return map.containsKey(key) ? map.get(key) : defaultValue;
      }

      /**
       * <pre>
       * , req/rsp,
       * </pre>
       *
       * <code>map&lt;string, string&gt; shortcut = 10;</code>
       */

      public String getShortcutOrThrow(
          String key) {
        if (key == null) {
          throw new NullPointerException();
        }
        java.util.Map<String, String> map =
            internalGetShortcut().getMap();
        if (!map.containsKey(key)) {
          throw new IllegalArgumentException();
        }
        return map.get(key);
      }

      public Builder clearShortcut() {
        internalGetMutableShortcut().getMutableMap()
            .clear();
        return this;
      }

      /**
       * <pre>
       * , req/rsp,
       * </pre>
       *
       * <code>map&lt;string, string&gt; shortcut = 10;</code>
       */

      public Builder removeShortcut(
          String key) {
        if (key == null) {
          throw new NullPointerException();
        }
        internalGetMutableShortcut().getMutableMap()
            .remove(key);
        return this;
      }

      /**
       * Use alternate mutation accessors instead.
       */
      @Deprecated
      public java.util.Map<String, String>
      getMutableShortcut() {
        return internalGetMutableShortcut().getMutableMap();
      }

      /**
       * <pre>
       * , req/rsp,
       * </pre>
       *
       * <code>map&lt;string, string&gt; shortcut = 10;</code>
       */
      public Builder putShortcut(
          String key,
          String value) {
        if (key == null) {
          throw new NullPointerException();
        }
        if (value == null) {
          throw new NullPointerException();
        }
        internalGetMutableShortcut().getMutableMap()
            .put(key, value);
        return this;
      }

      /**
       * <pre>
       * , req/rsp,
       * </pre>
       *
       * <code>map&lt;string, string&gt; shortcut = 10;</code>
       */

      public Builder putAllShortcut(
          java.util.Map<String, String> values) {
        internalGetMutableShortcut().getMutableMap()
            .putAll(values);
        return this;
      }

      @Override
      public final Builder setUnknownFields(
          final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.setUnknownFieldsProto3(unknownFields);
      }

      @Override
      public final Builder mergeUnknownFields(
          final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.mergeUnknownFields(unknownFields);
      }


      // @@protoc_insertion_point(builder_scope:collie_log)
    }

  }

  /**
   * <pre>
   *
   * </pre>
   * <p>
   * Protobuf type {@code rule}
   */
  public static final class rule extends
      com.google.protobuf.GeneratedMessageV3 implements
      // @@protoc_insertion_point(message_implements:rule)
      ruleOrBuilder {
    public static final int ID_FIELD_NUMBER = 1;
    public static final int APP_ID_FIELD_NUMBER = 2;
    public static final int TASK_ID_FIELD_NUMBER = 3;
    public static final int EXPR_FIELD_NUMBER = 10;
    public static final int START_AT_FIELD_NUMBER = 11;
    public static final int END_AT_FIELD_NUMBER = 12;
    public static final int STATUS_FIELD_NUMBER = 13;
    public static final int SMALL_FLOW_PPM_FIELD_NUMBER = 14;
    public static final int MTIME_FIELD_NUMBER = 15;
    public static final int NAME_FIELD_NUMBER = 30;
    public static final int DESCR_FIELD_NUMBER = 31;
    public static final int CREATOR_FIELD_NUMBER = 32;
    private static final long serialVersionUID = 0L;
    // @@protoc_insertion_point(class_scope:rule)
    private static final rule DEFAULT_INSTANCE;
    private static final com.google.protobuf.Parser<rule>
        PARSER = new com.google.protobuf.AbstractParser<rule>() {
      @Override
      public rule parsePartialFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws com.google.protobuf.InvalidProtocolBufferException {
        return new rule(input, extensionRegistry);
      }
    };

    static {
      DEFAULT_INSTANCE = new rule();
    }

    private long id_;
    private long appId_;
    private long taskId_;
    private volatile Object expr_;
    private long startAt_;
    private long endAt_;
    private int status_;
    private long smallFlowPpm_;
    private volatile Object mtime_;
    private volatile Object name_;
    private volatile Object descr_;
    private volatile Object creator_;
    private byte memoizedIsInitialized = -1;

    // Use rule.newBuilder() to construct.
    private rule(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
      super(builder);
    }

    private rule() {
      id_ = 0L;
      appId_ = 0L;
      taskId_ = 0L;
      expr_ = "";
      startAt_ = 0L;
      endAt_ = 0L;
      status_ = 0;
      smallFlowPpm_ = 0L;
      mtime_ = "";
      name_ = "";
      descr_ = "";
      creator_ = "";
    }
    private rule(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      this();
      if (extensionRegistry == null) {
        throw new NullPointerException();
      }
      int mutable_bitField0_ = 0;
      com.google.protobuf.UnknownFieldSet.Builder unknownFields =
          com.google.protobuf.UnknownFieldSet.newBuilder();
      try {
        boolean done = false;
        while (!done) {
          int tag = input.readTag();
          switch (tag) {
            case 0:
              done = true;
              break;
            case 8: {

              id_ = input.readInt64();
              break;
            }
            case 16: {

              appId_ = input.readInt64();
              break;
            }
            case 24: {

              taskId_ = input.readInt64();
              break;
            }
            case 82: {
              String s = input.readStringRequireUtf8();

              expr_ = s;
              break;
            }
            case 88: {

              startAt_ = input.readInt64();
              break;
            }
            case 96: {

              endAt_ = input.readInt64();
              break;
            }
            case 104: {
              int rawValue = input.readEnum();

              status_ = rawValue;
              break;
            }
            case 112: {

              smallFlowPpm_ = input.readInt64();
              break;
            }
            case 122: {
              String s = input.readStringRequireUtf8();

              mtime_ = s;
              break;
            }
            case 242: {
              String s = input.readStringRequireUtf8();

              name_ = s;
              break;
            }
            case 250: {
              String s = input.readStringRequireUtf8();

              descr_ = s;
              break;
            }
            case 258: {
              String s = input.readStringRequireUtf8();

              creator_ = s;
              break;
            }
            default: {
              if (!parseUnknownFieldProto3(
                  input, unknownFields, extensionRegistry, tag)) {
                done = true;
              }
              break;
            }
          }
        }
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        throw e.setUnfinishedMessage(this);
      } catch (java.io.IOException e) {
        throw new com.google.protobuf.InvalidProtocolBufferException(
            e).setUnfinishedMessage(this);
      } finally {
        this.unknownFields = unknownFields.build();
        makeExtensionsImmutable();
      }
    }

    public static final com.google.protobuf.Descriptors.Descriptor
    getDescriptor() {
      return TestMap.internal_static_rule_descriptor;
    }

    public static rule parseFrom(
        java.nio.ByteBuffer data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static rule parseFrom(
        java.nio.ByteBuffer data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static rule parseFrom(
        com.google.protobuf.ByteString data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static rule parseFrom(
        com.google.protobuf.ByteString data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static rule parseFrom(byte[] data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static rule parseFrom(
        byte[] data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static rule parseFrom(java.io.InputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input);
    }

    public static rule parseFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static rule parseDelimitedFrom(java.io.InputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseDelimitedWithIOException(PARSER, input);
    }

    public static rule parseDelimitedFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
    }

    public static rule parseFrom(
        com.google.protobuf.CodedInputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input);
    }

    public static rule parseFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static Builder newBuilder() {
      return DEFAULT_INSTANCE.toBuilder();
    }

    public static Builder newBuilder(rule prototype) {
      return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }

    public static rule getDefaultInstance() {
      return DEFAULT_INSTANCE;
    }

    public static com.google.protobuf.Parser<rule> parser() {
      return PARSER;
    }

    @Override
    public final com.google.protobuf.UnknownFieldSet
    getUnknownFields() {
      return this.unknownFields;
    }

    @Override
    protected FieldAccessorTable
    internalGetFieldAccessorTable() {
      return TestMap.internal_static_rule_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              rule.class, Builder.class);
    }

    /**
     * <pre>
     * id()
     * </pre>
     *
     * <code>int64 id = 1;</code>
     */
    public long getId() {
      return id_;
    }

    /**
     * <pre>
     * appid
     * </pre>
     *
     * <code>int64 app_id = 2;</code>
     */
    public long getAppId() {
      return appId_;
    }

    /**
     * <pre>
     * id(id)
     * </pre>
     *
     * <code>int64 task_id = 3;</code>
     */
    public long getTaskId() {
      return taskId_;
    }

    /**
     * <pre>
     * id()
     * </pre>
     *
     * <code>string expr = 10;</code>
     */
    public String getExpr() {
      Object ref = expr_;
      if (ref instanceof String) {
        return (String) ref;
      } else {
        com.google.protobuf.ByteString bs =
            (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        expr_ = s;
        return s;
      }
    }

    /**
     * <pre>
     * id()
     * </pre>
     *
     * <code>string expr = 10;</code>
     */
    public com.google.protobuf.ByteString
    getExprBytes() {
      Object ref = expr_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b =
            com.google.protobuf.ByteString.copyFromUtf8(
                (String) ref);
        expr_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    /**
     * <pre>
     * (: )
     * </pre>
     *
     * <code>int64 start_at = 11;</code>
     */
    public long getStartAt() {
      return startAt_;
    }

    /**
     * <pre>
     * (: )
     * </pre>
     *
     * <code>int64 end_at = 12;</code>
     */
    public long getEndAt() {
      return endAt_;
    }

    /**
     * <code>.rule_status status = 13;</code>
     */
    public int getStatusValue() {
      return status_;
    }

    /**
     * <code>.rule_status status = 13;</code>
     */
    public rule_status getStatus() {
      @SuppressWarnings("deprecation")
      rule_status result = rule_status.valueOf(status_);
      return result == null ? rule_status.UNRECOGNIZED : result;
    }

    /**
     * <pre>
     * (: 10^-6, 0.0001%), status=status_online_small_flow
     * </pre>
     *
     * <code>int64 small_flow_ppm = 14;</code>
     */
    public long getSmallFlowPpm() {
      return smallFlowPpm_;
    }

    /**
     * <pre>
     * (: 2006-01-02 15:04:05)
     * </pre>
     *
     * <code>string mtime = 15;</code>
     */
    public String getMtime() {
      Object ref = mtime_;
      if (ref instanceof String) {
        return (String) ref;
      } else {
        com.google.protobuf.ByteString bs =
            (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        mtime_ = s;
        return s;
      }
    }

    /**
     * <pre>
     * (: 2006-01-02 15:04:05)
     * </pre>
     *
     * <code>string mtime = 15;</code>
     */
    public com.google.protobuf.ByteString
    getMtimeBytes() {
      Object ref = mtime_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b =
            com.google.protobuf.ByteString.copyFromUtf8(
                (String) ref);
        mtime_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    /**
     * <pre>
     * ()
     * </pre>
     *
     * <code>string name = 30;</code>
     */
    public String getName() {
      Object ref = name_;
      if (ref instanceof String) {
        return (String) ref;
      } else {
        com.google.protobuf.ByteString bs =
            (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        name_ = s;
        return s;
      }
    }

    /**
     * <pre>
     * ()
     * </pre>
     *
     * <code>string name = 30;</code>
     */
    public com.google.protobuf.ByteString
    getNameBytes() {
      Object ref = name_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b =
            com.google.protobuf.ByteString.copyFromUtf8(
                (String) ref);
        name_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    /**
     * <code>string descr = 31;</code>
     */
    public String getDescr() {
      Object ref = descr_;
      if (ref instanceof String) {
        return (String) ref;
      } else {
        com.google.protobuf.ByteString bs =
            (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        descr_ = s;
        return s;
      }
    }

    /**
     * <code>string descr = 31;</code>
     */
    public com.google.protobuf.ByteString
    getDescrBytes() {
      Object ref = descr_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b =
            com.google.protobuf.ByteString.copyFromUtf8(
                (String) ref);
        descr_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    /**
     * <pre>
     *
     * </pre>
     *
     * <code>string creator = 32;</code>
     */
    public String getCreator() {
      Object ref = creator_;
      if (ref instanceof String) {
        return (String) ref;
      } else {
        com.google.protobuf.ByteString bs =
            (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        creator_ = s;
        return s;
      }
    }

    /**
     * <pre>
     *
     * </pre>
     *
     * <code>string creator = 32;</code>
     */
    public com.google.protobuf.ByteString
    getCreatorBytes() {
      Object ref = creator_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b =
            com.google.protobuf.ByteString.copyFromUtf8(
                (String) ref);
        creator_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    @Override
    public final boolean isInitialized() {
      byte isInitialized = memoizedIsInitialized;
      if (isInitialized == 1) {
        return true;
      }
      if (isInitialized == 0) {
        return false;
      }

      memoizedIsInitialized = 1;
      return true;
    }

    @Override
    public void writeTo(com.google.protobuf.CodedOutputStream output)
        throws java.io.IOException {
      if (id_ != 0L) {
        output.writeInt64(1, id_);
      }
      if (appId_ != 0L) {
        output.writeInt64(2, appId_);
      }
      if (taskId_ != 0L) {
        output.writeInt64(3, taskId_);
      }
      if (!getExprBytes().isEmpty()) {
        com.google.protobuf.GeneratedMessageV3.writeString(output, 10, expr_);
      }
      if (startAt_ != 0L) {
        output.writeInt64(11, startAt_);
      }
      if (endAt_ != 0L) {
        output.writeInt64(12, endAt_);
      }
      if (status_ != rule_status.status_unused.getNumber()) {
        output.writeEnum(13, status_);
      }
      if (smallFlowPpm_ != 0L) {
        output.writeInt64(14, smallFlowPpm_);
      }
      if (!getMtimeBytes().isEmpty()) {
        com.google.protobuf.GeneratedMessageV3.writeString(output, 15, mtime_);
      }
      if (!getNameBytes().isEmpty()) {
        com.google.protobuf.GeneratedMessageV3.writeString(output, 30, name_);
      }
      if (!getDescrBytes().isEmpty()) {
        com.google.protobuf.GeneratedMessageV3.writeString(output, 31, descr_);
      }
      if (!getCreatorBytes().isEmpty()) {
        com.google.protobuf.GeneratedMessageV3.writeString(output, 32, creator_);
      }
      unknownFields.writeTo(output);
    }

    @Override
    public int getSerializedSize() {
      int size = memoizedSize;
      if (size != -1) {
        return size;
      }

      size = 0;
      if (id_ != 0L) {
        size += com.google.protobuf.CodedOutputStream
            .computeInt64Size(1, id_);
      }
      if (appId_ != 0L) {
        size += com.google.protobuf.CodedOutputStream
            .computeInt64Size(2, appId_);
      }
      if (taskId_ != 0L) {
        size += com.google.protobuf.CodedOutputStream
            .computeInt64Size(3, taskId_);
      }
      if (!getExprBytes().isEmpty()) {
        size += com.google.protobuf.GeneratedMessageV3.computeStringSize(10, expr_);
      }
      if (startAt_ != 0L) {
        size += com.google.protobuf.CodedOutputStream
            .computeInt64Size(11, startAt_);
      }
      if (endAt_ != 0L) {
        size += com.google.protobuf.CodedOutputStream
            .computeInt64Size(12, endAt_);
      }
      if (status_ != rule_status.status_unused.getNumber()) {
        size += com.google.protobuf.CodedOutputStream
            .computeEnumSize(13, status_);
      }
      if (smallFlowPpm_ != 0L) {
        size += com.google.protobuf.CodedOutputStream
            .computeInt64Size(14, smallFlowPpm_);
      }
      if (!getMtimeBytes().isEmpty()) {
        size += com.google.protobuf.GeneratedMessageV3.computeStringSize(15, mtime_);
      }
      if (!getNameBytes().isEmpty()) {
        size += com.google.protobuf.GeneratedMessageV3.computeStringSize(30, name_);
      }
      if (!getDescrBytes().isEmpty()) {
        size += com.google.protobuf.GeneratedMessageV3.computeStringSize(31, descr_);
      }
      if (!getCreatorBytes().isEmpty()) {
        size += com.google.protobuf.GeneratedMessageV3.computeStringSize(32, creator_);
      }
      size += unknownFields.getSerializedSize();
      memoizedSize = size;
      return size;
    }

    @Override
    public boolean equals(final Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof rule)) {
        return super.equals(obj);
      }
      rule other = (rule) obj;

      boolean result = true;
      result = result && (getId()
          == other.getId());
      result = result && (getAppId()
          == other.getAppId());
      result = result && (getTaskId()
          == other.getTaskId());
      result = result && getExpr()
          .equals(other.getExpr());
      result = result && (getStartAt()
          == other.getStartAt());
      result = result && (getEndAt()
          == other.getEndAt());
      result = result && status_ == other.status_;
      result = result && (getSmallFlowPpm()
          == other.getSmallFlowPpm());
      result = result && getMtime()
          .equals(other.getMtime());
      result = result && getName()
          .equals(other.getName());
      result = result && getDescr()
          .equals(other.getDescr());
      result = result && getCreator()
          .equals(other.getCreator());
      result = result && unknownFields.equals(other.unknownFields);
      return result;
    }

    @Override
    public int hashCode() {
      if (memoizedHashCode != 0) {
        return memoizedHashCode;
      }
      int hash = 41;
      hash = (19 * hash) + getDescriptor().hashCode();
      hash = (37 * hash) + ID_FIELD_NUMBER;
      hash = (53 * hash) + com.google.protobuf.Internal.hashLong(
          getId());
      hash = (37 * hash) + APP_ID_FIELD_NUMBER;
      hash = (53 * hash) + com.google.protobuf.Internal.hashLong(
          getAppId());
      hash = (37 * hash) + TASK_ID_FIELD_NUMBER;
      hash = (53 * hash) + com.google.protobuf.Internal.hashLong(
          getTaskId());
      hash = (37 * hash) + EXPR_FIELD_NUMBER;
      hash = (53 * hash) + getExpr().hashCode();
      hash = (37 * hash) + START_AT_FIELD_NUMBER;
      hash = (53 * hash) + com.google.protobuf.Internal.hashLong(
          getStartAt());
      hash = (37 * hash) + END_AT_FIELD_NUMBER;
      hash = (53 * hash) + com.google.protobuf.Internal.hashLong(
          getEndAt());
      hash = (37 * hash) + STATUS_FIELD_NUMBER;
      hash = (53 * hash) + status_;
      hash = (37 * hash) + SMALL_FLOW_PPM_FIELD_NUMBER;
      hash = (53 * hash) + com.google.protobuf.Internal.hashLong(
          getSmallFlowPpm());
      hash = (37 * hash) + MTIME_FIELD_NUMBER;
      hash = (53 * hash) + getMtime().hashCode();
      hash = (37 * hash) + NAME_FIELD_NUMBER;
      hash = (53 * hash) + getName().hashCode();
      hash = (37 * hash) + DESCR_FIELD_NUMBER;
      hash = (53 * hash) + getDescr().hashCode();
      hash = (37 * hash) + CREATOR_FIELD_NUMBER;
      hash = (53 * hash) + getCreator().hashCode();
      hash = (29 * hash) + unknownFields.hashCode();
      memoizedHashCode = hash;
      return hash;
    }

    @Override
    public Builder newBuilderForType() {
      return newBuilder();
    }

    @Override
    public Builder toBuilder() {
      return this == DEFAULT_INSTANCE
          ? new Builder() : new Builder().mergeFrom(this);
    }

    @Override
    protected Builder newBuilderForType(
        BuilderParent parent) {
      Builder builder = new Builder(parent);
      return builder;
    }

    @Override
    public com.google.protobuf.Parser<rule> getParserForType() {
      return PARSER;
    }

    @Override
    public rule getDefaultInstanceForType() {
      return DEFAULT_INSTANCE;
    }

    /**
     * <pre>
     *
     * </pre>
     * <p>
     * Protobuf type {@code rule}
     */
    public static final class Builder extends
        com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
        // @@protoc_insertion_point(builder_implements:rule)
        ruleOrBuilder {
      private long id_;
      private long appId_;
      private long taskId_;
      private Object expr_ = "";
      private long startAt_;
      private long endAt_;
      private int status_ = 0;
      private long smallFlowPpm_;
      private Object mtime_ = "";
      private Object name_ = "";
      private Object descr_ = "";
      private Object creator_ = "";

      // Construct using TestMap.rule.newBuilder()
      private Builder() {
        maybeForceBuilderInitialization();
      }

      private Builder(
          BuilderParent parent) {
        super(parent);
        maybeForceBuilderInitialization();
      }

      public static final com.google.protobuf.Descriptors.Descriptor
      getDescriptor() {
        return TestMap.internal_static_rule_descriptor;
      }

      @Override
      protected FieldAccessorTable
      internalGetFieldAccessorTable() {
        return TestMap.internal_static_rule_fieldAccessorTable
            .ensureFieldAccessorsInitialized(
                rule.class, Builder.class);
      }

      private void maybeForceBuilderInitialization() {
        if (com.google.protobuf.GeneratedMessageV3
            .alwaysUseFieldBuilders) {
        }
      }

      @Override
      public Builder clear() {
        super.clear();
        id_ = 0L;

        appId_ = 0L;

        taskId_ = 0L;

        expr_ = "";

        startAt_ = 0L;

        endAt_ = 0L;

        status_ = 0;

        smallFlowPpm_ = 0L;

        mtime_ = "";

        name_ = "";

        descr_ = "";

        creator_ = "";

        return this;
      }

      @Override
      public com.google.protobuf.Descriptors.Descriptor
      getDescriptorForType() {
        return TestMap.internal_static_rule_descriptor;
      }

      @Override
      public rule getDefaultInstanceForType() {
        return rule.getDefaultInstance();
      }

      @Override
      public rule build() {
        rule result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }

      @Override
      public rule buildPartial() {
        rule result = new rule(this);
        result.id_ = id_;
        result.appId_ = appId_;
        result.taskId_ = taskId_;
        result.expr_ = expr_;
        result.startAt_ = startAt_;
        result.endAt_ = endAt_;
        result.status_ = status_;
        result.smallFlowPpm_ = smallFlowPpm_;
        result.mtime_ = mtime_;
        result.name_ = name_;
        result.descr_ = descr_;
        result.creator_ = creator_;
        onBuilt();
        return result;
      }

      @Override
      public Builder clone() {
        return (Builder) super.clone();
      }

      @Override
      public Builder setField(
          com.google.protobuf.Descriptors.FieldDescriptor field,
          Object value) {
        return (Builder) super.setField(field, value);
      }

      @Override
      public Builder clearField(
          com.google.protobuf.Descriptors.FieldDescriptor field) {
        return (Builder) super.clearField(field);
      }

      @Override
      public Builder clearOneof(
          com.google.protobuf.Descriptors.OneofDescriptor oneof) {
        return (Builder) super.clearOneof(oneof);
      }

      @Override
      public Builder setRepeatedField(
          com.google.protobuf.Descriptors.FieldDescriptor field,
          int index, Object value) {
        return (Builder) super.setRepeatedField(field, index, value);
      }

      @Override
      public Builder addRepeatedField(
          com.google.protobuf.Descriptors.FieldDescriptor field,
          Object value) {
        return (Builder) super.addRepeatedField(field, value);
      }

      @Override
      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof rule) {
          return mergeFrom((rule) other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }

      public Builder mergeFrom(rule other) {
        if (other == rule.getDefaultInstance()) {
          return this;
        }
        if (other.getId() != 0L) {
          setId(other.getId());
        }
        if (other.getAppId() != 0L) {
          setAppId(other.getAppId());
        }
        if (other.getTaskId() != 0L) {
          setTaskId(other.getTaskId());
        }
        if (!other.getExpr().isEmpty()) {
          expr_ = other.expr_;
          onChanged();
        }
        if (other.getStartAt() != 0L) {
          setStartAt(other.getStartAt());
        }
        if (other.getEndAt() != 0L) {
          setEndAt(other.getEndAt());
        }
        if (other.status_ != 0) {
          setStatusValue(other.getStatusValue());
        }
        if (other.getSmallFlowPpm() != 0L) {
          setSmallFlowPpm(other.getSmallFlowPpm());
        }
        if (!other.getMtime().isEmpty()) {
          mtime_ = other.mtime_;
          onChanged();
        }
        if (!other.getName().isEmpty()) {
          name_ = other.name_;
          onChanged();
        }
        if (!other.getDescr().isEmpty()) {
          descr_ = other.descr_;
          onChanged();
        }
        if (!other.getCreator().isEmpty()) {
          creator_ = other.creator_;
          onChanged();
        }
        this.mergeUnknownFields(other.unknownFields);
        onChanged();
        return this;
      }

      @Override
      public final boolean isInitialized() {
        return true;
      }

      @Override
      public Builder mergeFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws java.io.IOException {
        rule parsedMessage = null;
        try {
          parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          parsedMessage = (rule) e.getUnfinishedMessage();
          throw e.unwrapIOException();
        } finally {
          if (parsedMessage != null) {
            mergeFrom(parsedMessage);
          }
        }
        return this;
      }

      /**
       * <pre>
       * id()
       * </pre>
       *
       * <code>int64 id = 1;</code>
       */
      public long getId() {
        return id_;
      }

      /**
       * <pre>
       * id()
       * </pre>
       *
       * <code>int64 id = 1;</code>
       */
      public Builder setId(long value) {

        id_ = value;
        onChanged();
        return this;
      }

      /**
       * <pre>
       * id()
       * </pre>
       *
       * <code>int64 id = 1;</code>
       */
      public Builder clearId() {

        id_ = 0L;
        onChanged();
        return this;
      }

      /**
       * <pre>
       * appid
       * </pre>
       *
       * <code>int64 app_id = 2;</code>
       */
      public long getAppId() {
        return appId_;
      }

      /**
       * <pre>
       * appid
       * </pre>
       *
       * <code>int64 app_id = 2;</code>
       */
      public Builder setAppId(long value) {

        appId_ = value;
        onChanged();
        return this;
      }

      /**
       * <pre>
       * appid
       * </pre>
       *
       * <code>int64 app_id = 2;</code>
       */
      public Builder clearAppId() {

        appId_ = 0L;
        onChanged();
        return this;
      }

      /**
       * <pre>
       * id(id)
       * </pre>
       *
       * <code>int64 task_id = 3;</code>
       */
      public long getTaskId() {
        return taskId_;
      }

      /**
       * <pre>
       * id(id)
       * </pre>
       *
       * <code>int64 task_id = 3;</code>
       */
      public Builder setTaskId(long value) {

        taskId_ = value;
        onChanged();
        return this;
      }

      /**
       * <pre>
       * id(id)
       * </pre>
       *
       * <code>int64 task_id = 3;</code>
       */
      public Builder clearTaskId() {

        taskId_ = 0L;
        onChanged();
        return this;
      }

      /**
       * <pre>
       * id()
       * </pre>
       *
       * <code>string expr = 10;</code>
       */
      public String getExpr() {
        Object ref = expr_;
        if (!(ref instanceof String)) {
          com.google.protobuf.ByteString bs =
              (com.google.protobuf.ByteString) ref;
          String s = bs.toStringUtf8();
          expr_ = s;
          return s;
        } else {
          return (String) ref;
        }
      }

      /**
       * <pre>
       * id()
       * </pre>
       *
       * <code>string expr = 10;</code>
       */
      public Builder setExpr(
          String value) {
        if (value == null) {
          throw new NullPointerException();
        }

        expr_ = value;
        onChanged();
        return this;
      }

      /**
       * <pre>
       * id()
       * </pre>
       *
       * <code>string expr = 10;</code>
       */
      public com.google.protobuf.ByteString
      getExprBytes() {
        Object ref = expr_;
        if (ref instanceof String) {
          com.google.protobuf.ByteString b =
              com.google.protobuf.ByteString.copyFromUtf8(
                  (String) ref);
          expr_ = b;
          return b;
        } else {
          return (com.google.protobuf.ByteString) ref;
        }
      }

      /**
       * <pre>
       * id()
       * </pre>
       *
       * <code>string expr = 10;</code>
       */
      public Builder setExprBytes(
          com.google.protobuf.ByteString value) {
        if (value == null) {
          throw new NullPointerException();
        }
        checkByteStringIsUtf8(value);

        expr_ = value;
        onChanged();
        return this;
      }

      /**
       * <pre>
       * id()
       * </pre>
       *
       * <code>string expr = 10;</code>
       */
      public Builder clearExpr() {

        expr_ = getDefaultInstance().getExpr();
        onChanged();
        return this;
      }

      /**
       * <pre>
       * (: )
       * </pre>
       *
       * <code>int64 start_at = 11;</code>
       */
      public long getStartAt() {
        return startAt_;
      }

      /**
       * <pre>
       * (: )
       * </pre>
       *
       * <code>int64 start_at = 11;</code>
       */
      public Builder setStartAt(long value) {

        startAt_ = value;
        onChanged();
        return this;
      }

      /**
       * <pre>
       * (: )
       * </pre>
       *
       * <code>int64 start_at = 11;</code>
       */
      public Builder clearStartAt() {

        startAt_ = 0L;
        onChanged();
        return this;
      }

      /**
       * <pre>
       * (: )
       * </pre>
       *
       * <code>int64 end_at = 12;</code>
       */
      public long getEndAt() {
        return endAt_;
      }

      /**
       * <pre>
       * (: )
       * </pre>
       *
       * <code>int64 end_at = 12;</code>
       */
      public Builder setEndAt(long value) {

        endAt_ = value;
        onChanged();
        return this;
      }

      /**
       * <pre>
       * (: )
       * </pre>
       *
       * <code>int64 end_at = 12;</code>
       */
      public Builder clearEndAt() {

        endAt_ = 0L;
        onChanged();
        return this;
      }

      /**
       * <code>.rule_status status = 13;</code>
       */
      public int getStatusValue() {
        return status_;
      }

      /**
       * <code>.rule_status status = 13;</code>
       */
      public Builder setStatusValue(int value) {
        status_ = value;
        onChanged();
        return this;
      }

      /**
       * <code>.rule_status status = 13;</code>
       */
      public rule_status getStatus() {
        @SuppressWarnings("deprecation")
        rule_status result = rule_status.valueOf(status_);
        return result == null ? rule_status.UNRECOGNIZED : result;
      }

      /**
       * <code>.rule_status status = 13;</code>
       */
      public Builder setStatus(rule_status value) {
        if (value == null) {
          throw new NullPointerException();
        }

        status_ = value.getNumber();
        onChanged();
        return this;
      }

      /**
       * <code>.rule_status status = 13;</code>
       */
      public Builder clearStatus() {

        status_ = 0;
        onChanged();
        return this;
      }

      /**
       * <pre>
       * (: 10^-6, 0.0001%), status=status_online_small_flow
       * </pre>
       *
       * <code>int64 small_flow_ppm = 14;</code>
       */
      public long getSmallFlowPpm() {
        return smallFlowPpm_;
      }

      /**
       * <pre>
       * (: 10^-6, 0.0001%), status=status_online_small_flow
       * </pre>
       *
       * <code>int64 small_flow_ppm = 14;</code>
       */
      public Builder setSmallFlowPpm(long value) {

        smallFlowPpm_ = value;
        onChanged();
        return this;
      }

      /**
       * <pre>
       * (: 10^-6, 0.0001%), status=status_online_small_flow
       * </pre>
       *
       * <code>int64 small_flow_ppm = 14;</code>
       */
      public Builder clearSmallFlowPpm() {

        smallFlowPpm_ = 0L;
        onChanged();
        return this;
      }

      /**
       * <pre>
       * (: 2006-01-02 15:04:05)
       * </pre>
       *
       * <code>string mtime = 15;</code>
       */
      public String getMtime() {
        Object ref = mtime_;
        if (!(ref instanceof String)) {
          com.google.protobuf.ByteString bs =
              (com.google.protobuf.ByteString) ref;
          String s = bs.toStringUtf8();
          mtime_ = s;
          return s;
        } else {
          return (String) ref;
        }
      }

      /**
       * <pre>
       * (: 2006-01-02 15:04:05)
       * </pre>
       *
       * <code>string mtime = 15;</code>
       */
      public Builder setMtime(
          String value) {
        if (value == null) {
          throw new NullPointerException();
        }

        mtime_ = value;
        onChanged();
        return this;
      }

      /**
       * <pre>
       * (: 2006-01-02 15:04:05)
       * </pre>
       *
       * <code>string mtime = 15;</code>
       */
      public com.google.protobuf.ByteString
      getMtimeBytes() {
        Object ref = mtime_;
        if (ref instanceof String) {
          com.google.protobuf.ByteString b =
              com.google.protobuf.ByteString.copyFromUtf8(
                  (String) ref);
          mtime_ = b;
          return b;
        } else {
          return (com.google.protobuf.ByteString) ref;
        }
      }

      /**
       * <pre>
       * (: 2006-01-02 15:04:05)
       * </pre>
       *
       * <code>string mtime = 15;</code>
       */
      public Builder setMtimeBytes(
          com.google.protobuf.ByteString value) {
        if (value == null) {
          throw new NullPointerException();
        }
        checkByteStringIsUtf8(value);

        mtime_ = value;
        onChanged();
        return this;
      }

      /**
       * <pre>
       * (: 2006-01-02 15:04:05)
       * </pre>
       *
       * <code>string mtime = 15;</code>
       */
      public Builder clearMtime() {

        mtime_ = getDefaultInstance().getMtime();
        onChanged();
        return this;
      }

      /**
       * <pre>
       * ()
       * </pre>
       *
       * <code>string name = 30;</code>
       */
      public String getName() {
        Object ref = name_;
        if (!(ref instanceof String)) {
          com.google.protobuf.ByteString bs =
              (com.google.protobuf.ByteString) ref;
          String s = bs.toStringUtf8();
          name_ = s;
          return s;
        } else {
          return (String) ref;
        }
      }

      /**
       * <pre>
       * ()
       * </pre>
       *
       * <code>string name = 30;</code>
       */
      public Builder setName(
          String value) {
        if (value == null) {
          throw new NullPointerException();
        }

        name_ = value;
        onChanged();
        return this;
      }

      /**
       * <pre>
       * ()
       * </pre>
       *
       * <code>string name = 30;</code>
       */
      public com.google.protobuf.ByteString
      getNameBytes() {
        Object ref = name_;
        if (ref instanceof String) {
          com.google.protobuf.ByteString b =
              com.google.protobuf.ByteString.copyFromUtf8(
                  (String) ref);
          name_ = b;
          return b;
        } else {
          return (com.google.protobuf.ByteString) ref;
        }
      }

      /**
       * <pre>
       * ()
       * </pre>
       *
       * <code>string name = 30;</code>
       */
      public Builder setNameBytes(
          com.google.protobuf.ByteString value) {
        if (value == null) {
          throw new NullPointerException();
        }
        checkByteStringIsUtf8(value);

        name_ = value;
        onChanged();
        return this;
      }

      /**
       * <pre>
       * ()
       * </pre>
       *
       * <code>string name = 30;</code>
       */
      public Builder clearName() {

        name_ = getDefaultInstance().getName();
        onChanged();
        return this;
      }

      /**
       * <code>string descr = 31;</code>
       */
      public String getDescr() {
        Object ref = descr_;
        if (!(ref instanceof String)) {
          com.google.protobuf.ByteString bs =
              (com.google.protobuf.ByteString) ref;
          String s = bs.toStringUtf8();
          descr_ = s;
          return s;
        } else {
          return (String) ref;
        }
      }

      /**
       * <code>string descr = 31;</code>
       */
      public Builder setDescr(
          String value) {
        if (value == null) {
          throw new NullPointerException();
        }

        descr_ = value;
        onChanged();
        return this;
      }

      /**
       * <code>string descr = 31;</code>
       */
      public com.google.protobuf.ByteString
      getDescrBytes() {
        Object ref = descr_;
        if (ref instanceof String) {
          com.google.protobuf.ByteString b =
              com.google.protobuf.ByteString.copyFromUtf8(
                  (String) ref);
          descr_ = b;
          return b;
        } else {
          return (com.google.protobuf.ByteString) ref;
        }
      }

      /**
       * <code>string descr = 31;</code>
       */
      public Builder setDescrBytes(
          com.google.protobuf.ByteString value) {
        if (value == null) {
          throw new NullPointerException();
        }
        checkByteStringIsUtf8(value);

        descr_ = value;
        onChanged();
        return this;
      }

      /**
       * <code>string descr = 31;</code>
       */
      public Builder clearDescr() {

        descr_ = getDefaultInstance().getDescr();
        onChanged();
        return this;
      }

      /**
       * <pre>
       *
       * </pre>
       *
       * <code>string creator = 32;</code>
       */
      public String getCreator() {
        Object ref = creator_;
        if (!(ref instanceof String)) {
          com.google.protobuf.ByteString bs =
              (com.google.protobuf.ByteString) ref;
          String s = bs.toStringUtf8();
          creator_ = s;
          return s;
        } else {
          return (String) ref;
        }
      }

      /**
       * <pre>
       *
       * </pre>
       *
       * <code>string creator = 32;</code>
       */
      public Builder setCreator(
          String value) {
        if (value == null) {
          throw new NullPointerException();
        }

        creator_ = value;
        onChanged();
        return this;
      }

      /**
       * <pre>
       *
       * </pre>
       *
       * <code>string creator = 32;</code>
       */
      public com.google.protobuf.ByteString
      getCreatorBytes() {
        Object ref = creator_;
        if (ref instanceof String) {
          com.google.protobuf.ByteString b =
              com.google.protobuf.ByteString.copyFromUtf8(
                  (String) ref);
          creator_ = b;
          return b;
        } else {
          return (com.google.protobuf.ByteString) ref;
        }
      }

      /**
       * <pre>
       *
       * </pre>
       *
       * <code>string creator = 32;</code>
       */
      public Builder setCreatorBytes(
          com.google.protobuf.ByteString value) {
        if (value == null) {
          throw new NullPointerException();
        }
        checkByteStringIsUtf8(value);

        creator_ = value;
        onChanged();
        return this;
      }

      /**
       * <pre>
       *
       * </pre>
       *
       * <code>string creator = 32;</code>
       */
      public Builder clearCreator() {

        creator_ = getDefaultInstance().getCreator();
        onChanged();
        return this;
      }

      @Override
      public final Builder setUnknownFields(
          final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.setUnknownFieldsProto3(unknownFields);
      }

      @Override
      public final Builder mergeUnknownFields(
          final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.mergeUnknownFields(unknownFields);
      }


      // @@protoc_insertion_point(builder_scope:rule)
    }

  }

  /**
   * Protobuf type {@code req}
   */
  public static final class req extends
      com.google.protobuf.GeneratedMessageV3 implements
      // @@protoc_insertion_point(message_implements:req)
      reqOrBuilder {
    public static final int APPID_FIELD_NUMBER = 1;
    public static final int TASK_ID_LIST_FIELD_NUMBER = 2;
    public static final int USER_ID_FIELD_NUMBER = 3;
    public static final int DEVICE_ID_FIELD_NUMBER = 4;
    public static final int API_NO_FIELD_NUMBER = 5;
    public static final int JSON_STR_FIELD_NUMBER = 6;
    private static final long serialVersionUID = 0L;
    // @@protoc_insertion_point(class_scope:req)
    private static final req DEFAULT_INSTANCE;
    private static final com.google.protobuf.Parser<req>
        PARSER = new com.google.protobuf.AbstractParser<req>() {
      @Override
      public req parsePartialFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws com.google.protobuf.InvalidProtocolBufferException {
        return new req(input, extensionRegistry);
      }
    };

    static {
      DEFAULT_INSTANCE = new req();
    }

    private int bitField0_;
    private long appid_;
    private java.util.List<Long> taskIdList_;
    private int taskIdListMemoizedSerializedSize = -1;
    private long userId_;
    private long deviceId_;
    private long apiNo_;
    private volatile Object jsonStr_;
    private byte memoizedIsInitialized = -1;

    // Use req.newBuilder() to construct.
    private req(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
      super(builder);
    }

    private req() {
      appid_ = 0L;
      taskIdList_ = java.util.Collections.emptyList();
      userId_ = 0L;
      deviceId_ = 0L;
      apiNo_ = 0L;
      jsonStr_ = "";
    }
    private req(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      this();
      if (extensionRegistry == null) {
        throw new NullPointerException();
      }
      int mutable_bitField0_ = 0;
      com.google.protobuf.UnknownFieldSet.Builder unknownFields =
          com.google.protobuf.UnknownFieldSet.newBuilder();
      try {
        boolean done = false;
        while (!done) {
          int tag = input.readTag();
          switch (tag) {
            case 0:
              done = true;
              break;
            case 8: {

              appid_ = input.readInt64();
              break;
            }
            case 16: {
              if (!((mutable_bitField0_ & 0x00000002) == 0x00000002)) {
                taskIdList_ = new java.util.ArrayList<Long>();
                mutable_bitField0_ |= 0x00000002;
              }
              taskIdList_.add(input.readInt64());
              break;
            }
            case 18: {
              int length = input.readRawVarint32();
              int limit = input.pushLimit(length);
              if (!((mutable_bitField0_ & 0x00000002) == 0x00000002) && input.getBytesUntilLimit() > 0) {
                taskIdList_ = new java.util.ArrayList<Long>();
                mutable_bitField0_ |= 0x00000002;
              }
              while (input.getBytesUntilLimit() > 0) {
                taskIdList_.add(input.readInt64());
              }
              input.popLimit(limit);
              break;
            }
            case 24: {

              userId_ = input.readInt64();
              break;
            }
            case 32: {

              deviceId_ = input.readInt64();
              break;
            }
            case 40: {

              apiNo_ = input.readInt64();
              break;
            }
            case 50: {
              String s = input.readStringRequireUtf8();

              jsonStr_ = s;
              break;
            }
            default: {
              if (!parseUnknownFieldProto3(
                  input, unknownFields, extensionRegistry, tag)) {
                done = true;
              }
              break;
            }
          }
        }
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        throw e.setUnfinishedMessage(this);
      } catch (java.io.IOException e) {
        throw new com.google.protobuf.InvalidProtocolBufferException(
            e).setUnfinishedMessage(this);
      } finally {
        if (((mutable_bitField0_ & 0x00000002) == 0x00000002)) {
          taskIdList_ = java.util.Collections.unmodifiableList(taskIdList_);
        }
        this.unknownFields = unknownFields.build();
        makeExtensionsImmutable();
      }
    }

    public static final com.google.protobuf.Descriptors.Descriptor
    getDescriptor() {
      return TestMap.internal_static_req_descriptor;
    }

    public static req parseFrom(
        java.nio.ByteBuffer data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static req parseFrom(
        java.nio.ByteBuffer data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static req parseFrom(
        com.google.protobuf.ByteString data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static req parseFrom(
        com.google.protobuf.ByteString data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static req parseFrom(byte[] data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static req parseFrom(
        byte[] data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static req parseFrom(java.io.InputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input);
    }

    public static req parseFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static req parseDelimitedFrom(java.io.InputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseDelimitedWithIOException(PARSER, input);
    }

    public static req parseDelimitedFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
    }

    public static req parseFrom(
        com.google.protobuf.CodedInputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input);
    }

    public static req parseFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static Builder newBuilder() {
      return DEFAULT_INSTANCE.toBuilder();
    }

    public static Builder newBuilder(req prototype) {
      return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }

    public static req getDefaultInstance() {
      return DEFAULT_INSTANCE;
    }

    public static com.google.protobuf.Parser<req> parser() {
      return PARSER;
    }

    @Override
    public final com.google.protobuf.UnknownFieldSet
    getUnknownFields() {
      return this.unknownFields;
    }

    @Override
    protected FieldAccessorTable
    internalGetFieldAccessorTable() {
      return TestMap.internal_static_req_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              req.class, Builder.class);
    }

    /**
     * <code>int64 appid = 1;</code>
     */
    public long getAppid() {
      return appid_;
    }

    /**
     * <code>repeated int64 task_id_list = 2;</code>
     */
    public java.util.List<Long>
    getTaskIdListList() {
      return taskIdList_;
    }

    /**
     * <code>repeated int64 task_id_list = 2;</code>
     */
    public int getTaskIdListCount() {
      return taskIdList_.size();
    }

    /**
     * <code>repeated int64 task_id_list = 2;</code>
     */
    public long getTaskIdList(int index) {
      return taskIdList_.get(index);
    }

    /**
     * <code>int64 user_id = 3;</code>
     */
    public long getUserId() {
      return userId_;
    }

    /**
     * <code>int64 device_id = 4;</code>
     */
    public long getDeviceId() {
      return deviceId_;
    }

    /**
     * <pre>
     *
     * </pre>
     *
     * <code>int64 api_no = 5;</code>
     */
    public long getApiNo() {
      return apiNo_;
    }

    /**
     * <pre>
     * json
     * </pre>
     *
     * <code>string json_str = 6;</code>
     */
    public String getJsonStr() {
      Object ref = jsonStr_;
      if (ref instanceof String) {
        return (String) ref;
      } else {
        com.google.protobuf.ByteString bs =
            (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        jsonStr_ = s;
        return s;
      }
    }

    /**
     * <pre>
     * json
     * </pre>
     *
     * <code>string json_str = 6;</code>
     */
    public com.google.protobuf.ByteString
    getJsonStrBytes() {
      Object ref = jsonStr_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b =
            com.google.protobuf.ByteString.copyFromUtf8(
                (String) ref);
        jsonStr_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    @Override
    public final boolean isInitialized() {
      byte isInitialized = memoizedIsInitialized;
      if (isInitialized == 1) {
        return true;
      }
      if (isInitialized == 0) {
        return false;
      }

      memoizedIsInitialized = 1;
      return true;
    }

    @Override
    public void writeTo(com.google.protobuf.CodedOutputStream output)
        throws java.io.IOException {
      getSerializedSize();
      if (appid_ != 0L) {
        output.writeInt64(1, appid_);
      }
      if (getTaskIdListList().size() > 0) {
        output.writeUInt32NoTag(18);
        output.writeUInt32NoTag(taskIdListMemoizedSerializedSize);
      }
      for (int i = 0; i < taskIdList_.size(); i++) {
        output.writeInt64NoTag(taskIdList_.get(i));
      }
      if (userId_ != 0L) {
        output.writeInt64(3, userId_);
      }
      if (deviceId_ != 0L) {
        output.writeInt64(4, deviceId_);
      }
      if (apiNo_ != 0L) {
        output.writeInt64(5, apiNo_);
      }
      if (!getJsonStrBytes().isEmpty()) {
        com.google.protobuf.GeneratedMessageV3.writeString(output, 6, jsonStr_);
      }
      unknownFields.writeTo(output);
    }

    @Override
    public int getSerializedSize() {
      int size = memoizedSize;
      if (size != -1) {
        return size;
      }

      size = 0;
      if (appid_ != 0L) {
        size += com.google.protobuf.CodedOutputStream
            .computeInt64Size(1, appid_);
      }
      {
        int dataSize = 0;
        for (int i = 0; i < taskIdList_.size(); i++) {
          dataSize += com.google.protobuf.CodedOutputStream
              .computeInt64SizeNoTag(taskIdList_.get(i));
        }
        size += dataSize;
        if (!getTaskIdListList().isEmpty()) {
          size += 1;
          size += com.google.protobuf.CodedOutputStream
              .computeInt32SizeNoTag(dataSize);
        }
        taskIdListMemoizedSerializedSize = dataSize;
      }
      if (userId_ != 0L) {
        size += com.google.protobuf.CodedOutputStream
            .computeInt64Size(3, userId_);
      }
      if (deviceId_ != 0L) {
        size += com.google.protobuf.CodedOutputStream
            .computeInt64Size(4, deviceId_);
      }
      if (apiNo_ != 0L) {
        size += com.google.protobuf.CodedOutputStream
            .computeInt64Size(5, apiNo_);
      }
      if (!getJsonStrBytes().isEmpty()) {
        size += com.google.protobuf.GeneratedMessageV3.computeStringSize(6, jsonStr_);
      }
      size += unknownFields.getSerializedSize();
      memoizedSize = size;
      return size;
    }

    @Override
    public boolean equals(final Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof req)) {
        return super.equals(obj);
      }
      req other = (req) obj;

      boolean result = true;
      result = result && (getAppid()
          == other.getAppid());
      result = result && getTaskIdListList()
          .equals(other.getTaskIdListList());
      result = result && (getUserId()
          == other.getUserId());
      result = result && (getDeviceId()
          == other.getDeviceId());
      result = result && (getApiNo()
          == other.getApiNo());
      result = result && getJsonStr()
          .equals(other.getJsonStr());
      result = result && unknownFields.equals(other.unknownFields);
      return result;
    }

    @Override
    public int hashCode() {
      if (memoizedHashCode != 0) {
        return memoizedHashCode;
      }
      int hash = 41;
      hash = (19 * hash) + getDescriptor().hashCode();
      hash = (37 * hash) + APPID_FIELD_NUMBER;
      hash = (53 * hash) + com.google.protobuf.Internal.hashLong(
          getAppid());
      if (getTaskIdListCount() > 0) {
        hash = (37 * hash) + TASK_ID_LIST_FIELD_NUMBER;
        hash = (53 * hash) + getTaskIdListList().hashCode();
      }
      hash = (37 * hash) + USER_ID_FIELD_NUMBER;
      hash = (53 * hash) + com.google.protobuf.Internal.hashLong(
          getUserId());
      hash = (37 * hash) + DEVICE_ID_FIELD_NUMBER;
      hash = (53 * hash) + com.google.protobuf.Internal.hashLong(
          getDeviceId());
      hash = (37 * hash) + API_NO_FIELD_NUMBER;
      hash = (53 * hash) + com.google.protobuf.Internal.hashLong(
          getApiNo());
      hash = (37 * hash) + JSON_STR_FIELD_NUMBER;
      hash = (53 * hash) + getJsonStr().hashCode();
      hash = (29 * hash) + unknownFields.hashCode();
      memoizedHashCode = hash;
      return hash;
    }

    @Override
    public Builder newBuilderForType() {
      return newBuilder();
    }

    @Override
    public Builder toBuilder() {
      return this == DEFAULT_INSTANCE
          ? new Builder() : new Builder().mergeFrom(this);
    }

    @Override
    protected Builder newBuilderForType(
        BuilderParent parent) {
      Builder builder = new Builder(parent);
      return builder;
    }

    @Override
    public com.google.protobuf.Parser<req> getParserForType() {
      return PARSER;
    }

    @Override
    public req getDefaultInstanceForType() {
      return DEFAULT_INSTANCE;
    }

    /**
     * Protobuf type {@code req}
     */
    public static final class Builder extends
        com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
        // @@protoc_insertion_point(builder_implements:req)
        reqOrBuilder {
      private int bitField0_;
      private long appid_;
      private java.util.List<Long> taskIdList_ = java.util.Collections.emptyList();
      private long userId_;
      private long deviceId_;
      private long apiNo_;
      private Object jsonStr_ = "";

      // Construct using TestMap.req.newBuilder()
      private Builder() {
        maybeForceBuilderInitialization();
      }

      private Builder(
          BuilderParent parent) {
        super(parent);
        maybeForceBuilderInitialization();
      }

      public static final com.google.protobuf.Descriptors.Descriptor
      getDescriptor() {
        return TestMap.internal_static_req_descriptor;
      }

      @Override
      protected FieldAccessorTable
      internalGetFieldAccessorTable() {
        return TestMap.internal_static_req_fieldAccessorTable
            .ensureFieldAccessorsInitialized(
                req.class, Builder.class);
      }

      private void maybeForceBuilderInitialization() {
        if (com.google.protobuf.GeneratedMessageV3
            .alwaysUseFieldBuilders) {
        }
      }

      @Override
      public Builder clear() {
        super.clear();
        appid_ = 0L;

        taskIdList_ = java.util.Collections.emptyList();
        bitField0_ = (bitField0_ & ~0x00000002);
        userId_ = 0L;

        deviceId_ = 0L;

        apiNo_ = 0L;

        jsonStr_ = "";

        return this;
      }

      @Override
      public com.google.protobuf.Descriptors.Descriptor
      getDescriptorForType() {
        return TestMap.internal_static_req_descriptor;
      }

      @Override
      public req getDefaultInstanceForType() {
        return req.getDefaultInstance();
      }

      @Override
      public req build() {
        req result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }

      @Override
      public req buildPartial() {
        req result = new req(this);
        int from_bitField0_ = bitField0_;
        int to_bitField0_ = 0;
        result.appid_ = appid_;
        if (((bitField0_ & 0x00000002) == 0x00000002)) {
          taskIdList_ = java.util.Collections.unmodifiableList(taskIdList_);
          bitField0_ = (bitField0_ & ~0x00000002);
        }
        result.taskIdList_ = taskIdList_;
        result.userId_ = userId_;
        result.deviceId_ = deviceId_;
        result.apiNo_ = apiNo_;
        result.jsonStr_ = jsonStr_;
        result.bitField0_ = to_bitField0_;
        onBuilt();
        return result;
      }

      @Override
      public Builder clone() {
        return (Builder) super.clone();
      }

      @Override
      public Builder setField(
          com.google.protobuf.Descriptors.FieldDescriptor field,
          Object value) {
        return (Builder) super.setField(field, value);
      }

      @Override
      public Builder clearField(
          com.google.protobuf.Descriptors.FieldDescriptor field) {
        return (Builder) super.clearField(field);
      }

      @Override
      public Builder clearOneof(
          com.google.protobuf.Descriptors.OneofDescriptor oneof) {
        return (Builder) super.clearOneof(oneof);
      }

      @Override
      public Builder setRepeatedField(
          com.google.protobuf.Descriptors.FieldDescriptor field,
          int index, Object value) {
        return (Builder) super.setRepeatedField(field, index, value);
      }

      @Override
      public Builder addRepeatedField(
          com.google.protobuf.Descriptors.FieldDescriptor field,
          Object value) {
        return (Builder) super.addRepeatedField(field, value);
      }

      @Override
      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof req) {
          return mergeFrom((req) other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }

      public Builder mergeFrom(req other) {
        if (other == req.getDefaultInstance()) {
          return this;
        }
        if (other.getAppid() != 0L) {
          setAppid(other.getAppid());
        }
        if (!other.taskIdList_.isEmpty()) {
          if (taskIdList_.isEmpty()) {
            taskIdList_ = other.taskIdList_;
            bitField0_ = (bitField0_ & ~0x00000002);
          } else {
            ensureTaskIdListIsMutable();
            taskIdList_.addAll(other.taskIdList_);
          }
          onChanged();
        }
        if (other.getUserId() != 0L) {
          setUserId(other.getUserId());
        }
        if (other.getDeviceId() != 0L) {
          setDeviceId(other.getDeviceId());
        }
        if (other.getApiNo() != 0L) {
          setApiNo(other.getApiNo());
        }
        if (!other.getJsonStr().isEmpty()) {
          jsonStr_ = other.jsonStr_;
          onChanged();
        }
        this.mergeUnknownFields(other.unknownFields);
        onChanged();
        return this;
      }

      @Override
      public final boolean isInitialized() {
        return true;
      }

      @Override
      public Builder mergeFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws java.io.IOException {
        req parsedMessage = null;
        try {
          parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          parsedMessage = (req) e.getUnfinishedMessage();
          throw e.unwrapIOException();
        } finally {
          if (parsedMessage != null) {
            mergeFrom(parsedMessage);
          }
        }
        return this;
      }

      /**
       * <code>int64 appid = 1;</code>
       */
      public long getAppid() {
        return appid_;
      }

      /**
       * <code>int64 appid = 1;</code>
       */
      public Builder setAppid(long value) {

        appid_ = value;
        onChanged();
        return this;
      }

      /**
       * <code>int64 appid = 1;</code>
       */
      public Builder clearAppid() {

        appid_ = 0L;
        onChanged();
        return this;
      }

      private void ensureTaskIdListIsMutable() {
        if (!((bitField0_ & 0x00000002) == 0x00000002)) {
          taskIdList_ = new java.util.ArrayList<Long>(taskIdList_);
          bitField0_ |= 0x00000002;
        }
      }

      /**
       * <code>repeated int64 task_id_list = 2;</code>
       */
      public java.util.List<Long>
      getTaskIdListList() {
        return java.util.Collections.unmodifiableList(taskIdList_);
      }

      /**
       * <code>repeated int64 task_id_list = 2;</code>
       */
      public int getTaskIdListCount() {
        return taskIdList_.size();
      }

      /**
       * <code>repeated int64 task_id_list = 2;</code>
       */
      public long getTaskIdList(int index) {
        return taskIdList_.get(index);
      }

      /**
       * <code>repeated int64 task_id_list = 2;</code>
       */
      public Builder setTaskIdList(
          int index, long value) {
        ensureTaskIdListIsMutable();
        taskIdList_.set(index, value);
        onChanged();
        return this;
      }

      /**
       * <code>repeated int64 task_id_list = 2;</code>
       */
      public Builder addTaskIdList(long value) {
        ensureTaskIdListIsMutable();
        taskIdList_.add(value);
        onChanged();
        return this;
      }

      /**
       * <code>repeated int64 task_id_list = 2;</code>
       */
      public Builder addAllTaskIdList(
          Iterable<? extends Long> values) {
        ensureTaskIdListIsMutable();
        com.google.protobuf.AbstractMessageLite.Builder.addAll(
            values, taskIdList_);
        onChanged();
        return this;
      }

      /**
       * <code>repeated int64 task_id_list = 2;</code>
       */
      public Builder clearTaskIdList() {
        taskIdList_ = java.util.Collections.emptyList();
        bitField0_ = (bitField0_ & ~0x00000002);
        onChanged();
        return this;
      }

      /**
       * <code>int64 user_id = 3;</code>
       */
      public long getUserId() {
        return userId_;
      }

      /**
       * <code>int64 user_id = 3;</code>
       */
      public Builder setUserId(long value) {

        userId_ = value;
        onChanged();
        return this;
      }

      /**
       * <code>int64 user_id = 3;</code>
       */
      public Builder clearUserId() {

        userId_ = 0L;
        onChanged();
        return this;
      }

      /**
       * <code>int64 device_id = 4;</code>
       */
      public long getDeviceId() {
        return deviceId_;
      }

      /**
       * <code>int64 device_id = 4;</code>
       */
      public Builder setDeviceId(long value) {

        deviceId_ = value;
        onChanged();
        return this;
      }

      /**
       * <code>int64 device_id = 4;</code>
       */
      public Builder clearDeviceId() {

        deviceId_ = 0L;
        onChanged();
        return this;
      }

      /**
       * <pre>
       *
       * </pre>
       *
       * <code>int64 api_no = 5;</code>
       */
      public long getApiNo() {
        return apiNo_;
      }

      /**
       * <pre>
       *
       * </pre>
       *
       * <code>int64 api_no = 5;</code>
       */
      public Builder setApiNo(long value) {

        apiNo_ = value;
        onChanged();
        return this;
      }

      /**
       * <pre>
       *
       * </pre>
       *
       * <code>int64 api_no = 5;</code>
       */
      public Builder clearApiNo() {

        apiNo_ = 0L;
        onChanged();
        return this;
      }

      /**
       * <pre>
       * json
       * </pre>
       *
       * <code>string json_str = 6;</code>
       */
      public String getJsonStr() {
        Object ref = jsonStr_;
        if (!(ref instanceof String)) {
          com.google.protobuf.ByteString bs =
              (com.google.protobuf.ByteString) ref;
          String s = bs.toStringUtf8();
          jsonStr_ = s;
          return s;
        } else {
          return (String) ref;
        }
      }

      /**
       * <pre>
       * json
       * </pre>
       *
       * <code>string json_str = 6;</code>
       */
      public Builder setJsonStr(
          String value) {
        if (value == null) {
          throw new NullPointerException();
        }

        jsonStr_ = value;
        onChanged();
        return this;
      }

      /**
       * <pre>
       * json
       * </pre>
       *
       * <code>string json_str = 6;</code>
       */
      public com.google.protobuf.ByteString
      getJsonStrBytes() {
        Object ref = jsonStr_;
        if (ref instanceof String) {
          com.google.protobuf.ByteString b =
              com.google.protobuf.ByteString.copyFromUtf8(
                  (String) ref);
          jsonStr_ = b;
          return b;
        } else {
          return (com.google.protobuf.ByteString) ref;
        }
      }

      /**
       * <pre>
       * json
       * </pre>
       *
       * <code>string json_str = 6;</code>
       */
      public Builder setJsonStrBytes(
          com.google.protobuf.ByteString value) {
        if (value == null) {
          throw new NullPointerException();
        }
        checkByteStringIsUtf8(value);

        jsonStr_ = value;
        onChanged();
        return this;
      }

      /**
       * <pre>
       * json
       * </pre>
       *
       * <code>string json_str = 6;</code>
       */
      public Builder clearJsonStr() {

        jsonStr_ = getDefaultInstance().getJsonStr();
        onChanged();
        return this;
      }

      @Override
      public final Builder setUnknownFields(
          final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.setUnknownFieldsProto3(unknownFields);
      }

      @Override
      public final Builder mergeUnknownFields(
          final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.mergeUnknownFields(unknownFields);
      }


      // @@protoc_insertion_point(builder_scope:req)
    }

  }

  /**
   * Protobuf type {@code rsp}
   */
  public static final class rsp extends
      com.google.protobuf.GeneratedMessageV3 implements
      // @@protoc_insertion_point(message_implements:rsp)
      rspOrBuilder {
    public static final int STATUS_CODE_FIELD_NUMBER = 1;
    public static final int STATUS_MSG_FIELD_NUMBER = 2;
    public static final int JSON_STR_FIELD_NUMBER = 3;
    private static final long serialVersionUID = 0L;
    // @@protoc_insertion_point(class_scope:rsp)
    private static final rsp DEFAULT_INSTANCE;
    private static final com.google.protobuf.Parser<rsp>
        PARSER = new com.google.protobuf.AbstractParser<rsp>() {
      @Override
      public rsp parsePartialFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws com.google.protobuf.InvalidProtocolBufferException {
        return new rsp(input, extensionRegistry);
      }
    };

    static {
      DEFAULT_INSTANCE = new rsp();
    }

    private int statusCode_;
    private volatile Object statusMsg_;
    private volatile Object jsonStr_;
    private byte memoizedIsInitialized = -1;
    // Use rsp.newBuilder() to construct.
    private rsp(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
      super(builder);
    }

    private rsp() {
      statusCode_ = 0;
      statusMsg_ = "";
      jsonStr_ = "";
    }

    private rsp(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      this();
      if (extensionRegistry == null) {
        throw new NullPointerException();
      }
      int mutable_bitField0_ = 0;
      com.google.protobuf.UnknownFieldSet.Builder unknownFields =
          com.google.protobuf.UnknownFieldSet.newBuilder();
      try {
        boolean done = false;
        while (!done) {
          int tag = input.readTag();
          switch (tag) {
            case 0:
              done = true;
              break;
            case 8: {

              statusCode_ = input.readInt32();
              break;
            }
            case 18: {
              String s = input.readStringRequireUtf8();

              statusMsg_ = s;
              break;
            }
            case 26: {
              String s = input.readStringRequireUtf8();

              jsonStr_ = s;
              break;
            }
            default: {
              if (!parseUnknownFieldProto3(
                  input, unknownFields, extensionRegistry, tag)) {
                done = true;
              }
              break;
            }
          }
        }
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        throw e.setUnfinishedMessage(this);
      } catch (java.io.IOException e) {
        throw new com.google.protobuf.InvalidProtocolBufferException(
            e).setUnfinishedMessage(this);
      } finally {
        this.unknownFields = unknownFields.build();
        makeExtensionsImmutable();
      }
    }

    public static final com.google.protobuf.Descriptors.Descriptor
    getDescriptor() {
      return TestMap.internal_static_rsp_descriptor;
    }

    public static rsp parseFrom(
        java.nio.ByteBuffer data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static rsp parseFrom(
        java.nio.ByteBuffer data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static rsp parseFrom(
        com.google.protobuf.ByteString data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static rsp parseFrom(
        com.google.protobuf.ByteString data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static rsp parseFrom(byte[] data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static rsp parseFrom(
        byte[] data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static rsp parseFrom(java.io.InputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input);
    }

    public static rsp parseFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static rsp parseDelimitedFrom(java.io.InputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseDelimitedWithIOException(PARSER, input);
    }

    public static rsp parseDelimitedFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
    }

    public static rsp parseFrom(
        com.google.protobuf.CodedInputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input);
    }

    public static rsp parseFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static Builder newBuilder() {
      return DEFAULT_INSTANCE.toBuilder();
    }

    public static Builder newBuilder(rsp prototype) {
      return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }

    public static rsp getDefaultInstance() {
      return DEFAULT_INSTANCE;
    }

    public static com.google.protobuf.Parser<rsp> parser() {
      return PARSER;
    }

    @Override
    public final com.google.protobuf.UnknownFieldSet
    getUnknownFields() {
      return this.unknownFields;
    }

    @Override
    protected FieldAccessorTable
    internalGetFieldAccessorTable() {
      return TestMap.internal_static_rsp_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              rsp.class, Builder.class);
    }

    /**
     * <code>int32 status_code = 1;</code>
     */
    public int getStatusCode() {
      return statusCode_;
    }

    /**
     * <code>string status_msg = 2;</code>
     */
    public String getStatusMsg() {
      Object ref = statusMsg_;
      if (ref instanceof String) {
        return (String) ref;
      } else {
        com.google.protobuf.ByteString bs =
            (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        statusMsg_ = s;
        return s;
      }
    }

    /**
     * <code>string status_msg = 2;</code>
     */
    public com.google.protobuf.ByteString
    getStatusMsgBytes() {
      Object ref = statusMsg_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b =
            com.google.protobuf.ByteString.copyFromUtf8(
                (String) ref);
        statusMsg_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    /**
     * <pre>
     * json
     * </pre>
     *
     * <code>string json_str = 3;</code>
     */
    public String getJsonStr() {
      Object ref = jsonStr_;
      if (ref instanceof String) {
        return (String) ref;
      } else {
        com.google.protobuf.ByteString bs =
            (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        jsonStr_ = s;
        return s;
      }
    }

    /**
     * <pre>
     * json
     * </pre>
     *
     * <code>string json_str = 3;</code>
     */
    public com.google.protobuf.ByteString
    getJsonStrBytes() {
      Object ref = jsonStr_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b =
            com.google.protobuf.ByteString.copyFromUtf8(
                (String) ref);
        jsonStr_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    @Override
    public final boolean isInitialized() {
      byte isInitialized = memoizedIsInitialized;
      if (isInitialized == 1) {
        return true;
      }
      if (isInitialized == 0) {
        return false;
      }

      memoizedIsInitialized = 1;
      return true;
    }

    @Override
    public void writeTo(com.google.protobuf.CodedOutputStream output)
        throws java.io.IOException {
      if (statusCode_ != 0) {
        output.writeInt32(1, statusCode_);
      }
      if (!getStatusMsgBytes().isEmpty()) {
        com.google.protobuf.GeneratedMessageV3.writeString(output, 2, statusMsg_);
      }
      if (!getJsonStrBytes().isEmpty()) {
        com.google.protobuf.GeneratedMessageV3.writeString(output, 3, jsonStr_);
      }
      unknownFields.writeTo(output);
    }

    @Override
    public int getSerializedSize() {
      int size = memoizedSize;
      if (size != -1) {
        return size;
      }

      size = 0;
      if (statusCode_ != 0) {
        size += com.google.protobuf.CodedOutputStream
            .computeInt32Size(1, statusCode_);
      }
      if (!getStatusMsgBytes().isEmpty()) {
        size += com.google.protobuf.GeneratedMessageV3.computeStringSize(2, statusMsg_);
      }
      if (!getJsonStrBytes().isEmpty()) {
        size += com.google.protobuf.GeneratedMessageV3.computeStringSize(3, jsonStr_);
      }
      size += unknownFields.getSerializedSize();
      memoizedSize = size;
      return size;
    }

    @Override
    public boolean equals(final Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof rsp)) {
        return super.equals(obj);
      }
      rsp other = (rsp) obj;

      boolean result = true;
      result = result && (getStatusCode()
          == other.getStatusCode());
      result = result && getStatusMsg()
          .equals(other.getStatusMsg());
      result = result && getJsonStr()
          .equals(other.getJsonStr());
      result = result && unknownFields.equals(other.unknownFields);
      return result;
    }

    @Override
    public int hashCode() {
      if (memoizedHashCode != 0) {
        return memoizedHashCode;
      }
      int hash = 41;
      hash = (19 * hash) + getDescriptor().hashCode();
      hash = (37 * hash) + STATUS_CODE_FIELD_NUMBER;
      hash = (53 * hash) + getStatusCode();
      hash = (37 * hash) + STATUS_MSG_FIELD_NUMBER;
      hash = (53 * hash) + getStatusMsg().hashCode();
      hash = (37 * hash) + JSON_STR_FIELD_NUMBER;
      hash = (53 * hash) + getJsonStr().hashCode();
      hash = (29 * hash) + unknownFields.hashCode();
      memoizedHashCode = hash;
      return hash;
    }

    @Override
    public Builder newBuilderForType() {
      return newBuilder();
    }

    @Override
    public Builder toBuilder() {
      return this == DEFAULT_INSTANCE
          ? new Builder() : new Builder().mergeFrom(this);
    }

    @Override
    protected Builder newBuilderForType(
        BuilderParent parent) {
      Builder builder = new Builder(parent);
      return builder;
    }

    @Override
    public com.google.protobuf.Parser<rsp> getParserForType() {
      return PARSER;
    }

    @Override
    public rsp getDefaultInstanceForType() {
      return DEFAULT_INSTANCE;
    }

    /**
     * Protobuf type {@code rsp}
     */
    public static final class Builder extends
        com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
        // @@protoc_insertion_point(builder_implements:rsp)
        rspOrBuilder {
      private int statusCode_;
      private Object statusMsg_ = "";
      private Object jsonStr_ = "";

      // Construct using TestMap.rsp.newBuilder()
      private Builder() {
        maybeForceBuilderInitialization();
      }

      private Builder(
          BuilderParent parent) {
        super(parent);
        maybeForceBuilderInitialization();
      }

      public static final com.google.protobuf.Descriptors.Descriptor
      getDescriptor() {
        return TestMap.internal_static_rsp_descriptor;
      }

      @Override
      protected FieldAccessorTable
      internalGetFieldAccessorTable() {
        return TestMap.internal_static_rsp_fieldAccessorTable
            .ensureFieldAccessorsInitialized(
                rsp.class, Builder.class);
      }

      private void maybeForceBuilderInitialization() {
        if (com.google.protobuf.GeneratedMessageV3
            .alwaysUseFieldBuilders) {
        }
      }

      @Override
      public Builder clear() {
        super.clear();
        statusCode_ = 0;

        statusMsg_ = "";

        jsonStr_ = "";

        return this;
      }

      @Override
      public com.google.protobuf.Descriptors.Descriptor
      getDescriptorForType() {
        return TestMap.internal_static_rsp_descriptor;
      }

      @Override
      public rsp getDefaultInstanceForType() {
        return rsp.getDefaultInstance();
      }

      @Override
      public rsp build() {
        rsp result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }

      @Override
      public rsp buildPartial() {
        rsp result = new rsp(this);
        result.statusCode_ = statusCode_;
        result.statusMsg_ = statusMsg_;
        result.jsonStr_ = jsonStr_;
        onBuilt();
        return result;
      }

      @Override
      public Builder clone() {
        return (Builder) super.clone();
      }

      @Override
      public Builder setField(
          com.google.protobuf.Descriptors.FieldDescriptor field,
          Object value) {
        return (Builder) super.setField(field, value);
      }

      @Override
      public Builder clearField(
          com.google.protobuf.Descriptors.FieldDescriptor field) {
        return (Builder) super.clearField(field);
      }

      @Override
      public Builder clearOneof(
          com.google.protobuf.Descriptors.OneofDescriptor oneof) {
        return (Builder) super.clearOneof(oneof);
      }

      @Override
      public Builder setRepeatedField(
          com.google.protobuf.Descriptors.FieldDescriptor field,
          int index, Object value) {
        return (Builder) super.setRepeatedField(field, index, value);
      }

      @Override
      public Builder addRepeatedField(
          com.google.protobuf.Descriptors.FieldDescriptor field,
          Object value) {
        return (Builder) super.addRepeatedField(field, value);
      }

      @Override
      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof rsp) {
          return mergeFrom((rsp) other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }

      public Builder mergeFrom(rsp other) {
        if (other == rsp.getDefaultInstance()) {
          return this;
        }
        if (other.getStatusCode() != 0) {
          setStatusCode(other.getStatusCode());
        }
        if (!other.getStatusMsg().isEmpty()) {
          statusMsg_ = other.statusMsg_;
          onChanged();
        }
        if (!other.getJsonStr().isEmpty()) {
          jsonStr_ = other.jsonStr_;
          onChanged();
        }
        this.mergeUnknownFields(other.unknownFields);
        onChanged();
        return this;
      }

      @Override
      public final boolean isInitialized() {
        return true;
      }

      @Override
      public Builder mergeFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws java.io.IOException {
        rsp parsedMessage = null;
        try {
          parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          parsedMessage = (rsp) e.getUnfinishedMessage();
          throw e.unwrapIOException();
        } finally {
          if (parsedMessage != null) {
            mergeFrom(parsedMessage);
          }
        }
        return this;
      }

      /**
       * <code>int32 status_code = 1;</code>
       */
      public int getStatusCode() {
        return statusCode_;
      }

      /**
       * <code>int32 status_code = 1;</code>
       */
      public Builder setStatusCode(int value) {

        statusCode_ = value;
        onChanged();
        return this;
      }

      /**
       * <code>int32 status_code = 1;</code>
       */
      public Builder clearStatusCode() {

        statusCode_ = 0;
        onChanged();
        return this;
      }

      /**
       * <code>string status_msg = 2;</code>
       */
      public String getStatusMsg() {
        Object ref = statusMsg_;
        if (!(ref instanceof String)) {
          com.google.protobuf.ByteString bs =
              (com.google.protobuf.ByteString) ref;
          String s = bs.toStringUtf8();
          statusMsg_ = s;
          return s;
        } else {
          return (String) ref;
        }
      }

      /**
       * <code>string status_msg = 2;</code>
       */
      public Builder setStatusMsg(
          String value) {
        if (value == null) {
          throw new NullPointerException();
        }

        statusMsg_ = value;
        onChanged();
        return this;
      }

      /**
       * <code>string status_msg = 2;</code>
       */
      public com.google.protobuf.ByteString
      getStatusMsgBytes() {
        Object ref = statusMsg_;
        if (ref instanceof String) {
          com.google.protobuf.ByteString b =
              com.google.protobuf.ByteString.copyFromUtf8(
                  (String) ref);
          statusMsg_ = b;
          return b;
        } else {
          return (com.google.protobuf.ByteString) ref;
        }
      }

      /**
       * <code>string status_msg = 2;</code>
       */
      public Builder setStatusMsgBytes(
          com.google.protobuf.ByteString value) {
        if (value == null) {
          throw new NullPointerException();
        }
        checkByteStringIsUtf8(value);

        statusMsg_ = value;
        onChanged();
        return this;
      }

      /**
       * <code>string status_msg = 2;</code>
       */
      public Builder clearStatusMsg() {

        statusMsg_ = getDefaultInstance().getStatusMsg();
        onChanged();
        return this;
      }

      /**
       * <pre>
       * json
       * </pre>
       *
       * <code>string json_str = 3;</code>
       */
      public String getJsonStr() {
        Object ref = jsonStr_;
        if (!(ref instanceof String)) {
          com.google.protobuf.ByteString bs =
              (com.google.protobuf.ByteString) ref;
          String s = bs.toStringUtf8();
          jsonStr_ = s;
          return s;
        } else {
          return (String) ref;
        }
      }

      /**
       * <pre>
       * json
       * </pre>
       *
       * <code>string json_str = 3;</code>
       */
      public Builder setJsonStr(
          String value) {
        if (value == null) {
          throw new NullPointerException();
        }

        jsonStr_ = value;
        onChanged();
        return this;
      }

      /**
       * <pre>
       * json
       * </pre>
       *
       * <code>string json_str = 3;</code>
       */
      public com.google.protobuf.ByteString
      getJsonStrBytes() {
        Object ref = jsonStr_;
        if (ref instanceof String) {
          com.google.protobuf.ByteString b =
              com.google.protobuf.ByteString.copyFromUtf8(
                  (String) ref);
          jsonStr_ = b;
          return b;
        } else {
          return (com.google.protobuf.ByteString) ref;
        }
      }

      /**
       * <pre>
       * json
       * </pre>
       *
       * <code>string json_str = 3;</code>
       */
      public Builder setJsonStrBytes(
          com.google.protobuf.ByteString value) {
        if (value == null) {
          throw new NullPointerException();
        }
        checkByteStringIsUtf8(value);

        jsonStr_ = value;
        onChanged();
        return this;
      }

      /**
       * <pre>
       * json
       * </pre>
       *
       * <code>string json_str = 3;</code>
       */
      public Builder clearJsonStr() {

        jsonStr_ = getDefaultInstance().getJsonStr();
        onChanged();
        return this;
      }

      @Override
      public final Builder setUnknownFields(
          final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.setUnknownFieldsProto3(unknownFields);
      }

      @Override
      public final Builder mergeUnknownFields(
          final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.mergeUnknownFields(unknownFields);
      }


      // @@protoc_insertion_point(builder_scope:rsp)
    }

  }

  // @@protoc_insertion_point(outer_class_scope)
}
