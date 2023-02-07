/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.store.image;

import java.util.TimeZone;

import org.apache.drill.test.BaseTestQuery;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestImageTagValue extends BaseTestQuery {

  private static TimeZone defaultTimeZone;

  @BeforeClass
  public static void setUp() {
    defaultTimeZone = TimeZone.getDefault();
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
  }

  private void createAndQuery(String tableName, String imageFile) throws Exception {
    final String query = String.format(
      "select * from table(cp.`image/%s`(type => 'image', fileSystemMetadata => false))",
      imageFile);

    runSQL("alter session set `store.format`='json'");
    test("create table dfs.`%s` as %s", tableName, query);

    testBuilder()
      .sqlQuery("select * from dfs.`%s`", tableName)
      .ordered()
      .jsonBaselineFile("image/" + tableName + ".json")
      .go();
    runSQL("alter session set `store.format` = 'parquet'");
  }

  @Test
  public void testBmpImage() throws Exception {
    createAndQuery("bmp", "rose-128x174-24bit.bmp");
  }

  @Test
  public void testGifImage() throws Exception {
    createAndQuery("gif", "rose-128x174-8bit-alpha.gif");
  }

  @Test
  public void testIcoImage() throws Exception {
    createAndQuery("ico", "rose-32x32-32bit-alpha.ico");
  }

  @Test
  public void testJpegImage() throws Exception {
    createAndQuery("jpeg", "withExifAndIptc.jpg");
  }

  @Test
  public void testPcxImage() throws Exception {
    createAndQuery("pcx", "rose-128x174-24bit.pcx");
  }

  @Test
  public void testPngImage() throws Exception {
    createAndQuery("png", "rose-128x174-32bit-alpha.png");
  }

  @Test
  public void testPsdImage() throws Exception {
    createAndQuery("psd", "rose-128x174-32bit-alpha.psd");
  }

  @Test
  public void testTiffImage() throws Exception {
    createAndQuery("tiff", "rose-128x174-24bit-lzw.tiff");
  }

  @Test
  public void testWavImage() throws Exception {
    createAndQuery("wav", "sample.wav");
  }

  @Test
  public void testAviImage() throws Exception {
    createAndQuery("avi", "sample.avi");
  }

  @Test
  public void testWebpImage() throws Exception {
    createAndQuery("webp", "1_webp_a.webp");
  }

  @Test
  public void testMovImage() throws Exception {
    createAndQuery("mov", "sample.mov");
  }

  @Test
  public void testMp4Image() throws Exception {
    createAndQuery("mp4", "sample.mp4");
  }

  @Test
  public void testEpsImage() throws Exception {
    createAndQuery("eps", "adobeJpeg1.eps");
  }

  @AfterClass
  public static void cleanUp() {
    TimeZone.setDefault(defaultTimeZone);
  }
}