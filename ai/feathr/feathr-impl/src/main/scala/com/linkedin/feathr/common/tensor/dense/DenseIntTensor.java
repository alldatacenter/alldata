package com.linkedin.feathr.common.tensor.dense;

import com.google.common.primitives.Ints;
import com.linkedin.feathr.common.tensor.Representable;
import com.linkedin.feathr.common.tensor.TensorIterator;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.List;

import static com.linkedin.feathr.common.tensor.Primitive.INT;

/**
 * Dense tensor which can be converted from/to a tensorflow tensor object.
 * The underlying data is backed up by IntBuffer.
 */
public class DenseIntTensor extends ByteBufferDenseTensor {
    public DenseIntTensor(ByteBuffer byteBuffer, long[] shape) {
        super(byteBuffer, shape, INT, Integer.BYTES);
    }

    public DenseIntTensor(ByteBuffer byteBuffer, long[] shape, Representable[] types, int cardinality) {
        super(byteBuffer, shape, types, cardinality, Integer.BYTES);
    }

    @Override
    public TensorIterator iterator() {
        return new DenseIntTensor.MyIterator();
    }

    @Override
    public List<?> asList() {
        IntBuffer intBuffer = _byteBuffer.asIntBuffer();
        int[] intArray = new int[intBuffer.remaining()];
        intBuffer.get(intArray);
        return  Ints.asList(intArray);
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
            return getInt(_indices.length);
        }

        public int getInt(int index) {
            if (index >= 0 && index < _indices.length) {
                return _indices[index];
            }
            if (index == _indices.length) {
                return _byteBuffer.getInt(_position);
            }
            throw new IndexOutOfBoundsException(
                    "The index " + index + " is out of the expected range 0 <= index <= " + _indices.length + ".");
        }

        @Override
        public TensorIterator getCopy() {
            return new DenseIntTensor.MyIterator(this);
        }
    }
}
