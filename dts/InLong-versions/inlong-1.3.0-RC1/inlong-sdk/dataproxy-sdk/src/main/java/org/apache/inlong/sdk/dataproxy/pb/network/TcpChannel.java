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

package org.apache.inlong.sdk.dataproxy.pb.network;

import java.util.concurrent.Semaphore;

import org.jboss.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * TcpChannel
 */
public class TcpChannel {

    public static final Logger LOG = LoggerFactory.getLogger(TcpChannelGroup.class);

    private IpPort ipPort;
    private Channel channel;
    private boolean hasException = false;
    private boolean reconnectFail = false;
    private Semaphore packToken;

    /**
     * Constructor
     * 
     * @param channel
     * @param ipPort
     */
    public TcpChannel(Channel channel, IpPort ipPort) {
        this.channel = channel;
        this.ipPort = ipPort;
        this.packToken = new Semaphore(1, true);
    }

    /**
     * toString
     * 
     * @return
     */
    @Override
    public String toString() {
        return ipPort.key;
    }

    /**
     * get ipPort
     * 
     * @return the ipPort
     */
    public IpPort getIpPort() {
        return ipPort;
    }

    /**
     * set ipPort
     * 
     * @param ipPort the ipPort to set
     */
    public void setIpPort(IpPort ipPort) {
        this.ipPort = ipPort;
    }

    /**
     * get channel
     * 
     * @return the channel
     */
    public Channel getChannel() {
        return channel;
    }

    /**
     * set channel
     * 
     * @param channel the channel to set
     */
    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    /**
     * get hasException
     * 
     * @return the hasException
     */
    public boolean isHasException() {
        return hasException;
    }

    /**
     * set hasException
     * 
     * @param hasException the hasException to set
     */
    public void setHasException(boolean hasException) {
        this.hasException = hasException;
    }

    /**
     * get reconnectFail
     * 
     * @return the reconnectFail
     */
    public boolean isReconnectFail() {
        return reconnectFail;
    }

    /**
     * set reconnectFail
     * 
     * @param reconnectFail the reconnectFail to set
     */
    public void setReconnectFail(boolean reconnectFail) {
        this.reconnectFail = reconnectFail;
    }

    /**
     * 
     * close
     */
    public void close() {
        try {
            if (channel != null && channel.isConnected()) {
                channel.disconnect();
                channel.close();
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    /**
     * tryAcquire
     * 
     * @return
     */
    public boolean tryAcquire() {
        return packToken.tryAcquire();
    }

    /**
     * acquireUninterruptibly
     */
    public void acquireUninterruptibly() {
        packToken.acquireUninterruptibly();;
    }

    /**
     * release
     */
    public void release() {
        packToken.release();
    }

    /**
     * get packToken
     * 
     * @return the packToken
     */
    public Semaphore getPackToken() {
        return packToken;
    }

}
