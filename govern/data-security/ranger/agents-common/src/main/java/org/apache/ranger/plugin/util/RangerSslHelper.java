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

package org.apache.ranger.plugin.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.apache.hadoop.conf.Configuration;
import org.apache.ranger.authorization.hadoop.utils.RangerCredentialProvider;
import org.apache.ranger.authorization.utils.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RangerSslHelper {
	private static final Logger LOG = LoggerFactory.getLogger(RangerSslHelper.class);

	static final String RANGER_POLICYMGR_CLIENT_KEY_FILE                  = "xasecure.policymgr.clientssl.keystore";	
	static final String RANGER_POLICYMGR_CLIENT_KEY_FILE_TYPE             = "xasecure.policymgr.clientssl.keystore.type";
	static final String RANGER_POLICYMGR_CLIENT_KEY_FILE_CREDENTIAL       = "xasecure.policymgr.clientssl.keystore.credential.file";
	static final String RANGER_POLICYMGR_CLIENT_KEY_FILE_CREDENTIAL_ALIAS = "sslKeyStore";
	static final String RANGER_POLICYMGR_CLIENT_KEY_FILE_TYPE_DEFAULT     = "jks";	

	static final String RANGER_POLICYMGR_TRUSTSTORE_FILE                  = "xasecure.policymgr.clientssl.truststore";	
	static final String RANGER_POLICYMGR_TRUSTSTORE_FILE_TYPE             = "xasecure.policymgr.clientssl.truststore.type";	
	static final String RANGER_POLICYMGR_TRUSTSTORE_FILE_CREDENTIAL       = "xasecure.policymgr.clientssl.truststore.credential.file";
	static final String RANGER_POLICYMGR_TRUSTSTORE_FILE_CREDENTIAL_ALIAS = "sslTrustStore";
	static final String RANGER_POLICYMGR_TRUSTSTORE_FILE_TYPE_DEFAULT     = "jks";	

	static final String RANGER_SSL_KEYMANAGER_ALGO_TYPE                   = KeyManagerFactory.getDefaultAlgorithm();
	static final String RANGER_SSL_TRUSTMANAGER_ALGO_TYPE                 = TrustManagerFactory.getDefaultAlgorithm();
	static final String RANGER_SSL_CONTEXT_ALGO_TYPE                      = "TLSv1.2";

	private String mKeyStoreURL;
	private String mKeyStoreAlias;
	private String mKeyStoreFile;
	private String mKeyStoreType;
	private String mTrustStoreURL;
	private String mTrustStoreAlias;
	private String mTrustStoreFile;
	private String mTrustStoreType;

	final static HostnameVerifier _Hv = new HostnameVerifier() {
		
		@Override
		public boolean verify(String urlHostName, SSLSession session) {
			return session.getPeerHost().equals(urlHostName);
		}
	};
	
	final String mSslConfigFileName;

	public RangerSslHelper(String sslConfigFileName) {
		mSslConfigFileName = sslConfigFileName;

		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerSslHelper(" + mSslConfigFileName + ")");
		}

	}
	
	public SSLContext createContext() {
		readConfig();
		KeyManager[]   kmList     = getKeyManagers();
		TrustManager[] tmList     = getTrustManagers();
		SSLContext     sslContext = getSSLContext(kmList, tmList);
		return sslContext;
	}
	
	public HostnameVerifier getHostnameVerifier() {
		return _Hv;
	}
	
	void readConfig() {
		InputStream in =  null;

		try {
			Configuration conf = new Configuration();

			in = getFileInputStream(mSslConfigFileName);

			if (in != null) {
				conf.addResource(in);
			}

			mKeyStoreURL   = conf.get(RANGER_POLICYMGR_CLIENT_KEY_FILE_CREDENTIAL);
			mKeyStoreAlias = RANGER_POLICYMGR_CLIENT_KEY_FILE_CREDENTIAL_ALIAS;
			mKeyStoreType  = conf.get(RANGER_POLICYMGR_CLIENT_KEY_FILE_TYPE, RANGER_POLICYMGR_CLIENT_KEY_FILE_TYPE_DEFAULT);
			mKeyStoreFile  = conf.get(RANGER_POLICYMGR_CLIENT_KEY_FILE);

			mTrustStoreURL   = conf.get(RANGER_POLICYMGR_TRUSTSTORE_FILE_CREDENTIAL);
			mTrustStoreAlias = RANGER_POLICYMGR_TRUSTSTORE_FILE_CREDENTIAL_ALIAS;
			mTrustStoreType  = conf.get(RANGER_POLICYMGR_TRUSTSTORE_FILE_TYPE, RANGER_POLICYMGR_TRUSTSTORE_FILE_TYPE_DEFAULT);
			mTrustStoreFile  = conf.get(RANGER_POLICYMGR_TRUSTSTORE_FILE);

			if (LOG.isDebugEnabled()) {
				LOG.debug(toString());
			}
		}
		catch(IOException ioe) {
			LOG.error("Unable to load SSL Config FileName: [" + mSslConfigFileName + "]", ioe);
		}
		finally {
			close(in, mSslConfigFileName);
		}
	}
	
	private KeyManager[] getKeyManagers() {
		KeyManager[] kmList = null;

		String keyStoreFilepwd = getCredential(mKeyStoreURL, mKeyStoreAlias);

		if (!StringUtil.isEmpty(mKeyStoreFile) && !StringUtil.isEmpty(keyStoreFilepwd)) {
			InputStream in =  null;

			try {
				in = getFileInputStream(mKeyStoreFile);

				if (in != null) {
					KeyStore keyStore = KeyStore.getInstance(mKeyStoreType);

					keyStore.load(in, keyStoreFilepwd.toCharArray());

					KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(RANGER_SSL_KEYMANAGER_ALGO_TYPE);

					keyManagerFactory.init(keyStore, keyStoreFilepwd.toCharArray());

					kmList = keyManagerFactory.getKeyManagers();
				} else {
					LOG.error("Unable to obtain keystore from file [" + mKeyStoreFile + "]");
				}
			} catch (KeyStoreException e) {
				LOG.error("Unable to obtain from KeyStore", e);
			} catch (NoSuchAlgorithmException e) {
				LOG.error("SSL algorithm is available in the environment", e);
			} catch (CertificateException e) {
				LOG.error("Unable to obtain the requested certification ", e);
			} catch (FileNotFoundException e) {
				LOG.error("Unable to find the necessary SSL Keystore and TrustStore Files", e);
			} catch (IOException e) {
				LOG.error("Unable to read the necessary SSL Keystore and TrustStore Files", e);
			} catch (UnrecoverableKeyException e) {
				LOG.error("Unable to recover the key from keystore", e);
			} finally {
				close(in, mKeyStoreFile);
			}
		}

		return kmList;
	}

	private TrustManager[] getTrustManagers() {
		TrustManager[] tmList = null;

		String trustStoreFilepwd = getCredential(mTrustStoreURL, mTrustStoreAlias);

		if (!StringUtil.isEmpty(mTrustStoreFile) && !StringUtil.isEmpty(trustStoreFilepwd)) {
			InputStream in =  null;

			try {
				in = getFileInputStream(mTrustStoreFile);

				if (in != null) {
					KeyStore trustStore = KeyStore.getInstance(mTrustStoreType);

					trustStore.load(in, trustStoreFilepwd.toCharArray());

					TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(RANGER_SSL_TRUSTMANAGER_ALGO_TYPE);

					trustManagerFactory.init(trustStore);

					tmList = trustManagerFactory.getTrustManagers();
				} else {
					LOG.error("Unable to obtain keystore from file [" + mTrustStoreFile + "]");
				}
			} catch (KeyStoreException e) {
				LOG.error("Unable to obtain from KeyStore", e);
			} catch (NoSuchAlgorithmException e) {
				LOG.error("SSL algorithm is available in the environment", e);
			} catch (CertificateException e) {
				LOG.error("Unable to obtain the requested certification ", e);
			} catch (FileNotFoundException e) {
				LOG.error("Unable to find the necessary SSL Keystore and TrustStore Files", e);
			} catch (IOException e) {
				LOG.error("Unable to read the necessary SSL Keystore and TrustStore Files", e);
			} finally {
				close(in, mTrustStoreFile);
			}
		}
		
		return tmList;
	}
	
	private SSLContext getSSLContext(KeyManager[] kmList, TrustManager[] tmList) {
		try {
			if(tmList != null) {
				SSLContext sslContext = SSLContext.getInstance(RANGER_SSL_CONTEXT_ALGO_TYPE);
	
				sslContext.init(kmList, tmList, new SecureRandom());
				
				return sslContext;
			}
		} catch (NoSuchAlgorithmException e) {
			LOG.error("SSL algorithm is available in the environment", e);
		} catch (Exception e) {
			LOG.error("Unable to initialize the SSLContext", e);
		}
		
		return null;
	}

	private String getCredential(String url, String alias) {
		return RangerCredentialProvider.getInstance().getCredentialString(url, alias);
	}

	private InputStream getFileInputStream(String fileName)  throws IOException {
		InputStream in = null;

		if(! StringUtil.isEmpty(fileName)) {
			File f = new File(fileName);

			if (f.exists()) {
				in = new FileInputStream(f);
			}
			else {
				in = ClassLoader.getSystemResourceAsStream(fileName);
			}
		}

		return in;
	}

	private void close(InputStream str, String filename) {
		if (str != null) {
			try {
				str.close();
			} catch (IOException excp) {
				LOG.error("Error while closing file: [" + filename + "]", excp);
			}
		}
	}

	@Override
	public String toString() {
		return "keyStoreAlias=" + mKeyStoreAlias + ", "
				+ "keyStoreFile=" + mKeyStoreFile + ", "
				+ "keyStoreType="+ mKeyStoreType + ", "
				+ "keyStoreURL=" + mKeyStoreURL + ", "
				+ "trustStoreAlias=" + mTrustStoreAlias + ", "
				+ "trustStoreFile=" + mTrustStoreFile + ", "
				+ "trustStoreType=" + mTrustStoreType + ", "
				+ "trustStoreURL=" + mTrustStoreURL
				;
	}
}
