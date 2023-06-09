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
package org.apache.drill.exec.udf.dynamic;

import org.apache.maven.cli.MavenCli;
import org.apache.maven.cli.logging.Slf4jLogger;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.logging.BaseLoggerManager;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class JarBuilder {

  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(JarBuilder.class);
  private static final String MAVEN_MULTI_MODULE_PROJECT_DIRECTORY = "maven.multiModuleProjectDirectory";

  private final MavenCli cli;
  private final String projectDirectory;

  public JarBuilder(String projectDirectory) {
    this.cli = new MavenCli() {
      @Override
      protected void customizeContainer(PlexusContainer container) {
        ((DefaultPlexusContainer) container).setLoggerManager(new BaseLoggerManager() {
          @Override
          protected org.codehaus.plexus.logging.Logger createLogger(String s) {
            return new Slf4jLogger(logger);
          }
        });
      }
    };
    this.projectDirectory = projectDirectory;
  }

  /**
   * Builds jars using embedded maven in provided build directory.
   * Includes files / resources based given pattern, otherwise using defaults provided in pom.xml.
   * Checks if build exit code is 0, i.e. build was successful.
   *
   * @param jarName jar name
   * @param buildDirectory build directory
   * @param includeFiles pattern indicating which files should be included
   * @param includeResources pattern indicating which resources should be included
   *
   * @return binary jar name with jar extension (my-jar.jar)
   */
  public String build(String jarName, String buildDirectory, String includeFiles, String includeResources) {
    String originalPropertyValue = System.setProperty(MAVEN_MULTI_MODULE_PROJECT_DIRECTORY, projectDirectory);
    try {
      List<String> params = new LinkedList<>();
      params.add("clean");
      params.add("package");
      params.add("-DskipTests");
      // uncomment to build with current Drill version
      // params.add("-Ddrill.version=" + DrillVersionInfo.getVersion());
      params.add("-Djar.finalName=" + jarName);
      params.add("-Dcustom.buildDirectory=" + buildDirectory);
      if (includeFiles != null) {
        params.add("-Dinclude.files=" + includeFiles);
      }
      if (includeResources != null) {
        params.add("-Dinclude.resources=" + includeResources);
      }
      int result = cli.doMain(params.toArray(new String[params.size()]), projectDirectory, System.out, System.err);
      assertEquals("Build should be successful.", 0, result);
      return jarName + ".jar";
    } finally {
      if (originalPropertyValue != null) {
        System.setProperty(MAVEN_MULTI_MODULE_PROJECT_DIRECTORY, originalPropertyValue);
      } else {
        System.clearProperty(MAVEN_MULTI_MODULE_PROJECT_DIRECTORY);
      }
    }
  }

}

