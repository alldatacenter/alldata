/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.obs.services.internal;

/**
 * OBS系统中的用到的常量。通过常量名称获取配置信息
 */
public class ObsConstraint {
    /**
     * OBS域名的名称
     */
    public static final String END_POINT = "obs-endpoint";

    /**
     * 是否使用OPEN SSL(HTTPS)
     */
    public static final String HTTPS_ONLY = "obs.https-only";

    /**
     * 是否不把请求中的桶名加入域名
     */
    public static final String DISABLE_DNS_BUCKET = "obs.disable-dns-buckets";

    /**
     * 若使用HTTP，使用的端口号
     */
    public static final String HTTP_PORT = "obs-endpoint-http-port";

    /**
     * 若使用HTTPS，使用的端口号
     */
    public static final String HTTPS_PORT = "obs-endpoint-https-port";

    /**
     * 与OBS服务端建立的最大连接数
     */
    public static final String HTTP_MAX_CONNECT = "httpclient.max-connections";

    /**
     * 发送OBS业务请求最大重复次数
     */
    public static final String HTTP_RETRY_MAX = "httpclient.retry-max";

    /**
     * 发送OBS业务请求连接超时时间
     */
    public static final String HTTP_CONNECT_TIMEOUT = "httpclient.connection-timeout-ms";

    /**
     * SOCKET的超时时间
     */
    public static final String HTTP_SOCKET_TIMEOUT = "httpclient.socket-timeout-ms";

    public static final String HTTP_IDLE_CONNECTION_TIME = "httpclient.idle-connection-time";

    public static final String HTTP_MAX_IDLE_CONNECTIONS = "httpclient.max-idle-connections";

    public static final String HTTP_STRICT_HOSTNAME_VERIFICATION = "httpclient.strict-hostname-verification";

    public static final int HTTP_CONNECT_TIMEOUT_VALUE = 60000;

    public static final int HTTP_MAX_CONNECT_VALUE = 1000;

    public static final int HTTP_RETRY_MAX_VALUE = 3;

    public static final int HTTP_SOCKET_TIMEOUT_VALUE = 60000;

    public static final int HTTP_PORT_VALUE = 80;

    public static final int HTTPS_PORT_VALUE = 443;

    public static final long DEFAULT_PROGRESS_INTERVAL = 100 * 1024L;

    public static final int DEFAULT_CHUNK_SIZE = 4096;

    public static final String DEFAULT_BUCKET_LOCATION_VALUE = "region";

//    public static final int DEFAULT_BUFFER_STREAM = 512 * 1024;// 512KB

    public static final int DEFAULT_READ_BUFFER_STREAM = 8192;// 8KB

    public static final int DEFAULT_WRITE_BUFFER_STREAM = 8192;// 8KB

    public static final long DEFAULT_EXPIRE_SECONEDS = 300;

    public static final int DEFAULT_IDLE_CONNECTION_TIME = 30000;

    public static final int DEFAULT_MAX_IDLE_CONNECTIONS = HTTP_MAX_CONNECT_VALUE;

    public static final int DEFAULT_TASK_THREAD_NUM = 10;

    public static final int DEFAULT_WORK_QUEUE_NUM = 20000;

    public static final int DEFAULT_TASK_PROGRESS_INTERVAL = 50;

    public static final int DEFAULT_LOCAL_AUTH_TYPE_CACHE_CAPACITY = 50;

    // HTTP代理配置
    public static final String PROXY_ISABLE = "httpclient.proxy-enable";

    public static final String PROXY_HOST = "httpclient.proxy-host";

    public static final String PROXY_PORT = "httpclient.proxy-port";

    public static final String PROXY_UNAME = "httpclient.proxy-user";

    public static final String PROXY_PAWD = "httpclient.proxy-password";

    public static final String PROXY_DOMAIN = "httpclient.proxy-domain";

    public static final String PROXY_WORKSTATION = "httpclient.proxy-workstation";

//    public static final String BUFFER_STREAM = "uploads.stream-retry-buffer-size";

    public static final String VALIDATE_CERTIFICATE = "httpclient.validate-certificate";

    public static final String VERIFY_RESPONSE_CONTENT_TYPE = "obs.verify-content-type";

    public static final String WRITE_BUFFER_SIZE = "httpclient.write-buffer-size";
    public static final String READ_BUFFER_SIZE = "httpclient.read-buffer-size";

    public static final String SOCKET_WRITE_BUFFER_SIZE = "socket.write-buffer-size";
    public static final String SOCKET_READ_BUFFER_SIZE = "socket.read-buffer-size";

    public static final String KEEP_ALIVE = "httpclient.keep-alive";

    public static final String HTTP_PROTOCOL = "httpclient.protocol";

    public static final String FS_DELIMITER = "filesystem.delimiter";

    public static final String AUTH_TYPE_NEGOTIATION = "httpclient.auth-type-negotiation";

    public static final String IS_CNAME = "httpclient.is-cname";

    public static final String SSL_PROVIDER = "httpclient.ssl-provider";

    /**
     * Environment variable name for the obs accesskey
     */
    public static final String ACCESS_KEY_ENV_VAR = "OBS_ACCESS_KEY_ID";

    /**
     * Environment variable name for the oss secretKey
     */
    public static final String SECRET_KEY_ENV_VAR = "OBS_SECRET_ACCESS_KEY";

    /**
     * Environment variable name for the securityToken
     */
    public static final String SECURITY_TOKEN_ENV_VAR = "OBS_SECURITY_TOKEN";

    public static final String OBS_XML_DOC_BUILDER_FACTORY = "obs.xml.document.builder.factory";
    
    public static final String OBS_XML_DOC_BUILDER_FACTORY_CLASS = 
            "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl";
}
