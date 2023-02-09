/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.obs.services.model;

/**
 * SSE-C encryption algorithm
 */
public final class ServerAlgorithm {
    /**
     * SSE-C supports only AES256.
     */
    public static final ServerAlgorithm AES256 = new ServerAlgorithm("AES256");

    private String serverAlgorithm = "";

    private ServerAlgorithm(String serverAlgorithm) {
        this.serverAlgorithm = serverAlgorithm;
    }

    public String getServerAlgorithm() {
        return serverAlgorithm;
    }

    public static ServerAlgorithm parseServerAlgorithm(String str) {
        ServerAlgorithm serverAlgorithm = null;

        if (null != str && str.equals(AES256.toString())) {
            serverAlgorithm = AES256;
        }
        return serverAlgorithm;
    }

    @Override
    public String toString() {
        return serverAlgorithm;
    }

    public boolean equals(Object obj) {
        return (obj instanceof ServerAlgorithm) && toString().equals(obj.toString());
    }

    public int hashCode() {
        return serverAlgorithm.hashCode();
    }
}
