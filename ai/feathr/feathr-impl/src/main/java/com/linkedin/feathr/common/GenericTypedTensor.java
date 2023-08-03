package com.linkedin.feathr.common;

import com.linkedin.feathr.common.tensor.TensorData;
import com.linkedin.feathr.common.tensor.TensorType;

import java.util.Objects;

/**
 * This is the base tensor class, may be created from the basic properties, from a feature or operator applied
 * to another tensor
 */
public class GenericTypedTensor implements TypedTensor {

    private static final UnsupportedOperationException UNSUPPORTED_OPERATION_EXCEPTION =
            new UnsupportedOperationException("");
    protected final TensorData _data;
    protected final TensorType _type;

    /**
     * Build for tensors that have a dynamic type, for example as a result of
     * a tensor operation that changes the shape.
     *
     * @param data actual data
     * @param type the tensor type
     * @return the tensor
     *
     * NOTE: this is an advanced use case. When possible application code should stick to well
     * defined types for their feature tensor.
     */
    public GenericTypedTensor(TensorData data, TensorType type) {
        _type = type;
        _data = data;
    }

    /**
     * Returns data content.
     */
    @Override
    public TensorData getData() {
        return _data;
    }

    /**
     * Returns type metadata.
     */
    @Override
    public TensorType getType() {
        return _type;
    }

    /**
     * Slice along 1st dimension. Returns tensor with N dimensions.
     */
    @Override
    public TypedTensor slice(final Object val) {
        throw UNSUPPORTED_OPERATION_EXCEPTION;
    }

    @Override
    public TypedTensor subSlice(Object val) { throw UNSUPPORTED_OPERATION_EXCEPTION; }

    /**
     * Returns human-readable summary suitable for debugging.
     */
    @Override
    public String toDebugString() {
        return toDebugString(TensorUtils.DEFAULT_MAX_STRING_LEN);
    }

    @Override
    public String toDebugString(int maxStringLenLimit) {
        return TensorUtils.getDebugString(_type, _data, maxStringLenLimit);
    }

    /**
     * Is never equal to instances of other classes, including Marmalade-generated.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GenericTypedTensor that = (GenericTypedTensor) o;
        return Objects.equals(_type, that._type) && Equal.INSTANCE.apply(_data, that._data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Hasher.INSTANCE.apply(_data, false), _type);
    }
}
