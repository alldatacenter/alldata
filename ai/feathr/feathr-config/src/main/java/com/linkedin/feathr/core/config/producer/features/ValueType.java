package com.linkedin.feathr.core.config.producer.features;

import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * Specifies the value type of a feature. It includes all primitive types and string.
 */
public enum ValueType {
  STRING,
  INT,
  LONG,
  DOUBLE,
  FLOAT,
  BOOLEAN,
  BYTE;

  private static final Logger logger = LogManager.getLogger(ValueType.class);

  public static Optional<ValueType> fromName(String name) {
    ValueType res = null;

    for (ValueType vt : values()) {
      if (vt.name().equalsIgnoreCase(name)) {
        res = vt;
        break;
      }
    }

    return Optional.ofNullable(res);
  }
}
