/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.aliyun.oss;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Date;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.X509TrustManager;
import java.security.SecureRandom;

import com.aliyun.oss.common.auth.RequestSigner;
import com.aliyun.oss.common.comm.IdleConnectionReaper;
import com.aliyun.oss.common.comm.Protocol;
import com.aliyun.oss.common.comm.RetryStrategy;
import com.aliyun.oss.common.comm.SignVersion;
import com.aliyun.oss.common.utils.ResourceManager;
import com.aliyun.oss.common.utils.VersionInfoUtils;
import com.aliyun.oss.internal.OSSConstants;

/**
 * Client configurations for accessing to OSS services.
 */
public class ClientConfiguration {

    public static final String DEFAULT_USER_AGENT = VersionInfoUtils.getDefaultUserAgent();

    public static final int DEFAULT_MAX_RETRIES = 3;

    public static final int DEFAULT_CONNECTION_REQUEST_TIMEOUT = -1;
    public static final int DEFAULT_CONNECTION_TIMEOUT = 50 * 1000;
    public static final int DEFAULT_SOCKET_TIMEOUT = 50 * 1000;
    public static final int DEFAULT_MAX_CONNECTIONS = 1024;
    public static final long DEFAULT_CONNECTION_TTL = -1;
    public static final long DEFAULT_IDLE_CONNECTION_TIME = 60 * 1000;
    public static final int DEFAULT_VALIDATE_AFTER_INACTIVITY = 2 * 1000;
    public static final int DEFAULT_THREAD_POOL_WAIT_TIME = 60 * 1000;
    public static final int DEFAULT_REQUEST_TIMEOUT = 5 * 60 * 1000;
    public static final long DEFAULT_SLOW_REQUESTS_THRESHOLD = 5 * 60 * 1000;

    public static final boolean DEFAULT_USE_REAPER = true;

    public static final String DEFAULT_CNAME_EXCLUDE_LIST = "aliyuncs.com,aliyun-inc.com,aliyun.com";

    public static final SignVersion DEFAULT_SIGNATURE_VERSION = SignVersion.V1;

    protected String userAgent = DEFAULT_USER_AGENT;
    protected int maxErrorRetry = DEFAULT_MAX_RETRIES;
    protected int connectionRequestTimeout = DEFAULT_CONNECTION_REQUEST_TIMEOUT;
    protected int connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
    protected int socketTimeout = DEFAULT_SOCKET_TIMEOUT;
    protected int maxConnections = DEFAULT_MAX_CONNECTIONS;
    protected long connectionTTL = DEFAULT_CONNECTION_TTL;
    protected boolean useReaper = DEFAULT_USE_REAPER;
    protected long idleConnectionTime = DEFAULT_IDLE_CONNECTION_TIME;

    protected Protocol protocol = Protocol.HTTP;

    protected String proxyHost = null;
    protected int proxyPort = -1;
    protected String proxyUsername = null;
    protected String proxyPassword = null;
    protected String proxyDomain = null;
    protected String proxyWorkstation = null;

    protected boolean supportCname = true;
    protected List<String> cnameExcludeList = new ArrayList<String>();
    protected Lock rlock = new ReentrantLock();

    protected boolean sldEnabled = false;

    protected int requestTimeout = DEFAULT_REQUEST_TIMEOUT;
    protected boolean requestTimeoutEnabled = false;
    protected long slowRequestsThreshold = DEFAULT_SLOW_REQUESTS_THRESHOLD;

    protected Map<String, String> defaultHeaders = new LinkedHashMap<String, String>();

    protected boolean crcCheckEnabled = true;

    protected List<RequestSigner> signerHandlers = new LinkedList<RequestSigner>();

    protected SignVersion signatureVersion = DEFAULT_SIGNATURE_VERSION;

    protected long tickOffset = 0;

    private RetryStrategy retryStrategy;

    private boolean redirectEnable = true;

    private boolean verifySSLEnable = true;
    private KeyManager[] keyManagers = null;
    private X509TrustManager[] x509TrustManagers = null;
    private SecureRandom secureRandom = null;
    private HostnameVerifier hostnameVerifier = null;

    protected boolean logConnectionPoolStats = false;

    protected boolean useSystemPropertyValues = false;

    private boolean extractSettingFromEndpoint = true;

    public ClientConfiguration() {
        super();
        AppendDefaultExcludeList(this.cnameExcludeList);
    }

    /**
     * Gets the user agent string.
     *
     * @return The user agent string.
     */
    public String getUserAgent() {
        return userAgent;
    }

    /**
     * Sets the user agent string.
     *
     * @param userAgent
     *            The user agent string.
     */
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    /**
     * Gets proxy host.
     *
     * @return The proxy host in string.
     */
    public String getProxyHost() {
        return proxyHost;
    }

    /**
     * Sets the proxy host.
     *
     * @param proxyHost
     *            The proxy host in string.
     */
    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    /**
     * Gets the proxy host's port.
     *
     * @return The proxy host.
     */
    public int getProxyPort() {
        return proxyPort;
    }

    /**
     * Sets proxy port.
     *
     * @param proxyPort
     *            The proxy port.
     * @throws ClientException
     *           If any client side error occurs such as parameter invalid, an IO related failure,etc.
     */
    public void setProxyPort(int proxyPort) throws ClientException {
        if (proxyPort <= 0) {
            throw new ClientException(
                    ResourceManager.getInstance(OSSConstants.RESOURCE_NAME_COMMON).getString("ParameterIsInvalid"),
                    null);
        }
        this.proxyPort = proxyPort;
    }

    /**
     * Gets the proxy user name.
     *
     * @return The user name.
     */
    public String getProxyUsername() {
        return proxyUsername;
    }

    /**
     * Sets the proxy user name.
     *
     * @param proxyUsername
     *            The user name.
     */
    public void setProxyUsername(String proxyUsername) {
        this.proxyUsername = proxyUsername;
    }

    /**
     * Gets the proxy user password.
     *
     * @return The proxy user password.
     */
    public String getProxyPassword() {
        return proxyPassword;
    }

    /**
     * Sets the proxy user password.
     *
     * @param proxyPassword
     *            The proxy user password.
     */
    public void setProxyPassword(String proxyPassword) {
        this.proxyPassword = proxyPassword;
    }

    /**
     * Gets the proxy server's domain, which could do the NTLM authentiation
     * (optional).
     *
     * @return The proxy domain name.
     */
    public String getProxyDomain() {
        return proxyDomain;
    }

    /**
     * Sets the proxy server's domain, which could do the NTLM authentication
     * (optional).
     *
     * @param proxyDomain
     *            The proxy domain name.
     */
    public void setProxyDomain(String proxyDomain) {
        this.proxyDomain = proxyDomain;
    }

    /**
     * Gets the proxy host's NTLM authentication server.
     *
     * @return The NTLM authentication server name.
     */
    public String getProxyWorkstation() {
        return proxyWorkstation;
    }

    /**
     * Sets the proxy host's NTLM authentication server(optional, if the proxy
     * server does not require NTLM authentication, then it's not needed).
     *
     * @param proxyWorkstation
     *            The proxy host's NTLM authentication server name.
     */
    public void setProxyWorkstation(String proxyWorkstation) {
        this.proxyWorkstation = proxyWorkstation;
    }

    /**
     * Gets the max connection count.
     *
     * @return The max connection count. By default it's 1024.
     */
    public int getMaxConnections() {
        return maxConnections;
    }

    /**
     * Sets the max connection count.
     *
     * @param maxConnections
     *            The max connection count.
     */
    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    /**
     * Gets the socket timeout in millisecond. 0 means infinite timeout, not
     * recommended.
     *
     * @return The socket timeout in millisecond.
     */
    public int getSocketTimeout() {
        return socketTimeout;
    }

    /**
     * Sets the socket timeout in millisecond. 0 means infinite timeout, not
     * recommended.
     *
     * @param socketTimeout
     *            The socket timeout in millisecond.
     */
    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    /**
     * Gets the socket connection timeout in millisecond.
     *
     * @return The socket connection timeout in millisecond.
     */
    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    /**
     * Sets the socket connection timeout in millisecond.
     *
     * @param connectionTimeout
     *            The socket connection timeout in millisecond.
     */
    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    /**
     * Gets the timeout in millisecond for retrieving an available connection
     * from the connection manager. 0 means infinite and -1 means not defined.
     * By default it's -1.
     *
     * @return The timeout in millisecond.
     */
    public int getConnectionRequestTimeout() {
        return connectionRequestTimeout;
    }

    /**
     * Sets the timeout in millisecond for retrieving an available connection
     * from the connection manager.
     *
     * @param connectionRequestTimeout
     *            The timeout in millisecond.
     */
    public void setConnectionRequestTimeout(int connectionRequestTimeout) {
        this.connectionRequestTimeout = connectionRequestTimeout;
    }

    /**
     * Gets the max retry count upon a retryable error. By default it's 3.
     *
     * @return The max retry count.
     */
    public int getMaxErrorRetry() {
        return maxErrorRetry;
    }

    /**
     * Sets the max retry count upon a retryable error. By default it's 3.
     *
     * @param maxErrorRetry
     *            The max retry count.
     */
    public void setMaxErrorRetry(int maxErrorRetry) {
        this.maxErrorRetry = maxErrorRetry;
    }

    /**
     * Gets the connection TTL (time to live). Http connection is cached by the
     * connection manager with a TTL.
     *
     * @return The connection TTL in millisecond.
     */
    public long getConnectionTTL() {
        return connectionTTL;
    }

    /**
     * Sets the connection TTL (time to live). Http connection is cached by the
     * connection manager with a TTL.
     *
     * @param connectionTTL
     *            The connection TTL in millisecond.
     */
    public void setConnectionTTL(long connectionTTL) {
        this.connectionTTL = connectionTTL;
    }

    /**
     * Gets the flag of using {@link IdleConnectionReaper} to manage expired
     * connection.
     *
     * @return True if it's enabled; False if it's disabled.
     */
    public boolean isUseReaper() {
        return useReaper;
    }

    /**
     * Sets the flag of using {@link IdleConnectionReaper} to manage expired
     * connection.
     *
     * @param useReaper
     *            The flag of using {@link IdleConnectionReaper}.
     */
    public void setUseReaper(boolean useReaper) {
        this.useReaper = useReaper;
    }

    /**
     * Gets the connection's max idle time. If a connection has been idle for
     * more than this number, it would be closed.
     *
     * @return The connection's max idle time in millisecond.
     */
    public long getIdleConnectionTime() {
        return idleConnectionTime;
    }

    /**
     * Sets the connection's max idle time. If a connection has been idle for
     * more than this number, it would be closed.
     *
     * @param idleConnectionTime
     *            The connection's max idle time in millisecond.
     */
    public void setIdleConnectionTime(long idleConnectionTime) {
        this.idleConnectionTime = idleConnectionTime;
    }

    /**
     * Gets the OSS's protocol (HTTP or HTTPS).
     *
     * @return The OSS's protocol.
     */
    public Protocol getProtocol() {
        return protocol;
    }

    /**
     * Sets the OSS's protocol (HTTP or HTTPS).
     *
     * @param protocol
     *            The OSS's protocol.
     */
    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

    /**
     * Gets the immutable excluded CName list----any domain ends with an item in
     * this list will not do Cname resolution.
     *
     * @return The excluded CName list, immutable.
     */
    public List<String> getCnameExcludeList() {
        return Collections.unmodifiableList(this.cnameExcludeList);
    }

    /**
     * Sets the immutable excluded CName list----any domain ends with an item in
     * this list will not do Cname resolution.
     *
     * @param cnameExcludeList
     *            The excluded CName list, immutable.
     */
    public void setCnameExcludeList(List<String> cnameExcludeList) {
        if (cnameExcludeList == null) {
            throw new IllegalArgumentException("cname exclude list should not be null.");
        }

        this.cnameExcludeList.clear();
        for (String excl : cnameExcludeList) {
            if (!excl.trim().isEmpty()) {
                this.cnameExcludeList.add(excl);
            }
        }

        AppendDefaultExcludeList(this.cnameExcludeList);
    }

    /**
     * Append default excluded CName list.
     *
     * @param excludeList
     *            The excluded CName list.
     */
    private static void AppendDefaultExcludeList(List<String> excludeList) {
        String[] excludes = DEFAULT_CNAME_EXCLUDE_LIST.split(",");
        for (String excl : excludes) {
            if (!excl.trim().isEmpty() && !excludeList.contains(excl)) {
                excludeList.add(excl.trim().toLowerCase());
            }
        }
    }

    /**
     * Gets the flag if supporting Cname in the endpoint. By default it's true.
     *
     * @return True if supporting Cname; False if not.
     */
    public boolean isSupportCname() {
        return supportCname;
    }

    /**
     * Sets the flag if supporting Cname in the endpoint. By default it's true.
     *
     * <p>
     * If this value is set true, when building a canonical url, the host would
     * be checked against the Cname excluded list. If that host is found in the
     * list, then it's treated as non-CName and accessed as TLD (third level
     * domain). If the host is found, then it's thought as CName. If this value
     * is set false, then always uses TLD to access the endpoint.
     * </p>
     *
     * @param supportCname
     *            The flag if supporting CName.
     *
     * @return This {@link ClientConfiguration}, enabling additional method
     *         calls to be chained together.
     */
    public ClientConfiguration setSupportCname(boolean supportCname) {
        this.supportCname = supportCname;
        return this;
    }

    /**
     * Gets the flag of using SLD (Second Level Domain) style to access the
     * endpoint. By default it's false. When using SLD, then the bucket endpoint
     * would be: http://host/bucket. Otherwise, it will be http://bucket.host
     *
     * @return True if it's enabled; False if it's disabled.
     */
    public boolean isSLDEnabled() {
        return sldEnabled;
    }

    /**
     * Sets the flag of using SLD (Second Level Domain) style to access the
     * endpoint. By default it's false.
     *
     * @param enabled
     *            True if it's enabled; False if it's disabled.
     *
     * @return This {@link ClientConfiguration}, enabling additional method
     *         calls to be chained together.
     */
    public ClientConfiguration setSLDEnabled(boolean enabled) {
        this.sldEnabled = enabled;
        return this;
    }

    /**
     * The connection idle time threshold in millisecond that triggers the
     * validation. By default it's 2000.
     *
     * @return The connection idle time threshold.
     */
    public int getValidateAfterInactivity() {
        return DEFAULT_VALIDATE_AFTER_INACTIVITY;
    }

    /**
     * Gets the flag of enabling request timeout. By default it's disabled.
     *
     * @return true enabled; false disabled.
     */
    public boolean isRequestTimeoutEnabled() {
        return requestTimeoutEnabled;
    }

    /**
     * Gets the flag of enabling request timeout. By default it's disabled.
     *
     * @param requestTimeoutEnabled
     *            true to enable; false to disable.
     */
    public void setRequestTimeoutEnabled(boolean requestTimeoutEnabled) {
        this.requestTimeoutEnabled = requestTimeoutEnabled;
    }

    /**
     * Sets the timeout value in millisecond. By default it's 5 min.
     *
     * @param requestTimeout
     *            The timeout value in millisecond.
     */
    public void setRequestTimeout(int requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    /**
     * Gets the timeout value in millisecond.
     *
     * @return the tiemout value.
     */
    public int getRequestTimeout() {
        return requestTimeout;
    }

    /**
     * Sets the slow request's latency threshold. If a request's latency is more
     * than it, the request will be logged. By default the threshold is 5 min.
     *
     * @return The slow request's latency threshold in millisecond.
     */
    public long getSlowRequestsThreshold() {
        return slowRequestsThreshold;
    }

    /**
     * Gets the slow request's latency threshold. If a request's latency is more
     * than it, the request will be logged.
     *
     * @param slowRequestsThreshold
     *            The slow request's latency threshold in millisecond.
     */
    public void setSlowRequestsThreshold(long slowRequestsThreshold) {
        this.slowRequestsThreshold = slowRequestsThreshold;
    }

    /**
     * Gets the default http headers. All these headers would be automatically
     * added in every request. And if a header is also specified in the request,
     * the default one will be overwritten.
     *
     * @return The default http headers.
     */
    public Map<String, String> getDefaultHeaders() {
        return defaultHeaders;
    }

    /**
     * Sets the default http headers. All these headers would be automatically
     * added in every request. And if a header is also specified in the request,
     * the default one will be overwritten.
     *
     * @param defaultHeaders
     *            Default http headers.
     */
    public void setDefaultHeaders(Map<String, String> defaultHeaders) {
        this.defaultHeaders = defaultHeaders;
    }

    /**
     * Add a default header into the default header list.
     *
     * @param key
     *            The default header name.
     * @param value
     *            The default header value.
     */
    public void addDefaultHeader(String key, String value) {
        this.defaultHeaders.put(key, value);
    }

    /**
     * Gets the flag of enabling CRC checksum on upload and download. By default
     * it's true.
     *
     * @return true enable CRC;false disable CRC.
     */
    public boolean isCrcCheckEnabled() {
        return crcCheckEnabled;
    }

    /**
     * Sets the flag of enabling CRC checksum on upload and download. By default
     * it's true.
     *
     * @param crcCheckEnabled
     *            True to enable CRC; False to disable CRC.
     */
    public void setCrcCheckEnabled(boolean crcCheckEnabled) {
        this.crcCheckEnabled = crcCheckEnabled;
    }

    /**
     * Gets signer handlers
     *
     * @return signer handlers
     */
    public List<RequestSigner> getSignerHandlers() {
        return signerHandlers;
    }

    /**
     * Sets signer handlers using for authentication of the proxy server.
     *
     * @param signerHandlers
     *            A list of {@link RequestSigner} handlers.
     */
    public void setSignerHandlers(List<RequestSigner> signerHandlers) {
        if (signerHandlers == null) {
            return;
        }
        this.signerHandlers.clear();
        for (RequestSigner signer : signerHandlers) {
            if (signer != null) {
                this.signerHandlers.add(signer);
            }
        }
    }

    /**
     * Gets signature version
     *
     * @return signature version
     */
    public SignVersion getSignatureVersion() {
        return signatureVersion;
    }

    /**
     * Sets signature version for all request.
     *
     * @param signatureVersion
     *             The signature version.
     */
    public void setSignatureVersion(SignVersion signatureVersion) {
        this.signatureVersion = signatureVersion;
    }

    /**
     * Gets the difference between customized epoch time and local time, in millisecond.
     *
     * @return tick Offset
     */
    public long getTickOffset() {
        return tickOffset;
    }

    /**
     * Sets the custom base time.
     * OSS's token validation logic depends on the time.
     * It requires that there's no more than 15 min time difference between client and OSS server.
     * This API calculates the difference between local time to epoch time.
     * Later one other APIs use this difference to offset the local time before sending request to OSS.
     *
     * @param epochTicks
     *             Custom Epoch ticks (in millisecond).
     */
    public void setTickOffset(long epochTicks) {
        long localTime = new Date().getTime();
        this.tickOffset = epochTicks - localTime;
    }

    /**
     * Gets the retry strategy
     *
     * @return {@link RetryStrategy} object.
     */
    public RetryStrategy getRetryStrategy() {
        return retryStrategy;
    }

    /**
     * Sets the retry strategy.
     *
     * @param retryStrategy
     *          The retryStrategy is used to check whether to retry request or not.
     *          If it has been specified, the client will prefer this retryStrategy
     *          to its private strategy, you can check the SDK private class
     *          com.aliyun.oss.common.comm.DefaultServiceClient#DefaultRetryStrategy
     *          to see the service client's private default strategy.
     */
    public void setRetryStrategy(RetryStrategy retryStrategy) {
        this.retryStrategy = retryStrategy;
    }

    /**
     * Gets the flag of http redirection.
     *
     * @return the flag of http redirection.
     */
    public boolean isRedirectEnable() {
        return redirectEnable;
    }

    /**
     * Sets the flag of http redirection.
     *
     * @param redirectEnable
     *          Determines whether redirects should be handled automatically.
     */
    public void setRedirectEnable(boolean redirectEnable) {
        this.redirectEnable = redirectEnable;
    }

    /**
     * Gets the flag of verifing SSL certificate. By default it's true.
     *
     * @return true verify SSL certificate;false ignore SSL certificate.
     */
    public boolean isVerifySSLEnable() {
        return verifySSLEnable;
    }

    /**
     * Sets the flag of verifing SSL certificate.
     *
     * @param verifySSLEnable
     *            True to verify SSL certificate; False to ignore SSL certificate.
     */
    public void setVerifySSLEnable(boolean verifySSLEnable) {
        this.verifySSLEnable = verifySSLEnable;
    }

    /**
     * Gets the KeyManagers are responsible for managing the key material
     * which is used to authenticate the local SSLSocket to its peer.
     *
     * @return the key managers.
     */
    public KeyManager[] getKeyManagers() {
        return keyManagers;
    }

    /**
     * Sets the key managers are responsible for managing the key material
     * which is used to authenticate the local SSLSocket to its peer.
     *
     * @param keyManagers
     *            the key managers
     */
    public void setKeyManagers(KeyManager[] keyManagers) {
        this.keyManagers = keyManagers;
    }

    /**
     * Gets the instance of this interface manage which X509 certificates
     * may be used to authenticate the remote side of a secure socket.
     *
     * @return the x509 trust managers .
     */
    public X509TrustManager[] getX509TrustManagers() {
        return x509TrustManagers;
    }

    /**
     * Sets the instance of this interface manage which X509 certificates
     * may be used to authenticate the remote side of a secure socket.
     *
     * @param x509TrustManagers
     *            x509 trust managers
     */
    public void setX509TrustManagers(X509TrustManager[] x509TrustManagers) {
        this.x509TrustManagers = x509TrustManagers;
    }

    /**
     * Gets the cryptographically strong random number.
     *
     * @return random number.
     */
    public SecureRandom getSecureRandom() {
        return secureRandom;
    }

    /**
     * Sets the cryptographically strong random number.
     *
     * @param secureRandom
     *            the cryptographically strong random number
     */
    public void setSecureRandom(SecureRandom secureRandom) {
        this.secureRandom = secureRandom;
    }

    /**
     * Gets the instance of this interface for hostname verification.
     *
     * @return the hostname verification instance.
     */
    public HostnameVerifier getHostnameVerifier() {
        return hostnameVerifier;
    }

	/**
     * Sets instance of this interface for hostname verification.
     *
     * @param hostnameVerifier
     *            the hostname verification instance
     */
    public void setHostnameVerifier(HostnameVerifier hostnameVerifier) {
        this.hostnameVerifier = hostnameVerifier;
    }

    /**
     * Sets the flag of logging connection pool statistics.
     *
     * @param enabled
     *            True if it's enabled; False if it's disabled.
     */
    public void setLogConnectionPoolStats(boolean enabled) {
        this.logConnectionPoolStats = enabled;
    }

    /**
     * Gets the flag of logging connection pool statistics. By default it's disabled.
     *
     * @return true enabled; false disabled.
     */
    public boolean isLogConnectionPoolStatsEnable() {
        return logConnectionPoolStats;
    }

    /**
     * Sets the flag of using system property values if any of the config options are missing.
     * The properties currently supported are http.proxyHost, http.proxyPort, http.proxyUser
     * and http.proxyPassword.
     *
     * @param enabled True if it's enabled; False if it's disabled.
     */
    public void setUseSystemPropertyValues(boolean enabled) {
        this.useSystemPropertyValues = enabled;
    }

    /**
     * Gets the flag of using system property values. By default it's disabled.
     *
     * @return true enabled; false disabled.
     */
    public boolean isUseSystemPropertyValues() {
        return useSystemPropertyValues;
    }

    /**
     * Sets the flag of extracting setting from endpoint.
     *
     * @param enabled True if it's enabled; False if it's disabled.
     */
    public void setExtractSettingFromEndpoint(boolean enabled) {
        this.extractSettingFromEndpoint = enabled;
    }

    /**
     * Gets the flag of extracting setting from endpoint. By default it's enabled.
     *
     * @return true enabled; false disabled.
     */
    public boolean isExtractSettingFromEndpointEnable() {
        return extractSettingFromEndpoint;
    }
}
