package com.platform.field.dynamicrules.functions;

import com.platform.field.dynamicrules.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.util.Collector;

@Slf4j
public class JsonSerializer<T> extends RichFlatMapFunction<T, String> {

  private JsonMapper<T> parser;
  private final Class<T> targetClass;

  public JsonSerializer(Class<T> sourceClass) {
    this.targetClass = sourceClass;
  }

  @Override
  public void open(Configuration parameters) throws Exception {
    super.open(parameters);
    parser = new JsonMapper<>(targetClass);
  }

  @Override
  public void flatMap(T value, Collector<String> out) throws Exception {
    System.out.println(value);
    try {
      String serialized = parser.toString(value);
      out.collect(serialized);
    } catch (Exception e) {
      log.warn("Failed serializing to JSON dropping it:", e);
    }
  }
}
