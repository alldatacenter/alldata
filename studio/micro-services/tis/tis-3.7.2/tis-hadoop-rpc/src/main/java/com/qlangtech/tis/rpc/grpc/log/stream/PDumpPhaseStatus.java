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
 * Protobuf type {@code stream.PDumpPhaseStatus}
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/09/25
 */
public final class PDumpPhaseStatus extends com.google.protobuf.GeneratedMessageV3 implements // @@protoc_insertion_point(message_implements:stream.PDumpPhaseStatus)
PDumpPhaseStatusOrBuilder {

    private static final long serialVersionUID = 0L;

    // Use PDumpPhaseStatus.newBuilder() to construct.
    private PDumpPhaseStatus(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
        super(builder);
    }

    private PDumpPhaseStatus() {
    }

    @java.lang.Override
    public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
        return this.unknownFields;
    }

    private PDumpPhaseStatus(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
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
                                tablesDump_ = com.google.protobuf.MapField.newMapField(TablesDumpDefaultEntryHolder.defaultEntry);
                                mutable_bitField0_ |= 0x00000001;
                            }
                            com.google.protobuf.MapEntry<java.lang.String, com.qlangtech.tis.rpc.grpc.log.common.TableDumpStatus> tablesDump__ = input.readMessage(TablesDumpDefaultEntryHolder.defaultEntry.getParserForType(), extensionRegistry);
                            tablesDump_.getMutableMap().put(tablesDump__.getKey(), tablesDump__.getValue());
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
        return com.qlangtech.tis.rpc.grpc.log.stream.LogCollectorProto.internal_static_stream_PDumpPhaseStatus_descriptor;
    }

    @SuppressWarnings({ "rawtypes" })
    @java.lang.Override
    protected com.google.protobuf.MapField internalGetMapField(int number) {
        switch(number) {
            case 1:
                return internalGetTablesDump();
            default:
                throw new RuntimeException("Invalid map field number: " + number);
        }
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
        return com.qlangtech.tis.rpc.grpc.log.stream.LogCollectorProto.internal_static_stream_PDumpPhaseStatus_fieldAccessorTable.ensureFieldAccessorsInitialized(com.qlangtech.tis.rpc.grpc.log.stream.PDumpPhaseStatus.class, com.qlangtech.tis.rpc.grpc.log.stream.PDumpPhaseStatus.Builder.class);
    }

    public static final int TABLESDUMP_FIELD_NUMBER = 1;

    private static final class TablesDumpDefaultEntryHolder {

        static final com.google.protobuf.MapEntry<java.lang.String, com.qlangtech.tis.rpc.grpc.log.common.TableDumpStatus> defaultEntry = com.google.protobuf.MapEntry.<java.lang.String, com.qlangtech.tis.rpc.grpc.log.common.TableDumpStatus>newDefaultInstance(com.qlangtech.tis.rpc.grpc.log.stream.LogCollectorProto.internal_static_stream_PDumpPhaseStatus_TablesDumpEntry_descriptor, com.google.protobuf.WireFormat.FieldType.STRING, "", com.google.protobuf.WireFormat.FieldType.MESSAGE, com.qlangtech.tis.rpc.grpc.log.common.TableDumpStatus.getDefaultInstance());
    }

    private com.google.protobuf.MapField<java.lang.String, com.qlangtech.tis.rpc.grpc.log.common.TableDumpStatus> tablesDump_;

    private com.google.protobuf.MapField<java.lang.String, com.qlangtech.tis.rpc.grpc.log.common.TableDumpStatus> internalGetTablesDump() {
        if (tablesDump_ == null) {
            return com.google.protobuf.MapField.emptyMapField(TablesDumpDefaultEntryHolder.defaultEntry);
        }
        return tablesDump_;
    }

    public int getTablesDumpCount() {
        return internalGetTablesDump().getMap().size();
    }

    /**
     * <code>map&lt;string, .TableDumpStatus&gt; tablesDump = 1;</code>
     */
    public boolean containsTablesDump(java.lang.String key) {
        if (key == null) {
            throw new java.lang.NullPointerException();
        }
        return internalGetTablesDump().getMap().containsKey(key);
    }

    /**
     * Use {@link #getTablesDumpMap()} instead.
     */
    @java.lang.Deprecated
    public java.util.Map<java.lang.String, com.qlangtech.tis.rpc.grpc.log.common.TableDumpStatus> getTablesDump() {
        return getTablesDumpMap();
    }

    /**
     * <code>map&lt;string, .TableDumpStatus&gt; tablesDump = 1;</code>
     */
    public java.util.Map<java.lang.String, com.qlangtech.tis.rpc.grpc.log.common.TableDumpStatus> getTablesDumpMap() {
        return internalGetTablesDump().getMap();
    }

    /**
     * <code>map&lt;string, .TableDumpStatus&gt; tablesDump = 1;</code>
     */
    public com.qlangtech.tis.rpc.grpc.log.common.TableDumpStatus getTablesDumpOrDefault(java.lang.String key, com.qlangtech.tis.rpc.grpc.log.common.TableDumpStatus defaultValue) {
        if (key == null) {
            throw new java.lang.NullPointerException();
        }
        java.util.Map<java.lang.String, com.qlangtech.tis.rpc.grpc.log.common.TableDumpStatus> map = internalGetTablesDump().getMap();
        return map.containsKey(key) ? map.get(key) : defaultValue;
    }

    /**
     * <code>map&lt;string, .TableDumpStatus&gt; tablesDump = 1;</code>
     */
    public com.qlangtech.tis.rpc.grpc.log.common.TableDumpStatus getTablesDumpOrThrow(java.lang.String key) {
        if (key == null) {
            throw new java.lang.NullPointerException();
        }
        java.util.Map<java.lang.String, com.qlangtech.tis.rpc.grpc.log.common.TableDumpStatus> map = internalGetTablesDump().getMap();
        if (!map.containsKey(key)) {
            throw new java.lang.IllegalArgumentException();
        }
        return map.get(key);
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
        com.google.protobuf.GeneratedMessageV3.serializeStringMapTo(output, internalGetTablesDump(), TablesDumpDefaultEntryHolder.defaultEntry, 1);
        unknownFields.writeTo(output);
    }

    @java.lang.Override
    public int getSerializedSize() {
        int size = memoizedSize;
        if (size != -1)
            return size;
        size = 0;
        for (java.util.Map.Entry<java.lang.String, com.qlangtech.tis.rpc.grpc.log.common.TableDumpStatus> entry : internalGetTablesDump().getMap().entrySet()) {
            com.google.protobuf.MapEntry<java.lang.String, com.qlangtech.tis.rpc.grpc.log.common.TableDumpStatus> tablesDump__ = TablesDumpDefaultEntryHolder.defaultEntry.newBuilderForType().setKey(entry.getKey()).setValue(entry.getValue()).build();
            size += com.google.protobuf.CodedOutputStream.computeMessageSize(1, tablesDump__);
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
        if (!(obj instanceof com.qlangtech.tis.rpc.grpc.log.stream.PDumpPhaseStatus)) {
            return super.equals(obj);
        }
        com.qlangtech.tis.rpc.grpc.log.stream.PDumpPhaseStatus other = (com.qlangtech.tis.rpc.grpc.log.stream.PDumpPhaseStatus) obj;
        if (!internalGetTablesDump().equals(other.internalGetTablesDump()))
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
        if (!internalGetTablesDump().getMap().isEmpty()) {
            hash = (37 * hash) + TABLESDUMP_FIELD_NUMBER;
            hash = (53 * hash) + internalGetTablesDump().hashCode();
        }
        hash = (29 * hash) + unknownFields.hashCode();
        memoizedHashCode = hash;
        return hash;
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PDumpPhaseStatus parseFrom(java.nio.ByteBuffer data) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PDumpPhaseStatus parseFrom(java.nio.ByteBuffer data, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PDumpPhaseStatus parseFrom(com.google.protobuf.ByteString data) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PDumpPhaseStatus parseFrom(com.google.protobuf.ByteString data, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PDumpPhaseStatus parseFrom(byte[] data) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PDumpPhaseStatus parseFrom(byte[] data, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PDumpPhaseStatus parseFrom(java.io.InputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PDumpPhaseStatus parseFrom(java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PDumpPhaseStatus parseDelimitedFrom(java.io.InputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PDumpPhaseStatus parseDelimitedFrom(java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input, extensionRegistry);
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PDumpPhaseStatus parseFrom(com.google.protobuf.CodedInputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PDumpPhaseStatus parseFrom(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    @java.lang.Override
    public Builder newBuilderForType() {
        return newBuilder();
    }

    public static Builder newBuilder() {
        return DEFAULT_INSTANCE.toBuilder();
    }

    public static Builder newBuilder(com.qlangtech.tis.rpc.grpc.log.stream.PDumpPhaseStatus prototype) {
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
     * Protobuf type {@code stream.PDumpPhaseStatus}
     */
    public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements // @@protoc_insertion_point(builder_implements:stream.PDumpPhaseStatus)
    com.qlangtech.tis.rpc.grpc.log.stream.PDumpPhaseStatusOrBuilder {

        public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
            return com.qlangtech.tis.rpc.grpc.log.stream.LogCollectorProto.internal_static_stream_PDumpPhaseStatus_descriptor;
        }

        @SuppressWarnings({ "rawtypes" })
        protected com.google.protobuf.MapField internalGetMapField(int number) {
            switch(number) {
                case 1:
                    return internalGetTablesDump();
                default:
                    throw new RuntimeException("Invalid map field number: " + number);
            }
        }

        @SuppressWarnings({ "rawtypes" })
        protected com.google.protobuf.MapField internalGetMutableMapField(int number) {
            switch(number) {
                case 1:
                    return internalGetMutableTablesDump();
                default:
                    throw new RuntimeException("Invalid map field number: " + number);
            }
        }

        @java.lang.Override
        protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
            return com.qlangtech.tis.rpc.grpc.log.stream.LogCollectorProto.internal_static_stream_PDumpPhaseStatus_fieldAccessorTable.ensureFieldAccessorsInitialized(com.qlangtech.tis.rpc.grpc.log.stream.PDumpPhaseStatus.class, com.qlangtech.tis.rpc.grpc.log.stream.PDumpPhaseStatus.Builder.class);
        }

        // Construct using com.qlangtech.tis.rpc.grpc.log.stream.PDumpPhaseStatus.newBuilder()
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
            internalGetMutableTablesDump().clear();
            return this;
        }

        @java.lang.Override
        public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
            return com.qlangtech.tis.rpc.grpc.log.stream.LogCollectorProto.internal_static_stream_PDumpPhaseStatus_descriptor;
        }

        @java.lang.Override
        public com.qlangtech.tis.rpc.grpc.log.stream.PDumpPhaseStatus getDefaultInstanceForType() {
            return com.qlangtech.tis.rpc.grpc.log.stream.PDumpPhaseStatus.getDefaultInstance();
        }

        @java.lang.Override
        public com.qlangtech.tis.rpc.grpc.log.stream.PDumpPhaseStatus build() {
            com.qlangtech.tis.rpc.grpc.log.stream.PDumpPhaseStatus result = buildPartial();
            if (!result.isInitialized()) {
                throw newUninitializedMessageException(result);
            }
            return result;
        }

        @java.lang.Override
        public com.qlangtech.tis.rpc.grpc.log.stream.PDumpPhaseStatus buildPartial() {
            com.qlangtech.tis.rpc.grpc.log.stream.PDumpPhaseStatus result = new com.qlangtech.tis.rpc.grpc.log.stream.PDumpPhaseStatus(this);
            int from_bitField0_ = bitField0_;
            result.tablesDump_ = internalGetTablesDump();
            result.tablesDump_.makeImmutable();
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
            if (other instanceof com.qlangtech.tis.rpc.grpc.log.stream.PDumpPhaseStatus) {
                return mergeFrom((com.qlangtech.tis.rpc.grpc.log.stream.PDumpPhaseStatus) other);
            } else {
                super.mergeFrom(other);
                return this;
            }
        }

        public Builder mergeFrom(com.qlangtech.tis.rpc.grpc.log.stream.PDumpPhaseStatus other) {
            if (other == com.qlangtech.tis.rpc.grpc.log.stream.PDumpPhaseStatus.getDefaultInstance())
                return this;
            internalGetMutableTablesDump().mergeFrom(other.internalGetTablesDump());
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
            com.qlangtech.tis.rpc.grpc.log.stream.PDumpPhaseStatus parsedMessage = null;
            try {
                parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
            } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                parsedMessage = (com.qlangtech.tis.rpc.grpc.log.stream.PDumpPhaseStatus) e.getUnfinishedMessage();
                throw e.unwrapIOException();
            } finally {
                if (parsedMessage != null) {
                    mergeFrom(parsedMessage);
                }
            }
            return this;
        }

        private int bitField0_;

        private com.google.protobuf.MapField<java.lang.String, com.qlangtech.tis.rpc.grpc.log.common.TableDumpStatus> tablesDump_;

        private com.google.protobuf.MapField<java.lang.String, com.qlangtech.tis.rpc.grpc.log.common.TableDumpStatus> internalGetTablesDump() {
            if (tablesDump_ == null) {
                return com.google.protobuf.MapField.emptyMapField(TablesDumpDefaultEntryHolder.defaultEntry);
            }
            return tablesDump_;
        }

        private com.google.protobuf.MapField<java.lang.String, com.qlangtech.tis.rpc.grpc.log.common.TableDumpStatus> internalGetMutableTablesDump() {
            onChanged();
            ;
            if (tablesDump_ == null) {
                tablesDump_ = com.google.protobuf.MapField.newMapField(TablesDumpDefaultEntryHolder.defaultEntry);
            }
            if (!tablesDump_.isMutable()) {
                tablesDump_ = tablesDump_.copy();
            }
            return tablesDump_;
        }

        public int getTablesDumpCount() {
            return internalGetTablesDump().getMap().size();
        }

        /**
         * <code>map&lt;string, .TableDumpStatus&gt; tablesDump = 1;</code>
         */
        public boolean containsTablesDump(java.lang.String key) {
            if (key == null) {
                throw new java.lang.NullPointerException();
            }
            return internalGetTablesDump().getMap().containsKey(key);
        }

        /**
         * Use {@link #getTablesDumpMap()} instead.
         */
        @java.lang.Deprecated
        public java.util.Map<java.lang.String, com.qlangtech.tis.rpc.grpc.log.common.TableDumpStatus> getTablesDump() {
            return getTablesDumpMap();
        }

        /**
         * <code>map&lt;string, .TableDumpStatus&gt; tablesDump = 1;</code>
         */
        public java.util.Map<java.lang.String, com.qlangtech.tis.rpc.grpc.log.common.TableDumpStatus> getTablesDumpMap() {
            return internalGetTablesDump().getMap();
        }

        /**
         * <code>map&lt;string, .TableDumpStatus&gt; tablesDump = 1;</code>
         */
        public com.qlangtech.tis.rpc.grpc.log.common.TableDumpStatus getTablesDumpOrDefault(java.lang.String key, com.qlangtech.tis.rpc.grpc.log.common.TableDumpStatus defaultValue) {
            if (key == null) {
                throw new java.lang.NullPointerException();
            }
            java.util.Map<java.lang.String, com.qlangtech.tis.rpc.grpc.log.common.TableDumpStatus> map = internalGetTablesDump().getMap();
            return map.containsKey(key) ? map.get(key) : defaultValue;
        }

        /**
         * <code>map&lt;string, .TableDumpStatus&gt; tablesDump = 1;</code>
         */
        public com.qlangtech.tis.rpc.grpc.log.common.TableDumpStatus getTablesDumpOrThrow(java.lang.String key) {
            if (key == null) {
                throw new java.lang.NullPointerException();
            }
            java.util.Map<java.lang.String, com.qlangtech.tis.rpc.grpc.log.common.TableDumpStatus> map = internalGetTablesDump().getMap();
            if (!map.containsKey(key)) {
                throw new java.lang.IllegalArgumentException();
            }
            return map.get(key);
        }

        public Builder clearTablesDump() {
            internalGetMutableTablesDump().getMutableMap().clear();
            return this;
        }

        /**
         * <code>map&lt;string, .TableDumpStatus&gt; tablesDump = 1;</code>
         */
        public Builder removeTablesDump(java.lang.String key) {
            if (key == null) {
                throw new java.lang.NullPointerException();
            }
            internalGetMutableTablesDump().getMutableMap().remove(key);
            return this;
        }

        /**
         * Use alternate mutation accessors instead.
         */
        @java.lang.Deprecated
        public java.util.Map<java.lang.String, com.qlangtech.tis.rpc.grpc.log.common.TableDumpStatus> getMutableTablesDump() {
            return internalGetMutableTablesDump().getMutableMap();
        }

        /**
         * <code>map&lt;string, .TableDumpStatus&gt; tablesDump = 1;</code>
         */
        public Builder putTablesDump(java.lang.String key, com.qlangtech.tis.rpc.grpc.log.common.TableDumpStatus value) {
            if (key == null) {
                throw new java.lang.NullPointerException();
            }
            if (value == null) {
                throw new java.lang.NullPointerException();
            }
            internalGetMutableTablesDump().getMutableMap().put(key, value);
            return this;
        }

        /**
         * <code>map&lt;string, .TableDumpStatus&gt; tablesDump = 1;</code>
         */
        public Builder putAllTablesDump(java.util.Map<java.lang.String, com.qlangtech.tis.rpc.grpc.log.common.TableDumpStatus> values) {
            internalGetMutableTablesDump().getMutableMap().putAll(values);
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
        // @@protoc_insertion_point(builder_scope:stream.PDumpPhaseStatus)
    }

    // @@protoc_insertion_point(class_scope:stream.PDumpPhaseStatus)
    private static final com.qlangtech.tis.rpc.grpc.log.stream.PDumpPhaseStatus DEFAULT_INSTANCE;

    static {
        DEFAULT_INSTANCE = new com.qlangtech.tis.rpc.grpc.log.stream.PDumpPhaseStatus();
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PDumpPhaseStatus getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    private static final com.google.protobuf.Parser<PDumpPhaseStatus> PARSER = new com.google.protobuf.AbstractParser<PDumpPhaseStatus>() {

        @java.lang.Override
        public PDumpPhaseStatus parsePartialFrom(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
            return new PDumpPhaseStatus(input, extensionRegistry);
        }
    };

    public static com.google.protobuf.Parser<PDumpPhaseStatus> parser() {
        return PARSER;
    }

    @java.lang.Override
    public com.google.protobuf.Parser<PDumpPhaseStatus> getParserForType() {
        return PARSER;
    }

    @java.lang.Override
    public com.qlangtech.tis.rpc.grpc.log.stream.PDumpPhaseStatus getDefaultInstanceForType() {
        return DEFAULT_INSTANCE;
    }
}
