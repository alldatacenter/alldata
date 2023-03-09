package com.linkedin.feathr.common.tensorbuilder;

import com.linkedin.feathr.common.tensor.TensorData;
import com.linkedin.feathr.common.tensor.TensorIterator;
import com.linkedin.feathr.common.tensor.WriteableTuple;

/**
 * A stateful object used to build instances of tensors of some specific representation without knowing its details.
 *
 * Values are written to the builder row-wise.
 *
 * How to use:
 *  1. Initialize the builder using {@link #start}
 *  2. Set values for each column in the first row, using {@link #setInt}, {@link #setFloat}, etc.
 *  3. Call {@link #append} to finish the row.
 *  4. Repeat steps 2 and 3 for any remaining rows.
 *  5. Call {@link #build} to build the TensorData object.
 *
 * Instances of TensorBuilder can be safely reused by calling {@link #start} to "reset" the builder.
 *
 * Except for any subclass implementation that explicitly states otherwise, instances of TensorBuilder MUST NOT be
 * used in a concurrent fashion.
 */
public interface TensorBuilder<T extends TensorBuilder<T>> extends TypedOperator, WriteableTuple<T> {

    /**
     * Build the tensor
     * Does NOT reset the internal state, so if you plan to reuse this instance, call {@link #start()} or {@link #start(int)} before reusing.
     * @return the final built tensor, forcing lexicographical sorting.
     */
    default TensorData build() {
        return build(false);
    }

    /**
     * Build the tensor
     * @param sort if set to true, the built tensor's rows will be sorted lexicographically
     * @return the built tensor
     */
    TensorData build(boolean sort);

    @Override
    default T setDouble(int index, double value) {
        throw new UnsupportedOperationException("No implementation by default, please implement. ");
    }

    @Override
    default T setString(int index, String value) {
        throw new UnsupportedOperationException("No implementation by default, please implement. ");
    }

    @Override
    default T setBoolean(int index, boolean value) {
        throw new UnsupportedOperationException("No implementation by default, please implement.");
    }

    /**
     * Tells this TensorBuilder that the current row is complete. This method MUST be called after using
     * {@link #setInt}, {@link #setFloat}, etc. to set all the columns of the current row.
     * @return a reference to this TensorBuilder (`this`)
     */
    T append();

    /**
     * Initializes this TensorBuilder. This method (or overloaded {@link #start()} MUST be called before setting values
     * to this TensorBuilder. This method may also be used to "reset" the builder, enabling it to be reused.
     *
     * @param estimatedRows The estimated number of rows in this tensor. Implementations can use this to allocate internal
     *                      array sizes, for example.
     * @return a reference to this TensorBuilder (`this`)
     */
    T start(int estimatedRows);

    /**
     * Initializes this TensorBuilder. This method (or overloaded {@link #start(int)} MUST be called before setting values
     * to this TensorBuilder. This method may also be used to "reset" the builder, enabling it to be reused.
     *
     * @return a reference to this TensorBuilder (`this`)
     */
    default T start() {
        return start(0);
    }

    /**
     * Copy a single column from source to this builder.
     * @param source the source from which to copy
     * @param sourceColumn the index number of the column to be copied from the source
     * @param destinationColumn the index number of the column to be written to in this TensorBuilder
     * @return a reference to this TensorBuilder (`this`)
     */
    // NOTE: The type of `source` should be ReadableTuple instead of TensorIterator, but that would be a binary
    // incompatible API change. So I won't do it now, but we should do it someday as part of a major release.
    default T copyColumn(TensorIterator source, int sourceColumn, int destinationColumn) {
        getTypes()[destinationColumn].getRepresentation().copy(source, sourceColumn, this, destinationColumn);
        return (T) this;
    }

    /**
     * Copy multiple columns from source to this builder.
     * @param source the source from which to copy
     * @param sourceColumns array of indices of the columns to be copied from the source and written to this
     *                      TensorBuilder. Note that the columns are copied from the source into the same position in this
     *                      builder; values read from column `i` are written to column `i`, etc.
     * @return a reference to this TensorBuilder (`this`)
     */
    // NOTE: The type of `source` should be ReadableTuple instead of TensorIterator, but that would be a binary
    // incompatible API change. So I won't do it now, but we should do it someday as part of a major release.
    default T copyColumns(TensorIterator source, int[] sourceColumns) {
        for (int i = 0; i < sourceColumns.length; i++) {
            copyColumn(source, sourceColumns[i], i);
        }
        return (T) this;
    }


    default T copyColumns(TensorIterator left, TensorIterator right, int[] sourceColumns) {
        return copyColumns(left, right, sourceColumns, left.getTensorData().getArity());
    }

    // Copy multiple columns from two sources to this builder. Columns are numbered throughout the sources, so columns
    // greater than the arity of the left source belong to the right one.
    default T copyColumns(TensorIterator left, TensorIterator right, int[] sourceColumns, int leftArity) {
        for (int i = 0; i < sourceColumns.length; i++) {
            int idx = sourceColumns[i];
            if (idx < leftArity) {
                copyColumn(left, idx, i);
            } else {
                copyColumn(right, idx - leftArity, i);
            }
        }
        return (T) this;
    }

    /**
     * Copy some left-most columns from source to this builder (slightly more efficient than using an array of columns).
     * @param source the source from which to copy
     * @param count the number of columns to copy, starting from the leftmost column (index 0)
     * @return a reference to this TensorBuilder (`this`)
     */
    // NOTE: The type of `source` should be ReadableTuple instead of TensorIterator, but that would be a binary
    // incompatible API change. So I won't do it now, but we should do it someday as part of a major release.
    default T copyLeftColumns(TensorIterator source, int count) {
        for (int i = 0; i < count; i++) {
            copyColumn(source, i, i);
        }
        return (T) this;
    }
}
