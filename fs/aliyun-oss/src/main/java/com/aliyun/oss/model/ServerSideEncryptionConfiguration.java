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

package com.aliyun.oss.model;

/**
 * Container for server-side encryption configuration rules. Currently OSS supports one rule only.
 */
public class ServerSideEncryptionConfiguration {

    private ServerSideEncryptionByDefault applyServerSideEncryptionByDefault;

    /**
     * Describes the default server-side encryption to apply to new objects in the bucket. If Put Object request does not specify
     * any server-side encryption, this default encryption will be applied.
     *
     * @return The current {@link ServerSideEncryptionByDefault}
     */
    public ServerSideEncryptionByDefault getApplyServerSideEncryptionByDefault() {
        return applyServerSideEncryptionByDefault;
    }

    /**
     * Sets the default server-side encryption to apply to new objects in the bucket. If Put Object request does not specify
     * any server-side encryption, this default encryption will be applied.
     *
     * @param applyServerSideEncryptionByDefault New default SSE configuration.
     */
    public void setApplyServerSideEncryptionByDefault(ServerSideEncryptionByDefault applyServerSideEncryptionByDefault) {
        this.applyServerSideEncryptionByDefault = applyServerSideEncryptionByDefault;
    }

    /**
     * Sets the default server-side encryption to apply to new objects in the bucket. If Put Object request does not specify
     * any server-side encryption, this default encryption will be applied.
     *
     * @param applyServerSideEncryptionByDefault New default SSE configuration.
     * @return This object for method chaining.
     */
    public ServerSideEncryptionConfiguration withApplyServerSideEncryptionByDefault(ServerSideEncryptionByDefault applyServerSideEncryptionByDefault) {
        setApplyServerSideEncryptionByDefault(applyServerSideEncryptionByDefault);
        return this;
    }
    
}
