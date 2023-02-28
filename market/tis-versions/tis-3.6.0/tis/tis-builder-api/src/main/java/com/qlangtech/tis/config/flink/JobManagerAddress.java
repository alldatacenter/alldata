/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.qlangtech.tis.config.flink;

import org.apache.commons.lang.StringUtils;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-10-23 12:23
 **/
public class JobManagerAddress {
    public final String host;
    public final int port;

    public static JobManagerAddress parse(String value) {
        String[] address = StringUtils.split(value, ":");
        if (address.length != 2) {
            throw new IllegalArgumentException("illegal jobManagerAddress:" + address);
        }
        return new JobManagerAddress(address[0], Integer.parseInt(address[1]));
    }

    public JobManagerAddress(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String getURL() {
        return "http://" + this.host + ":" + this.port;
    }


}
