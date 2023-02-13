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

import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ClientCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ClientCalls.asyncClientStreamingCall;
import static io.grpc.stub.ClientCalls.asyncServerStreamingCall;
import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.blockingServerStreamingCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.stub.ServerCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ServerCalls.asyncClientStreamingCall;
import static io.grpc.stub.ServerCalls.asyncServerStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;

/**
 * <pre>
 * IncrStatusUmbilicalProtocol
 * </pre>
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/09/25
 */
@javax.annotation.Generated(value = "by gRPC proto compiler (version 1.29.0)", comments = "Source: incr-status.proto")
public final class IncrStatusGrpc {

    private IncrStatusGrpc() {
    }

    public static final String SERVICE_NAME = "rpc.IncrStatus";

    // Static method descriptors that strictly reflect the proto.
    private static volatile io.grpc.MethodDescriptor<com.qlangtech.tis.grpc.Empty, com.qlangtech.tis.grpc.PingResult> getPingMethod;

    @io.grpc.stub.annotations.RpcMethod(fullMethodName = SERVICE_NAME + '/' + "Ping", requestType = com.qlangtech.tis.grpc.Empty.class, responseType = com.qlangtech.tis.grpc.PingResult.class, methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
    public static io.grpc.MethodDescriptor<com.qlangtech.tis.grpc.Empty, com.qlangtech.tis.grpc.PingResult> getPingMethod() {
        io.grpc.MethodDescriptor<com.qlangtech.tis.grpc.Empty, com.qlangtech.tis.grpc.PingResult> getPingMethod;
        if ((getPingMethod = IncrStatusGrpc.getPingMethod) == null) {
            synchronized (IncrStatusGrpc.class) {
                if ((getPingMethod = IncrStatusGrpc.getPingMethod) == null) {
                    IncrStatusGrpc.getPingMethod = getPingMethod = io.grpc.MethodDescriptor.<com.qlangtech.tis.grpc.Empty, com.qlangtech.tis.grpc.PingResult>newBuilder().setType(io.grpc.MethodDescriptor.MethodType.UNARY).setFullMethodName(generateFullMethodName(SERVICE_NAME, "Ping")).setSampledToLocalTracing(true).setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(com.qlangtech.tis.grpc.Empty.getDefaultInstance())).setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(com.qlangtech.tis.grpc.PingResult.getDefaultInstance())).setSchemaDescriptor(new IncrStatusMethodDescriptorSupplier("Ping")).build();
                }
            }
        }
        return getPingMethod;
    }

    private static volatile io.grpc.MethodDescriptor<com.qlangtech.tis.grpc.UpdateCounterMap, com.qlangtech.tis.grpc.MasterJob> getReportStatusMethod;

    @io.grpc.stub.annotations.RpcMethod(fullMethodName = SERVICE_NAME + '/' + "ReportStatus", requestType = com.qlangtech.tis.grpc.UpdateCounterMap.class, responseType = com.qlangtech.tis.grpc.MasterJob.class, methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
    public static io.grpc.MethodDescriptor<com.qlangtech.tis.grpc.UpdateCounterMap, com.qlangtech.tis.grpc.MasterJob> getReportStatusMethod() {
        io.grpc.MethodDescriptor<com.qlangtech.tis.grpc.UpdateCounterMap, com.qlangtech.tis.grpc.MasterJob> getReportStatusMethod;
        if ((getReportStatusMethod = IncrStatusGrpc.getReportStatusMethod) == null) {
            synchronized (IncrStatusGrpc.class) {
                if ((getReportStatusMethod = IncrStatusGrpc.getReportStatusMethod) == null) {
                    IncrStatusGrpc.getReportStatusMethod = getReportStatusMethod = io.grpc.MethodDescriptor.<com.qlangtech.tis.grpc.UpdateCounterMap, com.qlangtech.tis.grpc.MasterJob>newBuilder().setType(io.grpc.MethodDescriptor.MethodType.UNARY).setFullMethodName(generateFullMethodName(SERVICE_NAME, "ReportStatus")).setSampledToLocalTracing(true).setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(com.qlangtech.tis.grpc.UpdateCounterMap.getDefaultInstance())).setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(com.qlangtech.tis.grpc.MasterJob.getDefaultInstance())).setSchemaDescriptor(new IncrStatusMethodDescriptorSupplier("ReportStatus")).build();
                }
            }
        }
        return getReportStatusMethod;
    }

    private static volatile io.grpc.MethodDescriptor<com.qlangtech.tis.grpc.LaunchReportInfo, com.qlangtech.tis.grpc.Empty> getNodeLaunchReportMethod;

    @io.grpc.stub.annotations.RpcMethod(fullMethodName = SERVICE_NAME + '/' + "NodeLaunchReport", requestType = com.qlangtech.tis.grpc.LaunchReportInfo.class, responseType = com.qlangtech.tis.grpc.Empty.class, methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
    public static io.grpc.MethodDescriptor<com.qlangtech.tis.grpc.LaunchReportInfo, com.qlangtech.tis.grpc.Empty> getNodeLaunchReportMethod() {
        io.grpc.MethodDescriptor<com.qlangtech.tis.grpc.LaunchReportInfo, com.qlangtech.tis.grpc.Empty> getNodeLaunchReportMethod;
        if ((getNodeLaunchReportMethod = IncrStatusGrpc.getNodeLaunchReportMethod) == null) {
            synchronized (IncrStatusGrpc.class) {
                if ((getNodeLaunchReportMethod = IncrStatusGrpc.getNodeLaunchReportMethod) == null) {
                    IncrStatusGrpc.getNodeLaunchReportMethod = getNodeLaunchReportMethod = io.grpc.MethodDescriptor.<com.qlangtech.tis.grpc.LaunchReportInfo, com.qlangtech.tis.grpc.Empty>newBuilder().setType(io.grpc.MethodDescriptor.MethodType.UNARY).setFullMethodName(generateFullMethodName(SERVICE_NAME, "NodeLaunchReport")).setSampledToLocalTracing(true).setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(com.qlangtech.tis.grpc.LaunchReportInfo.getDefaultInstance())).setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(com.qlangtech.tis.grpc.Empty.getDefaultInstance())).setSchemaDescriptor(new IncrStatusMethodDescriptorSupplier("NodeLaunchReport")).build();
                }
            }
        }
        return getNodeLaunchReportMethod;
    }

    private static volatile io.grpc.MethodDescriptor<com.qlangtech.tis.rpc.grpc.log.common.TableDumpStatus, com.qlangtech.tis.grpc.Empty> getReportDumpTableStatusMethod;

    @io.grpc.stub.annotations.RpcMethod(fullMethodName = SERVICE_NAME + '/' + "reportDumpTableStatus", requestType = com.qlangtech.tis.rpc.grpc.log.common.TableDumpStatus.class, responseType = com.qlangtech.tis.grpc.Empty.class, methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
    public static io.grpc.MethodDescriptor<com.qlangtech.tis.rpc.grpc.log.common.TableDumpStatus, com.qlangtech.tis.grpc.Empty> getReportDumpTableStatusMethod() {
        io.grpc.MethodDescriptor<com.qlangtech.tis.rpc.grpc.log.common.TableDumpStatus, com.qlangtech.tis.grpc.Empty> getReportDumpTableStatusMethod;
        if ((getReportDumpTableStatusMethod = IncrStatusGrpc.getReportDumpTableStatusMethod) == null) {
            synchronized (IncrStatusGrpc.class) {
                if ((getReportDumpTableStatusMethod = IncrStatusGrpc.getReportDumpTableStatusMethod) == null) {
                    IncrStatusGrpc.getReportDumpTableStatusMethod = getReportDumpTableStatusMethod = io.grpc.MethodDescriptor.<com.qlangtech.tis.rpc.grpc.log.common.TableDumpStatus, com.qlangtech.tis.grpc.Empty>newBuilder().setType(io.grpc.MethodDescriptor.MethodType.UNARY).setFullMethodName(generateFullMethodName(SERVICE_NAME, "reportDumpTableStatus")).setSampledToLocalTracing(true).setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(com.qlangtech.tis.rpc.grpc.log.common.TableDumpStatus.getDefaultInstance())).setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(com.qlangtech.tis.grpc.Empty.getDefaultInstance())).setSchemaDescriptor(new IncrStatusMethodDescriptorSupplier("reportDumpTableStatus")).build();
                }
            }
        }
        return getReportDumpTableStatusMethod;
    }

    private static volatile io.grpc.MethodDescriptor<com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus, com.qlangtech.tis.grpc.Empty> getReportBuildIndexStatusMethod;

    @io.grpc.stub.annotations.RpcMethod(fullMethodName = SERVICE_NAME + '/' + "reportBuildIndexStatus", requestType = com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus.class, responseType = com.qlangtech.tis.grpc.Empty.class, methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
    public static io.grpc.MethodDescriptor<com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus, com.qlangtech.tis.grpc.Empty> getReportBuildIndexStatusMethod() {
        io.grpc.MethodDescriptor<com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus, com.qlangtech.tis.grpc.Empty> getReportBuildIndexStatusMethod;
        if ((getReportBuildIndexStatusMethod = IncrStatusGrpc.getReportBuildIndexStatusMethod) == null) {
            synchronized (IncrStatusGrpc.class) {
                if ((getReportBuildIndexStatusMethod = IncrStatusGrpc.getReportBuildIndexStatusMethod) == null) {
                    IncrStatusGrpc.getReportBuildIndexStatusMethod = getReportBuildIndexStatusMethod = io.grpc.MethodDescriptor.<com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus, com.qlangtech.tis.grpc.Empty>newBuilder().setType(io.grpc.MethodDescriptor.MethodType.UNARY).setFullMethodName(generateFullMethodName(SERVICE_NAME, "reportBuildIndexStatus")).setSampledToLocalTracing(true).setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus.getDefaultInstance())).setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(com.qlangtech.tis.grpc.Empty.getDefaultInstance())).setSchemaDescriptor(new IncrStatusMethodDescriptorSupplier("reportBuildIndexStatus")).build();
                }
            }
        }
        return getReportBuildIndexStatusMethod;
    }

    /**
     * Creates a new async stub that supports all call types for the service
     */
    public static IncrStatusStub newStub(io.grpc.Channel channel) {
        io.grpc.stub.AbstractStub.StubFactory<IncrStatusStub> factory = new io.grpc.stub.AbstractStub.StubFactory<IncrStatusStub>() {

            @java.lang.Override
            public IncrStatusStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
                return new IncrStatusStub(channel, callOptions);
            }
        };
        return IncrStatusStub.newStub(factory, channel);
    }

    /**
     * Creates a new blocking-style stub that supports unary and streaming output calls on the service
     */
    public static IncrStatusBlockingStub newBlockingStub(io.grpc.Channel channel) {
        io.grpc.stub.AbstractStub.StubFactory<IncrStatusBlockingStub> factory = new io.grpc.stub.AbstractStub.StubFactory<IncrStatusBlockingStub>() {

            @java.lang.Override
            public IncrStatusBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
                return new IncrStatusBlockingStub(channel, callOptions);
            }
        };
        return IncrStatusBlockingStub.newStub(factory, channel);
    }

    /**
     * Creates a new ListenableFuture-style stub that supports unary calls on the service
     */
    public static IncrStatusFutureStub newFutureStub(io.grpc.Channel channel) {
        io.grpc.stub.AbstractStub.StubFactory<IncrStatusFutureStub> factory = new io.grpc.stub.AbstractStub.StubFactory<IncrStatusFutureStub>() {

            @java.lang.Override
            public IncrStatusFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
                return new IncrStatusFutureStub(channel, callOptions);
            }
        };
        return IncrStatusFutureStub.newStub(factory, channel);
    }

    /**
     * <pre>
     * IncrStatusUmbilicalProtocol
     * </pre>
     */
    public abstract static class IncrStatusImplBase implements io.grpc.BindableService {

        /**
         */
        public void ping(com.qlangtech.tis.grpc.Empty request, io.grpc.stub.StreamObserver<com.qlangtech.tis.grpc.PingResult> responseObserver) {
            asyncUnimplementedUnaryCall(getPingMethod(), responseObserver);
        }

        /**
         */
        public void reportStatus(com.qlangtech.tis.grpc.UpdateCounterMap request, io.grpc.stub.StreamObserver<com.qlangtech.tis.grpc.MasterJob> responseObserver) {
            asyncUnimplementedUnaryCall(getReportStatusMethod(), responseObserver);
        }

        /**
         */
        public void nodeLaunchReport(com.qlangtech.tis.grpc.LaunchReportInfo request, io.grpc.stub.StreamObserver<com.qlangtech.tis.grpc.Empty> responseObserver) {
            asyncUnimplementedUnaryCall(getNodeLaunchReportMethod(), responseObserver);
        }

        /**
         */
        public void reportDumpTableStatus(com.qlangtech.tis.rpc.grpc.log.common.TableDumpStatus request, io.grpc.stub.StreamObserver<com.qlangtech.tis.grpc.Empty> responseObserver) {
            asyncUnimplementedUnaryCall(getReportDumpTableStatusMethod(), responseObserver);
        }

        /**
         */
        public void reportBuildIndexStatus(com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus request, io.grpc.stub.StreamObserver<com.qlangtech.tis.grpc.Empty> responseObserver) {
            asyncUnimplementedUnaryCall(getReportBuildIndexStatusMethod(), responseObserver);
        }

        @java.lang.Override
        public final io.grpc.ServerServiceDefinition bindService() {
            return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor()).addMethod(getPingMethod(), asyncUnaryCall(new MethodHandlers<com.qlangtech.tis.grpc.Empty, com.qlangtech.tis.grpc.PingResult>(this, METHODID_PING))).addMethod(getReportStatusMethod(), asyncUnaryCall(new MethodHandlers<com.qlangtech.tis.grpc.UpdateCounterMap, com.qlangtech.tis.grpc.MasterJob>(this, METHODID_REPORT_STATUS))).addMethod(getNodeLaunchReportMethod(), asyncUnaryCall(new MethodHandlers<com.qlangtech.tis.grpc.LaunchReportInfo, com.qlangtech.tis.grpc.Empty>(this, METHODID_NODE_LAUNCH_REPORT))).addMethod(getReportDumpTableStatusMethod(), asyncUnaryCall(new MethodHandlers<com.qlangtech.tis.rpc.grpc.log.common.TableDumpStatus, com.qlangtech.tis.grpc.Empty>(this, METHODID_REPORT_DUMP_TABLE_STATUS))).addMethod(getReportBuildIndexStatusMethod(), asyncUnaryCall(new MethodHandlers<com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus, com.qlangtech.tis.grpc.Empty>(this, METHODID_REPORT_BUILD_INDEX_STATUS))).build();
        }
    }

    /**
     * <pre>
     * IncrStatusUmbilicalProtocol
     * </pre>
     */
    public static final class IncrStatusStub extends io.grpc.stub.AbstractAsyncStub<IncrStatusStub> {

        private IncrStatusStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            super(channel, callOptions);
        }

        @java.lang.Override
        protected IncrStatusStub build(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            return new IncrStatusStub(channel, callOptions);
        }

        /**
         */
        public void ping(com.qlangtech.tis.grpc.Empty request, io.grpc.stub.StreamObserver<com.qlangtech.tis.grpc.PingResult> responseObserver) {
            asyncUnaryCall(getChannel().newCall(getPingMethod(), getCallOptions()), request, responseObserver);
        }

        /**
         */
        public void reportStatus(com.qlangtech.tis.grpc.UpdateCounterMap request, io.grpc.stub.StreamObserver<com.qlangtech.tis.grpc.MasterJob> responseObserver) {
            asyncUnaryCall(getChannel().newCall(getReportStatusMethod(), getCallOptions()), request, responseObserver);
        }

        /**
         */
        public void nodeLaunchReport(com.qlangtech.tis.grpc.LaunchReportInfo request, io.grpc.stub.StreamObserver<com.qlangtech.tis.grpc.Empty> responseObserver) {
            asyncUnaryCall(getChannel().newCall(getNodeLaunchReportMethod(), getCallOptions()), request, responseObserver);
        }

        /**
         */
        public void reportDumpTableStatus(com.qlangtech.tis.rpc.grpc.log.common.TableDumpStatus request, io.grpc.stub.StreamObserver<com.qlangtech.tis.grpc.Empty> responseObserver) {
            asyncUnaryCall(getChannel().newCall(getReportDumpTableStatusMethod(), getCallOptions()), request, responseObserver);
        }

        /**
         */
        public void reportBuildIndexStatus(com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus request, io.grpc.stub.StreamObserver<com.qlangtech.tis.grpc.Empty> responseObserver) {
            asyncUnaryCall(getChannel().newCall(getReportBuildIndexStatusMethod(), getCallOptions()), request, responseObserver);
        }
    }

    /**
     * <pre>
     * IncrStatusUmbilicalProtocol
     * </pre>
     */
    public static final class IncrStatusBlockingStub extends io.grpc.stub.AbstractBlockingStub<IncrStatusBlockingStub> {

        private IncrStatusBlockingStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            super(channel, callOptions);
        }

        @java.lang.Override
        protected IncrStatusBlockingStub build(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            return new IncrStatusBlockingStub(channel, callOptions);
        }

        /**
         */
        public com.qlangtech.tis.grpc.PingResult ping(com.qlangtech.tis.grpc.Empty request) {
            return blockingUnaryCall(getChannel(), getPingMethod(), getCallOptions(), request);
        }

        /**
         */
        public com.qlangtech.tis.grpc.MasterJob reportStatus(com.qlangtech.tis.grpc.UpdateCounterMap request) {
            return blockingUnaryCall(getChannel(), getReportStatusMethod(), getCallOptions(), request);
        }

        /**
         */
        public com.qlangtech.tis.grpc.Empty nodeLaunchReport(com.qlangtech.tis.grpc.LaunchReportInfo request) {
            return blockingUnaryCall(getChannel(), getNodeLaunchReportMethod(), getCallOptions(), request);
        }

        /**
         */
        public com.qlangtech.tis.grpc.Empty reportDumpTableStatus(com.qlangtech.tis.rpc.grpc.log.common.TableDumpStatus request) {
            return blockingUnaryCall(getChannel(), getReportDumpTableStatusMethod(), getCallOptions(), request);
        }

        /**
         */
        public com.qlangtech.tis.grpc.Empty reportBuildIndexStatus(com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus request) {
            return blockingUnaryCall(getChannel(), getReportBuildIndexStatusMethod(), getCallOptions(), request);
        }
    }

    /**
     * <pre>
     * IncrStatusUmbilicalProtocol
     * </pre>
     */
    public static final class IncrStatusFutureStub extends io.grpc.stub.AbstractFutureStub<IncrStatusFutureStub> {

        private IncrStatusFutureStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            super(channel, callOptions);
        }

        @java.lang.Override
        protected IncrStatusFutureStub build(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            return new IncrStatusFutureStub(channel, callOptions);
        }

        /**
         */
        public com.google.common.util.concurrent.ListenableFuture<com.qlangtech.tis.grpc.PingResult> ping(com.qlangtech.tis.grpc.Empty request) {
            return futureUnaryCall(getChannel().newCall(getPingMethod(), getCallOptions()), request);
        }

        /**
         */
        public com.google.common.util.concurrent.ListenableFuture<com.qlangtech.tis.grpc.MasterJob> reportStatus(com.qlangtech.tis.grpc.UpdateCounterMap request) {
            return futureUnaryCall(getChannel().newCall(getReportStatusMethod(), getCallOptions()), request);
        }

        /**
         */
        public com.google.common.util.concurrent.ListenableFuture<com.qlangtech.tis.grpc.Empty> nodeLaunchReport(com.qlangtech.tis.grpc.LaunchReportInfo request) {
            return futureUnaryCall(getChannel().newCall(getNodeLaunchReportMethod(), getCallOptions()), request);
        }

        /**
         */
        public com.google.common.util.concurrent.ListenableFuture<com.qlangtech.tis.grpc.Empty> reportDumpTableStatus(com.qlangtech.tis.rpc.grpc.log.common.TableDumpStatus request) {
            return futureUnaryCall(getChannel().newCall(getReportDumpTableStatusMethod(), getCallOptions()), request);
        }

        /**
         */
        public com.google.common.util.concurrent.ListenableFuture<com.qlangtech.tis.grpc.Empty> reportBuildIndexStatus(com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus request) {
            return futureUnaryCall(getChannel().newCall(getReportBuildIndexStatusMethod(), getCallOptions()), request);
        }
    }

    private static final int METHODID_PING = 0;

    private static final int METHODID_REPORT_STATUS = 1;

    private static final int METHODID_NODE_LAUNCH_REPORT = 2;

    private static final int METHODID_REPORT_DUMP_TABLE_STATUS = 3;

    private static final int METHODID_REPORT_BUILD_INDEX_STATUS = 4;

    private static final class MethodHandlers<Req, Resp> implements io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>, io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>, io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>, io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {

        private final IncrStatusImplBase serviceImpl;

        private final int methodId;

        MethodHandlers(IncrStatusImplBase serviceImpl, int methodId) {
            this.serviceImpl = serviceImpl;
            this.methodId = methodId;
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("unchecked")
        public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
            switch(methodId) {
                case METHODID_PING:
                    serviceImpl.ping((com.qlangtech.tis.grpc.Empty) request, (io.grpc.stub.StreamObserver<com.qlangtech.tis.grpc.PingResult>) responseObserver);
                    break;
                case METHODID_REPORT_STATUS:
                    serviceImpl.reportStatus((com.qlangtech.tis.grpc.UpdateCounterMap) request, (io.grpc.stub.StreamObserver<com.qlangtech.tis.grpc.MasterJob>) responseObserver);
                    break;
                case METHODID_NODE_LAUNCH_REPORT:
                    serviceImpl.nodeLaunchReport((com.qlangtech.tis.grpc.LaunchReportInfo) request, (io.grpc.stub.StreamObserver<com.qlangtech.tis.grpc.Empty>) responseObserver);
                    break;
                case METHODID_REPORT_DUMP_TABLE_STATUS:
                    serviceImpl.reportDumpTableStatus((com.qlangtech.tis.rpc.grpc.log.common.TableDumpStatus) request, (io.grpc.stub.StreamObserver<com.qlangtech.tis.grpc.Empty>) responseObserver);
                    break;
                case METHODID_REPORT_BUILD_INDEX_STATUS:
                    serviceImpl.reportBuildIndexStatus((com.qlangtech.tis.rpc.grpc.log.common.BuildSharedPhaseStatus) request, (io.grpc.stub.StreamObserver<com.qlangtech.tis.grpc.Empty>) responseObserver);
                    break;
                default:
                    throw new AssertionError();
            }
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("unchecked")
        public io.grpc.stub.StreamObserver<Req> invoke(io.grpc.stub.StreamObserver<Resp> responseObserver) {
            switch(methodId) {
                default:
                    throw new AssertionError();
            }
        }
    }

    private abstract static class IncrStatusBaseDescriptorSupplier implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {

        IncrStatusBaseDescriptorSupplier() {
        }

        @java.lang.Override
        public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
            return com.qlangtech.tis.grpc.IncrStatusProto.getDescriptor();
        }

        @java.lang.Override
        public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
            return getFileDescriptor().findServiceByName("IncrStatus");
        }
    }

    private static final class IncrStatusFileDescriptorSupplier extends IncrStatusBaseDescriptorSupplier {

        IncrStatusFileDescriptorSupplier() {
        }
    }

    private static final class IncrStatusMethodDescriptorSupplier extends IncrStatusBaseDescriptorSupplier implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {

        private final String methodName;

        IncrStatusMethodDescriptorSupplier(String methodName) {
            this.methodName = methodName;
        }

        @java.lang.Override
        public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
            return getServiceDescriptor().findMethodByName(methodName);
        }
    }

    private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

    public static io.grpc.ServiceDescriptor getServiceDescriptor() {
        io.grpc.ServiceDescriptor result = serviceDescriptor;
        if (result == null) {
            synchronized (IncrStatusGrpc.class) {
                result = serviceDescriptor;
                if (result == null) {
                    serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME).setSchemaDescriptor(new IncrStatusFileDescriptorSupplier()).addMethod(getPingMethod()).addMethod(getReportStatusMethod()).addMethod(getNodeLaunchReportMethod()).addMethod(getReportDumpTableStatusMethod()).addMethod(getReportBuildIndexStatusMethod()).build();
                }
            }
        }
        return result;
    }
}
