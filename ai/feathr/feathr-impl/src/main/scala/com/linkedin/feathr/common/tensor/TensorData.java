package com.linkedin.feathr.common.tensor;

/**
 * A typed container of structured data.
 * Maps a (potentially empty) tuple of dimensions to a scalar value.
 * For efficiency, a single object represents the entire data structure.
 * Sequential access is provided via TensorIterator.
 *
 * The typical use of TensorData is:
 * <pre>
 * TensorData td = ...;
 * TensorIterator ti = td.iterator();
 * ti.start();
 * while (ti.isValid()) {
 *  ti.getInt(0);
 *  ti.getFloat(1);
 *  ti.getInt(2);
 * // and others getXXX(),
 * // the getXXX(dimensionality) corresponding to the value
 *  ti.next();
 * }
 * </pre>
 */
public interface TensorData {
    /**
     * @return the arity, which is the total number of columns.
     * As columns are dimensions plus a value, arity is by one greater than dimensionality.
     */
    default int getArity() {
        return getTypes().length;
    }

    /**
     * @return the types of all dimensions and the value as a single array.
     */
    Representable[] getTypes();

    /**
     * @return conservative estimate of number of entries in the mapping from dimensions to values.
     * Can be cheaper to evaluate than the exact one.
     */
    int estimatedCardinality();

    /**
     * @return exact number of entries in the mapping from dimensions to values.
     */
    int cardinality();

    default boolean isEmpty() {
        return !this.iterator().isValid();
    }

    /**
     * @return an iterator over entries in the mapping from dimensions to the value.
     */
    TensorIterator iterator();

    /**
     *
     * @return the current shape of the data or the supplied shape of the data
     */
    default long[] getShape() {
        throw new UnsupportedOperationException("getShape has not been implemented for " + this.getClass().getCanonicalName());
    }
}
