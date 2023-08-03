package com.linkedin.feathr.common;

import com.linkedin.feathr.common.tensor.ReadableTuple;
import com.linkedin.feathr.common.tensor.Representable;
import com.linkedin.feathr.common.tensor.TensorData;
import com.linkedin.feathr.common.tensor.TensorIterator;

import java.util.Arrays;

/**
 * Generate hash for a tensor based on column types and values stored in the tensor.
 * This hash should be same for all tensor instances with same column types and values
 * even if they are implemented differently under the hood.
 */
public class Hasher {
    public static final Hasher INSTANCE = new Hasher();

    /**
     * Get hash for tensorData optionally excluding value columns
     * @param tensorData input tensorData for hash is to be computed
     * @param dimOnly if true, only dimensions (non-value columns) are use in computation
     * @return hash for the tensor
     */
    public int apply(TensorData tensorData, boolean dimOnly) {
        if (tensorData == null) {
            return 0;
        }
        Representable[] types = tensorData.getTypes();
        int numCols = dimOnly ? ColumnUtils.getDimArity(types) : types.length;
        int hash = getTypeHash(types, numCols);
        TensorIterator iterator = tensorData.iterator();
        while (iterator.isValid()) {
            hash = 31 * hash + getHash(iterator, numCols);
            iterator.next();
        }
        return hash;
    }

    private int getTypeHash(Representable[] types, int numCols) {
        int hash = 0;
        for (int i = 0; i < numCols; i++) {
            hash = hash * 31 + types[i].getRepresentation().hashCode();
        }
        return hash;
    }

    public static int getHash(ReadableTuple readableTuple, int numCols) {
        int hash = 0;
        Representable[] types = readableTuple.getTypes();
        for (int i = 0; i < numCols; i++) {
            switch (types[i].getRepresentation()) {
                case INT:
                    hash = hash * 31 + Integer.hashCode(readableTuple.getInt(i));
                    break;
                case LONG:
                    hash = hash * 31 + Long.hashCode(readableTuple.getLong(i));
                    break;
                case DOUBLE:
                    hash = hash * 31 + Double.hashCode(readableTuple.getDouble(i));
                    break;
                case FLOAT:
                    hash = hash * 31 + Float.hashCode(readableTuple.getFloat(i));
                    break;
                case STRING:
                    hash = hash * 31 + readableTuple.getString(i).hashCode();
                    break;
                case BOOLEAN:
                    hash = hash * 31 + Boolean.hashCode(readableTuple.getBoolean(i));
                    break;
                case BYTES:
                    hash = hash * 31 + Arrays.hashCode(readableTuple.getBytes(i));
                    break;
                default:
                    throw new IllegalArgumentException("Cannot generate hash for column type: " + types[i].getRepresentation());
            }
        }
        return hash;
    }

}
