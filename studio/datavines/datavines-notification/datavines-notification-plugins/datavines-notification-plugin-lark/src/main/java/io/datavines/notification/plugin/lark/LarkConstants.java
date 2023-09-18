/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.datavines.notification.plugin.lark;

public class LarkConstants {

    private LarkConstants() {
        throw new IllegalStateException(LarkConstants.class.getName());
    }

    public static final String MSG_TYPE = "msg_type";
    public static final String DEFAULT_MSG_TYPE = "interactive";
    public static final String CARD = "card";

    public static final String LARK_API = "https://open.feishu.cn/open-apis";
    public static final String GROUP_HOOK_URL = "/bot/v2/hook/%s";
    public static final String ACCESS_TOKEN_QUERY_URL = "/auth/v3/tenant_access_token/internal";
    public static final String USER_OPEN_ID_QUERY_URL = "/contact/v3/users/batch_get_id";
    public static final String MESSAGES_URL = "/im/v1/messages?receive_id_type=open_id";
}
