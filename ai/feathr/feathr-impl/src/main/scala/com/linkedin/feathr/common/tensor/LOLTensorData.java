package com.linkedin.feathr.common.tensor;

import com.linkedin.feathr.common.tensorbuilder.BufferUtils;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 *
 * Each list is of the same length and stores a single dimension (similarly to parallel arrays).
 *
 * There are as many dimensions as elements in columnTypes minus one.
 * columnTypes describe the primitive type of the corresponding column (a dimension or a value).
 *
 * Instances of this class are expected to be only constructed from existing lists (e.g., from Avro or Pegasus),
 * so no corresponding builder class is provided.
 */
public class LOLTensorData implements TensorData {
    private final Representable[] _columnTypes;
    private final List<List<?>> _dimensions;
    private List<?> _values;

    public LOLTensorData(Representable[] columnTypes, List<List<?>> dimensions, List<?> values, boolean sort) {
        if (columnTypes.length != dimensions.size() + 1) {
            throw new IllegalArgumentException("There must be a column type for every dimension and value.");
        }
        // Skipping validation of all dimensions and the value lists to have the same size as potentially expensive.

        _columnTypes = columnTypes;
        _dimensions = dimensions;
        _values = values;
        if (sort && !dimensions.isEmpty()) {
            sort();
        }
    }

    public LOLTensorData(Representable[] columnTypes, List<List<?>> dimensions, List<?> values) {
        this(columnTypes, dimensions, values, true);
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
        return _values.size();
    }

    @Override
    public boolean isEmpty() {
        return _values.isEmpty();
    }

    @Override
    public TensorIterator iterator() {
        return new LOLTensorIterator();
    }

    class LOLTensorIterator implements TensorIterator {
        private int _position;


        LOLTensorIterator() {
        }

        LOLTensorIterator(int position) {
            this._position = position;
        }

        public int getPosition() {
            return _position;
        }

        public void setPosition(int position) {
            this._position = position;
        }

        @Override
        public TensorData getTensorData() {
            return LOLTensorData.this;
        }

        @Override
        public void start() {
            _position = 0;
        }

        @Override
        public boolean isValid() {
            return _position < cardinality();
        }

        @Override
        public Object getValue(int index) {
            if (index == _dimensions.size()) {
                return _values.get(_position);
            }
            List<?> dimension;
            try {
                dimension = _dimensions.get(index);
            } catch (IndexOutOfBoundsException e) {
                throw new RuntimeException("Trying to access dimension " + index + " in tensor with column types " + Arrays.asList(_columnTypes), e);
            }
            try {
                return dimension.get(_position);
            } catch (IndexOutOfBoundsException e) {
                throw new RuntimeException("Length of values is " + _values.size() + " but of dimension " + index + " is " + dimension.size(), e);
            }
        }

        @Override
        public int getInt(int index) {
            return (Integer) getValue(index);
        }

        @Override
        public long getLong(int index) {
            return (Long) getValue(index);
        }

        @Override
        public float getFloat(int index) {
            return (Float) getValue(index);
        }

        @Override
        public String getString(int index) {
            return (String) getValue(index);
        }

        @Override
        public double getDouble(int index) {
            return (Double) getValue(index);
        }

        @Override
        public boolean getBoolean(int index) {
            return (Boolean) getValue(index);
        }

        @Override
        public byte[] getBytes(int index) {
            return (byte[]) getValue(index);
        }

        @Override
        public void next() {
            _position++;
        }

        @Override
        public TensorIterator getCopy() {
            return new LOLTensorIterator(_position);
        }
    }

    public List<List<?>> getDimensions() {
        return _dimensions;
    }

    public List<?> getValues() {
        return _values;
    }

    private Comparator<Integer> getComparatorFor(int column) {
        switch (_columnTypes[column].getRepresentation()) {
            case INT:
                return Comparator.comparing(i -> (Integer) _dimensions.get(column).get(i));
            case LONG:
                return Comparator.comparing(i -> (Long) _dimensions.get(column).get(i));
            case FLOAT:
                return Comparator.comparing(i -> (Float) _dimensions.get(column).get(i));
            case STRING:
                return Comparator.comparing(i -> (String) _dimensions.get(column).get(i));
            case BOOLEAN:
                return Comparator.comparing(i -> (Boolean) _dimensions.get(column).get(i));
            case BYTES:
                return (Comparator<Integer> & Serializable) (l, r) -> BufferUtils.compareBytes(
                        (byte[]) _dimensions.get(column).get(l), (byte[]) _dimensions.get(column).get(r));
            default:
                throw new IllegalArgumentException("Cannot get comparator for column: " + column);
        }
    }
    private void sort() {
        switch (_values.size()) {
            case 0:
            case 1:
                return;
            default:
                Comparator<Integer> comparator = getComparatorFor(0);
                for (int i = 1; i < _columnTypes.length - 1; i++) {
                    comparator = comparator.thenComparing(getComparatorFor(i));
                }
                Integer[] indexes = new Integer[_dimensions.get(0).size()];
                Arrays.setAll(indexes, i -> i);
                Arrays.sort(indexes, comparator);
                reorderLists(indexes);
                break;
        }
    }

    private void reorderLists(Integer[] order) {
        int[] invOrder = new int[order.length];
        for (int i = 0; i < order.length; i++) {
            invOrder[order[i]] = i;
        }

        for (int dest = 0; dest < order.length; dest++) {
            final int src = order[dest];
            if (src == dest) {
                continue;
            }

            for (List<?> dimension : _dimensions) {
                Collections.swap(dimension, src, dest);
            }
            Collections.swap(_values, src, dest);

            int whereDestHadToGo = invOrder[dest];
            order[whereDestHadToGo] =  src;
            invOrder[src] = whereDestHadToGo;
        }
    }
}
