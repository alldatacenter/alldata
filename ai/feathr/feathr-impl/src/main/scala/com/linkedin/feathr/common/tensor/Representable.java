package com.linkedin.feathr.common.tensor;

/**
 * A scalar with a primitive representation - either a dimension or a value.
 * Operators can permute these and return back to the client without understanding them.
 */
public interface Representable {
    Primitive getRepresentation();
}
