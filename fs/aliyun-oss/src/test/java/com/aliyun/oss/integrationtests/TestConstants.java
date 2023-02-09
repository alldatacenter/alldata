/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.aliyun.oss.integrationtests;

public final class TestConstants {
    
    public static final String COPY_OBJECT_DIFF_LOCATION_ERR = "Target object does not reside in the same data center as source object.";
    
    public static final String INVALID_ENCRYPTION_ALGO_ERR = "The Encryption request you specified is not valid. Supported value: AES25";
    
    public static final String NO_SUCH_BUCKET_ERR = "The specified bucket does not exist.";
    
    public static final String NO_SUCH_KEY_ERR = "The specified key does not exist.";
    
    public static final String PRECONDITION_FAILED_ERR = "Precondition Failed";
    
    public static final String NOT_MODIFIED_ERR = "Not Modified";
    
    public static final String INVALID_PART_ERR = "One or more of the specified parts could not be found or the specified entity tag might not have matched the part's entity tag.";
    
    public static final String INVALID_PART_ORDER_ERR = "The list of parts was not in ascending order. Parts list must specified in order by part number.";
    
    public static final String ENTITY_TOO_SMALL_ERR = "Your proposed upload smaller than the minimum allowed size.";
    
    public static final String NO_SUCH_UPLOAD_ERR = "The specified upload does not exist. The upload ID may be invalid, or the upload may have been aborted or completed.";

    public static final String BUCKET_NOT_EMPTY_ERR = "The bucket has objects. Please delete them first.";
    
    public static final String BUCKET_ALREADY_EXIST_ERR = "The requested bucket name is not available. The bucket namespace is shared by all users of the system. Please select a different name and try again.";
    
    public static final String BUCKET_ACCESS_DENIED_ERR = "AccessDenied";
    
    public static final String NO_SUCH_CORS_CONFIGURATION_ERR = "The CORS Configuration does not exist.";
    
    public static final String NO_SUCH_LIFECYCLE_ERR = "No Row found in Lifecycle Table.";
    
    public static final String INVALID_TARGET_BUCKET_FOR_LOGGING_ERR = "put bucket log requester is not target bucket owner.";
    
    public static final String NO_SUCH_WEBSITE_CONFIGURATION_ERR = "The specified bucket does not have a website configuration.";
    
    public static final String INVALID_LOCATION_CONSTRAINT_ERR = "The specified location-constraint is not valid";
    
    public static final String TOO_MANY_BUCKETS_ERR = "You have attempted to create more buckets than allowed.";
    
    public static final String ILLEGAL_ARGUMENT_ERR = "Invalid Argument";
    
    public static final String INVALID_OBJECT_NAME_ERR = "The Length of Object name must be less than 1024.";
    
    public static final String INVALID_DIGEST_ERR = "The Content-MD5 you specified was invalid.";
    
    public static final String INVALID_RANGE_ERR = "The requested range cannot be satisfied";
    
    public static final String SECURITY_TOKEN_NOT_SUPPORTED_ERR = "This interface does not support security token.";
    
    public static final String SECURITY_TOKEN_ACCESS_DENIED_ERR = "Access denied by authorizer's policy.";
    
    public static final String OBJECT_NOT_APPENDABLE_ERR = "The object is not appendable";
    
    public static final String POSITION_NOT_EQUAL_TO_LENGTH_ERROR = "Position is not equal to file length";
    
    public static final String MISSING_ARGUMENT_ERR = "Missing Some Required Arguments.";
    
    public static final String MODIFY_STORAGE_TYPE_ERR = "Cannot modify existing bucket's storage class.";

    public static final String ACCESS_DENIED_MSG_REQUESTER_PAY_BUCKET = "Access denied for requester pay bucket";

}
