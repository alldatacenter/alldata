/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.obs.services.internal;

public interface IHeaders {
    String defaultStorageClassHeader();

    String epidHeader();

    String aclHeader();

    String requestIdHeader();

    String requestId2Header();

    String bucketRegionHeader();

    String locationHeader();

    String storageClassHeader();

    String websiteRedirectLocationHeader();

    String successRedirectLocationHeader();

    String sseKmsHeader();

    String sseKmsKeyHeader();

    String sseKmsProjectIdHeader();

    String sseCHeader();

    String sseCKeyHeader();

    String sseCKeyMd5Header();

    String expiresHeader();

    String versionIdHeader();

    String copySourceHeader();

    String copySourceRangeHeader();

    String copySourceVersionIdHeader();

    String copySourceSseCHeader();

    String copySourceSseCKeyHeader();

    String copySourceSseCKeyMd5Header();

    String metadataDirectiveHeader();

    String dateHeader();

    String deleteMarkerHeader();

    String headerPrefix();

    String headerMetaPrefix();

    String securityTokenHeader();

    String contentSha256Header();

    String listTimeoutHeader();

    String objectTypeHeader();

    String nextPositionHeader();

    String expirationHeader();

    String restoreHeader();

    String serverVersionHeader();

    String grantReadHeader();

    String grantWriteHeader();

    String grantReadAcpHeader();

    String grantWriteAcpHeader();

    String grantFullControlHeader();

    String grantReadDeliveredHeader();

    String grantFullControlDeliveredHeader();

    String copySourceIfModifiedSinceHeader();

    String copySourceIfUnmodifiedSinceHeader();

    String copySourceIfNoneMatchHeader();

    String copySourceIfMatchHeader();

    String fsFileInterfaceHeader();

    String fsModeHeader();

    String azRedundancyHeader();

    String bucketTypeHeader();

    String requestPaymentHeader();
}
