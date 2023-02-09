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
**/

package com.obs.services.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Response to a request for listing multipart uploads
 */
public class MultipartUploadListing extends HeaderResponse {
    private String bucketName;

    private String keyMarker;

    private String uploadIdMarker;

    private String nextKeyMarker;

    private String nextUploadIdMarker;

    private String prefix;

    private int maxUploads;

    private boolean truncated;

    private List<MultipartUpload> multipartTaskList;

    private String delimiter;

    private String[] commonPrefixes;

    @Deprecated
    //CHECKSTYLE:OFF
    public MultipartUploadListing(String bucketName, String keyMarker, String uploadIdMarker, String nextKeyMarker,
            String nextUploadIdMarker, String prefix, int maxUploads, boolean truncated,
            List<MultipartUpload> multipartTaskList, String delimiter, String[] commonPrefixes) {
        super();
        this.bucketName = bucketName;
        this.keyMarker = keyMarker;
        this.uploadIdMarker = uploadIdMarker;
        this.nextKeyMarker = nextKeyMarker;
        this.nextUploadIdMarker = nextUploadIdMarker;
        this.prefix = prefix;
        this.maxUploads = maxUploads;
        this.truncated = truncated;
        this.multipartTaskList = multipartTaskList;
        this.delimiter = delimiter;
        if (null != commonPrefixes) {
            this.commonPrefixes = commonPrefixes.clone();
        } else {
            this.commonPrefixes = null;
        }
    }

    private MultipartUploadListing(Builder builder) {
        super();
        this.bucketName = builder.bucketName;
        this.keyMarker = builder.keyMarker;
        this.uploadIdMarker = builder.uploadIdMarker;
        this.nextKeyMarker = builder.nextKeyMarker;
        this.nextUploadIdMarker = builder.nextUploadIdMarker;
        this.prefix = builder.prefix;
        this.maxUploads = builder.maxUploads;
        this.truncated = builder.truncated;
        this.multipartTaskList = builder.multipartTaskList;
        this.delimiter = builder.delimiter;
        if (null != builder.commonPrefixes) {
            this.commonPrefixes = builder.commonPrefixes.clone();
        } else {
            this.commonPrefixes = null;
        }
    }
    
    public static final class Builder {
        private String bucketName;
        private String keyMarker;
        private String uploadIdMarker;
        private String nextKeyMarker;
        private String nextUploadIdMarker;
        private String prefix;
        private int maxUploads;
        private boolean truncated;
        private List<MultipartUpload> multipartTaskList;
        private String delimiter;
        private String[] commonPrefixes;
        
        public Builder bucketName(String bucketName) {
            this.bucketName = bucketName;
            return this;
        }
        
        public Builder keyMarker(String keyMarker) {
            this.keyMarker = keyMarker;
            return this;
        }
        
        public Builder uploadIdMarker(String uploadIdMarker) {
            this.uploadIdMarker = uploadIdMarker;
            return this;
        }
        
        public Builder nextKeyMarker(String nextKeyMarker) {
            this.nextKeyMarker = nextKeyMarker;
            return this;
        }
        
        public Builder nextUploadIdMarker(String nextUploadIdMarker) {
            this.nextUploadIdMarker = nextUploadIdMarker;
            return this;
        }
        
        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }
        
        public Builder maxUploads(int maxUploads) {
            this.maxUploads = maxUploads;
            return this;
        }
        
        public Builder truncated(boolean truncated) {
            this.truncated = truncated;
            return this;
        }
        
        public Builder multipartTaskList(List<MultipartUpload> multipartTaskList) {
            this.multipartTaskList = multipartTaskList;
            return this;
        }
        
        public Builder delimiter(String delimiter) {
            this.delimiter = delimiter;
            return this;
        }
        
        public Builder commonPrefixes(String[] commonPrefixes) {
            if (null != commonPrefixes) {
                this.commonPrefixes = commonPrefixes.clone();
            } else {
                this.commonPrefixes = null;
            }
            return this;
        }
        
        public MultipartUploadListing builder() {
            return new MultipartUploadListing(this);
        }
    }
    
    /**
     * Check whether the query result list is truncated. Value "true" indicates
     * that the results are incomplete while value "false" indicates that the
     * results are complete.
     * 
     * @return Truncation identifier
     */
    public boolean isTruncated() {
        return truncated;
    }

    /**
     * Obtain the list of prefixes to the names of grouped objects.
     * 
     * @return List of prefixes to the names of grouped objects
     */
    public String[] getCommonPrefixes() {
        if (null != commonPrefixes) {
            return commonPrefixes.clone();
        }
        return new String[0];
    }

    /**
     * Obtain the start position for listing multipart uploads in the request
     * (sorted by multipart upload ID).
     * 
     * @return Start position for listing multipart uploads in the request
     */
    public String getUploadIdMarker() {
        return uploadIdMarker;
    }

    /**
     * Start position for next listing (sorted by object name)
     * 
     * @return Start position for next listing
     */
    public String getNextKeyMarker() {
        return nextKeyMarker;
    }

    /**
     * Obtain the start position for next listing (sorted by multipart upload
     * ID).
     * 
     * @return Start position for next listing
     */
    public String getNextUploadIdMarker() {
        return nextUploadIdMarker;
    }

    /**
     * Obtain the list of multipart uploads unfinished in the bucket.
     * 
     * @return List of multipart uploads unfinished in the bucket
     */
    public List<MultipartUpload> getMultipartTaskList() {
        if (this.multipartTaskList == null) {
            this.multipartTaskList = new ArrayList<MultipartUpload>();
        }
        return multipartTaskList;
    }

    /**
     * Obtain the name of the bucket to which the multipart uploads belong.
     * 
     * @return Name of the bucket to which the multipart uploads belong
     */
    public String getBucketName() {
        return bucketName;
    }

    /**
     * Obtain the delimiter in the request for listing multipart uploads.
     * 
     * @return Delimiter in the request for listing multipart uploads
     */
    public String getDelimiter() {
        return delimiter;
    }

    /**
     * Obtain the start position for listing multipart uploads (sorted by object
     * name)
     * 
     * @return Start position for listing multipart uploads
     */
    public String getKeyMarker() {
        return keyMarker;
    }

    /**
     * Obtain the maximum number of multipart uploads to be listed.
     * 
     * @return Maximum number of multipart uploads to be listed
     */
    public int getMaxUploads() {
        return maxUploads;
    }

    /**
     * Obtain the prefix for listing multipart uploads.
     * 
     * @return Prefix for listing multipart uploads
     */
    public String getPrefix() {
        return prefix;
    }

    @Override
    public String toString() {
        return "MultipartUploadListing [bucketName=" + bucketName + ", keyMarker=" + keyMarker + ", uploadIdMarker="
                + uploadIdMarker + ", nextKeyMarker=" + nextKeyMarker + ", nextUploadIdMarker=" + nextUploadIdMarker
                + ", prefix=" + prefix + ", maxUploads=" + maxUploads + ", truncated=" + truncated
                + ", multipartTaskList=" + multipartTaskList + ", delimiter=" + delimiter + ", commonPrefixes="
                + Arrays.toString(commonPrefixes) + "]";
    }

}
