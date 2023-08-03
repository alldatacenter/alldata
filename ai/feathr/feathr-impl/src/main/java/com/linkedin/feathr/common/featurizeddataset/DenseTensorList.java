package com.linkedin.feathr.common.featurizeddataset;

import com.linkedin.feathr.common.tensor.DenseTensor;

import java.util.AbstractList;

/**
 * A list that is backed by a dense tensor.
 * Exposes only the values of the tensor in their natural order.
 * Warning: is only reasonably efficient for a single-threaded sequential access pattern.
 * Random access is quite inefficient.
 */
public class DenseTensorList extends AbstractList<Object> {
    private final DenseTensor _denseTensor;
    private final BaseDenseTensorIterator _iterator;

    public DenseTensorList(DenseTensor denseTensor) {
        _denseTensor = denseTensor;
        _iterator = (BaseDenseTensorIterator) _denseTensor.iterator();
        _iterator.start();
    }

    @Override
    public synchronized Object get(int index) {
        if (index < _iterator.getFlatIndex()) {
            _iterator.start();
        }
        while (index > _iterator.getFlatIndex()) {
            if (!_iterator.isValid()) {
                throw new IndexOutOfBoundsException(index + " is out of bounds, cardinality is " + _denseTensor.cardinality());
            }
            _iterator.next();
        }
        return _iterator.getValue(_denseTensor.getArity() - 1);
    }

    @Override
    public int size() {
        return _denseTensor.cardinality();
    }
}
