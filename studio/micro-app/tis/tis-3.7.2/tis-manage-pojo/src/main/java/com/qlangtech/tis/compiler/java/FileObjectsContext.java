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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import javax.tools.JavaFileObject;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.CRC32;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-10-20 16:59
 **/
public class FileObjectsContext {

    public Map<String /** className **/, IOutputEntry> classMap = Maps.newHashMap();

    Set<String> dirSet = Sets.newHashSet();

    public List<ResourcesFile> resources = Lists.newArrayList();

    public static void traversingFiles(Stack<String> childPath, File parent, FileObjectsContext result, IProcessFile fileProcess) {
        try {
            if (parent == null || !parent.exists()) {
                throw new IllegalStateException("parent is not exist:" + parent.getAbsolutePath());
            }
            File child = null;
            for (String c : parent.list()) {
                child = new File(parent, c);
                if (child.isDirectory()) {
                    childPath.push(c);
                    try {
                        result.dirSet.add(childPath.stream().collect(Collectors.joining("/")));
                        traversingFiles(childPath, child, result, fileProcess);
                    } finally {
                        childPath.pop();
                    }
                } else {
                    String zipPath = childPath.stream().collect(Collectors.joining("/"));
                    fileProcess.process(zipPath, child);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static FileObjectsContext getFileObjects(File sourceRootDir, SourceGetterStrategy sourceGetterStrategy) {
        final FileObjectsContext result = new FileObjectsContext();
        final Stack<String> childPath = new Stack<>();
        traversingFiles(childPath, new File(sourceRootDir, sourceGetterStrategy.childSourceDir), result, (zp, child) -> {
            String className;
            ZipPath zipPath;
            if (StringUtils.endsWith(child.getName(), sourceGetterStrategy.sourceCodeExtendsion)) {
                boolean isJavaSourceCode = sourceGetterStrategy.sourceCodeExtendsion.equals(JavaFileObject.Kind.SOURCE.extension);
                className = StringUtils.substringBefore(child.getName(), ".");
                // zipPath = new ZipPath(childPath.stream().collect(Collectors.joining("/")), className, //
                // isJavaSourceCode ? JavaFileObject.Kind.SOURCE : JavaFileObject.Kind.OTHER);// + ".class";
                zipPath = new //
                        ZipPath(//
                        zp, // + ".class";
                        className, isJavaSourceCode ? JavaFileObject.Kind.SOURCE : JavaFileObject.Kind.OTHER);
                result.classMap.put(childPath.stream().collect(Collectors.joining(".")) + "." + className, sourceGetterStrategy.processMyJavaFileObject(new MyJavaFileObject(child, zipPath, sourceGetterStrategy.getSourceKind(), isJavaSourceCode)));
            }
        });
        File resourceDir = new File(sourceRootDir, "resources");
        if (sourceGetterStrategy.getResource && resourceDir.exists()) {
            traversingFiles(childPath, resourceDir, result, (zp, child) -> {
                if (StringUtils.endsWith(child.getName(), ".xml")) {
                    // result.resources.add(new ResourcesFile(
                    // new ZipPath(childPath.stream().collect(Collectors.joining("/"))
                    // , child.getName(), JavaFileObject.Kind.OTHER), child));
                    result.resources.add(new ResourcesFile(new ZipPath(zp, child.getName(), JavaFileObject.Kind.OTHER), FileUtils.readFileToByteArray(child)));
                }
            });
        }
        return result;
    }

    public static void packageJar(File targetJarFile, FileObjectsContext... fileObjectsArry) throws Exception {
        packageJar(targetJarFile, new Manifest(), fileObjectsArry);
    }

    /**
     * // @param        Jar包保存的位置
     *
     * @param targetJarFile   Jar包的名称
     * @param fileObjectsArry 需要打包的资源文件
     * @throws Exception
     */
    public static void packageJar(File targetJarFile, Manifest man, FileObjectsContext... fileObjectsArry) throws Exception {
        try {
            final Set<String> savedEntryPaths = Sets.newHashSet();
            // 开始打包
            try (JarOutputStream jaroutput = new JarOutputStream(FileUtils.openOutputStream(targetJarFile), man)) {
                for (FileObjectsContext fileObjects : fileObjectsArry) {
                    // 添加文件夹entry
                    fileObjects.dirSet.stream().forEach((p) -> {
                        try {
                            JarEntry entry = new JarEntry(p + "/");
                            entry.setTime(System.currentTimeMillis());
                            if (savedEntryPaths.add(entry.getName())) {
                                jaroutput.putNextEntry(entry);
                                jaroutput.closeEntry();
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                }
                for (FileObjectsContext fileObjects : fileObjectsArry) {
                    // 添加class
                    for (IOutputEntry f : fileObjects.classMap.values()) {
                        // class 文件
                        if (f.containCompiledClass()) {
                            writeJarEntry(jaroutput, f);
                        }
                        // 添加.java文件
                        f.processSource(jaroutput);
                    }
                }
                for (FileObjectsContext fileObjects : fileObjectsArry) {
                    // 添加xml配置文件
                    for (ResourcesFile res : fileObjects.resources) {
                        JarEntry entry = new JarEntry(res.getZipPath().getFullPath());
                        if (!savedEntryPaths.add(entry.getName())) {
                            continue;
                        }
                        entry.setTime(System.currentTimeMillis());
                        byte[] data = res.getContent();
                        entry.setSize(data.length);
                        CRC32 crc = new CRC32();
                        crc.update(data);
                        entry.setCrc(crc.getValue());
                        jaroutput.putNextEntry(entry);
                        jaroutput.write(data);
                        jaroutput.closeEntry();
                    }
                }
                jaroutput.flush();
            }
        } catch (Exception e) {
            throw new RuntimeException("jarFileName:" + targetJarFile.getAbsolutePath(), e);
        }
    }

    private static void writeJarEntry(JarOutputStream jarOutput, IOutputEntry fileObj) throws IOException, FileNotFoundException {
        ZipPath zipPath = fileObj.getZipPath();
        JarEntry entry = new JarEntry(zipPath.getFullClassPath());
        entry.setTime(System.currentTimeMillis());
        byte[] data = fileObj.getOutputStream().toByteArray();
        entry.setSize(data.length);
        CRC32 crc = new CRC32();
        crc.update(data);
        entry.setCrc(crc.getValue());
        jarOutput.putNextEntry(entry);
        jarOutput.write(data);
        jarOutput.closeEntry();
    }

    public interface IProcessFile {

        public void process(String zipPath, File child) throws Exception;
    }


}
