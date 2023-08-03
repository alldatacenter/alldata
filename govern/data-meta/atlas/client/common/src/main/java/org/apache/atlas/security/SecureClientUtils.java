/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.security;

import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.client.urlconnection.HttpURLConnectionFactory;
import com.sun.jersey.client.urlconnection.URLConnectionClientHandler;
import org.apache.atlas.AtlasException;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.security.alias.CredentialProviderFactory;
import org.apache.hadoop.security.authentication.client.Authenticator;
import org.apache.hadoop.security.authentication.client.ConnectionConfigurator;
import org.apache.hadoop.security.ssl.SSLFactory;
import org.apache.hadoop.security.token.delegation.web.DelegationTokenAuthenticatedURL;
import org.apache.hadoop.security.token.delegation.web.DelegationTokenAuthenticator;
import org.apache.hadoop.security.token.delegation.web.KerberosDelegationTokenAuthenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.GeneralSecurityException;
import java.security.PrivilegedExceptionAction;

import static org.apache.atlas.security.SecurityProperties.CERT_STORES_CREDENTIAL_PROVIDER_PATH;
import static org.apache.atlas.security.SecurityProperties.CLIENT_AUTH_KEY;
import static org.apache.atlas.security.SecurityProperties.KEYSTORE_FILE_KEY;
import static org.apache.atlas.security.SecurityProperties.TRUSTSTORE_FILE_KEY;

/**
 *
 */
public class SecureClientUtils {

    public final static int DEFAULT_SOCKET_TIMEOUT_IN_MSECS = 1 * 60 * 1000; // 1 minute
    private static final Logger LOG = LoggerFactory.getLogger(SecureClientUtils.class);
    private SSLFactory factory = null;


    public  URLConnectionClientHandler getClientConnectionHandler(DefaultClientConfig config,
            org.apache.commons.configuration.Configuration clientConfig, String doAsUser,
            final UserGroupInformation ugi) {
        config.getProperties().put(URLConnectionClientHandler.PROPERTY_HTTP_URL_CONNECTION_SET_METHOD_WORKAROUND, true);
        Configuration conf = new Configuration();
        conf.addResource(conf.get(SSLFactory.SSL_CLIENT_CONF_KEY, SecurityProperties.SSL_CLIENT_PROPERTIES));
        UserGroupInformation.setConfiguration(conf);
        final ConnectionConfigurator connConfigurator = newConnConfigurator(conf);

        Authenticator authenticator = new KerberosDelegationTokenAuthenticator();

        authenticator.setConnectionConfigurator(connConfigurator);
        final DelegationTokenAuthenticator finalAuthenticator = (DelegationTokenAuthenticator) authenticator;
        final DelegationTokenAuthenticatedURL.Token token = new DelegationTokenAuthenticatedURL.Token();
        HttpURLConnectionFactory httpURLConnectionFactory = null;
        try {
            UserGroupInformation ugiToUse = ugi != null ? ugi : UserGroupInformation.getCurrentUser();
            final UserGroupInformation actualUgi =
                    (ugiToUse.getAuthenticationMethod() == UserGroupInformation.AuthenticationMethod.PROXY)
                    ? ugiToUse.getRealUser() : ugiToUse;
            LOG.info("Real User: {}, is from ticket cache? {}", actualUgi, actualUgi.isLoginTicketBased());
            if (StringUtils.isEmpty(doAsUser) || StringUtils.equals(doAsUser, actualUgi.getShortUserName())) {
                doAsUser = null;
            }

            LOG.info("doAsUser: {}", doAsUser);
            final String finalDoAsUser = doAsUser;
            httpURLConnectionFactory = new HttpURLConnectionFactory() {
                @Override
                public HttpURLConnection getHttpURLConnection(final URL url) throws IOException {
                    try {
                        return actualUgi.doAs(new PrivilegedExceptionAction<HttpURLConnection>() {
                            @Override
                            public HttpURLConnection run() throws Exception {
                                try {
                                    return new DelegationTokenAuthenticatedURL(finalAuthenticator, connConfigurator)
                                        .openConnection(url, token, finalDoAsUser);
                                } catch (Exception e) {
                                    throw new IOException(e);
                                }
                            }
                        });
                    } catch (Exception e) {
                        if (e instanceof IOException) {
                            throw (IOException) e;
                        } else {
                            throw new IOException(e);
                        }
                    }
                }
            };
        } catch (IOException e) {
            LOG.warn("Error obtaining user", e);
        }

        return new URLConnectionClientHandler(httpURLConnectionFactory);
    }

    private final static ConnectionConfigurator DEFAULT_TIMEOUT_CONN_CONFIGURATOR = new ConnectionConfigurator() {
        @Override
        public HttpURLConnection configure(HttpURLConnection conn) throws IOException {
            setTimeouts(conn, DEFAULT_SOCKET_TIMEOUT_IN_MSECS);
            return conn;
        }
    };

    private  ConnectionConfigurator newConnConfigurator(Configuration conf) {
        try {
            return newSslConnConfigurator(DEFAULT_SOCKET_TIMEOUT_IN_MSECS, conf);
        } catch (Exception e) {
            LOG.debug("Cannot load customized ssl related configuration. " + "Fallback to system-generic settings.", e);
            return DEFAULT_TIMEOUT_CONN_CONFIGURATOR;
        }
    }

    private  ConnectionConfigurator newSslConnConfigurator(final int timeout, Configuration conf)
    throws IOException, GeneralSecurityException {
        final SSLSocketFactory sf;
        final HostnameVerifier hv;

        factory = getSSLFactory(conf);
        sf = factory.createSSLSocketFactory();
        hv = factory.getHostnameVerifier();

        return new ConnectionConfigurator() {
            @Override
            public HttpURLConnection configure(HttpURLConnection conn) throws IOException {
                if (conn instanceof HttpsURLConnection) {
                    HttpsURLConnection c = (HttpsURLConnection) conn;
                    c.setSSLSocketFactory(sf);
                    c.setHostnameVerifier(hv);
                }
                setTimeouts(conn, timeout);
                return conn;
            }
        };
    }

    public SSLFactory getSSLFactory(Configuration conf) throws IOException, GeneralSecurityException {
        if (factory == null) {
            factory = new SSLFactory(SSLFactory.Mode.CLIENT, conf);
            factory.init();
        }
        return factory;
    }

    public void destroyFactory() {
        if (factory != null) {
            factory.destroy();
            factory = null;
        }
    }


    private static void setTimeouts(URLConnection connection, int socketTimeout) {
        connection.setConnectTimeout(socketTimeout);
        connection.setReadTimeout(socketTimeout);
    }

    private static File getSSLClientFile(String confLocation) throws AtlasException {
        File sslDir;
        try {
            if (confLocation == null) {
                String persistDir = null;
                URL resource = SecureClientUtils.class.getResource("/");
                if (resource != null) {
                    persistDir = resource.toURI().getPath();
                }
                assert persistDir != null;
                sslDir = new File(persistDir);
            } else {
                sslDir = new File(confLocation);
            }
            LOG.info("ssl-client.xml will be created in {}", sslDir);
        } catch (Exception e) {
            throw new AtlasException("Failed to find client configuration directory", e);
        }
        return new File(sslDir, SecurityProperties.SSL_CLIENT_PROPERTIES);
    }

    public static void persistSSLClientConfiguration(org.apache.commons.configuration.Configuration clientConfig, String confLocation)
    throws AtlasException, IOException {
        //trust settings
        Configuration configuration = new Configuration(false);
        File sslClientFile = getSSLClientFile(confLocation);
        if (!sslClientFile.exists()) {
            configuration.set("ssl.client.truststore.type", "jks");
            configuration.set("ssl.client.truststore.location", clientConfig.getString(TRUSTSTORE_FILE_KEY));
            if (clientConfig.getBoolean(CLIENT_AUTH_KEY, false)) {
                // need to get client key properties
                configuration.set("ssl.client.keystore.location", clientConfig.getString(KEYSTORE_FILE_KEY));
                configuration.set("ssl.client.keystore.type", "jks");
            }
            // add the configured credential provider
            configuration.set(CredentialProviderFactory.CREDENTIAL_PROVIDER_PATH,
                    clientConfig.getString(CERT_STORES_CREDENTIAL_PROVIDER_PATH));
            String hostnameVerifier = clientConfig.getString(SSLFactory.SSL_HOSTNAME_VERIFIER_KEY);
            if (hostnameVerifier != null) {
                configuration.set(SSLFactory.SSL_HOSTNAME_VERIFIER_KEY, hostnameVerifier);
            }

            configuration.writeXml(new FileWriter(sslClientFile));
        }
    }

    public  URLConnectionClientHandler getUrlConnectionClientHandler() {
        return new URLConnectionClientHandler(new HttpURLConnectionFactory() {
            @Override
            public HttpURLConnection getHttpURLConnection(URL url)
                    throws IOException {
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                if (connection instanceof HttpsURLConnection) {
                    LOG.debug("Attempting to configure HTTPS connection using client "
                            + "configuration");
                    final SSLFactory factory;
                    final SSLSocketFactory sf;
                    final HostnameVerifier hv;

                    try {
                        Configuration conf = new Configuration();
                        conf.addResource(conf.get(SSLFactory.SSL_CLIENT_CONF_KEY, SecurityProperties.SSL_CLIENT_PROPERTIES));
                        UserGroupInformation.setConfiguration(conf);

                        HttpsURLConnection c = (HttpsURLConnection) connection;
                        factory = getSSLFactory(conf);
                        sf = factory.createSSLSocketFactory();
                        hv = factory.getHostnameVerifier();
                        c.setSSLSocketFactory(sf);
                        c.setHostnameVerifier(hv);
                    } catch (Exception e) {
                        LOG.info("Unable to configure HTTPS connection from "
                                + "configuration.  Leveraging JDK properties.");
                    }
                }
                return connection;
            }
        });
    }
}
