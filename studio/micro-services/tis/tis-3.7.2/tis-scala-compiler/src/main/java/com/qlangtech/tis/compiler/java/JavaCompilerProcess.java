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
import com.qlangtech.tis.plugin.ds.DBConfig;
import com.qlangtech.tis.plugin.ds.IDbMeta;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.tools.*;
import javax.tools.JavaCompiler.CompilationTask;
import java.io.File;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 将自动生成出来的java类进行编译 https://blog.csdn.net/lmy86263/article/details/59742557
 * <br>
 * <p>
 * https://stackoverflow.com/questions/31289182/compile-scala-code-to-class-file-in-java
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2019年6月6日
 */
public class JavaCompilerProcess {

    private static final Logger logger = LoggerFactory.getLogger(JavaCompilerProcess.class);

    // public static final File rootDir = new File(
    // "D:\\j2ee_solution\\import_pj\\ibator_koubei\\ibator_koubei\\targett\\src\\main\\java");
    private final File sourceRootDir;

    private final File classpathDir;

    private final IDbMeta dbConfig;

    private final File sourceDir;

    public JavaCompilerProcess(IDbMeta dbConfig, File sourceDir, File classpathDir) {
        super();
        if (sourceDir == null || !sourceDir.exists()) {
            throw new IllegalArgumentException("param sourceDir can not be null");
        }
        this.sourceDir = sourceDir;
        this.sourceRootDir = new File(sourceDir, "src/main");
        this.classpathDir = classpathDir;
        if (dbConfig == null) {
            throw new IllegalStateException("param dbConfig can not be null");
        }
        this.dbConfig = dbConfig;
    }

    public static void main(String[] args) throws Exception {
        File rootDir = new File("D:\\j2ee_solution\\import_pj\\ibator_koubei\\ibator_koubei\\targett\\src\\main");
        File classpathDir = new File("D:/j2ee_solution/tis-ibatis/target/dependency/");
        DBConfig dbConfig = new DBConfig();
        dbConfig.setName("shop");
        JavaCompilerProcess compilerProcess = new JavaCompilerProcess(dbConfig, rootDir, classpathDir);
        compilerProcess.compileAndBuildJar();
        // JarFile jarFile = new JarFile(
        // "D:\\j2ee_solution\\mvn_repository\\com\\dfire\\tis\\tis-ibatis\\2.0\\tis-ibatis-2.0.jar");
        // JarEntry next = null;
        // Enumeration<JarEntry> entries = jarFile.entries();
        // InputStream input = null;
        // while (entries.hasMoreElements()) {
        // next = entries.nextElement();
        //
        // System.out.println(next.getName() + ",input is dir:" + next.isDirectory());
        //
        // }
        // jarFile.close();
    }

    /**
     * 编译加打包DAO层代码
     *
     * @throws Exception
     */
    public void compileAndBuildJar() throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<? super JavaFileObject> collector = new DiagnosticCollector<>();
        FileObjectsContext fileObjects = FileObjectsContext.getFileObjects(this.sourceRootDir, JAVA_GETTER);
        // 该JavaFileManager实例是com.sun.tools.javac.file.JavacFileManager
        JavaFileManager manager = new MyJavaFileManager(compiler.getStandardFileManager(collector, null, null), fileObjects.classMap);
        try {
            // TODO: 文件夹要做到可配置化
            // File classpathDir = new
            // File("D:/j2ee_solution/tis-ibatis/target/dependency/");
            File outdir = (new File(this.sourceRootDir, "out"));
            FileUtils.forceMkdir(outdir);
            // ......
            List<String> options = Lists.newArrayList();
            options.add("-classpath");
            this.setClasspath(options);
            options.add("-target");
            options.add("1.8");
            options.add("-d");
            options.add(outdir.getAbsolutePath());
            options.add("-nowarn");
            logger.info("javac options:{}", options.stream().collect(Collectors.joining(" ")));
            List<String> classes = Lists.newArrayList();
            // 在其他实例都已经准备完毕后, 构建编译任务, 其他实例的构建见如下
            //
            CompilationTask compileTask = //
                    compiler.getTask(//
                            new OutputStreamWriter(System.err), //
                            manager, //
                            collector, //
                            options, //
                            classes, //
                            fileObjects.classMap.values().stream().map((r) -> r.getFileObject()).collect(Collectors.toList()));
            compileTask.call();
            collector.getDiagnostics().forEach(item -> System.out.println(item.toString()));
            // final Set<String> zipDirSet = Sets.newHashSet();
            FileObjectsContext.packageJar(new File(this.sourceDir, this.dbConfig.getDAOJarName()), fileObjects);
        } finally {
            try {
                manager.close();
            } catch (Throwable e) {
            }
        }
    }

    private void setClasspath(List<String> options) {
        if (classpathDir != null) {
            if (!classpathDir.exists()) {
                throw new IllegalStateException("path:" + classpathDir.getAbsolutePath() + " is not exist");
            }
            List<File> jars = Lists.newArrayList();
            for (String c : classpathDir.list()) {
                jars.add(new File(classpathDir, c));
            }
            options.add(jars.stream().map((r) -> r.getAbsolutePath()).collect(Collectors.joining(SystemUtils.IS_OS_UNIX ? ":" : ";")));
        } else {
            options.add(System.getProperty("java.class.path"));
        }
    }


    private static final SourceGetterStrategy //
            JAVA_GETTER = new SourceGetterStrategy(true, "java", ".java");

}
