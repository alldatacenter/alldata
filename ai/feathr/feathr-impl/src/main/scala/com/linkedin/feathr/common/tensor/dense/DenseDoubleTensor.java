package com.linkedin.feathr.common.tensor.dense;

import com.google.common.primitives.Doubles;
import com.linkedin.feathr.common.tensor.Representable;
import com.linkedin.feathr.common.tensor.TensorIterator;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.util.List;

import static com.linkedin.feathr.common.tensor.Primitive.DOUBLE;

/**
 * Dense tensor which can be converted from/to a tensorflow tensor object.
 * The underlying data is backed up by DoubleBuffer.
 */
public class DenseDoubleTensor extends ByteBufferDenseTensor {
    public DenseDoubleTensor(ByteBuffer byteBuffer, long[] shape) {
        super(byteBuffer, shape, DOUBLE, Double.BYTES);
    }

    public DenseDoubleTensor(ByteBuffer byteBuffer, long[] shape, Representable[] types, int cardinality) {
        super(byteBuffer, shape, types, cardinality, Double.BYTES);
    }

    @Override
    public TensorIterator iterator() {
        return new DenseDoubleTensor.MyIterator();
    }

    @Override
    public List<?> asList() {
        DoubleBuffer doubleBuffer = _byteBuffer.asDoubleBuffer();
        double[] doubleArray = new double[doubleBuffer.remaining()];
        doubleBuffer.get(doubleArray);
        return Doubles.asList(doubleArray);
    }

    private class MyIterator extends ByteBufferDenseTensorIterator {
        MyIterator() {
            super();
        }

        MyIterator(ByteBufferDenseTensorIterator other) {
            super(other);
        }

        @Override
        public Object getValue() {
            return getDouble(_indices.length);
        }

        @Override
        public double getDouble(int index) {
            return _byteBuffer.getDouble(_position);
        }

        @Override
        public TensorIterator getCopy() {
            return new DenseDoubleTensor.MyIterator(this);
        }
    }
}
