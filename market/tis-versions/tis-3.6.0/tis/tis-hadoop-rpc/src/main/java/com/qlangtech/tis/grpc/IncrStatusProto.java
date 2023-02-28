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
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/09/25
 */
public final class IncrStatusProto {

    private IncrStatusProto() {
    }

    public static void registerAllExtensions(com.google.protobuf.ExtensionRegistryLite registry) {
    }

    public static void registerAllExtensions(com.google.protobuf.ExtensionRegistry registry) {
        registerAllExtensions((com.google.protobuf.ExtensionRegistryLite) registry);
    }

    static final com.google.protobuf.Descriptors.Descriptor internal_static_rpc_LaunchReportInfo_descriptor;

    static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_rpc_LaunchReportInfo_fieldAccessorTable;

    static final com.google.protobuf.Descriptors.Descriptor internal_static_rpc_LaunchReportInfo_CollectionFocusTopicInfoEntry_descriptor;

    static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_rpc_LaunchReportInfo_CollectionFocusTopicInfoEntry_fieldAccessorTable;

    static final com.google.protobuf.Descriptors.Descriptor internal_static_rpc_LaunchReportInfoEntry_descriptor;

    static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_rpc_LaunchReportInfoEntry_fieldAccessorTable;

    static final com.google.protobuf.Descriptors.Descriptor internal_static_rpc_TopicInfo_descriptor;

    static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_rpc_TopicInfo_fieldAccessorTable;

    static final com.google.protobuf.Descriptors.Descriptor internal_static_rpc_MasterJob_descriptor;

    static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_rpc_MasterJob_fieldAccessorTable;

    static final com.google.protobuf.Descriptors.Descriptor internal_static_rpc_UpdateCounterMap_descriptor;

    static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_rpc_UpdateCounterMap_fieldAccessorTable;

    static final com.google.protobuf.Descriptors.Descriptor internal_static_rpc_UpdateCounterMap_DataEntry_descriptor;

    static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_rpc_UpdateCounterMap_DataEntry_fieldAccessorTable;

    static final com.google.protobuf.Descriptors.Descriptor internal_static_rpc_TableSingleDataIndexStatus_descriptor;

    static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_rpc_TableSingleDataIndexStatus_fieldAccessorTable;

    static final com.google.protobuf.Descriptors.Descriptor internal_static_rpc_TableSingleDataIndexStatus_TableConsumeDataEntry_descriptor;

    static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_rpc_TableSingleDataIndexStatus_TableConsumeDataEntry_fieldAccessorTable;

    static final com.google.protobuf.Descriptors.Descriptor internal_static_rpc_Empty_descriptor;

    static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_rpc_Empty_fieldAccessorTable;

    static final com.google.protobuf.Descriptors.Descriptor internal_static_rpc_PingResult_descriptor;

    static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_rpc_PingResult_fieldAccessorTable;

    public static com.google.protobuf.Descriptors.FileDescriptor getDescriptor() {
        return descriptor;
    }

    private static com.google.protobuf.Descriptors.FileDescriptor descriptor;

    static {
        java.lang.String[] descriptorData = { "\n\021incr-status.proto\022\003rpc\032\020common-msg.pro" + "to\"\272\001\n\020LaunchReportInfo\022U\n\030collectionFoc" + "usTopicInfo\030\001 \003(\01323.rpc.LaunchReportInfo" + ".CollectionFocusTopicInfoEntry\032O\n\035Collec" + "tionFocusTopicInfoEntry\022\013\n\003key\030\001 \001(\t\022\035\n\005" + "value\030\002 \001(\0132\016.rpc.TopicInfo:\0028\001\";\n\025Launc" + "hReportInfoEntry\022\021\n\ttopicName\030\001 \001(\t\022\017\n\007t" + "agName\030\002 \003(\t\">\n\tTopicInfo\0221\n\rtopicWithTa" + "gs\030\001 \003(\0132\032.rpc.LaunchReportInfoEntry\"\241\001\n" + "\tMasterJob\022\'\n\007jobType\030\001 \001(\0162\026.rpc.Master" + "Job.JobType\022\014\n\004stop\030\002 \001(\010\022\021\n\tindexName\030\003" + " \001(\t\022\014\n\004uuid\030\004 \001(\t\022\022\n\ncreateTime\030\005 \001(\004\"(" + "\n\007JobType\022\010\n\004None\020\000\022\023\n\017IndexJobRunning\020\001" + "\"\304\001\n\020UpdateCounterMap\022-\n\004data\030\001 \003(\0132\037.rp" + "c.UpdateCounterMap.DataEntry\022\021\n\tgcCounte" + "r\030\002 \001(\004\022\014\n\004from\030\003 \001(\t\022\022\n\nupdateTime\030\004 \001(" + "\004\032L\n\tDataEntry\022\013\n\003key\030\001 \001(\t\022.\n\005value\030\002 \001" + "(\0132\037.rpc.TableSingleDataIndexStatus:\0028\001\"" + "\333\002\n\032TableSingleDataIndexStatus\022O\n\020tableC" + "onsumeData\030\001 \003(\01325.rpc.TableSingleDataIn" + "dexStatus.TableConsumeDataEntry\022$\n\034buffe" + "rQueueRemainingCapacity\030\002 \001(\r\022\033\n\023bufferQ" + "ueueUsedSize\030\003 \001(\r\022\031\n\021consumeErrorCount\030" + "\004 \001(\r\022\027\n\017ignoreRowsCount\030\005 \001(\r\022\014\n\004uuid\030\006" + " \001(\t\022\023\n\013tis30sAvgRT\030\007 \001(\004\022\031\n\021incrProcess" + "Paused\030\010 \001(\010\0327\n\025TableConsumeDataEntry\022\013\n" + "\003key\030\001 \001(\t\022\r\n\005value\030\002 \001(\004:\0028\001\"\007\n\005Empty\"\033" + "\n\nPingResult\022\r\n\005value\030\001 \001(\t2\237\002\n\nIncrStat" + "us\022%\n\004Ping\022\n.rpc.Empty\032\017.rpc.PingResult\"" + "\000\0227\n\014ReportStatus\022\025.rpc.UpdateCounterMap" + "\032\016.rpc.MasterJob\"\000\0227\n\020NodeLaunchReport\022\025" + ".rpc.LaunchReportInfo\032\n.rpc.Empty\"\000\0227\n\025r" + "eportDumpTableStatus\022\020.TableDumpStatus\032\n" + ".rpc.Empty\"\000\022?\n\026reportBuildIndexStatus\022\027" + ".BuildSharedPhaseStatus\032\n.rpc.Empty\"\000B2\n" + "\026com.qlangtech.tis.grpcB\017IncrStatusProto" + "P\001\242\002\004HLWSb\006proto3" };
        com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner = new com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner() {

            public com.google.protobuf.ExtensionRegistry assignDescriptors(com.google.protobuf.Descriptors.FileDescriptor root) {
                descriptor = root;
                return null;
            }
        };
        com.google.protobuf.Descriptors.FileDescriptor.internalBuildGeneratedFileFrom(descriptorData, new com.google.protobuf.Descriptors.FileDescriptor[] { com.qlangtech.tis.rpc.grpc.log.common.LogCollectorProto.getDescriptor() }, assigner);
        internal_static_rpc_LaunchReportInfo_descriptor = getDescriptor().getMessageTypes().get(0);
        internal_static_rpc_LaunchReportInfo_fieldAccessorTable = new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(internal_static_rpc_LaunchReportInfo_descriptor, new java.lang.String[] { "CollectionFocusTopicInfo" });
        internal_static_rpc_LaunchReportInfo_CollectionFocusTopicInfoEntry_descriptor = internal_static_rpc_LaunchReportInfo_descriptor.getNestedTypes().get(0);
        internal_static_rpc_LaunchReportInfo_CollectionFocusTopicInfoEntry_fieldAccessorTable = new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(internal_static_rpc_LaunchReportInfo_CollectionFocusTopicInfoEntry_descriptor, new java.lang.String[] { "Key", "Value" });
        internal_static_rpc_LaunchReportInfoEntry_descriptor = getDescriptor().getMessageTypes().get(1);
        internal_static_rpc_LaunchReportInfoEntry_fieldAccessorTable = new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(internal_static_rpc_LaunchReportInfoEntry_descriptor, new java.lang.String[] { "TopicName", "TagName" });
        internal_static_rpc_TopicInfo_descriptor = getDescriptor().getMessageTypes().get(2);
        internal_static_rpc_TopicInfo_fieldAccessorTable = new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(internal_static_rpc_TopicInfo_descriptor, new java.lang.String[] { "TopicWithTags" });
        internal_static_rpc_MasterJob_descriptor = getDescriptor().getMessageTypes().get(3);
        internal_static_rpc_MasterJob_fieldAccessorTable = new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(internal_static_rpc_MasterJob_descriptor, new java.lang.String[] { "JobType", "Stop", "IndexName", "Uuid", "CreateTime" });
        internal_static_rpc_UpdateCounterMap_descriptor = getDescriptor().getMessageTypes().get(4);
        internal_static_rpc_UpdateCounterMap_fieldAccessorTable = new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(internal_static_rpc_UpdateCounterMap_descriptor, new java.lang.String[] { "Data", "GcCounter", "From", "UpdateTime" });
        internal_static_rpc_UpdateCounterMap_DataEntry_descriptor = internal_static_rpc_UpdateCounterMap_descriptor.getNestedTypes().get(0);
        internal_static_rpc_UpdateCounterMap_DataEntry_fieldAccessorTable = new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(internal_static_rpc_UpdateCounterMap_DataEntry_descriptor, new java.lang.String[] { "Key", "Value" });
        internal_static_rpc_TableSingleDataIndexStatus_descriptor = getDescriptor().getMessageTypes().get(5);
        internal_static_rpc_TableSingleDataIndexStatus_fieldAccessorTable = new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(internal_static_rpc_TableSingleDataIndexStatus_descriptor, new java.lang.String[] { "TableConsumeData", "BufferQueueRemainingCapacity", "BufferQueueUsedSize", "ConsumeErrorCount", "IgnoreRowsCount", "Uuid", "Tis30SAvgRT", "IncrProcessPaused" });
        internal_static_rpc_TableSingleDataIndexStatus_TableConsumeDataEntry_descriptor = internal_static_rpc_TableSingleDataIndexStatus_descriptor.getNestedTypes().get(0);
        internal_static_rpc_TableSingleDataIndexStatus_TableConsumeDataEntry_fieldAccessorTable = new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(internal_static_rpc_TableSingleDataIndexStatus_TableConsumeDataEntry_descriptor, new java.lang.String[] { "Key", "Value" });
        internal_static_rpc_Empty_descriptor = getDescriptor().getMessageTypes().get(6);
        internal_static_rpc_Empty_fieldAccessorTable = new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(internal_static_rpc_Empty_descriptor, new java.lang.String[] {});
        internal_static_rpc_PingResult_descriptor = getDescriptor().getMessageTypes().get(7);
        internal_static_rpc_PingResult_fieldAccessorTable = new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(internal_static_rpc_PingResult_descriptor, new java.lang.String[] { "Value" });
        com.qlangtech.tis.rpc.grpc.log.common.LogCollectorProto.getDescriptor();
    }
    // @@protoc_insertion_point(outer_class_scope)
}
