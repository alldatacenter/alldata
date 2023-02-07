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

package org.apache.inlong.dataproxy.config;

import org.apache.inlong.common.util.BasicAuth;
import org.apache.inlong.dataproxy.consts.ConfigConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class AuthUtils {

    private static final Logger LOG = LoggerFactory.getLogger(AuthUtils.class);

    /**
     * Generate http basic auth credential from configured secretId and secretKey
     */
    public static String genBasicAuth() {
        Map<String, String> properties = ConfigManager.getInstance().getCommonProperties();
        String secretId = properties.get(ConfigConstants.MANAGER_AUTH_SECRET_ID);
        String secretKey = properties.get(ConfigConstants.MANAGER_AUTH_SECRET_KEY);
        return BasicAuth.genBasicAuthCredential(secretId, secretKey);
    }

}
