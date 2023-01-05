/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qlangtech.tis.plugins.hudi;

import com.qlangtech.tis.manage.common.CenterResource;
import com.qlangtech.tis.manage.common.TisUTF8;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hudi.common.fs.IExtraHadoopFileSystemGetter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.ServiceLoader;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-03-02 16:28
 **/
public class TestTISHadoopFileSystemGetter {

    @Before
    public void beforePreMethod() {
        CenterResource.setNotFetchFromCenterRepository();
    }

    @Test
    public void testFileSystem() throws Exception {

        ServiceLoader<IExtraHadoopFileSystemGetter>
                fsGetters = ServiceLoader.load(IExtraHadoopFileSystemGetter.class);
        String hdfsHost = "hdfs://namenode";
        ///user/admin/default/20220302124643/base/meta/schema.avsc

        for (IExtraHadoopFileSystemGetter getter : fsGetters) {
            FileSystem fs = getter.getHadoopFileSystem(hdfsHost);

            try (InputStream reader = fs.open(new Path("/user/admin/default/20220302124643/base/meta/schema.avsc"))) {
                Assert.assertNotNull(reader);
                System.out.println(IOUtils.toString(reader, TisUTF8.get()));
            }

            return;
        }

        Assert.fail("can not reach here");
    }
}
