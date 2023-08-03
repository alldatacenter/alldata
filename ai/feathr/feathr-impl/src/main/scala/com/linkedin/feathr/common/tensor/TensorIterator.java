package com.linkedin.feathr.common.tensor;

/**
 * TensorIterator is responsible for a range of rows (possibly empty).
 * At every moment of time, one row is current (zero if the range is empty).
 * Both dimensions and the value of the current row are available via getters.
 * Getters of column values are specialized for primitive types to save on an additional object allocation/dereferencing.
 */
public interface TensorIterator extends ReadableTuple {
    TensorData getTensorData();

    /**
     * Rewinds the iterator to the beginning of its range.
     */
    void start();

    /**
     * @return if the iterator is still within its range.
     */
    boolean isValid();

    /**
     * Move iterator to the next row.
     */
    void next();

    /**
     * @return a copy of the iterator at the current position. Can be used to return to a previously visited position.
     */
    TensorIterator getCopy();

    /**
     * Originally created for ease of debugging. Should NOT be used for returning arbitrary objects, only for primitives.
     * @deprecated this should not be used
     */
    @Deprecated
    Object getValue(int column);

    /**
     * @return a String at given column (dimension or value).
     */
    @Override
    default String getString(int column) {
        throw new UnsupportedOperationException("No implementation by default, please implement. ");
    }

    /**
     * @return a double at given column (dimension or value).
     */
    @Override
    default double getDouble(int column) {
        throw new UnsupportedOperationException("No implementation by default, please implement. ");
    }

    /**
     * @return a boolean at given column (dimension or value)
     */
    default boolean getBoolean(int column) {
        throw new UnsupportedOperationException("No implementation by default, please implement.");
    }

    /**
     * @return a byte array at given column (dimension or value)
     */
    @Override
    default byte[] getBytes(int column) {
        throw new UnsupportedOperationException("No implementation by default, please implement.");
    }

    default Representable[] getTypes() {
        return getTensorData().getTypes();
    }
}
