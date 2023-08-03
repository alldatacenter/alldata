package com.linkedin.feathr.common.value;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.google.common.collect.ImmutableMap;
import com.linkedin.feathr.common.FeatureValue;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.scalatest.testng.TestNGSuite;
import org.testng.annotations.Test;

import static java.util.Collections.*;
import static org.testng.Assert.*;


public class TestFeatureValueOldAPICompatibility extends TestNGSuite {
  @Test
  public void basicEqualityChecks() {
    {
      Object v1 = FeatureValue.createNumeric(10);
      Object v2 = FeatureValue.createNumeric(10);
      assertEquals(v1, v2);
    }
    {
      Object v1 = FeatureValue.createCategorical("foo");
      Object v2 = FeatureValue.createCategorical("foo");
      assertEquals(v1, v2);
    }
    {
      Object v1 = FeatureValue.createNumeric(1.0f);
      Object v2 = FeatureValue.createNumeric(40.0f);
      assertNotEquals(v1, v2);
    }
    {
      Object v1 = FeatureValue.createNumeric(1);
      Object v2 = FeatureValue.createCategorical("1");
      assertNotEquals(v1, v2);
    }
  }

  @Test
  public void testJavaObjectSerialization() throws Exception {
    {
      Object v1 = FeatureValue.createNumeric(5.0f);

      byte[] bytes = javaSerialize(v1);
      Object result = javaDeserialize(bytes);

      assertEquals(result, FeatureValue.createNumeric(5));
    }
    {
      Object v1 = FeatureValue.createDenseVector(Arrays.asList(0.1f, 0.2f, 0.3f, 0.4f));

      byte[] bytes = javaSerialize(v1);
      Object result = javaDeserialize(bytes);

      assertEquals(result, FeatureValue.createDenseVector(new Float[]{0.1f, 0.2f, 0.3f, 0.4f}));
    }
    {
      Object v1 = new FeatureValue(ImmutableMap.of("foo", 0.2f, "bar", 0.4f, "baz", 0.6f));

      byte[] bytes = javaSerialize(v1);
      Object result = javaDeserialize(bytes);

      assertEquals(result, FeatureValue.createStringTermVector(Arrays.asList(singletonMap("foo", 0.2f),
          singletonMap("bar", 0.4f), singletonMap("baz", 0.6f))));
    }
  }

  @Test
  public void testKryoSerialization() throws Exception {
    {
      Object v1 = FeatureValue.createNumeric(5.0f);

      byte[] bytes = kryoSerialize(v1);
      Object result = kryoDeserialize(bytes, FeatureValue.class);

      assertEquals(result, FeatureValue.createNumeric(5));
    }
    {
      Object v1 = FeatureValue.createDenseVector(Arrays.asList(0.1f, 0.2f, 0.3f, 0.4f));

      byte[] bytes = kryoSerialize(v1);
      Object result = kryoDeserialize(bytes, FeatureValue.class);

      assertEquals(result, FeatureValue.createDenseVector(new Float[]{0.1f, 0.2f, 0.3f, 0.4f}));
    }
    {
      Object v1 = new FeatureValue(ImmutableMap.of("foo", 0.2f, "bar", 0.4f, "baz", 0.6f));

      byte[] bytes = kryoSerialize(v1);
      Object result = kryoDeserialize(bytes, FeatureValue.class);

      assertEquals(result, FeatureValue.createStringTermVector(Arrays.asList(singletonMap("foo", 0.2f),
          singletonMap("bar", 0.4f), singletonMap("baz", 0.6f))));
    }
  }

  private static byte[] javaSerialize(Object object) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(baos);
    oos.writeObject(object);
    oos.close();
    baos.close();
    return baos.toByteArray();
  }

  private static Object javaDeserialize(byte[] bytes) throws IOException, ClassNotFoundException {
    ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
    ObjectInputStream ois = new ObjectInputStream(bais);
    Object result = ois.readObject();
    ois.close();
    bais.close();
    return result;
  }

  private static byte[] kryoSerialize(Object object) {
    Kryo kryo = new Kryo();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    Output output = new Output(baos);
    kryo.writeObject(output, object);
    output.close();
    return baos.toByteArray();
  }

  private static <T> T kryoDeserialize(byte[] bytes, Class<T> clazz) {
    Kryo kryo = new Kryo();
    ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
    Input input = new Input(bais);
    T object = (T) kryo.readObject(input, clazz);
    input.close();
    return object;
  }
}