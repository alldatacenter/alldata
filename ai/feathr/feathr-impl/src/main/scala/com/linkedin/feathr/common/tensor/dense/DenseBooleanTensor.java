package com.linkedin.feathr.common.tensor.dense;

import com.linkedin.feathr.common.tensor.Representable;
import com.linkedin.feathr.common.tensor.TensorIterator;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static com.linkedin.feathr.common.tensor.Primitive.BOOLEAN;


/**
 * Dense tensor which can be converted from/to a tensorflow tensor object.
 * The underlying data is backed up by IntBuffer.
 */
public class DenseBooleanTensor extends ByteBufferDenseTensor {
    public DenseBooleanTensor(ByteBuffer byteBuffer, long[] shape) {
        super(byteBuffer, shape, BOOLEAN, 1);
    }

    public DenseBooleanTensor(ByteBuffer byteBuffer, long[] shape, Representable[] types, int cardinality) {
        super(byteBuffer, shape, types, cardinality, 1);
    }

    @Override
    public TensorIterator iterator() {
        return new DenseBooleanTensor.MyIterator();
    }

    @Override
    public List<?> asList() {
        ByteBuffer buffer = _byteBuffer.asReadOnlyBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        List<Boolean> booleanList = new ArrayList<>(bytes.length);
        for (byte aByte : bytes) {
            booleanList.add(aByte != (byte) 0);
        }
        return booleanList;
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
            return getBoolean(_indices.length);
        }

        @Override
        public boolean getBoolean(int index) {
            return _byteBuffer.get(_position) != (byte) 0;
        }

        @Override
        public TensorIterator getCopy() {
            return new DenseBooleanTensor.MyIterator(this);
        }
    }
}