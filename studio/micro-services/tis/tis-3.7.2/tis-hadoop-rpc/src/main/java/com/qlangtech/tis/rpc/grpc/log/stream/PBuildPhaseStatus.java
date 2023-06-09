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
 * Protobuf type {@code stream.PBuildPhaseStatus}
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/09/25
 */
public final class PBuildPhaseStatus extends com.google.protobuf.GeneratedMessageV3 implements // @@protoc_insertion_point(message_implements:stream.PBuildPhaseStatus)
PBuildPhaseStatusOrBuilder {

    private static final long serialVersionUID = 0L;

    // Use PBuildPhaseStatus.newBuilder() to construct.
    private PBuildPhaseStatus(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
        super(builder);
    }

    private PBuildPhaseStatus() {
    }

    @java.lang.Override
    public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
        return this.unknownFields;
    }

    private PBuildPhaseStatus(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
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
                                nodeBuildStatus_ = com.google.protobuf.MapField.newMapField(NodeBuildStatusDefaultEntryHolder.defaultEntry);
                                mutable_bitField0_ |= 0x00000001;
                            }
                            com.google.protobuf.MapEntry<java.lang.String, com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus> nodeBuildStatus__ = input.readMessage(NodeBuildStatusDefaultEntryHolder.defaultEntry.getParserForType(), extensionRegistry);
                            nodeBuildStatus_.getMutableMap().put(nodeBuildStatus__.getKey(), nodeBuildStatus__.getValue());
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
        return com.qlangtech.tis.rpc.grpc.log.stream.LogCollectorProto.internal_static_stream_PBuildPhaseStatus_descriptor;
    }

    @SuppressWarnings({ "rawtypes" })
    @java.lang.Override
    protected com.google.protobuf.MapField internalGetMapField(int number) {
        switch(number) {
            case 1:
                return internalGetNodeBuildStatus();
            default:
                throw new RuntimeException("Invalid map field number: " + number);
        }
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
        return com.qlangtech.tis.rpc.grpc.log.stream.LogCollectorProto.internal_static_stream_PBuildPhaseStatus_fieldAccessorTable.ensureFieldAccessorsInitialized(com.qlangtech.tis.rpc.grpc.log.stream.PBuildPhaseStatus.class, com.qlangtech.tis.rpc.grpc.log.stream.PBuildPhaseStatus.Builder.class);
    }

    public static final int NODEBUILDSTATUS_FIELD_NUMBER = 1;

    private static final class NodeBuildStatusDefaultEntryHolder {

        static final com.google.protobuf.MapEntry<java.lang.String, com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus> defaultEntry = com.google.protobuf.MapEntry.<java.lang.String, com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus>newDefaultInstance(com.qlangtech.tis.rpc.grpc.log.stream.LogCollectorProto.internal_static_stream_PBuildPhaseStatus_NodeBuildStatusEntry_descriptor, com.google.protobuf.WireFormat.FieldType.STRING, "", com.google.protobuf.WireFormat.FieldType.MESSAGE, com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus.getDefaultInstance());
    }

    private com.google.protobuf.MapField<java.lang.String, com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus> nodeBuildStatus_;

    private com.google.protobuf.MapField<java.lang.String, com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus> internalGetNodeBuildStatus() {
        if (nodeBuildStatus_ == null) {
            return com.google.protobuf.MapField.emptyMapField(NodeBuildStatusDefaultEntryHolder.defaultEntry);
        }
        return nodeBuildStatus_;
    }

    public int getNodeBuildStatusCount() {
        return internalGetNodeBuildStatus().getMap().size();
    }

    /**
     * <code>map&lt;string, .BuildSharedPhaseStatus&gt; nodeBuildStatus = 1;</code>
     */
    public boolean containsNodeBuildStatus(java.lang.String key) {
        if (key == null) {
            throw new java.lang.NullPointerException();
        }
        return internalGetNodeBuildStatus().getMap().containsKey(key);
    }

    /**
     * Use {@link #getNodeBuildStatusMap()} instead.
     */
    @java.lang.Deprecated
    public java.util.Map<java.lang.String, com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus> getNodeBuildStatus() {
        return getNodeBuildStatusMap();
    }

    /**
     * <code>map&lt;string, .BuildSharedPhaseStatus&gt; nodeBuildStatus = 1;</code>
     */
    public java.util.Map<java.lang.String, com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus> getNodeBuildStatusMap() {
        return internalGetNodeBuildStatus().getMap();
    }

    /**
     * <code>map&lt;string, .BuildSharedPhaseStatus&gt; nodeBuildStatus = 1;</code>
     */
    public com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus getNodeBuildStatusOrDefault(java.lang.String key, com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus defaultValue) {
        if (key == null) {
            throw new java.lang.NullPointerException();
        }
        java.util.Map<java.lang.String, com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus> map = internalGetNodeBuildStatus().getMap();
        return map.containsKey(key) ? map.get(key) : defaultValue;
    }

    /**
     * <code>map&lt;string, .BuildSharedPhaseStatus&gt; nodeBuildStatus = 1;</code>
     */
    public com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus getNodeBuildStatusOrThrow(java.lang.String key) {
        if (key == null) {
            throw new java.lang.NullPointerException();
        }
        java.util.Map<java.lang.String, com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus> map = internalGetNodeBuildStatus().getMap();
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
        com.google.protobuf.GeneratedMessageV3.serializeStringMapTo(output, internalGetNodeBuildStatus(), NodeBuildStatusDefaultEntryHolder.defaultEntry, 1);
        unknownFields.writeTo(output);
    }

    @java.lang.Override
    public int getSerializedSize() {
        int size = memoizedSize;
        if (size != -1)
            return size;
        size = 0;
        for (java.util.Map.Entry<java.lang.String, com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus> entry : internalGetNodeBuildStatus().getMap().entrySet()) {
            com.google.protobuf.MapEntry<java.lang.String, com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus> nodeBuildStatus__ = NodeBuildStatusDefaultEntryHolder.defaultEntry.newBuilderForType().setKey(entry.getKey()).setValue(entry.getValue()).build();
            size += com.google.protobuf.CodedOutputStream.computeMessageSize(1, nodeBuildStatus__);
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
        if (!(obj instanceof com.qlangtech.tis.rpc.grpc.log.stream.PBuildPhaseStatus)) {
            return super.equals(obj);
        }
        com.qlangtech.tis.rpc.grpc.log.stream.PBuildPhaseStatus other = (com.qlangtech.tis.rpc.grpc.log.stream.PBuildPhaseStatus) obj;
        if (!internalGetNodeBuildStatus().equals(other.internalGetNodeBuildStatus()))
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
        if (!internalGetNodeBuildStatus().getMap().isEmpty()) {
            hash = (37 * hash) + NODEBUILDSTATUS_FIELD_NUMBER;
            hash = (53 * hash) + internalGetNodeBuildStatus().hashCode();
        }
        hash = (29 * hash) + unknownFields.hashCode();
        memoizedHashCode = hash;
        return hash;
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PBuildPhaseStatus parseFrom(java.nio.ByteBuffer data) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PBuildPhaseStatus parseFrom(java.nio.ByteBuffer data, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PBuildPhaseStatus parseFrom(com.google.protobuf.ByteString data) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PBuildPhaseStatus parseFrom(com.google.protobuf.ByteString data, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PBuildPhaseStatus parseFrom(byte[] data) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PBuildPhaseStatus parseFrom(byte[] data, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PBuildPhaseStatus parseFrom(java.io.InputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PBuildPhaseStatus parseFrom(java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PBuildPhaseStatus parseDelimitedFrom(java.io.InputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PBuildPhaseStatus parseDelimitedFrom(java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input, extensionRegistry);
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PBuildPhaseStatus parseFrom(com.google.protobuf.CodedInputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PBuildPhaseStatus parseFrom(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    @java.lang.Override
    public Builder newBuilderForType() {
        return newBuilder();
    }

    public static Builder newBuilder() {
        return DEFAULT_INSTANCE.toBuilder();
    }

    public static Builder newBuilder(com.qlangtech.tis.rpc.grpc.log.stream.PBuildPhaseStatus prototype) {
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
     * Protobuf type {@code stream.PBuildPhaseStatus}
     */
    public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements // @@protoc_insertion_point(builder_implements:stream.PBuildPhaseStatus)
    com.qlangtech.tis.rpc.grpc.log.stream.PBuildPhaseStatusOrBuilder {

        public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
            return com.qlangtech.tis.rpc.grpc.log.stream.LogCollectorProto.internal_static_stream_PBuildPhaseStatus_descriptor;
        }

        @SuppressWarnings({ "rawtypes" })
        protected com.google.protobuf.MapField internalGetMapField(int number) {
            switch(number) {
                case 1:
                    return internalGetNodeBuildStatus();
                default:
                    throw new RuntimeException("Invalid map field number: " + number);
            }
        }

        @SuppressWarnings({ "rawtypes" })
        protected com.google.protobuf.MapField internalGetMutableMapField(int number) {
            switch(number) {
                case 1:
                    return internalGetMutableNodeBuildStatus();
                default:
                    throw new RuntimeException("Invalid map field number: " + number);
            }
        }

        @java.lang.Override
        protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
            return com.qlangtech.tis.rpc.grpc.log.stream.LogCollectorProto.internal_static_stream_PBuildPhaseStatus_fieldAccessorTable.ensureFieldAccessorsInitialized(com.qlangtech.tis.rpc.grpc.log.stream.PBuildPhaseStatus.class, com.qlangtech.tis.rpc.grpc.log.stream.PBuildPhaseStatus.Builder.class);
        }

        // Construct using com.qlangtech.tis.rpc.grpc.log.stream.PBuildPhaseStatus.newBuilder()
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
            internalGetMutableNodeBuildStatus().clear();
            return this;
        }

        @java.lang.Override
        public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
            return com.qlangtech.tis.rpc.grpc.log.stream.LogCollectorProto.internal_static_stream_PBuildPhaseStatus_descriptor;
        }

        @java.lang.Override
        public com.qlangtech.tis.rpc.grpc.log.stream.PBuildPhaseStatus getDefaultInstanceForType() {
            return com.qlangtech.tis.rpc.grpc.log.stream.PBuildPhaseStatus.getDefaultInstance();
        }

        @java.lang.Override
        public com.qlangtech.tis.rpc.grpc.log.stream.PBuildPhaseStatus build() {
            com.qlangtech.tis.rpc.grpc.log.stream.PBuildPhaseStatus result = buildPartial();
            if (!result.isInitialized()) {
                throw newUninitializedMessageException(result);
            }
            return result;
        }

        @java.lang.Override
        public com.qlangtech.tis.rpc.grpc.log.stream.PBuildPhaseStatus buildPartial() {
            com.qlangtech.tis.rpc.grpc.log.stream.PBuildPhaseStatus result = new com.qlangtech.tis.rpc.grpc.log.stream.PBuildPhaseStatus(this);
            int from_bitField0_ = bitField0_;
            result.nodeBuildStatus_ = internalGetNodeBuildStatus();
            result.nodeBuildStatus_.makeImmutable();
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
            if (other instanceof com.qlangtech.tis.rpc.grpc.log.stream.PBuildPhaseStatus) {
                return mergeFrom((com.qlangtech.tis.rpc.grpc.log.stream.PBuildPhaseStatus) other);
            } else {
                super.mergeFrom(other);
                return this;
            }
        }

        public Builder mergeFrom(com.qlangtech.tis.rpc.grpc.log.stream.PBuildPhaseStatus other) {
            if (other == com.qlangtech.tis.rpc.grpc.log.stream.PBuildPhaseStatus.getDefaultInstance())
                return this;
            internalGetMutableNodeBuildStatus().mergeFrom(other.internalGetNodeBuildStatus());
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
            com.qlangtech.tis.rpc.grpc.log.stream.PBuildPhaseStatus parsedMessage = null;
            try {
                parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
            } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                parsedMessage = (com.qlangtech.tis.rpc.grpc.log.stream.PBuildPhaseStatus) e.getUnfinishedMessage();
                throw e.unwrapIOException();
            } finally {
                if (parsedMessage != null) {
                    mergeFrom(parsedMessage);
                }
            }
            return this;
        }

        private int bitField0_;

        private com.google.protobuf.MapField<java.lang.String, com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus> nodeBuildStatus_;

        private com.google.protobuf.MapField<java.lang.String, com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus> internalGetNodeBuildStatus() {
            if (nodeBuildStatus_ == null) {
                return com.google.protobuf.MapField.emptyMapField(NodeBuildStatusDefaultEntryHolder.defaultEntry);
            }
            return nodeBuildStatus_;
        }

        private com.google.protobuf.MapField<java.lang.String, com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus> internalGetMutableNodeBuildStatus() {
            onChanged();
            ;
            if (nodeBuildStatus_ == null) {
                nodeBuildStatus_ = com.google.protobuf.MapField.newMapField(NodeBuildStatusDefaultEntryHolder.defaultEntry);
            }
            if (!nodeBuildStatus_.isMutable()) {
                nodeBuildStatus_ = nodeBuildStatus_.copy();
            }
            return nodeBuildStatus_;
        }

        public int getNodeBuildStatusCount() {
            return internalGetNodeBuildStatus().getMap().size();
        }

        /**
         * <code>map&lt;string, .BuildSharedPhaseStatus&gt; nodeBuildStatus = 1;</code>
         */
        public boolean containsNodeBuildStatus(java.lang.String key) {
            if (key == null) {
                throw new java.lang.NullPointerException();
            }
            return internalGetNodeBuildStatus().getMap().containsKey(key);
        }

        /**
         * Use {@link #getNodeBuildStatusMap()} instead.
         */
        @java.lang.Deprecated
        public java.util.Map<java.lang.String, com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus> getNodeBuildStatus() {
            return getNodeBuildStatusMap();
        }

        /**
         * <code>map&lt;string, .BuildSharedPhaseStatus&gt; nodeBuildStatus = 1;</code>
         */
        public java.util.Map<java.lang.String, com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus> getNodeBuildStatusMap() {
            return internalGetNodeBuildStatus().getMap();
        }

        /**
         * <code>map&lt;string, .BuildSharedPhaseStatus&gt; nodeBuildStatus = 1;</code>
         */
        public com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus getNodeBuildStatusOrDefault(java.lang.String key, com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus defaultValue) {
            if (key == null) {
                throw new java.lang.NullPointerException();
            }
            java.util.Map<java.lang.String, com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus> map = internalGetNodeBuildStatus().getMap();
            return map.containsKey(key) ? map.get(key) : defaultValue;
        }

        /**
         * <code>map&lt;string, .BuildSharedPhaseStatus&gt; nodeBuildStatus = 1;</code>
         */
        public com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus getNodeBuildStatusOrThrow(java.lang.String key) {
            if (key == null) {
                throw new java.lang.NullPointerException();
            }
            java.util.Map<java.lang.String, com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus> map = internalGetNodeBuildStatus().getMap();
            if (!map.containsKey(key)) {
                throw new java.lang.IllegalArgumentException();
            }
            return map.get(key);
        }

        public Builder clearNodeBuildStatus() {
            internalGetMutableNodeBuildStatus().getMutableMap().clear();
            return this;
        }

        /**
         * <code>map&lt;string, .BuildSharedPhaseStatus&gt; nodeBuildStatus = 1;</code>
         */
        public Builder removeNodeBuildStatus(java.lang.String key) {
            if (key == null) {
                throw new java.lang.NullPointerException();
            }
            internalGetMutableNodeBuildStatus().getMutableMap().remove(key);
            return this;
        }

        /**
         * Use alternate mutation accessors instead.
         */
        @java.lang.Deprecated
        public java.util.Map<java.lang.String, com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus> getMutableNodeBuildStatus() {
            return internalGetMutableNodeBuildStatus().getMutableMap();
        }

        /**
         * <code>map&lt;string, .BuildSharedPhaseStatus&gt; nodeBuildStatus = 1;</code>
         */
        public Builder putNodeBuildStatus(java.lang.String key, com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus value) {
            if (key == null) {
                throw new java.lang.NullPointerException();
            }
            if (value == null) {
                throw new java.lang.NullPointerException();
            }
            internalGetMutableNodeBuildStatus().getMutableMap().put(key, value);
            return this;
        }

        /**
         * <code>map&lt;string, .BuildSharedPhaseStatus&gt; nodeBuildStatus = 1;</code>
         */
        public Builder putAllNodeBuildStatus(java.util.Map<java.lang.String, com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus> values) {
            internalGetMutableNodeBuildStatus().getMutableMap().putAll(values);
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
        // @@protoc_insertion_point(builder_scope:stream.PBuildPhaseStatus)
    }

    // @@protoc_insertion_point(class_scope:stream.PBuildPhaseStatus)
    private static final com.qlangtech.tis.rpc.grpc.log.stream.PBuildPhaseStatus DEFAULT_INSTANCE;

    static {
        DEFAULT_INSTANCE = new com.qlangtech.tis.rpc.grpc.log.stream.PBuildPhaseStatus();
    }

    public static com.qlangtech.tis.rpc.grpc.log.stream.PBuildPhaseStatus getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    private static final com.google.protobuf.Parser<PBuildPhaseStatus> PARSER = new com.google.protobuf.AbstractParser<PBuildPhaseStatus>() {

        @java.lang.Override
        public PBuildPhaseStatus parsePartialFrom(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
            return new PBuildPhaseStatus(input, extensionRegistry);
        }
    };

    public static com.google.protobuf.Parser<PBuildPhaseStatus> parser() {
        return PARSER;
    }

    @java.lang.Override
    public com.google.protobuf.Parser<PBuildPhaseStatus> getParserForType() {
        return PARSER;
    }

    @java.lang.Override
    public com.qlangtech.tis.rpc.grpc.log.stream.PBuildPhaseStatus getDefaultInstanceForType() {
        return DEFAULT_INSTANCE;
    }
}
