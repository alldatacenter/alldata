package com.aliyun.oss;

import com.aliyun.oss.common.auth.CredentialsProvider;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.crypto.CryptoConfiguration;
import com.aliyun.oss.crypto.EncryptionMaterials;

public class OSSEncryptionClientBuilder {
    /**
     * Constructs a instance of {@link OSSEncryptionClient} using the current builder configuration.
     * 
     * @param endpoint
     *            OSS services Endpoint.
     * @param accessKeyId
     *            Access Key ID.
     * @param accessKeySecret
     *            Secret Access Key.
     * @param encryptionMaterials
     *            Encryption materials contains the consumer managed key, 
     *            it can be a asymmetric key or a symmetric key.
     * @return an encrytion client. it can put/get/upload object secured.
     */
    public OSSEncryptionClient build(String endpoint, String accessKeyId, String accessKeySecret, EncryptionMaterials encryptionMaterials) {
        return new OSSEncryptionClient(endpoint, new DefaultCredentialProvider(accessKeyId, accessKeySecret), getClientConfiguration(), encryptionMaterials, getCryptoConfiguration());
    }

    /**
     * Constructs a instance of {@link OSSEncryptionClient} using the current builder configuration.
     * 
     * @param endpoint
     *            OSS services Endpoint.
     * @param accessKeyId
     *            Access Id from STS.
     * @param accessKeySecret
     *            Access Key from STS
     * @param securityToken
     *            Security Token from STS. 
     * @param encryptionMaterials
     *            Encryption materials contains the consumer managed key, 
     *            it can be a asymmetric key or a symmetric key.
     * @return an encrytion client. it can put/get/upload object secured.
     */
    public OSSEncryptionClient build(String endpoint, String accessKeyId, String accessKeySecret, String securityToken, EncryptionMaterials encryptionMaterials) {
        return new OSSEncryptionClient(endpoint, new DefaultCredentialProvider(accessKeyId, accessKeySecret, securityToken), getClientConfiguration(), encryptionMaterials, getCryptoConfiguration());
    }

    /**
     * Constructs a instance of {@link OSSEncryptionClient} using the current builder configuration.
     * 
     * @param endpoint
     *            OSS services Endpoint.
     * @param credsProvider
     *            Credentials provider which has access key Id and access Key
     *            secret.
     * @param encryptionMaterials
     *            Encryption materials contains the consumer managed key, 
     *            it can be a asymmetric key or a symmetric key.
     * @return an encrytion client. it can put/get/upload object secured.
     */
    public OSSEncryptionClient build(String endpoint, CredentialsProvider credsProvider, EncryptionMaterials encryptionMaterials) {
        return new OSSEncryptionClient(endpoint, credsProvider, getClientConfiguration(), encryptionMaterials, getCryptoConfiguration());
    }

    /**
     * Constructs a instance of {@link OSSEncryptionClient} using the current builder configuration.
     * 
     * @param endpoint
     *            OSS services Endpoint.
     * @param credsProvider
     *            Credentials provider which has access key Id and access Key
     *            secret.
     * @param encryptionMaterials
     *            Encryption materials contains the consumer managed key, 
     *            it can be a asymmetric key or a symmetric key.
     * @param clientConfig
     *            client configuration.
     * @return an encrytion client. it can put/get/upload object secured.
     */
    public OSSEncryptionClient build(String endpoint, CredentialsProvider credsProvider, EncryptionMaterials encryptionMaterials, ClientBuilderConfiguration clientConfig) {
        return new OSSEncryptionClient(endpoint, credsProvider, getClientConfiguration(clientConfig), encryptionMaterials, getCryptoConfiguration());
    }

    /**
     * Construct a synchronous implementation of OSSEncryptionClient using the current builder configuration.
     * 
     * @param endpoint
     *            OSS services Endpoint.
     * @param credsProvider
     *            Credentials provider which has access key Id and access Key
     *            secret.
     * @param encryptionMaterials
     *            Encryption materials contains the consumer managed key, 
     *            it can be a asymmetric key or a symmetric key.
     * @param cryptoConfig
     *            data crypto configuration.
     * @return an encrytion client. it can put/get/upload object secured.
     */
    public OSSEncryptionClient build(String endpoint, CredentialsProvider credsProvider, EncryptionMaterials encryptionMaterials, CryptoConfiguration cryptoConfig) {
        return new OSSEncryptionClient(endpoint, credsProvider, getClientConfiguration(), encryptionMaterials, getCryptoConfiguration(cryptoConfig));
    }

    /**
     * Construct a synchronous implementation of OSSEncryptionClient using the current builder configuration.
     * 
     * @param endpoint
     *            OSS services Endpoint.
     * @param credsProvider
     *            Credentials provider which has access key Id and access Key
     *            secret.
     * @param encryptionMaterials
     *            Encryption materials contains the consumer managed key, 
     *            it can be a asymmetric key or a symmetric key.
     * @param clientConfig
     *            client configuration.
     * @param cryptoConfig
     *            data crypto configuration.
     * @return an encrytion client. it can put/get/upload object secured.
     */
    public OSSEncryptionClient build(String endpoint, CredentialsProvider credsProvider, EncryptionMaterials encryptionMaterials, ClientBuilderConfiguration clientConfig, CryptoConfiguration cryptoConfig) {
        return new OSSEncryptionClient(endpoint, credsProvider, getClientConfiguration(clientConfig), encryptionMaterials, getCryptoConfiguration(cryptoConfig));
    }

    private static ClientBuilderConfiguration getClientConfiguration() {
        return new ClientBuilderConfiguration();
    }

    private static ClientBuilderConfiguration getClientConfiguration(ClientBuilderConfiguration config) {
        if (config == null) {
            config = new ClientBuilderConfiguration();
        }
        return config;
    }

    private static CryptoConfiguration getCryptoConfiguration() {
        return CryptoConfiguration.DEFAULT.clone();
    }

    private static CryptoConfiguration getCryptoConfiguration(CryptoConfiguration cryptoConfig) {
        if (cryptoConfig == null) {
            cryptoConfig = CryptoConfiguration.DEFAULT.clone();
        }
        return cryptoConfig;
    }
}