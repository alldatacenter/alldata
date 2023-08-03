package com.linkedin.feathr.common.tensor;

import com.linkedin.feathr.common.ColumnUtils;
import com.linkedin.feathr.common.Equal;
import com.linkedin.feathr.common.Hasher;

import java.util.Arrays;

/**
 *  A readable tuple implementation that stores single tensor row data independent of
 *  a tensor aka in standalone manner.
 *  This implements equality and hashcode methods so it can be used as a key in maps
 */
public class StandaloneReadableTuple implements ReadableTuple {
    //Stores representation of each column
    private final Representable[] _columnTypes;
    //Stores per type offset for each column
    private final int[] _columnIndex;
    //Stores all int dims
    private final int[] _ints;
    //Stores all long dims
    private final long[] _longs;
    //Stores all string dims
    private final String[] _strings;
    //Stores all float vals
    private final float[] _floats;
    //Stores all boolean vals
    private final boolean[] _booleans;
    //Stores all boolean vals
    private final Object[] _byteArrays;
    //Used for caching hash
    private int _hash = 0;
    /**
     * Copies StandaloneReadableTuple. It shares all the underlying data.
     * @param standaloneReadableTuple StandaloneReadableTuple to make copy of
     */
    public StandaloneReadableTuple(StandaloneReadableTuple standaloneReadableTuple) {
        _columnTypes = standaloneReadableTuple._columnTypes;
        _columnIndex = standaloneReadableTuple._columnIndex;
        _ints = standaloneReadableTuple._ints;
        _longs = standaloneReadableTuple._longs;
        _strings = standaloneReadableTuple._strings;
        _floats = standaloneReadableTuple._floats;
        _booleans = standaloneReadableTuple._booleans;
        _byteArrays = standaloneReadableTuple._byteArrays;
        _hash = standaloneReadableTuple._hash;
    }

    public StandaloneReadableTuple(ReadableTuple readableTuple) {
        this(readableTuple, false);
    }

    /**
     * Creates a StandaloneReadableTuple from a ReadableTuple optionally by dropping
     * last value (float/double) columns.
     * Note: this copies all the values from readableTuple so doesn't depend on underlying tensor, if any.
     * @param readableTuple ReadableTuple to convert into StandaloneReadableTuple
     * @param dimOnly value column(s) won't be copied if true, all columns will be copied otherwise.
     */
    public StandaloneReadableTuple(ReadableTuple readableTuple, boolean dimOnly) {
        ColumnUtils columnUtils = new ColumnUtils(readableTuple.getTypes());
        Representable[] columnTypes = columnUtils.getColumnTypes();
        int[] columnIndex = columnUtils.getColumnIndex();

        /*
         * We assume that all "values" are trailing float/double typed columns, so when user wants us to
         * create a StandaloneReadableTuple which is "dimension only", we only copy columns skipping those.
         * Note: For other value types (LONG, BOOLEAN, INT, STRING), it will assume there is only one value column
         */
        int numColumnsToCopy = dimOnly
                ? ColumnUtils.getDimArity(columnTypes)
                : columnTypes.length;
        _columnTypes = columnTypes.length == numColumnsToCopy ? columnTypes : Arrays.copyOf(columnTypes, numColumnsToCopy);
        _columnIndex = columnTypes.length == numColumnsToCopy ? columnIndex : Arrays.copyOf(columnIndex, numColumnsToCopy);
        _ints = new int[columnUtils.getIntArity()];
        _longs = new long[columnUtils.getLongArity()];
        _strings = new String[columnUtils.getStringArity()];
        _floats = new float[columnUtils.getFloatArity()];
        _booleans = new boolean[columnUtils.getBooleanArity()];
        _byteArrays = new Object[columnUtils.getBytesArity()];
        copyData(readableTuple, numColumnsToCopy);
    }

    private void copyData(ReadableTuple readableTuple, int dimArity) {
        int intCount = 0;
        int longCount = 0;
        int stringCount = 0;
        int floatCount = 0;
        int booleanCount = 0;
        int byteArrayCount = 0;
        for (int j = 0; j < dimArity; j++) {
            switch (_columnTypes[j].getRepresentation()) {
                case INT:
                    _ints[intCount] = readableTuple.getInt(j);
                    intCount++;
                    break;
                case LONG:
                    _longs[longCount] = readableTuple.getLong(j);
                    longCount++;
                    break;
                case STRING:
                    _strings[stringCount] = readableTuple.getString(j);
                    stringCount++;
                    break;
                case FLOAT:
                    _floats[floatCount] = readableTuple.getFloat(j);
                    floatCount++;
                    break;
                case BOOLEAN:
                    _booleans[booleanCount] = readableTuple.getBoolean(j);
                    booleanCount++;
                    break;
                case BYTES:
                    _byteArrays[byteArrayCount] = readableTuple.getBytes(j);
                    byteArrayCount++;
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported column type:" + _columnTypes[j].getRepresentation());
            }
        }
    }

    @Override
    public Representable[] getTypes() {
        return _columnTypes;
    }

    @Override
    public int getInt(int column) {
        return _ints[_columnIndex[column]];
    }

    @Override
    public long getLong(int column) {
        return _longs[_columnIndex[column]];
    }

    @Override
    public float getFloat(int column) {
        return _floats[_columnIndex[column]];
    }

    @Override
    public double getDouble(int column) {
        throw new UnsupportedOperationException("StandaloneReadableTuple doesn't support doubles");
    }

    @Override
    public boolean getBoolean(int column) {
        return _booleans[_columnIndex[column]];
    }

    @Override
    public String getString(int column) {
        return _strings[_columnIndex[column]];
    }

    @Override
    public byte[] getBytes(int column) {
        return (byte[]) _byteArrays[_columnIndex[column]];
    }

    @Override
    public ReadableTuple getCopy() {
        return new StandaloneReadableTuple(this);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReadableTuple)) {
            return false;
        }
        if (o instanceof StandaloneReadableTuple) {
            StandaloneReadableTuple that = (StandaloneReadableTuple) o;
            return Arrays.equals(_columnTypes, that._columnTypes) && Arrays.equals(_columnIndex, that._columnIndex)
                    && Arrays.equals(_ints, that._ints) && Arrays.equals(_longs, that._longs) && Arrays.equals(_strings,
                    that._strings) && Arrays.equals(_floats, that._floats) && Arrays.equals(_booleans, that._booleans);
        } else {
            ReadableTuple readableTuple = (ReadableTuple) o;
            return Arrays.equals(_columnTypes, readableTuple.getTypes())
                    && Equal.tupleEquals(this, readableTuple, _columnTypes, _columnTypes.length);
        }
    }

    @Override
    public int hashCode() {
        if (_hash == 0) {
            _hash =  Hasher.getHash(this, _columnTypes.length);
        }
        return _hash;
    }
}
