package com.linkedin.feathr.offline.config

import com.fasterxml.jackson.core.TreeNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.{BooleanNode, ObjectNode, TextNode}
import com.linkedin.feathr.common.{FeatureTypeConfig, FeatureTypes}

import java.util.{List => JavaList}
import scala.collection.JavaConverters._

private[offline] object ConfigLoaderUtils {

  /**
   * Get boolean value from node, treat booleanNode true, textNode with "true", "True", "TRUE" as true, otherwise, false
   * @param node input config ObjectNode
   * @param fieldName field to extract
   * @return if has 'true' node, return true, else return false
   */
  def getBoolean(node: ObjectNode, fieldName: String): Boolean = Option(node.get(fieldName)) match {
    case None => false
    case Some(child) =>
      child match {
        case node: BooleanNode => node.asBoolean()
        case node: TextNode => node.asText() equalsIgnoreCase "true"
      }
  }

  /**
   * Deserializes an ObjectNode into a tuple of FeatureTypes and FeatureTypeConfig. If the ObjectNode doesn't contain
   * "type" child node, then it will return an empty option. If it contains "type" child node, Option[FeatureTypeConfig].
   */
  def getTypeConfig(node: TreeNode): Option[FeatureTypeConfig] = {
    node match {
      case x: ObjectNode =>
        if (x.has("type")) {
          val mapper = new ObjectMapper()
          var featureTypeConfig = mapper.readValue(x.get("type").toString, classOf[FeatureTypeConfig])
          Some(featureTypeConfig)
        } else {
          None
        }
      case _ => None
    }
  }

  /**
   * Convert Java List[String] to Scala Seq[String], and make a deep copy to avoid any not-serializable exception
   */
  private[feathr] def javaListToSeqWithDeepCopy(inputList: JavaList[String]): Seq[String] = {
    Seq(inputList.asScala: _*)
  }
}
