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
package org.apache.drill.test;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;

public class HadoopUtils {
  public static final String LOCAL_FS_SCHEME = "file://";

  public static org.apache.hadoop.fs.Path javaToHadoopPath(java.nio.file.Path javaPath) {
    return new org.apache.hadoop.fs.Path(javaPath.toUri());
  }

  public static java.nio.file.Path hadoopToJavaPath(org.apache.hadoop.fs.Path hadoopPath) {
    final String pathString = hadoopPath.toUri().getPath();
    final URI uri;

    try {
      uri = new URI(LOCAL_FS_SCHEME + pathString);
    } catch (URISyntaxException e) {
      // This should never happen
      throw new RuntimeException(e);
    }

    return Paths.get(uri);
  }
}
