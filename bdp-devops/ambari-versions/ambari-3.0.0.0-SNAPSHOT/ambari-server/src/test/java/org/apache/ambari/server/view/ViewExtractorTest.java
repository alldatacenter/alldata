/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.view;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import javax.xml.bind.JAXBException;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.dao.ViewDAO;
import org.apache.ambari.server.orm.entities.ResourceTypeEntity;
import org.apache.ambari.server.orm.entities.ViewEntity;
import org.apache.ambari.server.orm.entities.ViewEntityTest;
import org.apache.ambari.server.view.configuration.ViewConfig;
import org.easymock.EasyMockSupport;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * ViewExtractor tests.
 */
public class ViewExtractorTest extends EasyMockSupport {

  private final File extractedArchiveDir = createNiceMock(File.class);
  private final File viewArchive = createNiceMock(File.class);
  private final File archiveDir = createNiceMock(File.class);
  private final File entryFile = createNiceMock(File.class);
  private final File classesDir = createNiceMock(File.class);
  private final File libDir = createNiceMock(File.class);
  private final File metaInfDir = createNiceMock(File.class);
  private final File metaInfManifest = createNiceMock(File.class);
  private final JarInputStream viewJarFile = createNiceMock(JarInputStream.class);
  private final JarEntry jarEntry = createNiceMock(JarEntry.class);
  private final FileOutputStream fos = createMock(FileOutputStream.class);
  private final Configuration configuration = createNiceMock(Configuration.class);
  private final File viewDir = createNiceMock(File.class);
  private final File fileEntry = createNiceMock(File.class);
  private final ViewDAO viewDAO = createMock(ViewDAO.class);

  @Before
  public void resetGlobalMocks() {
    resetAll();
  }

  @Test
  public void testExtractViewArchive() throws Exception {

    File addDirPath = createNiceMock(File.class);
    File addDirPathFile1 = createNiceMock(File.class);
    File addDirPathFile2 = createNiceMock(File.class);
    File addDirPath2 = createNiceMock(File.class);
    File addFilePath = createNiceMock(File.class);
    List<File> viewsAdditionalClasspath = Arrays.asList(addDirPath, addDirPath2, addFilePath);

    ResourceTypeEntity resourceTypeEntity = new ResourceTypeEntity();
    resourceTypeEntity.setId(10);
    resourceTypeEntity.setName("MY_VIEW{1.0.0}");

    ViewEntity viewDefinition = ViewEntityTest.getViewEntity();
    viewDefinition.setResourceType(resourceTypeEntity);

    // set expectations
    expect(configuration.getViewExtractionThreadPoolCoreSize()).andReturn(2).anyTimes();
    expect(configuration.getViewExtractionThreadPoolMaxSize()).andReturn(3).anyTimes();
    expect(configuration.getViewExtractionThreadPoolTimeout()).andReturn(10000L).anyTimes();

    if (System.getProperty("os.name").contains("Windows")) {
      expect(viewArchive.getAbsolutePath()).andReturn("\\var\\lib\\ambari-server\\resources\\views\\work\\MY_VIEW{1.0.0}").anyTimes();
      expect(metaInfManifest.getAbsolutePath()).andReturn("\\var\\lib\\ambari-server\\resources\\views\\work\\MY_VIEW{1.0.0}\\META-INF\\MANIFEST.MF").anyTimes();
      expect(archiveDir.getAbsolutePath()).andReturn("\\var\\lib\\ambari-server\\resources\\views\\work\\MY_VIEW{1.0.0}").anyTimes();
    }
    else {
      expect(viewArchive.getAbsolutePath()).andReturn("/var/lib/ambari-server/resources/views/work/MY_VIEW{1.0.0}").anyTimes();
      expect(metaInfManifest.getAbsolutePath()).andReturn("/var/lib/ambari-server/resources/views/work/MY_VIEW{1.0.0}/META-INF/MANIFEST.MF").anyTimes();
      expect(archiveDir.getAbsolutePath()).andReturn("/var/lib/ambari-server/resources/views/work/MY_VIEW{1.0.0}").anyTimes();
    }

    expect(archiveDir.exists()).andReturn(false);
        
    expect(archiveDir.mkdir()).andReturn(true);
    expect(archiveDir.toURI()).andReturn(new URI("file:./"));

    expect(metaInfDir.mkdir()).andReturn(true);

    expect(viewJarFile.getNextJarEntry()).andReturn(jarEntry);
    expect(viewJarFile.getNextJarEntry()).andReturn(null);

    expect(jarEntry.getName()).andReturn("view.xml");

    expect(viewJarFile.read(anyObject(byte[].class))).andReturn(10);
    expect(viewJarFile.read(anyObject(byte[].class))).andReturn(-1);
    fos.write(anyObject(byte[].class), eq(0), eq(10));

    fos.flush();
    fos.close();
    viewJarFile.closeEntry();
    viewJarFile.close();

    expect(classesDir.exists()).andReturn(true);
    expect(classesDir.toURI()).andReturn(new URI("file:./"));

    expect(libDir.exists()).andReturn(true);

    expect(libDir.listFiles()).andReturn(new File[]{fileEntry});
    expect(fileEntry.toURI()).andReturn(new URI("file:./"));

    expect(addDirPath.isDirectory()).andReturn(true);
    expect(addDirPath.exists()).andReturn(true);
    expect(addDirPath.listFiles()).andReturn(new File[]{addDirPathFile1, addDirPathFile2});
    expect(addDirPathFile1.isDirectory()).andReturn(false);
    expect(addDirPathFile1.toURI()).andReturn(new URI("file://file1"));
    expect(addDirPathFile2.isDirectory()).andReturn(false);
    expect(addDirPathFile2.toURI()).andReturn(new URI("file://file2"));

    expect(addDirPath2.isDirectory()).andReturn(true);
    expect(addDirPath2.exists()).andReturn(true);
    expect(addDirPath2.listFiles()).andReturn(new File[]{});

    expect(addFilePath.isDirectory()).andReturn(false);
    expect(addFilePath.isFile()).andReturn(true);
    expect(addFilePath.toURI()).andReturn(new URI("file://file3"));

    replayAll();

    ViewExtractor viewExtractor = getViewExtractor(viewDefinition);
    viewExtractor.extractViewArchive(viewDefinition, viewArchive, archiveDir, viewsAdditionalClasspath);

    verifyAll();
  }

  @Test
  public void testEnsureExtractedArchiveDirectory() throws Exception {

    ResourceTypeEntity resourceTypeEntity = new ResourceTypeEntity();
    resourceTypeEntity.setId(10);
    resourceTypeEntity.setName("MY_VIEW{1.0.0}");

    ViewEntity viewDefinition = ViewEntityTest.getViewEntity();
    viewDefinition.setResourceType(resourceTypeEntity);

    expect(extractedArchiveDir.exists()).andReturn(true);

    replayAll();

    ViewExtractor viewExtractor = getViewExtractor(viewDefinition);

    if (System.getProperty("os.name").contains("Windows")) {
      Assert.assertTrue(viewExtractor.ensureExtractedArchiveDirectory("\\var\\lib\\ambari-server\\resources\\views\\work"));
    }
    else {
      Assert.assertTrue(viewExtractor.ensureExtractedArchiveDirectory("/var/lib/ambari-server/resources/views/work"));
    }

    verifyAll();

    reset(extractedArchiveDir);

    expect(extractedArchiveDir.exists()).andReturn(false);
    expect(extractedArchiveDir.mkdir()).andReturn(true);

    replay(extractedArchiveDir);

    viewExtractor = getViewExtractor(viewDefinition);

    if (System.getProperty("os.name").contains("Windows")) {
      Assert.assertTrue(viewExtractor.ensureExtractedArchiveDirectory("\\var\\lib\\ambari-server\\resources\\views\\work"));
    }
    else {
      Assert.assertTrue(viewExtractor.ensureExtractedArchiveDirectory("/var/lib/ambari-server/resources/views/work"));
    }

    verify(extractedArchiveDir);

    reset(extractedArchiveDir);

    expect(extractedArchiveDir.exists()).andReturn(false);
    expect(extractedArchiveDir.mkdir()).andReturn(false);

    replay(extractedArchiveDir);

    viewExtractor = getViewExtractor(viewDefinition);

    if (System.getProperty("os.name").contains("Windows")) {
      Assert.assertFalse(viewExtractor.ensureExtractedArchiveDirectory("\\var\\lib\\ambari-server\\resources\\views\\work"));
    }
    else {
      Assert.assertFalse(viewExtractor.ensureExtractedArchiveDirectory("/var/lib/ambari-server/resources/views/work"));
    }
    verify(extractedArchiveDir);
  }

  private ViewExtractor getViewExtractor(ViewEntity viewDefinition) throws Exception {

    Map<File, ViewConfig> viewConfigs =
        Collections.singletonMap(viewArchive, viewDefinition.getConfiguration());

    Map<String, File> files = new HashMap<>();

    if (System.getProperty("os.name").contains("Windows")) {
      // sometimes JARs have odd orderings for the MANIFEST.MF, so put it before the META-INF directory
      files.put("\\var\\lib\\ambari-server\\resources\\views\\work\\MY_VIEW{1.0.0}\\META-INF\\MANIFEST.MF", metaInfManifest);
      files.put("\\var\\lib\\ambari-server\\resources\\views\\work\\MY_VIEW{1.0.0}\\META-INF", metaInfDir);
      files.put("\\var\\lib\\ambari-server\\resources\\views\\work", extractedArchiveDir);
      files.put("\\var\\lib\\ambari-server\\resources\\views\\work\\MY_VIEW{1.0.0}", archiveDir);
      files.put("\\var\\lib\\ambari-server\\resources\\views\\work\\MY_VIEW{1.0.0}\\view.xml", entryFile);
      files.put("\\var\\lib\\ambari-server\\resources\\views\\work\\MY_VIEW{1.0.0}\\WEB-INF/classes", classesDir);
      files.put("\\var\\lib\\ambari-server\\resources\\views\\work\\MY_VIEW{1.0.0}\\WEB-INF/lib", libDir);
    }
    else {
      // sometimes JARs have odd orderings for the MANIFEST.MF, so put it before the META-INF directory
      files.put("/var/lib/ambari-server/resources/views/work/MY_VIEW{1.0.0}/META-INF/MANIFEST.MF", metaInfManifest);
      files.put("/var/lib/ambari-server/resources/views/work/MY_VIEW{1.0.0}/META-INF", metaInfDir);
      files.put("/var/lib/ambari-server/resources/views/work", extractedArchiveDir);
      files.put("/var/lib/ambari-server/resources/views/work/MY_VIEW{1.0.0}", archiveDir);
      files.put("/var/lib/ambari-server/resources/views/work/MY_VIEW{1.0.0}/view.xml", entryFile);
      files.put("/var/lib/ambari-server/resources/views/work/MY_VIEW{1.0.0}/WEB-INF/classes", classesDir);
      files.put("/var/lib/ambari-server/resources/views/work/MY_VIEW{1.0.0}/WEB-INF/lib", libDir);
    }

    Map<File, FileOutputStream> outputStreams = new HashMap<>();
    outputStreams.put(entryFile, fos);

    Map<File, JarInputStream> jarFiles = new HashMap<>();
    jarFiles.put(viewArchive, viewJarFile);

    TestViewArchiveUtility archiveUtility = new TestViewArchiveUtility(viewConfigs, files, outputStreams, jarFiles);



    ViewExtractor viewExtractor = new ViewExtractor();
    viewExtractor.archiveUtility = archiveUtility;

    return viewExtractor;
  }

  public static class TestViewArchiveUtility extends ViewArchiveUtility {
    private final Map<File, ViewConfig> viewConfigs;
    private final Map<String, File> files;
    private final Map<File, FileOutputStream> outputStreams;
    private final Map<File, JarInputStream> jarFiles;

    public TestViewArchiveUtility(Map<File, ViewConfig> viewConfigs, Map<String, File> files, Map<File,
        FileOutputStream> outputStreams, Map<File, JarInputStream> jarFiles) {
      this.viewConfigs = viewConfigs;
      this.files = files;
      this.outputStreams = outputStreams;
      this.jarFiles = jarFiles;
    }

    @Override
    public ViewConfig getViewConfigFromArchive(File archiveFile) throws MalformedURLException, JAXBException {
      return viewConfigs.get(archiveFile);
    }

    @Override
    public ViewConfig getViewConfigFromExtractedArchive(String archivePath, boolean validate)
        throws JAXBException, FileNotFoundException {
      for (File viewConfigKey: viewConfigs.keySet()) {
        if (viewConfigKey.getAbsolutePath().equals(archivePath)) {
          return viewConfigs.get(viewConfigKey);
        }
      }
      return null;
    }

    @Override
    public File getFile(String path) {
      return files.get(path);
    }

    @Override
    public FileOutputStream getFileOutputStream(File file) throws FileNotFoundException {
      return outputStreams.get(file);
    }

    @Override
    public JarInputStream getJarFileStream(File file) throws IOException {
      return jarFiles.get(file);
    }
  }
}
