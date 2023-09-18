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

import org.apache.commons.lang.StringUtils;

import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Map;
import java.util.jar.JarOutputStream;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2019年10月9日
 */
public class NestClassFileObject extends SimpleJavaFileObject implements IOutputEntry {

    private ByteArrayOutputStream outPutStream;

    private final ZipPath zipPath;

    public NestClassFileObject(ZipPath zipPath) {
        super(URI.create("file:///mock"), Kind.CLASS);
        this.zipPath = zipPath;
    }

    public static NestClassFileObject getNestClassFileObject(String qualifiedClassName, Map<String, IOutputEntry> fileObjects) {
        String pathParent = StringUtils.substringBeforeLast(qualifiedClassName, ".");
        String className = StringUtils.substringAfterLast(qualifiedClassName, ".");
        ZipPath zipPath = new //
                ZipPath(//
                StringUtils.replace(pathParent, ".", "/"), className, Kind.CLASS);
        NestClassFileObject fileObj = new NestClassFileObject(zipPath);
        fileObjects.put(qualifiedClassName, fileObj);
        return fileObj;
    }

    @Override
    public boolean containCompiledClass() {
        return true;
    }

    @Override
    public void processSource(JarOutputStream jaroutput) throws Exception {
    }

    @Override
    public OutputStream openOutputStream() throws IOException {
        outPutStream = new ByteArrayOutputStream();
        return outPutStream;
    }

    public ByteArrayOutputStream getOutputStream() throws IOException {
        if (outPutStream == null) {
            throw new IllegalStateException("outputStream can not be null");
        }
        return outPutStream;
    }

    @Override
    public ZipPath getZipPath() {
        return this.zipPath;
    }

    @Override
    public JavaFileObject getFileObject() {
        return this;
    }
}
