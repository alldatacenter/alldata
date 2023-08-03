package com.linkedin.feathr.common.tensorbuilder;

import com.linkedin.feathr.common.tensor.Representable;

public interface TypedOperator {
    Representable[] getOutputTypes();
}
