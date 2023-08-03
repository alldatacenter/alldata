package com.netease.arctic.utils.map;

import com.google.common.collect.Maps;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openjdk.jol.info.GraphLayout;

import java.io.Serializable;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class TestSimpleSpillableMap {

  private static final Random random = new Random(100000);

  private SimpleSpillableMap<Key, Value> map;
  private long keySize;
  private long valueSize;

  @Before
  public void initSizes() {
    keySize = GraphLayout.parseInstance(new Key()).totalSize();
    valueSize = GraphLayout.parseInstance(new Value()).totalSize();
  }

  @Test
  public void testMemoryMap() {
    SimpleSpillableMap<Key, Value> map = testMap(10, 10);
    Assert.assertTrue(map.getSizeOfFileOnDiskInBytes() == 0);
    map.close();
  }

  @Test
  public void testSpilledMap() {
    SimpleSpillableMap<Key, Value> map = testMap(0, 20);
    Assert.assertTrue(map.getSizeOfFileOnDiskInBytes() > 0);
    map.close();
  }

  @Test
  public void testSpillableMap() {
    SimpleSpillableMap<Key, Value> map = testMap(10, 20);
    Assert.assertTrue(map.getSizeOfFileOnDiskInBytes() > 0);
    map.close();
  }

  private SimpleSpillableMap<Key, Value> testMap(long expectMemorySize, int expectKeyCount) {
    SimpleSpillableMap<Key, Value> actualMap = new SimpleSpillableMap<>(expectMemorySize * (keySize + valueSize),
        null, new DefaultSizeEstimator<>(), new DefaultSizeEstimator<>());
    Assert.assertTrue(actualMap.getSizeOfFileOnDiskInBytes() == 0);
    Map<Key, Value> expectedMap = Maps.newHashMap();
    for (int i = 0; i < expectKeyCount; i++) {
      Key key = new Key();
      Value value = new Value();
      expectedMap.put(key, value);
      actualMap.put(key, value);
    }
    for (Key key : expectedMap.keySet()) {
      Assert.assertEquals(expectedMap.get(key), actualMap.get(key));
    }
    Assert.assertEquals(expectMemorySize, actualMap.getMemoryMapSize());
    Assert.assertEquals(
        expectMemorySize * (keySize + valueSize),
        actualMap.getMemoryMapSpaceSize());
    return actualMap;
  }

  private static class Key implements Serializable {
    String id = UUID.randomUUID().toString();

    Long num = random.nextLong();

    @Override
    public int hashCode() {
      return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      return id.equals(((Key) obj).id);
    }
  }

  private static class Value implements Serializable {
    Long value = random.nextLong();
    String[] values = new String[10];

    Value() {
      for (int i = 0; i < values.length; i++) {
        values[i] = UUID.randomUUID().toString();
      }
    }

    @Override
    public boolean equals(Object obj) {
      return value == (long) ((Value) obj).value;
    }

    @Override
    public String toString() {
      return Long.toString(value);
    }
  }
}

