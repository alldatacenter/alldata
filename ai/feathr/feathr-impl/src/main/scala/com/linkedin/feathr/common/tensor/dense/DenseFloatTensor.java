package com.linkedin.feathr.common.tensor.dense;

import com.google.common.primitives.Floats;
import com.linkedin.feathr.common.tensor.Representable;
import com.linkedin.feathr.common.tensor.TensorIterator;
import static com.linkedin.feathr.common.tensor.Primitive.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.List;

/**
 * Dense tensor which can be converted from/to a tensorflow tensor object.
 * The underlying data is backed up by FloatBuffer.
 */
public class DenseFloatTensor extends ByteBufferDenseTensor {
    public DenseFloatTensor(ByteBuffer byteBuffer, long[] shape) {
        super(byteBuffer, shape, FLOAT, Float.BYTES);
    }

    public DenseFloatTensor(ByteBuffer byteBuffer, long[] shape, Representable[] types, int cardinality) {
        super(byteBuffer, shape, types, cardinality, Float.BYTES);
    }

    @Override
    public TensorIterator iterator() {
        return new DenseFloatTensor.MyIterator();
    }

    @Override
    public List<?> asList() {
        FloatBuffer floatBuffer = _byteBuffer.asFloatBuffer();
        float[] floatArray = new float[floatBuffer.remaining()];
        floatBuffer.get(floatArray);
        return Floats.asList(floatArray);
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
            return getFloat(_indices.length);
        }

        @Override
        public float getFloat(int index) {
            return _byteBuffer.getFloat(_position);
        }

        @Override
        public TensorIterator getCopy() {
            return new DenseFloatTensor.MyIterator(this);
        }
    }
}
