package com.linkedin.feathr.common.tensor;



import com.linkedin.feathr.common.types.PrimitiveType;
import com.linkedin.feathr.common.types.ValueType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


/**
 * Type definition for a TypedTensor, defines the value type, dimension types and dimension names.
 */
public final class TensorType implements Serializable {
    public static final TensorType EMPTY =
            new TensorType(PrimitiveType.FLOAT, Collections.emptyList(), Collections.emptyList());

    private final TensorCategory _tensorCategory;
    private final ValueType _valueType;
    private final List<DimensionType> _dimensionTypes;
    private final List<String> _dimensionNames;
    private volatile Representable[] _columnTypes = null;

    /**
     * Create a SPARSE tensor type with default local names for dimensions.
     * @param valueType the type of the value contained in the tensor.
     * @param dimensionTypes a sequence of types for all dimensions of the tensor (can be empty).
     * Dimension names will default to names taken from dimensionTypes.
     */
    public TensorType(ValueType valueType, List<DimensionType> dimensionTypes) {
        this(valueType, dimensionTypes, null);
    }

    /**
     * Create a SPARSE tensor type.
     *
     * @param valueType the type of the value contained in the tensor.
     * @param dimensionTypes a sequence of types for all dimensions of the tensor (can be empty).
     * @param dimensionNames are local to the TensorType,
     * allowing to differentiate between multiple instances of the same dimension type,
     * for example, x used for both viewer and viewee.
     * If null, defaults to names taken from dimensionTypes.
     */
    public TensorType(ValueType valueType, List<DimensionType> dimensionTypes, List<String> dimensionNames) {
        this(TensorCategory.SPARSE, valueType, dimensionTypes, dimensionNames);
    }

    /**
     * @param tensorCategory the category of the tensor type, such as dense, sparse, etc.
     *                       NOTE: at the moment this is stored as is, and no additional validation is guaranteed
     *                       (such as choosing the right tensor builder based on the tensor category).

     * @param valueType the type of the value contained in the tensor.
     * @param dimensionTypes a sequence of types for all dimensions of the tensor (can be empty).
     * @param dimensionNames are local to the TensorType,
     * allowing to differentiate between multiple instances of the same dimension type,
     * for example, x used for both viewer and viewee.
     * If null, defaults to names taken from dimensionTypes.
     */
    public TensorType(TensorCategory tensorCategory, ValueType valueType, List<DimensionType> dimensionTypes, List<String> dimensionNames) {
        this._tensorCategory = tensorCategory;
        List<String> dimNames = dimensionNames;
        if (dimNames == null) {
            dimNames = new ArrayList<>(dimensionTypes.size());
            for (DimensionType dt : dimensionTypes) {
                dimNames.add(dt.getName());
            }
        } else if (dimensionTypes.size() != dimNames.size()) {
            throw new IllegalArgumentException(
                    "The numbers of dimension types " + dimensionTypes + " and names " + dimNames + " have to be equal.");
        }
        this._valueType = valueType;
        this._dimensionTypes = dimensionTypes;
        this._dimensionNames = dimNames;
    }

    /**
     * @param tensorCategory the category of the tensor type, such as dense, sparse, etc.
     *                       NOTE: at the moment this is stored as is, and no additional validation is guaranteed
     *                       (such as choosing the right tensor builder based on the tensor category).

     * @param valueType the type of the value contained in the tensor.
     * @param dimensionTypes a sequence of types for all dimensions of the tensor (can be empty).
     */
    public TensorType(TensorCategory tensorCategory, ValueType valueType, List<DimensionType> dimensionTypes) {
        this(tensorCategory, valueType, dimensionTypes, null);
    }

    /**
     * @return the category of this tensor.
     * NOTE: only use in context of the FDS until adopted elsewhere.
     * This defaults to SPARSE if not specified in constructor.

     */
    public TensorCategory getTensorCategory() {
        return _tensorCategory;
    }

    public ValueType getValueType() {
        return _valueType;
    }

    public List<DimensionType> getDimensionTypes() {
        return _dimensionTypes;
    }

    public List<String> getDimensionNames() {
        return _dimensionNames;
    }

    /**
     * WARNING: this returns primitive representations, not DimensionType/ValueType.
     * @return Dimension types followed by a value type.
     */
    public Representable[] getColumnTypes() {
        if (_columnTypes == null) {
            Representable[] representables = new Representable[_dimensionTypes.size() + 1];
            int i = 0;
            for (DimensionType dimensionType : _dimensionTypes) {
                representables[i] = dimensionType.getRepresentation();
                i++;
            }
            representables[i] = _valueType.getRepresentation();

            _columnTypes = representables;
        }
        return _columnTypes;
    }

    public void setDimensions(WriteableTuple target, Object[] dimensions) {
        Objects.requireNonNull(target);
        Objects.requireNonNull(dimensions);
        if (dimensions.length != _dimensionTypes.size()) {
            throw new IllegalArgumentException("Wrong number of dimensions. Got " + dimensions.length + ", expected "
                    + _dimensionTypes.size());
        }
        for (int i = 0; i < dimensions.length; i++) {
            DimensionType dimensionType = _dimensionTypes.get(i);
            dimensionType.setDimensionValue(target, i, dimensions[i]);
        }
    }

    public int[] getShape() {

        int dimensionTypesSize = _dimensionTypes.size();
        int[] shape = new int[dimensionTypesSize];
        for (int i = 0; i < dimensionTypesSize; i++) {
            DimensionType dimensionType = _dimensionTypes.get(i);
            shape[i] = dimensionType.getShape();
        }
        return shape;
    }

    public int getDenseSize() {
        int[] shape = getShape();
        int size = 1;
        for (int value : shape) {
            if (value == DimensionType.UNKNOWN_SHAPE) {
                return DimensionType.UNKNOWN_SHAPE;
            }
            size *= value;
        }
        return size;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TensorType that = (TensorType) o;
        return Objects.equals(_tensorCategory, that._tensorCategory) && Objects.equals(_valueType, that._valueType)
                && Objects.equals(_dimensionNames, that._dimensionNames) && Objects.equals(_dimensionTypes,
                that._dimensionTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_tensorCategory, _valueType, _dimensionNames, _dimensionTypes);
    }

    @Override
    public String toString() {
        return "TENSOR<" + getTensorCategory() + ">" + getDimensionTypes().stream()
                .map(dimensionType -> "[" + dimensionType.toString() + "]")
                .collect(Collectors.joining()) + ":" + getValueType();
    }
}
