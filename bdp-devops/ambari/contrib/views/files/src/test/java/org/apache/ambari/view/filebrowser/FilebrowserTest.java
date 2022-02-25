/**
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

package org.apache.ambari.view.filebrowser;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.ViewResourceHandler;
import org.apache.ambari.view.commons.hdfs.FileOperationService;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataBodyPart;



public class FilebrowserTest{
  private ViewResourceHandler handler;
  private ViewContext context;
  private HttpHeaders httpHeaders;
  private UriInfo uriInfo;

  private Map<String, String> properties;
  private FileBrowserService fileBrowserService;

  private MiniDFSCluster hdfsCluster;
  public static final String BASE_URI = "http://localhost:8084/myapp/";


  @Before
  public void setUp() throws Exception {
    handler = createNiceMock(ViewResourceHandler.class);
    context = createNiceMock(ViewContext.class);
    httpHeaders = createNiceMock(HttpHeaders.class);
    uriInfo = createNiceMock(UriInfo.class);

    properties = new HashMap<String, String>();
    File baseDir = new File("./target/hdfs/" + "FilebrowserTest")
        .getAbsoluteFile();
    FileUtil.fullyDelete(baseDir);
    Configuration conf = new Configuration();
    conf.set(MiniDFSCluster.HDFS_MINIDFS_BASEDIR, baseDir.getAbsolutePath());
    conf.set("hadoop.proxyuser." + System.getProperty("user.name") + ".groups", "*");
    conf.set("hadoop.proxyuser." + System.getProperty("user.name") + ".hosts", "*");

    MiniDFSCluster.Builder builder = new MiniDFSCluster.Builder(conf);
    hdfsCluster = builder.build();
    String hdfsURI = hdfsCluster.getURI() + "/";
    properties.put("webhdfs.url", hdfsURI);
    expect(context.getProperties()).andReturn(properties).anyTimes();
    expect(context.getUsername()).andReturn(System.getProperty("user.name")).anyTimes();
    replay(handler, context, httpHeaders, uriInfo);
    fileBrowserService = getService(FileBrowserService.class, handler, context);

    FileOperationService.MkdirRequest request = new FileOperationService.MkdirRequest();
    request.path = "/tmp";
    fileBrowserService.fileOps().mkdir(request);
  }

  @After
  public void tearDown() {
    hdfsCluster.shutdown();
  }

  @Test
  public void testListDir() throws Exception {
    FileOperationService.MkdirRequest request = new FileOperationService.MkdirRequest();
    request.path = "/tmp1";
    fileBrowserService.fileOps().mkdir(request);
    Response response = fileBrowserService.fileOps().listdir("/", null);
    JSONObject responseObject = (JSONObject) response.getEntity();
    JSONArray statuses = (JSONArray) responseObject.get("files");
    System.out.println(response.getEntity());
    Assert.assertEquals(200, response.getStatus());
    Assert.assertTrue(statuses.size() > 0);
    System.out.println(statuses);
  }

  private Response uploadFile(String path, String fileName,
                              String fileExtension, String fileContent) throws Exception {
    File tempFile = File.createTempFile(fileName, fileExtension);
    BufferedWriter bw = new BufferedWriter(new FileWriter(tempFile));
    bw.write(fileContent);
    bw.close();
    InputStream content = new FileInputStream(tempFile);
    FormDataBodyPart inputStreamBody = new FormDataBodyPart(
        FormDataContentDisposition.name("file")
            .fileName(fileName + fileExtension).build(), content,
        MediaType.APPLICATION_OCTET_STREAM_TYPE);

    Response response = fileBrowserService.upload().uploadFile(content,
        inputStreamBody.getFormDataContentDisposition(), "/tmp/");
    return response;
  }

  @Test
  public void testUploadFile() throws Exception {
    Response response = uploadFile("/tmp/", "testUpload", ".tmp", "Hello world");
    Assert.assertEquals(200, response.getStatus());
    Response listdir = fileBrowserService.fileOps().listdir("/tmp", null);
    JSONObject responseObject = (JSONObject) listdir.getEntity();
    JSONArray statuses = (JSONArray) responseObject.get("files");
    System.out.println(statuses.size());
    Response response2 = fileBrowserService.download().browse("/tmp/testUpload.tmp", false, false, httpHeaders, uriInfo);
    Assert.assertEquals(200, response2.getStatus());
  }

  private void createDirectoryWithFiles(String dirPath) throws Exception {
    FileOperationService.MkdirRequest request = new FileOperationService.MkdirRequest();
    request.path = dirPath;
    File file = new File(dirPath);
    String fileName = file.getName();
    fileBrowserService.fileOps().mkdir(request);
    for (int i = 0; i < 10; i++) {
      uploadFile(dirPath, fileName + i, ".txt", "Hello world" + i);
    }
  }

  @Test
  public void testStreamingGzip() throws Exception {
    String gzipDir = "/tmp/testGzip";
    createDirectoryWithFiles(gzipDir);
    DownloadService.DownloadRequest dr = new DownloadService.DownloadRequest();
    dr.entries = new String[] { gzipDir };

    Response result = fileBrowserService.download().downloadGZip(dr);
  }

  @Test
  public void testStreamingDownloadGzipName() throws Exception {
    String gzipDir = "/tmp/testGzip1";
    createDirectoryWithFiles(gzipDir);

    // test download 1 folder
    validateDownloadZipName(new String[]{gzipDir}, "testGzip1.zip" );

    // test download 1 folder
    validateDownloadZipName(new String[]{gzipDir + "/testGzip11.txt"}, "testGzip11.txt.zip" );

    String gzipDir2 = "/tmp/testGzip2";
    createDirectoryWithFiles(gzipDir2);

    // test download 2 folders
    validateDownloadZipName(new String[] { gzipDir, gzipDir2 }, "hdfs.zip" );

    // test download 2 files of same folder
    validateDownloadZipName(new String[] { gzipDir + "/testGzip11", gzipDir + "/testGzip12" }, "hdfs.zip" );

    // test download 2 files of different folder -- although I think UI does not allow it
    validateDownloadZipName(new String[] { gzipDir + "/testGzip11", gzipDir2 + "/testGzip21" }, "hdfs.zip" );
  }

  private void validateDownloadZipName(String[] entries, String downloadedFileName) {
    DownloadService.DownloadRequest dr = new DownloadService.DownloadRequest();
    dr.entries = entries;

    Response result = fileBrowserService.download().downloadGZip(dr);
    List<Object> contentDisposition = result.getMetadata().get("Content-Disposition");
    Assert.assertEquals("inline; filename=\"" + downloadedFileName +"\"",contentDisposition.get(0));
  }

  @Test
  public void testUsername() throws Exception {
    Assert.assertEquals(System.getProperty("user.name"), fileBrowserService.upload().getDoAsUsername(context));
    properties.put("webhdfs.username", "test-user");
    Assert.assertEquals("test-user", fileBrowserService.upload().getDoAsUsername(context));
  }

  private static <T> T getService(Class<T> clazz,
                                  final ViewResourceHandler viewResourceHandler,
                                  final ViewContext viewInstanceContext) {
    Injector viewInstanceInjector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(ViewResourceHandler.class).toInstance(viewResourceHandler);
        bind(ViewContext.class).toInstance(viewInstanceContext);
      }
    });
    return viewInstanceInjector.getInstance(clazz);
  }


}
