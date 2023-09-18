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

import com.qlangtech.tis.TIS;

import java.net.URL;
import java.util.Enumeration;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-02-22 15:14
 **/
public class Test {

    @org.junit.Test
    public void test() throws Exception {
        //
//        Enumeration<URL> resources
//                = Thread.currentThread().getContextClassLoader().getResources("org/codehaus/janino/CompilerFactory.class");
//        while (resources.hasMoreElements()) {
//            System.out.println(resources.nextElement());
//        }



        Enumeration<URL> resources
                =  TIS.get().getPluginManager().uberClassLoader.getResources("com/esotericsoftware/kryo/Serializer.class");
        while (resources.hasMoreElements()) {
            System.out.println(resources.nextElement());
        }



    }
}
