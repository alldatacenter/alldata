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
package com.qlangtech.tis.grpc;

/**
 * Protobuf type {@code rpc.MasterJob}
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/09/25
 */
public final class MasterJob extends com.google.protobuf.GeneratedMessageV3 implements // @@protoc_insertion_point(message_implements:rpc.MasterJob)
MasterJobOrBuilder {

    private static final long serialVersionUID = 0L;

    // Use MasterJob.newBuilder() to construct.
    private MasterJob(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
        super(builder);
    }

    private MasterJob() {
        jobType_ = 0;
        indexName_ = "";
        uuid_ = "";
    }

    @java.lang.Override
    public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
        return this.unknownFields;
    }

    private MasterJob(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
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
                            jobType_ = rawValue;
                            break;
                        }
                    case 16:
                        {
                            stop_ = input.readBool();
                            break;
                        }
                    case 26:
                        {
                            java.lang.String s = input.readStringRequireUtf8();
                            indexName_ = s;
                            break;
                        }
                    case 34:
                        {
                            java.lang.String s = input.readStringRequireUtf8();
                            uuid_ = s;
                            break;
                        }
                    case 40:
                        {
                            createTime_ = input.readUInt64();
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
        return com.qlangtech.tis.grpc.IncrStatusProto.internal_static_rpc_MasterJob_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
        return com.qlangtech.tis.grpc.IncrStatusProto.internal_static_rpc_MasterJob_fieldAccessorTable.ensureFieldAccessorsInitialized(com.qlangtech.tis.grpc.MasterJob.class, com.qlangtech.tis.grpc.MasterJob.Builder.class);
    }

    /**
     * Protobuf enum {@code rpc.MasterJob.JobType}
     */
    public enum JobType implements com.google.protobuf.ProtocolMessageEnum {

        /**
         * <pre>
         * 说明是空对象
         * </pre>
         *
         * <code>None = 0;</code>
         */
        None(0),
        /**
         * <code>IndexJobRunning = 1;</code>
         */
        IndexJobRunning(1),
        UNRECOGNIZED(-1);

        /**
         * <pre>
         * 说明是空对象
         * </pre>
         *
         * <code>None = 0;</code>
         */
        public static final int None_VALUE = 0;

        /**
         * <code>IndexJobRunning = 1;</code>
         */
        public static final int IndexJobRunning_VALUE = 1;

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
        public static JobType valueOf(int value) {
            return forNumber(value);
        }

        public static JobType forNumber(int value) {
            switch(value) {
                case 0:
                    return None;
                case 1:
                    return IndexJobRunning;
                default:
                    return null;
            }
        }

        public static com.google.protobuf.Internal.EnumLiteMap<JobType> internalGetValueMap() {
            return internalValueMap;
        }

        private static final com.google.protobuf.Internal.EnumLiteMap<JobType> internalValueMap = new com.google.protobuf.Internal.EnumLiteMap<JobType>() {

            public JobType findValueByNumber(int number) {
                return JobType.forNumber(number);
            }
        };

        public final com.google.protobuf.Descriptors.EnumValueDescriptor getValueDescriptor() {
            return getDescriptor().getValues().get(ordinal());
        }

        public final com.google.protobuf.Descriptors.EnumDescriptor getDescriptorForType() {
            return getDescriptor();
        }

        public static final com.google.protobuf.Descriptors.EnumDescriptor getDescriptor() {
            return com.qlangtech.tis.grpc.MasterJob.getDescriptor().getEnumTypes().get(0);
        }

        private static final JobType[] VALUES = values();

        public static JobType valueOf(com.google.protobuf.Descriptors.EnumValueDescriptor desc) {
            if (desc.getType() != getDescriptor()) {
                throw new java.lang.IllegalArgumentException("EnumValueDescriptor is not for this type.");
            }
            if (desc.getIndex() == -1) {
                return UNRECOGNIZED;
            }
            return VALUES[desc.getIndex()];
        }

        private final int value;

        private JobType(int value) {
            this.value = value;
        }
    }

    public static final int JOBTYPE_FIELD_NUMBER = 1;

    private int jobType_;

    /**
     * <code>.rpc.MasterJob.JobType jobType = 1;</code>
     */
    public int getJobTypeValue() {
        return jobType_;
    }

    /**
     * <code>.rpc.MasterJob.JobType jobType = 1;</code>
     */
    public com.qlangtech.tis.grpc.MasterJob.JobType getJobType() {
        @SuppressWarnings("deprecation")
        com.qlangtech.tis.grpc.MasterJob.JobType result = com.qlangtech.tis.grpc.MasterJob.JobType.valueOf(jobType_);
        return result == null ? com.qlangtech.tis.grpc.MasterJob.JobType.UNRECOGNIZED : result;
    }

    public static final int STOP_FIELD_NUMBER = 2;

    private boolean stop_;

    /**
     * <code>bool stop = 2;</code>
     */
    public boolean getStop() {
        return stop_;
    }

    public static final int INDEXNAME_FIELD_NUMBER = 3;

    private volatile java.lang.Object indexName_;

    /**
     * <code>string indexName = 3;</code>
     */
    public java.lang.String getIndexName() {
        java.lang.Object ref = indexName_;
        if (ref instanceof java.lang.String) {
            return (java.lang.String) ref;
        } else {
            com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
            java.lang.String s = bs.toStringUtf8();
            indexName_ = s;
            return s;
        }
    }

    /**
     * <code>string indexName = 3;</code>
     */
    public com.google.protobuf.ByteString getIndexNameBytes() {
        java.lang.Object ref = indexName_;
        if (ref instanceof java.lang.String) {
            com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((java.lang.String) ref);
            indexName_ = b;
            return b;
        } else {
            return (com.google.protobuf.ByteString) ref;
        }
    }

    public static final int UUID_FIELD_NUMBER = 4;

    private volatile java.lang.Object uuid_;

    /**
     * <code>string uuid = 4;</code>
     */
    public java.lang.String getUuid() {
        java.lang.Object ref = uuid_;
        if (ref instanceof java.lang.String) {
            return (java.lang.String) ref;
        } else {
            com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
            java.lang.String s = bs.toStringUtf8();
            uuid_ = s;
            return s;
        }
    }

    /**
     * <code>string uuid = 4;</code>
     */
    public com.google.protobuf.ByteString getUuidBytes() {
        java.lang.Object ref = uuid_;
        if (ref instanceof java.lang.String) {
            com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((java.lang.String) ref);
            uuid_ = b;
            return b;
        } else {
            return (com.google.protobuf.ByteString) ref;
        }
    }

    public static final int CREATETIME_FIELD_NUMBER = 5;

    private long createTime_;

    /**
     * <code>uint64 createTime = 5;</code>
     */
    public long getCreateTime() {
        return createTime_;
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
        if (jobType_ != com.qlangtech.tis.grpc.MasterJob.JobType.None.getNumber()) {
            output.writeEnum(1, jobType_);
        }
        if (stop_ != false) {
            output.writeBool(2, stop_);
        }
        if (!getIndexNameBytes().isEmpty()) {
            com.google.protobuf.GeneratedMessageV3.writeString(output, 3, indexName_);
        }
        if (!getUuidBytes().isEmpty()) {
            com.google.protobuf.GeneratedMessageV3.writeString(output, 4, uuid_);
        }
        if (createTime_ != 0L) {
            output.writeUInt64(5, createTime_);
        }
        unknownFields.writeTo(output);
    }

    @java.lang.Override
    public int getSerializedSize() {
        int size = memoizedSize;
        if (size != -1)
            return size;
        size = 0;
        if (jobType_ != com.qlangtech.tis.grpc.MasterJob.JobType.None.getNumber()) {
            size += com.google.protobuf.CodedOutputStream.computeEnumSize(1, jobType_);
        }
        if (stop_ != false) {
            size += com.google.protobuf.CodedOutputStream.computeBoolSize(2, stop_);
        }
        if (!getIndexNameBytes().isEmpty()) {
            size += com.google.protobuf.GeneratedMessageV3.computeStringSize(3, indexName_);
        }
        if (!getUuidBytes().isEmpty()) {
            size += com.google.protobuf.GeneratedMessageV3.computeStringSize(4, uuid_);
        }
        if (createTime_ != 0L) {
            size += com.google.protobuf.CodedOutputStream.computeUInt64Size(5, createTime_);
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
        if (!(obj instanceof com.qlangtech.tis.grpc.MasterJob)) {
            return super.equals(obj);
        }
        com.qlangtech.tis.grpc.MasterJob other = (com.qlangtech.tis.grpc.MasterJob) obj;
        if (jobType_ != other.jobType_)
            return false;
        if (getStop() != other.getStop())
            return false;
        if (!getIndexName().equals(other.getIndexName()))
            return false;
        if (!getUuid().equals(other.getUuid()))
            return false;
        if (getCreateTime() != other.getCreateTime())
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
        hash = (37 * hash) + JOBTYPE_FIELD_NUMBER;
        hash = (53 * hash) + jobType_;
        hash = (37 * hash) + STOP_FIELD_NUMBER;
        hash = (53 * hash) + com.google.protobuf.Internal.hashBoolean(getStop());
        hash = (37 * hash) + INDEXNAME_FIELD_NUMBER;
        hash = (53 * hash) + getIndexName().hashCode();
        hash = (37 * hash) + UUID_FIELD_NUMBER;
        hash = (53 * hash) + getUuid().hashCode();
        hash = (37 * hash) + CREATETIME_FIELD_NUMBER;
        hash = (53 * hash) + com.google.protobuf.Internal.hashLong(getCreateTime());
        hash = (29 * hash) + unknownFields.hashCode();
        memoizedHashCode = hash;
        return hash;
    }

    public static com.qlangtech.tis.grpc.MasterJob parseFrom(java.nio.ByteBuffer data) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static com.qlangtech.tis.grpc.MasterJob parseFrom(java.nio.ByteBuffer data, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static com.qlangtech.tis.grpc.MasterJob parseFrom(com.google.protobuf.ByteString data) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static com.qlangtech.tis.grpc.MasterJob parseFrom(com.google.protobuf.ByteString data, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static com.qlangtech.tis.grpc.MasterJob parseFrom(byte[] data) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static com.qlangtech.tis.grpc.MasterJob parseFrom(byte[] data, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static com.qlangtech.tis.grpc.MasterJob parseFrom(java.io.InputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static com.qlangtech.tis.grpc.MasterJob parseFrom(java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static com.qlangtech.tis.grpc.MasterJob parseDelimitedFrom(java.io.InputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
    }

    public static com.qlangtech.tis.grpc.MasterJob parseDelimitedFrom(java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input, extensionRegistry);
    }

    public static com.qlangtech.tis.grpc.MasterJob parseFrom(com.google.protobuf.CodedInputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static com.qlangtech.tis.grpc.MasterJob parseFrom(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    @java.lang.Override
    public Builder newBuilderForType() {
        return newBuilder();
    }

    public static Builder newBuilder() {
        return DEFAULT_INSTANCE.toBuilder();
    }

    public static Builder newBuilder(com.qlangtech.tis.grpc.MasterJob prototype) {
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
     * Protobuf type {@code rpc.MasterJob}
     */
    public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements // @@protoc_insertion_point(builder_implements:rpc.MasterJob)
    com.qlangtech.tis.grpc.MasterJobOrBuilder {

        public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
            return com.qlangtech.tis.grpc.IncrStatusProto.internal_static_rpc_MasterJob_descriptor;
        }

        @java.lang.Override
        protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
            return com.qlangtech.tis.grpc.IncrStatusProto.internal_static_rpc_MasterJob_fieldAccessorTable.ensureFieldAccessorsInitialized(com.qlangtech.tis.grpc.MasterJob.class, com.qlangtech.tis.grpc.MasterJob.Builder.class);
        }

        // Construct using com.qlangtech.tis.grpc.MasterJob.newBuilder()
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
            jobType_ = 0;
            stop_ = false;
            indexName_ = "";
            uuid_ = "";
            createTime_ = 0L;
            return this;
        }

        @java.lang.Override
        public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
            return com.qlangtech.tis.grpc.IncrStatusProto.internal_static_rpc_MasterJob_descriptor;
        }

        @java.lang.Override
        public com.qlangtech.tis.grpc.MasterJob getDefaultInstanceForType() {
            return com.qlangtech.tis.grpc.MasterJob.getDefaultInstance();
        }

        @java.lang.Override
        public com.qlangtech.tis.grpc.MasterJob build() {
            com.qlangtech.tis.grpc.MasterJob result = buildPartial();
            if (!result.isInitialized()) {
                throw newUninitializedMessageException(result);
            }
            return result;
        }

        @java.lang.Override
        public com.qlangtech.tis.grpc.MasterJob buildPartial() {
            com.qlangtech.tis.grpc.MasterJob result = new com.qlangtech.tis.grpc.MasterJob(this);
            result.jobType_ = jobType_;
            result.stop_ = stop_;
            result.indexName_ = indexName_;
            result.uuid_ = uuid_;
            result.createTime_ = createTime_;
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
            if (other instanceof com.qlangtech.tis.grpc.MasterJob) {
                return mergeFrom((com.qlangtech.tis.grpc.MasterJob) other);
            } else {
                super.mergeFrom(other);
                return this;
            }
        }

        public Builder mergeFrom(com.qlangtech.tis.grpc.MasterJob other) {
            if (other == com.qlangtech.tis.grpc.MasterJob.getDefaultInstance())
                return this;
            if (other.jobType_ != 0) {
                setJobTypeValue(other.getJobTypeValue());
            }
            if (other.getStop() != false) {
                setStop(other.getStop());
            }
            if (!other.getIndexName().isEmpty()) {
                indexName_ = other.indexName_;
                onChanged();
            }
            if (!other.getUuid().isEmpty()) {
                uuid_ = other.uuid_;
                onChanged();
            }
            if (other.getCreateTime() != 0L) {
                setCreateTime(other.getCreateTime());
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
            com.qlangtech.tis.grpc.MasterJob parsedMessage = null;
            try {
                parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
            } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                parsedMessage = (com.qlangtech.tis.grpc.MasterJob) e.getUnfinishedMessage();
                throw e.unwrapIOException();
            } finally {
                if (parsedMessage != null) {
                    mergeFrom(parsedMessage);
                }
            }
            return this;
        }

        private int jobType_ = 0;

        /**
         * <code>.rpc.MasterJob.JobType jobType = 1;</code>
         */
        public int getJobTypeValue() {
            return jobType_;
        }

        /**
         * <code>.rpc.MasterJob.JobType jobType = 1;</code>
         */
        public Builder setJobTypeValue(int value) {
            jobType_ = value;
            onChanged();
            return this;
        }

        /**
         * <code>.rpc.MasterJob.JobType jobType = 1;</code>
         */
        public com.qlangtech.tis.grpc.MasterJob.JobType getJobType() {
            @SuppressWarnings("deprecation")
            com.qlangtech.tis.grpc.MasterJob.JobType result = com.qlangtech.tis.grpc.MasterJob.JobType.valueOf(jobType_);
            return result == null ? com.qlangtech.tis.grpc.MasterJob.JobType.UNRECOGNIZED : result;
        }

        /**
         * <code>.rpc.MasterJob.JobType jobType = 1;</code>
         */
        public Builder setJobType(com.qlangtech.tis.grpc.MasterJob.JobType value) {
            if (value == null) {
                throw new NullPointerException();
            }
            jobType_ = value.getNumber();
            onChanged();
            return this;
        }

        /**
         * <code>.rpc.MasterJob.JobType jobType = 1;</code>
         */
        public Builder clearJobType() {
            jobType_ = 0;
            onChanged();
            return this;
        }

        private boolean stop_;

        /**
         * <code>bool stop = 2;</code>
         */
        public boolean getStop() {
            return stop_;
        }

        /**
         * <code>bool stop = 2;</code>
         */
        public Builder setStop(boolean value) {
            stop_ = value;
            onChanged();
            return this;
        }

        /**
         * <code>bool stop = 2;</code>
         */
        public Builder clearStop() {
            stop_ = false;
            onChanged();
            return this;
        }

        private java.lang.Object indexName_ = "";

        /**
         * <code>string indexName = 3;</code>
         */
        public java.lang.String getIndexName() {
            java.lang.Object ref = indexName_;
            if (!(ref instanceof java.lang.String)) {
                com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
                java.lang.String s = bs.toStringUtf8();
                indexName_ = s;
                return s;
            } else {
                return (java.lang.String) ref;
            }
        }

        /**
         * <code>string indexName = 3;</code>
         */
        public com.google.protobuf.ByteString getIndexNameBytes() {
            java.lang.Object ref = indexName_;
            if (ref instanceof String) {
                com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((java.lang.String) ref);
                indexName_ = b;
                return b;
            } else {
                return (com.google.protobuf.ByteString) ref;
            }
        }

        /**
         * <code>string indexName = 3;</code>
         */
        public Builder setIndexName(java.lang.String value) {
            if (value == null) {
                throw new NullPointerException();
            }
            indexName_ = value;
            onChanged();
            return this;
        }

        /**
         * <code>string indexName = 3;</code>
         */
        public Builder clearIndexName() {
            indexName_ = getDefaultInstance().getIndexName();
            onChanged();
            return this;
        }

        /**
         * <code>string indexName = 3;</code>
         */
        public Builder setIndexNameBytes(com.google.protobuf.ByteString value) {
            if (value == null) {
                throw new NullPointerException();
            }
            checkByteStringIsUtf8(value);
            indexName_ = value;
            onChanged();
            return this;
        }

        private java.lang.Object uuid_ = "";

        /**
         * <code>string uuid = 4;</code>
         */
        public java.lang.String getUuid() {
            java.lang.Object ref = uuid_;
            if (!(ref instanceof java.lang.String)) {
                com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
                java.lang.String s = bs.toStringUtf8();
                uuid_ = s;
                return s;
            } else {
                return (java.lang.String) ref;
            }
        }

        /**
         * <code>string uuid = 4;</code>
         */
        public com.google.protobuf.ByteString getUuidBytes() {
            java.lang.Object ref = uuid_;
            if (ref instanceof String) {
                com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((java.lang.String) ref);
                uuid_ = b;
                return b;
            } else {
                return (com.google.protobuf.ByteString) ref;
            }
        }

        /**
         * <code>string uuid = 4;</code>
         */
        public Builder setUuid(java.lang.String value) {
            if (value == null) {
                throw new NullPointerException();
            }
            uuid_ = value;
            onChanged();
            return this;
        }

        /**
         * <code>string uuid = 4;</code>
         */
        public Builder clearUuid() {
            uuid_ = getDefaultInstance().getUuid();
            onChanged();
            return this;
        }

        /**
         * <code>string uuid = 4;</code>
         */
        public Builder setUuidBytes(com.google.protobuf.ByteString value) {
            if (value == null) {
                throw new NullPointerException();
            }
            checkByteStringIsUtf8(value);
            uuid_ = value;
            onChanged();
            return this;
        }

        private long createTime_;

        /**
         * <code>uint64 createTime = 5;</code>
         */
        public long getCreateTime() {
            return createTime_;
        }

        /**
         * <code>uint64 createTime = 5;</code>
         */
        public Builder setCreateTime(long value) {
            createTime_ = value;
            onChanged();
            return this;
        }

        /**
         * <code>uint64 createTime = 5;</code>
         */
        public Builder clearCreateTime() {
            createTime_ = 0L;
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
        // @@protoc_insertion_point(builder_scope:rpc.MasterJob)
    }

    // @@protoc_insertion_point(class_scope:rpc.MasterJob)
    private static final com.qlangtech.tis.grpc.MasterJob DEFAULT_INSTANCE;

    static {
        DEFAULT_INSTANCE = new com.qlangtech.tis.grpc.MasterJob();
    }

    public static com.qlangtech.tis.grpc.MasterJob getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    private static final com.google.protobuf.Parser<MasterJob> PARSER = new com.google.protobuf.AbstractParser<MasterJob>() {

        @java.lang.Override
        public MasterJob parsePartialFrom(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
            return new MasterJob(input, extensionRegistry);
        }
    };

    public static com.google.protobuf.Parser<MasterJob> parser() {
        return PARSER;
    }

    @java.lang.Override
    public com.google.protobuf.Parser<MasterJob> getParserForType() {
        return PARSER;
    }

    @java.lang.Override
    public com.qlangtech.tis.grpc.MasterJob getDefaultInstanceForType() {
        return DEFAULT_INSTANCE;
    }
}
