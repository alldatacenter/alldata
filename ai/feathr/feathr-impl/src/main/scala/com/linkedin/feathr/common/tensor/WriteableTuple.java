package com.linkedin.feathr.common.tensor;

/**
 * A writable tuple of primitives.
 */
public interface WriteableTuple<T extends WriteableTuple<T>> {
    /**
     * @return the types of all dimensions and the value as a single array.
     */
    Representable[] getTypes();

    /**
     * Sets the specified column to a given `int`
     * @param column The column in the tuple to write to
     * @param value The int value to be written
     * @return A reference to this object (`this`)
     */
    T setInt(int column, int value);

    /**
     * Sets the specified column to a given `long`
     * @param column The column in the tuple to write to
     * @param value The long value to be written
     * @return A reference to this object (`this`)
     */
    T setLong(int column, long value);

    /**
     * Sets the specified column to a given `float`
     * @param column The column in the tuple to write to
     * @param value The float value to be written
     * @return A reference to this object (`this`)
     */
    T setFloat(int column, float value);

    /**
     * Sets the specified column to a given `double`
     * @param column The column in the tuple to write to
     * @param value The double value to be written
     * @return A reference to this object (`this`)
     */
    T setDouble(int column, double value);

    /**
     * Sets the specified column to a given `boolean`
     * @param column The column in the tuple to write to
     * @param value The double value to be written
     * @return A reference to this object (`this`)
     */
    T setBoolean(int column, boolean value);

    /**
     * Sets the specified column to a given `String`
     * @param column The column in the tuple to write to
     * @param value The String value to be written
     * @return A reference to this object (`this`)
     */
    T setString(int column, String value);

    /**
     * Sets the specified column to a given `byte array`
     * @param column The column in the tuple to write to
     * @param value The byte array value to be written
     * @return A reference to this object (`this`)
     */
    default T setBytes(int column, byte[] value) {
        throw new UnsupportedOperationException("No implementation by default, please implement. ");
    }

    default T setValue(int index, Object value) {
        getTypes()[index].getRepresentation().from(value, this, index);
        return (T) this;
    }
}
