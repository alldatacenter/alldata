package com.linkedin.feathr.common.value;

import com.linkedin.feathr.common.*;
import com.linkedin.feathr.common.tensor.*;
import com.linkedin.feathr.common.tensor.scalar.ScalarBooleanTensor;
import com.linkedin.feathr.common.tensor.scalar.ScalarFloatTensor;
import com.linkedin.feathr.common.tensorbuilder.DenseTensorBuilder;
import com.linkedin.feathr.common.tensorbuilder.UniversalTensorBuilder;
import com.linkedin.feathr.common.types.*;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;

/**
 * A FeatureFormatMapper that can represent any FeatureValue as a Quince TensorData.
 */
public class QuinceFeatureFormatMapper extends AbstractFeatureFormatMapper<TensorData> {
    private static final float UNIT = 1.0f;
    public static final QuinceFeatureFormatMapper INSTANCE = new QuinceFeatureFormatMapper();

    private QuinceFeatureFormatMapper() {
    }

    @Override
    protected TensorData fromNumericFeatureValue(NumericFeatureValue featureValue) {
        return new ScalarFloatTensor(featureValue.getFloatValue());
    }

    @Override
    protected TensorData fromBooleanFeatureValue(BooleanFeatureValue featureValue) {
        return new ScalarBooleanTensor(featureValue.getBooleanValue());
    }

    private static final Representable[] CATEGORICAL_QUINCE_COLUMN_TYPES =
            QuinceFeatureTypeMapper.INSTANCE.fromFeatureType(CategoricalFeatureType.INSTANCE).getColumnTypes();
    @Override
    protected TensorData fromCategoricalFeatureValue(CategoricalFeatureValue featureValue) {
        return new LOLTensorData(CATEGORICAL_QUINCE_COLUMN_TYPES,
                singletonList(singletonList(featureValue.getStringValue())), singletonList(1.0f));
    }

    private static final Representable[] CATEGORICAL_SET_QUINCE_COLUMN_TYPES =
            QuinceFeatureTypeMapper.INSTANCE.fromFeatureType(CategoricalSetFeatureType.INSTANCE).getColumnTypes();
    private static final TensorData EMPTY_CATEGORICAL_SET_TENSOR =
            new LOLTensorData(CATEGORICAL_SET_QUINCE_COLUMN_TYPES, singletonList(new ArrayList<String>()), new ArrayList<Float>());
    @Override
    protected TensorData fromCategoricalSetFeatureValue(CategoricalSetFeatureValue featureValue) {
        List<String> indices = new ArrayList<>(featureValue.getStringSet());
        if (indices.isEmpty()) {
            return EMPTY_CATEGORICAL_SET_TENSOR;
        }
        // the values list needs to be mutable, per undocumented requirement in LOLTensorData constructor...
        List<Float> values = new ArrayList<>(Collections.nCopies(indices.size(), UNIT));
        return new LOLTensorData(CATEGORICAL_SET_QUINCE_COLUMN_TYPES, singletonList(indices), values);
    }

    private static final Representable[] TERM_VECTOR_QUINCE_COLUMN_TYPES =
            QuinceFeatureTypeMapper.INSTANCE.fromFeatureType(TermVectorFeatureType.INSTANCE).getColumnTypes();
    private static final TensorData EMPTY_UNIVERSAL_TENSOR =
            new UniversalTensorBuilder(TERM_VECTOR_QUINCE_COLUMN_TYPES).build();
    @Override
    protected TensorData fromTermVectorFeatureValue(TermVectorFeatureValue featureValue) {
        Map<String, Float> termVector = featureValue.getTermVector();

        // if feature value is empty, always return a singleton empty TensorData
        if (termVector.isEmpty()) {
            return EMPTY_UNIVERSAL_TENSOR;
        }

        UniversalTensorBuilder builder = new UniversalTensorBuilder(TERM_VECTOR_QUINCE_COLUMN_TYPES);
        builder.start(termVector.size());
        termVector.forEach((term, value) -> {
            builder.setString(0, term);
            builder.setFloat(1, value);
            builder.append();
        });
        return builder.build();
    }

    private static final Representable[] TENSOR_INT_FLOAT_TYPE = new Representable[]{Primitive.INT, Primitive.FLOAT};
    private static final TensorData EMPTY_DENSE_VECTOR_TENSOR =
            new DenseTensorBuilder(TENSOR_INT_FLOAT_TYPE, new long[]{-1}).build(new float[0]);
    @Override
    protected TensorData fromDenseVectorFeatureValue(DenseVectorFeatureValue featureValue) {
        float[] floatArray = featureValue.getFloatArray();

        if (floatArray.length == 0) {
            return EMPTY_DENSE_VECTOR_TENSOR;
        }

        TensorType tensorType = QuinceFeatureTypeMapper.INSTANCE.fromFeatureType(featureValue.getFeatureType());
        return new DenseTensorBuilder(tensorType.getColumnTypes(), TensorUtils.getShape(tensorType)).build(floatArray);
    }

    @Override
    protected TensorData fromTensorFeatureValue(TensorFeatureValue featureValue) {
        return featureValue.getAsTensor();
    }

    @Override
    protected NumericFeatureValue toNumericFeatureValue(NumericFeatureType featureType, TensorData tensor) {
        if (tensor.getArity() != 1) {
            throw new IllegalArgumentException("Not a scalar: " + getDebugString(tensor));
        } else {
            float floatValue = tensor.getTypes()[0].getRepresentation().toFloat(tensor.iterator(), 0);
            return NumericFeatureValue.fromFloat(floatValue);
        }
    }

    @Override
    protected BooleanFeatureValue toBooleanFeatureValue(BooleanFeatureType featureType, TensorData tensor) {
        if (tensor.getArity() != 1) {
            throw new IllegalArgumentException("Not a scalar: " + getDebugString(tensor));
        } else {
            boolean booleanValue = tensor.getTypes()[0].getRepresentation().toBoolean(tensor.iterator(), 0);
            return BooleanFeatureValue.fromBoolean(booleanValue);
        }
    }

    @Override
    protected CategoricalFeatureValue toCategoricalFeatureValue(CategoricalFeatureType featureType, TensorData tensor) {
        if ((tensor.getArity() == 2 && tensor.cardinality() == 1)
                || (tensor.getArity() == 1 && tensor.getTypes()[0].equals(Primitive.STRING))) {
            // incidentally the API call is the same whether this is a rank-1 categorical or rank-0 string-scalar...
            String stringValue = tensor.getTypes()[0].getRepresentation().toString(tensor.iterator(), 0);
            return CategoricalFeatureValue.fromString(stringValue);
        } else {
            throw new RuntimeException("Tensor doesn't represent a categorical: " + getDebugString(tensor));
        }
    }

    @Override
    protected CategoricalSetFeatureValue toCategoricalSetFeatureValue(CategoricalSetFeatureType featureType,
                                                                      TensorData tensor) {
        List<String> value = new ArrayList<>(tensor.cardinality());
        TensorIterator iter = tensor.iterator();
        Primitive rep = tensor.getTypes()[0].getRepresentation();
        while (iter.isValid()) {
            value.add(rep.toString(iter, 0));
            iter.next();
        }
        return CategoricalSetFeatureValue.fromStrings(value);
    }

    @Override
    protected TermVectorFeatureValue toTermVectorFeatureValue(TermVectorFeatureType featureType, TensorData tensor) {
        Map<String, Float> termVector = new HashMap<>(tensor.cardinality());
        Representable[] columnTypes = tensor.getTypes();
        TensorIterator iter = tensor.iterator();
        Primitive termRep = columnTypes[0].getRepresentation();
        Primitive valueRep = columnTypes[1].getRepresentation();
        while (iter.isValid()) {
            String term = termRep.toString(iter, 0);
            Float value = valueRep.toFloat(iter, 1);
            termVector.put(term, value);
            iter.next();
        }
        return TermVectorFeatureValue.fromMap(termVector);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected DenseVectorFeatureValue toDenseVectorFeatureValue(DenseVectorFeatureType featureType, TensorData tensor) {
        Primitive valueType = tensor.getTypes()[tensor.getArity() - 1].getRepresentation();
        if (tensor instanceof DenseTensor
                && tensor.getArity() == 2 // rank == 1
                && (valueType.equals(Primitive.FLOAT)
                || valueType.equals(Primitive.DOUBLE)
                || valueType.equals(Primitive.INT)
                || valueType.equals(Primitive.LONG))) {
            // Unchecked cast is OK because DenseTensor.asList always returns a Number when its value type is a Number.
            return DenseVectorFeatureValue.fromNumberList((List<? extends Number>) ((DenseTensor) tensor).asList());
        } else {
            throw new IllegalArgumentException("Not a DenseTensor with rank=1 and numeric value type: " + getDebugString(tensor));
        }
    }

    @Override
    protected TensorFeatureValue toTensorFeatureValue(TensorFeatureType featureType, TensorData externalValue) {
        return TensorFeatureValue.fromTensorData(featureType, externalValue);
    }

    private static String getDebugString(TensorData tensorData) {
        // synthesize a TensorType for this TensorData.
        Representable[] columnTypes = tensorData.getTypes();
        PrimitiveType valueType = new PrimitiveType(columnTypes[columnTypes.length - 1].getRepresentation());
        List<DimensionType> dimensionTypes = Arrays.asList(columnTypes).subList(0, columnTypes.length - 1).stream()
                .map(representable -> new PrimitiveDimensionType(representable.getRepresentation()))
                .collect(Collectors.toList());
        TensorType tensorType = new TensorType(valueType, dimensionTypes);
        return "{" + TensorUtils.getDebugString(tensorType, tensorData, TensorUtils.DEFAULT_MAX_STRING_LEN) + "}";
    }
}
