package com.linkedin.feathr.core.config.producer.definitions;

/**
 * Specifies the tensor category.
 * This is the same as com.linkedin.quince.relational.types.TensorCategory
 */
public enum TensorCategory {
  /**
   * Tensors of this category map some subset of the dimension space to values.
   */
  SPARSE,
  /**
   * Tensors of this category map the entire dimension space to values.
   * This includes scalar values (which are modeled as dense tensors with 0 dimensions).
   */
  DENSE,
  /**
   * More general than DENSE, this category relaxes the constraint that shape of every dimension is constant within a single data instance.
   */
  RAGGED
}
