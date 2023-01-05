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

package com.qlangtech.tis.datax.impl;

import com.qlangtech.tis.TIS;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.resource.Resource;
import org.apache.velocity.runtime.resource.loader.ResourceLoader;
import org.apache.velocity.util.ExtProperties;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

/**
 * velocity 可以从Plugin中加载模版文件
 *
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-02-21 17:38
 **/
public class TISClasspathResourceLoader extends ResourceLoader {


    @Override
    public void init(ExtProperties configuration) {

    }

    @Override
    public Reader getResourceReader(String source, String encoding) throws ResourceNotFoundException {
        InputStream res = TIS.get().pluginManager.uberClassLoader.getResourceAsStream(source);
        if (res == null) {
            //  throw new IllegalStateException("res:" + source + " relevant stream source can not be null");
            return null;
        }
        try {
            return new InputStreamReader(res, encoding);
        } catch (UnsupportedEncodingException e) {
            throw new ResourceNotFoundException(e);
        }
    }


    @Override
    public boolean isSourceModified(Resource resource) {
        return false;
    }

    @Override
    public long getLastModified(Resource resource) {
        return 0;
    }
}
