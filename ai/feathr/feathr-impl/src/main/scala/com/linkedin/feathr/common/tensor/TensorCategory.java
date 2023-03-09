package com.linkedin.feathr.common.tensor;

/**
 * The category of the tensor type, such as dense, sparse, etc.
 * NOTE: at the moment this is stored as is, and no additional validation is guaranteed
 * (such as choosing the right tensor builder based on the tensor category).

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
