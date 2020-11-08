package com.platform.field.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Config {

  private final Map<Param<?>, Object> values = new HashMap<>();

  public <T> void put(Param<T> key, T value) {
    values.put(key, value);
  }

  public <T> T get(Param<T> key) {
    return key.getType().cast(values.get(key));
  }

  public <T> Config(
      Parameters inputParams,
      List<Param<String>> stringParams,
      List<Param<Integer>> intParams,
      List<Param<Boolean>> boolParams) {
    overrideDefaults(inputParams, stringParams);
    overrideDefaults(inputParams, intParams);
    overrideDefaults(inputParams, boolParams);
  }

  public static Config fromParameters(Parameters parameters) {
    return new Config(
        parameters, Parameters.STRING_PARAMS, Parameters.INT_PARAMS, Parameters.BOOL_PARAMS);
  }

  private <T> void overrideDefaults(Parameters inputParams, List<Param<T>> params) {
    for (Param<T> param : params) {
      put(param, inputParams.getOrDefault(param));
    }
  }
}
