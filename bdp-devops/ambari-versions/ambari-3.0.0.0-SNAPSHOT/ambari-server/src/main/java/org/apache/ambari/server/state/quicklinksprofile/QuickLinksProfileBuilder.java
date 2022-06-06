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

import static org.apache.ambari.server.state.quicklinksprofile.Filter.VISIBLE;
import static org.apache.ambari.server.state.quicklinksprofile.LinkAttributeFilter.LINK_ATTRIBUTE;
import static org.apache.ambari.server.state.quicklinksprofile.LinkNameFilter.LINK_NAME;
import static org.apache.ambari.server.state.quicklinksprofile.LinkNameFilter.LINK_URL;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

/**
 * Class to create a {@link QuickLinksProfile} based on data received in a request
 */
public class QuickLinksProfileBuilder {

  public static final String NAME = "name";
  public static final String COMPONENTS = "components";
  public static final String FILTERS = "filters";
  public static final Set<String> ALLOWED_FILTER_ATTRIBUTES =
      ImmutableSet.of(VISIBLE, LINK_NAME, LINK_URL, LINK_ATTRIBUTE);

  /**
   *
   * @param globalFiltersRaw The data in the request belonging to the "quicklinks_profile/filters" key.
   * @param serviceFiltersRaw The data in the request belonging to "quicklinks_profile/services"
   * @return The quicklinks profile as Json encoded string.
   * @throws QuickLinksProfileEvaluationException when the received data defines an invalid profile. This can be of various
   *    reasons: the received data is has invalid structure, critical data is missing or there are contradicting filter
   *    rules in the profile
   */
  public String buildQuickLinksProfile(@Nullable Object globalFiltersRaw, @Nullable Object serviceFiltersRaw) throws QuickLinksProfileEvaluationException {
    try {
      List<Filter> globalFilters = buildQuickLinkFilters(globalFiltersRaw);
      List<Service> services = buildServices(serviceFiltersRaw);
      QuickLinksProfile profile = QuickLinksProfile.create(globalFilters, services);
      // sanity check: this should throw QuickLinksProfileEvaluationException if the profile is invalid
      new DefaultQuickLinkVisibilityController(profile);
      return new QuickLinksProfileParser().encode(profile);
    }
    catch (QuickLinksProfileEvaluationException ex) {
      throw ex;
    }
    catch (Exception ex) {
      throw new QuickLinksProfileEvaluationException("Error interpreting quicklinks profile data", ex);
    }
  }

  List<Service> buildServices(@Nullable Object servicesRaw) {
    if (null == servicesRaw) {
      return ImmutableList.of();
    }
    List<Service> services = new ArrayList<>();
    for (Map<String, Object> serviceAsMap: (Collection<Map<String, Object>>)servicesRaw) {
      String serviceName = (String)serviceAsMap.get(NAME);
      Object componentsRaw = serviceAsMap.get(COMPONENTS);
      Object filtersRaw = serviceAsMap.get(FILTERS);
      services.add(Service.create(serviceName,
          buildQuickLinkFilters(filtersRaw),
          buildComponents(componentsRaw)));
    }
    return services;
  }

  List<Component> buildComponents(@Nullable Object componentsRaw) {
    if (null == componentsRaw) {
      return ImmutableList.of();
    }
    List<Component> components = new ArrayList<>();
    for (Map<String, Object> componentAsMap: (Collection<Map<String, Object>>)componentsRaw) {
      String componentName = (String)componentAsMap.get(NAME);
      Object filtersRaw = componentAsMap.get(FILTERS);
      components.add(Component.create(componentName,
          buildQuickLinkFilters(filtersRaw)));
    }
    return components;  }


  List<Filter> buildQuickLinkFilters(@Nullable Object filtersRaw) throws ClassCastException, IllegalArgumentException {
    if (null == filtersRaw) {
      return ImmutableList.of();
    }
    List<Filter> filters  = new ArrayList<>();
    for (Map<String, String> filterAsMap: (Collection<Map<String, String>>)filtersRaw) {
      Set<String> invalidAttributes = Sets.difference(filterAsMap.keySet(), ALLOWED_FILTER_ATTRIBUTES);

      Preconditions.checkArgument(invalidAttributes.isEmpty(),
          "%s%s",
          QuickLinksFilterDeserializer.PARSE_ERROR_MESSAGE_INVALID_JSON_TAG,
          invalidAttributes);

      String linkName = filterAsMap.get(LINK_NAME);
      String linkUrl = filterAsMap.get(LINK_URL);
      String attributeName = filterAsMap.get(LINK_ATTRIBUTE);
      boolean visible = Boolean.parseBoolean(filterAsMap.get(VISIBLE));

      Preconditions.checkArgument(null == linkName || null == attributeName,
         "%s link_name: %s, link_attribute: %s",
          QuickLinksFilterDeserializer.PARSE_ERROR_MESSAGE_AMBIGUOUS_FILTER,
          linkName,
          attributeName);

      Preconditions.checkArgument(null == linkUrl || null != linkName,
        "Invalid filter. Link url can only be applied to link name filters. link_url: %s",
        linkUrl);

      if (null != linkName) {
        filters.add(Filter.linkNameFilter(linkName, linkUrl, visible));
      }
      else if (null != attributeName) {
        filters.add(Filter.linkAttributeFilter(attributeName, visible));
      }
      else {
        filters.add(Filter.acceptAllFilter(visible));
      }
    }
    return filters;
  }


}
