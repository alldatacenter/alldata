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

import static com.qcloud.cos.internal.LengthCheckInputStream.EXCLUDE_SKIPPED_BYTES;
import static com.qcloud.cos.internal.crypto.CryptoStorageMode.InstructionFile;
import static com.qcloud.cos.internal.crypto.CryptoStorageMode.ObjectMetadata;
import static com.qcloud.cos.model.CosDataSource.Utils.cleanupDataSource;
import static com.qcloud.cos.model.InstructionFileId.DEFAULT_INSTRUCTION_FILE_SUFFIX;
import static com.qcloud.cos.model.InstructionFileId.DOT;
import static com.qcloud.cos.utils.StringUtils.UTF8;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qcloud.cos.Headers;
import com.qcloud.cos.auth.COSCredentialsProvider;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.internal.COSDirect;
import com.qcloud.cos.internal.CosServiceRequest;
import com.qcloud.cos.internal.InputSubstream;
import com.qcloud.cos.internal.LengthCheckInputStream;
import com.qcloud.cos.internal.ReleasableInputStream;
import com.qcloud.cos.internal.ResettableInputStream;
import com.qcloud.cos.internal.SdkFilterInputStream;
import com.qcloud.cos.model.AbortMultipartUploadRequest;
import com.qcloud.cos.model.AbstractPutObjectRequest;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectId;
import com.qcloud.cos.model.CompleteMultipartUploadRequest;
import com.qcloud.cos.model.CompleteMultipartUploadResult;
import com.qcloud.cos.model.CopyPartRequest;
import com.qcloud.cos.model.CopyPartResult;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.InitiateMultipartUploadRequest;
import com.qcloud.cos.model.InitiateMultipartUploadResult;
import com.qcloud.cos.model.InstructionFileId;
import com.qcloud.cos.model.MaterialsDescriptionProvider;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutInstructionFileRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.UploadPartRequest;
import com.qcloud.cos.model.UploadPartResult;
import com.qcloud.cos.utils.Base64;
import com.qcloud.cos.utils.IOUtils;
import com.qcloud.cos.utils.Jackson;
import com.tencentcloudapi.kms.v20190118.models.GenerateDataKeyRequest;
import com.tencentcloudapi.kms.v20190118.models.GenerateDataKeyResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * Common implementation for different COS cryptographic modules.
 */
public abstract class COSCryptoModuleBase extends COSCryptoModule {
    private static final boolean IS_MULTI_PART = true;
    protected static final int DEFAULT_BUFFER_SIZE = 1024 * 2; // 2K
    protected final EncryptionMaterialsProvider kekMaterialsProvider;
    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final COSCryptoScheme cryptoScheme;
    protected final ContentCryptoScheme contentCryptoScheme;
    /** A read-only copy of the crypto configuration. */
    protected final CryptoConfiguration cryptoConfig;

    /** Map of data about in progress encrypted multipart uploads. */
    protected final Map<String, MultipartUploadCryptoContext> multipartUploadContexts =
            Collections.synchronizedMap(new HashMap<String, MultipartUploadCryptoContext>());
    protected final COSDirect cos;
    protected final QCLOUDKMS kms;

    /**
     * @param cryptoConfig a read-only copy of the crypto configuration.
     */
    protected COSCryptoModuleBase(QCLOUDKMS kms, COSDirect cos,
            COSCredentialsProvider credentialsProvider,
            EncryptionMaterialsProvider kekMaterialsProvider, CryptoConfiguration cryptoConfig) {
        if (!cryptoConfig.isReadOnly())
            throw new IllegalArgumentException(
                    "The cryto configuration parameter is required to be read-only");
        this.kekMaterialsProvider = kekMaterialsProvider;
        this.cos = cos;
        this.cryptoConfig = cryptoConfig;
        this.cryptoScheme = COSCryptoScheme.from(cryptoConfig.getCryptoMode());
        this.contentCryptoScheme = cryptoScheme.getContentCryptoScheme();
        this.kms = kms;

        // if have user defined iv, set it to contentCryptoScheme.
        this.contentCryptoScheme.setIV(cryptoConfig.getIV());
    }

    /**
     * For testing purposes only.
     */
    protected COSCryptoModuleBase(COSDirect cos, COSCredentialsProvider credentialsProvider,
            EncryptionMaterialsProvider kekMaterialsProvider, CryptoConfiguration cryptoConfig) {
        this.kekMaterialsProvider = kekMaterialsProvider;
        this.cos = cos;
        this.cryptoConfig = cryptoConfig;
        this.cryptoScheme = COSCryptoScheme.from(cryptoConfig.getCryptoMode());
        this.contentCryptoScheme = cryptoScheme.getContentCryptoScheme();
        this.kms = null;
    }

    /**
     * Returns the length of the ciphertext computed from the length of the plaintext.
     *
     * @param plaintextLength a non-negative number
     * @return a non-negative number
     */
    protected abstract long ciphertextLength(long plaintextLength);

    //////////////////////// Common Implementation ////////////////////////
    @Override
    public PutObjectResult putObjectSecurely(PutObjectRequest req) {
        return cryptoConfig.getStorageMode() == InstructionFile ? putObjectUsingInstructionFile(req)
                : putObjectUsingMetadata(req);
    }

    private PutObjectResult putObjectUsingMetadata(PutObjectRequest req) {
        ContentCryptoMaterial cekMaterial = createContentCryptoMaterial(req);
        // Wraps the object data with a cipher input stream
        final File fileOrig = req.getFile();
        final InputStream isOrig = req.getInputStream();
        PutObjectRequest wrappedReq = wrapWithCipher(req, cekMaterial);
        // Update the metadata
        req.setMetadata(updateMetadataWithContentCryptoMaterial(req.getMetadata(), req.getFile(),
                cekMaterial));
        // Put the encrypted object into COS
        try {
            return cos.putObject(wrappedReq);
        } finally {
            cleanupDataSource(req, fileOrig, isOrig, wrappedReq.getInputStream(), log);
        }
    }

    /**
     * Puts an encrypted object into COS, and puts an instruction file into COS. Encryption info is
     * stored in the instruction file.
     *
     * @param putObjectRequest The request object containing all the parameters to upload a new
     *        object to COS.
     * @return A {@link PutObjectResult} object containing the information returned by COS for
     *         the new, created object.
     */
    private PutObjectResult putObjectUsingInstructionFile(PutObjectRequest putObjectRequest) {
        final File fileOrig = putObjectRequest.getFile();
        final InputStream isOrig = putObjectRequest.getInputStream();
        final PutObjectRequest putInstFileRequest =
                putObjectRequest.clone().withFile(null).withInputStream(null);
        putInstFileRequest
                .setKey(putInstFileRequest.getKey() + DOT + DEFAULT_INSTRUCTION_FILE_SUFFIX);
        // Create instruction
        ContentCryptoMaterial cekMaterial = createContentCryptoMaterial(putObjectRequest);
        // Wraps the object data with a cipher input stream; note the metadata
        // is mutated as a side effect.
        PutObjectRequest req = wrapWithCipher(putObjectRequest, cekMaterial);
        // Put the encrypted object into COS
        final PutObjectResult result;
        try {
            result = cos.putObject(req);
        } finally {
            cleanupDataSource(putObjectRequest, fileOrig, isOrig, req.getInputStream(), log);
        }
        // Put the instruction file into COS
        cos.putObject(updateInstructionPutRequest(putInstFileRequest, cekMaterial));
        // Return the result of the encrypted object PUT.
        return result;
    }

    @Override
    public final void abortMultipartUploadSecurely(AbortMultipartUploadRequest req) {
        cos.abortMultipartUpload(req);
        multipartUploadContexts.remove(req.getUploadId());
    }

    @Override
    public final CopyPartResult copyPartSecurely(CopyPartRequest copyPartRequest) {
        String uploadId = copyPartRequest.getUploadId();
        MultipartUploadCryptoContext uploadContext = multipartUploadContexts.get(uploadId);
        CopyPartResult result = cos.copyPart(copyPartRequest);

        if (uploadContext != null && !uploadContext.hasFinalPartBeenSeen())
            uploadContext.setHasFinalPartBeenSeen(true);
        return result;
    }

    abstract MultipartUploadCryptoContext newUploadContext(InitiateMultipartUploadRequest req,
            ContentCryptoMaterial cekMaterial);

    @Override
    public InitiateMultipartUploadResult initiateMultipartUploadSecurely(
            InitiateMultipartUploadRequest req) {
        // Generate a one-time use symmetric key and initialize a cipher to
        // encrypt object data
        ContentCryptoMaterial cekMaterial = createContentCryptoMaterial(req);
        if (cryptoConfig.getStorageMode() == ObjectMetadata) {
            ObjectMetadata metadata = req.getObjectMetadata();
            if (metadata == null)
                metadata = new ObjectMetadata();

            long dataSize = req.getDataSize();
            long partSize = req.getPartSize();

            if (dataSize < 0 || partSize < 0) {
                throw new CosClientException("initiate multipart upload with encryption client must set dataSize and partSize");
            }

            if (partSize % 16 != 0) {
                throw new CosClientException("initiat multipart uplaod with encryption client must set part size a mutiple of 16"
                    + "but got " + partSize);
            }

            metadata.addUserMetadata(Headers.ENCRYPTION_DATA_SIZE, Long.toString(dataSize));
            metadata.addUserMetadata(Headers.ENCRYPTION_PART_SIZE, Long.toString(partSize));

            // Store encryption info in metadata
            req.setObjectMetadata(
                    updateMetadataWithContentCryptoMaterial(metadata, null, cekMaterial));
        }
        InitiateMultipartUploadResult result = cos.initiateMultipartUpload(req);
        MultipartUploadCryptoContext uploadContext = newUploadContext(req, cekMaterial);
        if (req instanceof MaterialsDescriptionProvider) {
            MaterialsDescriptionProvider p = (MaterialsDescriptionProvider) req;
            uploadContext.setMaterialsDescription(p.getMaterialsDescription());
        }
        multipartUploadContexts.put(result.getUploadId(), uploadContext);
        return result;
    }

    //// specific crypto module behavior for uploading parts.
    abstract CipherLite cipherLiteForNextPart(MultipartUploadCryptoContext uploadContext);

    abstract long computeLastPartSize(UploadPartRequest req);

    /**
     * {@inheritDoc}
     *
     * <p>
     * <b>NOTE:</b> Because the encryption process requires context from previous blocks, parts
     * uploaded with the COSEncryptionClient (as opposed to the normal COSClient) must be
     * uploaded serially, and in order. Otherwise, the previous encryption context isn't available
     * to use when encrypting the current part.
     */
    @Override
    public UploadPartResult uploadPartSecurely(UploadPartRequest req) {
        final int blockSize = contentCryptoScheme.getBlockSizeInBytes();
        final boolean isLastPart = req.isLastPart();
        final String uploadId = req.getUploadId();
        final long partSize = req.getPartSize();
        final boolean partSizeMultipleOfCipherBlockSize = 0 == (partSize % blockSize);
        if (!isLastPart && !partSizeMultipleOfCipherBlockSize) {
            throw new CosClientException(
                    "Invalid part size: part sizes for encrypted multipart uploads must be multiples "
                            + "of the cipher block size (" + blockSize
                            + ") with the exception of the last part.");
        }
        final MultipartUploadCryptoContext uploadContext = multipartUploadContexts.get(uploadId);
        if (uploadContext == null) {
            throw new CosClientException(
                    "No client-side information available on upload ID " + uploadId);
        }
        final UploadPartResult result;
        // Checks the parts are uploaded in series
        uploadContext.beginPartUpload(req.getPartNumber());
        CipherLite cipherLite = cipherLiteForNextPart(uploadContext);
        final File fileOrig = req.getFile();
        final InputStream isOrig = req.getInputStream();
        SdkFilterInputStream isCurr = null;
        try {
            CipherLiteInputStream clis = newMultipartCOSCipherInputStream(req, cipherLite);
            isCurr = clis; // so the clis will be closed (in the finally block below) upon
            // unexpected failure should we opened a file undereath
            req.setInputStream(isCurr);
            // Treat all encryption requests as input stream upload requests,
            // not as file upload requests.
            req.setFile(null);
            req.setFileOffset(0);
            // The last part of the multipart upload will contain an extra
            // 16-byte mac
            if (isLastPart) {
                // We only change the size of the last part
                long lastPartSize = computeLastPartSize(req);
                if (lastPartSize > -1)
                    req.setPartSize(lastPartSize);
                if (uploadContext.hasFinalPartBeenSeen()) {
                    throw new CosClientException(
                            "This part was specified as the last part in a multipart upload, but a previous part was already marked as the last part.  "
                                    + "Only the last part of the upload should be marked as the last part.");
                }
            }

            result = cos.uploadPart(req);
        } finally {
            cleanupDataSource(req, fileOrig, isOrig, isCurr, log);
            uploadContext.endPartUpload();
        }
        if (isLastPart)
            uploadContext.setHasFinalPartBeenSeen(true);
        return result;
    }

    protected final CipherLiteInputStream newMultipartCOSCipherInputStream(UploadPartRequest req,
            CipherLite cipherLite) {
        final File fileOrig = req.getFile();
        final InputStream isOrig = req.getInputStream();
        InputStream isCurr = null;
        try {
            if (fileOrig == null) {
                if (isOrig == null) {
                    throw new IllegalArgumentException(
                            "A File or InputStream must be specified when uploading part");
                }
                isCurr = isOrig;
            } else {
                isCurr = new ResettableInputStream(fileOrig);
            }
            isCurr = new InputSubstream(isCurr, req.getFileOffset(), req.getPartSize(),
                    req.isLastPart());
            return cipherLite.markSupported()
                    ? new CipherLiteInputStream(isCurr, cipherLite, DEFAULT_BUFFER_SIZE,
                            IS_MULTI_PART, req.isLastPart())
                    : new RenewableCipherLiteInputStream(isCurr, cipherLite, DEFAULT_BUFFER_SIZE,
                            IS_MULTI_PART, req.isLastPart());
        } catch (Exception e) {
            cleanupDataSource(req, fileOrig, isOrig, isCurr, log);
            throw new CosClientException("Unable to create cipher input stream", e);
        }
    }

    @Override
    public CompleteMultipartUploadResult completeMultipartUploadSecurely(
            CompleteMultipartUploadRequest req) {
        String uploadId = req.getUploadId();
        final MultipartUploadCryptoContext uploadContext = multipartUploadContexts.get(uploadId);

        if (uploadContext != null && !uploadContext.hasFinalPartBeenSeen()) {
            throw new CosClientException(
                    "Unable to complete an encrypted multipart upload without being told which part was the last.  "
                            + "Without knowing which part was the last, the encrypted data in COS is incomplete and corrupt.");
        }
        CompleteMultipartUploadResult result = cos.completeMultipartUpload(req);

        // In InstructionFile mode, we want to write the instruction file only
        // after the whole upload has completed correctly.
        if (uploadContext != null && cryptoConfig.getStorageMode() == InstructionFile) {
            // Put the instruction file into COS
            cos.putObject(createInstructionPutRequest(uploadContext.getBucketName(),
                    uploadContext.getKey(), uploadContext.getContentCryptoMaterial()));
        }
        multipartUploadContexts.remove(uploadId);
        return result;
    }

    protected final ObjectMetadata updateMetadataWithContentCryptoMaterial(ObjectMetadata metadata,
            File file, ContentCryptoMaterial instruction) {
        if (metadata == null)
            metadata = new ObjectMetadata();
        return instruction.toObjectMetadata(metadata, cryptoConfig.getCryptoMode());
    }

    /**
     * Creates and returns a non-null content crypto material for the given request.
     *
     * @throws CosClientException if no encryption material can be found.
     */
    protected final ContentCryptoMaterial createContentCryptoMaterial(CosServiceRequest req) {
        if (req instanceof EncryptionMaterialsFactory) {
            // per request level encryption materials
            EncryptionMaterialsFactory f = (EncryptionMaterialsFactory) req;
            final EncryptionMaterials materials = f.getEncryptionMaterials();
            if (materials != null) {
                return buildContentCryptoMaterial(materials, cryptoConfig.getCryptoProvider(), req);
            }
        }
        if (req instanceof MaterialsDescriptionProvider) {
            // per request level material description
            MaterialsDescriptionProvider mdp = (MaterialsDescriptionProvider) req;
            Map<String, String> matdesc_req = mdp.getMaterialsDescription();
            ContentCryptoMaterial ccm = newContentCryptoMaterial(kekMaterialsProvider, matdesc_req,
                    cryptoConfig.getCryptoProvider(), req);
            if (ccm != null)
                return ccm;
            if (matdesc_req != null) {
                // check to see if KMS is in use and if so we should fall thru
                // to the COS client level encryption material
                EncryptionMaterials material = kekMaterialsProvider.getEncryptionMaterials();
                if (!material.isKMSEnabled()) {
                    throw new CosClientException(
                            "No material available from the encryption material provider for description "
                                    + matdesc_req);
                }
            }
            // if there is no material description, fall thru to use
            // the per cos client level encryption materials
        }
        // per cos client level encryption materials
        return newContentCryptoMaterial(this.kekMaterialsProvider, cryptoConfig.getCryptoProvider(),
                req);
    }

    /**
     * Returns the content encryption material generated with the given kek material, material
     * description and security providers; or null if the encryption material cannot be found for
     * the specified description.
     */
    private ContentCryptoMaterial newContentCryptoMaterial(
            EncryptionMaterialsProvider kekMaterialProvider,
            Map<String, String> materialsDescription, Provider provider, CosServiceRequest req) {
        EncryptionMaterials kekMaterials =
                kekMaterialProvider.getEncryptionMaterials(materialsDescription);
        if (kekMaterials == null) {
            return null;
        }
        return buildContentCryptoMaterial(kekMaterials, provider, req);
    }

    /**
     * Returns a non-null content encryption material generated with the given kek material and
     * security providers.
     *
     * @throws SdkClientException if no encryption material can be found from the given encryption
     *         material provider.
     */
    private ContentCryptoMaterial newContentCryptoMaterial(
            EncryptionMaterialsProvider kekMaterialProvider, Provider provider,
            CosServiceRequest req) {
        EncryptionMaterials kekMaterials = kekMaterialProvider.getEncryptionMaterials();
        if (kekMaterials == null)
            throw new CosClientException(
                    "No material available from the encryption material provider");
        return buildContentCryptoMaterial(kekMaterials, provider, req);
    }

    /**
     * @param materials a non-null encryption material
     */
    private ContentCryptoMaterial buildContentCryptoMaterial(EncryptionMaterials materials,
            Provider provider, CosServiceRequest req) {
        byte[] iv = contentCryptoScheme.getIV();

        if (iv == null) {
            // Randomly generate the IV
            iv = new byte[contentCryptoScheme.getIVLengthInBytes()];
            cryptoScheme.getSecureRandom().nextBytes(iv);
        }

        if (materials.isKMSEnabled()) {
            final Map<String, String> encryptionContext =
                    ContentCryptoMaterial.mergeMaterialDescriptions(materials, req);

            GenerateDataKeyRequest keyGenReq = new GenerateDataKeyRequest();
            try {
                ObjectMapper mapper = new ObjectMapper();
                keyGenReq.setEncryptionContext(mapper.writeValueAsString(encryptionContext));
            } catch (JsonProcessingException e) {
                throw new CosClientException("generate datakey request set encryption context got json processing exception", e);
            }
            keyGenReq.setKeyId(materials.getCustomerMasterKeyId());
            keyGenReq.setKeySpec(contentCryptoScheme.getKeySpec());

            GenerateDataKeyResponse keyGenRes = kms.generateDataKey(keyGenReq);

            byte[] key = Base64.decode(keyGenRes.getPlaintext());
            final SecretKey cek = new SecretKeySpec(key,
                                  contentCryptoScheme.getKeyGeneratorAlgorithm());

            byte[] keyBlob = keyGenRes.getCiphertextBlob().getBytes();
            byte[] securedIV = ContentCryptoMaterial.encryptIV(iv, materials, cryptoScheme.getKeyWrapScheme(),
                                    cryptoScheme.getSecureRandom(), provider, kms, req);
            return ContentCryptoMaterial.wrap(cek, iv, contentCryptoScheme, provider,
                    new KMSSecuredCEK(keyBlob, encryptionContext), securedIV);
        } else {
            // Generate a one-time use symmetric key and initialize a cipher to encrypt object data
            return ContentCryptoMaterial.create(generateCEK(materials, provider), iv, materials,
                    cryptoScheme, provider, kms, req);
        }
    }

    /**
     * @param kekMaterials non-null encryption materials
     */
    protected final SecretKey generateCEK(final EncryptionMaterials kekMaterials,
            final Provider providerIn) {
        final String keygenAlgo = contentCryptoScheme.getKeyGeneratorAlgorithm();
        KeyGenerator generator;
        try {
            generator = providerIn == null ? KeyGenerator.getInstance(keygenAlgo)
                    : KeyGenerator.getInstance(keygenAlgo, providerIn);
            generator.init(contentCryptoScheme.getKeyLengthInBits(),
                    cryptoScheme.getSecureRandom());
            // Set to true if the key encryption involves the use of BC's public key
            boolean involvesBCPublicKey = false;
            KeyPair keypair = kekMaterials.getKeyPair();
            if (keypair != null) {
                String keyWrapAlgo =
                        cryptoScheme.getKeyWrapScheme().getKeyWrapAlgorithm(keypair.getPublic());
                if (keyWrapAlgo == null) {
                    Provider provider = generator.getProvider();
                    String providerName = provider == null ? null : provider.getName();
                    involvesBCPublicKey = CryptoRuntime.BOUNCY_CASTLE_PROVIDER.equals(providerName);
                }
            }
            SecretKey secretKey = generator.generateKey();
            if (!involvesBCPublicKey || secretKey.getEncoded()[0] != 0)
                return secretKey;
            for (int retry = 0; retry < 10; retry++) {
                secretKey = generator.generateKey();
                if (secretKey.getEncoded()[0] != 0)
                    return secretKey;
            }
            // The probability of getting here is 2^80, which is impossible in practice.
            throw new CosClientException("Failed to generate secret key");
        } catch (NoSuchAlgorithmException e) {
            throw new CosClientException(
                    "Unable to generate envelope symmetric key:" + e.getMessage(), e);
        }
    }

    /**
     * Returns the given <code>PutObjectRequest</code> but has the content as input stream wrapped
     * with a cipher, and configured with some meta data and user metadata.
     */
    protected final <R extends AbstractPutObjectRequest> R wrapWithCipher(final R request,
            ContentCryptoMaterial cekMaterial) {
        // Create a new metadata object if there is no metadata already.
        ObjectMetadata metadata = request.getMetadata();
        if (metadata == null) {
            metadata = new ObjectMetadata();
        }

        // Record the original Content MD5, if present, for the unencrypted data
        if (metadata.getContentMD5() != null) {
            metadata.addUserMetadata(Headers.ENCRYPTION_UNENCRYPTED_CONTENT_MD5, metadata.getContentMD5());
        }

        // Removes the original content MD5 if present from the meta data.
        metadata.setContentMD5(null);

        // Record the original, unencrypted content-length so it can be accessed
        // later
        final long plaintextLength = plaintextLength(request, metadata);
        if (plaintextLength >= 0) {
            metadata.addUserMetadata(Headers.ENCRYPTION_UNENCRYPTED_CONTENT_LENGTH,
                    Long.toString(plaintextLength));
            metadata.setContentLength(ciphertextLength(plaintextLength));
        }
        request.setMetadata(metadata);
        request.setInputStream(newCOSCipherLiteInputStream(request, cekMaterial, plaintextLength));
        // Treat all encryption requests as input stream upload requests, not as
        // file upload requests.
        request.setFile(null);
        return request;
    }

    private CipherLiteInputStream newCOSCipherLiteInputStream(AbstractPutObjectRequest req,
            ContentCryptoMaterial cekMaterial, long plaintextLength) {
        final File fileOrig = req.getFile();
        final InputStream isOrig = req.getInputStream();
        InputStream isCurr = null;
        try {
            if (fileOrig == null) {
                // When input is a FileInputStream, this wrapping enables
                // unlimited mark-and-reset
                isCurr = isOrig == null ? null : ReleasableInputStream.wrap(isOrig);
            } else {
                isCurr = new ResettableInputStream(fileOrig);
            }
            if (plaintextLength > -1) {
                // COS allows a single PUT to be no more than 5GB, which
                // therefore won't exceed the maximum length that can be
                // encrypted either using any cipher such as CBC or GCM.

                // This ensures the plain-text read from the underlying data
                // stream has the same length as the expected total.
                isCurr = new LengthCheckInputStream(isCurr, plaintextLength, EXCLUDE_SKIPPED_BYTES);
            }
            final CipherLite cipherLite = cekMaterial.getCipherLite();

            if (cipherLite.markSupported()) {
                return new CipherLiteInputStream(isCurr, cipherLite, DEFAULT_BUFFER_SIZE);
            } else {
                return new RenewableCipherLiteInputStream(isCurr, cipherLite, DEFAULT_BUFFER_SIZE);
            }
        } catch (Exception e) {
            cleanupDataSource(req, fileOrig, isOrig, isCurr, log);
            throw new CosClientException("Unable to create cipher input stream", e);
        }
    }

    /**
     * Returns the plaintext length from the request and metadata; or -1 if unknown.
     */
    protected final long plaintextLength(AbstractPutObjectRequest request,
            ObjectMetadata metadata) {
        if (request.getFile() != null) {
            return request.getFile().length();
        } else if (request.getInputStream() != null
                && metadata.getRawMetadataValue(Headers.CONTENT_LENGTH) != null) {
            return metadata.getContentLength();
        }
        return -1;
    }

    public final COSCryptoScheme getCOSCryptoScheme() {
        return cryptoScheme;
    }

    /**
     * Updates put request to store the specified instruction object in COS.
     *
     * @param req The put-instruction-file request for the instruction file to be stored in COS.
     * @param cekMaterial The instruction object to be stored in COS.
     * @return A put request to store the specified instruction object in COS.
     */
    protected final PutObjectRequest updateInstructionPutRequest(PutObjectRequest req,
            ContentCryptoMaterial cekMaterial) {
        byte[] bytes = cekMaterial.toJsonString().getBytes(UTF8);
        ObjectMetadata metadata = req.getMetadata();
        if (metadata == null) {
            metadata = new ObjectMetadata();
            req.setMetadata(metadata);
        }
        // Set the content-length of the upload
        metadata.setContentLength(bytes.length);
        // Set the crypto instruction file header
        metadata.addUserMetadata(Headers.CRYPTO_INSTRUCTION_FILE, "");
        // Update the instruction request
        req.setMetadata(metadata);
        req.setInputStream(new ByteArrayInputStream(bytes));
        // the file attribute in the request is always null before calling this
        // routine
        return req;
    }

    protected final PutObjectRequest createInstructionPutRequest(String bucketName, String key,
            ContentCryptoMaterial cekMaterial) {
        byte[] bytes = cekMaterial.toJsonString().getBytes(UTF8);
        InputStream is = new ByteArrayInputStream(bytes);
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(bytes.length);
        metadata.addUserMetadata(Headers.CRYPTO_INSTRUCTION_FILE, "");
        InstructionFileId ifileId = new COSObjectId(bucketName, key).instructionFileId();
        return new PutObjectRequest(ifileId.getBucket(), ifileId.getKey(), is, metadata);
    }

    /**
     * Checks if the the crypto scheme used in the given content crypto material is allowed to be
     * used in this crypto module. Default is no-op. Subclass may override.
     *
     * @throws SecurityException if the crypto scheme used in the given content crypto material is
     *         not allowed in this crypto module.
     */
    protected void securityCheck(ContentCryptoMaterial cekMaterial, COSObjectWrapper retrieved) {}

    /**
     * Retrieves an instruction file from COS; or null if no instruction file is found.
     *
     * @param cosObjectId the COS object id (not the instruction file id)
     * @param instFileSuffix suffix of the instruction file to be retrieved; or null to use the
     *        default suffix.
     * @return an instruction file, or null if no instruction file is found.
     */
    final COSObjectWrapper fetchInstructionFile(COSObjectId cosObjectId, String instFileSuffix) {
        try {
            COSObject o = cos.getObject(createInstructionGetRequest(cosObjectId, instFileSuffix));
            return o == null ? null : new COSObjectWrapper(o, cosObjectId);
        } catch (CosServiceException e) {
            // If no instruction file is found, log a debug message, and return
            // null.
            if (log.isDebugEnabled()) {
                log.debug("Unable to retrieve instruction file : " + e.getMessage());
            }
            return null;
        }
    }

    @Override
    public final PutObjectResult putInstructionFileSecurely(PutInstructionFileRequest req) {
        final COSObjectId id = req.getCOSObjectId();
        final GetObjectRequest getreq = new GetObjectRequest(id);
        // Get the object from cos
        final COSObject retrieved = cos.getObject(getreq);
        // We only need the meta-data already retrieved, not the data stream.
        // So close it immediately to prevent resource leakage.
        IOUtils.closeQuietly(retrieved, log);
        if (retrieved == null) {
            throw new IllegalArgumentException(
                    "The specified COS object (" + id + ") doesn't exist.");
        }
        COSObjectWrapper wrapped = new COSObjectWrapper(retrieved, id);
        try {
            final ContentCryptoMaterial origCCM = contentCryptoMaterialOf(wrapped);
            securityCheck(origCCM, wrapped);
            // Re-ecnrypt the CEK in a new content crypto material
            final EncryptionMaterials newKEK = req.getEncryptionMaterials();
            final ContentCryptoMaterial newCCM;
            if (newKEK == null) {
                newCCM = origCCM.recreate(req.getMaterialsDescription(), this.kekMaterialsProvider,
                        cryptoScheme, cryptoConfig.getCryptoProvider(), kms, req);
            } else {
                newCCM = origCCM.recreate(newKEK, this.kekMaterialsProvider, cryptoScheme,
                        cryptoConfig.getCryptoProvider(), kms, req);
            }
            PutObjectRequest putInstFileRequest = req.createPutObjectRequest(retrieved);
            // Put the new instruction file into COS
            return cos.putObject(updateInstructionPutRequest(putInstFileRequest, newCCM));
        } catch (RuntimeException ex) {
            // If we're unable to set up the decryption, make sure we close the
            // HTTP connection
            IOUtils.closeQuietly(retrieved, log);
            throw ex;
        } catch (Error error) {
            IOUtils.closeQuietly(retrieved, log);
            throw error;
        }
    }

    /**
     * Returns the content crypto material of an existing COS object.
     *
     * @param cosObjWrap an existing COS object (wrapper)
     *
     * @return a non-null content crypto material.
     */
    private ContentCryptoMaterial contentCryptoMaterialOf(COSObjectWrapper cosObjWrap) {
        // Check if encryption info is in object metadata
        if (cosObjWrap.hasEncryptionInfo()) {
            return ContentCryptoMaterial.fromObjectMetadata(cosObjWrap.getObjectMetadata(),
                    kekMaterialsProvider, cryptoConfig.getCryptoProvider(), false, // existing CEK
                                                                                   // not
                                                                                   // necessarily
                                                                                   // key-wrapped
                    kms);
        }
        COSObjectWrapper orig_ifile = fetchInstructionFile(cosObjWrap.getCOSObjectId(), null);
        if (orig_ifile == null) {
            throw new IllegalArgumentException("COS object is not encrypted: " + cosObjWrap);
        }
        String json = orig_ifile.toJsonString();
        return ccmFromJson(json);
    }

    private ContentCryptoMaterial ccmFromJson(String json) {
        @SuppressWarnings("unchecked")
        Map<String, String> instruction =
                Collections.unmodifiableMap(Jackson.fromJsonString(json, Map.class));
        return ContentCryptoMaterial.fromInstructionFile(instruction, kekMaterialsProvider,
                cryptoConfig.getCryptoProvider(), false, // existing CEK not necessarily key-wrapped
                kms);
    }

    /**
     * Creates a get object request for an instruction file using the default instruction file
     * suffix.
     *
     * @param id an COS object id (not the instruction file id)
     * @return A get request to retrieve an instruction file from COS.
     */
    final GetObjectRequest createInstructionGetRequest(COSObjectId id) {
        return createInstructionGetRequest(id, null);
    }

    /**
     * Creates and return a get object request for an instruction file.
     *
     * @param cosobjectId an COS object id (not the instruction file id)
     * @param instFileSuffix suffix of the specific instruction file to be used, or null if the
     *        default instruction file is to be used.
     */
    final GetObjectRequest createInstructionGetRequest(COSObjectId cosobjectId,
            String instFileSuffix) {
        return new GetObjectRequest(cosobjectId.instructionFileId(instFileSuffix));
    }

    static long[] getAdjustedCryptoRange(long[] range) {
        // If range is invalid, then return null.
        if (range == null || range[0] > range[1]) {
            return null;
        }
        long[] adjustedCryptoRange = new long[2];
        adjustedCryptoRange[0] = getCipherBlockLowerBound(range[0]);
        adjustedCryptoRange[1] = getCipherBlockUpperBound(range[1]);
        return adjustedCryptoRange;
    }

    private static long getCipherBlockLowerBound(long leftmostBytePosition) {
        long cipherBlockSize = JceEncryptionConstants.SYMMETRIC_CIPHER_BLOCK_SIZE;
        long offset = leftmostBytePosition % cipherBlockSize;
        long lowerBound = leftmostBytePosition - offset - cipherBlockSize;
        return lowerBound < 0 ? 0 : lowerBound;
    }

    /**
     * Takes the position of the rightmost desired byte of a user specified range and returns the
     * position of the end of the following cipher block; or {@value Long#MAX_VALUE} if the
     * resultant position has a value that exceeds {@value Long#MAX_VALUE}.
     */
    private static long getCipherBlockUpperBound(final long rightmostBytePosition) {
        long cipherBlockSize = JceEncryptionConstants.SYMMETRIC_CIPHER_BLOCK_SIZE;
        long offset = cipherBlockSize - (rightmostBytePosition % cipherBlockSize);
        long upperBound = rightmostBytePosition + offset + cipherBlockSize;
        return upperBound < 0 ? Long.MAX_VALUE : upperBound;
    }
}
