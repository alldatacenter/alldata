package com.netease.arctic.spark.distributions;

public interface Transform extends Expression {
  String name();

  NamedReference[] references();

  Expression[] arguments();
}
