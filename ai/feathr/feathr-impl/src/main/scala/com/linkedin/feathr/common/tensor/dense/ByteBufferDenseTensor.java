package com.linkedin.feathr.common.tensor.dense;

import com.linkedin.feathr.common.tensor.*;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;
import static com.linkedin.feathr.common.tensor.Primitive.INT;

/**
 * Base class for dense int/float/double/long/boolean/string/byte[] tensors backed by a ByteBuffer using TensorFlow-compatible layout.
 */
public abstract class ByteBufferDenseTensor extends DenseTensor {
    protected final ByteBuffer _byteBuffer;
    protected final int _start;
    protected final int _limit;

    protected final long[] _shape;
    protected final int _cardinality;
    protected final Representable[] _types;
    private final int _elementSize;

    public ByteBufferDenseTensor(ByteBuffer byteBuffer, long[] shape, Primitive type, int bytes) {
        this(byteBuffer, shape, genRepresentables(type, shape.length), getCardinality(shape), bytes);
    }

    ByteBufferDenseTensor(ByteBuffer byteBuffer, long[] shape, Representable[] types, int cardinality, int bytes) {
        _shape = Objects.requireNonNull(shape);
        _elementSize = bytes;
        _cardinality = cardinality;
        _types = types;
        _byteBuffer = byteBuffer.asReadOnlyBuffer().order(byteBuffer.order());
        _start = _byteBuffer.position();
        _limit = _byteBuffer.limit();
    }

    public ByteBuffer getByteBuffer() {
        return getByteBuffer(true);
    }

    public ByteBuffer getByteBuffer(boolean makeCopy) {
        return makeCopy ? _byteBuffer.duplicate().order(_byteBuffer.order()) : _byteBuffer;
    }

    @Override
    public long[] getShape() {
        return _shape;
    }

    @Override
    public Representable[] getTypes() {
        return _types;
    }

    @Override
    public int estimatedCardinality() {
        return cardinality();
    }

    @Override
    public int cardinality() {
        return _cardinality;
    }

    // Track the indices (coordinates in dimensions) and progress through them in the lexicographical order.
    // Also track the position in the buffer.
    abstract class ByteBufferDenseTensorIterator implements TensorIterator {
        protected final int[] _indices;
        protected int _position;

        ByteBufferDenseTensorIterator(ByteBufferDenseTensorIterator other) {
            this._position = other._position;
            this._indices = Arrays.copyOf(other._indices, _shape.length);
        }

        ByteBufferDenseTensorIterator() {
            this._position = _start;
            this._indices = new int[_shape.length];
        }

        @Override
        public TensorData getTensorData() {
            return ByteBufferDenseTensor.this;
        }

        @Override
        public void start() {
            this._position = _start;
            Arrays.fill(_indices, 0);
        }

        @Override
        public boolean isValid() {
            return this._position < _limit;
        }

        @Override
        public Object getValue(int index) {
            if (index < 0 || index > _indices.length) {
                throw new IndexOutOfBoundsException(
                        "The index " + index + " is out of the expected range 0 <= index <= " + _indices.length + ".");
            } else if (index < _indices.length) {
                return getInt(index);
            }
            return getValue();
        }

        protected abstract Object getValue();

        @Override
        public int getInt(int index) {
            if (index >= 0 && index < _indices.length) {
                return _indices[index];
            }
            throw new IndexOutOfBoundsException(
                    "The index " + index + " is out of the expected range 0 <= index < " + _indices.length + ".");
        }

        @Override
        public long getLong(int index) {
            throw new UnsupportedOperationException();
        }

        @Override
        public float getFloat(int index) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean getBoolean(int column) {
            throw new UnsupportedOperationException();
        }

        @Override
        public byte[] getBytes(int column) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void next() {
            int currDim = _shape.length - 1;
            while (currDim >= 0) {
                _indices[currDim]++;
                if (_indices[currDim] < (int) _shape[currDim]) {
                    break;
                }
                _indices[currDim] = 0;
                currDim--;
            }
            _position += _elementSize;
        }

        @Override
        public TensorIterator getCopy() {
            throw new UnsupportedOperationException();
        }
    }

    private static int getCardinality(long[] shape) {
        int cardinality = 1;
        for (long n: shape) {
            cardinality *= n;
        }
        return cardinality;
    }

    private static Representable[] genRepresentables(Primitive valueType, int numDims) {
        Representable[]  types = new Representable[numDims + 1];
        for (int i = 0; i < numDims; i++) {
            types[i] = INT;
        }
        types[numDims] = valueType;
        return types;
    }
}
