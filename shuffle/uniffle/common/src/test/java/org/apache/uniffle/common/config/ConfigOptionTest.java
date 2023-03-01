/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.uniffle.common.config;

import java.util.List;
import java.util.function.Function;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class ConfigOptionTest {

  @Test
  public void testSetKVWithStringTypeDirectly() {
    final ConfigOption<Integer> intConfig = ConfigOptions
            .key("rss.key1")
            .intType()
            .defaultValue(1000)
            .withDescription("Int config key1");

    RssConf conf = new RssBaseConf();
    conf.setString("rss.key1", "2000");
    assertEquals(2000, conf.get(intConfig));

    final ConfigOption<Boolean> booleanConfig = ConfigOptions
            .key("key2")
            .booleanType()
            .defaultValue(false)
            .withDescription("Boolean config key");

    conf.setString("key2", "true");
    assertTrue(conf.get(booleanConfig));
    conf.setString("key2", "False");
    assertFalse(conf.get(booleanConfig));
  }

  enum TestType {
    TYPE_1,
    TYPE_2,
  }

  @Test
  public void testEnumType() {
    final ConfigOption<TestType> enumConfigOption = ConfigOptions
        .key("rss.enum")
        .enumType(TestType.class)
        .defaultValue(TestType.TYPE_1)
        .withDescription("enum test");

    RssBaseConf conf = new RssBaseConf();

    // case1: default value
    assertEquals(TestType.TYPE_1, conf.get(enumConfigOption));

    // case2: return the user specified value
    conf.set(enumConfigOption, TestType.TYPE_2);
    assertEquals(TestType.TYPE_2, conf.get(enumConfigOption));

    // case3: set enum val with string
    conf = new RssBaseConf();
    conf.setString("rss.enum", "TYPE_2");
    assertEquals(TestType.TYPE_2, conf.get(enumConfigOption));

    // case4: set the illegal enum val with string
    conf = new RssBaseConf();
    conf.setString("rss.enum", "TYPE_3");
    try {
      conf.get(enumConfigOption);
      fail();
    } catch (IllegalArgumentException e) {
      // ignore
    }

    // case5: the case insensitive
    conf = new RssBaseConf();
    conf.setString("rss.enum", "type_2");
    assertEquals(TestType.TYPE_2, conf.get(enumConfigOption));
    conf.setString("rss.enum", "TyPe_2");
    assertEquals(TestType.TYPE_2, conf.get(enumConfigOption));
  }

  @Test
  public void testListTypes() {
    // test the string type list.
    final ConfigOption<List<String>> listStringConfigOption = ConfigOptions
            .key("rss.key1")
            .stringType()
            .asList()
            .defaultValues("h1", "h2")
            .withDescription("List config key1");

    List<String> defaultValues = listStringConfigOption.defaultValue();
    assertEquals(2, defaultValues.size());
    assertSame(String.class, listStringConfigOption.getClazz());

    RssBaseConf conf = new RssBaseConf();
    conf.set(listStringConfigOption, Lists.newArrayList("a", "b", "c"));

    List<String> vals = conf.get(listStringConfigOption);
    assertEquals(3, vals.size());
    assertEquals(Lists.newArrayList("a", "b", "c"), vals);

    // test the long type list
    final ConfigOption<List<Long>> listLongConfigOption = ConfigOptions
            .key("rss.key2")
            .longType()
            .asList()
            .defaultValues(1L)
            .withDescription("List long config key2");

    List<Long> longDefaultVals = listLongConfigOption.defaultValue();
    assertEquals(longDefaultVals.size(), 1);
    assertEquals(Lists.newArrayList(1L), longDefaultVals);

    conf.setString("rss.key2", "1,2,3");
    List<Long> longVals = conf.get(listLongConfigOption);
    assertEquals(Lists.newArrayList(1L, 2L, 3L), longVals);

    // test overwrite the same conf key.
    conf.set(listLongConfigOption, Lists.newArrayList(1L, 2L, 3L, 4L));
    assertEquals(Lists.newArrayList(1L, 2L, 3L, 4L), conf.get(listLongConfigOption));

    // test the no-default values
    final ConfigOption<List<Long>> listLongConfigOptionWithoutDefault = ConfigOptions
            .key("rss.key3")
            .longType()
            .asList()
            .noDefaultValue()
            .withDescription("List long config key3 without default values");
    List<Long> valsWithoutDefault = listLongConfigOptionWithoutDefault.defaultValue();
    assertNull(valsWithoutDefault);

    // test the method of check
    final ConfigOption<List<Integer>> checkLongValsOptions = ConfigOptions
            .key("rss.key4")
            .intType()
            .asList()
            .checkValue((Function<Integer, Boolean>) val -> val > 0, "Every number of list should be positive")
            .noDefaultValue()
            .withDescription("The key4 is illegal");

    conf.set(checkLongValsOptions, Lists.newArrayList(-1, 2, 3));

    try {
      conf.get(checkLongValsOptions);
      fail();
    } catch (IllegalArgumentException illegalArgumentException) {
      // no op
    }

    conf.set(checkLongValsOptions, Lists.newArrayList(1, 2, 3));
    try {
      conf.get(checkLongValsOptions);
    } catch (IllegalArgumentException illegalArgumentException) {
      fail();
    }

    // test the empty list
    final ConfigOption<List<String>> emptyListStringOption = ConfigOptions
            .key("rss.key5")
            .stringType()
            .asList()
            .noDefaultValue()
            .withDescription("List config key5");

    List<String> key5Val = conf.get(emptyListStringOption);
    assertNull(key5Val);

    conf.setString(emptyListStringOption.key(), "");
    assertEquals(conf.get(emptyListStringOption).size(), 0);
    conf.setString(emptyListStringOption.key(), ", ");
    assertEquals(conf.get(emptyListStringOption).size(), 0);
    conf.setString(emptyListStringOption.key(), " ");
    assertEquals(conf.get(emptyListStringOption).size(), 0);
  }

  @Test
  public void testBasicTypes() {
    final ConfigOption<Integer> intConfig = ConfigOptions
        .key("rss.key1")
        .intType()
        .defaultValue(1000)
        .withDescription("Int config key1");
    assertSame(Integer.class, intConfig.getClazz());
    assertEquals(1000, (int) intConfig.defaultValue());
    assertEquals("Int config key1", intConfig.description());

    final ConfigOption<Long> longConfig = ConfigOptions
        .key("rss.key2")
        .longType()
        .defaultValue(1999L);
    assertTrue(longConfig.hasDefaultValue());
    assertEquals(1999L, (long) longConfig.defaultValue());

    final ConfigOption<String> stringConfig = ConfigOptions
        .key("rss.key3")
        .stringType()
        .noDefaultValue();
    assertFalse(stringConfig.hasDefaultValue());
    assertEquals("", stringConfig.description());

    final ConfigOption<Boolean> booleanConfig = ConfigOptions
        .key("key4")
        .booleanType()
        .defaultValue(false)
        .withDescription("Boolean config key");
    assertFalse(booleanConfig.defaultValue());
    assertEquals("Boolean config key", booleanConfig.description());

    final ConfigOption<Integer> positiveInt = ConfigOptions
        .key("key5")
        .intType()
        .checkValue((v) -> {
          return v > 0;
        }, "The value of key5 must be positive")
        .defaultValue(1)
        .withDescription("Positive integer key");
    RssBaseConf conf = new RssBaseConf();
    conf.set(positiveInt, -1);
    boolean isException = false;
    try {
      conf.get(positiveInt);
    } catch (IllegalArgumentException ie) {
      isException = true;
      assertTrue(ie.getMessage().contains("The value of key5 must be positive"));
    }
    assertTrue(isException);
    conf.set(positiveInt, 1);
    try {
      conf.get(positiveInt);
    } catch (IllegalArgumentException ie) {
      fail();
    }
  }
}
