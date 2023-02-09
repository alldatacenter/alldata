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
import com.google.api.gax.httpjson.GaxHttpJsonProperties;
import com.google.api.gax.httpjson.testing.MockHttpService;
import com.google.api.gax.rpc.ApiClientHeaderProvider;
import com.google.api.gax.rpc.ApiException;
import com.google.api.gax.rpc.ApiExceptionFactory;
import com.google.api.gax.rpc.InvalidArgumentException;
import com.google.api.gax.rpc.StatusCode;
import com.google.api.gax.rpc.testing.FakeStatusCode;
import com.google.cloud.filestore.v1beta1.stub.HttpJsonCloudFilestoreManagerStub;
import com.google.common.collect.Lists;
import com.google.longrunning.Operation;
import com.google.protobuf.Any;
import com.google.protobuf.BoolValue;
import com.google.protobuf.Empty;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Timestamp;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.annotation.Generated;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

@Generated("by gapic-generator-java")
public class CloudFilestoreManagerClientHttpJsonTest {
  private static MockHttpService mockService;
  private static CloudFilestoreManagerClient client;

  @BeforeClass
  public static void startStaticServer() throws IOException {
    mockService =
        new MockHttpService(
            HttpJsonCloudFilestoreManagerStub.getMethodDescriptors(),
            CloudFilestoreManagerSettings.getDefaultEndpoint());
    CloudFilestoreManagerSettings settings =
        CloudFilestoreManagerSettings.newHttpJsonBuilder()
            .setTransportChannelProvider(
                CloudFilestoreManagerSettings.defaultHttpJsonTransportProviderBuilder()
                    .setHttpTransport(mockService)
                    .build())
            .setCredentialsProvider(NoCredentialsProvider.create())
            .build();
    client = CloudFilestoreManagerClient.create(settings);
  }

  @AfterClass
  public static void stopServer() {
    client.close();
  }

  @Before
  public void setUp() {}

  @After
  public void tearDown() throws Exception {
    mockService.reset();
  }

  @Test
  public void listInstancesTest() throws Exception {
    Instance responsesElement = Instance.newBuilder().build();
    ListInstancesResponse expectedResponse =
        ListInstancesResponse.newBuilder()
            .setNextPageToken("")
            .addAllInstances(Arrays.asList(responsesElement))
            .build();
    mockService.addResponse(expectedResponse);

    LocationName parent = LocationName.of("[PROJECT]", "[LOCATION]");

    ListInstancesPagedResponse pagedListResponse = client.listInstances(parent);

    List<Instance> resources = Lists.newArrayList(pagedListResponse.iterateAll());

    Assert.assertEquals(1, resources.size());
    Assert.assertEquals(expectedResponse.getInstancesList().get(0), resources.get(0));

    List<String> actualRequests = mockService.getRequestPaths();
    Assert.assertEquals(1, actualRequests.size());

    String apiClientHeaderKey =
        mockService
            .getRequestHeaders()
            .get(ApiClientHeaderProvider.getDefaultApiClientHeaderKey())
            .iterator()
            .next();
    Assert.assertTrue(
        GaxHttpJsonProperties.getDefaultApiClientHeaderPattern()
            .matcher(apiClientHeaderKey)
            .matches());
  }

  @Test
  public void listInstancesExceptionTest() throws Exception {
    ApiException exception =
        ApiExceptionFactory.createException(
            new Exception(), FakeStatusCode.of(StatusCode.Code.INVALID_ARGUMENT), false);
    mockService.addException(exception);

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
    mockService.addResponse(expectedResponse);

    String parent = "projects/project-5833/locations/location-5833";

    ListInstancesPagedResponse pagedListResponse = client.listInstances(parent);

    List<Instance> resources = Lists.newArrayList(pagedListResponse.iterateAll());

    Assert.assertEquals(1, resources.size());
    Assert.assertEquals(expectedResponse.getInstancesList().get(0), resources.get(0));

    List<String> actualRequests = mockService.getRequestPaths();
    Assert.assertEquals(1, actualRequests.size());

    String apiClientHeaderKey =
        mockService
            .getRequestHeaders()
            .get(ApiClientHeaderProvider.getDefaultApiClientHeaderKey())
            .iterator()
            .next();
    Assert.assertTrue(
        GaxHttpJsonProperties.getDefaultApiClientHeaderPattern()
            .matcher(apiClientHeaderKey)
            .matches());
  }

  @Test
  public void listInstancesExceptionTest2() throws Exception {
    ApiException exception =
        ApiExceptionFactory.createException(
            new Exception(), FakeStatusCode.of(StatusCode.Code.INVALID_ARGUMENT), false);
    mockService.addException(exception);

    try {
      String parent = "projects/project-5833/locations/location-5833";
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
    mockService.addResponse(expectedResponse);

    InstanceName name = InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]");

    Instance actualResponse = client.getInstance(name);
    Assert.assertEquals(expectedResponse, actualResponse);

    List<String> actualRequests = mockService.getRequestPaths();
    Assert.assertEquals(1, actualRequests.size());

    String apiClientHeaderKey =
        mockService
            .getRequestHeaders()
            .get(ApiClientHeaderProvider.getDefaultApiClientHeaderKey())
            .iterator()
            .next();
    Assert.assertTrue(
        GaxHttpJsonProperties.getDefaultApiClientHeaderPattern()
            .matcher(apiClientHeaderKey)
            .matches());
  }

  @Test
  public void getInstanceExceptionTest() throws Exception {
    ApiException exception =
        ApiExceptionFactory.createException(
            new Exception(), FakeStatusCode.of(StatusCode.Code.INVALID_ARGUMENT), false);
    mockService.addException(exception);

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
    mockService.addResponse(expectedResponse);

    String name = "projects/project-9412/locations/location-9412/instances/instance-9412";

    Instance actualResponse = client.getInstance(name);
    Assert.assertEquals(expectedResponse, actualResponse);

    List<String> actualRequests = mockService.getRequestPaths();
    Assert.assertEquals(1, actualRequests.size());

    String apiClientHeaderKey =
        mockService
            .getRequestHeaders()
            .get(ApiClientHeaderProvider.getDefaultApiClientHeaderKey())
            .iterator()
            .next();
    Assert.assertTrue(
        GaxHttpJsonProperties.getDefaultApiClientHeaderPattern()
            .matcher(apiClientHeaderKey)
            .matches());
  }

  @Test
  public void getInstanceExceptionTest2() throws Exception {
    ApiException exception =
        ApiExceptionFactory.createException(
            new Exception(), FakeStatusCode.of(StatusCode.Code.INVALID_ARGUMENT), false);
    mockService.addException(exception);

    try {
      String name = "projects/project-9412/locations/location-9412/instances/instance-9412";
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
    mockService.addResponse(resultOperation);

    LocationName parent = LocationName.of("[PROJECT]", "[LOCATION]");
    Instance instance = Instance.newBuilder().build();
    String instanceId = "instanceId902024336";

    Instance actualResponse = client.createInstanceAsync(parent, instance, instanceId).get();
    Assert.assertEquals(expectedResponse, actualResponse);

    List<String> actualRequests = mockService.getRequestPaths();
    Assert.assertEquals(1, actualRequests.size());

    String apiClientHeaderKey =
        mockService
            .getRequestHeaders()
            .get(ApiClientHeaderProvider.getDefaultApiClientHeaderKey())
            .iterator()
            .next();
    Assert.assertTrue(
        GaxHttpJsonProperties.getDefaultApiClientHeaderPattern()
            .matcher(apiClientHeaderKey)
            .matches());
  }

  @Test
  public void createInstanceExceptionTest() throws Exception {
    ApiException exception =
        ApiExceptionFactory.createException(
            new Exception(), FakeStatusCode.of(StatusCode.Code.INVALID_ARGUMENT), false);
    mockService.addException(exception);

    try {
      LocationName parent = LocationName.of("[PROJECT]", "[LOCATION]");
      Instance instance = Instance.newBuilder().build();
      String instanceId = "instanceId902024336";
      client.createInstanceAsync(parent, instance, instanceId).get();
      Assert.fail("No exception raised");
    } catch (ExecutionException e) {
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
    mockService.addResponse(resultOperation);

    String parent = "projects/project-5833/locations/location-5833";
    Instance instance = Instance.newBuilder().build();
    String instanceId = "instanceId902024336";

    Instance actualResponse = client.createInstanceAsync(parent, instance, instanceId).get();
    Assert.assertEquals(expectedResponse, actualResponse);

    List<String> actualRequests = mockService.getRequestPaths();
    Assert.assertEquals(1, actualRequests.size());

    String apiClientHeaderKey =
        mockService
            .getRequestHeaders()
            .get(ApiClientHeaderProvider.getDefaultApiClientHeaderKey())
            .iterator()
            .next();
    Assert.assertTrue(
        GaxHttpJsonProperties.getDefaultApiClientHeaderPattern()
            .matcher(apiClientHeaderKey)
            .matches());
  }

  @Test
  public void createInstanceExceptionTest2() throws Exception {
    ApiException exception =
        ApiExceptionFactory.createException(
            new Exception(), FakeStatusCode.of(StatusCode.Code.INVALID_ARGUMENT), false);
    mockService.addException(exception);

    try {
      String parent = "projects/project-5833/locations/location-5833";
      Instance instance = Instance.newBuilder().build();
      String instanceId = "instanceId902024336";
      client.createInstanceAsync(parent, instance, instanceId).get();
      Assert.fail("No exception raised");
    } catch (ExecutionException e) {
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
    mockService.addResponse(resultOperation);

    Instance instance =
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
    FieldMask updateMask = FieldMask.newBuilder().build();

    Instance actualResponse = client.updateInstanceAsync(instance, updateMask).get();
    Assert.assertEquals(expectedResponse, actualResponse);

    List<String> actualRequests = mockService.getRequestPaths();
    Assert.assertEquals(1, actualRequests.size());

    String apiClientHeaderKey =
        mockService
            .getRequestHeaders()
            .get(ApiClientHeaderProvider.getDefaultApiClientHeaderKey())
            .iterator()
            .next();
    Assert.assertTrue(
        GaxHttpJsonProperties.getDefaultApiClientHeaderPattern()
            .matcher(apiClientHeaderKey)
            .matches());
  }

  @Test
  public void updateInstanceExceptionTest() throws Exception {
    ApiException exception =
        ApiExceptionFactory.createException(
            new Exception(), FakeStatusCode.of(StatusCode.Code.INVALID_ARGUMENT), false);
    mockService.addException(exception);

    try {
      Instance instance =
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
      FieldMask updateMask = FieldMask.newBuilder().build();
      client.updateInstanceAsync(instance, updateMask).get();
      Assert.fail("No exception raised");
    } catch (ExecutionException e) {
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
    mockService.addResponse(resultOperation);

    RestoreInstanceRequest request =
        RestoreInstanceRequest.newBuilder()
            .setName(InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]").toString())
            .setFileShare("fileShare-1327728701")
            .build();

    Instance actualResponse = client.restoreInstanceAsync(request).get();
    Assert.assertEquals(expectedResponse, actualResponse);

    List<String> actualRequests = mockService.getRequestPaths();
    Assert.assertEquals(1, actualRequests.size());

    String apiClientHeaderKey =
        mockService
            .getRequestHeaders()
            .get(ApiClientHeaderProvider.getDefaultApiClientHeaderKey())
            .iterator()
            .next();
    Assert.assertTrue(
        GaxHttpJsonProperties.getDefaultApiClientHeaderPattern()
            .matcher(apiClientHeaderKey)
            .matches());
  }

  @Test
  public void restoreInstanceExceptionTest() throws Exception {
    ApiException exception =
        ApiExceptionFactory.createException(
            new Exception(), FakeStatusCode.of(StatusCode.Code.INVALID_ARGUMENT), false);
    mockService.addException(exception);

    try {
      RestoreInstanceRequest request =
          RestoreInstanceRequest.newBuilder()
              .setName(InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]").toString())
              .setFileShare("fileShare-1327728701")
              .build();
      client.restoreInstanceAsync(request).get();
      Assert.fail("No exception raised");
    } catch (ExecutionException e) {
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
    mockService.addResponse(resultOperation);

    RevertInstanceRequest request =
        RevertInstanceRequest.newBuilder()
            .setName(InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]").toString())
            .setTargetSnapshotId("targetSnapshotId-2099459472")
            .build();

    Instance actualResponse = client.revertInstanceAsync(request).get();
    Assert.assertEquals(expectedResponse, actualResponse);

    List<String> actualRequests = mockService.getRequestPaths();
    Assert.assertEquals(1, actualRequests.size());

    String apiClientHeaderKey =
        mockService
            .getRequestHeaders()
            .get(ApiClientHeaderProvider.getDefaultApiClientHeaderKey())
            .iterator()
            .next();
    Assert.assertTrue(
        GaxHttpJsonProperties.getDefaultApiClientHeaderPattern()
            .matcher(apiClientHeaderKey)
            .matches());
  }

  @Test
  public void revertInstanceExceptionTest() throws Exception {
    ApiException exception =
        ApiExceptionFactory.createException(
            new Exception(), FakeStatusCode.of(StatusCode.Code.INVALID_ARGUMENT), false);
    mockService.addException(exception);

    try {
      RevertInstanceRequest request =
          RevertInstanceRequest.newBuilder()
              .setName(InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]").toString())
              .setTargetSnapshotId("targetSnapshotId-2099459472")
              .build();
      client.revertInstanceAsync(request).get();
      Assert.fail("No exception raised");
    } catch (ExecutionException e) {
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
    mockService.addResponse(resultOperation);

    InstanceName name = InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]");

    client.deleteInstanceAsync(name).get();

    List<String> actualRequests = mockService.getRequestPaths();
    Assert.assertEquals(1, actualRequests.size());

    String apiClientHeaderKey =
        mockService
            .getRequestHeaders()
            .get(ApiClientHeaderProvider.getDefaultApiClientHeaderKey())
            .iterator()
            .next();
    Assert.assertTrue(
        GaxHttpJsonProperties.getDefaultApiClientHeaderPattern()
            .matcher(apiClientHeaderKey)
            .matches());
  }

  @Test
  public void deleteInstanceExceptionTest() throws Exception {
    ApiException exception =
        ApiExceptionFactory.createException(
            new Exception(), FakeStatusCode.of(StatusCode.Code.INVALID_ARGUMENT), false);
    mockService.addException(exception);

    try {
      InstanceName name = InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]");
      client.deleteInstanceAsync(name).get();
      Assert.fail("No exception raised");
    } catch (ExecutionException e) {
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
    mockService.addResponse(resultOperation);

    String name = "projects/project-9412/locations/location-9412/instances/instance-9412";

    client.deleteInstanceAsync(name).get();

    List<String> actualRequests = mockService.getRequestPaths();
    Assert.assertEquals(1, actualRequests.size());

    String apiClientHeaderKey =
        mockService
            .getRequestHeaders()
            .get(ApiClientHeaderProvider.getDefaultApiClientHeaderKey())
            .iterator()
            .next();
    Assert.assertTrue(
        GaxHttpJsonProperties.getDefaultApiClientHeaderPattern()
            .matcher(apiClientHeaderKey)
            .matches());
  }

  @Test
  public void deleteInstanceExceptionTest2() throws Exception {
    ApiException exception =
        ApiExceptionFactory.createException(
            new Exception(), FakeStatusCode.of(StatusCode.Code.INVALID_ARGUMENT), false);
    mockService.addException(exception);

    try {
      String name = "projects/project-9412/locations/location-9412/instances/instance-9412";
      client.deleteInstanceAsync(name).get();
      Assert.fail("No exception raised");
    } catch (ExecutionException e) {
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
    mockService.addResponse(expectedResponse);

    InstanceName parent = InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]");

    ListSnapshotsPagedResponse pagedListResponse = client.listSnapshots(parent);

    List<Snapshot> resources = Lists.newArrayList(pagedListResponse.iterateAll());

    Assert.assertEquals(1, resources.size());
    Assert.assertEquals(expectedResponse.getSnapshotsList().get(0), resources.get(0));

    List<String> actualRequests = mockService.getRequestPaths();
    Assert.assertEquals(1, actualRequests.size());

    String apiClientHeaderKey =
        mockService
            .getRequestHeaders()
            .get(ApiClientHeaderProvider.getDefaultApiClientHeaderKey())
            .iterator()
            .next();
    Assert.assertTrue(
        GaxHttpJsonProperties.getDefaultApiClientHeaderPattern()
            .matcher(apiClientHeaderKey)
            .matches());
  }

  @Test
  public void listSnapshotsExceptionTest() throws Exception {
    ApiException exception =
        ApiExceptionFactory.createException(
            new Exception(), FakeStatusCode.of(StatusCode.Code.INVALID_ARGUMENT), false);
    mockService.addException(exception);

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
    mockService.addResponse(expectedResponse);

    String parent = "projects/project-5197/locations/location-5197/instances/instance-5197";

    ListSnapshotsPagedResponse pagedListResponse = client.listSnapshots(parent);

    List<Snapshot> resources = Lists.newArrayList(pagedListResponse.iterateAll());

    Assert.assertEquals(1, resources.size());
    Assert.assertEquals(expectedResponse.getSnapshotsList().get(0), resources.get(0));

    List<String> actualRequests = mockService.getRequestPaths();
    Assert.assertEquals(1, actualRequests.size());

    String apiClientHeaderKey =
        mockService
            .getRequestHeaders()
            .get(ApiClientHeaderProvider.getDefaultApiClientHeaderKey())
            .iterator()
            .next();
    Assert.assertTrue(
        GaxHttpJsonProperties.getDefaultApiClientHeaderPattern()
            .matcher(apiClientHeaderKey)
            .matches());
  }

  @Test
  public void listSnapshotsExceptionTest2() throws Exception {
    ApiException exception =
        ApiExceptionFactory.createException(
            new Exception(), FakeStatusCode.of(StatusCode.Code.INVALID_ARGUMENT), false);
    mockService.addException(exception);

    try {
      String parent = "projects/project-5197/locations/location-5197/instances/instance-5197";
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
    mockService.addResponse(expectedResponse);

    SnapshotName name = SnapshotName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]", "[SNAPSHOT]");

    Snapshot actualResponse = client.getSnapshot(name);
    Assert.assertEquals(expectedResponse, actualResponse);

    List<String> actualRequests = mockService.getRequestPaths();
    Assert.assertEquals(1, actualRequests.size());

    String apiClientHeaderKey =
        mockService
            .getRequestHeaders()
            .get(ApiClientHeaderProvider.getDefaultApiClientHeaderKey())
            .iterator()
            .next();
    Assert.assertTrue(
        GaxHttpJsonProperties.getDefaultApiClientHeaderPattern()
            .matcher(apiClientHeaderKey)
            .matches());
  }

  @Test
  public void getSnapshotExceptionTest() throws Exception {
    ApiException exception =
        ApiExceptionFactory.createException(
            new Exception(), FakeStatusCode.of(StatusCode.Code.INVALID_ARGUMENT), false);
    mockService.addException(exception);

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
    mockService.addResponse(expectedResponse);

    String name =
        "projects/project-2497/locations/location-2497/instances/instance-2497/snapshots/snapshot-2497";

    Snapshot actualResponse = client.getSnapshot(name);
    Assert.assertEquals(expectedResponse, actualResponse);

    List<String> actualRequests = mockService.getRequestPaths();
    Assert.assertEquals(1, actualRequests.size());

    String apiClientHeaderKey =
        mockService
            .getRequestHeaders()
            .get(ApiClientHeaderProvider.getDefaultApiClientHeaderKey())
            .iterator()
            .next();
    Assert.assertTrue(
        GaxHttpJsonProperties.getDefaultApiClientHeaderPattern()
            .matcher(apiClientHeaderKey)
            .matches());
  }

  @Test
  public void getSnapshotExceptionTest2() throws Exception {
    ApiException exception =
        ApiExceptionFactory.createException(
            new Exception(), FakeStatusCode.of(StatusCode.Code.INVALID_ARGUMENT), false);
    mockService.addException(exception);

    try {
      String name =
          "projects/project-2497/locations/location-2497/instances/instance-2497/snapshots/snapshot-2497";
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
    mockService.addResponse(resultOperation);

    InstanceName parent = InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]");
    Snapshot snapshot = Snapshot.newBuilder().build();
    String snapshotId = "snapshotId-1113817601";

    Snapshot actualResponse = client.createSnapshotAsync(parent, snapshot, snapshotId).get();
    Assert.assertEquals(expectedResponse, actualResponse);

    List<String> actualRequests = mockService.getRequestPaths();
    Assert.assertEquals(1, actualRequests.size());

    String apiClientHeaderKey =
        mockService
            .getRequestHeaders()
            .get(ApiClientHeaderProvider.getDefaultApiClientHeaderKey())
            .iterator()
            .next();
    Assert.assertTrue(
        GaxHttpJsonProperties.getDefaultApiClientHeaderPattern()
            .matcher(apiClientHeaderKey)
            .matches());
  }

  @Test
  public void createSnapshotExceptionTest() throws Exception {
    ApiException exception =
        ApiExceptionFactory.createException(
            new Exception(), FakeStatusCode.of(StatusCode.Code.INVALID_ARGUMENT), false);
    mockService.addException(exception);

    try {
      InstanceName parent = InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]");
      Snapshot snapshot = Snapshot.newBuilder().build();
      String snapshotId = "snapshotId-1113817601";
      client.createSnapshotAsync(parent, snapshot, snapshotId).get();
      Assert.fail("No exception raised");
    } catch (ExecutionException e) {
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
    mockService.addResponse(resultOperation);

    String parent = "projects/project-5197/locations/location-5197/instances/instance-5197";
    Snapshot snapshot = Snapshot.newBuilder().build();
    String snapshotId = "snapshotId-1113817601";

    Snapshot actualResponse = client.createSnapshotAsync(parent, snapshot, snapshotId).get();
    Assert.assertEquals(expectedResponse, actualResponse);

    List<String> actualRequests = mockService.getRequestPaths();
    Assert.assertEquals(1, actualRequests.size());

    String apiClientHeaderKey =
        mockService
            .getRequestHeaders()
            .get(ApiClientHeaderProvider.getDefaultApiClientHeaderKey())
            .iterator()
            .next();
    Assert.assertTrue(
        GaxHttpJsonProperties.getDefaultApiClientHeaderPattern()
            .matcher(apiClientHeaderKey)
            .matches());
  }

  @Test
  public void createSnapshotExceptionTest2() throws Exception {
    ApiException exception =
        ApiExceptionFactory.createException(
            new Exception(), FakeStatusCode.of(StatusCode.Code.INVALID_ARGUMENT), false);
    mockService.addException(exception);

    try {
      String parent = "projects/project-5197/locations/location-5197/instances/instance-5197";
      Snapshot snapshot = Snapshot.newBuilder().build();
      String snapshotId = "snapshotId-1113817601";
      client.createSnapshotAsync(parent, snapshot, snapshotId).get();
      Assert.fail("No exception raised");
    } catch (ExecutionException e) {
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
    mockService.addResponse(resultOperation);

    SnapshotName name = SnapshotName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]", "[SNAPSHOT]");

    client.deleteSnapshotAsync(name).get();

    List<String> actualRequests = mockService.getRequestPaths();
    Assert.assertEquals(1, actualRequests.size());

    String apiClientHeaderKey =
        mockService
            .getRequestHeaders()
            .get(ApiClientHeaderProvider.getDefaultApiClientHeaderKey())
            .iterator()
            .next();
    Assert.assertTrue(
        GaxHttpJsonProperties.getDefaultApiClientHeaderPattern()
            .matcher(apiClientHeaderKey)
            .matches());
  }

  @Test
  public void deleteSnapshotExceptionTest() throws Exception {
    ApiException exception =
        ApiExceptionFactory.createException(
            new Exception(), FakeStatusCode.of(StatusCode.Code.INVALID_ARGUMENT), false);
    mockService.addException(exception);

    try {
      SnapshotName name = SnapshotName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]", "[SNAPSHOT]");
      client.deleteSnapshotAsync(name).get();
      Assert.fail("No exception raised");
    } catch (ExecutionException e) {
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
    mockService.addResponse(resultOperation);

    String name =
        "projects/project-2497/locations/location-2497/instances/instance-2497/snapshots/snapshot-2497";

    client.deleteSnapshotAsync(name).get();

    List<String> actualRequests = mockService.getRequestPaths();
    Assert.assertEquals(1, actualRequests.size());

    String apiClientHeaderKey =
        mockService
            .getRequestHeaders()
            .get(ApiClientHeaderProvider.getDefaultApiClientHeaderKey())
            .iterator()
            .next();
    Assert.assertTrue(
        GaxHttpJsonProperties.getDefaultApiClientHeaderPattern()
            .matcher(apiClientHeaderKey)
            .matches());
  }

  @Test
  public void deleteSnapshotExceptionTest2() throws Exception {
    ApiException exception =
        ApiExceptionFactory.createException(
            new Exception(), FakeStatusCode.of(StatusCode.Code.INVALID_ARGUMENT), false);
    mockService.addException(exception);

    try {
      String name =
          "projects/project-2497/locations/location-2497/instances/instance-2497/snapshots/snapshot-2497";
      client.deleteSnapshotAsync(name).get();
      Assert.fail("No exception raised");
    } catch (ExecutionException e) {
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
    mockService.addResponse(resultOperation);

    Snapshot snapshot =
        Snapshot.newBuilder()
            .setName(
                SnapshotName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]", "[SNAPSHOT]").toString())
            .setDescription("description-1724546052")
            .setCreateTime(Timestamp.newBuilder().build())
            .putAllLabels(new HashMap<String, String>())
            .setFilesystemUsedBytes(387235709)
            .build();
    FieldMask updateMask = FieldMask.newBuilder().build();

    Snapshot actualResponse = client.updateSnapshotAsync(snapshot, updateMask).get();
    Assert.assertEquals(expectedResponse, actualResponse);

    List<String> actualRequests = mockService.getRequestPaths();
    Assert.assertEquals(1, actualRequests.size());

    String apiClientHeaderKey =
        mockService
            .getRequestHeaders()
            .get(ApiClientHeaderProvider.getDefaultApiClientHeaderKey())
            .iterator()
            .next();
    Assert.assertTrue(
        GaxHttpJsonProperties.getDefaultApiClientHeaderPattern()
            .matcher(apiClientHeaderKey)
            .matches());
  }

  @Test
  public void updateSnapshotExceptionTest() throws Exception {
    ApiException exception =
        ApiExceptionFactory.createException(
            new Exception(), FakeStatusCode.of(StatusCode.Code.INVALID_ARGUMENT), false);
    mockService.addException(exception);

    try {
      Snapshot snapshot =
          Snapshot.newBuilder()
              .setName(
                  SnapshotName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]", "[SNAPSHOT]").toString())
              .setDescription("description-1724546052")
              .setCreateTime(Timestamp.newBuilder().build())
              .putAllLabels(new HashMap<String, String>())
              .setFilesystemUsedBytes(387235709)
              .build();
      FieldMask updateMask = FieldMask.newBuilder().build();
      client.updateSnapshotAsync(snapshot, updateMask).get();
      Assert.fail("No exception raised");
    } catch (ExecutionException e) {
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
    mockService.addResponse(expectedResponse);

    LocationName parent = LocationName.of("[PROJECT]", "[LOCATION]");

    ListBackupsPagedResponse pagedListResponse = client.listBackups(parent);

    List<Backup> resources = Lists.newArrayList(pagedListResponse.iterateAll());

    Assert.assertEquals(1, resources.size());
    Assert.assertEquals(expectedResponse.getBackupsList().get(0), resources.get(0));

    List<String> actualRequests = mockService.getRequestPaths();
    Assert.assertEquals(1, actualRequests.size());

    String apiClientHeaderKey =
        mockService
            .getRequestHeaders()
            .get(ApiClientHeaderProvider.getDefaultApiClientHeaderKey())
            .iterator()
            .next();
    Assert.assertTrue(
        GaxHttpJsonProperties.getDefaultApiClientHeaderPattern()
            .matcher(apiClientHeaderKey)
            .matches());
  }

  @Test
  public void listBackupsExceptionTest() throws Exception {
    ApiException exception =
        ApiExceptionFactory.createException(
            new Exception(), FakeStatusCode.of(StatusCode.Code.INVALID_ARGUMENT), false);
    mockService.addException(exception);

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
    mockService.addResponse(expectedResponse);

    String parent = "projects/project-5833/locations/location-5833";

    ListBackupsPagedResponse pagedListResponse = client.listBackups(parent);

    List<Backup> resources = Lists.newArrayList(pagedListResponse.iterateAll());

    Assert.assertEquals(1, resources.size());
    Assert.assertEquals(expectedResponse.getBackupsList().get(0), resources.get(0));

    List<String> actualRequests = mockService.getRequestPaths();
    Assert.assertEquals(1, actualRequests.size());

    String apiClientHeaderKey =
        mockService
            .getRequestHeaders()
            .get(ApiClientHeaderProvider.getDefaultApiClientHeaderKey())
            .iterator()
            .next();
    Assert.assertTrue(
        GaxHttpJsonProperties.getDefaultApiClientHeaderPattern()
            .matcher(apiClientHeaderKey)
            .matches());
  }

  @Test
  public void listBackupsExceptionTest2() throws Exception {
    ApiException exception =
        ApiExceptionFactory.createException(
            new Exception(), FakeStatusCode.of(StatusCode.Code.INVALID_ARGUMENT), false);
    mockService.addException(exception);

    try {
      String parent = "projects/project-5833/locations/location-5833";
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
    mockService.addResponse(expectedResponse);

    BackupName name = BackupName.of("[PROJECT]", "[LOCATION]", "[BACKUP]");

    Backup actualResponse = client.getBackup(name);
    Assert.assertEquals(expectedResponse, actualResponse);

    List<String> actualRequests = mockService.getRequestPaths();
    Assert.assertEquals(1, actualRequests.size());

    String apiClientHeaderKey =
        mockService
            .getRequestHeaders()
            .get(ApiClientHeaderProvider.getDefaultApiClientHeaderKey())
            .iterator()
            .next();
    Assert.assertTrue(
        GaxHttpJsonProperties.getDefaultApiClientHeaderPattern()
            .matcher(apiClientHeaderKey)
            .matches());
  }

  @Test
  public void getBackupExceptionTest() throws Exception {
    ApiException exception =
        ApiExceptionFactory.createException(
            new Exception(), FakeStatusCode.of(StatusCode.Code.INVALID_ARGUMENT), false);
    mockService.addException(exception);

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
    mockService.addResponse(expectedResponse);

    String name = "projects/project-1607/locations/location-1607/backups/backup-1607";

    Backup actualResponse = client.getBackup(name);
    Assert.assertEquals(expectedResponse, actualResponse);

    List<String> actualRequests = mockService.getRequestPaths();
    Assert.assertEquals(1, actualRequests.size());

    String apiClientHeaderKey =
        mockService
            .getRequestHeaders()
            .get(ApiClientHeaderProvider.getDefaultApiClientHeaderKey())
            .iterator()
            .next();
    Assert.assertTrue(
        GaxHttpJsonProperties.getDefaultApiClientHeaderPattern()
            .matcher(apiClientHeaderKey)
            .matches());
  }

  @Test
  public void getBackupExceptionTest2() throws Exception {
    ApiException exception =
        ApiExceptionFactory.createException(
            new Exception(), FakeStatusCode.of(StatusCode.Code.INVALID_ARGUMENT), false);
    mockService.addException(exception);

    try {
      String name = "projects/project-1607/locations/location-1607/backups/backup-1607";
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
    mockService.addResponse(resultOperation);

    LocationName parent = LocationName.of("[PROJECT]", "[LOCATION]");
    Backup backup = Backup.newBuilder().build();
    String backupId = "backupId2121930365";

    Backup actualResponse = client.createBackupAsync(parent, backup, backupId).get();
    Assert.assertEquals(expectedResponse, actualResponse);

    List<String> actualRequests = mockService.getRequestPaths();
    Assert.assertEquals(1, actualRequests.size());

    String apiClientHeaderKey =
        mockService
            .getRequestHeaders()
            .get(ApiClientHeaderProvider.getDefaultApiClientHeaderKey())
            .iterator()
            .next();
    Assert.assertTrue(
        GaxHttpJsonProperties.getDefaultApiClientHeaderPattern()
            .matcher(apiClientHeaderKey)
            .matches());
  }

  @Test
  public void createBackupExceptionTest() throws Exception {
    ApiException exception =
        ApiExceptionFactory.createException(
            new Exception(), FakeStatusCode.of(StatusCode.Code.INVALID_ARGUMENT), false);
    mockService.addException(exception);

    try {
      LocationName parent = LocationName.of("[PROJECT]", "[LOCATION]");
      Backup backup = Backup.newBuilder().build();
      String backupId = "backupId2121930365";
      client.createBackupAsync(parent, backup, backupId).get();
      Assert.fail("No exception raised");
    } catch (ExecutionException e) {
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
    mockService.addResponse(resultOperation);

    String parent = "projects/project-5833/locations/location-5833";
    Backup backup = Backup.newBuilder().build();
    String backupId = "backupId2121930365";

    Backup actualResponse = client.createBackupAsync(parent, backup, backupId).get();
    Assert.assertEquals(expectedResponse, actualResponse);

    List<String> actualRequests = mockService.getRequestPaths();
    Assert.assertEquals(1, actualRequests.size());

    String apiClientHeaderKey =
        mockService
            .getRequestHeaders()
            .get(ApiClientHeaderProvider.getDefaultApiClientHeaderKey())
            .iterator()
            .next();
    Assert.assertTrue(
        GaxHttpJsonProperties.getDefaultApiClientHeaderPattern()
            .matcher(apiClientHeaderKey)
            .matches());
  }

  @Test
  public void createBackupExceptionTest2() throws Exception {
    ApiException exception =
        ApiExceptionFactory.createException(
            new Exception(), FakeStatusCode.of(StatusCode.Code.INVALID_ARGUMENT), false);
    mockService.addException(exception);

    try {
      String parent = "projects/project-5833/locations/location-5833";
      Backup backup = Backup.newBuilder().build();
      String backupId = "backupId2121930365";
      client.createBackupAsync(parent, backup, backupId).get();
      Assert.fail("No exception raised");
    } catch (ExecutionException e) {
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
    mockService.addResponse(resultOperation);

    BackupName name = BackupName.of("[PROJECT]", "[LOCATION]", "[BACKUP]");

    client.deleteBackupAsync(name).get();

    List<String> actualRequests = mockService.getRequestPaths();
    Assert.assertEquals(1, actualRequests.size());

    String apiClientHeaderKey =
        mockService
            .getRequestHeaders()
            .get(ApiClientHeaderProvider.getDefaultApiClientHeaderKey())
            .iterator()
            .next();
    Assert.assertTrue(
        GaxHttpJsonProperties.getDefaultApiClientHeaderPattern()
            .matcher(apiClientHeaderKey)
            .matches());
  }

  @Test
  public void deleteBackupExceptionTest() throws Exception {
    ApiException exception =
        ApiExceptionFactory.createException(
            new Exception(), FakeStatusCode.of(StatusCode.Code.INVALID_ARGUMENT), false);
    mockService.addException(exception);

    try {
      BackupName name = BackupName.of("[PROJECT]", "[LOCATION]", "[BACKUP]");
      client.deleteBackupAsync(name).get();
      Assert.fail("No exception raised");
    } catch (ExecutionException e) {
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
    mockService.addResponse(resultOperation);

    String name = "projects/project-1607/locations/location-1607/backups/backup-1607";

    client.deleteBackupAsync(name).get();

    List<String> actualRequests = mockService.getRequestPaths();
    Assert.assertEquals(1, actualRequests.size());

    String apiClientHeaderKey =
        mockService
            .getRequestHeaders()
            .get(ApiClientHeaderProvider.getDefaultApiClientHeaderKey())
            .iterator()
            .next();
    Assert.assertTrue(
        GaxHttpJsonProperties.getDefaultApiClientHeaderPattern()
            .matcher(apiClientHeaderKey)
            .matches());
  }

  @Test
  public void deleteBackupExceptionTest2() throws Exception {
    ApiException exception =
        ApiExceptionFactory.createException(
            new Exception(), FakeStatusCode.of(StatusCode.Code.INVALID_ARGUMENT), false);
    mockService.addException(exception);

    try {
      String name = "projects/project-1607/locations/location-1607/backups/backup-1607";
      client.deleteBackupAsync(name).get();
      Assert.fail("No exception raised");
    } catch (ExecutionException e) {
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
    mockService.addResponse(resultOperation);

    Backup backup =
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
    FieldMask updateMask = FieldMask.newBuilder().build();

    Backup actualResponse = client.updateBackupAsync(backup, updateMask).get();
    Assert.assertEquals(expectedResponse, actualResponse);

    List<String> actualRequests = mockService.getRequestPaths();
    Assert.assertEquals(1, actualRequests.size());

    String apiClientHeaderKey =
        mockService
            .getRequestHeaders()
            .get(ApiClientHeaderProvider.getDefaultApiClientHeaderKey())
            .iterator()
            .next();
    Assert.assertTrue(
        GaxHttpJsonProperties.getDefaultApiClientHeaderPattern()
            .matcher(apiClientHeaderKey)
            .matches());
  }

  @Test
  public void updateBackupExceptionTest() throws Exception {
    ApiException exception =
        ApiExceptionFactory.createException(
            new Exception(), FakeStatusCode.of(StatusCode.Code.INVALID_ARGUMENT), false);
    mockService.addException(exception);

    try {
      Backup backup =
          Backup.newBuilder()
              .setName(BackupName.of("[PROJECT]", "[LOCATION]", "[BACKUP]").toString())
              .setDescription("description-1724546052")
              .setCreateTime(Timestamp.newBuilder().build())
              .putAllLabels(new HashMap<String, String>())
              .setCapacityGb(-1923585408)
              .setStorageBytes(2035244455)
              .setSourceInstance(
                  InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]").toString())
              .setSourceFileShare("sourceFileShare-646832664")
              .setDownloadBytes(971924980)
              .setSatisfiesPzs(BoolValue.newBuilder().build())
              .setKmsKeyName("kmsKeyName412586233")
              .build();
      FieldMask updateMask = FieldMask.newBuilder().build();
      client.updateBackupAsync(backup, updateMask).get();
      Assert.fail("No exception raised");
    } catch (ExecutionException e) {
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
    mockService.addResponse(expectedResponse);

    InstanceName parent = InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]");

    ListSharesPagedResponse pagedListResponse = client.listShares(parent);

    List<Share> resources = Lists.newArrayList(pagedListResponse.iterateAll());

    Assert.assertEquals(1, resources.size());
    Assert.assertEquals(expectedResponse.getSharesList().get(0), resources.get(0));

    List<String> actualRequests = mockService.getRequestPaths();
    Assert.assertEquals(1, actualRequests.size());

    String apiClientHeaderKey =
        mockService
            .getRequestHeaders()
            .get(ApiClientHeaderProvider.getDefaultApiClientHeaderKey())
            .iterator()
            .next();
    Assert.assertTrue(
        GaxHttpJsonProperties.getDefaultApiClientHeaderPattern()
            .matcher(apiClientHeaderKey)
            .matches());
  }

  @Test
  public void listSharesExceptionTest() throws Exception {
    ApiException exception =
        ApiExceptionFactory.createException(
            new Exception(), FakeStatusCode.of(StatusCode.Code.INVALID_ARGUMENT), false);
    mockService.addException(exception);

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
    mockService.addResponse(expectedResponse);

    String parent = "projects/project-5197/locations/location-5197/instances/instance-5197";

    ListSharesPagedResponse pagedListResponse = client.listShares(parent);

    List<Share> resources = Lists.newArrayList(pagedListResponse.iterateAll());

    Assert.assertEquals(1, resources.size());
    Assert.assertEquals(expectedResponse.getSharesList().get(0), resources.get(0));

    List<String> actualRequests = mockService.getRequestPaths();
    Assert.assertEquals(1, actualRequests.size());

    String apiClientHeaderKey =
        mockService
            .getRequestHeaders()
            .get(ApiClientHeaderProvider.getDefaultApiClientHeaderKey())
            .iterator()
            .next();
    Assert.assertTrue(
        GaxHttpJsonProperties.getDefaultApiClientHeaderPattern()
            .matcher(apiClientHeaderKey)
            .matches());
  }

  @Test
  public void listSharesExceptionTest2() throws Exception {
    ApiException exception =
        ApiExceptionFactory.createException(
            new Exception(), FakeStatusCode.of(StatusCode.Code.INVALID_ARGUMENT), false);
    mockService.addException(exception);

    try {
      String parent = "projects/project-5197/locations/location-5197/instances/instance-5197";
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
    mockService.addResponse(expectedResponse);

    ShareName name = ShareName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]", "[SHARE]");

    Share actualResponse = client.getShare(name);
    Assert.assertEquals(expectedResponse, actualResponse);

    List<String> actualRequests = mockService.getRequestPaths();
    Assert.assertEquals(1, actualRequests.size());

    String apiClientHeaderKey =
        mockService
            .getRequestHeaders()
            .get(ApiClientHeaderProvider.getDefaultApiClientHeaderKey())
            .iterator()
            .next();
    Assert.assertTrue(
        GaxHttpJsonProperties.getDefaultApiClientHeaderPattern()
            .matcher(apiClientHeaderKey)
            .matches());
  }

  @Test
  public void getShareExceptionTest() throws Exception {
    ApiException exception =
        ApiExceptionFactory.createException(
            new Exception(), FakeStatusCode.of(StatusCode.Code.INVALID_ARGUMENT), false);
    mockService.addException(exception);

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
    mockService.addResponse(expectedResponse);

    String name =
        "projects/project-8454/locations/location-8454/instances/instance-8454/shares/share-8454";

    Share actualResponse = client.getShare(name);
    Assert.assertEquals(expectedResponse, actualResponse);

    List<String> actualRequests = mockService.getRequestPaths();
    Assert.assertEquals(1, actualRequests.size());

    String apiClientHeaderKey =
        mockService
            .getRequestHeaders()
            .get(ApiClientHeaderProvider.getDefaultApiClientHeaderKey())
            .iterator()
            .next();
    Assert.assertTrue(
        GaxHttpJsonProperties.getDefaultApiClientHeaderPattern()
            .matcher(apiClientHeaderKey)
            .matches());
  }

  @Test
  public void getShareExceptionTest2() throws Exception {
    ApiException exception =
        ApiExceptionFactory.createException(
            new Exception(), FakeStatusCode.of(StatusCode.Code.INVALID_ARGUMENT), false);
    mockService.addException(exception);

    try {
      String name =
          "projects/project-8454/locations/location-8454/instances/instance-8454/shares/share-8454";
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
    mockService.addResponse(resultOperation);

    InstanceName parent = InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]");
    Share share = Share.newBuilder().build();
    String shareId = "shareId2054217050";

    Share actualResponse = client.createShareAsync(parent, share, shareId).get();
    Assert.assertEquals(expectedResponse, actualResponse);

    List<String> actualRequests = mockService.getRequestPaths();
    Assert.assertEquals(1, actualRequests.size());

    String apiClientHeaderKey =
        mockService
            .getRequestHeaders()
            .get(ApiClientHeaderProvider.getDefaultApiClientHeaderKey())
            .iterator()
            .next();
    Assert.assertTrue(
        GaxHttpJsonProperties.getDefaultApiClientHeaderPattern()
            .matcher(apiClientHeaderKey)
            .matches());
  }

  @Test
  public void createShareExceptionTest() throws Exception {
    ApiException exception =
        ApiExceptionFactory.createException(
            new Exception(), FakeStatusCode.of(StatusCode.Code.INVALID_ARGUMENT), false);
    mockService.addException(exception);

    try {
      InstanceName parent = InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]");
      Share share = Share.newBuilder().build();
      String shareId = "shareId2054217050";
      client.createShareAsync(parent, share, shareId).get();
      Assert.fail("No exception raised");
    } catch (ExecutionException e) {
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
    mockService.addResponse(resultOperation);

    String parent = "projects/project-5197/locations/location-5197/instances/instance-5197";
    Share share = Share.newBuilder().build();
    String shareId = "shareId2054217050";

    Share actualResponse = client.createShareAsync(parent, share, shareId).get();
    Assert.assertEquals(expectedResponse, actualResponse);

    List<String> actualRequests = mockService.getRequestPaths();
    Assert.assertEquals(1, actualRequests.size());

    String apiClientHeaderKey =
        mockService
            .getRequestHeaders()
            .get(ApiClientHeaderProvider.getDefaultApiClientHeaderKey())
            .iterator()
            .next();
    Assert.assertTrue(
        GaxHttpJsonProperties.getDefaultApiClientHeaderPattern()
            .matcher(apiClientHeaderKey)
            .matches());
  }

  @Test
  public void createShareExceptionTest2() throws Exception {
    ApiException exception =
        ApiExceptionFactory.createException(
            new Exception(), FakeStatusCode.of(StatusCode.Code.INVALID_ARGUMENT), false);
    mockService.addException(exception);

    try {
      String parent = "projects/project-5197/locations/location-5197/instances/instance-5197";
      Share share = Share.newBuilder().build();
      String shareId = "shareId2054217050";
      client.createShareAsync(parent, share, shareId).get();
      Assert.fail("No exception raised");
    } catch (ExecutionException e) {
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
    mockService.addResponse(resultOperation);

    ShareName name = ShareName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]", "[SHARE]");

    client.deleteShareAsync(name).get();

    List<String> actualRequests = mockService.getRequestPaths();
    Assert.assertEquals(1, actualRequests.size());

    String apiClientHeaderKey =
        mockService
            .getRequestHeaders()
            .get(ApiClientHeaderProvider.getDefaultApiClientHeaderKey())
            .iterator()
            .next();
    Assert.assertTrue(
        GaxHttpJsonProperties.getDefaultApiClientHeaderPattern()
            .matcher(apiClientHeaderKey)
            .matches());
  }

  @Test
  public void deleteShareExceptionTest() throws Exception {
    ApiException exception =
        ApiExceptionFactory.createException(
            new Exception(), FakeStatusCode.of(StatusCode.Code.INVALID_ARGUMENT), false);
    mockService.addException(exception);

    try {
      ShareName name = ShareName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]", "[SHARE]");
      client.deleteShareAsync(name).get();
      Assert.fail("No exception raised");
    } catch (ExecutionException e) {
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
    mockService.addResponse(resultOperation);

    String name =
        "projects/project-8454/locations/location-8454/instances/instance-8454/shares/share-8454";

    client.deleteShareAsync(name).get();

    List<String> actualRequests = mockService.getRequestPaths();
    Assert.assertEquals(1, actualRequests.size());

    String apiClientHeaderKey =
        mockService
            .getRequestHeaders()
            .get(ApiClientHeaderProvider.getDefaultApiClientHeaderKey())
            .iterator()
            .next();
    Assert.assertTrue(
        GaxHttpJsonProperties.getDefaultApiClientHeaderPattern()
            .matcher(apiClientHeaderKey)
            .matches());
  }

  @Test
  public void deleteShareExceptionTest2() throws Exception {
    ApiException exception =
        ApiExceptionFactory.createException(
            new Exception(), FakeStatusCode.of(StatusCode.Code.INVALID_ARGUMENT), false);
    mockService.addException(exception);

    try {
      String name =
          "projects/project-8454/locations/location-8454/instances/instance-8454/shares/share-8454";
      client.deleteShareAsync(name).get();
      Assert.fail("No exception raised");
    } catch (ExecutionException e) {
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
    mockService.addResponse(resultOperation);

    Share share =
        Share.newBuilder()
            .setName(ShareName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]", "[SHARE]").toString())
            .setMountName("mountName456881732")
            .setDescription("description-1724546052")
            .setCapacityGb(-1923585408)
            .addAllNfsExportOptions(new ArrayList<NfsExportOptions>())
            .setCreateTime(Timestamp.newBuilder().build())
            .putAllLabels(new HashMap<String, String>())
            .build();
    FieldMask updateMask = FieldMask.newBuilder().build();

    Share actualResponse = client.updateShareAsync(share, updateMask).get();
    Assert.assertEquals(expectedResponse, actualResponse);

    List<String> actualRequests = mockService.getRequestPaths();
    Assert.assertEquals(1, actualRequests.size());

    String apiClientHeaderKey =
        mockService
            .getRequestHeaders()
            .get(ApiClientHeaderProvider.getDefaultApiClientHeaderKey())
            .iterator()
            .next();
    Assert.assertTrue(
        GaxHttpJsonProperties.getDefaultApiClientHeaderPattern()
            .matcher(apiClientHeaderKey)
            .matches());
  }

  @Test
  public void updateShareExceptionTest() throws Exception {
    ApiException exception =
        ApiExceptionFactory.createException(
            new Exception(), FakeStatusCode.of(StatusCode.Code.INVALID_ARGUMENT), false);
    mockService.addException(exception);

    try {
      Share share =
          Share.newBuilder()
              .setName(ShareName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]", "[SHARE]").toString())
              .setMountName("mountName456881732")
              .setDescription("description-1724546052")
              .setCapacityGb(-1923585408)
              .addAllNfsExportOptions(new ArrayList<NfsExportOptions>())
              .setCreateTime(Timestamp.newBuilder().build())
              .putAllLabels(new HashMap<String, String>())
              .build();
      FieldMask updateMask = FieldMask.newBuilder().build();
      client.updateShareAsync(share, updateMask).get();
      Assert.fail("No exception raised");
    } catch (ExecutionException e) {
    }
  }
}
