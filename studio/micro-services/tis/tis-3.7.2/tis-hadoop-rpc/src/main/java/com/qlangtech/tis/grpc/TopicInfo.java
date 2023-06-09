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
 * Protobuf type {@code rpc.TopicInfo}
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/09/25
 */
public final class TopicInfo extends com.google.protobuf.GeneratedMessageV3 implements // @@protoc_insertion_point(message_implements:rpc.TopicInfo)
TopicInfoOrBuilder {

    private static final long serialVersionUID = 0L;

    // Use TopicInfo.newBuilder() to construct.
    private TopicInfo(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
        super(builder);
    }

    private TopicInfo() {
        topicWithTags_ = java.util.Collections.emptyList();
    }

    @java.lang.Override
    public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
        return this.unknownFields;
    }

    private TopicInfo(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
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
                                topicWithTags_ = new java.util.ArrayList<com.qlangtech.tis.grpc.LaunchReportInfoEntry>();
                                mutable_bitField0_ |= 0x00000001;
                            }
                            topicWithTags_.add(input.readMessage(com.qlangtech.tis.grpc.LaunchReportInfoEntry.parser(), extensionRegistry));
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
            if (((mutable_bitField0_ & 0x00000001) != 0)) {
                topicWithTags_ = java.util.Collections.unmodifiableList(topicWithTags_);
            }
            this.unknownFields = unknownFields.build();
            makeExtensionsImmutable();
        }
    }

    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
        return com.qlangtech.tis.grpc.IncrStatusProto.internal_static_rpc_TopicInfo_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
        return com.qlangtech.tis.grpc.IncrStatusProto.internal_static_rpc_TopicInfo_fieldAccessorTable.ensureFieldAccessorsInitialized(com.qlangtech.tis.grpc.TopicInfo.class, com.qlangtech.tis.grpc.TopicInfo.Builder.class);
    }

    public static final int TOPICWITHTAGS_FIELD_NUMBER = 1;

    private java.util.List<com.qlangtech.tis.grpc.LaunchReportInfoEntry> topicWithTags_;

    /**
     * <code>repeated .rpc.LaunchReportInfoEntry topicWithTags = 1;</code>
     */
    public java.util.List<com.qlangtech.tis.grpc.LaunchReportInfoEntry> getTopicWithTagsList() {
        return topicWithTags_;
    }

    /**
     * <code>repeated .rpc.LaunchReportInfoEntry topicWithTags = 1;</code>
     */
    public java.util.List<? extends com.qlangtech.tis.grpc.LaunchReportInfoEntryOrBuilder> getTopicWithTagsOrBuilderList() {
        return topicWithTags_;
    }

    /**
     * <code>repeated .rpc.LaunchReportInfoEntry topicWithTags = 1;</code>
     */
    public int getTopicWithTagsCount() {
        return topicWithTags_.size();
    }

    /**
     * <code>repeated .rpc.LaunchReportInfoEntry topicWithTags = 1;</code>
     */
    public com.qlangtech.tis.grpc.LaunchReportInfoEntry getTopicWithTags(int index) {
        return topicWithTags_.get(index);
    }

    /**
     * <code>repeated .rpc.LaunchReportInfoEntry topicWithTags = 1;</code>
     */
    public com.qlangtech.tis.grpc.LaunchReportInfoEntryOrBuilder getTopicWithTagsOrBuilder(int index) {
        return topicWithTags_.get(index);
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
        for (int i = 0; i < topicWithTags_.size(); i++) {
            output.writeMessage(1, topicWithTags_.get(i));
        }
        unknownFields.writeTo(output);
    }

    @java.lang.Override
    public int getSerializedSize() {
        int size = memoizedSize;
        if (size != -1)
            return size;
        size = 0;
        for (int i = 0; i < topicWithTags_.size(); i++) {
            size += com.google.protobuf.CodedOutputStream.computeMessageSize(1, topicWithTags_.get(i));
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
        if (!(obj instanceof com.qlangtech.tis.grpc.TopicInfo)) {
            return super.equals(obj);
        }
        com.qlangtech.tis.grpc.TopicInfo other = (com.qlangtech.tis.grpc.TopicInfo) obj;
        if (!getTopicWithTagsList().equals(other.getTopicWithTagsList()))
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
        if (getTopicWithTagsCount() > 0) {
            hash = (37 * hash) + TOPICWITHTAGS_FIELD_NUMBER;
            hash = (53 * hash) + getTopicWithTagsList().hashCode();
        }
        hash = (29 * hash) + unknownFields.hashCode();
        memoizedHashCode = hash;
        return hash;
    }

    public static com.qlangtech.tis.grpc.TopicInfo parseFrom(java.nio.ByteBuffer data) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static com.qlangtech.tis.grpc.TopicInfo parseFrom(java.nio.ByteBuffer data, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static com.qlangtech.tis.grpc.TopicInfo parseFrom(com.google.protobuf.ByteString data) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static com.qlangtech.tis.grpc.TopicInfo parseFrom(com.google.protobuf.ByteString data, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static com.qlangtech.tis.grpc.TopicInfo parseFrom(byte[] data) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static com.qlangtech.tis.grpc.TopicInfo parseFrom(byte[] data, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static com.qlangtech.tis.grpc.TopicInfo parseFrom(java.io.InputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static com.qlangtech.tis.grpc.TopicInfo parseFrom(java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static com.qlangtech.tis.grpc.TopicInfo parseDelimitedFrom(java.io.InputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
    }

    public static com.qlangtech.tis.grpc.TopicInfo parseDelimitedFrom(java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input, extensionRegistry);
    }

    public static com.qlangtech.tis.grpc.TopicInfo parseFrom(com.google.protobuf.CodedInputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static com.qlangtech.tis.grpc.TopicInfo parseFrom(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    @java.lang.Override
    public Builder newBuilderForType() {
        return newBuilder();
    }

    public static Builder newBuilder() {
        return DEFAULT_INSTANCE.toBuilder();
    }

    public static Builder newBuilder(com.qlangtech.tis.grpc.TopicInfo prototype) {
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
     * Protobuf type {@code rpc.TopicInfo}
     */
    public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements // @@protoc_insertion_point(builder_implements:rpc.TopicInfo)
    com.qlangtech.tis.grpc.TopicInfoOrBuilder {

        public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
            return com.qlangtech.tis.grpc.IncrStatusProto.internal_static_rpc_TopicInfo_descriptor;
        }

        @java.lang.Override
        protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
            return com.qlangtech.tis.grpc.IncrStatusProto.internal_static_rpc_TopicInfo_fieldAccessorTable.ensureFieldAccessorsInitialized(com.qlangtech.tis.grpc.TopicInfo.class, com.qlangtech.tis.grpc.TopicInfo.Builder.class);
        }

        // Construct using com.qlangtech.tis.grpc.TopicInfo.newBuilder()
        private Builder() {
            maybeForceBuilderInitialization();
        }

        private Builder(com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
            super(parent);
            maybeForceBuilderInitialization();
        }

        private void maybeForceBuilderInitialization() {
            if (com.google.protobuf.GeneratedMessageV3.alwaysUseFieldBuilders) {
                getTopicWithTagsFieldBuilder();
            }
        }

        @java.lang.Override
        public Builder clear() {
            super.clear();
            if (topicWithTagsBuilder_ == null) {
                topicWithTags_ = java.util.Collections.emptyList();
                bitField0_ = (bitField0_ & ~0x00000001);
            } else {
                topicWithTagsBuilder_.clear();
            }
            return this;
        }

        @java.lang.Override
        public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
            return com.qlangtech.tis.grpc.IncrStatusProto.internal_static_rpc_TopicInfo_descriptor;
        }

        @java.lang.Override
        public com.qlangtech.tis.grpc.TopicInfo getDefaultInstanceForType() {
            return com.qlangtech.tis.grpc.TopicInfo.getDefaultInstance();
        }

        @java.lang.Override
        public com.qlangtech.tis.grpc.TopicInfo build() {
            com.qlangtech.tis.grpc.TopicInfo result = buildPartial();
            if (!result.isInitialized()) {
                throw newUninitializedMessageException(result);
            }
            return result;
        }

        @java.lang.Override
        public com.qlangtech.tis.grpc.TopicInfo buildPartial() {
            com.qlangtech.tis.grpc.TopicInfo result = new com.qlangtech.tis.grpc.TopicInfo(this);
            int from_bitField0_ = bitField0_;
            if (topicWithTagsBuilder_ == null) {
                if (((bitField0_ & 0x00000001) != 0)) {
                    topicWithTags_ = java.util.Collections.unmodifiableList(topicWithTags_);
                    bitField0_ = (bitField0_ & ~0x00000001);
                }
                result.topicWithTags_ = topicWithTags_;
            } else {
                result.topicWithTags_ = topicWithTagsBuilder_.build();
            }
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
            if (other instanceof com.qlangtech.tis.grpc.TopicInfo) {
                return mergeFrom((com.qlangtech.tis.grpc.TopicInfo) other);
            } else {
                super.mergeFrom(other);
                return this;
            }
        }

        public Builder mergeFrom(com.qlangtech.tis.grpc.TopicInfo other) {
            if (other == com.qlangtech.tis.grpc.TopicInfo.getDefaultInstance())
                return this;
            if (topicWithTagsBuilder_ == null) {
                if (!other.topicWithTags_.isEmpty()) {
                    if (topicWithTags_.isEmpty()) {
                        topicWithTags_ = other.topicWithTags_;
                        bitField0_ = (bitField0_ & ~0x00000001);
                    } else {
                        ensureTopicWithTagsIsMutable();
                        topicWithTags_.addAll(other.topicWithTags_);
                    }
                    onChanged();
                }
            } else {
                if (!other.topicWithTags_.isEmpty()) {
                    if (topicWithTagsBuilder_.isEmpty()) {
                        topicWithTagsBuilder_.dispose();
                        topicWithTagsBuilder_ = null;
                        topicWithTags_ = other.topicWithTags_;
                        bitField0_ = (bitField0_ & ~0x00000001);
                        topicWithTagsBuilder_ = com.google.protobuf.GeneratedMessageV3.alwaysUseFieldBuilders ? getTopicWithTagsFieldBuilder() : null;
                    } else {
                        topicWithTagsBuilder_.addAllMessages(other.topicWithTags_);
                    }
                }
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
            com.qlangtech.tis.grpc.TopicInfo parsedMessage = null;
            try {
                parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
            } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                parsedMessage = (com.qlangtech.tis.grpc.TopicInfo) e.getUnfinishedMessage();
                throw e.unwrapIOException();
            } finally {
                if (parsedMessage != null) {
                    mergeFrom(parsedMessage);
                }
            }
            return this;
        }

        private int bitField0_;

        private java.util.List<com.qlangtech.tis.grpc.LaunchReportInfoEntry> topicWithTags_ = java.util.Collections.emptyList();

        private void ensureTopicWithTagsIsMutable() {
            if (!((bitField0_ & 0x00000001) != 0)) {
                topicWithTags_ = new java.util.ArrayList<com.qlangtech.tis.grpc.LaunchReportInfoEntry>(topicWithTags_);
                bitField0_ |= 0x00000001;
            }
        }

        private com.google.protobuf.RepeatedFieldBuilderV3<com.qlangtech.tis.grpc.LaunchReportInfoEntry, com.qlangtech.tis.grpc.LaunchReportInfoEntry.Builder, com.qlangtech.tis.grpc.LaunchReportInfoEntryOrBuilder> topicWithTagsBuilder_;

        /**
         * <code>repeated .rpc.LaunchReportInfoEntry topicWithTags = 1;</code>
         */
        public java.util.List<com.qlangtech.tis.grpc.LaunchReportInfoEntry> getTopicWithTagsList() {
            if (topicWithTagsBuilder_ == null) {
                return java.util.Collections.unmodifiableList(topicWithTags_);
            } else {
                return topicWithTagsBuilder_.getMessageList();
            }
        }

        /**
         * <code>repeated .rpc.LaunchReportInfoEntry topicWithTags = 1;</code>
         */
        public int getTopicWithTagsCount() {
            if (topicWithTagsBuilder_ == null) {
                return topicWithTags_.size();
            } else {
                return topicWithTagsBuilder_.getCount();
            }
        }

        /**
         * <code>repeated .rpc.LaunchReportInfoEntry topicWithTags = 1;</code>
         */
        public com.qlangtech.tis.grpc.LaunchReportInfoEntry getTopicWithTags(int index) {
            if (topicWithTagsBuilder_ == null) {
                return topicWithTags_.get(index);
            } else {
                return topicWithTagsBuilder_.getMessage(index);
            }
        }

        /**
         * <code>repeated .rpc.LaunchReportInfoEntry topicWithTags = 1;</code>
         */
        public Builder setTopicWithTags(int index, com.qlangtech.tis.grpc.LaunchReportInfoEntry value) {
            if (topicWithTagsBuilder_ == null) {
                if (value == null) {
                    throw new NullPointerException();
                }
                ensureTopicWithTagsIsMutable();
                topicWithTags_.set(index, value);
                onChanged();
            } else {
                topicWithTagsBuilder_.setMessage(index, value);
            }
            return this;
        }

        /**
         * <code>repeated .rpc.LaunchReportInfoEntry topicWithTags = 1;</code>
         */
        public Builder setTopicWithTags(int index, com.qlangtech.tis.grpc.LaunchReportInfoEntry.Builder builderForValue) {
            if (topicWithTagsBuilder_ == null) {
                ensureTopicWithTagsIsMutable();
                topicWithTags_.set(index, builderForValue.build());
                onChanged();
            } else {
                topicWithTagsBuilder_.setMessage(index, builderForValue.build());
            }
            return this;
        }

        /**
         * <code>repeated .rpc.LaunchReportInfoEntry topicWithTags = 1;</code>
         */
        public Builder addTopicWithTags(com.qlangtech.tis.grpc.LaunchReportInfoEntry value) {
            if (topicWithTagsBuilder_ == null) {
                if (value == null) {
                    throw new NullPointerException();
                }
                ensureTopicWithTagsIsMutable();
                topicWithTags_.add(value);
                onChanged();
            } else {
                topicWithTagsBuilder_.addMessage(value);
            }
            return this;
        }

        /**
         * <code>repeated .rpc.LaunchReportInfoEntry topicWithTags = 1;</code>
         */
        public Builder addTopicWithTags(int index, com.qlangtech.tis.grpc.LaunchReportInfoEntry value) {
            if (topicWithTagsBuilder_ == null) {
                if (value == null) {
                    throw new NullPointerException();
                }
                ensureTopicWithTagsIsMutable();
                topicWithTags_.add(index, value);
                onChanged();
            } else {
                topicWithTagsBuilder_.addMessage(index, value);
            }
            return this;
        }

        /**
         * <code>repeated .rpc.LaunchReportInfoEntry topicWithTags = 1;</code>
         */
        public Builder addTopicWithTags(com.qlangtech.tis.grpc.LaunchReportInfoEntry.Builder builderForValue) {
            if (topicWithTagsBuilder_ == null) {
                ensureTopicWithTagsIsMutable();
                topicWithTags_.add(builderForValue.build());
                onChanged();
            } else {
                topicWithTagsBuilder_.addMessage(builderForValue.build());
            }
            return this;
        }

        /**
         * <code>repeated .rpc.LaunchReportInfoEntry topicWithTags = 1;</code>
         */
        public Builder addTopicWithTags(int index, com.qlangtech.tis.grpc.LaunchReportInfoEntry.Builder builderForValue) {
            if (topicWithTagsBuilder_ == null) {
                ensureTopicWithTagsIsMutable();
                topicWithTags_.add(index, builderForValue.build());
                onChanged();
            } else {
                topicWithTagsBuilder_.addMessage(index, builderForValue.build());
            }
            return this;
        }

        /**
         * <code>repeated .rpc.LaunchReportInfoEntry topicWithTags = 1;</code>
         */
        public Builder addAllTopicWithTags(java.lang.Iterable<? extends com.qlangtech.tis.grpc.LaunchReportInfoEntry> values) {
            if (topicWithTagsBuilder_ == null) {
                ensureTopicWithTagsIsMutable();
                com.google.protobuf.AbstractMessageLite.Builder.addAll(values, topicWithTags_);
                onChanged();
            } else {
                topicWithTagsBuilder_.addAllMessages(values);
            }
            return this;
        }

        /**
         * <code>repeated .rpc.LaunchReportInfoEntry topicWithTags = 1;</code>
         */
        public Builder clearTopicWithTags() {
            if (topicWithTagsBuilder_ == null) {
                topicWithTags_ = java.util.Collections.emptyList();
                bitField0_ = (bitField0_ & ~0x00000001);
                onChanged();
            } else {
                topicWithTagsBuilder_.clear();
            }
            return this;
        }

        /**
         * <code>repeated .rpc.LaunchReportInfoEntry topicWithTags = 1;</code>
         */
        public Builder removeTopicWithTags(int index) {
            if (topicWithTagsBuilder_ == null) {
                ensureTopicWithTagsIsMutable();
                topicWithTags_.remove(index);
                onChanged();
            } else {
                topicWithTagsBuilder_.remove(index);
            }
            return this;
        }

        /**
         * <code>repeated .rpc.LaunchReportInfoEntry topicWithTags = 1;</code>
         */
        public com.qlangtech.tis.grpc.LaunchReportInfoEntry.Builder getTopicWithTagsBuilder(int index) {
            return getTopicWithTagsFieldBuilder().getBuilder(index);
        }

        /**
         * <code>repeated .rpc.LaunchReportInfoEntry topicWithTags = 1;</code>
         */
        public com.qlangtech.tis.grpc.LaunchReportInfoEntryOrBuilder getTopicWithTagsOrBuilder(int index) {
            if (topicWithTagsBuilder_ == null) {
                return topicWithTags_.get(index);
            } else {
                return topicWithTagsBuilder_.getMessageOrBuilder(index);
            }
        }

        /**
         * <code>repeated .rpc.LaunchReportInfoEntry topicWithTags = 1;</code>
         */
        public java.util.List<? extends com.qlangtech.tis.grpc.LaunchReportInfoEntryOrBuilder> getTopicWithTagsOrBuilderList() {
            if (topicWithTagsBuilder_ != null) {
                return topicWithTagsBuilder_.getMessageOrBuilderList();
            } else {
                return java.util.Collections.unmodifiableList(topicWithTags_);
            }
        }

        /**
         * <code>repeated .rpc.LaunchReportInfoEntry topicWithTags = 1;</code>
         */
        public com.qlangtech.tis.grpc.LaunchReportInfoEntry.Builder addTopicWithTagsBuilder() {
            return getTopicWithTagsFieldBuilder().addBuilder(com.qlangtech.tis.grpc.LaunchReportInfoEntry.getDefaultInstance());
        }

        /**
         * <code>repeated .rpc.LaunchReportInfoEntry topicWithTags = 1;</code>
         */
        public com.qlangtech.tis.grpc.LaunchReportInfoEntry.Builder addTopicWithTagsBuilder(int index) {
            return getTopicWithTagsFieldBuilder().addBuilder(index, com.qlangtech.tis.grpc.LaunchReportInfoEntry.getDefaultInstance());
        }

        /**
         * <code>repeated .rpc.LaunchReportInfoEntry topicWithTags = 1;</code>
         */
        public java.util.List<com.qlangtech.tis.grpc.LaunchReportInfoEntry.Builder> getTopicWithTagsBuilderList() {
            return getTopicWithTagsFieldBuilder().getBuilderList();
        }

        private com.google.protobuf.RepeatedFieldBuilderV3<com.qlangtech.tis.grpc.LaunchReportInfoEntry, com.qlangtech.tis.grpc.LaunchReportInfoEntry.Builder, com.qlangtech.tis.grpc.LaunchReportInfoEntryOrBuilder> getTopicWithTagsFieldBuilder() {
            if (topicWithTagsBuilder_ == null) {
                topicWithTagsBuilder_ = new com.google.protobuf.RepeatedFieldBuilderV3<com.qlangtech.tis.grpc.LaunchReportInfoEntry, com.qlangtech.tis.grpc.LaunchReportInfoEntry.Builder, com.qlangtech.tis.grpc.LaunchReportInfoEntryOrBuilder>(topicWithTags_, ((bitField0_ & 0x00000001) != 0), getParentForChildren(), isClean());
                topicWithTags_ = null;
            }
            return topicWithTagsBuilder_;
        }

        @java.lang.Override
        public final Builder setUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
            return super.setUnknownFields(unknownFields);
        }

        @java.lang.Override
        public final Builder mergeUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
            return super.mergeUnknownFields(unknownFields);
        }
        // @@protoc_insertion_point(builder_scope:rpc.TopicInfo)
    }

    // @@protoc_insertion_point(class_scope:rpc.TopicInfo)
    private static final com.qlangtech.tis.grpc.TopicInfo DEFAULT_INSTANCE;

    static {
        DEFAULT_INSTANCE = new com.qlangtech.tis.grpc.TopicInfo();
    }

    public static com.qlangtech.tis.grpc.TopicInfo getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    private static final com.google.protobuf.Parser<TopicInfo> PARSER = new com.google.protobuf.AbstractParser<TopicInfo>() {

        @java.lang.Override
        public TopicInfo parsePartialFrom(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
            return new TopicInfo(input, extensionRegistry);
        }
    };

    public static com.google.protobuf.Parser<TopicInfo> parser() {
        return PARSER;
    }

    @java.lang.Override
    public com.google.protobuf.Parser<TopicInfo> getParserForType() {
        return PARSER;
    }

    @java.lang.Override
    public com.qlangtech.tis.grpc.TopicInfo getDefaultInstanceForType() {
        return DEFAULT_INSTANCE;
    }
}
