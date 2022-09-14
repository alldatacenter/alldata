package com.webank.wedatasphere.streamis.jobmanager.manager.utils

import java.util
import scala.collection.JavaConverters.{asScalaSetConverter, mapAsScalaMapConverter}

object JobUtils {
  /**
   * Filter the illegal characters parameter specific
   * @param params parameters
   */
  def filterParameterSpec(params: util.Map[String, Any]): util.Map[String, Any] ={
    for (paramEntry <- params.entrySet().asScala){
      val value = paramEntry.getValue
      value match {
        case str: String => paramEntry.setValue(str.replace(" ", "\\0x001"))
        case _ =>
      }
    }
    params
  }

}
