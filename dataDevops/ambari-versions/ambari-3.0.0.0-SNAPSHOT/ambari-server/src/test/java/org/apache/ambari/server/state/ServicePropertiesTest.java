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
package org.apache.ambari.server.state;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import javax.xml.validation.Validator;

import org.apache.ambari.server.stack.StackManager;
import org.junit.Test;
import org.xml.sax.SAXException;

public class ServicePropertiesTest {

  /**
   * This unit test checks that all config xmls for all services pass validation.
   * They should match xsd schema configuration-schema.xsd.
   * Test checks real (production) configs like hdfs-site.xml. The reason why
   * this test exists is to make sure that anybody who adds new properties to stack
   * configs, explicitly defines whether they should be added/modified/deleted
   * during ambari upgrade and/or stack upgrade.
   *
   * @throws SAXException
   * @throws IOException
   */
  @Test
  public void validatePropertySchemaOfServiceXMLs() throws SAXException,
    IOException, URISyntaxException {
    Validator validator = StackManager.getPropertySchemaValidator();
    // TODO: make sure that test does not depend on mvn/junit working directory
    StackManager.validateAllPropertyXmlsInFolderRecursively(
      getDirectoryFromMainResources("common-services"), validator);
    StackManager.validateAllPropertyXmlsInFolderRecursively(
      getDirectoryFromMainResources("stacks"), validator);
  }


  /**
   * Looks for directory under ambari-server/src/main/resources/ path.
   * @param dir directory name
   * @return File that points to a requested directory
   */
  private File getDirectoryFromMainResources(String dir) throws URISyntaxException, IOException {
    File resourcesFolder = new File(resolveAbsolutePathToResourcesFolder(), "../../../src/main/resources");
    File resultDir = new File(resourcesFolder, dir);
    if (resultDir.exists()) {
      return resultDir;
    } else {
      String msg = String.format("Directory %s does not exist", resultDir.getAbsolutePath());
      throw new IOException(msg);
    }
  }

  /**
   * Resolves File of ambari-server/src/test/resources/ folder.
   * Determines path based on TestAmbaryServer.samples folder in test resources
   * Should not depend on how and from which location the ambari-server
   * was started.
   * @return File of ambari-server/src/test/resources/ folder.
   */
  private File resolveAbsolutePathToResourcesFolder() throws URISyntaxException {
    URL dirURL = this.getClass().getClassLoader().getResource("TestAmbaryServer.samples");
    if (dirURL != null && dirURL.getProtocol().equals("file")) {
      return new File(dirURL.toURI());
    } else {
      throw new UnsupportedOperationException(
        String.format("Dir uri %s does not seem to point to file. Maybe a jar?",
          dirURL.toURI()));
    }

  }
}
