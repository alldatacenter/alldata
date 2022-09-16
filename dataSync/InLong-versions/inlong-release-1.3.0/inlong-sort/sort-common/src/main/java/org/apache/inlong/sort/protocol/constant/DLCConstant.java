/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.inlong.sort.protocol.constant;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DLCConstant {
    /**
     * DLC internet access domain name.
     */
    public static final String DLC_ENDPOINT = "dlc.tencentcloudapi.com";
    // ============================== DLC AUTH PARAMS(Required) =====================================
    /**
     * dlc account region
     */
    public static final String DLC_REGION = "qcloud.dlc.region";
    /**
     * dlc account secret id
     */
    public static final String DLC_SECRET_ID = "qcloud.dlc.secret-id";
    /**
     * dlc account secret key
     */
    public static final String DLC_SECRET_KEY = "qcloud.dlc.secret-key";
    /**
     * Current user appid.
     */
    public static final String DLC_USER_APPID = "qcloud.dlc.user.appid";
    /**
     * Managed account uid.
     */
    public static final String DLC_MANAGED_ACCOUNT_UID = "qcloud.dlc.managed.account.uid";
    /**
     * dlc jdbc url(optional)
     */
    public static final String DLC_JDBC_URL = "qcloud.dlc.jdbc.url";

    // ============================== FS CREDENTIALS AUTH PARAMS =====================================
    public static final String FS_LAKEFS_IMPL  = "fs.lakefs.impl";
    public static final String FS_COS_IMPL  = "fs.cosn.impl";
    public static final String FS_COS_AUTH_PROVIDER  = "fs.cosn.credentials.provider";
    public static final String FS_COS_REGION  = "fs.cosn.userinfo.region";
    public static final String FS_COS_SECRET_ID  = "fs.cosn.userinfo.secretId";
    public static final String FS_COS_SECRET_KEY  = "fs.cosn.userinfo.secretKey";

    public static final String FS_AUTH_DLC_SECRET_ID = "service.secret.id";
    public static final String FS_AUTH_DLC_SECRET_KEY = "service.secret.key";
    public static final String FS_AUTH_DLC_REGION  = "service.region";
    public static final String FS_AUTH_DLC_ACCOUNT_APPID  = "user.appid";
    public static final String FS_AUTH_DLC_MANAGED_ACCOUNT_UID  = "request.identity.token";

    public static final String DLC_CATALOG_IMPL_CLASS =
            "org.apache.inlong.sort.iceberg.catalog.hybris.DlcWrappedHybrisCatalog";
    public static final Map<String, String> DLC_DEFAULT_IMPL =
            Collections.unmodifiableMap(new HashMap<String, String>() {
                {
                    put(FS_LAKEFS_IMPL, "org.apache.hadoop.fs.CosFileSystem");
                    put(FS_COS_IMPL, "org.apache.hadoop.fs.CosFileSystem");
                    put(FS_COS_AUTH_PROVIDER, "org.apache.hadoop.fs.auth.DlcCloudCredentialsProvider");
                }
            });
}
