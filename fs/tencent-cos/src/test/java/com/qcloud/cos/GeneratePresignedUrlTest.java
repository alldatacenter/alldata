package com.qcloud.cos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;

import com.qcloud.cos.auth.AnonymousCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.model.AccessControlList;
import com.qcloud.cos.model.CannedAccessControlList;
import com.qcloud.cos.model.GeneratePresignedUrlRequest;
import com.qcloud.cos.model.ResponseHeaderOverrides;
import com.qcloud.cos.region.Region;
import com.qcloud.cos.utils.DateUtils;
import com.qcloud.cos.utils.Md5Utils;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class GeneratePresignedUrlTest extends AbstractCOSClientTest {
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        AbstractCOSClientTest.initCosClient();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        AbstractCOSClientTest.destoryCosClient();
    }
    
    @Test
    public void testGetFile() throws IOException {
        if (!judgeUserInfoValid()) {
            return;
        }
        long localFileLen = 1024;
        File localFile = buildTestFile(1024);
        String key = "ut/" + localFile.getName();

        putObjectFromLocalFile(localFile, key);

        GeneratePresignedUrlRequest req =
                new GeneratePresignedUrlRequest(bucket, key, HttpMethodName.GET);
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
        req.setResponseHeaders(responseHeaders);
        URL url = cosclient.generatePresignedUrl(req);

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        int responseCode = connection.getResponseCode();
        assertEquals(200, responseCode);
        assertEquals(responseContentType, connection.getContentType());
        assertEquals(localFileLen, connection.getContentLength());
        assertEquals(responseContentLanguage, connection.getHeaderField("Content-Language"));
        assertEquals(responseContentDispositon, connection.getHeaderField("Content-Disposition"));
        assertEquals(responseCacheControl, connection.getHeaderField("Cache-Control"));

        clearObject(key);
        assertTrue(localFile.delete());
    }

    private void testPutFileWithUrl(URL putUrl, File localFile) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) putUrl.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("PUT");

        BufferedOutputStream bos = new BufferedOutputStream(connection.getOutputStream());
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(localFile));
        int readByte = -1;
        while ((readByte = bis.read()) != -1) {
            bos.write(readByte);
        }
        bis.close();
        bos.close();
        int responseCode = connection.getResponseCode();
        assertEquals(200, responseCode);
    }
    
    private void testGetFileWithUrl(URL getUrl, File downloadFile) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) getUrl.openConnection();
        connection.setDoOutput(false);
        connection.setRequestMethod("GET");

        BufferedInputStream bis = new BufferedInputStream(connection.getInputStream());
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(downloadFile));
        int readByte = -1;
        while ((readByte = bis.read()) != -1) {
            bos.write(readByte);
        }
        bis.close();
        bos.close();
        int responseCode = connection.getResponseCode();
        assertEquals(200, responseCode);
    }
    
    private void testDelFileWithUrl(URL delUrl) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) delUrl.openConnection();
        connection.setDoOutput(false);
        connection.setRequestMethod("DELETE");

        int responseCode = connection.getResponseCode();
        assertEquals(204, responseCode);
    }
    
    @Test
    public void testGeneralUrl() throws IOException {
        if (!judgeUserInfoValid()) {
            return;
        }
        String key = "ut/generate_url_test_upload.txt";
        File localFile = buildTestFile(1024);
        File downLoadFile = new File(localFile.getAbsolutePath() + ".down");
        try {
            clientConfig.setHttpProtocol(HttpProtocol.https);
            Date expirationTime = new Date(System.currentTimeMillis() + 30 * 60 * 1000);
            URL putUrl = cosclient.generatePresignedUrl(bucket, key, expirationTime, HttpMethodName.PUT, new HashMap<String, String>(), new HashMap<String, String>());
            URL getUrl = cosclient.generatePresignedUrl(bucket, key, expirationTime, HttpMethodName.GET, new HashMap<String, String>(), new HashMap<String, String>());
            URL delUrl = cosclient.generatePresignedUrl(bucket, key, expirationTime, HttpMethodName.DELETE, new HashMap<String, String>(), new HashMap<String, String>());
            assertTrue(putUrl.toString().startsWith("https://"));
            assertTrue(getUrl.toString().startsWith("https://"));
            assertTrue(delUrl.toString().startsWith("https://"));
            assertTrue(putUrl.toString().contains("sign="));
            assertTrue(getUrl.toString().contains("sign="));
            assertTrue(delUrl.toString().contains("sign="));
            testPutFileWithUrl(putUrl, localFile);
            headSimpleObject(key, localFile.length(), Md5Utils.md5Hex(localFile));
            testGetFileWithUrl(getUrl, downLoadFile);
            assertEquals(localFile.length(), downLoadFile.length());
            assertEquals(Md5Utils.md5Hex(localFile), Md5Utils.md5Hex(downLoadFile));
            testDelFileWithUrl(delUrl);
            assertFalse(cosclient.doesObjectExist(bucket, key));          
        } finally {
            clearObject(key);
            assertTrue(localFile.delete());
            assertTrue(downLoadFile.delete()); 
            clientConfig.setHttpProtocol(HttpProtocol.http);
        }
    }
    
    
    
    @Test
    public void testAnonymousUrl() throws InterruptedException, IOException {
        if (!judgeUserInfoValid()) {
            return;
        }
        AccessControlList oldAcl = cosclient.getBucketAcl(bucket);
        cosclient.setBucketAcl(bucket, CannedAccessControlList.PublicReadWrite);
        Thread.sleep(5000L);
        COSCredentials cred = new AnonymousCOSCredentials();
        ClientConfig anoyClientConfig = new ClientConfig(new Region(region));
        COSClient anoyCOSClient = new COSClient(cred, anoyClientConfig);


        String key = "ut/generate_url_test_upload.txt";
        File localFile = buildTestFile(1024);
        File downLoadFile = new File(localFile.getAbsolutePath() + ".down");
        try {
            Date expirationTime = new Date(System.currentTimeMillis() + 30 * 60 * 1000);
            URL putUrl = anoyCOSClient.generatePresignedUrl(bucket, key, expirationTime, HttpMethodName.PUT, new HashMap<String, String>(), new HashMap<String, String>());
            URL getUrl = anoyCOSClient.generatePresignedUrl(bucket, key, expirationTime, HttpMethodName.GET, new HashMap<String, String>(), new HashMap<String, String>());
            URL delUrl = anoyCOSClient.generatePresignedUrl(bucket, key, expirationTime, HttpMethodName.DELETE, new HashMap<String, String>(), new HashMap<String, String>());
            assertTrue(putUrl.toString().startsWith("https://"));
            assertTrue(getUrl.toString().startsWith("https://"));
            assertTrue(delUrl.toString().startsWith("https://"));
            assertFalse(putUrl.toString().contains("sign="));
            assertFalse(getUrl.toString().contains("sign="));
            assertFalse(delUrl.toString().contains("sign="));
            testPutFileWithUrl(putUrl, localFile);
            headSimpleObject(key, localFile.length(), Md5Utils.md5Hex(localFile));
            testGetFileWithUrl(getUrl, downLoadFile);
            assertEquals(localFile.length(), downLoadFile.length());
            assertEquals(Md5Utils.md5Hex(localFile), Md5Utils.md5Hex(downLoadFile));
            testDelFileWithUrl(delUrl);
            assertFalse(cosclient.doesObjectExist(bucket, key));          
        } finally {
            clearObject(key);
            assertTrue(localFile.delete());
            assertTrue(downLoadFile.delete()); 
            anoyCOSClient.shutdown();
            cosclient.setBucketAcl(bucket, oldAcl);
            Thread.sleep(5000L);
        }
    }
    
    @Test
    public void testTemporyTokenUrl() throws InterruptedException, IOException {
        if (!judgeUserInfoValid()) {
            return;
        }
        COSClient temporyCOSClient = buildTemporyCredentialsCOSClient(1800);
        String key = "ut/generate_url_test_upload.txt";
        File localFile = buildTestFile(1024);
        File downLoadFile = new File(localFile.getAbsolutePath() + ".down");
        try {
            Date expirationTime = new Date(System.currentTimeMillis() + 30 * 60 * 1000);
            URL putUrl = temporyCOSClient.generatePresignedUrl(bucket, key, expirationTime, HttpMethodName.PUT, new HashMap<String, String>(), new HashMap<String, String>());
            URL getUrl = temporyCOSClient.generatePresignedUrl(bucket, key, expirationTime, HttpMethodName.GET, new HashMap<String, String>(), new HashMap<String, String>());
            URL delUrl = temporyCOSClient.generatePresignedUrl(bucket, key, expirationTime, HttpMethodName.DELETE, new HashMap<String, String>(), new HashMap<String, String>());
            assertTrue(putUrl.toString().startsWith("https://"));
            assertTrue(getUrl.toString().startsWith("https://"));
            assertTrue(delUrl.toString().startsWith("https://"));
            assertTrue(putUrl.toString().contains("sign="));
            assertTrue(getUrl.toString().contains("sign="));
            assertTrue(delUrl.toString().contains("sign="));
            assertTrue(putUrl.toString().contains("&" + Headers.SECURITY_TOKEN));
            assertTrue(getUrl.toString().contains("&" + Headers.SECURITY_TOKEN));
            assertTrue(delUrl.toString().contains("&" + Headers.SECURITY_TOKEN));
            testPutFileWithUrl(putUrl, localFile);
            headSimpleObject(key, localFile.length(), Md5Utils.md5Hex(localFile));
            testGetFileWithUrl(getUrl, downLoadFile);
            assertEquals(localFile.length(), downLoadFile.length());
            assertEquals(Md5Utils.md5Hex(localFile), Md5Utils.md5Hex(downLoadFile));
            testDelFileWithUrl(delUrl);
            assertFalse(cosclient.doesObjectExist(bucket, key));          
        } finally {
            clearObject(key);
            assertTrue(localFile.delete());
            assertTrue(downLoadFile.delete()); 
            temporyCOSClient.shutdown();
        }
    }
}
