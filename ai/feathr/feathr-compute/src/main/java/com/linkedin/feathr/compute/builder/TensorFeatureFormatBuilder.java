package com.linkedin.feathr.compute.builder;

import com.linkedin.feathr.compute.DimensionArray;
import com.linkedin.feathr.compute.TensorCategory;
import com.linkedin.feathr.compute.TensorFeatureFormat;
import com.linkedin.feathr.compute.ValueType;


/**
 * Builder class that builds {@link TensorFeatureFormat} pegasus object, which define the format of feature data. It
 * unifies frame feature type (https://iwww.corp.linkedin.com/wiki/cf/display/ENGS/Feature+Representation+and+Feature+Type+System)
 * and Quince Tensor type (https://iwww.corp.linkedin.com/wiki/cf/display/ENGS/Frame+Tensor+Tutorial).
 */
public abstract class TensorFeatureFormatBuilder {
  public TensorFeatureFormat build() {
    validCheck();
    TensorFeatureFormat tensorFeatureFormat = new TensorFeatureFormat();
    tensorFeatureFormat.setValueType(buildValueType());
    tensorFeatureFormat.setDimensions(buildDimensions());
    tensorFeatureFormat.setTensorCategory(buildTensorCategory());
    return tensorFeatureFormat;
  }

  /**
   * build {@link ValueType} pegasus object that defines type of the value column.
   */
  abstract ValueType buildValueType();

  /**
   * build {@link DimensionArray}. A tensor can have 0 to n dimensions. Each element of this array represent the
   * attributes of one dimension. For scalar (rank-0) scalar, this should return an empty array.
   */
  abstract DimensionArray buildDimensions();

  /**
   * build {@link TensorCategory}, which defines the type of tensor, for example, dense tensor.
   */
  abstract TensorCategory buildTensorCategory();

  /**
   * Valid the arguments passed in from subclass constructor, to make sure a valid {@link TensorFeatureFormat} can be
   * built.
   */
  abstract void validCheck();
}
