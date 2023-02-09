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

// [START file_v1beta1_generated_CloudFilestoreManager_GetShare_async]
import com.google.api.core.ApiFuture;
import com.google.cloud.filestore.v1beta1.CloudFilestoreManagerClient;
import com.google.cloud.filestore.v1beta1.GetShareRequest;
import com.google.cloud.filestore.v1beta1.Share;
import com.google.cloud.filestore.v1beta1.ShareName;

public class AsyncGetShare {

  public static void main(String[] args) throws Exception {
    asyncGetShare();
  }

  public static void asyncGetShare() throws Exception {
    // This snippet has been automatically generated and should be regarded as a code template only.
    // It will require modifications to work:
    // - It may require correct/in-range values for request initialization.
    // - It may require specifying regional endpoints when creating the service client as shown in
    // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
    try (CloudFilestoreManagerClient cloudFilestoreManagerClient =
        CloudFilestoreManagerClient.create()) {
      GetShareRequest request =
          GetShareRequest.newBuilder()
              .setName(ShareName.of("[PROJECT]", "[LOCATION]", "[INSTANCE]", "[SHARE]").toString())
              .build();
      ApiFuture<Share> future = cloudFilestoreManagerClient.getShareCallable().futureCall(request);
      // Do something.
      Share response = future.get();
    }
  }
}
// [END file_v1beta1_generated_CloudFilestoreManager_GetShare_async]
