package com.netease.arctic.utils.map;

import com.netease.arctic.data.ChangedLsn;
import com.netease.arctic.iceberg.StructProjection;
import org.apache.iceberg.Schema;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.types.Types;
import org.apache.iceberg.util.StructLikeMap;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;

public class TestStructLikeMap {

  private static final Schema PK_SCHEMA = new Schema(
      Arrays.asList(
          Types.NestedField.of(1, false, "c1", Types.DoubleType.get()),
          Types.NestedField.of(2, false, "c2", Types.IntegerType.get()),
          Types.NestedField.of(3, false, "c3", Types.BooleanType.get())));

  private static final Schema DATA_SCHEMA = new Schema(
      Arrays.asList(
          Types.NestedField.of(1, false, "c1", Types.DoubleType.get()),
          Types.NestedField.of(2, false, "c2", Types.IntegerType.get()),
          Types.NestedField.of(3, false, "c3", Types.BooleanType.get()),
          Types.NestedField.of(4, false, "c4", Types.StringType.get()),
          Types.NestedField.of(5, false, "c5", Types.BinaryType.get())));

  private static final Schema DELETE_SCHEMA = new Schema(
      Arrays.asList(
          Types.NestedField.of(1, false, "c1", Types.DoubleType.get()),
          Types.NestedField.of(2, false, "c2", Types.IntegerType.get()),
          Types.NestedField.of(3, false, "c3", Types.BooleanType.get())));

  @Test
  public void testMemoryMap() throws IOException {
    testMap(StructLikeMemoryMap.create(PK_SCHEMA.asStruct()));
  }

  @Test
  public void testSpillableMap() throws IOException {
    testMap(StructLikeSpillableMap.create(PK_SCHEMA.asStruct(), 10L, null));
  }

  private void testMap(StructLikeBaseMap<ChangedLsn> actualMap) throws IOException {
    StructLikeMap<ChangedLsn> expectedMap = StructLikeMap.create(PK_SCHEMA.asStruct());
    long count = 100;
    for (long i = 0; i < count; i++) {
      StructLike delete = new DeleteStructLike();
      StructLike key = StructProjection.create(DELETE_SCHEMA, PK_SCHEMA).copyWrap(delete);
      expectedMap.put(key, ChangedLsn.of(i, i));
      actualMap.put(key, ChangedLsn.of(i, i));
    }

    for (long i = 0; i < count; i++) {
      StructLike data = new DataStructLike();
      StructLike key = StructProjection.create(DATA_SCHEMA, PK_SCHEMA).copyWrap(data);
      Assert.assertEquals(expectedMap.get(key), actualMap.get(key));
    }
    actualMap.close();
  }

  private static class DataStructLike implements StructLike {

    private static final Random RANDOM = new Random(100000);

    private final Object[] values = new Object[] {
        RANDOM.nextDouble(),
        RANDOM.nextInt(),
        RANDOM.nextBoolean(),
        UUID.randomUUID().toString(),
        UUID.randomUUID().toString().getBytes("utf8")
    };

    DataStructLike() throws UnsupportedEncodingException {
    }

    @Override
    public int size() {
      return 5;
    }

    @Override
    public <T> T get(int pos, Class<T> javaClass) {
      return javaClass.cast(values[pos]);
    }

    @Override
    public <T> void set(int pos, T value) {
      throw new UnsupportedOperationException();
    }
  }

  private static class DeleteStructLike implements StructLike {

    private static final Random RANDOM = new Random(100000);

    private final Object[] values = new Object[] {
        RANDOM.nextDouble(),
        RANDOM.nextInt(),
        RANDOM.nextBoolean()
    };

    DeleteStructLike() {
    }

    @Override
    public int size() {
      return 3;
    }

    @Override
    public <T> T get(int pos, Class<T> javaClass) {
      return javaClass.cast(values[pos]);
    }

    @Override
    public <T> void set(int pos, T value) {
      throw new UnsupportedOperationException();
    }
  }
}
