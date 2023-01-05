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
 * Protobuf type {@code rpc.TableSingleDataIndexStatus}
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/09/25
 */
public final class TableSingleDataIndexStatus extends com.google.protobuf.GeneratedMessageV3 implements // @@protoc_insertion_point(message_implements:rpc.TableSingleDataIndexStatus)
TableSingleDataIndexStatusOrBuilder {

    private static final long serialVersionUID = 0L;

    // Use TableSingleDataIndexStatus.newBuilder() to construct.
    private TableSingleDataIndexStatus(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
        super(builder);
    }

    private TableSingleDataIndexStatus() {
        uuid_ = "";
    }

    @java.lang.Override
    public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
        return this.unknownFields;
    }

    private TableSingleDataIndexStatus(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
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
                            if (!((mutable_bitField0_ & 0x00000001) != 0)) {
                                tableConsumeData_ = com.google.protobuf.MapField.newMapField(TableConsumeDataDefaultEntryHolder.defaultEntry);
                                mutable_bitField0_ |= 0x00000001;
                            }
                            com.google.protobuf.MapEntry<java.lang.String, java.lang.Long> tableConsumeData__ = input.readMessage(TableConsumeDataDefaultEntryHolder.defaultEntry.getParserForType(), extensionRegistry);
                            tableConsumeData_.getMutableMap().put(tableConsumeData__.getKey(), tableConsumeData__.getValue());
                            break;
                        }
                    case 16:
                        {
                            bufferQueueRemainingCapacity_ = input.readUInt32();
                            break;
                        }
                    case 24:
                        {
                            bufferQueueUsedSize_ = input.readUInt32();
                            break;
                        }
                    case 32:
                        {
                            consumeErrorCount_ = input.readUInt32();
                            break;
                        }
                    case 40:
                        {
                            ignoreRowsCount_ = input.readUInt32();
                            break;
                        }
                    case 50:
                        {
                            java.lang.String s = input.readStringRequireUtf8();
                            uuid_ = s;
                            break;
                        }
                    case 56:
                        {
                            tis30SAvgRT_ = input.readUInt64();
                            break;
                        }
                    case 64:
                        {
                            incrProcessPaused_ = input.readBool();
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
        return com.qlangtech.tis.grpc.IncrStatusProto.internal_static_rpc_TableSingleDataIndexStatus_descriptor;
    }

    @SuppressWarnings({ "rawtypes" })
    @java.lang.Override
    protected com.google.protobuf.MapField internalGetMapField(int number) {
        switch(number) {
            case 1:
                return internalGetTableConsumeData();
            default:
                throw new RuntimeException("Invalid map field number: " + number);
        }
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
        return com.qlangtech.tis.grpc.IncrStatusProto.internal_static_rpc_TableSingleDataIndexStatus_fieldAccessorTable.ensureFieldAccessorsInitialized(com.qlangtech.tis.grpc.TableSingleDataIndexStatus.class, com.qlangtech.tis.grpc.TableSingleDataIndexStatus.Builder.class);
    }

    private int bitField0_;

    public static final int TABLECONSUMEDATA_FIELD_NUMBER = 1;

    private static final class TableConsumeDataDefaultEntryHolder {

        static final com.google.protobuf.MapEntry<java.lang.String, java.lang.Long> defaultEntry = com.google.protobuf.MapEntry.<java.lang.String, java.lang.Long>newDefaultInstance(com.qlangtech.tis.grpc.IncrStatusProto.internal_static_rpc_TableSingleDataIndexStatus_TableConsumeDataEntry_descriptor, com.google.protobuf.WireFormat.FieldType.STRING, "", com.google.protobuf.WireFormat.FieldType.UINT64, 0L);
    }

    private com.google.protobuf.MapField<java.lang.String, java.lang.Long> tableConsumeData_;

    private com.google.protobuf.MapField<java.lang.String, java.lang.Long> internalGetTableConsumeData() {
        if (tableConsumeData_ == null) {
            return com.google.protobuf.MapField.emptyMapField(TableConsumeDataDefaultEntryHolder.defaultEntry);
        }
        return tableConsumeData_;
    }

    public int getTableConsumeDataCount() {
        return internalGetTableConsumeData().getMap().size();
    }

    /**
     * <code>map&lt;string, uint64&gt; tableConsumeData = 1;</code>
     */
    public boolean containsTableConsumeData(java.lang.String key) {
        if (key == null) {
            throw new java.lang.NullPointerException();
        }
        return internalGetTableConsumeData().getMap().containsKey(key);
    }

    /**
     * Use {@link #getTableConsumeDataMap()} instead.
     */
    @java.lang.Deprecated
    public java.util.Map<java.lang.String, java.lang.Long> getTableConsumeData() {
        return getTableConsumeDataMap();
    }

    /**
     * <code>map&lt;string, uint64&gt; tableConsumeData = 1;</code>
     */
    public java.util.Map<java.lang.String, java.lang.Long> getTableConsumeDataMap() {
        return internalGetTableConsumeData().getMap();
    }

    /**
     * <code>map&lt;string, uint64&gt; tableConsumeData = 1;</code>
     */
    public long getTableConsumeDataOrDefault(java.lang.String key, long defaultValue) {
        if (key == null) {
            throw new java.lang.NullPointerException();
        }
        java.util.Map<java.lang.String, java.lang.Long> map = internalGetTableConsumeData().getMap();
        return map.containsKey(key) ? map.get(key) : defaultValue;
    }

    /**
     * <code>map&lt;string, uint64&gt; tableConsumeData = 1;</code>
     */
    public long getTableConsumeDataOrThrow(java.lang.String key) {
        if (key == null) {
            throw new java.lang.NullPointerException();
        }
        java.util.Map<java.lang.String, java.lang.Long> map = internalGetTableConsumeData().getMap();
        if (!map.containsKey(key)) {
            throw new java.lang.IllegalArgumentException();
        }
        return map.get(key);
    }

    public static final int BUFFERQUEUEREMAININGCAPACITY_FIELD_NUMBER = 2;

    private int bufferQueueRemainingCapacity_;

    /**
     * <code>uint32 bufferQueueRemainingCapacity = 2;</code>
     */
    public int getBufferQueueRemainingCapacity() {
        return bufferQueueRemainingCapacity_;
    }

    public static final int BUFFERQUEUEUSEDSIZE_FIELD_NUMBER = 3;

    private int bufferQueueUsedSize_;

    /**
     * <code>uint32 bufferQueueUsedSize = 3;</code>
     */
    public int getBufferQueueUsedSize() {
        return bufferQueueUsedSize_;
    }

    public static final int CONSUMEERRORCOUNT_FIELD_NUMBER = 4;

    private int consumeErrorCount_;

    /**
     * <code>uint32 consumeErrorCount = 4;</code>
     */
    public int getConsumeErrorCount() {
        return consumeErrorCount_;
    }

    public static final int IGNOREROWSCOUNT_FIELD_NUMBER = 5;

    private int ignoreRowsCount_;

    /**
     * <code>uint32 ignoreRowsCount = 5;</code>
     */
    public int getIgnoreRowsCount() {
        return ignoreRowsCount_;
    }

    public static final int UUID_FIELD_NUMBER = 6;

    private volatile java.lang.Object uuid_;

    /**
     * <code>string uuid = 6;</code>
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
     * <code>string uuid = 6;</code>
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

    public static final int TIS30SAVGRT_FIELD_NUMBER = 7;

    private long tis30SAvgRT_;

    /**
     * <code>uint64 tis30sAvgRT = 7;</code>
     */
    public long getTis30SAvgRT() {
        return tis30SAvgRT_;
    }

    public static final int INCRPROCESSPAUSED_FIELD_NUMBER = 8;

    private boolean incrProcessPaused_;

    /**
     * <pre>
     * 增量任务执行是否暂停
     * </pre>
     *
     * <code>bool incrProcessPaused = 8;</code>
     */
    public boolean getIncrProcessPaused() {
        return incrProcessPaused_;
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
        com.google.protobuf.GeneratedMessageV3.serializeStringMapTo(output, internalGetTableConsumeData(), TableConsumeDataDefaultEntryHolder.defaultEntry, 1);
        if (bufferQueueRemainingCapacity_ != 0) {
            output.writeUInt32(2, bufferQueueRemainingCapacity_);
        }
        if (bufferQueueUsedSize_ != 0) {
            output.writeUInt32(3, bufferQueueUsedSize_);
        }
        if (consumeErrorCount_ != 0) {
            output.writeUInt32(4, consumeErrorCount_);
        }
        if (ignoreRowsCount_ != 0) {
            output.writeUInt32(5, ignoreRowsCount_);
        }
        if (!getUuidBytes().isEmpty()) {
            com.google.protobuf.GeneratedMessageV3.writeString(output, 6, uuid_);
        }
        if (tis30SAvgRT_ != 0L) {
            output.writeUInt64(7, tis30SAvgRT_);
        }
        if (incrProcessPaused_ != false) {
            output.writeBool(8, incrProcessPaused_);
        }
        unknownFields.writeTo(output);
    }

    @java.lang.Override
    public int getSerializedSize() {
        int size = memoizedSize;
        if (size != -1)
            return size;
        size = 0;
        for (java.util.Map.Entry<java.lang.String, java.lang.Long> entry : internalGetTableConsumeData().getMap().entrySet()) {
            com.google.protobuf.MapEntry<java.lang.String, java.lang.Long> tableConsumeData__ = TableConsumeDataDefaultEntryHolder.defaultEntry.newBuilderForType().setKey(entry.getKey()).setValue(entry.getValue()).build();
            size += com.google.protobuf.CodedOutputStream.computeMessageSize(1, tableConsumeData__);
        }
        if (bufferQueueRemainingCapacity_ != 0) {
            size += com.google.protobuf.CodedOutputStream.computeUInt32Size(2, bufferQueueRemainingCapacity_);
        }
        if (bufferQueueUsedSize_ != 0) {
            size += com.google.protobuf.CodedOutputStream.computeUInt32Size(3, bufferQueueUsedSize_);
        }
        if (consumeErrorCount_ != 0) {
            size += com.google.protobuf.CodedOutputStream.computeUInt32Size(4, consumeErrorCount_);
        }
        if (ignoreRowsCount_ != 0) {
            size += com.google.protobuf.CodedOutputStream.computeUInt32Size(5, ignoreRowsCount_);
        }
        if (!getUuidBytes().isEmpty()) {
            size += com.google.protobuf.GeneratedMessageV3.computeStringSize(6, uuid_);
        }
        if (tis30SAvgRT_ != 0L) {
            size += com.google.protobuf.CodedOutputStream.computeUInt64Size(7, tis30SAvgRT_);
        }
        if (incrProcessPaused_ != false) {
            size += com.google.protobuf.CodedOutputStream.computeBoolSize(8, incrProcessPaused_);
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
        if (!(obj instanceof com.qlangtech.tis.grpc.TableSingleDataIndexStatus)) {
            return super.equals(obj);
        }
        com.qlangtech.tis.grpc.TableSingleDataIndexStatus other = (com.qlangtech.tis.grpc.TableSingleDataIndexStatus) obj;
        if (!internalGetTableConsumeData().equals(other.internalGetTableConsumeData()))
            return false;
        if (getBufferQueueRemainingCapacity() != other.getBufferQueueRemainingCapacity())
            return false;
        if (getBufferQueueUsedSize() != other.getBufferQueueUsedSize())
            return false;
        if (getConsumeErrorCount() != other.getConsumeErrorCount())
            return false;
        if (getIgnoreRowsCount() != other.getIgnoreRowsCount())
            return false;
        if (!getUuid().equals(other.getUuid()))
            return false;
        if (getTis30SAvgRT() != other.getTis30SAvgRT())
            return false;
        if (getIncrProcessPaused() != other.getIncrProcessPaused())
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
        if (!internalGetTableConsumeData().getMap().isEmpty()) {
            hash = (37 * hash) + TABLECONSUMEDATA_FIELD_NUMBER;
            hash = (53 * hash) + internalGetTableConsumeData().hashCode();
        }
        hash = (37 * hash) + BUFFERQUEUEREMAININGCAPACITY_FIELD_NUMBER;
        hash = (53 * hash) + getBufferQueueRemainingCapacity();
        hash = (37 * hash) + BUFFERQUEUEUSEDSIZE_FIELD_NUMBER;
        hash = (53 * hash) + getBufferQueueUsedSize();
        hash = (37 * hash) + CONSUMEERRORCOUNT_FIELD_NUMBER;
        hash = (53 * hash) + getConsumeErrorCount();
        hash = (37 * hash) + IGNOREROWSCOUNT_FIELD_NUMBER;
        hash = (53 * hash) + getIgnoreRowsCount();
        hash = (37 * hash) + UUID_FIELD_NUMBER;
        hash = (53 * hash) + getUuid().hashCode();
        hash = (37 * hash) + TIS30SAVGRT_FIELD_NUMBER;
        hash = (53 * hash) + com.google.protobuf.Internal.hashLong(getTis30SAvgRT());
        hash = (37 * hash) + INCRPROCESSPAUSED_FIELD_NUMBER;
        hash = (53 * hash) + com.google.protobuf.Internal.hashBoolean(getIncrProcessPaused());
        hash = (29 * hash) + unknownFields.hashCode();
        memoizedHashCode = hash;
        return hash;
    }

    public static com.qlangtech.tis.grpc.TableSingleDataIndexStatus parseFrom(java.nio.ByteBuffer data) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static com.qlangtech.tis.grpc.TableSingleDataIndexStatus parseFrom(java.nio.ByteBuffer data, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static com.qlangtech.tis.grpc.TableSingleDataIndexStatus parseFrom(com.google.protobuf.ByteString data) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static com.qlangtech.tis.grpc.TableSingleDataIndexStatus parseFrom(com.google.protobuf.ByteString data, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static com.qlangtech.tis.grpc.TableSingleDataIndexStatus parseFrom(byte[] data) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static com.qlangtech.tis.grpc.TableSingleDataIndexStatus parseFrom(byte[] data, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static com.qlangtech.tis.grpc.TableSingleDataIndexStatus parseFrom(java.io.InputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static com.qlangtech.tis.grpc.TableSingleDataIndexStatus parseFrom(java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static com.qlangtech.tis.grpc.TableSingleDataIndexStatus parseDelimitedFrom(java.io.InputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
    }

    public static com.qlangtech.tis.grpc.TableSingleDataIndexStatus parseDelimitedFrom(java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input, extensionRegistry);
    }

    public static com.qlangtech.tis.grpc.TableSingleDataIndexStatus parseFrom(com.google.protobuf.CodedInputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static com.qlangtech.tis.grpc.TableSingleDataIndexStatus parseFrom(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    @java.lang.Override
    public Builder newBuilderForType() {
        return newBuilder();
    }

    public static Builder newBuilder() {
        return DEFAULT_INSTANCE.toBuilder();
    }

    public static Builder newBuilder(com.qlangtech.tis.grpc.TableSingleDataIndexStatus prototype) {
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
     * Protobuf type {@code rpc.TableSingleDataIndexStatus}
     */
    public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements // @@protoc_insertion_point(builder_implements:rpc.TableSingleDataIndexStatus)
    com.qlangtech.tis.grpc.TableSingleDataIndexStatusOrBuilder {

        public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
            return com.qlangtech.tis.grpc.IncrStatusProto.internal_static_rpc_TableSingleDataIndexStatus_descriptor;
        }

        @SuppressWarnings({ "rawtypes" })
        protected com.google.protobuf.MapField internalGetMapField(int number) {
            switch(number) {
                case 1:
                    return internalGetTableConsumeData();
                default:
                    throw new RuntimeException("Invalid map field number: " + number);
            }
        }

        @SuppressWarnings({ "rawtypes" })
        protected com.google.protobuf.MapField internalGetMutableMapField(int number) {
            switch(number) {
                case 1:
                    return internalGetMutableTableConsumeData();
                default:
                    throw new RuntimeException("Invalid map field number: " + number);
            }
        }

        @java.lang.Override
        protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
            return com.qlangtech.tis.grpc.IncrStatusProto.internal_static_rpc_TableSingleDataIndexStatus_fieldAccessorTable.ensureFieldAccessorsInitialized(com.qlangtech.tis.grpc.TableSingleDataIndexStatus.class, com.qlangtech.tis.grpc.TableSingleDataIndexStatus.Builder.class);
        }

        // Construct using com.qlangtech.tis.grpc.TableSingleDataIndexStatus.newBuilder()
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
            internalGetMutableTableConsumeData().clear();
            bufferQueueRemainingCapacity_ = 0;
            bufferQueueUsedSize_ = 0;
            consumeErrorCount_ = 0;
            ignoreRowsCount_ = 0;
            uuid_ = "";
            tis30SAvgRT_ = 0L;
            incrProcessPaused_ = false;
            return this;
        }

        @java.lang.Override
        public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
            return com.qlangtech.tis.grpc.IncrStatusProto.internal_static_rpc_TableSingleDataIndexStatus_descriptor;
        }

        @java.lang.Override
        public com.qlangtech.tis.grpc.TableSingleDataIndexStatus getDefaultInstanceForType() {
            return com.qlangtech.tis.grpc.TableSingleDataIndexStatus.getDefaultInstance();
        }

        @java.lang.Override
        public com.qlangtech.tis.grpc.TableSingleDataIndexStatus build() {
            com.qlangtech.tis.grpc.TableSingleDataIndexStatus result = buildPartial();
            if (!result.isInitialized()) {
                throw newUninitializedMessageException(result);
            }
            return result;
        }

        @java.lang.Override
        public com.qlangtech.tis.grpc.TableSingleDataIndexStatus buildPartial() {
            com.qlangtech.tis.grpc.TableSingleDataIndexStatus result = new com.qlangtech.tis.grpc.TableSingleDataIndexStatus(this);
            int from_bitField0_ = bitField0_;
            int to_bitField0_ = 0;
            result.tableConsumeData_ = internalGetTableConsumeData();
            result.tableConsumeData_.makeImmutable();
            result.bufferQueueRemainingCapacity_ = bufferQueueRemainingCapacity_;
            result.bufferQueueUsedSize_ = bufferQueueUsedSize_;
            result.consumeErrorCount_ = consumeErrorCount_;
            result.ignoreRowsCount_ = ignoreRowsCount_;
            result.uuid_ = uuid_;
            result.tis30SAvgRT_ = tis30SAvgRT_;
            result.incrProcessPaused_ = incrProcessPaused_;
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
            if (other instanceof com.qlangtech.tis.grpc.TableSingleDataIndexStatus) {
                return mergeFrom((com.qlangtech.tis.grpc.TableSingleDataIndexStatus) other);
            } else {
                super.mergeFrom(other);
                return this;
            }
        }

        public Builder mergeFrom(com.qlangtech.tis.grpc.TableSingleDataIndexStatus other) {
            if (other == com.qlangtech.tis.grpc.TableSingleDataIndexStatus.getDefaultInstance())
                return this;
            internalGetMutableTableConsumeData().mergeFrom(other.internalGetTableConsumeData());
            if (other.getBufferQueueRemainingCapacity() != 0) {
                setBufferQueueRemainingCapacity(other.getBufferQueueRemainingCapacity());
            }
            if (other.getBufferQueueUsedSize() != 0) {
                setBufferQueueUsedSize(other.getBufferQueueUsedSize());
            }
            if (other.getConsumeErrorCount() != 0) {
                setConsumeErrorCount(other.getConsumeErrorCount());
            }
            if (other.getIgnoreRowsCount() != 0) {
                setIgnoreRowsCount(other.getIgnoreRowsCount());
            }
            if (!other.getUuid().isEmpty()) {
                uuid_ = other.uuid_;
                onChanged();
            }
            if (other.getTis30SAvgRT() != 0L) {
                setTis30SAvgRT(other.getTis30SAvgRT());
            }
            if (other.getIncrProcessPaused() != false) {
                setIncrProcessPaused(other.getIncrProcessPaused());
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
            com.qlangtech.tis.grpc.TableSingleDataIndexStatus parsedMessage = null;
            try {
                parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
            } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                parsedMessage = (com.qlangtech.tis.grpc.TableSingleDataIndexStatus) e.getUnfinishedMessage();
                throw e.unwrapIOException();
            } finally {
                if (parsedMessage != null) {
                    mergeFrom(parsedMessage);
                }
            }
            return this;
        }

        private int bitField0_;

        private com.google.protobuf.MapField<java.lang.String, java.lang.Long> tableConsumeData_;

        private com.google.protobuf.MapField<java.lang.String, java.lang.Long> internalGetTableConsumeData() {
            if (tableConsumeData_ == null) {
                return com.google.protobuf.MapField.emptyMapField(TableConsumeDataDefaultEntryHolder.defaultEntry);
            }
            return tableConsumeData_;
        }

        private com.google.protobuf.MapField<java.lang.String, java.lang.Long> internalGetMutableTableConsumeData() {
            onChanged();
            ;
            if (tableConsumeData_ == null) {
                tableConsumeData_ = com.google.protobuf.MapField.newMapField(TableConsumeDataDefaultEntryHolder.defaultEntry);
            }
            if (!tableConsumeData_.isMutable()) {
                tableConsumeData_ = tableConsumeData_.copy();
            }
            return tableConsumeData_;
        }

        public int getTableConsumeDataCount() {
            return internalGetTableConsumeData().getMap().size();
        }

        /**
         * <code>map&lt;string, uint64&gt; tableConsumeData = 1;</code>
         */
        public boolean containsTableConsumeData(java.lang.String key) {
            if (key == null) {
                throw new java.lang.NullPointerException();
            }
            return internalGetTableConsumeData().getMap().containsKey(key);
        }

        /**
         * Use {@link #getTableConsumeDataMap()} instead.
         */
        @java.lang.Deprecated
        public java.util.Map<java.lang.String, java.lang.Long> getTableConsumeData() {
            return getTableConsumeDataMap();
        }

        /**
         * <code>map&lt;string, uint64&gt; tableConsumeData = 1;</code>
         */
        public java.util.Map<java.lang.String, java.lang.Long> getTableConsumeDataMap() {
            return internalGetTableConsumeData().getMap();
        }

        /**
         * <code>map&lt;string, uint64&gt; tableConsumeData = 1;</code>
         */
        public long getTableConsumeDataOrDefault(java.lang.String key, long defaultValue) {
            if (key == null) {
                throw new java.lang.NullPointerException();
            }
            java.util.Map<java.lang.String, java.lang.Long> map = internalGetTableConsumeData().getMap();
            return map.containsKey(key) ? map.get(key) : defaultValue;
        }

        /**
         * <code>map&lt;string, uint64&gt; tableConsumeData = 1;</code>
         */
        public long getTableConsumeDataOrThrow(java.lang.String key) {
            if (key == null) {
                throw new java.lang.NullPointerException();
            }
            java.util.Map<java.lang.String, java.lang.Long> map = internalGetTableConsumeData().getMap();
            if (!map.containsKey(key)) {
                throw new java.lang.IllegalArgumentException();
            }
            return map.get(key);
        }

        public Builder clearTableConsumeData() {
            internalGetMutableTableConsumeData().getMutableMap().clear();
            return this;
        }

        /**
         * <code>map&lt;string, uint64&gt; tableConsumeData = 1;</code>
         */
        public Builder removeTableConsumeData(java.lang.String key) {
            if (key == null) {
                throw new java.lang.NullPointerException();
            }
            internalGetMutableTableConsumeData().getMutableMap().remove(key);
            return this;
        }

        /**
         * Use alternate mutation accessors instead.
         */
        @java.lang.Deprecated
        public java.util.Map<java.lang.String, java.lang.Long> getMutableTableConsumeData() {
            return internalGetMutableTableConsumeData().getMutableMap();
        }

        /**
         * <code>map&lt;string, uint64&gt; tableConsumeData = 1;</code>
         */
        public Builder putTableConsumeData(java.lang.String key, long value) {
            if (key == null) {
                throw new java.lang.NullPointerException();
            }
            internalGetMutableTableConsumeData().getMutableMap().put(key, value);
            return this;
        }

        /**
         * <code>map&lt;string, uint64&gt; tableConsumeData = 1;</code>
         */
        public Builder putAllTableConsumeData(java.util.Map<java.lang.String, java.lang.Long> values) {
            internalGetMutableTableConsumeData().getMutableMap().putAll(values);
            return this;
        }

        private int bufferQueueRemainingCapacity_;

        /**
         * <code>uint32 bufferQueueRemainingCapacity = 2;</code>
         */
        public int getBufferQueueRemainingCapacity() {
            return bufferQueueRemainingCapacity_;
        }

        /**
         * <code>uint32 bufferQueueRemainingCapacity = 2;</code>
         */
        public Builder setBufferQueueRemainingCapacity(int value) {
            bufferQueueRemainingCapacity_ = value;
            onChanged();
            return this;
        }

        /**
         * <code>uint32 bufferQueueRemainingCapacity = 2;</code>
         */
        public Builder clearBufferQueueRemainingCapacity() {
            bufferQueueRemainingCapacity_ = 0;
            onChanged();
            return this;
        }

        private int bufferQueueUsedSize_;

        /**
         * <code>uint32 bufferQueueUsedSize = 3;</code>
         */
        public int getBufferQueueUsedSize() {
            return bufferQueueUsedSize_;
        }

        /**
         * <code>uint32 bufferQueueUsedSize = 3;</code>
         */
        public Builder setBufferQueueUsedSize(int value) {
            bufferQueueUsedSize_ = value;
            onChanged();
            return this;
        }

        /**
         * <code>uint32 bufferQueueUsedSize = 3;</code>
         */
        public Builder clearBufferQueueUsedSize() {
            bufferQueueUsedSize_ = 0;
            onChanged();
            return this;
        }

        private int consumeErrorCount_;

        /**
         * <code>uint32 consumeErrorCount = 4;</code>
         */
        public int getConsumeErrorCount() {
            return consumeErrorCount_;
        }

        /**
         * <code>uint32 consumeErrorCount = 4;</code>
         */
        public Builder setConsumeErrorCount(int value) {
            consumeErrorCount_ = value;
            onChanged();
            return this;
        }

        /**
         * <code>uint32 consumeErrorCount = 4;</code>
         */
        public Builder clearConsumeErrorCount() {
            consumeErrorCount_ = 0;
            onChanged();
            return this;
        }

        private int ignoreRowsCount_;

        /**
         * <code>uint32 ignoreRowsCount = 5;</code>
         */
        public int getIgnoreRowsCount() {
            return ignoreRowsCount_;
        }

        /**
         * <code>uint32 ignoreRowsCount = 5;</code>
         */
        public Builder setIgnoreRowsCount(int value) {
            ignoreRowsCount_ = value;
            onChanged();
            return this;
        }

        /**
         * <code>uint32 ignoreRowsCount = 5;</code>
         */
        public Builder clearIgnoreRowsCount() {
            ignoreRowsCount_ = 0;
            onChanged();
            return this;
        }

        private java.lang.Object uuid_ = "";

        /**
         * <code>string uuid = 6;</code>
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
         * <code>string uuid = 6;</code>
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
         * <code>string uuid = 6;</code>
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
         * <code>string uuid = 6;</code>
         */
        public Builder clearUuid() {
            uuid_ = getDefaultInstance().getUuid();
            onChanged();
            return this;
        }

        /**
         * <code>string uuid = 6;</code>
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

        private long tis30SAvgRT_;

        /**
         * <code>uint64 tis30sAvgRT = 7;</code>
         */
        public long getTis30SAvgRT() {
            return tis30SAvgRT_;
        }

        /**
         * <code>uint64 tis30sAvgRT = 7;</code>
         */
        public Builder setTis30SAvgRT(long value) {
            tis30SAvgRT_ = value;
            onChanged();
            return this;
        }

        /**
         * <code>uint64 tis30sAvgRT = 7;</code>
         */
        public Builder clearTis30SAvgRT() {
            tis30SAvgRT_ = 0L;
            onChanged();
            return this;
        }

        private boolean incrProcessPaused_;

        /**
         * <pre>
         * 增量任务执行是否暂停
         * </pre>
         *
         * <code>bool incrProcessPaused = 8;</code>
         */
        public boolean getIncrProcessPaused() {
            return incrProcessPaused_;
        }

        /**
         * <pre>
         * 增量任务执行是否暂停
         * </pre>
         *
         * <code>bool incrProcessPaused = 8;</code>
         */
        public Builder setIncrProcessPaused(boolean value) {
            incrProcessPaused_ = value;
            onChanged();
            return this;
        }

        /**
         * <pre>
         * 增量任务执行是否暂停
         * </pre>
         *
         * <code>bool incrProcessPaused = 8;</code>
         */
        public Builder clearIncrProcessPaused() {
            incrProcessPaused_ = false;
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
        // @@protoc_insertion_point(builder_scope:rpc.TableSingleDataIndexStatus)
    }

    // @@protoc_insertion_point(class_scope:rpc.TableSingleDataIndexStatus)
    private static final com.qlangtech.tis.grpc.TableSingleDataIndexStatus DEFAULT_INSTANCE;

    static {
        DEFAULT_INSTANCE = new com.qlangtech.tis.grpc.TableSingleDataIndexStatus();
    }

    public static com.qlangtech.tis.grpc.TableSingleDataIndexStatus getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    private static final com.google.protobuf.Parser<TableSingleDataIndexStatus> PARSER = new com.google.protobuf.AbstractParser<TableSingleDataIndexStatus>() {

        @java.lang.Override
        public TableSingleDataIndexStatus parsePartialFrom(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
            return new TableSingleDataIndexStatus(input, extensionRegistry);
        }
    };

    public static com.google.protobuf.Parser<TableSingleDataIndexStatus> parser() {
        return PARSER;
    }

    @java.lang.Override
    public com.google.protobuf.Parser<TableSingleDataIndexStatus> getParserForType() {
        return PARSER;
    }

    @java.lang.Override
    public com.qlangtech.tis.grpc.TableSingleDataIndexStatus getDefaultInstanceForType() {
        return DEFAULT_INSTANCE;
    }
}
