package com.linkedin.feathr.common.tensorbuilder;

import com.linkedin.feathr.common.ColumnUtils;
import com.linkedin.feathr.common.tensor.Representable;
import com.linkedin.feathr.common.tensor.TensorData;
import com.linkedin.feathr.common.TensorUtils;

import java.util.Arrays;

/**
 * Builds tensors with columns being a requested permutation of int, long, and float.
 * Am instance of UniversalTensorBuilder can be reused by calling start method.
 */
public final class UniversalTensorBuilder extends ColumnUtils implements TensorBuilder<UniversalTensorBuilder> {
    private static final int GROWTH_FACTOR = 2;
    private static final int[] EMPTY_INT_ARRAY = new int[0];
    private static final long[] EMPTY_LONG_ARRAY = new long[0];
    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    private static final float[] EMPTY_FLOAT_ARRAY = new float[0];
    private static final double[] EMPTY_DOUBLE_ARRAY = new double[0];
    private static final boolean[] EMPTY_BOOLEAN_ARRAY = new boolean[0];
    private static final Object[] EMPTY_BYTES_ARRAY = new Object[0];

    // Universal Tensors may have a value arity greater than 1
    private final int _valueArity;

    private int[] _intArray = EMPTY_INT_ARRAY;
    private long[] _longArray = EMPTY_LONG_ARRAY;
    private String[] _stringArray = EMPTY_STRING_ARRAY;
    private float[] _floatArray = EMPTY_FLOAT_ARRAY;
    private double[] _doubleArray = EMPTY_DOUBLE_ARRAY;
    private boolean[] _booleanArray = EMPTY_BOOLEAN_ARRAY;
    private Object[] _bytesArray = EMPTY_BYTES_ARRAY;

    private int _intPosition;
    private int _longPosition;
    private int _stringPosition;
    private int _floatPosition;
    private int _doublePosition;
    private int _booleanPosition;
    private int _bytesPosition;

    public UniversalTensorBuilder(Representable... columnTypes) {
        super(columnTypes);
        // This constructor assumes the value arity is 1
        _valueArity = 1;
    }

    public UniversalTensorBuilder(int valueArity, Representable... columnTypes) {
        super(columnTypes);
        _valueArity = valueArity;
    }

    public UniversalTensorBuilder start(int estimatedRows) {
        if (_intArity != 0) {
            _intArray = new int[(estimatedRows * _intArity)];
            _intPosition = 0;
        }
        if (_longArity != 0) {
            _longArray = new long[(estimatedRows * _longArity)];
            _longPosition = 0;
        }
        if (_stringArity != 0) {
            _stringArray = new String[(estimatedRows * _stringArity)];
            _stringPosition = 0;
        }
        if (_floatArity != 0) {
            _floatArray = new float[estimatedRows * _floatArity];
            _floatPosition = 0;
        }
        if (_doubleArity != 0) {
            _doubleArray = new double[estimatedRows * _doubleArity];
            _doublePosition = 0;
        }
        if (_booleanArity != 0) {
            _booleanArray = new boolean[estimatedRows * _booleanArity];
            _booleanPosition = 0;
        }
        if (_bytesArity != 0) {
            _bytesArray = new Object[estimatedRows * _bytesArity];
            _bytesPosition = 0;
        }
        return this;
    }

    @Override
    public UniversalTensorBuilder setInt(int index, int value) {
        int intIndex = _intPosition + _columnIndex[index];
        if (intIndex >= _intArray.length) {
            _intArray = Arrays.copyOf(_intArray, getNewLength(intIndex, _intArray.length));
        }
        _intArray[intIndex] = value;
        return this;
    }


    @Override
    public UniversalTensorBuilder setLong(int index, long value) {
        int longIndex = _longPosition + _columnIndex[index];
        if (longIndex >= _longArray.length) {
            _longArray = Arrays.copyOf(_longArray, getNewLength(longIndex, _longArray.length));
        }
        _longArray[longIndex] = value;
        return this;
    }

    @Override
    public UniversalTensorBuilder setString(int index, String value) {
        int stringIndex = _stringPosition + _columnIndex[index];
        if (stringIndex >= _stringArray.length) {
            _stringArray = Arrays.copyOf(_stringArray, getNewLength(stringIndex, _stringArray.length));
        }
        _stringArray[stringIndex] = value;
        return this;
    }

    @Override
    public UniversalTensorBuilder setFloat(int index, float value) {
        int floatIndex = _floatPosition + _columnIndex[index];
        if (floatIndex >= _floatArray.length) {
            _floatArray = Arrays.copyOf(_floatArray, getNewLength(floatIndex, _floatArray.length));
        }
        _floatArray[floatIndex] = value;
        return this;
    }

    @Override
    public UniversalTensorBuilder setDouble(int index, double value) {
        int doubleIndex = _doublePosition + _columnIndex[index];
        if (doubleIndex >= _doubleArray.length) {
            _doubleArray = Arrays.copyOf(_doubleArray, getNewLength(doubleIndex, _doubleArray.length));
        }
        _doubleArray[doubleIndex] = value;
        return this;
    }

    @Override
    public UniversalTensorBuilder setBoolean(int index, boolean value) {
        int booleanIndex = _booleanPosition + _columnIndex[index];
        if (booleanIndex >= _booleanArray.length) {
            _booleanArray = Arrays.copyOf(_booleanArray, getNewLength(booleanIndex, _booleanArray.length));
        }
        _booleanArray[booleanIndex] = value;
        return this;
    }

    @Override
    public UniversalTensorBuilder setBytes(int index, byte[] value) {
        int bytesIndex = _bytesPosition + _columnIndex[index];
        if (bytesIndex >= _bytesArray.length) {
            _bytesArray = Arrays.copyOf(_bytesArray, getNewLength(bytesIndex, _bytesArray.length));
        }
        _bytesArray[bytesIndex] = value;
        return this;
    }

    @Override
    public UniversalTensorBuilder append() {
        _intPosition += _intArity;
        _longPosition += _longArity;
        _floatPosition += _floatArity;
        _stringPosition += _stringArity;
        _doublePosition += _doubleArity;
        _booleanPosition += _booleanArity;
        _bytesPosition += _bytesArity;
        return this;
    }

    @Override
    public Representable[] getTypes() {
        return _columnTypes;
    }

    @Override
    public TensorData build(boolean sort) {
        return new UniversalTensor(_intArity, _longArity, _stringArity, _floatArity, _doubleArity, _booleanArity, _bytesArity,
                _valueArity, _intArray, _longArray, _stringArray, _floatArray, _doubleArray, _booleanArray, _bytesArray, _columnTypes,
                TensorUtils.safeRatio(
                        _floatPosition + _doublePosition + _intPosition + _longPosition + _stringPosition + _booleanPosition + _bytesPosition,
                        _floatArity + _doubleArity + _intArity + _longArity + _stringArity + _booleanArity + _bytesArity),
                sort, _columnIndex);
    }

    @Override
    public Representable[] getOutputTypes() {
        return _columnTypes;
    }

    private static int getNewLength(int index, int curLen) {
        int estNewLen = GROWTH_FACTOR * curLen;
        return index >= estNewLen ? index + 1 : estNewLen;
    }
}
