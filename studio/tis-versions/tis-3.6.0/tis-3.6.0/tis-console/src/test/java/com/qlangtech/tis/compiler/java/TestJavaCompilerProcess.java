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
package com.qlangtech.tis.compiler.java;

import com.qlangtech.tis.plugin.ds.DBConfig;
import junit.framework.TestCase;
import java.io.File;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class TestJavaCompilerProcess extends TestCase {

    public void testCreateJar() throws Exception {
        File rootDir = new File("/opt/data/dao/shop/20191025105833/src/main");
        File classpathDir = new File("/Users/mozhenghua/Desktop/j2ee_solution/project/tis-ibatis/target/dependency");
        DBConfig dbConfig = new DBConfig();
        dbConfig.setName("shop");
        JavaCompilerProcess compilerProcess = new JavaCompilerProcess(dbConfig, rootDir, classpathDir);
        compilerProcess.compileAndBuildJar();
    }
}
