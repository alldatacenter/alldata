package com.linkedin.feathr.common.tensor.scalar;

import com.linkedin.feathr.common.tensor.Primitive;
import com.linkedin.feathr.common.tensor.Representable;
import com.linkedin.feathr.common.tensor.TensorIterator;

public class ScalarDoubleTensor extends ScalarTensor {
    private static final Representable[] TYPES = {Primitive.DOUBLE};
    private final double _value;

    public ScalarDoubleTensor(double value) {
        this._value = value;
    }

    @Override
    public Representable[] getTypes() {
        return TYPES;
    }

    @Override
    public TensorIterator iterator() {
        return new MyIterator(0);
    }

    private final class MyIterator extends BaseIterator {
        private MyIterator(int i) {
            super(i);
        }

        @Override
        public Object getValue(int column) {
            return getDouble(column);
        }

        @Override
        public double getDouble(int column) {
            checkColumn(column);
            return _value;
        }

        @Override
        public TensorIterator getCopy() {
            return new MyIterator(_i);
        }
    }
}