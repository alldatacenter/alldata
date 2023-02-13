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

package com.qlangtech.tis.config.k8s.impl;

import com.alibaba.citrus.turbine.Context;
import com.qlangtech.tis.extension.impl.IOUtils;
import com.qlangtech.tis.plugin.common.PluginDesc;
import com.qlangtech.tis.runtime.module.misc.IFieldErrorHandler;
import junit.framework.TestCase;
import org.easymock.EasyMock;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-07-15 15:36
 **/
public class TestDefaultK8SImage extends TestCase {

    public void testHostAliasesValidate() {
        //https://blog.csdn.net/weixin_47729423/article/details/114288431
        DefaultK8SImage k8SImage = new DefaultK8SImage();
        DefaultK8SImage.DescriptorImpl descriptor = (DefaultK8SImage.DescriptorImpl) k8SImage.getDescriptor();
        assertNotNull("descriptor can not be null", descriptor);

        IFieldErrorHandler msgHandler = EasyMock.createMock("msgHandler", IFieldErrorHandler.class);
        Context context = EasyMock.createMock("context", Context.class);
        String fieldName = "hostAliases";

        EasyMock.replay(msgHandler, context);
        assertTrue(descriptor.validateHostAliases(msgHandler, context, fieldName
                , IOUtils.loadResourceFromClasspath(DefaultK8SImage.class, "DefaultK8SImage-hostaliases-content.yaml")));

        EasyMock.verify(msgHandler, context);
    }


    public void testPluginDescGenerate() {

        PluginDesc.testDescGenerate(DefaultK8SImage.class, "DefaultK8SImage-desc.json");
    }
}
