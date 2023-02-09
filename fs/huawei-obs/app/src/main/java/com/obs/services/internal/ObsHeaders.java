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

public class ObsHeaders extends V2Headers {

    private static ObsHeaders instance = new ObsHeaders();

    public static IHeaders getInstance() {
        return instance;
    }

    @Override
    public String defaultStorageClassHeader() {
        return this.headerPrefix() + "storage-class";
    }

    @Override
    public String sseKmsKeyHeader() {
        return this.headerPrefix() + "server-side-encryption-kms-key-id";
    }

    @Override
    public String expiresHeader() {
        return this.headerPrefix() + "expires";
    }

    @Override
    public String headerPrefix() {
        return Constants.OBS_HEADER_PREFIX;
    }

    @Override
    public String headerMetaPrefix() {
        return Constants.OBS_HEADER_META_PREFIX;
    }

    @Override
    public String grantReadDeliveredHeader() {
        return this.headerPrefix() + "grant-read-delivered";
    }

    @Override
    public String grantFullControlDeliveredHeader() {
        return this.headerPrefix() + "grant-full-control-delivered";
    }

    @Override
    public String serverVersionHeader() {
        return this.headerPrefix() + "version";
    }

    @Override
    public String bucketRegionHeader() {
        return this.headerPrefix() + "bucket-location";
    }

    @Override
    public String locationHeader() {
        return null;
    }

    @Override
    public String successRedirectLocationHeader() {
        return "success-action-redirect";
    }

    @Override
    public String contentSha256Header() {
        return null;
    }

    @Override
    public String objectTypeHeader() {
        return this.headerPrefix() + "object-type";
    }

    @Override
    public String nextPositionHeader() {
        return this.headerPrefix() + "next-append-position";
    }

    @Override
    public String fsFileInterfaceHeader() {
        return this.headerPrefix() + "fs-file-interface";
    }

    @Override
    public String azRedundancyHeader() {
        return this.headerPrefix() + "az-redundancy";
    }

    @Override
    public String bucketTypeHeader() {
        return this.headerPrefix() + "bucket-type";
    }
}
