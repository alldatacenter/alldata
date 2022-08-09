/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.apache.ambari.server.api.resources;

import static junit.framework.Assert.assertEquals;
import static org.apache.ambari.server.controller.internal.CompatibleRepositoryVersionResourceProvider.REPOSITORY_VERSION_ID_PROPERTY_ID;
import static org.apache.ambari.server.controller.internal.CompatibleRepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID;

import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.api.util.TreeNodeImpl;
import org.apache.ambari.server.controller.internal.ResourceImpl;
import org.apache.ambari.server.controller.spi.Resource;
import org.junit.Test;

public class CompatibleRepositoryVersionDefinitionTest {
  private static final String ORIGINAL_HREF = "http://host/api/v1/stacks/HDP/versions/2.6/compatible_repository_versions";
  private CompatibleRepositoryVersionDefinition def = new CompatibleRepositoryVersionDefinition();

  @Test
  public void testHrefReplace() throws Exception {
    TreeNode<Resource> repoVersionNode = nodeWithIdAndVersion("42", "3.0");
    postProcessHref(repoVersionNode);
    assertEquals(
      "http://host/api/v1/stacks/HDP/versions/3.0/compatible_repository_versions/42",
      repoVersionNode.getStringProperty("href"));
  }

  private TreeNode<Resource> nodeWithIdAndVersion(String id, String version) {
    ResourceImpl resource = new ResourceImpl(Resource.Type.CompatibleRepositoryVersion);
    resource.setProperty(REPOSITORY_VERSION_ID_PROPERTY_ID, id);
    resource.setProperty(REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID, version);
    return new TreeNodeImpl<>(null, resource, "any");
  }

  private void postProcessHref(TreeNode<Resource> repoVersionNode) {
    ResourceDefinition.PostProcessor hrefProcessor = hrefProcessor();
    hrefProcessor.process(null, repoVersionNode, ORIGINAL_HREF);
  }

  private ResourceDefinition.PostProcessor hrefProcessor() {
    return def.getPostProcessors().get(1);
  }
}