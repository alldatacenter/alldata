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

package com.aliyun.oss.integrationtests;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Date;

import junit.framework.Assert;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;

import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.common.utils.BinaryUtil;
import com.aliyun.oss.common.utils.DateUtil;
import com.aliyun.oss.common.utils.IOUtils;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.GenericResult;
import com.aliyun.oss.model.GetObjectRequest;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.ProcessObjectRequest;
import com.aliyun.oss.utils.ResourceUtils;

/**
 * Testing image process
 */
public class ImageProcessTest extends TestBase {

    final private static String originalImage = "oss/example.jpg";
    final private static String newImage = "oss/new-example.jpg";

    @Override
    public void setUp() throws Exception {
        super.setUp();
        ossClient.putObject(bucketName, originalImage, new File(ResourceUtils.getTestFilename(originalImage)));
    }

    @Override
    public void tearDown() throws Exception {
        ossClient.deleteObject(bucketName, originalImage);
        ossClient.deleteObject(bucketName, newImage);
        super.tearDown();
    }

    @Test
    public void testResizeImage() {
        String style = "image/resize,m_fixed,w_100,h_100";  // 缩放

        try {
            GetObjectRequest request = new GetObjectRequest(bucketName, originalImage);
            request.setProcess(style);

            OSSObject ossObject = ossClient.getObject(request);
            Assert.assertEquals(ossObject.getRequestId().length(), REQUEST_ID_LEN);
            ossClient.putObject(bucketName, newImage, ossObject.getObjectContent());

            ImageInfo imageInfo = getImageInfo(bucketName, newImage);
            Assert.assertEquals(imageInfo.getHeight(), 100);
            Assert.assertEquals(imageInfo.getWidth(), 100);
            Assert.assertEquals(imageInfo.getFormat(), "jpg");

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testCropImage() {
        String style = "image/crop,w_100,h_100,x_100,y_100,r_1"; // 裁剪

        try {
            GetObjectRequest request = new GetObjectRequest(bucketName, originalImage);
            request.setProcess(style);

            OSSObject ossObject = ossClient.getObject(request);
            Assert.assertEquals(ossObject.getRequestId().length(), REQUEST_ID_LEN);
            ossClient.putObject(bucketName, newImage, ossObject.getObjectContent());

            ImageInfo imageInfo = getImageInfo(bucketName, newImage);
            Assert.assertEquals(imageInfo.getHeight(), 100);
            Assert.assertEquals(imageInfo.getWidth(), 100);
            Assert.assertEquals(imageInfo.getFormat(), "jpg");

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testRotateImage() {
        String style = "image/rotate,90"; // 旋转

        try {
            GetObjectRequest request = new GetObjectRequest(bucketName,
                    originalImage);
            request.setProcess(style);

            OSSObject ossObject = ossClient.getObject(request);
            Assert.assertEquals(ossObject.getRequestId().length(), REQUEST_ID_LEN);
            ossClient.putObject(bucketName, newImage, ossObject.getObjectContent());

            ImageInfo imageInfo = getImageInfo(bucketName, originalImage);
            Assert.assertEquals(imageInfo.getHeight(), 267);
            Assert.assertEquals(imageInfo.getWidth(), 400);
            Assert.assertEquals(imageInfo.getSize(), 21839);
            Assert.assertEquals(imageInfo.getFormat(), "jpg");

            imageInfo = getImageInfo(bucketName, newImage);
            Assert.assertEquals(imageInfo.getHeight(), 400);
            Assert.assertEquals(imageInfo.getWidth(), 267);
            Assert.assertEquals(imageInfo.getFormat(), "jpg");

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testSharpenImage() {
        String style = "image/sharpen,100"; // 锐化

        try {
            GetObjectRequest request = new GetObjectRequest(bucketName, originalImage);
            request.setProcess(style);

            OSSObject ossObject = ossClient.getObject(request);
            Assert.assertEquals(ossObject.getRequestId().length(), REQUEST_ID_LEN);
            ossClient.putObject(bucketName, newImage, ossObject.getObjectContent());

            ImageInfo imageInfo = getImageInfo(bucketName, newImage);
            Assert.assertEquals(imageInfo.getHeight(), 267);
            Assert.assertEquals(imageInfo.getWidth(), 400);
            Assert.assertEquals(imageInfo.getFormat(), "jpg");

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testWatermarkImage() {
        String style = "image/watermark,text_SGVsbG8g5Zu-54mH5pyN5YqhIQ"; // 文字水印

        try {
            GetObjectRequest request = new GetObjectRequest(bucketName, originalImage);
            request.setProcess(style);

            OSSObject ossObject = ossClient.getObject(request);
            Assert.assertEquals(ossObject.getRequestId().length(), REQUEST_ID_LEN);
            ossClient.putObject(bucketName, newImage, ossObject.getObjectContent());

            ImageInfo imageInfo = getImageInfo(bucketName, newImage);
            Assert.assertEquals(imageInfo.getHeight(), 267);
            Assert.assertEquals(imageInfo.getWidth(), 400);
            Assert.assertEquals(imageInfo.getFormat(), "jpg");

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testFormatImage() {
        String style = "image/format,png"; // 文字水印

        try {
            GetObjectRequest request = new GetObjectRequest(bucketName, originalImage);
            request.setProcess(style);

            OSSObject ossObject = ossClient.getObject(request);
            Assert.assertEquals(ossObject.getRequestId().length(), REQUEST_ID_LEN);
            ossClient.putObject(bucketName, newImage, ossObject.getObjectContent());

            ImageInfo imageInfo = getImageInfo(bucketName, newImage);
            Assert.assertEquals(imageInfo.getHeight(), 267);
            Assert.assertEquals(imageInfo.getWidth(), 400);
            Assert.assertEquals(imageInfo.getFormat(), "png");

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testProcessObject() {
        StringBuilder styleBuilder = new StringBuilder();
        String saveAsKey = "saveaskey-process.jpg";

        try {
            styleBuilder.append("image/resize,m_fixed,w_100,h_100");  // resize
            styleBuilder.append("|sys/saveas,");
            styleBuilder.append("o_" + BinaryUtil.toBase64String(saveAsKey.getBytes()));
            styleBuilder.append(",");
            styleBuilder.append("b_" + BinaryUtil.toBase64String(bucketName.getBytes()));

            ProcessObjectRequest request = new ProcessObjectRequest(bucketName, originalImage, "");
            request.setProcess(styleBuilder.toString());
            GenericResult processResult = ossClient.processObject(request);
            Assert.assertEquals(processResult.getRequestId().length(), REQUEST_ID_LEN);
            String json = IOUtils.readStreamAsString(processResult.getResponse().getContent(), "UTF-8");
            processResult.getResponse().getContent().close();
            System.out.println(json);
            Assert.assertTrue(json.indexOf("\"status\": \"OK\"") > 0);

            ImageInfo imageInfo = getImageInfo(bucketName, saveAsKey);
            Assert.assertEquals(imageInfo.getHeight(), 100);
            Assert.assertEquals(imageInfo.getWidth(), 100);
            Assert.assertEquals(imageInfo.getFormat(), "jpg");
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testGeneratePresignedUrlWithProcess() {
        String style = "image/resize,m_fixed,w_100,h_100"; // 缩放

        try {
            Date expiration = DateUtil.parseRfc822Date("Wed, 21 Dec 2022 14:20:00 GMT");
            GeneratePresignedUrlRequest req = new GeneratePresignedUrlRequest(bucketName, originalImage, HttpMethod.GET);
            req.setExpiration(expiration);
            req.setProcess(style);

            URL signedUrl = ossClient.generatePresignedUrl(req);
            System.out.println(signedUrl);

            OSSObject ossObject = ossClient.getObject(signedUrl, null);
            ossClient.putObject(bucketName, newImage, ossObject.getObjectContent());

            ImageInfo imageInfo = getImageInfo(bucketName, newImage);
            Assert.assertEquals(imageInfo.getHeight(), 100);
            Assert.assertEquals(imageInfo.getWidth(), 100);
            Assert.assertEquals(imageInfo.getFormat(), "jpg");
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    private static ImageInfo getImageInfo(final String bucket, final String image) throws IOException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, JSONException {
        GetObjectRequest request = new GetObjectRequest(bucketName, image);
        request.setProcess("image/info");
        OSSObject ossObject = ossClient.getObject(request);

        String jsonStr = IOUtils.readStreamAsString(ossObject.getObjectContent(), "UTF-8");
        ossObject.getObjectContent().close();

        JSONObject jsonObject = new JSONObject(jsonStr);

        long height = jsonObject.getJSONObject("ImageHeight").getLong("value");
        long width = jsonObject.getJSONObject("ImageWidth").getLong("value");
        long size = jsonObject.getJSONObject("FileSize").getLong("value");
        String format = jsonObject.getJSONObject("Format").getString("value");
        return new ImageInfo(height, width, size, format);
    }

    static class ImageInfo {

        public ImageInfo(long height, long width, long size, String format) {
            super();
            this.height = height;
            this.width = width;
            this.size = size;
            this.format = format;
        }

        public long getHeight() {
            return height;
        }

        public void setHeight(long height) {
            this.height = height;
        }

        public long getWidth() {
            return width;
        }

        public void setWidth(long width) {
            this.width = width;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        public String getFormat() {
            return format;
        }

        public void setFormat(String format) {
            this.format = format;
        }

        public String toString() {
            return "[height:" + this.height + ",width:" + this.width +
                    ",size:" + this.size + ",format:" + this.format + "]\n";
        }

        private long height;
        private long width;
        private long size;
        private String format;
    }

}
