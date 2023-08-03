package com.linkedin.feathr.sparkcommon

import com.linkedin.feathr.common.FeatureDerivationFunctionBase
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.DataFrame
/**
  * Spark DataFrame based derived feature base class, Feathr user can extend this class to implement their
  * customized derivation function to produce derived features
  *
  * see [[TestDataFrameDerivationFunctionExtractor]] in feathr repo for example
  */
abstract class FeatureDerivationFunctionSpark extends FeatureDerivationFunctionBase {
  /**
    * Append the derived feature column, the column name should be the same as declared in the Feathr featureDef file
    * E.g, in the following TestDataFrameDerivationFunctionExtractor (extends FeatureDerivationFunctionSpark),
    * the input dataframe will contain dependent feature columns "f1" and "f2", output DataFrame should contain the
    * output column 'E'
    derivations: {
      E: {
       key: [x, y]
       inputs: {
         f1:  { key: x, feature: C },
         f2:  { key: y, feature: D }
       }
       class: "com.linkedin.feathr.offline.TestDataFrameDerivationFunctionExtractor"
     }
    *
    * @param dataframe  context DataFrame with all dependency feature columns ready to use, the input columns could be
    *                   of any valid [[org.apache.spark.sql.types.DataType]]
    * @return the transformed DataFrame
    */
  def transform(dataframe: DataFrame): DataFrame

  /**
    * Check the validity of the input DataFrame, raise an exception if the schema is invalid,
    * e.g, does not contain required input columns or has incorrect column types
    * It is the developer's responsibility to validate the input schema's validity
    * @param schema
    */
  def validateInputSchema(schema: StructType): Unit = {}
}
