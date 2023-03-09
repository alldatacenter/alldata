package com.linkedin.feathr.common.tensorbuilder;

import com.linkedin.feathr.common.*;
import com.linkedin.feathr.common.tensor.*;
import com.linkedin.feathr.common.tensor.dense.*;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;


/**
 * Build a dense tensor based on input column types and shape,
 * All columns except the last one, must have {@link Primitive#INT} representation.
 * The last column type is considered to be value type and determines
 */
public class DenseTensorBuilder implements TensorBuilder, BulkTensorBuilder {
    private final long[] _shape;
    private final Primitive _valueType;
    private final Representable[] _columnTypes;
    private final int _valColumn;
    // The full count of the tensor values, ignoring a possible unknown dimension.
    private final int _staticCardinality;
    private final SimpleWriteableTuple _simpleWriteableTuple;

    // Byte buffer doesn't work well with Strings. Use Object Array
    private Object[] _buffer;
    // The count of the optional unknown dimension, defaulting to 1.
    private int _dynamicCardinality;

    // Allow at most one dimension of unknown shape, this is its index.
    private final int _indexOfUnknown;

    public DenseTensorBuilder(Representable[] columnTypes, long[] shape) {
        int indexOfUnknown = -1;
        for (int i = 0; i < columnTypes.length - 1; i++) {
            if (shape[i] == DimensionType.UNKNOWN_SHAPE) {
                if (indexOfUnknown >= 0) {
                    throw new IllegalArgumentException("DenseTensor can only have at most one unbounded dimension.");
                }
                indexOfUnknown = i;
            }
            if (columnTypes[i].getRepresentation() != Primitive.INT) {
                throw new IllegalArgumentException("DenseTensor can only have INT dimensions.");
            }
        }
        _indexOfUnknown = indexOfUnknown;
        _valueType = columnTypes[columnTypes.length - 1].getRepresentation();
        _columnTypes = columnTypes;
        long cardinality = 1;
        _shape = shape;
        _valColumn = shape.length;
        for (int i = 0; i < _shape.length; i++) {
            if (i != _indexOfUnknown) {
                long n = _shape[i];
                cardinality *= n;
            }
        }
        _staticCardinality = (int) cardinality;
        _simpleWriteableTuple = new SimpleWriteableTuple(columnTypes);
    }

    public DenseTensorBuilder(TensorType tensorType) {
        this(tensorType.getColumnTypes(), TensorUtils.getShape(tensorType));
        if (tensorType.getTensorCategory() != TensorCategory.DENSE) {
            throw new IllegalArgumentException("Not a DENSE TensorType: " + tensorType);
        }
    }

    @Override
    public int getStaticCardinality() {
        return _staticCardinality;
    }

    @Override
    public boolean hasVariableCardinality() {
        return _indexOfUnknown > -1;
    }

    @Override
    public TensorBuilder append() {
        int index = 0;
        for (int i = 0; i < _valColumn; i++) {
            index = index * (int) _shape[i] + _simpleWriteableTuple.getInt(i);
        }

        _buffer[index] = _columnTypes[_valColumn].getRepresentation().toObject(_simpleWriteableTuple, _valColumn);

        _simpleWriteableTuple.resetColumns();
        return this;
    }

    @Override
    public TensorBuilder start(int estimatedRows) {
        if (_indexOfUnknown != -1) {
            throw new IllegalStateException(
                    "Tensors with statically unknown dimensions can only be built by using build(*[]).");
        }
        _dynamicCardinality = 1;
        _buffer = new Object[_staticCardinality];
        return this;
    }

    @Override
    public Representable[] getOutputTypes() {
        return _columnTypes;
    }

    @Override
    public Representable[] getTypes() {
        return _columnTypes;
    }

    /**
     * DenseTensorBuilder's setInt should be used seldomly. The proper usage to construct DenseTensors is by using the
     * build() method and passing in the associated arrays. By calling setInt for non value columns, the currentIndex
     * is changed to point to the associated location in the value array. It assumes the usage is correct (append one row at a time
     * and not overriding a given column, aka calling setInt on the same column multiple times).
     * Ex of incorrect usage:
     * shape is [2,2]
     * setInt(0, 0), setInt(0, 1) -> Here the currentIndex is set for column 0 with value 0 and then changed to column 0 with value 1.
     * Ex of correct usage:
     * setInt(0,0), setInt(1,0) -> Here the currentIndex is set for column 0 with value 0 and column 1 with value 0. Setting
     * the value here will be associated with the correct currentIndex and the correct row.
     *
     */
    @Override
    public TensorBuilder setInt(int column, int value) {
        if (column < _valColumn) {
            if (value >= _shape[column]) {
                throw new IllegalArgumentException(
                        String.format("Cannot have column value %d larger than shape value %d", value, _shape[column]));
            }
        }
        _simpleWriteableTuple.setInt(column, value);
        return this;
    }

    @Override
    public TensorBuilder setLong(int column, long value) {
        if (column == _valColumn) {
            _simpleWriteableTuple.setLong(column, value);
        } else {
            throw new IllegalArgumentException(String.format("Cannot write long value in column %d", column));
        }
        return this;
    }

    @Override
    public TensorBuilder setFloat(int column, float value) {
        if (column == _valColumn) {
            _simpleWriteableTuple.setFloat(column, value);
        } else {
            throw new IllegalArgumentException(String.format("Cannot write float value in column %d", column));
        }
        return this;
    }

    @Override
    public TensorBuilder setDouble(int column, double value) {
        if (column == _valColumn) {
            _simpleWriteableTuple.setDouble(column, value);
        } else {
            throw new IllegalArgumentException(String.format("Cannot write double value in column %d", column));
        }
        return this;
    }

    @Override
    public TensorBuilder setBoolean(int column, boolean value) {
        if (column == _valColumn) {
            _simpleWriteableTuple.setBoolean(column, value);
        } else {
            throw new IllegalArgumentException(String.format("Cannot write boolean value in column %d", column));
        }
        return this;
    }

    @Override
    public TensorBuilder setString(int column, String value) {
        if (column == _valColumn) {
            _simpleWriteableTuple.setString(column, value);
        } else {
            throw new IllegalArgumentException(String.format("Cannot write boolean value in column %d", column));
        }
        return this;
    }

    @Override
    public TensorBuilder setBytes(int column, byte[] value) {
        if (column == _valColumn) {
            _simpleWriteableTuple.setBytes(column, value);
        } else {
            throw new IllegalArgumentException(String.format("Cannot write bytes value in column %d", column));
        }
        return this;
    }

    @Override
    public TensorData build(float[] floats) {
        ByteBuffer byteBuffer = BufferUtils.createByteBuffer(floats, _valueType);
        return build(floats.length, byteBuffer);
    }

    @Override
    public TensorData build(int[] ints) {
        ByteBuffer byteBuffer = BufferUtils.createByteBuffer(ints, _valueType);
        return build(ints.length, byteBuffer);
    }

    @Override
    public TensorData build(long[] longs) {
        ByteBuffer byteBuffer = BufferUtils.createByteBuffer(longs, _valueType);
        return build(longs.length, byteBuffer);
    }

    @Override
    public TensorData build(double[] doubles) {
        ByteBuffer byteBuffer = BufferUtils.createByteBuffer(doubles, _valueType);
        return build(doubles.length, byteBuffer);
    }

    @Override
    public TensorData build(boolean[] booleans) {
        ByteBuffer byteBuffer = BufferUtils.createByteBuffer(booleans, _valueType);
        return build(booleans.length, byteBuffer);
    }

    @Override
    public TensorData build(List<?> values) {
        ByteBuffer byteBuffer = BufferUtils.createByteBuffer(values, _valueType);
        return build(values.size(), byteBuffer);
    }

    @Override
    public TensorData build(ByteBuffer values) {
        if (_indexOfUnknown == -1) {
            _dynamicCardinality = 1;
        } else {
            throw new IllegalStateException(
                    "Tensors with statically unknown dimensions can only be built by using build(*[]).");
        }
        return buildFromShape(_shape, values);
    }

    private TensorData build(int cardinality, ByteBuffer byteBuffer) {
        long[] shape;
        if (_indexOfUnknown == -1) {
            _dynamicCardinality = 1;
            shape = _shape;
        } else {
            shape = new long[_shape.length];
            for (int i = 0; i < shape.length; i++) {
                if (i != _indexOfUnknown) {
                    shape[i] = _shape[i];
                } else {
                    _dynamicCardinality = cardinality / _staticCardinality;
                    shape[i] = _dynamicCardinality;
                }
            }
        }
        return buildFromShape(shape, byteBuffer);
    }

    @Override
    public TensorData build(boolean sort) {
        if (_indexOfUnknown == -1) {
            _dynamicCardinality = 1;
        } else {
            throw new IllegalStateException(
                    "Tensors with statically unknown dimensions can only be built by using build(*[]).");
        }

        Object defaultVal = generateDefaultValue();
        for (int i = 0; i < _buffer.length; i++) {
            if (_buffer[i] == null) {
                _buffer[i] = defaultVal;
            }
        }
        return build(Arrays.asList(_buffer));
    }

    private TensorData buildFromShape(long[] shape, ByteBuffer byteBuffer) {
        byteBuffer.rewind();
        int cardinality = _staticCardinality * _dynamicCardinality;
        switch (_valueType) {
            case INT:
                return new DenseIntTensor(byteBuffer, shape, _columnTypes, cardinality);
            case DOUBLE:
                return new DenseDoubleTensor(byteBuffer, shape, _columnTypes, cardinality);
            case LONG:
                return new DenseLongTensor(byteBuffer, shape, _columnTypes, cardinality);
            case FLOAT:
                return new DenseFloatTensor(byteBuffer, shape, _columnTypes, cardinality);
            case STRING:
                return new DenseStringTensor(byteBuffer, shape, _columnTypes, cardinality);
            case BOOLEAN:
                return new DenseBooleanTensor(byteBuffer, shape, _columnTypes, cardinality);
            case BYTES:
                return new DenseBytesTensor(byteBuffer, shape, _columnTypes, cardinality);
            default:
                throw new IllegalArgumentException("Cannot support dense tensor with value type: " + _valueType);
        }
    }

    /**
     * The Object buffer array should not contain null elements. This method will assign a default value that will replace
     * null based on the value type of the tensor.
     * @return default value based on the value type of the tensor.
     */
    private Object generateDefaultValue() {
        switch (_columnTypes[_valColumn].getRepresentation()) {
            case INT:
                return 0;
            case LONG:
                return 0L;
            case FLOAT:
                return 0.0f;
            case DOUBLE:
                return 0.0d;
            case STRING:
                return "";
            case BOOLEAN:
                return false;
            case BYTES:
                return new byte[0];
            default:
                throw new IllegalArgumentException(
                        "Cannot handle value of type: " + _columnTypes[_valColumn].getRepresentation());
        }
    }
}
