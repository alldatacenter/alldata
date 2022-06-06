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
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.apache.ambari.server.state.quicklinks.Link;
import org.apache.ambari.server.utils.StreamUtils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;


/**
 * A filter that accepts quicklinks based on name match.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LinkNameFilter extends Filter {

  static final String LINK_NAME = "link_name";
  static final String LINK_URL = "link_url";

  @JsonProperty(LINK_NAME)
  private String linkName;

  /**
   * In addition to filtering this filter allows overriding the link url too.
   */
  @JsonProperty(LINK_URL)
  private String linkUrl;

  @JsonProperty(LINK_NAME)
  public String getLinkName() {
    return linkName;
  }

  @JsonProperty(LINK_NAME)
  public void setLinkName(String linkName) {
    this.linkName = linkName;
  }

  @JsonProperty(LINK_URL)
  public @Nullable String getLinkUrl() {
    return linkUrl;
  }

  @JsonProperty(LINK_URL)
  public void setLinkUrl(@Nullable String linkUrl) {
    this.linkUrl = linkUrl;
  }

  @Override
  public boolean accept(Link link) {
    return Objects.equals(link.getName(), linkName);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    LinkNameFilter that = (LinkNameFilter) o;
    return Objects.equals(linkName, that.linkName) &&
      Objects.equals(linkUrl, that.linkUrl) &&
      isVisible() == that.isVisible();
  }

  @Override
  public int hashCode() {
    return Objects.hash(linkName, linkUrl);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("linkName", linkName)
      .add("linkUrl", linkUrl)
      .add("visible", isVisible())
      .toString();
  }

  static Stream<LinkNameFilter> getLinkNameFilters(Stream<Filter> input) {
    return StreamUtils.instancesOf(input, LinkNameFilter.class);
  }
}
