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
 * Protobuf type {@code stream.PMonotorTarget}
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/09/25
 */
public final class PMonotorTarget extends com.google.protobuf.GeneratedMessageV3 implements // @@protoc_insertion_point(message_implements:stream.PMonotorTarget)
PMonotorTargetOrBuilder {

    private static final long serialVersionUID = 0L;

    // Use PMonotorTarget.newBuilder() to construct.
    private PMonotorTarget(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
        super(builder);
    }

    private PMonotorTarget() {
        collection_ = "";
        logtype_ = 0;
    }

    @java.lang.Override
    public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
        return this.unknownFields;
    }

    private PMonotorTarget(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
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
                            collection_ = s;
                            break;
                        }
                    case 16:
                        {
                            taskid_ = input.readUInt32();
                            break;
                        }
                    case 24:
                        {
                            int rawValue = input.readEnum();
                            logtype_ = rawValue;
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
        return com.qlangtech.tis.rpc.grpc.log.stream.LogCollectorProto.internal_static_stream_PMonotorTarget_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
        return com.qlangtech.tis.rpc.grpc.log.stream.LogCollectorProto.internal_static_stream_PMonotorTarget_fieldAccessorTable.ensureFieldAccessorsInitialized(com.qlangtech.tis.rpc.grpc.log.stream.PMonotorTarget.class, com.qlangtech.tis.rpc.grpc.log.stream.PMonotorTarget.Builder.class);
    }

    public static final int COLLECTION_FIELD_NUMBER = 1;

    private volatile java.lang.Object collection_;

    /**
     * <code>string collection = 1;</code>
     */
    public java.lang.String getCollection() {
        java.lang.Object ref = collection_;
        if (ref instanceof java.lang.String) {
            return (java.lang.String) ref;
        } else {
            com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
            java.lang.String s = bs.toStringUtf8();
            collection_ = s;
            return s;
        }
    }

    /**
     * <code>string collection = 1;</code>
     */
    public com.google.protobuf.ByteString getCollectionBytes() {
        java.lang.Object ref = collection_;
        if (ref instanceof java.lang.String) {
            com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((java.lang.String) ref);
            collection_ = b;
            return b;
        } else {
            return (com.google.protobuf.ByteString) ref;
        }
    }

    public static final int TASKID_FIELD_NUMBER = 2;

    private int taskid_;

    /**
     * <code>uint32 taskid = 2;</code>
     */
    public int getTaskid() {
        return taskid_;
    }

    public static final int LOGTYPE_FIELD_NUMBER = 3;

    private int logtype_;

    /**
     * <code>.stream.PExecuteState.LogType logtype = 3;</code>
     */
    public int getLogtypeValue() {
        return logtype_;
    }

    /**
     * <code>.stream.PExecuteState.LogType logtype = 3;</code>
     */
    public com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState.LogType getLogtype() {
        @SuppressWarnings("deprecation")
        com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState.LogType result = com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState.LogType.valueOf(logtype_);
        return result == null ? com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState.LogType.UNRECOGNIZED : result;
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
        if (!getCollectionBytes().isEmpty()) {
            com.google.protobuf.GeneratedMessageV3.writeString(output, 1, collection_);
        }
        if (taskid_ != 0) {
            output.writeUInt32(2, taskid_);
        }
        if (logtype_ != com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState.LogType.INCR_DEPLOY_STATUS_CHANGE.getNumber()) {
            output.writeEnum(3, logtype_);
        }
        unknownFields.writeTo(output);
    }

    @java.lang.Override
    public int getSerializedSize() {
        int size = memoizedSize;
        if (size != -1)
            return size;
        size = 0;
        if (!getCollectionBytes().isEmpty()) {
            size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, collection_);
        }
        if (taskid_ != 0) {
            size += com.google.protobuf.CodedOutputStream.computeUInt32Size(2, taskid_);
        }
        if (logtype_ != com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState.LogType.INCR_DEPLOY_STATUS_CHANGE.getNumber()) {
            size += com.google.protobuf.CodedOutputStream.computeEnumSize(3, logtype_);
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
        if (!(obj instanceof com.qlangtech.tis.rpc.grpc.log.stream.PMonotorTarget)) {
            return super.equals(obj);
        }
        com.qlangtech.tis.rpc.grpc.log.stream.PMonotorTarget other = (com.qlangtech.tis.rpc.grpc.log.stream.PMonotorTarget) obj;
        if (!getCollection().equals(other.getCollection()))
            return false;
        if (getTaskid() != other.getTaskid())
            return false;
        if (logtype_ != other.logtype_)
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
        hash = (37 * hash) + COLLECTION_FIELD_NUMBER;
        hash = (53 * hash) + getCollection().hashCode();
        hash = (37 * hash) + TASKID_FIELD_NUMBER;
        hash = (53 * hash) + getTaskid();
        hash = (37 * hash) + LOGTYPE_FIELD_NUMBER;
        hash = (53 * hash) + logtype_;
        hash = (29 * hash) + unknownFields.hashCode();
        memoizedHashCode = hash;
        return hash;
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PMonotorTarget parseFrom(java.nio.ByteBuffer data) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PMonotorTarget parseFrom(java.nio.ByteBuffer data, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PMonotorTarget parseFrom(com.google.protobuf.ByteString data) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PMonotorTarget parseFrom(com.google.protobuf.ByteString data, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PMonotorTarget parseFrom(byte[] data) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PMonotorTarget parseFrom(byte[] data, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PMonotorTarget parseFrom(java.io.InputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PMonotorTarget parseFrom(java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PMonotorTarget parseDelimitedFrom(java.io.InputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PMonotorTarget parseDelimitedFrom(java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input, extensionRegistry);
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PMonotorTarget parseFrom(com.google.protobuf.CodedInputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PMonotorTarget parseFrom(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    @java.lang.Override
    public Builder newBuilderForType() {
        return newBuilder();
    }

    public static Builder newBuilder() {
        return DEFAULT_INSTANCE.toBuilder();
    }

    public static Builder newBuilder(com.qlangtech.tis.rpc.grpc.log.stream.PMonotorTarget prototype) {
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
     * Protobuf type {@code stream.PMonotorTarget}
     */
    public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements // @@protoc_insertion_point(builder_implements:stream.PMonotorTarget)
    com.qlangtech.tis.rpc.grpc.log.stream.PMonotorTargetOrBuilder {

        public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
            return com.qlangtech.tis.rpc.grpc.log.stream.LogCollectorProto.internal_static_stream_PMonotorTarget_descriptor;
        }

        @java.lang.Override
        protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
            return com.qlangtech.tis.rpc.grpc.log.stream.LogCollectorProto.internal_static_stream_PMonotorTarget_fieldAccessorTable.ensureFieldAccessorsInitialized(com.qlangtech.tis.rpc.grpc.log.stream.PMonotorTarget.class, com.qlangtech.tis.rpc.grpc.log.stream.PMonotorTarget.Builder.class);
        }

        // Construct using com.qlangtech.tis.rpc.grpc.log.stream.PMonotorTarget.newBuilder()
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
            collection_ = "";
            taskid_ = 0;
            logtype_ = 0;
            return this;
        }

        @java.lang.Override
        public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
            return com.qlangtech.tis.rpc.grpc.log.stream.LogCollectorProto.internal_static_stream_PMonotorTarget_descriptor;
        }

        @java.lang.Override
        public com.qlangtech.tis.rpc.grpc.log.stream.PMonotorTarget getDefaultInstanceForType() {
            return com.qlangtech.tis.rpc.grpc.log.stream.PMonotorTarget.getDefaultInstance();
        }

        @java.lang.Override
        public com.qlangtech.tis.rpc.grpc.log.stream.PMonotorTarget build() {
            com.qlangtech.tis.rpc.grpc.log.stream.PMonotorTarget result = buildPartial();
            if (!result.isInitialized()) {
                throw newUninitializedMessageException(result);
            }
            return result;
        }

        @java.lang.Override
        public com.qlangtech.tis.rpc.grpc.log.stream.PMonotorTarget buildPartial() {
            com.qlangtech.tis.rpc.grpc.log.stream.PMonotorTarget result = new com.qlangtech.tis.rpc.grpc.log.stream.PMonotorTarget(this);
            result.collection_ = collection_;
            result.taskid_ = taskid_;
            result.logtype_ = logtype_;
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
            if (other instanceof com.qlangtech.tis.rpc.grpc.log.stream.PMonotorTarget) {
                return mergeFrom((com.qlangtech.tis.rpc.grpc.log.stream.PMonotorTarget) other);
            } else {
                super.mergeFrom(other);
                return this;
            }
        }

        public Builder mergeFrom(com.qlangtech.tis.rpc.grpc.log.stream.PMonotorTarget other) {
            if (other == com.qlangtech.tis.rpc.grpc.log.stream.PMonotorTarget.getDefaultInstance())
                return this;
            if (!other.getCollection().isEmpty()) {
                collection_ = other.collection_;
                onChanged();
            }
            if (other.getTaskid() != 0) {
                setTaskid(other.getTaskid());
            }
            if (other.logtype_ != 0) {
                setLogtypeValue(other.getLogtypeValue());
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
            com.qlangtech.tis.rpc.grpc.log.stream.PMonotorTarget parsedMessage = null;
            try {
                parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
            } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                parsedMessage = (com.qlangtech.tis.rpc.grpc.log.stream.PMonotorTarget) e.getUnfinishedMessage();
                throw e.unwrapIOException();
            } finally {
                if (parsedMessage != null) {
                    mergeFrom(parsedMessage);
                }
            }
            return this;
        }

        private java.lang.Object collection_ = "";

        /**
         * <code>string collection = 1;</code>
         */
        public java.lang.String getCollection() {
            java.lang.Object ref = collection_;
            if (!(ref instanceof java.lang.String)) {
                com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
                java.lang.String s = bs.toStringUtf8();
                collection_ = s;
                return s;
            } else {
                return (java.lang.String) ref;
            }
        }

        /**
         * <code>string collection = 1;</code>
         */
        public com.google.protobuf.ByteString getCollectionBytes() {
            java.lang.Object ref = collection_;
            if (ref instanceof String) {
                com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((java.lang.String) ref);
                collection_ = b;
                return b;
            } else {
                return (com.google.protobuf.ByteString) ref;
            }
        }

        /**
         * <code>string collection = 1;</code>
         */
        public Builder setCollection(java.lang.String value) {
            if (value == null) {
                throw new NullPointerException();
            }
            collection_ = value;
            onChanged();
            return this;
        }

        /**
         * <code>string collection = 1;</code>
         */
        public Builder clearCollection() {
            collection_ = getDefaultInstance().getCollection();
            onChanged();
            return this;
        }

        /**
         * <code>string collection = 1;</code>
         */
        public Builder setCollectionBytes(com.google.protobuf.ByteString value) {
            if (value == null) {
                throw new NullPointerException();
            }
            checkByteStringIsUtf8(value);
            collection_ = value;
            onChanged();
            return this;
        }

        private int taskid_;

        /**
         * <code>uint32 taskid = 2;</code>
         */
        public int getTaskid() {
            return taskid_;
        }

        /**
         * <code>uint32 taskid = 2;</code>
         */
        public Builder setTaskid(int value) {
            taskid_ = value;
            onChanged();
            return this;
        }

        /**
         * <code>uint32 taskid = 2;</code>
         */
        public Builder clearTaskid() {
            taskid_ = 0;
            onChanged();
            return this;
        }

        private int logtype_ = 0;

        /**
         * <code>.stream.PExecuteState.LogType logtype = 3;</code>
         */
        public int getLogtypeValue() {
            return logtype_;
        }

        /**
         * <code>.stream.PExecuteState.LogType logtype = 3;</code>
         */
        public Builder setLogtypeValue(int value) {
            logtype_ = value;
            onChanged();
            return this;
        }

        /**
         * <code>.stream.PExecuteState.LogType logtype = 3;</code>
         */
        public com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState.LogType getLogtype() {
            @SuppressWarnings("deprecation")
            com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState.LogType result = com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState.LogType.valueOf(logtype_);
            return result == null ? com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState.LogType.UNRECOGNIZED : result;
        }

        /**
         * <code>.stream.PExecuteState.LogType logtype = 3;</code>
         */
        public Builder setLogtype(com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState.LogType value) {
            if (value == null) {
                throw new NullPointerException();
            }
            logtype_ = value.getNumber();
            onChanged();
            return this;
        }

        /**
         * <code>.stream.PExecuteState.LogType logtype = 3;</code>
         */
        public Builder clearLogtype() {
            logtype_ = 0;
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
        // @@protoc_insertion_point(builder_scope:stream.PMonotorTarget)
    }

    // @@protoc_insertion_point(class_scope:stream.PMonotorTarget)
    private static final com.qlangtech.tis.rpc.grpc.log.stream.PMonotorTarget DEFAULT_INSTANCE;

    static {
        DEFAULT_INSTANCE = new com.qlangtech.tis.rpc.grpc.log.stream.PMonotorTarget();
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PMonotorTarget getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    private static final com.google.protobuf.Parser<PMonotorTarget> PARSER = new com.google.protobuf.AbstractParser<PMonotorTarget>() {

        @java.lang.Override
        public PMonotorTarget parsePartialFrom(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
            return new PMonotorTarget(input, extensionRegistry);
        }
    };

    public static com.google.protobuf.Parser<PMonotorTarget> parser() {
        return PARSER;
    }

    @java.lang.Override
    public com.google.protobuf.Parser<PMonotorTarget> getParserForType() {
        return PARSER;
    }

    @java.lang.Override
    public com.qlangtech.tis.rpc.grpc.log.stream.PMonotorTarget getDefaultInstanceForType() {
        return DEFAULT_INSTANCE;
    }
}
