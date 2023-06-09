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
package org.apache.drill.exec.work.batch;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import org.apache.drill.shaded.guava.com.google.common.base.Stopwatch;

public class FileTest {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(FileTest.class);

  public static void main(String[] args) throws IOException {
    Configuration conf = new Configuration();
    conf.set(FileSystem.FS_DEFAULT_NAME_KEY, "sync:///");
    logger.info(FileSystem.getDefaultUri(conf).toString());
    FileSystem fs = FileSystem.get(conf);
    Path path = new Path("/tmp/testFile");
    FSDataOutputStream out = fs.create(path);
    byte[] s = "hello world".getBytes();
    out.write(s);
    out.hflush();
    FSDataInputStream in = fs.open(path);
    byte[] bytes = new byte[s.length];
    in.read(bytes);
    logger.info(new String(bytes));
    File file = new File("/tmp/testFile");
    FileOutputStream fos = new FileOutputStream(file);
    FileInputStream fis = new FileInputStream(file);
    fos.write(s);
    fos.getFD().sync();
    fis.read(bytes);
    logger.info(new String(bytes));
    out = fs.create(new Path("/tmp/file"));
    for (int i = 0; i < 100; i++) {
      bytes = new byte[256*1024];
      Stopwatch watch = Stopwatch.createStarted();
      out.write(bytes);
      out.hflush();
      long t = watch.elapsed(TimeUnit.MILLISECONDS);
      logger.info(String.format("Elapsed: %d. Rate %d.\n", t, (long) ((long) bytes.length * 1000L / t)));
    }
  }
}
