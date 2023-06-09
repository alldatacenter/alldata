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
 * Protobuf type {@code stream.PPhaseStatusCollection}
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/09/25
 */
public final class PPhaseStatusCollection extends com.google.protobuf.GeneratedMessageV3 implements // @@protoc_insertion_point(message_implements:stream.PPhaseStatusCollection)
PPhaseStatusCollectionOrBuilder {

    private static final long serialVersionUID = 0L;

    // Use PPhaseStatusCollection.newBuilder() to construct.
    private PPhaseStatusCollection(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
        super(builder);
    }

    private PPhaseStatusCollection() {
    }

    @java.lang.Override
    public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
        return this.unknownFields;
    }

    private PPhaseStatusCollection(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
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
                            com.qlangtech.tis.rpc.grpc.log.stream.PDumpPhaseStatus.Builder subBuilder = null;
                            if (dumpPhase_ != null) {
                                subBuilder = dumpPhase_.toBuilder();
                            }
                            dumpPhase_ = input.readMessage(com.qlangtech.tis.rpc.grpc.log.stream.PDumpPhaseStatus.parser(), extensionRegistry);
                            if (subBuilder != null) {
                                subBuilder.mergeFrom(dumpPhase_);
                                dumpPhase_ = subBuilder.buildPartial();
                            }
                            break;
                        }
                    case 18:
                        {
                            com.qlangtech.tis.rpc.grpc.log.stream.PJoinPhaseStatus.Builder subBuilder = null;
                            if (joinPhase_ != null) {
                                subBuilder = joinPhase_.toBuilder();
                            }
                            joinPhase_ = input.readMessage(com.qlangtech.tis.rpc.grpc.log.stream.PJoinPhaseStatus.parser(), extensionRegistry);
                            if (subBuilder != null) {
                                subBuilder.mergeFrom(joinPhase_);
                                joinPhase_ = subBuilder.buildPartial();
                            }
                            break;
                        }
                    case 26:
                        {
                            com.qlangtech.tis.rpc.grpc.log.stream.PBuildPhaseStatus.Builder subBuilder = null;
                            if (buildPhase_ != null) {
                                subBuilder = buildPhase_.toBuilder();
                            }
                            buildPhase_ = input.readMessage(com.qlangtech.tis.rpc.grpc.log.stream.PBuildPhaseStatus.parser(), extensionRegistry);
                            if (subBuilder != null) {
                                subBuilder.mergeFrom(buildPhase_);
                                buildPhase_ = subBuilder.buildPartial();
                            }
                            break;
                        }
                    case 34:
                        {
                            com.qlangtech.tis.rpc.grpc.log.stream.PIndexBackFlowPhaseStatus.Builder subBuilder = null;
                            if (indexBackFlowPhaseStatus_ != null) {
                                subBuilder = indexBackFlowPhaseStatus_.toBuilder();
                            }
                            indexBackFlowPhaseStatus_ = input.readMessage(com.qlangtech.tis.rpc.grpc.log.stream.PIndexBackFlowPhaseStatus.parser(), extensionRegistry);
                            if (subBuilder != null) {
                                subBuilder.mergeFrom(indexBackFlowPhaseStatus_);
                                indexBackFlowPhaseStatus_ = subBuilder.buildPartial();
                            }
                            break;
                        }
                    case 40:
                        {
                            taskId_ = input.readUInt32();
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
        return com.qlangtech.tis.rpc.grpc.log.stream.LogCollectorProto.internal_static_stream_PPhaseStatusCollection_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
        return com.qlangtech.tis.rpc.grpc.log.stream.LogCollectorProto.internal_static_stream_PPhaseStatusCollection_fieldAccessorTable.ensureFieldAccessorsInitialized(com.qlangtech.tis.rpc.grpc.log.stream.PPhaseStatusCollection.class, com.qlangtech.tis.rpc.grpc.log.stream.PPhaseStatusCollection.Builder.class);
    }

    public static final int DUMPPHASE_FIELD_NUMBER = 1;

    private com.qlangtech.tis.rpc.grpc.log.stream.PDumpPhaseStatus dumpPhase_;

    /**
     * <code>.stream.PDumpPhaseStatus dumpPhase = 1;</code>
     */
    public boolean hasDumpPhase() {
        return dumpPhase_ != null;
    }

    /**
     * <code>.stream.PDumpPhaseStatus dumpPhase = 1;</code>
     */
    public com.qlangtech.tis.rpc.grpc.log.stream.PDumpPhaseStatus getDumpPhase() {
        return dumpPhase_ == null ? com.qlangtech.tis.rpc.grpc.log.stream.PDumpPhaseStatus.getDefaultInstance() : dumpPhase_;
    }

    /**
     * <code>.stream.PDumpPhaseStatus dumpPhase = 1;</code>
     */
    public com.qlangtech.tis.rpc.grpc.log.stream.PDumpPhaseStatusOrBuilder getDumpPhaseOrBuilder() {
        return getDumpPhase();
    }

    public static final int JOINPHASE_FIELD_NUMBER = 2;

    private com.qlangtech.tis.rpc.grpc.log.stream.PJoinPhaseStatus joinPhase_;

    /**
     * <code>.stream.PJoinPhaseStatus joinPhase = 2;</code>
     */
    public boolean hasJoinPhase() {
        return joinPhase_ != null;
    }

    /**
     * <code>.stream.PJoinPhaseStatus joinPhase = 2;</code>
     */
    public com.qlangtech.tis.rpc.grpc.log.stream.PJoinPhaseStatus getJoinPhase() {
        return joinPhase_ == null ? com.qlangtech.tis.rpc.grpc.log.stream.PJoinPhaseStatus.getDefaultInstance() : joinPhase_;
    }

    /**
     * <code>.stream.PJoinPhaseStatus joinPhase = 2;</code>
     */
    public com.qlangtech.tis.rpc.grpc.log.stream.PJoinPhaseStatusOrBuilder getJoinPhaseOrBuilder() {
        return getJoinPhase();
    }

    public static final int BUILDPHASE_FIELD_NUMBER = 3;

    private com.qlangtech.tis.rpc.grpc.log.stream.PBuildPhaseStatus buildPhase_;

    /**
     * <code>.stream.PBuildPhaseStatus buildPhase = 3;</code>
     */
    public boolean hasBuildPhase() {
        return buildPhase_ != null;
    }

    /**
     * <code>.stream.PBuildPhaseStatus buildPhase = 3;</code>
     */
    public com.qlangtech.tis.rpc.grpc.log.stream.PBuildPhaseStatus getBuildPhase() {
        return buildPhase_ == null ? com.qlangtech.tis.rpc.grpc.log.stream.PBuildPhaseStatus.getDefaultInstance() : buildPhase_;
    }

    /**
     * <code>.stream.PBuildPhaseStatus buildPhase = 3;</code>
     */
    public com.qlangtech.tis.rpc.grpc.log.stream.PBuildPhaseStatusOrBuilder getBuildPhaseOrBuilder() {
        return getBuildPhase();
    }

    public static final int INDEXBACKFLOWPHASESTATUS_FIELD_NUMBER = 4;

    private com.qlangtech.tis.rpc.grpc.log.stream.PIndexBackFlowPhaseStatus indexBackFlowPhaseStatus_;

    /**
     * <code>.stream.PIndexBackFlowPhaseStatus indexBackFlowPhaseStatus = 4;</code>
     */
    public boolean hasIndexBackFlowPhaseStatus() {
        return indexBackFlowPhaseStatus_ != null;
    }

    /**
     * <code>.stream.PIndexBackFlowPhaseStatus indexBackFlowPhaseStatus = 4;</code>
     */
    public com.qlangtech.tis.rpc.grpc.log.stream.PIndexBackFlowPhaseStatus getIndexBackFlowPhaseStatus() {
        return indexBackFlowPhaseStatus_ == null ? com.qlangtech.tis.rpc.grpc.log.stream.PIndexBackFlowPhaseStatus.getDefaultInstance() : indexBackFlowPhaseStatus_;
    }

    /**
     * <code>.stream.PIndexBackFlowPhaseStatus indexBackFlowPhaseStatus = 4;</code>
     */
    public com.qlangtech.tis.rpc.grpc.log.stream.PIndexBackFlowPhaseStatusOrBuilder getIndexBackFlowPhaseStatusOrBuilder() {
        return getIndexBackFlowPhaseStatus();
    }

    public static final int TASKID_FIELD_NUMBER = 5;

    private int taskId_;

    /**
     * <code>uint32 taskId = 5;</code>
     */
    public int getTaskId() {
        return taskId_;
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
        if (dumpPhase_ != null) {
            output.writeMessage(1, getDumpPhase());
        }
        if (joinPhase_ != null) {
            output.writeMessage(2, getJoinPhase());
        }
        if (buildPhase_ != null) {
            output.writeMessage(3, getBuildPhase());
        }
        if (indexBackFlowPhaseStatus_ != null) {
            output.writeMessage(4, getIndexBackFlowPhaseStatus());
        }
        if (taskId_ != 0) {
            output.writeUInt32(5, taskId_);
        }
        unknownFields.writeTo(output);
    }

    @java.lang.Override
    public int getSerializedSize() {
        int size = memoizedSize;
        if (size != -1)
            return size;
        size = 0;
        if (dumpPhase_ != null) {
            size += com.google.protobuf.CodedOutputStream.computeMessageSize(1, getDumpPhase());
        }
        if (joinPhase_ != null) {
            size += com.google.protobuf.CodedOutputStream.computeMessageSize(2, getJoinPhase());
        }
        if (buildPhase_ != null) {
            size += com.google.protobuf.CodedOutputStream.computeMessageSize(3, getBuildPhase());
        }
        if (indexBackFlowPhaseStatus_ != null) {
            size += com.google.protobuf.CodedOutputStream.computeMessageSize(4, getIndexBackFlowPhaseStatus());
        }
        if (taskId_ != 0) {
            size += com.google.protobuf.CodedOutputStream.computeUInt32Size(5, taskId_);
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
        if (!(obj instanceof com.qlangtech.tis.rpc.grpc.log.stream.PPhaseStatusCollection)) {
            return super.equals(obj);
        }
        com.qlangtech.tis.rpc.grpc.log.stream.PPhaseStatusCollection other = (com.qlangtech.tis.rpc.grpc.log.stream.PPhaseStatusCollection) obj;
        if (hasDumpPhase() != other.hasDumpPhase())
            return false;
        if (hasDumpPhase()) {
            if (!getDumpPhase().equals(other.getDumpPhase()))
                return false;
        }
        if (hasJoinPhase() != other.hasJoinPhase())
            return false;
        if (hasJoinPhase()) {
            if (!getJoinPhase().equals(other.getJoinPhase()))
                return false;
        }
        if (hasBuildPhase() != other.hasBuildPhase())
            return false;
        if (hasBuildPhase()) {
            if (!getBuildPhase().equals(other.getBuildPhase()))
                return false;
        }
        if (hasIndexBackFlowPhaseStatus() != other.hasIndexBackFlowPhaseStatus())
            return false;
        if (hasIndexBackFlowPhaseStatus()) {
            if (!getIndexBackFlowPhaseStatus().equals(other.getIndexBackFlowPhaseStatus()))
                return false;
        }
        if (getTaskId() != other.getTaskId())
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
        if (hasDumpPhase()) {
            hash = (37 * hash) + DUMPPHASE_FIELD_NUMBER;
            hash = (53 * hash) + getDumpPhase().hashCode();
        }
        if (hasJoinPhase()) {
            hash = (37 * hash) + JOINPHASE_FIELD_NUMBER;
            hash = (53 * hash) + getJoinPhase().hashCode();
        }
        if (hasBuildPhase()) {
            hash = (37 * hash) + BUILDPHASE_FIELD_NUMBER;
            hash = (53 * hash) + getBuildPhase().hashCode();
        }
        if (hasIndexBackFlowPhaseStatus()) {
            hash = (37 * hash) + INDEXBACKFLOWPHASESTATUS_FIELD_NUMBER;
            hash = (53 * hash) + getIndexBackFlowPhaseStatus().hashCode();
        }
        hash = (37 * hash) + TASKID_FIELD_NUMBER;
        hash = (53 * hash) + getTaskId();
        hash = (29 * hash) + unknownFields.hashCode();
        memoizedHashCode = hash;
        return hash;
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PPhaseStatusCollection parseFrom(java.nio.ByteBuffer data) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PPhaseStatusCollection parseFrom(java.nio.ByteBuffer data, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PPhaseStatusCollection parseFrom(com.google.protobuf.ByteString data) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PPhaseStatusCollection parseFrom(com.google.protobuf.ByteString data, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PPhaseStatusCollection parseFrom(byte[] data) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PPhaseStatusCollection parseFrom(byte[] data, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PPhaseStatusCollection parseFrom(java.io.InputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PPhaseStatusCollection parseFrom(java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PPhaseStatusCollection parseDelimitedFrom(java.io.InputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PPhaseStatusCollection parseDelimitedFrom(java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input, extensionRegistry);
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PPhaseStatusCollection parseFrom(com.google.protobuf.CodedInputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PPhaseStatusCollection parseFrom(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    @java.lang.Override
    public Builder newBuilderForType() {
        return newBuilder();
    }

    public static Builder newBuilder() {
        return DEFAULT_INSTANCE.toBuilder();
    }

    public static Builder newBuilder(com.qlangtech.tis.rpc.grpc.log.stream.PPhaseStatusCollection prototype) {
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
     * Protobuf type {@code stream.PPhaseStatusCollection}
     */
    public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements // @@protoc_insertion_point(builder_implements:stream.PPhaseStatusCollection)
    com.qlangtech.tis.rpc.grpc.log.stream.PPhaseStatusCollectionOrBuilder {

        public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
            return com.qlangtech.tis.rpc.grpc.log.stream.LogCollectorProto.internal_static_stream_PPhaseStatusCollection_descriptor;
        }

        @java.lang.Override
        protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
            return com.qlangtech.tis.rpc.grpc.log.stream.LogCollectorProto.internal_static_stream_PPhaseStatusCollection_fieldAccessorTable.ensureFieldAccessorsInitialized(com.qlangtech.tis.rpc.grpc.log.stream.PPhaseStatusCollection.class, com.qlangtech.tis.rpc.grpc.log.stream.PPhaseStatusCollection.Builder.class);
        }

        // Construct using com.qlangtech.tis.rpc.grpc.log.stream.PPhaseStatusCollection.newBuilder()
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
            if (dumpPhaseBuilder_ == null) {
                dumpPhase_ = null;
            } else {
                dumpPhase_ = null;
                dumpPhaseBuilder_ = null;
            }
            if (joinPhaseBuilder_ == null) {
                joinPhase_ = null;
            } else {
                joinPhase_ = null;
                joinPhaseBuilder_ = null;
            }
            if (buildPhaseBuilder_ == null) {
                buildPhase_ = null;
            } else {
                buildPhase_ = null;
                buildPhaseBuilder_ = null;
            }
            if (indexBackFlowPhaseStatusBuilder_ == null) {
                indexBackFlowPhaseStatus_ = null;
            } else {
                indexBackFlowPhaseStatus_ = null;
                indexBackFlowPhaseStatusBuilder_ = null;
            }
            taskId_ = 0;
            return this;
        }

        @java.lang.Override
        public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
            return com.qlangtech.tis.rpc.grpc.log.stream.LogCollectorProto.internal_static_stream_PPhaseStatusCollection_descriptor;
        }

        @java.lang.Override
        public com.qlangtech.tis.rpc.grpc.log.stream.PPhaseStatusCollection getDefaultInstanceForType() {
            return com.qlangtech.tis.rpc.grpc.log.stream.PPhaseStatusCollection.getDefaultInstance();
        }

        @java.lang.Override
        public com.qlangtech.tis.rpc.grpc.log.stream.PPhaseStatusCollection build() {
            com.qlangtech.tis.rpc.grpc.log.stream.PPhaseStatusCollection result = buildPartial();
            if (!result.isInitialized()) {
                throw newUninitializedMessageException(result);
            }
            return result;
        }

        @java.lang.Override
        public com.qlangtech.tis.rpc.grpc.log.stream.PPhaseStatusCollection buildPartial() {
            com.qlangtech.tis.rpc.grpc.log.stream.PPhaseStatusCollection result = new com.qlangtech.tis.rpc.grpc.log.stream.PPhaseStatusCollection(this);
            if (dumpPhaseBuilder_ == null) {
                result.dumpPhase_ = dumpPhase_;
            } else {
                result.dumpPhase_ = dumpPhaseBuilder_.build();
            }
            if (joinPhaseBuilder_ == null) {
                result.joinPhase_ = joinPhase_;
            } else {
                result.joinPhase_ = joinPhaseBuilder_.build();
            }
            if (buildPhaseBuilder_ == null) {
                result.buildPhase_ = buildPhase_;
            } else {
                result.buildPhase_ = buildPhaseBuilder_.build();
            }
            if (indexBackFlowPhaseStatusBuilder_ == null) {
                result.indexBackFlowPhaseStatus_ = indexBackFlowPhaseStatus_;
            } else {
                result.indexBackFlowPhaseStatus_ = indexBackFlowPhaseStatusBuilder_.build();
            }
            result.taskId_ = taskId_;
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
            if (other instanceof com.qlangtech.tis.rpc.grpc.log.stream.PPhaseStatusCollection) {
                return mergeFrom((com.qlangtech.tis.rpc.grpc.log.stream.PPhaseStatusCollection) other);
            } else {
                super.mergeFrom(other);
                return this;
            }
        }

        public Builder mergeFrom(com.qlangtech.tis.rpc.grpc.log.stream.PPhaseStatusCollection other) {
            if (other == com.qlangtech.tis.rpc.grpc.log.stream.PPhaseStatusCollection.getDefaultInstance())
                return this;
            if (other.hasDumpPhase()) {
                mergeDumpPhase(other.getDumpPhase());
            }
            if (other.hasJoinPhase()) {
                mergeJoinPhase(other.getJoinPhase());
            }
            if (other.hasBuildPhase()) {
                mergeBuildPhase(other.getBuildPhase());
            }
            if (other.hasIndexBackFlowPhaseStatus()) {
                mergeIndexBackFlowPhaseStatus(other.getIndexBackFlowPhaseStatus());
            }
            if (other.getTaskId() != 0) {
                setTaskId(other.getTaskId());
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
            com.qlangtech.tis.rpc.grpc.log.stream.PPhaseStatusCollection parsedMessage = null;
            try {
                parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
            } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                parsedMessage = (com.qlangtech.tis.rpc.grpc.log.stream.PPhaseStatusCollection) e.getUnfinishedMessage();
                throw e.unwrapIOException();
            } finally {
                if (parsedMessage != null) {
                    mergeFrom(parsedMessage);
                }
            }
            return this;
        }

        private com.qlangtech.tis.rpc.grpc.log.stream.PDumpPhaseStatus dumpPhase_;

        private com.google.protobuf.SingleFieldBuilderV3<com.qlangtech.tis.rpc.grpc.log.stream.PDumpPhaseStatus, com.qlangtech.tis.rpc.grpc.log.stream.PDumpPhaseStatus.Builder, com.qlangtech.tis.rpc.grpc.log.stream.PDumpPhaseStatusOrBuilder> dumpPhaseBuilder_;

        /**
         * <code>.stream.PDumpPhaseStatus dumpPhase = 1;</code>
         */
        public boolean hasDumpPhase() {
            return dumpPhaseBuilder_ != null || dumpPhase_ != null;
        }

        /**
         * <code>.stream.PDumpPhaseStatus dumpPhase = 1;</code>
         */
        public com.qlangtech.tis.rpc.grpc.log.stream.PDumpPhaseStatus getDumpPhase() {
            if (dumpPhaseBuilder_ == null) {
                return dumpPhase_ == null ? com.qlangtech.tis.rpc.grpc.log.stream.PDumpPhaseStatus.getDefaultInstance() : dumpPhase_;
            } else {
                return dumpPhaseBuilder_.getMessage();
            }
        }

        /**
         * <code>.stream.PDumpPhaseStatus dumpPhase = 1;</code>
         */
        public Builder setDumpPhase(com.qlangtech.tis.rpc.grpc.log.stream.PDumpPhaseStatus value) {
            if (dumpPhaseBuilder_ == null) {
                if (value == null) {
                    throw new NullPointerException();
                }
                dumpPhase_ = value;
                onChanged();
            } else {
                dumpPhaseBuilder_.setMessage(value);
            }
            return this;
        }

        /**
         * <code>.stream.PDumpPhaseStatus dumpPhase = 1;</code>
         */
        public Builder setDumpPhase(com.qlangtech.tis.rpc.grpc.log.stream.PDumpPhaseStatus.Builder builderForValue) {
            if (dumpPhaseBuilder_ == null) {
                dumpPhase_ = builderForValue.build();
                onChanged();
            } else {
                dumpPhaseBuilder_.setMessage(builderForValue.build());
            }
            return this;
        }

        /**
         * <code>.stream.PDumpPhaseStatus dumpPhase = 1;</code>
         */
        public Builder mergeDumpPhase(com.qlangtech.tis.rpc.grpc.log.stream.PDumpPhaseStatus value) {
            if (dumpPhaseBuilder_ == null) {
                if (dumpPhase_ != null) {
                    dumpPhase_ = com.qlangtech.tis.rpc.grpc.log.stream.PDumpPhaseStatus.newBuilder(dumpPhase_).mergeFrom(value).buildPartial();
                } else {
                    dumpPhase_ = value;
                }
                onChanged();
            } else {
                dumpPhaseBuilder_.mergeFrom(value);
            }
            return this;
        }

        /**
         * <code>.stream.PDumpPhaseStatus dumpPhase = 1;</code>
         */
        public Builder clearDumpPhase() {
            if (dumpPhaseBuilder_ == null) {
                dumpPhase_ = null;
                onChanged();
            } else {
                dumpPhase_ = null;
                dumpPhaseBuilder_ = null;
            }
            return this;
        }

        /**
         * <code>.stream.PDumpPhaseStatus dumpPhase = 1;</code>
         */
        public com.qlangtech.tis.rpc.grpc.log.stream.PDumpPhaseStatus.Builder getDumpPhaseBuilder() {
            onChanged();
            return getDumpPhaseFieldBuilder().getBuilder();
        }

        /**
         * <code>.stream.PDumpPhaseStatus dumpPhase = 1;</code>
         */
        public com.qlangtech.tis.rpc.grpc.log.stream.PDumpPhaseStatusOrBuilder getDumpPhaseOrBuilder() {
            if (dumpPhaseBuilder_ != null) {
                return dumpPhaseBuilder_.getMessageOrBuilder();
            } else {
                return dumpPhase_ == null ? com.qlangtech.tis.rpc.grpc.log.stream.PDumpPhaseStatus.getDefaultInstance() : dumpPhase_;
            }
        }

        /**
         * <code>.stream.PDumpPhaseStatus dumpPhase = 1;</code>
         */
        private com.google.protobuf.SingleFieldBuilderV3<com.qlangtech.tis.rpc.grpc.log.stream.PDumpPhaseStatus, com.qlangtech.tis.rpc.grpc.log.stream.PDumpPhaseStatus.Builder, com.qlangtech.tis.rpc.grpc.log.stream.PDumpPhaseStatusOrBuilder> getDumpPhaseFieldBuilder() {
            if (dumpPhaseBuilder_ == null) {
                dumpPhaseBuilder_ = new com.google.protobuf.SingleFieldBuilderV3<com.qlangtech.tis.rpc.grpc.log.stream.PDumpPhaseStatus, com.qlangtech.tis.rpc.grpc.log.stream.PDumpPhaseStatus.Builder, com.qlangtech.tis.rpc.grpc.log.stream.PDumpPhaseStatusOrBuilder>(getDumpPhase(), getParentForChildren(), isClean());
                dumpPhase_ = null;
            }
            return dumpPhaseBuilder_;
        }

        private com.qlangtech.tis.rpc.grpc.log.stream.PJoinPhaseStatus joinPhase_;

        private com.google.protobuf.SingleFieldBuilderV3<com.qlangtech.tis.rpc.grpc.log.stream.PJoinPhaseStatus, com.qlangtech.tis.rpc.grpc.log.stream.PJoinPhaseStatus.Builder, com.qlangtech.tis.rpc.grpc.log.stream.PJoinPhaseStatusOrBuilder> joinPhaseBuilder_;

        /**
         * <code>.stream.PJoinPhaseStatus joinPhase = 2;</code>
         */
        public boolean hasJoinPhase() {
            return joinPhaseBuilder_ != null || joinPhase_ != null;
        }

        /**
         * <code>.stream.PJoinPhaseStatus joinPhase = 2;</code>
         */
        public com.qlangtech.tis.rpc.grpc.log.stream.PJoinPhaseStatus getJoinPhase() {
            if (joinPhaseBuilder_ == null) {
                return joinPhase_ == null ? com.qlangtech.tis.rpc.grpc.log.stream.PJoinPhaseStatus.getDefaultInstance() : joinPhase_;
            } else {
                return joinPhaseBuilder_.getMessage();
            }
        }

        /**
         * <code>.stream.PJoinPhaseStatus joinPhase = 2;</code>
         */
        public Builder setJoinPhase(com.qlangtech.tis.rpc.grpc.log.stream.PJoinPhaseStatus value) {
            if (joinPhaseBuilder_ == null) {
                if (value == null) {
                    throw new NullPointerException();
                }
                joinPhase_ = value;
                onChanged();
            } else {
                joinPhaseBuilder_.setMessage(value);
            }
            return this;
        }

        /**
         * <code>.stream.PJoinPhaseStatus joinPhase = 2;</code>
         */
        public Builder setJoinPhase(com.qlangtech.tis.rpc.grpc.log.stream.PJoinPhaseStatus.Builder builderForValue) {
            if (joinPhaseBuilder_ == null) {
                joinPhase_ = builderForValue.build();
                onChanged();
            } else {
                joinPhaseBuilder_.setMessage(builderForValue.build());
            }
            return this;
        }

        /**
         * <code>.stream.PJoinPhaseStatus joinPhase = 2;</code>
         */
        public Builder mergeJoinPhase(com.qlangtech.tis.rpc.grpc.log.stream.PJoinPhaseStatus value) {
            if (joinPhaseBuilder_ == null) {
                if (joinPhase_ != null) {
                    joinPhase_ = com.qlangtech.tis.rpc.grpc.log.stream.PJoinPhaseStatus.newBuilder(joinPhase_).mergeFrom(value).buildPartial();
                } else {
                    joinPhase_ = value;
                }
                onChanged();
            } else {
                joinPhaseBuilder_.mergeFrom(value);
            }
            return this;
        }

        /**
         * <code>.stream.PJoinPhaseStatus joinPhase = 2;</code>
         */
        public Builder clearJoinPhase() {
            if (joinPhaseBuilder_ == null) {
                joinPhase_ = null;
                onChanged();
            } else {
                joinPhase_ = null;
                joinPhaseBuilder_ = null;
            }
            return this;
        }

        /**
         * <code>.stream.PJoinPhaseStatus joinPhase = 2;</code>
         */
        public com.qlangtech.tis.rpc.grpc.log.stream.PJoinPhaseStatus.Builder getJoinPhaseBuilder() {
            onChanged();
            return getJoinPhaseFieldBuilder().getBuilder();
        }

        /**
         * <code>.stream.PJoinPhaseStatus joinPhase = 2;</code>
         */
        public com.qlangtech.tis.rpc.grpc.log.stream.PJoinPhaseStatusOrBuilder getJoinPhaseOrBuilder() {
            if (joinPhaseBuilder_ != null) {
                return joinPhaseBuilder_.getMessageOrBuilder();
            } else {
                return joinPhase_ == null ? com.qlangtech.tis.rpc.grpc.log.stream.PJoinPhaseStatus.getDefaultInstance() : joinPhase_;
            }
        }

        /**
         * <code>.stream.PJoinPhaseStatus joinPhase = 2;</code>
         */
        private com.google.protobuf.SingleFieldBuilderV3<com.qlangtech.tis.rpc.grpc.log.stream.PJoinPhaseStatus, com.qlangtech.tis.rpc.grpc.log.stream.PJoinPhaseStatus.Builder, com.qlangtech.tis.rpc.grpc.log.stream.PJoinPhaseStatusOrBuilder> getJoinPhaseFieldBuilder() {
            if (joinPhaseBuilder_ == null) {
                joinPhaseBuilder_ = new com.google.protobuf.SingleFieldBuilderV3<com.qlangtech.tis.rpc.grpc.log.stream.PJoinPhaseStatus, com.qlangtech.tis.rpc.grpc.log.stream.PJoinPhaseStatus.Builder, com.qlangtech.tis.rpc.grpc.log.stream.PJoinPhaseStatusOrBuilder>(getJoinPhase(), getParentForChildren(), isClean());
                joinPhase_ = null;
            }
            return joinPhaseBuilder_;
        }

        private com.qlangtech.tis.rpc.grpc.log.stream.PBuildPhaseStatus buildPhase_;

        private com.google.protobuf.SingleFieldBuilderV3<com.qlangtech.tis.rpc.grpc.log.stream.PBuildPhaseStatus, com.qlangtech.tis.rpc.grpc.log.stream.PBuildPhaseStatus.Builder, com.qlangtech.tis.rpc.grpc.log.stream.PBuildPhaseStatusOrBuilder> buildPhaseBuilder_;

        /**
         * <code>.stream.PBuildPhaseStatus buildPhase = 3;</code>
         */
        public boolean hasBuildPhase() {
            return buildPhaseBuilder_ != null || buildPhase_ != null;
        }

        /**
         * <code>.stream.PBuildPhaseStatus buildPhase = 3;</code>
         */
        public com.qlangtech.tis.rpc.grpc.log.stream.PBuildPhaseStatus getBuildPhase() {
            if (buildPhaseBuilder_ == null) {
                return buildPhase_ == null ? com.qlangtech.tis.rpc.grpc.log.stream.PBuildPhaseStatus.getDefaultInstance() : buildPhase_;
            } else {
                return buildPhaseBuilder_.getMessage();
            }
        }

        /**
         * <code>.stream.PBuildPhaseStatus buildPhase = 3;</code>
         */
        public Builder setBuildPhase(com.qlangtech.tis.rpc.grpc.log.stream.PBuildPhaseStatus value) {
            if (buildPhaseBuilder_ == null) {
                if (value == null) {
                    throw new NullPointerException();
                }
                buildPhase_ = value;
                onChanged();
            } else {
                buildPhaseBuilder_.setMessage(value);
            }
            return this;
        }

        /**
         * <code>.stream.PBuildPhaseStatus buildPhase = 3;</code>
         */
        public Builder setBuildPhase(com.qlangtech.tis.rpc.grpc.log.stream.PBuildPhaseStatus.Builder builderForValue) {
            if (buildPhaseBuilder_ == null) {
                buildPhase_ = builderForValue.build();
                onChanged();
            } else {
                buildPhaseBuilder_.setMessage(builderForValue.build());
            }
            return this;
        }

        /**
         * <code>.stream.PBuildPhaseStatus buildPhase = 3;</code>
         */
        public Builder mergeBuildPhase(com.qlangtech.tis.rpc.grpc.log.stream.PBuildPhaseStatus value) {
            if (buildPhaseBuilder_ == null) {
                if (buildPhase_ != null) {
                    buildPhase_ = com.qlangtech.tis.rpc.grpc.log.stream.PBuildPhaseStatus.newBuilder(buildPhase_).mergeFrom(value).buildPartial();
                } else {
                    buildPhase_ = value;
                }
                onChanged();
            } else {
                buildPhaseBuilder_.mergeFrom(value);
            }
            return this;
        }

        /**
         * <code>.stream.PBuildPhaseStatus buildPhase = 3;</code>
         */
        public Builder clearBuildPhase() {
            if (buildPhaseBuilder_ == null) {
                buildPhase_ = null;
                onChanged();
            } else {
                buildPhase_ = null;
                buildPhaseBuilder_ = null;
            }
            return this;
        }

        /**
         * <code>.stream.PBuildPhaseStatus buildPhase = 3;</code>
         */
        public com.qlangtech.tis.rpc.grpc.log.stream.PBuildPhaseStatus.Builder getBuildPhaseBuilder() {
            onChanged();
            return getBuildPhaseFieldBuilder().getBuilder();
        }

        /**
         * <code>.stream.PBuildPhaseStatus buildPhase = 3;</code>
         */
        public com.qlangtech.tis.rpc.grpc.log.stream.PBuildPhaseStatusOrBuilder getBuildPhaseOrBuilder() {
            if (buildPhaseBuilder_ != null) {
                return buildPhaseBuilder_.getMessageOrBuilder();
            } else {
                return buildPhase_ == null ? com.qlangtech.tis.rpc.grpc.log.stream.PBuildPhaseStatus.getDefaultInstance() : buildPhase_;
            }
        }

        /**
         * <code>.stream.PBuildPhaseStatus buildPhase = 3;</code>
         */
        private com.google.protobuf.SingleFieldBuilderV3<com.qlangtech.tis.rpc.grpc.log.stream.PBuildPhaseStatus, com.qlangtech.tis.rpc.grpc.log.stream.PBuildPhaseStatus.Builder, com.qlangtech.tis.rpc.grpc.log.stream.PBuildPhaseStatusOrBuilder> getBuildPhaseFieldBuilder() {
            if (buildPhaseBuilder_ == null) {
                buildPhaseBuilder_ = new com.google.protobuf.SingleFieldBuilderV3<com.qlangtech.tis.rpc.grpc.log.stream.PBuildPhaseStatus, com.qlangtech.tis.rpc.grpc.log.stream.PBuildPhaseStatus.Builder, com.qlangtech.tis.rpc.grpc.log.stream.PBuildPhaseStatusOrBuilder>(getBuildPhase(), getParentForChildren(), isClean());
                buildPhase_ = null;
            }
            return buildPhaseBuilder_;
        }

        private com.qlangtech.tis.rpc.grpc.log.stream.PIndexBackFlowPhaseStatus indexBackFlowPhaseStatus_;

        private com.google.protobuf.SingleFieldBuilderV3<com.qlangtech.tis.rpc.grpc.log.stream.PIndexBackFlowPhaseStatus, com.qlangtech.tis.rpc.grpc.log.stream.PIndexBackFlowPhaseStatus.Builder, com.qlangtech.tis.rpc.grpc.log.stream.PIndexBackFlowPhaseStatusOrBuilder> indexBackFlowPhaseStatusBuilder_;

        /**
         * <code>.stream.PIndexBackFlowPhaseStatus indexBackFlowPhaseStatus = 4;</code>
         */
        public boolean hasIndexBackFlowPhaseStatus() {
            return indexBackFlowPhaseStatusBuilder_ != null || indexBackFlowPhaseStatus_ != null;
        }

        /**
         * <code>.stream.PIndexBackFlowPhaseStatus indexBackFlowPhaseStatus = 4;</code>
         */
        public com.qlangtech.tis.rpc.grpc.log.stream.PIndexBackFlowPhaseStatus getIndexBackFlowPhaseStatus() {
            if (indexBackFlowPhaseStatusBuilder_ == null) {
                return indexBackFlowPhaseStatus_ == null ? com.qlangtech.tis.rpc.grpc.log.stream.PIndexBackFlowPhaseStatus.getDefaultInstance() : indexBackFlowPhaseStatus_;
            } else {
                return indexBackFlowPhaseStatusBuilder_.getMessage();
            }
        }

        /**
         * <code>.stream.PIndexBackFlowPhaseStatus indexBackFlowPhaseStatus = 4;</code>
         */
        public Builder setIndexBackFlowPhaseStatus(com.qlangtech.tis.rpc.grpc.log.stream.PIndexBackFlowPhaseStatus value) {
            if (indexBackFlowPhaseStatusBuilder_ == null) {
                if (value == null) {
                    throw new NullPointerException();
                }
                indexBackFlowPhaseStatus_ = value;
                onChanged();
            } else {
                indexBackFlowPhaseStatusBuilder_.setMessage(value);
            }
            return this;
        }

        /**
         * <code>.stream.PIndexBackFlowPhaseStatus indexBackFlowPhaseStatus = 4;</code>
         */
        public Builder setIndexBackFlowPhaseStatus(com.qlangtech.tis.rpc.grpc.log.stream.PIndexBackFlowPhaseStatus.Builder builderForValue) {
            if (indexBackFlowPhaseStatusBuilder_ == null) {
                indexBackFlowPhaseStatus_ = builderForValue.build();
                onChanged();
            } else {
                indexBackFlowPhaseStatusBuilder_.setMessage(builderForValue.build());
            }
            return this;
        }

        /**
         * <code>.stream.PIndexBackFlowPhaseStatus indexBackFlowPhaseStatus = 4;</code>
         */
        public Builder mergeIndexBackFlowPhaseStatus(com.qlangtech.tis.rpc.grpc.log.stream.PIndexBackFlowPhaseStatus value) {
            if (indexBackFlowPhaseStatusBuilder_ == null) {
                if (indexBackFlowPhaseStatus_ != null) {
                    indexBackFlowPhaseStatus_ = com.qlangtech.tis.rpc.grpc.log.stream.PIndexBackFlowPhaseStatus.newBuilder(indexBackFlowPhaseStatus_).mergeFrom(value).buildPartial();
                } else {
                    indexBackFlowPhaseStatus_ = value;
                }
                onChanged();
            } else {
                indexBackFlowPhaseStatusBuilder_.mergeFrom(value);
            }
            return this;
        }

        /**
         * <code>.stream.PIndexBackFlowPhaseStatus indexBackFlowPhaseStatus = 4;</code>
         */
        public Builder clearIndexBackFlowPhaseStatus() {
            if (indexBackFlowPhaseStatusBuilder_ == null) {
                indexBackFlowPhaseStatus_ = null;
                onChanged();
            } else {
                indexBackFlowPhaseStatus_ = null;
                indexBackFlowPhaseStatusBuilder_ = null;
            }
            return this;
        }

        /**
         * <code>.stream.PIndexBackFlowPhaseStatus indexBackFlowPhaseStatus = 4;</code>
         */
        public com.qlangtech.tis.rpc.grpc.log.stream.PIndexBackFlowPhaseStatus.Builder getIndexBackFlowPhaseStatusBuilder() {
            onChanged();
            return getIndexBackFlowPhaseStatusFieldBuilder().getBuilder();
        }

        /**
         * <code>.stream.PIndexBackFlowPhaseStatus indexBackFlowPhaseStatus = 4;</code>
         */
        public com.qlangtech.tis.rpc.grpc.log.stream.PIndexBackFlowPhaseStatusOrBuilder getIndexBackFlowPhaseStatusOrBuilder() {
            if (indexBackFlowPhaseStatusBuilder_ != null) {
                return indexBackFlowPhaseStatusBuilder_.getMessageOrBuilder();
            } else {
                return indexBackFlowPhaseStatus_ == null ? com.qlangtech.tis.rpc.grpc.log.stream.PIndexBackFlowPhaseStatus.getDefaultInstance() : indexBackFlowPhaseStatus_;
            }
        }

        /**
         * <code>.stream.PIndexBackFlowPhaseStatus indexBackFlowPhaseStatus = 4;</code>
         */
        private com.google.protobuf.SingleFieldBuilderV3<com.qlangtech.tis.rpc.grpc.log.stream.PIndexBackFlowPhaseStatus, com.qlangtech.tis.rpc.grpc.log.stream.PIndexBackFlowPhaseStatus.Builder, com.qlangtech.tis.rpc.grpc.log.stream.PIndexBackFlowPhaseStatusOrBuilder> getIndexBackFlowPhaseStatusFieldBuilder() {
            if (indexBackFlowPhaseStatusBuilder_ == null) {
                indexBackFlowPhaseStatusBuilder_ = new com.google.protobuf.SingleFieldBuilderV3<com.qlangtech.tis.rpc.grpc.log.stream.PIndexBackFlowPhaseStatus, com.qlangtech.tis.rpc.grpc.log.stream.PIndexBackFlowPhaseStatus.Builder, com.qlangtech.tis.rpc.grpc.log.stream.PIndexBackFlowPhaseStatusOrBuilder>(getIndexBackFlowPhaseStatus(), getParentForChildren(), isClean());
                indexBackFlowPhaseStatus_ = null;
            }
            return indexBackFlowPhaseStatusBuilder_;
        }

        private int taskId_;

        /**
         * <code>uint32 taskId = 5;</code>
         */
        public int getTaskId() {
            return taskId_;
        }

        /**
         * <code>uint32 taskId = 5;</code>
         */
        public Builder setTaskId(int value) {
            taskId_ = value;
            onChanged();
            return this;
        }

        /**
         * <code>uint32 taskId = 5;</code>
         */
        public Builder clearTaskId() {
            taskId_ = 0;
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
        // @@protoc_insertion_point(builder_scope:stream.PPhaseStatusCollection)
    }

    // @@protoc_insertion_point(class_scope:stream.PPhaseStatusCollection)
    private static final com.qlangtech.tis.rpc.grpc.log.stream.PPhaseStatusCollection DEFAULT_INSTANCE;

    static {
        DEFAULT_INSTANCE = new com.qlangtech.tis.rpc.grpc.log.stream.PPhaseStatusCollection();
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PPhaseStatusCollection getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    private static final com.google.protobuf.Parser<PPhaseStatusCollection> PARSER = new com.google.protobuf.AbstractParser<PPhaseStatusCollection>() {

        @java.lang.Override
        public PPhaseStatusCollection parsePartialFrom(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
            return new PPhaseStatusCollection(input, extensionRegistry);
        }
    };

    public static com.google.protobuf.Parser<PPhaseStatusCollection> parser() {
        return PARSER;
    }

    @java.lang.Override
    public com.google.protobuf.Parser<PPhaseStatusCollection> getParserForType() {
        return PARSER;
    }

    @java.lang.Override
    public com.qlangtech.tis.rpc.grpc.log.stream.PPhaseStatusCollection getDefaultInstanceForType() {
        return DEFAULT_INSTANCE;
    }
}
