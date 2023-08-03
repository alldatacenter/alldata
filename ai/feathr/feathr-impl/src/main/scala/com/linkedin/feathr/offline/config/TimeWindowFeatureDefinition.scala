package com.linkedin.feathr.offline.config

import java.time.Duration
import com.fasterxml.jackson.core.{JsonParser, TreeNode}
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.node.{IntNode, NumericNode, ObjectNode, TextNode}
import com.fasterxml.jackson.databind.{DeserializationContext, JsonDeserializer}
import com.linkedin.feathr.common.FeatureTypeConfig
import com.linkedin.feathr.common.exception.{ErrorLabel, FeathrConfigException}
import com.linkedin.feathr.offline.anchored.WindowTimeUnit
import com.linkedin.feathr.offline.config
import com.linkedin.feathr.offline.transformation.FeatureColumnFormat
import com.linkedin.feathr.offline.transformation.FeatureColumnFormat.FeatureColumnFormat
import com.linkedin.feathr.sparkcommon.ComplexAggregation
import com.linkedin.feathr.swj.SlidingWindowFeature
import com.linkedin.feathr.swj.aggregate.AggregationType

/**
 * representation of a sliding window aggregation feature optionally with complex aggregations
 * @param timeWindowFeatureDefinition config object of the feature
 * @param swaFeature sliding window aggregation definiton of the feature
 * @param complexAggregations complex aggregations of the feature
 */
private[feathr] case class ComplexAggregationFeature(
    timeWindowFeatureDefinition: TimeWindowFeatureDefinition,
    swaFeature: SlidingWindowFeature,
    complexAggregations: Option[Seq[ComplexAggregation]] = None)

/*
 * TimeWindowFeatureDefinition contains the information on the definition of time-window features,
 * including feature name and feature parameters
 */
@JsonDeserialize(using = classOf[TimeWindowFeatureDefinitionDeserializer])
case class TimeWindowFeatureDefinition(
    `def`: String,
    aggregationType: AggregationType.Value,
    window: Duration,
    groupBy: Option[String],
    limit: Option[Int],
    filter: Option[String],
    decay: Option[String],
    weight: Option[String],
    normalization: Option[String],
    decayAgg: Option[String],
    topKSetting: Option[String],
    embeddingSize: Option[Int],
    featureTypeConfig: Option[FeatureTypeConfig] = Option.empty,
    columnFormat: FeatureColumnFormat = FeatureColumnFormat.RAW)

class TimeWindowFeatureDefinitionDeserializer extends JsonDeserializer[TimeWindowFeatureDefinition] {
  override def deserialize(p: JsonParser, ctxt: DeserializationContext): TimeWindowFeatureDefinition = {
    val codec = p.getCodec
    val node = codec.readTree[TreeNode](p)
    val typeConfig = ConfigLoaderUtils.getTypeConfig(node)
    node match {
      case x: ObjectNode =>
        config.TimeWindowFeatureDefinition(
          node.get("def") match {
            case field: TextNode => field.textValue()
            case _ =>
              throw new FeathrConfigException(ErrorLabel.FEATHR_USER_ERROR, s"'def' field is required in aggregation feature but it's not provided in $node.")
          },
          node.get("aggregation") match {
            case field: TextNode =>
              try {
                AggregationType.withName(field.textValue())
              } catch {
                case e: NoSuchElementException =>
                  throw new FeathrConfigException(
                    ErrorLabel.FEATHR_USER_ERROR,
                    s"Trying to deserialize the config into TimeWindowFeatureDefinition." +
                      s"Aggregation type ${field.textValue()} is not supported. Supported types are: SUM, COUNT, AVG, MAX, TIMESINCE." +
                      "Please use a supported aggregation type.",
                    e)
              }
            case _ =>
              throw new FeathrConfigException(
                ErrorLabel.FEATHR_USER_ERROR,
                s"'aggregation' field is required in aggregation feature but is not provided in $node.")
          },
          node.get("window") match {
            case field: TextNode => WindowTimeUnit.parseWindowTime(field.textValue())
            case _ =>
              throw new FeathrConfigException(ErrorLabel.FEATHR_USER_ERROR, s"'window' field is required in aggregation feature but is not provided $node.")
          },
          node.get("groupBy") match {
            case field: TextNode => Option(field.textValue())
            case _ => None
          },
          node.get("limit") match {
            case field: IntNode => Option(field.intValue())
            case _ => None
          },
          node.get("filter") match {
            case field: TextNode => Option(field.textValue())
            case _ => None
          },
          node.get("decay") match {
            case field: TextNode => Option(field.textValue())
            case _ => None
          },
          node.get("weight") match {
            case field: TextNode => Option(field.textValue())
            case _ => None
          },
          node.get("normalization") match {
            case field: ObjectNode => Option(field.toString())
            case _ => None
          },
          node.get("advancedDecay") match {
            case field: ObjectNode => Option(field.toString())
            case _ => None
          },
          node.get("topK") match {
            case field: ObjectNode => Option(field.toString())
            case _ => None
          },
          node.get("embeddingSize") match {
            case field: NumericNode => Option(field.asInt())
            case _ => None
          }, typeConfig)
    }
  }
}
