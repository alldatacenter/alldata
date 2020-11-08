package com.platform.field.dynamicrules;

import java.lang.reflect.Field;
import java.math.BigDecimal;

public class FieldsExtractor {

  public static String getFieldAsString(Object object, String fieldName)
      throws IllegalAccessException, NoSuchFieldException {
    Class cls = object.getClass();
    Field field = cls.getField(fieldName);
    return field.get(object).toString();
  }

  public static double getDoubleByName(String fieldName, Object object)
      throws NoSuchFieldException, IllegalAccessException {
    Field field = object.getClass().getField(fieldName);
    return (double) field.get(object);
  }

  public static BigDecimal getBigDecimalByName(String fieldName, Object object)
      throws NoSuchFieldException, IllegalAccessException {
    Field field = object.getClass().getField(fieldName);
    return new BigDecimal(field.get(object).toString());
  }

  @SuppressWarnings("unchecked")
  public static <T> T getByKeyAs(String keyName, Object object)
      throws NoSuchFieldException, IllegalAccessException {
    Field field = object.getClass().getField(keyName);
    return (T) field.get(object);
  }
}
