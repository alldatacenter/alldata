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
package org.apache.drill.exec.compile;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.drill.exec.exception.ClassTransformationException;

import org.apache.drill.shaded.guava.com.google.common.cache.CacheBuilder;
import org.apache.drill.shaded.guava.com.google.common.cache.CacheLoader;
import org.apache.drill.shaded.guava.com.google.common.cache.LoadingCache;
import org.apache.drill.shaded.guava.com.google.common.io.Resources;

class ByteCodeLoader {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ByteCodeLoader.class);


  private final LoadingCache<String, byte[]> byteCode = CacheBuilder.newBuilder().maximumSize(10000)
      .expireAfterWrite(10, TimeUnit.MINUTES).build(new ClassBytesCacheLoader());

  private class ClassBytesCacheLoader extends CacheLoader<String, byte[]> {
    @Override
    public byte[] load(String path) throws ClassTransformationException, IOException {
      URL u = this.getClass().getResource(path);
      if (u == null) {
        throw new ClassTransformationException(String.format("Unable to find TemplateClass at path %s", path));
      }
      return Resources.toByteArray(u);
    }
  };

  public byte[] getClassByteCodeFromPath(String path) throws ClassTransformationException, IOException {
    try {
      return byteCode.get(path);
    } catch (ExecutionException e) {
      Throwable c = e.getCause();
      if (c instanceof ClassTransformationException) {
        throw (ClassTransformationException) c;
      }
      if (c instanceof IOException) {
        throw (IOException) c;
      }
      throw new ClassTransformationException(c);
    }
  }

}
