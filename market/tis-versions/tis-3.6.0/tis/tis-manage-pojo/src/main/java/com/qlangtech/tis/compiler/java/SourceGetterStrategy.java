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

import javax.tools.JavaFileObject;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-10-21 09:42
 **/
public class SourceGetterStrategy {

    public final boolean getResource;

    public final String childSourceDir;

    public final String sourceCodeExtendsion;

    public SourceGetterStrategy(boolean getResource, String childSourceDir, String sourceCodeExtendsion) {
        this.getResource = getResource;
        this.childSourceDir = childSourceDir;
        this.sourceCodeExtendsion = sourceCodeExtendsion;
    }

    public MyJavaFileObject processMyJavaFileObject(MyJavaFileObject fileObj) {
        return fileObj;
    }

    public JavaFileObject.Kind getSourceKind() {
        return JavaFileObject.Kind.SOURCE;
    }
}
