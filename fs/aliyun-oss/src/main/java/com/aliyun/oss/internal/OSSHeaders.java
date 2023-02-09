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

package com.aliyun.oss.internal;

import com.aliyun.oss.common.utils.HttpHeaders;

public interface OSSHeaders extends HttpHeaders {

    static final String OSS_PREFIX = "x-oss-";
    static final String OSS_USER_METADATA_PREFIX = "x-oss-meta-";

    static final String OSS_CANNED_ACL = "x-oss-acl";
    static final String STORAGE_CLASS = "x-oss-storage-class";
    static final String OSS_VERSION_ID = "x-oss-version-id";

    static final String OSS_SERVER_SIDE_ENCRYPTION = "x-oss-server-side-encryption";
    static final String OSS_SERVER_SIDE_ENCRYPTION_KEY_ID = "x-oss-server-side-encryption-key-id";
    static final String OSS_SERVER_SIDE_DATA_ENCRYPTION = "x-oss-server-side-data-encryption";

    static final String GET_OBJECT_IF_MODIFIED_SINCE = "If-Modified-Since";
    static final String GET_OBJECT_IF_UNMODIFIED_SINCE = "If-Unmodified-Since";
    static final String GET_OBJECT_IF_MATCH = "If-Match";
    static final String GET_OBJECT_IF_NONE_MATCH = "If-None-Match";

    static final String HEAD_OBJECT_IF_MODIFIED_SINCE = "If-Modified-Since";
    static final String HEAD_OBJECT_IF_UNMODIFIED_SINCE = "If-Unmodified-Since";
    static final String HEAD_OBJECT_IF_MATCH = "If-Match";
    static final String HEAD_OBJECT_IF_NONE_MATCH = "If-None-Match";

    static final String COPY_OBJECT_SOURCE = "x-oss-copy-source";
    static final String COPY_SOURCE_RANGE = "x-oss-copy-source-range";
    static final String COPY_OBJECT_SOURCE_IF_MATCH = "x-oss-copy-source-if-match";
    static final String COPY_OBJECT_SOURCE_IF_NONE_MATCH = "x-oss-copy-source-if-none-match";
    static final String COPY_OBJECT_SOURCE_IF_UNMODIFIED_SINCE = "x-oss-copy-source-if-unmodified-since";
    static final String COPY_OBJECT_SOURCE_IF_MODIFIED_SINCE = "x-oss-copy-source-if-modified-since";
    static final String COPY_OBJECT_METADATA_DIRECTIVE = "x-oss-metadata-directive";
    static final String COPY_OBJECT_TAGGING_DIRECTIVE = "x-oss-tagging-directive";

    static final String OSS_HEADER_REQUEST_ID = "x-oss-request-id";
    static final String OSS_HEADER_VERSION_ID = "x-oss-version-id";

    static final String ORIGIN = "origin";
    static final String ACCESS_CONTROL_REQUEST_METHOD = "Access-Control-Request-Method";
    static final String ACCESS_CONTROL_REQUEST_HEADER = "Access-Control-Request-Headers";

    static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
    static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
    static final String ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers";
    static final String ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age";

    static final String OSS_SECURITY_TOKEN = "x-oss-security-token";

    static final String OSS_NEXT_APPEND_POSITION = "x-oss-next-append-position";
    static final String OSS_HASH_CRC64_ECMA = "x-oss-hash-crc64ecma";
    static final String OSS_OBJECT_TYPE = "x-oss-object-type";

    static final String OSS_OBJECT_ACL = "x-oss-object-acl";

    static final String OSS_HEADER_CALLBACK = "x-oss-callback";
    static final String OSS_HEADER_CALLBACK_VAR = "x-oss-callback-var";
    static final String OSS_HEADER_SYMLINK_TARGET = "x-oss-symlink-target";

    static final String OSS_STORAGE_CLASS = "x-oss-storage-class";
    static final String OSS_RESTORE = "x-oss-restore";
    static final String OSS_ONGOING_RESTORE = "ongoing-request=\"true\"";

    static final String OSS_BUCKET_REGION = "x-oss-bucket-region";

    static final String OSS_SELECT_PREFIX = "x-oss-select";
    static final String OSS_SELECT_CSV_ROWS = OSS_SELECT_PREFIX + "-csv-rows";
    static final String OSS_SELECT_OUTPUT_RAW = OSS_SELECT_PREFIX + "-output-raw";
    static final String OSS_SELECT_CSV_SPLITS = OSS_SELECT_PREFIX + "-csv-splits";
    static final String OSS_SELECT_INPUT_LINE_RANGE = OSS_SELECT_PREFIX + "-line-range";
    static final String OSS_SELECT_INPUT_SPLIT_RANGE = OSS_SELECT_PREFIX + "-split-range";
    
    static final String OSS_TAGGING = "x-oss-tagging";

    static final String OSS_REQUEST_PAYER = "x-oss-request-payer";

    static final String OSS_HEADER_TRAFFIC_LIMIT = "x-oss-traffic-limit";

    static final String OSS_HEADER_TASK_ID = "x-oss-task-id";

    static final String OSS_HEADER_WORM_ID = "x-oss-worm-id";

    static final String OSS_HEADER_CERT_ID = "x-oss-yundun-certificate-id";

    static final String OSS_HNS_STATUS = "x-oss-hns-status";

    static final String OSS_DELETE_RECURSIVE = "x-oss-delete-recursive";
    static final String OSS_DELETE_TOKEN = "x-oss-delete-token";

    static final String OSS_RENAME_SOURCE = "x-oss-rename-source";

    static final String OSS_RESOURCE_GROUP_ID = "x-oss-resource-group-id";

    static final String OSS_DATE = "x-oss-date";

    static final String OSS_CONTENT_SHA256 = "x-oss-content-sha256";
}
