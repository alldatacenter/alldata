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

package org.apache.ambari.server.stack;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.ValidationMessage;

import junit.framework.Assert;

/**
 * KerberosDescriptorTest tests the stack- and service-level descriptors for certain stacks
 * and services
 */
@Category({category.KerberosTest.class})
public class KerberosDescriptorTest {
  private static Logger LOG = LoggerFactory.getLogger(KerberosDescriptorTest.class);

  private static final Pattern PATTERN_KERBEROS_DESCRIPTOR_FILENAME = Pattern.compile("^kerberos(?:_preconfigure)?\\.json$");

  private static File stacksDirectory;
  private static File commonServicesDirectory;

  @BeforeClass
  public static void beforeClass() {
    URL rootDirectoryURL = KerberosDescriptorTest.class.getResource("/");
    Assert.assertNotNull(rootDirectoryURL);

    File resourcesDirectory = new File(new File(rootDirectoryURL.getFile()).getParentFile().getParentFile(), "src/main/resources");
    Assert.assertNotNull(resourcesDirectory);
    Assert.assertTrue(resourcesDirectory.canRead());

    stacksDirectory = new File(resourcesDirectory, "stacks");
    Assert.assertNotNull(stacksDirectory);
    Assert.assertTrue(stacksDirectory.canRead());

    commonServicesDirectory = new File(resourcesDirectory, "common-services");
    Assert.assertNotNull(commonServicesDirectory);
    Assert.assertTrue(commonServicesDirectory.canRead());

  }

  @Test
  public void testCommonServiceDescriptor() throws Exception {
    JsonSchema schema = getJsonSchemaFromPath("kerberos_descriptor_schema.json");
    Assert.assertTrue(visitFile(schema, commonServicesDirectory, true));
  }

  @Test
  public void testStackServiceDescriptor() throws Exception {
    JsonSchema schema = getJsonSchemaFromPath("kerberos_descriptor_schema.json");
    Assert.assertTrue(visitFile(schema, stacksDirectory, true));
  }

  private boolean visitFile(JsonSchema schema, File file, boolean previousResult) throws Exception {

    if (file.isDirectory()) {
      boolean currentResult = true;

      File[] files = file.listFiles();
      if (files != null) {
        for (File currentFile : files) {
          currentResult = visitFile(schema, currentFile, previousResult) && currentResult;
        }
      }
      return previousResult && currentResult;
    } else if (file.isFile()) {
      if (PATTERN_KERBEROS_DESCRIPTOR_FILENAME.matcher(file.getName()).matches()) {
        LOG.info("Validating " + file.getAbsolutePath());

        JsonNode node = getJsonNodeFromUrl(file.toURI().toURL().toExternalForm());
        Set<ValidationMessage> errors = schema.validate(node);

        if ((errors != null) && !errors.isEmpty()) {
          for (ValidationMessage message : errors) {
            LOG.error(message.getMessage());
          }

          return false;
        }

        return true;
      } else {
        return true;
      }
    }

    return previousResult;
  }

  private JsonNode getJsonNodeFromUrl(String url) throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    return mapper.readTree(new URL(url));
  }

  private JsonSchema getJsonSchemaFromPath(String name) throws Exception {
    JsonSchemaFactory factory = new JsonSchemaFactory();
    InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
    return factory.getSchema(is);
  }
}
