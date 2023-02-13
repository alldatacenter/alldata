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

package com.qlangtech.tis.plugin.datax;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.ObjectInputStream;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-03-29 16:14
 **/
public class T {
    @Test
    public void test() throws Exception {
        ObjectInputStream o = new ObjectInputStream(FileUtils.openInputStream(
                new File("/Users/mozhenghua/j2ee_solution/project/plugins/tis-datax/tis-datax-local-executor/target/classes/META-INF/annotations/com.qlangtech.tis.extension.TISExtension")));

        while (true) {
            Object o1 = o.readObject();
            System.out.println(o1);
            if (o1 == null) {
                break;
            }
        }
    }
}
