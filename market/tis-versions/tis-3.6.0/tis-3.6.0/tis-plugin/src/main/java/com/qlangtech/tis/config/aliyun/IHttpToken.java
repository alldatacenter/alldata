/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.qlangtech.tis.config.aliyun;

import com.qlangtech.tis.config.ParamsConfig;
import com.qlangtech.tis.plugin.IdentityName;

import java.util.Objects;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public interface IHttpToken extends IdentityName {

    String KEY_FIELD_ALIYUN_TOKEN = "aliyunToken";
    String KEY_DISPLAY_NAME = "httpToken";

    public static IHttpToken getToken(String endpoint) {
        IHttpToken aliyunToken = ParamsConfig.getItem(endpoint, KEY_DISPLAY_NAME);
        Objects.requireNonNull(aliyunToken, "aliyunToekn can not be null");
        return aliyunToken;
    }

    // private static String endpoint = "*** Provide OSS endpoint ***";
    // private static String accessKeyId = "*** Provide your AccessKeyId ***";
    // private static String accessKeySecret = "*** Provide your AccessKeySecret ***";
    // public String getEndpoint();
    //    String getAccessKeyId();
    //    String getAccessKeySecret();

    String getEndpoint();
}
