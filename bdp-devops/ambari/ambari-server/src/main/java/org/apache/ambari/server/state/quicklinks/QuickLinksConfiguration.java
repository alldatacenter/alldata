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

package org.apache.ambari.server.state.quicklinks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonSerialize(include= JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuickLinksConfiguration{
  @JsonProperty("protocol")
  private Protocol protocol;

  @JsonProperty("links")
  private List<Link> links;

  public Protocol getProtocol() {
    return protocol;
  }

  public void setProtocol(Protocol protocol) {
    this.protocol = protocol;
  }

  public List<Link> getLinks() {
    return links;
  }

  public void setLinks(List<Link> links) {
    this.links = links;
  }

  public void mergeWithParent(QuickLinksConfiguration parent) {
    if (parent == null) {
      return;
    }

    //protocol uses override merge, if the child has it, then use it
    if(protocol == null)
      protocol = parent.getProtocol();

    if (links == null) {
      links = parent.getLinks();
    } else if (parent.getLinks() != null) {
      links = mergeLinks(parent.getLinks(), links);
    }
  }

  private List<Link> mergeLinks(List<Link> parentLinks, List<Link> childLinks) {
    Map<String, Link> mergedLinks = new HashMap<>();

    for (Link parentLink : parentLinks) {
      mergedLinks.put(parentLink.getName(), parentLink);
    }

    for (Link childLink : childLinks) {
      if (childLink.getName() != null) {
        if(childLink.isRemoved()){
          mergedLinks.remove(childLink.getName());
        } else {
          Link parentLink = mergedLinks.get(childLink.getName());
          childLink.mergeWithParent(parentLink);
          mergedLinks.put(childLink.getName(), childLink);
        }
      }
    }
    return new ArrayList<>(mergedLinks.values());
  }
}