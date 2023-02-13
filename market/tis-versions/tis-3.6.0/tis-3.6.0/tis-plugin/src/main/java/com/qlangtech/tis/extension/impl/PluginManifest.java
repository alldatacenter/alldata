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

package com.qlangtech.tis.extension.impl;

import com.qlangtech.tis.extension.PluginManager;
import com.qlangtech.tis.extension.PluginStrategy;
import com.qlangtech.tis.extension.PluginWrapper;
import com.qlangtech.tis.manage.common.Config;
import com.qlangtech.tis.maven.plugins.tpi.PluginClassifier;
import com.qlangtech.tis.util.PluginMeta;
import com.qlangtech.tis.util.Util;
import com.qlangtech.tis.util.YesNoMaybe;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Expand;
import org.apache.tools.ant.taskdefs.Zip;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.PatternSet;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ZipFileSet;
import org.apache.tools.ant.types.resources.MappedResourceCollection;
import org.apache.tools.ant.util.GlobPatternMapper;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipExtraField;
import org.apache.tools.zip.ZipOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.io.FilenameUtils.getBaseName;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-06-25 17:02
 **/
public class PluginManifest {
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginManifest.class);

    public final URL baseResourceURL;
    private final Attributes atts;
    private final List<File> libs;

    /**
     * 获得运行时已经解压的PluginMetaData信息
     *
     * @param classInPlugin
     * @return
     */
    public static ExplodePluginManifest create(Class<?> classInPlugin) {
        if (classInPlugin == null) {
            throw new IllegalArgumentException("classInPlugin can not be null");
        }
        String clazz = classInPlugin.getName();

        URL location = classInPlugin.getResource("/" + StringUtils.replace(clazz, ".", "/") + ".class");

        if (location != null) {
            final Pattern p = Pattern.compile("^.*file:(.+?)/" + Config.PLUGIN_LIB_DIR + ".+?!.*$");
            Matcher m = p.matcher(location.toString());
            if (m.find()) {
                //   return URLDecoder.decode(, "UTF-8");
                File pluginDir = new File(m.group(1));
                if (!pluginDir.exists()) {
                    throw new IllegalStateException("plugin Dir is not exist:" + pluginDir.getAbsolutePath());
                }
                File manifest = new File(pluginDir, JarFile.MANIFEST_NAME);
                if (!manifest.exists()) {
                    throw new IllegalStateException(JarFile.MANIFEST_NAME + " is not exist :" + manifest.getAbsolutePath());
                }
                Manifest mfst = new Manifest();
                try (InputStream in = FileUtils.openInputStream(manifest)) {
                    mfst.read(in);
                    return new ExplodePluginManifest(mfst.getMainAttributes(), pluginDir); //createPluginManifest(mfst);
                } catch (Exception e) {
                    throw new RuntimeException(manifest.getAbsolutePath(), e);
                }
            } else {
                throw new IllegalStateException("location is illegal:" + location);
            }
            //   throw new ClassNotFoundException("Cannot parse location of '" + location + "'.  Probably not loaded from a Jar");
        }
        throw new IllegalStateException("Cannot find class '" + classInPlugin.getName() + " using the classloader");
    }

    public static class ExplodePluginManifest extends PluginManifest {
        private final File pluginDir;

        public ExplodePluginManifest(Attributes atts, File pluginDir) {
            super(atts, null, Collections.emptyList());
            this.pluginDir = pluginDir;
        }

        public File getPluginLibDir() {
            return new File(this.pluginDir, Config.PLUGIN_LIB_DIR);
        }
    }

    /**
     * 不需要将tpi包解压
     *
     * @param
     * @return
     * @throws IOException
     */
    public static PluginManifest create(File tpi) {

        if (!tpi.exists()) {
            // throw new IllegalStateException("file:" + f.getPath() + " is not exist");
            return null;//manProcess.apply(null);
        }

        try (JarInputStream tpiFIle = new JarInputStream(FileUtils.openInputStream(tpi), false)) {
            Manifest mfst = tpiFIle.getManifest();
            Objects.requireNonNull(mfst, "tpi relevant manifest can not be null:" + tpi.getAbsolutePath());
            return createPluginManifest(mfst);
        } catch (IOException e) {
            throw new RuntimeException("tpi path:" + tpi.getAbsolutePath(), e);
        }

    }

    private static PluginManifest createPluginManifest(Manifest mfst) {
        return new PluginManifest(mfst.getMainAttributes(), null, Collections.emptyList()) {
            @Override
            public List<File> getLibs() {
                // return super.getLibs();
                throw new UnsupportedOperationException();
            }
        };
    }


    public static PluginManifest create(File pluginWorkDir, File archive) throws IOException {

        final Manifest manifest;
        URL baseResourceURL = null;
        File expandDir = null;
        // if .hpi, this is the directory where war is expanded
        boolean isLinked = isLinked(archive);
        if (isLinked) {
            manifest = loadLinkedManifest(archive);
        } else {
            if (archive.isDirectory()) {
                // already expanded
                expandDir = archive;
            } else {
                // File f = pluginManager.getWorkDir();
                expandDir = new File(pluginWorkDir == null ? archive.getParentFile() : pluginWorkDir, getBaseName(archive.getName()));
                explode(archive, expandDir);
            }
            File manifestFile = new File(expandDir, PluginWrapper.MANIFEST_FILENAME);
            if (!manifestFile.exists()) {
                throw new IOException("Plugin installation failed. No manifest at " + manifestFile);
            }
            FileInputStream fin = new FileInputStream(manifestFile);
            try {
                manifest = new Manifest(fin);
            } finally {
                fin.close();
            }
        }
        final Attributes atts = manifest.getMainAttributes();

        // TODO: define a mechanism to hide classes
        // String export = manifest.getMainAttributes().getValue("Export");
        List<File> paths = new ArrayList<File>();
        if (isLinked) {
            parseClassPath(manifest, archive, paths, "Libraries", ",");
            // backward compatibility
            parseClassPath(manifest, archive, paths, "Class-Path", " +");
            baseResourceURL = resolve(archive, atts.getValue("Resource-Path")).toURI().toURL();
        } else {
            File classes = new File(expandDir, "WEB-INF/classes");
            if (classes.exists())
                paths.add(classes);
            File lib = new File(expandDir, "WEB-INF/lib");
            File[] libs = lib.listFiles(JAR_FILTER);
            if (libs != null) {
                paths.addAll(Arrays.asList(libs));
            }
            baseResourceURL = expandDir.toPath().toUri().toURL();
        }

        return new PluginManifest(atts, baseResourceURL, paths);
    }


    public PluginManifest(final Attributes atts, URL baseResourceURL, List<File> libs) {
        this.baseResourceURL = baseResourceURL;
        this.atts = Objects.requireNonNull(atts, "param atts can not be null");
        this.libs = libs;
    }

    public YesNoMaybe supportsDynamicLoad() {
        String v = this.atts.getValue("Support-Dynamic-Loading");
        if (v == null) { return YesNoMaybe.MAYBE;}
        return Boolean.parseBoolean(v) ? YesNoMaybe.YES : YesNoMaybe.NO;
    }

    public String getVersionOf() {
        String v = this.atts.getValue(PluginStrategy.KEY_MANIFEST_PLUGIN_VERSION);
        if (v != null)
            return v;
        // plugins generated before maven-hpi-plugin 1.3 should still have this attribute
        v = this.atts.getValue("Implementation-Version");
        if (v != null)
            return v;
        return "???";
    }

    public String getPluginClass() {
        return this.atts.getValue("Plugin-Class");
    }

    public long getLastModfiyTime() {
        return Long.parseLong(this.atts.getValue(PluginStrategy.KEY_LAST_MODIFY_TIME));
    }

    public String getGroupId() {
        return this.atts.getValue("Group-Id");
    }

    public String getURL() {
        return atts.getValue("Url");
    }

    public String getLongName() {
        String name = this.atts.getValue("Long-Name");
        return name;
    }

    public PluginMeta getPluginMeta() {
        // String name, String ver, Optional<PluginClassifier> classifier, Long lastModifyTimeStamp
        return new PluginMeta(this.computeShortName(StringUtils.EMPTY), this.getVersionOf(), this.parseClassifier(), this.getLastModfiyTime());
    }


//    public Attributes getAttrs() {
//        return this.atts;
//    }

    public boolean getUsePluginFirstClassLoader() {
        if (atts == null) {
            return false;
        }
        String usePluginFirstClassLoader = atts.getValue(PluginStrategy.KEY_MANIFEST_PLUGIN_FIRST_CLASSLOADER);
        // return usePluginFirstClassLoader;
        return Boolean.valueOf(usePluginFirstClassLoader);
    }


    public List<File> getLibs() {
        return this.libs;
    }

    public String getMaskedClasses() {
        String masked = atts.getValue("Mask-Classes");
        return masked;
    }


    public Optional<PluginClassifier> parseClassifier() {
        String attrClazzier = this.atts.getValue(PluginManager.PACAKGE_CLASSIFIER);
//        if (PluginClassifier.MATCH_ALL_CLASSIFIER.getClassifier().equals(attrClazzier)) {
//            return Optional.of(PluginClassifier.MATCH_ALL_CLASSIFIER);
//        }
        return Optional.ofNullable(StringUtils.isEmpty(attrClazzier) ? null : PluginClassifier.create(attrClazzier));
    }

    public String computeShortName(String fileName) {
//        Objects.requireNonNull(manifest, "manifest can not be null");
//        // use the name captured in the manifest, as often plugins
//        // depend on the specific short name in its URLs.
//        Attributes attrs = manifest.getMainAttributes();
//        if (attrs == null) {
//            throw new IllegalStateException(
//                    "fileName:" + fileName + " relevant MainAttributes can not be null");
//        }
        return parseShortName(this.atts, fileName);
    }

    public static String parseShortName(Attributes atts, String fileName) {
        String n = atts.getValue(PluginStrategy.KEY_MANIFEST_SHORTNAME);
        if (n != null) {
            return n;
        }
        // maven seems to put this automatically, so good fallback to check.
        n = atts.getValue("Extension-Name");
        if (n != null) {
            return n;
        }
        // this entry.
        return getBaseName(fileName);
    }

    public ClassicPluginStrategy.DependencyMeta getDependencyMeta() {
        ClassicPluginStrategy.DependencyMeta dependencyMeta = new ClassicPluginStrategy.DependencyMeta();
        String v = atts.getValue(PluginStrategy.KEY_MANIFEST_DEPENDENCIES);
        if (v != null) {
            for (String s : v.split(",")) {
                PluginWrapper.Dependency d = PluginWrapper.Dependency.parse(s);
                if (d.optional) {
                    dependencyMeta.optionalDependencies.add(d);
                } else {
                    dependencyMeta.dependencies.add(d);
                }
            }
        }
        return dependencyMeta;
    }

    public String getMasked() {
        String masked = atts.getValue("Global-Mask-Classes");
        return masked;
    }

    /**
     * Filter for jar files.
     */
    private static final FilenameFilter JAR_FILTER = new FilenameFilter() {

        public boolean accept(File dir, String name) {
            return name.endsWith(".jar");
        }
    };

    public static boolean isLinked(File archive) {
        return archive.getName().endsWith(".hpl") || archive.getName().endsWith(".jpl");
    }

    private static File resolve(File base, String relative) {
        File rel = new File(relative);
        if (rel.isAbsolute())
            return rel;
        else
            return new File(base.getParentFile(), relative);
    }

    public static Manifest loadLinkedManifest(File archive) throws IOException {
        // resolve the .hpl file to the location of the manifest file
        try {
            // Locate the manifest
            String firstLine;
            FileInputStream manifestHeaderInput = new FileInputStream(archive);
            try {
                firstLine = IOUtils.readFirstLine(manifestHeaderInput, "UTF-8");
            } finally {
                manifestHeaderInput.close();
            }
            if (firstLine.startsWith("Manifest-Version:")) {
                // this is the manifest already
            } else {
                // indirection
                archive = resolve(archive, firstLine);
            }
            // Read the manifest
            FileInputStream manifestInput = new FileInputStream(archive);
            try {
                return new Manifest(manifestInput);
            } finally {
                manifestInput.close();
            }
        } catch (IOException e) {
            throw new IOException("Failed to load " + archive, e);
        }
    }


    private static void parseClassPath(Manifest manifest, File archive
            , List<File> paths, String attributeName, String separator) throws IOException {
        String classPath = manifest.getMainAttributes().getValue(attributeName);
        // attribute not found
        if (classPath == null)
            return;
        for (String s : classPath.split(separator)) {
            File file = resolve(archive, s);
            if (file.getName().contains("*")) {
                // handle wildcard
                FileSet fs = new FileSet();
                File dir = file.getParentFile();
                fs.setDir(dir);
                fs.setIncludes(file.getName());
                for (String included : fs.getDirectoryScanner(new Project()).getIncludedFiles()) {
                    paths.add(new File(dir, included));
                }
            } else {
                if (!file.exists())
                    throw new IOException("No such file: " + file);
                paths.add(file);
            }
        }
    }

    /**
     * Explodes the plugin into a directory, if necessary.
     */
    private static void explode(File archive, File destDir) throws IOException {
        destDir.mkdirs();
        // timestamp check
        File explodeTime = new File(destDir, PluginStrategy.FILE_NAME_timestamp2);
        if (explodeTime.exists() && explodeTime.lastModified() == archive.lastModified()) {
            // no need to expand
            return;
        }
        LOGGER.info("start to explode archive:{}", archive.getAbsolutePath());
        // delete the contents so that old files won't interfere with new files
        Util.deleteRecursive(destDir);
        try {
            Project prj = new Project();
            unzipExceptClasses(archive, destDir, prj);
            createClassJarFromWebInfClasses(archive, destDir, prj);
        } catch (BuildException x) {
            throw new IOException("Failed to expand " + archive, x);
        }
        try {
            // new FilePath(explodeTime).touch(archive.lastModified());
            FileUtils.touch(explodeTime);
            explodeTime.setLastModified(archive.lastModified());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Repackage classes directory into a jar file to make it remoting friendly.
     * The remoting layer can cache jar files but not class files.
     */
    private static void createClassJarFromWebInfClasses(File archive, File destDir, Project prj) throws IOException {
        File classesJar = new File(destDir, "WEB-INF/lib/classes.jar");
        ZipFileSet zfs = new ZipFileSet();
        zfs.setProject(prj);
        zfs.setSrc(archive);
        zfs.setIncludes("WEB-INF/classes/");
        MappedResourceCollection mapper = new MappedResourceCollection();
        mapper.add(zfs);
        GlobPatternMapper gm = new GlobPatternMapper();
        gm.setFrom("WEB-INF/classes/*");
        gm.setTo("*");
        mapper.add(gm);
        final long dirTime = archive.lastModified();
        // this ZipOutputStream is reused and not created for each directory
        final ZipOutputStream wrappedZOut = new ZipOutputStream(new NullOutputStream()) {

            @Override
            public void putNextEntry(ZipEntry ze) throws IOException {
                // roundup
                ze.setTime(dirTime + 1999);
                super.putNextEntry(ze);
            }
        };
        try {
            Zip z = new Zip() {

                /**
                 * Forces the fixed timestamp for directories to make sure
                 * classes.jar always get a consistent checksum.
                 */
                protected void zipDir(Resource dir, ZipOutputStream zOut, String vPath, int mode, ZipExtraField[] extra) throws IOException {
                    // use wrappedZOut instead of zOut
                    super.zipDir(dir, wrappedZOut, vPath, mode, extra);
                }
            };
            z.setProject(prj);
            z.setTaskType("zip");
            classesJar.getParentFile().mkdirs();
            z.setDestFile(classesJar);
            z.add(mapper);
            z.execute();
        } finally {
            wrappedZOut.close();
        }
    }

    private static void unzipExceptClasses(File archive, File destDir, Project prj) {
        Expand e = new Expand();
        e.setProject(prj);
        e.setTaskType("unzip");
        e.setSrc(archive);
        e.setDest(destDir);
        PatternSet p = new PatternSet();
        p.setExcludes("WEB-INF/classes/");
        e.addPatternset(p);
        e.execute();
    }
}
