/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.aliyun.oss.crypto;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.CheckedInputStream;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import com.aliyun.oss.model.*;
import org.apache.http.protocol.HTTP;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSSEncryptionClient;
import com.aliyun.oss.common.utils.BinaryUtil;
import com.aliyun.oss.common.utils.IOUtils;
import com.aliyun.oss.internal.Mimetypes;
import com.aliyun.oss.internal.OSSHeaders;
import com.aliyun.oss.internal.OSSUtils;
import static com.aliyun.oss.common.utils.CodingUtils.assertParameterNotNull;
import static com.aliyun.oss.common.utils.IOUtils.safeClose;
import static com.aliyun.oss.common.utils.LogUtils.logException;
import static com.aliyun.oss.internal.OSSUtils.OSS_RESOURCE_MANAGER;

public abstract class CryptoModuleBase implements CryptoModule {
    protected static final int DEFAULT_BUFFER_SIZE = 1024 * 2;
    protected final EncryptionMaterials encryptionMaterials;
    protected final CryptoScheme contentCryptoScheme;
    protected final CryptoConfiguration cryptoConfig;
    protected final OSSDirect ossDirect;
    protected final String encryptionClientUserAgent;

    protected CryptoModuleBase(OSSDirect ossDirect,
                                 EncryptionMaterials encryptionMaterials,
                                 CryptoConfiguration cryptoConfig) {
        this.encryptionMaterials = encryptionMaterials;
        this.ossDirect = ossDirect;
        this.cryptoConfig = cryptoConfig;
        this.contentCryptoScheme = getCryptoScheme(cryptoConfig.getContentCryptoMode());
        encryptionClientUserAgent = this.ossDirect.getInnerClientConfiguration().getUserAgent()
                + OSSEncryptionClient.USER_AGENT_SUFFIX;
    }

    private final static CryptoScheme getCryptoScheme(ContentCryptoMode contentCryptoMode) {
        switch (contentCryptoMode) {
        case AES_CTR_MODE:
        default:
            return CryptoScheme.AES_CTR;
        }
    }

    abstract byte[] generateIV();
    abstract CryptoCipher createCryptoCipherFromContentMaterial(ContentCryptoMaterial cekMaterial,
                                                       int cipherMode, long[] cryptoRange, long skipBlock);

    /**
     * Puts the object with data encrypted.
     * 
     * @param req
     *          The put object request.
     * @return the result of the request.
     */
    @Override
    public PutObjectResult putObjectSecurely(PutObjectRequest req) {
        // Update User-Agent.
        setUserAgent(req, encryptionClientUserAgent);

        // Build content crypto material.
        ContentCryptoMaterial cekMaterial = buildContentCryptoMaterials();

        // Update the metadata
        ObjectMetadata meta = updateMetadataWithContentCryptoMaterial(req.getMetadata(), req.getFile(), cekMaterial);
        req.setMetadata(meta);

        // Wraps the object data with a cipher input stream
        final File fileOrig = req.getFile();
        final InputStream isOrig = req.getInputStream();
        PutObjectRequest wrappedReq = wrapPutRequestWithCipher(req, cekMaterial);

        // Put the encrypted object by the oss client
        try {
            return ossDirect.putObject(wrappedReq);
        } finally {
            safeCloseSource(wrappedReq.getInputStream());
            req.setFile(fileOrig);
            req.setInputStream(isOrig);
        }
    }


    // Returns the given {@link PutObjectRequest} instance but has the content as
    // input stream wrapped with a cipher, and configured with some meta data and
    // user metadata.
    protected final PutObjectRequest wrapPutRequestWithCipher(final PutObjectRequest request,
            ContentCryptoMaterial cekMaterial) {
        // Create a new metadata object if there is no metadata already.
        ObjectMetadata metadata = request.getMetadata();
        if (metadata == null) {
            metadata = new ObjectMetadata();
        }

        // update content md5 and length headers.
        updateContentMd5(request, metadata);
        updateContentLength(request, metadata);

        // Create content crypto cipher.
        CryptoCipher cryptoCipher = createCryptoCipherFromContentMaterial(cekMaterial, Cipher.ENCRYPT_MODE, null, 0);

        // Treat all encryption requests as input stream upload requests.
        request.setInputStream(newOSSCryptoCipherInputStream(request, cryptoCipher));

        request.setFile(null);
        return request;
    }

    private void updateContentMd5(final PutObjectRequest request, final ObjectMetadata metadata) {
        if (metadata.getContentMD5() != null) {
            metadata.addUserMetadata(CryptoHeaders.CRYPTO_UNENCRYPTION_CONTENT_MD5, metadata.getContentMD5());
            metadata.removeHeader(OSSHeaders.CONTENT_MD5);
        }

        Map<String, String> headers = request.getHeaders();
        if (headers.containsKey(OSSHeaders.CONTENT_MD5)) {
            metadata.addUserMetadata(CryptoHeaders.CRYPTO_UNENCRYPTION_CONTENT_MD5, headers.get(OSSHeaders.CONTENT_MD5));
            headers.remove(OSSHeaders.CONTENT_MD5);
        }

        request.setMetadata(metadata);
    }

    private void updateContentLength(final PutObjectRequest request, final ObjectMetadata metadata) {
        final long plaintextLength = plaintextLength(request, metadata);
        if (plaintextLength >= 0) {
            metadata.addUserMetadata(CryptoHeaders.CRYPTO_UNENCRYPTION_CONTENT_LENGTH, Long.toString(plaintextLength));
            metadata.setContentLength(plaintextLength);
        }

        Map<String, String> headers = request.getHeaders();
        if (headers.containsKey(OSSHeaders.CONTENT_LENGTH)) {
            metadata.addUserMetadata(CryptoHeaders.CRYPTO_UNENCRYPTION_CONTENT_LENGTH, headers.get(OSSHeaders.CONTENT_LENGTH));
        }

        request.setMetadata(metadata);
    }

    /**
     * Checks there an encryption info in the metadata.
     *
     * @param metadata
     *           the object metadata.
     *
     * @return True if has encryption info; False if not.
     */
    public static boolean hasEncryptionInfo(ObjectMetadata metadata) {
        Map<String, String> userMeta = metadata.getUserMetadata();
        return userMeta != null && userMeta.containsKey(CryptoHeaders.CRYPTO_KEY)
                && userMeta.containsKey(CryptoHeaders.CRYPTO_IV);
    }

    /**
     * Gets the object in OSS, if it was an encrypted object then decrypt it and
     * return the result, otherwise return the object directly.
     *
     * @param req The {@link GetObjectRequest} instance.
     * @return  The {@link OSSObject} instance.
     */
    @Override
    public OSSObject getObjectSecurely(GetObjectRequest req) {
        // Update User-Agent.
        setUserAgent(req, encryptionClientUserAgent);
        
        // Adjust range-get
        long[] desiredRange = req.getRange();
        long[] adjustedCryptoRange = getAdjustedCryptoRange(desiredRange);
        if (adjustedCryptoRange != null) {
            req.setRange(adjustedCryptoRange[0], adjustedCryptoRange[1]);
        }

        // Get the object from OSS
        OSSObject retrieved = ossDirect.getObject(req);

        // Recheck range-get
        String contentRange = (String) retrieved.getObjectMetadata().getRawMetadata().get("Content-Range");
        if (contentRange == null && adjustedCryptoRange != null) {
            desiredRange[0] = 0;
            desiredRange[1] = retrieved.getObjectMetadata().getContentLength() - 1;
            adjustedCryptoRange = desiredRange.clone();
        }

        // Convert OSSObject content with cipher insteam
        try {
            if (hasEncryptionInfo(retrieved.getObjectMetadata())) {
                return decipherWithMetadata(req, desiredRange, adjustedCryptoRange, retrieved);
            }
            OSSObject adjustedOSSObject = adjustToDesiredRange(retrieved, desiredRange);
            return adjustedOSSObject;
        } catch (Exception e) {
            safeCloseSource(retrieved);
            throw new ClientException(e);
        }
    }

    // Decrypt the encypted object by the metadata achieved.
    protected OSSObject decipherWithMetadata(GetObjectRequest req,
            long[] desiredRange,
            long[] cryptoRange, OSSObject retrieved) {

        // Create ContentCryptoMaterial by parse metadata.
        ContentCryptoMaterial cekMaterial = createContentMaterialFromMetadata(retrieved.getObjectMetadata());

        // Create crypto cipher by contentCryptoMaterial
        CryptoCipher cryptoCipher = createCryptoCipherFromContentMaterial(cekMaterial, Cipher.DECRYPT_MODE, cryptoRange,
                0);

        // Wrap retrieved object with cipherInputStream.
        InputStream objectContent = retrieved.getObjectContent();
        retrieved.setObjectContent(
                new CipherInputStream(objectContent,
                    cryptoCipher,
                    DEFAULT_BUFFER_SIZE));

        // Adjust the output to the desired range of bytes.
        OSSObject adjusted = adjustToDesiredRange(retrieved, desiredRange);
        return adjusted;
    }

    protected void safeCloseSource(Closeable is) {
        if (is != null) {
            try {
                is.close();
            } catch (IOException ex) {
            }
        }
    }

    // Adjusts the retrieved OSSObject so that the object contents contain only the
    // range of bytes desired by the user. Since encrypted contents can only be
    // retrieved in CIPHER_BLOCK_SIZE (16 bytes) chunks, the OSSObject potentially
    // contains more bytes than desired, so this method adjusts the contents range.
    protected final OSSObject adjustToDesiredRange(OSSObject OSSobject, long[] range) {
        if (range == null)
            return OSSobject;

        try {
            InputStream objectContent = OSSobject.getObjectContent();
            InputStream adjustedRangeContents = new AdjustedRangeInputStream(objectContent, range[0], range[1]);
            OSSobject.setObjectContent(adjustedRangeContents);
            return OSSobject;
        } catch (IOException e) {
            throw new ClientException("Error adjusting output to desired byte range: " + e.getMessage());
        }
    }

    private void checkMultipartContext(MultipartUploadCryptoContext context) {
        if (context == null) {
            throw new IllegalArgumentException("MultipartUploadCryptoContext should not be null.");
        }

        if (0 != (context.getPartSize() % CryptoScheme.BLOCK_SIZE) || context.getPartSize() <= 0) {
            throw new IllegalArgumentException("MultipartUploadCryptoContext part size is not 16 bytes alignment.");
        }
    }

    /**
     * Gets the object in OSS and write it in a file, if it was an encrypted object
     * then decrypt it, otherwise wirte the object directly.
     * @param getObjectRequest The {@link GetObjectRequest} instance.
     * @param file The {@link File} instance to write into.
     * @return  The {@link ObjectMetadata} instance.
     */
    @Override
    public ObjectMetadata getObjectSecurely(GetObjectRequest getObjectRequest, File file) {
        assertParameterNotNull(file, "file");
        OSSObject ossObject = getObjectSecurely(getObjectRequest);
        OutputStream outputStream = null;
        try {
            outputStream = new BufferedOutputStream(new FileOutputStream(file));
            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = IOUtils.readNBytes(ossObject.getObjectContent(), buffer, 0, buffer.length)) > 0) {
                outputStream.write(buffer, 0, bytesRead);
            }

            if (ossDirect.getInnerClientConfiguration().isCrcCheckEnabled() && getObjectRequest.getRange() == null) {
                Long clientCRC = null;
                InputStream contentInputStream = ossObject.getObjectContent();
                if (contentInputStream instanceof CipherInputStream) {
                    InputStream subStream = ((CipherInputStream) contentInputStream).getDelegateStream();
                    if (subStream instanceof CheckedInputStream){
                        clientCRC = ((CheckedInputStream) subStream).getChecksum().getValue();
                    }
                }
                else {
                    clientCRC = IOUtils.getCRCValue(ossObject.getObjectContent());
                }
                OSSUtils.checkChecksum(clientCRC, ossObject.getServerCRC(), ossObject.getRequestId());
            }
            return ossObject.getObjectMetadata();
        } catch (IOException ex) {
            logException("Cannot read object content stream: ", ex);
            throw new ClientException(OSS_RESOURCE_MANAGER.getString("CannotReadContentStream"), ex);
        } finally {
            safeClose(outputStream);
            safeClose(ossObject.getObjectContent());
        }
    }

    /**
     * Initiates the multipart upload request, and bulid the crypto context.
     * @param req
     *         The {@link InitiateMultipartUploadRequest} instance.
     * @param context
     *         The multi part crypto context contains the content crypto materials and
     *         upload information, it should created on the outside with part-size and data-size set done, the 
     *         content crypto materials and other upload information will be filled after initiate request done.
     * @return  The {@link InitiateMultipartUploadResult} instance.
     */
    @Override
    public InitiateMultipartUploadResult initiateMultipartUploadSecurely(InitiateMultipartUploadRequest req,
            MultipartUploadCryptoContext context) {
        checkMultipartContext(context);

        // Update User-Agent.
        setUserAgent(req, encryptionClientUserAgent);

        // Get content crypto material.
        ContentCryptoMaterial cekMaterial = buildContentCryptoMaterials();

        ObjectMetadata metadata = req.getObjectMetadata();
        if (metadata == null) {
            metadata = new ObjectMetadata();
        }

        // Store encryption info in metadata
        metadata = updateMetadataWithContentCryptoMaterial(metadata, null, cekMaterial);
        metadata = updateMetadataWithUploadContext(metadata, context);
        req.setObjectMetadata(metadata);

        // Fill context
        InitiateMultipartUploadResult result = ossDirect.initiateMultipartUpload(req);
        context.setUploadId(result.getUploadId());
        context.setContentCryptoMaterial(cekMaterial);

        return result;
    }

    /**
     * Uploads the part secured.
     * @param req
     *         The {@link UploadPartRequest} instance.
     * @param context
     *         The multi part crypto context contains the content crypto materials and
     *         upload information, it should created on the outside with part-size and data-size set done, the
     *         content crypto materials and other upload information will be filled after initiate request done.
     * @return  The {@link UploadPartResult} instance.
     */
    @Override
    public UploadPartResult uploadPartSecurely(UploadPartRequest req, MultipartUploadCryptoContext context) {
        final UploadPartResult result;

        // Check partsize and context
        checkMultipartContext(context);
        if (!context.getUploadId().equals(req.getUploadId())) {
            throw new ClientException("The multipartUploadCryptoContextcontext input upload id is invalid."
                    + "context uploadid:" + context.getUploadId() + ",uploadRequest uploadid:" + req.getUploadId());
        }

        // Update User-Agent.
        setUserAgent(req, encryptionClientUserAgent);

        // Create CryptoCipher
        long offset = context.getPartSize() * (req.getPartNumber() - 1);
        long skipBlock = offset / CryptoScheme.BLOCK_SIZE;
        CryptoCipher cryptoCipher = createCryptoCipherFromContentMaterial(context.getContentCryptoMaterial(),
                Cipher.ENCRYPT_MODE, null, skipBlock);
        // Wrap InputStram to CipherInputStream
        final InputStream isOrig = req.getInputStream();
        CipherInputStream isCurr = null;
        try {
            isCurr = new RenewableCipherInputStream(isOrig, cryptoCipher, DEFAULT_BUFFER_SIZE);
            req.setInputStream(isCurr);
            result = ossDirect.uploadPart(req);
        } finally {
            safeCloseSource(isCurr);
            req.setInputStream(isOrig);
        }
        return result;
    }

    // Wraps the inputStream with an crypto cipher.
    private CipherInputStream newOSSCryptoCipherInputStream(PutObjectRequest req, CryptoCipher cryptoCipher) {
        final File fileOrig = req.getFile();
        final InputStream isOrig = req.getInputStream();
        InputStream isCurr = isOrig;
        try {
            if (fileOrig != null) {
                isCurr = new FileInputStream(fileOrig);
            }
            return new RenewableCipherInputStream(isCurr, cryptoCipher, DEFAULT_BUFFER_SIZE);
        } catch (Exception e) {
            safeCloseSource(isCurr);
            req.setFile(fileOrig);
            req.setInputStream(isOrig);
            throw new ClientException("Unable to create cipher input stream." + e.getMessage(), e);
        }
    }

    // Returns the plaintext length from the request and metadata; or -1 if unknown.
    protected final long plaintextLength(PutObjectRequest request, ObjectMetadata metadata) {
        if (request.getFile() != null) {
            return request.getFile().length();
        } else if (request.getInputStream() != null && metadata.getRawMetadata().get("Content-Length") != null) {
            return metadata.getContentLength();
        }
        return -1;
    }

    // Adjustes the range-get start offset to allgn with cipher block.
    long[] getAdjustedCryptoRange(long[] range) {
        if (range == null) {
            return null;
        }

        if ((range[0] > range[1]) || (range[0] < 0) || (range[1] <= 0)) {
            throw new ClientException("Your input get-range is illegal. + range:" + range[0] + "~" + range[1]);
        }

        long[] adjustedCryptoRange = new long[2];
        adjustedCryptoRange[0] = getCipherBlockLowerBound(range[0]);
        adjustedCryptoRange[1] = range[1];
        return adjustedCryptoRange;
    }

    private long getCipherBlockLowerBound(long leftmostBytePosition) {
        long cipherBlockSize = CryptoScheme.BLOCK_SIZE;
        long offset = leftmostBytePosition % cipherBlockSize;
        long lowerBound = leftmostBytePosition - offset;
        return lowerBound;
    }

    // Build a new content crypto material read-only.
    protected final ContentCryptoMaterial buildContentCryptoMaterials() {
        // Generate random CEK IV
        byte[] iv = generateIV();
        SecretKey cek = generateCEK();

        // Build content crypto Materials by encryptionMaterials.
        ContentCryptoMaterialRW contentMaterialRW = new ContentCryptoMaterialRW();
        contentMaterialRW.setIV(iv);
        contentMaterialRW.setCEK(cek);
        contentMaterialRW.setContentCryptoAlgorithm(contentCryptoScheme.getContentChiperAlgorithm());
        encryptionMaterials.encryptCEK(contentMaterialRW);

        return contentMaterialRW;
    }

    // Add the upload part info into metadata.
    protected final ObjectMetadata updateMetadataWithUploadContext(ObjectMetadata metadata,
            MultipartUploadCryptoContext context) {
        if (metadata == null) {
            metadata = new ObjectMetadata();
        }
        metadata.addUserMetadata(CryptoHeaders.CRYPTO_PART_SIZE, String.valueOf(context.getPartSize()));
        if (context.getDataSize() > 0) {
            metadata.addUserMetadata(CryptoHeaders.CRYPTO_DATA_SIZE, String.valueOf(context.getDataSize()));
        }
        return metadata;
    }

    // Storages the encrytion materials in the object metadata.
    protected final ObjectMetadata updateMetadataWithContentCryptoMaterial(ObjectMetadata metadata, File file,
            ContentCryptoMaterial contentCryptoMaterial) {
        if (metadata == null)
            metadata = new ObjectMetadata();
        if (file != null) {
            Mimetypes mimetypes = Mimetypes.getInstance();
            metadata.setContentType(mimetypes.getMimetype(file));
        }
        // Put the encrypted content encrypt key into the object meatadata
        byte[] encryptedCEK = contentCryptoMaterial.getEncryptedCEK();
        metadata.addUserMetadata(CryptoHeaders.CRYPTO_KEY, BinaryUtil.toBase64String(encryptedCEK));

        // Put the iv into the object metadata
        byte[] encryptedIV = contentCryptoMaterial.getEncryptedIV();
        metadata.addUserMetadata(CryptoHeaders.CRYPTO_IV, BinaryUtil.toBase64String(encryptedIV));

        // Put the content encrypt key algorithm into the object metadata
        String contentCryptoAlgo = contentCryptoMaterial.getContentCryptoAlgorithm();
        metadata.addUserMetadata(CryptoHeaders.CRYPTO_CEK_ALG, contentCryptoAlgo);

        // Put the key wrap algorithm into the object metadata
        String keyWrapAlgo = contentCryptoMaterial.getKeyWrapAlgorithm();
        metadata.addUserMetadata(CryptoHeaders.CRYPTO_WRAP_ALG, keyWrapAlgo);

        // Put the crypto description into the object metadata
        Map<String, String> materialDesc = contentCryptoMaterial.getMaterialsDescription();
        if (materialDesc != null && materialDesc.size() > 0) {
            JSONObject descJson = new JSONObject(materialDesc);
            String descStr = descJson.toString();
            metadata.addUserMetadata(CryptoHeaders.CRYPTO_MATDESC, descStr);
        }

        return metadata;
    }

    // Builds a new content crypto material for decrypting the object achieved.
    protected ContentCryptoMaterial createContentMaterialFromMetadata(ObjectMetadata meta) {
        Map<String, String> userMeta = meta.getUserMetadata();
        // Encrypted CEK and encrypted IV.
        String b64CEK = userMeta.get(CryptoHeaders.CRYPTO_KEY);
        String b64IV = userMeta.get(CryptoHeaders.CRYPTO_IV);
        if (b64CEK == null || b64IV == null) {
            throw new ClientException("Content encrypted key  or encrypted iv not found.");
        }
        byte[] encryptedCEK = BinaryUtil.fromBase64String(b64CEK);
        byte[] encryptedIV = BinaryUtil.fromBase64String(b64IV);

        // Key wrap algorithm
        final String keyWrapAlgo = userMeta.get(CryptoHeaders.CRYPTO_WRAP_ALG);
        if (keyWrapAlgo == null)
            throw new ClientException("Key wrap algorithm should not be null.");

        // CEK algorithm
        String cekAlgo = userMeta.get(CryptoHeaders.CRYPTO_CEK_ALG);

        // Description
        String mateDescString = userMeta.get(CryptoHeaders.CRYPTO_MATDESC);
        Map<String, String> matDesc = getDescFromJsonString(mateDescString);

        // Decrypt the secured CEK to CEK.
        ContentCryptoMaterialRW contentMaterialRW = new ContentCryptoMaterialRW();
        contentMaterialRW.setEncryptedCEK(encryptedCEK);
        contentMaterialRW.setEncryptedIV(encryptedIV);
        contentMaterialRW.setMaterialsDescription(matDesc);
        contentMaterialRW.setContentCryptoAlgorithm(cekAlgo);
        contentMaterialRW.setKeyWrapAlgorithm(keyWrapAlgo);
        encryptionMaterials.decryptCEK(contentMaterialRW);

        // Convert to read-only object.
        return contentMaterialRW;
    }

    // return the corresponding material description from the given json string.
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected static Map<String, String> getDescFromJsonString(String jsonString) {
        Map<String, String> map = new HashMap<String, String>();
        if (jsonString == null) {
            return map;
        }
        try {
            JSONObject obj = new JSONObject(jsonString);
            Iterator iter = obj.keys();
            while (iter.hasNext()) {
                String key = (String) iter.next();
                String value = obj.getString(key);
                map.put(key, value);
            }
            return map;
        } catch (JSONException e) {
            throw new ClientException("Unable to parse Json string:" + "json", e);
        }
    }

    // Returns a srcret key for encrypting content.
    protected SecretKey generateCEK() {
        KeyGenerator generator;
        final String keygenAlgo = contentCryptoScheme.getKeyGeneratorAlgorithm();
        final int keyLength = contentCryptoScheme.getKeyLengthInBits();
        try {
            generator = KeyGenerator.getInstance(keygenAlgo);
            generator.init(keyLength, cryptoConfig.getSecureRandom());
            SecretKey secretKey = generator.generateKey();
            for (int retry = 0; retry < 9; retry++) {
                secretKey = generator.generateKey();
                if (secretKey.getEncoded()[0] != 0)
                    return secretKey;
            }
            throw new ClientException("Failed to generate secret key");
        } catch (NoSuchAlgorithmException e) {
            throw new ClientException("No such algorithm:" + keygenAlgo + ", " + e.getMessage(), e);
        }
    }

    protected void setUserAgent(WebServiceRequest req, String userAgent) {
        req.addHeader(HTTP.USER_AGENT, userAgent);
    }
}
