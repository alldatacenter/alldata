package com.linkedin.feathr.common.tensor;

import java.util.Objects;

/**
 * The most basic type of a dimension.
 * Only specifies the primitive type and an optional shape ("size"), without any high-level metadata.
 */
public final class PrimitiveDimensionType extends DimensionType {
    // Pre-create some common types so that they can be shared.
    public static final PrimitiveDimensionType LONG = new PrimitiveDimensionType(Primitive.LONG);
    public static final PrimitiveDimensionType INT = new PrimitiveDimensionType(Primitive.INT);
    public static final PrimitiveDimensionType STRING = new PrimitiveDimensionType(Primitive.STRING);
    public static final PrimitiveDimensionType BOOLEAN = new PrimitiveDimensionType(Primitive.BOOLEAN, 2);
    public static final PrimitiveDimensionType BYTES = new PrimitiveDimensionType(Primitive.BYTES);

    private final Primitive _primitive;
    private final int _shape;

    public PrimitiveDimensionType(Primitive primitive, int shape) {
        if (shape != UNKNOWN_SHAPE) {
            if (!primitive.canBeDenseDimension()) {
                throw new IllegalArgumentException("The type " + primitive + " can not be dense so cannot have a shape.");
            }
        }
        _primitive = primitive;
        _shape = shape;
    }

    public PrimitiveDimensionType(Primitive primitive) {
        this(primitive, UNKNOWN_SHAPE);
    }

    /**
     * @return a copy of this type with a new shape.
     * @param shape the shape of the new type.
     */
    public PrimitiveDimensionType withShape(int shape) {
        return new PrimitiveDimensionType(_primitive, shape);
    }

    @Override
    public int getShape() {
        return _shape;
    }

    @Override
    public Primitive getRepresentation() {
        return _primitive;
    }

    @Override
    public String getName() {
        if (_shape == UNKNOWN_SHAPE || _primitive == Primitive.BOOLEAN) {
            return _primitive.name();
        } else {
            return _primitive.name() + "(" + _shape + ")";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PrimitiveDimensionType that = (PrimitiveDimensionType) o;
        return _shape == that._shape && _primitive == that._primitive;
    }

    @Override
    public int hashCode() {
        return Objects.hash(_primitive, _shape);
    }

    @Override
    public void setDimensionValue(WriteableTuple target, int column, Object dimensionValue) {
        // Only validate the bounds for Number values - for others it's difficult to define.
        // NOTE This will allow setting of out-of-bound values if set using String, for example.

        if (_shape != UNKNOWN_SHAPE && dimensionValue instanceof Number) {
            int index = ((Number) dimensionValue).intValue();
            if (index < 0 || index >= _shape) {
                throw new IllegalArgumentException(
                        "The passed value " + dimensionValue + " is out of bound for [0, " + _shape + ").");
            }
        }
        super.setDimensionValue(target, column, dimensionValue);
    }

    @Override
    public String toString() {
        return getRepresentation() + (getShape() == UNKNOWN_SHAPE ? "" : "(" + getShape() + ")");
    }
}
