package com.netease.arctic.utils;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class CatalogUtilTest {
  /**
   * when log-store flag is on , fill up with default related props and other user-defined prop should be keep
   */
  @Test
  public void testMergeCatalogPropertiesToTable() {
    Map<String, String> expected = new HashMap<>();
    expected.put("log-store.enabled", "true");
    expected.put("log-store.address", "168.0.0.1:9092");
    expected.put("log-store.type", "kafka");
    expected.put("other.prop", "10");
    expected.put("log-store.consistency.guarantee.enable", "true");

    Map<String, String> userDefined = new HashMap<>();
    userDefined.put("log-store.enabled", "true");
    userDefined.put("other.prop", "10");

    Map<String, String> catalogProperties = new HashMap<>();
    catalogProperties.put("table.log-store.enabled", "false");
    catalogProperties.put("table.log-store.address", "168.0.0.1:9092");
    catalogProperties.put("table.log-store.type", "kafka");
    catalogProperties.put("table.log-store.consistency.guarantee.enable", "true");
    catalogProperties.put("ams.address", "127.0.0.1");

    Map<String, String> result = CatalogUtil.mergeCatalogPropertiesToTable(userDefined, catalogProperties);
    Assert.assertEquals(expected, result);
  }

  /**
   * when log-store flag is off, remove all related props
   */
  @Test
  public void testMergeCatalogPropertiesToTable1() {
    Map<String, String> expected = new HashMap<>();
    expected.put("log-store.enabled", "false");

    Map<String, String> userDefined = new HashMap<>();
    userDefined.put("log-store.enabled", "false");

    Map<String, String> catalogProperties = new HashMap<>();
    catalogProperties.put("table.log-store.enabled", "false");
    catalogProperties.put("table.log-store.address", "168.0.0.1:9092");
    catalogProperties.put("table.log-store.type", "kafka");
    catalogProperties.put("table.log-store.consistency.guarantee.enable", "true");
    catalogProperties.put("ams.address", "127.0.0.1");

    Map<String, String> result = CatalogUtil.mergeCatalogPropertiesToTable(userDefined, catalogProperties);
    Assert.assertEquals(expected, result);
  }

  /**
   * user-defined prop should not be overwritten by default props
   */
  @Test
  public void testMergeCatalogPropertiesToTable2() {
    Map<String, String> expected = new HashMap<>();
    expected.put("log-store.enabled", "true");
    expected.put("log-store.address", "168.0.0.1:9092");
    expected.put("log-store.type", "pulsar");
    expected.put("log-store.consistency.guarantee.enable", "true");

    Map<String, String> userDefined = new HashMap<>();
    userDefined.put("log-store.enabled", "true");
    userDefined.put("log-store.type", "pulsar");

    Map<String, String> catalogProperties = new HashMap<>();
    catalogProperties.put("table.log-store.enabled", "false");
    catalogProperties.put("table.log-store.address", "168.0.0.1:9092");
    catalogProperties.put("table.log-store.type", "kafka");
    catalogProperties.put("table.log-store.consistency.guarantee.enable", "true");
    catalogProperties.put("ams.address", "127.0.0.1");

    Map<String, String> result = CatalogUtil.mergeCatalogPropertiesToTable(userDefined, catalogProperties);
    Assert.assertEquals(expected, result);
  }

  /**
   * Other user-defined prop should not lose
   */
  @Test
  public void testMergeCatalogPropertiesToTable3() {
    Map<String, String> expected = new HashMap<>();
    expected.put("log-store.enabled", "true");
    expected.put("log-store.address", "168.0.0.1:9092");
    expected.put("log-store.type", "kafka");
    expected.put("log-store.consistency.guarantee.enable", "true");
    expected.put("table.other-props", "foo");

    Map<String, String> userDefined = new HashMap<>();
    userDefined.put("log-store.enabled", "true");
    userDefined.put("log-store.type", "kafka");
    userDefined.put("table.other-props", "foo");

    Map<String, String> catalogProperties = new HashMap<>();
    catalogProperties.put("table.log-store.enabled", "false");
    catalogProperties.put("table.log-store.address", "168.0.0.1:9092");
    catalogProperties.put("table.log-store.type", "kafka");
    catalogProperties.put("table.log-store.consistency.guarantee.enable", "true");
    catalogProperties.put("ams.address", "127.0.0.1");

    Map<String, String> result = CatalogUtil.mergeCatalogPropertiesToTable(userDefined, catalogProperties);
    Assert.assertEquals(expected, result);
  }

  /**
   * user-defined and default catalog 'self-optimizing.enabled' are both switched on,
   * keep all related props
   */
  @Test
  public void testMergeCatalogPropertiesToTable4() {
    Map<String, String> expected = new HashMap<>();
    expected.put("log-store.enabled", "true");
    expected.put("log-store.address", "168.0.0.1:9092");
    expected.put("log-store.type", "kafka");
    expected.put("log-store.consistency.guarantee.enable", "true");
    expected.put("self-optimizing.enabled", "true");
    expected.put("self-optimizing.quota", "0.2"); // should not overwritten by default
    expected.put("self-optimizing.group", "mygroup"); // inherit from default prop
    expected.put("table.other-props", "foo");

    Map<String, String> userDefined = new HashMap<>();
    userDefined.put("log-store.enabled", "true");
    userDefined.put("self-optimizing.enabled", "true");
    userDefined.put("self-optimizing.quota", "0.2");
    userDefined.put("table.other-props", "foo");

    Map<String, String> catalogProperties = new HashMap<>();
    catalogProperties.put("table.log-store.enabled", "false");
    catalogProperties.put("table.log-store.address", "168.0.0.1:9092");
    catalogProperties.put("table.log-store.type", "kafka");
    catalogProperties.put("table.log-store.consistency.guarantee.enable", "true");
    catalogProperties.put("table.self-optimizing.enabled", "false");
    catalogProperties.put("table.self-optimizing.quota", "0.1");
    catalogProperties.put("table.self-optimizing.group", "mygroup");
    catalogProperties.put("ams.address", "127.0.0.1");

    Map<String, String> result = CatalogUtil.mergeCatalogPropertiesToTable(userDefined, catalogProperties);
    Assert.assertEquals(expected, result);
  }

  /**
   * user-defined and default catalog prop 'self-optimizing.enabled' are both switched off,
   * remove optimizer related props from default catalog
   * while keep user-defined related prop and 'self-optimizing.enabled' itself.
   */
  @Test
  public void testMergeCatalogPropertiesToTable5() {
    Map<String, String> expected = new HashMap<>();
    expected.put("log-store.enabled", "true");
    expected.put("log-store.address", "168.0.0.1:9092");
    expected.put("log-store.type", "kafka");
    expected.put("log-store.consistency.guarantee.enable", "true");
    expected.put("self-optimizing.enabled", "false");
    // user-defined related prop should be kept
    expected.put("self-optimizing.quota", "0.2");
    expected.put("table.other-props", "foo");

    Map<String, String> userDefined = new HashMap<>();
    userDefined.put("log-store.enabled", "true");
    userDefined.put("self-optimizing.enabled", "false");
    userDefined.put("self-optimizing.quota", "0.2");
    userDefined.put("table.other-props", "foo");

    Map<String, String> catalogProperties = new HashMap<>();
    catalogProperties.put("table.log-store.enabled", "false");
    catalogProperties.put("table.log-store.address", "168.0.0.1:9092");
    catalogProperties.put("table.log-store.type", "kafka");
    catalogProperties.put("table.log-store.consistency.guarantee.enable", "true");
    catalogProperties.put("table.self-optimizing.enabled", "false");
    catalogProperties.put("table.self-optimizing.quota", "0.1");
    catalogProperties.put("table.self-optimizing.group", "mygroup");
    catalogProperties.put("ams.address", "127.0.0.1");

    Map<String, String> result = CatalogUtil.mergeCatalogPropertiesToTable(userDefined, catalogProperties);
    Assert.assertEquals(expected, result);
  }

  /**
   * when optimized flag is off in catalog props and no user-defined value,
   * remove optimizer related props but 'self-optimizing.enabled' itself.
   */
  @Test
  public void testMergeCatalogPropertiesToTable6() {
    Map<String, String> expected = new HashMap<>();
    expected.put("log-store.enabled", "true");
    expected.put("log-store.address", "168.0.0.1:9092");
    expected.put("log-store.type", "kafka");
    expected.put("log-store.consistency.guarantee.enable", "true");
    expected.put("self-optimizing.enabled", "false");
    expected.put("table.other-props", "foo");

    Map<String, String> userDefined = new HashMap<>();
    userDefined.put("log-store.enabled", "true");
    userDefined.put("table.other-props", "foo");

    Map<String, String> catalogProperties = new HashMap<>();
    catalogProperties.put("table.log-store.enabled", "false");
    catalogProperties.put("table.log-store.address", "168.0.0.1:9092");
    catalogProperties.put("table.log-store.type", "kafka");
    catalogProperties.put("table.log-store.consistency.guarantee.enable", "true");
    catalogProperties.put("table.self-optimizing.enabled", "false");
    catalogProperties.put("table.self-optimizing.quota", "0.1");
    catalogProperties.put("table.self-optimizing.group", "mygroup");
    catalogProperties.put("ams.address", "127.0.0.1");

    Map<String, String> result = CatalogUtil.mergeCatalogPropertiesToTable(userDefined, catalogProperties);
    Assert.assertEquals(expected, result);
  }

  /**
   * user-defined 'self-optimizing.enabled' is switched on,
   * overwrite behavior of default catalog props
   */
  @Test
  public void testMergeCatalogPropertiesToTable7() {
    Map<String, String> expected = new HashMap<>();
    expected.put("self-optimizing.enabled", "true");
    expected.put("self-optimizing.quota", "0.1");
    expected.put("self-optimizing.group", "mygroup");
    expected.put("table.other-props", "foo");

    Map<String, String> userDefined = new HashMap<>();
    userDefined.put("self-optimizing.enabled", "true");
    userDefined.put("table.other-props", "foo");

    Map<String, String> catalogProperties = new HashMap<>();
    catalogProperties.put("table.log-store.enabled", "false");
    catalogProperties.put("table.log-store.address", "168.0.0.1:9092");
    catalogProperties.put("table.log-store.type", "kafka");
    catalogProperties.put("table.log-store.consistency.guarantee.enable", "true");
    catalogProperties.put("table.self-optimizing.enabled", "false");
    catalogProperties.put("table.self-optimizing.quota", "0.1");
    catalogProperties.put("table.self-optimizing.group", "mygroup");
    catalogProperties.put("ams.address", "127.0.0.1");

    Map<String, String> result = CatalogUtil.mergeCatalogPropertiesToTable(userDefined, catalogProperties);
    Assert.assertEquals(expected, result);
  }
}