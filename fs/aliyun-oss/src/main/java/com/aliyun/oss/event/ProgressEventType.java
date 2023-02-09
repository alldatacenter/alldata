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

package com.aliyun.oss.event;

public enum ProgressEventType {

    /**
     * Event of the content length to be sent in a request.
     */
    REQUEST_CONTENT_LENGTH_EVENT,

    /**
     * Event of the content length received in a response.
     */
    RESPONSE_CONTENT_LENGTH_EVENT,

    /**
     * Used to indicate the number of bytes to be sent to OSS.
     */
    REQUEST_BYTE_TRANSFER_EVENT,

    /**
     * Used to indicate the number of bytes received from OSS.
     */
    RESPONSE_BYTE_TRANSFER_EVENT,

    /**
     * Transfer events.
     */
    TRANSFER_PREPARING_EVENT, TRANSFER_STARTED_EVENT, TRANSFER_COMPLETED_EVENT, TRANSFER_FAILED_EVENT, TRANSFER_CANCELED_EVENT, TRANSFER_PART_STARTED_EVENT, TRANSFER_PART_COMPLETED_EVENT, TRANSFER_PART_FAILED_EVENT,

    /**
     * Select object events.
     */
    SELECT_STARTED_EVENT, SELECT_SCAN_EVENT, SELECT_COMPLETED_EVENT, SELECT_FAILED_EVENT
}
