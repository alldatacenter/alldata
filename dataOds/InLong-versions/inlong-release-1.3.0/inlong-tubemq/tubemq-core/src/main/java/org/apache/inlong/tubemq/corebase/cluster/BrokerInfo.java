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
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.corebase.TokenConstants;
import org.apache.inlong.tubemq.corebase.utils.TStringUtils;

/**
 * The BrokerInfo hold basic info of an tube broker the brokerId, the broker host
 * and the port and so on.
 */

public class BrokerInfo implements Comparable<BrokerInfo>, Serializable {

    private static final long serialVersionUID = -6907854104544421368L;
    private int brokerId;
    private String host;
    private int port;
    private boolean enableTLS = false;
    private int tlsPort =
            TBaseConstants.META_DEFAULT_BROKER_TLS_PORT;
    private String addr;
    private String simpleInfo;
    private String fullInfo;
    private String fullTLSInfo;

    //create with strBrokerInfo (brokerId:host:port)
    public BrokerInfo(String strBrokerInfo) {
        String[] strBrokers =
                strBrokerInfo.split(TokenConstants.ATTR_SEP);
        this.brokerId = Integer.parseInt(strBrokers[0]);
        this.host = strBrokers[1];
        this.port = Integer.parseInt(strBrokers[2]);
        this.tlsPort = port;
        this.buildStrInfo();
    }

    //create with strBrokerInfo (brokerId:host)  brokerPort
    public BrokerInfo(String strBrokerInfo, int brokerPort) {
        String[] strBrokers =
                strBrokerInfo.split(TokenConstants.ATTR_SEP);
        this.brokerId = Integer.parseInt(strBrokers[0]);
        this.host = strBrokers[1];
        this.port = brokerPort;
        if (!TStringUtils.isBlank(strBrokers[2])) {
            this.port = Integer.parseInt(strBrokers[2]);
        }
        this.buildStrInfo();
    }

    //create with brokerId host port
    public BrokerInfo(final int brokerId, final String host, final int port) {
        this.brokerId = brokerId;
        this.host = host;
        this.port = port;
        this.buildStrInfo();
    }

    //create with brokerId uri data
    public BrokerInfo(final int brokerId, final String data) {
        this.brokerId = brokerId;
        try {
            final URI uri = new URI(data);
            this.host = uri.getHost();
            this.port = uri.getPort();
            this.buildStrInfo();
        } catch (final URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    //create with strBrokerInfo (brokerId:host:port) and enableTls tlsPort
    public BrokerInfo(String strBrokerInfo, boolean enableTls, int tlsPort) {
        String[] strBrokers = strBrokerInfo.split(TokenConstants.ATTR_SEP);
        this.brokerId = Integer.parseInt(strBrokers[0]);
        this.host = strBrokers[1];
        this.port = Integer.parseInt(strBrokers[2]);
        this.enableTLS = enableTls;
        this.tlsPort = tlsPort;
        this.buildStrInfo();
    }

    //create with brokerId host enableTls  tlsPort (port = tlsPort)
    public BrokerInfo(int brokerId, String host, boolean enableTls, int tlsPort) {
        this.brokerId = brokerId;
        this.host = host;
        this.port = tlsPort;
        this.enableTLS = enableTls;
        this.tlsPort = tlsPort;
        this.buildStrInfo();
    }

    public int getBrokerId() {
        return this.brokerId;
    }

    public void setBrokerId(final int brokerId) {
        this.brokerId = brokerId;
    }

    public String getBrokerAddr() {
        return this.addr;
    }

    public String getHost() {
        return this.host;
    }

    public void setHost(final String host) {
        this.host = host;
    }

    public int getPort() {
        return this.port;
    }

    public void setPort(final int port) {
        this.port = port;
    }

    public String getBrokerStrInfo() {
        return fullInfo;
    }

    public int getTlsPort() {
        return tlsPort;
    }

    public boolean isEnableTLS() {
        return enableTLS;
    }

    @Override
    public String toString() {
        return this.fullInfo;
    }

    public String getSimpleInfo() {
        if (this.port == TBaseConstants.META_DEFAULT_BROKER_PORT) {
            return this.simpleInfo;
        } else {
            return this.fullInfo;
        }
    }

    public String getFullTLSInfo() {
        return fullTLSInfo;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.host == null ? 0 : this.host.hashCode());
        result = prime * result + this.brokerId;
        result = prime * result + this.port;
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        final BrokerInfo other = (BrokerInfo) obj;
        if (!this.host.equals(other.host)) {
            return false;
        }
        if (this.brokerId != other.brokerId) {
            return false;
        }
        if (this.port != other.port) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(BrokerInfo o) {
        if (this.brokerId != o.brokerId) {
            return this.brokerId > o.brokerId ? 1 : -1;
        }
        if (!this.host.equals(o.host)) {
            return this.host.compareTo(o.host);
        } else if (this.port != o.port) {
            return this.port > o.port ? 1 : -1;
        }
        return 0;
    }

    // init the tube broker string info
    private void buildStrInfo() {
        StringBuilder sBuilder = new StringBuilder(512);
        this.addr = sBuilder.append(this.host)
                .append(TokenConstants.ATTR_SEP)
                .append(this.port).toString();
        sBuilder.delete(0, sBuilder.length());
        this.simpleInfo = sBuilder.append(this.brokerId)
                .append(TokenConstants.ATTR_SEP).append(host)
                .append(TokenConstants.ATTR_SEP).append(" ").toString();
        sBuilder.delete(0, sBuilder.length());
        this.fullInfo = sBuilder.append(this.brokerId)
                .append(TokenConstants.ATTR_SEP).append(host)
                .append(TokenConstants.ATTR_SEP).append(this.port).toString();
        sBuilder.delete(0, sBuilder.length());
        this.fullTLSInfo = sBuilder.append(this.brokerId)
                .append(TokenConstants.ATTR_SEP).append(host)
                .append(TokenConstants.ATTR_SEP).append(this.tlsPort).toString();
    }

}
