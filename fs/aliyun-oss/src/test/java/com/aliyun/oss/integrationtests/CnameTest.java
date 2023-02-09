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

import static com.aliyun.oss.integrationtests.TestConfig.*;

import java.util.ArrayList;
import java.util.List;

import com.aliyun.oss.*;
import com.aliyun.oss.common.utils.IOUtils;
import com.aliyun.oss.model.AddBucketCnameRequest;
import com.aliyun.oss.model.CertificateConfiguration;
import com.aliyun.oss.model.CnameConfiguration;
import com.aliyun.oss.utils.ResourceUtils;
import junit.framework.Assert;

import org.junit.Ignore;
import org.junit.Test;

public class CnameTest extends TestBase {

    @Test
    @SuppressWarnings("unused")
    public void testCnameExcludeList() {
        ClientBuilderConfiguration cc = new ClientBuilderConfiguration();
        // Defalut CNAME Exclude List: [aliyuncs.com, aliyun-inc.com, aliyun.com]
        List<String> currentExcludeList = cc.getCnameExcludeList();
        Assert.assertEquals(currentExcludeList.size(), 3);
        Assert.assertTrue(currentExcludeList.contains("aliyuncs.com"));
        Assert.assertTrue(currentExcludeList.contains("aliyun-inc.com"));
        Assert.assertTrue(currentExcludeList.contains("aliyun.com"));

        List<String> cnameExcludeList = new ArrayList<String>();
        String excludeItem = "http://oss-cn-hangzhou.aliyuncs.gd";
        // Add your customized host name here
        cnameExcludeList.add(excludeItem);
        cc.setCnameExcludeList(cnameExcludeList);
        currentExcludeList = cc.getCnameExcludeList();
        Assert.assertEquals(currentExcludeList.size(), 4);
        Assert.assertTrue(currentExcludeList.contains(excludeItem));
        Assert.assertTrue(currentExcludeList.contains("aliyuncs.com"));
        Assert.assertTrue(currentExcludeList.contains("aliyun-inc.com"));
        Assert.assertTrue(currentExcludeList.contains("aliyun.com"));

        OSS client = new OSSClientBuilder().build(OSS_TEST_ENDPOINT, OSS_TEST_ACCESS_KEY_ID, OSS_TEST_ACCESS_KEY_SECRET, cc);
        // Do some operations with client here...
    }

    @Ignore
    @SuppressWarnings("unused")
    public void testPutCname() throws Exception {
        OSS client = new OSSClientBuilder().build(OSS_TEST_ENDPOINT, OSS_TEST_ACCESS_KEY_ID, OSS_TEST_ACCESS_KEY_SECRET);
        String bucketName = "testputcname";
        String cname1 = "cname1.testputcname.com";
        String cname2 = "cname2.testputcname.com";
        try {
            client.deleteBucket(bucketName);
        } catch (ClientException e) {
        }
        client.createBucket(bucketName);

        String pubKey = IOUtils.readStreamAsString(ResourceUtils.getTestInputStream("oss/generic.testputcname.com.crt"), "utf8");
        String priKey = IOUtils.readStreamAsString(ResourceUtils.getTestInputStream("oss/generic.testputcname.com.key"), "utf8");

        AddBucketCnameRequest request1 = new AddBucketCnameRequest(bucketName)
            .withDomain(cname1)
            .withCertificateConfiguration(new CertificateConfiguration()
                .withPublicKey(pubKey)
                .withPrivateKey(priKey));

        String certId1 = client.addBucketCname(request1).getCertId();
        Assert.assertNotNull(certId1);

        AddBucketCnameRequest request2 = new AddBucketCnameRequest(bucketName)
            .withDomain(cname2)
            .withCertificateConfiguration(new CertificateConfiguration()
                .withId(certId1));
        String certId2 = client.addBucketCname(request2).getCertId();
        Assert.assertEquals(certId1, certId2);

        boolean flag = false;
        try {
            client.addBucketCname(request2);
        } catch (OSSException e) {
            Assert.assertEquals("CasRenewCertificateConflict", e.getErrorCode());
            flag = true;
        }
        Assert.assertTrue(flag);

        request2.getCertificateConfiguration().setPreviousId(certId1);
        client.addBucketCname(request2);

        request2.getCertificateConfiguration().setPreviousId(null);
        request2.getCertificateConfiguration().setForceOverwriteCert(true);
        client.addBucketCname(request2);

        List<CnameConfiguration> cnameConfigurations = client.getBucketCname(bucketName);
        Assert.assertEquals(2, cnameConfigurations.size());

        Assert.assertEquals(certId1, cnameConfigurations.get(0).getCertId());
        Assert.assertEquals(certId1, cnameConfigurations.get(1).getCertId());
        Assert.assertEquals(CnameConfiguration.CertType.CAS, cnameConfigurations.get(0).getCertType());
        Assert.assertEquals(CnameConfiguration.CertType.CAS, cnameConfigurations.get(1).getCertType());
    }
}
