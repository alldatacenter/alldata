package com.linkedin.feathr.common.value;

import com.linkedin.feathr.common.CoercingTensorData;
import com.linkedin.feathr.common.tensor.Primitive;
import com.linkedin.feathr.common.tensor.TensorData;
import com.linkedin.feathr.common.tensor.TensorIterator;
import com.linkedin.feathr.common.types.BooleanFeatureType;
import com.linkedin.feathr.common.types.CategoricalSetFeatureType;
import com.linkedin.feathr.common.types.CategoricalFeatureType;
import com.linkedin.feathr.common.types.DenseVectorFeatureType;
import com.linkedin.feathr.common.types.NumericFeatureType;
import com.linkedin.feathr.common.types.TensorFeatureType;
import com.linkedin.feathr.common.types.TermVectorFeatureType;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * A FeatureFormatMapper that converts in and out of NTV representation, using Feathr's legacy rules for representing
 * types like NUMERIC, BOOLEAN, and DENSE_VECTOR as term vectors.
 */
public class NTVFeatureFormatMapper extends AbstractFeatureFormatMapper<Map<String, Float>> {
  private static final String EMPTY_TERM = "";
  private static final float UNIT = 1.0f;

  public static final NTVFeatureFormatMapper INSTANCE = new NTVFeatureFormatMapper();

  private NTVFeatureFormatMapper() {
  }

  @Override
  protected Map<String, Float> fromNumericFeatureValue(NumericFeatureValue featureValue) {
    return Collections.singletonMap(EMPTY_TERM, featureValue.getFloatValue());
  }

  private static final Map<String, Float> NTV_BOOLEAN_FALSE = Collections.emptyMap();
  private static final Map<String, Float> NTV_BOOLEAN_TRUE = Collections.singletonMap(EMPTY_TERM, UNIT);
  private static final Map<String, Float> NTV_EMPTY = Collections.emptyMap();
  @Override
  protected Map<String, Float> fromBooleanFeatureValue(BooleanFeatureValue featureValue) {
    return featureValue.getBooleanValue() ? NTV_BOOLEAN_TRUE : NTV_BOOLEAN_FALSE;
  }

  @Override
  protected Map<String, Float> fromCategoricalFeatureValue(CategoricalFeatureValue featureValue) {
    return Collections.singletonMap(featureValue.getStringValue(), UNIT);
  }

  @Override
  protected Map<String, Float> fromCategoricalSetFeatureValue(CategoricalSetFeatureValue featureValue) {
    return Collections.unmodifiableMap(
        featureValue.getStringSet().stream()
            .collect(Collectors.toMap(Function.identity(), x -> UNIT)));
  }

  @Override
  protected Map<String, Float> fromTermVectorFeatureValue(TermVectorFeatureValue featureValue) {
    return featureValue.getTermVector();
  }

  @Override
  protected Map<String, Float> fromDenseVectorFeatureValue(DenseVectorFeatureValue featureValue) {
    float[] floatArray = featureValue.getFloatArray();
    return Collections.unmodifiableMap(
        IntStream.range(0, floatArray.length).boxed()
            .collect(Collectors.toMap(String::valueOf, i -> floatArray[i])));
  }

  // For any other feature type, if reformulated as a TensorFeatureValue, does it produce the same NTV output?
  // Should that be part of the contract?
  @Override
  protected Map<String, Float> fromTensorFeatureValue(TensorFeatureValue featureValue) {
    TensorData tensorData = featureValue.getAsTensor();
    // Return empty NTV for null tensors to help legacy users.
    if (tensorData == null) {
      return NTV_EMPTY;
    }
    switch (tensorRank(tensorData)) {
      case 0:
        Primitive scalarRepresentation = tensorData.getTypes()[0].getRepresentation();
        switch (scalarRepresentation) {
          case INT:
          case LONG:
          case FLOAT:
          case DOUBLE:
            float floatValue = scalarRepresentation.toFloat(tensorData.iterator(), 0);
            return Collections.singletonMap(EMPTY_TERM, floatValue);
          case BOOLEAN:
            boolean booleanValue = tensorData.iterator().getBoolean(0);
            return booleanValue ? NTV_BOOLEAN_TRUE : NTV_BOOLEAN_FALSE;
          case STRING:
            // we don't support this for categorical yet, but plan to support it soon for TEXT/STRING type, so why not?
            String stringValue = tensorData.iterator().getString(0);
            return Collections.singletonMap(stringValue, UNIT);
          default:
            throw new IllegalArgumentException("Unhandled scalar type " + scalarRepresentation + " in " + tensorData);
        }
      case 1:
        TensorData coercingTensor = new CoercingTensorData(tensorData);
        Map<String, Float> termVector = new HashMap<>(tensorData.cardinality());
        TensorIterator iterator = coercingTensor.iterator();
        while (iterator.isValid()) {
          termVector.put(iterator.getString(0), iterator.getFloat(1));
          iterator.next();
        }
        return Collections.unmodifiableMap(termVector);
      default:
        throw new IllegalArgumentException("Cannot convert a tensor of rank > 1 to NTV for feature value " + featureValue);
    }
  }

  @Override
  protected NumericFeatureValue toNumericFeatureValue(NumericFeatureType featureType, Map<String, Float> ntv) {
    if (ntv.size() == 1) {
      Float floatValue = ntv.get(EMPTY_TERM);
      if (floatValue != null) {
        return NumericFeatureValue.fromFloat(floatValue);
      }
    }
    throw cannotConvertToFeatureValue(featureType, ntv);
  }

  @Override
  protected BooleanFeatureValue toBooleanFeatureValue(BooleanFeatureType featureType, Map<String, Float> ntv) {
    if (ntv.isEmpty()) {
      return BooleanFeatureValue.FALSE;
    } else if (ntv.equals(NTV_BOOLEAN_TRUE)) {
      return BooleanFeatureValue.TRUE;
    }
    throw cannotConvertToFeatureValue(featureType, ntv);
  }

  @Override
  protected CategoricalFeatureValue toCategoricalFeatureValue(CategoricalFeatureType featureType,
      Map<String, Float> ntv) {
    Iterator<Map.Entry<String, Float>> iterator = ntv.entrySet().iterator();
    if (iterator.hasNext()) {
      Map.Entry<String, Float> onlyEntry = iterator.next();
      if (onlyEntry.getValue() == UNIT && !iterator.hasNext()) {
        String stringValue = onlyEntry.getKey();
        return CategoricalFeatureValue.fromString(stringValue);
      }
    }
    throw cannotConvertToFeatureValue(featureType, ntv);
  }

  @Override
  protected CategoricalSetFeatureValue toCategoricalSetFeatureValue(CategoricalSetFeatureType featureType,
      Map<String, Float> ntv) {
    if (ntv.values().stream().allMatch(x -> x == UNIT)) { // should we have an "unchecked mode" that skips expensive validation?
      return CategoricalSetFeatureValue.fromStrings(ntv.keySet());
    }
    throw cannotConvertToFeatureValue(featureType, ntv);
  }

  @Override
  protected TermVectorFeatureValue toTermVectorFeatureValue(TermVectorFeatureType featureType, Map<String, Float> ntv) {
    return TermVectorFeatureValue.fromMap(ntv);
  }

  @Override
  protected DenseVectorFeatureValue toDenseVectorFeatureValue(DenseVectorFeatureType featureType, Map<String, Float> ntv) {
    Exception exception = null;
    int n = ntv.size();
    if (n == 0) {
      return DenseVectorFeatureValue.fromFloatArray(new float[0]);
    } else {
      int[] indices = new int[n];
      float[] values = new float[n];
      int i = 0;
      try {
        // The term-vector map contains a "sparsified" representation of the dense-vector.
        // Example: Input term-vector: {"0": 0.5, "1": 0.7, "3": 0.9}
        //          Output dense-vector: [0.5, 0.7, 0.0, 0.9]
        // Note that the map entries in the term vector may appear in any order, and any missing integer keys
        // should be interpreted as zero since this is a sparse representation.
        for (Map.Entry<String, Float> entry : ntv.entrySet()) {
          indices[i] = Integer.parseInt(entry.getKey());
          values[i] = entry.getValue();
          i++;
        }
        // Since we currently don't support static/fixed size dense-vector, we need to "discover" the dense size
        // by looking for the maximum index value observed in the sparse-representation (the term-vector).
        // Add one to the max observed value, otherwise the last entry will not fit in returned dense array.
        int denseSize = Arrays.stream(indices).max().getAsInt() + 1;
        float[] denseArray = new float[denseSize];
        for (i = 0; i < n; i++) {
          denseArray[indices[i]] = values[i];
        }
        return DenseVectorFeatureValue.fromFloatArray(denseArray);
      } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
        exception = e;
      }
    }
    throw cannotConvertToFeatureValue(featureType, ntv, exception);
  }

  @Override
  protected TensorFeatureValue toTensorFeatureValue(TensorFeatureType featureType, Map<String, Float> ntv) {
    throw cannotConvertToFeatureValue(featureType, ntv, new UnsupportedOperationException());
  }

  private static int tensorRank(TensorData tensorData) {
    return tensorData.getArity() - 1;
  }
}
