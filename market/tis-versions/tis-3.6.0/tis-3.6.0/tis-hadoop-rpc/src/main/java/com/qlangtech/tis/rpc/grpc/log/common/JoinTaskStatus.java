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
package com.qlangtech.tis.rpc.grpc.log.common;

/**
 * Protobuf type {@code JoinTaskStatus}
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/09/25
 */
public final class JoinTaskStatus extends com.google.protobuf.GeneratedMessageV3 implements // @@protoc_insertion_point(message_implements:JoinTaskStatus)
JoinTaskStatusOrBuilder {

    private static final long serialVersionUID = 0L;

    // Use JoinTaskStatus.newBuilder() to construct.
    private JoinTaskStatus(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
        super(builder);
    }

    private JoinTaskStatus() {
        joinTaskName_ = "";
    }

    @java.lang.Override
    public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
        return this.unknownFields;
    }

    private JoinTaskStatus(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
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
                    case 10:
                        {
                            java.lang.String s = input.readStringRequireUtf8();
                            joinTaskName_ = s;
                            break;
                        }
                    case 18:
                        {
                            if (!((mutable_bitField0_ & 0x00000002) != 0)) {
                                jobStatus_ = com.google.protobuf.MapField.newMapField(JobStatusDefaultEntryHolder.defaultEntry);
                                mutable_bitField0_ |= 0x00000002;
                            }
                            com.google.protobuf.MapEntry<java.lang.Integer, com.qlangtech.tis.rpc.grpc.log.common.JobLog> jobStatus__ = input.readMessage(JobStatusDefaultEntryHolder.defaultEntry.getParserForType(), extensionRegistry);
                            jobStatus_.getMutableMap().put(jobStatus__.getKey(), jobStatus__.getValue());
                            break;
                        }
                    case 40:
                        {
                            faild_ = input.readBool();
                            break;
                        }
                    case 48:
                        {
                            complete_ = input.readBool();
                            break;
                        }
                    case 56:
                        {
                            waiting_ = input.readBool();
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
        return com.qlangtech.tis.rpc.grpc.log.common.LogCollectorProto.internal_static_JoinTaskStatus_descriptor;
    }

    @SuppressWarnings({ "rawtypes" })
    @java.lang.Override
    protected com.google.protobuf.MapField internalGetMapField(int number) {
        switch(number) {
            case 2:
                return internalGetJobStatus();
            default:
                throw new RuntimeException("Invalid map field number: " + number);
        }
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
        return com.qlangtech.tis.rpc.grpc.log.common.LogCollectorProto.internal_static_JoinTaskStatus_fieldAccessorTable.ensureFieldAccessorsInitialized(com.qlangtech.tis.rpc.grpc.log.common.JoinTaskStatus.class, com.qlangtech.tis.rpc.grpc.log.common.JoinTaskStatus.Builder.class);
    }

    private int bitField0_;

    public static final int JOINTASKNAME_FIELD_NUMBER = 1;

    private volatile java.lang.Object joinTaskName_;

    /**
     * <code>string joinTaskName = 1;</code>
     */
    public java.lang.String getJoinTaskName() {
        java.lang.Object ref = joinTaskName_;
        if (ref instanceof java.lang.String) {
            return (java.lang.String) ref;
        } else {
            com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
            java.lang.String s = bs.toStringUtf8();
            joinTaskName_ = s;
            return s;
        }
    }

    /**
     * <code>string joinTaskName = 1;</code>
     */
    public com.google.protobuf.ByteString getJoinTaskNameBytes() {
        java.lang.Object ref = joinTaskName_;
        if (ref instanceof java.lang.String) {
            com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((java.lang.String) ref);
            joinTaskName_ = b;
            return b;
        } else {
            return (com.google.protobuf.ByteString) ref;
        }
    }

    public static final int JOBSTATUS_FIELD_NUMBER = 2;

    private static final class JobStatusDefaultEntryHolder {

        static final com.google.protobuf.MapEntry<java.lang.Integer, com.qlangtech.tis.rpc.grpc.log.common.JobLog> defaultEntry = com.google.protobuf.MapEntry.<java.lang.Integer, com.qlangtech.tis.rpc.grpc.log.common.JobLog>newDefaultInstance(com.qlangtech.tis.rpc.grpc.log.common.LogCollectorProto.internal_static_JoinTaskStatus_JobStatusEntry_descriptor, com.google.protobuf.WireFormat.FieldType.UINT32, 0, com.google.protobuf.WireFormat.FieldType.MESSAGE, com.qlangtech.tis.rpc.grpc.log.common.JobLog.getDefaultInstance());
    }

    private com.google.protobuf.MapField<java.lang.Integer, com.qlangtech.tis.rpc.grpc.log.common.JobLog> jobStatus_;

    private com.google.protobuf.MapField<java.lang.Integer, com.qlangtech.tis.rpc.grpc.log.common.JobLog> internalGetJobStatus() {
        if (jobStatus_ == null) {
            return com.google.protobuf.MapField.emptyMapField(JobStatusDefaultEntryHolder.defaultEntry);
        }
        return jobStatus_;
    }

    public int getJobStatusCount() {
        return internalGetJobStatus().getMap().size();
    }

    /**
     * <code>map&lt;uint32, .JobLog&gt; jobStatus = 2;</code>
     */
    public boolean containsJobStatus(int key) {
        return internalGetJobStatus().getMap().containsKey(key);
    }

    /**
     * Use {@link #getJobStatusMap()} instead.
     */
    @java.lang.Deprecated
    public java.util.Map<java.lang.Integer, com.qlangtech.tis.rpc.grpc.log.common.JobLog> getJobStatus() {
        return getJobStatusMap();
    }

    /**
     * <code>map&lt;uint32, .JobLog&gt; jobStatus = 2;</code>
     */
    public java.util.Map<java.lang.Integer, com.qlangtech.tis.rpc.grpc.log.common.JobLog> getJobStatusMap() {
        return internalGetJobStatus().getMap();
    }

    /**
     * <code>map&lt;uint32, .JobLog&gt; jobStatus = 2;</code>
     */
    public com.qlangtech.tis.rpc.grpc.log.common.JobLog getJobStatusOrDefault(int key, com.qlangtech.tis.rpc.grpc.log.common.JobLog defaultValue) {
        java.util.Map<java.lang.Integer, com.qlangtech.tis.rpc.grpc.log.common.JobLog> map = internalGetJobStatus().getMap();
        return map.containsKey(key) ? map.get(key) : defaultValue;
    }

    /**
     * <code>map&lt;uint32, .JobLog&gt; jobStatus = 2;</code>
     */
    public com.qlangtech.tis.rpc.grpc.log.common.JobLog getJobStatusOrThrow(int key) {
        java.util.Map<java.lang.Integer, com.qlangtech.tis.rpc.grpc.log.common.JobLog> map = internalGetJobStatus().getMap();
        if (!map.containsKey(key)) {
            throw new java.lang.IllegalArgumentException();
        }
        return map.get(key);
    }

    public static final int FAILD_FIELD_NUMBER = 5;

    private boolean faild_;

    /**
     * <code>bool faild = 5;</code>
     */
    public boolean getFaild() {
        return faild_;
    }

    public static final int COMPLETE_FIELD_NUMBER = 6;

    private boolean complete_;

    /**
     * <code>bool complete = 6;</code>
     */
    public boolean getComplete() {
        return complete_;
    }

    public static final int WAITING_FIELD_NUMBER = 7;

    private boolean waiting_;

    /**
     * <code>bool waiting = 7;</code>
     */
    public boolean getWaiting() {
        return waiting_;
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
        if (!getJoinTaskNameBytes().isEmpty()) {
            com.google.protobuf.GeneratedMessageV3.writeString(output, 1, joinTaskName_);
        }
        com.google.protobuf.GeneratedMessageV3.serializeIntegerMapTo(output, internalGetJobStatus(), JobStatusDefaultEntryHolder.defaultEntry, 2);
        if (faild_ != false) {
            output.writeBool(5, faild_);
        }
        if (complete_ != false) {
            output.writeBool(6, complete_);
        }
        if (waiting_ != false) {
            output.writeBool(7, waiting_);
        }
        unknownFields.writeTo(output);
    }

    @java.lang.Override
    public int getSerializedSize() {
        int size = memoizedSize;
        if (size != -1)
            return size;
        size = 0;
        if (!getJoinTaskNameBytes().isEmpty()) {
            size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, joinTaskName_);
        }
        for (java.util.Map.Entry<java.lang.Integer, com.qlangtech.tis.rpc.grpc.log.common.JobLog> entry : internalGetJobStatus().getMap().entrySet()) {
            com.google.protobuf.MapEntry<java.lang.Integer, com.qlangtech.tis.rpc.grpc.log.common.JobLog> jobStatus__ = JobStatusDefaultEntryHolder.defaultEntry.newBuilderForType().setKey(entry.getKey()).setValue(entry.getValue()).build();
            size += com.google.protobuf.CodedOutputStream.computeMessageSize(2, jobStatus__);
        }
        if (faild_ != false) {
            size += com.google.protobuf.CodedOutputStream.computeBoolSize(5, faild_);
        }
        if (complete_ != false) {
            size += com.google.protobuf.CodedOutputStream.computeBoolSize(6, complete_);
        }
        if (waiting_ != false) {
            size += com.google.protobuf.CodedOutputStream.computeBoolSize(7, waiting_);
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
        if (!(obj instanceof com.qlangtech.tis.rpc.grpc.log.common.JoinTaskStatus)) {
            return super.equals(obj);
        }
        com.qlangtech.tis.rpc.grpc.log.common.JoinTaskStatus other = (com.qlangtech.tis.rpc.grpc.log.common.JoinTaskStatus) obj;
        if (!getJoinTaskName().equals(other.getJoinTaskName()))
            return false;
        if (!internalGetJobStatus().equals(other.internalGetJobStatus()))
            return false;
        if (getFaild() != other.getFaild())
            return false;
        if (getComplete() != other.getComplete())
            return false;
        if (getWaiting() != other.getWaiting())
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
        hash = (37 * hash) + JOINTASKNAME_FIELD_NUMBER;
        hash = (53 * hash) + getJoinTaskName().hashCode();
        if (!internalGetJobStatus().getMap().isEmpty()) {
            hash = (37 * hash) + JOBSTATUS_FIELD_NUMBER;
            hash = (53 * hash) + internalGetJobStatus().hashCode();
        }
        hash = (37 * hash) + FAILD_FIELD_NUMBER;
        hash = (53 * hash) + com.google.protobuf.Internal.hashBoolean(getFaild());
        hash = (37 * hash) + COMPLETE_FIELD_NUMBER;
        hash = (53 * hash) + com.google.protobuf.Internal.hashBoolean(getComplete());
        hash = (37 * hash) + WAITING_FIELD_NUMBER;
        hash = (53 * hash) + com.google.protobuf.Internal.hashBoolean(getWaiting());
        hash = (29 * hash) + unknownFields.hashCode();
        memoizedHashCode = hash;
        return hash;
    }

    public static com.qlangtech.tis.rpc.grpc.log.common.JoinTaskStatus parseFrom(java.nio.ByteBuffer data) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static com.qlangtech.tis.rpc.grpc.log.common.JoinTaskStatus parseFrom(java.nio.ByteBuffer data, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static com.qlangtech.tis.rpc.grpc.log.common.JoinTaskStatus parseFrom(com.google.protobuf.ByteString data) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static com.qlangtech.tis.rpc.grpc.log.common.JoinTaskStatus parseFrom(com.google.protobuf.ByteString data, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static com.qlangtech.tis.rpc.grpc.log.common.JoinTaskStatus parseFrom(byte[] data) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static com.qlangtech.tis.rpc.grpc.log.common.JoinTaskStatus parseFrom(byte[] data, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static com.qlangtech.tis.rpc.grpc.log.common.JoinTaskStatus parseFrom(java.io.InputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static com.qlangtech.tis.rpc.grpc.log.common.JoinTaskStatus parseFrom(java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static com.qlangtech.tis.rpc.grpc.log.common.JoinTaskStatus parseDelimitedFrom(java.io.InputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
    }

    public static com.qlangtech.tis.rpc.grpc.log.common.JoinTaskStatus parseDelimitedFrom(java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input, extensionRegistry);
    }

    public static com.qlangtech.tis.rpc.grpc.log.common.JoinTaskStatus parseFrom(com.google.protobuf.CodedInputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static com.qlangtech.tis.rpc.grpc.log.common.JoinTaskStatus parseFrom(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    @java.lang.Override
    public Builder newBuilderForType() {
        return newBuilder();
    }

    public static Builder newBuilder() {
        return DEFAULT_INSTANCE.toBuilder();
    }

    public static Builder newBuilder(com.qlangtech.tis.rpc.grpc.log.common.JoinTaskStatus prototype) {
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
     * Protobuf type {@code JoinTaskStatus}
     */
    public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements // @@protoc_insertion_point(builder_implements:JoinTaskStatus)
    com.qlangtech.tis.rpc.grpc.log.common.JoinTaskStatusOrBuilder {

        public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
            return com.qlangtech.tis.rpc.grpc.log.common.LogCollectorProto.internal_static_JoinTaskStatus_descriptor;
        }

        @SuppressWarnings({ "rawtypes" })
        protected com.google.protobuf.MapField internalGetMapField(int number) {
            switch(number) {
                case 2:
                    return internalGetJobStatus();
                default:
                    throw new RuntimeException("Invalid map field number: " + number);
            }
        }

        @SuppressWarnings({ "rawtypes" })
        protected com.google.protobuf.MapField internalGetMutableMapField(int number) {
            switch(number) {
                case 2:
                    return internalGetMutableJobStatus();
                default:
                    throw new RuntimeException("Invalid map field number: " + number);
            }
        }

        @java.lang.Override
        protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
            return com.qlangtech.tis.rpc.grpc.log.common.LogCollectorProto.internal_static_JoinTaskStatus_fieldAccessorTable.ensureFieldAccessorsInitialized(com.qlangtech.tis.rpc.grpc.log.common.JoinTaskStatus.class, com.qlangtech.tis.rpc.grpc.log.common.JoinTaskStatus.Builder.class);
        }

        // Construct using com.qlangtech.tis.rpc.grpc.log.common.JoinTaskStatus.newBuilder()
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
            joinTaskName_ = "";
            internalGetMutableJobStatus().clear();
            faild_ = false;
            complete_ = false;
            waiting_ = false;
            return this;
        }

        @java.lang.Override
        public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
            return com.qlangtech.tis.rpc.grpc.log.common.LogCollectorProto.internal_static_JoinTaskStatus_descriptor;
        }

        @java.lang.Override
        public com.qlangtech.tis.rpc.grpc.log.common.JoinTaskStatus getDefaultInstanceForType() {
            return com.qlangtech.tis.rpc.grpc.log.common.JoinTaskStatus.getDefaultInstance();
        }

        @java.lang.Override
        public com.qlangtech.tis.rpc.grpc.log.common.JoinTaskStatus build() {
            com.qlangtech.tis.rpc.grpc.log.common.JoinTaskStatus result = buildPartial();
            if (!result.isInitialized()) {
                throw newUninitializedMessageException(result);
            }
            return result;
        }

        @java.lang.Override
        public com.qlangtech.tis.rpc.grpc.log.common.JoinTaskStatus buildPartial() {
            com.qlangtech.tis.rpc.grpc.log.common.JoinTaskStatus result = new com.qlangtech.tis.rpc.grpc.log.common.JoinTaskStatus(this);
            int from_bitField0_ = bitField0_;
            int to_bitField0_ = 0;
            result.joinTaskName_ = joinTaskName_;
            result.jobStatus_ = internalGetJobStatus();
            result.jobStatus_.makeImmutable();
            result.faild_ = faild_;
            result.complete_ = complete_;
            result.waiting_ = waiting_;
            result.bitField0_ = to_bitField0_;
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
            if (other instanceof com.qlangtech.tis.rpc.grpc.log.common.JoinTaskStatus) {
                return mergeFrom((com.qlangtech.tis.rpc.grpc.log.common.JoinTaskStatus) other);
            } else {
                super.mergeFrom(other);
                return this;
            }
        }

        public Builder mergeFrom(com.qlangtech.tis.rpc.grpc.log.common.JoinTaskStatus other) {
            if (other == com.qlangtech.tis.rpc.grpc.log.common.JoinTaskStatus.getDefaultInstance())
                return this;
            if (!other.getJoinTaskName().isEmpty()) {
                joinTaskName_ = other.joinTaskName_;
                onChanged();
            }
            internalGetMutableJobStatus().mergeFrom(other.internalGetJobStatus());
            if (other.getFaild() != false) {
                setFaild(other.getFaild());
            }
            if (other.getComplete() != false) {
                setComplete(other.getComplete());
            }
            if (other.getWaiting() != false) {
                setWaiting(other.getWaiting());
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
            com.qlangtech.tis.rpc.grpc.log.common.JoinTaskStatus parsedMessage = null;
            try {
                parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
            } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                parsedMessage = (com.qlangtech.tis.rpc.grpc.log.common.JoinTaskStatus) e.getUnfinishedMessage();
                throw e.unwrapIOException();
            } finally {
                if (parsedMessage != null) {
                    mergeFrom(parsedMessage);
                }
            }
            return this;
        }

        private int bitField0_;

        private java.lang.Object joinTaskName_ = "";

        /**
         * <code>string joinTaskName = 1;</code>
         */
        public java.lang.String getJoinTaskName() {
            java.lang.Object ref = joinTaskName_;
            if (!(ref instanceof java.lang.String)) {
                com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
                java.lang.String s = bs.toStringUtf8();
                joinTaskName_ = s;
                return s;
            } else {
                return (java.lang.String) ref;
            }
        }

        /**
         * <code>string joinTaskName = 1;</code>
         */
        public com.google.protobuf.ByteString getJoinTaskNameBytes() {
            java.lang.Object ref = joinTaskName_;
            if (ref instanceof String) {
                com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((java.lang.String) ref);
                joinTaskName_ = b;
                return b;
            } else {
                return (com.google.protobuf.ByteString) ref;
            }
        }

        /**
         * <code>string joinTaskName = 1;</code>
         */
        public Builder setJoinTaskName(java.lang.String value) {
            if (value == null) {
                throw new NullPointerException();
            }
            joinTaskName_ = value;
            onChanged();
            return this;
        }

        /**
         * <code>string joinTaskName = 1;</code>
         */
        public Builder clearJoinTaskName() {
            joinTaskName_ = getDefaultInstance().getJoinTaskName();
            onChanged();
            return this;
        }

        /**
         * <code>string joinTaskName = 1;</code>
         */
        public Builder setJoinTaskNameBytes(com.google.protobuf.ByteString value) {
            if (value == null) {
                throw new NullPointerException();
            }
            checkByteStringIsUtf8(value);
            joinTaskName_ = value;
            onChanged();
            return this;
        }

        private com.google.protobuf.MapField<java.lang.Integer, com.qlangtech.tis.rpc.grpc.log.common.JobLog> jobStatus_;

        private com.google.protobuf.MapField<java.lang.Integer, com.qlangtech.tis.rpc.grpc.log.common.JobLog> internalGetJobStatus() {
            if (jobStatus_ == null) {
                return com.google.protobuf.MapField.emptyMapField(JobStatusDefaultEntryHolder.defaultEntry);
            }
            return jobStatus_;
        }

        private com.google.protobuf.MapField<java.lang.Integer, com.qlangtech.tis.rpc.grpc.log.common.JobLog> internalGetMutableJobStatus() {
            onChanged();
            ;
            if (jobStatus_ == null) {
                jobStatus_ = com.google.protobuf.MapField.newMapField(JobStatusDefaultEntryHolder.defaultEntry);
            }
            if (!jobStatus_.isMutable()) {
                jobStatus_ = jobStatus_.copy();
            }
            return jobStatus_;
        }

        public int getJobStatusCount() {
            return internalGetJobStatus().getMap().size();
        }

        /**
         * <code>map&lt;uint32, .JobLog&gt; jobStatus = 2;</code>
         */
        public boolean containsJobStatus(int key) {
            return internalGetJobStatus().getMap().containsKey(key);
        }

        /**
         * Use {@link #getJobStatusMap()} instead.
         */
        @java.lang.Deprecated
        public java.util.Map<java.lang.Integer, com.qlangtech.tis.rpc.grpc.log.common.JobLog> getJobStatus() {
            return getJobStatusMap();
        }

        /**
         * <code>map&lt;uint32, .JobLog&gt; jobStatus = 2;</code>
         */
        public java.util.Map<java.lang.Integer, com.qlangtech.tis.rpc.grpc.log.common.JobLog> getJobStatusMap() {
            return internalGetJobStatus().getMap();
        }

        /**
         * <code>map&lt;uint32, .JobLog&gt; jobStatus = 2;</code>
         */
        public com.qlangtech.tis.rpc.grpc.log.common.JobLog getJobStatusOrDefault(int key, com.qlangtech.tis.rpc.grpc.log.common.JobLog defaultValue) {
            java.util.Map<java.lang.Integer, com.qlangtech.tis.rpc.grpc.log.common.JobLog> map = internalGetJobStatus().getMap();
            return map.containsKey(key) ? map.get(key) : defaultValue;
        }

        /**
         * <code>map&lt;uint32, .JobLog&gt; jobStatus = 2;</code>
         */
        public com.qlangtech.tis.rpc.grpc.log.common.JobLog getJobStatusOrThrow(int key) {
            java.util.Map<java.lang.Integer, com.qlangtech.tis.rpc.grpc.log.common.JobLog> map = internalGetJobStatus().getMap();
            if (!map.containsKey(key)) {
                throw new java.lang.IllegalArgumentException();
            }
            return map.get(key);
        }

        public Builder clearJobStatus() {
            internalGetMutableJobStatus().getMutableMap().clear();
            return this;
        }

        /**
         * <code>map&lt;uint32, .JobLog&gt; jobStatus = 2;</code>
         */
        public Builder removeJobStatus(int key) {
            internalGetMutableJobStatus().getMutableMap().remove(key);
            return this;
        }

        /**
         * Use alternate mutation accessors instead.
         */
        @java.lang.Deprecated
        public java.util.Map<java.lang.Integer, com.qlangtech.tis.rpc.grpc.log.common.JobLog> getMutableJobStatus() {
            return internalGetMutableJobStatus().getMutableMap();
        }

        /**
         * <code>map&lt;uint32, .JobLog&gt; jobStatus = 2;</code>
         */
        public Builder putJobStatus(int key, com.qlangtech.tis.rpc.grpc.log.common.JobLog value) {
            if (value == null) {
                throw new java.lang.NullPointerException();
            }
            internalGetMutableJobStatus().getMutableMap().put(key, value);
            return this;
        }

        /**
         * <code>map&lt;uint32, .JobLog&gt; jobStatus = 2;</code>
         */
        public Builder putAllJobStatus(java.util.Map<java.lang.Integer, com.qlangtech.tis.rpc.grpc.log.common.JobLog> values) {
            internalGetMutableJobStatus().getMutableMap().putAll(values);
            return this;
        }

        private boolean faild_;

        /**
         * <code>bool faild = 5;</code>
         */
        public boolean getFaild() {
            return faild_;
        }

        /**
         * <code>bool faild = 5;</code>
         */
        public Builder setFaild(boolean value) {
            faild_ = value;
            onChanged();
            return this;
        }

        /**
         * <code>bool faild = 5;</code>
         */
        public Builder clearFaild() {
            faild_ = false;
            onChanged();
            return this;
        }

        private boolean complete_;

        /**
         * <code>bool complete = 6;</code>
         */
        public boolean getComplete() {
            return complete_;
        }

        /**
         * <code>bool complete = 6;</code>
         */
        public Builder setComplete(boolean value) {
            complete_ = value;
            onChanged();
            return this;
        }

        /**
         * <code>bool complete = 6;</code>
         */
        public Builder clearComplete() {
            complete_ = false;
            onChanged();
            return this;
        }

        private boolean waiting_;

        /**
         * <code>bool waiting = 7;</code>
         */
        public boolean getWaiting() {
            return waiting_;
        }

        /**
         * <code>bool waiting = 7;</code>
         */
        public Builder setWaiting(boolean value) {
            waiting_ = value;
            onChanged();
            return this;
        }

        /**
         * <code>bool waiting = 7;</code>
         */
        public Builder clearWaiting() {
            waiting_ = false;
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
        // @@protoc_insertion_point(builder_scope:JoinTaskStatus)
    }

    // @@protoc_insertion_point(class_scope:JoinTaskStatus)
    private static final com.qlangtech.tis.rpc.grpc.log.common.JoinTaskStatus DEFAULT_INSTANCE;

    static {
        DEFAULT_INSTANCE = new com.qlangtech.tis.rpc.grpc.log.common.JoinTaskStatus();
    }

    public static com.qlangtech.tis.rpc.grpc.log.common.JoinTaskStatus getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    private static final com.google.protobuf.Parser<JoinTaskStatus> PARSER = new com.google.protobuf.AbstractParser<JoinTaskStatus>() {

        @java.lang.Override
        public JoinTaskStatus parsePartialFrom(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
            return new JoinTaskStatus(input, extensionRegistry);
        }
    };

    public static com.google.protobuf.Parser<JoinTaskStatus> parser() {
        return PARSER;
    }

    @java.lang.Override
    public com.google.protobuf.Parser<JoinTaskStatus> getParserForType() {
        return PARSER;
    }

    @java.lang.Override
    public com.qlangtech.tis.rpc.grpc.log.common.JoinTaskStatus getDefaultInstanceForType() {
        return DEFAULT_INSTANCE;
    }
}
