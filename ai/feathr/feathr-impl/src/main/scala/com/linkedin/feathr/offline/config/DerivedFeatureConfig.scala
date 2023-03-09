package com.linkedin.feathr.offline.config

import com.fasterxml.jackson.annotation.JsonProperty
import com.linkedin.feathr.common.FeatureTypes


private[offline] case class TaggedDependency(@JsonProperty("key") key: Seq[String], @JsonProperty("feature") feature: String) {
  require(key != null)
  require(key.nonEmpty)
  require(feature != null) // there's probably some way to set up the Jackson ObjectMapper differently such that we don't need to say these things
}

/**
 * Special case of taggedDependency for SeqJoin base feature. It takes in a transformation expression and an output key tag.
 * OutputKey and transformation expressions are made optional to allow for backward compatibility.
 */
private[offline] case class BaseTaggedDependency(
    @JsonProperty("key") key: Seq[String],
    @JsonProperty("feature") feature: String,
    @JsonProperty("outputKey") outputKey: Option[Seq[String]],
    @JsonProperty("transformation") transformation: Option[String]) {
  require(key != null)
  require(key.nonEmpty)
  require(feature != null)
}

private[offline] case class DerivedFeatureConfig(
    key: Seq[String],
    inputs: Map[String, TaggedDependency],
    definition: Option[String],
    SQLExpr: Option[String],
    featureType: FeatureTypes = FeatureTypes.UNSPECIFIED)

private[offline] case class SeqTaggedDependency(dependencies: Seq[TaggedDependency])

/**
 * customized derived feature config container class
 *
 * @param key
 * @param inputs
 * @param parameterNames
 * @param `class`
 */
private[offline] case class CustomDerivedFeatureConfig(key: Seq[String], inputs: Seq[TaggedDependency],
  parameterNames: Option[Seq[String]], `class`: Class[_]) {}

/**
 * Data model for a sequential join config. Example:
 *
 */
private[offline] case class SeqJoinFeatureConfig(
    @JsonProperty("key") key: Seq[String],
    @JsonProperty("join") join: SeqJoinDependency,
    @JsonProperty("aggregation") aggregation: String) {
  require(key != null)
  require(key.nonEmpty)
  require(join != null)
  require(aggregation != null)
}

private[offline] case class SeqJoinDependency(@JsonProperty("base") base: BaseTaggedDependency, @JsonProperty("expansion") expansion: TaggedDependency) {
  require(base != null)
  require(expansion != null)
}
