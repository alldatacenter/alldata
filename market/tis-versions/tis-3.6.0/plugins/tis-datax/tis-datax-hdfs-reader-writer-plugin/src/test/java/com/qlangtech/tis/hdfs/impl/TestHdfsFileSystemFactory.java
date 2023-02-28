/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.qlangtech.tis.hdfs.impl;

import com.google.common.collect.Sets;
import com.qlangtech.tis.TIS;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.offline.FileSystemFactory;
import junit.framework.TestCase;

import java.util.List;
import java.util.Set;

/**
 * @author: baisui 百岁
 * @create: 2020-11-24 17:50
 **/
public class TestHdfsFileSystemFactory extends TestCase {
    public void testPluginGet() {
        List<Descriptor<FileSystemFactory>> descList
                = TIS.get().getDescriptorList(FileSystemFactory.class);
        assertNotNull(descList);
        assertEquals(2, descList.size());
        Set<String> displayNames = Sets.newHashSet("AliyunOSSFileSystemFactory", "HDFS");
        for (Descriptor<FileSystemFactory> desc : descList) {
            System.out.println(desc.getDisplayName());
        }
    }
}
