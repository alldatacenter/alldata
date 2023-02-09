/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.cloud.filestore.v1beta1;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 *
 *
 * <pre>
 * Configures and manages Filestore resources.
 * Filestore Manager v1beta1.
 * The `file.googleapis.com` service implements the Filestore API and
 * defines the following model for managing resources:
 * * The service works with a collection of cloud projects, named: `/projects/&#42;`
 * * Each project has a collection of available locations, named: `/locations/&#42;`
 * * Each location has a collection of instances and backups, named:
 * `/instances/&#42;` and `/backups/&#42;` respectively.
 * * As such, Filestore instances are resources of the form:
 *   `/projects/{project_id}/locations/{location_id}/instances/{instance_id}`
 *   backups are resources of the form:
 *   `/projects/{project_id}/locations/{location_id}/backup/{backup_id}`
 * Note that location_id can represent a GCP `zone` or `region` depending on the
 * resource.
 * for example:
 * A zonal Filestore instance:
 * * `projects/my-project/locations/us-central1-c/instances/my-basic-tier-filer`
 * A regional Filestore instance:
 * * `projects/my-project/locations/us-central1/instances/my-enterprise-filer`
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler",
    comments = "Source: google/cloud/filestore/v1beta1/cloud_filestore_service.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class CloudFilestoreManagerGrpc {

  private CloudFilestoreManagerGrpc() {}

  public static final String SERVICE_NAME = "google.cloud.filestore.v1beta1.CloudFilestoreManager";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<
          com.google.cloud.filestore.v1beta1.ListInstancesRequest,
          com.google.cloud.filestore.v1beta1.ListInstancesResponse>
      getListInstancesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListInstances",
      requestType = com.google.cloud.filestore.v1beta1.ListInstancesRequest.class,
      responseType = com.google.cloud.filestore.v1beta1.ListInstancesResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<
          com.google.cloud.filestore.v1beta1.ListInstancesRequest,
          com.google.cloud.filestore.v1beta1.ListInstancesResponse>
      getListInstancesMethod() {
    io.grpc.MethodDescriptor<
            com.google.cloud.filestore.v1beta1.ListInstancesRequest,
            com.google.cloud.filestore.v1beta1.ListInstancesResponse>
        getListInstancesMethod;
    if ((getListInstancesMethod = CloudFilestoreManagerGrpc.getListInstancesMethod) == null) {
      synchronized (CloudFilestoreManagerGrpc.class) {
        if ((getListInstancesMethod = CloudFilestoreManagerGrpc.getListInstancesMethod) == null) {
          CloudFilestoreManagerGrpc.getListInstancesMethod =
              getListInstancesMethod =
                  io.grpc.MethodDescriptor
                      .<com.google.cloud.filestore.v1beta1.ListInstancesRequest,
                          com.google.cloud.filestore.v1beta1.ListInstancesResponse>
                          newBuilder()
                      .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                      .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListInstances"))
                      .setSampledToLocalTracing(true)
                      .setRequestMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.cloud.filestore.v1beta1.ListInstancesRequest
                                  .getDefaultInstance()))
                      .setResponseMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.cloud.filestore.v1beta1.ListInstancesResponse
                                  .getDefaultInstance()))
                      .setSchemaDescriptor(
                          new CloudFilestoreManagerMethodDescriptorSupplier("ListInstances"))
                      .build();
        }
      }
    }
    return getListInstancesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<
          com.google.cloud.filestore.v1beta1.GetInstanceRequest,
          com.google.cloud.filestore.v1beta1.Instance>
      getGetInstanceMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetInstance",
      requestType = com.google.cloud.filestore.v1beta1.GetInstanceRequest.class,
      responseType = com.google.cloud.filestore.v1beta1.Instance.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<
          com.google.cloud.filestore.v1beta1.GetInstanceRequest,
          com.google.cloud.filestore.v1beta1.Instance>
      getGetInstanceMethod() {
    io.grpc.MethodDescriptor<
            com.google.cloud.filestore.v1beta1.GetInstanceRequest,
            com.google.cloud.filestore.v1beta1.Instance>
        getGetInstanceMethod;
    if ((getGetInstanceMethod = CloudFilestoreManagerGrpc.getGetInstanceMethod) == null) {
      synchronized (CloudFilestoreManagerGrpc.class) {
        if ((getGetInstanceMethod = CloudFilestoreManagerGrpc.getGetInstanceMethod) == null) {
          CloudFilestoreManagerGrpc.getGetInstanceMethod =
              getGetInstanceMethod =
                  io.grpc.MethodDescriptor
                      .<com.google.cloud.filestore.v1beta1.GetInstanceRequest,
                          com.google.cloud.filestore.v1beta1.Instance>
                          newBuilder()
                      .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                      .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetInstance"))
                      .setSampledToLocalTracing(true)
                      .setRequestMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.cloud.filestore.v1beta1.GetInstanceRequest
                                  .getDefaultInstance()))
                      .setResponseMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.cloud.filestore.v1beta1.Instance.getDefaultInstance()))
                      .setSchemaDescriptor(
                          new CloudFilestoreManagerMethodDescriptorSupplier("GetInstance"))
                      .build();
        }
      }
    }
    return getGetInstanceMethod;
  }

  private static volatile io.grpc.MethodDescriptor<
          com.google.cloud.filestore.v1beta1.CreateInstanceRequest,
          com.google.longrunning.Operation>
      getCreateInstanceMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CreateInstance",
      requestType = com.google.cloud.filestore.v1beta1.CreateInstanceRequest.class,
      responseType = com.google.longrunning.Operation.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<
          com.google.cloud.filestore.v1beta1.CreateInstanceRequest,
          com.google.longrunning.Operation>
      getCreateInstanceMethod() {
    io.grpc.MethodDescriptor<
            com.google.cloud.filestore.v1beta1.CreateInstanceRequest,
            com.google.longrunning.Operation>
        getCreateInstanceMethod;
    if ((getCreateInstanceMethod = CloudFilestoreManagerGrpc.getCreateInstanceMethod) == null) {
      synchronized (CloudFilestoreManagerGrpc.class) {
        if ((getCreateInstanceMethod = CloudFilestoreManagerGrpc.getCreateInstanceMethod) == null) {
          CloudFilestoreManagerGrpc.getCreateInstanceMethod =
              getCreateInstanceMethod =
                  io.grpc.MethodDescriptor
                      .<com.google.cloud.filestore.v1beta1.CreateInstanceRequest,
                          com.google.longrunning.Operation>
                          newBuilder()
                      .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                      .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CreateInstance"))
                      .setSampledToLocalTracing(true)
                      .setRequestMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.cloud.filestore.v1beta1.CreateInstanceRequest
                                  .getDefaultInstance()))
                      .setResponseMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.longrunning.Operation.getDefaultInstance()))
                      .setSchemaDescriptor(
                          new CloudFilestoreManagerMethodDescriptorSupplier("CreateInstance"))
                      .build();
        }
      }
    }
    return getCreateInstanceMethod;
  }

  private static volatile io.grpc.MethodDescriptor<
          com.google.cloud.filestore.v1beta1.UpdateInstanceRequest,
          com.google.longrunning.Operation>
      getUpdateInstanceMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "UpdateInstance",
      requestType = com.google.cloud.filestore.v1beta1.UpdateInstanceRequest.class,
      responseType = com.google.longrunning.Operation.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<
          com.google.cloud.filestore.v1beta1.UpdateInstanceRequest,
          com.google.longrunning.Operation>
      getUpdateInstanceMethod() {
    io.grpc.MethodDescriptor<
            com.google.cloud.filestore.v1beta1.UpdateInstanceRequest,
            com.google.longrunning.Operation>
        getUpdateInstanceMethod;
    if ((getUpdateInstanceMethod = CloudFilestoreManagerGrpc.getUpdateInstanceMethod) == null) {
      synchronized (CloudFilestoreManagerGrpc.class) {
        if ((getUpdateInstanceMethod = CloudFilestoreManagerGrpc.getUpdateInstanceMethod) == null) {
          CloudFilestoreManagerGrpc.getUpdateInstanceMethod =
              getUpdateInstanceMethod =
                  io.grpc.MethodDescriptor
                      .<com.google.cloud.filestore.v1beta1.UpdateInstanceRequest,
                          com.google.longrunning.Operation>
                          newBuilder()
                      .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                      .setFullMethodName(generateFullMethodName(SERVICE_NAME, "UpdateInstance"))
                      .setSampledToLocalTracing(true)
                      .setRequestMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.cloud.filestore.v1beta1.UpdateInstanceRequest
                                  .getDefaultInstance()))
                      .setResponseMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.longrunning.Operation.getDefaultInstance()))
                      .setSchemaDescriptor(
                          new CloudFilestoreManagerMethodDescriptorSupplier("UpdateInstance"))
                      .build();
        }
      }
    }
    return getUpdateInstanceMethod;
  }

  private static volatile io.grpc.MethodDescriptor<
          com.google.cloud.filestore.v1beta1.RestoreInstanceRequest,
          com.google.longrunning.Operation>
      getRestoreInstanceMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "RestoreInstance",
      requestType = com.google.cloud.filestore.v1beta1.RestoreInstanceRequest.class,
      responseType = com.google.longrunning.Operation.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<
          com.google.cloud.filestore.v1beta1.RestoreInstanceRequest,
          com.google.longrunning.Operation>
      getRestoreInstanceMethod() {
    io.grpc.MethodDescriptor<
            com.google.cloud.filestore.v1beta1.RestoreInstanceRequest,
            com.google.longrunning.Operation>
        getRestoreInstanceMethod;
    if ((getRestoreInstanceMethod = CloudFilestoreManagerGrpc.getRestoreInstanceMethod) == null) {
      synchronized (CloudFilestoreManagerGrpc.class) {
        if ((getRestoreInstanceMethod = CloudFilestoreManagerGrpc.getRestoreInstanceMethod)
            == null) {
          CloudFilestoreManagerGrpc.getRestoreInstanceMethod =
              getRestoreInstanceMethod =
                  io.grpc.MethodDescriptor
                      .<com.google.cloud.filestore.v1beta1.RestoreInstanceRequest,
                          com.google.longrunning.Operation>
                          newBuilder()
                      .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                      .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RestoreInstance"))
                      .setSampledToLocalTracing(true)
                      .setRequestMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.cloud.filestore.v1beta1.RestoreInstanceRequest
                                  .getDefaultInstance()))
                      .setResponseMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.longrunning.Operation.getDefaultInstance()))
                      .setSchemaDescriptor(
                          new CloudFilestoreManagerMethodDescriptorSupplier("RestoreInstance"))
                      .build();
        }
      }
    }
    return getRestoreInstanceMethod;
  }

  private static volatile io.grpc.MethodDescriptor<
          com.google.cloud.filestore.v1beta1.RevertInstanceRequest,
          com.google.longrunning.Operation>
      getRevertInstanceMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "RevertInstance",
      requestType = com.google.cloud.filestore.v1beta1.RevertInstanceRequest.class,
      responseType = com.google.longrunning.Operation.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<
          com.google.cloud.filestore.v1beta1.RevertInstanceRequest,
          com.google.longrunning.Operation>
      getRevertInstanceMethod() {
    io.grpc.MethodDescriptor<
            com.google.cloud.filestore.v1beta1.RevertInstanceRequest,
            com.google.longrunning.Operation>
        getRevertInstanceMethod;
    if ((getRevertInstanceMethod = CloudFilestoreManagerGrpc.getRevertInstanceMethod) == null) {
      synchronized (CloudFilestoreManagerGrpc.class) {
        if ((getRevertInstanceMethod = CloudFilestoreManagerGrpc.getRevertInstanceMethod) == null) {
          CloudFilestoreManagerGrpc.getRevertInstanceMethod =
              getRevertInstanceMethod =
                  io.grpc.MethodDescriptor
                      .<com.google.cloud.filestore.v1beta1.RevertInstanceRequest,
                          com.google.longrunning.Operation>
                          newBuilder()
                      .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                      .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RevertInstance"))
                      .setSampledToLocalTracing(true)
                      .setRequestMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.cloud.filestore.v1beta1.RevertInstanceRequest
                                  .getDefaultInstance()))
                      .setResponseMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.longrunning.Operation.getDefaultInstance()))
                      .setSchemaDescriptor(
                          new CloudFilestoreManagerMethodDescriptorSupplier("RevertInstance"))
                      .build();
        }
      }
    }
    return getRevertInstanceMethod;
  }

  private static volatile io.grpc.MethodDescriptor<
          com.google.cloud.filestore.v1beta1.DeleteInstanceRequest,
          com.google.longrunning.Operation>
      getDeleteInstanceMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeleteInstance",
      requestType = com.google.cloud.filestore.v1beta1.DeleteInstanceRequest.class,
      responseType = com.google.longrunning.Operation.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<
          com.google.cloud.filestore.v1beta1.DeleteInstanceRequest,
          com.google.longrunning.Operation>
      getDeleteInstanceMethod() {
    io.grpc.MethodDescriptor<
            com.google.cloud.filestore.v1beta1.DeleteInstanceRequest,
            com.google.longrunning.Operation>
        getDeleteInstanceMethod;
    if ((getDeleteInstanceMethod = CloudFilestoreManagerGrpc.getDeleteInstanceMethod) == null) {
      synchronized (CloudFilestoreManagerGrpc.class) {
        if ((getDeleteInstanceMethod = CloudFilestoreManagerGrpc.getDeleteInstanceMethod) == null) {
          CloudFilestoreManagerGrpc.getDeleteInstanceMethod =
              getDeleteInstanceMethod =
                  io.grpc.MethodDescriptor
                      .<com.google.cloud.filestore.v1beta1.DeleteInstanceRequest,
                          com.google.longrunning.Operation>
                          newBuilder()
                      .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                      .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeleteInstance"))
                      .setSampledToLocalTracing(true)
                      .setRequestMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.cloud.filestore.v1beta1.DeleteInstanceRequest
                                  .getDefaultInstance()))
                      .setResponseMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.longrunning.Operation.getDefaultInstance()))
                      .setSchemaDescriptor(
                          new CloudFilestoreManagerMethodDescriptorSupplier("DeleteInstance"))
                      .build();
        }
      }
    }
    return getDeleteInstanceMethod;
  }

  private static volatile io.grpc.MethodDescriptor<
          com.google.cloud.filestore.v1beta1.ListSnapshotsRequest,
          com.google.cloud.filestore.v1beta1.ListSnapshotsResponse>
      getListSnapshotsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListSnapshots",
      requestType = com.google.cloud.filestore.v1beta1.ListSnapshotsRequest.class,
      responseType = com.google.cloud.filestore.v1beta1.ListSnapshotsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<
          com.google.cloud.filestore.v1beta1.ListSnapshotsRequest,
          com.google.cloud.filestore.v1beta1.ListSnapshotsResponse>
      getListSnapshotsMethod() {
    io.grpc.MethodDescriptor<
            com.google.cloud.filestore.v1beta1.ListSnapshotsRequest,
            com.google.cloud.filestore.v1beta1.ListSnapshotsResponse>
        getListSnapshotsMethod;
    if ((getListSnapshotsMethod = CloudFilestoreManagerGrpc.getListSnapshotsMethod) == null) {
      synchronized (CloudFilestoreManagerGrpc.class) {
        if ((getListSnapshotsMethod = CloudFilestoreManagerGrpc.getListSnapshotsMethod) == null) {
          CloudFilestoreManagerGrpc.getListSnapshotsMethod =
              getListSnapshotsMethod =
                  io.grpc.MethodDescriptor
                      .<com.google.cloud.filestore.v1beta1.ListSnapshotsRequest,
                          com.google.cloud.filestore.v1beta1.ListSnapshotsResponse>
                          newBuilder()
                      .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                      .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListSnapshots"))
                      .setSampledToLocalTracing(true)
                      .setRequestMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.cloud.filestore.v1beta1.ListSnapshotsRequest
                                  .getDefaultInstance()))
                      .setResponseMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.cloud.filestore.v1beta1.ListSnapshotsResponse
                                  .getDefaultInstance()))
                      .setSchemaDescriptor(
                          new CloudFilestoreManagerMethodDescriptorSupplier("ListSnapshots"))
                      .build();
        }
      }
    }
    return getListSnapshotsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<
          com.google.cloud.filestore.v1beta1.GetSnapshotRequest,
          com.google.cloud.filestore.v1beta1.Snapshot>
      getGetSnapshotMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetSnapshot",
      requestType = com.google.cloud.filestore.v1beta1.GetSnapshotRequest.class,
      responseType = com.google.cloud.filestore.v1beta1.Snapshot.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<
          com.google.cloud.filestore.v1beta1.GetSnapshotRequest,
          com.google.cloud.filestore.v1beta1.Snapshot>
      getGetSnapshotMethod() {
    io.grpc.MethodDescriptor<
            com.google.cloud.filestore.v1beta1.GetSnapshotRequest,
            com.google.cloud.filestore.v1beta1.Snapshot>
        getGetSnapshotMethod;
    if ((getGetSnapshotMethod = CloudFilestoreManagerGrpc.getGetSnapshotMethod) == null) {
      synchronized (CloudFilestoreManagerGrpc.class) {
        if ((getGetSnapshotMethod = CloudFilestoreManagerGrpc.getGetSnapshotMethod) == null) {
          CloudFilestoreManagerGrpc.getGetSnapshotMethod =
              getGetSnapshotMethod =
                  io.grpc.MethodDescriptor
                      .<com.google.cloud.filestore.v1beta1.GetSnapshotRequest,
                          com.google.cloud.filestore.v1beta1.Snapshot>
                          newBuilder()
                      .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                      .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetSnapshot"))
                      .setSampledToLocalTracing(true)
                      .setRequestMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.cloud.filestore.v1beta1.GetSnapshotRequest
                                  .getDefaultInstance()))
                      .setResponseMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.cloud.filestore.v1beta1.Snapshot.getDefaultInstance()))
                      .setSchemaDescriptor(
                          new CloudFilestoreManagerMethodDescriptorSupplier("GetSnapshot"))
                      .build();
        }
      }
    }
    return getGetSnapshotMethod;
  }

  private static volatile io.grpc.MethodDescriptor<
          com.google.cloud.filestore.v1beta1.CreateSnapshotRequest,
          com.google.longrunning.Operation>
      getCreateSnapshotMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CreateSnapshot",
      requestType = com.google.cloud.filestore.v1beta1.CreateSnapshotRequest.class,
      responseType = com.google.longrunning.Operation.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<
          com.google.cloud.filestore.v1beta1.CreateSnapshotRequest,
          com.google.longrunning.Operation>
      getCreateSnapshotMethod() {
    io.grpc.MethodDescriptor<
            com.google.cloud.filestore.v1beta1.CreateSnapshotRequest,
            com.google.longrunning.Operation>
        getCreateSnapshotMethod;
    if ((getCreateSnapshotMethod = CloudFilestoreManagerGrpc.getCreateSnapshotMethod) == null) {
      synchronized (CloudFilestoreManagerGrpc.class) {
        if ((getCreateSnapshotMethod = CloudFilestoreManagerGrpc.getCreateSnapshotMethod) == null) {
          CloudFilestoreManagerGrpc.getCreateSnapshotMethod =
              getCreateSnapshotMethod =
                  io.grpc.MethodDescriptor
                      .<com.google.cloud.filestore.v1beta1.CreateSnapshotRequest,
                          com.google.longrunning.Operation>
                          newBuilder()
                      .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                      .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CreateSnapshot"))
                      .setSampledToLocalTracing(true)
                      .setRequestMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.cloud.filestore.v1beta1.CreateSnapshotRequest
                                  .getDefaultInstance()))
                      .setResponseMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.longrunning.Operation.getDefaultInstance()))
                      .setSchemaDescriptor(
                          new CloudFilestoreManagerMethodDescriptorSupplier("CreateSnapshot"))
                      .build();
        }
      }
    }
    return getCreateSnapshotMethod;
  }

  private static volatile io.grpc.MethodDescriptor<
          com.google.cloud.filestore.v1beta1.DeleteSnapshotRequest,
          com.google.longrunning.Operation>
      getDeleteSnapshotMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeleteSnapshot",
      requestType = com.google.cloud.filestore.v1beta1.DeleteSnapshotRequest.class,
      responseType = com.google.longrunning.Operation.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<
          com.google.cloud.filestore.v1beta1.DeleteSnapshotRequest,
          com.google.longrunning.Operation>
      getDeleteSnapshotMethod() {
    io.grpc.MethodDescriptor<
            com.google.cloud.filestore.v1beta1.DeleteSnapshotRequest,
            com.google.longrunning.Operation>
        getDeleteSnapshotMethod;
    if ((getDeleteSnapshotMethod = CloudFilestoreManagerGrpc.getDeleteSnapshotMethod) == null) {
      synchronized (CloudFilestoreManagerGrpc.class) {
        if ((getDeleteSnapshotMethod = CloudFilestoreManagerGrpc.getDeleteSnapshotMethod) == null) {
          CloudFilestoreManagerGrpc.getDeleteSnapshotMethod =
              getDeleteSnapshotMethod =
                  io.grpc.MethodDescriptor
                      .<com.google.cloud.filestore.v1beta1.DeleteSnapshotRequest,
                          com.google.longrunning.Operation>
                          newBuilder()
                      .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                      .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeleteSnapshot"))
                      .setSampledToLocalTracing(true)
                      .setRequestMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.cloud.filestore.v1beta1.DeleteSnapshotRequest
                                  .getDefaultInstance()))
                      .setResponseMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.longrunning.Operation.getDefaultInstance()))
                      .setSchemaDescriptor(
                          new CloudFilestoreManagerMethodDescriptorSupplier("DeleteSnapshot"))
                      .build();
        }
      }
    }
    return getDeleteSnapshotMethod;
  }

  private static volatile io.grpc.MethodDescriptor<
          com.google.cloud.filestore.v1beta1.UpdateSnapshotRequest,
          com.google.longrunning.Operation>
      getUpdateSnapshotMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "UpdateSnapshot",
      requestType = com.google.cloud.filestore.v1beta1.UpdateSnapshotRequest.class,
      responseType = com.google.longrunning.Operation.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<
          com.google.cloud.filestore.v1beta1.UpdateSnapshotRequest,
          com.google.longrunning.Operation>
      getUpdateSnapshotMethod() {
    io.grpc.MethodDescriptor<
            com.google.cloud.filestore.v1beta1.UpdateSnapshotRequest,
            com.google.longrunning.Operation>
        getUpdateSnapshotMethod;
    if ((getUpdateSnapshotMethod = CloudFilestoreManagerGrpc.getUpdateSnapshotMethod) == null) {
      synchronized (CloudFilestoreManagerGrpc.class) {
        if ((getUpdateSnapshotMethod = CloudFilestoreManagerGrpc.getUpdateSnapshotMethod) == null) {
          CloudFilestoreManagerGrpc.getUpdateSnapshotMethod =
              getUpdateSnapshotMethod =
                  io.grpc.MethodDescriptor
                      .<com.google.cloud.filestore.v1beta1.UpdateSnapshotRequest,
                          com.google.longrunning.Operation>
                          newBuilder()
                      .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                      .setFullMethodName(generateFullMethodName(SERVICE_NAME, "UpdateSnapshot"))
                      .setSampledToLocalTracing(true)
                      .setRequestMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.cloud.filestore.v1beta1.UpdateSnapshotRequest
                                  .getDefaultInstance()))
                      .setResponseMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.longrunning.Operation.getDefaultInstance()))
                      .setSchemaDescriptor(
                          new CloudFilestoreManagerMethodDescriptorSupplier("UpdateSnapshot"))
                      .build();
        }
      }
    }
    return getUpdateSnapshotMethod;
  }

  private static volatile io.grpc.MethodDescriptor<
          com.google.cloud.filestore.v1beta1.ListBackupsRequest,
          com.google.cloud.filestore.v1beta1.ListBackupsResponse>
      getListBackupsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListBackups",
      requestType = com.google.cloud.filestore.v1beta1.ListBackupsRequest.class,
      responseType = com.google.cloud.filestore.v1beta1.ListBackupsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<
          com.google.cloud.filestore.v1beta1.ListBackupsRequest,
          com.google.cloud.filestore.v1beta1.ListBackupsResponse>
      getListBackupsMethod() {
    io.grpc.MethodDescriptor<
            com.google.cloud.filestore.v1beta1.ListBackupsRequest,
            com.google.cloud.filestore.v1beta1.ListBackupsResponse>
        getListBackupsMethod;
    if ((getListBackupsMethod = CloudFilestoreManagerGrpc.getListBackupsMethod) == null) {
      synchronized (CloudFilestoreManagerGrpc.class) {
        if ((getListBackupsMethod = CloudFilestoreManagerGrpc.getListBackupsMethod) == null) {
          CloudFilestoreManagerGrpc.getListBackupsMethod =
              getListBackupsMethod =
                  io.grpc.MethodDescriptor
                      .<com.google.cloud.filestore.v1beta1.ListBackupsRequest,
                          com.google.cloud.filestore.v1beta1.ListBackupsResponse>
                          newBuilder()
                      .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                      .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListBackups"))
                      .setSampledToLocalTracing(true)
                      .setRequestMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.cloud.filestore.v1beta1.ListBackupsRequest
                                  .getDefaultInstance()))
                      .setResponseMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.cloud.filestore.v1beta1.ListBackupsResponse
                                  .getDefaultInstance()))
                      .setSchemaDescriptor(
                          new CloudFilestoreManagerMethodDescriptorSupplier("ListBackups"))
                      .build();
        }
      }
    }
    return getListBackupsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<
          com.google.cloud.filestore.v1beta1.GetBackupRequest,
          com.google.cloud.filestore.v1beta1.Backup>
      getGetBackupMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetBackup",
      requestType = com.google.cloud.filestore.v1beta1.GetBackupRequest.class,
      responseType = com.google.cloud.filestore.v1beta1.Backup.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<
          com.google.cloud.filestore.v1beta1.GetBackupRequest,
          com.google.cloud.filestore.v1beta1.Backup>
      getGetBackupMethod() {
    io.grpc.MethodDescriptor<
            com.google.cloud.filestore.v1beta1.GetBackupRequest,
            com.google.cloud.filestore.v1beta1.Backup>
        getGetBackupMethod;
    if ((getGetBackupMethod = CloudFilestoreManagerGrpc.getGetBackupMethod) == null) {
      synchronized (CloudFilestoreManagerGrpc.class) {
        if ((getGetBackupMethod = CloudFilestoreManagerGrpc.getGetBackupMethod) == null) {
          CloudFilestoreManagerGrpc.getGetBackupMethod =
              getGetBackupMethod =
                  io.grpc.MethodDescriptor
                      .<com.google.cloud.filestore.v1beta1.GetBackupRequest,
                          com.google.cloud.filestore.v1beta1.Backup>
                          newBuilder()
                      .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                      .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetBackup"))
                      .setSampledToLocalTracing(true)
                      .setRequestMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.cloud.filestore.v1beta1.GetBackupRequest
                                  .getDefaultInstance()))
                      .setResponseMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.cloud.filestore.v1beta1.Backup.getDefaultInstance()))
                      .setSchemaDescriptor(
                          new CloudFilestoreManagerMethodDescriptorSupplier("GetBackup"))
                      .build();
        }
      }
    }
    return getGetBackupMethod;
  }

  private static volatile io.grpc.MethodDescriptor<
          com.google.cloud.filestore.v1beta1.CreateBackupRequest, com.google.longrunning.Operation>
      getCreateBackupMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CreateBackup",
      requestType = com.google.cloud.filestore.v1beta1.CreateBackupRequest.class,
      responseType = com.google.longrunning.Operation.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<
          com.google.cloud.filestore.v1beta1.CreateBackupRequest, com.google.longrunning.Operation>
      getCreateBackupMethod() {
    io.grpc.MethodDescriptor<
            com.google.cloud.filestore.v1beta1.CreateBackupRequest,
            com.google.longrunning.Operation>
        getCreateBackupMethod;
    if ((getCreateBackupMethod = CloudFilestoreManagerGrpc.getCreateBackupMethod) == null) {
      synchronized (CloudFilestoreManagerGrpc.class) {
        if ((getCreateBackupMethod = CloudFilestoreManagerGrpc.getCreateBackupMethod) == null) {
          CloudFilestoreManagerGrpc.getCreateBackupMethod =
              getCreateBackupMethod =
                  io.grpc.MethodDescriptor
                      .<com.google.cloud.filestore.v1beta1.CreateBackupRequest,
                          com.google.longrunning.Operation>
                          newBuilder()
                      .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                      .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CreateBackup"))
                      .setSampledToLocalTracing(true)
                      .setRequestMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.cloud.filestore.v1beta1.CreateBackupRequest
                                  .getDefaultInstance()))
                      .setResponseMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.longrunning.Operation.getDefaultInstance()))
                      .setSchemaDescriptor(
                          new CloudFilestoreManagerMethodDescriptorSupplier("CreateBackup"))
                      .build();
        }
      }
    }
    return getCreateBackupMethod;
  }

  private static volatile io.grpc.MethodDescriptor<
          com.google.cloud.filestore.v1beta1.DeleteBackupRequest, com.google.longrunning.Operation>
      getDeleteBackupMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeleteBackup",
      requestType = com.google.cloud.filestore.v1beta1.DeleteBackupRequest.class,
      responseType = com.google.longrunning.Operation.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<
          com.google.cloud.filestore.v1beta1.DeleteBackupRequest, com.google.longrunning.Operation>
      getDeleteBackupMethod() {
    io.grpc.MethodDescriptor<
            com.google.cloud.filestore.v1beta1.DeleteBackupRequest,
            com.google.longrunning.Operation>
        getDeleteBackupMethod;
    if ((getDeleteBackupMethod = CloudFilestoreManagerGrpc.getDeleteBackupMethod) == null) {
      synchronized (CloudFilestoreManagerGrpc.class) {
        if ((getDeleteBackupMethod = CloudFilestoreManagerGrpc.getDeleteBackupMethod) == null) {
          CloudFilestoreManagerGrpc.getDeleteBackupMethod =
              getDeleteBackupMethod =
                  io.grpc.MethodDescriptor
                      .<com.google.cloud.filestore.v1beta1.DeleteBackupRequest,
                          com.google.longrunning.Operation>
                          newBuilder()
                      .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                      .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeleteBackup"))
                      .setSampledToLocalTracing(true)
                      .setRequestMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.cloud.filestore.v1beta1.DeleteBackupRequest
                                  .getDefaultInstance()))
                      .setResponseMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.longrunning.Operation.getDefaultInstance()))
                      .setSchemaDescriptor(
                          new CloudFilestoreManagerMethodDescriptorSupplier("DeleteBackup"))
                      .build();
        }
      }
    }
    return getDeleteBackupMethod;
  }

  private static volatile io.grpc.MethodDescriptor<
          com.google.cloud.filestore.v1beta1.UpdateBackupRequest, com.google.longrunning.Operation>
      getUpdateBackupMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "UpdateBackup",
      requestType = com.google.cloud.filestore.v1beta1.UpdateBackupRequest.class,
      responseType = com.google.longrunning.Operation.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<
          com.google.cloud.filestore.v1beta1.UpdateBackupRequest, com.google.longrunning.Operation>
      getUpdateBackupMethod() {
    io.grpc.MethodDescriptor<
            com.google.cloud.filestore.v1beta1.UpdateBackupRequest,
            com.google.longrunning.Operation>
        getUpdateBackupMethod;
    if ((getUpdateBackupMethod = CloudFilestoreManagerGrpc.getUpdateBackupMethod) == null) {
      synchronized (CloudFilestoreManagerGrpc.class) {
        if ((getUpdateBackupMethod = CloudFilestoreManagerGrpc.getUpdateBackupMethod) == null) {
          CloudFilestoreManagerGrpc.getUpdateBackupMethod =
              getUpdateBackupMethod =
                  io.grpc.MethodDescriptor
                      .<com.google.cloud.filestore.v1beta1.UpdateBackupRequest,
                          com.google.longrunning.Operation>
                          newBuilder()
                      .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                      .setFullMethodName(generateFullMethodName(SERVICE_NAME, "UpdateBackup"))
                      .setSampledToLocalTracing(true)
                      .setRequestMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.cloud.filestore.v1beta1.UpdateBackupRequest
                                  .getDefaultInstance()))
                      .setResponseMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.longrunning.Operation.getDefaultInstance()))
                      .setSchemaDescriptor(
                          new CloudFilestoreManagerMethodDescriptorSupplier("UpdateBackup"))
                      .build();
        }
      }
    }
    return getUpdateBackupMethod;
  }

  private static volatile io.grpc.MethodDescriptor<
          com.google.cloud.filestore.v1beta1.ListSharesRequest,
          com.google.cloud.filestore.v1beta1.ListSharesResponse>
      getListSharesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListShares",
      requestType = com.google.cloud.filestore.v1beta1.ListSharesRequest.class,
      responseType = com.google.cloud.filestore.v1beta1.ListSharesResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<
          com.google.cloud.filestore.v1beta1.ListSharesRequest,
          com.google.cloud.filestore.v1beta1.ListSharesResponse>
      getListSharesMethod() {
    io.grpc.MethodDescriptor<
            com.google.cloud.filestore.v1beta1.ListSharesRequest,
            com.google.cloud.filestore.v1beta1.ListSharesResponse>
        getListSharesMethod;
    if ((getListSharesMethod = CloudFilestoreManagerGrpc.getListSharesMethod) == null) {
      synchronized (CloudFilestoreManagerGrpc.class) {
        if ((getListSharesMethod = CloudFilestoreManagerGrpc.getListSharesMethod) == null) {
          CloudFilestoreManagerGrpc.getListSharesMethod =
              getListSharesMethod =
                  io.grpc.MethodDescriptor
                      .<com.google.cloud.filestore.v1beta1.ListSharesRequest,
                          com.google.cloud.filestore.v1beta1.ListSharesResponse>
                          newBuilder()
                      .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                      .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListShares"))
                      .setSampledToLocalTracing(true)
                      .setRequestMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.cloud.filestore.v1beta1.ListSharesRequest
                                  .getDefaultInstance()))
                      .setResponseMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.cloud.filestore.v1beta1.ListSharesResponse
                                  .getDefaultInstance()))
                      .setSchemaDescriptor(
                          new CloudFilestoreManagerMethodDescriptorSupplier("ListShares"))
                      .build();
        }
      }
    }
    return getListSharesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<
          com.google.cloud.filestore.v1beta1.GetShareRequest,
          com.google.cloud.filestore.v1beta1.Share>
      getGetShareMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetShare",
      requestType = com.google.cloud.filestore.v1beta1.GetShareRequest.class,
      responseType = com.google.cloud.filestore.v1beta1.Share.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<
          com.google.cloud.filestore.v1beta1.GetShareRequest,
          com.google.cloud.filestore.v1beta1.Share>
      getGetShareMethod() {
    io.grpc.MethodDescriptor<
            com.google.cloud.filestore.v1beta1.GetShareRequest,
            com.google.cloud.filestore.v1beta1.Share>
        getGetShareMethod;
    if ((getGetShareMethod = CloudFilestoreManagerGrpc.getGetShareMethod) == null) {
      synchronized (CloudFilestoreManagerGrpc.class) {
        if ((getGetShareMethod = CloudFilestoreManagerGrpc.getGetShareMethod) == null) {
          CloudFilestoreManagerGrpc.getGetShareMethod =
              getGetShareMethod =
                  io.grpc.MethodDescriptor
                      .<com.google.cloud.filestore.v1beta1.GetShareRequest,
                          com.google.cloud.filestore.v1beta1.Share>
                          newBuilder()
                      .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                      .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetShare"))
                      .setSampledToLocalTracing(true)
                      .setRequestMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.cloud.filestore.v1beta1.GetShareRequest
                                  .getDefaultInstance()))
                      .setResponseMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.cloud.filestore.v1beta1.Share.getDefaultInstance()))
                      .setSchemaDescriptor(
                          new CloudFilestoreManagerMethodDescriptorSupplier("GetShare"))
                      .build();
        }
      }
    }
    return getGetShareMethod;
  }

  private static volatile io.grpc.MethodDescriptor<
          com.google.cloud.filestore.v1beta1.CreateShareRequest, com.google.longrunning.Operation>
      getCreateShareMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CreateShare",
      requestType = com.google.cloud.filestore.v1beta1.CreateShareRequest.class,
      responseType = com.google.longrunning.Operation.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<
          com.google.cloud.filestore.v1beta1.CreateShareRequest, com.google.longrunning.Operation>
      getCreateShareMethod() {
    io.grpc.MethodDescriptor<
            com.google.cloud.filestore.v1beta1.CreateShareRequest, com.google.longrunning.Operation>
        getCreateShareMethod;
    if ((getCreateShareMethod = CloudFilestoreManagerGrpc.getCreateShareMethod) == null) {
      synchronized (CloudFilestoreManagerGrpc.class) {
        if ((getCreateShareMethod = CloudFilestoreManagerGrpc.getCreateShareMethod) == null) {
          CloudFilestoreManagerGrpc.getCreateShareMethod =
              getCreateShareMethod =
                  io.grpc.MethodDescriptor
                      .<com.google.cloud.filestore.v1beta1.CreateShareRequest,
                          com.google.longrunning.Operation>
                          newBuilder()
                      .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                      .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CreateShare"))
                      .setSampledToLocalTracing(true)
                      .setRequestMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.cloud.filestore.v1beta1.CreateShareRequest
                                  .getDefaultInstance()))
                      .setResponseMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.longrunning.Operation.getDefaultInstance()))
                      .setSchemaDescriptor(
                          new CloudFilestoreManagerMethodDescriptorSupplier("CreateShare"))
                      .build();
        }
      }
    }
    return getCreateShareMethod;
  }

  private static volatile io.grpc.MethodDescriptor<
          com.google.cloud.filestore.v1beta1.DeleteShareRequest, com.google.longrunning.Operation>
      getDeleteShareMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeleteShare",
      requestType = com.google.cloud.filestore.v1beta1.DeleteShareRequest.class,
      responseType = com.google.longrunning.Operation.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<
          com.google.cloud.filestore.v1beta1.DeleteShareRequest, com.google.longrunning.Operation>
      getDeleteShareMethod() {
    io.grpc.MethodDescriptor<
            com.google.cloud.filestore.v1beta1.DeleteShareRequest, com.google.longrunning.Operation>
        getDeleteShareMethod;
    if ((getDeleteShareMethod = CloudFilestoreManagerGrpc.getDeleteShareMethod) == null) {
      synchronized (CloudFilestoreManagerGrpc.class) {
        if ((getDeleteShareMethod = CloudFilestoreManagerGrpc.getDeleteShareMethod) == null) {
          CloudFilestoreManagerGrpc.getDeleteShareMethod =
              getDeleteShareMethod =
                  io.grpc.MethodDescriptor
                      .<com.google.cloud.filestore.v1beta1.DeleteShareRequest,
                          com.google.longrunning.Operation>
                          newBuilder()
                      .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                      .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeleteShare"))
                      .setSampledToLocalTracing(true)
                      .setRequestMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.cloud.filestore.v1beta1.DeleteShareRequest
                                  .getDefaultInstance()))
                      .setResponseMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.longrunning.Operation.getDefaultInstance()))
                      .setSchemaDescriptor(
                          new CloudFilestoreManagerMethodDescriptorSupplier("DeleteShare"))
                      .build();
        }
      }
    }
    return getDeleteShareMethod;
  }

  private static volatile io.grpc.MethodDescriptor<
          com.google.cloud.filestore.v1beta1.UpdateShareRequest, com.google.longrunning.Operation>
      getUpdateShareMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "UpdateShare",
      requestType = com.google.cloud.filestore.v1beta1.UpdateShareRequest.class,
      responseType = com.google.longrunning.Operation.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<
          com.google.cloud.filestore.v1beta1.UpdateShareRequest, com.google.longrunning.Operation>
      getUpdateShareMethod() {
    io.grpc.MethodDescriptor<
            com.google.cloud.filestore.v1beta1.UpdateShareRequest, com.google.longrunning.Operation>
        getUpdateShareMethod;
    if ((getUpdateShareMethod = CloudFilestoreManagerGrpc.getUpdateShareMethod) == null) {
      synchronized (CloudFilestoreManagerGrpc.class) {
        if ((getUpdateShareMethod = CloudFilestoreManagerGrpc.getUpdateShareMethod) == null) {
          CloudFilestoreManagerGrpc.getUpdateShareMethod =
              getUpdateShareMethod =
                  io.grpc.MethodDescriptor
                      .<com.google.cloud.filestore.v1beta1.UpdateShareRequest,
                          com.google.longrunning.Operation>
                          newBuilder()
                      .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                      .setFullMethodName(generateFullMethodName(SERVICE_NAME, "UpdateShare"))
                      .setSampledToLocalTracing(true)
                      .setRequestMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.cloud.filestore.v1beta1.UpdateShareRequest
                                  .getDefaultInstance()))
                      .setResponseMarshaller(
                          io.grpc.protobuf.ProtoUtils.marshaller(
                              com.google.longrunning.Operation.getDefaultInstance()))
                      .setSchemaDescriptor(
                          new CloudFilestoreManagerMethodDescriptorSupplier("UpdateShare"))
                      .build();
        }
      }
    }
    return getUpdateShareMethod;
  }

  /** Creates a new async stub that supports all call types for the service */
  public static CloudFilestoreManagerStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<CloudFilestoreManagerStub> factory =
        new io.grpc.stub.AbstractStub.StubFactory<CloudFilestoreManagerStub>() {
          @java.lang.Override
          public CloudFilestoreManagerStub newStub(
              io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            return new CloudFilestoreManagerStub(channel, callOptions);
          }
        };
    return CloudFilestoreManagerStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static CloudFilestoreManagerBlockingStub newBlockingStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<CloudFilestoreManagerBlockingStub> factory =
        new io.grpc.stub.AbstractStub.StubFactory<CloudFilestoreManagerBlockingStub>() {
          @java.lang.Override
          public CloudFilestoreManagerBlockingStub newStub(
              io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            return new CloudFilestoreManagerBlockingStub(channel, callOptions);
          }
        };
    return CloudFilestoreManagerBlockingStub.newStub(factory, channel);
  }

  /** Creates a new ListenableFuture-style stub that supports unary calls on the service */
  public static CloudFilestoreManagerFutureStub newFutureStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<CloudFilestoreManagerFutureStub> factory =
        new io.grpc.stub.AbstractStub.StubFactory<CloudFilestoreManagerFutureStub>() {
          @java.lang.Override
          public CloudFilestoreManagerFutureStub newStub(
              io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            return new CloudFilestoreManagerFutureStub(channel, callOptions);
          }
        };
    return CloudFilestoreManagerFutureStub.newStub(factory, channel);
  }

  /**
   *
   *
   * <pre>
   * Configures and manages Filestore resources.
   * Filestore Manager v1beta1.
   * The `file.googleapis.com` service implements the Filestore API and
   * defines the following model for managing resources:
   * * The service works with a collection of cloud projects, named: `/projects/&#42;`
   * * Each project has a collection of available locations, named: `/locations/&#42;`
   * * Each location has a collection of instances and backups, named:
   * `/instances/&#42;` and `/backups/&#42;` respectively.
   * * As such, Filestore instances are resources of the form:
   *   `/projects/{project_id}/locations/{location_id}/instances/{instance_id}`
   *   backups are resources of the form:
   *   `/projects/{project_id}/locations/{location_id}/backup/{backup_id}`
   * Note that location_id can represent a GCP `zone` or `region` depending on the
   * resource.
   * for example:
   * A zonal Filestore instance:
   * * `projects/my-project/locations/us-central1-c/instances/my-basic-tier-filer`
   * A regional Filestore instance:
   * * `projects/my-project/locations/us-central1/instances/my-enterprise-filer`
   * </pre>
   */
  public abstract static class CloudFilestoreManagerImplBase implements io.grpc.BindableService {

    /**
     *
     *
     * <pre>
     * Lists all instances in a project for either a specified location
     * or for all locations.
     * </pre>
     */
    public void listInstances(
        com.google.cloud.filestore.v1beta1.ListInstancesRequest request,
        io.grpc.stub.StreamObserver<com.google.cloud.filestore.v1beta1.ListInstancesResponse>
            responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(
          getListInstancesMethod(), responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Gets the details of a specific instance.
     * </pre>
     */
    public void getInstance(
        com.google.cloud.filestore.v1beta1.GetInstanceRequest request,
        io.grpc.stub.StreamObserver<com.google.cloud.filestore.v1beta1.Instance> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(
          getGetInstanceMethod(), responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Creates an instance.
     * When creating from a backup, the capacity of the new instance needs to be
     * equal to or larger than the capacity of the backup (and also equal to or
     * larger than the minimum capacity of the tier).
     * </pre>
     */
    public void createInstance(
        com.google.cloud.filestore.v1beta1.CreateInstanceRequest request,
        io.grpc.stub.StreamObserver<com.google.longrunning.Operation> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(
          getCreateInstanceMethod(), responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Updates the settings of a specific instance.
     * </pre>
     */
    public void updateInstance(
        com.google.cloud.filestore.v1beta1.UpdateInstanceRequest request,
        io.grpc.stub.StreamObserver<com.google.longrunning.Operation> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(
          getUpdateInstanceMethod(), responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Restores an existing instance's file share from a backup.
     * The capacity of the instance needs to be equal to or larger than the
     * capacity of the backup (and also equal to or larger than the minimum
     * capacity of the tier).
     * </pre>
     */
    public void restoreInstance(
        com.google.cloud.filestore.v1beta1.RestoreInstanceRequest request,
        io.grpc.stub.StreamObserver<com.google.longrunning.Operation> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(
          getRestoreInstanceMethod(), responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Revert an existing instance's file system to a specified snapshot.
     * </pre>
     */
    public void revertInstance(
        com.google.cloud.filestore.v1beta1.RevertInstanceRequest request,
        io.grpc.stub.StreamObserver<com.google.longrunning.Operation> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(
          getRevertInstanceMethod(), responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Deletes an instance.
     * </pre>
     */
    public void deleteInstance(
        com.google.cloud.filestore.v1beta1.DeleteInstanceRequest request,
        io.grpc.stub.StreamObserver<com.google.longrunning.Operation> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(
          getDeleteInstanceMethod(), responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Lists all snapshots in a project for either a specified location
     * or for all locations.
     * </pre>
     */
    public void listSnapshots(
        com.google.cloud.filestore.v1beta1.ListSnapshotsRequest request,
        io.grpc.stub.StreamObserver<com.google.cloud.filestore.v1beta1.ListSnapshotsResponse>
            responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(
          getListSnapshotsMethod(), responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Gets the details of a specific snapshot.
     * </pre>
     */
    public void getSnapshot(
        com.google.cloud.filestore.v1beta1.GetSnapshotRequest request,
        io.grpc.stub.StreamObserver<com.google.cloud.filestore.v1beta1.Snapshot> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(
          getGetSnapshotMethod(), responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Creates a snapshot.
     * </pre>
     */
    public void createSnapshot(
        com.google.cloud.filestore.v1beta1.CreateSnapshotRequest request,
        io.grpc.stub.StreamObserver<com.google.longrunning.Operation> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(
          getCreateSnapshotMethod(), responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Deletes a snapshot.
     * </pre>
     */
    public void deleteSnapshot(
        com.google.cloud.filestore.v1beta1.DeleteSnapshotRequest request,
        io.grpc.stub.StreamObserver<com.google.longrunning.Operation> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(
          getDeleteSnapshotMethod(), responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Updates the settings of a specific snapshot.
     * </pre>
     */
    public void updateSnapshot(
        com.google.cloud.filestore.v1beta1.UpdateSnapshotRequest request,
        io.grpc.stub.StreamObserver<com.google.longrunning.Operation> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(
          getUpdateSnapshotMethod(), responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Lists all backups in a project for either a specified location or for all
     * locations.
     * </pre>
     */
    public void listBackups(
        com.google.cloud.filestore.v1beta1.ListBackupsRequest request,
        io.grpc.stub.StreamObserver<com.google.cloud.filestore.v1beta1.ListBackupsResponse>
            responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(
          getListBackupsMethod(), responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Gets the details of a specific backup.
     * </pre>
     */
    public void getBackup(
        com.google.cloud.filestore.v1beta1.GetBackupRequest request,
        io.grpc.stub.StreamObserver<com.google.cloud.filestore.v1beta1.Backup> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetBackupMethod(), responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Creates a backup.
     * </pre>
     */
    public void createBackup(
        com.google.cloud.filestore.v1beta1.CreateBackupRequest request,
        io.grpc.stub.StreamObserver<com.google.longrunning.Operation> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(
          getCreateBackupMethod(), responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Deletes a backup.
     * </pre>
     */
    public void deleteBackup(
        com.google.cloud.filestore.v1beta1.DeleteBackupRequest request,
        io.grpc.stub.StreamObserver<com.google.longrunning.Operation> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(
          getDeleteBackupMethod(), responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Updates the settings of a specific backup.
     * </pre>
     */
    public void updateBackup(
        com.google.cloud.filestore.v1beta1.UpdateBackupRequest request,
        io.grpc.stub.StreamObserver<com.google.longrunning.Operation> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(
          getUpdateBackupMethod(), responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Lists all shares for a specified instance.
     * </pre>
     */
    public void listShares(
        com.google.cloud.filestore.v1beta1.ListSharesRequest request,
        io.grpc.stub.StreamObserver<com.google.cloud.filestore.v1beta1.ListSharesResponse>
            responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListSharesMethod(), responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Gets the details of a specific share.
     * </pre>
     */
    public void getShare(
        com.google.cloud.filestore.v1beta1.GetShareRequest request,
        io.grpc.stub.StreamObserver<com.google.cloud.filestore.v1beta1.Share> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetShareMethod(), responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Creates a share.
     * </pre>
     */
    public void createShare(
        com.google.cloud.filestore.v1beta1.CreateShareRequest request,
        io.grpc.stub.StreamObserver<com.google.longrunning.Operation> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(
          getCreateShareMethod(), responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Deletes a share.
     * </pre>
     */
    public void deleteShare(
        com.google.cloud.filestore.v1beta1.DeleteShareRequest request,
        io.grpc.stub.StreamObserver<com.google.longrunning.Operation> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(
          getDeleteShareMethod(), responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Updates the settings of a specific share.
     * </pre>
     */
    public void updateShare(
        com.google.cloud.filestore.v1beta1.UpdateShareRequest request,
        io.grpc.stub.StreamObserver<com.google.longrunning.Operation> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(
          getUpdateShareMethod(), responseObserver);
    }

    @java.lang.Override
    public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
              getListInstancesMethod(),
              io.grpc.stub.ServerCalls.asyncUnaryCall(
                  new MethodHandlers<
                      com.google.cloud.filestore.v1beta1.ListInstancesRequest,
                      com.google.cloud.filestore.v1beta1.ListInstancesResponse>(
                      this, METHODID_LIST_INSTANCES)))
          .addMethod(
              getGetInstanceMethod(),
              io.grpc.stub.ServerCalls.asyncUnaryCall(
                  new MethodHandlers<
                      com.google.cloud.filestore.v1beta1.GetInstanceRequest,
                      com.google.cloud.filestore.v1beta1.Instance>(this, METHODID_GET_INSTANCE)))
          .addMethod(
              getCreateInstanceMethod(),
              io.grpc.stub.ServerCalls.asyncUnaryCall(
                  new MethodHandlers<
                      com.google.cloud.filestore.v1beta1.CreateInstanceRequest,
                      com.google.longrunning.Operation>(this, METHODID_CREATE_INSTANCE)))
          .addMethod(
              getUpdateInstanceMethod(),
              io.grpc.stub.ServerCalls.asyncUnaryCall(
                  new MethodHandlers<
                      com.google.cloud.filestore.v1beta1.UpdateInstanceRequest,
                      com.google.longrunning.Operation>(this, METHODID_UPDATE_INSTANCE)))
          .addMethod(
              getRestoreInstanceMethod(),
              io.grpc.stub.ServerCalls.asyncUnaryCall(
                  new MethodHandlers<
                      com.google.cloud.filestore.v1beta1.RestoreInstanceRequest,
                      com.google.longrunning.Operation>(this, METHODID_RESTORE_INSTANCE)))
          .addMethod(
              getRevertInstanceMethod(),
              io.grpc.stub.ServerCalls.asyncUnaryCall(
                  new MethodHandlers<
                      com.google.cloud.filestore.v1beta1.RevertInstanceRequest,
                      com.google.longrunning.Operation>(this, METHODID_REVERT_INSTANCE)))
          .addMethod(
              getDeleteInstanceMethod(),
              io.grpc.stub.ServerCalls.asyncUnaryCall(
                  new MethodHandlers<
                      com.google.cloud.filestore.v1beta1.DeleteInstanceRequest,
                      com.google.longrunning.Operation>(this, METHODID_DELETE_INSTANCE)))
          .addMethod(
              getListSnapshotsMethod(),
              io.grpc.stub.ServerCalls.asyncUnaryCall(
                  new MethodHandlers<
                      com.google.cloud.filestore.v1beta1.ListSnapshotsRequest,
                      com.google.cloud.filestore.v1beta1.ListSnapshotsResponse>(
                      this, METHODID_LIST_SNAPSHOTS)))
          .addMethod(
              getGetSnapshotMethod(),
              io.grpc.stub.ServerCalls.asyncUnaryCall(
                  new MethodHandlers<
                      com.google.cloud.filestore.v1beta1.GetSnapshotRequest,
                      com.google.cloud.filestore.v1beta1.Snapshot>(this, METHODID_GET_SNAPSHOT)))
          .addMethod(
              getCreateSnapshotMethod(),
              io.grpc.stub.ServerCalls.asyncUnaryCall(
                  new MethodHandlers<
                      com.google.cloud.filestore.v1beta1.CreateSnapshotRequest,
                      com.google.longrunning.Operation>(this, METHODID_CREATE_SNAPSHOT)))
          .addMethod(
              getDeleteSnapshotMethod(),
              io.grpc.stub.ServerCalls.asyncUnaryCall(
                  new MethodHandlers<
                      com.google.cloud.filestore.v1beta1.DeleteSnapshotRequest,
                      com.google.longrunning.Operation>(this, METHODID_DELETE_SNAPSHOT)))
          .addMethod(
              getUpdateSnapshotMethod(),
              io.grpc.stub.ServerCalls.asyncUnaryCall(
                  new MethodHandlers<
                      com.google.cloud.filestore.v1beta1.UpdateSnapshotRequest,
                      com.google.longrunning.Operation>(this, METHODID_UPDATE_SNAPSHOT)))
          .addMethod(
              getListBackupsMethod(),
              io.grpc.stub.ServerCalls.asyncUnaryCall(
                  new MethodHandlers<
                      com.google.cloud.filestore.v1beta1.ListBackupsRequest,
                      com.google.cloud.filestore.v1beta1.ListBackupsResponse>(
                      this, METHODID_LIST_BACKUPS)))
          .addMethod(
              getGetBackupMethod(),
              io.grpc.stub.ServerCalls.asyncUnaryCall(
                  new MethodHandlers<
                      com.google.cloud.filestore.v1beta1.GetBackupRequest,
                      com.google.cloud.filestore.v1beta1.Backup>(this, METHODID_GET_BACKUP)))
          .addMethod(
              getCreateBackupMethod(),
              io.grpc.stub.ServerCalls.asyncUnaryCall(
                  new MethodHandlers<
                      com.google.cloud.filestore.v1beta1.CreateBackupRequest,
                      com.google.longrunning.Operation>(this, METHODID_CREATE_BACKUP)))
          .addMethod(
              getDeleteBackupMethod(),
              io.grpc.stub.ServerCalls.asyncUnaryCall(
                  new MethodHandlers<
                      com.google.cloud.filestore.v1beta1.DeleteBackupRequest,
                      com.google.longrunning.Operation>(this, METHODID_DELETE_BACKUP)))
          .addMethod(
              getUpdateBackupMethod(),
              io.grpc.stub.ServerCalls.asyncUnaryCall(
                  new MethodHandlers<
                      com.google.cloud.filestore.v1beta1.UpdateBackupRequest,
                      com.google.longrunning.Operation>(this, METHODID_UPDATE_BACKUP)))
          .addMethod(
              getListSharesMethod(),
              io.grpc.stub.ServerCalls.asyncUnaryCall(
                  new MethodHandlers<
                      com.google.cloud.filestore.v1beta1.ListSharesRequest,
                      com.google.cloud.filestore.v1beta1.ListSharesResponse>(
                      this, METHODID_LIST_SHARES)))
          .addMethod(
              getGetShareMethod(),
              io.grpc.stub.ServerCalls.asyncUnaryCall(
                  new MethodHandlers<
                      com.google.cloud.filestore.v1beta1.GetShareRequest,
                      com.google.cloud.filestore.v1beta1.Share>(this, METHODID_GET_SHARE)))
          .addMethod(
              getCreateShareMethod(),
              io.grpc.stub.ServerCalls.asyncUnaryCall(
                  new MethodHandlers<
                      com.google.cloud.filestore.v1beta1.CreateShareRequest,
                      com.google.longrunning.Operation>(this, METHODID_CREATE_SHARE)))
          .addMethod(
              getDeleteShareMethod(),
              io.grpc.stub.ServerCalls.asyncUnaryCall(
                  new MethodHandlers<
                      com.google.cloud.filestore.v1beta1.DeleteShareRequest,
                      com.google.longrunning.Operation>(this, METHODID_DELETE_SHARE)))
          .addMethod(
              getUpdateShareMethod(),
              io.grpc.stub.ServerCalls.asyncUnaryCall(
                  new MethodHandlers<
                      com.google.cloud.filestore.v1beta1.UpdateShareRequest,
                      com.google.longrunning.Operation>(this, METHODID_UPDATE_SHARE)))
          .build();
    }
  }

  /**
   *
   *
   * <pre>
   * Configures and manages Filestore resources.
   * Filestore Manager v1beta1.
   * The `file.googleapis.com` service implements the Filestore API and
   * defines the following model for managing resources:
   * * The service works with a collection of cloud projects, named: `/projects/&#42;`
   * * Each project has a collection of available locations, named: `/locations/&#42;`
   * * Each location has a collection of instances and backups, named:
   * `/instances/&#42;` and `/backups/&#42;` respectively.
   * * As such, Filestore instances are resources of the form:
   *   `/projects/{project_id}/locations/{location_id}/instances/{instance_id}`
   *   backups are resources of the form:
   *   `/projects/{project_id}/locations/{location_id}/backup/{backup_id}`
   * Note that location_id can represent a GCP `zone` or `region` depending on the
   * resource.
   * for example:
   * A zonal Filestore instance:
   * * `projects/my-project/locations/us-central1-c/instances/my-basic-tier-filer`
   * A regional Filestore instance:
   * * `projects/my-project/locations/us-central1/instances/my-enterprise-filer`
   * </pre>
   */
  public static final class CloudFilestoreManagerStub
      extends io.grpc.stub.AbstractAsyncStub<CloudFilestoreManagerStub> {
    private CloudFilestoreManagerStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected CloudFilestoreManagerStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new CloudFilestoreManagerStub(channel, callOptions);
    }

    /**
     *
     *
     * <pre>
     * Lists all instances in a project for either a specified location
     * or for all locations.
     * </pre>
     */
    public void listInstances(
        com.google.cloud.filestore.v1beta1.ListInstancesRequest request,
        io.grpc.stub.StreamObserver<com.google.cloud.filestore.v1beta1.ListInstancesResponse>
            responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListInstancesMethod(), getCallOptions()),
          request,
          responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Gets the details of a specific instance.
     * </pre>
     */
    public void getInstance(
        com.google.cloud.filestore.v1beta1.GetInstanceRequest request,
        io.grpc.stub.StreamObserver<com.google.cloud.filestore.v1beta1.Instance> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetInstanceMethod(), getCallOptions()),
          request,
          responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Creates an instance.
     * When creating from a backup, the capacity of the new instance needs to be
     * equal to or larger than the capacity of the backup (and also equal to or
     * larger than the minimum capacity of the tier).
     * </pre>
     */
    public void createInstance(
        com.google.cloud.filestore.v1beta1.CreateInstanceRequest request,
        io.grpc.stub.StreamObserver<com.google.longrunning.Operation> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCreateInstanceMethod(), getCallOptions()),
          request,
          responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Updates the settings of a specific instance.
     * </pre>
     */
    public void updateInstance(
        com.google.cloud.filestore.v1beta1.UpdateInstanceRequest request,
        io.grpc.stub.StreamObserver<com.google.longrunning.Operation> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getUpdateInstanceMethod(), getCallOptions()),
          request,
          responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Restores an existing instance's file share from a backup.
     * The capacity of the instance needs to be equal to or larger than the
     * capacity of the backup (and also equal to or larger than the minimum
     * capacity of the tier).
     * </pre>
     */
    public void restoreInstance(
        com.google.cloud.filestore.v1beta1.RestoreInstanceRequest request,
        io.grpc.stub.StreamObserver<com.google.longrunning.Operation> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRestoreInstanceMethod(), getCallOptions()),
          request,
          responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Revert an existing instance's file system to a specified snapshot.
     * </pre>
     */
    public void revertInstance(
        com.google.cloud.filestore.v1beta1.RevertInstanceRequest request,
        io.grpc.stub.StreamObserver<com.google.longrunning.Operation> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRevertInstanceMethod(), getCallOptions()),
          request,
          responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Deletes an instance.
     * </pre>
     */
    public void deleteInstance(
        com.google.cloud.filestore.v1beta1.DeleteInstanceRequest request,
        io.grpc.stub.StreamObserver<com.google.longrunning.Operation> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDeleteInstanceMethod(), getCallOptions()),
          request,
          responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Lists all snapshots in a project for either a specified location
     * or for all locations.
     * </pre>
     */
    public void listSnapshots(
        com.google.cloud.filestore.v1beta1.ListSnapshotsRequest request,
        io.grpc.stub.StreamObserver<com.google.cloud.filestore.v1beta1.ListSnapshotsResponse>
            responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListSnapshotsMethod(), getCallOptions()),
          request,
          responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Gets the details of a specific snapshot.
     * </pre>
     */
    public void getSnapshot(
        com.google.cloud.filestore.v1beta1.GetSnapshotRequest request,
        io.grpc.stub.StreamObserver<com.google.cloud.filestore.v1beta1.Snapshot> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetSnapshotMethod(), getCallOptions()),
          request,
          responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Creates a snapshot.
     * </pre>
     */
    public void createSnapshot(
        com.google.cloud.filestore.v1beta1.CreateSnapshotRequest request,
        io.grpc.stub.StreamObserver<com.google.longrunning.Operation> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCreateSnapshotMethod(), getCallOptions()),
          request,
          responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Deletes a snapshot.
     * </pre>
     */
    public void deleteSnapshot(
        com.google.cloud.filestore.v1beta1.DeleteSnapshotRequest request,
        io.grpc.stub.StreamObserver<com.google.longrunning.Operation> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDeleteSnapshotMethod(), getCallOptions()),
          request,
          responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Updates the settings of a specific snapshot.
     * </pre>
     */
    public void updateSnapshot(
        com.google.cloud.filestore.v1beta1.UpdateSnapshotRequest request,
        io.grpc.stub.StreamObserver<com.google.longrunning.Operation> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getUpdateSnapshotMethod(), getCallOptions()),
          request,
          responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Lists all backups in a project for either a specified location or for all
     * locations.
     * </pre>
     */
    public void listBackups(
        com.google.cloud.filestore.v1beta1.ListBackupsRequest request,
        io.grpc.stub.StreamObserver<com.google.cloud.filestore.v1beta1.ListBackupsResponse>
            responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListBackupsMethod(), getCallOptions()),
          request,
          responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Gets the details of a specific backup.
     * </pre>
     */
    public void getBackup(
        com.google.cloud.filestore.v1beta1.GetBackupRequest request,
        io.grpc.stub.StreamObserver<com.google.cloud.filestore.v1beta1.Backup> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetBackupMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Creates a backup.
     * </pre>
     */
    public void createBackup(
        com.google.cloud.filestore.v1beta1.CreateBackupRequest request,
        io.grpc.stub.StreamObserver<com.google.longrunning.Operation> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCreateBackupMethod(), getCallOptions()),
          request,
          responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Deletes a backup.
     * </pre>
     */
    public void deleteBackup(
        com.google.cloud.filestore.v1beta1.DeleteBackupRequest request,
        io.grpc.stub.StreamObserver<com.google.longrunning.Operation> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDeleteBackupMethod(), getCallOptions()),
          request,
          responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Updates the settings of a specific backup.
     * </pre>
     */
    public void updateBackup(
        com.google.cloud.filestore.v1beta1.UpdateBackupRequest request,
        io.grpc.stub.StreamObserver<com.google.longrunning.Operation> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getUpdateBackupMethod(), getCallOptions()),
          request,
          responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Lists all shares for a specified instance.
     * </pre>
     */
    public void listShares(
        com.google.cloud.filestore.v1beta1.ListSharesRequest request,
        io.grpc.stub.StreamObserver<com.google.cloud.filestore.v1beta1.ListSharesResponse>
            responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListSharesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Gets the details of a specific share.
     * </pre>
     */
    public void getShare(
        com.google.cloud.filestore.v1beta1.GetShareRequest request,
        io.grpc.stub.StreamObserver<com.google.cloud.filestore.v1beta1.Share> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetShareMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Creates a share.
     * </pre>
     */
    public void createShare(
        com.google.cloud.filestore.v1beta1.CreateShareRequest request,
        io.grpc.stub.StreamObserver<com.google.longrunning.Operation> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCreateShareMethod(), getCallOptions()),
          request,
          responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Deletes a share.
     * </pre>
     */
    public void deleteShare(
        com.google.cloud.filestore.v1beta1.DeleteShareRequest request,
        io.grpc.stub.StreamObserver<com.google.longrunning.Operation> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDeleteShareMethod(), getCallOptions()),
          request,
          responseObserver);
    }

    /**
     *
     *
     * <pre>
     * Updates the settings of a specific share.
     * </pre>
     */
    public void updateShare(
        com.google.cloud.filestore.v1beta1.UpdateShareRequest request,
        io.grpc.stub.StreamObserver<com.google.longrunning.Operation> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getUpdateShareMethod(), getCallOptions()),
          request,
          responseObserver);
    }
  }

  /**
   *
   *
   * <pre>
   * Configures and manages Filestore resources.
   * Filestore Manager v1beta1.
   * The `file.googleapis.com` service implements the Filestore API and
   * defines the following model for managing resources:
   * * The service works with a collection of cloud projects, named: `/projects/&#42;`
   * * Each project has a collection of available locations, named: `/locations/&#42;`
   * * Each location has a collection of instances and backups, named:
   * `/instances/&#42;` and `/backups/&#42;` respectively.
   * * As such, Filestore instances are resources of the form:
   *   `/projects/{project_id}/locations/{location_id}/instances/{instance_id}`
   *   backups are resources of the form:
   *   `/projects/{project_id}/locations/{location_id}/backup/{backup_id}`
   * Note that location_id can represent a GCP `zone` or `region` depending on the
   * resource.
   * for example:
   * A zonal Filestore instance:
   * * `projects/my-project/locations/us-central1-c/instances/my-basic-tier-filer`
   * A regional Filestore instance:
   * * `projects/my-project/locations/us-central1/instances/my-enterprise-filer`
   * </pre>
   */
  public static final class CloudFilestoreManagerBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<CloudFilestoreManagerBlockingStub> {
    private CloudFilestoreManagerBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected CloudFilestoreManagerBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new CloudFilestoreManagerBlockingStub(channel, callOptions);
    }

    /**
     *
     *
     * <pre>
     * Lists all instances in a project for either a specified location
     * or for all locations.
     * </pre>
     */
    public com.google.cloud.filestore.v1beta1.ListInstancesResponse listInstances(
        com.google.cloud.filestore.v1beta1.ListInstancesRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListInstancesMethod(), getCallOptions(), request);
    }

    /**
     *
     *
     * <pre>
     * Gets the details of a specific instance.
     * </pre>
     */
    public com.google.cloud.filestore.v1beta1.Instance getInstance(
        com.google.cloud.filestore.v1beta1.GetInstanceRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetInstanceMethod(), getCallOptions(), request);
    }

    /**
     *
     *
     * <pre>
     * Creates an instance.
     * When creating from a backup, the capacity of the new instance needs to be
     * equal to or larger than the capacity of the backup (and also equal to or
     * larger than the minimum capacity of the tier).
     * </pre>
     */
    public com.google.longrunning.Operation createInstance(
        com.google.cloud.filestore.v1beta1.CreateInstanceRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCreateInstanceMethod(), getCallOptions(), request);
    }

    /**
     *
     *
     * <pre>
     * Updates the settings of a specific instance.
     * </pre>
     */
    public com.google.longrunning.Operation updateInstance(
        com.google.cloud.filestore.v1beta1.UpdateInstanceRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getUpdateInstanceMethod(), getCallOptions(), request);
    }

    /**
     *
     *
     * <pre>
     * Restores an existing instance's file share from a backup.
     * The capacity of the instance needs to be equal to or larger than the
     * capacity of the backup (and also equal to or larger than the minimum
     * capacity of the tier).
     * </pre>
     */
    public com.google.longrunning.Operation restoreInstance(
        com.google.cloud.filestore.v1beta1.RestoreInstanceRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRestoreInstanceMethod(), getCallOptions(), request);
    }

    /**
     *
     *
     * <pre>
     * Revert an existing instance's file system to a specified snapshot.
     * </pre>
     */
    public com.google.longrunning.Operation revertInstance(
        com.google.cloud.filestore.v1beta1.RevertInstanceRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRevertInstanceMethod(), getCallOptions(), request);
    }

    /**
     *
     *
     * <pre>
     * Deletes an instance.
     * </pre>
     */
    public com.google.longrunning.Operation deleteInstance(
        com.google.cloud.filestore.v1beta1.DeleteInstanceRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDeleteInstanceMethod(), getCallOptions(), request);
    }

    /**
     *
     *
     * <pre>
     * Lists all snapshots in a project for either a specified location
     * or for all locations.
     * </pre>
     */
    public com.google.cloud.filestore.v1beta1.ListSnapshotsResponse listSnapshots(
        com.google.cloud.filestore.v1beta1.ListSnapshotsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListSnapshotsMethod(), getCallOptions(), request);
    }

    /**
     *
     *
     * <pre>
     * Gets the details of a specific snapshot.
     * </pre>
     */
    public com.google.cloud.filestore.v1beta1.Snapshot getSnapshot(
        com.google.cloud.filestore.v1beta1.GetSnapshotRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetSnapshotMethod(), getCallOptions(), request);
    }

    /**
     *
     *
     * <pre>
     * Creates a snapshot.
     * </pre>
     */
    public com.google.longrunning.Operation createSnapshot(
        com.google.cloud.filestore.v1beta1.CreateSnapshotRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCreateSnapshotMethod(), getCallOptions(), request);
    }

    /**
     *
     *
     * <pre>
     * Deletes a snapshot.
     * </pre>
     */
    public com.google.longrunning.Operation deleteSnapshot(
        com.google.cloud.filestore.v1beta1.DeleteSnapshotRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDeleteSnapshotMethod(), getCallOptions(), request);
    }

    /**
     *
     *
     * <pre>
     * Updates the settings of a specific snapshot.
     * </pre>
     */
    public com.google.longrunning.Operation updateSnapshot(
        com.google.cloud.filestore.v1beta1.UpdateSnapshotRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getUpdateSnapshotMethod(), getCallOptions(), request);
    }

    /**
     *
     *
     * <pre>
     * Lists all backups in a project for either a specified location or for all
     * locations.
     * </pre>
     */
    public com.google.cloud.filestore.v1beta1.ListBackupsResponse listBackups(
        com.google.cloud.filestore.v1beta1.ListBackupsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListBackupsMethod(), getCallOptions(), request);
    }

    /**
     *
     *
     * <pre>
     * Gets the details of a specific backup.
     * </pre>
     */
    public com.google.cloud.filestore.v1beta1.Backup getBackup(
        com.google.cloud.filestore.v1beta1.GetBackupRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetBackupMethod(), getCallOptions(), request);
    }

    /**
     *
     *
     * <pre>
     * Creates a backup.
     * </pre>
     */
    public com.google.longrunning.Operation createBackup(
        com.google.cloud.filestore.v1beta1.CreateBackupRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCreateBackupMethod(), getCallOptions(), request);
    }

    /**
     *
     *
     * <pre>
     * Deletes a backup.
     * </pre>
     */
    public com.google.longrunning.Operation deleteBackup(
        com.google.cloud.filestore.v1beta1.DeleteBackupRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDeleteBackupMethod(), getCallOptions(), request);
    }

    /**
     *
     *
     * <pre>
     * Updates the settings of a specific backup.
     * </pre>
     */
    public com.google.longrunning.Operation updateBackup(
        com.google.cloud.filestore.v1beta1.UpdateBackupRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getUpdateBackupMethod(), getCallOptions(), request);
    }

    /**
     *
     *
     * <pre>
     * Lists all shares for a specified instance.
     * </pre>
     */
    public com.google.cloud.filestore.v1beta1.ListSharesResponse listShares(
        com.google.cloud.filestore.v1beta1.ListSharesRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListSharesMethod(), getCallOptions(), request);
    }

    /**
     *
     *
     * <pre>
     * Gets the details of a specific share.
     * </pre>
     */
    public com.google.cloud.filestore.v1beta1.Share getShare(
        com.google.cloud.filestore.v1beta1.GetShareRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetShareMethod(), getCallOptions(), request);
    }

    /**
     *
     *
     * <pre>
     * Creates a share.
     * </pre>
     */
    public com.google.longrunning.Operation createShare(
        com.google.cloud.filestore.v1beta1.CreateShareRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCreateShareMethod(), getCallOptions(), request);
    }

    /**
     *
     *
     * <pre>
     * Deletes a share.
     * </pre>
     */
    public com.google.longrunning.Operation deleteShare(
        com.google.cloud.filestore.v1beta1.DeleteShareRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDeleteShareMethod(), getCallOptions(), request);
    }

    /**
     *
     *
     * <pre>
     * Updates the settings of a specific share.
     * </pre>
     */
    public com.google.longrunning.Operation updateShare(
        com.google.cloud.filestore.v1beta1.UpdateShareRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getUpdateShareMethod(), getCallOptions(), request);
    }
  }

  /**
   *
   *
   * <pre>
   * Configures and manages Filestore resources.
   * Filestore Manager v1beta1.
   * The `file.googleapis.com` service implements the Filestore API and
   * defines the following model for managing resources:
   * * The service works with a collection of cloud projects, named: `/projects/&#42;`
   * * Each project has a collection of available locations, named: `/locations/&#42;`
   * * Each location has a collection of instances and backups, named:
   * `/instances/&#42;` and `/backups/&#42;` respectively.
   * * As such, Filestore instances are resources of the form:
   *   `/projects/{project_id}/locations/{location_id}/instances/{instance_id}`
   *   backups are resources of the form:
   *   `/projects/{project_id}/locations/{location_id}/backup/{backup_id}`
   * Note that location_id can represent a GCP `zone` or `region` depending on the
   * resource.
   * for example:
   * A zonal Filestore instance:
   * * `projects/my-project/locations/us-central1-c/instances/my-basic-tier-filer`
   * A regional Filestore instance:
   * * `projects/my-project/locations/us-central1/instances/my-enterprise-filer`
   * </pre>
   */
  public static final class CloudFilestoreManagerFutureStub
      extends io.grpc.stub.AbstractFutureStub<CloudFilestoreManagerFutureStub> {
    private CloudFilestoreManagerFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected CloudFilestoreManagerFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new CloudFilestoreManagerFutureStub(channel, callOptions);
    }

    /**
     *
     *
     * <pre>
     * Lists all instances in a project for either a specified location
     * or for all locations.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<
            com.google.cloud.filestore.v1beta1.ListInstancesResponse>
        listInstances(com.google.cloud.filestore.v1beta1.ListInstancesRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListInstancesMethod(), getCallOptions()), request);
    }

    /**
     *
     *
     * <pre>
     * Gets the details of a specific instance.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<
            com.google.cloud.filestore.v1beta1.Instance>
        getInstance(com.google.cloud.filestore.v1beta1.GetInstanceRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetInstanceMethod(), getCallOptions()), request);
    }

    /**
     *
     *
     * <pre>
     * Creates an instance.
     * When creating from a backup, the capacity of the new instance needs to be
     * equal to or larger than the capacity of the backup (and also equal to or
     * larger than the minimum capacity of the tier).
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.longrunning.Operation>
        createInstance(com.google.cloud.filestore.v1beta1.CreateInstanceRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCreateInstanceMethod(), getCallOptions()), request);
    }

    /**
     *
     *
     * <pre>
     * Updates the settings of a specific instance.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.longrunning.Operation>
        updateInstance(com.google.cloud.filestore.v1beta1.UpdateInstanceRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getUpdateInstanceMethod(), getCallOptions()), request);
    }

    /**
     *
     *
     * <pre>
     * Restores an existing instance's file share from a backup.
     * The capacity of the instance needs to be equal to or larger than the
     * capacity of the backup (and also equal to or larger than the minimum
     * capacity of the tier).
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.longrunning.Operation>
        restoreInstance(com.google.cloud.filestore.v1beta1.RestoreInstanceRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRestoreInstanceMethod(), getCallOptions()), request);
    }

    /**
     *
     *
     * <pre>
     * Revert an existing instance's file system to a specified snapshot.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.longrunning.Operation>
        revertInstance(com.google.cloud.filestore.v1beta1.RevertInstanceRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRevertInstanceMethod(), getCallOptions()), request);
    }

    /**
     *
     *
     * <pre>
     * Deletes an instance.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.longrunning.Operation>
        deleteInstance(com.google.cloud.filestore.v1beta1.DeleteInstanceRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDeleteInstanceMethod(), getCallOptions()), request);
    }

    /**
     *
     *
     * <pre>
     * Lists all snapshots in a project for either a specified location
     * or for all locations.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<
            com.google.cloud.filestore.v1beta1.ListSnapshotsResponse>
        listSnapshots(com.google.cloud.filestore.v1beta1.ListSnapshotsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListSnapshotsMethod(), getCallOptions()), request);
    }

    /**
     *
     *
     * <pre>
     * Gets the details of a specific snapshot.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<
            com.google.cloud.filestore.v1beta1.Snapshot>
        getSnapshot(com.google.cloud.filestore.v1beta1.GetSnapshotRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetSnapshotMethod(), getCallOptions()), request);
    }

    /**
     *
     *
     * <pre>
     * Creates a snapshot.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.longrunning.Operation>
        createSnapshot(com.google.cloud.filestore.v1beta1.CreateSnapshotRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCreateSnapshotMethod(), getCallOptions()), request);
    }

    /**
     *
     *
     * <pre>
     * Deletes a snapshot.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.longrunning.Operation>
        deleteSnapshot(com.google.cloud.filestore.v1beta1.DeleteSnapshotRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDeleteSnapshotMethod(), getCallOptions()), request);
    }

    /**
     *
     *
     * <pre>
     * Updates the settings of a specific snapshot.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.longrunning.Operation>
        updateSnapshot(com.google.cloud.filestore.v1beta1.UpdateSnapshotRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getUpdateSnapshotMethod(), getCallOptions()), request);
    }

    /**
     *
     *
     * <pre>
     * Lists all backups in a project for either a specified location or for all
     * locations.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<
            com.google.cloud.filestore.v1beta1.ListBackupsResponse>
        listBackups(com.google.cloud.filestore.v1beta1.ListBackupsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListBackupsMethod(), getCallOptions()), request);
    }

    /**
     *
     *
     * <pre>
     * Gets the details of a specific backup.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<
            com.google.cloud.filestore.v1beta1.Backup>
        getBackup(com.google.cloud.filestore.v1beta1.GetBackupRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetBackupMethod(), getCallOptions()), request);
    }

    /**
     *
     *
     * <pre>
     * Creates a backup.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.longrunning.Operation>
        createBackup(com.google.cloud.filestore.v1beta1.CreateBackupRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCreateBackupMethod(), getCallOptions()), request);
    }

    /**
     *
     *
     * <pre>
     * Deletes a backup.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.longrunning.Operation>
        deleteBackup(com.google.cloud.filestore.v1beta1.DeleteBackupRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDeleteBackupMethod(), getCallOptions()), request);
    }

    /**
     *
     *
     * <pre>
     * Updates the settings of a specific backup.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.longrunning.Operation>
        updateBackup(com.google.cloud.filestore.v1beta1.UpdateBackupRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getUpdateBackupMethod(), getCallOptions()), request);
    }

    /**
     *
     *
     * <pre>
     * Lists all shares for a specified instance.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<
            com.google.cloud.filestore.v1beta1.ListSharesResponse>
        listShares(com.google.cloud.filestore.v1beta1.ListSharesRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListSharesMethod(), getCallOptions()), request);
    }

    /**
     *
     *
     * <pre>
     * Gets the details of a specific share.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<
            com.google.cloud.filestore.v1beta1.Share>
        getShare(com.google.cloud.filestore.v1beta1.GetShareRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetShareMethod(), getCallOptions()), request);
    }

    /**
     *
     *
     * <pre>
     * Creates a share.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.longrunning.Operation>
        createShare(com.google.cloud.filestore.v1beta1.CreateShareRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCreateShareMethod(), getCallOptions()), request);
    }

    /**
     *
     *
     * <pre>
     * Deletes a share.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.longrunning.Operation>
        deleteShare(com.google.cloud.filestore.v1beta1.DeleteShareRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDeleteShareMethod(), getCallOptions()), request);
    }

    /**
     *
     *
     * <pre>
     * Updates the settings of a specific share.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.longrunning.Operation>
        updateShare(com.google.cloud.filestore.v1beta1.UpdateShareRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getUpdateShareMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_LIST_INSTANCES = 0;
  private static final int METHODID_GET_INSTANCE = 1;
  private static final int METHODID_CREATE_INSTANCE = 2;
  private static final int METHODID_UPDATE_INSTANCE = 3;
  private static final int METHODID_RESTORE_INSTANCE = 4;
  private static final int METHODID_REVERT_INSTANCE = 5;
  private static final int METHODID_DELETE_INSTANCE = 6;
  private static final int METHODID_LIST_SNAPSHOTS = 7;
  private static final int METHODID_GET_SNAPSHOT = 8;
  private static final int METHODID_CREATE_SNAPSHOT = 9;
  private static final int METHODID_DELETE_SNAPSHOT = 10;
  private static final int METHODID_UPDATE_SNAPSHOT = 11;
  private static final int METHODID_LIST_BACKUPS = 12;
  private static final int METHODID_GET_BACKUP = 13;
  private static final int METHODID_CREATE_BACKUP = 14;
  private static final int METHODID_DELETE_BACKUP = 15;
  private static final int METHODID_UPDATE_BACKUP = 16;
  private static final int METHODID_LIST_SHARES = 17;
  private static final int METHODID_GET_SHARE = 18;
  private static final int METHODID_CREATE_SHARE = 19;
  private static final int METHODID_DELETE_SHARE = 20;
  private static final int METHODID_UPDATE_SHARE = 21;

  private static final class MethodHandlers<Req, Resp>
      implements io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
          io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
          io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
          io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final CloudFilestoreManagerImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(CloudFilestoreManagerImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_LIST_INSTANCES:
          serviceImpl.listInstances(
              (com.google.cloud.filestore.v1beta1.ListInstancesRequest) request,
              (io.grpc.stub.StreamObserver<
                      com.google.cloud.filestore.v1beta1.ListInstancesResponse>)
                  responseObserver);
          break;
        case METHODID_GET_INSTANCE:
          serviceImpl.getInstance(
              (com.google.cloud.filestore.v1beta1.GetInstanceRequest) request,
              (io.grpc.stub.StreamObserver<com.google.cloud.filestore.v1beta1.Instance>)
                  responseObserver);
          break;
        case METHODID_CREATE_INSTANCE:
          serviceImpl.createInstance(
              (com.google.cloud.filestore.v1beta1.CreateInstanceRequest) request,
              (io.grpc.stub.StreamObserver<com.google.longrunning.Operation>) responseObserver);
          break;
        case METHODID_UPDATE_INSTANCE:
          serviceImpl.updateInstance(
              (com.google.cloud.filestore.v1beta1.UpdateInstanceRequest) request,
              (io.grpc.stub.StreamObserver<com.google.longrunning.Operation>) responseObserver);
          break;
        case METHODID_RESTORE_INSTANCE:
          serviceImpl.restoreInstance(
              (com.google.cloud.filestore.v1beta1.RestoreInstanceRequest) request,
              (io.grpc.stub.StreamObserver<com.google.longrunning.Operation>) responseObserver);
          break;
        case METHODID_REVERT_INSTANCE:
          serviceImpl.revertInstance(
              (com.google.cloud.filestore.v1beta1.RevertInstanceRequest) request,
              (io.grpc.stub.StreamObserver<com.google.longrunning.Operation>) responseObserver);
          break;
        case METHODID_DELETE_INSTANCE:
          serviceImpl.deleteInstance(
              (com.google.cloud.filestore.v1beta1.DeleteInstanceRequest) request,
              (io.grpc.stub.StreamObserver<com.google.longrunning.Operation>) responseObserver);
          break;
        case METHODID_LIST_SNAPSHOTS:
          serviceImpl.listSnapshots(
              (com.google.cloud.filestore.v1beta1.ListSnapshotsRequest) request,
              (io.grpc.stub.StreamObserver<
                      com.google.cloud.filestore.v1beta1.ListSnapshotsResponse>)
                  responseObserver);
          break;
        case METHODID_GET_SNAPSHOT:
          serviceImpl.getSnapshot(
              (com.google.cloud.filestore.v1beta1.GetSnapshotRequest) request,
              (io.grpc.stub.StreamObserver<com.google.cloud.filestore.v1beta1.Snapshot>)
                  responseObserver);
          break;
        case METHODID_CREATE_SNAPSHOT:
          serviceImpl.createSnapshot(
              (com.google.cloud.filestore.v1beta1.CreateSnapshotRequest) request,
              (io.grpc.stub.StreamObserver<com.google.longrunning.Operation>) responseObserver);
          break;
        case METHODID_DELETE_SNAPSHOT:
          serviceImpl.deleteSnapshot(
              (com.google.cloud.filestore.v1beta1.DeleteSnapshotRequest) request,
              (io.grpc.stub.StreamObserver<com.google.longrunning.Operation>) responseObserver);
          break;
        case METHODID_UPDATE_SNAPSHOT:
          serviceImpl.updateSnapshot(
              (com.google.cloud.filestore.v1beta1.UpdateSnapshotRequest) request,
              (io.grpc.stub.StreamObserver<com.google.longrunning.Operation>) responseObserver);
          break;
        case METHODID_LIST_BACKUPS:
          serviceImpl.listBackups(
              (com.google.cloud.filestore.v1beta1.ListBackupsRequest) request,
              (io.grpc.stub.StreamObserver<com.google.cloud.filestore.v1beta1.ListBackupsResponse>)
                  responseObserver);
          break;
        case METHODID_GET_BACKUP:
          serviceImpl.getBackup(
              (com.google.cloud.filestore.v1beta1.GetBackupRequest) request,
              (io.grpc.stub.StreamObserver<com.google.cloud.filestore.v1beta1.Backup>)
                  responseObserver);
          break;
        case METHODID_CREATE_BACKUP:
          serviceImpl.createBackup(
              (com.google.cloud.filestore.v1beta1.CreateBackupRequest) request,
              (io.grpc.stub.StreamObserver<com.google.longrunning.Operation>) responseObserver);
          break;
        case METHODID_DELETE_BACKUP:
          serviceImpl.deleteBackup(
              (com.google.cloud.filestore.v1beta1.DeleteBackupRequest) request,
              (io.grpc.stub.StreamObserver<com.google.longrunning.Operation>) responseObserver);
          break;
        case METHODID_UPDATE_BACKUP:
          serviceImpl.updateBackup(
              (com.google.cloud.filestore.v1beta1.UpdateBackupRequest) request,
              (io.grpc.stub.StreamObserver<com.google.longrunning.Operation>) responseObserver);
          break;
        case METHODID_LIST_SHARES:
          serviceImpl.listShares(
              (com.google.cloud.filestore.v1beta1.ListSharesRequest) request,
              (io.grpc.stub.StreamObserver<com.google.cloud.filestore.v1beta1.ListSharesResponse>)
                  responseObserver);
          break;
        case METHODID_GET_SHARE:
          serviceImpl.getShare(
              (com.google.cloud.filestore.v1beta1.GetShareRequest) request,
              (io.grpc.stub.StreamObserver<com.google.cloud.filestore.v1beta1.Share>)
                  responseObserver);
          break;
        case METHODID_CREATE_SHARE:
          serviceImpl.createShare(
              (com.google.cloud.filestore.v1beta1.CreateShareRequest) request,
              (io.grpc.stub.StreamObserver<com.google.longrunning.Operation>) responseObserver);
          break;
        case METHODID_DELETE_SHARE:
          serviceImpl.deleteShare(
              (com.google.cloud.filestore.v1beta1.DeleteShareRequest) request,
              (io.grpc.stub.StreamObserver<com.google.longrunning.Operation>) responseObserver);
          break;
        case METHODID_UPDATE_SHARE:
          serviceImpl.updateShare(
              (com.google.cloud.filestore.v1beta1.UpdateShareRequest) request,
              (io.grpc.stub.StreamObserver<com.google.longrunning.Operation>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  private abstract static class CloudFilestoreManagerBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier,
          io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    CloudFilestoreManagerBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.google.cloud.filestore.v1beta1.CloudFilestoreServiceProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("CloudFilestoreManager");
    }
  }

  private static final class CloudFilestoreManagerFileDescriptorSupplier
      extends CloudFilestoreManagerBaseDescriptorSupplier {
    CloudFilestoreManagerFileDescriptorSupplier() {}
  }

  private static final class CloudFilestoreManagerMethodDescriptorSupplier
      extends CloudFilestoreManagerBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    CloudFilestoreManagerMethodDescriptorSupplier(String methodName) {
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
      synchronized (CloudFilestoreManagerGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor =
              result =
                  io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
                      .setSchemaDescriptor(new CloudFilestoreManagerFileDescriptorSupplier())
                      .addMethod(getListInstancesMethod())
                      .addMethod(getGetInstanceMethod())
                      .addMethod(getCreateInstanceMethod())
                      .addMethod(getUpdateInstanceMethod())
                      .addMethod(getRestoreInstanceMethod())
                      .addMethod(getRevertInstanceMethod())
                      .addMethod(getDeleteInstanceMethod())
                      .addMethod(getListSnapshotsMethod())
                      .addMethod(getGetSnapshotMethod())
                      .addMethod(getCreateSnapshotMethod())
                      .addMethod(getDeleteSnapshotMethod())
                      .addMethod(getUpdateSnapshotMethod())
                      .addMethod(getListBackupsMethod())
                      .addMethod(getGetBackupMethod())
                      .addMethod(getCreateBackupMethod())
                      .addMethod(getDeleteBackupMethod())
                      .addMethod(getUpdateBackupMethod())
                      .addMethod(getListSharesMethod())
                      .addMethod(getGetShareMethod())
                      .addMethod(getCreateShareMethod())
                      .addMethod(getDeleteShareMethod())
                      .addMethod(getUpdateShareMethod())
                      .build();
        }
      }
    }
    return result;
  }
}
