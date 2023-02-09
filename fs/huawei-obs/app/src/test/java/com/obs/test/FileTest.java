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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.obs.services.ObsClient;
import com.obs.services.model.ObjectMetadata;
import com.obs.services.model.fs.NewFolderRequest;
import com.obs.services.model.fs.ObsFSFolder;

public class FileTest {
    @Test
    public void test_create_newfolder_1() {
        ObsClient obsClient = TestTools.getPipelineEnvironment();
        
        String bucketName = "test";
        String objectKey = "%#123";
        NewFolderRequest request = new NewFolderRequest(bucketName, objectKey);
        
        ObsFSFolder folder = obsClient.newFolder(request);
        
        System.out.println(folder);
        
        assertEquals(folder.getObjectKey(), objectKey + "/");
        
        ObjectMetadata metadata1 = obsClient.getObjectMetadata(bucketName, objectKey + "/");
        System.out.println(metadata1);
        
        ObjectMetadata metadata2 = obsClient.getObjectMetadata(bucketName, objectKey);
    }
}
