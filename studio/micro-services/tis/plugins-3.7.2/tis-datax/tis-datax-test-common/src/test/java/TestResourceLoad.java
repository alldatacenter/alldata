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

import com.qlangtech.tis.TIS;
import junit.framework.TestCase;

import java.io.InputStream;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-07-14 19:21
 **/
public class TestResourceLoad extends TestCase {

    public void testLoad() {
        // hadoop-common 包中的一个资源文件
        String respath = "common-version-info.properties";
        InputStream res = TIS.get().pluginManager.uberClassLoader.getResourceAsStream(respath);
        assertNotNull(respath, res);
    }
}
