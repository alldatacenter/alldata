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
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/09/25
 */
public final class LogCollectorProto {

    private LogCollectorProto() {
    }

    public static void registerAllExtensions(com.google.protobuf.ExtensionRegistryLite registry) {
    }

    public static void registerAllExtensions(com.google.protobuf.ExtensionRegistry registry) {
        registerAllExtensions((com.google.protobuf.ExtensionRegistryLite) registry);
    }

    static final com.google.protobuf.Descriptors.Descriptor internal_static_stream_PBuildPhaseStatusParam_descriptor;

    static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_stream_PBuildPhaseStatusParam_fieldAccessorTable;

    static final com.google.protobuf.Descriptors.Descriptor internal_static_stream_PPhaseStatusCollection_descriptor;

    static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_stream_PPhaseStatusCollection_fieldAccessorTable;

    static final com.google.protobuf.Descriptors.Descriptor internal_static_stream_PDumpPhaseStatus_descriptor;

    static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_stream_PDumpPhaseStatus_fieldAccessorTable;

    static final com.google.protobuf.Descriptors.Descriptor internal_static_stream_PDumpPhaseStatus_TablesDumpEntry_descriptor;

    static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_stream_PDumpPhaseStatus_TablesDumpEntry_fieldAccessorTable;

    static final com.google.protobuf.Descriptors.Descriptor internal_static_stream_PJoinPhaseStatus_descriptor;

    static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_stream_PJoinPhaseStatus_fieldAccessorTable;

    static final com.google.protobuf.Descriptors.Descriptor internal_static_stream_PJoinPhaseStatus_TaskStatusEntry_descriptor;

    static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_stream_PJoinPhaseStatus_TaskStatusEntry_fieldAccessorTable;

    static final com.google.protobuf.Descriptors.Descriptor internal_static_stream_PBuildPhaseStatus_descriptor;

    static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_stream_PBuildPhaseStatus_fieldAccessorTable;

    static final com.google.protobuf.Descriptors.Descriptor internal_static_stream_PBuildPhaseStatus_NodeBuildStatusEntry_descriptor;

    static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_stream_PBuildPhaseStatus_NodeBuildStatusEntry_fieldAccessorTable;

    static final com.google.protobuf.Descriptors.Descriptor internal_static_stream_PIndexBackFlowPhaseStatus_descriptor;

    static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_stream_PIndexBackFlowPhaseStatus_fieldAccessorTable;

    static final com.google.protobuf.Descriptors.Descriptor internal_static_stream_PIndexBackFlowPhaseStatus_NodesStatusEntry_descriptor;

    static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_stream_PIndexBackFlowPhaseStatus_NodesStatusEntry_fieldAccessorTable;

    static final com.google.protobuf.Descriptors.Descriptor internal_static_stream_PMonotorTarget_descriptor;

    static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_stream_PMonotorTarget_fieldAccessorTable;

    static final com.google.protobuf.Descriptors.Descriptor internal_static_stream_PExecuteState_descriptor;

    static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_stream_PExecuteState_fieldAccessorTable;

    public static com.google.protobuf.Descriptors.FileDescriptor getDescriptor() {
        return descriptor;
    }

    private static com.google.protobuf.Descriptors.FileDescriptor descriptor;

    static {
        java.lang.String[] descriptorData = { "\n\023log-collector.proto\022\006stream\032\020common-ms" + "g.proto\"(\n\026PBuildPhaseStatusParam\022\016\n\006tas" + "kid\030\001 \001(\004\"\366\001\n\026PPhaseStatusCollection\022+\n\t" + "dumpPhase\030\001 \001(\0132\030.stream.PDumpPhaseStatu" + "s\022+\n\tjoinPhase\030\002 \001(\0132\030.stream.PJoinPhase" + "Status\022-\n\nbuildPhase\030\003 \001(\0132\031.stream.PBui" + "ldPhaseStatus\022C\n\030indexBackFlowPhaseStatu" + "s\030\004 \001(\0132!.stream.PIndexBackFlowPhaseStat" + "us\022\016\n\006taskId\030\005 \001(\r\"\225\001\n\020PDumpPhaseStatus\022" + "<\n\ntablesDump\030\001 \003(\0132(.stream.PDumpPhaseS" + "tatus.TablesDumpEntry\032C\n\017TablesDumpEntry" + "\022\013\n\003key\030\001 \001(\t\022\037\n\005value\030\002 \001(\0132\020.TableDump" + "Status:\0028\001\"\224\001\n\020PJoinPhaseStatus\022<\n\ntaskS" + "tatus\030\001 \003(\0132(.stream.PJoinPhaseStatus.Ta" + "skStatusEntry\032B\n\017TaskStatusEntry\022\013\n\003key\030" + "\001 \001(\t\022\036\n\005value\030\002 \001(\0132\017.JoinTaskStatus:\0028" + "\001\"\255\001\n\021PBuildPhaseStatus\022G\n\017nodeBuildStat" + "us\030\001 \003(\0132..stream.PBuildPhaseStatus.Node" + "BuildStatusEntry\032O\n\024NodeBuildStatusEntry" + "\022\013\n\003key\030\001 \001(\t\022&\n\005value\030\002 \001(\0132\027.BuildShar" + "edPhaseStatus:\0028\001\"\255\001\n\031PIndexBackFlowPhas" + "eStatus\022G\n\013nodesStatus\030\001 \003(\01322.stream.PI" + "ndexBackFlowPhaseStatus.NodesStatusEntry" + "\032G\n\020NodesStatusEntry\022\013\n\003key\030\001 \001(\t\022\"\n\005val" + "ue\030\002 \001(\0132\023.NodeBackflowStatus:\0028\001\"d\n\016PMo" + "notorTarget\022\022\n\ncollection\030\001 \001(\t\022\016\n\006taski" + "d\030\002 \001(\r\022.\n\007logtype\030\003 \001(\0162\035.stream.PExecu" + "teState.LogType\"\213\003\n\rPExecuteState\0220\n\010inf" + "oType\030\001 \001(\0162\036.stream.PExecuteState.InfoT" + "ype\022.\n\007logType\030\002 \001(\0162\035.stream.PExecuteSt" + "ate.LogType\022\013\n\003msg\030\003 \001(\t\022\014\n\004from\030\004 \001(\t\022\r" + "\n\005jobId\030\005 \001(\004\022\016\n\006taskId\030\006 \001(\004\022\023\n\013service" + "Name\030\007 \001(\t\022\021\n\texecState\030\010 \001(\t\022\014\n\004time\030\t " + "\001(\004\022\021\n\tcomponent\030\n \001(\t\"_\n\007LogType\022\035\n\031INC" + "R_DEPLOY_STATUS_CHANGE\020\000\022\022\n\016MQ_TAGS_STAT" + "US\020\001\022\010\n\004FULL\020\002\022\010\n\004INCR\020\003\022\r\n\tINCR_SEND\020\004\"" + "4\n\010InfoType\022\010\n\004INFO\020\000\022\010\n\004WARN\020\001\022\t\n\005ERROR" + "\020\002\022\t\n\005FATAL\020\0032\264\001\n\014LogCollector\022K\n\024Regist" + "erMonitorEvent\022\026.stream.PMonotorTarget\032\025" + ".stream.PExecuteState\"\000(\0010\001\022W\n\021BuildPhra" + "seStatus\022\036.stream.PBuildPhaseStatusParam" + "\032\036.stream.PPhaseStatusCollection\"\0000\001BC\n%" + "com.qlangtech.tis.rpc.grpc.log.streamB\021L" + "ogCollectorProtoP\001\242\002\004HLWSb\006proto3" };
        com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner = new com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner() {

            public com.google.protobuf.ExtensionRegistry assignDescriptors(com.google.protobuf.Descriptors.FileDescriptor root) {
                descriptor = root;
                return null;
            }
        };
        com.google.protobuf.Descriptors.FileDescriptor.internalBuildGeneratedFileFrom(descriptorData, new com.google.protobuf.Descriptors.FileDescriptor[] { com.qlangtech.tis.rpc.grpc.log.common.LogCollectorProto.getDescriptor() }, assigner);
        internal_static_stream_PBuildPhaseStatusParam_descriptor = getDescriptor().getMessageTypes().get(0);
        internal_static_stream_PBuildPhaseStatusParam_fieldAccessorTable = new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(internal_static_stream_PBuildPhaseStatusParam_descriptor, new java.lang.String[] { "Taskid" });
        internal_static_stream_PPhaseStatusCollection_descriptor = getDescriptor().getMessageTypes().get(1);
        internal_static_stream_PPhaseStatusCollection_fieldAccessorTable = new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(internal_static_stream_PPhaseStatusCollection_descriptor, new java.lang.String[] { "DumpPhase", "JoinPhase", "BuildPhase", "IndexBackFlowPhaseStatus", "TaskId" });
        internal_static_stream_PDumpPhaseStatus_descriptor = getDescriptor().getMessageTypes().get(2);
        internal_static_stream_PDumpPhaseStatus_fieldAccessorTable = new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(internal_static_stream_PDumpPhaseStatus_descriptor, new java.lang.String[] { "TablesDump" });
        internal_static_stream_PDumpPhaseStatus_TablesDumpEntry_descriptor = internal_static_stream_PDumpPhaseStatus_descriptor.getNestedTypes().get(0);
        internal_static_stream_PDumpPhaseStatus_TablesDumpEntry_fieldAccessorTable = new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(internal_static_stream_PDumpPhaseStatus_TablesDumpEntry_descriptor, new java.lang.String[] { "Key", "Value" });
        internal_static_stream_PJoinPhaseStatus_descriptor = getDescriptor().getMessageTypes().get(3);
        internal_static_stream_PJoinPhaseStatus_fieldAccessorTable = new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(internal_static_stream_PJoinPhaseStatus_descriptor, new java.lang.String[] { "TaskStatus" });
        internal_static_stream_PJoinPhaseStatus_TaskStatusEntry_descriptor = internal_static_stream_PJoinPhaseStatus_descriptor.getNestedTypes().get(0);
        internal_static_stream_PJoinPhaseStatus_TaskStatusEntry_fieldAccessorTable = new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(internal_static_stream_PJoinPhaseStatus_TaskStatusEntry_descriptor, new java.lang.String[] { "Key", "Value" });
        internal_static_stream_PBuildPhaseStatus_descriptor = getDescriptor().getMessageTypes().get(4);
        internal_static_stream_PBuildPhaseStatus_fieldAccessorTable = new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(internal_static_stream_PBuildPhaseStatus_descriptor, new java.lang.String[] { "NodeBuildStatus" });
        internal_static_stream_PBuildPhaseStatus_NodeBuildStatusEntry_descriptor = internal_static_stream_PBuildPhaseStatus_descriptor.getNestedTypes().get(0);
        internal_static_stream_PBuildPhaseStatus_NodeBuildStatusEntry_fieldAccessorTable = new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(internal_static_stream_PBuildPhaseStatus_NodeBuildStatusEntry_descriptor, new java.lang.String[] { "Key", "Value" });
        internal_static_stream_PIndexBackFlowPhaseStatus_descriptor = getDescriptor().getMessageTypes().get(5);
        internal_static_stream_PIndexBackFlowPhaseStatus_fieldAccessorTable = new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(internal_static_stream_PIndexBackFlowPhaseStatus_descriptor, new java.lang.String[] { "NodesStatus" });
        internal_static_stream_PIndexBackFlowPhaseStatus_NodesStatusEntry_descriptor = internal_static_stream_PIndexBackFlowPhaseStatus_descriptor.getNestedTypes().get(0);
        internal_static_stream_PIndexBackFlowPhaseStatus_NodesStatusEntry_fieldAccessorTable = new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(internal_static_stream_PIndexBackFlowPhaseStatus_NodesStatusEntry_descriptor, new java.lang.String[] { "Key", "Value" });
        internal_static_stream_PMonotorTarget_descriptor = getDescriptor().getMessageTypes().get(6);
        internal_static_stream_PMonotorTarget_fieldAccessorTable = new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(internal_static_stream_PMonotorTarget_descriptor, new java.lang.String[] { "Collection", "Taskid", "Logtype" });
        internal_static_stream_PExecuteState_descriptor = getDescriptor().getMessageTypes().get(7);
        internal_static_stream_PExecuteState_fieldAccessorTable = new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(internal_static_stream_PExecuteState_descriptor, new java.lang.String[] { "InfoType", "LogType", "Msg", "From", "JobId", "TaskId", "ServiceName", "ExecState", "Time", "Component" });
        com.qlangtech.tis.rpc.grpc.log.common.LogCollectorProto.getDescriptor();
    }
    // @@protoc_insertion_point(outer_class_scope)
}
