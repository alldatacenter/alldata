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

package com.aliyun.oss.testing;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Ignore;
import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;

public class CnameTest {

    @Ignore
    @SuppressWarnings("unused")
    public void testCnameExcludeList() {
        ClientBuilderConfiguration cc = new ClientBuilderConfiguration();
        // Defalut CNAME Exclude List: [.aliyuncs.com, .aliyun-inc.com, localhost]
        List<String> currentExcludeList = cc.getCnameExcludeList();
        Assert.assertEquals(currentExcludeList.size(), 3);
        Assert.assertTrue(currentExcludeList.contains(".aliyuncs.com"));
        Assert.assertTrue(currentExcludeList.contains(".aliyun-inc.com"));
        Assert.assertTrue(currentExcludeList.contains("localhost"));
        
        List<String> cnameExcludeList = new ArrayList<String>();
        String excludeItem = "http://oss-cn-hangzhou.aliyuncs.gd";
        // Add your customized host name here
        cnameExcludeList.add(excludeItem);
        cc.setCnameExcludeList(cnameExcludeList);
        currentExcludeList = cc.getCnameExcludeList();
        Assert.assertEquals(currentExcludeList.size(), 4);
        Assert.assertTrue(currentExcludeList.contains(excludeItem));
        Assert.assertTrue(currentExcludeList.contains(".aliyuncs.com"));
        Assert.assertTrue(currentExcludeList.contains(".aliyun-inc.com"));
        Assert.assertTrue(currentExcludeList.contains("localhost"));
        
        OSS client = new OSSClientBuilder().build("<input your customized host name>", 
                "<input your access id>", "<input your access key>", cc);
        // Do some operations with client here...
    }

}
