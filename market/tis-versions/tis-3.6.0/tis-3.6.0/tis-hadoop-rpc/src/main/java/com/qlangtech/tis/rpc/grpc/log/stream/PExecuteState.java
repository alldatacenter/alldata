/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.tis.rpc.grpc.log.stream;

/**
 * Protobuf type {@code stream.PExecuteState}
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/09/25
 */
public final class PExecuteState extends com.google.protobuf.GeneratedMessageV3 implements // @@protoc_insertion_point(message_implements:stream.PExecuteState)
PExecuteStateOrBuilder {

    private static final long serialVersionUID = 0L;

    // Use PExecuteState.newBuilder() to construct.
    private PExecuteState(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
        super(builder);
    }

    private PExecuteState() {
        infoType_ = 0;
        logType_ = 0;
        msg_ = "";
        from_ = "";
        serviceName_ = "";
        execState_ = "";
        component_ = "";
    }

    @java.lang.Override
    public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
        return this.unknownFields;
    }

    private PExecuteState(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
        this();
        if (extensionRegistry == null) {
            throw new java.lang.NullPointerException();
        }
        int mutable_bitField0_ = 0;
        com.google.protobuf.UnknownFieldSet.Builder unknownFields = com.google.protobuf.UnknownFieldSet.newBuilder();
        try {
            boolean done = false;
            while (!done) {
                int tag = input.readTag();
                switch(tag) {
                    case 0:
                        done = true;
                        break;
                    case 8:
                        {
                            int rawValue = input.readEnum();
                            infoType_ = rawValue;
                            break;
                        }
                    case 16:
                        {
                            int rawValue = input.readEnum();
                            logType_ = rawValue;
                            break;
                        }
                    case 26:
                        {
                            java.lang.String s = input.readStringRequireUtf8();
                            msg_ = s;
                            break;
                        }
                    case 34:
                        {
                            java.lang.String s = input.readStringRequireUtf8();
                            from_ = s;
                            break;
                        }
                    case 40:
                        {
                            jobId_ = input.readUInt64();
                            break;
                        }
                    case 48:
                        {
                            taskId_ = input.readUInt64();
                            break;
                        }
                    case 58:
                        {
                            java.lang.String s = input.readStringRequireUtf8();
                            serviceName_ = s;
                            break;
                        }
                    case 66:
                        {
                            java.lang.String s = input.readStringRequireUtf8();
                            execState_ = s;
                            break;
                        }
                    case 72:
                        {
                            time_ = input.readUInt64();
                            break;
                        }
                    case 82:
                        {
                            java.lang.String s = input.readStringRequireUtf8();
                            component_ = s;
                            break;
                        }
                    default:
                        {
                            if (!parseUnknownField(input, unknownFields, extensionRegistry, tag)) {
                                done = true;
                            }
                            break;
                        }
                }
            }
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
            throw e.setUnfinishedMessage(this);
        } catch (java.io.IOException e) {
            throw new com.google.protobuf.InvalidProtocolBufferException(e).setUnfinishedMessage(this);
        } finally {
            this.unknownFields = unknownFields.build();
            makeExtensionsImmutable();
        }
    }

    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
        return com.qlangtech.tis.rpc.grpc.log.stream.LogCollectorProto.internal_static_stream_PExecuteState_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
        return com.qlangtech.tis.rpc.grpc.log.stream.LogCollectorProto.internal_static_stream_PExecuteState_fieldAccessorTable.ensureFieldAccessorsInitialized(com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState.class, com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState.Builder.class);
    }

    /**
     * Protobuf enum {@code stream.PExecuteState.LogType}
     */
    public enum LogType implements com.google.protobuf.ProtocolMessageEnum {

        /**
         * <pre>
         * 部署状态变化
         * </pre>
         *
         * <code>INCR_DEPLOY_STATUS_CHANGE = 0;</code>
         */
        INCR_DEPLOY_STATUS_CHANGE(0),
        /**
         * <pre>
         * 近一段时间内增量监听的topic下各个tag的多少值
         * </pre>
         *
         * <code>MQ_TAGS_STATUS = 1;</code>
         */
        MQ_TAGS_STATUS(1),
        /**
         * <pre>
         * 全量构建
         * </pre>
         *
         * <code>FULL = 2;</code>
         */
        FULL(2),
        /**
         * <pre>
         * 增量构建
         * </pre>
         *
         * <code>INCR = 3;</code>
         */
        INCR(3),
        /**
         * <pre>
         * 增量记录详细发送
         * </pre>
         *
         * <code>INCR_SEND = 4;</code>
         */
        INCR_SEND(4),
        UNRECOGNIZED(-1);

        /**
         * <pre>
         * 部署状态变化
         * </pre>
         *
         * <code>INCR_DEPLOY_STATUS_CHANGE = 0;</code>
         */
        public static final int INCR_DEPLOY_STATUS_CHANGE_VALUE = 0;

        /**
         * <pre>
         * 近一段时间内增量监听的topic下各个tag的多少值
         * </pre>
         *
         * <code>MQ_TAGS_STATUS = 1;</code>
         */
        public static final int MQ_TAGS_STATUS_VALUE = 1;

        /**
         * <pre>
         * 全量构建
         * </pre>
         *
         * <code>FULL = 2;</code>
         */
        public static final int FULL_VALUE = 2;

        /**
         * <pre>
         * 增量构建
         * </pre>
         *
         * <code>INCR = 3;</code>
         */
        public static final int INCR_VALUE = 3;

        /**
         * <pre>
         * 增量记录详细发送
         * </pre>
         *
         * <code>INCR_SEND = 4;</code>
         */
        public static final int INCR_SEND_VALUE = 4;

        public final int getNumber() {
            if (this == UNRECOGNIZED) {
                throw new java.lang.IllegalArgumentException("Can't get the number of an unknown enum value.");
            }
            return value;
        }

        /**
         * @deprecated Use {@link #forNumber(int)} instead.
         */
        @java.lang.Deprecated
        public static LogType valueOf(int value) {
            return forNumber(value);
        }

        public static LogType forNumber(int value) {
            switch(value) {
                case 0:
                    return INCR_DEPLOY_STATUS_CHANGE;
                case 1:
                    return MQ_TAGS_STATUS;
                case 2:
                    return FULL;
                case 3:
                    return INCR;
                case 4:
                    return INCR_SEND;
                default:
                    return null;
            }
        }

        public static com.google.protobuf.Internal.EnumLiteMap<LogType> internalGetValueMap() {
            return internalValueMap;
        }

        private static final com.google.protobuf.Internal.EnumLiteMap<LogType> internalValueMap = new com.google.protobuf.Internal.EnumLiteMap<LogType>() {

            public LogType findValueByNumber(int number) {
                return LogType.forNumber(number);
            }
        };

        public final com.google.protobuf.Descriptors.EnumValueDescriptor getValueDescriptor() {
            return getDescriptor().getValues().get(ordinal());
        }

        public final com.google.protobuf.Descriptors.EnumDescriptor getDescriptorForType() {
            return getDescriptor();
        }

        public static final com.google.protobuf.Descriptors.EnumDescriptor getDescriptor() {
            return com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState.getDescriptor().getEnumTypes().get(0);
        }

        private static final LogType[] VALUES = values();

        public static LogType valueOf(com.google.protobuf.Descriptors.EnumValueDescriptor desc) {
            if (desc.getType() != getDescriptor()) {
                throw new java.lang.IllegalArgumentException("EnumValueDescriptor is not for this type.");
            }
            if (desc.getIndex() == -1) {
                return UNRECOGNIZED;
            }
            return VALUES[desc.getIndex()];
        }

        private final int value;

        private LogType(int value) {
            this.value = value;
        }
    }

    /**
     * Protobuf enum {@code stream.PExecuteState.InfoType}
     */
    public enum InfoType implements com.google.protobuf.ProtocolMessageEnum {

        /**
         * <code>INFO = 0;</code>
         */
        INFO(0),
        /**
         * <code>WARN = 1;</code>
         */
        WARN(1),
        /**
         * <code>ERROR = 2;</code>
         */
        ERROR(2),
        /**
         * <code>FATAL = 3;</code>
         */
        FATAL(3),
        UNRECOGNIZED(-1);

        /**
         * <code>INFO = 0;</code>
         */
        public static final int INFO_VALUE = 0;

        /**
         * <code>WARN = 1;</code>
         */
        public static final int WARN_VALUE = 1;

        /**
         * <code>ERROR = 2;</code>
         */
        public static final int ERROR_VALUE = 2;

        /**
         * <code>FATAL = 3;</code>
         */
        public static final int FATAL_VALUE = 3;

        public final int getNumber() {
            if (this == UNRECOGNIZED) {
                throw new java.lang.IllegalArgumentException("Can't get the number of an unknown enum value.");
            }
            return value;
        }

        /**
         * @deprecated Use {@link #forNumber(int)} instead.
         */
        @java.lang.Deprecated
        public static InfoType valueOf(int value) {
            return forNumber(value);
        }

        public static InfoType forNumber(int value) {
            switch(value) {
                case 0:
                    return INFO;
                case 1:
                    return WARN;
                case 2:
                    return ERROR;
                case 3:
                    return FATAL;
                default:
                    return null;
            }
        }

        public static com.google.protobuf.Internal.EnumLiteMap<InfoType> internalGetValueMap() {
            return internalValueMap;
        }

        private static final com.google.protobuf.Internal.EnumLiteMap<InfoType> internalValueMap = new com.google.protobuf.Internal.EnumLiteMap<InfoType>() {

            public InfoType findValueByNumber(int number) {
                return InfoType.forNumber(number);
            }
        };

        public final com.google.protobuf.Descriptors.EnumValueDescriptor getValueDescriptor() {
            return getDescriptor().getValues().get(ordinal());
        }

        public final com.google.protobuf.Descriptors.EnumDescriptor getDescriptorForType() {
            return getDescriptor();
        }

        public static final com.google.protobuf.Descriptors.EnumDescriptor getDescriptor() {
            return com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState.getDescriptor().getEnumTypes().get(1);
        }

        private static final InfoType[] VALUES = values();

        public static InfoType valueOf(com.google.protobuf.Descriptors.EnumValueDescriptor desc) {
            if (desc.getType() != getDescriptor()) {
                throw new java.lang.IllegalArgumentException("EnumValueDescriptor is not for this type.");
            }
            if (desc.getIndex() == -1) {
                return UNRECOGNIZED;
            }
            return VALUES[desc.getIndex()];
        }

        private final int value;

        private InfoType(int value) {
            this.value = value;
        }
    }

    public static final int INFOTYPE_FIELD_NUMBER = 1;

    private int infoType_;

    /**
     * <code>.stream.PExecuteState.InfoType infoType = 1;</code>
     */
    public int getInfoTypeValue() {
        return infoType_;
    }

    /**
     * <code>.stream.PExecuteState.InfoType infoType = 1;</code>
     */
    public com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState.InfoType getInfoType() {
        @SuppressWarnings("deprecation")
        com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState.InfoType result = com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState.InfoType.valueOf(infoType_);
        return result == null ? com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState.InfoType.UNRECOGNIZED : result;
    }

    public static final int LOGTYPE_FIELD_NUMBER = 2;

    private int logType_;

    /**
     * <code>.stream.PExecuteState.LogType logType = 2;</code>
     */
    public int getLogTypeValue() {
        return logType_;
    }

    /**
     * <code>.stream.PExecuteState.LogType logType = 2;</code>
     */
    public com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState.LogType getLogType() {
        @SuppressWarnings("deprecation")
        com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState.LogType result = com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState.LogType.valueOf(logType_);
        return result == null ? com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState.LogType.UNRECOGNIZED : result;
    }

    public static final int MSG_FIELD_NUMBER = 3;

    private volatile java.lang.Object msg_;

    /**
     * <code>string msg = 3;</code>
     */
    public java.lang.String getMsg() {
        java.lang.Object ref = msg_;
        if (ref instanceof java.lang.String) {
            return (java.lang.String) ref;
        } else {
            com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
            java.lang.String s = bs.toStringUtf8();
            msg_ = s;
            return s;
        }
    }

    /**
     * <code>string msg = 3;</code>
     */
    public com.google.protobuf.ByteString getMsgBytes() {
        java.lang.Object ref = msg_;
        if (ref instanceof java.lang.String) {
            com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((java.lang.String) ref);
            msg_ = b;
            return b;
        } else {
            return (com.google.protobuf.ByteString) ref;
        }
    }

    public static final int FROM_FIELD_NUMBER = 4;

    private volatile java.lang.Object from_;

    /**
     * <code>string from = 4;</code>
     */
    public java.lang.String getFrom() {
        java.lang.Object ref = from_;
        if (ref instanceof java.lang.String) {
            return (java.lang.String) ref;
        } else {
            com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
            java.lang.String s = bs.toStringUtf8();
            from_ = s;
            return s;
        }
    }

    /**
     * <code>string from = 4;</code>
     */
    public com.google.protobuf.ByteString getFromBytes() {
        java.lang.Object ref = from_;
        if (ref instanceof java.lang.String) {
            com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((java.lang.String) ref);
            from_ = b;
            return b;
        } else {
            return (com.google.protobuf.ByteString) ref;
        }
    }

    public static final int JOBID_FIELD_NUMBER = 5;

    private long jobId_;

    /**
     * <code>uint64 jobId = 5;</code>
     */
    public long getJobId() {
        return jobId_;
    }

    public static final int TASKID_FIELD_NUMBER = 6;

    private long taskId_;

    /**
     * <code>uint64 taskId = 6;</code>
     */
    public long getTaskId() {
        return taskId_;
    }

    public static final int SERVICENAME_FIELD_NUMBER = 7;

    private volatile java.lang.Object serviceName_;

    /**
     * <code>string serviceName = 7;</code>
     */
    public java.lang.String getServiceName() {
        java.lang.Object ref = serviceName_;
        if (ref instanceof java.lang.String) {
            return (java.lang.String) ref;
        } else {
            com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
            java.lang.String s = bs.toStringUtf8();
            serviceName_ = s;
            return s;
        }
    }

    /**
     * <code>string serviceName = 7;</code>
     */
    public com.google.protobuf.ByteString getServiceNameBytes() {
        java.lang.Object ref = serviceName_;
        if (ref instanceof java.lang.String) {
            com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((java.lang.String) ref);
            serviceName_ = b;
            return b;
        } else {
            return (com.google.protobuf.ByteString) ref;
        }
    }

    public static final int EXECSTATE_FIELD_NUMBER = 8;

    private volatile java.lang.Object execState_;

    /**
     * <code>string execState = 8;</code>
     */
    public java.lang.String getExecState() {
        java.lang.Object ref = execState_;
        if (ref instanceof java.lang.String) {
            return (java.lang.String) ref;
        } else {
            com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
            java.lang.String s = bs.toStringUtf8();
            execState_ = s;
            return s;
        }
    }

    /**
     * <code>string execState = 8;</code>
     */
    public com.google.protobuf.ByteString getExecStateBytes() {
        java.lang.Object ref = execState_;
        if (ref instanceof java.lang.String) {
            com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((java.lang.String) ref);
            execState_ = b;
            return b;
        } else {
            return (com.google.protobuf.ByteString) ref;
        }
    }

    public static final int TIME_FIELD_NUMBER = 9;

    private long time_;

    /**
     * <code>uint64 time = 9;</code>
     */
    public long getTime() {
        return time_;
    }

    public static final int COMPONENT_FIELD_NUMBER = 10;

    private volatile java.lang.Object component_;

    /**
     * <code>string component = 10;</code>
     */
    public java.lang.String getComponent() {
        java.lang.Object ref = component_;
        if (ref instanceof java.lang.String) {
            return (java.lang.String) ref;
        } else {
            com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
            java.lang.String s = bs.toStringUtf8();
            component_ = s;
            return s;
        }
    }

    /**
     * <code>string component = 10;</code>
     */
    public com.google.protobuf.ByteString getComponentBytes() {
        java.lang.Object ref = component_;
        if (ref instanceof java.lang.String) {
            com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((java.lang.String) ref);
            component_ = b;
            return b;
        } else {
            return (com.google.protobuf.ByteString) ref;
        }
    }

    private byte memoizedIsInitialized = -1;

    @java.lang.Override
    public final boolean isInitialized() {
        byte isInitialized = memoizedIsInitialized;
        if (isInitialized == 1)
            return true;
        if (isInitialized == 0)
            return false;
        memoizedIsInitialized = 1;
        return true;
    }

    @java.lang.Override
    public void writeTo(com.google.protobuf.CodedOutputStream output) throws java.io.IOException {
        if (infoType_ != com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState.InfoType.INFO.getNumber()) {
            output.writeEnum(1, infoType_);
        }
        if (logType_ != com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState.LogType.INCR_DEPLOY_STATUS_CHANGE.getNumber()) {
            output.writeEnum(2, logType_);
        }
        if (!getMsgBytes().isEmpty()) {
            com.google.protobuf.GeneratedMessageV3.writeString(output, 3, msg_);
        }
        if (!getFromBytes().isEmpty()) {
            com.google.protobuf.GeneratedMessageV3.writeString(output, 4, from_);
        }
        if (jobId_ != 0L) {
            output.writeUInt64(5, jobId_);
        }
        if (taskId_ != 0L) {
            output.writeUInt64(6, taskId_);
        }
        if (!getServiceNameBytes().isEmpty()) {
            com.google.protobuf.GeneratedMessageV3.writeString(output, 7, serviceName_);
        }
        if (!getExecStateBytes().isEmpty()) {
            com.google.protobuf.GeneratedMessageV3.writeString(output, 8, execState_);
        }
        if (time_ != 0L) {
            output.writeUInt64(9, time_);
        }
        if (!getComponentBytes().isEmpty()) {
            com.google.protobuf.GeneratedMessageV3.writeString(output, 10, component_);
        }
        unknownFields.writeTo(output);
    }

    @java.lang.Override
    public int getSerializedSize() {
        int size = memoizedSize;
        if (size != -1)
            return size;
        size = 0;
        if (infoType_ != com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState.InfoType.INFO.getNumber()) {
            size += com.google.protobuf.CodedOutputStream.computeEnumSize(1, infoType_);
        }
        if (logType_ != com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState.LogType.INCR_DEPLOY_STATUS_CHANGE.getNumber()) {
            size += com.google.protobuf.CodedOutputStream.computeEnumSize(2, logType_);
        }
        if (!getMsgBytes().isEmpty()) {
            size += com.google.protobuf.GeneratedMessageV3.computeStringSize(3, msg_);
        }
        if (!getFromBytes().isEmpty()) {
            size += com.google.protobuf.GeneratedMessageV3.computeStringSize(4, from_);
        }
        if (jobId_ != 0L) {
            size += com.google.protobuf.CodedOutputStream.computeUInt64Size(5, jobId_);
        }
        if (taskId_ != 0L) {
            size += com.google.protobuf.CodedOutputStream.computeUInt64Size(6, taskId_);
        }
        if (!getServiceNameBytes().isEmpty()) {
            size += com.google.protobuf.GeneratedMessageV3.computeStringSize(7, serviceName_);
        }
        if (!getExecStateBytes().isEmpty()) {
            size += com.google.protobuf.GeneratedMessageV3.computeStringSize(8, execState_);
        }
        if (time_ != 0L) {
            size += com.google.protobuf.CodedOutputStream.computeUInt64Size(9, time_);
        }
        if (!getComponentBytes().isEmpty()) {
            size += com.google.protobuf.GeneratedMessageV3.computeStringSize(10, component_);
        }
        size += unknownFields.getSerializedSize();
        memoizedSize = size;
        return size;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState)) {
            return super.equals(obj);
        }
        com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState other = (com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState) obj;
        if (infoType_ != other.infoType_)
            return false;
        if (logType_ != other.logType_)
            return false;
        if (!getMsg().equals(other.getMsg()))
            return false;
        if (!getFrom().equals(other.getFrom()))
            return false;
        if (getJobId() != other.getJobId())
            return false;
        if (getTaskId() != other.getTaskId())
            return false;
        if (!getServiceName().equals(other.getServiceName()))
            return false;
        if (!getExecState().equals(other.getExecState()))
            return false;
        if (getTime() != other.getTime())
            return false;
        if (!getComponent().equals(other.getComponent()))
            return false;
        if (!unknownFields.equals(other.unknownFields))
            return false;
        return true;
    }

    @java.lang.Override
    public int hashCode() {
        if (memoizedHashCode != 0) {
            return memoizedHashCode;
        }
        int hash = 41;
        hash = (19 * hash) + getDescriptor().hashCode();
        hash = (37 * hash) + INFOTYPE_FIELD_NUMBER;
        hash = (53 * hash) + infoType_;
        hash = (37 * hash) + LOGTYPE_FIELD_NUMBER;
        hash = (53 * hash) + logType_;
        hash = (37 * hash) + MSG_FIELD_NUMBER;
        hash = (53 * hash) + getMsg().hashCode();
        hash = (37 * hash) + FROM_FIELD_NUMBER;
        hash = (53 * hash) + getFrom().hashCode();
        hash = (37 * hash) + JOBID_FIELD_NUMBER;
        hash = (53 * hash) + com.google.protobuf.Internal.hashLong(getJobId());
        hash = (37 * hash) + TASKID_FIELD_NUMBER;
        hash = (53 * hash) + com.google.protobuf.Internal.hashLong(getTaskId());
        hash = (37 * hash) + SERVICENAME_FIELD_NUMBER;
        hash = (53 * hash) + getServiceName().hashCode();
        hash = (37 * hash) + EXECSTATE_FIELD_NUMBER;
        hash = (53 * hash) + getExecState().hashCode();
        hash = (37 * hash) + TIME_FIELD_NUMBER;
        hash = (53 * hash) + com.google.protobuf.Internal.hashLong(getTime());
        hash = (37 * hash) + COMPONENT_FIELD_NUMBER;
        hash = (53 * hash) + getComponent().hashCode();
        hash = (29 * hash) + unknownFields.hashCode();
        memoizedHashCode = hash;
        return hash;
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState parseFrom(java.nio.ByteBuffer data) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState parseFrom(java.nio.ByteBuffer data, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState parseFrom(com.google.protobuf.ByteString data) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState parseFrom(com.google.protobuf.ByteString data, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState parseFrom(byte[] data) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState parseFrom(byte[] data, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState parseFrom(java.io.InputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState parseFrom(java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState parseDelimitedFrom(java.io.InputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState parseDelimitedFrom(java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input, extensionRegistry);
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState parseFrom(com.google.protobuf.CodedInputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState parseFrom(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    @java.lang.Override
    public Builder newBuilderForType() {
        return newBuilder();
    }

    public static Builder newBuilder() {
        return DEFAULT_INSTANCE.toBuilder();
    }

    public static Builder newBuilder(com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState prototype) {
        return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }

    @java.lang.Override
    public Builder toBuilder() {
        return this == DEFAULT_INSTANCE ? new Builder() : new Builder().mergeFrom(this);
    }

    @java.lang.Override
    protected Builder newBuilderForType(com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
        Builder builder = new Builder(parent);
        return builder;
    }

    /**
     * Protobuf type {@code stream.PExecuteState}
     */
    public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements // @@protoc_insertion_point(builder_implements:stream.PExecuteState)
    com.qlangtech.tis.rpc.grpc.log.stream.PExecuteStateOrBuilder {

        public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
            return com.qlangtech.tis.rpc.grpc.log.stream.LogCollectorProto.internal_static_stream_PExecuteState_descriptor;
        }

        @java.lang.Override
        protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
            return com.qlangtech.tis.rpc.grpc.log.stream.LogCollectorProto.internal_static_stream_PExecuteState_fieldAccessorTable.ensureFieldAccessorsInitialized(com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState.class, com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState.Builder.class);
        }

        // Construct using com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState.newBuilder()
        private Builder() {
            maybeForceBuilderInitialization();
        }

        private Builder(com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
            super(parent);
            maybeForceBuilderInitialization();
        }

        private void maybeForceBuilderInitialization() {
            if (com.google.protobuf.GeneratedMessageV3.alwaysUseFieldBuilders) {
            }
        }

        @java.lang.Override
        public Builder clear() {
            super.clear();
            infoType_ = 0;
            logType_ = 0;
            msg_ = "";
            from_ = "";
            jobId_ = 0L;
            taskId_ = 0L;
            serviceName_ = "";
            execState_ = "";
            time_ = 0L;
            component_ = "";
            return this;
        }

        @java.lang.Override
        public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
            return com.qlangtech.tis.rpc.grpc.log.stream.LogCollectorProto.internal_static_stream_PExecuteState_descriptor;
        }

        @java.lang.Override
        public com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState getDefaultInstanceForType() {
            return com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState.getDefaultInstance();
        }

        @java.lang.Override
        public com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState build() {
            com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState result = buildPartial();
            if (!result.isInitialized()) {
                throw newUninitializedMessageException(result);
            }
            return result;
        }

        @java.lang.Override
        public com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState buildPartial() {
            com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState result = new com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState(this);
            result.infoType_ = infoType_;
            result.logType_ = logType_;
            result.msg_ = msg_;
            result.from_ = from_;
            result.jobId_ = jobId_;
            result.taskId_ = taskId_;
            result.serviceName_ = serviceName_;
            result.execState_ = execState_;
            result.time_ = time_;
            result.component_ = component_;
            onBuilt();
            return result;
        }

        @java.lang.Override
        public Builder clone() {
            return super.clone();
        }

        @java.lang.Override
        public Builder setField(com.google.protobuf.Descriptors.FieldDescriptor field, java.lang.Object value) {
            return super.setField(field, value);
        }

        @java.lang.Override
        public Builder clearField(com.google.protobuf.Descriptors.FieldDescriptor field) {
            return super.clearField(field);
        }

        @java.lang.Override
        public Builder clearOneof(com.google.protobuf.Descriptors.OneofDescriptor oneof) {
            return super.clearOneof(oneof);
        }

        @java.lang.Override
        public Builder setRepeatedField(com.google.protobuf.Descriptors.FieldDescriptor field, int index, java.lang.Object value) {
            return super.setRepeatedField(field, index, value);
        }

        @java.lang.Override
        public Builder addRepeatedField(com.google.protobuf.Descriptors.FieldDescriptor field, java.lang.Object value) {
            return super.addRepeatedField(field, value);
        }

        @java.lang.Override
        public Builder mergeFrom(com.google.protobuf.Message other) {
            if (other instanceof com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState) {
                return mergeFrom((com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState) other);
            } else {
                super.mergeFrom(other);
                return this;
            }
        }

        public Builder mergeFrom(com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState other) {
            if (other == com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState.getDefaultInstance())
                return this;
            if (other.infoType_ != 0) {
                setInfoTypeValue(other.getInfoTypeValue());
            }
            if (other.logType_ != 0) {
                setLogTypeValue(other.getLogTypeValue());
            }
            if (!other.getMsg().isEmpty()) {
                msg_ = other.msg_;
                onChanged();
            }
            if (!other.getFrom().isEmpty()) {
                from_ = other.from_;
                onChanged();
            }
            if (other.getJobId() != 0L) {
                setJobId(other.getJobId());
            }
            if (other.getTaskId() != 0L) {
                setTaskId(other.getTaskId());
            }
            if (!other.getServiceName().isEmpty()) {
                serviceName_ = other.serviceName_;
                onChanged();
            }
            if (!other.getExecState().isEmpty()) {
                execState_ = other.execState_;
                onChanged();
            }
            if (other.getTime() != 0L) {
                setTime(other.getTime());
            }
            if (!other.getComponent().isEmpty()) {
                component_ = other.component_;
                onChanged();
            }
            this.mergeUnknownFields(other.unknownFields);
            onChanged();
            return this;
        }

        @java.lang.Override
        public final boolean isInitialized() {
            return true;
        }

        @java.lang.Override
        public Builder mergeFrom(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
            com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState parsedMessage = null;
            try {
                parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
            } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                parsedMessage = (com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState) e.getUnfinishedMessage();
                throw e.unwrapIOException();
            } finally {
                if (parsedMessage != null) {
                    mergeFrom(parsedMessage);
                }
            }
            return this;
        }

        private int infoType_ = 0;

        /**
         * <code>.stream.PExecuteState.InfoType infoType = 1;</code>
         */
        public int getInfoTypeValue() {
            return infoType_;
        }

        /**
         * <code>.stream.PExecuteState.InfoType infoType = 1;</code>
         */
        public Builder setInfoTypeValue(int value) {
            infoType_ = value;
            onChanged();
            return this;
        }

        /**
         * <code>.stream.PExecuteState.InfoType infoType = 1;</code>
         */
        public com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState.InfoType getInfoType() {
            @SuppressWarnings("deprecation")
            com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState.InfoType result = com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState.InfoType.valueOf(infoType_);
            return result == null ? com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState.InfoType.UNRECOGNIZED : result;
        }

        /**
         * <code>.stream.PExecuteState.InfoType infoType = 1;</code>
         */
        public Builder setInfoType(com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState.InfoType value) {
            if (value == null) {
                throw new NullPointerException();
            }
            infoType_ = value.getNumber();
            onChanged();
            return this;
        }

        /**
         * <code>.stream.PExecuteState.InfoType infoType = 1;</code>
         */
        public Builder clearInfoType() {
            infoType_ = 0;
            onChanged();
            return this;
        }

        private int logType_ = 0;

        /**
         * <code>.stream.PExecuteState.LogType logType = 2;</code>
         */
        public int getLogTypeValue() {
            return logType_;
        }

        /**
         * <code>.stream.PExecuteState.LogType logType = 2;</code>
         */
        public Builder setLogTypeValue(int value) {
            logType_ = value;
            onChanged();
            return this;
        }

        /**
         * <code>.stream.PExecuteState.LogType logType = 2;</code>
         */
        public com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState.LogType getLogType() {
            @SuppressWarnings("deprecation")
            com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState.LogType result = com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState.LogType.valueOf(logType_);
            return result == null ? com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState.LogType.UNRECOGNIZED : result;
        }

        /**
         * <code>.stream.PExecuteState.LogType logType = 2;</code>
         */
        public Builder setLogType(com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState.LogType value) {
            if (value == null) {
                throw new NullPointerException();
            }
            logType_ = value.getNumber();
            onChanged();
            return this;
        }

        /**
         * <code>.stream.PExecuteState.LogType logType = 2;</code>
         */
        public Builder clearLogType() {
            logType_ = 0;
            onChanged();
            return this;
        }

        private java.lang.Object msg_ = "";

        /**
         * <code>string msg = 3;</code>
         */
        public java.lang.String getMsg() {
            java.lang.Object ref = msg_;
            if (!(ref instanceof java.lang.String)) {
                com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
                java.lang.String s = bs.toStringUtf8();
                msg_ = s;
                return s;
            } else {
                return (java.lang.String) ref;
            }
        }

        /**
         * <code>string msg = 3;</code>
         */
        public com.google.protobuf.ByteString getMsgBytes() {
            java.lang.Object ref = msg_;
            if (ref instanceof String) {
                com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((java.lang.String) ref);
                msg_ = b;
                return b;
            } else {
                return (com.google.protobuf.ByteString) ref;
            }
        }

        /**
         * <code>string msg = 3;</code>
         */
        public Builder setMsg(java.lang.String value) {
            if (value == null) {
                throw new NullPointerException();
            }
            msg_ = value;
            onChanged();
            return this;
        }

        /**
         * <code>string msg = 3;</code>
         */
        public Builder clearMsg() {
            msg_ = getDefaultInstance().getMsg();
            onChanged();
            return this;
        }

        /**
         * <code>string msg = 3;</code>
         */
        public Builder setMsgBytes(com.google.protobuf.ByteString value) {
            if (value == null) {
                throw new NullPointerException();
            }
            checkByteStringIsUtf8(value);
            msg_ = value;
            onChanged();
            return this;
        }

        private java.lang.Object from_ = "";

        /**
         * <code>string from = 4;</code>
         */
        public java.lang.String getFrom() {
            java.lang.Object ref = from_;
            if (!(ref instanceof java.lang.String)) {
                com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
                java.lang.String s = bs.toStringUtf8();
                from_ = s;
                return s;
            } else {
                return (java.lang.String) ref;
            }
        }

        /**
         * <code>string from = 4;</code>
         */
        public com.google.protobuf.ByteString getFromBytes() {
            java.lang.Object ref = from_;
            if (ref instanceof String) {
                com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((java.lang.String) ref);
                from_ = b;
                return b;
            } else {
                return (com.google.protobuf.ByteString) ref;
            }
        }

        /**
         * <code>string from = 4;</code>
         */
        public Builder setFrom(java.lang.String value) {
            if (value == null) {
                throw new NullPointerException();
            }
            from_ = value;
            onChanged();
            return this;
        }

        /**
         * <code>string from = 4;</code>
         */
        public Builder clearFrom() {
            from_ = getDefaultInstance().getFrom();
            onChanged();
            return this;
        }

        /**
         * <code>string from = 4;</code>
         */
        public Builder setFromBytes(com.google.protobuf.ByteString value) {
            if (value == null) {
                throw new NullPointerException();
            }
            checkByteStringIsUtf8(value);
            from_ = value;
            onChanged();
            return this;
        }

        private long jobId_;

        /**
         * <code>uint64 jobId = 5;</code>
         */
        public long getJobId() {
            return jobId_;
        }

        /**
         * <code>uint64 jobId = 5;</code>
         */
        public Builder setJobId(long value) {
            jobId_ = value;
            onChanged();
            return this;
        }

        /**
         * <code>uint64 jobId = 5;</code>
         */
        public Builder clearJobId() {
            jobId_ = 0L;
            onChanged();
            return this;
        }

        private long taskId_;

        /**
         * <code>uint64 taskId = 6;</code>
         */
        public long getTaskId() {
            return taskId_;
        }

        /**
         * <code>uint64 taskId = 6;</code>
         */
        public Builder setTaskId(long value) {
            taskId_ = value;
            onChanged();
            return this;
        }

        /**
         * <code>uint64 taskId = 6;</code>
         */
        public Builder clearTaskId() {
            taskId_ = 0L;
            onChanged();
            return this;
        }

        private java.lang.Object serviceName_ = "";

        /**
         * <code>string serviceName = 7;</code>
         */
        public java.lang.String getServiceName() {
            java.lang.Object ref = serviceName_;
            if (!(ref instanceof java.lang.String)) {
                com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
                java.lang.String s = bs.toStringUtf8();
                serviceName_ = s;
                return s;
            } else {
                return (java.lang.String) ref;
            }
        }

        /**
         * <code>string serviceName = 7;</code>
         */
        public com.google.protobuf.ByteString getServiceNameBytes() {
            java.lang.Object ref = serviceName_;
            if (ref instanceof String) {
                com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((java.lang.String) ref);
                serviceName_ = b;
                return b;
            } else {
                return (com.google.protobuf.ByteString) ref;
            }
        }

        /**
         * <code>string serviceName = 7;</code>
         */
        public Builder setServiceName(java.lang.String value) {
            if (value == null) {
                throw new NullPointerException();
            }
            serviceName_ = value;
            onChanged();
            return this;
        }

        /**
         * <code>string serviceName = 7;</code>
         */
        public Builder clearServiceName() {
            serviceName_ = getDefaultInstance().getServiceName();
            onChanged();
            return this;
        }

        /**
         * <code>string serviceName = 7;</code>
         */
        public Builder setServiceNameBytes(com.google.protobuf.ByteString value) {
            if (value == null) {
                throw new NullPointerException();
            }
            checkByteStringIsUtf8(value);
            serviceName_ = value;
            onChanged();
            return this;
        }

        private java.lang.Object execState_ = "";

        /**
         * <code>string execState = 8;</code>
         */
        public java.lang.String getExecState() {
            java.lang.Object ref = execState_;
            if (!(ref instanceof java.lang.String)) {
                com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
                java.lang.String s = bs.toStringUtf8();
                execState_ = s;
                return s;
            } else {
                return (java.lang.String) ref;
            }
        }

        /**
         * <code>string execState = 8;</code>
         */
        public com.google.protobuf.ByteString getExecStateBytes() {
            java.lang.Object ref = execState_;
            if (ref instanceof String) {
                com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((java.lang.String) ref);
                execState_ = b;
                return b;
            } else {
                return (com.google.protobuf.ByteString) ref;
            }
        }

        /**
         * <code>string execState = 8;</code>
         */
        public Builder setExecState(java.lang.String value) {
            if (value == null) {
                throw new NullPointerException();
            }
            execState_ = value;
            onChanged();
            return this;
        }

        /**
         * <code>string execState = 8;</code>
         */
        public Builder clearExecState() {
            execState_ = getDefaultInstance().getExecState();
            onChanged();
            return this;
        }

        /**
         * <code>string execState = 8;</code>
         */
        public Builder setExecStateBytes(com.google.protobuf.ByteString value) {
            if (value == null) {
                throw new NullPointerException();
            }
            checkByteStringIsUtf8(value);
            execState_ = value;
            onChanged();
            return this;
        }

        private long time_;

        /**
         * <code>uint64 time = 9;</code>
         */
        public long getTime() {
            return time_;
        }

        /**
         * <code>uint64 time = 9;</code>
         */
        public Builder setTime(long value) {
            time_ = value;
            onChanged();
            return this;
        }

        /**
         * <code>uint64 time = 9;</code>
         */
        public Builder clearTime() {
            time_ = 0L;
            onChanged();
            return this;
        }

        private java.lang.Object component_ = "";

        /**
         * <code>string component = 10;</code>
         */
        public java.lang.String getComponent() {
            java.lang.Object ref = component_;
            if (!(ref instanceof java.lang.String)) {
                com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
                java.lang.String s = bs.toStringUtf8();
                component_ = s;
                return s;
            } else {
                return (java.lang.String) ref;
            }
        }

        /**
         * <code>string component = 10;</code>
         */
        public com.google.protobuf.ByteString getComponentBytes() {
            java.lang.Object ref = component_;
            if (ref instanceof String) {
                com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((java.lang.String) ref);
                component_ = b;
                return b;
            } else {
                return (com.google.protobuf.ByteString) ref;
            }
        }

        /**
         * <code>string component = 10;</code>
         */
        public Builder setComponent(java.lang.String value) {
            if (value == null) {
                throw new NullPointerException();
            }
            component_ = value;
            onChanged();
            return this;
        }

        /**
         * <code>string component = 10;</code>
         */
        public Builder clearComponent() {
            component_ = getDefaultInstance().getComponent();
            onChanged();
            return this;
        }

        /**
         * <code>string component = 10;</code>
         */
        public Builder setComponentBytes(com.google.protobuf.ByteString value) {
            if (value == null) {
                throw new NullPointerException();
            }
            checkByteStringIsUtf8(value);
            component_ = value;
            onChanged();
            return this;
        }

        @java.lang.Override
        public final Builder setUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
            return super.setUnknownFields(unknownFields);
        }

        @java.lang.Override
        public final Builder mergeUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
            return super.mergeUnknownFields(unknownFields);
        }
        // @@protoc_insertion_point(builder_scope:stream.PExecuteState)
    }

    // @@protoc_insertion_point(class_scope:stream.PExecuteState)
    private static final com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState DEFAULT_INSTANCE;

    static {
        DEFAULT_INSTANCE = new com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState();
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    private static final com.google.protobuf.Parser<PExecuteState> PARSER = new com.google.protobuf.AbstractParser<PExecuteState>() {

        @java.lang.Override
        public PExecuteState parsePartialFrom(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
            return new PExecuteState(input, extensionRegistry);
        }
    };

    public static com.google.protobuf.Parser<PExecuteState> parser() {
        return PARSER;
    }

    @java.lang.Override
    public com.google.protobuf.Parser<PExecuteState> getParserForType() {
        return PARSER;
    }

    @java.lang.Override
    public com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState getDefaultInstanceForType() {
        return DEFAULT_INSTANCE;
    }
}
