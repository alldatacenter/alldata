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
 * Protobuf type {@code JobLog}
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/09/25
 */
public final class JobLog extends com.google.protobuf.GeneratedMessageV3 implements // @@protoc_insertion_point(message_implements:JobLog)
JobLogOrBuilder {

    private static final long serialVersionUID = 0L;

    // Use JobLog.newBuilder() to construct.
    private JobLog(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
        super(builder);
    }

    private JobLog() {
    }

    @java.lang.Override
    public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
        return this.unknownFields;
    }

    private JobLog(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
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
                            waiting_ = input.readBool();
                            break;
                        }
                    case 16:
                        {
                            mapper_ = input.readUInt32();
                            break;
                        }
                    case 24:
                        {
                            reducer_ = input.readUInt32();
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
        return com.qlangtech.tis.rpc.grpc.log.common.LogCollectorProto.internal_static_JobLog_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
        return com.qlangtech.tis.rpc.grpc.log.common.LogCollectorProto.internal_static_JobLog_fieldAccessorTable.ensureFieldAccessorsInitialized(com.qlangtech.tis.rpc.grpc.log.common.JobLog.class, com.qlangtech.tis.rpc.grpc.log.common.JobLog.Builder.class);
    }

    public static final int WAITING_FIELD_NUMBER = 1;

    private boolean waiting_;

    /**
     * <code>bool waiting = 1;</code>
     */
    public boolean getWaiting() {
        return waiting_;
    }

    public static final int MAPPER_FIELD_NUMBER = 2;

    private int mapper_;

    /**
     * <code>uint32 mapper = 2;</code>
     */
    public int getMapper() {
        return mapper_;
    }

    public static final int REDUCER_FIELD_NUMBER = 3;

    private int reducer_;

    /**
     * <code>uint32 reducer = 3;</code>
     */
    public int getReducer() {
        return reducer_;
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
        if (waiting_ != false) {
            output.writeBool(1, waiting_);
        }
        if (mapper_ != 0) {
            output.writeUInt32(2, mapper_);
        }
        if (reducer_ != 0) {
            output.writeUInt32(3, reducer_);
        }
        unknownFields.writeTo(output);
    }

    @java.lang.Override
    public int getSerializedSize() {
        int size = memoizedSize;
        if (size != -1)
            return size;
        size = 0;
        if (waiting_ != false) {
            size += com.google.protobuf.CodedOutputStream.computeBoolSize(1, waiting_);
        }
        if (mapper_ != 0) {
            size += com.google.protobuf.CodedOutputStream.computeUInt32Size(2, mapper_);
        }
        if (reducer_ != 0) {
            size += com.google.protobuf.CodedOutputStream.computeUInt32Size(3, reducer_);
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
        if (!(obj instanceof com.qlangtech.tis.rpc.grpc.log.common.JobLog)) {
            return super.equals(obj);
        }
        com.qlangtech.tis.rpc.grpc.log.common.JobLog other = (com.qlangtech.tis.rpc.grpc.log.common.JobLog) obj;
        if (getWaiting() != other.getWaiting())
            return false;
        if (getMapper() != other.getMapper())
            return false;
        if (getReducer() != other.getReducer())
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
        hash = (37 * hash) + WAITING_FIELD_NUMBER;
        hash = (53 * hash) + com.google.protobuf.Internal.hashBoolean(getWaiting());
        hash = (37 * hash) + MAPPER_FIELD_NUMBER;
        hash = (53 * hash) + getMapper();
        hash = (37 * hash) + REDUCER_FIELD_NUMBER;
        hash = (53 * hash) + getReducer();
        hash = (29 * hash) + unknownFields.hashCode();
        memoizedHashCode = hash;
        return hash;
    }

    public static com.qlangtech.tis.rpc.grpc.log.common.JobLog parseFrom(java.nio.ByteBuffer data) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static com.qlangtech.tis.rpc.grpc.log.common.JobLog parseFrom(java.nio.ByteBuffer data, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static com.qlangtech.tis.rpc.grpc.log.common.JobLog parseFrom(com.google.protobuf.ByteString data) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static com.qlangtech.tis.rpc.grpc.log.common.JobLog parseFrom(com.google.protobuf.ByteString data, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static com.qlangtech.tis.rpc.grpc.log.common.JobLog parseFrom(byte[] data) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static com.qlangtech.tis.rpc.grpc.log.common.JobLog parseFrom(byte[] data, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static com.qlangtech.tis.rpc.grpc.log.common.JobLog parseFrom(java.io.InputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static com.qlangtech.tis.rpc.grpc.log.common.JobLog parseFrom(java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static com.qlangtech.tis.rpc.grpc.log.common.JobLog parseDelimitedFrom(java.io.InputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
    }

    public static com.qlangtech.tis.rpc.grpc.log.common.JobLog parseDelimitedFrom(java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input, extensionRegistry);
    }

    public static com.qlangtech.tis.rpc.grpc.log.common.JobLog parseFrom(com.google.protobuf.CodedInputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static com.qlangtech.tis.rpc.grpc.log.common.JobLog parseFrom(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    @java.lang.Override
    public Builder newBuilderForType() {
        return newBuilder();
    }

    public static Builder newBuilder() {
        return DEFAULT_INSTANCE.toBuilder();
    }

    public static Builder newBuilder(com.qlangtech.tis.rpc.grpc.log.common.JobLog prototype) {
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
     * Protobuf type {@code JobLog}
     */
    public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements // @@protoc_insertion_point(builder_implements:JobLog)
    com.qlangtech.tis.rpc.grpc.log.common.JobLogOrBuilder {

        public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
            return com.qlangtech.tis.rpc.grpc.log.common.LogCollectorProto.internal_static_JobLog_descriptor;
        }

        @java.lang.Override
        protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
            return com.qlangtech.tis.rpc.grpc.log.common.LogCollectorProto.internal_static_JobLog_fieldAccessorTable.ensureFieldAccessorsInitialized(com.qlangtech.tis.rpc.grpc.log.common.JobLog.class, com.qlangtech.tis.rpc.grpc.log.common.JobLog.Builder.class);
        }

        // Construct using com.qlangtech.tis.rpc.grpc.log.common.JobLog.newBuilder()
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
            waiting_ = false;
            mapper_ = 0;
            reducer_ = 0;
            return this;
        }

        @java.lang.Override
        public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
            return com.qlangtech.tis.rpc.grpc.log.common.LogCollectorProto.internal_static_JobLog_descriptor;
        }

        @java.lang.Override
        public com.qlangtech.tis.rpc.grpc.log.common.JobLog getDefaultInstanceForType() {
            return com.qlangtech.tis.rpc.grpc.log.common.JobLog.getDefaultInstance();
        }

        @java.lang.Override
        public com.qlangtech.tis.rpc.grpc.log.common.JobLog build() {
            com.qlangtech.tis.rpc.grpc.log.common.JobLog result = buildPartial();
            if (!result.isInitialized()) {
                throw newUninitializedMessageException(result);
            }
            return result;
        }

        @java.lang.Override
        public com.qlangtech.tis.rpc.grpc.log.common.JobLog buildPartial() {
            com.qlangtech.tis.rpc.grpc.log.common.JobLog result = new com.qlangtech.tis.rpc.grpc.log.common.JobLog(this);
            result.waiting_ = waiting_;
            result.mapper_ = mapper_;
            result.reducer_ = reducer_;
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
            if (other instanceof com.qlangtech.tis.rpc.grpc.log.common.JobLog) {
                return mergeFrom((com.qlangtech.tis.rpc.grpc.log.common.JobLog) other);
            } else {
                super.mergeFrom(other);
                return this;
            }
        }

        public Builder mergeFrom(com.qlangtech.tis.rpc.grpc.log.common.JobLog other) {
            if (other == com.qlangtech.tis.rpc.grpc.log.common.JobLog.getDefaultInstance())
                return this;
            if (other.getWaiting() != false) {
                setWaiting(other.getWaiting());
            }
            if (other.getMapper() != 0) {
                setMapper(other.getMapper());
            }
            if (other.getReducer() != 0) {
                setReducer(other.getReducer());
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
            com.qlangtech.tis.rpc.grpc.log.common.JobLog parsedMessage = null;
            try {
                parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
            } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                parsedMessage = (com.qlangtech.tis.rpc.grpc.log.common.JobLog) e.getUnfinishedMessage();
                throw e.unwrapIOException();
            } finally {
                if (parsedMessage != null) {
                    mergeFrom(parsedMessage);
                }
            }
            return this;
        }

        private boolean waiting_;

        /**
         * <code>bool waiting = 1;</code>
         */
        public boolean getWaiting() {
            return waiting_;
        }

        /**
         * <code>bool waiting = 1;</code>
         */
        public Builder setWaiting(boolean value) {
            waiting_ = value;
            onChanged();
            return this;
        }

        /**
         * <code>bool waiting = 1;</code>
         */
        public Builder clearWaiting() {
            waiting_ = false;
            onChanged();
            return this;
        }

        private int mapper_;

        /**
         * <code>uint32 mapper = 2;</code>
         */
        public int getMapper() {
            return mapper_;
        }

        /**
         * <code>uint32 mapper = 2;</code>
         */
        public Builder setMapper(int value) {
            mapper_ = value;
            onChanged();
            return this;
        }

        /**
         * <code>uint32 mapper = 2;</code>
         */
        public Builder clearMapper() {
            mapper_ = 0;
            onChanged();
            return this;
        }

        private int reducer_;

        /**
         * <code>uint32 reducer = 3;</code>
         */
        public int getReducer() {
            return reducer_;
        }

        /**
         * <code>uint32 reducer = 3;</code>
         */
        public Builder setReducer(int value) {
            reducer_ = value;
            onChanged();
            return this;
        }

        /**
         * <code>uint32 reducer = 3;</code>
         */
        public Builder clearReducer() {
            reducer_ = 0;
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
        // @@protoc_insertion_point(builder_scope:JobLog)
    }

    // @@protoc_insertion_point(class_scope:JobLog)
    private static final com.qlangtech.tis.rpc.grpc.log.common.JobLog DEFAULT_INSTANCE;

    static {
        DEFAULT_INSTANCE = new com.qlangtech.tis.rpc.grpc.log.common.JobLog();
    }

    public static com.qlangtech.tis.rpc.grpc.log.common.JobLog getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    private static final com.google.protobuf.Parser<JobLog> PARSER = new com.google.protobuf.AbstractParser<JobLog>() {

        @java.lang.Override
        public JobLog parsePartialFrom(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
            return new JobLog(input, extensionRegistry);
        }
    };

    public static com.google.protobuf.Parser<JobLog> parser() {
        return PARSER;
    }

    @java.lang.Override
    public com.google.protobuf.Parser<JobLog> getParserForType() {
        return PARSER;
    }

    @java.lang.Override
    public com.qlangtech.tis.rpc.grpc.log.common.JobLog getDefaultInstanceForType() {
        return DEFAULT_INSTANCE;
    }
}
