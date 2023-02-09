package com.qcloud.cos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;

import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.BasicSessionCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.auth.COSCredentialsProvider;
import com.qcloud.cos.auth.COSStaticCredentialsProvider;
import com.qcloud.cos.auth.InstanceCredentialsFetcher;
import com.qcloud.cos.auth.InstanceCredentialsProvider;
import com.qcloud.cos.auth.InstanceMetadataCredentialsEndpointProvider;
import com.qcloud.cos.endpoint.UserSpecifiedEndpointBuilder;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.internal.SkipMd5CheckStrategy;
import com.qcloud.cos.internal.crypto.CryptoConfiguration;
import com.qcloud.cos.internal.crypto.CryptoMode;
import com.qcloud.cos.internal.crypto.EncryptionMaterials;
import com.qcloud.cos.internal.crypto.EncryptionMaterialsProvider;
import com.qcloud.cos.internal.crypto.KMSEncryptionMaterials;
import com.qcloud.cos.internal.crypto.KMSEncryptionMaterialsProvider;
import com.qcloud.cos.internal.crypto.QCLOUDKMS;
import com.qcloud.cos.internal.crypto.StaticEncryptionMaterialsProvider;
import com.qcloud.cos.model.AbortMultipartUploadRequest;
import com.qcloud.cos.model.AccessControlList;
import com.qcloud.cos.model.AppendObjectRequest;
import com.qcloud.cos.model.AppendObjectResult;
import com.qcloud.cos.model.Bucket;
import com.qcloud.cos.model.BucketVersioningConfiguration;
import com.qcloud.cos.model.COSObjectSummary;
import com.qcloud.cos.model.COSVersionSummary;
import com.qcloud.cos.model.CompleteMultipartUploadRequest;
import com.qcloud.cos.model.CompleteMultipartUploadResult;
import com.qcloud.cos.model.CreateBucketRequest;
import com.qcloud.cos.model.GetBucketVersioningConfigurationRequest;
import com.qcloud.cos.model.GetObjectMetadataRequest;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.InitiateMultipartUploadRequest;
import com.qcloud.cos.model.InitiateMultipartUploadResult;
import com.qcloud.cos.model.ListMultipartUploadsRequest;
import com.qcloud.cos.model.ListObjectsRequest;
import com.qcloud.cos.model.ListPartsRequest;
import com.qcloud.cos.model.ListVersionsRequest;
import com.qcloud.cos.model.MultipartUpload;
import com.qcloud.cos.model.MultipartUploadListing;
import com.qcloud.cos.model.ObjectListing;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PartETag;
import com.qcloud.cos.model.PartListing;
import com.qcloud.cos.model.PartSummary;
import com.qcloud.cos.model.Permission;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ResponseHeaderOverrides;
import com.qcloud.cos.model.SSECOSKeyManagementParams;
import com.qcloud.cos.model.SSECustomerKey;
import com.qcloud.cos.model.StorageClass;
import com.qcloud.cos.model.UinGrantee;
import com.qcloud.cos.model.UploadPartRequest;
import com.qcloud.cos.model.UploadPartResult;
import com.qcloud.cos.model.VersionListing;
import com.qcloud.cos.region.Region;
import com.qcloud.cos.utils.DateUtils;
import com.qcloud.cos.utils.Md5Utils;
import com.tencent.cloud.CosStsClient;

import org.json.JSONObject;

public class AbstractCOSClientTest {
    protected static String appid = null;
    protected static String accountId = null;
    protected static String secretId = null;
    protected static String secretKey = null;
    protected static String region = null;
    protected static String bucket = null;
    protected static String generalApiEndpoint = null;
    protected static String serviceApiEndpoint = null;
    protected static ClientConfig clientConfig = null;
    protected static COSClient cosclient = null;
    protected static File tmpDir = null;
    protected static String cmk = null;

    protected static boolean useClientEncryption = false;
    protected static boolean useServerEncryption = false;
    protected static QCLOUDKMS qcloudkms = null;
    protected static EncryptionMaterials encryptionMaterials = null;
    protected static CryptoConfiguration cryptoConfiguration = null;
    protected static boolean useCVMInstanceCredentials = false;
    protected static boolean useCPMInstanceCredentials = false;

    protected static File buildTestFile(long fileSize) throws IOException {
        String prefix = String.format("ut_size_%d_time_%d_", fileSize, System.currentTimeMillis());
        String suffix = ".tmp";
        File tmpFile = null;
        tmpFile = File.createTempFile(prefix, suffix, tmpDir);

        BufferedOutputStream bos = null;
        try {
            bos = new BufferedOutputStream(new FileOutputStream(tmpFile));
            final int buffSize = 1024;
            byte[] tmpBuf = new byte[buffSize];
            long byteWriten = 0;
            while (byteWriten < fileSize) {
                ThreadLocalRandom.current().nextBytes(tmpBuf);
                long maxWriteLen = Math.min(buffSize, fileSize - byteWriten);
                bos.write(tmpBuf, 0, new Long(maxWriteLen).intValue());
                byteWriten += maxWriteLen;
            }
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                }
            }
        }
        return tmpFile;
    }

    protected static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }

    protected static boolean initConfig() throws IOException {
        appid = System.getenv("appid");
        accountId = System.getenv("accountId");
        secretId = System.getenv("secretId");
        secretKey = System.getenv("secretKey");
        region = System.getenv("region");
        bucket = System.getenv("bucket");
        generalApiEndpoint = System.getenv("generalApiEndpoint");
        serviceApiEndpoint = System.getenv("serviceApiEndpoint");
        cmk = System.getenv("cmk");

        File propFile = new File("ut_account.prop");
        if (propFile.exists() && propFile.canRead()) {
            Properties prop = new Properties();
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(propFile);
                prop.load(fis);
                appid = prop.getProperty("appid");
                accountId = prop.getProperty("accountId");
                secretId = prop.getProperty("secretId");
                secretKey = prop.getProperty("secretKey");
                region = prop.getProperty("region");
                bucket = prop.getProperty("bucket");
                generalApiEndpoint = prop.getProperty("generalApiEndpoint");
                serviceApiEndpoint = prop.getProperty("serviceApiEndpoint");
                useCPMInstanceCredentials = Boolean.parseBoolean(prop.getProperty("useCPMInstanceCredentials", "false"
                ));
                useCVMInstanceCredentials = Boolean.parseBoolean(prop.getProperty("useCVMInstanceCredentials", "false"));
                cmk = prop.getProperty("cmk");
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (Exception e) {
                    }
                }
            }
        }

        if (secretId == null || secretKey == null || bucket == null || region == null) {
            System.out.println("cos ut user info missing. skip all test");
            return false;
        }
        return true;
    }

    protected static void initCustomCosClient() {
        if (useClientEncryption) {
            initEncryptionClient();
        } else {
            initNormalCOSClient();
        }
    }

    protected static void initNormalCOSClient() {
        if(useCVMInstanceCredentials) {
            cosclient = buildCVMInstanceCredentialsCOSClient();
        }else if (useCPMInstanceCredentials){
            cosclient = buildCPMInstanceCredentialsCOSClient();
        }else {
            COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
            clientConfig = new ClientConfig(new Region(region));
            if (generalApiEndpoint != null && generalApiEndpoint.trim().length() > 0 &&
                    serviceApiEndpoint != null && serviceApiEndpoint.trim().length() > 0) {
                UserSpecifiedEndpointBuilder userSpecifiedEndpointBuilder =
                        new UserSpecifiedEndpointBuilder(generalApiEndpoint, serviceApiEndpoint);
                clientConfig.setEndpointBuilder(userSpecifiedEndpointBuilder);
            }
            cosclient = new COSClient(cred, clientConfig);
        }
    }

    protected static COSClient buildTemporyCredentialsCOSClient(long tokenDuration) {
        TemporyToken temporyToken = fetchTempToken(tokenDuration);
        BasicSessionCredentials tempCred =
                new BasicSessionCredentials(temporyToken.getTempSecretId(),
                        temporyToken.getTempSecretKey(), temporyToken.getTempToken());
        ClientConfig temporyClientConfig = new ClientConfig(new Region(region));
        COSClient tempCOSClient = new COSClient(tempCred, temporyClientConfig);
        return tempCOSClient;
    }

    protected static COSClient buildCVMInstanceCredentialsCOSClient() {
        InstanceMetadataCredentialsEndpointProvider endpointProvider =
                new InstanceMetadataCredentialsEndpointProvider(InstanceMetadataCredentialsEndpointProvider.Instance.CVM);

        InstanceCredentialsFetcher instanceCredentialsFetcher = new InstanceCredentialsFetcher(endpointProvider);
        COSCredentialsProvider cosCredentialsProvider = new InstanceCredentialsProvider(instanceCredentialsFetcher);
        clientConfig = new ClientConfig(new Region(region));
        return new COSClient(cosCredentialsProvider, clientConfig);
    }

    protected static COSClient buildCPMInstanceCredentialsCOSClient() {
        InstanceMetadataCredentialsEndpointProvider endpointProvider =
                new InstanceMetadataCredentialsEndpointProvider(InstanceMetadataCredentialsEndpointProvider.Instance.CPM);

        InstanceCredentialsFetcher instanceCredentialsFetcher = new InstanceCredentialsFetcher(endpointProvider);
        COSCredentialsProvider cosCredentialsProvider = new InstanceCredentialsProvider(instanceCredentialsFetcher);
        clientConfig = new ClientConfig(new Region(region));
        return new COSClient(cosCredentialsProvider, clientConfig);
    }

    protected static void initEncryptionClient() {
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        clientConfig = new ClientConfig(new Region(region));
        if (generalApiEndpoint != null && generalApiEndpoint.trim().length() > 0 &&
                serviceApiEndpoint != null && serviceApiEndpoint.trim().length() > 0) {
            UserSpecifiedEndpointBuilder userSpecifiedEndpointBuilder = new UserSpecifiedEndpointBuilder(generalApiEndpoint, serviceApiEndpoint);
            clientConfig.setEndpointBuilder(userSpecifiedEndpointBuilder);
        }

        EncryptionMaterialsProvider encryptionMaterialsProvider;
        if (encryptionMaterials instanceof KMSEncryptionMaterials) {
            KMSEncryptionMaterials kmsEncryptionMaterials = new KMSEncryptionMaterials(cmk);
            encryptionMaterialsProvider = new KMSEncryptionMaterialsProvider(kmsEncryptionMaterials);
        } else {
            encryptionMaterialsProvider = new StaticEncryptionMaterialsProvider(encryptionMaterials);
        }

        cosclient = new COSEncryptionClient(qcloudkms, new COSStaticCredentialsProvider(cred),
                encryptionMaterialsProvider, clientConfig, cryptoConfiguration);
    }

    public static void initCosClient() throws Exception {
        if (!initConfig()) {
            return;
        }
        initCustomCosClient();
        tmpDir = new File("ut_test_tmp_data");
        if (!tmpDir.exists()) {
            tmpDir.mkdirs();
        }
        deleteBucket();             // 先清理掉原先的bucket
        createBucket();             // 然后再重新创建
    }

    private static void createBucket() throws Exception {
        try {
            // 避免有查询缓存，导致创建bucket失败
            Thread.sleep(5000L);
            String bucketName = bucket;
            CreateBucketRequest createBucketRequest = new CreateBucketRequest(bucketName);
            Bucket createdBucket = cosclient.createBucket(createBucketRequest);
            assertEquals(bucketName, createdBucket.getName());
            Thread.sleep(5000L);
            assertTrue(cosclient.doesBucketExist(bucketName));
        } catch (CosServiceException cse) {
            fail(cse.toString());
        }
    }

    private static void abortAllNotFinishedMultipartUpload() throws Exception {
        ListMultipartUploadsRequest listMultipartUploadsRequest =
                new ListMultipartUploadsRequest(bucket);
        MultipartUploadListing multipartUploadListing = null;
        while (true) {
            multipartUploadListing = cosclient.listMultipartUploads(listMultipartUploadsRequest);
            List<MultipartUpload> multipartUploads = multipartUploadListing.getMultipartUploads();
            for (MultipartUpload mUpload : multipartUploads) {
                String key = mUpload.getKey();
                String uploadId = mUpload.getUploadId();
                cosclient.abortMultipartUpload(
                        new AbortMultipartUploadRequest(bucket, key, uploadId));
            }
            if (!multipartUploadListing.isTruncated()) {
                break;
            }
            listMultipartUploadsRequest.setKeyMarker(multipartUploadListing.getNextKeyMarker());
            listMultipartUploadsRequest
                    .setUploadIdMarker(multipartUploadListing.getNextUploadIdMarker());
        }
    }


    private static void clearObjectVersions() throws Exception {
        ListVersionsRequest listVersionsReq = new ListVersionsRequest();
        listVersionsReq.setBucketName(bucket);
        VersionListing versionListing = null;
        while (true) {
            versionListing = cosclient.listVersions(listVersionsReq);
            List<COSVersionSummary> versionSummaries = versionListing.getVersionSummaries();
            for (COSVersionSummary summary : versionSummaries) {
                String key = summary.getKey();
                String versionId = summary.getVersionId();
                cosclient.deleteVersion(bucket, key, versionId);
            }
            if (!versionListing.isTruncated()) {
                break;
            }
            listVersionsReq.setKeyMarker(versionListing.getNextKeyMarker());
            listVersionsReq.setVersionIdMarker(versionListing.getNextVersionIdMarker());
        }
    }

    private static void clearBucket() throws Exception {
        abortAllNotFinishedMultipartUpload();
        // 先判断bucket是否开启了版本控制
        GetBucketVersioningConfigurationRequest getBucketVersioningConfigurationRequest =
                new GetBucketVersioningConfigurationRequest(bucket);
        BucketVersioningConfiguration bucketVersioningConfiguration = cosclient.getBucketVersioningConfiguration(
                getBucketVersioningConfigurationRequest
        );
        if (bucketVersioningConfiguration.getStatus().compareToIgnoreCase(BucketVersioningConfiguration.ENABLED) == 0) {
            clearObjectVersions();
        }
        String nextMarker = "";
        boolean isTruncated = false;
        do {
            ListObjectsRequest listObjectsRequest = new ListObjectsRequest();
            listObjectsRequest.setBucketName(bucket);
            listObjectsRequest.setMaxKeys(1000);
            listObjectsRequest.setPrefix("");
            listObjectsRequest.setDelimiter("");
            listObjectsRequest.setMarker(nextMarker);
            ObjectListing objectListing = cosclient.listObjects(listObjectsRequest);
            for (COSObjectSummary cosObjectSummary : objectListing.getObjectSummaries()) {
                String key = cosObjectSummary.getKey();
                // 删除这个key
                System.out.println(key);
                cosclient.deleteObject(bucket, key);
            }
            nextMarker = objectListing.getNextMarker();
            isTruncated = objectListing.isTruncated();
        } while (isTruncated);
    }

    private static void deleteBucket() throws Exception {
        if (!cosclient.doesBucketExist(bucket)) {
            return;
        }

        try {
            clearBucket();
            cosclient.deleteBucket(bucket);
            // 删除bucket后, 由于server端有缓存 需要稍后查询, 这里sleep 5 秒
            Thread.sleep(5000L);
            assertFalse(cosclient.doesBucketExist(bucket));
        } catch (CosServiceException cse) {
            fail(cse.toString());
        }
    }

    public static void destoryCosClient() throws Exception {
        if (cosclient != null) {
            deleteBucket();
            cosclient.shutdown();
        }
        if (tmpDir != null) {
            deleteDir(tmpDir);
        }
    }

    protected static boolean judgeUserInfoValid() {
        return cosclient != null;
    }

    protected static PutObjectResult putObjectFromLocalFile(File localFile, String key) {
        return putObjectFromLocalFile(localFile, key, null, null);
    }

    // 从本地上传
    protected static PutObjectResult putObjectFromLocalFile(File localFile, String key,
                                                            SSECustomerKey sseCKey, SSECOSKeyManagementParams params) {
        if (!judgeUserInfoValid()) {
            return null;
        }

        AccessControlList acl = new AccessControlList();
        UinGrantee uinGrantee = new UinGrantee("734000014");
        acl.grantPermission(uinGrantee, Permission.Read);
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucket, key, localFile);
        putObjectRequest.setStorageClass(StorageClass.Standard_IA);
        if (sseCKey != null) {
            putObjectRequest.setSSECustomerKey(sseCKey);
        }
        if (params != null) {
            putObjectRequest.setSSECOSKeyManagementParams(params);
        }
        // putObjectRequest.setCannedAcl(CannedAccessControlList.PublicRead);
        // putObjectRequest.setAccessControlList(acl);

        PutObjectResult putObjectResult = cosclient.putObject(putObjectRequest);
        assertNotNull(putObjectResult.getRequestId());
        assertNotNull(putObjectResult.getDateStr());
        String etag = putObjectResult.getETag();
        String expectEtag = null;
        try {
            expectEtag = Md5Utils.md5Hex(localFile);
        } catch (IOException e) {
            fail(e.toString());
        }
        if (useClientEncryption) {
            assertEquals(false, expectEtag.equals(etag));
        } else {
            assertEquals(true, expectEtag.equals(etag));
        }
        return putObjectResult;
    }

    // 流式上传
    protected static void putObjectFromLocalFileByInputStream(File localFile, long uploadSize,
                                                              String uploadEtag, String key) {
        if (!judgeUserInfoValid()) {
            return;
        }
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentLength(uploadSize);
        putObjectFromLocalFileByInputStream(localFile, uploadSize, uploadEtag, key, objectMetadata);
    }

    protected static void putObjectFromLocalFileByInputStream(File localFile, long uploadSize,
                                                              String uploadEtag, String key, ObjectMetadata objectMetadata) {
        if (!judgeUserInfoValid()) {
            return;
        }

        FileInputStream input = null;
        try {
            input = new FileInputStream(localFile);
            PutObjectRequest putObjectRequest =
                    new PutObjectRequest(bucket, key, input, objectMetadata);
            putObjectRequest.setStorageClass(StorageClass.Standard_IA);
            PutObjectResult putObjectResult = cosclient.putObject(putObjectRequest);
            String etag = putObjectResult.getETag();
            if (useClientEncryption) {
                assertEquals(false, uploadEtag.equals(etag));
            } else {
                assertEquals(true, uploadEtag.equals(etag));
            }
        } catch (IOException e) {
            fail(e.toString());
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                }
            }
        }
    }

    protected static ObjectMetadata headSimpleObject(String key, long expectedLength,
                                                     String expectedEtag) {
        ObjectMetadata objectMetadata =
                cosclient.getObjectMetadata(new GetObjectMetadataRequest(bucket, key));
        if (!useClientEncryption) {
            assertEquals(expectedLength, objectMetadata.getContentLength());
        } else {
            assertEquals(expectedLength,
                    Long.valueOf(
                            objectMetadata.getUserMetaDataOf(Headers.UNENCRYPTED_CONTENT_LENGTH))
                            .longValue());
        }
        if (useClientEncryption) {
            assertEquals(false, expectedEtag.equals(objectMetadata.getETag()));
        } else {
            assertEquals(true, expectedEtag.equals(objectMetadata.getETag()));
        }
        assertNotNull(objectMetadata.getLastModified());
        return objectMetadata;
    }

    protected static ObjectMetadata headMultiPartObject(String key, long expectedLength,
                                                        int expectedPartNum) {
        ObjectMetadata objectMetadata =
                cosclient.getObjectMetadata(new GetObjectMetadataRequest(bucket, key));
        if (!useClientEncryption) {
            assertEquals(expectedLength, objectMetadata.getContentLength());
        }
        String etag = objectMetadata.getETag();
        assertTrue(etag.contains("-"));
        try {
            int etagPartNum = Integer.valueOf(etag.substring(etag.indexOf("-") + 1));
            assertEquals(expectedPartNum, etagPartNum);
        } catch (NumberFormatException e) {
            fail("part number in etag is invalid. etag: " + etag);
        }
        assertNotNull(objectMetadata.getLastModified());
        return objectMetadata;
    }

    // 下载COS的object
    protected static void getObject(String key, File localDownFile, long[] range,
                                    long expectedLength, String expectedMd5) {
        System.setProperty(SkipMd5CheckStrategy.DISABLE_GET_OBJECT_MD5_VALIDATION_PROPERTY, "true");
        GetObjectRequest getObjectRequest = new GetObjectRequest(bucket, key);
        ResponseHeaderOverrides responseHeaders = new ResponseHeaderOverrides();
        String responseContentType = "image/x-icon";
        String responseContentLanguage = "zh-CN";
        String responseContentDispositon = "filename=\"abc.txt\"";
        String responseCacheControl = "no-cache";
        String expireStr =
                DateUtils.formatRFC822Date(new Date(System.currentTimeMillis() + 24 * 3600 * 1000));
        responseHeaders.setContentType(responseContentType);
        responseHeaders.setContentLanguage(responseContentLanguage);
        responseHeaders.setContentDisposition(responseContentDispositon);
        responseHeaders.setCacheControl(responseCacheControl);
        responseHeaders.setExpires(expireStr);

        getObjectRequest.setResponseHeaders(responseHeaders);
        if (range != null) {
            if (range[1] == range[0] && range[1] == 0) {
                assertEquals(expectedLength, 1);
                getObjectRequest.setRange(range[0], range[1]);
            } else {
                assertEquals(expectedLength, range[1] - range[0] + 1);
                getObjectRequest.setRange(range[0], range[1]);
            }
        }
        try {
            ObjectMetadata objectMetadata = cosclient.getObject(getObjectRequest, localDownFile);
            assertEquals(responseContentType, objectMetadata.getContentType());
            assertEquals(responseContentLanguage, objectMetadata.getContentLanguage());
            assertEquals(responseContentDispositon, objectMetadata.getContentDisposition());
            assertEquals(responseCacheControl, objectMetadata.getCacheControl());
            assertEquals(expectedLength, localDownFile.length());
            assertEquals(expectedMd5, Md5Utils.md5Hex(localDownFile));
        } catch (SecurityException se) {
            if (cosclient instanceof COSEncryptionClient && cryptoConfiguration != null
                    && cryptoConfiguration
                    .getCryptoMode() == CryptoMode.StrictAuthenticatedEncryption
                    && range != null) {
                return;
            }
            fail(se.toString());
        } catch (Exception e) {
            fail(e.toString());
        }
    }

    protected void checkMetaData(ObjectMetadata originMetaData, ObjectMetadata queryMetaData) {
        Map<String, Object> originRawMeta = originMetaData.getRawMetadata();
        Map<String, Object> queryRawMeta = queryMetaData.getRawMetadata();

        Map<String, String> originRawUserMeta = originMetaData.getUserMetadata();
        Map<String, String> queryRawUserMeta = queryMetaData.getUserMetadata();

        for (Entry<String, Object> entry : originRawMeta.entrySet()) {
            String key = entry.getKey();
            if (key.equals(Headers.CONTENT_LENGTH)) {
                if (useClientEncryption) {
                    assertTrue(queryRawUserMeta.containsKey(Headers.UNENCRYPTED_CONTENT_LENGTH));
                    assertEquals(entry.getValue(),
                            Long.valueOf(queryRawUserMeta.get(Headers.UNENCRYPTED_CONTENT_LENGTH)));
                } else {
                    assertTrue(queryRawMeta.containsKey(key));
                    assertEquals(entry.getValue(), queryRawMeta.get(key));
                }
            } else {
                assertTrue(queryRawMeta.containsKey(key));
                assertEquals(entry.getValue(), queryRawMeta.get(key));
            }
        }

        for (Entry<String, String> entry : originRawUserMeta.entrySet()) {
            assertTrue(queryRawUserMeta.containsKey(entry.getKey()));
            assertEquals(entry.getValue(), queryRawUserMeta.get(entry.getKey()));
        }

    }

    // 删除COS上的object
    protected static void clearObject(String key) {
        if (!judgeUserInfoValid()) {
            return;
        }

        cosclient.deleteObject(bucket, key);
        assertFalse(cosclient.doesObjectExist(bucket, key));
    }

    // 流式上传不同尺寸的文件
    protected void testPutObjectByStreamDiffSize(long size, ObjectMetadata originMetaData)
            throws IOException {
        if (!judgeUserInfoValid()) {
            return;
        }
        File localFile = buildTestFile(size);
        String localFileMd5 = Md5Utils.md5Hex(localFile);
        File downLoadFile = new File(localFile.getAbsolutePath() + ".down");
        String key = "ut/" + localFile.getName();
        originMetaData.setContentLength(size);
        try {
            // put object
            putObjectFromLocalFileByInputStream(localFile, localFile.length(),
                    Md5Utils.md5Hex(localFile), key, originMetaData);
            // get object
            getObject(key, downLoadFile, null, size, localFileMd5);
            // head object
            ObjectMetadata queryObjectMeta = headSimpleObject(key, size, localFileMd5);
            // check meta data
            checkMetaData(originMetaData, queryObjectMeta);
        } finally {
            // delete file on cos
            clearObject(key);
            // delete local file
            if (localFile.exists()) {
                assertTrue(localFile.delete());
            }
            if (downLoadFile.exists()) {
                assertTrue(downLoadFile.delete());
            }
        }
    }

    private byte[] getFilePartByte(File localFile, int offset, int len) {
        byte[] content = new byte[len];
        BufferedInputStream bis = null;
        try {
            bis = new BufferedInputStream(new FileInputStream(localFile));
            bis.skip(offset);
            bis.read(content);
        } catch (IOException e) {
            fail(e.toString());
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                }
            }
        }
        return content;
    }

    protected void testPutObjectByTruncateDiffSize(long originSize, long truncateSize)
            throws IOException {
        if (!judgeUserInfoValid()) {
            return;
        }
        File localFile = buildTestFile(originSize);
        File downLoadFile = new File(localFile.getAbsolutePath() + ".down");
        String key = "ut/" + localFile.getName();
        try {
            byte[] partByte = getFilePartByte(localFile, 0, new Long(truncateSize).intValue());
            String originMd5 = Md5Utils.md5Hex(partByte);
            String uploadEtag = Md5Utils.md5Hex(partByte);
            // put object
            putObjectFromLocalFileByInputStream(localFile, truncateSize, uploadEtag, key);
            // head object
            headSimpleObject(key, truncateSize, uploadEtag);
            // get object
            long[] range = null;
            if (truncateSize > 0) {
                range = new long[]{0, truncateSize - 1};
            }
            getObject(key, downLoadFile, range, truncateSize, originMd5);
            // check file
            assertEquals(uploadEtag, Md5Utils.md5Hex(downLoadFile));
        } finally {
            // delete file on cos
            clearObject(key);
            // delete local file
            if (localFile.exists()) {
                assertTrue(localFile.delete());
            }
            if (downLoadFile.exists()) {
                assertTrue(downLoadFile.delete());
            }
        }
    }
    protected void testAppendGetDelObjectDiffSize(long size, boolean isStream) throws IOException {
        String key = "ut/" + size;
        long nextAppendPosition = 0;
        for(int i = 0; i < 3; i++) {
            File localFile = buildTestFile(size);
            AppendObjectRequest appendObjectRequest = null;
            if(!isStream) {
                appendObjectRequest = new AppendObjectRequest(bucket, key, localFile);
            } else {
                ObjectMetadata objectMetadata = new ObjectMetadata();
                objectMetadata.setContentLength(size); 
                appendObjectRequest = new AppendObjectRequest(bucket, key, new FileInputStream(localFile), objectMetadata);
            }
            appendObjectRequest.setPosition(nextAppendPosition);
            AppendObjectResult appendObjectResult = cosclient.appendObject(appendObjectRequest);
            nextAppendPosition = appendObjectResult.getNextAppendPosition();
            localFile.delete();
        }
        ObjectMetadata objectMetadata = cosclient.getObjectMetadata(bucket, key);
        assertEquals(objectMetadata.getContentLength(), size * 3);
        cosclient.deleteObject(bucket, key);
    }

    // 在本地生成不同大小的文件, 并上传， 下载，删除
    protected void testPutGetDelObjectDiffSize(long size) throws CosServiceException, IOException {
        if (!judgeUserInfoValid()) {
            return;
        }
        File localFile = buildTestFile(size);
        File downLoadFile = new File(localFile.getAbsolutePath() + ".down");
        String key = "ut/" + localFile.getName();
        testPutGetObjectAndClear(key, localFile, downLoadFile);
    }

    protected void testPutGetObjectAndClear(String key, File localFile, File downLoadFile)
            throws CosServiceException, IOException {
        testPutGetObjectAndClear(key, localFile, downLoadFile, null, null);
    }

    protected void testPutGetObjectAndClear(String key, File localFile, File downLoadFile,
                                            SSECustomerKey sseCKey, SSECOSKeyManagementParams params)
            throws CosServiceException, IOException {
        if (!judgeUserInfoValid()) {
            return;
        }

        try {
            // put object
            putObjectFromLocalFile(localFile, key, sseCKey, params);
            // head object
            headSimpleObject(key, localFile.length(), Md5Utils.md5Hex(localFile));
            long range[] = null;
            if (localFile.length() > 0) {
                range = new long[]{0, localFile.length() - 1};
            }
            // get object
            getObject(key, downLoadFile, range, localFile.length(), Md5Utils.md5Hex(localFile));
        } finally {
            // delete file on cos
            clearObject(key);
            // delete local file
            if (localFile.exists()) {
                assertTrue(localFile.delete());
            }
            if (downLoadFile.exists()) {
                assertTrue(downLoadFile.delete());
            }
        }
    }

    protected String testInitMultipart(String key) {
        InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(bucket, key);
        request.setStorageClass(StorageClass.Standard_IA);
        InitiateMultipartUploadResult initResult = cosclient.initiateMultipartUpload(request);
        return initResult.getUploadId();
    }

    protected void testUploadPart(String key, String uploadId, int partNumber, byte[] data,
                                  String dataMd5, boolean isLastPart) {
        UploadPartRequest uploadPartRequest = new UploadPartRequest();
        uploadPartRequest.setBucketName(bucket);
        uploadPartRequest.setKey(key);
        uploadPartRequest.setUploadId(uploadId);
        uploadPartRequest.setInputStream(new ByteArrayInputStream(data));
        uploadPartRequest.setPartSize(data.length);
        uploadPartRequest.setPartNumber(partNumber);
        uploadPartRequest.setLastPart(isLastPart);

        UploadPartResult uploadPartResult = cosclient.uploadPart(uploadPartRequest);
        if (useClientEncryption) {
            assertEquals(false, dataMd5.equals(uploadPartResult.getETag()));
        } else {
            assertEquals(true, dataMd5.equals(uploadPartResult.getETag()));
        }
        assertNotNull(uploadPartResult.getCrc64Ecma());
        assertEquals(partNumber, uploadPartResult.getPartNumber());
    }

    protected List<PartETag> testListMultipart(String key, String uploadId, int expectedPartNum,
                                               List<String> originDataMd5Array) {
        List<PartETag> partETags = new LinkedList<>();
        PartListing partListing = null;
        ListPartsRequest listPartsRequest = new ListPartsRequest(bucket, key, uploadId);
        int currentPartNum = 0;
        do {
            partListing = cosclient.listParts(listPartsRequest);
            for (PartSummary partSummary : partListing.getParts()) {
                ++currentPartNum;
                assertEquals(currentPartNum, partSummary.getPartNumber());
                if (useClientEncryption) {
                    assertEquals(false, partSummary.getETag()
                            .equals(originDataMd5Array.get(currentPartNum - 1)));
                } else {
                    assertEquals(true, partSummary.getETag()
                            .equals(originDataMd5Array.get(currentPartNum - 1)));
                }
                partETags.add(new PartETag(partSummary.getPartNumber(), partSummary.getETag()));
            }
            listPartsRequest.setPartNumberMarker(partListing.getNextPartNumberMarker());
        } while (partListing.isTruncated());
        assertEquals(expectedPartNum, currentPartNum);
        return partETags;
    }

    protected void testCompleteMultiPart(String key, String uploadId, List<PartETag> partETags,
                                         int expectedPartNum) {
        CompleteMultipartUploadRequest completeMultipartUploadRequest =
                new CompleteMultipartUploadRequest(bucket, key, uploadId, partETags);
        CompleteMultipartUploadResult completeResult =
                cosclient.completeMultipartUpload(completeMultipartUploadRequest);
        assertNotNull(completeResult.getRequestId());
        assertNotNull(completeResult.getDateStr());
        String etag = completeResult.getETag();
        assertTrue(etag.contains("-"));
        assertNotNull(completeResult.getCrc64Ecma());
        try {
            int etagPartNum = Integer.valueOf(etag.substring(etag.indexOf("-") + 1));
            assertEquals(expectedPartNum, etagPartNum);
        } catch (NumberFormatException e) {
            fail("part number in etag is invalid. etag: " + etag);
        }
    }

    protected void testGetEachPart(String key, long partSize, long fileSize,
                                   List<PartETag> partETags, List<String> originDataMd5Array) throws IOException {
        File downloadFile = buildTestFile(0L);
        long partBegin = 0;
        long partEnd = 0;
        assertEquals(partETags.size(), originDataMd5Array.size());
        try {
            for (PartETag partETag : partETags) {
                int partNumber = partETag.getPartNumber();
                partBegin = (partNumber - 1) * partSize;
                partEnd = partNumber * partSize - 1;
                if (partEnd >= fileSize) {
                    partEnd = fileSize - 1;
                }
                long range[] = new long[]{partBegin, partEnd};
                getObject(key, downloadFile, range, partEnd - partBegin + 1,
                        originDataMd5Array.get(partNumber - 1));
                assertEquals(partEnd - partBegin + 1, downloadFile.length());
                assertEquals(originDataMd5Array.get(partNumber - 1), Md5Utils.md5Hex(downloadFile));
                assertEquals(!useClientEncryption,
                        originDataMd5Array.get(partNumber - 1).equals(partETag.getETag()));
            }
        } catch (Exception e) {
            downloadFile.delete();
        }
    }

    protected void testMultiPartUploadObject(long filesize, long partSize) throws IOException {
        if (!judgeUserInfoValid()) {
            return;
        }
        assertTrue(partSize >= 1024 * 1024L);
        assertTrue(filesize >= partSize);
        String key = String.format("ut_multipart_size_%d_part_%d_time_%d_random_%d", filesize,
                partSize, System.currentTimeMillis(), ThreadLocalRandom.current().nextLong());

        try {
            String uploadId = testInitMultipart(key);
            assertNotNull(uploadId);
            int partNum = 0;
            long byteUploaded = 0;
            List<String> dataMd5Array = new ArrayList<>();
            while (byteUploaded < filesize) {
                ++partNum;
                long currentPartSize = Math.min(filesize - byteUploaded, partSize);
                byte[] dataUploaded = new byte[new Long(currentPartSize).intValue()];
                ThreadLocalRandom.current().nextBytes(dataUploaded);
                boolean isLastPart = false;
                if (byteUploaded + currentPartSize == filesize) {
                    isLastPart = true;
                }
                String dataMd5 = Md5Utils.md5Hex(dataUploaded);
                testUploadPart(key, uploadId, partNum, dataUploaded, dataMd5, isLastPart);
                dataMd5Array.add(dataMd5);
                byteUploaded += currentPartSize;
            }
            List<PartETag> partETags = testListMultipart(key, uploadId, partNum, dataMd5Array);
            testCompleteMultiPart(key, uploadId, partETags, partNum);
            headMultiPartObject(key, filesize, partNum);
            testGetEachPart(key, partSize, filesize, partETags, dataMd5Array);
        } finally {
            clearObject(key);
        }
    }

    protected static TemporyToken fetchTempToken(long durationSeconds) {
        TreeMap<String, Object> config = new TreeMap<String, Object>();
        try {
            // 替换为您的 SecretId
            config.put("SecretId", secretId);
            // 替换为您的 SecretKey
            config.put("SecretKey", secretKey);

            // 临时密钥有效时长，单位是秒，默认1800秒，目前主账号最长2小时（即7200秒），子账号最长36小时（即129600秒）
            config.put("durationSeconds", (int)durationSeconds);

            // 换成您的 bucket
            config.put("bucket", bucket);
            // 换成 bucket 所在地区
            config.put("region", region);

            // 这里改成允许的路径前缀，可以根据自己网站的用户登录态判断允许上传的具体路径，例子：a.jpg 或者 a/* 或者 * 。
            // 如果填写了“*”，将允许用户访问所有资源；除非业务需要，否则请按照最小权限原则授予用户相应的访问权限范围。
            config.put("allowPrefix", "*");

            // 密钥的权限列表。简单上传、表单上传和分片上传需要以下的权限，其他权限列表请看 https://cloud.tencent.com/document/product/436/31923
            String[] allowActions = new String[] {
                    "name/cos:*",
            };
            config.put("allowActions", allowActions);

            JSONObject credentialsJson = CosStsClient.getCredential(config).getJSONObject("credentials");
            String tmpSecretId = credentialsJson.getString("tmpSecretId");
            String tmpSecretKey = credentialsJson.getString("tmpSecretKey");
            String sessionToken = credentialsJson.getString("sessionToken");
            return new TemporyToken(tmpSecretId, tmpSecretKey, sessionToken);
        } catch (Exception e) {
            fail("fetchTempToken occur a exception: " + e.toString());
        }
        return null;
    }
}
