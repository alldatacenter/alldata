package com.aliyun.oss.integrationtests;

import com.aliyun.oss.ClientConfiguration;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.common.auth.Credentials;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.auth.DefaultCredentials;
import com.aliyun.oss.internal.OSSHeaders;
import com.aliyun.oss.model.AppendObjectRequest;
import com.aliyun.oss.model.AppendObjectResult;
import com.aliyun.oss.model.CompleteMultipartUploadRequest;
import com.aliyun.oss.model.CompleteMultipartUploadResult;
import com.aliyun.oss.model.CopyObjectRequest;
import com.aliyun.oss.model.CopyObjectResult;
import com.aliyun.oss.model.DataEncryptionAlgorithm;
import com.aliyun.oss.model.InitiateMultipartUploadRequest;
import com.aliyun.oss.model.InitiateMultipartUploadResult;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.PutObjectResult;
import com.aliyun.oss.model.SSEAlgorithm;
import com.aliyun.oss.model.UploadPartCopyRequest;
import com.aliyun.oss.model.UploadPartCopyResult;
import com.aliyun.oss.model.UploadPartRequest;
import com.aliyun.oss.model.UploadPartResult;
import junit.framework.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.Map;

import static com.aliyun.oss.integrationtests.TestUtils.*;

public class ObjectEncryptionTest extends TestBase {
    private static final int ITERATIONS = 1;

    private OSSClient ossClient;
    private String bucketName;
    private String endpoint;

    public void setUp() throws Exception {
        super.setUp();

        bucketName = super.bucketName + "-object-encryption";
        endpoint = TestConfig.OSS_TEST_ENDPOINT;

        //create client
        ClientConfiguration conf = new ClientConfiguration().setSupportCname(false);
        Credentials credentials = new DefaultCredentials(TestConfig.OSS_TEST_ACCESS_KEY_ID, TestConfig.OSS_TEST_ACCESS_KEY_SECRET);
        ossClient = new OSSClient(endpoint, new DefaultCredentialProvider(credentials), conf);

        ossClient.createBucket(bucketName);
        waitForCacheExpiration(2);
    }

    public void tearDown() throws Exception {
        if (ossClient != null) {
            ossClient.shutdown();
            ossClient = null;
        }
        super.tearDown();
    }

    @Test
    public void testNormalObject() throws Exception {
        testNormalObject(null, null);
        testNormalObject(SSEAlgorithm.AES256, null);
        testNormalObject(SSEAlgorithm.SM4, null);
        testNormalObject(SSEAlgorithm.KMS, null);
        testNormalObject(SSEAlgorithm.KMS, DataEncryptionAlgorithm.SM4);
    }

    private void testNormalObject(SSEAlgorithm algorithm, DataEncryptionAlgorithm dataEncryptionAlgorithm) throws Exception {
        for (int i = 0; i < ITERATIONS; ++i) {
            final String keyPrefix = "normal-file-encryption-";
            final String filePath = genFixedLengthFile(i * 1024 * 100 + 1024); // 1KB
            try {
                // 1. put
                ObjectMetadata metadata = new ObjectMetadata();
                if (algorithm != null) {
                    metadata.setServerSideEncryption(algorithm.toString());
                }
                if (dataEncryptionAlgorithm != null) {
                    metadata.setServerSideDataEncryption(dataEncryptionAlgorithm.toString());
                }
                PutObjectRequest request = new PutObjectRequest(bucketName, buildObjectKey(keyPrefix, i), new File(filePath), metadata);
                request.setProcess("");
                PutObjectResult result = ossClient.putObject(request);
                Map<String, String> headers = result.getResponse().getHeaders();
                Assert.assertEquals(algorithm != null,
                        headers.containsKey(OSSHeaders.OSS_SERVER_SIDE_ENCRYPTION));
                if (algorithm != null) {
                    Assert.assertEquals(headers.get(OSSHeaders.OSS_SERVER_SIDE_ENCRYPTION), algorithm.toString());
                }
                Assert.assertEquals(dataEncryptionAlgorithm != null,
                        headers.containsKey(OSSHeaders.OSS_SERVER_SIDE_DATA_ENCRYPTION));
                if (dataEncryptionAlgorithm != null) {
                    Assert.assertEquals(dataEncryptionAlgorithm.toString(), headers.get(OSSHeaders.OSS_SERVER_SIDE_DATA_ENCRYPTION));
                }
                // 2. get
                byte[] target = InputStream2ByteArray(filePath);
                checkObjectContent(target, buildObjectKey(keyPrefix, i), algorithm, dataEncryptionAlgorithm);
                // 3. copy without encryption
                copyObject(target, buildObjectKey(keyPrefix, i), buildObjectKey(keyPrefix + "copy-", i), null, null);
                // 4. copy with the same encryption
                copyObject(target, buildObjectKey(keyPrefix, i), buildObjectKey(keyPrefix + "copy-", i), algorithm, dataEncryptionAlgorithm);
                // 5. copy with different encryption
                if (dataEncryptionAlgorithm == null) {
                    copyObject(target, buildObjectKey(keyPrefix, i), buildObjectKey(keyPrefix + "copy-", i), SSEAlgorithm.KMS, DataEncryptionAlgorithm.SM4);
                } else {
                    copyObject(target, buildObjectKey(keyPrefix, i), buildObjectKey(keyPrefix + "copy-", i), SSEAlgorithm.KMS, null);
                    copyObject(target, buildObjectKey(keyPrefix, i), buildObjectKey(keyPrefix + "copy-", i), SSEAlgorithm.SM4, null);
                    copyObject(target, buildObjectKey(keyPrefix, i), buildObjectKey(keyPrefix + "copy-", i), SSEAlgorithm.AES256, null);
                }
            } catch (Exception ex) {
                Assert.fail(ex.getMessage());
            } finally {
                removeFile(filePath);
            }
        }
    }

    private void copyObject(byte[] target, String srcKey, String dstKey, SSEAlgorithm algorithm, DataEncryptionAlgorithm dataEncryptionAlgorithm) throws Exception {
        CopyObjectRequest copyObjectRequest = new CopyObjectRequest(bucketName, srcKey, bucketName, dstKey);
        ObjectMetadata metadata = new ObjectMetadata();
        if (algorithm != null) {
            metadata.setServerSideEncryption(algorithm.toString());
        }
        if (dataEncryptionAlgorithm != null) {
            metadata.setServerSideDataEncryption(dataEncryptionAlgorithm.toString());
        }
        copyObjectRequest.setNewObjectMetadata(metadata);
        CopyObjectResult copyObjectResult = ossClient.copyObject(copyObjectRequest);
        checkObjectContent(target, dstKey, algorithm, dataEncryptionAlgorithm);
        Map<String, String> headers = copyObjectResult.getResponse().getHeaders();
        Assert.assertEquals(algorithm != null,
                headers.containsKey(OSSHeaders.OSS_SERVER_SIDE_ENCRYPTION));
        if (algorithm != null) {
            Assert.assertEquals(headers.get(OSSHeaders.OSS_SERVER_SIDE_ENCRYPTION), algorithm.toString());
        }
        Assert.assertEquals(dataEncryptionAlgorithm != null,
                headers.containsKey(OSSHeaders.OSS_SERVER_SIDE_DATA_ENCRYPTION));
        if (dataEncryptionAlgorithm != null) {
            Assert.assertEquals(dataEncryptionAlgorithm.toString(), headers.get(OSSHeaders.OSS_SERVER_SIDE_DATA_ENCRYPTION));
        }
    }

    private void checkObjectContent(byte[] target, String key, SSEAlgorithm algorithm, DataEncryptionAlgorithm dataEncryptionAlgorithm) throws Exception {
        OSSObject ossObject = ossClient.getObject(bucketName, key);
        Assert.assertEquals(target.length, ossObject.getObjectMetadata().getContentLength());
        byte[] source = new byte[target.length];
        int read;
        int len = 0;
        while ((read = ossObject.getObjectContent().read(source, len, 1024 * 1024)) != -1) {
            len += read;
        }
        Assert.assertEquals(new String(target), new String(source));
        Assert.assertEquals(algorithm != null,
                ossObject.getResponse().getHeaders().containsKey(OSSHeaders.OSS_SERVER_SIDE_ENCRYPTION));
        if (algorithm != null) {
            Assert.assertEquals(ossObject.getResponse().getHeaders().get(OSSHeaders.OSS_SERVER_SIDE_ENCRYPTION), algorithm.toString());
        }
        Assert.assertEquals(dataEncryptionAlgorithm != null,
                ossObject.getResponse().getHeaders().containsKey(OSSHeaders.OSS_SERVER_SIDE_DATA_ENCRYPTION));
        if (dataEncryptionAlgorithm != null) {
            Assert.assertEquals(dataEncryptionAlgorithm.toString(), ossObject.getResponse().getHeaders().get(OSSHeaders.OSS_SERVER_SIDE_DATA_ENCRYPTION));
        }
    }

    @Test
    public void testAppendObject() throws Exception {
        testAppendObject(null, null);
        testAppendObject(SSEAlgorithm.AES256, null);
        testAppendObject(SSEAlgorithm.SM4, null);
        testAppendObject(SSEAlgorithm.KMS, null);
        testAppendObject(SSEAlgorithm.KMS, DataEncryptionAlgorithm.SM4);
    }

    private void testAppendObject(SSEAlgorithm algorithm, DataEncryptionAlgorithm dataEncryptionAlgorithm) throws Exception {
        for (int i = 0; i < ITERATIONS; ++i) {
            final String keyPrefix = "append-file-encryption-";
            final String filePath = genFixedLengthFile(i * 1024 * 100 + 1024); // 1KB
            try {
                ObjectMetadata metadata = new ObjectMetadata();
                if (algorithm != null) {
                    metadata.setServerSideEncryption(algorithm.toString());
                }
                if (dataEncryptionAlgorithm != null) {
                    metadata.setServerSideDataEncryption(dataEncryptionAlgorithm.toString());
                }
                // first append
                AppendObjectRequest request = new AppendObjectRequest(bucketName, buildObjectKey(keyPrefix, i), new File(filePath), metadata);
                request.setPosition(0L);
                AppendObjectResult result = ossClient.appendObject(request);
                Map<String, String> headers = result.getResponse().getHeaders();
                Assert.assertEquals(algorithm != null,
                        headers.containsKey(OSSHeaders.OSS_SERVER_SIDE_ENCRYPTION));
                if (algorithm != null) {
                    Assert.assertEquals(headers.get(OSSHeaders.OSS_SERVER_SIDE_ENCRYPTION), algorithm.toString());
                }
                Assert.assertEquals(dataEncryptionAlgorithm != null,
                        headers.containsKey(OSSHeaders.OSS_SERVER_SIDE_DATA_ENCRYPTION));
                if (dataEncryptionAlgorithm != null) {
                    Assert.assertEquals(dataEncryptionAlgorithm.toString(), headers.get(OSSHeaders.OSS_SERVER_SIDE_DATA_ENCRYPTION));
                }

                // second append with different encryption
                metadata = new ObjectMetadata();
                if (dataEncryptionAlgorithm == null) {
                    metadata.setServerSideEncryption(SSEAlgorithm.KMS.toString());
                    metadata.setServerSideDataEncryption(DataEncryptionAlgorithm.SM4.toString());
                } else {
                    metadata.setServerSideEncryption(SSEAlgorithm.KMS.toString());
                }
                request = new AppendObjectRequest(bucketName, buildObjectKey(keyPrefix, i), new File(filePath), metadata);
                request.setPosition(i * 1024 * 100 + 1024L);
                result = ossClient.appendObject(request);
                headers = result.getResponse().getHeaders();
                Assert.assertEquals(algorithm != null,
                        headers.containsKey(OSSHeaders.OSS_SERVER_SIDE_ENCRYPTION));
                if (algorithm != null) {
                    Assert.assertEquals(headers.get(OSSHeaders.OSS_SERVER_SIDE_ENCRYPTION), algorithm.toString());
                }
                Assert.assertEquals(dataEncryptionAlgorithm != null,
                        headers.containsKey(OSSHeaders.OSS_SERVER_SIDE_DATA_ENCRYPTION));
                if (dataEncryptionAlgorithm != null) {
                    Assert.assertEquals(dataEncryptionAlgorithm.toString(), headers.get(OSSHeaders.OSS_SERVER_SIDE_DATA_ENCRYPTION));
                }

                // third append with the same encryption
                if (algorithm != null) {
                    metadata.setServerSideEncryption(algorithm.toString());
                }
                if (dataEncryptionAlgorithm != null) {
                    metadata.setServerSideDataEncryption(dataEncryptionAlgorithm.toString());
                }
                request = new AppendObjectRequest(bucketName, buildObjectKey(keyPrefix, i), new File(filePath), metadata);
                request.setPosition(2 * (i * 1024 * 100 + 1024L));
                result = ossClient.appendObject(request);
                headers = result.getResponse().getHeaders();
                Assert.assertEquals(algorithm != null,
                        headers.containsKey(OSSHeaders.OSS_SERVER_SIDE_ENCRYPTION));
                if (algorithm != null) {
                    Assert.assertEquals(headers.get(OSSHeaders.OSS_SERVER_SIDE_ENCRYPTION), algorithm.toString());
                }
                Assert.assertEquals(dataEncryptionAlgorithm != null,
                        headers.containsKey(OSSHeaders.OSS_SERVER_SIDE_DATA_ENCRYPTION));
                if (dataEncryptionAlgorithm != null) {
                    Assert.assertEquals(dataEncryptionAlgorithm.toString(), headers.get(OSSHeaders.OSS_SERVER_SIDE_DATA_ENCRYPTION));
                }

                // 2. get
                byte[] content = InputStream2ByteArray(filePath);
                byte[] target = new byte[content.length * 3];
                System.arraycopy(content, 0, target, 0, content.length);
                System.arraycopy(content, 0, target, content.length, content.length);
                System.arraycopy(content, 0, target, content.length * 2, content.length);
                checkObjectContent(target, buildObjectKey(keyPrefix, i), algorithm, dataEncryptionAlgorithm);
                // 3. copy without encryption
                copyObject(target, buildObjectKey(keyPrefix, i), buildObjectKey(keyPrefix + "copy-", i), null, null);
                // 4. copy with the same encryption
                copyObject(target, buildObjectKey(keyPrefix, i), buildObjectKey(keyPrefix + "copy-", i), algorithm, dataEncryptionAlgorithm);
                // 5. copy with different encryption
                if (dataEncryptionAlgorithm == null) {
                    copyObject(target, buildObjectKey(keyPrefix, i), buildObjectKey(keyPrefix + "copy-", i), SSEAlgorithm.KMS, DataEncryptionAlgorithm.SM4);
                } else {
                    copyObject(target, buildObjectKey(keyPrefix, i), buildObjectKey(keyPrefix + "copy-", i), SSEAlgorithm.KMS, null);
                    copyObject(target, buildObjectKey(keyPrefix, i), buildObjectKey(keyPrefix + "copy-", i), SSEAlgorithm.SM4, null);
                    copyObject(target, buildObjectKey(keyPrefix, i), buildObjectKey(keyPrefix + "copy-", i), SSEAlgorithm.AES256, null);
                }
            } catch (Exception ex) {
                Assert.fail(ex.getMessage());
            } finally {
                ossClient.deleteObject(bucketName, buildObjectKey(keyPrefix, i));
                removeFile(filePath);
            }
        }
    }

    @Test
    public void testMultipartObject() throws Exception {
        testMultipartObject(null, null);
        testMultipartObject(SSEAlgorithm.AES256, null);
        testMultipartObject(SSEAlgorithm.SM4, null);
        testMultipartObject(SSEAlgorithm.KMS, null);
        testMultipartObject(SSEAlgorithm.KMS, DataEncryptionAlgorithm.SM4);
    }

    private void testMultipartObject(SSEAlgorithm algorithm, DataEncryptionAlgorithm dataEncryptionAlgorithm) throws Exception {
        for (int i = 0; i < ITERATIONS; ++i) {
            final String keyPrefix = "multipart-file-encryption-";
            final String filePath = genFixedLengthFile(i * 1024 * 100 + 102400); // 1KB
            try {
                ObjectMetadata metadata = new ObjectMetadata();
                if (algorithm != null) {
                    metadata.setServerSideEncryption(algorithm.toString());
                }
                if (dataEncryptionAlgorithm != null) {
                    metadata.setServerSideDataEncryption(dataEncryptionAlgorithm.toString());
                }
                InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(bucketName, buildObjectKey(keyPrefix, i), metadata);
                InitiateMultipartUploadResult result = ossClient.initiateMultipartUpload(request);
                Map<String, String> headers = result.getResponse().getHeaders();
                Assert.assertEquals(algorithm != null,
                        headers.containsKey(OSSHeaders.OSS_SERVER_SIDE_ENCRYPTION));
                if (algorithm != null) {
                    Assert.assertEquals(headers.get(OSSHeaders.OSS_SERVER_SIDE_ENCRYPTION), algorithm.toString());
                }
                Assert.assertEquals(dataEncryptionAlgorithm != null,
                        headers.containsKey(OSSHeaders.OSS_SERVER_SIDE_DATA_ENCRYPTION));
                if (dataEncryptionAlgorithm != null) {
                    Assert.assertEquals(dataEncryptionAlgorithm.toString(), headers.get(OSSHeaders.OSS_SERVER_SIDE_DATA_ENCRYPTION));
                }

                UploadPartRequest uploadPartRequest = new UploadPartRequest(bucketName, buildObjectKey(keyPrefix, i),
                        result.getUploadId(), 1, new FileInputStream(new File(filePath)), i * 1024 * 100 + 102400);
                UploadPartResult uploadPartResult = ossClient.uploadPart(uploadPartRequest);
                headers = uploadPartResult.getResponse().getHeaders();
                Assert.assertEquals(algorithm != null,
                        headers.containsKey(OSSHeaders.OSS_SERVER_SIDE_ENCRYPTION));
                if (algorithm != null) {
                    Assert.assertEquals(headers.get(OSSHeaders.OSS_SERVER_SIDE_ENCRYPTION), algorithm.toString());
                }
                Assert.assertEquals(dataEncryptionAlgorithm != null,
                        headers.containsKey(OSSHeaders.OSS_SERVER_SIDE_DATA_ENCRYPTION));
                if (dataEncryptionAlgorithm != null) {
                    Assert.assertEquals(dataEncryptionAlgorithm.toString(), headers.get(OSSHeaders.OSS_SERVER_SIDE_DATA_ENCRYPTION));
                }

                //String sourceBucketName, String sourceKey, String targetBucketName, String targetKey,
                //            String uploadId, int partNumber, Long beginIndex, Long partSize
                ossClient.putObject(bucketName, buildObjectKey(keyPrefix, i) + ".normal", new File(filePath));
                UploadPartCopyRequest uploadPartCopyRequest = new UploadPartCopyRequest(bucketName, buildObjectKey(keyPrefix, i) + ".normal", bucketName, buildObjectKey(keyPrefix, i),
                        result.getUploadId(), 2, 0L, i * 1024 * 100 + 102400L);
                UploadPartCopyResult uploadPartCopyResult = ossClient.uploadPartCopy(uploadPartCopyRequest);
                headers = uploadPartCopyResult.getResponse().getHeaders();
                Assert.assertEquals(algorithm != null,
                        headers.containsKey(OSSHeaders.OSS_SERVER_SIDE_ENCRYPTION));
                if (algorithm != null) {
                    Assert.assertEquals(headers.get(OSSHeaders.OSS_SERVER_SIDE_ENCRYPTION), algorithm.toString());
                }
                Assert.assertEquals(dataEncryptionAlgorithm != null,
                        headers.containsKey(OSSHeaders.OSS_SERVER_SIDE_DATA_ENCRYPTION));
                if (dataEncryptionAlgorithm != null) {
                    Assert.assertEquals(dataEncryptionAlgorithm.toString(), headers.get(OSSHeaders.OSS_SERVER_SIDE_DATA_ENCRYPTION));
                }

                CompleteMultipartUploadRequest completeMultipartUploadRequest = new CompleteMultipartUploadRequest(bucketName, buildObjectKey(keyPrefix, i), result.getUploadId(),
                        Arrays.asList(uploadPartResult.getPartETag(), uploadPartCopyResult.getPartETag()));
                completeMultipartUploadRequest.setProcess("");
                CompleteMultipartUploadResult completeMultipartUploadResult = ossClient.completeMultipartUpload(completeMultipartUploadRequest);
                headers = completeMultipartUploadResult.getResponse().getHeaders();
                Assert.assertEquals(algorithm != null,
                        headers.containsKey(OSSHeaders.OSS_SERVER_SIDE_ENCRYPTION));
                if (algorithm != null) {
                    Assert.assertEquals(headers.get(OSSHeaders.OSS_SERVER_SIDE_ENCRYPTION), algorithm.toString());
                }
                Assert.assertEquals(dataEncryptionAlgorithm != null,
                        headers.containsKey(OSSHeaders.OSS_SERVER_SIDE_DATA_ENCRYPTION));
                if (dataEncryptionAlgorithm != null) {
                    Assert.assertEquals(dataEncryptionAlgorithm.toString(), headers.get(OSSHeaders.OSS_SERVER_SIDE_DATA_ENCRYPTION));
                }

                // 2. get
                byte[] content = InputStream2ByteArray(filePath);
                byte[] target = new byte[content.length * 2];
                System.arraycopy(content, 0, target, 0, content.length);
                System.arraycopy(content, 0, target, content.length, content.length);
                checkObjectContent(target, buildObjectKey(keyPrefix, i), algorithm, dataEncryptionAlgorithm);
                // 3. copy without encryption
                copyObject(target, buildObjectKey(keyPrefix, i), buildObjectKey(keyPrefix + "copy-", i), null, null);
                // 4. copy with the same encryption
                copyObject(target, buildObjectKey(keyPrefix, i), buildObjectKey(keyPrefix + "copy-", i), algorithm, dataEncryptionAlgorithm);
                // 5. copy with different encryption
                if (dataEncryptionAlgorithm == null) {
                    copyObject(target, buildObjectKey(keyPrefix, i), buildObjectKey(keyPrefix + "copy-", i), SSEAlgorithm.KMS, DataEncryptionAlgorithm.SM4);
                } else {
                    copyObject(target, buildObjectKey(keyPrefix, i), buildObjectKey(keyPrefix + "copy-", i), SSEAlgorithm.KMS, null);
                    copyObject(target, buildObjectKey(keyPrefix, i), buildObjectKey(keyPrefix + "copy-", i), SSEAlgorithm.SM4, null);
                    copyObject(target, buildObjectKey(keyPrefix, i), buildObjectKey(keyPrefix + "copy-", i), SSEAlgorithm.AES256, null);
                }
            } catch (Exception ex) {
                Assert.fail(ex.getMessage());
            } finally {
                ossClient.deleteObject(bucketName, buildObjectKey(keyPrefix, i));
                removeFile(filePath);
            }
        }
    }
}
