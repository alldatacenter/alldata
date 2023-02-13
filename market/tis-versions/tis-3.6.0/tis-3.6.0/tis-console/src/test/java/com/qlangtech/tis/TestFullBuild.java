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
package com.qlangtech.tis;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import com.qlangtech.tis.manage.common.ConfigFileContext.StreamProcess;
import com.qlangtech.tis.manage.common.HttpUtils;
import junit.framework.TestCase;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2017年7月3日
 */
public class TestFullBuild extends TestCase {

    public void testFullBuild() throws Exception {
        URL url = new URL("http://10.1.5.129:8080/trigger?appname=search4xxx&workflow_id=15");
        HttpUtils.processContent(url, new StreamProcess<String>() {

            @Override
            public String p(int status, InputStream stream, Map<String, List<String>> headerFields) {
                try {
                    System.out.println(IOUtils.toString(stream, "utf8"));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return null;
            }
        });
    }
}
