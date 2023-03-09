package com.linkedin.feathr.offline.util

object FCMUtils {
  def makeFeatureNameForDuplicates(keyTags: Seq[String], featureName: String): String = {
    keyTags.mkString("_") + "__" + featureName
  }
}
