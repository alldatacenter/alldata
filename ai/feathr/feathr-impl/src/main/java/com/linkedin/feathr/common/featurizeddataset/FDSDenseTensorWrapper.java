package com.linkedin.feathr.common.featurizeddataset;

import com.linkedin.feathr.common.tensor.DenseTensor;
import com.linkedin.feathr.common.tensor.Representable;
import com.linkedin.feathr.common.tensor.TensorData;
import com.linkedin.feathr.common.tensor.TensorIterator;
import scala.collection.Seq;

import java.util.List;

/**
 * A wrapper to adapt FDS Spark array to TensorData interface, avoiding allocation of buffers/arrays.
 *
 * The main complexity is in implementation of the TensorIterator.
 * Handles static dense and ragged cases.
 *
 * NOTE that this class directly operates with scala.collection.Seq values coming from Spark DataFrame.
 * An alternative would be converting those to java.util.List, but given the performance requirements of this class, that was not chosen.
 */
class FDSDenseTensorWrapper extends DenseTensor {
    private final Representable[] _columnTypes;
    private final int _rank;
    private final boolean _regular;
    private final Seq<?> _featureValue;

    // The following transient values can be null and later computed, but once computed become immutable.
    private transient Integer _count;
    private transient long[] _shape;

    /**
     * Adapts a (potentially nested) list of Spark values to a TensorData.
     *
     * @param columnTypes the primitives of all the columns (all dimensions must be INT)
     * @param regular if the dense tensor has a regular shape as opposed to ragged
     *                (so that all arrays on a given level have the same size - this optimizes cardinality)
     * @param featureValue the list of Spark values to wrap
     */
    FDSDenseTensorWrapper(Representable[] columnTypes, boolean regular, Seq<Object> featureValue) {
        _columnTypes = columnTypes;
        _rank = columnTypes.length - 1;
        _regular = regular;
        _featureValue = featureValue;
    }

    @Override
    public Representable[] getTypes() {
        return _columnTypes;
    }

    @Override
    public int estimatedCardinality() {
        return cardinality();
    }

    @Override
    public int cardinality() {
        if (_count == null) {
            // This is VERY expensive for RAGGED, and still expensive for regular deep DENSE.
            int count;
            if (_regular) {
                count = 1;
                Object value = _featureValue;
                while (value instanceof Seq) {
                    Seq<?> array = (Seq<?>) value;
                    count *= array.size();
                    if (count == 0) {
                        break;
                    }
                    value = array.apply(0);
                }
            } else {
                count = 0;
                TensorIterator iterator = iterator();
                iterator.start();
                while (iterator.isValid()) {
                    count++;
                    iterator.next();
                }
            }
            _count = count;
        }
        return _count;
    }

    @Override
    public TensorIterator iterator() {
        return new FDSTensorIterator();
    }

    @Override
    public long[] getShape() {
        if (_shape != null) {
            return _shape;
        }
        if (_regular) {
            long[] shape = new long[_columnTypes.length - 1];
            Object value = _featureValue;
            for (int i = 0; i < shape.length; i++) {
                Seq<?> array = (Seq<?>) value;
                shape[i] = array.size();
                value = array.apply(0);
            }
            _shape = shape;
            return shape;
        } else {
            throw new UnsupportedOperationException("getShape is not supported on RAGGED tensors.");
        }
    }

    @Override
    public List<?> asList() {
        return new DenseTensorList(this);
    }

    private class FDSTensorIterator extends BaseDenseTensorIterator {
        // While indices are in principle enough to access data, resolving them on every access is inefficient.
        // This array holds the nested arrays on the current path of indices, one array per dimension, starting with the root one.
        // _cachedArrays[i+1] is the element of _cachedArrays[i] at the index _indices[i].
        private final Seq<?>[] _cachedArrays;

        FDSTensorIterator() {
            super(_rank);
            _cachedArrays = new Seq<?>[_rank];
            start();
        }

        FDSTensorIterator(FDSTensorIterator original) {
            super(original);
            _cachedArrays = original._cachedArrays;
        }

        @Override
        public TensorData getTensorData() {
            return FDSDenseTensorWrapper.this;
        }

        @Override
        protected boolean cache(int level, int childIndex) {
            if (childIndex >= _cachedArrays[level - 1].size()) {
                return false;
            }

            // Do not cache the leaves.
            if (level < _rank) {
                _cachedArrays[level] = (Seq<?>) _cachedArrays[level - 1].apply(childIndex);
            }
            return true;
        }

        @Override
        protected void cacheRoot() {
            _cachedArrays[0] = _featureValue;
        }

        @Override
        public TensorIterator getCopy() {
            return new FDSTensorIterator(this);
        }

        @Override
        public Object getValue(int column) {
            if (column < _rank) {
                return getIndex(column);
            }
            return _cachedArrays[_rank - 1].apply(getIndex(_rank - 1));
        }

        @Override
        public int getInt(int column) {
            if (column < _rank) {
                return getIndex(column);
            }
            return (int) getValue(column);
        }

        @Override
        public long getLong(int column) {
            if (column < _rank) {
                return getIndex(column);
            }
            return (long) getValue(column);
        }

        @Override
        public float getFloat(int column) {
            if (column < _rank) {
                throw new IllegalArgumentException("Dimensions of dense tensors cannot be accessed as a float, requested dimension: " + column);
            }
            return (float) getValue(column);
        }

        @Override
        public String getString(int column) {
            if (column < _rank) {
                throw new IllegalArgumentException("Dimensions of dense tensors cannot be accessed as a String, requested dimension: " + column);
            }
            return (String) getValue(column);
        }

        @Override
        public double getDouble(int column) {
            if (column < _rank) {
                throw new IllegalArgumentException("Dimensions of dense tensors cannot be accessed as a double, requested dimension: " + column);
            }
            return (double) getValue(column);
        }

        @Override
        public boolean getBoolean(int column) {
            if (column < _rank) {
                throw new IllegalArgumentException("Dimensions of dense tensors cannot be accessed as a boolean, requested dimension: " + column);
            }
            return (boolean) getValue(column);
        }

        @Override
        public byte[] getBytes(int column) {
            if (column < _rank) {
                throw new IllegalArgumentException("Dimensions of dense tensors cannot be accessed as a byte[], requested dimension: " + column);
            }
            return (byte[]) getValue(column);
        }
    }
}
