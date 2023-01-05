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
 * Protobuf type {@code BuildSharedPhaseStatus}
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/09/25
 */
public final class BuildSharedPhaseStatus extends com.google.protobuf.GeneratedMessageV3 implements // @@protoc_insertion_point(message_implements:BuildSharedPhaseStatus)
BuildSharedPhaseStatusOrBuilder {

    private static final long serialVersionUID = 0L;

    // Use BuildSharedPhaseStatus.newBuilder() to construct.
    private BuildSharedPhaseStatus(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
        super(builder);
    }

    private BuildSharedPhaseStatus() {
        sharedName_ = "";
    }

    @java.lang.Override
    public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
        return this.unknownFields;
    }

    private BuildSharedPhaseStatus(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
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
                            allBuildSize_ = input.readUInt64();
                            break;
                        }
                    case 16:
                        {
                            buildReaded_ = input.readUInt64();
                            break;
                        }
                    case 24:
                        {
                            taskid_ = input.readUInt32();
                            break;
                        }
                    case 34:
                        {
                            java.lang.String s = input.readStringRequireUtf8();
                            sharedName_ = s;
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
        return com.qlangtech.tis.rpc.grpc.log.common.LogCollectorProto.internal_static_BuildSharedPhaseStatus_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
        return com.qlangtech.tis.rpc.grpc.log.common.LogCollectorProto.internal_static_BuildSharedPhaseStatus_fieldAccessorTable.ensureFieldAccessorsInitialized(com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus.class, com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus.Builder.class);
    }

    public static final int ALLBUILDSIZE_FIELD_NUMBER = 1;

    private long allBuildSize_;

    /**
     * <code>uint64 allBuildSize = 1;</code>
     */
    public long getAllBuildSize() {
        return allBuildSize_;
    }

    public static final int BUILDREADED_FIELD_NUMBER = 2;

    private long buildReaded_;

    /**
     * <code>uint64 buildReaded = 2;</code>
     */
    public long getBuildReaded() {
        return buildReaded_;
    }

    public static final int TASKID_FIELD_NUMBER = 3;

    private int taskid_;

    /**
     * <code>uint32 taskid = 3;</code>
     */
    public int getTaskid() {
        return taskid_;
    }

    public static final int SHAREDNAME_FIELD_NUMBER = 4;

    private volatile java.lang.Object sharedName_;

    /**
     * <pre>
     * 分组名称
     * </pre>
     *
     * <code>string sharedName = 4;</code>
     */
    public java.lang.String getSharedName() {
        java.lang.Object ref = sharedName_;
        if (ref instanceof java.lang.String) {
            return (java.lang.String) ref;
        } else {
            com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
            java.lang.String s = bs.toStringUtf8();
            sharedName_ = s;
            return s;
        }
    }

    /**
     * <pre>
     * 分组名称
     * </pre>
     *
     * <code>string sharedName = 4;</code>
     */
    public com.google.protobuf.ByteString getSharedNameBytes() {
        java.lang.Object ref = sharedName_;
        if (ref instanceof java.lang.String) {
            com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((java.lang.String) ref);
            sharedName_ = b;
            return b;
        } else {
            return (com.google.protobuf.ByteString) ref;
        }
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
        if (allBuildSize_ != 0L) {
            output.writeUInt64(1, allBuildSize_);
        }
        if (buildReaded_ != 0L) {
            output.writeUInt64(2, buildReaded_);
        }
        if (taskid_ != 0) {
            output.writeUInt32(3, taskid_);
        }
        if (!getSharedNameBytes().isEmpty()) {
            com.google.protobuf.GeneratedMessageV3.writeString(output, 4, sharedName_);
        }
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
        if (allBuildSize_ != 0L) {
            size += com.google.protobuf.CodedOutputStream.computeUInt64Size(1, allBuildSize_);
        }
        if (buildReaded_ != 0L) {
            size += com.google.protobuf.CodedOutputStream.computeUInt64Size(2, buildReaded_);
        }
        if (taskid_ != 0) {
            size += com.google.protobuf.CodedOutputStream.computeUInt32Size(3, taskid_);
        }
        if (!getSharedNameBytes().isEmpty()) {
            size += com.google.protobuf.GeneratedMessageV3.computeStringSize(4, sharedName_);
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
        if (!(obj instanceof com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus)) {
            return super.equals(obj);
        }
        com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus other = (com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus) obj;
        if (getAllBuildSize() != other.getAllBuildSize())
            return false;
        if (getBuildReaded() != other.getBuildReaded())
            return false;
        if (getTaskid() != other.getTaskid())
            return false;
        if (!getSharedName().equals(other.getSharedName()))
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
        hash = (37 * hash) + ALLBUILDSIZE_FIELD_NUMBER;
        hash = (53 * hash) + com.google.protobuf.Internal.hashLong(getAllBuildSize());
        hash = (37 * hash) + BUILDREADED_FIELD_NUMBER;
        hash = (53 * hash) + com.google.protobuf.Internal.hashLong(getBuildReaded());
        hash = (37 * hash) + TASKID_FIELD_NUMBER;
        hash = (53 * hash) + getTaskid();
        hash = (37 * hash) + SHAREDNAME_FIELD_NUMBER;
        hash = (53 * hash) + getSharedName().hashCode();
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

    public static com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus parseFrom(java.nio.ByteBuffer data) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus parseFrom(java.nio.ByteBuffer data, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus parseFrom(com.google.protobuf.ByteString data) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus parseFrom(com.google.protobuf.ByteString data, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus parseFrom(byte[] data) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus parseFrom(byte[] data, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus parseFrom(java.io.InputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus parseFrom(java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus parseDelimitedFrom(java.io.InputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
    }

    public static com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus parseDelimitedFrom(java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input, extensionRegistry);
    }

    public static com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus parseFrom(com.google.protobuf.CodedInputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus parseFrom(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    @java.lang.Override
    public Builder newBuilderForType() {
        return newBuilder();
    }

    public static Builder newBuilder() {
        return DEFAULT_INSTANCE.toBuilder();
    }

    public static Builder newBuilder(com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus prototype) {
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
     * Protobuf type {@code BuildSharedPhaseStatus}
     */
    public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements // @@protoc_insertion_point(builder_implements:BuildSharedPhaseStatus)
    com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatusOrBuilder {

        public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
            return com.qlangtech.tis.rpc.grpc.log.common.LogCollectorProto.internal_static_BuildSharedPhaseStatus_descriptor;
        }

        @java.lang.Override
        protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
            return com.qlangtech.tis.rpc.grpc.log.common.LogCollectorProto.internal_static_BuildSharedPhaseStatus_fieldAccessorTable.ensureFieldAccessorsInitialized(com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus.class, com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus.Builder.class);
        }

        // Construct using com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus.newBuilder()
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
            allBuildSize_ = 0L;
            buildReaded_ = 0L;
            taskid_ = 0;
            sharedName_ = "";
            faild_ = false;
            complete_ = false;
            waiting_ = false;
            return this;
        }

        @java.lang.Override
        public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
            return com.qlangtech.tis.rpc.grpc.log.common.LogCollectorProto.internal_static_BuildSharedPhaseStatus_descriptor;
        }

        @java.lang.Override
        public com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus getDefaultInstanceForType() {
            return com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus.getDefaultInstance();
        }

        @java.lang.Override
        public com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus build() {
            com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus result = buildPartial();
            if (!result.isInitialized()) {
                throw newUninitializedMessageException(result);
            }
            return result;
        }

        @java.lang.Override
        public com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus buildPartial() {
            com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus result = new com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus(this);
            result.allBuildSize_ = allBuildSize_;
            result.buildReaded_ = buildReaded_;
            result.taskid_ = taskid_;
            result.sharedName_ = sharedName_;
            result.faild_ = faild_;
            result.complete_ = complete_;
            result.waiting_ = waiting_;
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
            if (other instanceof com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus) {
                return mergeFrom((com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus) other);
            } else {
                super.mergeFrom(other);
                return this;
            }
        }

        public Builder mergeFrom(com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus other) {
            if (other == com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus.getDefaultInstance())
                return this;
            if (other.getAllBuildSize() != 0L) {
                setAllBuildSize(other.getAllBuildSize());
            }
            if (other.getBuildReaded() != 0L) {
                setBuildReaded(other.getBuildReaded());
            }
            if (other.getTaskid() != 0) {
                setTaskid(other.getTaskid());
            }
            if (!other.getSharedName().isEmpty()) {
                sharedName_ = other.sharedName_;
                onChanged();
            }
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
            com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus parsedMessage = null;
            try {
                parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
            } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                parsedMessage = (com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus) e.getUnfinishedMessage();
                throw e.unwrapIOException();
            } finally {
                if (parsedMessage != null) {
                    mergeFrom(parsedMessage);
                }
            }
            return this;
        }

        private long allBuildSize_;

        /**
         * <code>uint64 allBuildSize = 1;</code>
         */
        public long getAllBuildSize() {
            return allBuildSize_;
        }

        /**
         * <code>uint64 allBuildSize = 1;</code>
         */
        public Builder setAllBuildSize(long value) {
            allBuildSize_ = value;
            onChanged();
            return this;
        }

        /**
         * <code>uint64 allBuildSize = 1;</code>
         */
        public Builder clearAllBuildSize() {
            allBuildSize_ = 0L;
            onChanged();
            return this;
        }

        private long buildReaded_;

        /**
         * <code>uint64 buildReaded = 2;</code>
         */
        public long getBuildReaded() {
            return buildReaded_;
        }

        /**
         * <code>uint64 buildReaded = 2;</code>
         */
        public Builder setBuildReaded(long value) {
            buildReaded_ = value;
            onChanged();
            return this;
        }

        /**
         * <code>uint64 buildReaded = 2;</code>
         */
        public Builder clearBuildReaded() {
            buildReaded_ = 0L;
            onChanged();
            return this;
        }

        private int taskid_;

        /**
         * <code>uint32 taskid = 3;</code>
         */
        public int getTaskid() {
            return taskid_;
        }

        /**
         * <code>uint32 taskid = 3;</code>
         */
        public Builder setTaskid(int value) {
            taskid_ = value;
            onChanged();
            return this;
        }

        /**
         * <code>uint32 taskid = 3;</code>
         */
        public Builder clearTaskid() {
            taskid_ = 0;
            onChanged();
            return this;
        }

        private java.lang.Object sharedName_ = "";

        /**
         * <pre>
         * 分组名称
         * </pre>
         *
         * <code>string sharedName = 4;</code>
         */
        public java.lang.String getSharedName() {
            java.lang.Object ref = sharedName_;
            if (!(ref instanceof java.lang.String)) {
                com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
                java.lang.String s = bs.toStringUtf8();
                sharedName_ = s;
                return s;
            } else {
                return (java.lang.String) ref;
            }
        }

        /**
         * <pre>
         * 分组名称
         * </pre>
         *
         * <code>string sharedName = 4;</code>
         */
        public com.google.protobuf.ByteString getSharedNameBytes() {
            java.lang.Object ref = sharedName_;
            if (ref instanceof String) {
                com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((java.lang.String) ref);
                sharedName_ = b;
                return b;
            } else {
                return (com.google.protobuf.ByteString) ref;
            }
        }

        /**
         * <pre>
         * 分组名称
         * </pre>
         *
         * <code>string sharedName = 4;</code>
         */
        public Builder setSharedName(java.lang.String value) {
            if (value == null) {
                throw new NullPointerException();
            }
            sharedName_ = value;
            onChanged();
            return this;
        }

        /**
         * <pre>
         * 分组名称
         * </pre>
         *
         * <code>string sharedName = 4;</code>
         */
        public Builder clearSharedName() {
            sharedName_ = getDefaultInstance().getSharedName();
            onChanged();
            return this;
        }

        /**
         * <pre>
         * 分组名称
         * </pre>
         *
         * <code>string sharedName = 4;</code>
         */
        public Builder setSharedNameBytes(com.google.protobuf.ByteString value) {
            if (value == null) {
                throw new NullPointerException();
            }
            checkByteStringIsUtf8(value);
            sharedName_ = value;
            onChanged();
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
        // @@protoc_insertion_point(builder_scope:BuildSharedPhaseStatus)
    }

    // @@protoc_insertion_point(class_scope:BuildSharedPhaseStatus)
    private static final com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus DEFAULT_INSTANCE;

    static {
        DEFAULT_INSTANCE = new com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus();
    }

    public static com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    private static final com.google.protobuf.Parser<BuildSharedPhaseStatus> PARSER = new com.google.protobuf.AbstractParser<BuildSharedPhaseStatus>() {

        @java.lang.Override
        public BuildSharedPhaseStatus parsePartialFrom(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
            return new BuildSharedPhaseStatus(input, extensionRegistry);
        }
    };

    public static com.google.protobuf.Parser<BuildSharedPhaseStatus> parser() {
        return PARSER;
    }

    @java.lang.Override
    public com.google.protobuf.Parser<BuildSharedPhaseStatus> getParserForType() {
        return PARSER;
    }

    @java.lang.Override
    public com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus getDefaultInstanceForType() {
        return DEFAULT_INSTANCE;
    }
}
