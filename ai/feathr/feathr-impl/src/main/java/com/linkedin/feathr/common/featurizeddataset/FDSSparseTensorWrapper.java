package com.linkedin.feathr.common.featurizeddataset;

import com.linkedin.feathr.common.tensor.Representable;
import com.linkedin.feathr.common.tensor.TensorData;
import com.linkedin.feathr.common.tensor.TensorIterator;
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema;
import org.apache.spark.sql.types.StructType;
import scala.collection.Seq;

/**
 * A wrapper around Spark GenericRowWithSchema that respects FDS spec.
 *
 * The main complexity is in implementation of the TensorIterator.
 *
 * NOTE that this class directly operates with scala.collection.Seq values coming from Spark DataFrame.
 * An alternative would be converting those to java.util.List, but given the performance requirements of this class, that was not chosen.
 */
class FDSSparseTensorWrapper implements TensorData {
    private final Representable[] _columnTypes;
    private final int _rank;
    private final GenericRowWithSchema _featureValue;
    private final int _cardinality;

    private final int[] _fieldIndices;

    /**
     * Adapts a record of Spark values to a TensorData.
     *
     * @param columnTypes the primitives of all the columns
     * @param featureValue the record of Spark values to wrap (must only use SchemaUtils.indexFieldName and SchemaUtils.valueFieldName)
     */
    FDSSparseTensorWrapper(Representable[] columnTypes, GenericRowWithSchema featureValue) {
        _columnTypes = columnTypes;
        _rank = columnTypes.length - 1;
        _featureValue = featureValue;
        _fieldIndices = new int[_rank + 1];
        StructType schema = featureValue.schema();
        for (int i = 0; i < _rank; i++) {
            _fieldIndices[i] = schema.fieldIndex(SchemaMetadataUtils.indexFieldName(i));
        }
        _fieldIndices[_rank] = schema.fieldIndex(SchemaMetadataUtils.valueFieldName());
        _cardinality = ((Seq<?>) _featureValue.get(_fieldIndices[_rank])).size();
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
        return _cardinality;
    }

    @Override
    public TensorIterator iterator() {
        return new FDSTensorIterator();
    }

    private class FDSTensorIterator implements TensorIterator {
        private int _index;
        private final Seq<?>[] _cachedArrays;

        FDSTensorIterator() {
            _index = 0;
            _cachedArrays = new Seq<?>[_rank + 1];
            start();
        }

        FDSTensorIterator(FDSTensorIterator original) {
            _index = original._index;
            _cachedArrays = original._cachedArrays;
        }

        @Override
        public TensorData getTensorData() {
            return FDSSparseTensorWrapper.this;
        }

        @Override
        public void start() {
            _index = 0;
            for (int i = 0; i <= _rank; i++) {
                _cachedArrays[i] = (Seq<?>) _featureValue.get(_fieldIndices[i]);
            }
        }

        @Override
        public boolean isValid() {
            return _index < _cardinality;
        }

        @Override
        public void next() {
            _index++;
        }

        @Override
        public TensorIterator getCopy() {
            return new FDSTensorIterator(this);
        }

        @Override
        public Object getValue(int column) {
            return _cachedArrays[column].apply(_index);
        }

        @Override
        public int getInt(int column) {
            return (int) getValue(column);
        }

        @Override
        public long getLong(int column) {
            return (long) getValue(column);
        }

        @Override
        public float getFloat(int column) {
            return (float) getValue(column);
        }

        @Override
        public String getString(int column) {
            return (String) getValue(column);
        }

        @Override
        public double getDouble(int column) {
            return (double) getValue(column);
        }

        @Override
        public boolean getBoolean(int column) {
            return (boolean) getValue(column);
        }

        @Override
        public byte[] getBytes(int column) {
            return (byte[]) getValue(column);
        }
    }
}
