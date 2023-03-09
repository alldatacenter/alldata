package com.linkedin.feathr.common;

import com.linkedin.feathr.common.tensor.Primitive;
import com.linkedin.feathr.common.tensor.Representable;
import com.linkedin.feathr.common.tensor.TensorData;
import com.linkedin.feathr.common.tensor.TensorIterator;

/**
 * Coerce types in any TensorData to the one requested by the client.
 */
public class CoercingTensorData implements TensorData {
    private final TensorData _inner;

    public CoercingTensorData(TensorData inner) {
        this._inner = inner;
    }

    @Override
    public Representable[] getTypes() {
        return _inner.getTypes();
    }

    @Override
    public int estimatedCardinality() {
        return _inner.estimatedCardinality();
    }

    @Override
    public int cardinality() {
        return _inner.cardinality();
    }

    @Override
    public TensorIterator iterator() {
        return new MyTensorIterator(_inner.iterator());
    }

    @Override
    public long[] getShape() {
        return _inner.getShape();
    }

    public TensorData getInner() {
        return _inner;
    }

    private final class MyTensorIterator implements TensorIterator {
        private final TensorIterator _innerIterator;

        private MyTensorIterator(TensorIterator innerIterator) {
            this._innerIterator = innerIterator;
        }

        @Override
        public TensorData getTensorData() {
            return CoercingTensorData.this;
        }

        @Override
        public void start() {
            _innerIterator.start();
        }

        @Override
        public boolean isValid() {
            return _innerIterator.isValid();
        }

        @Override
        public Object getValue(int index) {
            return _innerIterator.getValue(index);
        }

        private Primitive getRepresentation(int index) {
            return CoercingTensorData.this.getTypes()[index].getRepresentation();
        }

        @Override
        public int getInt(int index) {
            return getRepresentation(index).toInt(_innerIterator, index);
        }

        @Override
        public long getLong(int index) {
            return getRepresentation(index).toLong(_innerIterator, index);
        }

        @Override
        public float getFloat(int index) {
            return getRepresentation(index).toFloat(_innerIterator, index);
        }

        @Override
        public double getDouble(int index) {
            return getRepresentation(index).toDouble(_innerIterator, index);
        }

        @Override
        public String getString(int index) {
            return getRepresentation(index).toString(_innerIterator, index);
        }

        @Override
        public boolean getBoolean(int index) {
            return getRepresentation(index).toBoolean(_innerIterator, index);
        }

        @Override
        public byte[] getBytes(int index) {
            return getRepresentation(index).toBytes(_innerIterator, index);
        }

        @Override
        public void next() {
            _innerIterator.next();
        }

        @Override
        public TensorIterator getCopy() {
            return new MyTensorIterator(_innerIterator.getCopy());
        }
    }
}
