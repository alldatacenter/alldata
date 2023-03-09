package com.linkedin.feathr.offline.generation

import com.linkedin.feathr.common.Header
import com.linkedin.feathr.common.exception.{ErrorLabel, FeathrException}

import scala.collection.JavaConverters._
// Utils for feature generation
private[offline] object FeatureGenUtils {

  /**
   * get key columns from the header of feature generation API
   * @param header header returned by feature generation API
   * @return key columns in the dataFrame
   */
  def getKeyColumnsFromHeader(header: Header): Seq[String] = {
    if (header.featureInfoMap.isEmpty) {
      throw new FeathrException(ErrorLabel.FEATHR_ERROR, s"Header ${header} in feature generation is empty!")
    }
    // We always use the first feature's key tag as the key tag in the output dataframe
    header.featureInfoMap.head._1.getKeyTag.asScala
  }

}
