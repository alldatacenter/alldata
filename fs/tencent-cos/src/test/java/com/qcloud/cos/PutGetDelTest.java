package com.qcloud.cos;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.apache.http.client.CredentialsProvider;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.qcloud.cos.auth.COSCredentialsProvider;
import com.qcloud.cos.auth.AnonymousCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.DeleteObjectRequest;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.SSECOSKeyManagementParams;
import com.qcloud.cos.model.SSECustomerKey;
import com.qcloud.cos.region.Region;
import com.qcloud.cos.utils.Md5Utils;
import com.qcloud.cos.utils.StringUtils;
import com.qcloud.cos.utils.UrlEncoderUtils;

public class PutGetDelTest extends AbstractCOSClientTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        AbstractCOSClientTest.initCosClient();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        AbstractCOSClientTest.destoryCosClient();
    }


    private void testPutObjectByStreamDiffSize(long size) throws IOException {
        if (!judgeUserInfoValid()) {
            return;
        }
        testPutObjectByStreamDiffSize(size, new ObjectMetadata());
    }


    @Test
    public void testPutGetDelObjectNameContainSpecialLetter() throws IOException {
        File localFile = buildTestFile(0L);
        File downLoadFile = new File(localFile.getAbsolutePath() + ".down");
        // 目前server端有bug, 不支持冒号，问号，待修复后添加上
        String key =
                "→↓←→↖↗↙↘!? /\"#$%&()*+',-./0123456789;<=>@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~";
        testPutGetObjectAndClear(key, localFile, downLoadFile);
    }

    @Test
    public void testPutGetDelObjectNameContainChinese() throws IOException {
        if (!judgeUserInfoValid()) {
            return;
        }
        File localFile = buildTestFile(1L);
        File downLoadFile = new File(localFile.getAbsolutePath() + ".down");
        String key = "ut/" + "测试文件.png";
        testPutGetObjectAndClear(key, localFile, downLoadFile);
    }


    @Test
    public void testPutGetDelHttps() throws IOException {
        if (!judgeUserInfoValid()) {
            return;
        }
        clientConfig.setHttpProtocol(HttpProtocol.https);
        testPutGetDelObjectDiffSize(1L);
        clientConfig.setHttpProtocol(HttpProtocol.http);
    }

    // 测试从本地上传文件
    @Test
    public void testPutGetDelObjectEmpty() throws CosServiceException, IOException {
        testPutGetDelObjectDiffSize(0L);
    }

    @Test
    public void testPutGetDelObject256k() throws CosServiceException, IOException {
        testPutGetDelObjectDiffSize(256 * 1024L);
    }

    @Test
    public void testPutGetDelObject1M() throws CosServiceException, IOException {
        testPutGetDelObjectDiffSize(1024 * 1024L);
    }

    @Test
    public void testPutGetDelObject4M() throws CosServiceException, IOException {
        testPutGetDelObjectDiffSize(4 * 1024 * 1024L);
    }

    @Test
    public void testPutGetDelObject32M() throws CosServiceException, IOException {
        testPutGetDelObjectDiffSize(32 * 1024 * 1024L);
    }

    @Ignore
    public void testPutGetDelObject100M() throws CosServiceException, IOException {
        testPutGetDelObjectDiffSize(100 * 1024 * 1024L);
    }

    // 测试从流上传文件
    @Test
    public void testPutObjectByStreamEmpty() throws IOException {
        testPutObjectByStreamDiffSize(0L);
    }

    @Test
    public void testPutObjectByStream256K() throws IOException {
        testPutObjectByStreamDiffSize(256 * 1024L);
    }

    @Test
    public void testPutObjectByStream1M() throws IOException {
        testPutObjectByStreamDiffSize(1024 * 1024L);
    }

    @Test
    public void testPutObjectByStream4M() throws IOException {
        testPutObjectByStreamDiffSize(4 * 1024 * 1024L);
    }

    @Test
    public void testPutObjectByStream32M() throws IOException {
        testPutObjectByStreamDiffSize(32 * 1024 * 1024L);
    }

    @Ignore
    public void testPutObjectByStream100M() throws IOException {
        testPutObjectByStreamDiffSize(100 * 1024 * 1024L);
    }


    // 测试只上传流中的一部分字节
    @Test
    public void testPutObjectByTruncateSize_0() throws IOException {
        testPutObjectByTruncateDiffSize(4 * 1024 * 1024, 0L);
    }

    @Test
    public void testPutObjectByTruncateSize_1M() throws IOException {
        testPutObjectByTruncateDiffSize(4 * 1024 * 1024, 1024 * 1024L);
    }

    @Test
    public void testPutObjectByTruncateSize_8M() throws IOException {
        testPutObjectByTruncateDiffSize(32 * 1024 * 1024, 8 * 1024 * 1024L - 1);
    }

    @Test
    public void testPutObjectWithChineseContentDisposition() throws IOException {
        ObjectMetadata originObjectMeta = new ObjectMetadata();
        String disposition = "attachment;filename=\""
                + UrlEncoderUtils.encode(new String("测试文件.txt".getBytes(), "UTF-8")) + ".jpg\"";
        originObjectMeta.setContentDisposition(disposition);
        testPutObjectByStreamDiffSize(0L, originObjectMeta);
    }

    @Test
    public void testPutObjectWithContentType() throws IOException {
        ObjectMetadata originObjectMeta = new ObjectMetadata();
        originObjectMeta.setContentType("image/tiff");
        testPutObjectByStreamDiffSize(0L, originObjectMeta);
    }

    @Test
    public void testPutObjectWithGzipEncoding() throws IOException {
        ObjectMetadata originObjectMeta = new ObjectMetadata();
        originObjectMeta.setContentEncoding("gzip");
        testPutObjectByStreamDiffSize(1024 * 1024L, originObjectMeta);
    }

    @Test
    public void testPutObjectWithServerSideEncryption() throws IOException {
        useServerEncryption = true;
        ObjectMetadata originObjectMeta = new ObjectMetadata();
        originObjectMeta.setServerSideEncryption("AES256");
        testPutObjectByStreamDiffSize(1L, originObjectMeta);
        useServerEncryption = false;
    }

    @Test
    public void testDeleteNotExistObject() throws IOException {
        if (!judgeUserInfoValid()) {
            return;
        }
        String key = "ut/not-exist.txt";
        try {
            cosclient.deleteObject(bucket, key);
        } catch (CosServiceException e) {
            fail(e.toString());
        }
    }

    @Test
    public void testGetObjectIfMatchWrongEtag() throws IOException {
        if (!judgeUserInfoValid()) {
            return;
        }
        File localFile = buildTestFile(1024);
        String key = "ut/" + localFile.getName();
        cosclient.putObject(bucket, key, localFile);
        try {
            String fileEtag = Md5Utils.md5Hex(localFile);
            // 打乱一下，得到一个错误的etag
            String wrongEtag = fileEtag.substring(5) + fileEtag.substring(0, 5);
            GetObjectRequest getObjectRequest = new GetObjectRequest(bucket, key);
            List<String> eTagList = new ArrayList<>();
            eTagList.add(wrongEtag);
            getObjectRequest.setMatchingETagConstraints(eTagList);

            COSObject cosObject = cosclient.getObject(getObjectRequest);
            assertNull(cosObject);
        } catch (CosServiceException cse) {
            fail(cse.toString());
        } finally {
            cosclient.deleteObject(bucket, key);
            assertTrue(localFile.delete());
        }
    }

    @Test
    public void testGetObjectIfMatchRightEtag() throws IOException {
        if (!judgeUserInfoValid()) {
            return;
        }
        File localFile = buildTestFile(1024);
        String key = "ut/" + localFile.getName();
        cosclient.putObject(bucket, key, localFile);
        try {
            String fileEtag = Md5Utils.md5Hex(localFile);
            GetObjectRequest getObjectRequest = new GetObjectRequest(bucket, key);
            List<String> eTagList = new ArrayList<>();
            eTagList.add("\"" + fileEtag + "\"");
            getObjectRequest.setMatchingETagConstraints(eTagList);

            COSObject cosObject = cosclient.getObject(getObjectRequest);
            assertNotNull(cosObject);
        } catch (CosServiceException cse) {
            fail(cse.toString());
        } finally {
            cosclient.deleteObject(bucket, key);
            assertTrue(localFile.delete());
        }
    }

    @Ignore
    public void testGetObjectIfMatchContainRightEtag() throws IOException {
        if (!judgeUserInfoValid()) {
            return;
        }
        File localFile = buildTestFile(1024);
        String key = "ut/" + localFile.getName();
        cosclient.putObject(bucket, key, localFile);
        try {
            String fileEtag = Md5Utils.md5Hex(localFile);
            String wrongEtag = fileEtag.substring(5) + fileEtag.substring(0, 5);
            GetObjectRequest getObjectRequest = new GetObjectRequest(bucket, key);
            List<String> eTagList = new ArrayList<>();
            eTagList.add("\"" + wrongEtag + "\"");
            eTagList.add("\"" + fileEtag + "\"");
            getObjectRequest.setMatchingETagConstraints(eTagList);

            COSObject cosObject = cosclient.getObject(getObjectRequest);
            assertNotNull(cosObject);
        } catch (CosServiceException cse) {
            fail(cse.toString());
        } finally {
            cosclient.deleteObject(bucket, key);
            assertTrue(localFile.delete());
        }
    }

    @Test
    public void testGetObjectIfNoneMatchRightEtag() throws IOException {
        if (!judgeUserInfoValid()) {
            return;
        }
        File localFile = buildTestFile(1024);
        String key = "ut/" + localFile.getName();
        cosclient.putObject(bucket, key, localFile);
        try {
            String fileEtag = Md5Utils.md5Hex(localFile);
            GetObjectRequest getObjectRequest = new GetObjectRequest(bucket, key);
            List<String> eTagList = new ArrayList<>();
            eTagList.add("\"" + fileEtag + "\"");
            getObjectRequest.setNonmatchingETagConstraints(eTagList);

            COSObject cosObject = cosclient.getObject(getObjectRequest);
            assertNull(cosObject);
        } catch (CosServiceException cse) {
            fail(cse.toString());
        } finally {
            cosclient.deleteObject(bucket, key);
            assertTrue(localFile.delete());
        }
    }

    @Test
    public void testGetObjectIfNoneMatchWrongEtag() throws IOException {
        if (!judgeUserInfoValid()) {
            return;
        }
        File localFile = buildTestFile(1024);
        String key = "ut/" + localFile.getName();
        cosclient.putObject(bucket, key, localFile);
        try {
            String fileEtag = Md5Utils.md5Hex(localFile);
            String wrongEtag = fileEtag.substring(5) + fileEtag.substring(0, 5);
            GetObjectRequest getObjectRequest = new GetObjectRequest(bucket, key);
            List<String> eTagList = new ArrayList<>();
            eTagList.add("\"" + wrongEtag + "\"");
            getObjectRequest.setNonmatchingETagConstraints(eTagList);

            COSObject cosObject = cosclient.getObject(getObjectRequest);
            assertNotNull(cosObject);
        } catch (CosServiceException cse) {
            fail(cse.toString());
        } finally {
            cosclient.deleteObject(bucket, key);
            assertTrue(localFile.delete());
        }
    }

    @Ignore
    public void testPutGetDelObjectWithSSEKey() throws NoSuchAlgorithmException, IOException {
        KeyGenerator symKeyGenerator = KeyGenerator.getInstance("AES");
        symKeyGenerator.init(128);
        SecretKey symKey = symKeyGenerator.generateKey();
        SSECustomerKey sseCKey = new SSECustomerKey(symKey);

        File localFile = buildTestFile(1025);
        String key = "ut/" + localFile.getName();
        File downLoadFile = new File(localFile.getAbsolutePath() + ".down");

        try {
            useServerEncryption = true;
            testPutGetObjectAndClear(key, localFile, downLoadFile, sseCKey, null);
        } finally {
            useServerEncryption = false;
        }
    }

    @Test
    public void testTemporyTokenPutGetDel() throws CosServiceException, IOException {
        COSClient normalClient = cosclient;
        COSClient temporyCOSClient = buildTemporyCredentialsCOSClient(1800L);
        try {
            cosclient = temporyCOSClient;
            testPutGetDelObjectEmpty();
            testPutGetDelObject256k();
            testPutGetDelObject32M();
            testPutGetDelHttps();
            testPutGetDelObjectNameContainChinese();
        } finally {
            cosclient = normalClient;
            temporyCOSClient.shutdown();
        }
    }

    @Test
    public void testTemporyTokenExpired() throws CosServiceException, IOException, InterruptedException {
        COSClient normalClient = cosclient;
        COSClient temporyCOSClient = buildTemporyCredentialsCOSClient(10);
        try {
            cosclient = temporyCOSClient;
            try {
                testPutGetDelObjectEmpty();
            } catch (Exception e) {
                fail(e.toString());
            }
            Thread.sleep(20000L);
            try {
                testPutGetDelObject256k();
            } catch (CosServiceException cse) {
                assertEquals(403, cse.getStatusCode());
                return;
            } catch (Exception e) {
                fail(e.toString());
            }
            fail();
        } finally {
            cosclient = normalClient;
            temporyCOSClient.shutdown();
        }
    }

    private void putObjectFromString(String key, String content) throws IOException {
        if (!judgeUserInfoValid()) {
            return;
        }
        File downloadFile = buildTestFile(0L);
        try {
            String contentEtag = Md5Utils.md5Hex(content);
            PutObjectResult putObjectResult = cosclient.putObject(bucket, key, content);
            String etag = putObjectResult.getETag();
            assertEquals(!useClientEncryption, contentEtag.equals(etag));
            headSimpleObject(key, content.getBytes(StringUtils.UTF8).length, contentEtag);
            getObject(key, downloadFile, null, content.getBytes(StringUtils.UTF8).length,
                    contentEtag);
            cosclient.getObjectMetadata(bucket, key);
        } finally {
            cosclient.deleteObject(bucket, key);
            assertTrue(downloadFile.delete());
        }

    }

    @Test
    public void testPutObjectWithContent() throws CosServiceException, IOException {
        putObjectFromString("ut/content_put_empty", "");
        putObjectFromString("ut/content_put_chinese中文", "中文做个测试*xxfafa///   \\\\cafa* ==++ --^%$#@#!()*测试结束");
    }

    @Test
    public void testPutObjectWithNullParam() throws CosServiceException, IOException {
        try {
            cosclient.putObject(null, "xxx/key.txt", "");
        } catch (IllegalArgumentException iae) {
            return;
        } catch (Exception e) {
            fail(e.toString());
        }

        try {
            cosclient.putObject(bucket, null, "");
        } catch (IllegalArgumentException iae) {
            return;
        } catch (Exception e) {
            fail(e.toString());
        }

        try {
            String content = null;
            cosclient.putObject(bucket, "ut/xx", content);
        } catch (IllegalArgumentException iae) {
            return;
        } catch (Exception e) {
            fail(e.toString());
        }
    }

    @Test
    public void testCachedTemporyTokenCredentialsProviderPutGetDel() throws CosServiceException, IOException, InterruptedException {
        COSClient normalClient = cosclient;
        CachedTemporyTokenCredentialsProvider credentialsProvider = new CachedTemporyTokenCredentialsProvider(5, 10);
        COSClient temporyCOSClient = new COSClient(credentialsProvider, new ClientConfig(new Region(region)));
        try {
            cosclient = temporyCOSClient;
            testPutGetDelObjectEmpty();
            Thread.sleep(15000L);
            testPutGetDelObject256k();
            testPutGetDelHttps();
            testPutGetDelObjectNameContainChinese();
        } finally {
            cosclient = normalClient;
            temporyCOSClient.shutdown();
            credentialsProvider.close();
        }
    }

    @Test
    public void testRequestSpecifiedTmpKeyInfoPutGetDel() throws CosServiceException, IOException, InterruptedException {
        COSClient cosclient = buildTemporyCredentialsCOSClient(1800L);
        File localFile = buildTestFile(1024L);
        COSCredentials cosCredentials = new BasicCOSCredentials(secretId, secretKey);
        try {
            String key = "ut/request-specified-key";
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucket, key, localFile);
            putObjectRequest.setCosCredentials(cosCredentials);
            cosclient.putObject(putObjectRequest);
            GetObjectRequest getObjectRequest = new GetObjectRequest(bucket, key);
            getObjectRequest.setCosCredentials(cosCredentials);
            cosclient.getObject(getObjectRequest);
            DeleteObjectRequest deleteObjectRequest = new DeleteObjectRequest(bucket, key);
            deleteObjectRequest.setCosCredentials(cosCredentials);
            cosclient.deleteObject(deleteObjectRequest);
        } finally {
            localFile.delete();
            cosclient.shutdown();
        }
    }

    @Test
    public void testRequestSpecifiedKeyInfoPutGetDel() throws CosServiceException, IOException, InterruptedException { 
        COSClient cosclient = new COSClient(new AnonymousCOSCredentials(), clientConfig);
        File localFile = buildTestFile(1024L);
        COSCredentials cosCredentials = new BasicCOSCredentials(secretId, secretKey);

        try {
            String key = "ut/request-specified-key";
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucket, key, localFile);
            putObjectRequest.setCosCredentials(cosCredentials);
            cosclient.putObject(putObjectRequest);
            GetObjectRequest getObjectRequest = new GetObjectRequest(bucket, key);
            getObjectRequest.setCosCredentials(cosCredentials);
            cosclient.getObject(getObjectRequest);
            DeleteObjectRequest deleteObjectRequest = new DeleteObjectRequest(bucket, key);
            deleteObjectRequest.setCosCredentials(cosCredentials);
            cosclient.deleteObject(deleteObjectRequest);
        } finally {
            localFile.delete();
            cosclient.shutdown();
        }
    }
}
