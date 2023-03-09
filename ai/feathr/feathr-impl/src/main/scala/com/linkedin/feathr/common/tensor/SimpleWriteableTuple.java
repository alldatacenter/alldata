package com.linkedin.feathr.common.tensor;

import com.linkedin.feathr.common.ColumnUtils;

import java.util.Arrays;

/**
 * An implementation of {@link WriteableTuple} which keeps all values in primitive arrays.
 * This can be used as a scratch space for a single row in a tensor.
 * @param <T>
 */
public class SimpleWriteableTuple<T extends WriteableTuple<T>> extends ColumnUtils implements WriteableTuple<T>, ReadableTuple {
    private static final int[] EMPTY_INT_ARRAY = new int[0];
    private static final long[] EMPTY_LONG_ARRAY = new long[0];
    private static final float[] EMPTY_FLOAT_ARRAY = new float[0];
    private static final double[] EMPTY_DOUBLE_ARRAY = new double[0];
    private static final boolean[] EMPTY_BOOLEAN_ARRAY = new boolean[0];
    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    private static final Object[] EMPTY_BYTE_ARRAY = new Object[0];

    private final int[] _intArray;
    private final long[] _longArray;
    private final float[] _floatArray;
    private final double[] _doubleArray;
    private final boolean[] _booleanArray;
    private final String[] _stringArray;
    private final Object[] _bytesArray;

    public SimpleWriteableTuple(Representable[] types) {
        super(types);
        _intArray = _intArity == 0 ? EMPTY_INT_ARRAY : new int[(_intArity)];
        _longArray = _longArity == 0 ? EMPTY_LONG_ARRAY : new long[(_longArity)];
        _stringArray = _stringArity == 0 ? EMPTY_STRING_ARRAY : new String[(_stringArity)];
        _floatArray = _floatArity == 0 ? EMPTY_FLOAT_ARRAY : new float[_floatArity];
        _doubleArray = _doubleArity == 0 ? EMPTY_DOUBLE_ARRAY : new double[_doubleArity];
        _booleanArray = _booleanArity == 0 ? EMPTY_BOOLEAN_ARRAY : new boolean[_booleanArity];
        _bytesArray = _bytesArity == 0 ? EMPTY_BYTE_ARRAY : new Object[_bytesArity];
    }

    SimpleWriteableTuple(SimpleWriteableTuple other) {
        super(other._columnTypes);
        _intArray = other._intArray == EMPTY_INT_ARRAY ? EMPTY_INT_ARRAY : other._intArray.clone();
        _longArray = other._longArray == EMPTY_LONG_ARRAY ? EMPTY_LONG_ARRAY : other._longArray.clone();
        _floatArray = other._floatArray == EMPTY_FLOAT_ARRAY ? EMPTY_FLOAT_ARRAY : other._floatArray.clone();
        _doubleArray = other._doubleArray == EMPTY_DOUBLE_ARRAY ? EMPTY_DOUBLE_ARRAY : other._doubleArray.clone();
        _stringArray = other._stringArray == EMPTY_STRING_ARRAY ? EMPTY_STRING_ARRAY : other._stringArray.clone();
        _booleanArray = other._booleanArray == EMPTY_BOOLEAN_ARRAY ? EMPTY_BOOLEAN_ARRAY : other._booleanArray.clone();
        _bytesArray = other._bytesArray == EMPTY_BYTE_ARRAY ? EMPTY_BYTE_ARRAY : other._bytesArray.clone();
    }

    @Override
    public Representable[] getTypes() {
        return _columnTypes;
    }

    @Override
    public int getInt(int column) {
        return _intArray[_columnIndex[column]];
    }

    @Override
    public long getLong(int column) {
        return _longArray[_columnIndex[column]];
    }

    @Override
    public float getFloat(int column) {
        return _floatArray[_columnIndex[column]];
    }

    @Override
    public double getDouble(int column) {
        return _doubleArray[_columnIndex[column]];
    }

    @Override
    public boolean getBoolean(int column) {
        return _booleanArray[_columnIndex[column]];
    }

    @Override
    public String getString(int column) {
        return _stringArray[_columnIndex[column]];
    }

    @Override
    public byte[] getBytes(int column) {
        return (byte[]) _bytesArray[_columnIndex[column]];
    }

    @Override
    public ReadableTuple getCopy() {
        return new SimpleWriteableTuple(this);
    }

    @Override
    public T setInt(int column, int value) {
        _intArray[_columnIndex[column]] = value;
        return (T) this;
    }

    @Override
    public T setLong(int column, long value) {
        _longArray[_columnIndex[column]] = value;
        return (T) this;
    }

    @Override
    public T setFloat(int column, float value) {
        _floatArray[_columnIndex[column]] = value;
        return (T) this;
    }

    @Override
    public T setDouble(int column, double value) {
        _doubleArray[_columnIndex[column]] = value;
        return (T) this;
    }

    @Override
    public T setBoolean(int column, boolean value) {
        _booleanArray[_columnIndex[column]] = value;
        return (T) this;
    }

    @Override
    public T setString(int column, String value) {
        _stringArray[_columnIndex[column]] = value;
        return (T) this;
    }

    @Override
    public T setBytes(int column, byte[] value) {
        _bytesArray[_columnIndex[column]] = value;
        return (T) this;
    }

    /**
     * Resets all values to 0 (for primitives) and null for Strings
     */
    public void resetColumns() {
        Arrays.fill(_intArray, 0);
        Arrays.fill(_longArray, 0L);
        Arrays.fill(_floatArray, 0F);
        Arrays.fill(_doubleArray, 0D);
        Arrays.fill(_booleanArray, false);
        Arrays.fill(_stringArray, null);
        Arrays.fill(_bytesArray, null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SimpleWriteableTuple<?> that = (SimpleWriteableTuple<?>) o;
        return Arrays.equals(_intArray, that._intArray) && Arrays.equals(_longArray, that._longArray) && Arrays.equals(
                _floatArray, that._floatArray) && Arrays.equals(_doubleArray, that._doubleArray) && Arrays.equals(_stringArray,
                that._stringArray) && Arrays.equals(_booleanArray, that._booleanArray) && Arrays.equals(_bytesArray, that._bytesArray);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(_intArray);
        result = 31 * result + Arrays.hashCode(_longArray);
        result = 31 * result + Arrays.hashCode(_floatArray);
        result = 31 * result + Arrays.hashCode(_doubleArray);
        result = 31 * result + Arrays.hashCode(_stringArray);
        result = 31 * result + Arrays.hashCode(_booleanArray);
        result = 31 * result + Arrays.hashCode(_bytesArray);
        return result;
    }

}
