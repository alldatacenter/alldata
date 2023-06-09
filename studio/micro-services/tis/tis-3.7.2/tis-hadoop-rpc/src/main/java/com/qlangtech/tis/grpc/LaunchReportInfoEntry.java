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
 * Protobuf type {@code rpc.LaunchReportInfoEntry}
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/09/25
 */
public final class LaunchReportInfoEntry extends com.google.protobuf.GeneratedMessageV3 implements // @@protoc_insertion_point(message_implements:rpc.LaunchReportInfoEntry)
LaunchReportInfoEntryOrBuilder {

    private static final long serialVersionUID = 0L;

    // Use LaunchReportInfoEntry.newBuilder() to construct.
    private LaunchReportInfoEntry(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
        super(builder);
    }

    private LaunchReportInfoEntry() {
        topicName_ = "";
        tagName_ = com.google.protobuf.LazyStringArrayList.EMPTY;
    }

    @java.lang.Override
    public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
        return this.unknownFields;
    }

    private LaunchReportInfoEntry(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
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
                            topicName_ = s;
                            break;
                        }
                    case 18:
                        {
                            java.lang.String s = input.readStringRequireUtf8();
                            if (!((mutable_bitField0_ & 0x00000002) != 0)) {
                                tagName_ = new com.google.protobuf.LazyStringArrayList();
                                mutable_bitField0_ |= 0x00000002;
                            }
                            tagName_.add(s);
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
            if (((mutable_bitField0_ & 0x00000002) != 0)) {
                tagName_ = tagName_.getUnmodifiableView();
            }
            this.unknownFields = unknownFields.build();
            makeExtensionsImmutable();
        }
    }

    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
        return com.qlangtech.tis.grpc.IncrStatusProto.internal_static_rpc_LaunchReportInfoEntry_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
        return com.qlangtech.tis.grpc.IncrStatusProto.internal_static_rpc_LaunchReportInfoEntry_fieldAccessorTable.ensureFieldAccessorsInitialized(com.qlangtech.tis.grpc.LaunchReportInfoEntry.class, com.qlangtech.tis.grpc.LaunchReportInfoEntry.Builder.class);
    }

    private int bitField0_;

    public static final int TOPICNAME_FIELD_NUMBER = 1;

    private volatile java.lang.Object topicName_;

    /**
     * <pre>
     * topic
     * </pre>
     *
     * <code>string topicName = 1;</code>
     */
    public java.lang.String getTopicName() {
        java.lang.Object ref = topicName_;
        if (ref instanceof java.lang.String) {
            return (java.lang.String) ref;
        } else {
            com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
            java.lang.String s = bs.toStringUtf8();
            topicName_ = s;
            return s;
        }
    }

    /**
     * <pre>
     * topic
     * </pre>
     *
     * <code>string topicName = 1;</code>
     */
    public com.google.protobuf.ByteString getTopicNameBytes() {
        java.lang.Object ref = topicName_;
        if (ref instanceof java.lang.String) {
            com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((java.lang.String) ref);
            topicName_ = b;
            return b;
        } else {
            return (com.google.protobuf.ByteString) ref;
        }
    }

    public static final int TAGNAME_FIELD_NUMBER = 2;

    private com.google.protobuf.LazyStringList tagName_;

    /**
     * <pre>
     * tags
     * </pre>
     *
     * <code>repeated string tagName = 2;</code>
     */
    public com.google.protobuf.ProtocolStringList getTagNameList() {
        return tagName_;
    }

    /**
     * <pre>
     * tags
     * </pre>
     *
     * <code>repeated string tagName = 2;</code>
     */
    public int getTagNameCount() {
        return tagName_.size();
    }

    /**
     * <pre>
     * tags
     * </pre>
     *
     * <code>repeated string tagName = 2;</code>
     */
    public java.lang.String getTagName(int index) {
        return tagName_.get(index);
    }

    /**
     * <pre>
     * tags
     * </pre>
     *
     * <code>repeated string tagName = 2;</code>
     */
    public com.google.protobuf.ByteString getTagNameBytes(int index) {
        return tagName_.getByteString(index);
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
        if (!getTopicNameBytes().isEmpty()) {
            com.google.protobuf.GeneratedMessageV3.writeString(output, 1, topicName_);
        }
        for (int i = 0; i < tagName_.size(); i++) {
            com.google.protobuf.GeneratedMessageV3.writeString(output, 2, tagName_.getRaw(i));
        }
        unknownFields.writeTo(output);
    }

    @java.lang.Override
    public int getSerializedSize() {
        int size = memoizedSize;
        if (size != -1)
            return size;
        size = 0;
        if (!getTopicNameBytes().isEmpty()) {
            size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, topicName_);
        }
        {
            int dataSize = 0;
            for (int i = 0; i < tagName_.size(); i++) {
                dataSize += computeStringSizeNoTag(tagName_.getRaw(i));
            }
            size += dataSize;
            size += 1 * getTagNameList().size();
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
        if (!(obj instanceof com.qlangtech.tis.grpc.LaunchReportInfoEntry)) {
            return super.equals(obj);
        }
        com.qlangtech.tis.grpc.LaunchReportInfoEntry other = (com.qlangtech.tis.grpc.LaunchReportInfoEntry) obj;
        if (!getTopicName().equals(other.getTopicName()))
            return false;
        if (!getTagNameList().equals(other.getTagNameList()))
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
        hash = (37 * hash) + TOPICNAME_FIELD_NUMBER;
        hash = (53 * hash) + getTopicName().hashCode();
        if (getTagNameCount() > 0) {
            hash = (37 * hash) + TAGNAME_FIELD_NUMBER;
            hash = (53 * hash) + getTagNameList().hashCode();
        }
        hash = (29 * hash) + unknownFields.hashCode();
        memoizedHashCode = hash;
        return hash;
    }

    public static com.qlangtech.tis.grpc.LaunchReportInfoEntry parseFrom(java.nio.ByteBuffer data) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static com.qlangtech.tis.grpc.LaunchReportInfoEntry parseFrom(java.nio.ByteBuffer data, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static com.qlangtech.tis.grpc.LaunchReportInfoEntry parseFrom(com.google.protobuf.ByteString data) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static com.qlangtech.tis.grpc.LaunchReportInfoEntry parseFrom(com.google.protobuf.ByteString data, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static com.qlangtech.tis.grpc.LaunchReportInfoEntry parseFrom(byte[] data) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static com.qlangtech.tis.grpc.LaunchReportInfoEntry parseFrom(byte[] data, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static com.qlangtech.tis.grpc.LaunchReportInfoEntry parseFrom(java.io.InputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static com.qlangtech.tis.grpc.LaunchReportInfoEntry parseFrom(java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static com.qlangtech.tis.grpc.LaunchReportInfoEntry parseDelimitedFrom(java.io.InputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
    }

    public static com.qlangtech.tis.grpc.LaunchReportInfoEntry parseDelimitedFrom(java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input, extensionRegistry);
    }

    public static com.qlangtech.tis.grpc.LaunchReportInfoEntry parseFrom(com.google.protobuf.CodedInputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static com.qlangtech.tis.grpc.LaunchReportInfoEntry parseFrom(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    @java.lang.Override
    public Builder newBuilderForType() {
        return newBuilder();
    }

    public static Builder newBuilder() {
        return DEFAULT_INSTANCE.toBuilder();
    }

    public static Builder newBuilder(com.qlangtech.tis.grpc.LaunchReportInfoEntry prototype) {
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
     * Protobuf type {@code rpc.LaunchReportInfoEntry}
     */
    public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements // @@protoc_insertion_point(builder_implements:rpc.LaunchReportInfoEntry)
    com.qlangtech.tis.grpc.LaunchReportInfoEntryOrBuilder {

        public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
            return com.qlangtech.tis.grpc.IncrStatusProto.internal_static_rpc_LaunchReportInfoEntry_descriptor;
        }

        @java.lang.Override
        protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
            return com.qlangtech.tis.grpc.IncrStatusProto.internal_static_rpc_LaunchReportInfoEntry_fieldAccessorTable.ensureFieldAccessorsInitialized(com.qlangtech.tis.grpc.LaunchReportInfoEntry.class, com.qlangtech.tis.grpc.LaunchReportInfoEntry.Builder.class);
        }

        // Construct using com.qlangtech.tis.grpc.LaunchReportInfoEntry.newBuilder()
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
            topicName_ = "";
            tagName_ = com.google.protobuf.LazyStringArrayList.EMPTY;
            bitField0_ = (bitField0_ & ~0x00000002);
            return this;
        }

        @java.lang.Override
        public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
            return com.qlangtech.tis.grpc.IncrStatusProto.internal_static_rpc_LaunchReportInfoEntry_descriptor;
        }

        @java.lang.Override
        public com.qlangtech.tis.grpc.LaunchReportInfoEntry getDefaultInstanceForType() {
            return com.qlangtech.tis.grpc.LaunchReportInfoEntry.getDefaultInstance();
        }

        @java.lang.Override
        public com.qlangtech.tis.grpc.LaunchReportInfoEntry build() {
            com.qlangtech.tis.grpc.LaunchReportInfoEntry result = buildPartial();
            if (!result.isInitialized()) {
                throw newUninitializedMessageException(result);
            }
            return result;
        }

        @java.lang.Override
        public com.qlangtech.tis.grpc.LaunchReportInfoEntry buildPartial() {
            com.qlangtech.tis.grpc.LaunchReportInfoEntry result = new com.qlangtech.tis.grpc.LaunchReportInfoEntry(this);
            int from_bitField0_ = bitField0_;
            int to_bitField0_ = 0;
            result.topicName_ = topicName_;
            if (((bitField0_ & 0x00000002) != 0)) {
                tagName_ = tagName_.getUnmodifiableView();
                bitField0_ = (bitField0_ & ~0x00000002);
            }
            result.tagName_ = tagName_;
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
            if (other instanceof com.qlangtech.tis.grpc.LaunchReportInfoEntry) {
                return mergeFrom((com.qlangtech.tis.grpc.LaunchReportInfoEntry) other);
            } else {
                super.mergeFrom(other);
                return this;
            }
        }

        public Builder mergeFrom(com.qlangtech.tis.grpc.LaunchReportInfoEntry other) {
            if (other == com.qlangtech.tis.grpc.LaunchReportInfoEntry.getDefaultInstance())
                return this;
            if (!other.getTopicName().isEmpty()) {
                topicName_ = other.topicName_;
                onChanged();
            }
            if (!other.tagName_.isEmpty()) {
                if (tagName_.isEmpty()) {
                    tagName_ = other.tagName_;
                    bitField0_ = (bitField0_ & ~0x00000002);
                } else {
                    ensureTagNameIsMutable();
                    tagName_.addAll(other.tagName_);
                }
                onChanged();
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
            com.qlangtech.tis.grpc.LaunchReportInfoEntry parsedMessage = null;
            try {
                parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
            } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                parsedMessage = (com.qlangtech.tis.grpc.LaunchReportInfoEntry) e.getUnfinishedMessage();
                throw e.unwrapIOException();
            } finally {
                if (parsedMessage != null) {
                    mergeFrom(parsedMessage);
                }
            }
            return this;
        }

        private int bitField0_;

        private java.lang.Object topicName_ = "";

        /**
         * <pre>
         * topic
         * </pre>
         *
         * <code>string topicName = 1;</code>
         */
        public java.lang.String getTopicName() {
            java.lang.Object ref = topicName_;
            if (!(ref instanceof java.lang.String)) {
                com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
                java.lang.String s = bs.toStringUtf8();
                topicName_ = s;
                return s;
            } else {
                return (java.lang.String) ref;
            }
        }

        /**
         * <pre>
         * topic
         * </pre>
         *
         * <code>string topicName = 1;</code>
         */
        public com.google.protobuf.ByteString getTopicNameBytes() {
            java.lang.Object ref = topicName_;
            if (ref instanceof String) {
                com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((java.lang.String) ref);
                topicName_ = b;
                return b;
            } else {
                return (com.google.protobuf.ByteString) ref;
            }
        }

        /**
         * <pre>
         * topic
         * </pre>
         *
         * <code>string topicName = 1;</code>
         */
        public Builder setTopicName(java.lang.String value) {
            if (value == null) {
                throw new NullPointerException();
            }
            topicName_ = value;
            onChanged();
            return this;
        }

        /**
         * <pre>
         * topic
         * </pre>
         *
         * <code>string topicName = 1;</code>
         */
        public Builder clearTopicName() {
            topicName_ = getDefaultInstance().getTopicName();
            onChanged();
            return this;
        }

        /**
         * <pre>
         * topic
         * </pre>
         *
         * <code>string topicName = 1;</code>
         */
        public Builder setTopicNameBytes(com.google.protobuf.ByteString value) {
            if (value == null) {
                throw new NullPointerException();
            }
            checkByteStringIsUtf8(value);
            topicName_ = value;
            onChanged();
            return this;
        }

        private com.google.protobuf.LazyStringList tagName_ = com.google.protobuf.LazyStringArrayList.EMPTY;

        private void ensureTagNameIsMutable() {
            if (!((bitField0_ & 0x00000002) != 0)) {
                tagName_ = new com.google.protobuf.LazyStringArrayList(tagName_);
                bitField0_ |= 0x00000002;
            }
        }

        /**
         * <pre>
         * tags
         * </pre>
         *
         * <code>repeated string tagName = 2;</code>
         */
        public com.google.protobuf.ProtocolStringList getTagNameList() {
            return tagName_.getUnmodifiableView();
        }

        /**
         * <pre>
         * tags
         * </pre>
         *
         * <code>repeated string tagName = 2;</code>
         */
        public int getTagNameCount() {
            return tagName_.size();
        }

        /**
         * <pre>
         * tags
         * </pre>
         *
         * <code>repeated string tagName = 2;</code>
         */
        public java.lang.String getTagName(int index) {
            return tagName_.get(index);
        }

        /**
         * <pre>
         * tags
         * </pre>
         *
         * <code>repeated string tagName = 2;</code>
         */
        public com.google.protobuf.ByteString getTagNameBytes(int index) {
            return tagName_.getByteString(index);
        }

        /**
         * <pre>
         * tags
         * </pre>
         *
         * <code>repeated string tagName = 2;</code>
         */
        public Builder setTagName(int index, java.lang.String value) {
            if (value == null) {
                throw new NullPointerException();
            }
            ensureTagNameIsMutable();
            tagName_.set(index, value);
            onChanged();
            return this;
        }

        /**
         * <pre>
         * tags
         * </pre>
         *
         * <code>repeated string tagName = 2;</code>
         */
        public Builder addTagName(java.lang.String value) {
            if (value == null) {
                throw new NullPointerException();
            }
            ensureTagNameIsMutable();
            tagName_.add(value);
            onChanged();
            return this;
        }

        /**
         * <pre>
         * tags
         * </pre>
         *
         * <code>repeated string tagName = 2;</code>
         */
        public Builder addAllTagName(java.lang.Iterable<java.lang.String> values) {
            ensureTagNameIsMutable();
            com.google.protobuf.AbstractMessageLite.Builder.addAll(values, tagName_);
            onChanged();
            return this;
        }

        /**
         * <pre>
         * tags
         * </pre>
         *
         * <code>repeated string tagName = 2;</code>
         */
        public Builder clearTagName() {
            tagName_ = com.google.protobuf.LazyStringArrayList.EMPTY;
            bitField0_ = (bitField0_ & ~0x00000002);
            onChanged();
            return this;
        }

        /**
         * <pre>
         * tags
         * </pre>
         *
         * <code>repeated string tagName = 2;</code>
         */
        public Builder addTagNameBytes(com.google.protobuf.ByteString value) {
            if (value == null) {
                throw new NullPointerException();
            }
            checkByteStringIsUtf8(value);
            ensureTagNameIsMutable();
            tagName_.add(value);
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
        // @@protoc_insertion_point(builder_scope:rpc.LaunchReportInfoEntry)
    }

    // @@protoc_insertion_point(class_scope:rpc.LaunchReportInfoEntry)
    private static final com.qlangtech.tis.grpc.LaunchReportInfoEntry DEFAULT_INSTANCE;

    static {
        DEFAULT_INSTANCE = new com.qlangtech.tis.grpc.LaunchReportInfoEntry();
    }

    public static com.qlangtech.tis.grpc.LaunchReportInfoEntry getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    private static final com.google.protobuf.Parser<LaunchReportInfoEntry> PARSER = new com.google.protobuf.AbstractParser<LaunchReportInfoEntry>() {

        @java.lang.Override
        public LaunchReportInfoEntry parsePartialFrom(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
            return new LaunchReportInfoEntry(input, extensionRegistry);
        }
    };

    public static com.google.protobuf.Parser<LaunchReportInfoEntry> parser() {
        return PARSER;
    }

    @java.lang.Override
    public com.google.protobuf.Parser<LaunchReportInfoEntry> getParserForType() {
        return PARSER;
    }

    @java.lang.Override
    public com.qlangtech.tis.grpc.LaunchReportInfoEntry getDefaultInstanceForType() {
        return DEFAULT_INSTANCE;
    }
}
