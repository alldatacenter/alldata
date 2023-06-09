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
 * Protobuf type {@code rpc.LaunchReportInfo}
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/09/25
 */
public final class LaunchReportInfo extends com.google.protobuf.GeneratedMessageV3 implements // @@protoc_insertion_point(message_implements:rpc.LaunchReportInfo)
LaunchReportInfoOrBuilder {

    private static final long serialVersionUID = 0L;

    // Use LaunchReportInfo.newBuilder() to construct.
    private LaunchReportInfo(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
        super(builder);
    }

    private LaunchReportInfo() {
    }

    @java.lang.Override
    public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
        return this.unknownFields;
    }

    private LaunchReportInfo(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
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
                                collectionFocusTopicInfo_ = com.google.protobuf.MapField.newMapField(CollectionFocusTopicInfoDefaultEntryHolder.defaultEntry);
                                mutable_bitField0_ |= 0x00000001;
                            }
                            com.google.protobuf.MapEntry<java.lang.String, com.qlangtech.tis.grpc.TopicInfo> collectionFocusTopicInfo__ = input.readMessage(CollectionFocusTopicInfoDefaultEntryHolder.defaultEntry.getParserForType(), extensionRegistry);
                            collectionFocusTopicInfo_.getMutableMap().put(collectionFocusTopicInfo__.getKey(), collectionFocusTopicInfo__.getValue());
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
        return com.qlangtech.tis.grpc.IncrStatusProto.internal_static_rpc_LaunchReportInfo_descriptor;
    }

    @SuppressWarnings({ "rawtypes" })
    @java.lang.Override
    protected com.google.protobuf.MapField internalGetMapField(int number) {
        switch(number) {
            case 1:
                return internalGetCollectionFocusTopicInfo();
            default:
                throw new RuntimeException("Invalid map field number: " + number);
        }
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
        return com.qlangtech.tis.grpc.IncrStatusProto.internal_static_rpc_LaunchReportInfo_fieldAccessorTable.ensureFieldAccessorsInitialized(com.qlangtech.tis.grpc.LaunchReportInfo.class, com.qlangtech.tis.grpc.LaunchReportInfo.Builder.class);
    }

    public static final int COLLECTIONFOCUSTOPICINFO_FIELD_NUMBER = 1;

    private static final class CollectionFocusTopicInfoDefaultEntryHolder {

        static final com.google.protobuf.MapEntry<java.lang.String, com.qlangtech.tis.grpc.TopicInfo> defaultEntry = com.google.protobuf.MapEntry.<java.lang.String, com.qlangtech.tis.grpc.TopicInfo>newDefaultInstance(com.qlangtech.tis.grpc.IncrStatusProto.internal_static_rpc_LaunchReportInfo_CollectionFocusTopicInfoEntry_descriptor, com.google.protobuf.WireFormat.FieldType.STRING, "", com.google.protobuf.WireFormat.FieldType.MESSAGE, com.qlangtech.tis.grpc.TopicInfo.getDefaultInstance());
    }

    private com.google.protobuf.MapField<java.lang.String, com.qlangtech.tis.grpc.TopicInfo> collectionFocusTopicInfo_;

    private com.google.protobuf.MapField<java.lang.String, com.qlangtech.tis.grpc.TopicInfo> internalGetCollectionFocusTopicInfo() {
        if (collectionFocusTopicInfo_ == null) {
            return com.google.protobuf.MapField.emptyMapField(CollectionFocusTopicInfoDefaultEntryHolder.defaultEntry);
        }
        return collectionFocusTopicInfo_;
    }

    public int getCollectionFocusTopicInfoCount() {
        return internalGetCollectionFocusTopicInfo().getMap().size();
    }

    /**
     * <code>map&lt;string, .rpc.TopicInfo&gt; collectionFocusTopicInfo = 1;</code>
     */
    public boolean containsCollectionFocusTopicInfo(java.lang.String key) {
        if (key == null) {
            throw new java.lang.NullPointerException();
        }
        return internalGetCollectionFocusTopicInfo().getMap().containsKey(key);
    }

    /**
     * Use {@link #getCollectionFocusTopicInfoMap()} instead.
     */
    @java.lang.Deprecated
    public java.util.Map<java.lang.String, com.qlangtech.tis.grpc.TopicInfo> getCollectionFocusTopicInfo() {
        return getCollectionFocusTopicInfoMap();
    }

    /**
     * <code>map&lt;string, .rpc.TopicInfo&gt; collectionFocusTopicInfo = 1;</code>
     */
    public java.util.Map<java.lang.String, com.qlangtech.tis.grpc.TopicInfo> getCollectionFocusTopicInfoMap() {
        return internalGetCollectionFocusTopicInfo().getMap();
    }

    /**
     * <code>map&lt;string, .rpc.TopicInfo&gt; collectionFocusTopicInfo = 1;</code>
     */
    public com.qlangtech.tis.grpc.TopicInfo getCollectionFocusTopicInfoOrDefault(java.lang.String key, com.qlangtech.tis.grpc.TopicInfo defaultValue) {
        if (key == null) {
            throw new java.lang.NullPointerException();
        }
        java.util.Map<java.lang.String, com.qlangtech.tis.grpc.TopicInfo> map = internalGetCollectionFocusTopicInfo().getMap();
        return map.containsKey(key) ? map.get(key) : defaultValue;
    }

    /**
     * <code>map&lt;string, .rpc.TopicInfo&gt; collectionFocusTopicInfo = 1;</code>
     */
    public com.qlangtech.tis.grpc.TopicInfo getCollectionFocusTopicInfoOrThrow(java.lang.String key) {
        if (key == null) {
            throw new java.lang.NullPointerException();
        }
        java.util.Map<java.lang.String, com.qlangtech.tis.grpc.TopicInfo> map = internalGetCollectionFocusTopicInfo().getMap();
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
        com.google.protobuf.GeneratedMessageV3.serializeStringMapTo(output, internalGetCollectionFocusTopicInfo(), CollectionFocusTopicInfoDefaultEntryHolder.defaultEntry, 1);
        unknownFields.writeTo(output);
    }

    @java.lang.Override
    public int getSerializedSize() {
        int size = memoizedSize;
        if (size != -1)
            return size;
        size = 0;
        for (java.util.Map.Entry<java.lang.String, com.qlangtech.tis.grpc.TopicInfo> entry : internalGetCollectionFocusTopicInfo().getMap().entrySet()) {
            com.google.protobuf.MapEntry<java.lang.String, com.qlangtech.tis.grpc.TopicInfo> collectionFocusTopicInfo__ = CollectionFocusTopicInfoDefaultEntryHolder.defaultEntry.newBuilderForType().setKey(entry.getKey()).setValue(entry.getValue()).build();
            size += com.google.protobuf.CodedOutputStream.computeMessageSize(1, collectionFocusTopicInfo__);
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
        if (!(obj instanceof com.qlangtech.tis.grpc.LaunchReportInfo)) {
            return super.equals(obj);
        }
        com.qlangtech.tis.grpc.LaunchReportInfo other = (com.qlangtech.tis.grpc.LaunchReportInfo) obj;
        if (!internalGetCollectionFocusTopicInfo().equals(other.internalGetCollectionFocusTopicInfo()))
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
        if (!internalGetCollectionFocusTopicInfo().getMap().isEmpty()) {
            hash = (37 * hash) + COLLECTIONFOCUSTOPICINFO_FIELD_NUMBER;
            hash = (53 * hash) + internalGetCollectionFocusTopicInfo().hashCode();
        }
        hash = (29 * hash) + unknownFields.hashCode();
        memoizedHashCode = hash;
        return hash;
    }

    public static com.qlangtech.tis.grpc.LaunchReportInfo parseFrom(java.nio.ByteBuffer data) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static com.qlangtech.tis.grpc.LaunchReportInfo parseFrom(java.nio.ByteBuffer data, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static com.qlangtech.tis.grpc.LaunchReportInfo parseFrom(com.google.protobuf.ByteString data) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static com.qlangtech.tis.grpc.LaunchReportInfo parseFrom(com.google.protobuf.ByteString data, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static com.qlangtech.tis.grpc.LaunchReportInfo parseFrom(byte[] data) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static com.qlangtech.tis.grpc.LaunchReportInfo parseFrom(byte[] data, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static com.qlangtech.tis.grpc.LaunchReportInfo parseFrom(java.io.InputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static com.qlangtech.tis.grpc.LaunchReportInfo parseFrom(java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static com.qlangtech.tis.grpc.LaunchReportInfo parseDelimitedFrom(java.io.InputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
    }

    public static com.qlangtech.tis.grpc.LaunchReportInfo parseDelimitedFrom(java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input, extensionRegistry);
    }

    public static com.qlangtech.tis.grpc.LaunchReportInfo parseFrom(com.google.protobuf.CodedInputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static com.qlangtech.tis.grpc.LaunchReportInfo parseFrom(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    @java.lang.Override
    public Builder newBuilderForType() {
        return newBuilder();
    }

    public static Builder newBuilder() {
        return DEFAULT_INSTANCE.toBuilder();
    }

    public static Builder newBuilder(com.qlangtech.tis.grpc.LaunchReportInfo prototype) {
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
     * Protobuf type {@code rpc.LaunchReportInfo}
     */
    public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements // @@protoc_insertion_point(builder_implements:rpc.LaunchReportInfo)
    com.qlangtech.tis.grpc.LaunchReportInfoOrBuilder {

        public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
            return com.qlangtech.tis.grpc.IncrStatusProto.internal_static_rpc_LaunchReportInfo_descriptor;
        }

        @SuppressWarnings({ "rawtypes" })
        protected com.google.protobuf.MapField internalGetMapField(int number) {
            switch(number) {
                case 1:
                    return internalGetCollectionFocusTopicInfo();
                default:
                    throw new RuntimeException("Invalid map field number: " + number);
            }
        }

        @SuppressWarnings({ "rawtypes" })
        protected com.google.protobuf.MapField internalGetMutableMapField(int number) {
            switch(number) {
                case 1:
                    return internalGetMutableCollectionFocusTopicInfo();
                default:
                    throw new RuntimeException("Invalid map field number: " + number);
            }
        }

        @java.lang.Override
        protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
            return com.qlangtech.tis.grpc.IncrStatusProto.internal_static_rpc_LaunchReportInfo_fieldAccessorTable.ensureFieldAccessorsInitialized(com.qlangtech.tis.grpc.LaunchReportInfo.class, com.qlangtech.tis.grpc.LaunchReportInfo.Builder.class);
        }

        // Construct using com.qlangtech.tis.grpc.LaunchReportInfo.newBuilder()
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
            internalGetMutableCollectionFocusTopicInfo().clear();
            return this;
        }

        @java.lang.Override
        public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
            return com.qlangtech.tis.grpc.IncrStatusProto.internal_static_rpc_LaunchReportInfo_descriptor;
        }

        @java.lang.Override
        public com.qlangtech.tis.grpc.LaunchReportInfo getDefaultInstanceForType() {
            return com.qlangtech.tis.grpc.LaunchReportInfo.getDefaultInstance();
        }

        @java.lang.Override
        public com.qlangtech.tis.grpc.LaunchReportInfo build() {
            com.qlangtech.tis.grpc.LaunchReportInfo result = buildPartial();
            if (!result.isInitialized()) {
                throw newUninitializedMessageException(result);
            }
            return result;
        }

        @java.lang.Override
        public com.qlangtech.tis.grpc.LaunchReportInfo buildPartial() {
            com.qlangtech.tis.grpc.LaunchReportInfo result = new com.qlangtech.tis.grpc.LaunchReportInfo(this);
            int from_bitField0_ = bitField0_;
            result.collectionFocusTopicInfo_ = internalGetCollectionFocusTopicInfo();
            result.collectionFocusTopicInfo_.makeImmutable();
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
            if (other instanceof com.qlangtech.tis.grpc.LaunchReportInfo) {
                return mergeFrom((com.qlangtech.tis.grpc.LaunchReportInfo) other);
            } else {
                super.mergeFrom(other);
                return this;
            }
        }

        public Builder mergeFrom(com.qlangtech.tis.grpc.LaunchReportInfo other) {
            if (other == com.qlangtech.tis.grpc.LaunchReportInfo.getDefaultInstance())
                return this;
            internalGetMutableCollectionFocusTopicInfo().mergeFrom(other.internalGetCollectionFocusTopicInfo());
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
            com.qlangtech.tis.grpc.LaunchReportInfo parsedMessage = null;
            try {
                parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
            } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                parsedMessage = (com.qlangtech.tis.grpc.LaunchReportInfo) e.getUnfinishedMessage();
                throw e.unwrapIOException();
            } finally {
                if (parsedMessage != null) {
                    mergeFrom(parsedMessage);
                }
            }
            return this;
        }

        private int bitField0_;

        private com.google.protobuf.MapField<java.lang.String, com.qlangtech.tis.grpc.TopicInfo> collectionFocusTopicInfo_;

        private com.google.protobuf.MapField<java.lang.String, com.qlangtech.tis.grpc.TopicInfo> internalGetCollectionFocusTopicInfo() {
            if (collectionFocusTopicInfo_ == null) {
                return com.google.protobuf.MapField.emptyMapField(CollectionFocusTopicInfoDefaultEntryHolder.defaultEntry);
            }
            return collectionFocusTopicInfo_;
        }

        private com.google.protobuf.MapField<java.lang.String, com.qlangtech.tis.grpc.TopicInfo> internalGetMutableCollectionFocusTopicInfo() {
            onChanged();
            ;
            if (collectionFocusTopicInfo_ == null) {
                collectionFocusTopicInfo_ = com.google.protobuf.MapField.newMapField(CollectionFocusTopicInfoDefaultEntryHolder.defaultEntry);
            }
            if (!collectionFocusTopicInfo_.isMutable()) {
                collectionFocusTopicInfo_ = collectionFocusTopicInfo_.copy();
            }
            return collectionFocusTopicInfo_;
        }

        public int getCollectionFocusTopicInfoCount() {
            return internalGetCollectionFocusTopicInfo().getMap().size();
        }

        /**
         * <code>map&lt;string, .rpc.TopicInfo&gt; collectionFocusTopicInfo = 1;</code>
         */
        public boolean containsCollectionFocusTopicInfo(java.lang.String key) {
            if (key == null) {
                throw new java.lang.NullPointerException();
            }
            return internalGetCollectionFocusTopicInfo().getMap().containsKey(key);
        }

        /**
         * Use {@link #getCollectionFocusTopicInfoMap()} instead.
         */
        @java.lang.Deprecated
        public java.util.Map<java.lang.String, com.qlangtech.tis.grpc.TopicInfo> getCollectionFocusTopicInfo() {
            return getCollectionFocusTopicInfoMap();
        }

        /**
         * <code>map&lt;string, .rpc.TopicInfo&gt; collectionFocusTopicInfo = 1;</code>
         */
        public java.util.Map<java.lang.String, com.qlangtech.tis.grpc.TopicInfo> getCollectionFocusTopicInfoMap() {
            return internalGetCollectionFocusTopicInfo().getMap();
        }

        /**
         * <code>map&lt;string, .rpc.TopicInfo&gt; collectionFocusTopicInfo = 1;</code>
         */
        public com.qlangtech.tis.grpc.TopicInfo getCollectionFocusTopicInfoOrDefault(java.lang.String key, com.qlangtech.tis.grpc.TopicInfo defaultValue) {
            if (key == null) {
                throw new java.lang.NullPointerException();
            }
            java.util.Map<java.lang.String, com.qlangtech.tis.grpc.TopicInfo> map = internalGetCollectionFocusTopicInfo().getMap();
            return map.containsKey(key) ? map.get(key) : defaultValue;
        }

        /**
         * <code>map&lt;string, .rpc.TopicInfo&gt; collectionFocusTopicInfo = 1;</code>
         */
        public com.qlangtech.tis.grpc.TopicInfo getCollectionFocusTopicInfoOrThrow(java.lang.String key) {
            if (key == null) {
                throw new java.lang.NullPointerException();
            }
            java.util.Map<java.lang.String, com.qlangtech.tis.grpc.TopicInfo> map = internalGetCollectionFocusTopicInfo().getMap();
            if (!map.containsKey(key)) {
                throw new java.lang.IllegalArgumentException();
            }
            return map.get(key);
        }

        public Builder clearCollectionFocusTopicInfo() {
            internalGetMutableCollectionFocusTopicInfo().getMutableMap().clear();
            return this;
        }

        /**
         * <code>map&lt;string, .rpc.TopicInfo&gt; collectionFocusTopicInfo = 1;</code>
         */
        public Builder removeCollectionFocusTopicInfo(java.lang.String key) {
            if (key == null) {
                throw new java.lang.NullPointerException();
            }
            internalGetMutableCollectionFocusTopicInfo().getMutableMap().remove(key);
            return this;
        }

        /**
         * Use alternate mutation accessors instead.
         */
        @java.lang.Deprecated
        public java.util.Map<java.lang.String, com.qlangtech.tis.grpc.TopicInfo> getMutableCollectionFocusTopicInfo() {
            return internalGetMutableCollectionFocusTopicInfo().getMutableMap();
        }

        /**
         * <code>map&lt;string, .rpc.TopicInfo&gt; collectionFocusTopicInfo = 1;</code>
         */
        public Builder putCollectionFocusTopicInfo(java.lang.String key, com.qlangtech.tis.grpc.TopicInfo value) {
            if (key == null) {
                throw new java.lang.NullPointerException();
            }
            if (value == null) {
                throw new java.lang.NullPointerException();
            }
            internalGetMutableCollectionFocusTopicInfo().getMutableMap().put(key, value);
            return this;
        }

        /**
         * <code>map&lt;string, .rpc.TopicInfo&gt; collectionFocusTopicInfo = 1;</code>
         */
        public Builder putAllCollectionFocusTopicInfo(java.util.Map<java.lang.String, com.qlangtech.tis.grpc.TopicInfo> values) {
            internalGetMutableCollectionFocusTopicInfo().getMutableMap().putAll(values);
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
        // @@protoc_insertion_point(builder_scope:rpc.LaunchReportInfo)
    }

    // @@protoc_insertion_point(class_scope:rpc.LaunchReportInfo)
    private static final com.qlangtech.tis.grpc.LaunchReportInfo DEFAULT_INSTANCE;

    static {
        DEFAULT_INSTANCE = new com.qlangtech.tis.grpc.LaunchReportInfo();
    }

    public static com.qlangtech.tis.grpc.LaunchReportInfo getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    private static final com.google.protobuf.Parser<LaunchReportInfo> PARSER = new com.google.protobuf.AbstractParser<LaunchReportInfo>() {

        @java.lang.Override
        public LaunchReportInfo parsePartialFrom(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
            return new LaunchReportInfo(input, extensionRegistry);
        }
    };

    public static com.google.protobuf.Parser<LaunchReportInfo> parser() {
        return PARSER;
    }

    @java.lang.Override
    public com.google.protobuf.Parser<LaunchReportInfo> getParserForType() {
        return PARSER;
    }

    @java.lang.Override
    public com.qlangtech.tis.grpc.LaunchReportInfo getDefaultInstanceForType() {
        return DEFAULT_INSTANCE;
    }
}
