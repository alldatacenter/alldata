/*
 * Copyright (c) 2017. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amazonaws.services.s3;

import com.amazonaws.client.AwsSyncClientParams;
import com.amazonaws.regions.Region;
import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.s3.model.CryptoConfigurationV2;
import com.amazonaws.services.s3.model.EncryptionMaterialsProvider;

public final class AmazonS3EncryptionClientV2Builder extends
        AmazonS3Builder<AmazonS3EncryptionClientV2Builder, AmazonS3EncryptionV2> {
    private EncryptionMaterialsProvider encryptionMaterialsProvider;
    private CryptoConfigurationV2 cryptoConfig;
    private AWSKMS kmsClient;

    /**
     * @return Create new instance of builder with all defaults set.
     */
    public static AmazonS3EncryptionClientV2Builder standard() {
        return new AmazonS3EncryptionClientV2Builder();
    }

    /**
     * Sets the encryption materials to be used to encrypt and decrypt data
     * <p>
     * <b>Note: </b> If a non-KMS encryption materials provider is used, an internal KMS client will still be created by default.
     * See {@link #setKmsClient(AWSKMS)} for more information.
     *
     * @param encryptionMaterialsProvider a provider for the encryption materials
     */
    public void setEncryptionMaterialsProvider(EncryptionMaterialsProvider encryptionMaterialsProvider) {
        this.encryptionMaterialsProvider = encryptionMaterialsProvider;
    }

    /**
     * Sets the encryption materials to be used to encrypt and decrypt data.
     * <p>
     * <b>Note: </b> If a non-KMS encryption materials provider is used, an internal KMS client will still be created by default.
     * See {@link #withKmsClient(AWSKMS)} for more information.
     *
     * @param encryptionMaterialsProvider A provider for the encryption materials to be used to encrypt and decrypt data.
     * @return this object for method chaining
     */
    public AmazonS3EncryptionClientV2Builder withEncryptionMaterialsProvider(
            EncryptionMaterialsProvider encryptionMaterialsProvider) {
        setEncryptionMaterialsProvider(encryptionMaterialsProvider);
        return this;
    }

    /**
     * Sets the crypto configuration whose parameters will be used to encrypt and decrypt data.
     * @param cryptoConfig crypto configuration
     */
    public void setCryptoConfiguration(CryptoConfigurationV2 cryptoConfig) {
        this.cryptoConfig = cryptoConfig;
    }

    /**
     * Sets the crypto configuration whose parameters will be used to encrypt and decrypt data.
     * @param cryptoConfig crypto configuration
     * @return this object for method chaining
     */
    public AmazonS3EncryptionClientV2Builder withCryptoConfiguration(CryptoConfigurationV2 cryptoConfig) {
        setCryptoConfiguration(cryptoConfig);
        return this;
    }

    /**
     * Sets the KMS implementation to be used throughout the crypto process.
     * <p>
     * If a KMS client isn't specified here, the encryption client creates a default internal KMS client.
     * <br>
     * <b>Note:</b> An internal KMS client is created even if the user supply their own encryption materials. If the KMS
     * client fails to find a region when it's instantiated, supply a client through this method or set the
     * KMS region through {@link CryptoConfigurationV2#withAwsKmsRegion(Region)}.
     *
     * @param kmsClient an {@link AWSKMS} implementation (e.g. {@link com.amazonaws.services.kms.AWSKMSClient})
     */
    public void setKmsClient(AWSKMS kmsClient) {
        this.kmsClient = kmsClient;
    }

    /**
     * Sets the KMS implementation to be used throughout the crypto process
     * <p>
     * If a KMS client isn't specified here, the encryption client creates a default internal KMS client.
     * <br>
     * <b>Note:</b> An internal KMS client is created even if the user supply their own encryption materials. If the KMS
     * client fails to find a region when it's instantiated, supply a client through this method or set the
     * KMS region through {@link CryptoConfigurationV2#withAwsKmsRegion(Region)}.
     *
     * @param kmsClient an {@link AWSKMS} implementation (e.g. {@link com.amazonaws.services.kms.AWSKMSClient})
     * @return this object for method chaining
     */
    public AmazonS3EncryptionClientV2Builder withKmsClient(AWSKMS kmsClient) {
        setKmsClient(kmsClient);
        return this;
    }

    /**
     * Construct a synchronous implementation of AmazonS3Encryption using the current builder configuration.
     *
     * @return Fully configured implementation of AmazonS3EncryptionV2.
     */
    @Override
    protected AmazonS3EncryptionV2 build(AwsSyncClientParams clientParams) {
        return new AmazonS3EncryptionClientV2(
                new AmazonS3EncryptionClientV2ParamsWrapper(
                        clientParams,
                        resolveS3ClientOptions(),
                        encryptionMaterialsProvider,
                        cryptoConfig != null ? cryptoConfig : new CryptoConfigurationV2(),
                        kmsClient));
    }
}
