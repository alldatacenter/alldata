/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.tubemq.corebase.cluster;

import java.io.Serializable;
import org.apache.inlong.tubemq.corebase.TokenConstants;
import org.apache.inlong.tubemq.corebase.utils.TStringUtils;

public class NodeAddrInfo implements Comparable<NodeAddrInfo>, Serializable {

    private static final long serialVersionUID = -1L;
    private String hostPortStr;
    private String host;
    private int port;

    public NodeAddrInfo(String host, int port) {
        if (TStringUtils.isBlank(host)) {
            throw new IllegalArgumentException("Argument host is Blank!");
        }
        this.host = host.trim();
        this.port = port;
        this.hostPortStr = new StringBuilder(256).append(host)
                .append(TokenConstants.ATTR_SEP).append(port).toString();
    }

    public NodeAddrInfo(String host, int port, String hostPortStr) {
        if (TStringUtils.isBlank(host)) {
            throw new IllegalArgumentException("Argument host is Blank!");
        }
        if (TStringUtils.isBlank(hostPortStr)) {
            throw new IllegalArgumentException("Argument hostPortStr is Blank!");
        }
        this.host = host.trim();
        this.port = port;
        this.hostPortStr = hostPortStr.trim();
    }

    public String getHostPortStr() {
        return hostPortStr;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    @Override
    public int compareTo(NodeAddrInfo o) {
        if (!this.host.equals(o.host)) {
            return this.host.compareTo(o.host);
        } else if (this.port != o.port) {
            return this.port > o.port ? 1 : -1;
        }
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NodeAddrInfo)) {
            return false;
        }
        NodeAddrInfo that = (NodeAddrInfo) o;
        if (port != that.port) {
            return false;
        }
        return host.equals(that.host);
    }

    @Override
    public int hashCode() {
        int result = host.hashCode();
        result = 31 * result + port;
        return result;
    }

    @Override
    public String toString() {
        return this.hostPortStr;
    }
}
