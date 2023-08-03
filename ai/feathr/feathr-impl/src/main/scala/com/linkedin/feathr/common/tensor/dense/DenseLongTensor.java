package com.linkedin.feathr.common.tensor.dense;

import com.google.common.primitives.Longs;
import com.linkedin.feathr.common.tensor.Representable;
import com.linkedin.feathr.common.tensor.TensorIterator;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.List;

import static com.linkedin.feathr.common.tensor.Primitive.LONG;


/**
 * Dense tensor which can be converted from/to a tensorflow tensor object.
 * The underlying data is backed up by LongBuffer.
 */
public class DenseLongTensor extends ByteBufferDenseTensor {
    public DenseLongTensor(ByteBuffer byteBuffer, long[] shape) {
        super(byteBuffer, shape, LONG, Long.BYTES);
    }

    public DenseLongTensor(ByteBuffer byteBuffer, long[] shape, Representable[] types, int cardinality) {
        super(byteBuffer, shape, types, cardinality, Long.BYTES);
    }

    @Override
    public TensorIterator iterator() {
        return new DenseLongTensor.MyIterator();
    }

    @Override
    public List<?> asList() {
        LongBuffer longBuffer = _byteBuffer.asLongBuffer();
        long[] longArray = new long[longBuffer.remaining()];
        longBuffer.get(longArray);
        return Longs.asList(longArray);
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
            return getLong(_indices.length);
        }

        @Override
        public long getLong(int index) {
            return _byteBuffer.getLong(_position);
        }

        @Override
        public TensorIterator getCopy() {
            return new DenseLongTensor.MyIterator(this);
        }
    }
}
