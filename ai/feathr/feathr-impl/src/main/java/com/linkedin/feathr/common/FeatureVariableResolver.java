package com.linkedin.feathr.common;

import com.linkedin.feathr.common.tensor.Primitive;
import com.linkedin.feathr.common.tensor.TensorIterator;
import com.linkedin.feathr.common.types.ValueType;
import com.linkedin.feathr.common.util.CoercionUtils;
import org.mvel2.integration.impl.SimpleValueResolver;


/**
 * FeatureVariableResolver takes a FeatureValue object for member variable during MVEL expression evaluation,
 * and then resolve the value for that variable.
 */
public class FeatureVariableResolver extends SimpleValueResolver {
  private FeatureValue _featureValue;

  public FeatureVariableResolver(FeatureValue featureValue) {
    super(featureValue);
    _featureValue = featureValue;
  }

  @Override
  public Object getValue() {
    if (_featureValue == null) {
      return null;
    }

    switch (_featureValue.getFeatureType().getBasicType()) {
      case NUMERIC:
        return _featureValue.getAsNumeric();
      case TERM_VECTOR:
        return getValueFromTermVector();
      case BOOLEAN:
      case CATEGORICAL:
      case CATEGORICAL_SET:
      case DENSE_VECTOR:

      case TENSOR:
        return getValueFromTensor();

      default:
        throw new IllegalArgumentException("Unexpected feature type: " + _featureValue.getFeatureType().getBasicType());
    }
  }

  private Object getValueFromTensor() {
    TypedTensor tensor = _featureValue.getAsTypedTensor();
    int rank = tensor.getType().getDimensionTypes().size();

    /*
     * For scalar tensors, we will return the value, so that mvel expression can operate on the primitive value.
     * For all other tensors, we return the FeatureValue object. Presumably there'll be a UDF that knows how to
     * process the rank > 0 tensors.
     */
    if (rank == 0) {
      ValueType valueType = tensor.getType().getValueType();
      Primitive primitive = valueType.getRepresentation();

      TensorIterator iterator = tensor.getData().iterator();
      iterator.start();
      int colIndex = 0;     // this is a scalar, hence only one column

      switch (primitive) {
        case DOUBLE:
          return iterator.getDouble(colIndex);

        case FLOAT:
          return iterator.getFloat(colIndex);

        case INT:
          return iterator.getInt(colIndex);

        case LONG:
          return iterator.getLong(colIndex);

        default:
          // consistent with non-tensor type mapping, there will be no conversion for the rest types
          return _featureValue;
      }
    } else {
      return _featureValue;
    }
  }

  private Object getValueFromTermVector() {
    /*
     * If the variable represents a NUMERIC feature, we unpack the term vector to a float number,
     * so that MVEL could directly do math operations on the value. For all other features, the
     * FeatureValue object is returned. Presumably there'll be a UDF that knows how to process
     * this object.
     */
    if (CoercionUtils.isNumeric(_featureValue)) {
      return _featureValue.getAsNumeric();
    } else {
      return _featureValue;
    }
  }
}
