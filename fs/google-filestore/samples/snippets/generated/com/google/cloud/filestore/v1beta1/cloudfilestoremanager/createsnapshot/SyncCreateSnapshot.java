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

package com.google.cloud.filestore.v1beta1.samples;

// [START file_v1beta1_generated_CloudFilestoreManager_CreateSnapshot_sync]
import com.google.cloud.filestore.v1beta1.CloudFilestoreManagerClient;
import com.google.cloud.filestore.v1beta1.CreateSnapshotRequest;
import com.google.cloud.filestore.v1beta1.InstanceName;
import com.google.cloud.filestore.v1beta1.Snapshot;

public class SyncCreateSnapshot {

  public static void main(String[] args) throws Exception {
    syncCreateSnapshot();
  }

  public static void syncCreateSnapshot() throws Exception {
    // This snippet has been automatically generated and should be regarded as a code template only.
    // It will require modifications to work:
    // - It may require correct/in-range values for request initialization.
    // - It may require specifying regional endpoints when creating the service client as shown in
    // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
    try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
        CloudFilestoreManagerClient.create()) {
      CreateSnapshotRequest request =
          CreateSnapshotRequest.newBuilder()
              .setParent(InstanceName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]").toString())
              .setSnapshotId("snapshotId-1113817601")
              .setSnapshot(Snapshot.newBuilder().build())
              .build();
      Snapshot response = cloudFilestoreManagerClient.createSnapshotAsync(request).get();
    }
  }
}
// [END file_v1beta1_generated_CloudFilestoreManager_CreateSnapshot_sync]
