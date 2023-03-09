package com.linkedin.feathr.offline

import com.linkedin.feathr.common

import scala.collection.JavaConverters._

/**
 * A companion object for common.ErasedEntityTaggedFeature
 * it provides the interface for scala specific usage, such as pattern matching, etc
 */
private[offline] object ErasedEntityTaggedFeature {
  def apply(keyTag: Seq[Int], featureRefStr: String): common.ErasedEntityTaggedFeature =
    new common.ErasedEntityTaggedFeature(keyTag.map(_.asInstanceOf[java.lang.Integer]).asJava, featureRefStr)

  def apply(keyTag: java.util.List[Integer], featureRefStr: String): common.ErasedEntityTaggedFeature =
    new common.ErasedEntityTaggedFeature(keyTag, featureRefStr)

  def unapply(arg: common.ErasedEntityTaggedFeature): Option[(Seq[Int], String)] =
    Some((arg.getBinding.asScala.map(_.asInstanceOf[Int]), arg.getFeatureName))
}
