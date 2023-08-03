package com.linkedin.feathr.common.tensor.scalar;

import com.linkedin.feathr.common.tensor.Primitive;
import com.linkedin.feathr.common.tensor.TensorData;
import com.linkedin.feathr.common.tensor.TensorIterator;

/**
 * A base for all wrappers of scalars.
 * Has arity of 1: 0 dimensions and 1 value. Cardinality is always 1.
 */
public abstract class ScalarTensor implements TensorData {

    // scalars have 0 dimensionality and return an empty array as a result
    private static final long[] SHAPE = new long[0];

    public static ScalarTensor wrap(Object scalar, Primitive type) {
        switch (type) {
            case INT:
                return new ScalarIntTensor(((Number)scalar).intValue());
            case LONG:
                return new ScalarLongTensor(((Number)scalar).longValue());
            case FLOAT:
                return new ScalarFloatTensor(((Number)scalar).floatValue());
            case DOUBLE:
                return new ScalarDoubleTensor(((Number)scalar).doubleValue());
            case STRING:
                /*
                 * Subclasses of CharSequence (e.g. org.apache.avro.util.Utf8) need to be supported in here because
                 * Java deserialization of avro GenericRecord returns Utf8 by default. Thus, toString() is used.
                 */
                return new ScalarStringTensor(scalar.toString());
            case BYTES:
                return new ScalarBytesTensor((byte[]) scalar);
            case BOOLEAN:
                return new ScalarBooleanTensor((Boolean) scalar);
            default:
                throw new IllegalArgumentException("The primitive type " + type + " is not supported.");
        }
    }

    public static ScalarTensor wrap(Object scalar) {
        if (scalar instanceof Integer) {
            return new ScalarIntTensor((Integer) scalar);
        }
        if (scalar instanceof Long) {
            return new ScalarLongTensor((Long) scalar);
        }
        if (scalar instanceof Float) {
            return new ScalarFloatTensor((Float) scalar);
        }
        if (scalar instanceof Double) {
            return new ScalarDoubleTensor((Double) scalar);
        }
        if (scalar instanceof CharSequence) {
            return new ScalarStringTensor(scalar.toString());
        }
        if (scalar instanceof Boolean) {
            return new ScalarBooleanTensor((Boolean) scalar);
        }
        if (scalar instanceof byte[]) {
            return new ScalarBytesTensor((byte[]) scalar);
        }
        throw new IllegalArgumentException("The primitive type of " + scalar + " is not supported.");
    }

    protected final Primitive getType() {
        return (Primitive) getTypes()[0];
    }

    @Override
    public int estimatedCardinality() {
        return 1;
    }

    @Override
    public int cardinality() {
        return 1;
    }

    @Override
    public long[] getShape() {
        return SHAPE;
    }

    abstract class BaseIterator implements TensorIterator {
        int _i;

        BaseIterator(int i) {
            this._i = i;
        }

        @Override
        public TensorData getTensorData() {
            return ScalarTensor.this;
        }

        @Override
        public void start() {
            _i = 0;
        }

        @Override
        public boolean isValid() {
            return _i == 0;
        }

        // The following methods are coercers, provided only as a fallback.
        @Override
        public int getInt(int column) {
            return getType().toInt(this, column);
        }

        @Override
        public long getLong(int column) {
            return getType().toLong(this, column);
        }

        @Override
        public float getFloat(int column) {
            return getType().toFloat(this, column);
        }

        @Override
        public double getDouble(int column) {
            return getType().toDouble(this, column);
        }

        @Override
        public boolean getBoolean(int column) {
            return getType().toBoolean(this, column);
        }

        @Override
        public String getString(int column) {
            return getType().toString(this, column);
        }

        @Override
        public byte[] getBytes(int column) {
            return getType().toBytes(this, column);
        }

        @Override
        public void next() {
            _i++;
        }

        protected void checkColumn(int column) {
            if (column > 0) {
                throw new IllegalArgumentException("The tensor only supports column 0.");
            }
        }
    }
}
