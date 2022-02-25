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

package org.apache.ambari.server.controller.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.junit.Test;

/**
 * Base provider tests.
 */
public class BaseProviderTest {
  @Test
  public void testGetProperties() {
    Set<String> propertyIds = new HashSet<>();
    propertyIds.add("foo");
    propertyIds.add("bar");
    propertyIds.add("cat1/prop1");
    propertyIds.add("cat2/prop2");
    propertyIds.add("cat3/subcat3/prop3");

    BaseProvider provider = new TestProvider(propertyIds);

    Set<String> supportedPropertyIds = provider.getPropertyIds();
    assertTrue(supportedPropertyIds.containsAll(propertyIds));
  }

  @Test
  public void testCheckPropertyIds() {
    Set<String> propertyIds = new HashSet<>();
    propertyIds.add("foo");
    propertyIds.add("bar");
    propertyIds.add("cat1/prop1");
    propertyIds.add("cat2/prop2");
    propertyIds.add("cat3/subcat3/prop3");
    propertyIds.add("cat4/subcat4/map");

    BaseProvider provider = new TestProvider(propertyIds);

    assertTrue(provider.checkPropertyIds(propertyIds).isEmpty());

    assertTrue(provider.checkPropertyIds(Collections.singleton("cat1")).isEmpty());
    assertTrue(provider.checkPropertyIds(Collections.singleton("cat2")).isEmpty());
    assertTrue(provider.checkPropertyIds(Collections.singleton("cat3")).isEmpty());
    assertTrue(provider.checkPropertyIds(Collections.singleton("cat3/subcat3")).isEmpty());
    assertTrue(provider.checkPropertyIds(
        Collections.singleton("cat4/subcat4/map")).isEmpty());

    // note that key is not in the set of known property ids.  We allow it if its parent is a known property.
    // this allows for Map type properties where we want to treat the entries as individual properties
    assertTrue(provider.checkPropertyIds(
        Collections.singleton("cat4/subcat4/map/key")).isEmpty());

    propertyIds.add("badprop");
    propertyIds.add("badcat");

    Set<String> unsupportedPropertyIds = provider.checkPropertyIds(propertyIds);
    assertFalse(unsupportedPropertyIds.isEmpty());
    assertEquals(2, unsupportedPropertyIds.size());
    assertTrue(unsupportedPropertyIds.contains("badprop"));
    assertTrue(unsupportedPropertyIds.contains("badcat"));
  }

  @Test
  public void testGetRequestPropertyIds() {
    Set<String> providerPropertyIds = new HashSet<>();
    providerPropertyIds.add("foo");
    providerPropertyIds.add("bar");
    providerPropertyIds.add("cat1/sub1");

    BaseProvider provider = new TestProvider(providerPropertyIds);

    Request request = PropertyHelper.getReadRequest("foo");

    Set<String> requestedPropertyIds = provider.getRequestPropertyIds(request, null);

    assertEquals(1, requestedPropertyIds.size());
    assertTrue(requestedPropertyIds.contains("foo"));

    request = PropertyHelper.getReadRequest("foo", "bar");

    requestedPropertyIds = provider.getRequestPropertyIds(request, null);

    assertEquals(2, requestedPropertyIds.size());
    assertTrue(requestedPropertyIds.contains("foo"));
    assertTrue(requestedPropertyIds.contains("bar"));

    request = PropertyHelper.getReadRequest("foo", "baz", "bar", "cat", "cat1/prop1");

    requestedPropertyIds = provider.getRequestPropertyIds(request, null);

    assertEquals(2, requestedPropertyIds.size());
    assertTrue(requestedPropertyIds.contains("foo"));
    assertTrue(requestedPropertyIds.contains("bar"));

    // ask for a property that isn't specified as supported, but its category is... the property
    // should end up in the returned set for the case where the category is a Map property
    request = PropertyHelper.getReadRequest("foo", "cat1/sub1/prop1");

    requestedPropertyIds = provider.getRequestPropertyIds(request, null);

    assertEquals(2, requestedPropertyIds.size());
    assertTrue(requestedPropertyIds.contains("foo"));
    assertTrue(requestedPropertyIds.contains("cat1/sub1/prop1"));
  }

  @Test
  public void testSetResourceProperty() {
    Set<String> propertyIds = new HashSet<>();
    propertyIds.add("p1");
    propertyIds.add("foo");
    propertyIds.add("cat1/foo");
    propertyIds.add("cat2/bar");
    propertyIds.add("cat2/baz");
    propertyIds.add("cat3/sub1/bam");
    propertyIds.add("cat4/sub2/sub3/bat");
    propertyIds.add("cat5/sub5");

    Resource resource = new ResourceImpl(Resource.Type.Service);

    assertNull(resource.getPropertyValue("foo"));

    BaseProvider.setResourceProperty(resource, "foo", "value1", propertyIds);
    assertEquals("value1", resource.getPropertyValue("foo"));

    BaseProvider.setResourceProperty(resource, "cat2/bar", "value2", propertyIds);
    assertEquals("value2", resource.getPropertyValue("cat2/bar"));

    assertNull(resource.getPropertyValue("unsupported"));
    BaseProvider.setResourceProperty(resource, "unsupported", "valueX", propertyIds);
    assertNull(resource.getPropertyValue("unsupported"));

    // we should allow anything under the category cat5/sub5
    BaseProvider.setResourceProperty(resource, "cat5/sub5/prop5", "value5", propertyIds);
    assertEquals("value5", resource.getPropertyValue("cat5/sub5/prop5"));
    BaseProvider.setResourceProperty(resource, "cat5/sub5/sub5a/prop5a", "value5", propertyIds);
    assertEquals("value5", resource.getPropertyValue("cat5/sub5/sub5a/prop5a"));
    // we shouldn't allow anything under the category cat5/sub7
    BaseProvider.setResourceProperty(resource, "cat5/sub7/unsupported", "valueX", propertyIds);
    assertNull(resource.getPropertyValue("cat5/sub7/unsupported"));
  }

  @Test
  public void testIsPropertyRequested() {
    Set<String> propertyIds = new HashSet<>();
    propertyIds.add("p1");
    propertyIds.add("foo");
    propertyIds.add("cat1/foo");
    propertyIds.add("cat2/bar");
    propertyIds.add("cat2/baz");
    propertyIds.add("cat3/sub1/bam");
    propertyIds.add("cat4/sub2/sub3/bat");
    propertyIds.add("cat5/sub5");

    assertTrue(BaseProvider.isPropertyRequested("foo", propertyIds));

    assertTrue(BaseProvider.isPropertyRequested("cat2", propertyIds));

    assertTrue(BaseProvider.isPropertyRequested("cat2/bar", propertyIds));

    assertFalse(BaseProvider.isPropertyRequested("unsupported", propertyIds));

    // we should allow anything under the category cat5/sub5
    assertTrue(BaseProvider.isPropertyRequested("cat5/sub5/prop5", propertyIds));
    assertTrue(BaseProvider.isPropertyRequested("cat5/sub5/sub5a/prop5a", propertyIds));

    // we shouldn't allow anything under the category cat5/sub7
    assertFalse(BaseProvider.isPropertyRequested("cat5/sub7/unsupported", propertyIds));
  }

  @Test
  public void testSetResourcePropertyWithMaps() {
    Set<String> propertyIds = new HashSet<>();
    propertyIds.add("cat1/emptyMapProperty");
    propertyIds.add("cat1/mapProperty");
    propertyIds.add("cat2/mapMapProperty");
    propertyIds.add("cat3/mapProperty3/key2");
    propertyIds.add("cat4/mapMapProperty4/subMap1/key3");
    propertyIds.add("cat4/mapMapProperty4/subMap2");

    Resource resource = new ResourceImpl(Resource.Type.Service);

    // Adding an empty Map as a property should add the actual Map as a property
    Map<String, String> emptyMapProperty = new HashMap<>();
    BaseProvider.setResourceProperty(resource, "cat1/emptyMapProperty", emptyMapProperty, propertyIds);
    assertTrue(resource.getPropertiesMap().containsKey("cat1/emptyMapProperty"));

    Map<String, String> mapProperty = new HashMap<>();
    mapProperty.put("key1", "value1");
    mapProperty.put("key2", "value2");
    mapProperty.put("key3", "value3");

    // Adding a property of type Map should add all of its keys as sub properties
    // if the map property was requested
    BaseProvider.setResourceProperty(resource, "cat1/mapProperty", mapProperty, propertyIds);
    assertNull(resource.getPropertyValue("cat1/mapProperty"));
    assertEquals("value1", resource.getPropertyValue("cat1/mapProperty/key1"));
    assertEquals("value2", resource.getPropertyValue("cat1/mapProperty/key2"));
    assertEquals("value3", resource.getPropertyValue("cat1/mapProperty/key3"));

    Map<String, Map<String, String>> mapMapProperty = new HashMap<>();
    Map<String, String> mapSubProperty1 = new HashMap<>();
    mapSubProperty1.put("key1", "value11");
    mapSubProperty1.put("key2", "value12");
    mapSubProperty1.put("key3", "value13");
    mapMapProperty.put("subMap1", mapSubProperty1);
    Map<String, String> mapSubProperty2 = new HashMap<>();
    mapSubProperty2.put("key1", "value21");
    mapSubProperty2.put("key2", "value22");
    mapSubProperty2.put("key3", "value23");
    mapMapProperty.put("subMap2", mapSubProperty2);
    Map<String, String> mapSubProperty3 = new HashMap<>();
    mapMapProperty.put("subMap3", mapSubProperty3);

    // Map of maps ... adding a property of type Map should add all of its keys as sub properties
    // if the map property was requested
    BaseProvider.setResourceProperty(resource, "cat2/mapMapProperty", mapMapProperty, propertyIds);
    assertNull(resource.getPropertyValue("cat2/mapMapProperty"));
    assertNull(resource.getPropertyValue("cat2/mapMapProperty/subMap1"));
    assertNull(resource.getPropertyValue("cat2/mapMapProperty/subMap2"));
    assertTrue(resource.getPropertiesMap().containsKey(
        "cat2/mapMapProperty/subMap3"));
    assertEquals("value11",
        resource.getPropertyValue("cat2/mapMapProperty/subMap1/key1"));
    assertEquals("value12",
        resource.getPropertyValue("cat2/mapMapProperty/subMap1/key2"));
    assertEquals("value13",
        resource.getPropertyValue("cat2/mapMapProperty/subMap1/key3"));
    assertEquals("value21",
        resource.getPropertyValue("cat2/mapMapProperty/subMap2/key1"));
    assertEquals("value22",
        resource.getPropertyValue("cat2/mapMapProperty/subMap2/key2"));
    assertEquals("value23",
        resource.getPropertyValue("cat2/mapMapProperty/subMap2/key3"));

    Map<String, String> mapProperty3 = new HashMap<>();
    mapProperty3.put("key1", "value1");
    mapProperty3.put("key2", "value2");
    mapProperty3.put("key3", "value3");

    // Adding a property of type Map shouldn't add the map if it wasn't requested and
    // should only add requested keys as sub properties ...
    // only "cat3/mapProperty3/key2" was requested
    BaseProvider.setResourceProperty(resource, "cat3/mapProperty3", mapProperty3, propertyIds);
    assertNull(resource.getPropertyValue("cat3/mapProperty3"));
    assertNull(resource.getPropertyValue("cat3/mapProperty3/key1"));
    assertEquals("value2", resource.getPropertyValue("cat3/mapProperty3/key2"));
    assertNull(resource.getPropertyValue("cat3/mapProperty3/key3"));

    Map<String, Map<String, String>> mapMapProperty4 = new HashMap<>();
    mapMapProperty4.put("subMap1", mapSubProperty1);
    mapMapProperty4.put("subMap2", mapSubProperty2);
    // Map of maps ... adding a property of type Map shouldn't add the map if it wasn't requested and
    // should only add requested keys as sub properties ...
    // only "cat4/mapMapProperty4/subMap1/key3" and "cat4/mapMapProperty4/subMap2" are requested
    BaseProvider.setResourceProperty(resource, "cat4/mapMapProperty4", mapMapProperty4, propertyIds);
    assertNull(resource.getPropertyValue("cat4/mapMapProperty4"));
    assertNull(resource.getPropertyValue("cat4/mapMapProperty4/subMap1"));
    assertNull(resource.getPropertyValue("cat4/mapMapProperty4/subMap2"));
    assertNull(resource.getPropertyValue("cat4/mapMapProperty4/subMap1/key1"));
    assertNull(resource.getPropertyValue("cat4/mapMapProperty4/subMap1/key2"));
    assertEquals("value13",
        resource.getPropertyValue("cat4/mapMapProperty4/subMap1/key3"));
    assertEquals("value21",
        resource.getPropertyValue("cat4/mapMapProperty4/subMap2/key1"));
    assertEquals("value22",
        resource.getPropertyValue("cat4/mapMapProperty4/subMap2/key2"));
    assertEquals("value23",
        resource.getPropertyValue("cat4/mapMapProperty4/subMap2/key3"));
  }

  @Test
  public void testRegexpMethods() {
    Set<String> propertyIds = new HashSet<>();
    String regexp = "cat/$1.replaceAll(\\\"([.])\\\",\\\"/\\\")/key";
    String propertyId = "cat/sub/key";
    String regexp2 = "cat/$1.replaceAll(\\\"([.])\\\",\\\"/\\\")/something/$2/key";
    String propertyId2 = "cat/sub/something/sub2/key";

    String incorrectPropertyId = "some/property/id";
    propertyIds.add(regexp);
    propertyIds.add(regexp2);

    BaseProvider provider = new TestProvider(propertyIds);
    Map.Entry<String, Pattern> regexEntry = provider.getRegexEntry(propertyId);

    assertEquals(regexp, regexEntry.getKey());
    assertNull(provider.getRegexEntry(incorrectPropertyId));
    assertEquals("sub", provider.getRegexGroups(regexp, propertyId).get(0));
    assertEquals("sub2", provider.getRegexGroups(regexp2, propertyId2).get(1));
    assertTrue(provider.getRegexGroups(regexp, incorrectPropertyId).isEmpty());
  }

  @Test
  public void testComplexMetricParsing() {
    Set<String> propertyIds = new HashSet<>();
    propertyIds.add("metrics/flume/$1.substring(0)/CHANNEL/$2.replaceAll(\"[^-]+\",\"\")EventPutSuccessCount/rate/sum");
    propertyIds.add("metrics/yarn/Queue/$1.replaceAll(\"([.])\",\"/\")/AppsCompleted");
    propertyIds.add("metrics/yarn/Queue/$1.replaceAll(\",q(\\d+)=\",\"/\").substring(1)/AppsFailed");

    TestProvider provider = new TestProvider(propertyIds);
    Entry<String, Pattern> entry = provider.getRegexEntry("metrics/flume/flume");
    assertEquals("metrics/flume/$1", entry.getKey());
    assertEquals("metrics/flume/(\\S*)", entry.getValue().pattern());

    entry = provider.getRegexEntry("metrics/flume/flume/CHANNEL");
    assertEquals("metrics/flume/$1/CHANNEL", entry.getKey());
    assertEquals("metrics/flume/(\\S*)/CHANNEL", entry.getValue().pattern());

    entry = provider.getRegexEntry("metrics/flume/flume/CHANNEL/EventPutSuccessCount");
    assertEquals("metrics/flume/$1/CHANNEL/$2EventPutSuccessCount", entry.getKey());
    assertEquals("metrics/flume/(\\S*)/CHANNEL/(\\S*)EventPutSuccessCount",
        entry.getValue().pattern());

    entry = provider.getRegexEntry("metrics/flume/flume/CHANNEL/EventPutSuccessCount/rate");
    assertEquals("metrics/flume/$1/CHANNEL/$2EventPutSuccessCount/rate",
        entry.getKey());
    assertEquals(
        "metrics/flume/(\\S*)/CHANNEL/(\\S*)EventPutSuccessCount/rate",
        entry.getValue().pattern());

    entry = provider.getRegexEntry("metrics/yarn/Queue/root/AppsCompleted");
    assertEquals("metrics/yarn/Queue/$1.replaceAll(\"([.])\",\"/\")/AppsCompleted",
        entry.getKey());
    assertEquals(
        "metrics/yarn/Queue/(\\S*)/AppsCompleted",
        entry.getValue().pattern());

    entry = provider.getRegexEntry("metrics/yarn/Queue/root/default/AppsCompleted");
    assertEquals("metrics/yarn/Queue/$1.replaceAll(\"([.])\",\"/\")/AppsCompleted",
        entry.getKey());
    assertEquals(
        "metrics/yarn/Queue/(\\S*)/AppsCompleted",
        entry.getValue().pattern());

    entry = provider.getRegexEntry("metrics/yarn/Queue/root/default/AppsFailed");
    assertEquals("metrics/yarn/Queue/$1.replaceAll(\",q(\\d+)=\",\"/\").substring(1)/AppsFailed",
        entry.getKey());
    assertEquals(
        "metrics/yarn/Queue/(\\S*)/AppsFailed",
        entry.getValue().pattern());
  }

  static class TestProvider extends BaseProvider {
    public TestProvider(Set<String> propertyIds) {
      super(propertyIds);
    }
  }
}
