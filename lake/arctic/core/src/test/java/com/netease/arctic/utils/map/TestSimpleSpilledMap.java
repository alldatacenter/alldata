package com.netease.arctic.utils.map;

import com.netease.arctic.utils.SerializationUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.Serializable;

public class TestSimpleSpilledMap {

  private SimpleSpillableMap.SimpleSpilledMap map;

  @Before
  public void createMap() {
    SimpleSpillableMap spillableMap = new SimpleSpillableMap(100L,
        null, new StructLikeWrapperSizeEstimator(), new DefaultSizeEstimator<>());
    map = spillableMap.new SimpleSpilledMap(
        SerializationUtils.createJavaSimpleSerializer(),
        SerializationUtils.createJavaSimpleSerializer(), null);
  }

  @After
  public void disposeMap() {
    map.close();
    map = null;
  }

  @Test
  public void testPutGetRemove() {
    Key key = new Key();
    Value value = new Value();
    map.put("name", 555);
    map.put(2, "zjs");
    map.put(4556, "zyx");
    map.put(key, value);
    Assert.assertEquals(555, map.get("name"));
    Assert.assertEquals("zjs", map.get(2));
    Assert.assertEquals("zyx", map.get(4556));
    Assert.assertEquals(value, map.get(key));
    map.delete(4556);
    Assert.assertNull(map.get(4556));
    map.put(4556, value);
    Assert.assertEquals(value, map.get(4556));
  }

  @Test
  public void testPutNull() {
    Key key = new Key();
    Value value = new Value();
    map.put(key, value);
    Assert.assertEquals(value, map.get(key));
    Assert.assertThrows(Exception.class, () -> map.put(key, null));
    Assert.assertThrows(Exception.class, () -> map.put(null, value));
  }

  private static class Key implements Serializable {
    String key = "Key";

    @Override
    public boolean equals(Object obj) {
      return ((Key) obj).key == key;
    }
  }

  private class Value implements Serializable {
    int value = 666;

    @Override
    public boolean equals(Object obj) {
      return ((Value) obj).value == value;
    }
  }
}
