/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.obs.services;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import com.obs.services.internal.ObsConstraint;
import com.obs.services.model.AuthTypeEnum;
import com.obs.services.model.HttpProtocolTypeEnum;

import okhttp3.Dispatcher;

/**
 * Configuration parameters of ObsClient
 */
public class ObsConfiguration implements Cloneable {

    private int connectionTimeout;

    private int idleConnectionTime;

    private int maxIdleConnections;

    private int maxConnections;

    private int maxErrorRetry;

    private int socketTimeout;

    private String endPoint;

    private int endpointHttpPort;

    private int endpointHttpsPort;

    private boolean httpsOnly;

    private boolean pathStyle;

    private HttpProxyConfiguration httpProxy;

    private int uploadStreamRetryBufferSize;

    private boolean validateCertificate;

    private boolean verifyResponseContentType;

    private int readBufferSize;

    private int writeBufferSize;

    private KeyManagerFactory keyManagerFactory;

    private TrustManagerFactory trustManagerFactory;

    private boolean isStrictHostnameVerification;

    private AuthTypeEnum authType;

    private int localAuthTypeCacheCapacity;

    private String signatString;
    private String defaultBucketLocation;
    private int bufferSize;
    private int socketWriteBufferSize;
    private int socketReadBufferSize;
    private boolean isNio;
    private boolean useReaper;
    private boolean keepAlive;
    private int connectionRequestTimeout;
    private boolean authTypeNegotiation;

    private boolean cname;

    private String delimiter;

    private String sslProvider;

    private HttpProtocolTypeEnum httpProtocolType;

    private Dispatcher httpDispatcher;
    
    private String xmlDocumentBuilderFactoryClass;

    /**
     * Constructor
     */
    public ObsConfiguration() {
        this.connectionTimeout = ObsConstraint.HTTP_CONNECT_TIMEOUT_VALUE;
        this.maxConnections = ObsConstraint.HTTP_MAX_CONNECT_VALUE;
        this.maxErrorRetry = ObsConstraint.HTTP_RETRY_MAX_VALUE;
        this.socketTimeout = ObsConstraint.HTTP_SOCKET_TIMEOUT_VALUE;
        this.endpointHttpPort = ObsConstraint.HTTP_PORT_VALUE;
        this.endpointHttpsPort = ObsConstraint.HTTPS_PORT_VALUE;
        this.httpsOnly = true;
        this.endPoint = "";
        this.pathStyle = false;
        this.validateCertificate = false;
        this.verifyResponseContentType = true;
        this.isStrictHostnameVerification = false;
        this.uploadStreamRetryBufferSize = -1;
        this.socketWriteBufferSize = -1;
        this.socketReadBufferSize = -1;
        this.readBufferSize = -1;
        this.writeBufferSize = -1;
        this.idleConnectionTime = ObsConstraint.DEFAULT_IDLE_CONNECTION_TIME;
        this.maxIdleConnections = ObsConstraint.DEFAULT_MAX_IDLE_CONNECTIONS;
        this.authType = AuthTypeEnum.OBS;
        this.keepAlive = true;
        this.signatString = "";
        this.defaultBucketLocation = "";
        this.authTypeNegotiation = true;
        this.cname = false;
        this.delimiter = "/";
        this.httpProtocolType = HttpProtocolTypeEnum.HTTP1_1;
        this.xmlDocumentBuilderFactoryClass = ObsConstraint.OBS_XML_DOC_BUILDER_FACTORY_CLASS;
        this.localAuthTypeCacheCapacity = ObsConstraint.DEFAULT_LOCAL_AUTH_TYPE_CACHE_CAPACITY;
    }

    public String getDelimiter() {
        return delimiter;
    }

    /**
     * Set folder isolators to slashes.
     * 
     * @param delimiter
     *            Folder isolator
     */
    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    @Deprecated
    public String getSignatString() {
        return signatString;
    }

    @Deprecated
    public void setSignatString(String signatString) {
        this.signatString = signatString;
    }

    @Deprecated
    public String getDefaultBucketLocation() {
        return defaultBucketLocation;
    }

    @Deprecated
    public void setDefaultBucketLocation(String defaultBucketLocation) {
        this.defaultBucketLocation = defaultBucketLocation;
    }

    @Deprecated
    public int getBufferSize() {
        return bufferSize;
    }

    @Deprecated
    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    /**
     * Check whether path-style access to OBS is enabled. "true" indicates that
     * path-style access is enabled while "false" (default) indicates virtual
     * hosted-style access is enabled. Note: If the path-style access is
     * enabled, new bucket features of OBS 3.0 are not supported.
     * 
     * @return Whether to enable path-style access to OBS.
     */
    public boolean isDisableDnsBucket() {
        return this.isPathStyle();
    }

    /**
     * Specify whether to enable path-style access to OBS. "true" indicates that
     * path-style access is enabled while "false" (default) indicates that
     * virtual hosted-style access is enabled. Note: If the path-style access is
     * enabled, new bucket features of OBS 3.0 are not supported.
     * 
     * @param disableDns
     *            Whether to enable path-style access to OBS.
     */
    public void setDisableDnsBucket(boolean disableDns) {
        this.setPathStyle(disableDns);
    }

    /**
     * Obtain the size (bytes) of the socket receive buffer, corresponding to
     * the "java.net.SocketOptions.SO_RVCBUF" parameter. The default value is
     * "-1", indicating that the size is not set.
     * 
     * @return Socket receive buffer size
     */
    public int getSocketReadBufferSize() {
        return socketReadBufferSize;
    }

    /**
     * Set the size (bytes) of the socket receive buffer, corresponding to the
     * "java.net.SocketOptions.SO_RVCBUF" parameter. The default value is "-1",
     * indicating that the size is not set.
     * 
     * @param socketReadBufferSize
     *            Socket receive buffer size
     */
    public void setSocketReadBufferSize(int socketReadBufferSize) {
        this.socketReadBufferSize = socketReadBufferSize;
    }

    /**
     * Obtain the size (bytes) of the socket send buffer, corresponding to the
     * "java.net.SocketOptions.SO_SNDBUF" parameter. The default value is "-1",
     * indicating that the size is not set.
     * 
     * @return socketSocket send buffer size
     */
    public int getSocketWriteBufferSize() {
        return socketWriteBufferSize;
    }

    /**
     * Set the size (bytes) of the socket send buffer (in bytes), corresponding
     * to the "java.net.SocketOptions.SO_SNDBUF" parameter. The default value is
     * "-1", indicating that the size is not set.
     * 
     * @param socketWriteBufferSize
     *            socket Socket send buffer size
     */
    public void setSocketWriteBufferSize(int socketWriteBufferSize) {
        this.socketWriteBufferSize = socketWriteBufferSize;
    }

    @Deprecated
    public int getConnectionRequestTimeout() {
        return connectionRequestTimeout;
    }

    @Deprecated
    public void setConnectionRequestTimeout(int connectionRequestTimeout) {
        this.connectionRequestTimeout = connectionRequestTimeout;
    }

    @Deprecated
    public void disableNio() {
        this.isNio = false;
    }

    @Deprecated
    public void enableNio() {
        this.isNio = true;
    }

    @Deprecated
    public boolean isNio() {
        return this.isNio;
    }

    @Deprecated
    public boolean isUseReaper() {
        return useReaper;
    }

    @Deprecated
    public void setUseReaper(boolean useReaper) {
        this.useReaper = useReaper;
    }

    /**
     * Obtain the factory for generating the "KeyManager" array.
     * 
     * @return Factory for generating the "KeyManager" array
     */
    public KeyManagerFactory getKeyManagerFactory() {
        return keyManagerFactory;
    }

    /**
     * Set the factory for generating the "KeyManager" array.
     * 
     * @param keyManagerFactory
     *            Factory for generating the "KeyManager" array
     */
    public void setKeyManagerFactory(KeyManagerFactory keyManagerFactory) {
        this.keyManagerFactory = keyManagerFactory;
    }

    /**
     * Obtain the factory for generating the "TrustManager" array.
     * 
     * @return Factory for generating the "TrustManager" array
     */
    public TrustManagerFactory getTrustManagerFactory() {
        return trustManagerFactory;
    }

    /**
     * Set the factory for generating the "TrustManager" array.
     * 
     * @param trustManagerFactory
     *            Factory for generating the "TrustManager" array
     */
    public void setTrustManagerFactory(TrustManagerFactory trustManagerFactory) {
        this.trustManagerFactory = trustManagerFactory;
    }

    /**
     * Obtain the identifier specifying whether to verify the domain name
     * ("false" by default).
     * 
     * @return Identifier specifying whether to verify the domain name
     */
    public boolean isStrictHostnameVerification() {
        return isStrictHostnameVerification;
    }

    /**
     * Set the identifier specifying whether to verify the domain name.
     * 
     * @param isStrictHostnameVerification
     *            Identifier specifying whether to verify the domain name
     */
    public void setIsStrictHostnameVerification(boolean isStrictHostnameVerification) {
        this.isStrictHostnameVerification = isStrictHostnameVerification;
    }

    /**
     * Check whether path-style access to OBS is enabled. "true" indicates that
     * path-style access is enabled while "false" (default) indicates virtual
     * hosted-style access is enabled. Note: If the path-style access is
     * enabled, new bucket features of OBS 3.0 are not supported.
     * 
     * @return Whether to enable path-style access to OBS.
     */
    public boolean isPathStyle() {
        return pathStyle;
    }

    /**
     * Specify whether to enable path-style access to OBS. "true" indicates that
     * path-style access is enabled while "false" (default) indicates that
     * virtual hosted-style access is enabled. Note: If the path-style access is
     * enabled, new bucket features of OBS 3.0 are not supported.
     * 
     * @param pathStyle
     *            Whether to enable path-style access to OBS.
     */
    public void setPathStyle(boolean pathStyle) {
        this.pathStyle = pathStyle;
    }

    /**
     * Obtain the timeout interval for establishing HTTP/HTTPS connections (in
     * milliseconds, 60,000 milliseconds by default).
     * 
     * @return Timeout interval for establishing HTTP/HTTPS connections
     */
    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    /**
     * Set the timeout interval for establishing HTTP/HTTPS connections (in
     * milliseconds, 60,000 milliseconds by default).
     * 
     * @param connectionTimeout
     *            Timeout interval for establishing HTTP/HTTPS connections
     */
    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    /**
     * Obtain the maximum number of HTTP connections (1000 by default) that can
     * be opened.
     * 
     * @return Maximum number of concurrently opened HTTP connections
     */
    public int getMaxConnections() {
        return maxConnections;
    }

    /**
     * Set the maximum number of HTTP connections (1000 by default) that can be
     * opened.
     * 
     * @param maxConnections
     *            Maximum number of concurrently opened HTTP connections
     */
    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    /**
     * Obtain the maximum number of retry attempts (5 by default) upon a request
     * failure (request exception, or error 500 or 503 on the server).
     * 
     * @return Maximum number of retry attempts upon a request failure
     */
    public int getMaxErrorRetry() {
        return maxErrorRetry;
    }

    /**
     * Set the maximum number of retry attempts (5 by default) upon a request
     * failure (request exception, or error 500 or 503 on the server).
     * 
     * @param maxErrorRetry
     *            Maximum number of retry attempts upon a request failure
     */
    public void setMaxErrorRetry(int maxErrorRetry) {
        this.maxErrorRetry = maxErrorRetry;
    }

    /**
     * Obtain the timeout interval for data transmission at the socket layer (in
     * milliseconds, 60,000 milliseconds by default).
     * 
     * @return Timeout interval for data transmission at the socket layer
     */
    public int getSocketTimeout() {
        return socketTimeout;
    }

    /**
     * Set the timeout interval for data transmission at the socket layer (in
     * milliseconds, 60,000 milliseconds by default).
     * 
     * @param socketTimeout
     *            Timeout interval for data transmission at the socket layer
     */
    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    /**
     * Obtain the endpoint for accessing OBS.
     * 
     * @return OBS endpoint
     */
    public String getEndPoint() {
        if (endPoint == null || endPoint.trim().equals("")) {
            throw new IllegalArgumentException("EndPoint is not set");
        }
        return endPoint.trim();
    }

    /**
     * Set the endpoint for accessing OBS.
     * 
     * @param endPoint
     *            OBS endpoint
     */
    public void setEndPoint(String endPoint) {
        this.endPoint = endPoint;
    }

    /**
     * Obtain the port number of the HTTP request (80 by default).
     * 
     * @return Port number of the HTTP request
     */
    public int getEndpointHttpPort() {
        return endpointHttpPort;
    }

    /**
     * Set the port number of the HTTP request (80 by default).
     * 
     * @param endpointHttpPort
     *            Port number of the HTTP request
     */
    public void setEndpointHttpPort(int endpointHttpPort) {
        this.endpointHttpPort = endpointHttpPort;
    }

    /**
     * Obtain the port number of the HTTPS request (443 by default).
     * 
     * @return Port number of the HTTPS request
     */
    public int getEndpointHttpsPort() {
        return endpointHttpsPort;
    }

    /**
     * Set the port number of the HTTPS request (443 by default).
     * 
     * @param endpointHttpsPort
     *            Port number of the HTTPS request
     */
    public void setEndpointHttpsPort(int endpointHttpsPort) {
        this.endpointHttpsPort = endpointHttpsPort;
    }

    /**
     * Specify whether to use HTTPS to connect OBS ("true" by default).
     * 
     * @param httpsOnly
     *            Identifier specifying whether to use HTTPS to connect OBS
     */
    public void setHttpsOnly(boolean httpsOnly) {
        this.httpsOnly = httpsOnly;
    }

    /**
     * Check whether HTTPS is used to connect OBS ("true" by default).
     * 
     * @return Identifier specifying whether to use HTTPS to connect OBS
     */
    public boolean isHttpsOnly() {
        return httpsOnly;
    }

    @Override
    public ObsConfiguration clone() throws CloneNotSupportedException {
        return (ObsConfiguration) super.clone();
    }

    /**
     * Obtain the proxy configuration.
     * 
     * @return Proxy configuration
     */
    public HttpProxyConfiguration getHttpProxy() {
        return httpProxy;
    }

    /**
     * Configure the proxy.
     * 
     * @param httpProxy
     *            HTTP proxy configuration
     */
    public void setHttpProxy(HttpProxyConfiguration httpProxy) {
        this.httpProxy = httpProxy;
    }

    /**
     * Configure the proxy server.
     * 
     * @param proxyAddr
     *            Proxy server address
     * @param proxyPort
     *            Proxy server port
     * @param userName
     *            Proxy username
     * @param password
     *            Proxy password
     * @param domain
     *            Proxy domain
     */
    @Deprecated
    public void setHttpProxy(String proxyAddr, int proxyPort, String userName, String password, String domain) {
        this.httpProxy = new HttpProxyConfiguration(proxyAddr, proxyPort, userName, password, domain);
    }

    /**
     * Configure the proxy server.
     * 
     * @param proxyAddr
     *            Proxy server address
     * @param proxyPort
     *            Proxy server port
     * @param userName
     *            Proxy username
     * @param password
     *            Proxy password
     */
    public void setHttpProxy(String proxyAddr, int proxyPort, String userName, String password) {
        this.httpProxy = new HttpProxyConfiguration(proxyAddr, proxyPort, userName, password, null);
    }

    /**
     * Set the buffer size used for uploading stream objects (in bytes). The
     * default value is 512 KB.
     * 
     * @param uploadStreamRetryBufferSize
     *            Buffer size used for uploading stream objects
     */
    @Deprecated
    public void setUploadStreamRetryBufferSize(int uploadStreamRetryBufferSize) {
        this.uploadStreamRetryBufferSize = uploadStreamRetryBufferSize;
    }

    /**
     * Obtain the buffer size used for uploading stream objects (in bytes). The
     * default value is 512 KB.
     * 
     * @return Buffer size used for uploading stream objects
     */
    @Deprecated
    public int getUploadStreamRetryBufferSize() {
        return this.uploadStreamRetryBufferSize;
    }

    /**
     * Check whether server-side verification is enabled. The default value is
     * "false".
     * 
     * @return Identifier specifying whether to enable server-side verification
     */
    public boolean isValidateCertificate() {
        return validateCertificate;
    }

    /**
     * Specify whether to enable server-side certificate verification. The
     * default value is "false".
     ** 
     * @param validateCertificate
     *            Identifier specifying whether to enable server-side
     *            verification
     */
    public void setValidateCertificate(boolean validateCertificate) {
        this.validateCertificate = validateCertificate;
    }

    /**
     * Check whether "ContentType" in the response is verified. The default
     * value is "true".
     * 
     * @return Identifier specifying whether to verify "ContentType" in the
     *         response
     */
    public boolean isVerifyResponseContentType() {
        return verifyResponseContentType;
    }

    /**
     * Specify whether to verify "ContentType" in the response. The default
     * value is "true".
     * 
     * @param verifyResponseContentType
     *            Identifier specifying whether to verify "ContentType" in the
     *            response
     */
    public void setVerifyResponseContentType(boolean verifyResponseContentType) {
        this.verifyResponseContentType = verifyResponseContentType;
    }

    /**
     * Obtain the read cache size used for uploading objects to socket streams
     * (in bytes). The default value is 8192.
     * 
     * @return Read cache size used for uploading objects to socket streams
     */
    public int getReadBufferSize() {
        return readBufferSize;
    }

    /**
     * Set the read cache size used for uploading objects to socket streams (in
     * bytes). The default value is 8192.
     * 
     * @param readBufferSize
     *            Read cache size used for uploading objects to socket streams
     */
    public void setReadBufferSize(int readBufferSize) {
        this.readBufferSize = readBufferSize;
    }

    /**
     * Obtain the write cache size used for uploading objects to socket streams
     * (in bytes). The default value is 8192.
     * 
     * @return Write cache size used for uploading objects to socket streams
     */
    public int getWriteBufferSize() {
        return writeBufferSize;
    }

    /**
     * Set the write cache size used for uploading objects to socket streams (in
     * bytes). The default value is 8192.
     * 
     * @param writeBufferSize
     *            Write cache size used for uploading objects to socket streams
     */
    public void setWriteBufferSize(int writeBufferSize) {
        this.writeBufferSize = writeBufferSize;
    }

    /**
     * Obtain the idle time for obtaining connections from the connection pool.
     * The unit is millisecond and the default value is 30000.
     * 
     * @return Maximum idle time for obtaining connections from the connection
     *         pool
     */
    public int getIdleConnectionTime() {
        return idleConnectionTime;
    }

    /**
     * Set the idle time for obtaining connections from the connection pool. The
     * unit is millisecond and the default value is 30000.
     * 
     * @param idleConnectionTime
     *            Maximum idle time for obtaining connections from the
     *            connection pool
     */
    public void setIdleConnectionTime(int idleConnectionTime) {
        this.idleConnectionTime = idleConnectionTime;
    }

    /**
     * Obtain the maximum number of idle connections in the connection pool. The
     * default value is 1000.
     * 
     * @return Maximum number of idle connections in the connection pool
     */
    public int getMaxIdleConnections() {
        return maxIdleConnections;
    }

    /**
     * Set the maximum number of idle connections in the connection pool. The
     * default value is 1000.
     * 
     * @param maxIdleConnections
     *            Maximum number of idle connections in the connection pool
     */
    public void setMaxIdleConnections(int maxIdleConnections) {
        this.maxIdleConnections = maxIdleConnections;
    }

    /**
     * Obtain the authentication type.
     * 
     * @return Authentication type
     */
    public AuthTypeEnum getAuthType() {
        return authType;
    }

    /**
     * Set the authentication type.
     * 
     * @param authType
     *            Authentication type
     */
    public void setAuthType(AuthTypeEnum authType) {
        this.authType = authType;
    }

    /**
     * Specify whether to use persistent connections.
     * 
     * @return Identifier specifying whether to use the persistent connections
     */
    public boolean isKeepAlive() {
        return keepAlive;
    }

    /**
     * Check whether persistent connections are used.
     * 
     * @param keepAlive
     *            Identifier specifying whether to use the persistent
     *            connections
     */
    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    /**
     * Check whether protocol negotiation is used.
     * 
     * @return Identifier specifying whether to use protocol negotiation
     */
    public boolean isAuthTypeNegotiation() {
        return authTypeNegotiation;
    }

    /**
     * Specify whether to use protocol negotiation.
     * 
     * @param authTypeNegotiation
     *            Identifier specifying whether to use protocol negotiation
     */
    public void setAuthTypeNegotiation(boolean authTypeNegotiation) {
        this.authTypeNegotiation = authTypeNegotiation;
    }

    /**
     * Check whether user-defined domain name is used.
     * 
     * @return Identifier specifying whether to use user-defined domain name
     */
    public boolean isCname() {
        return cname;
    }

    /**
     * Specify whether to use user-defined domain name.
     * 
     * @param cname
     *            Identifier specifying whether to use user-defined domain name
     */
    public void setCname(boolean cname) {
        this.cname = cname;
    }

    /**
     * Set the provider of SSLContext.
     * 
     * @return SSLContext provider
     */
    public String getSslProvider() {
        return sslProvider;
    }

    /**
     * Obtain the provider of SSLContext.
     * 
     * @param sslProvider
     *            SSLContext provider
     */
    public void setSslProvider(String sslProvider) {
        this.sslProvider = sslProvider;
    }

    /**
     * Set the HTTP type used for accessing OBS servers.
     * 
     * @return HTTP type
     */
    public HttpProtocolTypeEnum getHttpProtocolType() {
        return httpProtocolType;
    }

    /**
     * Obtain the HTTP type used for accessing OBS servers.
     * 
     * @param httpProtocolType
     *            HTTP type
     */
    public void setHttpProtocolType(HttpProtocolTypeEnum httpProtocolType) {
        this.httpProtocolType = httpProtocolType;
    }

    /**
     * Set the customized dispatcher.
     * 
     * @return Customized dispatcher
     */
    public Dispatcher getHttpDispatcher() {
        return httpDispatcher;
    }

    /**
     * Obtain the customized dispatcher.
     * 
     * @param httpDispatcher
     *            Customized dispatcher
     */
    public void setHttpDispatcher(Dispatcher httpDispatcher) {
        this.httpDispatcher = httpDispatcher;
    }

    public String getXmlDocumentBuilderFactoryClass() {
        return xmlDocumentBuilderFactoryClass;
    }

    public void setXmlDocumentBuilderFactoryClass(String xmlDocumentBuilderFactoryClass) {
        this.xmlDocumentBuilderFactoryClass = xmlDocumentBuilderFactoryClass;
    }

   
    public int getLocalAuthTypeCacheCapacity() {
        return localAuthTypeCacheCapacity;
    }

    public void setLocalAuthTypeCacheCapacity(int localAuthTypeCacheCapacity) {
        this.localAuthTypeCacheCapacity = localAuthTypeCacheCapacity;
    }
}
