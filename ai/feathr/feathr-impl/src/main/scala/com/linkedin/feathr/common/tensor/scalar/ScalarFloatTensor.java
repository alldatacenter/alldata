package com.linkedin.feathr.common.tensor.scalar;

import com.linkedin.feathr.common.tensor.Primitive;
import com.linkedin.feathr.common.tensor.Representable;
import com.linkedin.feathr.common.tensor.TensorIterator;

public class ScalarFloatTensor extends ScalarTensor {
    private static final Representable[] TYPES = {Primitive.FLOAT};
    private final float _value;

    public ScalarFloatTensor(float value) {
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
            return getFloat(column);
        }

        @Override
        public float getFloat(int column) {
            checkColumn(column);
            return _value;
        }

        @Override
        public TensorIterator getCopy() {
            return new MyIterator(_i);
        }
    }
}