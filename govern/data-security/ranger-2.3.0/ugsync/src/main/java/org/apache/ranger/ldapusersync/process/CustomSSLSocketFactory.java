/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ranger.ldapusersync.process;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.apache.ranger.unixusersync.config.UserGroupSyncConfig;
import org.apache.ranger.unixusersync.process.PolicyMgrUserGroupBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomSSLSocketFactory extends SSLSocketFactory{
	private static final Logger LOG = LoggerFactory.getLogger(CustomSSLSocketFactory.class);
	private SSLSocketFactory sockFactory;
	private UserGroupSyncConfig config = UserGroupSyncConfig.getInstance();

    public CustomSSLSocketFactory() {
    	SSLContext sslContext = null;
    	String keyStoreFile =  config.getSSLKeyStorePath();
    	String keyStoreFilepwd = config.getSSLKeyStorePathPassword();
    	String trustStoreFile = config.getSSLTrustStorePath();
    	String trustStoreFilepwd = config.getSSLTrustStorePathPassword();
    	String keyStoreType = config.getSSLKeyStoreType();
    	String trustStoreType = config.getSSLTrustStoreType();
    	try {

			KeyManager[] kmList = null;
			TrustManager[] tmList = null;

			if (keyStoreFile != null && keyStoreFilepwd != null) {

				KeyStore keyStore = KeyStore.getInstance(keyStoreType);
				InputStream in = null;
				try {
					in = getFileInputStream(keyStoreFile);
					if (in == null) {
						LOG.error("Unable to obtain keystore from file [" + keyStoreFile + "]");
						return;
					}
					keyStore.load(in, keyStoreFilepwd.toCharArray());
					KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
					keyManagerFactory.init(keyStore, keyStoreFilepwd.toCharArray());
					kmList = keyManagerFactory.getKeyManagers();
				}
				finally {
					if (in != null) {
						in.close();
					}
				}
				
			}

			if (trustStoreFile != null && trustStoreFilepwd != null) {

				KeyStore trustStore = KeyStore.getInstance(trustStoreType);
				InputStream in = null;
				try {
					in = getFileInputStream(trustStoreFile);
					if (in == null) {
						LOG.error("Unable to obtain keystore from file [" + trustStoreFile + "]");
						return;
					}
					trustStore.load(in, trustStoreFilepwd.toCharArray());
					TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
					trustManagerFactory.init(trustStore);
					tmList = trustManagerFactory.getTrustManagers();
				}
				finally {
					if (in != null) {
						in.close();
					}
				}
			}

			sslContext = SSLContext.getInstance("TLSv1.2");

			sslContext.init(kmList, tmList, new SecureRandom());
			sockFactory = sslContext.getSocketFactory();
			}
			catch(Throwable t) {
				throw new RuntimeException("Unable to create SSLConext for communication to policy manager", t);
			}
    }

    public static SSLSocketFactory getDefault() {
        return new CustomSSLSocketFactory();
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return sockFactory.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return sockFactory.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket(Socket socket, String host, int port, boolean bln) throws IOException {
        return sockFactory.createSocket(socket, host, port, bln);
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
        return sockFactory.createSocket(host, port);
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException, UnknownHostException {
        return sockFactory.createSocket(host, port, localHost, localPort);
    }

    @Override
    public Socket createSocket(InetAddress localHost, int localPort) throws IOException {
        return sockFactory.createSocket(localHost, localPort);
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localHost, int localPort) throws IOException {
        return sockFactory.createSocket(address, port, localHost, localPort);
    }

    private InputStream getFileInputStream(String path) throws FileNotFoundException {

		InputStream ret = null;

		File f = new File(path);

		if (f.exists()) {
			ret = new FileInputStream(f);
		} else {
			ret = PolicyMgrUserGroupBuilder.class.getResourceAsStream(path);
			
			if (ret == null) {
				if (! path.startsWith("/")) {
					ret = getClass().getResourceAsStream("/" + path);
				}
			}
			
			if (ret == null) {
				ret = ClassLoader.getSystemClassLoader().getResourceAsStream(path);
				if (ret == null) {
					if (! path.startsWith("/")) {
						ret = ClassLoader.getSystemResourceAsStream("/" + path);
					}
				}
			}
		}

		return ret;
	}
}
