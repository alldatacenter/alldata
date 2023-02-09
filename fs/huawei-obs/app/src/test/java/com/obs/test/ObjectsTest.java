/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.obs.test;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.obs.services.model.ListVersionsRequest;
import org.junit.Test;

import com.obs.services.ObsClient;
import com.obs.services.exception.ObsException;
import com.obs.services.model.CopyObjectResult;
import com.obs.services.model.GetObjectRequest;
import com.obs.services.model.HttpMethodEnum;
import com.obs.services.model.InitiateMultipartUploadRequest;
import com.obs.services.model.InitiateMultipartUploadResult;
import com.obs.services.model.ListVersionsResult;
import com.obs.services.model.ObjectMetadata;
import com.obs.services.model.ObsObject;
import com.obs.services.model.PutObjectRequest;
import com.obs.services.model.SetObjectMetadataRequest;
import com.obs.services.model.TemporarySignatureRequest;
import com.obs.services.model.TemporarySignatureResponse;

public class ObjectsTest {
    private static String bucketName = "osm-bj4";

    @Test
    public void test_set_object_metadata() {
        ObsClient obsClient = TestTools.getPipelineEnvironment();
        SetObjectMetadataRequest request = new SetObjectMetadataRequest(bucketName, "1");
        request.getMetadata().put("property1", "property-value1");
        request.getMetadata().put("property2", "%#123");
        ObjectMetadata metadata = obsClient.setObjectMetadata(request);

        System.out.println(metadata);

        System.out.println(metadata.getUserMetadata("property1"));
        System.out.println(metadata.getMetadata().get("property1"));
    }

    @Test
    public void test_get_object_metadata_1() {
        ObsClient obsClient = TestTools.getPipelineEnvironment();

        ObjectMetadata metadata = obsClient.getObjectMetadata(bucketName, "s3curl.txt");

        System.out.println(metadata);

        assertNotNull(metadata);
    }
    
    @Test
    public void test_get_object_metadata_2() {
        ObsClient obsClient = TestTools.getExternalEnvironment();

        ObjectMetadata metadata = obsClient.getObjectMetadata(bucketName, "/DES1597308083694/第一次备份文件/10.70.3.41/F/F/产品处/02_产品开发/04 结项存档/2012年/银代/金如意D/1130/新光海航金如意D款两全保险（分红型）-67LOADING/新光海航金如意D款两全保险（分红型）精算报告.doc");

        System.out.println(metadata);

        assertNotNull(metadata);
    }

    @Test
    public void test_download_object_metadata() {
        ObsClient obsClient = TestTools.getInnerTempEnvironment();

        ObsObject metadata = obsClient.getObject(bucketName,
                "00d451d484662462a0ea6d7e507dd48c-87-tcmeitulongmixwithobsfunc002-long-1-1");

        System.out.println(metadata);

        assertNotNull(metadata);
    }

    @Test
    public void test_put_object_and_set_acl() {
        ObsClient obsClient = TestTools.getInnerTempEnvironment();

        ObsObject metadata = obsClient.getObject(bucketName,
                "00d451d484662462a0ea6d7e507dd48c-87-tcmeitulongmixwithobsfunc002-long-1-1");

        System.out.println(metadata);

        assertNotNull(metadata);
    }

    @Test
    public void test_put_object_base() {
        ObsClient obsClient = TestTools.getPipelineEnvironment();

        File file = new File("C:\\Users\\xxx\\Desktop\\现网问题\\123\\test.file.css");

        PutObjectRequest request = new PutObjectRequest(bucketName, "test.file.css", file);

        obsClient.putObject(request);
        
    }
    
    @Test
    public void test_put_object_metadata() {
        ObsClient obsClient = TestTools.getPipelineEnvironment();

        File file = new File("D:\\5MB");

        // Case-1
        PutObjectRequest request = new PutObjectRequest(bucketName, "test.file.css", file);

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentDisposition("mytest,file=test");
        metadata.setCacheControl("no-cache-me");
        metadata.setContentEncoding("test-encoding");
        metadata.setContentType("test/type");
        metadata.setContentLanguage("test-language-zh-CN");
        metadata.setExpires("test-expires");
        request.setMetadata(metadata);
        
        obsClient.putObject(request);
        
        ObjectMetadata metadata2 = obsClient.getObjectMetadata(bucketName, "test.file.css");
        
        System.out.println(metadata2);
        assertEquals(metadata2.getContentEncoding(), "test-encoding");
        assertEquals(metadata2.getContentType(), "test/type");
        assertEquals(metadata2.getContentDisposition(), "mytest,file=test");
        assertEquals(metadata2.getCacheControl(), "no-cache-me");
        assertEquals(metadata2.getContentLanguage(), "test-language-zh-CN");
        assertEquals(metadata2.getExpires(), "test-expires");
        
        
        // Case-2
        request = new PutObjectRequest(bucketName, "test.file.css", file);

        metadata = new ObjectMetadata();
        request.setMetadata(metadata);
        
        obsClient.putObject(request);
        
        metadata2 = obsClient.getObjectMetadata(bucketName, "test.file.css");
        System.out.println(metadata2);
        assertEquals(metadata2.getContentEncoding(), null);
        assertEquals(metadata2.getContentDisposition(), null);
        assertEquals(metadata2.getCacheControl(), null);
        assertEquals(metadata2.getExpires(), null);
        assertEquals(metadata2.getContentLanguage(), null);
        assertEquals(metadata2.getContentType(), "text/css");
    }

    @Test
    public void test_get_object_base() throws IOException {
        ObsClient obsClient = TestTools.getExternalEnvironment();

        ObsObject obsObject = obsClient.getObject(bucketName, "/202009070920312367/20200906122041.zip");

        // 读取对象内容
        System.out.println("Object content:");
        InputStream input = obsObject.getObjectContent();

        int byteread = 0;

        FileOutputStream fs = null;
        try {
            fs = new FileOutputStream("C:\\Users\\xxxxxxxxxx\\Desktop\\abc.zip");

            byte[] buffer = new byte[1204];
            int length;
            while ((byteread = input.read(buffer)) != -1) {
                fs.write(buffer, 0, byteread);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            fs.close();
        }
    }
    
    @Test
    public void test_get_object_by_range() throws IOException {
        ObsClient obsClient = TestTools.getExternalEnvironment();

        GetObjectRequest request = new GetObjectRequest("wjb-test-browserplus-cn-north-1", "obs_browser-plus.rar");
        request.setRangeStart(10L);
        request.setRangeEnd(1000L);
        ObsObject obsObject = obsClient.getObject(request);

        // 读取对象内容
        System.out.println("Object content:");
        InputStream input = obsObject.getObjectContent();

        int byteread = 0;

        FileOutputStream fs = null;
        try {
            fs = new FileOutputStream("C:\\Users\\xxxxxxxxxxx\\Desktop\\png\\abc.gif");

            byte[] buffer = new byte[1204];
            int length;
            while ((byteread = input.read(buffer)) != -1) {
                fs.write(buffer, 0, byteread);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            fs.close();
        }
    }

    @Test
    public void test_copy_object() {
        ObsClient obsClient = TestTools.getPipelineEnvironment();

        // ObsObject metadata = obsClient.getObject(bucketName,
        // "00d451d484662462a0ea6d7e507dd48c-87-tcmeitulongmixwithobsfunc002-long-1-1");

        try {
            CopyObjectResult result = obsClient.copyObject("sourcebucketname", "sourceobjectname", "destbucketname",
                    "destobjectname");

            System.out.println("\t" + result.getStatusCode());
            System.out.println("\t" + result.getEtag());
        } catch (ObsException e) {
            System.out.println("HTTP Code: " + e.getResponseCode());
            System.out.println("Error Code:" + e.getErrorCode());
            System.out.println("Error Message: " + e.getErrorMessage());

            System.out.println("Request ID:" + e.getErrorRequestId());
            System.out.println("Host ID:" + e.getErrorHostId());
        }
    }

    @Test
    public void test_temporarySignatureRequest_for_image() {
        ObsClient obsClient = TestTools.getExternalEnvironment();

        long expireSeconds = 3600L;

        TemporarySignatureRequest request = new TemporarySignatureRequest(HttpMethodEnum.GET, expireSeconds);
        request.setBucketName("bucketname");
        request.setObjectKey("objectname.jpg");

        // 设置图片处理参数，对图片依次进行缩放、旋转
        Map<String, Object> queryParams = new HashMap<String, Object>();
        queryParams.put("x-image-process", "image/resize,m_fixed,w_100,h_100/rotate,90");
        request.setQueryParams(queryParams);

        // 生成临时授权URL
        TemporarySignatureResponse response = obsClient.createTemporarySignature(request);
        System.out.println(response.getSignedUrl());
    }

    @Test
    public void initiateMultipartUpload() {
        ObsClient obsClient = TestTools.getExternalEnvironment();

        String objectKey = "initiateMultipartUpload_test_";
        for (int i = 0; i < 1000; i++) {
            InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest("shenqing-test-sdk-obs-0000001",
                    objectKey + "-" + i);
            InitiateMultipartUploadResult result = obsClient.initiateMultipartUpload(request);
            System.out.println(result.getObjectKey() + ";   " + result.getUploadId());
        }
    }
    
    @Test
    public void test_list_version_base() throws IOException {
        ObsClient obsClient = TestTools.getExternalEnvironment();

        ListVersionsResult result = obsClient.listVersions("test-version-002");
    }
}
