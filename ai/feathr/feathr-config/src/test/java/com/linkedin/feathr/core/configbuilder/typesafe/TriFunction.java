package com.linkedin.feathr.core.configbuilder.typesafe;

@FunctionalInterface
public interface TriFunction<T, U, V, R> {
  R apply(T t, U u, V v);
}
