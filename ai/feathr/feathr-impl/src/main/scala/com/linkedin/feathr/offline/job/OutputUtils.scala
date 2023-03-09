package com.linkedin.feathr.offline.job

import com.linkedin.avroutil1.compatibility.AvroCompatibilityHelper
import com.linkedin.feathr.common.{Header, JoiningFeatureParams}
import org.apache.avro.Schema

import java.util

private[offline] object OutputUtils {

  /**
   * For a given sequence of tagged feature names, figure out what their String representation should be when we write them
   * in the output data set.
   *
   * Much of the time, the String representation will just be the name of the feature. For example:
   *
   * x:a_x => "a_x"
   *
   * The exception is when this would produce duplicate mappings. Since the mapping must be one-to-one, we must deconflict
   * in cases like the following:
   *
   * viewerId:a_x => "viewerId__a_x"
   * vieweeId:a_x => "vieweeId__a_x"
   *
   * @param requestedFeatures Seq of key-tagged feature names
   * @return Map of the input feature names to their String forms
   */
  def getStringificationTable(joinFeatures: Seq[JoiningFeatureParams]): Map[JoiningFeatureParams, String] = {
    val result = joinFeatures
      .groupBy(_.featureName)
      .values
      .flatMap { x =>
        assert(x.nonEmpty)
        if (x.size == 1) List(x.head -> x.head.featureName)
        else x.map(y => y -> (y.keyTags.mkString("_") + "__" + y.featureName))
      }
      .toMap
    assert(result.size == joinFeatures.size)
    result
  }

  // Quince-based FDS will be the only format we support.
  val OUTPUT_FORMAT_FDS = "quince-fds"

  type KeyTag = Seq[String]
  type FeatureName = String

  // defined as val rather than def with params for performance
  val compactFeatureSchemaFloat = {
    val schema = Schema.createRecord("Feature", null, null, false)
    schema.setFields(util.Arrays
      .asList(AvroCompatibilityHelper.createSchemaField("term", Schema.create(Schema.Type.STRING), null, null), AvroCompatibilityHelper.createSchemaField("value", Schema.create(Schema.Type.FLOAT), null, null)))
    schema
  }

  // defined as val rather than def with params for performance
  val compactFeatureSchemaDouble = {
    val schema = Schema.createRecord("Feature", null, null, false)
    schema.setFields(
      util.Arrays.asList(
        AvroCompatibilityHelper.createSchemaField("term", Schema.create(Schema.Type.STRING), null, null),
        AvroCompatibilityHelper.createSchemaField("value", Schema.create(Schema.Type.DOUBLE), null, null)))
    schema
  }

  // defined as val rather than def with params for performance
  val featureSchemaFloat = {
    val schema = Schema.createRecord("Feature", null, null, false)
    schema.setFields(
      util.Arrays.asList(
        AvroCompatibilityHelper.createSchemaField("name", Schema.create(Schema.Type.STRING), null, null),
        AvroCompatibilityHelper.createSchemaField("term", Schema.create(Schema.Type.STRING), null, null),
        AvroCompatibilityHelper.createSchemaField("value", Schema.create(Schema.Type.FLOAT), null, null)))
    schema
  }

  // defined as val rather than def with params for performance
  val featureSchemaDouble = {
    val schema = Schema.createRecord("Feature", null, null, false)
    schema.setFields(
      util.Arrays.asList(
        AvroCompatibilityHelper.createSchemaField("name", Schema.create(Schema.Type.STRING), null, null),
        AvroCompatibilityHelper.createSchemaField("term", Schema.create(Schema.Type.STRING), null, null),
        AvroCompatibilityHelper.createSchemaField("value", Schema.create(Schema.Type.DOUBLE), null, null)))
    schema
  }

  /**
   * Create the compact-name-term-value schema.
   */
  def compactFeatureSchema(useFloatInNTV: Boolean): Schema = {
    if (!useFloatInNTV) compactFeatureSchemaDouble else compactFeatureSchemaFloat
  }

  def compactFeatureListSchema(useFloatInNTV: Boolean): Schema = Schema.createArray(compactFeatureSchema(useFloatInNTV))

  /**
   * Create the name-term-value schema.
   */
  def featureSchema(useFloatInNTV: Boolean): Schema = {
    if (!useFloatInNTV) featureSchemaDouble else featureSchemaFloat
  }

  def featureListSchema(useFloatInNTV: Boolean): Schema = Schema.createArray(featureSchema(useFloatInNTV))

  // merge a sequence of headers
  def mergeHeaders(headers: Seq[Header]): Header = {
    val headerInfo = headers.flatMap(_.featureInfoMap).distinct.toMap
    new Header(headerInfo)
  }
}
