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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sdk.dataproxy.config;

public class HostInfo implements Comparable<HostInfo>, java.io.Serializable {
    private final String referenceName;
    private final String hostName;
    private final int portNumber;

    public HostInfo(String referenceName, String hostName, int portNumber) {
        this.referenceName = referenceName;
        this.hostName = hostName;
        this.portNumber = portNumber;
    }

    public String getReferenceName() {
        return referenceName;
    }

    public String getHostName() {
        return hostName;
    }

    public int getPortNumber() {
        return portNumber;
    }

    @Override
    public String toString() {
        return referenceName + "{" + hostName + ":" + portNumber + "}";
    }

    public int compareTo(HostInfo other) {
        return referenceName.compareTo(other.getReferenceName());
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof HostInfo)) {
            return false;
        }
        if (other == this) {
            return true;
        }
        HostInfo info = (HostInfo) other;
        return (this.referenceName.equals(info.getReferenceName()))
                && (this.hostName.equals(info.getHostName()))
                 && (this.portNumber == info.getPortNumber());
    }

}
