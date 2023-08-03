package com.linkedin.feathr.common.util;

import com.google.common.collect.ImmutableMap;
import com.linkedin.feathr.common.FeatureValue;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.scalatest.testng.TestNGSuite;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.linkedin.feathr.common.util.MvelContextUDFs.*;
import static org.testng.Assert.*;


/**
 * Unit tests for {@link MvelContextUDFs}
 */
public class TestMvelContextUDFs extends TestNGSuite {
  @Test
  public void testGetDataType() {
    Assert.assertEquals(get_data_type("A"), "java.lang.String");
    Assert.assertEquals(get_data_type(null), "null");
  }

  @Test
  public void testCastDouble() {
    Assert.assertEquals(cast_double("1.1"), 1.1d);
    Assert.assertEquals(cast_double("1.1e10"), 1.1E10d);
  }


  @Test
  public void testCastFloat() {
    Assert.assertEquals(cast_float("1.1"), 1.1f);
    Assert.assertEquals(cast_float("1.1e10"), 1.1E10f);
  }

  @Test
  public void testCastInteger() {
    Assert.assertEquals(cast_int("1"), (Integer)1);
  }

  @Test
  public void testIfElse() {
    Assert.assertEquals(if_else(true, "a", "b"), "a");
    Assert.assertEquals(if_else(false, "a", "b"), "b");
  }

  @Test
  public void testCosineSimilarity() {
    // Test basic cosine similarity calculation
    Map<String, Float> categoricalOutput1 = new HashMap<>();
    categoricalOutput1.put("A", 1F);
    categoricalOutput1.put("B", 1F);

    Map<String, Float> categoricalOutput2 = new HashMap<>();
    categoricalOutput2.put("B", 1F);
    categoricalOutput2.put("C", 1F);

    assertEquals(cosineSimilarity(categoricalOutput1, categoricalOutput2), 0.5F, 1e-5);

    // Test cosine similarity of zero vectors
    categoricalOutput1.clear();
    assertEquals(cosineSimilarity(categoricalOutput1, categoricalOutput2), 0.0F);
    categoricalOutput2.clear();
    assertEquals(cosineSimilarity(categoricalOutput1, categoricalOutput2), 0.0F);
  }

  @DataProvider
  public Object[][] isNonZeroCases() {
    return new Object[][]{
        new Object[]{true, true},
        new Object[]{1f, true},
        new Object[]{"foo", true}, // categoricals are considered to be non-zero
        new Object[]{Arrays.asList(100, 200), true}, // dense vectors with non-zeros are non-zero
        new Object[]{Arrays.asList("foo", "bar"), true}, // categoricals are considered to be non-zero
        new Object[]{Collections.singletonMap("foo", 0f), false},
        new Object[]{Arrays.asList(0, 0), false}, // dense vectors with only zeros are not non-zero
        new Object[]{0f, false},
    };
  }
  @Test(dataProvider = "isNonZeroCases")
  public void testIsNonZero(Object input, boolean expectedOutput) {
    Assert.assertEquals(isNonZero(input), expectedOutput);

  }

  @DataProvider
  public Object[][] isPresentCases() {
    return new Object[][]{
        new Object[]{true, true},
        new Object[]{FeatureValue.createNumeric(10), true},
        new Object[]{null, false},
    };
  }
  @Test(dataProvider = "isPresentCases")
  public void testIsPresent(Object input, boolean expectedOutput) {
    Assert.assertEquals(isPresent(input), expectedOutput);
  }

  @DataProvider
  public Object[][] isBooleanCases() {
    return new Object[][]{
        new Object[]{true, true},
        new Object[]{false, false},
        new Object[]{false, false},
        new Object[]{FeatureValue.createBoolean(true), true},
        new Object[]{FeatureValue.createBoolean(false), false},
        new Object[]{Collections.emptyMap(), false}, // raw term vector which is compatible w/ boolean
    };
  }

  @Test
  public void testToUpperCase() {
    Assert.assertEquals(toUpperCase("Aa"), "AA");
  }

  @Test
  public void testToLowerCase() {
    Assert.assertEquals(toLowerCase("Aa"), "aa");
  }

  @DataProvider
  public Object[][] testToCompoundKeyCases() {
    return new Object[][]{
        new Object[]{ ImmutableMap.of("a", 123L, "b", 234L)},
        new Object[]{ ImmutableMap.of("a", "123", "b", 234L)},
        new Object[]{ ImmutableMap.of("someNumber", BigDecimal.ZERO)},
        new Object[]{ ImmutableMap.of()},
    };
  }
}