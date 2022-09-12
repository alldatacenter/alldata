/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.audit.send;

import org.apache.inlong.audit.util.IpPort;
import io.netty.channel.Channel;
import java.util.concurrent.Semaphore;

public class SenderChannel {

    private IpPort ipPort;
    private Channel channel;
    private Semaphore packToken;

    /**
     * Constructor
     *
     * @param channel
     * @param ipPort
     */
    public SenderChannel(Channel channel, IpPort ipPort, int maxSynchRequest) {
        this.channel = channel;
        this.ipPort = ipPort;
        this.packToken = new Semaphore(maxSynchRequest);
    }

    /**
     * Try acquire channel
     *
     * @return
     */
    public boolean tryAcquire() {
        return packToken.tryAcquire();
    }

    /**
     * release channel
     */
    public void release() {
        packToken.release();
    }

    /**
     * toString
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
}
