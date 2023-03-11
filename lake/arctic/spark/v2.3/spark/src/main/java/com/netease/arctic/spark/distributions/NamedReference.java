package com.netease.arctic.spark.distributions;

public interface NamedReference extends Expression {
  String[] fieldNames();
}
