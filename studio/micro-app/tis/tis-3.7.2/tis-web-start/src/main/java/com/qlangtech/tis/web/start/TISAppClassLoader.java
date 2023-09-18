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

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/09/25
 */
public class TISAppClassLoader extends URLClassLoader {
    private final String context;

    public TISAppClassLoader(String context, ClassLoader parent, URL[] urls) throws IOException {
        super(urls, parent);
        this.context = context;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        try {
            return super.loadClass(name, resolve);
        } catch (ClassNotFoundException e) {
            throw e;
        } catch (Exception e) {
            URL[] urLs = this.getURLs();
            StringBuffer urlsToStr = new StringBuffer("submodule ");
            urlsToStr.append("'").append(context).append("',");
//            for (URL url : urLs) {
//                urlsToStr.append(String.valueOf(url)).append(",");
//            }
            throw new RuntimeException(urlsToStr.toString(), e);
        }
    }
}
