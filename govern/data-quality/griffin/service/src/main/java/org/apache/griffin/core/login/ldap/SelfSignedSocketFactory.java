/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/

package org.apache.griffin.core.login.ldap;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.griffin.core.exception.GriffinException;

/**
 * SocketFactory ignoring insecure (self-signed, expired) certificates.
 * <p>
 * Maintains internal {@code SSLSocketFactory} configured with {@code NoopTrustManager}.
 * All SocketFactory methods are proxied to internal SSLSocketFactory instance.
 * Accepts all client and server certificates, from any issuers.
 */
public class SelfSignedSocketFactory extends SocketFactory {
    private SSLSocketFactory sf;

    private SelfSignedSocketFactory() throws Exception {
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(null, new TrustManager[]{new NoopTrustManager()}, null);
        sf = ctx.getSocketFactory();
    }

    /**
     * Part of SocketFactory contract, used by javax.net internals to create new instance.
     */
    public static SocketFactory getDefault() {
        try {
            return new SelfSignedSocketFactory();
        } catch (Exception e) {
            throw new GriffinException.ServiceException("Failed to create socket factory", e);
        }
    }

    /**
     * Insecure trust manager accepting any client and server certificates.
     */
    public static class NoopTrustManager implements X509TrustManager {
        public void checkClientTrusted(X509Certificate[] xcs, String string) throws CertificateException {
        }

        public void checkServerTrusted(X509Certificate[] xcs, String string) throws CertificateException {
        }

        public X509Certificate[] getAcceptedIssuers() {
            return new java.security.cert.X509Certificate[0];
        }
    }

    @Override
    public Socket createSocket() throws IOException {
        return sf.createSocket();
    }

    @Override
    public Socket createSocket(String s, int i) throws IOException, UnknownHostException {
        return sf.createSocket(s, i);
    }

    @Override
    public Socket createSocket(String s, int i, InetAddress inetAddress, int i1) throws IOException, UnknownHostException {
        return sf.createSocket(s, i, inetAddress, i1);
    }

    @Override
    public Socket createSocket(InetAddress inetAddress, int i) throws IOException {
        return sf.createSocket(inetAddress, i);
    }

    @Override
    public Socket createSocket(InetAddress inetAddress, int i, InetAddress inetAddress1, int i1) throws IOException {
        return sf.createSocket(inetAddress, i, inetAddress1, i1);
    }
}
