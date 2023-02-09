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

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.api.core.BetaApi;
import com.google.api.gax.core.BackgroundResource;
import com.google.api.gax.httpjson.longrunning.OperationsClient;
import com.google.api.gax.longrunning.OperationFuture;
import com.google.api.gax.paging.AbstractFixedSizeCollection;
import com.google.api.gax.paging.AbstractPage;
import com.google.api.gax.paging.AbstractPagedListResponse;
import com.google.api.gax.rpc.OperationCallable;
import com.google.api.gax.rpc.PageContext;
import com.google.api.gax.rpc.UnaryCallable;
import com.google.cloud.common.OperationMetadata;
import com.google.cloud.filestore.v1beta1.stub.CloudFilestoreManagerStub;
import com.google.cloud.filestore.v1beta1.stub.CloudFilestoreManagerStubSettings;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.longrunning.Operation;
import com.google.protobuf.Empty;
import com.google.protobuf.FieldMask;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.annotation.Generated;

// AUTO-GENERATED DOCUMENTATION AND CLASS.
/**
 * Service Description: Configures and manages Filestore resources.
 *
 * <p>Filestore Manager v1beta1.
 *
 * <p>The `file.googleapis.com` service implements the Filestore API and defines the following model
 * for managing resources:
 *
 * <ul>
 *   <li>The service works with a collection of cloud projects, named: `/projects/&#42;`
 *   <li>Each project has a collection of available locations, named: `/locations/&#42;`
 *   <li>Each location has a collection of instances and backups, named: `/instances/&#42;` and
 *       `/backups/&#42;` respectively.
 *   <li>As such, Filestore instances are resources of the form:
 *       `/projects/{project_id}/locations/{location_id}/instances/{instance_id}` backups are
 *       resources of the form: `/projects/{project_id}/locations/{location_id}/backup/{backup_id}`
 * </ul>
 *
 * <p>Note that location_id can represent a GCP `zone` or `region` depending on the resource. for
 * example: A zonal Filestore instance:
 *
 * <ul>
 *   <li>`projects/my-project/locations/us-central1-c/instances/my-basic-tier-filer` A regional
 *       Filestore instance:
 *   <li>`projects/my-project/locations/us-central1/instances/my-enterprise-filer`
 * </ul>
 *
 * <p>This class provides the ability to make remote calls to the backing service through method
 * calls that map to API methods. Sample code to get started:
 *
 * <pre>{@code
 * // This snippet has been automatically generated and should be regarded as a code template only.
 * // It will require modifications to work:
 * // - It may require correct/in-range values for request initialization.
 * // - It may require specifying regional endpoints when creating the service client as shown in
 * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
 * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
 *     CloudFilestoreManagerClient.create()) {
 *   InstanceName name = InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]");
 *   Instance response = cloudFilestoreManagerClient.getInstance(name);
 * }
 * }</pre>
 *
 * <p>Note: close() needs to be called on the CloudFilestoreManagerClient object to clean up
 * resources such as threads. In the example above, try-with-resources is used, which automatically
 * calls close().
 *
 * <p>The surface of this class includes several types of Java methods for each of the API's
 * methods:
 *
 * <ol>
 *   <li>A "flattened" method. With this type of method, the fields of the request type have been
 *       converted into function parameters. It may be the case that not all fields are available as
 *       parameters, and not every API method will have a flattened method entry point.
 *   <li>A "request object" method. This type of method only takes one parameter, a request object,
 *       which must be constructed before the call. Not every API method will have a request object
 *       method.
 *   <li>A "callable" method. This type of method takes no parameters and returns an immutable API
 *       callable object, which can be used to initiate calls to the service.
 * </ol>
 *
 * <p>See the individual methods for example code.
 *
 * <p>Many parameters require resource names to be formatted in a particular way. To assist with
 * these names, this class includes a format method for each type of name, and additionally a parse
 * method to extract the individual identifiers contained within names that are returned.
 *
 * <p>This class can be customized by passing in a custom instance of CloudFilestoreManagerSettings
 * to create(). For example:
 *
 * <p>To customize credentials:
 *
 * <pre>{@code
 * // This snippet has been automatically generated and should be regarded as a code template only.
 * // It will require modifications to work:
 * // - It may require correct/in-range values for request initialization.
 * // - It may require specifying regional endpoints when creating the service client as shown in
 * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
 * CloudFilestoreManagerSettings cloudFilestoreManagerSettings =
 *     CloudFilestoreManagerSettings.newBuilder()
 *         .setCredentialsProvider(FixedCredentialsProvider.create(myCredentials))
 *         .build();
 * CloudFilestoreManagerClient cloudFilestoreManagerClient =
 *     CloudFilestoreManagerClient.create(cloudFilestoreManagerSettings);
 * }</pre>
 *
 * <p>To customize the endpoint:
 *
 * <pre>{@code
 * // This snippet has been automatically generated and should be regarded as a code template only.
 * // It will require modifications to work:
 * // - It may require correct/in-range values for request initialization.
 * // - It may require specifying regional endpoints when creating the service client as shown in
 * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
 * CloudFilestoreManagerSettings cloudFilestoreManagerSettings =
 *     CloudFilestoreManagerSettings.newBuilder().setEndpoint(myEndpoint).build();
 * CloudFilestoreManagerClient cloudFilestoreManagerClient =
 *     CloudFilestoreManagerClient.create(cloudFilestoreManagerSettings);
 * }</pre>
 *
 * <p>To use REST (HTTP1.1/JSON) transport (instead of gRPC) for sending and receiving requests over
 * the wire:
 *
 * <pre>{@code
 * // This snippet has been automatically generated and should be regarded as a code template only.
 * // It will require modifications to work:
 * // - It may require correct/in-range values for request initialization.
 * // - It may require specifying regional endpoints when creating the service client as shown in
 * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
 * CloudFilestoreManagerSettings cloudFilestoreManagerSettings =
 *     CloudFilestoreManagerSettings.newHttpJsonBuilder().build();
 * CloudFilestoreManagerClient cloudFilestoreManagerClient =
 *     CloudFilestoreManagerClient.create(cloudFilestoreManagerSettings);
 * }</pre>
 *
 * <p>Please refer to the GitHub repository's samples for more quickstart code snippets.
 */
@BetaApi
@Generated("by gapic-generator-java")
public class CloudFilestoreManagerClient implements BackgroundResource {
  private final CloudFilestoreManagerSettings settings;
  private final CloudFilestoreManagerStub stub;
  private final OperationsClient httpJsonOperationsClient;
  private final com.google.longrunning.OperationsClient operationsClient;

  /** Constructs an instance of CloudFilestoreManagerClient with default settings. */
  public static final CloudFilestoreManagerClient create() throws IOException {
    return create(CloudFilestoreManagerSettings.newBuilder().build());
  }

  /**
   * Constructs an instance of CloudFilestoreManagerClient, using the given settings. The channels
   * are created based on the settings passed in, or defaults for any settings that are not set.
   */
  public static final CloudFilestoreManagerClient create(CloudFilestoreManagerSettings settings)
      throws IOException {
    return new CloudFilestoreManagerClient(settings);
  }

  /**
   * Constructs an instance of CloudFilestoreManagerClient, using the given stub for making calls.
   * This is for advanced usage - prefer using create(CloudFilestoreManagerSettings).
   */
  public static final CloudFilestoreManagerClient create(CloudFilestoreManagerStub stub) {
    return new CloudFilestoreManagerClient(stub);
  }

  /**
   * Constructs an instance of CloudFilestoreManagerClient, using the given settings. This is
   * protected so that it is easy to make a subclass, but otherwise, the static factory methods
   * should be preferred.
   */
  protected CloudFilestoreManagerClient(CloudFilestoreManagerSettings settings) throws IOException {
    this.settings = settings;
    this.stub = ((CloudFilestoreManagerStubSettings) settings.getStubSettings()).createStub();
    this.operationsClient =
        com.google.longrunning.OperationsClient.create(this.stub.getOperationsStub());
    this.httpJsonOperationsClient = OperationsClient.create(this.stub.getHttpJsonOperationsStub());
  }

  protected CloudFilestoreManagerClient(CloudFilestoreManagerStub stub) {
    this.settings = null;
    this.stub = stub;
    this.operationsClient =
        com.google.longrunning.OperationsClient.create(this.stub.getOperationsStub());
    this.httpJsonOperationsClient = OperationsClient.create(this.stub.getHttpJsonOperationsStub());
  }

  public final CloudFilestoreManagerSettings getSettings() {
    return settings;
  }

  public CloudFilestoreManagerStub getStub() {
    return stub;
  }

  /**
   * Returns the OperationsClient that can be used to query the status of a long-running operation
   * returned by another API method call.
   */
  public final com.google.longrunning.OperationsClient getOperationsClient() {
    return operationsClient;
  }

  /**
   * Returns the OperationsClient that can be used to query the status of a long-running operation
   * returned by another API method call.
   */
  @BetaApi
  public final OperationsClient getHttpJsonOperationsClient() {
    return httpJsonOperationsClient;
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Lists all instances in a project for either a specified location or for all locations.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   LocationName parent = LocationName.of("[PROJECT]", "[LOCATION]");
   *   for (Instance element : cloudFilestoreManagerClient.listInstances(parent).iterateAll()) {
   *     // doThingsWith(element);
   *   }
   * }
   * }</pre>
   *
   * @param parent Required. The project and location for which to retrieve instance information, in
   *     the format `projects/{project_id}/locations/{location}`. In Cloud Filestore, locations map
   *     to GCP zones, for example &#42;&#42;us-west1-b&#42;&#42;. To retrieve instance information
   *     for all locations, use "-" for the `{location}` value.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final ListInstancesPagedResponse listInstances(LocationName parent) {
    ListInstancesRequest request =
        ListInstancesRequest.newBuilder()
            .setParent(parent == null ? null : parent.toString())
            .build();
    return listInstances(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Lists all instances in a project for either a specified location or for all locations.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   String parent = LocationName.of("[PROJECT]", "[LOCATION]").toString();
   *   for (Instance element : cloudFilestoreManagerClient.listInstances(parent).iterateAll()) {
   *     // doThingsWith(element);
   *   }
   * }
   * }</pre>
   *
   * @param parent Required. The project and location for which to retrieve instance information, in
   *     the format `projects/{project_id}/locations/{location}`. In Cloud Filestore, locations map
   *     to GCP zones, for example &#42;&#42;us-west1-b&#42;&#42;. To retrieve instance information
   *     for all locations, use "-" for the `{location}` value.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final ListInstancesPagedResponse listInstances(String parent) {
    ListInstancesRequest request = ListInstancesRequest.newBuilder().setParent(parent).build();
    return listInstances(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Lists all instances in a project for either a specified location or for all locations.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   ListInstancesRequest request =
   *       ListInstancesRequest.newBuilder()
   *           .setParent(LocationName.of("[PROJECT]", "[LOCATION]").toString())
   *           .setPageSize(883849137)
   *           .setPageToken("pageToken873572522")
   *           .setOrderBy("orderBy-1207110587")
   *           .setFilter("filter-1274492040")
   *           .build();
   *   for (Instance element : cloudFilestoreManagerClient.listInstances(request).iterateAll()) {
   *     // doThingsWith(element);
   *   }
   * }
   * }</pre>
   *
   * @param request The request object containing all of the parameters for the API call.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final ListInstancesPagedResponse listInstances(ListInstancesRequest request) {
    return listInstancesPagedCallable().call(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Lists all instances in a project for either a specified location or for all locations.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   ListInstancesRequest request =
   *       ListInstancesRequest.newBuilder()
   *           .setParent(LocationName.of("[PROJECT]", "[LOCATION]").toString())
   *           .setPageSize(883849137)
   *           .setPageToken("pageToken873572522")
   *           .setOrderBy("orderBy-1207110587")
   *           .setFilter("filter-1274492040")
   *           .build();
   *   ApiFuture<Instance> future =
   *       cloudFilestoreManagerClient.listInstancesPagedCallable().futureCall(request);
   *   // Do something.
   *   for (Instance element : future.get().iterateAll()) {
   *     // doThingsWith(element);
   *   }
   * }
   * }</pre>
   */
  public final UnaryCallable<ListInstancesRequest, ListInstancesPagedResponse>
      listInstancesPagedCallable() {
    return stub.listInstancesPagedCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Lists all instances in a project for either a specified location or for all locations.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   ListInstancesRequest request =
   *       ListInstancesRequest.newBuilder()
   *           .setParent(LocationName.of("[PROJECT]", "[LOCATION]").toString())
   *           .setPageSize(883849137)
   *           .setPageToken("pageToken873572522")
   *           .setOrderBy("orderBy-1207110587")
   *           .setFilter("filter-1274492040")
   *           .build();
   *   while (true) {
   *     ListInstancesResponse response =
   *         cloudFilestoreManagerClient.listInstancesCallable().call(request);
   *     for (Instance element : response.getInstancesList()) {
   *       // doThingsWith(element);
   *     }
   *     String nextPageToken = response.getNextPageToken();
   *     if (!Strings.isNullOrEmpty(nextPageToken)) {
   *       request = request.toBuilder().setPageToken(nextPageToken).build();
   *     } else {
   *       break;
   *     }
   *   }
   * }
   * }</pre>
   */
  public final UnaryCallable<ListInstancesRequest, ListInstancesResponse> listInstancesCallable() {
    return stub.listInstancesCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Gets the details of a specific instance.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   InstanceName name = InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]");
   *   Instance response = cloudFilestoreManagerClient.getInstance(name);
   * }
   * }</pre>
   *
   * @param name Required. The instance resource name, in the format
   *     `projects/{project_id}/locations/{location}/instances/{instance_id}`.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final Instance getInstance(InstanceName name) {
    GetInstanceRequest request =
        GetInstanceRequest.newBuilder().setName(name == null ? null : name.toString()).build();
    return getInstance(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Gets the details of a specific instance.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   String name = InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]").toString();
   *   Instance response = cloudFilestoreManagerClient.getInstance(name);
   * }
   * }</pre>
   *
   * @param name Required. The instance resource name, in the format
   *     `projects/{project_id}/locations/{location}/instances/{instance_id}`.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final Instance getInstance(String name) {
    GetInstanceRequest request = GetInstanceRequest.newBuilder().setName(name).build();
    return getInstance(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Gets the details of a specific instance.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   GetInstanceRequest request =
   *       GetInstanceRequest.newBuilder()
   *           .setName(InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]").toString())
   *           .build();
   *   Instance response = cloudFilestoreManagerClient.getInstance(request);
   * }
   * }</pre>
   *
   * @param request The request object containing all of the parameters for the API call.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final Instance getInstance(GetInstanceRequest request) {
    return getInstanceCallable().call(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Gets the details of a specific instance.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   GetInstanceRequest request =
   *       GetInstanceRequest.newBuilder()
   *           .setName(InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]").toString())
   *           .build();
   *   ApiFuture<Instance> future =
   *       cloudFilestoreManagerClient.getInstanceCallable().futureCall(request);
   *   // Do something.
   *   Instance response = future.get();
   * }
   * }</pre>
   */
  public final UnaryCallable<GetInstanceRequest, Instance> getInstanceCallable() {
    return stub.getInstanceCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Creates an instance. When creating from a backup, the capacity of the new instance needs to be
   * equal to or larger than the capacity of the backup (and also equal to or larger than the
   * minimum capacity of the tier).
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   LocationName parent = LocationName.of("[PROJECT]", "[LOCATION]");
   *   Instance instance = Instance.newBuilder().build();
   *   String instanceId = "instanceId902024336";
   *   Instance response =
   *       cloudFilestoreManagerClient.createInstanceAsync(parent, instance, instanceId).get();
   * }
   * }</pre>
   *
   * @param parent Required. The instance's project and location, in the format
   *     `projects/{project_id}/locations/{location}`. In Filestore, locations map to GCP zones, for
   *     example &#42;&#42;us-west1-b&#42;&#42;.
   * @param instance Required. An [instance resource][google.cloud.filestore.v1beta1.Instance]
   * @param instanceId Required. The ID of the instance to create. The ID must be unique within the
   *     specified project and location.
   *     <p>This value must start with a lowercase letter followed by up to 62 lowercase letters,
   *     numbers, or hyphens, and cannot end with a hyphen.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final OperationFuture<Instance, OperationMetadata> createInstanceAsync(
      LocationName parent, Instance instance, String instanceId) {
    CreateInstanceRequest request =
        CreateInstanceRequest.newBuilder()
            .setParent(parent == null ? null : parent.toString())
            .setInstance(instance)
            .setInstanceId(instanceId)
            .build();
    return createInstanceAsync(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Creates an instance. When creating from a backup, the capacity of the new instance needs to be
   * equal to or larger than the capacity of the backup (and also equal to or larger than the
   * minimum capacity of the tier).
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   String parent = LocationName.of("[PROJECT]", "[LOCATION]").toString();
   *   Instance instance = Instance.newBuilder().build();
   *   String instanceId = "instanceId902024336";
   *   Instance response =
   *       cloudFilestoreManagerClient.createInstanceAsync(parent, instance, instanceId).get();
   * }
   * }</pre>
   *
   * @param parent Required. The instance's project and location, in the format
   *     `projects/{project_id}/locations/{location}`. In Filestore, locations map to GCP zones, for
   *     example &#42;&#42;us-west1-b&#42;&#42;.
   * @param instance Required. An [instance resource][google.cloud.filestore.v1beta1.Instance]
   * @param instanceId Required. The ID of the instance to create. The ID must be unique within the
   *     specified project and location.
   *     <p>This value must start with a lowercase letter followed by up to 62 lowercase letters,
   *     numbers, or hyphens, and cannot end with a hyphen.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final OperationFuture<Instance, OperationMetadata> createInstanceAsync(
      String parent, Instance instance, String instanceId) {
    CreateInstanceRequest request =
        CreateInstanceRequest.newBuilder()
            .setParent(parent)
            .setInstance(instance)
            .setInstanceId(instanceId)
            .build();
    return createInstanceAsync(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Creates an instance. When creating from a backup, the capacity of the new instance needs to be
   * equal to or larger than the capacity of the backup (and also equal to or larger than the
   * minimum capacity of the tier).
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   CreateInstanceRequest request =
   *       CreateInstanceRequest.newBuilder()
   *           .setParent(LocationName.of("[PROJECT]", "[LOCATION]").toString())
   *           .setInstanceId("instanceId902024336")
   *           .setInstance(Instance.newBuilder().build())
   *           .build();
   *   Instance response = cloudFilestoreManagerClient.createInstanceAsync(request).get();
   * }
   * }</pre>
   *
   * @param request The request object containing all of the parameters for the API call.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final OperationFuture<Instance, OperationMetadata> createInstanceAsync(
      CreateInstanceRequest request) {
    return createInstanceOperationCallable().futureCall(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Creates an instance. When creating from a backup, the capacity of the new instance needs to be
   * equal to or larger than the capacity of the backup (and also equal to or larger than the
   * minimum capacity of the tier).
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   CreateInstanceRequest request =
   *       CreateInstanceRequest.newBuilder()
   *           .setParent(LocationName.of("[PROJECT]", "[LOCATION]").toString())
   *           .setInstanceId("instanceId902024336")
   *           .setInstance(Instance.newBuilder().build())
   *           .build();
   *   OperationFuture<Instance, OperationMetadata> future =
   *       cloudFilestoreManagerClient.createInstanceOperationCallable().futureCall(request);
   *   // Do something.
   *   Instance response = future.get();
   * }
   * }</pre>
   */
  public final OperationCallable<CreateInstanceRequest, Instance, OperationMetadata>
      createInstanceOperationCallable() {
    return stub.createInstanceOperationCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Creates an instance. When creating from a backup, the capacity of the new instance needs to be
   * equal to or larger than the capacity of the backup (and also equal to or larger than the
   * minimum capacity of the tier).
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   CreateInstanceRequest request =
   *       CreateInstanceRequest.newBuilder()
   *           .setParent(LocationName.of("[PROJECT]", "[LOCATION]").toString())
   *           .setInstanceId("instanceId902024336")
   *           .setInstance(Instance.newBuilder().build())
   *           .build();
   *   ApiFuture<Operation> future =
   *       cloudFilestoreManagerClient.createInstanceCallable().futureCall(request);
   *   // Do something.
   *   Operation response = future.get();
   * }
   * }</pre>
   */
  public final UnaryCallable<CreateInstanceRequest, Operation> createInstanceCallable() {
    return stub.createInstanceCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Updates the settings of a specific instance.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   Instance instance = Instance.newBuilder().build();
   *   FieldMask updateMask = FieldMask.newBuilder().build();
   *   Instance response =
   *       cloudFilestoreManagerClient.updateInstanceAsync(instance, updateMask).get();
   * }
   * }</pre>
   *
   * @param instance Required. Only fields specified in update_mask are updated.
   * @param updateMask Required. Mask of fields to update. At least one path must be supplied in
   *     this field. The elements of the repeated paths field may only include these fields:
   *     <ul>
   *       <li>"description"
   *       <li>"file_shares"
   *       <li>"labels"
   *     </ul>
   *
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final OperationFuture<Instance, OperationMetadata> updateInstanceAsync(
      Instance instance, FieldMask updateMask) {
    UpdateInstanceRequest request =
        UpdateInstanceRequest.newBuilder().setInstance(instance).setUpdateMask(updateMask).build();
    return updateInstanceAsync(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Updates the settings of a specific instance.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   UpdateInstanceRequest request =
   *       UpdateInstanceRequest.newBuilder()
   *           .setUpdateMask(FieldMask.newBuilder().build())
   *           .setInstance(Instance.newBuilder().build())
   *           .build();
   *   Instance response = cloudFilestoreManagerClient.updateInstanceAsync(request).get();
   * }
   * }</pre>
   *
   * @param request The request object containing all of the parameters for the API call.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final OperationFuture<Instance, OperationMetadata> updateInstanceAsync(
      UpdateInstanceRequest request) {
    return updateInstanceOperationCallable().futureCall(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Updates the settings of a specific instance.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   UpdateInstanceRequest request =
   *       UpdateInstanceRequest.newBuilder()
   *           .setUpdateMask(FieldMask.newBuilder().build())
   *           .setInstance(Instance.newBuilder().build())
   *           .build();
   *   OperationFuture<Instance, OperationMetadata> future =
   *       cloudFilestoreManagerClient.updateInstanceOperationCallable().futureCall(request);
   *   // Do something.
   *   Instance response = future.get();
   * }
   * }</pre>
   */
  public final OperationCallable<UpdateInstanceRequest, Instance, OperationMetadata>
      updateInstanceOperationCallable() {
    return stub.updateInstanceOperationCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Updates the settings of a specific instance.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   UpdateInstanceRequest request =
   *       UpdateInstanceRequest.newBuilder()
   *           .setUpdateMask(FieldMask.newBuilder().build())
   *           .setInstance(Instance.newBuilder().build())
   *           .build();
   *   ApiFuture<Operation> future =
   *       cloudFilestoreManagerClient.updateInstanceCallable().futureCall(request);
   *   // Do something.
   *   Operation response = future.get();
   * }
   * }</pre>
   */
  public final UnaryCallable<UpdateInstanceRequest, Operation> updateInstanceCallable() {
    return stub.updateInstanceCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Restores an existing instance's file share from a backup.
   *
   * <p>The capacity of the instance needs to be equal to or larger than the capacity of the backup
   * (and also equal to or larger than the minimum capacity of the tier).
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   RestoreInstanceRequest request =
   *       RestoreInstanceRequest.newBuilder()
   *           .setName(InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]").toString())
   *           .setFileShare("fileShare-1327728701")
   *           .build();
   *   Instance response = cloudFilestoreManagerClient.restoreInstanceAsync(request).get();
   * }
   * }</pre>
   *
   * @param request The request object containing all of the parameters for the API call.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final OperationFuture<Instance, OperationMetadata> restoreInstanceAsync(
      RestoreInstanceRequest request) {
    return restoreInstanceOperationCallable().futureCall(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Restores an existing instance's file share from a backup.
   *
   * <p>The capacity of the instance needs to be equal to or larger than the capacity of the backup
   * (and also equal to or larger than the minimum capacity of the tier).
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   RestoreInstanceRequest request =
   *       RestoreInstanceRequest.newBuilder()
   *           .setName(InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]").toString())
   *           .setFileShare("fileShare-1327728701")
   *           .build();
   *   OperationFuture<Instance, OperationMetadata> future =
   *       cloudFilestoreManagerClient.restoreInstanceOperationCallable().futureCall(request);
   *   // Do something.
   *   Instance response = future.get();
   * }
   * }</pre>
   */
  public final OperationCallable<RestoreInstanceRequest, Instance, OperationMetadata>
      restoreInstanceOperationCallable() {
    return stub.restoreInstanceOperationCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Restores an existing instance's file share from a backup.
   *
   * <p>The capacity of the instance needs to be equal to or larger than the capacity of the backup
   * (and also equal to or larger than the minimum capacity of the tier).
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   RestoreInstanceRequest request =
   *       RestoreInstanceRequest.newBuilder()
   *           .setName(InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]").toString())
   *           .setFileShare("fileShare-1327728701")
   *           .build();
   *   ApiFuture<Operation> future =
   *       cloudFilestoreManagerClient.restoreInstanceCallable().futureCall(request);
   *   // Do something.
   *   Operation response = future.get();
   * }
   * }</pre>
   */
  public final UnaryCallable<RestoreInstanceRequest, Operation> restoreInstanceCallable() {
    return stub.restoreInstanceCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Revert an existing instance's file system to a specified snapshot.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   RevertInstanceRequest request =
   *       RevertInstanceRequest.newBuilder()
   *           .setName(InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]").toString())
   *           .setTargetSnapshotId("targetSnapshotId-2099459472")
   *           .build();
   *   Instance response = cloudFilestoreManagerClient.revertInstanceAsync(request).get();
   * }
   * }</pre>
   *
   * @param request The request object containing all of the parameters for the API call.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final OperationFuture<Instance, OperationMetadata> revertInstanceAsync(
      RevertInstanceRequest request) {
    return revertInstanceOperationCallable().futureCall(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Revert an existing instance's file system to a specified snapshot.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   RevertInstanceRequest request =
   *       RevertInstanceRequest.newBuilder()
   *           .setName(InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]").toString())
   *           .setTargetSnapshotId("targetSnapshotId-2099459472")
   *           .build();
   *   OperationFuture<Instance, OperationMetadata> future =
   *       cloudFilestoreManagerClient.revertInstanceOperationCallable().futureCall(request);
   *   // Do something.
   *   Instance response = future.get();
   * }
   * }</pre>
   */
  public final OperationCallable<RevertInstanceRequest, Instance, OperationMetadata>
      revertInstanceOperationCallable() {
    return stub.revertInstanceOperationCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Revert an existing instance's file system to a specified snapshot.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   RevertInstanceRequest request =
   *       RevertInstanceRequest.newBuilder()
   *           .setName(InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]").toString())
   *           .setTargetSnapshotId("targetSnapshotId-2099459472")
   *           .build();
   *   ApiFuture<Operation> future =
   *       cloudFilestoreManagerClient.revertInstanceCallable().futureCall(request);
   *   // Do something.
   *   Operation response = future.get();
   * }
   * }</pre>
   */
  public final UnaryCallable<RevertInstanceRequest, Operation> revertInstanceCallable() {
    return stub.revertInstanceCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Deletes an instance.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   InstanceName name = InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]");
   *   cloudFilestoreManagerClient.deleteInstanceAsync(name).get();
   * }
   * }</pre>
   *
   * @param name Required. The instance resource name, in the format
   *     `projects/{project_id}/locations/{location}/instances/{instance_id}`
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final OperationFuture<Empty, OperationMetadata> deleteInstanceAsync(InstanceName name) {
    DeleteInstanceRequest request =
        DeleteInstanceRequest.newBuilder().setName(name == null ? null : name.toString()).build();
    return deleteInstanceAsync(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Deletes an instance.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   String name = InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]").toString();
   *   cloudFilestoreManagerClient.deleteInstanceAsync(name).get();
   * }
   * }</pre>
   *
   * @param name Required. The instance resource name, in the format
   *     `projects/{project_id}/locations/{location}/instances/{instance_id}`
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final OperationFuture<Empty, OperationMetadata> deleteInstanceAsync(String name) {
    DeleteInstanceRequest request = DeleteInstanceRequest.newBuilder().setName(name).build();
    return deleteInstanceAsync(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Deletes an instance.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   DeleteInstanceRequest request =
   *       DeleteInstanceRequest.newBuilder()
   *           .setName(InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]").toString())
   *           .setForce(true)
   *           .build();
   *   cloudFilestoreManagerClient.deleteInstanceAsync(request).get();
   * }
   * }</pre>
   *
   * @param request The request object containing all of the parameters for the API call.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final OperationFuture<Empty, OperationMetadata> deleteInstanceAsync(
      DeleteInstanceRequest request) {
    return deleteInstanceOperationCallable().futureCall(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Deletes an instance.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   DeleteInstanceRequest request =
   *       DeleteInstanceRequest.newBuilder()
   *           .setName(InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]").toString())
   *           .setForce(true)
   *           .build();
   *   OperationFuture<Empty, OperationMetadata> future =
   *       cloudFilestoreManagerClient.deleteInstanceOperationCallable().futureCall(request);
   *   // Do something.
   *   future.get();
   * }
   * }</pre>
   */
  public final OperationCallable<DeleteInstanceRequest, Empty, OperationMetadata>
      deleteInstanceOperationCallable() {
    return stub.deleteInstanceOperationCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Deletes an instance.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   DeleteInstanceRequest request =
   *       DeleteInstanceRequest.newBuilder()
   *           .setName(InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]").toString())
   *           .setForce(true)
   *           .build();
   *   ApiFuture<Operation> future =
   *       cloudFilestoreManagerClient.deleteInstanceCallable().futureCall(request);
   *   // Do something.
   *   future.get();
   * }
   * }</pre>
   */
  public final UnaryCallable<DeleteInstanceRequest, Operation> deleteInstanceCallable() {
    return stub.deleteInstanceCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Lists all snapshots in a project for either a specified location or for all locations.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   InstanceName parent = InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]");
   *   for (Snapshot element : cloudFilestoreManagerClient.listSnapshots(parent).iterateAll()) {
   *     // doThingsWith(element);
   *   }
   * }
   * }</pre>
   *
   * @param parent Required. The instance for which to retrieve snapshot information, in the format
   *     `projects/{project_id}/locations/{location}/instances/{instance_id}`.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final ListSnapshotsPagedResponse listSnapshots(InstanceName parent) {
    ListSnapshotsRequest request =
        ListSnapshotsRequest.newBuilder()
            .setParent(parent == null ? null : parent.toString())
            .build();
    return listSnapshots(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Lists all snapshots in a project for either a specified location or for all locations.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   String parent = InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]").toString();
   *   for (Snapshot element : cloudFilestoreManagerClient.listSnapshots(parent).iterateAll()) {
   *     // doThingsWith(element);
   *   }
   * }
   * }</pre>
   *
   * @param parent Required. The instance for which to retrieve snapshot information, in the format
   *     `projects/{project_id}/locations/{location}/instances/{instance_id}`.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final ListSnapshotsPagedResponse listSnapshots(String parent) {
    ListSnapshotsRequest request = ListSnapshotsRequest.newBuilder().setParent(parent).build();
    return listSnapshots(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Lists all snapshots in a project for either a specified location or for all locations.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   ListSnapshotsRequest request =
   *       ListSnapshotsRequest.newBuilder()
   *           .setParent(InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]").toString())
   *           .setPageSize(883849137)
   *           .setPageToken("pageToken873572522")
   *           .setOrderBy("orderBy-1207110587")
   *           .setFilter("filter-1274492040")
   *           .build();
   *   for (Snapshot element : cloudFilestoreManagerClient.listSnapshots(request).iterateAll()) {
   *     // doThingsWith(element);
   *   }
   * }
   * }</pre>
   *
   * @param request The request object containing all of the parameters for the API call.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final ListSnapshotsPagedResponse listSnapshots(ListSnapshotsRequest request) {
    return listSnapshotsPagedCallable().call(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Lists all snapshots in a project for either a specified location or for all locations.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   ListSnapshotsRequest request =
   *       ListSnapshotsRequest.newBuilder()
   *           .setParent(InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]").toString())
   *           .setPageSize(883849137)
   *           .setPageToken("pageToken873572522")
   *           .setOrderBy("orderBy-1207110587")
   *           .setFilter("filter-1274492040")
   *           .build();
   *   ApiFuture<Snapshot> future =
   *       cloudFilestoreManagerClient.listSnapshotsPagedCallable().futureCall(request);
   *   // Do something.
   *   for (Snapshot element : future.get().iterateAll()) {
   *     // doThingsWith(element);
   *   }
   * }
   * }</pre>
   */
  public final UnaryCallable<ListSnapshotsRequest, ListSnapshotsPagedResponse>
      listSnapshotsPagedCallable() {
    return stub.listSnapshotsPagedCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Lists all snapshots in a project for either a specified location or for all locations.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   ListSnapshotsRequest request =
   *       ListSnapshotsRequest.newBuilder()
   *           .setParent(InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]").toString())
   *           .setPageSize(883849137)
   *           .setPageToken("pageToken873572522")
   *           .setOrderBy("orderBy-1207110587")
   *           .setFilter("filter-1274492040")
   *           .build();
   *   while (true) {
   *     ListSnapshotsResponse response =
   *         cloudFilestoreManagerClient.listSnapshotsCallable().call(request);
   *     for (Snapshot element : response.getSnapshotsList()) {
   *       // doThingsWith(element);
   *     }
   *     String nextPageToken = response.getNextPageToken();
   *     if (!Strings.isNullOrEmpty(nextPageToken)) {
   *       request = request.toBuilder().setPageToken(nextPageToken).build();
   *     } else {
   *       break;
   *     }
   *   }
   * }
   * }</pre>
   */
  public final UnaryCallable<ListSnapshotsRequest, ListSnapshotsResponse> listSnapshotsCallable() {
    return stub.listSnapshotsCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Gets the details of a specific snapshot.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   SnapshotName name = SnapshotName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]", "[SNAPSHOT]");
   *   Snapshot response = cloudFilestoreManagerClient.getSnapshot(name);
   * }
   * }</pre>
   *
   * @param name Required. The snapshot resource name, in the format
   *     `projects/{project_id}/locations/{location}/instances/{instance_id}/snapshots/{snapshot_id}`
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final Snapshot getSnapshot(SnapshotName name) {
    GetSnapshotRequest request =
        GetSnapshotRequest.newBuilder().setName(name == null ? null : name.toString()).build();
    return getSnapshot(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Gets the details of a specific snapshot.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   String name =
   *       SnapshotName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]", "[SNAPSHOT]").toString();
   *   Snapshot response = cloudFilestoreManagerClient.getSnapshot(name);
   * }
   * }</pre>
   *
   * @param name Required. The snapshot resource name, in the format
   *     `projects/{project_id}/locations/{location}/instances/{instance_id}/snapshots/{snapshot_id}`
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final Snapshot getSnapshot(String name) {
    GetSnapshotRequest request = GetSnapshotRequest.newBuilder().setName(name).build();
    return getSnapshot(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Gets the details of a specific snapshot.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   GetSnapshotRequest request =
   *       GetSnapshotRequest.newBuilder()
   *           .setName(
   *               SnapshotName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]", "[SNAPSHOT]").toString())
   *           .build();
   *   Snapshot response = cloudFilestoreManagerClient.getSnapshot(request);
   * }
   * }</pre>
   *
   * @param request The request object containing all of the parameters for the API call.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final Snapshot getSnapshot(GetSnapshotRequest request) {
    return getSnapshotCallable().call(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Gets the details of a specific snapshot.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   GetSnapshotRequest request =
   *       GetSnapshotRequest.newBuilder()
   *           .setName(
   *               SnapshotName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]", "[SNAPSHOT]").toString())
   *           .build();
   *   ApiFuture<Snapshot> future =
   *       cloudFilestoreManagerClient.getSnapshotCallable().futureCall(request);
   *   // Do something.
   *   Snapshot response = future.get();
   * }
   * }</pre>
   */
  public final UnaryCallable<GetSnapshotRequest, Snapshot> getSnapshotCallable() {
    return stub.getSnapshotCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Creates a snapshot.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   InstanceName parent = InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]");
   *   Snapshot snapshot = Snapshot.newBuilder().build();
   *   String snapshotId = "snapshotId-1113817601";
   *   Snapshot response =
   *       cloudFilestoreManagerClient.createSnapshotAsync(parent, snapshot, snapshotId).get();
   * }
   * }</pre>
   *
   * @param parent Required. The Filestore Instance to create the snapshots of, in the format
   *     `projects/{project_id}/locations/{location}/instances/{instance_id}`
   * @param snapshot Required. A snapshot resource
   * @param snapshotId Required. The ID to use for the snapshot. The ID must be unique within the
   *     specified instance.
   *     <p>This value must start with a lowercase letter followed by up to 62 lowercase letters,
   *     numbers, or hyphens, and cannot end with a hyphen.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final OperationFuture<Snapshot, OperationMetadata> createSnapshotAsync(
      InstanceName parent, Snapshot snapshot, String snapshotId) {
    CreateSnapshotRequest request =
        CreateSnapshotRequest.newBuilder()
            .setParent(parent == null ? null : parent.toString())
            .setSnapshot(snapshot)
            .setSnapshotId(snapshotId)
            .build();
    return createSnapshotAsync(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Creates a snapshot.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   String parent = InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]").toString();
   *   Snapshot snapshot = Snapshot.newBuilder().build();
   *   String snapshotId = "snapshotId-1113817601";
   *   Snapshot response =
   *       cloudFilestoreManagerClient.createSnapshotAsync(parent, snapshot, snapshotId).get();
   * }
   * }</pre>
   *
   * @param parent Required. The Filestore Instance to create the snapshots of, in the format
   *     `projects/{project_id}/locations/{location}/instances/{instance_id}`
   * @param snapshot Required. A snapshot resource
   * @param snapshotId Required. The ID to use for the snapshot. The ID must be unique within the
   *     specified instance.
   *     <p>This value must start with a lowercase letter followed by up to 62 lowercase letters,
   *     numbers, or hyphens, and cannot end with a hyphen.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final OperationFuture<Snapshot, OperationMetadata> createSnapshotAsync(
      String parent, Snapshot snapshot, String snapshotId) {
    CreateSnapshotRequest request =
        CreateSnapshotRequest.newBuilder()
            .setParent(parent)
            .setSnapshot(snapshot)
            .setSnapshotId(snapshotId)
            .build();
    return createSnapshotAsync(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Creates a snapshot.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   CreateSnapshotRequest request =
   *       CreateSnapshotRequest.newBuilder()
   *           .setParent(InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]").toString())
   *           .setSnapshotId("snapshotId-1113817601")
   *           .setSnapshot(Snapshot.newBuilder().build())
   *           .build();
   *   Snapshot response = cloudFilestoreManagerClient.createSnapshotAsync(request).get();
   * }
   * }</pre>
   *
   * @param request The request object containing all of the parameters for the API call.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final OperationFuture<Snapshot, OperationMetadata> createSnapshotAsync(
      CreateSnapshotRequest request) {
    return createSnapshotOperationCallable().futureCall(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Creates a snapshot.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   CreateSnapshotRequest request =
   *       CreateSnapshotRequest.newBuilder()
   *           .setParent(InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]").toString())
   *           .setSnapshotId("snapshotId-1113817601")
   *           .setSnapshot(Snapshot.newBuilder().build())
   *           .build();
   *   OperationFuture<Snapshot, OperationMetadata> future =
   *       cloudFilestoreManagerClient.createSnapshotOperationCallable().futureCall(request);
   *   // Do something.
   *   Snapshot response = future.get();
   * }
   * }</pre>
   */
  public final OperationCallable<CreateSnapshotRequest, Snapshot, OperationMetadata>
      createSnapshotOperationCallable() {
    return stub.createSnapshotOperationCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Creates a snapshot.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   CreateSnapshotRequest request =
   *       CreateSnapshotRequest.newBuilder()
   *           .setParent(InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]").toString())
   *           .setSnapshotId("snapshotId-1113817601")
   *           .setSnapshot(Snapshot.newBuilder().build())
   *           .build();
   *   ApiFuture<Operation> future =
   *       cloudFilestoreManagerClient.createSnapshotCallable().futureCall(request);
   *   // Do something.
   *   Operation response = future.get();
   * }
   * }</pre>
   */
  public final UnaryCallable<CreateSnapshotRequest, Operation> createSnapshotCallable() {
    return stub.createSnapshotCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Deletes a snapshot.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   SnapshotName name = SnapshotName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]", "[SNAPSHOT]");
   *   cloudFilestoreManagerClient.deleteSnapshotAsync(name).get();
   * }
   * }</pre>
   *
   * @param name Required. The snapshot resource name, in the format
   *     `projects/{project_id}/locations/{location}/instances/{instance_id}/snapshots/{snapshot_id}`
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final OperationFuture<Empty, OperationMetadata> deleteSnapshotAsync(SnapshotName name) {
    DeleteSnapshotRequest request =
        DeleteSnapshotRequest.newBuilder().setName(name == null ? null : name.toString()).build();
    return deleteSnapshotAsync(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Deletes a snapshot.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   String name =
   *       SnapshotName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]", "[SNAPSHOT]").toString();
   *   cloudFilestoreManagerClient.deleteSnapshotAsync(name).get();
   * }
   * }</pre>
   *
   * @param name Required. The snapshot resource name, in the format
   *     `projects/{project_id}/locations/{location}/instances/{instance_id}/snapshots/{snapshot_id}`
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final OperationFuture<Empty, OperationMetadata> deleteSnapshotAsync(String name) {
    DeleteSnapshotRequest request = DeleteSnapshotRequest.newBuilder().setName(name).build();
    return deleteSnapshotAsync(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Deletes a snapshot.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   DeleteSnapshotRequest request =
   *       DeleteSnapshotRequest.newBuilder()
   *           .setName(
   *               SnapshotName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]", "[SNAPSHOT]").toString())
   *           .build();
   *   cloudFilestoreManagerClient.deleteSnapshotAsync(request).get();
   * }
   * }</pre>
   *
   * @param request The request object containing all of the parameters for the API call.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final OperationFuture<Empty, OperationMetadata> deleteSnapshotAsync(
      DeleteSnapshotRequest request) {
    return deleteSnapshotOperationCallable().futureCall(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Deletes a snapshot.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   DeleteSnapshotRequest request =
   *       DeleteSnapshotRequest.newBuilder()
   *           .setName(
   *               SnapshotName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]", "[SNAPSHOT]").toString())
   *           .build();
   *   OperationFuture<Empty, OperationMetadata> future =
   *       cloudFilestoreManagerClient.deleteSnapshotOperationCallable().futureCall(request);
   *   // Do something.
   *   future.get();
   * }
   * }</pre>
   */
  public final OperationCallable<DeleteSnapshotRequest, Empty, OperationMetadata>
      deleteSnapshotOperationCallable() {
    return stub.deleteSnapshotOperationCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Deletes a snapshot.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   DeleteSnapshotRequest request =
   *       DeleteSnapshotRequest.newBuilder()
   *           .setName(
   *               SnapshotName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]", "[SNAPSHOT]").toString())
   *           .build();
   *   ApiFuture<Operation> future =
   *       cloudFilestoreManagerClient.deleteSnapshotCallable().futureCall(request);
   *   // Do something.
   *   future.get();
   * }
   * }</pre>
   */
  public final UnaryCallable<DeleteSnapshotRequest, Operation> deleteSnapshotCallable() {
    return stub.deleteSnapshotCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Updates the settings of a specific snapshot.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   Snapshot snapshot = Snapshot.newBuilder().build();
   *   FieldMask updateMask = FieldMask.newBuilder().build();
   *   Snapshot response =
   *       cloudFilestoreManagerClient.updateSnapshotAsync(snapshot, updateMask).get();
   * }
   * }</pre>
   *
   * @param snapshot Required. A snapshot resource
   * @param updateMask Required. Mask of fields to update. At least one path must be supplied in
   *     this field.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final OperationFuture<Snapshot, OperationMetadata> updateSnapshotAsync(
      Snapshot snapshot, FieldMask updateMask) {
    UpdateSnapshotRequest request =
        UpdateSnapshotRequest.newBuilder().setSnapshot(snapshot).setUpdateMask(updateMask).build();
    return updateSnapshotAsync(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Updates the settings of a specific snapshot.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   UpdateSnapshotRequest request =
   *       UpdateSnapshotRequest.newBuilder()
   *           .setUpdateMask(FieldMask.newBuilder().build())
   *           .setSnapshot(Snapshot.newBuilder().build())
   *           .build();
   *   Snapshot response = cloudFilestoreManagerClient.updateSnapshotAsync(request).get();
   * }
   * }</pre>
   *
   * @param request The request object containing all of the parameters for the API call.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final OperationFuture<Snapshot, OperationMetadata> updateSnapshotAsync(
      UpdateSnapshotRequest request) {
    return updateSnapshotOperationCallable().futureCall(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Updates the settings of a specific snapshot.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   UpdateSnapshotRequest request =
   *       UpdateSnapshotRequest.newBuilder()
   *           .setUpdateMask(FieldMask.newBuilder().build())
   *           .setSnapshot(Snapshot.newBuilder().build())
   *           .build();
   *   OperationFuture<Snapshot, OperationMetadata> future =
   *       cloudFilestoreManagerClient.updateSnapshotOperationCallable().futureCall(request);
   *   // Do something.
   *   Snapshot response = future.get();
   * }
   * }</pre>
   */
  public final OperationCallable<UpdateSnapshotRequest, Snapshot, OperationMetadata>
      updateSnapshotOperationCallable() {
    return stub.updateSnapshotOperationCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Updates the settings of a specific snapshot.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   UpdateSnapshotRequest request =
   *       UpdateSnapshotRequest.newBuilder()
   *           .setUpdateMask(FieldMask.newBuilder().build())
   *           .setSnapshot(Snapshot.newBuilder().build())
   *           .build();
   *   ApiFuture<Operation> future =
   *       cloudFilestoreManagerClient.updateSnapshotCallable().futureCall(request);
   *   // Do something.
   *   Operation response = future.get();
   * }
   * }</pre>
   */
  public final UnaryCallable<UpdateSnapshotRequest, Operation> updateSnapshotCallable() {
    return stub.updateSnapshotCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Lists all backups in a project for either a specified location or for all locations.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   LocationName parent = LocationName.of("[PROJECT]", "[LOCATION]");
   *   for (Backup element : cloudFilestoreManagerClient.listBackups(parent).iterateAll()) {
   *     // doThingsWith(element);
   *   }
   * }
   * }</pre>
   *
   * @param parent Required. The project and location for which to retrieve backup information, in
   *     the format `projects/{project_id}/locations/{location}`. In Filestore, backup locations map
   *     to GCP regions, for example &#42;&#42;us-west1&#42;&#42;. To retrieve backup information
   *     for all locations, use "-" for the `{location}` value.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final ListBackupsPagedResponse listBackups(LocationName parent) {
    ListBackupsRequest request =
        ListBackupsRequest.newBuilder()
            .setParent(parent == null ? null : parent.toString())
            .build();
    return listBackups(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Lists all backups in a project for either a specified location or for all locations.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   String parent = LocationName.of("[PROJECT]", "[LOCATION]").toString();
   *   for (Backup element : cloudFilestoreManagerClient.listBackups(parent).iterateAll()) {
   *     // doThingsWith(element);
   *   }
   * }
   * }</pre>
   *
   * @param parent Required. The project and location for which to retrieve backup information, in
   *     the format `projects/{project_id}/locations/{location}`. In Filestore, backup locations map
   *     to GCP regions, for example &#42;&#42;us-west1&#42;&#42;. To retrieve backup information
   *     for all locations, use "-" for the `{location}` value.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final ListBackupsPagedResponse listBackups(String parent) {
    ListBackupsRequest request = ListBackupsRequest.newBuilder().setParent(parent).build();
    return listBackups(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Lists all backups in a project for either a specified location or for all locations.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   ListBackupsRequest request =
   *       ListBackupsRequest.newBuilder()
   *           .setParent(LocationName.of("[PROJECT]", "[LOCATION]").toString())
   *           .setPageSize(883849137)
   *           .setPageToken("pageToken873572522")
   *           .setOrderBy("orderBy-1207110587")
   *           .setFilter("filter-1274492040")
   *           .build();
   *   for (Backup element : cloudFilestoreManagerClient.listBackups(request).iterateAll()) {
   *     // doThingsWith(element);
   *   }
   * }
   * }</pre>
   *
   * @param request The request object containing all of the parameters for the API call.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final ListBackupsPagedResponse listBackups(ListBackupsRequest request) {
    return listBackupsPagedCallable().call(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Lists all backups in a project for either a specified location or for all locations.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   ListBackupsRequest request =
   *       ListBackupsRequest.newBuilder()
   *           .setParent(LocationName.of("[PROJECT]", "[LOCATION]").toString())
   *           .setPageSize(883849137)
   *           .setPageToken("pageToken873572522")
   *           .setOrderBy("orderBy-1207110587")
   *           .setFilter("filter-1274492040")
   *           .build();
   *   ApiFuture<Backup> future =
   *       cloudFilestoreManagerClient.listBackupsPagedCallable().futureCall(request);
   *   // Do something.
   *   for (Backup element : future.get().iterateAll()) {
   *     // doThingsWith(element);
   *   }
   * }
   * }</pre>
   */
  public final UnaryCallable<ListBackupsRequest, ListBackupsPagedResponse>
      listBackupsPagedCallable() {
    return stub.listBackupsPagedCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Lists all backups in a project for either a specified location or for all locations.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   ListBackupsRequest request =
   *       ListBackupsRequest.newBuilder()
   *           .setParent(LocationName.of("[PROJECT]", "[LOCATION]").toString())
   *           .setPageSize(883849137)
   *           .setPageToken("pageToken873572522")
   *           .setOrderBy("orderBy-1207110587")
   *           .setFilter("filter-1274492040")
   *           .build();
   *   while (true) {
   *     ListBackupsResponse response =
   *         cloudFilestoreManagerClient.listBackupsCallable().call(request);
   *     for (Backup element : response.getBackupsList()) {
   *       // doThingsWith(element);
   *     }
   *     String nextPageToken = response.getNextPageToken();
   *     if (!Strings.isNullOrEmpty(nextPageToken)) {
   *       request = request.toBuilder().setPageToken(nextPageToken).build();
   *     } else {
   *       break;
   *     }
   *   }
   * }
   * }</pre>
   */
  public final UnaryCallable<ListBackupsRequest, ListBackupsResponse> listBackupsCallable() {
    return stub.listBackupsCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Gets the details of a specific backup.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   BackupName name = BackupName.of("[PROJECT]", "[LOCATION]", "[BACKUP]");
   *   Backup response = cloudFilestoreManagerClient.getBackup(name);
   * }
   * }</pre>
   *
   * @param name Required. The backup resource name, in the format
   *     `projects/{project_id}/locations/{location}/backups/{backup_id}`.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final Backup getBackup(BackupName name) {
    GetBackupRequest request =
        GetBackupRequest.newBuilder().setName(name == null ? null : name.toString()).build();
    return getBackup(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Gets the details of a specific backup.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   String name = BackupName.of("[PROJECT]", "[LOCATION]", "[BACKUP]").toString();
   *   Backup response = cloudFilestoreManagerClient.getBackup(name);
   * }
   * }</pre>
   *
   * @param name Required. The backup resource name, in the format
   *     `projects/{project_id}/locations/{location}/backups/{backup_id}`.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final Backup getBackup(String name) {
    GetBackupRequest request = GetBackupRequest.newBuilder().setName(name).build();
    return getBackup(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Gets the details of a specific backup.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   GetBackupRequest request =
   *       GetBackupRequest.newBuilder()
   *           .setName(BackupName.of("[PROJECT]", "[LOCATION]", "[BACKUP]").toString())
   *           .build();
   *   Backup response = cloudFilestoreManagerClient.getBackup(request);
   * }
   * }</pre>
   *
   * @param request The request object containing all of the parameters for the API call.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final Backup getBackup(GetBackupRequest request) {
    return getBackupCallable().call(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Gets the details of a specific backup.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   GetBackupRequest request =
   *       GetBackupRequest.newBuilder()
   *           .setName(BackupName.of("[PROJECT]", "[LOCATION]", "[BACKUP]").toString())
   *           .build();
   *   ApiFuture<Backup> future =
   *       cloudFilestoreManagerClient.getBackupCallable().futureCall(request);
   *   // Do something.
   *   Backup response = future.get();
   * }
   * }</pre>
   */
  public final UnaryCallable<GetBackupRequest, Backup> getBackupCallable() {
    return stub.getBackupCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Creates a backup.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   LocationName parent = LocationName.of("[PROJECT]", "[LOCATION]");
   *   Backup backup = Backup.newBuilder().build();
   *   String backupId = "backupId2121930365";
   *   Backup response =
   *       cloudFilestoreManagerClient.createBackupAsync(parent, backup, backupId).get();
   * }
   * }</pre>
   *
   * @param parent Required. The backup's project and location, in the format
   *     `projects/{project_id}/locations/{location}`. In Filestore, backup locations map to GCP
   *     regions, for example &#42;&#42;us-west1&#42;&#42;.
   * @param backup Required. A [backup resource][google.cloud.filestore.v1beta1.Backup]
   * @param backupId Required. The ID to use for the backup. The ID must be unique within the
   *     specified project and location.
   *     <p>This value must start with a lowercase letter followed by up to 62 lowercase letters,
   *     numbers, or hyphens, and cannot end with a hyphen.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final OperationFuture<Backup, OperationMetadata> createBackupAsync(
      LocationName parent, Backup backup, String backupId) {
    CreateBackupRequest request =
        CreateBackupRequest.newBuilder()
            .setParent(parent == null ? null : parent.toString())
            .setBackup(backup)
            .setBackupId(backupId)
            .build();
    return createBackupAsync(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Creates a backup.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   String parent = LocationName.of("[PROJECT]", "[LOCATION]").toString();
   *   Backup backup = Backup.newBuilder().build();
   *   String backupId = "backupId2121930365";
   *   Backup response =
   *       cloudFilestoreManagerClient.createBackupAsync(parent, backup, backupId).get();
   * }
   * }</pre>
   *
   * @param parent Required. The backup's project and location, in the format
   *     `projects/{project_id}/locations/{location}`. In Filestore, backup locations map to GCP
   *     regions, for example &#42;&#42;us-west1&#42;&#42;.
   * @param backup Required. A [backup resource][google.cloud.filestore.v1beta1.Backup]
   * @param backupId Required. The ID to use for the backup. The ID must be unique within the
   *     specified project and location.
   *     <p>This value must start with a lowercase letter followed by up to 62 lowercase letters,
   *     numbers, or hyphens, and cannot end with a hyphen.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final OperationFuture<Backup, OperationMetadata> createBackupAsync(
      String parent, Backup backup, String backupId) {
    CreateBackupRequest request =
        CreateBackupRequest.newBuilder()
            .setParent(parent)
            .setBackup(backup)
            .setBackupId(backupId)
            .build();
    return createBackupAsync(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Creates a backup.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   CreateBackupRequest request =
   *       CreateBackupRequest.newBuilder()
   *           .setParent(LocationName.of("[PROJECT]", "[LOCATION]").toString())
   *           .setBackup(Backup.newBuilder().build())
   *           .setBackupId("backupId2121930365")
   *           .build();
   *   Backup response = cloudFilestoreManagerClient.createBackupAsync(request).get();
   * }
   * }</pre>
   *
   * @param request The request object containing all of the parameters for the API call.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final OperationFuture<Backup, OperationMetadata> createBackupAsync(
      CreateBackupRequest request) {
    return createBackupOperationCallable().futureCall(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Creates a backup.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   CreateBackupRequest request =
   *       CreateBackupRequest.newBuilder()
   *           .setParent(LocationName.of("[PROJECT]", "[LOCATION]").toString())
   *           .setBackup(Backup.newBuilder().build())
   *           .setBackupId("backupId2121930365")
   *           .build();
   *   OperationFuture<Backup, OperationMetadata> future =
   *       cloudFilestoreManagerClient.createBackupOperationCallable().futureCall(request);
   *   // Do something.
   *   Backup response = future.get();
   * }
   * }</pre>
   */
  public final OperationCallable<CreateBackupRequest, Backup, OperationMetadata>
      createBackupOperationCallable() {
    return stub.createBackupOperationCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Creates a backup.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   CreateBackupRequest request =
   *       CreateBackupRequest.newBuilder()
   *           .setParent(LocationName.of("[PROJECT]", "[LOCATION]").toString())
   *           .setBackup(Backup.newBuilder().build())
   *           .setBackupId("backupId2121930365")
   *           .build();
   *   ApiFuture<Operation> future =
   *       cloudFilestoreManagerClient.createBackupCallable().futureCall(request);
   *   // Do something.
   *   Operation response = future.get();
   * }
   * }</pre>
   */
  public final UnaryCallable<CreateBackupRequest, Operation> createBackupCallable() {
    return stub.createBackupCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Deletes a backup.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   BackupName name = BackupName.of("[PROJECT]", "[LOCATION]", "[BACKUP]");
   *   cloudFilestoreManagerClient.deleteBackupAsync(name).get();
   * }
   * }</pre>
   *
   * @param name Required. The backup resource name, in the format
   *     `projects/{project_id}/locations/{location}/backups/{backup_id}`
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final OperationFuture<Empty, OperationMetadata> deleteBackupAsync(BackupName name) {
    DeleteBackupRequest request =
        DeleteBackupRequest.newBuilder().setName(name == null ? null : name.toString()).build();
    return deleteBackupAsync(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Deletes a backup.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   String name = BackupName.of("[PROJECT]", "[LOCATION]", "[BACKUP]").toString();
   *   cloudFilestoreManagerClient.deleteBackupAsync(name).get();
   * }
   * }</pre>
   *
   * @param name Required. The backup resource name, in the format
   *     `projects/{project_id}/locations/{location}/backups/{backup_id}`
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final OperationFuture<Empty, OperationMetadata> deleteBackupAsync(String name) {
    DeleteBackupRequest request = DeleteBackupRequest.newBuilder().setName(name).build();
    return deleteBackupAsync(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Deletes a backup.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   DeleteBackupRequest request =
   *       DeleteBackupRequest.newBuilder()
   *           .setName(BackupName.of("[PROJECT]", "[LOCATION]", "[BACKUP]").toString())
   *           .build();
   *   cloudFilestoreManagerClient.deleteBackupAsync(request).get();
   * }
   * }</pre>
   *
   * @param request The request object containing all of the parameters for the API call.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final OperationFuture<Empty, OperationMetadata> deleteBackupAsync(
      DeleteBackupRequest request) {
    return deleteBackupOperationCallable().futureCall(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Deletes a backup.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   DeleteBackupRequest request =
   *       DeleteBackupRequest.newBuilder()
   *           .setName(BackupName.of("[PROJECT]", "[LOCATION]", "[BACKUP]").toString())
   *           .build();
   *   OperationFuture<Empty, OperationMetadata> future =
   *       cloudFilestoreManagerClient.deleteBackupOperationCallable().futureCall(request);
   *   // Do something.
   *   future.get();
   * }
   * }</pre>
   */
  public final OperationCallable<DeleteBackupRequest, Empty, OperationMetadata>
      deleteBackupOperationCallable() {
    return stub.deleteBackupOperationCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Deletes a backup.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   DeleteBackupRequest request =
   *       DeleteBackupRequest.newBuilder()
   *           .setName(BackupName.of("[PROJECT]", "[LOCATION]", "[BACKUP]").toString())
   *           .build();
   *   ApiFuture<Operation> future =
   *       cloudFilestoreManagerClient.deleteBackupCallable().futureCall(request);
   *   // Do something.
   *   future.get();
   * }
   * }</pre>
   */
  public final UnaryCallable<DeleteBackupRequest, Operation> deleteBackupCallable() {
    return stub.deleteBackupCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Updates the settings of a specific backup.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   Backup backup = Backup.newBuilder().build();
   *   FieldMask updateMask = FieldMask.newBuilder().build();
   *   Backup response = cloudFilestoreManagerClient.updateBackupAsync(backup, updateMask).get();
   * }
   * }</pre>
   *
   * @param backup Required. A [backup resource][google.cloud.filestore.v1beta1.Backup]
   * @param updateMask Required. Mask of fields to update. At least one path must be supplied in
   *     this field.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final OperationFuture<Backup, OperationMetadata> updateBackupAsync(
      Backup backup, FieldMask updateMask) {
    UpdateBackupRequest request =
        UpdateBackupRequest.newBuilder().setBackup(backup).setUpdateMask(updateMask).build();
    return updateBackupAsync(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Updates the settings of a specific backup.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   UpdateBackupRequest request =
   *       UpdateBackupRequest.newBuilder()
   *           .setBackup(Backup.newBuilder().build())
   *           .setUpdateMask(FieldMask.newBuilder().build())
   *           .build();
   *   Backup response = cloudFilestoreManagerClient.updateBackupAsync(request).get();
   * }
   * }</pre>
   *
   * @param request The request object containing all of the parameters for the API call.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final OperationFuture<Backup, OperationMetadata> updateBackupAsync(
      UpdateBackupRequest request) {
    return updateBackupOperationCallable().futureCall(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Updates the settings of a specific backup.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   UpdateBackupRequest request =
   *       UpdateBackupRequest.newBuilder()
   *           .setBackup(Backup.newBuilder().build())
   *           .setUpdateMask(FieldMask.newBuilder().build())
   *           .build();
   *   OperationFuture<Backup, OperationMetadata> future =
   *       cloudFilestoreManagerClient.updateBackupOperationCallable().futureCall(request);
   *   // Do something.
   *   Backup response = future.get();
   * }
   * }</pre>
   */
  public final OperationCallable<UpdateBackupRequest, Backup, OperationMetadata>
      updateBackupOperationCallable() {
    return stub.updateBackupOperationCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Updates the settings of a specific backup.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   UpdateBackupRequest request =
   *       UpdateBackupRequest.newBuilder()
   *           .setBackup(Backup.newBuilder().build())
   *           .setUpdateMask(FieldMask.newBuilder().build())
   *           .build();
   *   ApiFuture<Operation> future =
   *       cloudFilestoreManagerClient.updateBackupCallable().futureCall(request);
   *   // Do something.
   *   Operation response = future.get();
   * }
   * }</pre>
   */
  public final UnaryCallable<UpdateBackupRequest, Operation> updateBackupCallable() {
    return stub.updateBackupCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Lists all shares for a specified instance.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   InstanceName parent = InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]");
   *   for (Share element : cloudFilestoreManagerClient.listShares(parent).iterateAll()) {
   *     // doThingsWith(element);
   *   }
   * }
   * }</pre>
   *
   * @param parent Required. The instance for which to retrieve share information, in the format
   *     `projects/{project_id}/locations/{location}/instances/{instance_id}`.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final ListSharesPagedResponse listShares(InstanceName parent) {
    ListSharesRequest request =
        ListSharesRequest.newBuilder().setParent(parent == null ? null : parent.toString()).build();
    return listShares(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Lists all shares for a specified instance.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   String parent = InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]").toString();
   *   for (Share element : cloudFilestoreManagerClient.listShares(parent).iterateAll()) {
   *     // doThingsWith(element);
   *   }
   * }
   * }</pre>
   *
   * @param parent Required. The instance for which to retrieve share information, in the format
   *     `projects/{project_id}/locations/{location}/instances/{instance_id}`.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final ListSharesPagedResponse listShares(String parent) {
    ListSharesRequest request = ListSharesRequest.newBuilder().setParent(parent).build();
    return listShares(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Lists all shares for a specified instance.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   ListSharesRequest request =
   *       ListSharesRequest.newBuilder()
   *           .setParent(InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]").toString())
   *           .setPageSize(883849137)
   *           .setPageToken("pageToken873572522")
   *           .setOrderBy("orderBy-1207110587")
   *           .setFilter("filter-1274492040")
   *           .build();
   *   for (Share element : cloudFilestoreManagerClient.listShares(request).iterateAll()) {
   *     // doThingsWith(element);
   *   }
   * }
   * }</pre>
   *
   * @param request The request object containing all of the parameters for the API call.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final ListSharesPagedResponse listShares(ListSharesRequest request) {
    return listSharesPagedCallable().call(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Lists all shares for a specified instance.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   ListSharesRequest request =
   *       ListSharesRequest.newBuilder()
   *           .setParent(InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]").toString())
   *           .setPageSize(883849137)
   *           .setPageToken("pageToken873572522")
   *           .setOrderBy("orderBy-1207110587")
   *           .setFilter("filter-1274492040")
   *           .build();
   *   ApiFuture<Share> future =
   *       cloudFilestoreManagerClient.listSharesPagedCallable().futureCall(request);
   *   // Do something.
   *   for (Share element : future.get().iterateAll()) {
   *     // doThingsWith(element);
   *   }
   * }
   * }</pre>
   */
  public final UnaryCallable<ListSharesRequest, ListSharesPagedResponse> listSharesPagedCallable() {
    return stub.listSharesPagedCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Lists all shares for a specified instance.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   ListSharesRequest request =
   *       ListSharesRequest.newBuilder()
   *           .setParent(InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]").toString())
   *           .setPageSize(883849137)
   *           .setPageToken("pageToken873572522")
   *           .setOrderBy("orderBy-1207110587")
   *           .setFilter("filter-1274492040")
   *           .build();
   *   while (true) {
   *     ListSharesResponse response =
   *         cloudFilestoreManagerClient.listSharesCallable().call(request);
   *     for (Share element : response.getSharesList()) {
   *       // doThingsWith(element);
   *     }
   *     String nextPageToken = response.getNextPageToken();
   *     if (!Strings.isNullOrEmpty(nextPageToken)) {
   *       request = request.toBuilder().setPageToken(nextPageToken).build();
   *     } else {
   *       break;
   *     }
   *   }
   * }
   * }</pre>
   */
  public final UnaryCallable<ListSharesRequest, ListSharesResponse> listSharesCallable() {
    return stub.listSharesCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Gets the details of a specific share.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   ShareName name = ShareName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]", "[SHARE]");
   *   Share response = cloudFilestoreManagerClient.getShare(name);
   * }
   * }</pre>
   *
   * @param name Required. The share resource name, in the format
   *     `projects/{project_id}/locations/{location}/instances/{instance_id}/shares/{share_id}`
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final Share getShare(ShareName name) {
    GetShareRequest request =
        GetShareRequest.newBuilder().setName(name == null ? null : name.toString()).build();
    return getShare(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Gets the details of a specific share.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   String name = ShareName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]", "[SHARE]").toString();
   *   Share response = cloudFilestoreManagerClient.getShare(name);
   * }
   * }</pre>
   *
   * @param name Required. The share resource name, in the format
   *     `projects/{project_id}/locations/{location}/instances/{instance_id}/shares/{share_id}`
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final Share getShare(String name) {
    GetShareRequest request = GetShareRequest.newBuilder().setName(name).build();
    return getShare(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Gets the details of a specific share.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   GetShareRequest request =
   *       GetShareRequest.newBuilder()
   *           .setName(ShareName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]", "[SHARE]").toString())
   *           .build();
   *   Share response = cloudFilestoreManagerClient.getShare(request);
   * }
   * }</pre>
   *
   * @param request The request object containing all of the parameters for the API call.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final Share getShare(GetShareRequest request) {
    return getShareCallable().call(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Gets the details of a specific share.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   GetShareRequest request =
   *       GetShareRequest.newBuilder()
   *           .setName(ShareName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]", "[SHARE]").toString())
   *           .build();
   *   ApiFuture<Share> future = cloudFilestoreManagerClient.getShareCallable().futureCall(request);
   *   // Do something.
   *   Share response = future.get();
   * }
   * }</pre>
   */
  public final UnaryCallable<GetShareRequest, Share> getShareCallable() {
    return stub.getShareCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Creates a share.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   InstanceName parent = InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]");
   *   Share share = Share.newBuilder().build();
   *   String shareId = "shareId2054217050";
   *   Share response = cloudFilestoreManagerClient.createShareAsync(parent, share, shareId).get();
   * }
   * }</pre>
   *
   * @param parent Required. The Filestore Instance to create the share for, in the format
   *     `projects/{project_id}/locations/{location}/instances/{instance_id}`
   * @param share Required. A share resource
   * @param shareId Required. The ID to use for the share. The ID must be unique within the
   *     specified instance.
   *     <p>This value must start with a lowercase letter followed by up to 62 lowercase letters,
   *     numbers, or hyphens, and cannot end with a hyphen.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final OperationFuture<Share, OperationMetadata> createShareAsync(
      InstanceName parent, Share share, String shareId) {
    CreateShareRequest request =
        CreateShareRequest.newBuilder()
            .setParent(parent == null ? null : parent.toString())
            .setShare(share)
            .setShareId(shareId)
            .build();
    return createShareAsync(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Creates a share.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   String parent = InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]").toString();
   *   Share share = Share.newBuilder().build();
   *   String shareId = "shareId2054217050";
   *   Share response = cloudFilestoreManagerClient.createShareAsync(parent, share, shareId).get();
   * }
   * }</pre>
   *
   * @param parent Required. The Filestore Instance to create the share for, in the format
   *     `projects/{project_id}/locations/{location}/instances/{instance_id}`
   * @param share Required. A share resource
   * @param shareId Required. The ID to use for the share. The ID must be unique within the
   *     specified instance.
   *     <p>This value must start with a lowercase letter followed by up to 62 lowercase letters,
   *     numbers, or hyphens, and cannot end with a hyphen.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final OperationFuture<Share, OperationMetadata> createShareAsync(
      String parent, Share share, String shareId) {
    CreateShareRequest request =
        CreateShareRequest.newBuilder()
            .setParent(parent)
            .setShare(share)
            .setShareId(shareId)
            .build();
    return createShareAsync(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Creates a share.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   CreateShareRequest request =
   *       CreateShareRequest.newBuilder()
   *           .setParent(InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]").toString())
   *           .setShareId("shareId2054217050")
   *           .setShare(Share.newBuilder().build())
   *           .build();
   *   Share response = cloudFilestoreManagerClient.createShareAsync(request).get();
   * }
   * }</pre>
   *
   * @param request The request object containing all of the parameters for the API call.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final OperationFuture<Share, OperationMetadata> createShareAsync(
      CreateShareRequest request) {
    return createShareOperationCallable().futureCall(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Creates a share.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   CreateShareRequest request =
   *       CreateShareRequest.newBuilder()
   *           .setParent(InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]").toString())
   *           .setShareId("shareId2054217050")
   *           .setShare(Share.newBuilder().build())
   *           .build();
   *   OperationFuture<Share, OperationMetadata> future =
   *       cloudFilestoreManagerClient.createShareOperationCallable().futureCall(request);
   *   // Do something.
   *   Share response = future.get();
   * }
   * }</pre>
   */
  public final OperationCallable<CreateShareRequest, Share, OperationMetadata>
      createShareOperationCallable() {
    return stub.createShareOperationCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Creates a share.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   CreateShareRequest request =
   *       CreateShareRequest.newBuilder()
   *           .setParent(InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]").toString())
   *           .setShareId("shareId2054217050")
   *           .setShare(Share.newBuilder().build())
   *           .build();
   *   ApiFuture<Operation> future =
   *       cloudFilestoreManagerClient.createShareCallable().futureCall(request);
   *   // Do something.
   *   Operation response = future.get();
   * }
   * }</pre>
   */
  public final UnaryCallable<CreateShareRequest, Operation> createShareCallable() {
    return stub.createShareCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Deletes a share.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   ShareName name = ShareName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]", "[SHARE]");
   *   cloudFilestoreManagerClient.deleteShareAsync(name).get();
   * }
   * }</pre>
   *
   * @param name Required. The share resource name, in the format
   *     `projects/{project_id}/locations/{location}/instances/{instance_id}/share/{share_id}`
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final OperationFuture<Empty, OperationMetadata> deleteShareAsync(ShareName name) {
    DeleteShareRequest request =
        DeleteShareRequest.newBuilder().setName(name == null ? null : name.toString()).build();
    return deleteShareAsync(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Deletes a share.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   String name = ShareName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]", "[SHARE]").toString();
   *   cloudFilestoreManagerClient.deleteShareAsync(name).get();
   * }
   * }</pre>
   *
   * @param name Required. The share resource name, in the format
   *     `projects/{project_id}/locations/{location}/instances/{instance_id}/share/{share_id}`
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final OperationFuture<Empty, OperationMetadata> deleteShareAsync(String name) {
    DeleteShareRequest request = DeleteShareRequest.newBuilder().setName(name).build();
    return deleteShareAsync(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Deletes a share.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   DeleteShareRequest request =
   *       DeleteShareRequest.newBuilder()
   *           .setName(ShareName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]", "[SHARE]").toString())
   *           .build();
   *   cloudFilestoreManagerClient.deleteShareAsync(request).get();
   * }
   * }</pre>
   *
   * @param request The request object containing all of the parameters for the API call.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final OperationFuture<Empty, OperationMetadata> deleteShareAsync(
      DeleteShareRequest request) {
    return deleteShareOperationCallable().futureCall(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Deletes a share.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   DeleteShareRequest request =
   *       DeleteShareRequest.newBuilder()
   *           .setName(ShareName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]", "[SHARE]").toString())
   *           .build();
   *   OperationFuture<Empty, OperationMetadata> future =
   *       cloudFilestoreManagerClient.deleteShareOperationCallable().futureCall(request);
   *   // Do something.
   *   future.get();
   * }
   * }</pre>
   */
  public final OperationCallable<DeleteShareRequest, Empty, OperationMetadata>
      deleteShareOperationCallable() {
    return stub.deleteShareOperationCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Deletes a share.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   DeleteShareRequest request =
   *       DeleteShareRequest.newBuilder()
   *           .setName(ShareName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]", "[SHARE]").toString())
   *           .build();
   *   ApiFuture<Operation> future =
   *       cloudFilestoreManagerClient.deleteShareCallable().futureCall(request);
   *   // Do something.
   *   future.get();
   * }
   * }</pre>
   */
  public final UnaryCallable<DeleteShareRequest, Operation> deleteShareCallable() {
    return stub.deleteShareCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Updates the settings of a specific share.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   Share share = Share.newBuilder().build();
   *   FieldMask updateMask = FieldMask.newBuilder().build();
   *   Share response = cloudFilestoreManagerClient.updateShareAsync(share, updateMask).get();
   * }
   * }</pre>
   *
   * @param share Required. A share resource. Only fields specified in update_mask are updated.
   * @param updateMask Required. Mask of fields to update. At least one path must be supplied in
   *     this field. The elements of the repeated paths field may only include these fields:
   *     <ul>
   *       <li>"description"
   *       <li>"capacity_gb"
   *       <li>"labels"
   *       <li>"nfs_export_options"
   *     </ul>
   *
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final OperationFuture<Share, OperationMetadata> updateShareAsync(
      Share share, FieldMask updateMask) {
    UpdateShareRequest request =
        UpdateShareRequest.newBuilder().setShare(share).setUpdateMask(updateMask).build();
    return updateShareAsync(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Updates the settings of a specific share.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   UpdateShareRequest request =
   *       UpdateShareRequest.newBuilder()
   *           .setShare(Share.newBuilder().build())
   *           .setUpdateMask(FieldMask.newBuilder().build())
   *           .build();
   *   Share response = cloudFilestoreManagerClient.updateShareAsync(request).get();
   * }
   * }</pre>
   *
   * @param request The request object containing all of the parameters for the API call.
   * @throws com.google.api.gax.rpc.ApiException if the remote call fails
   */
  public final OperationFuture<Share, OperationMetadata> updateShareAsync(
      UpdateShareRequest request) {
    return updateShareOperationCallable().futureCall(request);
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Updates the settings of a specific share.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   UpdateShareRequest request =
   *       UpdateShareRequest.newBuilder()
   *           .setShare(Share.newBuilder().build())
   *           .setUpdateMask(FieldMask.newBuilder().build())
   *           .build();
   *   OperationFuture<Share, OperationMetadata> future =
   *       cloudFilestoreManagerClient.updateShareOperationCallable().futureCall(request);
   *   // Do something.
   *   Share response = future.get();
   * }
   * }</pre>
   */
  public final OperationCallable<UpdateShareRequest, Share, OperationMetadata>
      updateShareOperationCallable() {
    return stub.updateShareOperationCallable();
  }

  // AUTO-GENERATED DOCUMENTATION AND METHOD.
  /**
   * Updates the settings of a specific share.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * // This snippet has been automatically generated and should be regarded as a code template only.
   * // It will require modifications to work:
   * // - It may require correct/in-range values for request initialization.
   * // - It may require specifying regional endpoints when creating the service client as shown in
   * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
   * try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
   *     CloudFilestoreManagerClient.create()) {
   *   UpdateShareRequest request =
   *       UpdateShareRequest.newBuilder()
   *           .setShare(Share.newBuilder().build())
   *           .setUpdateMask(FieldMask.newBuilder().build())
   *           .build();
   *   ApiFuture<Operation> future =
   *       cloudFilestoreManagerClient.updateShareCallable().futureCall(request);
   *   // Do something.
   *   Operation response = future.get();
   * }
   * }</pre>
   */
  public final UnaryCallable<UpdateShareRequest, Operation> updateShareCallable() {
    return stub.updateShareCallable();
  }

  @Override
  public final void close() {
    stub.close();
  }

  @Override
  public void shutdown() {
    stub.shutdown();
  }

  @Override
  public boolean isShutdown() {
    return stub.isShutdown();
  }

  @Override
  public boolean isTerminated() {
    return stub.isTerminated();
  }

  @Override
  public void shutdownNow() {
    stub.shutdownNow();
  }

  @Override
  public boolean awaitTermination(long duration, TimeUnit unit) throws InterruptedException {
    return stub.awaitTermination(duration, unit);
  }

  public static class ListInstancesPagedResponse
      extends AbstractPagedListResponse<
          ListInstancesRequest,
          ListInstancesResponse,
          Instance,
          ListInstancesPage,
          ListInstancesFixedSizeCollection> {

    public static ApiFuture<ListInstancesPagedResponse> createAsync(
        PageContext<ListInstancesRequest, ListInstancesResponse, Instance> context,
        ApiFuture<ListInstancesResponse> futureResponse) {
      ApiFuture<ListInstancesPage> futurePage =
          ListInstancesPage.createEmptyPage().createPageAsync(context, futureResponse);
      return ApiFutures.transform(
          futurePage,
          input -> new ListInstancesPagedResponse(input),
          MoreExecutors.directExecutor());
    }

    private ListInstancesPagedResponse(ListInstancesPage page) {
      super(page, ListInstancesFixedSizeCollection.createEmptyCollection());
    }
  }

  public static class ListInstancesPage
      extends AbstractPage<
          ListInstancesRequest, ListInstancesResponse, Instance, ListInstancesPage> {

    private ListInstancesPage(
        PageContext<ListInstancesRequest, ListInstancesResponse, Instance> context,
        ListInstancesResponse response) {
      super(context, response);
    }

    private static ListInstancesPage createEmptyPage() {
      return new ListInstancesPage(null, null);
    }

    @Override
    protected ListInstancesPage createPage(
        PageContext<ListInstancesRequest, ListInstancesResponse, Instance> context,
        ListInstancesResponse response) {
      return new ListInstancesPage(context, response);
    }

    @Override
    public ApiFuture<ListInstancesPage> createPageAsync(
        PageContext<ListInstancesRequest, ListInstancesResponse, Instance> context,
        ApiFuture<ListInstancesResponse> futureResponse) {
      return super.createPageAsync(context, futureResponse);
    }
  }

  public static class ListInstancesFixedSizeCollection
      extends AbstractFixedSizeCollection<
          ListInstancesRequest,
          ListInstancesResponse,
          Instance,
          ListInstancesPage,
          ListInstancesFixedSizeCollection> {

    private ListInstancesFixedSizeCollection(List<ListInstancesPage> pages, int collectionSize) {
      super(pages, collectionSize);
    }

    private static ListInstancesFixedSizeCollection createEmptyCollection() {
      return new ListInstancesFixedSizeCollection(null, 0);
    }

    @Override
    protected ListInstancesFixedSizeCollection createCollection(
        List<ListInstancesPage> pages, int collectionSize) {
      return new ListInstancesFixedSizeCollection(pages, collectionSize);
    }
  }

  public static class ListSnapshotsPagedResponse
      extends AbstractPagedListResponse<
          ListSnapshotsRequest,
          ListSnapshotsResponse,
          Snapshot,
          ListSnapshotsPage,
          ListSnapshotsFixedSizeCollection> {

    public static ApiFuture<ListSnapshotsPagedResponse> createAsync(
        PageContext<ListSnapshotsRequest, ListSnapshotsResponse, Snapshot> context,
        ApiFuture<ListSnapshotsResponse> futureResponse) {
      ApiFuture<ListSnapshotsPage> futurePage =
          ListSnapshotsPage.createEmptyPage().createPageAsync(context, futureResponse);
      return ApiFutures.transform(
          futurePage,
          input -> new ListSnapshotsPagedResponse(input),
          MoreExecutors.directExecutor());
    }

    private ListSnapshotsPagedResponse(ListSnapshotsPage page) {
      super(page, ListSnapshotsFixedSizeCollection.createEmptyCollection());
    }
  }

  public static class ListSnapshotsPage
      extends AbstractPage<
          ListSnapshotsRequest, ListSnapshotsResponse, Snapshot, ListSnapshotsPage> {

    private ListSnapshotsPage(
        PageContext<ListSnapshotsRequest, ListSnapshotsResponse, Snapshot> context,
        ListSnapshotsResponse response) {
      super(context, response);
    }

    private static ListSnapshotsPage createEmptyPage() {
      return new ListSnapshotsPage(null, null);
    }

    @Override
    protected ListSnapshotsPage createPage(
        PageContext<ListSnapshotsRequest, ListSnapshotsResponse, Snapshot> context,
        ListSnapshotsResponse response) {
      return new ListSnapshotsPage(context, response);
    }

    @Override
    public ApiFuture<ListSnapshotsPage> createPageAsync(
        PageContext<ListSnapshotsRequest, ListSnapshotsResponse, Snapshot> context,
        ApiFuture<ListSnapshotsResponse> futureResponse) {
      return super.createPageAsync(context, futureResponse);
    }
  }

  public static class ListSnapshotsFixedSizeCollection
      extends AbstractFixedSizeCollection<
          ListSnapshotsRequest,
          ListSnapshotsResponse,
          Snapshot,
          ListSnapshotsPage,
          ListSnapshotsFixedSizeCollection> {

    private ListSnapshotsFixedSizeCollection(List<ListSnapshotsPage> pages, int collectionSize) {
      super(pages, collectionSize);
    }

    private static ListSnapshotsFixedSizeCollection createEmptyCollection() {
      return new ListSnapshotsFixedSizeCollection(null, 0);
    }

    @Override
    protected ListSnapshotsFixedSizeCollection createCollection(
        List<ListSnapshotsPage> pages, int collectionSize) {
      return new ListSnapshotsFixedSizeCollection(pages, collectionSize);
    }
  }

  public static class ListBackupsPagedResponse
      extends AbstractPagedListResponse<
          ListBackupsRequest,
          ListBackupsResponse,
          Backup,
          ListBackupsPage,
          ListBackupsFixedSizeCollection> {

    public static ApiFuture<ListBackupsPagedResponse> createAsync(
        PageContext<ListBackupsRequest, ListBackupsResponse, Backup> context,
        ApiFuture<ListBackupsResponse> futureResponse) {
      ApiFuture<ListBackupsPage> futurePage =
          ListBackupsPage.createEmptyPage().createPageAsync(context, futureResponse);
      return ApiFutures.transform(
          futurePage, input -> new ListBackupsPagedResponse(input), MoreExecutors.directExecutor());
    }

    private ListBackupsPagedResponse(ListBackupsPage page) {
      super(page, ListBackupsFixedSizeCollection.createEmptyCollection());
    }
  }

  public static class ListBackupsPage
      extends AbstractPage<ListBackupsRequest, ListBackupsResponse, Backup, ListBackupsPage> {

    private ListBackupsPage(
        PageContext<ListBackupsRequest, ListBackupsResponse, Backup> context,
        ListBackupsResponse response) {
      super(context, response);
    }

    private static ListBackupsPage createEmptyPage() {
      return new ListBackupsPage(null, null);
    }

    @Override
    protected ListBackupsPage createPage(
        PageContext<ListBackupsRequest, ListBackupsResponse, Backup> context,
        ListBackupsResponse response) {
      return new ListBackupsPage(context, response);
    }

    @Override
    public ApiFuture<ListBackupsPage> createPageAsync(
        PageContext<ListBackupsRequest, ListBackupsResponse, Backup> context,
        ApiFuture<ListBackupsResponse> futureResponse) {
      return super.createPageAsync(context, futureResponse);
    }
  }

  public static class ListBackupsFixedSizeCollection
      extends AbstractFixedSizeCollection<
          ListBackupsRequest,
          ListBackupsResponse,
          Backup,
          ListBackupsPage,
          ListBackupsFixedSizeCollection> {

    private ListBackupsFixedSizeCollection(List<ListBackupsPage> pages, int collectionSize) {
      super(pages, collectionSize);
    }

    private static ListBackupsFixedSizeCollection createEmptyCollection() {
      return new ListBackupsFixedSizeCollection(null, 0);
    }

    @Override
    protected ListBackupsFixedSizeCollection createCollection(
        List<ListBackupsPage> pages, int collectionSize) {
      return new ListBackupsFixedSizeCollection(pages, collectionSize);
    }
  }

  public static class ListSharesPagedResponse
      extends AbstractPagedListResponse<
          ListSharesRequest,
          ListSharesResponse,
          Share,
          ListSharesPage,
          ListSharesFixedSizeCollection> {

    public static ApiFuture<ListSharesPagedResponse> createAsync(
        PageContext<ListSharesRequest, ListSharesResponse, Share> context,
        ApiFuture<ListSharesResponse> futureResponse) {
      ApiFuture<ListSharesPage> futurePage =
          ListSharesPage.createEmptyPage().createPageAsync(context, futureResponse);
      return ApiFutures.transform(
          futurePage, input -> new ListSharesPagedResponse(input), MoreExecutors.directExecutor());
    }

    private ListSharesPagedResponse(ListSharesPage page) {
      super(page, ListSharesFixedSizeCollection.createEmptyCollection());
    }
  }

  public static class ListSharesPage
      extends AbstractPage<ListSharesRequest, ListSharesResponse, Share, ListSharesPage> {

    private ListSharesPage(
        PageContext<ListSharesRequest, ListSharesResponse, Share> context,
        ListSharesResponse response) {
      super(context, response);
    }

    private static ListSharesPage createEmptyPage() {
      return new ListSharesPage(null, null);
    }

    @Override
    protected ListSharesPage createPage(
        PageContext<ListSharesRequest, ListSharesResponse, Share> context,
        ListSharesResponse response) {
      return new ListSharesPage(context, response);
    }

    @Override
    public ApiFuture<ListSharesPage> createPageAsync(
        PageContext<ListSharesRequest, ListSharesResponse, Share> context,
        ApiFuture<ListSharesResponse> futureResponse) {
      return super.createPageAsync(context, futureResponse);
    }
  }

  public static class ListSharesFixedSizeCollection
      extends AbstractFixedSizeCollection<
          ListSharesRequest,
          ListSharesResponse,
          Share,
          ListSharesPage,
          ListSharesFixedSizeCollection> {

    private ListSharesFixedSizeCollection(List<ListSharesPage> pages, int collectionSize) {
      super(pages, collectionSize);
    }

    private static ListSharesFixedSizeCollection createEmptyCollection() {
      return new ListSharesFixedSizeCollection(null, 0);
    }

    @Override
    protected ListSharesFixedSizeCollection createCollection(
        List<ListSharesPage> pages, int collectionSize) {
      return new ListSharesFixedSizeCollection(pages, collectionSize);
    }
  }
}
