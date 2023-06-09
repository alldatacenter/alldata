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
package com.qlangtech.tis.util;

import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.io.xml.XppDriver;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class XStream2PluginInfoReader extends XStream2 {

    public Field mock;

    public XStream2PluginInfoReader(XppDriver xppDruver) {
        super(xppDruver);
    }

    @Override
    public ReflectionProvider createReflectionProvider() {
        return (ReflectionProvider) Proxy.newProxyInstance(this.getClassLoader(), new Class[] { ReflectionProvider.class }, new InvocationHandler() {

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if ("newInstance".equals(method.getName())) {
                    return new Object();
                }
                if ("getFieldOrNull".equals(method.getName())) {
                    // System.out.println("==============================" + method.getName() + ",args[1]:" + args[1]);
                    if (RobustReflectionConverter.KEY_ATT_PLUGIN.equals(args[1]) || "class".equals(args[1])) {
                        return null;
                    }
                    return getMockField();
                }
                if ("getField".equals(method.getName())) {
                    return getMockField();
                }
                if ("getFieldType".equals(method.getName())) {
                    return Object.class;
                }
                return null;
            }
        });
    // return super.createReflectionProvider();
    }

    private Object getMockField() throws NoSuchFieldException {
        return XStream2PluginInfoReader.class.getField("mock");
    }

    public static void main(String[] args) {
    // XStream2PluginInfoReader pluginInfoReader = new XStream2PluginInfoReader();
    // pluginInfoReader.un
    }
}
