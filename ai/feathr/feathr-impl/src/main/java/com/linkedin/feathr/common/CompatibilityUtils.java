package com.linkedin.feathr.common;

import com.linkedin.feathr.common.tensor.Primitive;
import com.linkedin.feathr.common.tensor.TensorData;
import com.linkedin.feathr.common.tensor.TensorIterator;
import com.linkedin.feathr.common.tensor.TensorType;
import com.linkedin.feathr.common.types.FeatureType;
import com.linkedin.feathr.common.types.TensorFeatureType;
import com.linkedin.feathr.common.value.*;
import com.linkedin.feathr.common.value.FeatureValue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/**
 * Utilities for enabling the old FeatureValue API to layer around the new FeatureValue API while keeping compatibility.
 */
class CompatibilityUtils {
  private static final String EMPTY_TERM = "";
  private static final float UNIT = 1.0f;
  private static final Map<String, Float> NTV_BOOLEAN_TRUE = Collections.singletonMap(EMPTY_TERM, UNIT);
  // no need for an NTV_BOOLEAN_FALSE because it is empty-map, and we test for that case using Map.isEmpty.

  private CompatibilityUtils() {
  }

  /**
   * Get a boolean from a FeatureValue following the legacy NTV conversion rules. E.g. this works even when the
   * FeatureValue is not boolean nor even a scalar-tensor, but a rank-1 term-vector that "looks like a boolean"
   * according to Feathr's convention under NTV.
   * Specifically, empty-map {@code {}} means false, and {@code {"": 1.0f}} means true.
   *
   * This function requires the FeatureValue to be one of: a boolean, a term-vector that looks like a boolean according
   * to the legacy rules, or a scalar-boolean-tensor.
   */
  static boolean getBoolean(FeatureValue featureValue) {
    if (featureValue instanceof BooleanFeatureValue) {
      return ((BooleanFeatureValue) featureValue).getBooleanValue();
    } else if (featureValue instanceof TermVectorFeatureValue) {
      Map<String, Float> tv = ((TermVectorFeatureValue) featureValue).getTermVector();
      if (tv.isEmpty()) {
        return false;
      } else if (tv.equals(NTV_BOOLEAN_TRUE)) {
        return true;
      }
    } else if (featureValue instanceof TensorFeatureValue) {
      TensorData tensor = ((TensorFeatureValue) featureValue).getAsTensor();
      // "if this tensor is a scalar and its value type is boolean"
      if (tensor.getTypes().length == 1 && tensor.getTypes()[0].getRepresentation() == Primitive.BOOLEAN) {
        // return the first (only) boolean value in the tensor
        return tensor.iterator().getBoolean(0);
      }
    }
    throw new IllegalArgumentException("Couldn't convert " + featureValue + " to boolean");
  }

  /**
   * Get a float from a FeatureValue following the legacy NTV conversion rules. E.g. this works even when the
   * FeatureValue is not numeric nor even a scalar, but a rank-1 term-vector that "looks like a numeric" according
   * to Feathr's convention under NTV.
   *
   * This function requires the FeatureValue to be one of: a numeric, a term-vector that looks like a numeric according
   * to the legacy rules, or a scalar-numeric-tensor.
   */
  static float getNumeric(FeatureValue featureValue) {
    if (featureValue instanceof NumericFeatureValue) {
      return ((NumericFeatureValue) featureValue).getFloatValue();
    } else if (featureValue instanceof TermVectorFeatureValue) {
      Map<String, Float> tv = ((TermVectorFeatureValue) featureValue).getTermVector();
      if (tv.size() == 1) {
        Float floatValue = tv.get(EMPTY_TERM);
        if (floatValue != null) {
          return floatValue;
        }
      }
    } else if (featureValue instanceof TensorFeatureValue) {
      TensorData tensor = ((TensorFeatureValue) featureValue).getAsTensor();
      if (tensor.getTypes().length == 1) { // "if this is tensor is a scalar"
        return tensor.getTypes()[0].getRepresentation().toFloat(tensor.iterator(), 0);
      }
    }
    throw new IllegalArgumentException("Couldn't convert " + featureValue + " to numeric");
  }

  /**
   * Get a categorical-string from a FeatureValue following the legacy NTV conversion rules.
   *
   * This function requires the FeatureValue to be one of: a categorical, a term-vector that looks like a categorical
   * according to the legacy rules, or a TENSOR[STRING]:FLOAT that looks like a single-categorical (contains only one
   * mapping). For future-proofing for text features which needed to be modelled as categorical in the legacy NTV API,
   * this function also supports the case where the tensor is a scalar-string-tensor.
   */
  static String getCategoricalString(FeatureValue featureValue) {
    if (featureValue instanceof CategoricalFeatureValue) {
      return ((CategoricalFeatureValue) featureValue).getStringValue();
    } else if (featureValue instanceof TermVectorFeatureValue) {
      Map<String, Float> tv = ((TermVectorFeatureValue) featureValue).getTermVector();
      Iterator<Map.Entry<String, Float>> iterator = tv.entrySet().iterator();
      if (iterator.hasNext()) {
        Map.Entry<String, Float> onlyEntry = iterator.next();
        if (onlyEntry.getValue() == 1.0f && !iterator.hasNext()) {
          return onlyEntry.getKey();
        }
      }
    } else if (featureValue instanceof TensorFeatureValue) {
      TensorData tensorData = ((TensorFeatureValue) featureValue).getAsTensor();
      if (tensorData.cardinality() == 1 // "if there is only one 'mapping'/entry in this tensor"
          && tensorData.getArity() == 2 // "and it has one dimension (arity == 2) whose type is STRING"
          && tensorData.getTypes()[0].getRepresentation().equals(Primitive.STRING)) {
        TensorIterator iter = tensorData.iterator();
        // "if the single mapping's value is UNIT, as is the expectation for categorical features"
        if (tensorData.getTypes()[1].getRepresentation().toFloat(iter, 1) == UNIT) {
          return iter.getString(0);
        }
      } else if (tensorData.getArity() == 1 && tensorData.getTypes()[0].getRepresentation().equals(Primitive.STRING)) {
        // "if this is a scalar-string-tensor"
        return tensorData.iterator().getString(0);
      }
    }
    throw new IllegalArgumentException("Couldn't convert " + featureValue + " to categorical string");
  }

  /**
   * A TermVectorFeatureValue that is allowed to be mutable for legacy compatibility reasons.
   * The Map provided at construction can be modified elsewhere, causing this instance's value to change.
   */
  static class MutableTermVectorFeatureValue extends TermVectorFeatureValue {
    MutableTermVectorFeatureValue(Map<String, Float> termVector) {
      super(ensureMutable(termVector));
    }

    Map<String, Float> getMutableView() {
      return _termVector;
    }

    @Override
    public String toString() {
      return "MutableTermVectorFeatureValue{" + "_termVector=" + _termVector + '}';
    }
  }

  private static <K, V> HashMap<K, V> ensureMutable(Map<K, V> map) {
    if (map instanceof HashMap) {
      return (HashMap<K, V>) map;
    } else {
      return new HashMap<>(map);
    }
  }

  /**
   * A FeatureFormatMapper that supports Quince TypedTensor instead of the more commonly used TensorData.
   * Some legacy users of Feathr's older tensors-API expected to be able to use arbitrary subtypes of TypedTensor.
   * This mapper (along with special handling currently in TensorFeatureValue) make it possible to support this.
   */
  static class TypedTensorFormatMapper implements FeatureFormatMapper<TypedTensor> {
    static final TypedTensorFormatMapper INSTANCE = new TypedTensorFormatMapper();

    private TypedTensorFormatMapper() {
    }

    @Override
    public TypedTensor fromFeatureValue(FeatureValue featureValue) {
      if (featureValue instanceof TensorFeatureValue) {
        return ((TensorFeatureValue) featureValue).getAsTypedTensor();
      } else {
        TensorData tensorData = QuinceFeatureFormatMapper.INSTANCE.fromFeatureValue(featureValue);
        TensorType tensorType = QuinceFeatureTypeMapper.INSTANCE.fromFeatureType(featureValue.getFeatureType());
        return new GenericTypedTensor(tensorData, tensorType);
      }
    }

    @Override
    public FeatureValue toFeatureValue(FeatureType featureType, TypedTensor typedTensor) {
      if (featureType instanceof TensorFeatureType) {
        if (((TensorFeatureType) featureType).getTensorType().equals(typedTensor.getType())) {
          throw new IllegalArgumentException("Tensor " + typedTensor + " type did not match type " + featureType);
        }
        return TensorFeatureValue.fromTypedTensor(typedTensor);
      } else {
        return QuinceFeatureFormatMapper.INSTANCE.toFeatureValue(featureType, typedTensor.getData());
      }
    }
  }
}
