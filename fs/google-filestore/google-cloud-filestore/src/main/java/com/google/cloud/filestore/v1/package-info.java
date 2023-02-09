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

/**
 * A client to Cloud Filestore API
 *
 * <p>The interfaces provided are listed below, along with usage samples.
 *
 * <p>======================= CloudFilestoreManagerClient =======================
 *
 * <p>Service Description: Configures and manages Cloud Filestore resources.
 *
 * <p>Cloud Filestore Manager v1.
 *
 * <p>The `file.googleapis.com` service implements the Cloud Filestore API and defines the following
 * resource model for managing instances:
 *
 * <ul>
 *   <li>The service works with a collection of cloud projects, named: `/projects/&#42;`
 *   <li>Each project has a collection of available locations, named: `/locations/&#42;`
 *   <li>Each location has a collection of instances and backups, named: `/instances/&#42;` and
 *       `/backups/&#42;` respectively.
 *   <li>As such, Cloud Filestore instances are resources of the form:
 *       `/projects/{project_number}/locations/{location_id}/instances/{instance_id}` and backups
 *       are resources of the form:
 *       `/projects/{project_number}/locations/{location_id}/backup/{backup_id}`
 * </ul>
 *
 * <p>Note that location_id must be a GCP `zone` for instances and but to a GCP `region` for
 * backups; for example:
 *
 * <ul>
 *   <li>`projects/12345/locations/us-central1-c/instances/my-filestore`
 *   <li>`projects/12345/locations/us-central1/backups/my-backup`
 * </ul>
 *
 * <p>Sample for CloudFilestoreManagerClient:
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
 */
@Generated("by gapic-generator-java")
package com.google.cloud.filestore.v1;

import javax.annotation.Generated;
