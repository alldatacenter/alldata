package com.netease.arctic.server.persistence;

import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestStatedPersistentBase {

  private static class ExtendedPersistency extends StatedPersistentBase {
    @StatedPersistentBase.StateField
    private String stringState = "";
    @StatedPersistentBase.StateField
    private int intState = 0;
    private boolean booleanField = false;
    private long longField = 0L;
  }

  private static class NormalClass {
    @StatedPersistentBase.StateField
    private String stringState = "";
    @StatedPersistentBase.StateField
    private int intState = 0;
    private boolean booleanField = false;
    private long longField = 0L;
  }

  @Test
  public void testStateField() throws Throwable {
    ExtendedPersistency proxy = new ExtendedPersistency();
    try {
      proxy.invokeConsisitency(() -> {
        proxy.stringState = "test";
        proxy.intState = 42;
        // simulate an exception being thrown
        throw new RuntimeException();
      });
    } catch (Throwable throwable) {
      // ignore
    }
    assertEquals("", proxy.stringState);
    assertEquals(0, proxy.intState);
  }

  @Test
  public void testNormalField() throws Throwable {
    ExtendedPersistency proxy = new ExtendedPersistency();
    try {
      proxy.invokeConsisitency(() -> {
        proxy.booleanField = true;
        proxy.longField = 123456789L;
        // simulate an exception being thrown
        throw new RuntimeException();
      });
    } catch (Throwable throwable) {
      // ignore
    }
    assertEquals(123456789L, proxy.longField);
    assertTrue(proxy.booleanField);
  }

  private void testNormalClass() {
    NormalClass obj = new NormalClass();
    obj.stringState = "test";
    obj.intState = 42;
    for (int i = 0; i < 10; i++) {
      obj.stringState = UUID.randomUUID().toString();
      obj.intState++;
    }
  }

  private void testStatedClass() {
    ExtendedPersistency obj = new ExtendedPersistency();
    obj.stringState = "test";
    obj.intState = 42;
    for (int i = 0; i < 10; i++) {
      obj.stringState = UUID.randomUUID().toString();
      obj.intState++;
    }
  }

  public static void main(String[] args) {
    for (int i = 0; i < 10; i++) {
      new TestStatedPersistentBase().testNormalClass();
      new TestStatedPersistentBase().testStatedClass();
    }

    long start = System.currentTimeMillis();
    for (int i = 0; i < 100000; i++) {
      new TestStatedPersistentBase().testNormalClass();
    }
    System.out.println("Normal class: " + (System.currentTimeMillis() - start));

    start = System.currentTimeMillis();
    for (int i = 0; i < 100000; i++) {
      new TestStatedPersistentBase().testStatedClass();
    }
    System.out.println("Stated class: " + (System.currentTimeMillis() - start));
  }
}