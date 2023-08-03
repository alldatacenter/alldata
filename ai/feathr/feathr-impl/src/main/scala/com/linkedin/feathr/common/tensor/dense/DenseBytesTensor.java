package com.linkedin.feathr.common.tensor.dense;

import com.linkedin.feathr.common.tensor.Representable;
import com.linkedin.feathr.common.tensor.TensorIterator;
import com.linkedin.feathr.common.tensorbuilder.BufferUtils;
import static com.linkedin.feathr.common.tensor.Primitive.*;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Dense tensor which can be converted from/to a TensorFlow tensor object.
 * The underlying data is backed up by ByteBuffer, using TensorFlow-compatible format:
 * https://github.com/tensorflow/tensorflow/blob/98c0deb828c2f98f0d6d77a12d32f3b33ed92887/tensorflow/c/c_api.h#L206.
 *
 * <p/>
 * All individual byte[] are added up in one ByteBuffer. ByteBuffer contains byte[] length
 * (as a varint/ULEB128 https://en.wikipedia.org/wiki/LEB128#Unsigned_LEB128), followed by the contents of the byte[]
 * at data[start_offset[i]]]
 * <p/>
 * start_offset: array[uint64]
 * data:         byte[...]
 * [offset A][offset B][offset C][length A][data A][length B][data B][length C][data C].
 */

public class DenseBytesTensor extends ByteBufferDenseTensor {
    private final int _endOfOffsets = _cardinality * Long.BYTES;

    public DenseBytesTensor(ByteBuffer byteBuffer, long[] shape) {
        super(byteBuffer, shape, BYTES, Long.BYTES);
    }

    public DenseBytesTensor(ByteBuffer byteBuffer, long[] shape, Representable[] types, int cardinality) {
        super(byteBuffer, shape, types, cardinality, Long.BYTES);
    }

    @Override
    public TensorIterator iterator() {
        return new DenseBytesTensor.MyIterator();
    }

    @Override
    public List<?> asList() {
        ByteBuffer view = _byteBuffer.asReadOnlyBuffer();
        List<ByteBuffer> list = new ArrayList<>(_cardinality);
        for (int i = 0; i < _cardinality; i++) {
            // Create a slice while keeping the position/limit of the original buffer.
            int offset = (int) view.getLong(i * Long.BYTES);
            view.position(offset);
            int length = BufferUtils.decodeVarint(view);
            view.limit(view.position() + length);
            ByteBuffer slice = view.slice();
            view.limit(_byteBuffer.limit());
            list.add(slice);
        }
        return list;
    }

    private class MyIterator extends ByteBufferDenseTensorIterator {
        MyIterator() {
            super();
        }

        MyIterator(ByteBufferDenseTensorIterator other) {
            super(other);
        }

        @Override
        public boolean isValid() {
            // For byte[], only the offsets are actually "dense".
            return _position < _endOfOffsets;
        }

        @Override
        public Object getValue() {
            return getBytes(_indices.length);
        }

        @Override
        public byte[] getBytes(int index) {
            // Rewind to the start of the varint length
            ByteBuffer view = _byteBuffer.asReadOnlyBuffer();
            int offset = (int) _byteBuffer.getLong(_position);
            view.position(offset);

            // Read the length
            int length = BufferUtils.decodeVarint(view);
            view.limit(view.position() + length);

            byte[] array = new byte[view.remaining()];
            view.get(array);

            return array;
        }

        @Override
        public TensorIterator getCopy() {
            return new DenseBytesTensor.MyIterator(this);
        }
    }
}
