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

package com.obs.services.model;

/**
 * Encryption mode of SSE-KMS
 */
@Deprecated
public final class ServerEncryption {
    /**
     * SSE-KMS supports kms only.
     */
    public static final ServerEncryption OBS_KMS = new ServerEncryption("kms");

    private String serverEncryption = "";

    private ServerEncryption(String serverEncryption) {
        this.serverEncryption = serverEncryption;
    }

    public String getServerEncryption() {
        return serverEncryption;
    }

    public static ServerEncryption parseServerEncryption(String str) {
        ServerEncryption serverEncryption = null;

        if (null != str && str.equals(OBS_KMS.toString())) {
            serverEncryption = OBS_KMS;
        }
        return serverEncryption;
    }

    @Override
    public String toString() {
        return serverEncryption;
    }

    public boolean equals(Object obj) {
        return (obj instanceof ServerEncryption) && toString().equals(obj.toString());
    }

    public int hashCode() {
        return serverEncryption.hashCode();
    }
}
