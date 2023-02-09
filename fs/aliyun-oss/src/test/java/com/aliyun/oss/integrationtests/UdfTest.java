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

import com.aliyun.oss.model.*;
import junit.framework.Assert;

import java.io.*;
import java.util.Date;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import com.aliyun.oss.common.utils.DateUtil;


public class UdfTest extends TestBase {

    @Test
    public void testUdf() {
        String udf = "udf-go-pingpong-1";
        String desc = "udf-go-pingpong-1";

        try {
            // create udf
            CreateUdfRequest createUdfRequest = new CreateUdfRequest(udf);
            createUdfRequest = new CreateUdfRequest(udf, "", desc);
            Assert.assertEquals(createUdfRequest.getId(),"");
            createUdfRequest = new CreateUdfRequest(udf, desc);
            createUdfRequest.setDesc("desc");
            createUdfRequest.setId("id");
            Assert.assertEquals(createUdfRequest.getDesc(),"desc");
            Assert.assertEquals(createUdfRequest.getId(),"id");
            ossClient.createUdf(createUdfRequest);
            Assert.fail("Udf API is removed.");
        } catch (Exception e) {
            Assert.assertTrue(true);
        }

        try {
            // get udf
            UdfGenericRequest genericRequest = new UdfGenericRequest(udf);
            genericRequest = new UdfGenericRequest();
            genericRequest.setName("name");
            Assert.assertEquals(genericRequest.getName(), "name");
            UdfInfo ui = ossClient.getUdfInfo(genericRequest);
            Assert.fail("Udf API is removed.");
        } catch (Exception e) {
            Assert.assertTrue(true);
        }

        try {
            Date date = DateUtil.parseRfc822Date("Wed, 15 Mar 2017 03:23:45 GMT");
            UdfImageInfo imageInfo = new UdfImageInfo(1, "status", "desc",
                    "region", date);
            imageInfo.setVersion(2);
            imageInfo.setStatus("new status");
            imageInfo.setDesc("new desc");
            imageInfo.setCanonicalRegion("new region");
            imageInfo.setCreationDate(date);
            Assert.assertEquals(imageInfo.getVersion(), new Integer(2));
            Assert.assertEquals(imageInfo.getStatus(), "new status");
            Assert.assertEquals(imageInfo.getDesc(), "new desc");
            Assert.assertEquals(imageInfo.getCanonicalRegion(), "new region");
            Assert.assertEquals(imageInfo.getCreationDate(), date);
            String dump = imageInfo.toString();

            // list image info
            UdfGenericRequest genericRequest = new UdfGenericRequest(udf);
            List<UdfImageInfo> udfImages = ossClient.getUdfImageInfo(genericRequest);
            Assert.fail("Udf API is removed.");
        } catch (Exception e) {
            Assert.assertTrue(true);
        }

        try {
            Date date = DateUtil.parseRfc822Date("Wed, 15 Mar 2017 03:23:45 GMT");
            UdfInfo info = new UdfInfo("name", "owner", "id",
                    "desc", CannedUdfAcl.Private, date);
            info.setName("new name");
            info.setOwner("new owner");
            info.setId("new id");
            info.setDesc("new desc");
            info.setAcl(CannedUdfAcl.parse("public"));
            info.setCreationDate(date);
            Assert.assertEquals(info.getName(), "new name");
            Assert.assertEquals(info.getOwner(), "new owner");
            Assert.assertEquals(info.getId(), "new id");
            Assert.assertEquals(info.getDesc(), "new desc");
            Assert.assertEquals(info.getCreationDate(), date);
            Assert.assertEquals(info.getAcl(), CannedUdfAcl.Public);
            String dump = info.toString();
            // list image info
            List<UdfInfo> udfs = ossClient.listUdfs();
            Assert.fail("Udf API is removed.");
        } catch (Exception e) {
            Assert.assertTrue(true);
        }

        try {
            UdfGenericRequest genericRequest = new UdfGenericRequest(udf);
            ossClient.deleteUdf(genericRequest);
            Assert.fail("Udf API is removed.");
        } catch (Exception e) {
            Assert.assertTrue(true);
        }

        try {
            CannedUdfAcl.parse("UN");
            Assert.assertTrue(false);
        } catch (Exception e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testUdfImage() {
        String udf = "udf-go-pingpong-1";
        String desc = "udf-go-pingpong-1";

        try {
            // upload image
            String content = "Hello OSS";
            InputStream input = new ByteArrayInputStream(content.getBytes());
            UploadUdfImageRequest uploadUdfImageRequest = new UploadUdfImageRequest(udf, null);
            uploadUdfImageRequest = new UploadUdfImageRequest(udf, desc,null);
            uploadUdfImageRequest = new UploadUdfImageRequest(udf, desc,null);
            uploadUdfImageRequest.setUdfImage(input);
            uploadUdfImageRequest.setUdfImageDesc("desc");
            Assert.assertEquals(uploadUdfImageRequest.getUdfImageDesc(),"desc");
            Assert.assertEquals(uploadUdfImageRequest.getUdfImage(),input);
            ossClient.uploadUdfImage(uploadUdfImageRequest);
            Assert.fail("Udf API is removed.");
        } catch (Exception e) {
            Assert.assertTrue(true);
        }

        try {
            // upload image
            UdfGenericRequest genericRequest = new UdfGenericRequest(udf);
            ossClient.deleteUdfImage(genericRequest);
            Assert.fail("Udf API is removed.");
        } catch (Exception e) {
            Assert.assertTrue(true);
        }
    }
    
    @Test
    public void testUdfApplication() {
        String udf = "udf-go-pingpong-1";
        String desc = "udf-go-pingpong-1";

        try {
            // list applications
            List<UdfApplicationInfo> appInfos = ossClient.listUdfApplications();
            for (UdfApplicationInfo app : appInfos) {
                System.out.println(app);
            }
            Assert.fail("Udf API is removed.");
        } catch (Exception e) {
            Assert.assertTrue(true);
        }

        try {
            // create application
            InstanceFlavor flavor = new InstanceFlavor("ecs.n1.middle");
            Assert.assertEquals(flavor.getInstanceType(), "ecs.n1.middle");
            Assert.assertEquals(flavor.toString(), "InstanceFlavor [instanceType=ecs.n1.middle]");
            UdfApplicationConfiguration configuration = new UdfApplicationConfiguration(1, 1,flavor);
            configuration = new UdfApplicationConfiguration(1, 1);
            configuration.setImageVersion(2);
            configuration.setInstanceNum(2);
            InstanceFlavor flavor2 = new InstanceFlavor("ecs.n1.big");
            configuration.setFlavor(flavor2);
            Assert.assertEquals(configuration.getImageVersion(),new Integer(2));
            Assert.assertEquals(configuration.getInstanceNum(),new Integer(2));
            Assert.assertEquals(configuration.getFlavor(),flavor2);
            CreateUdfApplicationRequest createUdfApplicationRequest = new CreateUdfApplicationRequest(udf, configuration);
            configuration = createUdfApplicationRequest.getUdfApplicationConfiguration();
            createUdfApplicationRequest.setUdfApplicationConfiguration(configuration);
            ossClient.createUdfApplication(createUdfApplicationRequest);
            Assert.fail("Udf API is removed.");
        } catch (Exception e) {
            Assert.assertTrue(true);
        }

        try {
            //
            Date startTime = DateUtil.parseRfc822Date("Wed, 15 Mar 2017 03:23:45 GMT");
            InstanceFlavor flavor = new InstanceFlavor("ecs.n1.middle");
            UdfApplicationInfo appInfo = new UdfApplicationInfo("name", "id", "region",
                    "status", 1, 2, startTime, flavor);
            appInfo.setName("new name");
            appInfo.setId("new id");
            appInfo.setRegion("new region");
            appInfo.setStatus("new status");
            appInfo.setImageVersion(2);
            appInfo.setInstanceNum(3);
            appInfo.setFlavor(flavor);
            appInfo.setCreationDate(startTime);
            Assert.assertEquals(appInfo.getName(),"new name");
            Assert.assertEquals(appInfo.getId(),"new id");
            Assert.assertEquals(appInfo.getRegion(),"new region");
            Assert.assertEquals(appInfo.getStatus(),"new status");
            Assert.assertEquals(appInfo.getImageVersion(),new Integer(2));
            Assert.assertEquals(appInfo.getInstanceNum(),new Integer(3));
            Assert.assertEquals(appInfo.getFlavor(),flavor);
            Assert.assertEquals(appInfo.getCreationDate(),startTime);
            String dump = appInfo.toString();
            // get application info
            UdfGenericRequest genericRequest = new UdfGenericRequest(udf);
            appInfo = ossClient.getUdfApplicationInfo(genericRequest);
            Assert.fail("Udf API is removed.");
        } catch (Exception e) {
            Assert.assertTrue(true);
        }

        try {
            // upgrade application
            UpgradeUdfApplicationRequest upgradeUdfApplicationRequest = new UpgradeUdfApplicationRequest(udf, 2);
            upgradeUdfApplicationRequest.setImageVersion(3);
            Assert.assertEquals(upgradeUdfApplicationRequest.getImageVersion(),new Integer(3));
            ossClient.upgradeUdfApplication(upgradeUdfApplicationRequest);
            Assert.fail("Udf API is removed.");
        } catch (Exception e) {
            Assert.assertTrue(true);
        }

        try {
            // resize application
            ResizeUdfApplicationRequest resizeUdfApplicationRequest = new ResizeUdfApplicationRequest(udf, 2);
            resizeUdfApplicationRequest.setInstanceNum(3);
            Assert.assertEquals(resizeUdfApplicationRequest.getInstanceNum(),new Integer(3));
            ossClient.resizeUdfApplication(resizeUdfApplicationRequest);
            Assert.fail("Udf API is removed.");
        } catch (Exception e) {
            Assert.assertTrue(true);
        }

        try {
            // get application log
            GetUdfApplicationLogRequest getUdfApplicationLogRequest = new GetUdfApplicationLogRequest(udf,
                    DateUtil.parseRfc822Date("Wed, 15 Mar 2017 02:23:45 GMT"), 200L);
            Assert.assertEquals(getUdfApplicationLogRequest.getEndLines(),new Long(200L));
            getUdfApplicationLogRequest = new GetUdfApplicationLogRequest(udf, 100L);
            getUdfApplicationLogRequest = new GetUdfApplicationLogRequest(udf);
            Date startTime = DateUtil.parseRfc822Date("Wed, 15 Mar 2017 03:23:45 GMT");
            getUdfApplicationLogRequest.setStartTime(startTime);
            getUdfApplicationLogRequest.setEndLines(100L);
            Assert.assertEquals(getUdfApplicationLogRequest.getEndLines(),new Long(100L));
            Assert.assertEquals(getUdfApplicationLogRequest.getStartTime(),startTime);

            UdfApplicationLog udfApplicationLog = new UdfApplicationLog();
            udfApplicationLog.setUdfName("name");
            udfApplicationLog.setLogContent(null);
            Assert.assertEquals(udfApplicationLog.getUdfName(),"name");
            Assert.assertEquals(udfApplicationLog.getLogContent(), null);
            udfApplicationLog.close();
            udfApplicationLog = new UdfApplicationLog("name", new ByteArrayInputStream("".getBytes()));
            udfApplicationLog.close();
            udfApplicationLog = new UdfApplicationLog("name");
            udfApplicationLog = ossClient.getUdfApplicationLog(getUdfApplicationLogRequest);
            Assert.fail("Udf API is removed.");
        } catch (Exception e) {
            Assert.assertTrue(true);
        }

        try {
            // delete application
            UdfGenericRequest genericRequest = new UdfGenericRequest(udf);
            ossClient.deleteUdfApplication(genericRequest);
            Assert.fail("Udf API is removed.");
        } catch (Exception e) {
            Assert.assertTrue(true);
        }
    }

    private static void displayTextInputStream(InputStream input) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        int lines = 0;
        while (true) {
            String line = reader.readLine();
            if (line == null) break;
            
            lines++;
            System.out.println("    " + line);
        }
        System.out.println("Lines:" + lines);
        
        reader.close();
    }
}
