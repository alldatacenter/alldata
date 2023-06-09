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
package org.apache.drill.jdbc;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

public class DrillbitClassLoader extends URLClassLoader {

  DrillbitClassLoader() {
    super(URLS);
  }

  private static final URL[] URLS;

  static {
    List<URL> urlList = new ArrayList<>();
    final String classPath = System.getProperty("app.class.path");
    final String[] st = classPath.split(File.pathSeparator);
    final int l = st.length;
    for (int i = 0; i < l; i++) {
      try {
        if (st[i].length() == 0) {
          st[i] = ".";
        }
        urlList.add(new File(st[i]).toURI().toURL());
      } catch (MalformedURLException e) {
        assert false : e.toString();
      }
    }

    URLS = urlList.toArray(new URL[0]);
  }

  @Override
  protected Class<?> findClass(String name) throws ClassNotFoundException {
    return super.findClass(name);
  }

  @Override
  public Class<?> loadClass(String name) throws ClassNotFoundException {
    return super.loadClass(name);
  }

  @Override
  protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    return super.loadClass(name, resolve);
  }

}
