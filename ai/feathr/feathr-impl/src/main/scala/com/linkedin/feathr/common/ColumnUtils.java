package com.linkedin.feathr.common;


import com.linkedin.feathr.common.tensor.Primitive;
import com.linkedin.feathr.common.tensor.Representable;

import java.util.Objects;

/**
 * Utility function to build internal flat array representation based on the types of the input.
 */
public class ColumnUtils {
    public final int[] _columnIndex;
    public int _intArity;
    public int _longArity;
    public int _stringArity;
    public int _floatArity;
    public int _doubleArity;
    public int _booleanArity;
    public int _bytesArity;

    public final Representable[] _columnTypes;

    public ColumnUtils(Representable... columnTypes) {
        this._columnTypes = Objects.requireNonNull(columnTypes, "columnTypes should not be null in UniversalTensorBuilder.");
        _columnIndex = new int[columnTypes.length];
        for (int i = 0; i < columnTypes.length; i++) {
            switch (columnTypes[i].getRepresentation()) {
                case INT:
                    _columnIndex[i] = _intArity;
                    _intArity++;
                    break;
                case LONG:
                    _columnIndex[i] = _longArity;
                    _longArity++;
                    break;
                case FLOAT:
                    _columnIndex[i] = _floatArity;
                    _floatArity++;
                    break;
                case STRING:
                    _columnIndex[i] = _stringArity;
                    _stringArity++;
                    break;
                case DOUBLE:
                    _columnIndex[i] = _doubleArity;
                    _doubleArity++;
                    break;
                case BOOLEAN:
                    _columnIndex[i] = _booleanArity;
                    _booleanArity++;
                    break;
                case BYTES:
                    _columnIndex[i] = _bytesArity;
                    _bytesArity++;
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported column type: " + columnTypes[i].getRepresentation());
            }
        }
    }

    public int[] getColumnIndex() {
        return _columnIndex;
    }

    public int getIntArity() {
        return _intArity;
    }

    public int getLongArity() {
        return _longArity;
    }

    public int getStringArity() {
        return _stringArity;
    }

    public int getFloatArity() {
        return _floatArity;
    }

    public int getDoubleArity() {
        return _doubleArity;
    }

    public int getBooleanArity() {
        return _booleanArity;
    }

    public int getBytesArity() {
        return _bytesArity;
    }

    public Representable[] getColumnTypes() {
        return _columnTypes;
    }

    /**
     * Compute the number of dimension (non-value) columns
     * This assumes that all value columns are at end and
     * are of type FLOAT or DOUBLE.
     * For other value types, assumes only one value column
     * @param types column types
     * @return number of dimension(non-value) columns. For non float/double columns, returns types.length - 1
     */
    public static int getDimArity(Representable[] types) {
        int i = types.length;
        Primitive p = types[types.length - 1].getRepresentation();
        if (p.getRepresentation() != Primitive.FLOAT && p.getRepresentation() != Primitive.DOUBLE) {
            // Value Type is not a float/double. Assume number of values columns is one
            return types.length - 1;
        }
        while (i > 0) {
            p = types[i - 1].getRepresentation();
            if (p == Primitive.FLOAT || p == Primitive.DOUBLE) {
                i--;
            } else {
                break;
            }
        }
        return i;
    }
}
