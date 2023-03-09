package com.linkedin.feathr.common.types;

import com.linkedin.feathr.common.tensor.Primitive;

/**
 * Type definitions for the supported primitives.
 */
public final class PrimitiveType extends ValueType {
    public static final PrimitiveType INT = new PrimitiveType(Primitive.INT);
    public static final PrimitiveType LONG = new PrimitiveType(Primitive.LONG);
    public static final PrimitiveType STRING = new PrimitiveType(Primitive.STRING);
    public static final PrimitiveType FLOAT = new PrimitiveType(Primitive.FLOAT);
    public static final PrimitiveType DOUBLE = new PrimitiveType(Primitive.DOUBLE);
    public static final PrimitiveType BOOLEAN = new PrimitiveType(Primitive.BOOLEAN);
    public static final PrimitiveType BYTES = new PrimitiveType(Primitive.BYTES);

    private final Primitive _primitive;

    /**
     * Consider using pre-allocated instances.
     * Use the constructor for generic code when you have a primitive.
     * @param primitive the primitive representing the type of the tensor.
     */
    public PrimitiveType(Primitive primitive) {
        this._primitive = primitive;
    }

    public Primitive getPrimitive() {
        return _primitive;
    }

    @Override
    public Primitive getRepresentation() {
        return _primitive;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PrimitiveType) {
            return _primitive.equals(((PrimitiveType) obj)._primitive);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return _primitive.hashCode();
    }

    @Override
    public String toString() {
        return getRepresentation().toString();
    }
}
