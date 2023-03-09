package com.linkedin.feathr.common.tensorbuilder;

import com.linkedin.feathr.common.*;
import com.linkedin.feathr.common.tensor.Primitive;
import com.linkedin.feathr.common.tensor.Representable;
import com.linkedin.feathr.common.tensor.TensorData;
import com.linkedin.feathr.common.tensor.TensorIterator;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;


/**
 * Implements a tensor from pre-allocated arrays.
 *
 * The keys (int and long) and the values are stored in parallel arrays to float values.
 * Multiple keys of a certain type can be associated to a value, in that case the key
 * array size will be a multiple of the value array size.
 *
 */
public class UniversalTensor implements TensorData {
    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    private static final double[] EMPTY_DOUBLE_ARRAY = new double[0];
    private static final float[] EMPTY_FLOAT_ARRAY = new float[0];
    private static final boolean[] EMPTY_BOOLEAN_ARRAY = new boolean[0];
    private static final Object[] EMPTY_BYTES_ARRAY = new Object[0];

    // Maps column index to "global" array index.
    private final int[] _columnIndex;
    private final Representable[] _columnTypes;
    private final int[] _intArray;
    private final int _intArity;
    private final long[] _longArray;
    private final int _longArity;
    private final String[] _stringArray;
    private final int _stringArity;
    private final float[] _floatArray;
    private final int _floatArity;
    private final double[] _doubleArray;
    private final int _doubleArity;
    private final boolean[] _booleanArray;
    private final int _booleanArity;
    private final int _size;
    private final Object[] _bytesArray;
    private final int _bytesArity;
    private final Primitive _valueType;
    private final int _valueArity;

    /**
     * @deprecated Use {@link UniversalTensorBuilder}
     * */
    @Deprecated
    public UniversalTensor(int intArity, int longArity, int floatArity, int[] intArray, long[] longArray,
                           float[] floatArray, Representable[] columnTypes, int... columnIndex) {
        this(intArity, longArity, floatArity, intArray, longArray, floatArray, columnTypes, false, columnIndex);
    }

    /**
     * @deprecated Use {@link UniversalTensorBuilder}
     * */
    @Deprecated
    public UniversalTensor(int intArity, int longArity, int floatArity, int[] intArray, long[] longArray,
                           float[] floatArray, Representable[] columnTypes, int size, boolean sort, int... columnIndex) {
        this(intArity, longArity, 0, floatArity, 0, intArray, longArray, EMPTY_STRING_ARRAY, floatArray, EMPTY_DOUBLE_ARRAY,
                columnTypes, size, sort, columnIndex);
    }

    /**
     * @deprecated Use {@link UniversalTensorBuilder}
     * */
    @Deprecated
    public UniversalTensor(int intArity, int longArity, int floatArity, int[] intArray, long[] longArray,
                           float[] floatArray, Representable[] columnTypes, boolean sort, int... columnIndex) {
        this(intArity, longArity, 0, floatArity, intArray, longArray, EMPTY_STRING_ARRAY, floatArray, columnTypes, sort,
                columnIndex);
    }

    /**
     * @deprecated Use {@link UniversalTensorBuilder}
     * */
    @Deprecated
    public UniversalTensor(int intArity, int longArity, int stringArity, int floatArity, int[] intArray, long[] longArray,
                           String[] stringArray, float[] floatArray, Representable[] columnTypes, int... columnIndex) {
        this(intArity, longArity, stringArity, floatArity, intArray, longArray, stringArray, floatArray, columnTypes, false,
                columnIndex);
    }

    /**
     * @deprecated Use {@link UniversalTensorBuilder}
     * */
    @Deprecated
    public UniversalTensor(int intArity, int longArity, int stringArity, int floatArity, int[] intArray, long[] longArray,
                           String[] stringArray, float[] floatArray, Representable[] columnTypes, boolean sort, int... columnIndex) {
        this(intArity, longArity, stringArity, floatArity, 0, intArray, longArray, stringArray, floatArray,
                EMPTY_DOUBLE_ARRAY, columnTypes, sort, columnIndex);
    }

    /**
     * @deprecated Use {@link UniversalTensorBuilder}
     * */
    @Deprecated
    public UniversalTensor(int intArity, int longArity, int stringArity, int floatArity, int doubleArity, int[] intArray,
                           long[] longArray, String[] stringArray, float[] floatArray, double[] doubleArray, Representable[] columnTypes,
                           boolean sort, int... columnIndex) {
        this(intArity, longArity, stringArity, floatArity, doubleArity, intArray, longArray, stringArray, floatArray,
                doubleArray, columnTypes, TensorUtils.safeRatio(
                        floatArray.length + doubleArray.length + intArray.length + longArray.length + stringArray.length,
                        floatArity + doubleArity + intArity + longArity + stringArity), sort, columnIndex);
    }

    /**
     * Generate arities and columns from types, assuming sequential ordering within types.
     * @deprecated Use {@link UniversalTensorBuilder}
     * */
    @Deprecated
    public UniversalTensor(int[] intArray, long[] longArray, float[] floatArray, Representable[] columnTypes) {
        this(intArray, longArray, EMPTY_STRING_ARRAY, floatArray, columnTypes);
    }

    /**
     * @deprecated Use {@link UniversalTensorBuilder}
     * */
    @Deprecated
    public UniversalTensor(int[] intArray, long[] longArray, String[] stringArray, float[] floatArray,
                           Representable[] columnTypes) {
        this(intArray, longArray, stringArray, floatArray, EMPTY_DOUBLE_ARRAY, columnTypes);
    }

    /**
     * @deprecated Use {@link UniversalTensorBuilder}
     * */
    @Deprecated
    public UniversalTensor(int[] intArray, long[] longArray, String[] stringArray, double[] doubleArray,
                           Representable[] columnTypes) {
        this(intArray, longArray, stringArray, EMPTY_FLOAT_ARRAY, doubleArray, columnTypes);
    }

    /**
     * @deprecated Use {@link UniversalTensorBuilder}
     * */
    @Deprecated
    public UniversalTensor(int[] intArray, long[] longArray, String[] stringArray, float[] floatArray,
                           double[] doubleArray, Representable[] columnTypes) {
        this(intArray, longArray, stringArray, floatArray, doubleArray, new ColumnUtils(columnTypes));
    }

    /**
     * @deprecated Use {@link UniversalTensorBuilder}
     * */
    @Deprecated
    public UniversalTensor(int[] intArray, long[] longArray, String[] stringArray, float[] floatArray,
                           double[] doubleArray, ColumnUtils utils) {
        this(utils._intArity, utils._longArity, utils._stringArity, utils._floatArity, utils._doubleArity, intArray, longArray,
                stringArray, floatArray, doubleArray, utils._columnTypes, true, utils._columnIndex);
    }

    /**
     * @deprecated Use {@link UniversalTensorBuilder}
     * */
    @Deprecated
    public UniversalTensor(int[] intArray, long[] longArray, String[] stringArray, float[] floatArray,
                           double[] doubleArray, Object[] bytesArray, ColumnUtils utils) {
        this(
                utils._intArity, utils._longArity, utils._stringArity, utils._floatArity,
                utils._doubleArity, 0, utils._bytesArity, utils._floatArity + utils._doubleArity,
                intArray, longArray, stringArray, floatArray, doubleArray, EMPTY_BOOLEAN_ARRAY, bytesArray,
                utils._columnTypes,
                TensorUtils.safeRatio(
                        floatArray.length + doubleArray.length + intArray.length + longArray.length + stringArray.length + bytesArray.length,
                        utils._intArity + utils._longArity + utils._stringArity + utils._floatArity + utils._doubleArity + utils._bytesArity), true, utils._columnIndex);
    }

    /**
     * @deprecated Use {@link UniversalTensorBuilder}
     * */
    @Deprecated
    public UniversalTensor(int intArity, int longArity, int stringArity, int floatArity, int doubleArity, int[] intArray,
                           long[] longArray, String[] stringArray, float[] floatArray, double[] doubleArray, Representable[] columnTypes,
                           int size, boolean sort, int... columnIndex) {
        // This constructor assumes the value is a float/double. Pass in floatArity + doubleArity for valueArity
        this(intArity, longArity, stringArity, floatArity, doubleArity, 0, 0, floatArity + doubleArity, intArray, longArray, stringArray, floatArray,
                doubleArray, EMPTY_BOOLEAN_ARRAY, EMPTY_BYTES_ARRAY, columnTypes, size, sort, columnIndex);
    }

    /**
     * Universal Tensor constructors should ideally not be used to create a UniversalTensor. Universal Tensors should be
     * created by using the UniversalTensorBuilder. The other constructors are deprecated as they only support
     * FLOAT/DOUBLE values.
     * @param intArity number of int columns in a tensor row (includes dimensions and values)
     * @param longArity number of long columns in a tensor row (includes dimensions and values)
     * @param stringArity number of string columns in a tensor row (includes dimensions and values)
     * @param floatArity number of float columns in a tensor row (includes dimensions and values)
     * @param doubleArity number of double columns in a tensor row (includes dimensions and values)
     * @param booleanArity number of boolean columns in a tensor row (includes dimensions and values)
     * @param valueArity number of value columns in a tensor row. This value is needed as it's possible for UniversalTensors
     *                   to have more than one value dimension
     * @param intArray array corresponding to the int dimensions/values
     * @param longArray array corresponding to the long dimensions/values
     * @param stringArray array corresponding to the string dimensions
     * @param floatArray array corresponding to the float values. Floats can not be a dimension
     * @param doubleArray array corresponding to the double values. Doubles can not be a dimension
     * @param booleanArray array corresponding to the boolean dimensions/values
     * @param columnTypes columnTypes representing the tensor
     * @param size size of the tensor. (Number of rows).
     * @param sort sorts the tensor by the dimension if true
     * @param columnIndex columnIndices
     */
    public UniversalTensor(int intArity, int longArity, int stringArity, int floatArity, int doubleArity,
                           int booleanArity, int valueArity, int[] intArray, long[] longArray, String[] stringArray, float[] floatArray,
                           double[] doubleArray, boolean[] booleanArray, Representable[] columnTypes, int size, boolean sort, int... columnIndex) {
        this(
                intArity, longArity, stringArity, floatArity, doubleArity, booleanArity, 0, valueArity,
                intArray, longArray, stringArray, floatArray, doubleArray, booleanArray, EMPTY_BYTES_ARRAY,
                columnTypes, size, sort, columnIndex
        );
    }

    /**
     * Universal Tensor constructors should ideally not be used to create a UniversalTensor. Universal Tensors should be
     * created by using the UniversalTensorBuilder. The other constructors are deprecated as they only support
     * FLOAT/DOUBLE values.
     * @param intArity number of int columns in a tensor row (includes dimensions and values)
     * @param longArity number of long columns in a tensor row (includes dimensions and values)
     * @param stringArity number of string columns in a tensor row (includes dimensions and values)
     * @param floatArity number of float columns in a tensor row (includes dimensions and values)
     * @param doubleArity number of double columns in a tensor row (includes dimensions and values)
     * @param booleanArity number of boolean columns in a tensor row (includes dimensions and values)
     * @param bytesArity number of bytes columns in a tensor row (includes dimensions and values)
     * @param valueArity number of value columns in a tensor row. This value is needed as it's possible for UniversalTensors
     *                   to have more than one value dimension
     * @param intArray array corresponding to the int dimensions/values
     * @param longArray array corresponding to the long dimensions/values
     * @param stringArray array corresponding to the string dimensions
     * @param floatArray array corresponding to the float values. Floats can not be a dimension
     * @param doubleArray array corresponding to the double values. Doubles can not be a dimension
     * @param booleanArray array corresponding to the boolean dimensions/values
     * @param bytesArray array corresponding to the bytes dimensions/values
     * @param columnTypes columnTypes representing the tensor
     * @param size size of the tensor. (Number of rows).
     * @param sort sorts the tensor by the dimension if true
     * @param columnIndex columnIndices
     */
    public UniversalTensor(int intArity, int longArity, int stringArity, int floatArity, int doubleArity,
                           int booleanArity, int bytesArity, int valueArity, int[] intArray, long[] longArray, String[] stringArray, float[] floatArray,
                           double[] doubleArray, boolean[] booleanArray, Object[] bytesArray, Representable[] columnTypes, int size, boolean sort, int... columnIndex) {
        if (floatArity > 0 && doubleArity > 0) {
            throw new IllegalArgumentException("Cannot have both float values and double values.");
        }
        int arity = intArity + longArity + stringArity + floatArity + doubleArity + booleanArity + bytesArity;
        if (arity != columnIndex.length) {
            throw new IllegalArgumentException("Arities conflict with columnIndex");
        }
        if (columnTypes.length != columnIndex.length) {
            throw new IllegalArgumentException("ColumnTypes conflict with columnIndex");
        }
        if (columnTypes.length == 0) {
            throw new IllegalArgumentException("ColumnTypes length cannot be 0");
        }

        Primitive valueType = columnTypes[columnTypes.length - 1].getRepresentation();
        if (!valueType.canBeValue()) {
            throw new IllegalArgumentException("Cannot support value column of type: "
                    + valueType.getRepresentation());
        }
        _valueType = valueType;

        // Make sure float dimensions are at the right-most columns
        for (int i = 1; i <= floatArity + doubleArity; ++i) {
            Primitive representation = columnTypes[arity - i].getRepresentation();
            if (representation != Primitive.FLOAT && representation != Primitive.DOUBLE) {
                throw new IllegalArgumentException("FLOAT/DOUBLE dimensions should be at the right-most columns.");
            }
        }

        // Make sure all the values are the same type
        for (int i = arity - 1; i > arity - valueArity; i--) {
            Primitive representation = columnTypes[i].getRepresentation();
            if (representation != _valueType.getRepresentation()) {
                throw new IllegalArgumentException(String.format("Value dimensions should all be the same time."
                        + "Expected: %s but found: %s", _valueType.getRepresentation(), representation));
            }
        }
        _intArity = intArity;
        _longArity = longArity;
        _floatArity = floatArity;
        _stringArity = stringArity;
        _doubleArity = doubleArity;
        _booleanArity = booleanArity;
        _bytesArity = bytesArity;
        _valueArity = valueArity;
        _intArray = Objects.requireNonNull(intArray, "intArray cannot be null in UniversalTensor");
        _longArray = Objects.requireNonNull(longArray, "longArray cannot be null in UniversalTensor");
        _stringArray = Objects.requireNonNull(stringArray, "stringArray cannot be null in UniversalTensor.");
        _floatArray = Objects.requireNonNull(floatArray, "floatArray cannot be null in UniversalTensor.");
        _doubleArray = Objects.requireNonNull(doubleArray, "doubleArray cannot be null in UniversalTensor.");
        _booleanArray = Objects.requireNonNull(booleanArray, "booleanArray cannot be null in the UniversalTensor.");
        _bytesArray = Objects.requireNonNull(bytesArray, "bytesArray cannot be null in the UniversalTensor.");
        _columnTypes = Objects.requireNonNull(columnTypes, "columnTypes cannot be null in UniversalTensor.");
        _columnIndex = Objects.requireNonNull(columnIndex, "columnIndex cannot be null in UniversalTensor.");
        _size = size;
        int stringSize = size * stringArity;
        for (int i = 0; i < stringSize; ++i) {
            if (_stringArray[i] == null) {
                throw new IllegalArgumentException("String index is null at position: " + i);
            }
        }
        if (sort && cardinality() > 1) {
            throw new IllegalArgumentException("Tensor sorting is not supported.");
        }
    }

    class MyIterator implements TensorIterator {
        private int _rowIndex;
        private int _intPosition;
        private int _longPosition;
        private int _stringPosition;
        private int _floatPosition;
        private int _doublePosition;
        private int _booleanPosition;
        private int _bytesPosition;

        MyIterator(MyIterator other) {
            this._rowIndex = other._rowIndex;
            this._intPosition = other._intPosition;
            this._longPosition = other._longPosition;
            this._stringPosition = other._stringPosition;
            this._floatPosition = other._floatPosition;
            this._doublePosition = other._doublePosition;
            this._booleanPosition = other._booleanPosition;
            this._bytesPosition = other._bytesPosition;
        }

        MyIterator() {
        }

        void setIndex(int index) {
            _rowIndex = index;
            _intPosition = index * _intArity;
            _longPosition = index * _longArity;
            _stringPosition = index * _stringArity;
            _floatPosition = index * _floatArity;
            _doublePosition = index * _doubleArity;
            _booleanPosition = index * _booleanArity;
            _bytesPosition = index * _bytesPosition;
        }

        int getIndex() {
            return _rowIndex;
        }

        @Override
        public TensorData getTensorData() {
            return UniversalTensor.this;
        }

        @Override
        public void start() {
            _rowIndex = 0;
            _intPosition = 0;
            _longPosition = 0;
            _stringPosition = 0;
            _floatPosition = 0;
            _doublePosition = 0;
            _booleanPosition = 0;
            _bytesPosition = 0;
        }

        @Override
        public boolean isValid() {
            return _rowIndex < _size;
        }

        @Override
        public Object getValue(int index) {
            int i = _columnIndex[index];
            switch (_columnTypes[index].getRepresentation()) {
                case INT:
                    return _intArray[_intPosition + i];
                case LONG:
                    return _longArray[_longPosition + i];
                case FLOAT:
                    return _floatArray[_floatPosition + i];
                case STRING:
                    return _stringArray[_stringPosition + i];
                case DOUBLE:
                    return _doubleArray[_doublePosition + i];
                case BOOLEAN:
                    return _booleanArray[_booleanPosition + i];
                case BYTES:
                    return _bytesArray[_bytesPosition + i];
                default:
                    throw new IllegalArgumentException("Cannot get value for unknown column type" + _columnTypes[index]);
            }
        }

        @Override
        public int getInt(int index) {
            return _intArray[_intPosition + _columnIndex[index]];
        }

        @Override
        public long getLong(int index) {
            return _longArray[_longPosition + _columnIndex[index]];
        }

        @Override
        public String getString(int index) {
            return _stringArray[_stringPosition + _columnIndex[index]];
        }

        @Override
        public float getFloat(int index) {
            return _floatArray[_floatPosition + _columnIndex[index]];
        }

        @Override
        public double getDouble(int index) {
            return _doubleArray[_doublePosition + _columnIndex[index]];
        }

        @Override
        public boolean getBoolean(int index) {
            return _booleanArray[_booleanPosition + _columnIndex[index]];
        }

        @Override
        public byte[] getBytes(int index) {
            return (byte[]) _bytesArray[_bytesPosition + _columnIndex[index]];
        }

        @Override
        public void next() {
            _rowIndex++;
            _intPosition += _intArity;
            _longPosition += _longArity;
            _stringPosition += _stringArity;
            _floatPosition += _floatArity;
            _doublePosition += _doubleArity;
            _booleanPosition += _booleanArity;
            _bytesPosition += _bytesArity;
        }

        @Override
        public TensorIterator getCopy() {
            return new MyIterator(this);
        }
    }

    @Override
    public int getArity() {
        return _intArity + _longArity + _stringArity + _floatArity + _doubleArity + _booleanArity + _bytesArity;
    }

    @Override
    public Representable[] getTypes() {
        return _columnTypes;
    }

    @Override
    public int estimatedCardinality() {
        return cardinality();
    }

    @Override
    public int cardinality() {
        return _size;
    }

    @Override
    public boolean isEmpty() {
        return _size == 0;
    }

    @Override
    public TensorIterator iterator() {
        return new MyIterator();
    }

    public int[] getIntArray() {
        return _intArray;
    }

    public int getIntArity() {
        return _intArity;
    }

    public long[] getLongArray() {
        return _longArray;
    }

    public int getLongArity() {
        return _longArity;
    }

    public String[] getStringArray() {
        return _stringArray;
    }

    public int getStringArity() {
        return _stringArity;
    }

    public float[] getFloatArray() {
        return _floatArray;
    }

    public int getFloatArity() {
        return _floatArity;
    }

    public double[] getDoubleArray() {
        return _doubleArray;
    }

    public int getDoubleArity() {
        return _doubleArity;
    }

    public boolean[] getBooleanArray() {
        return _booleanArray;
    }

    public int getBooleanArity() {
        return _booleanArity;
    }

    public Object[] getBytesArray() {
        return _bytesArray;
    }

    public int getBytesArity() {
        return _bytesArity;
    }

    public int getValueArity() {
        return _valueArity;
    }

    public int[] getColumnIndex() {
        return _columnIndex;
    }

    class TensorIndexComparator implements Comparator<Integer> {
        private final MyIterator _leftIter = new MyIterator();
        private final MyIterator _rightIter = new MyIterator();
        private final Primitive[] _primitiveTypes = new Primitive[_columnTypes.length];

        TensorIndexComparator() {
            for (int c = 0; c < _columnTypes.length; c++) {
                _primitiveTypes[c] = _columnTypes[c].getRepresentation();
            }
        }

        @Override
        public int compare(Integer o1, Integer o2) {
            _leftIter.setIndex(o1);
            _rightIter.setIndex(o2);
            int result;
            int maxColLen = _primitiveTypes.length - _valueArity;
            for (int c = 0; c < maxColLen; c++) {
                result = _primitiveTypes[c].compare(_leftIter, c, _rightIter, c);
                if (result != 0) {
                    return result;
                }
            }
            return 0;
        }
    }

    private void reorderArrays(Integer[] order) {
        int[] invOrder = new int[cardinality()];
        for (int i = 0; i < order.length; i++) {
            invOrder[order[i]] = i;
        }
        int[] tmpInts = new int[_intArity];
        long[] tmpLongs = new long[_longArity];
        boolean[] tmpBooleans = new boolean[_booleanArity];
        String[] tmpStrings = new String[_stringArity];
        float[] floatBuffer = new float[_floatArity];
        double[] doubleBuffer = new double[_doubleArity];

        for (int dest = 0; dest < order.length; dest++) {
            final int src = order[dest];
            if (src == dest) {
                continue;
            }
            switch (_intArity) {
                case 0:
                    break;
                case 1:
                    int tmp = _intArray[dest];
                    _intArray[dest] = _intArray[src];
                    _intArray[src] = tmp;
                    break;
                default:
                    System.arraycopy(_intArray, dest * _intArity, tmpInts, 0, _intArity);
                    System.arraycopy(_intArray, src * _intArity, _intArray, dest * _intArity, _intArity);
                    System.arraycopy(tmpInts, 0, _intArray, src * _intArity, _intArity);
                    break;
            }

            switch (_longArity) {
                case 0:
                    break;
                case 1:
                    long tmp = _longArray[dest];
                    _longArray[dest] = _longArray[src];
                    _longArray[src] = tmp;
                    break;
                default:
                    System.arraycopy(_longArray, dest * _longArity, tmpLongs, 0, _longArity);
                    System.arraycopy(_longArray, src * _longArity, _longArray, dest * _longArity, _longArity);
                    System.arraycopy(tmpLongs, 0, _longArray, src * _longArity, _longArity);
                    break;
            }

            switch (_stringArity) {
                case 0:
                    break;
                case 1:
                    String tmp = _stringArray[dest];
                    _stringArray[dest] = _stringArray[src];
                    _stringArray[src] = tmp;
                    break;
                default:
                    System.arraycopy(_stringArray, dest * _stringArity, tmpStrings, 0, _stringArity);
                    System.arraycopy(_stringArray, src * _stringArity, _stringArray, dest * _stringArity, _stringArity);
                    System.arraycopy(tmpStrings, 0, _stringArray, src * _stringArity, _stringArity);
                    break;
            }

            switch (_booleanArity) {
                case 0:
                    break;
                case 1:
                    boolean tmp = _booleanArray[dest];
                    _booleanArray[dest] = _booleanArray[src];
                    _booleanArray[src] = tmp;
                    break;
                default:
                    System.arraycopy(_booleanArray, dest * _booleanArity, tmpBooleans, 0, _booleanArity);
                    System.arraycopy(_booleanArray, src * _booleanArity, _booleanArray, dest * _booleanArity, _booleanArity);
                    System.arraycopy(tmpBooleans, 0, _booleanArray, src * _booleanArity, _booleanArity);
                    break;
            }

            if (_valueType == Primitive.FLOAT) {
                SortUtils.swapFloat(_floatArity, _floatArray, src, dest, floatBuffer);
            } else {
                SortUtils.swapDouble(_doubleArity, _doubleArray, src, dest, doubleBuffer);
            }

            int whereDestHadToGo = invOrder[dest];
            order[whereDestHadToGo] =  src;
            invOrder[src] = whereDestHadToGo;
        }
    }
}
