/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.api.resources;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.controller.internal.ResourceImpl;
import org.apache.ambari.server.controller.spi.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Resource Definition for Mpack Resource types.
 */
public class MpackResourceDefinition extends BaseResourceDefinition {

  private final static Logger LOG =
          LoggerFactory.getLogger(MpackResourceDefinition.class);

  public MpackResourceDefinition() {
    super(Resource.Type.Mpack);
  }

  @Override
  public String getPluralName() {
    return "mpacks";
  }

  @Override
  public String getSingularName() {
    return "mpack";
  }

  @Override
  public Set<SubResourceDefinition> getSubResourceDefinitions() {
    Set<SubResourceDefinition> setChildren = new HashSet<>();
    setChildren.add(new SubResourceDefinition(Resource.Type.StackVersion, null, false));
    return setChildren;
  }

  @Override
  public List<PostProcessor> getPostProcessors() {
    List<PostProcessor> listProcessors = new ArrayList<>();
    listProcessors.add(new MpackHrefProcessor());
    listProcessors.add(new MpackPostProcessor());
    return listProcessors;
  }

  /**
   * Post Processing the mpack href when the call comes from stack endpoint to ensure that the
   * href is a backreference to the mpacks end point
   */
  private class MpackHrefProcessor extends BaseHrefPostProcessor {
    @Override
    public void process(Request request, TreeNode<Resource> resultNode, String href) {
      if (href.contains("/stacks/")) {
        ResourceImpl mpack = (ResourceImpl) resultNode.getObject();
        Map<String, Map<String, Object>> mapInfo = mpack.getPropertiesMap();
        Map<String, Object> mpackInfo = mapInfo.get("MpackInfo");

        int idx = href.indexOf("stacks/");
        Long mpackId = (Long)mpackInfo.get("id");
        href = href.substring(0, idx) + "mpacks/" + mpackId;
        resultNode.setProperty("href", href);
      } else {
        super.process(request, resultNode, href);
      }
    }
  }

  /***
   * Post processing to change the name of the result node to mpack
   */
  private class MpackPostProcessor implements PostProcessor {
    @Override
    public void process(Request request, TreeNode<Resource> resultNode, String href) {
      if (href.contains("/stacks/")) {
        resultNode.setName("mpack");

      }
    }
  }


}
