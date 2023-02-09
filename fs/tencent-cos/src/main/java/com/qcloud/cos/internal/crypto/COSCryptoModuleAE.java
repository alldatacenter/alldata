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

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Map;

import com.qcloud.cos.auth.COSCredentialsProvider;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.internal.COSDirect;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectId;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.model.EncryptedGetObjectRequest;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.InitiateMultipartUploadRequest;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.UploadPartRequest;
import com.qcloud.cos.utils.IOUtils;
import com.qcloud.cos.utils.Jackson;

public class COSCryptoModuleAE extends COSCryptoModuleBase {

    static {
        // Enable bouncy castle if available
        CryptoRuntime.enableBouncyCastle();
    }

    public COSCryptoModuleAE(COSDirect cos, COSCredentialsProvider credentialsProvider,
            EncryptionMaterialsProvider kekMaterialsProvider, CryptoConfiguration cryptoConfig) {
        this(null, cos, credentialsProvider, kekMaterialsProvider, cryptoConfig);
    }

    public COSCryptoModuleAE(QCLOUDKMS kms, COSDirect cos,
            COSCredentialsProvider credentialsProvider,
            EncryptionMaterialsProvider kekMaterialsProvider, CryptoConfiguration cryptoConfig) {
        super(kms, cos, credentialsProvider, kekMaterialsProvider, cryptoConfig);
    }

    /**
     * Returns true if a strict encryption mode is in use in the current crypto module; false
     * otherwise.
     */
    protected boolean isStrict() {
        return false;
    }

    @Override
    public COSObject getObjectSecurely(GetObjectRequest req) {
        // Adjust the crypto range to retrieve all of the cipher blocks needed to contain the user's
        // desired
        // range of bytes.
        long[] desiredRange = req.getRange();
        if (isStrict() && (desiredRange != null))
            throw new SecurityException(
                    "Range get and getting a part are not allowed in strict crypto mode");
        long[] adjustedCryptoRange = getAdjustedCryptoRange(desiredRange);
        if (adjustedCryptoRange != null)
            req.setRange(adjustedCryptoRange[0], adjustedCryptoRange[1]);
        // Get the object from COS
        COSObject retrieved = cos.getObject(req);
        // If the caller has specified constraints, it's possible that super.getObject(...)
        // would return null, so we simply return null as well.
        if (retrieved == null)
            return null;
        String suffix = null;
        if (req instanceof EncryptedGetObjectRequest) {
            EncryptedGetObjectRequest ereq = (EncryptedGetObjectRequest) req;
            suffix = ereq.getInstructionFileSuffix();
        }
        try {
            return suffix == null || suffix.trim().isEmpty()
                    ? decipher(req, desiredRange, adjustedCryptoRange, retrieved)
                    : decipherWithInstFileSuffix(req, desiredRange, adjustedCryptoRange, retrieved,
                            suffix);
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

    private COSObject decipher(GetObjectRequest req, long[] desiredRange, long[] cryptoRange,
            COSObject retrieved) {
        COSObjectWrapper wrapped = new COSObjectWrapper(retrieved, req.getCOSObjectId());
        // Check if encryption info is in object metadata
        if (wrapped.hasEncryptionInfo())
            return decipherWithMetadata(req, desiredRange, cryptoRange, wrapped);
        // Check if encrypted info is in an instruction file
        COSObjectWrapper ifile = fetchInstructionFile(req.getCOSObjectId(), null);
        if (ifile != null) {
            try {
                return decipherWithInstructionFile(req, desiredRange, cryptoRange, wrapped, ifile);
            } finally {
                IOUtils.closeQuietly(ifile, log);
            }
        }

        if (isStrict() || !cryptoConfig.isIgnoreMissingInstructionFile()) {
            IOUtils.closeQuietly(wrapped, log);
            throw new SecurityException(
                    "Instruction file not found for COS object with bucket name: "
                            + retrieved.getBucketName() + ", key: " + retrieved.getKey());
        }
        // To keep backward compatible:
        // ignore the missing instruction file and treat the object as un-encrypted.
        log.warn(String.format(
                "Unable to detect encryption information for object '%s' in bucket '%s'. "
                        + "Returning object without decryption.",
                retrieved.getKey(), retrieved.getBucketName()));
        // Adjust the output to the desired range of bytes.
        COSObjectWrapper adjusted = adjustToDesiredRange(wrapped, desiredRange, null);
        return adjusted.getCOSObject();
    }

    /**
     * Same as {@link #decipher(GetObjectRequest, long[], long[], COSObject)} but makes use of an
     * instruction file with the specified suffix.
     * 
     * @param instFileSuffix never null or empty (which is assumed to have been sanitized upstream.)
     */
    private COSObject decipherWithInstFileSuffix(GetObjectRequest req, long[] desiredRange,
            long[] cryptoRange, COSObject retrieved, String instFileSuffix) {
        final COSObjectId id = req.getCOSObjectId();
        // Check if encrypted info is in an instruction file
        final COSObjectWrapper ifile = fetchInstructionFile(id, instFileSuffix);
        if (ifile == null) {
            throw new CosClientException("Instruction file with suffix " + instFileSuffix
                    + " is not found for " + retrieved);
        }
        try {
            return decipherWithInstructionFile(req, desiredRange, cryptoRange,
                    new COSObjectWrapper(retrieved, id), ifile);
        } finally {
            IOUtils.closeQuietly(ifile, log);
        }
    }

    private COSObject decipherWithInstructionFile(GetObjectRequest req, long[] desiredRange,
            long[] cryptoRange, COSObjectWrapper retrieved, COSObjectWrapper instructionFile) {
        boolean keyWrapExpected = isStrict();
        if (req instanceof EncryptedGetObjectRequest) {
            EncryptedGetObjectRequest ereq = (EncryptedGetObjectRequest) req;
            if (!keyWrapExpected)
                keyWrapExpected = ereq.isKeyWrapExpected();
        }
        String json = instructionFile.toJsonString();
        @SuppressWarnings("unchecked")
        Map<String, String> matdesc =
                Collections.unmodifiableMap(Jackson.fromJsonString(json, Map.class));
        ContentCryptoMaterial cekMaterial = ContentCryptoMaterial.fromInstructionFile(matdesc,
                kekMaterialsProvider, cryptoConfig.getCryptoProvider(), cryptoRange, // range is
                                                                                     // sometimes
                                                                                     // necessary to
                                                                                     // compute the
                                                                                     // adjusted IV
                keyWrapExpected, kms);
        securityCheck(cekMaterial, retrieved);
        COSObjectWrapper decrypted = decrypt(retrieved, cekMaterial, cryptoRange);
        // Adjust the output to the desired range of bytes.
        COSObjectWrapper adjusted = adjustToDesiredRange(decrypted, desiredRange, matdesc);
        return adjusted.getCOSObject();
    }

    private COSObject decipherWithMetadata(GetObjectRequest req, long[] desiredRange,
            long[] cryptoRange, COSObjectWrapper retrieved) {
        boolean keyWrapExpected = isStrict();
        if (req instanceof EncryptedGetObjectRequest) {
            EncryptedGetObjectRequest ereq = (EncryptedGetObjectRequest) req;
            if (!keyWrapExpected)
                keyWrapExpected = ereq.isKeyWrapExpected();
        }
        ContentCryptoMaterial cekMaterial =
                ContentCryptoMaterial.fromObjectMetadata(retrieved.getObjectMetadata(),
                        kekMaterialsProvider, cryptoConfig.getCryptoProvider(),
                        // range is sometimes necessary to compute the adjusted IV
                        cryptoRange, keyWrapExpected, kms);
        securityCheck(cekMaterial, retrieved);
        COSObjectWrapper decrypted = decrypt(retrieved, cekMaterial, cryptoRange);
        // Adjust the output to the desired range of bytes.
        COSObjectWrapper adjusted = adjustToDesiredRange(decrypted, desiredRange, null);
        return adjusted.getCOSObject();
    }

    /**
     * Adjusts the retrieved COSObject so that the object contents contain only the range of bytes
     * desired by the user. Since encrypted contents can only be retrieved in CIPHER_BLOCK_SIZE (16
     * bytes) chunks, the COSObject potentially contains more bytes than desired, so this method
     * adjusts the contents range.
     *
     * @param cosObject The COSObject retrieved from COS that could possibly contain more bytes than
     *        desired by the user.
     * @param range A two-element array of longs corresponding to the start and finish (inclusive)
     *        of a desired range of bytes.
     * @param instruction Instruction file in JSON or null if no instruction file is involved
     * @return The COSObject with adjusted object contents containing only the range desired by the
     *         user. If the range specified is invalid, then the COSObject is returned without any
     *         modifications.
     */
    protected final COSObjectWrapper adjustToDesiredRange(COSObjectWrapper cosObject, long[] range,
            Map<String, String> instruction) {
        if (range == null)
            return cosObject;
        // Figure out the original encryption scheme used, which can be
        // different from the crypto scheme used for decryption.
        ContentCryptoScheme encryptionScheme = cosObject.encryptionSchemeOf(instruction);
        // range get on data encrypted using AES_GCM
        final long instanceLen = cosObject.getObjectMetadata().getInstanceLength();
        final long maxOffset = instanceLen - encryptionScheme.getTagLengthInBits() / 8 - 1;
        if (range[1] > maxOffset) {
            range[1] = maxOffset;
            if (range[0] > range[1]) {
                // Return empty content
                // First let's close the existing input stream to avoid resource
                // leakage
                IOUtils.closeQuietly(cosObject.getObjectContent(), log);
                cosObject.setObjectContent(new ByteArrayInputStream(new byte[0]));
                return cosObject;
            }
        }
        if (range[0] > range[1]) {
            // Make no modifications if range is invalid.
            return cosObject;
        }
        try {
            COSObjectInputStream objectContent = cosObject.getObjectContent();
            InputStream adjustedRangeContents =
                    new AdjustedRangeInputStream(objectContent, range[0], range[1]);
            cosObject.setObjectContent(new COSObjectInputStream(adjustedRangeContents,
                    objectContent.getHttpRequest()));
            return cosObject;
        } catch (IOException e) {
            throw new CosClientException(
                    "Error adjusting output to desired byte range: " + e.getMessage());
        }
    }

    @Override
    public ObjectMetadata getObjectSecurely(GetObjectRequest getObjectRequest,
            File destinationFile) {
        assertParameterNotNull(destinationFile,
                "The destination file parameter must be specified when downloading an object directly to a file");

        COSObject cosObject = getObjectSecurely(getObjectRequest);
        // getObject can return null if constraints were specified but not met
        if (cosObject == null)
            return null;

        OutputStream outputStream = null;
        try {
            outputStream = new BufferedOutputStream(new FileOutputStream(destinationFile));
            byte[] buffer = new byte[1024 * 10];
            int bytesRead;
            while ((bytesRead = cosObject.getObjectContent().read(buffer)) > -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            throw new CosClientException(
                    "Unable to store object contents to disk: " + e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(outputStream, log);
            IOUtils.closeQuietly(cosObject.getObjectContent(), log);
        }

        /*
         * Unlike the standard Client, the Encryption Client does not do an MD5 check
         * here because the contents stored in COS and the contents we just retrieved are different.  In
         * COS, the stored contents are encrypted, and locally, the retrieved contents are decrypted.
         */

        return cosObject.getObjectMetadata();
    }

    @Override
    final MultipartUploadCryptoContext newUploadContext(InitiateMultipartUploadRequest req,
            ContentCryptoMaterial cekMaterial) {
        return new MultipartUploadCryptoContext(req.getBucketName(), req.getKey(), cekMaterial);
    }

    //// specific overrides for uploading parts.
    @Override
    final CipherLite cipherLiteForNextPart(MultipartUploadCryptoContext uploadContext) {
        return uploadContext.getCipherLite();
    }

    @Override
    final long computeLastPartSize(UploadPartRequest req) {
        return req.getPartSize() + (contentCryptoScheme.getTagLengthInBits() / 8);
    }

    /*
     * Private helper methods
     */

    /**
     * Returns an updated object where the object content input stream contains the decrypted
     * contents.
     *
     * @param wrapper The object whose contents are to be decrypted.
     * @param cekMaterial The instruction that will be used to decrypt the object data.
     * @return The updated object where the object content input stream contains the decrypted
     *         contents.
     */
    private COSObjectWrapper decrypt(COSObjectWrapper wrapper, ContentCryptoMaterial cekMaterial,
            long[] range) {
        COSObjectInputStream objectContent = wrapper.getObjectContent();
        wrapper.setObjectContent(new COSObjectInputStream(new CipherLiteInputStream(objectContent,
                cekMaterial.getCipherLite(), DEFAULT_BUFFER_SIZE), objectContent.getHttpRequest()));
        return wrapper;
    }

    /**
     * Asserts that the specified parameter value is not null and if it is, throws an
     * IllegalArgumentException with the specified error message.
     *
     * @param parameterValue The parameter value being checked.
     * @param errorMessage The error message to include in the IllegalArgumentException if the
     *        specified parameter is null.
     */
    private void assertParameterNotNull(Object parameterValue, String errorMessage) {
        if (parameterValue == null)
            throw new IllegalArgumentException(errorMessage);
    }

    @Override
    protected long ciphertextLength(long originalContentLength) {
        // Add 16 bytes for the 128-bit tag length using AES/GCM
        return originalContentLength + contentCryptoScheme.getTagLengthInBits() / 8;
    }

}
