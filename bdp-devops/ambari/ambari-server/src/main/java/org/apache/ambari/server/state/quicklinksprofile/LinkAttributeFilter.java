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

package org.apache.ambari.server.state.quicklinksprofile;

import java.util.Objects;

import org.apache.ambari.server.state.quicklinks.Link;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

/**
 * A quicklink filter based on link attribute match (the filter's link_attribute is contained by the links set of
 * attributes)
 */
public class LinkAttributeFilter extends Filter {
  static final String LINK_ATTRIBUTE = "link_attribute";

  @JsonProperty(LINK_ATTRIBUTE)
  private String linkAttribute;

  public String getLinkAttribute() {
    return linkAttribute;
  }

  public void setLinkAttribute(String linkAttribute) {
    this.linkAttribute = linkAttribute;
  }

  @Override
  public boolean accept(Link link) {
    return link.getAttributes().contains(linkAttribute);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    LinkAttributeFilter that = (LinkAttributeFilter) o;
    return isVisible() == that.isVisible() && Objects.equals(linkAttribute, that.linkAttribute);
  }

  @Override
  public int hashCode() {
    return Objects.hash(isVisible(), linkAttribute);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("linkAttribute", linkAttribute)
      .toString();
  }
}
