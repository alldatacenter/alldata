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

package com.obs.services.model.fs;

import java.util.List;

import com.obs.services.model.AvailableZoneEnum;
import com.obs.services.model.BucketMetadataInfoResult;
import com.obs.services.model.BucketTypeEnum;
import com.obs.services.model.StorageClassEnum;

/**
 * Response to a request of obtaining status of the file gateway feature of a
 * bucket
 *
 */
public class GetBucketFSStatusResult extends BucketMetadataInfoResult {

    private FSStatusEnum status;

    public GetBucketFSStatusResult(String allowOrigin, List<String> allowHeaders, int maxAge, List<String> allowMethods,
            List<String> exposeHeaders, StorageClassEnum storageClass, String location, String obsVersion) {
        super(allowOrigin, allowHeaders, maxAge, allowMethods, exposeHeaders, storageClass, location, obsVersion);
    }

    @Deprecated
    //CHECKSTYLE:OFF
    public GetBucketFSStatusResult(String allowOrigin, List<String> allowHeaders, int maxAge, List<String> allowMethods,
            List<String> exposeHeaders, StorageClassEnum storageClass, String location, String obsVersion,
            FSStatusEnum status) {
        this(allowOrigin, allowHeaders, maxAge, allowMethods, exposeHeaders, storageClass, location, obsVersion);
        this.status = status;
    }

    @Deprecated
    //CHECKSTYLE:OFF
    public GetBucketFSStatusResult(String allowOrigin, List<String> allowHeaders, int maxAge, List<String> allowMethods,
            List<String> exposeHeaders, StorageClassEnum storageClass, String location, String obsVersion,
            FSStatusEnum status, AvailableZoneEnum availableZone) {
        this(allowOrigin, allowHeaders, maxAge, allowMethods, exposeHeaders, storageClass, location, obsVersion);
        this.availableZone = availableZone;
        this.status = status;
    }

    @Deprecated
    //CHECKSTYLE:OFF
    public GetBucketFSStatusResult(String allowOrigin, List<String> allowHeaders, int maxAge, List<String> allowMethods,
            List<String> exposeHeaders, StorageClassEnum storageClass, String location, String obsVersion,
            FSStatusEnum status, AvailableZoneEnum availableZone, String epid, BucketTypeEnum bucketTypeEnum) {
        this(allowOrigin, allowHeaders, maxAge, allowMethods, exposeHeaders, storageClass, location, obsVersion);
        this.availableZone = availableZone;
        this.epid = epid;
        this.bucketType = bucketTypeEnum;
        this.status = status;
    }

    private GetBucketFSStatusResult(Builder builder) {
        super();
        this.allowOrigin = builder.allowOrigin;
        this.allowHeaders = builder.allowHeaders;
        this.maxAge = builder.maxAge;
        this.allowMethods = builder.allowMethods;
        this.exposeHeaders = builder.exposeHeaders;
        this.storageClass = builder.storageClass;
        this.location = builder.location;
        this.obsVersion = builder.obsVersion;
        this.availableZone = builder.availableZone;
        this.epid = builder.epid;
        this.bucketType = builder.bucketType;
        this.status = builder.status;
    }
    
    public static final class Builder {
        private String allowOrigin;
        private List<String> allowHeaders;
        private int maxAge;
        private List<String> allowMethods;
        private List<String> exposeHeaders;
        private StorageClassEnum storageClass;
        private String location;
        private String obsVersion;
        private AvailableZoneEnum availableZone;
        private String epid;
        private BucketTypeEnum bucketType = BucketTypeEnum.OBJECT;
        private FSStatusEnum status;
        
        public Builder allowOrigin(String allowOrigin) {
            this.allowOrigin = allowOrigin;
            return this;
        }
        
        public Builder allowHeaders(List<String> allowHeaders) {
            this.allowHeaders = allowHeaders;
            return this;
        }
        
        public Builder maxAge(int maxAge) {
            this.maxAge = maxAge;
            return this;
        }
        
        public Builder allowMethods(List<String> allowMethods) {
            this.allowMethods = allowMethods;
            return this;
        }
        
        public Builder exposeHeaders(List<String> exposeHeaders) {
            this.exposeHeaders = exposeHeaders;
            return this;
        }
        
        public Builder storageClass(StorageClassEnum storageClass) {
            this.storageClass = storageClass;
            return this;
        }
        
        public Builder location(String location) {
            this.location = location;
            return this;
        }
        
        public Builder obsVersion(String obsVersion) {
            this.obsVersion = obsVersion;
            return this;
        }
        
        public Builder availableZone(AvailableZoneEnum availableZone) {
            this.availableZone = availableZone;
            return this;
        }
        
        public Builder epid(String epid) {
            this.epid = epid;
            return this;
        }
        
        public Builder bucketType(BucketTypeEnum bucketType) {
            this.bucketType = bucketType;
            return this;
        }
        
        public Builder status(FSStatusEnum status) {
            this.status = status;
            return this;
        }
        
        public GetBucketFSStatusResult build() {
            return new GetBucketFSStatusResult(this);
        }
        
        public BucketMetadataInfoResult.Builder builderBucketMetadataInfo() {
            return new BucketMetadataInfoResult.Builder()
                    .allowOrigin(this.allowOrigin)
                    .allowHeaders(this.allowHeaders)
                    .maxAge(this.maxAge)
                    .allowMethods(this.allowMethods)
                    .exposeHeaders(this.exposeHeaders)
                    .storageClass(this.storageClass)
                    .location(this.location)
                    .obsVersion(this.obsVersion)
                    .availableZone(this.availableZone)
                    .epid(this.epid)
                    .bucketType(this.bucketType);
        }
    }

    /**
     * Obtain the status of the file gateway feature of a bucket.
     * 
     * @return Status of the file gateway feature
     */
    public FSStatusEnum getStatus() {
        return status;
    }

}
