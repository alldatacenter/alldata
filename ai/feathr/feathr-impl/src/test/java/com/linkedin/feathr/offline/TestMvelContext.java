package com.linkedin.feathr.offline;

import org.scalatest.testng.TestNGSuite;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static com.linkedin.feathr.common.util.MvelContextUDFs.*;
import static org.testng.Assert.assertEquals;


public class TestMvelContext extends TestNGSuite {
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

  @Test
  public void testDotProduct() {
    // Test basic dot product calculation
    Map<String, Float> categoricalOutput1 = new HashMap<>();
    categoricalOutput1.put("A", 1F);
    categoricalOutput1.put("B", 1F);

    Map<String, Float> categoricalOutput2 = new HashMap<>();
    categoricalOutput2.put("B", 1F);
    categoricalOutput2.put("C", 1F);

    assertEquals(dotProduct(categoricalOutput1, categoricalOutput2), 1.0D);

    // Test dot product of zero vectors
    categoricalOutput1.clear();
    assertEquals(dotProduct(categoricalOutput1, categoricalOutput2), 0.0D);
    categoricalOutput2.clear();
    assertEquals(dotProduct(categoricalOutput1, categoricalOutput2), 0.0D);
  }
}
