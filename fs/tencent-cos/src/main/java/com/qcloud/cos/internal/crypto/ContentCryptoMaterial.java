/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 
 * According to cos feature, we modify some class，comment, field name, etc.
 */


package com.qcloud.cos.internal.crypto;

import static com.qcloud.cos.internal.crypto.KMSSecuredCEK.isKMSKeyWrapped;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.security.Key;
import java.security.Provider;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qcloud.cos.Headers;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.internal.CosServiceRequest;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.MaterialsDescriptionProvider;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.utils.Base64;
import com.qcloud.cos.utils.Jackson;
import com.qcloud.cos.utils.StringUtils;
import com.tencentcloudapi.kms.v20190118.models.DecryptRequest;
import com.tencentcloudapi.kms.v20190118.models.DecryptResponse;
import com.tencentcloudapi.kms.v20190118.models.EncryptRequest;
import com.tencentcloudapi.kms.v20190118.models.EncryptResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cryptographic material used for client-side content encrypt/decryption in COS. This includes the
 * randomly generated one-time secured CEK (content-encryption-key) and the respective key wrapping
 * algorithm, if any, and the cryptographic scheme in use.
 */
final class ContentCryptoMaterial {
    // null if cek is not secured via key wrapping
    private final String keyWrappingAlgorithm;
    private final CipherLite cipherLite;

    private final Map<String, String> kekMaterialsDescription;
    private final byte[] encryptedCEK;

    private final byte[] securedIV;

    private static final Logger log = LoggerFactory.getLogger(ContentCryptoMaterial.class);

    ContentCryptoMaterial(Map<String, String> kekMaterialsDescription, byte[] encryptedCEK,
            String keyWrappingAlgorithm, CipherLite cipherLite, byte[] securedIV) {
        this.cipherLite = cipherLite;
        this.keyWrappingAlgorithm = keyWrappingAlgorithm;
        this.encryptedCEK = encryptedCEK.clone();
        this.kekMaterialsDescription = kekMaterialsDescription;
        this.securedIV = securedIV;
    }

    /**
     * Returns the key wrapping algorithm, or null if the content key is not secured via a key
     * wrapping algorithm.
     * <p>
     * Note if the returned value is {@value KMSSecuredCEK#KEY_PROTECTION_MECHANISM}, it means the
     * key is protected via KMS rather than a direct key-wrapping algorithm.
     */
    String getKeyWrappingAlgorithm() {
        return keyWrappingAlgorithm;
    }

    /**
     * Returns true if KMS is used for the content key protection; false otherwise.
     */
    private boolean usesKMSKey() {
        return isKMSKeyWrapped(keyWrappingAlgorithm);
    }


    /**
     * Returns the content crypto scheme.
     */
    ContentCryptoScheme getContentCryptoScheme() {
        return cipherLite.getContentCryptoScheme();
    }

    /**
     * Returns the given metadata updated with this content crypto material.
     */
    ObjectMetadata toObjectMetadata(ObjectMetadata metadata, CryptoMode mode) {
        return toObjectMetadata(metadata);
    }

    /**
     * Returns the metadata in the latest format.
     */
    private ObjectMetadata toObjectMetadata(ObjectMetadata metadata) {
        // If we generated a symmetric key to encrypt the data, store it in the
        // object metadata.
        byte[] encryptedCEK = getEncryptedCEK();
        metadata.addUserMetadata(Headers.ENCRYPTION_KEY, Base64.encodeAsString(encryptedCEK));
        // Put the cipher initialization vector (IV) into the object metadata
        metadata.addUserMetadata(Headers.ENCRYPTION_START, Base64.encodeAsString(securedIV));
        // Put the materials description into the object metadata as JSON
        String matdesc = kekMaterialDescAsJson();
        if (matdesc != null) {
            metadata.addUserMetadata(Headers.ENCRYPTION_MATDESC, kekMaterialDescAsJson());
        }
        // The CRYPTO_CEK_ALGORITHM, CRYPTO_TAG_LENGTH and
        // CRYPTO_KEYWRAP_ALGORITHM were not available in the Encryption Only
        // (EO) implementation
        ContentCryptoScheme scheme = getContentCryptoScheme();
        metadata.addUserMetadata(Headers.ENCRYPTION_CEK_ALG, scheme.getCipherAlgorithm());
        int tagLen = scheme.getTagLengthInBits();
        if (tagLen > 0)
            metadata.addUserMetadata(Headers.CRYPTO_TAG_LENGTH, String.valueOf(tagLen));
        String keyWrapAlgo = getKeyWrappingAlgorithm();
        if (keyWrapAlgo != null)
            metadata.addUserMetadata(Headers.ENCRYPTION_WRAP_ALG, keyWrapAlgo);
        return metadata;
    }

    /**
     * Returns the json string in the latest format.
     */
    String toJsonString() {
        Map<String, String> map = new HashMap<String, String>();
        byte[] encryptedCEK = getEncryptedCEK();
        map.put(Headers.CRYPTO_KEY_V2, Base64.encodeAsString(encryptedCEK));
        byte[] iv = cipherLite.getIV();
        map.put(Headers.CRYPTO_IV, Base64.encodeAsString(iv));
        map.put(Headers.MATERIALS_DESCRIPTION, kekMaterialDescAsJson());
        ContentCryptoScheme scheme = getContentCryptoScheme();
        map.put(Headers.CRYPTO_CEK_ALGORITHM, scheme.getCipherAlgorithm());
        int tagLen = scheme.getTagLengthInBits();
        if (tagLen > 0)
            map.put(Headers.CRYPTO_TAG_LENGTH, String.valueOf(tagLen));
        String keyWrapAlgo = getKeyWrappingAlgorithm();
        if (keyWrapAlgo != null)
            map.put(Headers.CRYPTO_KEYWRAP_ALGORITHM, keyWrapAlgo);
        return Jackson.toJsonString(map);
    }

    /**
     * Returns the key-encrypting-key material description as a non-null json string;
     */
    private String kekMaterialDescAsJson() {
        Map<String, String> kekMaterialDesc = getKEKMaterialsDescription();
        if (kekMaterialDesc == null || kekMaterialDesc.isEmpty())
            return null;
        return Jackson.toJsonString(kekMaterialDesc);
    }

    /**
     * Returns the corresponding kek material description from the given json; or null if the input
     * is null.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, String> matdescFromJson(String json) {
        Map<String, String> map = Jackson.fromJsonString(json, Map.class);
        return map == null ? null : Collections.unmodifiableMap(map);
    }

    /**
     * Returns the content encrypting key unwrapped or decrypted. Note if KMS is used for key
     * protection, a remote call will be made to KMS to decrypt the ciphertext blob.
     *
     * @param cekSecured the content encrypting key in wrapped or encrypted form; must not be null
     * @param keyWrapAlgo key wrapping algorithm; or null if direct encryption instead of key
     *        wrapping is used
     * @param materials the client key encrypting key material for the content encrypting key
     * @param securityProvider security provider or null if the default security provider of the JCE
     *        is used
     */
    private static SecretKey cek(byte[] cekSecured, String keyWrapAlgo,
            EncryptionMaterials materials, Provider securityProvider,
            ContentCryptoScheme contentCryptoScheme, QCLOUDKMS kms) {
        if (isKMSKeyWrapped(keyWrapAlgo))
            return cekByKMS(cekSecured, keyWrapAlgo, materials, contentCryptoScheme, kms);
        Key kek;
        if (materials.getKeyPair() != null) {
            // Do envelope decryption with private key from key pair
            kek = materials.getKeyPair().getPrivate();
            if (kek == null) {
                throw new CosClientException("Key encrypting key not available");
            }
        } else {
            // Do envelope decryption with symmetric key
            kek = materials.getSymmetricKey();
            if (kek == null) {
                throw new CosClientException("Key encrypting key not available");
            }
        }

        try {
            if (keyWrapAlgo != null) {
                // Key wrapping specified
                Cipher cipher = securityProvider == null ? Cipher.getInstance(keyWrapAlgo)
                        : Cipher.getInstance(keyWrapAlgo, securityProvider);
                cipher.init(Cipher.UNWRAP_MODE, kek);
                return (SecretKey) cipher.unwrap(cekSecured, keyWrapAlgo, Cipher.SECRET_KEY);
            }
            // fall back to the Encryption Only (EO) key decrypting method
            Cipher cipher;
            if (securityProvider != null) {
                cipher = Cipher.getInstance(kek.getAlgorithm(), securityProvider);
            } else {
                cipher = Cipher.getInstance(kek.getAlgorithm());
            }
            cipher.init(Cipher.DECRYPT_MODE, kek);
            byte[] decryptedSymmetricKeyBytes = cipher.doFinal(cekSecured);
            return new SecretKeySpec(decryptedSymmetricKeyBytes,
                    JceEncryptionConstants.SYMMETRIC_KEY_ALGORITHM);
        } catch (Exception e) {
            throw new CosClientException("Unable to decrypt symmetric key from object metadata", e);
        }
    }

    /**
     * Decrypts the secured CEK via KMS; involves network calls.
     *
     * @return the CEK (in plaintext).
     */
    private static SecretKey cekByKMS(byte[] cekSecured, String keyWrapAlgo,
            EncryptionMaterials materials, ContentCryptoScheme contentCryptoScheme, QCLOUDKMS kms) {
        DecryptRequest decryptReq = new DecryptRequest();
        Map<String, String> materialDesc = materials.getMaterialsDescription();
        try {
            ObjectMapper mapper = new ObjectMapper();
            decryptReq.setEncryptionContext(mapper.writeValueAsString(materialDesc));
        } catch (JsonProcessingException e) {
            throw new CosClientException("decrypt request set encryption context got json processing exception", e);
        }
        decryptReq.setCiphertextBlob(new String(cekSecured));

        DecryptResponse decryptRes = kms.decrypt(decryptReq);
        byte[] key = Base64.decode(decryptRes.getPlaintext());

        return new SecretKeySpec(key, contentCryptoScheme.getKeyGeneratorAlgorithm());
    }

    /**
     * @return a non-null content crypto material.
     */
    static ContentCryptoMaterial fromObjectMetadata(ObjectMetadata metadata,
            EncryptionMaterialsAccessor kekMaterialAccessor, Provider securityProvider,
            boolean keyWrapExpected, QCLOUDKMS kms) {
        return fromObjectMetadata0(metadata, kekMaterialAccessor, securityProvider, null,
                keyWrapExpected, kms);
    }

    /**
     * Factory method to return the content crypto material from the COS object meta data, using the
     * specified key encrypting key material accessor and an optional security provider.
     *
     * @return a non-null content crypto material.
     */
    static ContentCryptoMaterial fromObjectMetadata(ObjectMetadata metadata,
            EncryptionMaterialsAccessor kekMaterialAccessor, Provider securityProvider,
            long[] range, boolean keyWrapExpected, QCLOUDKMS kms) {
        return fromObjectMetadata0(metadata, kekMaterialAccessor, securityProvider, range,
                keyWrapExpected, kms);
    }

    /**
     * @return a non-null content crypto material.
     */
    private static ContentCryptoMaterial fromObjectMetadata0(ObjectMetadata metadata,
            EncryptionMaterialsAccessor kekMaterialAccessor, Provider securityProvider,
            long[] range, boolean keyWrapExpected, QCLOUDKMS kms) {
        // CEK and IV
        Map<String, String> userMeta = metadata.getUserMetadata();

        // new version has different header
        if (userMeta.get(Headers.ENCRYPTION_KEY) != null) {
            return fromObjectMetadata1(userMeta, kekMaterialAccessor, securityProvider,
                    range, keyWrapExpected, kms);
        }

        String b64key = userMeta.get(Headers.CRYPTO_KEY_V2);
        if (b64key == null) {
            b64key = userMeta.get(Headers.CRYPTO_KEY);
            if (b64key == null)
                throw new CosClientException("Content encrypting key not found.");
        }
        byte[] cekWrapped = Base64.decode(b64key);
        //byte[] iv = userMeta.get(Headers.CRYPTO_IV).getBytes();
        byte[] iv = Base64.decode(userMeta.get(Headers.CRYPTO_IV));
        if (cekWrapped == null || iv == null) {
            throw new CosClientException("Content encrypting key or IV not found.");
        }
        // Material description
        String matdescStr = userMeta.get(Headers.MATERIALS_DESCRIPTION);
        final String keyWrapAlgo = userMeta.get(Headers.CRYPTO_KEYWRAP_ALGORITHM);
        final boolean isKMS = isKMSKeyWrapped(keyWrapAlgo);
        final Map<String, String> core = matdescFromJson(matdescStr);
        final EncryptionMaterials materials;
        if (isKMS) {
            materials = new KMSEncryptionMaterials(
                    core.get(KMSEncryptionMaterials.CUSTOMER_MASTER_KEY_ID));
            materials.addDescriptions(core);
        } else {
            materials = kekMaterialAccessor == null ? null
                    : kekMaterialAccessor.getEncryptionMaterials(core);
            if (materials == null) {
                throw new CosClientException("Unable to retrieve the client encryption materials");
            }
        }
        // CEK algorithm
        String cekAlgo = userMeta.get(Headers.CRYPTO_CEK_ALGORITHM);
        boolean isRangeGet = range != null;
        // The content crypto scheme may vary depending on whether
        // it is a range get operation
        ContentCryptoScheme contentCryptoScheme =
                ContentCryptoScheme.fromCEKAlgo(cekAlgo, isRangeGet);
        if (isRangeGet) {
            // Adjust the IV as needed
            iv = contentCryptoScheme.adjustIV(iv, range[0]);
        } else {
            // Validate the tag length supported
            int tagLenExpected = contentCryptoScheme.getTagLengthInBits();
            if (tagLenExpected > 0) {
                String s = userMeta.get(Headers.CRYPTO_TAG_LENGTH);
                int tagLenActual = Integer.parseInt(s);
                if (tagLenExpected != tagLenActual) {
                    throw new CosClientException("Unsupported tag length: " + tagLenActual
                            + ", expected: " + tagLenExpected);
                }
            }
        }
        // Unwrap or decrypt the CEK
        if (keyWrapExpected && keyWrapAlgo == null)
            throw newKeyWrapException();
        SecretKey cek =
                cek(cekWrapped, keyWrapAlgo, materials, securityProvider, contentCryptoScheme, kms);
        return new ContentCryptoMaterial(core, cekWrapped, keyWrapAlgo, contentCryptoScheme
                .createCipherLite(cek, iv, Cipher.DECRYPT_MODE, securityProvider), null);
    }

    private static ContentCryptoMaterial fromObjectMetadata1(Map<String, String> userMeta,
            EncryptionMaterialsAccessor kekMaterialAccessor, Provider securityProvider,
            long[] range, boolean keyWrapExpected, QCLOUDKMS kms) {
        String b64SecuredKey = userMeta.get(Headers.ENCRYPTION_KEY);
        if (b64SecuredKey == null) {
            throw new CosClientException("Content encrypting key not found.");
        }

        byte[] securedKey = Base64.decode(b64SecuredKey);

        String b64SecuredIV = userMeta.get(Headers.ENCRYPTION_START);
        if (b64SecuredIV == null) {
            throw new CosClientException("Content encrypting key or IV not found.");
        }

        byte[] securedIV = Base64.decode(b64SecuredIV);

        final String keyWrapAlgo = userMeta.get(Headers.ENCRYPTION_WRAP_ALG);
        final String matdescStr = userMeta.get(Headers.ENCRYPTION_MATDESC);

        final boolean isKMS = isKMSKeyWrapped(keyWrapAlgo);
        final Map<String, String> metadesc = matdescFromJson(matdescStr);
        final EncryptionMaterials materials;
        if (isKMS) {
            if (kekMaterialAccessor instanceof KMSEncryptionMaterialsProvider) {
                KMSEncryptionMaterialsProvider kmsMaterialsProvider = (KMSEncryptionMaterialsProvider) kekMaterialAccessor;
                materials = kmsMaterialsProvider.getEncryptionMaterials();
            } else {
                throw new CosClientException("Must use KMSEncryptionMaterials");
            }
            if (metadesc != null) {
                materials.addDescriptions(metadesc);
            }
        } else {
            materials = kekMaterialAccessor == null ? null
                    : kekMaterialAccessor.getEncryptionMaterials(metadesc);
            if (materials == null) {
                throw new CosClientException("Unable to retrieve the client encryption materials");
            }
        }

        // CEK algorithm
        String cekAlgo = userMeta.get(Headers.ENCRYPTION_CEK_ALG);
        boolean isRangeGet = range != null;

        // The content crypto scheme may vary depending on whether
        // it is a range get operation
        ContentCryptoScheme contentCryptoScheme =
                ContentCryptoScheme.fromCEKAlgo(cekAlgo, isRangeGet);

        byte[] iv = decryptIV(securedIV, keyWrapAlgo, materials, securityProvider, contentCryptoScheme, kms);

        if (isRangeGet) {
            // Adjust the IV as needed
            iv = contentCryptoScheme.adjustIV(iv, range[0]);
        }

        // Unwrap or decrypt the CEK
        if (keyWrapExpected && keyWrapAlgo == null)
            throw newKeyWrapException();

        SecretKey cek =
                cek(securedKey, keyWrapAlgo, materials, securityProvider, contentCryptoScheme, kms);

        return new ContentCryptoMaterial(metadesc, securedKey, keyWrapAlgo, contentCryptoScheme
                .createCipherLite(cek, iv, Cipher.DECRYPT_MODE, securityProvider), null);
    }

    private static CosClientException newKeyWrapException() {
        return new CosClientException("Missing key-wrap for the content-encrypting-key");
    }

    /**
     * @return a non-null content crypto material.
     */
    static ContentCryptoMaterial fromInstructionFile(Map<String, String> instFile,
            EncryptionMaterialsAccessor kekMaterialAccessor, Provider securityProvider,
            boolean keyWrapExpected, QCLOUDKMS kms) {
        return fromInstructionFile0(instFile, kekMaterialAccessor, securityProvider, null,
                keyWrapExpected, kms);
    }

    /**
     * Factory method to return the content crypto material from the cos instruction file, using the
     * specified key encrypting key material accessor and an optional security provider.
     *
     * @return a non-null content crypto material.
     */
    static ContentCryptoMaterial fromInstructionFile(Map<String, String> instFile,
            EncryptionMaterialsAccessor kekMaterialAccessor, Provider securityProvider,
            long[] range, boolean keyWrapExpected, QCLOUDKMS kms) {
        return fromInstructionFile0(instFile, kekMaterialAccessor, securityProvider, range,
                keyWrapExpected, kms);
    }

    /**
     * @return a non-null content crypto material.
     */
    private static ContentCryptoMaterial fromInstructionFile0(Map<String, String> instFile,
            EncryptionMaterialsAccessor kekMaterialAccessor, Provider securityProvider,
            long[] range, boolean keyWrapExpected, QCLOUDKMS kms) {
        // CEK and IV
        String b64key = instFile.get(Headers.CRYPTO_KEY_V2);
        if (b64key == null) {
            b64key = instFile.get(Headers.CRYPTO_KEY);
            if (b64key == null)
                throw new CosClientException("Content encrypting key not found.");
        }
        byte[] cekWrapped = Base64.decode(b64key);
        byte[] iv = Base64.decode(instFile.get(Headers.CRYPTO_IV));
        if (cekWrapped == null || iv == null) {
            throw new CosClientException(
                    "Necessary encryption info not found in the instruction file " + instFile);
        }
        final String keyWrapAlgo = instFile.get(Headers.CRYPTO_KEYWRAP_ALGORITHM);
        final boolean isKMS = isKMSKeyWrapped(keyWrapAlgo);
        // Material description
        String matdescStr = instFile.get(Headers.MATERIALS_DESCRIPTION);
        final Map<String, String> core = matdescFromJson(matdescStr);
        EncryptionMaterials materials;
        if (isKMS) {
            materials = new KMSEncryptionMaterials(
                    core.get(KMSEncryptionMaterials.CUSTOMER_MASTER_KEY_ID));
            materials.addDescriptions(core);
        } else {
            materials = kekMaterialAccessor == null ? null
                    : kekMaterialAccessor.getEncryptionMaterials(core);
            if (materials == null) {
                throw new CosClientException(
                        "Unable to retrieve the encryption materials that originally "
                                + "encrypted object corresponding to instruction file " + instFile);
            }
        }
        // CEK algorithm
        final String cekAlgo = instFile.get(Headers.CRYPTO_CEK_ALGORITHM);
        final boolean isRangeGet = range != null;
        // The content crypto scheme may vary depending on whether
        // it is a range get operation
        ContentCryptoScheme contentCryptoScheme =
                ContentCryptoScheme.fromCEKAlgo(cekAlgo, isRangeGet);
        if (isRangeGet) {
            // Adjust the IV as needed
            iv = contentCryptoScheme.adjustIV(iv, range[0]);
        } else {
            // Validate the tag length supported
            int tagLenExpected = contentCryptoScheme.getTagLengthInBits();
            if (tagLenExpected > 0) {
                String s = instFile.get(Headers.CRYPTO_TAG_LENGTH);
                int tagLenActual = Integer.parseInt(s);
                if (tagLenExpected != tagLenActual) {
                    throw new CosClientException("Unsupported tag length: " + tagLenActual
                            + ", expected: " + tagLenExpected);
                }
            }
        }
        // Unwrap or decrypt the CEK
        if (keyWrapExpected && keyWrapAlgo == null)
            throw newKeyWrapException();
        SecretKey cek =
                cek(cekWrapped, keyWrapAlgo, materials, securityProvider, contentCryptoScheme, kms);
        return new ContentCryptoMaterial(core, cekWrapped, keyWrapAlgo, contentCryptoScheme
                .createCipherLite(cek, iv, Cipher.DECRYPT_MODE, securityProvider), null);
    }

    /**
     * Parses instruction data retrieved from COS and returns a JSON string representing the
     * instruction. Made for testing purposes.
     */
    static String parseInstructionFile(COSObject instructionFile) {
        try {
            return convertStreamToString(instructionFile.getObjectContent());
        } catch (Exception e) {
            throw new CosClientException("Error parsing JSON instruction file", e);
        }
    }

    /**
     * Converts the contents of an input stream to a String
     */
    private static String convertStreamToString(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "";
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            try {
                BufferedReader reader =
                        new BufferedReader(new InputStreamReader(inputStream, StringUtils.UTF8));
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }
            } finally {
                inputStream.close();
            }
            return stringBuilder.toString();
        }
    }

    /**
     * Return the cipher lite used for content encryption/decryption purposes.
     */
    CipherLite getCipherLite() {
        return cipherLite;
    }

    /**
     * Returns the description of the kek materials that were used to encrypt the cek.
     */
    Map<String, String> getKEKMaterialsDescription() {
        return this.kekMaterialsDescription;
    }

    /**
     * Returns an array of bytes representing the encrypted envelope symmetric key.
     *
     * @return an array of bytes representing the encrypted envelope symmetric key.
     */
    byte[] getEncryptedCEK() {
        return this.encryptedCEK.clone();
    }

    /**
     * Recreates a new content crypto material from the current material given a new KEK
     * material-descriptions. The purpose is to re-encrypt the CEK under a different KEK.
     *
     * Note network calls are involved if the CEK has been or is to be protected by KMS.
     *
     * @param newKEKMatDesc material descriptions for the new KEK; never null
     * @param accessor used to retrieve the KEK given the corresponding material description
     * @param targetScheme the target crypto scheme to be used for key wrapping, etc.
     * @param p optional security provider; null means to use the default.
     * @throws SecurityException if the old and new material description are the same; or if the old
     *         and new KEK are the same
     */
    ContentCryptoMaterial recreate(Map<String, String> newKEKMatDesc,
            EncryptionMaterialsAccessor accessor, COSCryptoScheme targetScheme, Provider p,
            QCLOUDKMS kms, CosServiceRequest req) {
        if (!usesKMSKey() && newKEKMatDesc.equals(kekMaterialsDescription)) {
            throw new SecurityException(
                    "Material description of the new KEK must differ from the current one");
        }
        final EncryptionMaterials origKEK;
        if (usesKMSKey()) {
            origKEK = new KMSEncryptionMaterials(
                    kekMaterialsDescription.get(KMSEncryptionMaterials.CUSTOMER_MASTER_KEY_ID));
        } else {
            origKEK = accessor.getEncryptionMaterials(kekMaterialsDescription);
            if (origKEK == null) {
                throw new CosClientException("Unable to retrieve the origin encryption materials");
            }
        }
        EncryptionMaterials newKEK = accessor.getEncryptionMaterials(newKEKMatDesc);
        if (newKEK == null) {
            throw new CosClientException("No material available with the description "
                    + newKEKMatDesc + " from the encryption material provider");
        }
        SecretKey cek =
                cek(encryptedCEK, keyWrappingAlgorithm, origKEK, p, getContentCryptoScheme(), kms);
        ContentCryptoMaterial output =
                create(cek, cipherLite.getIV(), newKEK, getContentCryptoScheme(), // must use same
                                                                                  // content crypto
                                                                                  // scheme
                        targetScheme, p, kms, req);
        if (Arrays.equals(output.encryptedCEK, encryptedCEK)) {
            throw new SecurityException("The new KEK must differ from the original");
        }
        return output;
    }

    /**
     * Recreates a new content crypto material from the current material given a new KEK encryption
     * materials. The purpose is to re-encrypt the CEK under the new KEK.
     *
     * Note network calls are involved if the CEK has been or is to be protected by KMS.
     *
     * @param newKEK encryption materials for the new KEK; must not be null
     * @param accessor used to retrieve the original KEK given the corresponding material
     *        description
     * @param targetScheme the target crypto scheme to use for recreating the content crypto
     *        material
     * @param p optional security provider; null means to use the default.
     * @throws SecurityException if the old and new material description are the same; or if the old
     *         and new KEK are the same
     */
    ContentCryptoMaterial recreate(EncryptionMaterials newKEK, EncryptionMaterialsAccessor accessor,
            COSCryptoScheme targetScheme, Provider p, QCLOUDKMS kms, CosServiceRequest req) {
        if (!usesKMSKey() && newKEK.getMaterialsDescription().equals(kekMaterialsDescription)) {
            throw new SecurityException(
                    "Material description of the new KEK must differ from the current one");
        }
        final EncryptionMaterials origKEK;
        if (usesKMSKey()) {
            origKEK = new KMSEncryptionMaterials(
                    kekMaterialsDescription.get(KMSEncryptionMaterials.CUSTOMER_MASTER_KEY_ID));
        } else {
            origKEK = accessor.getEncryptionMaterials(kekMaterialsDescription);
            if (origKEK == null) {
                throw new CosClientException("Unable to retrieve the origin encryption materials");
            }
        }
        SecretKey cek =
                cek(encryptedCEK, keyWrappingAlgorithm, origKEK, p, getContentCryptoScheme(), kms);
        ContentCryptoMaterial output =
                create(cek, cipherLite.getIV(), newKEK, getContentCryptoScheme(), // must use same
                                                                                  // content crypto
                                                                                  // scheme
                        targetScheme, // target scheme used to recreate the content crypto material
                        p, kms, req);
        if (Arrays.equals(output.encryptedCEK, encryptedCEK)) {
            throw new SecurityException("The new KEK must differ from the original");
        }
        return output;
    }

    /**
     * Returns a new instance of <code>ContentCryptoMaterial</code> for the input parameters using
     * the specified content crypto scheme, and the key wrapping and secure randomness specified of
     * the specified cos crypto scheme.
     *
     * Note network calls are involved if the CEK is to be protected by KMS.
     *
     * @param cek content encrypting key; must not be null.
     * @param iv initialization vector; must not be null.
     * @param contentCryptoScheme content crypto scheme to be used
     * @param targetScheme the target cos crypto scheme to be used for recreating the content crypto
     *        material by providing the key wrapping scheme and mechanism for secure randomness
     * @param provider optional security provider
     */
    static ContentCryptoMaterial create(SecretKey cek, byte[] iv, EncryptionMaterials kekMaterials,
            ContentCryptoScheme contentCryptoScheme, COSCryptoScheme targetScheme,
            Provider provider, QCLOUDKMS kms, CosServiceRequest req) {
        return doCreate(cek, iv, kekMaterials, contentCryptoScheme, targetScheme, provider, kms,
                req);
    }

    /**
     * Returns a new instance of <code>ContentCryptoMaterial</code> for the input parameters using
     * the specified cos crypto scheme. Note network calls are involved if the CEK is to be
     * protected by KMS.
     *
     * @param cek content encrypting key
     * @param iv initialization vector
     * @param kekMaterials kek encryption material used to secure the CEK; can be KMS enabled.
     * @param scheme cos crypto scheme to be used for the content crypto material by providing the
     *        content crypto scheme, key wrapping scheme and mechanism for secure randomness
     * @param provider optional security provider
     * @param kms reference to the KMS client
     * @param req originating service request
     */
    static ContentCryptoMaterial create(SecretKey cek, byte[] iv, EncryptionMaterials kekMaterials,
            COSCryptoScheme scheme, Provider provider, QCLOUDKMS kms, CosServiceRequest req) {
        return doCreate(cek, iv, kekMaterials, scheme.getContentCryptoScheme(), scheme, provider,
                kms, req);
    }

    /**
     * Returns a new instance of <code>ContentCryptoMaterial</code> for the given input parameters
     * by using the specified content crypto scheme, and COS crypto scheme.
     *
     * Note network calls are involved if the CEK is to be protected by KMS.
     *
     * @param cek content encrypting key
     * @param iv initialization vector
     * @param kekMaterials kek encryption material used to secure the CEK; can be KMS enabled.
     * @param contentCryptoScheme content crypto scheme to be used, which can differ from the one of
     *        <code>targetCOSCryptoScheme</code>
     * @param targetCOSCryptoScheme the target cos crypto scheme to be used for providing the key
     *        wrapping scheme and mechanism for secure randomness
     * @param provider security provider
     * @param kms reference to the KMS client
     * @param req the originating COS service request
     */
    private static ContentCryptoMaterial doCreate(SecretKey cek, byte[] iv,
            EncryptionMaterials kekMaterials, ContentCryptoScheme contentCryptoScheme,
            COSCryptoScheme targetCOSCryptoScheme, Provider provider, QCLOUDKMS kms,
            CosServiceRequest req) {
        // Secure the envelope symmetric key either by encryption, key wrapping
        // or KMS.
        SecuredCEK cekSecured =
                secureCEK(cek, kekMaterials, targetCOSCryptoScheme.getKeyWrapScheme(),
                        targetCOSCryptoScheme.getSecureRandom(), provider, kms, req);
        byte[] securedIV =
                encryptIV(iv, kekMaterials, targetCOSCryptoScheme.getKeyWrapScheme(),
                        targetCOSCryptoScheme.getSecureRandom(), provider, kms, req);
        return wrap(cek, iv, contentCryptoScheme, provider, cekSecured, securedIV);
    }

    /**
     * Returns a new instance of <code>ContentCryptoMaterial</code> by wrapping the input
     * parameters, including the already secured CEK. No network calls are involved.
     */
    public static ContentCryptoMaterial wrap(SecretKey cek, byte[] iv,
            ContentCryptoScheme contentCryptoScheme, Provider provider, SecuredCEK cekSecured, byte[] securedIV) {
        return new ContentCryptoMaterial(cekSecured.getMaterialDescription(),
                cekSecured.getEncrypted(), cekSecured.getKeyWrapAlgorithm(),
                contentCryptoScheme.createCipherLite(cek, iv, Cipher.ENCRYPT_MODE, provider), securedIV);
    }

    /**
     * Secure the given CEK. Note network calls are involved if the CEK is to be protected by KMS.
     *
     * @param cek content encrypting key to be secured
     * @param materials used to provide the key-encryption-key (KEK); or if it is KMS-enabled, the
     *        customer master key id and material description.
     * @param contentCryptoScheme the content crypto scheme
     * @param p optional security provider; can be null if the default is used.
     * @return a secured CEK in the form of ciphertext or ciphertext blob.
     */
    private static SecuredCEK secureCEK(SecretKey cek, EncryptionMaterials materials,
            COSKeyWrapScheme kwScheme, SecureRandom srand, Provider p, QCLOUDKMS kms,
            CosServiceRequest req) {
        final Map<String, String> matdesc;

        if (materials.isKMSEnabled()) {
            matdesc = mergeMaterialDescriptions(materials, req);
            EncryptRequest encryptRequest = new EncryptRequest();
            try {
                ObjectMapper mapper = new ObjectMapper();
                encryptRequest.setEncryptionContext(mapper.writeValueAsString(matdesc));
            } catch (JsonProcessingException e) {
                throw new CosClientException("encrypt request set encryption context got json processing exception", e);
            }
            encryptRequest.setKeyId(materials.getCustomerMasterKeyId());
            encryptRequest.setPlaintext(cek.getEncoded().toString());

            EncryptResponse encryptResponse = kms.encrypt(encryptRequest);
            byte[] keyBlob = encryptResponse.getCiphertextBlob().getBytes();
            return new KMSSecuredCEK(keyBlob, matdesc);
        } else {
            matdesc = materials.getMaterialsDescription();
        }
        Key kek;
        if (materials.getKeyPair() != null) {
            // Do envelope encryption with public key from key pair
            kek = materials.getKeyPair().getPublic();
        } else {
            // Do envelope encryption with symmetric key
            kek = materials.getSymmetricKey();
        }
        String keyWrapAlgo = kwScheme.getKeyWrapAlgorithm(kek);
        try {
            Cipher cipher = p == null ? Cipher.getInstance(keyWrapAlgo)
                    : Cipher.getInstance(keyWrapAlgo, p);
            cipher.init(Cipher.WRAP_MODE, kek, srand);
            return new SecuredCEK(cipher.wrap(cek), keyWrapAlgo, matdesc);
        } catch (Exception e) {
            throw new CosClientException("Unable to encrypt symmetric key", e);
        }
    }

    public static byte[] encryptIV(byte[] iv, EncryptionMaterials materials,
            COSKeyWrapScheme kwScheme, SecureRandom srand, Provider p, QCLOUDKMS kms,
            CosServiceRequest req) {

        if (materials.isKMSEnabled()) {
            Map<String, String> matdesc = mergeMaterialDescriptions(materials, req);
            EncryptRequest encryptRequest = new EncryptRequest();
            try {
                ObjectMapper mapper = new ObjectMapper();
                encryptRequest.setEncryptionContext(mapper.writeValueAsString(matdesc));
            } catch (JsonProcessingException e) {
                throw new CosClientException("encrypt request set encryption context got json processing exception", e);
            }
            encryptRequest.setKeyId(materials.getCustomerMasterKeyId());
            encryptRequest.setPlaintext(Base64.encodeAsString(iv));

            EncryptResponse encryptResponse = kms.encrypt(encryptRequest);

            String cipherIV = encryptResponse.getCiphertextBlob();
            return cipherIV.getBytes(Charset.forName("UTF-8"));
        }

        Key kek;
        if (materials.getKeyPair() != null) {
            // Do envelope encryption with public key from key pair
            kek = materials.getKeyPair().getPublic();
        } else {
            // Do envelope encryption with symmetric key
            kek = materials.getSymmetricKey();
        }

        String keyWrapAlgo = kwScheme.getKeyWrapAlgorithm(kek);
        try {
            Cipher cipher = p == null ? Cipher.getInstance(keyWrapAlgo)
                    : Cipher.getInstance(keyWrapAlgo, p);
            cipher.init(Cipher.ENCRYPT_MODE, kek, srand);
            return cipher.doFinal(iv);
        } catch (Exception e) {
            throw new CosClientException("Unable to encrypt IV", e);
        }
    }

    public static byte[] decryptIV(byte[] iv, String keyWrapAlgo,
            EncryptionMaterials materials, Provider securityProvider,
            ContentCryptoScheme contentCryptoScheme, QCLOUDKMS kms) {
        if (materials.isKMSEnabled()) {

            DecryptRequest decryptReq = new DecryptRequest();
            Map<String, String> materialDesc = materials.getMaterialsDescription();
            try {
                ObjectMapper mapper = new ObjectMapper();
                decryptReq.setEncryptionContext(mapper.writeValueAsString(materialDesc));
            } catch (JsonProcessingException e) {
                throw new CosClientException("decrypt request set encryption context got json processing exception", e);
            }
            decryptReq.setCiphertextBlob(new String(iv, Charset.forName("UTF-8")));

            DecryptResponse decryptRes = kms.decrypt(decryptReq);

            return Base64.decode(decryptRes.getPlaintext());
        }

        Key kek;
        if (materials.getKeyPair() != null) {
            // Do envelope decryption with private key from key pair
            kek = materials.getKeyPair().getPrivate();
            if (kek == null) {
                throw new CosClientException("Key encrypting key not available");
            }
        } else {
            // Do envelope decryption with symmetric key
            kek = materials.getSymmetricKey();
            if (kek == null) {
                throw new CosClientException("Key encrypting key not available");
            }
        }

        try {
            if (keyWrapAlgo != null) {
                // Key wrapping specified
                Cipher cipher = securityProvider == null ? Cipher.getInstance(keyWrapAlgo)
                        : Cipher.getInstance(keyWrapAlgo, securityProvider);
                cipher.init(Cipher.DECRYPT_MODE, kek);
                return cipher.doFinal(iv);
            }

            // fall back to the Encryption Only (EO) key decrypting method
            Cipher cipher;
            if (securityProvider != null) {
                cipher = Cipher.getInstance(kek.getAlgorithm(), securityProvider);
            } else {
                cipher = Cipher.getInstance(kek.getAlgorithm());
            }
            cipher.init(Cipher.DECRYPT_MODE, kek);
            return cipher.doFinal(iv);
        } catch (Exception e) {
            throw new CosClientException("Unable to decrypt symmetric key from object metadata", e);
        }
    }

    static Map<String, String> mergeMaterialDescriptions(EncryptionMaterials materials,
            CosServiceRequest req) {
        Map<String, String> matdesc = materials.getMaterialsDescription();
        if (req instanceof MaterialsDescriptionProvider) {
            MaterialsDescriptionProvider mdp = (MaterialsDescriptionProvider) req;
            Map<String, String> matdesc_req = mdp.getMaterialsDescription();
            if (matdesc_req != null) {
                matdesc = new TreeMap<String, String>(matdesc);
                matdesc.putAll(matdesc_req); // request takes precedence
            }
        }
        return matdesc;
    }
}
