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

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.apache.drill.shaded.guava.com.google.common.collect.Maps;

/**
 * Class loader for "plain-old Java" generated classes.
 * Very basic implementation: allows defining a class from
 * byte codes and finding the loaded classes. Delegates
 * all other class requests to the thread context class
 * loader. This structure ensures that a generated class can
 * find both its own inner classes as well as all the standard
 * Drill implementation classes.
 */

public class CachedClassLoader extends URLClassLoader {

  /**
   * Cache of generated classes. Semantics: a single thread defines
   * the classes, many threads may access the classes.
   */

  private ConcurrentMap<String, Class<?>> cache = Maps.newConcurrentMap();

  public CachedClassLoader() {
    super(new URL[0], Thread.currentThread().getContextClassLoader());
  }

  public void addClass(String fqcn, byte[] byteCodes) {
    Class<?> newClass = defineClass(fqcn, byteCodes, 0, byteCodes.length);
    cache.put(fqcn, newClass);
  }

  @Override
  public Class<?> findClass(String className) throws ClassNotFoundException {
    Class<?> theClass = cache.get(className);
    if (theClass != null) {
      return theClass;
    }
    return super.findClass(className);
  }

  public void addClasses(Map<String, byte[]> results) {
    for (Map.Entry<String, byte[]> result : results.entrySet()) {
      addClass(result.getKey(), result.getValue());
    }
  }
}
