package com.linkedin.feathr.offline.config

import com.fasterxml.jackson.core.{JsonParser, TreeNode}
import com.fasterxml.jackson.databind.{DeserializationContext, JsonDeserializer}
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.node.{ObjectNode, TextNode}
import com.linkedin.feathr.common.FeatureTypeConfig
import com.linkedin.feathr.common.exception.{ErrorLabel, FeathrConfigException}

private[offline] sealed trait FeatureDefinition {
  val featureExpr: String
  val featureTypeConfig: Option[FeatureTypeConfig]
}

/**
 * This case class contains the information on the definition of SQL based features
 */
@JsonDeserialize(using = classOf[SQLFeatureDefinitionDeserializer])
private[offline] case class SQLFeatureDefinition(featureExpr: String, featureTypeConfig: Option[FeatureTypeConfig] = Option.empty) extends FeatureDefinition

class SQLFeatureDefinitionDeserializer extends JsonDeserializer[SQLFeatureDefinition] {
  override def deserialize(p: JsonParser, ctxt: DeserializationContext): SQLFeatureDefinition = {
    val codec = p.getCodec
    val node = codec.readTree[TreeNode](p)
    val typeConfig = ConfigLoaderUtils.getTypeConfig(node)
    node match {
      case x: ObjectNode =>
        SQLFeatureDefinition(node.get("def") match {
          case field: TextNode =>
            throw new FeathrConfigException(
              ErrorLabel.FEATHR_USER_ERROR,
              s"Trying to deserialize the config into SQLFeatureDefinition." +
                s"Invalid anchor config ${node} is found. Should not use 'def' " +
                "field with string value, since'def' field with a string value is reserved for MVEL expression" +
                "Please provide def.sqlExpr field.")
          case field: ObjectNode =>
            if (field.size() != 1) {
              throw new FeathrConfigException(
                ErrorLabel.FEATHR_USER_ERROR,
                s"Trying to deserialize the config into SQLFeatureDefinition." +
                  s"Invalid anchor config ${node} found: 'def' can only has one child node which " +
                  "is 'sqlExpr'." +
                  "Please only specify sqlExpr under def node and for all other field, such as default/type, please set as sibling node of 'def'")
            }
            field.get("sqlExpr").textValue()
          case _ =>
            throw new FeathrConfigException(
              ErrorLabel.FEATHR_USER_ERROR,
              s"Trying to deserialize the config into SQLFeatureDefinition." +
                s"Invalid anchor config ${node} found: 'def' field with 'sqlExpr' is required in SQL based feature" +
                "Please provide def.sqlExpr field.")
        }, typeConfig)
    }
  }
}

/**
 * This case class contains the information on the definition of MVEL based features
 */
@JsonDeserialize(using = classOf[FeatureDefinitionDeserializer])
private[offline] case class MVELFeatureDefinition(featureExpr: String, featureTypeConfig: Option[FeatureTypeConfig] = Option.empty) extends FeatureDefinition

class FeatureDefinitionDeserializer extends JsonDeserializer[FeatureDefinition] {
  override def deserialize(p: JsonParser, ctxt: DeserializationContext): MVELFeatureDefinition = {
    val codec = p.getCodec
    val node = codec.readTree[TreeNode](p)
    val typeConfig = ConfigLoaderUtils.getTypeConfig(node)
    node match {
      // if it is "FeatureName: <mvel expression>"
      case x: TextNode => MVELFeatureDefinition(node.asInstanceOf[TextNode].textValue(), None)
      // if it is "FeatureName: { def: <mvel expression> type: <feature type> }"
      case x: ObjectNode =>
        MVELFeatureDefinition(
          node.get("def") match {
            case field: TextNode => field.textValue()
            case _ =>
              throw new FeathrConfigException(
                ErrorLabel.FEATHR_USER_ERROR,
                s"Trying to deserialize the config into FeatureDefinition." +
                  s"Feature definition ${node} is not correctly set as it doesn't have a def field." +
                  "Please provide def field.")
          }, typeConfig)
    }
  }
}
