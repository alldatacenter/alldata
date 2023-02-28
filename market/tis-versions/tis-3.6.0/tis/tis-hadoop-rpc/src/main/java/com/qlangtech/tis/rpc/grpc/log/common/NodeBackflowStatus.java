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
 * Protobuf type {@code NodeBackflowStatus}
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/09/25
 */
public final class NodeBackflowStatus extends com.google.protobuf.GeneratedMessageV3 implements // @@protoc_insertion_point(message_implements:NodeBackflowStatus)
NodeBackflowStatusOrBuilder {

    private static final long serialVersionUID = 0L;

    // Use NodeBackflowStatus.newBuilder() to construct.
    private NodeBackflowStatus(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
        super(builder);
    }

    private NodeBackflowStatus() {
        nodeName_ = "";
    }

    @java.lang.Override
    public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
        return this.unknownFields;
    }

    private NodeBackflowStatus(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
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
                            nodeName_ = s;
                            break;
                        }
                    case 16:
                        {
                            allSize_ = input.readUInt64();
                            break;
                        }
                    case 24:
                        {
                            readed_ = input.readUInt64();
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
        return com.qlangtech.tis.rpc.grpc.log.common.LogCollectorProto.internal_static_NodeBackflowStatus_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
        return com.qlangtech.tis.rpc.grpc.log.common.LogCollectorProto.internal_static_NodeBackflowStatus_fieldAccessorTable.ensureFieldAccessorsInitialized(com.qlangtech.tis.rpc.grpc.log.common.NodeBackflowStatus.class, com.qlangtech.tis.rpc.grpc.log.common.NodeBackflowStatus.Builder.class);
    }

    public static final int NODENAME_FIELD_NUMBER = 1;

    private volatile java.lang.Object nodeName_;

    /**
     * <code>string nodeName = 1;</code>
     */
    public java.lang.String getNodeName() {
        java.lang.Object ref = nodeName_;
        if (ref instanceof java.lang.String) {
            return (java.lang.String) ref;
        } else {
            com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
            java.lang.String s = bs.toStringUtf8();
            nodeName_ = s;
            return s;
        }
    }

    /**
     * <code>string nodeName = 1;</code>
     */
    public com.google.protobuf.ByteString getNodeNameBytes() {
        java.lang.Object ref = nodeName_;
        if (ref instanceof java.lang.String) {
            com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((java.lang.String) ref);
            nodeName_ = b;
            return b;
        } else {
            return (com.google.protobuf.ByteString) ref;
        }
    }

    public static final int ALLSIZE_FIELD_NUMBER = 2;

    private long allSize_;

    /**
     * <code>uint64 allSize = 2;</code>
     */
    public long getAllSize() {
        return allSize_;
    }

    public static final int READED_FIELD_NUMBER = 3;

    private long readed_;

    /**
     * <code>uint64 readed = 3;</code>
     */
    public long getReaded() {
        return readed_;
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
        if (!getNodeNameBytes().isEmpty()) {
            com.google.protobuf.GeneratedMessageV3.writeString(output, 1, nodeName_);
        }
        if (allSize_ != 0L) {
            output.writeUInt64(2, allSize_);
        }
        if (readed_ != 0L) {
            output.writeUInt64(3, readed_);
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
        if (!getNodeNameBytes().isEmpty()) {
            size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, nodeName_);
        }
        if (allSize_ != 0L) {
            size += com.google.protobuf.CodedOutputStream.computeUInt64Size(2, allSize_);
        }
        if (readed_ != 0L) {
            size += com.google.protobuf.CodedOutputStream.computeUInt64Size(3, readed_);
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
        if (!(obj instanceof com.qlangtech.tis.rpc.grpc.log.common.NodeBackflowStatus)) {
            return super.equals(obj);
        }
        com.qlangtech.tis.rpc.grpc.log.common.NodeBackflowStatus other = (com.qlangtech.tis.rpc.grpc.log.common.NodeBackflowStatus) obj;
        if (!getNodeName().equals(other.getNodeName()))
            return false;
        if (getAllSize() != other.getAllSize())
            return false;
        if (getReaded() != other.getReaded())
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
        hash = (37 * hash) + NODENAME_FIELD_NUMBER;
        hash = (53 * hash) + getNodeName().hashCode();
        hash = (37 * hash) + ALLSIZE_FIELD_NUMBER;
        hash = (53 * hash) + com.google.protobuf.Internal.hashLong(getAllSize());
        hash = (37 * hash) + READED_FIELD_NUMBER;
        hash = (53 * hash) + com.google.protobuf.Internal.hashLong(getReaded());
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

    public static com.qlangtech.tis.rpc.grpc.log.common.NodeBackflowStatus parseFrom(java.nio.ByteBuffer data) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static com.qlangtech.tis.rpc.grpc.log.common.NodeBackflowStatus parseFrom(java.nio.ByteBuffer data, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static com.qlangtech.tis.rpc.grpc.log.common.NodeBackflowStatus parseFrom(com.google.protobuf.ByteString data) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static com.qlangtech.tis.rpc.grpc.log.common.NodeBackflowStatus parseFrom(com.google.protobuf.ByteString data, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static com.qlangtech.tis.rpc.grpc.log.common.NodeBackflowStatus parseFrom(byte[] data) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static com.qlangtech.tis.rpc.grpc.log.common.NodeBackflowStatus parseFrom(byte[] data, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static com.qlangtech.tis.rpc.grpc.log.common.NodeBackflowStatus parseFrom(java.io.InputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static com.qlangtech.tis.rpc.grpc.log.common.NodeBackflowStatus parseFrom(java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static com.qlangtech.tis.rpc.grpc.log.common.NodeBackflowStatus parseDelimitedFrom(java.io.InputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
    }

    public static com.qlangtech.tis.rpc.grpc.log.common.NodeBackflowStatus parseDelimitedFrom(java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input, extensionRegistry);
    }

    public static com.qlangtech.tis.rpc.grpc.log.common.NodeBackflowStatus parseFrom(com.google.protobuf.CodedInputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static com.qlangtech.tis.rpc.grpc.log.common.NodeBackflowStatus parseFrom(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    @java.lang.Override
    public Builder newBuilderForType() {
        return newBuilder();
    }

    public static Builder newBuilder() {
        return DEFAULT_INSTANCE.toBuilder();
    }

    public static Builder newBuilder(com.qlangtech.tis.rpc.grpc.log.common.NodeBackflowStatus prototype) {
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
     * Protobuf type {@code NodeBackflowStatus}
     */
    public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements // @@protoc_insertion_point(builder_implements:NodeBackflowStatus)
    com.qlangtech.tis.rpc.grpc.log.common.NodeBackflowStatusOrBuilder {

        public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
            return com.qlangtech.tis.rpc.grpc.log.common.LogCollectorProto.internal_static_NodeBackflowStatus_descriptor;
        }

        @java.lang.Override
        protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
            return com.qlangtech.tis.rpc.grpc.log.common.LogCollectorProto.internal_static_NodeBackflowStatus_fieldAccessorTable.ensureFieldAccessorsInitialized(com.qlangtech.tis.rpc.grpc.log.common.NodeBackflowStatus.class, com.qlangtech.tis.rpc.grpc.log.common.NodeBackflowStatus.Builder.class);
        }

        // Construct using com.qlangtech.tis.rpc.grpc.log.common.NodeBackflowStatus.newBuilder()
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
            nodeName_ = "";
            allSize_ = 0L;
            readed_ = 0L;
            faild_ = false;
            complete_ = false;
            waiting_ = false;
            return this;
        }

        @java.lang.Override
        public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
            return com.qlangtech.tis.rpc.grpc.log.common.LogCollectorProto.internal_static_NodeBackflowStatus_descriptor;
        }

        @java.lang.Override
        public com.qlangtech.tis.rpc.grpc.log.common.NodeBackflowStatus getDefaultInstanceForType() {
            return com.qlangtech.tis.rpc.grpc.log.common.NodeBackflowStatus.getDefaultInstance();
        }

        @java.lang.Override
        public com.qlangtech.tis.rpc.grpc.log.common.NodeBackflowStatus build() {
            com.qlangtech.tis.rpc.grpc.log.common.NodeBackflowStatus result = buildPartial();
            if (!result.isInitialized()) {
                throw newUninitializedMessageException(result);
            }
            return result;
        }

        @java.lang.Override
        public com.qlangtech.tis.rpc.grpc.log.common.NodeBackflowStatus buildPartial() {
            com.qlangtech.tis.rpc.grpc.log.common.NodeBackflowStatus result = new com.qlangtech.tis.rpc.grpc.log.common.NodeBackflowStatus(this);
            result.nodeName_ = nodeName_;
            result.allSize_ = allSize_;
            result.readed_ = readed_;
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
            if (other instanceof com.qlangtech.tis.rpc.grpc.log.common.NodeBackflowStatus) {
                return mergeFrom((com.qlangtech.tis.rpc.grpc.log.common.NodeBackflowStatus) other);
            } else {
                super.mergeFrom(other);
                return this;
            }
        }

        public Builder mergeFrom(com.qlangtech.tis.rpc.grpc.log.common.NodeBackflowStatus other) {
            if (other == com.qlangtech.tis.rpc.grpc.log.common.NodeBackflowStatus.getDefaultInstance())
                return this;
            if (!other.getNodeName().isEmpty()) {
                nodeName_ = other.nodeName_;
                onChanged();
            }
            if (other.getAllSize() != 0L) {
                setAllSize(other.getAllSize());
            }
            if (other.getReaded() != 0L) {
                setReaded(other.getReaded());
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
            com.qlangtech.tis.rpc.grpc.log.common.NodeBackflowStatus parsedMessage = null;
            try {
                parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
            } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                parsedMessage = (com.qlangtech.tis.rpc.grpc.log.common.NodeBackflowStatus) e.getUnfinishedMessage();
                throw e.unwrapIOException();
            } finally {
                if (parsedMessage != null) {
                    mergeFrom(parsedMessage);
                }
            }
            return this;
        }

        private java.lang.Object nodeName_ = "";

        /**
         * <code>string nodeName = 1;</code>
         */
        public java.lang.String getNodeName() {
            java.lang.Object ref = nodeName_;
            if (!(ref instanceof java.lang.String)) {
                com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
                java.lang.String s = bs.toStringUtf8();
                nodeName_ = s;
                return s;
            } else {
                return (java.lang.String) ref;
            }
        }

        /**
         * <code>string nodeName = 1;</code>
         */
        public com.google.protobuf.ByteString getNodeNameBytes() {
            java.lang.Object ref = nodeName_;
            if (ref instanceof String) {
                com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((java.lang.String) ref);
                nodeName_ = b;
                return b;
            } else {
                return (com.google.protobuf.ByteString) ref;
            }
        }

        /**
         * <code>string nodeName = 1;</code>
         */
        public Builder setNodeName(java.lang.String value) {
            if (value == null) {
                throw new NullPointerException();
            }
            nodeName_ = value;
            onChanged();
            return this;
        }

        /**
         * <code>string nodeName = 1;</code>
         */
        public Builder clearNodeName() {
            nodeName_ = getDefaultInstance().getNodeName();
            onChanged();
            return this;
        }

        /**
         * <code>string nodeName = 1;</code>
         */
        public Builder setNodeNameBytes(com.google.protobuf.ByteString value) {
            if (value == null) {
                throw new NullPointerException();
            }
            checkByteStringIsUtf8(value);
            nodeName_ = value;
            onChanged();
            return this;
        }

        private long allSize_;

        /**
         * <code>uint64 allSize = 2;</code>
         */
        public long getAllSize() {
            return allSize_;
        }

        /**
         * <code>uint64 allSize = 2;</code>
         */
        public Builder setAllSize(long value) {
            allSize_ = value;
            onChanged();
            return this;
        }

        /**
         * <code>uint64 allSize = 2;</code>
         */
        public Builder clearAllSize() {
            allSize_ = 0L;
            onChanged();
            return this;
        }

        private long readed_;

        /**
         * <code>uint64 readed = 3;</code>
         */
        public long getReaded() {
            return readed_;
        }

        /**
         * <code>uint64 readed = 3;</code>
         */
        public Builder setReaded(long value) {
            readed_ = value;
            onChanged();
            return this;
        }

        /**
         * <code>uint64 readed = 3;</code>
         */
        public Builder clearReaded() {
            readed_ = 0L;
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
        // @@protoc_insertion_point(builder_scope:NodeBackflowStatus)
    }

    // @@protoc_insertion_point(class_scope:NodeBackflowStatus)
    private static final com.qlangtech.tis.rpc.grpc.log.common.NodeBackflowStatus DEFAULT_INSTANCE;

    static {
        DEFAULT_INSTANCE = new com.qlangtech.tis.rpc.grpc.log.common.NodeBackflowStatus();
    }

    public static com.qlangtech.tis.rpc.grpc.log.common.NodeBackflowStatus getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    private static final com.google.protobuf.Parser<NodeBackflowStatus> PARSER = new com.google.protobuf.AbstractParser<NodeBackflowStatus>() {

        @java.lang.Override
        public NodeBackflowStatus parsePartialFrom(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
            return new NodeBackflowStatus(input, extensionRegistry);
        }
    };

    public static com.google.protobuf.Parser<NodeBackflowStatus> parser() {
        return PARSER;
    }

    @java.lang.Override
    public com.google.protobuf.Parser<NodeBackflowStatus> getParserForType() {
        return PARSER;
    }

    @java.lang.Override
    public com.qlangtech.tis.rpc.grpc.log.common.NodeBackflowStatus getDefaultInstanceForType() {
        return DEFAULT_INSTANCE;
    }
}
