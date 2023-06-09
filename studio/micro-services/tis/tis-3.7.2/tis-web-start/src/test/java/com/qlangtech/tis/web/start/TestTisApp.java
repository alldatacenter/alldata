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
package com.qlangtech.tis.web.start;

import junit.framework.TestCase;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @create: 2020-05-04 11:44
 */
public class TestTisApp extends TestCase {

    public void testLaunchAssemble() throws Exception {
        //
        // TisApp.setWebRootDir(new File("/Users/mozhenghua/j2ee_solution/project/tis-saturn2/tmp"));
        // TisApp.setWebRootDir(new File("/Users/mozhenghua/j2ee_solution/project/tis-saturn2/tmp"));
        System.setProperty(TisApp.KEY_WEB_ROOT_DIR, "../tmp");
        String[] args = new String[0];
        TisApp.main(args);
    }

    public void testGetResources() throws Exception {
        List<URL> urls = new ArrayList<>();
        File dir = new File("target/dependency");
        for (String c : dir.list()) {
            urls.add((new File(dir, c)).toURI().toURL());
        }
        System.out.println("urls size:" + urls.size());
        URLClassLoader cl = new URLClassLoader(urls.toArray(new URL[urls.size()]), this.getClass().getClassLoader());
        Enumeration<URL> resources = cl.getResources("");
        while (resources.hasMoreElements()) {
            System.out.println(resources.nextElement());
        }
    }
}
