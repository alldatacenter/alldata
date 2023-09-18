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
 * Protobuf type {@code rpc.UpdateCounterMap}
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/09/25
 */
public final class UpdateCounterMap extends com.google.protobuf.GeneratedMessageV3 implements // @@protoc_insertion_point(message_implements:rpc.UpdateCounterMap)
UpdateCounterMapOrBuilder {

    private static final long serialVersionUID = 0L;

    // Use UpdateCounterMap.newBuilder() to construct.
    private UpdateCounterMap(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
        super(builder);
    }

    private UpdateCounterMap() {
        from_ = "";
    }

    @java.lang.Override
    public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
        return this.unknownFields;
    }

    private UpdateCounterMap(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
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
                                data_ = com.google.protobuf.MapField.newMapField(DataDefaultEntryHolder.defaultEntry);
                                mutable_bitField0_ |= 0x00000001;
                            }
                            com.google.protobuf.MapEntry<java.lang.String, com.qlangtech.tis.grpc.TableSingleDataIndexStatus> data__ = input.readMessage(DataDefaultEntryHolder.defaultEntry.getParserForType(), extensionRegistry);
                            data_.getMutableMap().put(data__.getKey(), data__.getValue());
                            break;
                        }
                    case 16:
                        {
                            gcCounter_ = input.readUInt64();
                            break;
                        }
                    case 26:
                        {
                            java.lang.String s = input.readStringRequireUtf8();
                            from_ = s;
                            break;
                        }
                    case 32:
                        {
                            updateTime_ = input.readUInt64();
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
        return com.qlangtech.tis.grpc.IncrStatusProto.internal_static_rpc_UpdateCounterMap_descriptor;
    }

    @SuppressWarnings({ "rawtypes" })
    @java.lang.Override
    protected com.google.protobuf.MapField internalGetMapField(int number) {
        switch(number) {
            case 1:
                return internalGetData();
            default:
                throw new RuntimeException("Invalid map field number: " + number);
        }
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
        return com.qlangtech.tis.grpc.IncrStatusProto.internal_static_rpc_UpdateCounterMap_fieldAccessorTable.ensureFieldAccessorsInitialized(com.qlangtech.tis.grpc.UpdateCounterMap.class, com.qlangtech.tis.grpc.UpdateCounterMap.Builder.class);
    }

    private int bitField0_;

    public static final int DATA_FIELD_NUMBER = 1;

    private static final class DataDefaultEntryHolder {

        static final com.google.protobuf.MapEntry<java.lang.String, com.qlangtech.tis.grpc.TableSingleDataIndexStatus> defaultEntry = com.google.protobuf.MapEntry.<java.lang.String, com.qlangtech.tis.grpc.TableSingleDataIndexStatus>newDefaultInstance(com.qlangtech.tis.grpc.IncrStatusProto.internal_static_rpc_UpdateCounterMap_DataEntry_descriptor, com.google.protobuf.WireFormat.FieldType.STRING, "", com.google.protobuf.WireFormat.FieldType.MESSAGE, com.qlangtech.tis.grpc.TableSingleDataIndexStatus.getDefaultInstance());
    }

    private com.google.protobuf.MapField<java.lang.String, com.qlangtech.tis.grpc.TableSingleDataIndexStatus> data_;

    private com.google.protobuf.MapField<java.lang.String, com.qlangtech.tis.grpc.TableSingleDataIndexStatus> internalGetData() {
        if (data_ == null) {
            return com.google.protobuf.MapField.emptyMapField(DataDefaultEntryHolder.defaultEntry);
        }
        return data_;
    }

    public int getDataCount() {
        return internalGetData().getMap().size();
    }

    /**
     * <code>map&lt;string, .rpc.TableSingleDataIndexStatus&gt; data = 1;</code>
     */
    public boolean containsData(java.lang.String key) {
        if (key == null) {
            throw new java.lang.NullPointerException();
        }
        return internalGetData().getMap().containsKey(key);
    }

    /**
     * Use {@link #getDataMap()} instead.
     */
    @java.lang.Deprecated
    public java.util.Map<java.lang.String, com.qlangtech.tis.grpc.TableSingleDataIndexStatus> getData() {
        return getDataMap();
    }

    /**
     * <code>map&lt;string, .rpc.TableSingleDataIndexStatus&gt; data = 1;</code>
     */
    public java.util.Map<java.lang.String, com.qlangtech.tis.grpc.TableSingleDataIndexStatus> getDataMap() {
        return internalGetData().getMap();
    }

    /**
     * <code>map&lt;string, .rpc.TableSingleDataIndexStatus&gt; data = 1;</code>
     */
    public com.qlangtech.tis.grpc.TableSingleDataIndexStatus getDataOrDefault(java.lang.String key, com.qlangtech.tis.grpc.TableSingleDataIndexStatus defaultValue) {
        if (key == null) {
            throw new java.lang.NullPointerException();
        }
        java.util.Map<java.lang.String, com.qlangtech.tis.grpc.TableSingleDataIndexStatus> map = internalGetData().getMap();
        return map.containsKey(key) ? map.get(key) : defaultValue;
    }

    /**
     * <code>map&lt;string, .rpc.TableSingleDataIndexStatus&gt; data = 1;</code>
     */
    public com.qlangtech.tis.grpc.TableSingleDataIndexStatus getDataOrThrow(java.lang.String key) {
        if (key == null) {
            throw new java.lang.NullPointerException();
        }
        java.util.Map<java.lang.String, com.qlangtech.tis.grpc.TableSingleDataIndexStatus> map = internalGetData().getMap();
        if (!map.containsKey(key)) {
            throw new java.lang.IllegalArgumentException();
        }
        return map.get(key);
    }

    public static final int GCCOUNTER_FIELD_NUMBER = 2;

    private long gcCounter_;

    /**
     * <code>uint64 gcCounter = 2;</code>
     */
    public long getGcCounter() {
        return gcCounter_;
    }

    public static final int FROM_FIELD_NUMBER = 3;

    private volatile java.lang.Object from_;

    /**
     * <pre>
     * 从哪个地址发送过来的
     * </pre>
     *
     * <code>string from = 3;</code>
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
     * <pre>
     * 从哪个地址发送过来的
     * </pre>
     *
     * <code>string from = 3;</code>
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

    public static final int UPDATETIME_FIELD_NUMBER = 4;

    private long updateTime_;

    /**
     * <code>uint64 updateTime = 4;</code>
     */
    public long getUpdateTime() {
        return updateTime_;
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
        com.google.protobuf.GeneratedMessageV3.serializeStringMapTo(output, internalGetData(), DataDefaultEntryHolder.defaultEntry, 1);
        if (gcCounter_ != 0L) {
            output.writeUInt64(2, gcCounter_);
        }
        if (!getFromBytes().isEmpty()) {
            com.google.protobuf.GeneratedMessageV3.writeString(output, 3, from_);
        }
        if (updateTime_ != 0L) {
            output.writeUInt64(4, updateTime_);
        }
        unknownFields.writeTo(output);
    }

    @java.lang.Override
    public int getSerializedSize() {
        int size = memoizedSize;
        if (size != -1)
            return size;
        size = 0;
        for (java.util.Map.Entry<java.lang.String, com.qlangtech.tis.grpc.TableSingleDataIndexStatus> entry : internalGetData().getMap().entrySet()) {
            com.google.protobuf.MapEntry<java.lang.String, com.qlangtech.tis.grpc.TableSingleDataIndexStatus> data__ = DataDefaultEntryHolder.defaultEntry.newBuilderForType().setKey(entry.getKey()).setValue(entry.getValue()).build();
            size += com.google.protobuf.CodedOutputStream.computeMessageSize(1, data__);
        }
        if (gcCounter_ != 0L) {
            size += com.google.protobuf.CodedOutputStream.computeUInt64Size(2, gcCounter_);
        }
        if (!getFromBytes().isEmpty()) {
            size += com.google.protobuf.GeneratedMessageV3.computeStringSize(3, from_);
        }
        if (updateTime_ != 0L) {
            size += com.google.protobuf.CodedOutputStream.computeUInt64Size(4, updateTime_);
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
        if (!(obj instanceof com.qlangtech.tis.grpc.UpdateCounterMap)) {
            return super.equals(obj);
        }
        com.qlangtech.tis.grpc.UpdateCounterMap other = (com.qlangtech.tis.grpc.UpdateCounterMap) obj;
        if (!internalGetData().equals(other.internalGetData()))
            return false;
        if (getGcCounter() != other.getGcCounter())
            return false;
        if (!getFrom().equals(other.getFrom()))
            return false;
        if (getUpdateTime() != other.getUpdateTime())
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
        if (!internalGetData().getMap().isEmpty()) {
            hash = (37 * hash) + DATA_FIELD_NUMBER;
            hash = (53 * hash) + internalGetData().hashCode();
        }
        hash = (37 * hash) + GCCOUNTER_FIELD_NUMBER;
        hash = (53 * hash) + com.google.protobuf.Internal.hashLong(getGcCounter());
        hash = (37 * hash) + FROM_FIELD_NUMBER;
        hash = (53 * hash) + getFrom().hashCode();
        hash = (37 * hash) + UPDATETIME_FIELD_NUMBER;
        hash = (53 * hash) + com.google.protobuf.Internal.hashLong(getUpdateTime());
        hash = (29 * hash) + unknownFields.hashCode();
        memoizedHashCode = hash;
        return hash;
    }

    public static com.qlangtech.tis.grpc.UpdateCounterMap parseFrom(java.nio.ByteBuffer data) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static com.qlangtech.tis.grpc.UpdateCounterMap parseFrom(java.nio.ByteBuffer data, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static com.qlangtech.tis.grpc.UpdateCounterMap parseFrom(com.google.protobuf.ByteString data) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static com.qlangtech.tis.grpc.UpdateCounterMap parseFrom(com.google.protobuf.ByteString data, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static com.qlangtech.tis.grpc.UpdateCounterMap parseFrom(byte[] data) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static com.qlangtech.tis.grpc.UpdateCounterMap parseFrom(byte[] data, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static com.qlangtech.tis.grpc.UpdateCounterMap parseFrom(java.io.InputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static com.qlangtech.tis.grpc.UpdateCounterMap parseFrom(java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static com.qlangtech.tis.grpc.UpdateCounterMap parseDelimitedFrom(java.io.InputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
    }

    public static com.qlangtech.tis.grpc.UpdateCounterMap parseDelimitedFrom(java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input, extensionRegistry);
    }

    public static com.qlangtech.tis.grpc.UpdateCounterMap parseFrom(com.google.protobuf.CodedInputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static com.qlangtech.tis.grpc.UpdateCounterMap parseFrom(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    @java.lang.Override
    public Builder newBuilderForType() {
        return newBuilder();
    }

    public static Builder newBuilder() {
        return DEFAULT_INSTANCE.toBuilder();
    }

    public static Builder newBuilder(com.qlangtech.tis.grpc.UpdateCounterMap prototype) {
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
     * Protobuf type {@code rpc.UpdateCounterMap}
     */
    public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements // @@protoc_insertion_point(builder_implements:rpc.UpdateCounterMap)
    com.qlangtech.tis.grpc.UpdateCounterMapOrBuilder {

        public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
            return com.qlangtech.tis.grpc.IncrStatusProto.internal_static_rpc_UpdateCounterMap_descriptor;
        }

        @SuppressWarnings({ "rawtypes" })
        protected com.google.protobuf.MapField internalGetMapField(int number) {
            switch(number) {
                case 1:
                    return internalGetData();
                default:
                    throw new RuntimeException("Invalid map field number: " + number);
            }
        }

        @SuppressWarnings({ "rawtypes" })
        protected com.google.protobuf.MapField internalGetMutableMapField(int number) {
            switch(number) {
                case 1:
                    return internalGetMutableData();
                default:
                    throw new RuntimeException("Invalid map field number: " + number);
            }
        }

        @java.lang.Override
        protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
            return com.qlangtech.tis.grpc.IncrStatusProto.internal_static_rpc_UpdateCounterMap_fieldAccessorTable.ensureFieldAccessorsInitialized(com.qlangtech.tis.grpc.UpdateCounterMap.class, com.qlangtech.tis.grpc.UpdateCounterMap.Builder.class);
        }

        // Construct using com.qlangtech.tis.grpc.UpdateCounterMap.newBuilder()
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
            internalGetMutableData().clear();
            gcCounter_ = 0L;
            from_ = "";
            updateTime_ = 0L;
            return this;
        }

        @java.lang.Override
        public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
            return com.qlangtech.tis.grpc.IncrStatusProto.internal_static_rpc_UpdateCounterMap_descriptor;
        }

        @java.lang.Override
        public com.qlangtech.tis.grpc.UpdateCounterMap getDefaultInstanceForType() {
            return com.qlangtech.tis.grpc.UpdateCounterMap.getDefaultInstance();
        }

        @java.lang.Override
        public com.qlangtech.tis.grpc.UpdateCounterMap build() {
            com.qlangtech.tis.grpc.UpdateCounterMap result = buildPartial();
            if (!result.isInitialized()) {
                throw newUninitializedMessageException(result);
            }
            return result;
        }

        @java.lang.Override
        public com.qlangtech.tis.grpc.UpdateCounterMap buildPartial() {
            com.qlangtech.tis.grpc.UpdateCounterMap result = new com.qlangtech.tis.grpc.UpdateCounterMap(this);
            int from_bitField0_ = bitField0_;
            int to_bitField0_ = 0;
            result.data_ = internalGetData();
            result.data_.makeImmutable();
            result.gcCounter_ = gcCounter_;
            result.from_ = from_;
            result.updateTime_ = updateTime_;
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
            if (other instanceof com.qlangtech.tis.grpc.UpdateCounterMap) {
                return mergeFrom((com.qlangtech.tis.grpc.UpdateCounterMap) other);
            } else {
                super.mergeFrom(other);
                return this;
            }
        }

        public Builder mergeFrom(com.qlangtech.tis.grpc.UpdateCounterMap other) {
            if (other == com.qlangtech.tis.grpc.UpdateCounterMap.getDefaultInstance())
                return this;
            internalGetMutableData().mergeFrom(other.internalGetData());
            if (other.getGcCounter() != 0L) {
                setGcCounter(other.getGcCounter());
            }
            if (!other.getFrom().isEmpty()) {
                from_ = other.from_;
                onChanged();
            }
            if (other.getUpdateTime() != 0L) {
                setUpdateTime(other.getUpdateTime());
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
            com.qlangtech.tis.grpc.UpdateCounterMap parsedMessage = null;
            try {
                parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
            } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                parsedMessage = (com.qlangtech.tis.grpc.UpdateCounterMap) e.getUnfinishedMessage();
                throw e.unwrapIOException();
            } finally {
                if (parsedMessage != null) {
                    mergeFrom(parsedMessage);
                }
            }
            return this;
        }

        private int bitField0_;

        private com.google.protobuf.MapField<java.lang.String, com.qlangtech.tis.grpc.TableSingleDataIndexStatus> data_;

        private com.google.protobuf.MapField<java.lang.String, com.qlangtech.tis.grpc.TableSingleDataIndexStatus> internalGetData() {
            if (data_ == null) {
                return com.google.protobuf.MapField.emptyMapField(DataDefaultEntryHolder.defaultEntry);
            }
            return data_;
        }

        private com.google.protobuf.MapField<java.lang.String, com.qlangtech.tis.grpc.TableSingleDataIndexStatus> internalGetMutableData() {
            onChanged();
            ;
            if (data_ == null) {
                data_ = com.google.protobuf.MapField.newMapField(DataDefaultEntryHolder.defaultEntry);
            }
            if (!data_.isMutable()) {
                data_ = data_.copy();
            }
            return data_;
        }

        public int getDataCount() {
            return internalGetData().getMap().size();
        }

        /**
         * <code>map&lt;string, .rpc.TableSingleDataIndexStatus&gt; data = 1;</code>
         */
        public boolean containsData(java.lang.String key) {
            if (key == null) {
                throw new java.lang.NullPointerException();
            }
            return internalGetData().getMap().containsKey(key);
        }

        /**
         * Use {@link #getDataMap()} instead.
         */
        @java.lang.Deprecated
        public java.util.Map<java.lang.String, com.qlangtech.tis.grpc.TableSingleDataIndexStatus> getData() {
            return getDataMap();
        }

        /**
         * <code>map&lt;string, .rpc.TableSingleDataIndexStatus&gt; data = 1;</code>
         */
        public java.util.Map<java.lang.String, com.qlangtech.tis.grpc.TableSingleDataIndexStatus> getDataMap() {
            return internalGetData().getMap();
        }

        /**
         * <code>map&lt;string, .rpc.TableSingleDataIndexStatus&gt; data = 1;</code>
         */
        public com.qlangtech.tis.grpc.TableSingleDataIndexStatus getDataOrDefault(java.lang.String key, com.qlangtech.tis.grpc.TableSingleDataIndexStatus defaultValue) {
            if (key == null) {
                throw new java.lang.NullPointerException();
            }
            java.util.Map<java.lang.String, com.qlangtech.tis.grpc.TableSingleDataIndexStatus> map = internalGetData().getMap();
            return map.containsKey(key) ? map.get(key) : defaultValue;
        }

        /**
         * <code>map&lt;string, .rpc.TableSingleDataIndexStatus&gt; data = 1;</code>
         */
        public com.qlangtech.tis.grpc.TableSingleDataIndexStatus getDataOrThrow(java.lang.String key) {
            if (key == null) {
                throw new java.lang.NullPointerException();
            }
            java.util.Map<java.lang.String, com.qlangtech.tis.grpc.TableSingleDataIndexStatus> map = internalGetData().getMap();
            if (!map.containsKey(key)) {
                throw new java.lang.IllegalArgumentException();
            }
            return map.get(key);
        }

        public Builder clearData() {
            internalGetMutableData().getMutableMap().clear();
            return this;
        }

        /**
         * <code>map&lt;string, .rpc.TableSingleDataIndexStatus&gt; data = 1;</code>
         */
        public Builder removeData(java.lang.String key) {
            if (key == null) {
                throw new java.lang.NullPointerException();
            }
            internalGetMutableData().getMutableMap().remove(key);
            return this;
        }

        /**
         * Use alternate mutation accessors instead.
         */
        @java.lang.Deprecated
        public java.util.Map<java.lang.String, com.qlangtech.tis.grpc.TableSingleDataIndexStatus> getMutableData() {
            return internalGetMutableData().getMutableMap();
        }

        /**
         * <code>map&lt;string, .rpc.TableSingleDataIndexStatus&gt; data = 1;</code>
         */
        public Builder putData(java.lang.String key, com.qlangtech.tis.grpc.TableSingleDataIndexStatus value) {
            if (key == null) {
                throw new java.lang.NullPointerException();
            }
            if (value == null) {
                throw new java.lang.NullPointerException();
            }
            internalGetMutableData().getMutableMap().put(key, value);
            return this;
        }

        /**
         * <code>map&lt;string, .rpc.TableSingleDataIndexStatus&gt; data = 1;</code>
         */
        public Builder putAllData(java.util.Map<java.lang.String, com.qlangtech.tis.grpc.TableSingleDataIndexStatus> values) {
            internalGetMutableData().getMutableMap().putAll(values);
            return this;
        }

        private long gcCounter_;

        /**
         * <code>uint64 gcCounter = 2;</code>
         */
        public long getGcCounter() {
            return gcCounter_;
        }

        /**
         * <code>uint64 gcCounter = 2;</code>
         */
        public Builder setGcCounter(long value) {
            gcCounter_ = value;
            onChanged();
            return this;
        }

        /**
         * <code>uint64 gcCounter = 2;</code>
         */
        public Builder clearGcCounter() {
            gcCounter_ = 0L;
            onChanged();
            return this;
        }

        private java.lang.Object from_ = "";

        /**
         * <pre>
         * 从哪个地址发送过来的
         * </pre>
         *
         * <code>string from = 3;</code>
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
         * <pre>
         * 从哪个地址发送过来的
         * </pre>
         *
         * <code>string from = 3;</code>
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
         * <pre>
         * 从哪个地址发送过来的
         * </pre>
         *
         * <code>string from = 3;</code>
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
         * <pre>
         * 从哪个地址发送过来的
         * </pre>
         *
         * <code>string from = 3;</code>
         */
        public Builder clearFrom() {
            from_ = getDefaultInstance().getFrom();
            onChanged();
            return this;
        }

        /**
         * <pre>
         * 从哪个地址发送过来的
         * </pre>
         *
         * <code>string from = 3;</code>
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

        private long updateTime_;

        /**
         * <code>uint64 updateTime = 4;</code>
         */
        public long getUpdateTime() {
            return updateTime_;
        }

        /**
         * <code>uint64 updateTime = 4;</code>
         */
        public Builder setUpdateTime(long value) {
            updateTime_ = value;
            onChanged();
            return this;
        }

        /**
         * <code>uint64 updateTime = 4;</code>
         */
        public Builder clearUpdateTime() {
            updateTime_ = 0L;
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
        // @@protoc_insertion_point(builder_scope:rpc.UpdateCounterMap)
    }

    // @@protoc_insertion_point(class_scope:rpc.UpdateCounterMap)
    private static final com.qlangtech.tis.grpc.UpdateCounterMap DEFAULT_INSTANCE;

    static {
        DEFAULT_INSTANCE = new com.qlangtech.tis.grpc.UpdateCounterMap();
    }

    public static com.qlangtech.tis.grpc.UpdateCounterMap getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    private static final com.google.protobuf.Parser<UpdateCounterMap> PARSER = new com.google.protobuf.AbstractParser<UpdateCounterMap>() {

        @java.lang.Override
        public UpdateCounterMap parsePartialFrom(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
            return new UpdateCounterMap(input, extensionRegistry);
        }
    };

    public static com.google.protobuf.Parser<UpdateCounterMap> parser() {
        return PARSER;
    }

    @java.lang.Override
    public com.google.protobuf.Parser<UpdateCounterMap> getParserForType() {
        return PARSER;
    }

    @java.lang.Override
    public com.qlangtech.tis.grpc.UpdateCounterMap getDefaultInstanceForType() {
        return DEFAULT_INSTANCE;
    }
}
