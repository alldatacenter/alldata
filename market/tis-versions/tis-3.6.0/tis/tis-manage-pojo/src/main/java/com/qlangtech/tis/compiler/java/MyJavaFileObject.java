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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import java.io.*;
import java.nio.charset.Charset;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2019年6月6日
 */
public class MyJavaFileObject extends SimpleJavaFileObject implements IOutputEntry {

    // private String source;
    private ByteArrayOutputStream outPutStream;

    private File sourceFile;

    private static final Charset utf8 = Charset.forName("utf8");

    private final ZipPath zipPath;

    // 是否含有编译后的字节码
    private final boolean containCompiledClass;

    // 该构造器用来输入源代码
    public MyJavaFileObject(File file, ZipPath zipPath, Kind sourceKind, boolean containCompiledClass) {
        // 3、这里加的String:///并不是一个真正的URL的schema, 只是为了区分来源
        super(file.toURI(), sourceKind);
        this.sourceFile = file;
        this.zipPath = zipPath;
        this.containCompiledClass = containCompiledClass;
    }

    @Override
    public boolean containCompiledClass() {
        return this.containCompiledClass;
    }

    public File getSourceFile() {
        return this.sourceFile;
    }

    public void processSource(JarOutputStream jaroutput) throws Exception {
        // 将原文件写入到Jar包中
        // JarEntry entry = new JarEntry(this.kind == Kind.SOURCE ? zipPath.getFullJavaPath() : zipPath.getFullScalaPath());
        JarEntry entry = new JarEntry(zipPath.getFullSourcePath());
        entry.setTime(System.currentTimeMillis());
        try (InputStream sourceInput = FileUtils.openInputStream(sourceFile)) {
            jaroutput.putNextEntry(entry);
            IOUtils.copy(sourceInput, jaroutput);
            jaroutput.closeEntry();
        }
    }

    @Override
    public JavaFileObject getFileObject() {
        return this;
    }

    public ZipPath getZipPath() {
        return this.zipPath;
    }

    @Override
    public Reader openReader(boolean ignoreEncodingErrors) throws IOException {
        return new InputStreamReader(FileUtils.openInputStream(sourceFile), utf8);
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
        return FileUtils.readFileToString(sourceFile, utf8);
    }

    // private static final AtomicInteger count = new AtomicInteger();
    @Override
    public OutputStream openOutputStream() throws IOException {
        outPutStream = new ByteArrayOutputStream();
        return outPutStream;
    }

    public ByteArrayOutputStream getOutputStream() throws IOException {
        if (outPutStream == null) {
            throw new IllegalStateException("outputStream can not be null,source file:" + sourceFile.getAbsolutePath());
        }
        return outPutStream;
    }

    // 获取编译成功的字节码byte[]
    public byte[] getCompiledBytes() {
        if (outPutStream == null) {
            throw new IllegalStateException("outPutStream can not be null");
        }
        return outPutStream.toByteArray();
    }
}
