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

import java.io.IOException;
import java.net.URL;

import org.apache.ambari.server.view.configuration.ViewConfig;
import org.eclipse.jetty.webapp.WebAppClassLoader;
import org.eclipse.jetty.webapp.WebAppContext;

/**
 * Class loader used to load classes and resources from a search path of URLs referring to both JAR files
 * and directories.  The URLs will be searched in the order specified for classes and resources before
 * searching the parent class loader.
 */
public class ViewClassLoader extends WebAppClassLoader {

  // ----- Constructors ------------------------------------------------------

  /**
   * Constructs a new ViewClassLoader for the given URLs using a default parent class loader.
   * The URLs will be searched in the order specified for classes and resources before searching
   * the parent class loader.
   *
   * @param viewConfig  the view configuration
   * @param urls        the URLs from which to load classes and resources
   */
  public ViewClassLoader(ViewConfig viewConfig, URL[] urls) throws IOException {
    this(viewConfig, null, urls);
  }

  /**
   * Constructs a new ViewClassLoader for the given URLs.
   * The URLs will be searched in the order specified for classes and resources before searching the specified
   * parent class loader.
   *
   * @param viewConfig  the view configuration
   * @param parent      the parent class loader
   * @param urls        the URLs from which to load classes and resources
   */
  public ViewClassLoader(ViewConfig viewConfig, ClassLoader parent, URL[] urls) throws IOException {
    super(parent, getInitContext(viewConfig));

    for (URL url : urls) {
      addURL(url);
    }
  }


  // ----- helper methods ----------------------------------------------------

  // Get a context to initialize the class loader.
  private static WebAppContext getInitContext(ViewConfig viewConfig) {

    WebAppContext webAppContext = new WebAppContext();

    // add ambari classes as system classes
    webAppContext.addSystemClass("org.apache.ambari.server.");
    webAppContext.addSystemClass("org.apache.ambari.view.");

    // add com.google.inject as system classes to allow for injection in view components using the google annotation
    webAppContext.addSystemClass("com.google.inject.");

    // add as system classes to avoid conflicts and linkage errors
    webAppContext.addSystemClass("org.slf4j.");
    webAppContext.addSystemClass("com.sun.jersey.");
    webAppContext.addSystemClass("org.apache.velocity.");

    // set the class loader settings from the configuration
    if (viewConfig != null) {
      String extraClasspath = viewConfig.getExtraClasspath();
      if (extraClasspath != null) {
        webAppContext.setExtraClasspath(extraClasspath);
      }
    }
    return webAppContext;
  }
}
