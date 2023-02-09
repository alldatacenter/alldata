/*
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.filestore.v1beta1;

import static com.google.cloud.filestore.v1beta1.CloudFilestoreManagerClient.ListBackupsPagedResponse;
import static com.google.cloud.filestore.v1beta1.CloudFilestoreManagerClient.ListInstancesPagedResponse;
import static com.google.cloud.filestore.v1beta1.CloudFilestoreManagerClient.ListSharesPagedResponse;
import static com.google.cloud.filestore.v1beta1.CloudFilestoreManagerClient.ListSnapshotsPagedResponse;

import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GaxGrpcProperties;
import com.google.api.gax.grpc.testing.LocalChannelProvider;
import com.google.api.gax.grpc.testing.MockGrpcService;
import com.google.api.gax.grpc.testing.MockServiceHelper;
import com.google.api.gax.rpc.ApiClientHeaderProvider;
import com.google.api.gax.rpc.InvalidArgumentException;
import com.google.api.gax.rpc.StatusCode;
import com.google.common.collect.Lists;
import com.google.longrunning.Operation;
import com.google.protobuf.AbstractMessage;
import com.google.protobuf.Any;
import com.google.protobuf.BoolValue;
import com.google.protobuf.Empty;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Timestamp;
import io.grpc.StatusRuntimeException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import javax.annotation.Generated;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

@Generated("by gapic-generator-java")
public class CloudFilestoreManagerClientTest {
  private static MockCloudFilestoreManager mockCloudFilestoreManager;
  private static MockServiceHelper mockServiceHelper;
  private LocalChannelProvider channelProvider;
  private CloudFilestoreManagerClient client;

  @BeforeClass
  public static void startStaticServer() {
    mockCloudFilestoreManager = new MockCloudFilestoreManager();
    mockServiceHelper =
        new MockServiceHelper(
            UUID.randomUUID().toString(),
            Arrays.<MockGrpcService>asList(mockCloudFilestoreManager));
    mockServiceHelper.start();
  }

  @AfterClass
  public static void stopServer() {
    mockServiceHelper.stop();
  }

  @Before
  public void setUp() throws IOException {
    mockServiceHelper.reset();
    channelProvider = mockServiceHelper.createChannelProvider();
    CloudFilestoreManagerSettings settings =
        CloudFilestoreManagerSettings.newBuilder()
            .setTransportChannelProvider(channelProvider)
            .setCredentialsProvider(NoCredentialsProvider.create())
            .build();
    client = CloudFilestoreManagerClient.create(settings);
  }

  @After
  public void tearDown() throws Exception {
    client.close();
  }

  @Test
  public void listInstancesTest() throws Exception {
    Instance responsesElement = Instance.newBuilder().build();
    ListInstancesResponse expectedResponse =
        ListInstancesResponse.newBuilder()
            .setNextPageToken("")
            .addAllInstances(Arrays.asList(responsesElement))
            .build();
    mockCloudFilestoreManager.addResponse(expectedResponse);

    LocationName parent = LocationName.of("[PROJECT]", "[LOCATION]");

    ListInstancesPagedResponse pagedListResponse = client.listInstances(parent);

    List<Instance> resources = Lists.newArrayList(pagedListResponse.iterateAll());

    Assert.assertEquals(1, resources.size());
    Assert.assertEquals(expectedResponse.getInstancesList().get(0), resources.get(0));

    List<AbstractMessage> actualRequests = mockCloudFilestoreManager.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    ListInstancesRequest actualRequest = ((ListInstancesRequest) actualRequests.get(0));

    Assert.assertEquals(parent.toString(), actualRequest.getParent());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void listInstancesExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockCloudFilestoreManager.addException(exception);

    try {
      LocationName parent = LocationName.of("[PROJECT]", "[LOCATION]");
      client.listInstances(parent);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception.
    }
  }

  @Test
  public void listInstancesTest2() throws Exception {
    Instance responsesElement = Instance.newBuilder().build();
    ListInstancesResponse expectedResponse =
        ListInstancesResponse.newBuilder()
            .setNextPageToken("")
            .addAllInstances(Arrays.asList(responsesElement))
            .build();
    mockCloudFilestoreManager.addResponse(expectedResponse);

    String parent = "parent-995424086";

    ListInstancesPagedResponse pagedListResponse = client.listInstances(parent);

    List<Instance> resources = Lists.newArrayList(pagedListResponse.iterateAll());

    Assert.assertEquals(1, resources.size());
    Assert.assertEquals(expectedResponse.getInstancesList().get(0), resources.get(0));

    List<AbstractMessage> actualRequests = mockCloudFilestoreManager.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    ListInstancesRequest actualRequest = ((ListInstancesRequest) actualRequests.get(0));

    Assert.assertEquals(parent, actualRequest.getParent());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void listInstancesExceptionTest2() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockCloudFilestoreManager.addException(exception);

    try {
      String parent = "parent-995424086";
      client.listInstances(parent);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception.
    }
  }

  @Test
  public void getInstanceTest() throws Exception {
    Instance expectedResponse =
        Instance.newBuilder()
            .setName(InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]").toString())
            .setDescription("description-1724546052")
            .setStatusMessage("statusMessage-958704715")
            .setCreateTime(Timestamp.newBuilder().build())
            .putAllLabels(new HashMap<String, String>())
            .addAllFileShares(new ArrayList<FileShareConfig>())
            .addAllNetworks(new ArrayList<NetworkConfig>())
            .setEtag("etag3123477")
            .setSatisfiesPzs(BoolValue.newBuilder().build())
            .setKmsKeyName("kmsKeyName412586233")
            .addAllSuspensionReasons(new ArrayList<Instance.SuspensionReason>())
            .setMaxCapacityGb(220559397)
            .setCapacityStepSizeGb(18315979)
            .setMaxShareCount(-1758695852)
            .setCapacityGb(-1923585408)
            .setMultiShareEnabled(true)
            .build();
    mockCloudFilestoreManager.addResponse(expectedResponse);

    InstanceName name = InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]");

    Instance actualResponse = client.getInstance(name);
    Assert.assertEquals(expectedResponse, actualResponse);

    List<AbstractMessage> actualRequests = mockCloudFilestoreManager.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    GetInstanceRequest actualRequest = ((GetInstanceRequest) actualRequests.get(0));

    Assert.assertEquals(name.toString(), actualRequest.getName());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void getInstanceExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockCloudFilestoreManager.addException(exception);

    try {
      InstanceName name = InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]");
      client.getInstance(name);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception.
    }
  }

  @Test
  public void getInstanceTest2() throws Exception {
    Instance expectedResponse =
        Instance.newBuilder()
            .setName(InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]").toString())
            .setDescription("description-1724546052")
            .setStatusMessage("statusMessage-958704715")
            .setCreateTime(Timestamp.newBuilder().build())
            .putAllLabels(new HashMap<String, String>())
            .addAllFileShares(new ArrayList<FileShareConfig>())
            .addAllNetworks(new ArrayList<NetworkConfig>())
            .setEtag("etag3123477")
            .setSatisfiesPzs(BoolValue.newBuilder().build())
            .setKmsKeyName("kmsKeyName412586233")
            .addAllSuspensionReasons(new ArrayList<Instance.SuspensionReason>())
            .setMaxCapacityGb(220559397)
            .setCapacityStepSizeGb(18315979)
            .setMaxShareCount(-1758695852)
            .setCapacityGb(-1923585408)
            .setMultiShareEnabled(true)
            .build();
    mockCloudFilestoreManager.addResponse(expectedResponse);

    String name = "name3373707";

    Instance actualResponse = client.getInstance(name);
    Assert.assertEquals(expectedResponse, actualResponse);

    List<AbstractMessage> actualRequests = mockCloudFilestoreManager.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    GetInstanceRequest actualRequest = ((GetInstanceRequest) actualRequests.get(0));

    Assert.assertEquals(name, actualRequest.getName());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void getInstanceExceptionTest2() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockCloudFilestoreManager.addException(exception);

    try {
      String name = "name3373707";
      client.getInstance(name);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception.
    }
  }

  @Test
  public void createInstanceTest() throws Exception {
    Instance expectedResponse =
        Instance.newBuilder()
            .setName(InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]").toString())
            .setDescription("description-1724546052")
            .setStatusMessage("statusMessage-958704715")
            .setCreateTime(Timestamp.newBuilder().build())
            .putAllLabels(new HashMap<String, String>())
            .addAllFileShares(new ArrayList<FileShareConfig>())
            .addAllNetworks(new ArrayList<NetworkConfig>())
            .setEtag("etag3123477")
            .setSatisfiesPzs(BoolValue.newBuilder().build())
            .setKmsKeyName("kmsKeyName412586233")
            .addAllSuspensionReasons(new ArrayList<Instance.SuspensionReason>())
            .setMaxCapacityGb(220559397)
            .setCapacityStepSizeGb(18315979)
            .setMaxShareCount(-1758695852)
            .setCapacityGb(-1923585408)
            .setMultiShareEnabled(true)
            .build();
    Operation resultOperation =
        Operation.newBuilder()
            .setName("createInstanceTest")
            .setDone(true)
            .setResponse(Any.pack(expectedResponse))
            .build();
    mockCloudFilestoreManager.addResponse(resultOperation);

    LocationName parent = LocationName.of("[PROJECT]", "[LOCATION]");
    Instance instance = Instance.newBuilder().build();
    String instanceId = "instanceId902024336";

    Instance actualResponse = client.createInstanceAsync(parent, instance, instanceId).get();
    Assert.assertEquals(expectedResponse, actualResponse);

    List<AbstractMessage> actualRequests = mockCloudFilestoreManager.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    CreateInstanceRequest actualRequest = ((CreateInstanceRequest) actualRequests.get(0));

    Assert.assertEquals(parent.toString(), actualRequest.getParent());
    Assert.assertEquals(instance, actualRequest.getInstance());
    Assert.assertEquals(instanceId, actualRequest.getInstanceId());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void createInstanceExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockCloudFilestoreManager.addException(exception);

    try {
      LocationName parent = LocationName.of("[PROJECT]", "[LOCATION]");
      Instance instance = Instance.newBuilder().build();
      String instanceId = "instanceId902024336";
      client.createInstanceAsync(parent, instance, instanceId).get();
      Assert.fail("No exception raised");
    } catch (ExecutionException e) {
      Assert.assertEquals(InvalidArgumentException.class, e.getCause().getClass());
      InvalidArgumentException apiException = ((InvalidArgumentException) e.getCause());
      Assert.assertEquals(StatusCode.Code.INVALID_ARGUMENT, apiException.getStatusCode().getCode());
    }
  }

  @Test
  public void createInstanceTest2() throws Exception {
    Instance expectedResponse =
        Instance.newBuilder()
            .setName(InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]").toString())
            .setDescription("description-1724546052")
            .setStatusMessage("statusMessage-958704715")
            .setCreateTime(Timestamp.newBuilder().build())
            .putAllLabels(new HashMap<String, String>())
            .addAllFileShares(new ArrayList<FileShareConfig>())
            .addAllNetworks(new ArrayList<NetworkConfig>())
            .setEtag("etag3123477")
            .setSatisfiesPzs(BoolValue.newBuilder().build())
            .setKmsKeyName("kmsKeyName412586233")
            .addAllSuspensionReasons(new ArrayList<Instance.SuspensionReason>())
            .setMaxCapacityGb(220559397)
            .setCapacityStepSizeGb(18315979)
            .setMaxShareCount(-1758695852)
            .setCapacityGb(-1923585408)
            .setMultiShareEnabled(true)
            .build();
    Operation resultOperation =
        Operation.newBuilder()
            .setName("createInstanceTest")
            .setDone(true)
            .setResponse(Any.pack(expectedResponse))
            .build();
    mockCloudFilestoreManager.addResponse(resultOperation);

    String parent = "parent-995424086";
    Instance instance = Instance.newBuilder().build();
    String instanceId = "instanceId902024336";

    Instance actualResponse = client.createInstanceAsync(parent, instance, instanceId).get();
    Assert.assertEquals(expectedResponse, actualResponse);

    List<AbstractMessage> actualRequests = mockCloudFilestoreManager.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    CreateInstanceRequest actualRequest = ((CreateInstanceRequest) actualRequests.get(0));

    Assert.assertEquals(parent, actualRequest.getParent());
    Assert.assertEquals(instance, actualRequest.getInstance());
    Assert.assertEquals(instanceId, actualRequest.getInstanceId());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void createInstanceExceptionTest2() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockCloudFilestoreManager.addException(exception);

    try {
      String parent = "parent-995424086";
      Instance instance = Instance.newBuilder().build();
      String instanceId = "instanceId902024336";
      client.createInstanceAsync(parent, instance, instanceId).get();
      Assert.fail("No exception raised");
    } catch (ExecutionException e) {
      Assert.assertEquals(InvalidArgumentException.class, e.getCause().getClass());
      InvalidArgumentException apiException = ((InvalidArgumentException) e.getCause());
      Assert.assertEquals(StatusCode.Code.INVALID_ARGUMENT, apiException.getStatusCode().getCode());
    }
  }

  @Test
  public void updateInstanceTest() throws Exception {
    Instance expectedResponse =
        Instance.newBuilder()
            .setName(InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]").toString())
            .setDescription("description-1724546052")
            .setStatusMessage("statusMessage-958704715")
            .setCreateTime(Timestamp.newBuilder().build())
            .putAllLabels(new HashMap<String, String>())
            .addAllFileShares(new ArrayList<FileShareConfig>())
            .addAllNetworks(new ArrayList<NetworkConfig>())
            .setEtag("etag3123477")
            .setSatisfiesPzs(BoolValue.newBuilder().build())
            .setKmsKeyName("kmsKeyName412586233")
            .addAllSuspensionReasons(new ArrayList<Instance.SuspensionReason>())
            .setMaxCapacityGb(220559397)
            .setCapacityStepSizeGb(18315979)
            .setMaxShareCount(-1758695852)
            .setCapacityGb(-1923585408)
            .setMultiShareEnabled(true)
            .build();
    Operation resultOperation =
        Operation.newBuilder()
            .setName("updateInstanceTest")
            .setDone(true)
            .setResponse(Any.pack(expectedResponse))
            .build();
    mockCloudFilestoreManager.addResponse(resultOperation);

    Instance instance = Instance.newBuilder().build();
    FieldMask updateMask = FieldMask.newBuilder().build();

    Instance actualResponse = client.updateInstanceAsync(instance, updateMask).get();
    Assert.assertEquals(expectedResponse, actualResponse);

    List<AbstractMessage> actualRequests = mockCloudFilestoreManager.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    UpdateInstanceRequest actualRequest = ((UpdateInstanceRequest) actualRequests.get(0));

    Assert.assertEquals(instance, actualRequest.getInstance());
    Assert.assertEquals(updateMask, actualRequest.getUpdateMask());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void updateInstanceExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockCloudFilestoreManager.addException(exception);

    try {
      Instance instance = Instance.newBuilder().build();
      FieldMask updateMask = FieldMask.newBuilder().build();
      client.updateInstanceAsync(instance, updateMask).get();
      Assert.fail("No exception raised");
    } catch (ExecutionException e) {
      Assert.assertEquals(InvalidArgumentException.class, e.getCause().getClass());
      InvalidArgumentException apiException = ((InvalidArgumentException) e.getCause());
      Assert.assertEquals(StatusCode.Code.INVALID_ARGUMENT, apiException.getStatusCode().getCode());
    }
  }

  @Test
  public void restoreInstanceTest() throws Exception {
    Instance expectedResponse =
        Instance.newBuilder()
            .setName(InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]").toString())
            .setDescription("description-1724546052")
            .setStatusMessage("statusMessage-958704715")
            .setCreateTime(Timestamp.newBuilder().build())
            .putAllLabels(new HashMap<String, String>())
            .addAllFileShares(new ArrayList<FileShareConfig>())
            .addAllNetworks(new ArrayList<NetworkConfig>())
            .setEtag("etag3123477")
            .setSatisfiesPzs(BoolValue.newBuilder().build())
            .setKmsKeyName("kmsKeyName412586233")
            .addAllSuspensionReasons(new ArrayList<Instance.SuspensionReason>())
            .setMaxCapacityGb(220559397)
            .setCapacityStepSizeGb(18315979)
            .setMaxShareCount(-1758695852)
            .setCapacityGb(-1923585408)
            .setMultiShareEnabled(true)
            .build();
    Operation resultOperation =
        Operation.newBuilder()
            .setName("restoreInstanceTest")
            .setDone(true)
            .setResponse(Any.pack(expectedResponse))
            .build();
    mockCloudFilestoreManager.addResponse(resultOperation);

    RestoreInstanceRequest request =
        RestoreInstanceRequest.newBuilder()
            .setName(InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]").toString())
            .setFileShare("fileShare-1327728701")
            .build();

    Instance actualResponse = client.restoreInstanceAsync(request).get();
    Assert.assertEquals(expectedResponse, actualResponse);

    List<AbstractMessage> actualRequests = mockCloudFilestoreManager.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    RestoreInstanceRequest actualRequest = ((RestoreInstanceRequest) actualRequests.get(0));

    Assert.assertEquals(request.getName(), actualRequest.getName());
    Assert.assertEquals(request.getFileShare(), actualRequest.getFileShare());
    Assert.assertEquals(request.getSourceSnapshot(), actualRequest.getSourceSnapshot());
    Assert.assertEquals(request.getSourceBackup(), actualRequest.getSourceBackup());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void restoreInstanceExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockCloudFilestoreManager.addException(exception);

    try {
      RestoreInstanceRequest request =
          RestoreInstanceRequest.newBuilder()
              .setName(InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]").toString())
              .setFileShare("fileShare-1327728701")
              .build();
      client.restoreInstanceAsync(request).get();
      Assert.fail("No exception raised");
    } catch (ExecutionException e) {
      Assert.assertEquals(InvalidArgumentException.class, e.getCause().getClass());
      InvalidArgumentException apiException = ((InvalidArgumentException) e.getCause());
      Assert.assertEquals(StatusCode.Code.INVALID_ARGUMENT, apiException.getStatusCode().getCode());
    }
  }

  @Test
  public void revertInstanceTest() throws Exception {
    Instance expectedResponse =
        Instance.newBuilder()
            .setName(InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]").toString())
            .setDescription("description-1724546052")
            .setStatusMessage("statusMessage-958704715")
            .setCreateTime(Timestamp.newBuilder().build())
            .putAllLabels(new HashMap<String, String>())
            .addAllFileShares(new ArrayList<FileShareConfig>())
            .addAllNetworks(new ArrayList<NetworkConfig>())
            .setEtag("etag3123477")
            .setSatisfiesPzs(BoolValue.newBuilder().build())
            .setKmsKeyName("kmsKeyName412586233")
            .addAllSuspensionReasons(new ArrayList<Instance.SuspensionReason>())
            .setMaxCapacityGb(220559397)
            .setCapacityStepSizeGb(18315979)
            .setMaxShareCount(-1758695852)
            .setCapacityGb(-1923585408)
            .setMultiShareEnabled(true)
            .build();
    Operation resultOperation =
        Operation.newBuilder()
            .setName("revertInstanceTest")
            .setDone(true)
            .setResponse(Any.pack(expectedResponse))
            .build();
    mockCloudFilestoreManager.addResponse(resultOperation);

    RevertInstanceRequest request =
        RevertInstanceRequest.newBuilder()
            .setName(InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]").toString())
            .setTargetSnapshotId("targetSnapshotId-2099459472")
            .build();

    Instance actualResponse = client.revertInstanceAsync(request).get();
    Assert.assertEquals(expectedResponse, actualResponse);

    List<AbstractMessage> actualRequests = mockCloudFilestoreManager.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    RevertInstanceRequest actualRequest = ((RevertInstanceRequest) actualRequests.get(0));

    Assert.assertEquals(request.getName(), actualRequest.getName());
    Assert.assertEquals(request.getTargetSnapshotId(), actualRequest.getTargetSnapshotId());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void revertInstanceExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockCloudFilestoreManager.addException(exception);

    try {
      RevertInstanceRequest request =
          RevertInstanceRequest.newBuilder()
              .setName(InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]").toString())
              .setTargetSnapshotId("targetSnapshotId-2099459472")
              .build();
      client.revertInstanceAsync(request).get();
      Assert.fail("No exception raised");
    } catch (ExecutionException e) {
      Assert.assertEquals(InvalidArgumentException.class, e.getCause().getClass());
      InvalidArgumentException apiException = ((InvalidArgumentException) e.getCause());
      Assert.assertEquals(StatusCode.Code.INVALID_ARGUMENT, apiException.getStatusCode().getCode());
    }
  }

  @Test
  public void deleteInstanceTest() throws Exception {
    Empty expectedResponse = Empty.newBuilder().build();
    Operation resultOperation =
        Operation.newBuilder()
            .setName("deleteInstanceTest")
            .setDone(true)
            .setResponse(Any.pack(expectedResponse))
            .build();
    mockCloudFilestoreManager.addResponse(resultOperation);

    InstanceName name = InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]");

    client.deleteInstanceAsync(name).get();

    List<AbstractMessage> actualRequests = mockCloudFilestoreManager.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    DeleteInstanceRequest actualRequest = ((DeleteInstanceRequest) actualRequests.get(0));

    Assert.assertEquals(name.toString(), actualRequest.getName());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void deleteInstanceExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockCloudFilestoreManager.addException(exception);

    try {
      InstanceName name = InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]");
      client.deleteInstanceAsync(name).get();
      Assert.fail("No exception raised");
    } catch (ExecutionException e) {
      Assert.assertEquals(InvalidArgumentException.class, e.getCause().getClass());
      InvalidArgumentException apiException = ((InvalidArgumentException) e.getCause());
      Assert.assertEquals(StatusCode.Code.INVALID_ARGUMENT, apiException.getStatusCode().getCode());
    }
  }

  @Test
  public void deleteInstanceTest2() throws Exception {
    Empty expectedResponse = Empty.newBuilder().build();
    Operation resultOperation =
        Operation.newBuilder()
            .setName("deleteInstanceTest")
            .setDone(true)
            .setResponse(Any.pack(expectedResponse))
            .build();
    mockCloudFilestoreManager.addResponse(resultOperation);

    String name = "name3373707";

    client.deleteInstanceAsync(name).get();

    List<AbstractMessage> actualRequests = mockCloudFilestoreManager.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    DeleteInstanceRequest actualRequest = ((DeleteInstanceRequest) actualRequests.get(0));

    Assert.assertEquals(name, actualRequest.getName());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void deleteInstanceExceptionTest2() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockCloudFilestoreManager.addException(exception);

    try {
      String name = "name3373707";
      client.deleteInstanceAsync(name).get();
      Assert.fail("No exception raised");
    } catch (ExecutionException e) {
      Assert.assertEquals(InvalidArgumentException.class, e.getCause().getClass());
      InvalidArgumentException apiException = ((InvalidArgumentException) e.getCause());
      Assert.assertEquals(StatusCode.Code.INVALID_ARGUMENT, apiException.getStatusCode().getCode());
    }
  }

  @Test
  public void listSnapshotsTest() throws Exception {
    Snapshot responsesElement = Snapshot.newBuilder().build();
    ListSnapshotsResponse expectedResponse =
        ListSnapshotsResponse.newBuilder()
            .setNextPageToken("")
            .addAllSnapshots(Arrays.asList(responsesElement))
            .build();
    mockCloudFilestoreManager.addResponse(expectedResponse);

    InstanceName parent = InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]");

    ListSnapshotsPagedResponse pagedListResponse = client.listSnapshots(parent);

    List<Snapshot> resources = Lists.newArrayList(pagedListResponse.iterateAll());

    Assert.assertEquals(1, resources.size());
    Assert.assertEquals(expectedResponse.getSnapshotsList().get(0), resources.get(0));

    List<AbstractMessage> actualRequests = mockCloudFilestoreManager.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    ListSnapshotsRequest actualRequest = ((ListSnapshotsRequest) actualRequests.get(0));

    Assert.assertEquals(parent.toString(), actualRequest.getParent());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void listSnapshotsExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockCloudFilestoreManager.addException(exception);

    try {
      InstanceName parent = InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]");
      client.listSnapshots(parent);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception.
    }
  }

  @Test
  public void listSnapshotsTest2() throws Exception {
    Snapshot responsesElement = Snapshot.newBuilder().build();
    ListSnapshotsResponse expectedResponse =
        ListSnapshotsResponse.newBuilder()
            .setNextPageToken("")
            .addAllSnapshots(Arrays.asList(responsesElement))
            .build();
    mockCloudFilestoreManager.addResponse(expectedResponse);

    String parent = "parent-995424086";

    ListSnapshotsPagedResponse pagedListResponse = client.listSnapshots(parent);

    List<Snapshot> resources = Lists.newArrayList(pagedListResponse.iterateAll());

    Assert.assertEquals(1, resources.size());
    Assert.assertEquals(expectedResponse.getSnapshotsList().get(0), resources.get(0));

    List<AbstractMessage> actualRequests = mockCloudFilestoreManager.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    ListSnapshotsRequest actualRequest = ((ListSnapshotsRequest) actualRequests.get(0));

    Assert.assertEquals(parent, actualRequest.getParent());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void listSnapshotsExceptionTest2() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockCloudFilestoreManager.addException(exception);

    try {
      String parent = "parent-995424086";
      client.listSnapshots(parent);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception.
    }
  }

  @Test
  public void getSnapshotTest() throws Exception {
    Snapshot expectedResponse =
        Snapshot.newBuilder()
            .setName(
                SnapshotName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]", "[SNAPSHOT]").toString())
            .setDescription("description-1724546052")
            .setCreateTime(Timestamp.newBuilder().build())
            .putAllLabels(new HashMap<String, String>())
            .setFilesystemUsedBytes(387235709)
            .build();
    mockCloudFilestoreManager.addResponse(expectedResponse);

    SnapshotName name = SnapshotName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]", "[SNAPSHOT]");

    Snapshot actualResponse = client.getSnapshot(name);
    Assert.assertEquals(expectedResponse, actualResponse);

    List<AbstractMessage> actualRequests = mockCloudFilestoreManager.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    GetSnapshotRequest actualRequest = ((GetSnapshotRequest) actualRequests.get(0));

    Assert.assertEquals(name.toString(), actualRequest.getName());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void getSnapshotExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockCloudFilestoreManager.addException(exception);

    try {
      SnapshotName name = SnapshotName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]", "[SNAPSHOT]");
      client.getSnapshot(name);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception.
    }
  }

  @Test
  public void getSnapshotTest2() throws Exception {
    Snapshot expectedResponse =
        Snapshot.newBuilder()
            .setName(
                SnapshotName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]", "[SNAPSHOT]").toString())
            .setDescription("description-1724546052")
            .setCreateTime(Timestamp.newBuilder().build())
            .putAllLabels(new HashMap<String, String>())
            .setFilesystemUsedBytes(387235709)
            .build();
    mockCloudFilestoreManager.addResponse(expectedResponse);

    String name = "name3373707";

    Snapshot actualResponse = client.getSnapshot(name);
    Assert.assertEquals(expectedResponse, actualResponse);

    List<AbstractMessage> actualRequests = mockCloudFilestoreManager.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    GetSnapshotRequest actualRequest = ((GetSnapshotRequest) actualRequests.get(0));

    Assert.assertEquals(name, actualRequest.getName());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void getSnapshotExceptionTest2() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockCloudFilestoreManager.addException(exception);

    try {
      String name = "name3373707";
      client.getSnapshot(name);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception.
    }
  }

  @Test
  public void createSnapshotTest() throws Exception {
    Snapshot expectedResponse =
        Snapshot.newBuilder()
            .setName(
                SnapshotName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]", "[SNAPSHOT]").toString())
            .setDescription("description-1724546052")
            .setCreateTime(Timestamp.newBuilder().build())
            .putAllLabels(new HashMap<String, String>())
            .setFilesystemUsedBytes(387235709)
            .build();
    Operation resultOperation =
        Operation.newBuilder()
            .setName("createSnapshotTest")
            .setDone(true)
            .setResponse(Any.pack(expectedResponse))
            .build();
    mockCloudFilestoreManager.addResponse(resultOperation);

    InstanceName parent = InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]");
    Snapshot snapshot = Snapshot.newBuilder().build();
    String snapshotId = "snapshotId-1113817601";

    Snapshot actualResponse = client.createSnapshotAsync(parent, snapshot, snapshotId).get();
    Assert.assertEquals(expectedResponse, actualResponse);

    List<AbstractMessage> actualRequests = mockCloudFilestoreManager.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    CreateSnapshotRequest actualRequest = ((CreateSnapshotRequest) actualRequests.get(0));

    Assert.assertEquals(parent.toString(), actualRequest.getParent());
    Assert.assertEquals(snapshot, actualRequest.getSnapshot());
    Assert.assertEquals(snapshotId, actualRequest.getSnapshotId());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void createSnapshotExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockCloudFilestoreManager.addException(exception);

    try {
      InstanceName parent = InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]");
      Snapshot snapshot = Snapshot.newBuilder().build();
      String snapshotId = "snapshotId-1113817601";
      client.createSnapshotAsync(parent, snapshot, snapshotId).get();
      Assert.fail("No exception raised");
    } catch (ExecutionException e) {
      Assert.assertEquals(InvalidArgumentException.class, e.getCause().getClass());
      InvalidArgumentException apiException = ((InvalidArgumentException) e.getCause());
      Assert.assertEquals(StatusCode.Code.INVALID_ARGUMENT, apiException.getStatusCode().getCode());
    }
  }

  @Test
  public void createSnapshotTest2() throws Exception {
    Snapshot expectedResponse =
        Snapshot.newBuilder()
            .setName(
                SnapshotName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]", "[SNAPSHOT]").toString())
            .setDescription("description-1724546052")
            .setCreateTime(Timestamp.newBuilder().build())
            .putAllLabels(new HashMap<String, String>())
            .setFilesystemUsedBytes(387235709)
            .build();
    Operation resultOperation =
        Operation.newBuilder()
            .setName("createSnapshotTest")
            .setDone(true)
            .setResponse(Any.pack(expectedResponse))
            .build();
    mockCloudFilestoreManager.addResponse(resultOperation);

    String parent = "parent-995424086";
    Snapshot snapshot = Snapshot.newBuilder().build();
    String snapshotId = "snapshotId-1113817601";

    Snapshot actualResponse = client.createSnapshotAsync(parent, snapshot, snapshotId).get();
    Assert.assertEquals(expectedResponse, actualResponse);

    List<AbstractMessage> actualRequests = mockCloudFilestoreManager.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    CreateSnapshotRequest actualRequest = ((CreateSnapshotRequest) actualRequests.get(0));

    Assert.assertEquals(parent, actualRequest.getParent());
    Assert.assertEquals(snapshot, actualRequest.getSnapshot());
    Assert.assertEquals(snapshotId, actualRequest.getSnapshotId());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void createSnapshotExceptionTest2() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockCloudFilestoreManager.addException(exception);

    try {
      String parent = "parent-995424086";
      Snapshot snapshot = Snapshot.newBuilder().build();
      String snapshotId = "snapshotId-1113817601";
      client.createSnapshotAsync(parent, snapshot, snapshotId).get();
      Assert.fail("No exception raised");
    } catch (ExecutionException e) {
      Assert.assertEquals(InvalidArgumentException.class, e.getCause().getClass());
      InvalidArgumentException apiException = ((InvalidArgumentException) e.getCause());
      Assert.assertEquals(StatusCode.Code.INVALID_ARGUMENT, apiException.getStatusCode().getCode());
    }
  }

  @Test
  public void deleteSnapshotTest() throws Exception {
    Empty expectedResponse = Empty.newBuilder().build();
    Operation resultOperation =
        Operation.newBuilder()
            .setName("deleteSnapshotTest")
            .setDone(true)
            .setResponse(Any.pack(expectedResponse))
            .build();
    mockCloudFilestoreManager.addResponse(resultOperation);

    SnapshotName name = SnapshotName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]", "[SNAPSHOT]");

    client.deleteSnapshotAsync(name).get();

    List<AbstractMessage> actualRequests = mockCloudFilestoreManager.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    DeleteSnapshotRequest actualRequest = ((DeleteSnapshotRequest) actualRequests.get(0));

    Assert.assertEquals(name.toString(), actualRequest.getName());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void deleteSnapshotExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockCloudFilestoreManager.addException(exception);

    try {
      SnapshotName name = SnapshotName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]", "[SNAPSHOT]");
      client.deleteSnapshotAsync(name).get();
      Assert.fail("No exception raised");
    } catch (ExecutionException e) {
      Assert.assertEquals(InvalidArgumentException.class, e.getCause().getClass());
      InvalidArgumentException apiException = ((InvalidArgumentException) e.getCause());
      Assert.assertEquals(StatusCode.Code.INVALID_ARGUMENT, apiException.getStatusCode().getCode());
    }
  }

  @Test
  public void deleteSnapshotTest2() throws Exception {
    Empty expectedResponse = Empty.newBuilder().build();
    Operation resultOperation =
        Operation.newBuilder()
            .setName("deleteSnapshotTest")
            .setDone(true)
            .setResponse(Any.pack(expectedResponse))
            .build();
    mockCloudFilestoreManager.addResponse(resultOperation);

    String name = "name3373707";

    client.deleteSnapshotAsync(name).get();

    List<AbstractMessage> actualRequests = mockCloudFilestoreManager.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    DeleteSnapshotRequest actualRequest = ((DeleteSnapshotRequest) actualRequests.get(0));

    Assert.assertEquals(name, actualRequest.getName());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void deleteSnapshotExceptionTest2() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockCloudFilestoreManager.addException(exception);

    try {
      String name = "name3373707";
      client.deleteSnapshotAsync(name).get();
      Assert.fail("No exception raised");
    } catch (ExecutionException e) {
      Assert.assertEquals(InvalidArgumentException.class, e.getCause().getClass());
      InvalidArgumentException apiException = ((InvalidArgumentException) e.getCause());
      Assert.assertEquals(StatusCode.Code.INVALID_ARGUMENT, apiException.getStatusCode().getCode());
    }
  }

  @Test
  public void updateSnapshotTest() throws Exception {
    Snapshot expectedResponse =
        Snapshot.newBuilder()
            .setName(
                SnapshotName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]", "[SNAPSHOT]").toString())
            .setDescription("description-1724546052")
            .setCreateTime(Timestamp.newBuilder().build())
            .putAllLabels(new HashMap<String, String>())
            .setFilesystemUsedBytes(387235709)
            .build();
    Operation resultOperation =
        Operation.newBuilder()
            .setName("updateSnapshotTest")
            .setDone(true)
            .setResponse(Any.pack(expectedResponse))
            .build();
    mockCloudFilestoreManager.addResponse(resultOperation);

    Snapshot snapshot = Snapshot.newBuilder().build();
    FieldMask updateMask = FieldMask.newBuilder().build();

    Snapshot actualResponse = client.updateSnapshotAsync(snapshot, updateMask).get();
    Assert.assertEquals(expectedResponse, actualResponse);

    List<AbstractMessage> actualRequests = mockCloudFilestoreManager.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    UpdateSnapshotRequest actualRequest = ((UpdateSnapshotRequest) actualRequests.get(0));

    Assert.assertEquals(snapshot, actualRequest.getSnapshot());
    Assert.assertEquals(updateMask, actualRequest.getUpdateMask());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void updateSnapshotExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockCloudFilestoreManager.addException(exception);

    try {
      Snapshot snapshot = Snapshot.newBuilder().build();
      FieldMask updateMask = FieldMask.newBuilder().build();
      client.updateSnapshotAsync(snapshot, updateMask).get();
      Assert.fail("No exception raised");
    } catch (ExecutionException e) {
      Assert.assertEquals(InvalidArgumentException.class, e.getCause().getClass());
      InvalidArgumentException apiException = ((InvalidArgumentException) e.getCause());
      Assert.assertEquals(StatusCode.Code.INVALID_ARGUMENT, apiException.getStatusCode().getCode());
    }
  }

  @Test
  public void listBackupsTest() throws Exception {
    Backup responsesElement = Backup.newBuilder().build();
    ListBackupsResponse expectedResponse =
        ListBackupsResponse.newBuilder()
            .setNextPageToken("")
            .addAllBackups(Arrays.asList(responsesElement))
            .build();
    mockCloudFilestoreManager.addResponse(expectedResponse);

    LocationName parent = LocationName.of("[PROJECT]", "[LOCATION]");

    ListBackupsPagedResponse pagedListResponse = client.listBackups(parent);

    List<Backup> resources = Lists.newArrayList(pagedListResponse.iterateAll());

    Assert.assertEquals(1, resources.size());
    Assert.assertEquals(expectedResponse.getBackupsList().get(0), resources.get(0));

    List<AbstractMessage> actualRequests = mockCloudFilestoreManager.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    ListBackupsRequest actualRequest = ((ListBackupsRequest) actualRequests.get(0));

    Assert.assertEquals(parent.toString(), actualRequest.getParent());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void listBackupsExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockCloudFilestoreManager.addException(exception);

    try {
      LocationName parent = LocationName.of("[PROJECT]", "[LOCATION]");
      client.listBackups(parent);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception.
    }
  }

  @Test
  public void listBackupsTest2() throws Exception {
    Backup responsesElement = Backup.newBuilder().build();
    ListBackupsResponse expectedResponse =
        ListBackupsResponse.newBuilder()
            .setNextPageToken("")
            .addAllBackups(Arrays.asList(responsesElement))
            .build();
    mockCloudFilestoreManager.addResponse(expectedResponse);

    String parent = "parent-995424086";

    ListBackupsPagedResponse pagedListResponse = client.listBackups(parent);

    List<Backup> resources = Lists.newArrayList(pagedListResponse.iterateAll());

    Assert.assertEquals(1, resources.size());
    Assert.assertEquals(expectedResponse.getBackupsList().get(0), resources.get(0));

    List<AbstractMessage> actualRequests = mockCloudFilestoreManager.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    ListBackupsRequest actualRequest = ((ListBackupsRequest) actualRequests.get(0));

    Assert.assertEquals(parent, actualRequest.getParent());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void listBackupsExceptionTest2() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockCloudFilestoreManager.addException(exception);

    try {
      String parent = "parent-995424086";
      client.listBackups(parent);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception.
    }
  }

  @Test
  public void getBackupTest() throws Exception {
    Backup expectedResponse =
        Backup.newBuilder()
            .setName(BackupName.of("[PROJECT]", "[LOCATION]", "[BACKUP]").toString())
            .setDescription("description-1724546052")
            .setCreateTime(Timestamp.newBuilder().build())
            .putAllLabels(new HashMap<String, String>())
            .setCapacityGb(-1923585408)
            .setStorageBytes(2035244455)
            .setSourceInstance(InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]").toString())
            .setSourceFileShare("sourceFileShare-646832664")
            .setDownloadBytes(971924980)
            .setSatisfiesPzs(BoolValue.newBuilder().build())
            .setKmsKeyName("kmsKeyName412586233")
            .build();
    mockCloudFilestoreManager.addResponse(expectedResponse);

    BackupName name = BackupName.of("[PROJECT]", "[LOCATION]", "[BACKUP]");

    Backup actualResponse = client.getBackup(name);
    Assert.assertEquals(expectedResponse, actualResponse);

    List<AbstractMessage> actualRequests = mockCloudFilestoreManager.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    GetBackupRequest actualRequest = ((GetBackupRequest) actualRequests.get(0));

    Assert.assertEquals(name.toString(), actualRequest.getName());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void getBackupExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockCloudFilestoreManager.addException(exception);

    try {
      BackupName name = BackupName.of("[PROJECT]", "[LOCATION]", "[BACKUP]");
      client.getBackup(name);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception.
    }
  }

  @Test
  public void getBackupTest2() throws Exception {
    Backup expectedResponse =
        Backup.newBuilder()
            .setName(BackupName.of("[PROJECT]", "[LOCATION]", "[BACKUP]").toString())
            .setDescription("description-1724546052")
            .setCreateTime(Timestamp.newBuilder().build())
            .putAllLabels(new HashMap<String, String>())
            .setCapacityGb(-1923585408)
            .setStorageBytes(2035244455)
            .setSourceInstance(InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]").toString())
            .setSourceFileShare("sourceFileShare-646832664")
            .setDownloadBytes(971924980)
            .setSatisfiesPzs(BoolValue.newBuilder().build())
            .setKmsKeyName("kmsKeyName412586233")
            .build();
    mockCloudFilestoreManager.addResponse(expectedResponse);

    String name = "name3373707";

    Backup actualResponse = client.getBackup(name);
    Assert.assertEquals(expectedResponse, actualResponse);

    List<AbstractMessage> actualRequests = mockCloudFilestoreManager.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    GetBackupRequest actualRequest = ((GetBackupRequest) actualRequests.get(0));

    Assert.assertEquals(name, actualRequest.getName());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void getBackupExceptionTest2() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockCloudFilestoreManager.addException(exception);

    try {
      String name = "name3373707";
      client.getBackup(name);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception.
    }
  }

  @Test
  public void createBackupTest() throws Exception {
    Backup expectedResponse =
        Backup.newBuilder()
            .setName(BackupName.of("[PROJECT]", "[LOCATION]", "[BACKUP]").toString())
            .setDescription("description-1724546052")
            .setCreateTime(Timestamp.newBuilder().build())
            .putAllLabels(new HashMap<String, String>())
            .setCapacityGb(-1923585408)
            .setStorageBytes(2035244455)
            .setSourceInstance(InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]").toString())
            .setSourceFileShare("sourceFileShare-646832664")
            .setDownloadBytes(971924980)
            .setSatisfiesPzs(BoolValue.newBuilder().build())
            .setKmsKeyName("kmsKeyName412586233")
            .build();
    Operation resultOperation =
        Operation.newBuilder()
            .setName("createBackupTest")
            .setDone(true)
            .setResponse(Any.pack(expectedResponse))
            .build();
    mockCloudFilestoreManager.addResponse(resultOperation);

    LocationName parent = LocationName.of("[PROJECT]", "[LOCATION]");
    Backup backup = Backup.newBuilder().build();
    String backupId = "backupId2121930365";

    Backup actualResponse = client.createBackupAsync(parent, backup, backupId).get();
    Assert.assertEquals(expectedResponse, actualResponse);

    List<AbstractMessage> actualRequests = mockCloudFilestoreManager.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    CreateBackupRequest actualRequest = ((CreateBackupRequest) actualRequests.get(0));

    Assert.assertEquals(parent.toString(), actualRequest.getParent());
    Assert.assertEquals(backup, actualRequest.getBackup());
    Assert.assertEquals(backupId, actualRequest.getBackupId());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void createBackupExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockCloudFilestoreManager.addException(exception);

    try {
      LocationName parent = LocationName.of("[PROJECT]", "[LOCATION]");
      Backup backup = Backup.newBuilder().build();
      String backupId = "backupId2121930365";
      client.createBackupAsync(parent, backup, backupId).get();
      Assert.fail("No exception raised");
    } catch (ExecutionException e) {
      Assert.assertEquals(InvalidArgumentException.class, e.getCause().getClass());
      InvalidArgumentException apiException = ((InvalidArgumentException) e.getCause());
      Assert.assertEquals(StatusCode.Code.INVALID_ARGUMENT, apiException.getStatusCode().getCode());
    }
  }

  @Test
  public void createBackupTest2() throws Exception {
    Backup expectedResponse =
        Backup.newBuilder()
            .setName(BackupName.of("[PROJECT]", "[LOCATION]", "[BACKUP]").toString())
            .setDescription("description-1724546052")
            .setCreateTime(Timestamp.newBuilder().build())
            .putAllLabels(new HashMap<String, String>())
            .setCapacityGb(-1923585408)
            .setStorageBytes(2035244455)
            .setSourceInstance(InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]").toString())
            .setSourceFileShare("sourceFileShare-646832664")
            .setDownloadBytes(971924980)
            .setSatisfiesPzs(BoolValue.newBuilder().build())
            .setKmsKeyName("kmsKeyName412586233")
            .build();
    Operation resultOperation =
        Operation.newBuilder()
            .setName("createBackupTest")
            .setDone(true)
            .setResponse(Any.pack(expectedResponse))
            .build();
    mockCloudFilestoreManager.addResponse(resultOperation);

    String parent = "parent-995424086";
    Backup backup = Backup.newBuilder().build();
    String backupId = "backupId2121930365";

    Backup actualResponse = client.createBackupAsync(parent, backup, backupId).get();
    Assert.assertEquals(expectedResponse, actualResponse);

    List<AbstractMessage> actualRequests = mockCloudFilestoreManager.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    CreateBackupRequest actualRequest = ((CreateBackupRequest) actualRequests.get(0));

    Assert.assertEquals(parent, actualRequest.getParent());
    Assert.assertEquals(backup, actualRequest.getBackup());
    Assert.assertEquals(backupId, actualRequest.getBackupId());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void createBackupExceptionTest2() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockCloudFilestoreManager.addException(exception);

    try {
      String parent = "parent-995424086";
      Backup backup = Backup.newBuilder().build();
      String backupId = "backupId2121930365";
      client.createBackupAsync(parent, backup, backupId).get();
      Assert.fail("No exception raised");
    } catch (ExecutionException e) {
      Assert.assertEquals(InvalidArgumentException.class, e.getCause().getClass());
      InvalidArgumentException apiException = ((InvalidArgumentException) e.getCause());
      Assert.assertEquals(StatusCode.Code.INVALID_ARGUMENT, apiException.getStatusCode().getCode());
    }
  }

  @Test
  public void deleteBackupTest() throws Exception {
    Empty expectedResponse = Empty.newBuilder().build();
    Operation resultOperation =
        Operation.newBuilder()
            .setName("deleteBackupTest")
            .setDone(true)
            .setResponse(Any.pack(expectedResponse))
            .build();
    mockCloudFilestoreManager.addResponse(resultOperation);

    BackupName name = BackupName.of("[PROJECT]", "[LOCATION]", "[BACKUP]");

    client.deleteBackupAsync(name).get();

    List<AbstractMessage> actualRequests = mockCloudFilestoreManager.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    DeleteBackupRequest actualRequest = ((DeleteBackupRequest) actualRequests.get(0));

    Assert.assertEquals(name.toString(), actualRequest.getName());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void deleteBackupExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockCloudFilestoreManager.addException(exception);

    try {
      BackupName name = BackupName.of("[PROJECT]", "[LOCATION]", "[BACKUP]");
      client.deleteBackupAsync(name).get();
      Assert.fail("No exception raised");
    } catch (ExecutionException e) {
      Assert.assertEquals(InvalidArgumentException.class, e.getCause().getClass());
      InvalidArgumentException apiException = ((InvalidArgumentException) e.getCause());
      Assert.assertEquals(StatusCode.Code.INVALID_ARGUMENT, apiException.getStatusCode().getCode());
    }
  }

  @Test
  public void deleteBackupTest2() throws Exception {
    Empty expectedResponse = Empty.newBuilder().build();
    Operation resultOperation =
        Operation.newBuilder()
            .setName("deleteBackupTest")
            .setDone(true)
            .setResponse(Any.pack(expectedResponse))
            .build();
    mockCloudFilestoreManager.addResponse(resultOperation);

    String name = "name3373707";

    client.deleteBackupAsync(name).get();

    List<AbstractMessage> actualRequests = mockCloudFilestoreManager.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    DeleteBackupRequest actualRequest = ((DeleteBackupRequest) actualRequests.get(0));

    Assert.assertEquals(name, actualRequest.getName());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void deleteBackupExceptionTest2() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockCloudFilestoreManager.addException(exception);

    try {
      String name = "name3373707";
      client.deleteBackupAsync(name).get();
      Assert.fail("No exception raised");
    } catch (ExecutionException e) {
      Assert.assertEquals(InvalidArgumentException.class, e.getCause().getClass());
      InvalidArgumentException apiException = ((InvalidArgumentException) e.getCause());
      Assert.assertEquals(StatusCode.Code.INVALID_ARGUMENT, apiException.getStatusCode().getCode());
    }
  }

  @Test
  public void updateBackupTest() throws Exception {
    Backup expectedResponse =
        Backup.newBuilder()
            .setName(BackupName.of("[PROJECT]", "[LOCATION]", "[BACKUP]").toString())
            .setDescription("description-1724546052")
            .setCreateTime(Timestamp.newBuilder().build())
            .putAllLabels(new HashMap<String, String>())
            .setCapacityGb(-1923585408)
            .setStorageBytes(2035244455)
            .setSourceInstance(InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]").toString())
            .setSourceFileShare("sourceFileShare-646832664")
            .setDownloadBytes(971924980)
            .setSatisfiesPzs(BoolValue.newBuilder().build())
            .setKmsKeyName("kmsKeyName412586233")
            .build();
    Operation resultOperation =
        Operation.newBuilder()
            .setName("updateBackupTest")
            .setDone(true)
            .setResponse(Any.pack(expectedResponse))
            .build();
    mockCloudFilestoreManager.addResponse(resultOperation);

    Backup backup = Backup.newBuilder().build();
    FieldMask updateMask = FieldMask.newBuilder().build();

    Backup actualResponse = client.updateBackupAsync(backup, updateMask).get();
    Assert.assertEquals(expectedResponse, actualResponse);

    List<AbstractMessage> actualRequests = mockCloudFilestoreManager.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    UpdateBackupRequest actualRequest = ((UpdateBackupRequest) actualRequests.get(0));

    Assert.assertEquals(backup, actualRequest.getBackup());
    Assert.assertEquals(updateMask, actualRequest.getUpdateMask());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void updateBackupExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockCloudFilestoreManager.addException(exception);

    try {
      Backup backup = Backup.newBuilder().build();
      FieldMask updateMask = FieldMask.newBuilder().build();
      client.updateBackupAsync(backup, updateMask).get();
      Assert.fail("No exception raised");
    } catch (ExecutionException e) {
      Assert.assertEquals(InvalidArgumentException.class, e.getCause().getClass());
      InvalidArgumentException apiException = ((InvalidArgumentException) e.getCause());
      Assert.assertEquals(StatusCode.Code.INVALID_ARGUMENT, apiException.getStatusCode().getCode());
    }
  }

  @Test
  public void listSharesTest() throws Exception {
    Share responsesElement = Share.newBuilder().build();
    ListSharesResponse expectedResponse =
        ListSharesResponse.newBuilder()
            .setNextPageToken("")
            .addAllShares(Arrays.asList(responsesElement))
            .build();
    mockCloudFilestoreManager.addResponse(expectedResponse);

    InstanceName parent = InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]");

    ListSharesPagedResponse pagedListResponse = client.listShares(parent);

    List<Share> resources = Lists.newArrayList(pagedListResponse.iterateAll());

    Assert.assertEquals(1, resources.size());
    Assert.assertEquals(expectedResponse.getSharesList().get(0), resources.get(0));

    List<AbstractMessage> actualRequests = mockCloudFilestoreManager.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    ListSharesRequest actualRequest = ((ListSharesRequest) actualRequests.get(0));

    Assert.assertEquals(parent.toString(), actualRequest.getParent());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void listSharesExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockCloudFilestoreManager.addException(exception);

    try {
      InstanceName parent = InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]");
      client.listShares(parent);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception.
    }
  }

  @Test
  public void listSharesTest2() throws Exception {
    Share responsesElement = Share.newBuilder().build();
    ListSharesResponse expectedResponse =
        ListSharesResponse.newBuilder()
            .setNextPageToken("")
            .addAllShares(Arrays.asList(responsesElement))
            .build();
    mockCloudFilestoreManager.addResponse(expectedResponse);

    String parent = "parent-995424086";

    ListSharesPagedResponse pagedListResponse = client.listShares(parent);

    List<Share> resources = Lists.newArrayList(pagedListResponse.iterateAll());

    Assert.assertEquals(1, resources.size());
    Assert.assertEquals(expectedResponse.getSharesList().get(0), resources.get(0));

    List<AbstractMessage> actualRequests = mockCloudFilestoreManager.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    ListSharesRequest actualRequest = ((ListSharesRequest) actualRequests.get(0));

    Assert.assertEquals(parent, actualRequest.getParent());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void listSharesExceptionTest2() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockCloudFilestoreManager.addException(exception);

    try {
      String parent = "parent-995424086";
      client.listShares(parent);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception.
    }
  }

  @Test
  public void getShareTest() throws Exception {
    Share expectedResponse =
        Share.newBuilder()
            .setName(ShareName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]", "[SHARE]").toString())
            .setMountName("mountName456881732")
            .setDescription("description-1724546052")
            .setCapacityGb(-1923585408)
            .addAllNfsExportOptions(new ArrayList<NfsExportOptions>())
            .setCreateTime(Timestamp.newBuilder().build())
            .putAllLabels(new HashMap<String, String>())
            .build();
    mockCloudFilestoreManager.addResponse(expectedResponse);

    ShareName name = ShareName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]", "[SHARE]");

    Share actualResponse = client.getShare(name);
    Assert.assertEquals(expectedResponse, actualResponse);

    List<AbstractMessage> actualRequests = mockCloudFilestoreManager.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    GetShareRequest actualRequest = ((GetShareRequest) actualRequests.get(0));

    Assert.assertEquals(name.toString(), actualRequest.getName());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void getShareExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockCloudFilestoreManager.addException(exception);

    try {
      ShareName name = ShareName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]", "[SHARE]");
      client.getShare(name);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception.
    }
  }

  @Test
  public void getShareTest2() throws Exception {
    Share expectedResponse =
        Share.newBuilder()
            .setName(ShareName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]", "[SHARE]").toString())
            .setMountName("mountName456881732")
            .setDescription("description-1724546052")
            .setCapacityGb(-1923585408)
            .addAllNfsExportOptions(new ArrayList<NfsExportOptions>())
            .setCreateTime(Timestamp.newBuilder().build())
            .putAllLabels(new HashMap<String, String>())
            .build();
    mockCloudFilestoreManager.addResponse(expectedResponse);

    String name = "name3373707";

    Share actualResponse = client.getShare(name);
    Assert.assertEquals(expectedResponse, actualResponse);

    List<AbstractMessage> actualRequests = mockCloudFilestoreManager.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    GetShareRequest actualRequest = ((GetShareRequest) actualRequests.get(0));

    Assert.assertEquals(name, actualRequest.getName());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void getShareExceptionTest2() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockCloudFilestoreManager.addException(exception);

    try {
      String name = "name3373707";
      client.getShare(name);
      Assert.fail("No exception raised");
    } catch (InvalidArgumentException e) {
      // Expected exception.
    }
  }

  @Test
  public void createShareTest() throws Exception {
    Share expectedResponse =
        Share.newBuilder()
            .setName(ShareName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]", "[SHARE]").toString())
            .setMountName("mountName456881732")
            .setDescription("description-1724546052")
            .setCapacityGb(-1923585408)
            .addAllNfsExportOptions(new ArrayList<NfsExportOptions>())
            .setCreateTime(Timestamp.newBuilder().build())
            .putAllLabels(new HashMap<String, String>())
            .build();
    Operation resultOperation =
        Operation.newBuilder()
            .setName("createShareTest")
            .setDone(true)
            .setResponse(Any.pack(expectedResponse))
            .build();
    mockCloudFilestoreManager.addResponse(resultOperation);

    InstanceName parent = InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]");
    Share share = Share.newBuilder().build();
    String shareId = "shareId2054217050";

    Share actualResponse = client.createShareAsync(parent, share, shareId).get();
    Assert.assertEquals(expectedResponse, actualResponse);

    List<AbstractMessage> actualRequests = mockCloudFilestoreManager.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    CreateShareRequest actualRequest = ((CreateShareRequest) actualRequests.get(0));

    Assert.assertEquals(parent.toString(), actualRequest.getParent());
    Assert.assertEquals(share, actualRequest.getShare());
    Assert.assertEquals(shareId, actualRequest.getShareId());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void createShareExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockCloudFilestoreManager.addException(exception);

    try {
      InstanceName parent = InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]");
      Share share = Share.newBuilder().build();
      String shareId = "shareId2054217050";
      client.createShareAsync(parent, share, shareId).get();
      Assert.fail("No exception raised");
    } catch (ExecutionException e) {
      Assert.assertEquals(InvalidArgumentException.class, e.getCause().getClass());
      InvalidArgumentException apiException = ((InvalidArgumentException) e.getCause());
      Assert.assertEquals(StatusCode.Code.INVALID_ARGUMENT, apiException.getStatusCode().getCode());
    }
  }

  @Test
  public void createShareTest2() throws Exception {
    Share expectedResponse =
        Share.newBuilder()
            .setName(ShareName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]", "[SHARE]").toString())
            .setMountName("mountName456881732")
            .setDescription("description-1724546052")
            .setCapacityGb(-1923585408)
            .addAllNfsExportOptions(new ArrayList<NfsExportOptions>())
            .setCreateTime(Timestamp.newBuilder().build())
            .putAllLabels(new HashMap<String, String>())
            .build();
    Operation resultOperation =
        Operation.newBuilder()
            .setName("createShareTest")
            .setDone(true)
            .setResponse(Any.pack(expectedResponse))
            .build();
    mockCloudFilestoreManager.addResponse(resultOperation);

    String parent = "parent-995424086";
    Share share = Share.newBuilder().build();
    String shareId = "shareId2054217050";

    Share actualResponse = client.createShareAsync(parent, share, shareId).get();
    Assert.assertEquals(expectedResponse, actualResponse);

    List<AbstractMessage> actualRequests = mockCloudFilestoreManager.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    CreateShareRequest actualRequest = ((CreateShareRequest) actualRequests.get(0));

    Assert.assertEquals(parent, actualRequest.getParent());
    Assert.assertEquals(share, actualRequest.getShare());
    Assert.assertEquals(shareId, actualRequest.getShareId());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void createShareExceptionTest2() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockCloudFilestoreManager.addException(exception);

    try {
      String parent = "parent-995424086";
      Share share = Share.newBuilder().build();
      String shareId = "shareId2054217050";
      client.createShareAsync(parent, share, shareId).get();
      Assert.fail("No exception raised");
    } catch (ExecutionException e) {
      Assert.assertEquals(InvalidArgumentException.class, e.getCause().getClass());
      InvalidArgumentException apiException = ((InvalidArgumentException) e.getCause());
      Assert.assertEquals(StatusCode.Code.INVALID_ARGUMENT, apiException.getStatusCode().getCode());
    }
  }

  @Test
  public void deleteShareTest() throws Exception {
    Empty expectedResponse = Empty.newBuilder().build();
    Operation resultOperation =
        Operation.newBuilder()
            .setName("deleteShareTest")
            .setDone(true)
            .setResponse(Any.pack(expectedResponse))
            .build();
    mockCloudFilestoreManager.addResponse(resultOperation);

    ShareName name = ShareName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]", "[SHARE]");

    client.deleteShareAsync(name).get();

    List<AbstractMessage> actualRequests = mockCloudFilestoreManager.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    DeleteShareRequest actualRequest = ((DeleteShareRequest) actualRequests.get(0));

    Assert.assertEquals(name.toString(), actualRequest.getName());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void deleteShareExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockCloudFilestoreManager.addException(exception);

    try {
      ShareName name = ShareName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]", "[SHARE]");
      client.deleteShareAsync(name).get();
      Assert.fail("No exception raised");
    } catch (ExecutionException e) {
      Assert.assertEquals(InvalidArgumentException.class, e.getCause().getClass());
      InvalidArgumentException apiException = ((InvalidArgumentException) e.getCause());
      Assert.assertEquals(StatusCode.Code.INVALID_ARGUMENT, apiException.getStatusCode().getCode());
    }
  }

  @Test
  public void deleteShareTest2() throws Exception {
    Empty expectedResponse = Empty.newBuilder().build();
    Operation resultOperation =
        Operation.newBuilder()
            .setName("deleteShareTest")
            .setDone(true)
            .setResponse(Any.pack(expectedResponse))
            .build();
    mockCloudFilestoreManager.addResponse(resultOperation);

    String name = "name3373707";

    client.deleteShareAsync(name).get();

    List<AbstractMessage> actualRequests = mockCloudFilestoreManager.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    DeleteShareRequest actualRequest = ((DeleteShareRequest) actualRequests.get(0));

    Assert.assertEquals(name, actualRequest.getName());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void deleteShareExceptionTest2() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockCloudFilestoreManager.addException(exception);

    try {
      String name = "name3373707";
      client.deleteShareAsync(name).get();
      Assert.fail("No exception raised");
    } catch (ExecutionException e) {
      Assert.assertEquals(InvalidArgumentException.class, e.getCause().getClass());
      InvalidArgumentException apiException = ((InvalidArgumentException) e.getCause());
      Assert.assertEquals(StatusCode.Code.INVALID_ARGUMENT, apiException.getStatusCode().getCode());
    }
  }

  @Test
  public void updateShareTest() throws Exception {
    Share expectedResponse =
        Share.newBuilder()
            .setName(ShareName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]", "[SHARE]").toString())
            .setMountName("mountName456881732")
            .setDescription("description-1724546052")
            .setCapacityGb(-1923585408)
            .addAllNfsExportOptions(new ArrayList<NfsExportOptions>())
            .setCreateTime(Timestamp.newBuilder().build())
            .putAllLabels(new HashMap<String, String>())
            .build();
    Operation resultOperation =
        Operation.newBuilder()
            .setName("updateShareTest")
            .setDone(true)
            .setResponse(Any.pack(expectedResponse))
            .build();
    mockCloudFilestoreManager.addResponse(resultOperation);

    Share share = Share.newBuilder().build();
    FieldMask updateMask = FieldMask.newBuilder().build();

    Share actualResponse = client.updateShareAsync(share, updateMask).get();
    Assert.assertEquals(expectedResponse, actualResponse);

    List<AbstractMessage> actualRequests = mockCloudFilestoreManager.getRequests();
    Assert.assertEquals(1, actualRequests.size());
    UpdateShareRequest actualRequest = ((UpdateShareRequest) actualRequests.get(0));

    Assert.assertEquals(share, actualRequest.getShare());
    Assert.assertEquals(updateMask, actualRequest.getUpdateMask());
    Assert.assertTrue(
        channelProvider.isHeaderSent(
            ApiClientHeaderProvider.getDefaultApiClientHeaderKey(),
            GaxGrpcProperties.getDefaultApiClientHeaderPattern()));
  }

  @Test
  public void updateShareExceptionTest() throws Exception {
    StatusRuntimeException exception = new StatusRuntimeException(io.grpc.Status.INVALID_ARGUMENT);
    mockCloudFilestoreManager.addException(exception);

    try {
      Share share = Share.newBuilder().build();
      FieldMask updateMask = FieldMask.newBuilder().build();
      client.updateShareAsync(share, updateMask).get();
      Assert.fail("No exception raised");
    } catch (ExecutionException e) {
      Assert.assertEquals(InvalidArgumentException.class, e.getCause().getClass());
      InvalidArgumentException apiException = ((InvalidArgumentException) e.getCause());
      Assert.assertEquals(StatusCode.Code.INVALID_ARGUMENT, apiException.getStatusCode().getCode());
    }
  }
}
