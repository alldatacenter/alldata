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

package org.apache.hadoop.crypto.key;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.microsoft.aad.adal4j.AsymmetricKeyCredential;
import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.aad.adal4j.ClientCredential;
import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.keyvault.authentication.KeyVaultCredentials;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.pkcs.PKCSException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AzureKeyVaultClientAuthenticator extends KeyVaultCredentials {
    private static final Logger logger = LoggerFactory.getLogger(AzureKeyVaultClientAuthenticator.class);

    private final String authClientID;
    private final String authClientSecret;

    public AzureKeyVaultClientAuthenticator(String clientID, String clientSecret) {
        if (logger.isDebugEnabled()) {
            logger.debug("==> AzureKeyVaultClientAuthenticator({})", clientID);
        }

        this.authClientID     = clientID;
        this.authClientSecret = clientSecret;

        if (logger.isDebugEnabled()) {
            logger.debug("<== AzureKeyVaultClientAuthenticator({})", clientID);
        }
    }

    public AzureKeyVaultClientAuthenticator(String clientID) {
        if (logger.isDebugEnabled()) {
            logger.debug("==> AzureKeyVaultClientAuthenticator({})", clientID);
        }

        this.authClientID     = clientID;
        this.authClientSecret = null;

        if (logger.isDebugEnabled()) {
            logger.debug("<== AzureKeyVaultClientAuthenticator({})", clientID);
        }
    }

    /**
     * It does the authentication. This method will be called by the super
     * class.
     */
    @Override
    public String doAuthenticate(String authorization, String resource, String scope) {
        if (logger.isDebugEnabled()) {
            logger.debug("==> doAuthenticate({}, {}, {})", authorization, resource, scope);
        }

        AuthenticationResult token = getAccessTokenFromClientCredentials(authorization, resource, authClientID, authClientSecret);
        String               ret   = token.getAccessToken();

        if (logger.isDebugEnabled()) {
            logger.debug("<== doAuthenticate({}, {}, {}): ret={}", authorization, resource, scope, ret);
        }

        return ret;
    }

    /**
     * Do certificate based authentication using pfx file
     */
    public KeyVaultClient getAuthentication(String path, String certPassword) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("==> getAuthentication({})", path);
        }

        KeyVaultClient ret     = null;
        KeyCert        keyCert = null;

        if (path.endsWith(".pfx")) {
            try {
                keyCert = readPfx(path, certPassword);
            } catch (Exception ex) {
                throw new Exception("Error while parsing pfx certificate. Error : " + ex);
            }
        } else if(path.endsWith(".pem")) {
            try {
                keyCert = readPem(path, certPassword);
            } catch (Exception ex) {
                throw new Exception("Error while parsing pem certificate. Error : " + ex);
            }
        }

        final KeyCert certificateKey = keyCert;

        if (certificateKey != null) {
            PrivateKey privateKey = certificateKey.getKey();

            // Do certificate based authentication
            ret = new KeyVaultClient(
                    new KeyVaultCredentials() {
                        @Override
                        public String doAuthenticate(String authorization, String resource, String scope) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("==> getAuthentication().doAuthenticate({}, {}, {})", authorization, resource, scope);
                            }

                            ExecutorService service = null;

                            try {
                                service = Executors.newFixedThreadPool(1, new ThreadFactoryBuilder().setDaemon(true)
                                                                                                    .setNameFormat("kms-azure-akc_acquireToken_thread")
                                                                                                    .build());
                                AuthenticationContext   context                 = new AuthenticationContext(authorization, false, service);
                                AsymmetricKeyCredential asymmetricKeyCredential = AsymmetricKeyCredential.create(authClientID, privateKey, certificateKey.getCertificate());
                                AuthenticationResult    result                  = context.acquireToken(resource, asymmetricKeyCredential, null).get();
                                String                  ret                     = result.getAccessToken();

                                if (logger.isDebugEnabled()) {
                                    logger.debug("<== getAuthentication().doAuthenticate({}, {}, {})", authorization, resource, scope);
                                }

                                return ret;
                            } catch (Exception e) {
                                throw new RuntimeException("Error while getting authenticated access token from azure key vault with certificate : " + e);
                            } finally {
                                if (service != null) {
                                    service.shutdown();
                                }
                            }
                        }
                    });
        }

        if (logger.isDebugEnabled()) {
            logger.debug("<== getAuthentication({}): ret={}", path, ret);
        }

        return ret;
    }

        private static AuthenticationResult getAccessTokenFromClientCredentials(String authorization, String resource, String clientId, String clientKey) {
        if (logger.isDebugEnabled()) {
            logger.debug("==> getAccessTokenFromClientCredentials({}, {}, {})", authorization, resource, clientId);
        }

        AuthenticationResult  result;
        ExecutorService       service = null;

        try {
            service = Executors.newFixedThreadPool(1, new ThreadFactoryBuilder().setDaemon(true)
                                                                                .setNameFormat("kms-azure-cc_acquireToken-thread")
                                                                                .build());

            AuthenticationContext        context     = new AuthenticationContext(authorization, false, service);
            ClientCredential             credentials = new ClientCredential(clientId, clientKey);
            Future<AuthenticationResult> future      = context.acquireToken(resource, credentials, null);

            result = future.get();
        } catch (Exception e) {
            throw new RuntimeException(" Error while getting Access token for client id: " + clientId + " and client secret. Error : " + e);
        } finally {
            if (service != null) {
                service.shutdown();
            }
        }

        if (result == null) {
            throw new RuntimeException("authentication result was null");
        }

        if (logger.isDebugEnabled()) {
            logger.debug("<== getAccessTokenFromClientCredentials({}, {}, {})", authorization, resource, clientId);
        }

        return result;
    }

    private KeyCert readPem(String path, String password) throws IOException, CertificateException, OperatorCreationException, PKCSException {
        if (logger.isDebugEnabled()) {
            logger.debug("==> readPem({})", path);
        }

        Security.addProvider(new BouncyCastleProvider());

        PEMParser       pemParser  = new PEMParser(new FileReader(path));
        PrivateKey      privateKey = null;
        X509Certificate cert       = null;
        Object          object     = pemParser.readObject();

        while (object != null) {
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");

            if (object instanceof X509CertificateHolder) {
                cert = new JcaX509CertificateConverter().getCertificate((X509CertificateHolder) object);
            } else if (object instanceof PKCS8EncryptedPrivateKeyInfo) {
                PKCS8EncryptedPrivateKeyInfo pinfo    = (PKCS8EncryptedPrivateKeyInfo) object;
                InputDecryptorProvider       provider = new JceOpenSSLPKCS8DecryptorProviderBuilder().build(password.toCharArray());
                PrivateKeyInfo               info     = pinfo.decryptPrivateKeyInfo(provider);

                privateKey = converter.getPrivateKey(info);
            } else if (object instanceof PrivateKeyInfo) {
                privateKey = converter.getPrivateKey((PrivateKeyInfo) object);
            }

            object = pemParser.readObject();
        }

        KeyCert keycert = new KeyCert(cert, privateKey);

        pemParser.close();

        if (logger.isDebugEnabled()) {
            logger.debug("<== readPem({})", path);
        }

        return keycert;
    }

    private KeyCert readPfx(String path, String password) throws NoSuchProviderException, KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException {
        logger.debug("==> readPfx({})", path);

        try (FileInputStream stream = new FileInputStream(path)) {
            KeyCert  keyCert               = null;
            boolean  isAliasWithPrivateKey = false;
            KeyStore store                 = KeyStore.getInstance("pkcs12", "SunJSSE");

            store.load(stream, password.toCharArray());

            // Iterate over all aliases to find the private key
            Enumeration<String> aliases = store.aliases();
            String              alias   = "";

            while (aliases.hasMoreElements()) {
                alias = aliases.nextElement();
                // Break if alias refers to a private key because we want to use that certificate
                isAliasWithPrivateKey = store.isKeyEntry(alias);

                if (isAliasWithPrivateKey) {
                    break;
                }
            }

            if (isAliasWithPrivateKey) {
                // Retrieves the certificate from the Java keystore
                X509Certificate certificate = (X509Certificate) store.getCertificate(alias);
                PrivateKey      key         = (PrivateKey) store.getKey(alias, password.toCharArray());

                keyCert = new KeyCert(certificate, key);
            }

            logger.debug("<== readPfx({})", path);

            return keyCert;
        }
    }

    private static class KeyCert {
        private final X509Certificate certificate;
        private final PrivateKey      key;

        public KeyCert(X509Certificate certificate, PrivateKey key) {
            this.certificate = certificate;
            this.key         = key;
        }

        public X509Certificate getCertificate() {
            return certificate;
        }

        public PrivateKey getKey() {
            return key;
        }
    }
}

