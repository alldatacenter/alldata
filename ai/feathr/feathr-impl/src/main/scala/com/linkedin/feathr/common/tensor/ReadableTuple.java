package com.linkedin.feathr.common.tensor;

/**
 * A readable tuple of primitives.
 */
public interface ReadableTuple {
    Representable[] getTypes();

    /**
     * @return an int at given column.
     */
    int getInt(int column);

    /**
     * @return a long at given column.
     */
    long getLong(int column);

    /**
     * @return a float at given column.
     */
    float getFloat(int column);

    /**
     * @return a double at given column.
     */
    double getDouble(int column);

    /**
     * @return a boolean at given column.
     */
    boolean getBoolean(int column);

    /**
     * @return a string at given column.
     */
    String getString(int column);

    /**
     * @return a byte array at given column.
     */
    byte[] getBytes(int column);

    /**
     * @return a copy of this tuple, so that changes to the original tuple will not affect the new one.
     */
    ReadableTuple getCopy();
}