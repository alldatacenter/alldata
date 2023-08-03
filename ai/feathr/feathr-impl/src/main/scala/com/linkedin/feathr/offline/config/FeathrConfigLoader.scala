package com.linkedin.feathr.offline.config

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.{JsonParser, ObjectCodec, TreeNode}
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node._
import com.jasonclawson.jackson.dataformat.hocon.HoconFactory
import com.linkedin.feathr.common.exception.{ErrorLabel, FeathrConfigException, FeathrException}
import com.linkedin.feathr.common.{AnchorExtractor, AnchorExtractorBase, FeathrJacksonScalaModule, FeatureDerivationFunctionBase, FeatureTypeConfig, FeatureTypes}
import com.linkedin.feathr.offline
import com.linkedin.feathr.offline.ErasedEntityTaggedFeature
import com.linkedin.feathr.offline.anchored.anchorExtractor.{SQLConfigurableAnchorExtractor, SimpleConfigurableAnchorExtractor, TimeWindowConfigurableAnchorExtractor}
import com.linkedin.feathr.offline.anchored.feature.{FeatureAnchor, FeatureAnchorWithSource}
import com.linkedin.feathr.offline.anchored.keyExtractor.{MVELSourceKeyExtractor, SQLSourceKeyExtractor}
import com.linkedin.feathr.offline.client.plugins.{AnchorExtractorAdaptor, FeathrUdfPluginContext, FeatureDerivationFunctionAdaptor, SimpleAnchorExtractorSparkAdaptor, SourceKeyExtractorAdaptor}
import com.linkedin.feathr.offline.config.location.{DataLocation, KafkaEndpoint, LocationUtils, SimplePath, Snowflake}
import com.linkedin.feathr.offline.derived._
import com.linkedin.feathr.offline.derived.functions.{MvelFeatureDerivationFunction, SQLFeatureDerivationFunction, SeqJoinDerivationFunction, SimpleMvelDerivationFunction}
import com.linkedin.feathr.offline.source.{DataSource, SourceFormatType, TimeWindowParams}
import com.linkedin.feathr.offline.util.FeatureValueTypeValidator
import com.linkedin.feathr.sparkcommon.{SimpleAnchorExtractorSpark, SourceKeyExtractor}
import com.linkedin.feathr.swj.LateralViewParams
import com.typesafe.config.ConfigFactory
import org.apache.logging.log4j.{LogManager, Logger}

import java.io.File
import java.net.URL
import scala.collection.JavaConverters._
import scala.collection.convert.wrapAll._

/**
 * Loads one Feathr config file and uses it to construct a FeathrConfig object.
 */
private[offline] class FeathrConfigLoader {

}

private[offline] object FeathrConfigLoader {

  /**
    * Override configDoc using overrideConfDoc
    * @param configDoc base config
    * @param overrideConfDoc config used to override configDoc
    * @return overridden configDoc
    */
  def resolveOverride(configDoc: String, overrideConfDoc: String): String = {
    val conf = ConfigFactory.parseString(configDoc)
    val overrideConf = ConfigFactory.parseString(overrideConfDoc)
    overrideConf.withFallback(conf).root().render()
  }



  def apply(): FeathrConfig = FeathrConfig(None, Map(), None, None)

  /**
   * extract string list contained in a json node
   */
  private[config] def extractStringList(jsonNode: JsonNode): Seq[String] = {
    if (jsonNode.isArray) {
      jsonNode.asInstanceOf[ArrayNode].toList.map(i => i.textValue().trim)
    } else if (jsonNode.isTextual) {
      Seq(jsonNode.textValue().trim)
    } else {
      throw new FeathrConfigException(ErrorLabel.FEATHR_USER_ERROR, s"No valid string list found for node ${jsonNode}.")
    }
  }

  /**
   * extract optional string list contained in a json node
   * @param jsonNode
   * @return
   */
  private[config] def extractStringListOpt(jsonNode: JsonNode): Option[Seq[String]] = {
    Option(jsonNode) match {
      case Some(node) => Some(extractStringList(node))
      case _ => None
    }
  }

  /**
   * extract inputs/dependency feature list for a derived feature
   * @param inputsNode 'inputs' node for a derived feature
   * @param codec jackson codec
   * @return pair of seq of dependency and seq of parameter name in the same order
   */
  private[config] def extractTaggedDependency(inputsNode: JsonNode, codec: ObjectCodec): (Seq[TaggedDependency], Option[Seq[String]]) = {
    if (inputsNode.isObject) {
      val fields = inputsNode.asInstanceOf[ObjectNode].fields().asScala.toSeq
      val inputs = fields.map(item => codec.treeToValue(item.getValue, classOf[TaggedDependency]))
      val parameterNames = fields.map(item => item.getKey)
      (inputs, Some(parameterNames))
    } else if (inputsNode.isArray) {
      val seqTaggedDependency = codec.treeToValue(inputsNode, classOf[SeqTaggedDependency])
      (seqTaggedDependency.dependencies, None)
    } else if (inputsNode.isTextual && inputsNode.textValue() == "PROVIDED_BY_CLASS") {
      (Seq(), None)
    } else {
      throw new FeathrConfigException(
        ErrorLabel.FEATHR_USER_ERROR,
        s"Fail to extract inputs/dependency feature list for a derived feature since ${inputsNode} is invalid.")
    }
  }
}

/**
 * FeathrConfig consists of anchor and derived feature definitions.
 *
 * @param anchors map of feature anchors. the keys are the anchor names (currently unused, but must be unique)
 * @param derivationsArg optional map of feature derivations. the keys are the feature names; the value for a given key is the derivation for that feature name
 * @param sources optional map of sources, the key are the source names; the value is the source definition
 */
case class FeathrConfig(
                                         @JsonProperty("sources") sources: Option[Map[String, DataSource]] = None,
                                         @JsonProperty("anchors") anchors: Map[String, FeatureAnchor] = Map(),
                                         @JsonProperty("derivations") derivationsArg: Option[Map[String, DerivedFeature]] = None,
                                         @JsonProperty("advancedDerivations") multiDerivationsArg: Option[Seq[DerivedFeature]] = None) {
  val jackson: ObjectMapper = new ObjectMapper(new HoconFactory)
    .registerModule(FeathrJacksonScalaModule) // DefaultScalaModule causes a fail on holdem
    .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .registerModule(
      new SimpleModule()
        .addDeserializer(classOf[DerivedFeature], new DerivationLoader)
        .addDeserializer(classOf[FeatureAnchor], new AnchorLoader)
        .addDeserializer(classOf[DataSource], new DataSourceLoader)
        .addDeserializer(classOf[CustomDerivedFeatureConfig], new CustomDerivedFeatureConfigLoader)
        .addDeserializer(classOf[DerivedFeatureConfig], new DerivedFeatureConfigLoader)
        .addDeserializer(classOf[SeqTaggedDependency], new SeqTaggedDependencyLoader))
  def load(configDoc: String): FeathrConfig = jackson.readValue(configDoc, classOf[FeathrConfig])

  def load(url: URL): FeathrConfig = jackson.readValue(url, classOf[FeathrConfig])

  def load(file: File): FeathrConfig = jackson.readValue(file, classOf[FeathrConfig])

  /**
   * Override configDoc using overrideConfDoc
   * @param configDoc base config
   * @param overrideConfDoc config used to override configDoc
   * @return overridden configDoc
   */
  def resolveOverride(configDoc: String, overrideConfDoc: String): String = {
    val conf = ConfigFactory.parseString(configDoc)
    val overrideConf = ConfigFactory.parseString(overrideConfDoc)
    overrideConf.withFallback(conf).root().render()
  }
  getAllFeatures()
    .groupBy(identity)
    .mapValues(_.size)
    .foreach(tuple => {
      if (tuple._2 != 1) {
        throw new FeathrConfigException(
          ErrorLabel.FEATHR_USER_ERROR,
          s"The feature ${tuple._1} is defined more than once. All defined features are ${getAllFeatures()}.")
      }
    })

  val derivedFeatures: Map[String, DerivedFeature] = derivationsArg.getOrElse(Map()) ++
    multiDerivationsArg.getOrElse(Seq()).flatMap(derived => derived.producedFeatureNames.map(name => name -> derived)).toMap

  val anchoredFeatures: Map[String, FeatureAnchorWithSource] = if (anchors != null) {
    anchors.values
      .flatMap(
        // fill the sources into each Feature Anchor
        anchor => anchor.getProvidedFeatureNames.map(_ -> FeatureAnchorWithSource(anchor, sources)))
      .toMap
  } else {
    Map()
  }

  private def getAllFeatures() = {
    val derivations = derivationsArg.getOrElse(Map()).values ++ multiDerivationsArg.getOrElse(Seq())
    val derivedFeatureList = if (derivations != null) {
      derivations.flatMap(derivation => derivation.producedFeatureNames)
    } else {
      Seq()
    }
    val anchoredFeatureList = if (anchors != null) {
      anchors.values.flatMap(anchor => anchor.getProvidedFeatureNames)
    } else {
      Seq()
    }
    derivedFeatureList ++ anchoredFeatureList
  }
}

/**
 * Loads an Anchor from a json-like config
 */
private[offline] class AnchorLoader extends JsonDeserializer[FeatureAnchor] {
  val log: Logger = LogManager.getLogger(getClass)

  val LATERAL_VIEW_PARAMETERS = "lateralViewParameters"
  val LATERAL_VIEW_DEF = "lateralViewDef"
  val LATERAL_VIEW_ITEM_ALIAS = "lateralViewItemAlias"
  val LATERAL_VIEW_FILTER = "lateralViewFilter"
  // key tag alias, which provides a name for the key expression, so that:
  // 1. it can be used as part of column name in dataframe
  // 2. old extractor interface AnchorExtractor[_] based feature have a way to provide key column names, this
  //    is useful in both feature join and feature generation, this will also make it consistent with other
  //    extractor interfaces, e.g, SimpleAnchorExtractorSpark, MVEL/SQL key based anchor, etc.
  val KEY_ALIAS = "keyAlias"

  override def deserialize(p: JsonParser, ctxt: DeserializationContext): FeatureAnchor = {
    val codec = p.getCodec
    val node = codec.readTree[TreeNode](p).asInstanceOf[ObjectNode]
    val parsingContextName = if (p.getParsingContext != null) p.getParsingContext.getCurrentName else "<anonymous>"

    val featuresNode = node.get("features")
    // get feature name list
    val features = featuresNode match {
      case field: ArrayNode => field.elements.map(_.asText).toSet
      case field: ObjectNode => field.fieldNames.toSet
      case _ =>
        throw new FeathrConfigException(
          ErrorLabel.FEATHR_USER_ERROR,
          s"$parsingContextName: field 'features' is required for " +
            s"anchor and must be an array or map.")
    }

    // using extractor as the indicator of the extractor class, also add the backward compatibility of "transformer"
    val anchorExtractorName = Option(node.get("extractor")) match {
      case Some(field: ObjectNode) => Some(field.get("class").textValue())
      case Some(field: TextNode) => Some(field.textValue())
      case None => Option(node.get("transformer")).map(_.textValue)
    }

    // Check 'aggregation' node (if exists, indicate time-window feature) in the config to apply corresponding
    // anchor extractor
    val timeWindowFeatureNode = featuresNode.path(features.head).path("aggregation")
    val hasSameAnchorType = featuresNode
      .filter(_.has("def"))
      .map(feature => feature.path("def").isObject)
      .toSeq
      .distinct
      .size <= 1
    if (!hasSameAnchorType) {
      throw new FeathrConfigException(
        ErrorLabel.FEATHR_USER_ERROR,
        "Anchor " + node + " cannot have both SQL def (object) and MVEL def (string). Only one is needed.")
    }
    val isTimeWindowAnchor = !timeWindowFeatureNode.isMissingNode()
    val isSQLDefFeatureNode = featuresNode.path(features.head).path("def").isObject
    val extractorClass = anchorExtractorName
      .map(Class.forName)
      .getOrElse(
        // if extractorClass is not defined, e.g, using def to define extractor, then use the default ones
        if (isTimeWindowAnchor) {
          classOf[TimeWindowConfigurableAnchorExtractor]
        } else if (isSQLDefFeatureNode) {
          classOf[SQLConfigurableAnchorExtractor]
        } else {
          classOf[SimpleConfigurableAnchorExtractor]
        })

    // default, extractor can be the class defined in the config or the SimpleConfigurableAnchorExtractor
    // or TimeWindowConfigurableAnchorExtractor

    // if extractor is used, it is recommended that it is used to defined an extractor class (UDF)
    // SimpleConfigurableAnchorExtractor will extract the feature type if defined

    // if it is UDF, no extra information other than the class name is required
    val anchorExtractor: AnyRef = {
      val extractor = codec.treeToValue(node, extractorClass).asInstanceOf[AnyRef]
      FeathrUdfPluginContext.getRegisteredUdfAdaptor(extractorClass) match {
        case None => extractor
        case Some(adaptor: SimpleAnchorExtractorSparkAdaptor) =>
          adaptor.adaptUdf(extractor)
        case Some(adaptor: AnchorExtractorAdaptor) =>
          adaptor.adaptUdf(extractor)
      }
    }

    anchorExtractor match {
      case extractor: AnchorExtractorBase[_] =>
          val extractorNode = node.get("extractor")
          if (extractorNode != null && extractorNode.get("params") != null) {
            // init the param into the extractor
            val config = ConfigFactory.parseString(extractorNode.get("params").toString)
            extractor.init(config)
          }
    }

    // cast the the extractor class to AnchorExtractor[Any]
    val lateralViewParameterNode = Option(node.get(LATERAL_VIEW_PARAMETERS)) match {
      case Some(field: ObjectNode) => Option(field)
      case None => None
      case _ =>
        throw new FeathrConfigException(
          ErrorLabel.FEATHR_USER_ERROR,
          s"Illegal setting for $LATERAL_VIEW_PARAMETERS. ${node.get(LATERAL_VIEW_PARAMETERS)} is provided but expecting map")
    }
    val lateralViewParameters = lateralViewParameterNode match {
      case Some(node: ObjectNode) =>
        Some(LateralViewParams(node.get(LATERAL_VIEW_DEF) match {
          case field: TextNode => field.textValue()
          case _ => throw new FeathrConfigException(ErrorLabel.FEATHR_USER_ERROR, s"field '$LATERAL_VIEW_DEF' is not correctly set")
        }, node.get(LATERAL_VIEW_ITEM_ALIAS) match {
          case field: TextNode => field.textValue()
          case _ => throw new FeathrConfigException(ErrorLabel.FEATHR_USER_ERROR, s"field '$LATERAL_VIEW_ITEM_ALIAS' is not correctly set")
        }, node.get(LATERAL_VIEW_FILTER) match {
          case field: TextNode => Some(field.textValue())
          case _ => None
        }))
      case None => None
    }
    val sourceKeyExtractor = loadSourceKeyExtractor(node, isTimeWindowAnchor, anchorExtractor, lateralViewParameters)
    // extract the default values from the features definition, while the extractor can be either MVEL or class
    val defaultValuesConfigs = {
      featuresNode match {
        case node: ObjectNode =>
          node
            .fields()
            .toList
            .map(field =>
              field.getValue match {
                case x: ObjectNode =>
                  Some(field.getKey, Option(x.get("default")), ConfigLoaderUtils.getTypeConfig(field.getValue))
                case _ => None
              })
        // feature defs are provided in the extractor, we just have the feature names here
        case _: ArrayNode => List()
        case _ => throw new FeathrConfigException(ErrorLabel.FEATHR_USER_ERROR, "features can only be defined as array or map")
      }
    }.flatten

    val defaultValues = defaultValuesConfigs
      .filter { case (_, defaultValue: Option[JsonNode], _) => defaultValue.isDefined }
      .map {
        case (key, defaultValue: Option[JsonNode], featureTypeConfig: Option[FeatureTypeConfig]) =>
          val featureType = featureTypeConfig.map(x => x.getFeatureType).getOrElse(FeatureTypes.UNSPECIFIED)
          val rawValue = normalizeJsonNodeIntoStandardForm(defaultValue.get, codec)
          val featureValue = featureTypeConfig match {
            case Some(tType) => offline.FeatureValue.fromTypeConfig(rawValue, tType)
            case None => offline.FeatureValue(rawValue, featureType, key)
          }
          FeatureValueTypeValidator.validate(featureValue, featureTypeConfig, key)
          (key, featureValue)
      }
      .toMap

    val featureTypeConfigs: Map[String, FeatureTypeConfig] = defaultValuesConfigs
      .filter { case (_, _, featureTypeConfig: Option[FeatureTypeConfig]) => featureTypeConfig.isDefined }
      .map { case (key, _, featureTypeConfig: Option[FeatureTypeConfig]) => (key, featureTypeConfig.get) }
      .toMap

    val sourceIdentifier = node.get("source").textValue
    val anchor = FeatureAnchor(sourceIdentifier, anchorExtractor, defaultValues, lateralViewParameters, sourceKeyExtractor, features, featureTypeConfigs)
    val extractorProvidedFeatures = getExtractorProvidedFeatureNames(anchorExtractor)
    if (!extractorProvidedFeatures.containsAll(features)) {
      // as long provided feature names is a superset of the claimed ones
      throw new FeathrConfigException(
        ErrorLabel.FEATHR_USER_ERROR,
        s"Anchor $parsingContextName configured for some features different" +
          s" from its extractor claims to provide. Anchor returned: $features; extractor provided: $extractorProvidedFeatures")
    }
    if (!features.equals(extractorProvidedFeatures.toSet)) {
      // when the anchor extractor getProvidedFeatureNames provide more features than needed, we should warn the users.
      // Returning more features than needed is not a good practice. Since there are already existing extractors doing
      // this today, so we don't want to throw exception here.
      log.warn(
        s"Anchor $parsingContextName extractor's getProvidedFeatureNames and feature definition are different. Anchor " +
          s"returned: $extractorProvidedFeatures; config provided: $features")
    }
    anchor
  }

  /**
   * load source key extractor for an anchor
   * @param node the json node contains the source extractor
   * @param isTimeWindowAnchor is loading sliding window aggregation anchor
   * @param anchorExtractorBase anchorExtractor for current anchor
   * @param lateralViewParameters lateral view parameters
   * @return source extractor class in the json node
   */
  def loadSourceKeyExtractor(
                              node: JsonNode,
                              isTimeWindowAnchor: Boolean,
                              anchorExtractorBase: AnyRef,
                              lateralViewParameters: Option[LateralViewParams]): SourceKeyExtractor = {
    Option(node.get("keyExtractor")).map(_.textValue) match {
      case Some(keyExtractorClassName) =>
        val keyExtractorClass = Class.forName(keyExtractorClassName)
        val newInstance = keyExtractorClass.getDeclaredConstructor().newInstance().asInstanceOf[AnyRef]
        FeathrUdfPluginContext.getRegisteredUdfAdaptor(keyExtractorClass) match {
          case Some(adaptor: SourceKeyExtractorAdaptor) => adaptor.adaptUdf(newInstance)
          case _ => newInstance.asInstanceOf[SourceKeyExtractor]
        }
      case _ =>
        Option(node.get("key")) match {
          case Some(keyNode) =>
            if (keyNode.isArray || keyNode.isTextual) {
              if (!isTimeWindowAnchor && !anchorExtractorBase.isInstanceOf[AnchorExtractor[_]]) {
                // for non-sliding window aggregation feature, we reserved the 'key' field for MVEL expressions, which
                // only works with AnchorExtractor, so if the extracot is not AnchorExtractor, e.g., SimpleAnchorExtractorSpark,
                // the user should use 'keyExtractor' or key.sqlExpr
                throw new FeathrConfigException(
                  ErrorLabel.FEATHR_USER_ERROR,
                  s"In ${node}, ${anchorExtractorBase} cannot be used with 'key' section, " +
                    s"use 'key.sqlExpr' for Spark SQL expression, or 'keyExtractor' instead")
              }
              val keyExprs = FeathrConfigLoader.extractStringList(keyNode)
              val keyAlias = FeathrConfigLoader.extractStringListOpt(node.get(KEY_ALIAS))

              if (isTimeWindowAnchor) {
                new SQLSourceKeyExtractor(keyExprs, keyAlias, lateralViewParameters)
              } else {
                val anchorExtractor = anchorExtractorBase.asInstanceOf[AnchorExtractor[Any]]
                new MVELSourceKeyExtractor(anchorExtractor, keyAlias)
              }
            } else if (keyNode.isObject) {
              val sqlExprNode = keyNode.get("sqlExpr")
              val keys = FeathrConfigLoader.extractStringList(sqlExprNode)
              val keyAlias = FeathrConfigLoader.extractStringListOpt(keyNode.get(KEY_ALIAS))
              new SQLSourceKeyExtractor(keys, keyAlias, lateralViewParameters)
            } else {
              val anchorExtractor = anchorExtractorBase.asInstanceOf[AnchorExtractor[Any]]
              new MVELSourceKeyExtractor(anchorExtractor)
            }
          case None =>
            val keyAlias = FeathrConfigLoader.extractStringListOpt(node.get(KEY_ALIAS))
            val anchorExtractor = if (!anchorExtractorBase.isInstanceOf[AnchorExtractorBase[_]]) {
              FeathrUdfPluginContext.getRegisteredUdfAdaptor(anchorExtractorBase.getClass) match {
                case Some(adaptor: AnchorExtractorAdaptor) =>
                  adaptor.adaptUdf(anchorExtractorBase).asInstanceOf[AnchorExtractor[Any]]
                case _ =>
                  throw new FeathrException(
                    ErrorLabel.FEATHR_USER_ERROR,
                    s"In ${node}, ${anchorExtractorBase} with no key and no keyExtractor must be extends AnchorExtractorBase")
              }
            } else {
              anchorExtractorBase.asInstanceOf[AnchorExtractor[Any]]
            }
            new MVELSourceKeyExtractor(anchorExtractor, keyAlias)
        }
    }
  }

  /**
   * Normalize configuration values into 'standard' ones, where all numeric values are converted to double
   * Only string can be used to represent categorical values
   * integer values will not cannot be used to represent categorical values any more, and will be convert to numeric double
   * We 'normalize' the jsonNode deeply(e.g, convert elements within map and array)\\
   */
  private def normalizeJsonNodeIntoStandardForm(jsonNode: JsonNode, codec: ObjectCodec): Any = jsonNode match {
    case x: TextNode => x.textValue()
    case x: NumericNode => x.asDouble()
    case x: ArrayNode =>
      val arr = codec.treeToValue(x, classOf[java.util.List[_]])
      for (i <- arr) {
        i match {
          case v: Number => v.floatValue()
          case v: String => v
          case _ => throw new FeathrConfigException(ErrorLabel.FEATHR_USER_ERROR, "Value array can only contains string or number")
        }
      }
      arr
    case x: ObjectNode =>
      (x.fieldNames() zip x.elements().map(y => y.asDouble())).toMap.asJava
    case x: BooleanNode => x.asBoolean()
    case _ => throw new FeathrConfigException(ErrorLabel.FEATHR_USER_ERROR, "Unrecognized features value type")
  }

  private def getExtractorProvidedFeatureNames(extractor: AnyRef): Seq[String] = extractor match {
    case extractor: AnchorExtractor[_] => extractor.getProvidedFeatureNames
    case extractor: SimpleAnchorExtractorSpark => extractor.getProvidedFeatureNames
    case _ => throw new FeathrConfigException(ErrorLabel.FEATHR_USER_ERROR, s"unrecognized extractor ${this}")
  }
}

/**
 * Jackson cannot not load Seq[SomeCustomClass] by just annotating SomeCustomClass
 * This class is to workaround loading Seq[TaggedDependency]
 */
private[offline] class SeqTaggedDependencyLoader extends JsonDeserializer[SeqTaggedDependency] {
  override def deserialize(p: JsonParser, ctxt: DeserializationContext): SeqTaggedDependency = {
    val codec = p.getCodec
    val baseNode = codec.readTree[TreeNode](p)
    if (!baseNode.isArray) {
      throw new FeathrConfigException(ErrorLabel.FEATHR_USER_ERROR, "Seq of TagggedDependency has to be an array")
    }
    val node = baseNode.asInstanceOf[ArrayNode]
    val dependencies = node.toList.map { child =>
      val keyNode = child.get("key")
      val key = if (keyNode.isArray) {
        keyNode.asInstanceOf[ArrayNode].toList.map(i => i.textValue())
      } else {
        List(keyNode.textValue())
      }
      val feature = child.get("feature").textValue()
      TaggedDependency(key, feature)
    }
    SeqTaggedDependency(dependencies)
  }
}

private[offline] class CustomDerivedFeatureConfigLoader extends JsonDeserializer[CustomDerivedFeatureConfig] {
  override def deserialize(p: JsonParser, ctxt: DeserializationContext): CustomDerivedFeatureConfig = {
    val codec = p.getCodec
    val node = codec.readTree[TreeNode](p).asInstanceOf[ObjectNode]
    if (!node.has("key")) {
      throw new FeathrConfigException(ErrorLabel.FEATHR_USER_ERROR, "Does not have key in derived function")
    }
    val keyNode = node.get("key")
    val key: Seq[String] = if (keyNode.isArray) {
      codec.treeToValue(keyNode, classOf[java.util.List[String]]).asScala
    } else {
      Seq(keyNode.asText().trim)
    }
    val (inputsNode, parameterNames) = FeathrConfigLoader.extractTaggedDependency(node.get("inputs"), codec)
    val className = Class.forName(node.get("class").asText().trim)
    CustomDerivedFeatureConfig(key, inputsNode, parameterNames, className)
  }
}

private[offline] class DerivedFeatureConfigLoader extends JsonDeserializer[DerivedFeatureConfig] {
  override def deserialize(p: JsonParser, ctxt: DeserializationContext): DerivedFeatureConfig = {
    val codec = p.getCodec
    val node = codec.readTree[TreeNode](p).asInstanceOf[ObjectNode]
    if (!node.has("key")) {
      throw new FeathrConfigException(ErrorLabel.FEATHR_USER_ERROR, "Does not have key in complex derived function")
    }
    val keyNode = node.get("key")
    val key: Seq[String] = if (keyNode.isArray) {
      codec.treeToValue(keyNode, classOf[java.util.List[String]]).asScala
    } else {
      Seq(keyNode.asText().trim)
    }
    val inputsNode = node.get("inputs")
    val inputsObject: Map[String, TaggedDependency] = if (inputsNode.isObject) {
      inputsNode.asInstanceOf[ObjectNode].fields().asScala.map(item => (item.getKey, codec.treeToValue(item.getValue, classOf[TaggedDependency]))).toMap
    } else {
      throw new FeathrConfigException(ErrorLabel.FEATHR_USER_ERROR, "'inputs' of " + node + " should be a map")
    }
    val definitionNode = node.get("definition")
    val typeNode = node.get("type")
    val definition = if (!definitionNode.isObject) Some(definitionNode.textValue()) else None
    val sqlExpr = if (definitionNode.isObject) Some(definitionNode.get("sqlExpr").textValue()) else None
    val featureType = if (typeNode != null && !typeNode.isObject) FeatureTypes.valueOf(typeNode.textValue()) else FeatureTypes.UNSPECIFIED
    DerivedFeatureConfig(key, inputsObject, definition, sqlExpr, featureType)
  }
}

/**
 * Loads a Derivation from a json-like config.
 *
 * If the value is a string, it will be interpreted as a SimpleMvelDerivation. Otherwise, "class" must be specified
 * (must be a subtype of FeatureDerivation).
 */
private[offline] class DerivationLoader extends JsonDeserializer[DerivedFeature] {
  val log: Logger = LogManager.getLogger(getClass)

  override def deserialize(p: JsonParser, ctxt: DeserializationContext): DerivedFeature = {
    val codec = p.getCodec
    val featureName = p.getParsingContext.getCurrentName // TODO It's not ideal that the value depends on the key here

    val producedFeatures = Seq(featureName)
    val node = codec.readTree[TreeNode](p)

    val typeConfig = ConfigLoaderUtils.getTypeConfig(node)
    val featureTypeConfigMap = typeConfig.map(featureTypeConfig => Map(featureName -> featureTypeConfig)).getOrElse(Map.empty[String, FeatureTypeConfig])

    val derivedFeature = node match {
      case x: TextNode => // deprecate this one? Probably not, since it will be easier for single key feature renaming
        val derivationFunction = new SimpleMvelDerivationFunction(x.textValue, featureName, featureTypeConfigMap.get(featureName))
        val keyTag = Seq(0) // <=== should work assuming that we only use single-key features in these simplest expression-based derivations
        val consumedFeatures = derivationFunction.dependencyFeatureNames.map(ErasedEntityTaggedFeature(keyTag, _))
        DerivedFeature(consumedFeatures, producedFeatures, derivationFunction, None, featureTypeConfigMap)
      case x: ObjectNode =>
        if (x.has("features")) {
          // advanced derived feature
          // type syntax is not supported for advanced derived feature as this syntax is going to be deprecated
          loadAdvancedDerivedFeature(x, codec)
        } else if (x.has("class")) {
          val config = codec.treeToValue(x, classOf[CustomDerivedFeatureConfig])
          val derivationFunctionClass = config.`class`
          val derivationFunction = derivationFunctionClass.getDeclaredConstructor().newInstance().asInstanceOf[AnyRef]
          // possibly "adapt" the derivation function, in case it doesn't implement Feathr's FeatureDerivationFunction,
          // using FeathrUdfPluginContext
          val maybeAdaptedDerivationFunction = FeathrUdfPluginContext.getRegisteredUdfAdaptor(derivationFunctionClass) match {
            case Some(adaptor: FeatureDerivationFunctionAdaptor) => adaptor.adaptUdf(derivationFunction)
            case _ => derivationFunction
          }
          val consumedFeatures = config.inputs.map(x => ErasedEntityTaggedFeature(x.key.map(config.key.zipWithIndex.toMap), x.feature)).toIndexedSeq

          // consumedFeatures and parameterNames have same order, since they are all from config.inputs
          DerivedFeature(consumedFeatures, producedFeatures, maybeAdaptedDerivationFunction, config.parameterNames, featureTypeConfigMap)
        } else if (x.has("join")) { // when the derived feature config is a seqJoin config
          val config = codec.treeToValue(x, classOf[SeqJoinFeatureConfig])
          if (config.aggregation.isEmpty) {
            throw new FeathrConfigException(
              ErrorLabel.FEATHR_USER_ERROR,
              s"Feathr does not support empty aggregation function." +
                s"Sequential+Join+Features+in+Feathr#SequentialJoinFeaturesinFeathr-ExpansionFeatureTypesandAggregation " +
                s"to provide the right default aggregation (based on the base feature value type)")
          }
          val baseFeature = config.join.base
          val expansionFeature = config.join.expansion
          val consumedFeatures = Seq(
            ErasedEntityTaggedFeature(baseFeature.key.map(config.key.zipWithIndex.toMap), baseFeature.feature),
            // denote expansion feature's keyTag to be empty(so that the expand feature data source will not be filtered by
            // the wrong bloomfilter keys), it does not need to be included in join stages,
            // In practice, expansion feature's keyTag doesn't matter because seqJoin is processed
            // separately from all other features.
            ErasedEntityTaggedFeature(Seq(), expansionFeature.feature))
          val seqJoinDerivationFunction = new SeqJoinDerivationFunction(baseFeature, expansionFeature, config.aggregation)
          // consumedFeatures and parameterNames have same order, since they are all from config.inputs
          DerivedFeature(consumedFeatures, producedFeatures, seqJoinDerivationFunction, None, featureTypeConfigMap)
        } else if (x.has("sqlExpr")) {
          val definition = x.get("sqlExpr").textValue()
          val keyTag = Seq(0) // <=== should work assuming that we only use single-key features in these simplest expression-based derivations
          val derivationFunction = new SQLFeatureDerivationFunction(definition)
          val consumedFeatures = derivationFunction.dependencyFeatureNames.map(ErasedEntityTaggedFeature(keyTag, _))
          DerivedFeature(consumedFeatures, producedFeatures, derivationFunction, None, featureTypeConfigMap)
        } else if (x.has("definition") && !x.has("inputs")) {
          val definition = x.get("definition").textValue()
          val keyTag = Seq(0) // <=== should work assuming that we only use single-key features in these simplest expression-based derivations
          val derivationFunction = new SimpleMvelDerivationFunction(definition, featureName, featureTypeConfigMap.get(featureName))
          val consumedFeatures = derivationFunction.dependencyFeatureNames.map(ErasedEntityTaggedFeature(keyTag, _))
          DerivedFeature(consumedFeatures, producedFeatures, derivationFunction, None, featureTypeConfigMap)
        } else {
          val config = codec.treeToValue(x, classOf[DerivedFeatureConfig])
          val consumedFeatures = config.inputs.values.map(x => ErasedEntityTaggedFeature(x.key.map(config.key.zipWithIndex.toMap), x.feature)).toIndexedSeq
          val derivationFunction = if (config.SQLExpr != None) {
            new SQLFeatureDerivationFunction(config.SQLExpr.get, Some(config.inputs.keys.toIndexedSeq))
          } else {
            new MvelFeatureDerivationFunction(config.inputs, config.definition.get, featureName, featureTypeConfigMap.get(featureName))
          }
          val parameterNames = Some(config.inputs.keys.toIndexedSeq)
          // consumedFeatures and parameterNames have same order, since they are all from config.inputs
          DerivedFeature(consumedFeatures, producedFeatures, derivationFunction, parameterNames, featureTypeConfigMap)
        }
    }
    // Perform necessary validations
    val configProvidedFeatures = derivedFeature.producedFeatureNames

    try {
      val extractorProvidedFeatures = derivedFeature.getOutputFeatureList()
      if (!extractorProvidedFeatures.containsAll(configProvidedFeatures)) {
        // as long provided feature names is a superset of the claimed ones
        throw new FeathrConfigException(
          ErrorLabel.FEATHR_USER_ERROR,
          s"Derivation ${p.getParsingContext.getCurrentName} configured for some features different" +
            s"from its transformer claims to provide. Derivation returned: $extractorProvidedFeatures; config provided: $configProvidedFeatures")
      }
      if (!configProvidedFeatures.equals(extractorProvidedFeatures.toSet)) {
        // when the anchor extractor getProvidedFeatureNames provide more features than needed, we should warn the users.
        // Returning more features than needed is not a good practice. Since there are already existing extractors doing
        // this today, so we don't want to throw exception here.
        log.warn(
          s"Derivation ${p.getParsingContext.getCurrentName} extractor's getOutputFeatureList and feature definition are different. Derivation " +
            s"returned: $extractorProvidedFeatures; config provided: $configProvidedFeatures")
      }
    } catch {
      case _: RuntimeException =>
    }
    derivedFeature
  }

  /**
   * load an advanced derived feature
   * @param node json config node that contains an advanced derived feature
   * @param codec json codec
   * @return the loaded advanced derived feature
   */
  private[config] def loadAdvancedDerivedFeature(node: ObjectNode, codec: ObjectCodec): DerivedFeature = {
    val keys = FeathrConfigLoader.extractStringList(node.get("key")).zipWithIndex
    // we are using typesafe config to load parameters, as we don't know the expected field names of the params
    val (derivationClassName, paramsConfigsOpt) = if (node.get("class").isTextual) {
      (node.get("class").textValue(), None)
    } else if (node.get("class").isObject) {
      val config = ConfigFactory.parseString(node.get("class").toString())
      (config.getString("name"), Some(config))
    } else {
      throw new FeathrConfigException(ErrorLabel.FEATHR_USER_ERROR, s"the class of $node should be either an object or a string")
    }
    val derivationFunction = Class.forName(derivationClassName).newInstance().asInstanceOf[FeatureDerivationFunctionBase]
    if (paramsConfigsOpt.isDefined) {
      derivationFunction.init(paramsConfigsOpt.get)
    }

    // load dependency(input) features
    val (inputs, parameterNamesInConfig) = FeathrConfigLoader.extractTaggedDependency(node.get("inputs"), codec)
    // convert inputs from tagged dependency to ErasedEntityTaggedFeature
    val (consumedFeatures, parameterNames) = if (inputs.isEmpty) {
      // if input is empty, get input/dependency from getInputFeatureList
      val dependFeatureList = derivationFunction.getInputFeatureList()
      val features = dependFeatureList
        .map(stringTaggedFeature => {
          val taggedDependency = TaggedDependency(stringTaggedFeature.keyTags, stringTaggedFeature.featureName)
          ErasedEntityTaggedFeature(taggedDependency.key.map(keys.toMap), taggedDependency.feature)
        })
        .toIndexedSeq
      val parameterNames = Some(dependFeatureList.map(_.featureName))
      (features, parameterNames)
    } else {
      val features = inputs.map(x => ErasedEntityTaggedFeature(x.key.map(keys.toMap), x.feature)).toIndexedSeq
      (features, parameterNamesInConfig)
    }
    // consumedFeatures and parameterNames have same order, since they are all from config.inputs
    val producedFeatures = derivationFunction.getOutputFeatureList()
    val outputFeaturesInConfig = FeathrConfigLoader.extractStringList(node.get("features"))
    if (!producedFeatures.containsAll(outputFeaturesInConfig)) {
      throw new FeathrConfigException(
        ErrorLabel.FEATHR_USER_ERROR,
        s"getOutputFeatureList of $derivationClassName should contain all feature names listed in config $node, but found " +
          s"$producedFeatures and $outputFeaturesInConfig, respectively")
    }
    DerivedFeature(consumedFeatures, outputFeaturesInConfig, derivationFunction, parameterNames)
  }
}

/**
 * Loads a DataSource from a json-like config
 */
private[offline] class DataSourceLoader extends JsonDeserializer[DataSource] {

  override def deserialize(p: JsonParser, ctxt: DeserializationContext): DataSource = {
    val codec = p.getCodec
    val node = codec.readTree[TreeNode](p).asInstanceOf[ObjectNode]
    // for now only HDFS can be set, in the future, here may allow more options
    // also to form a unified interface with online
    val dataSourceType = Option(node.get("type")) match {
      case Some(field: TextNode) => field.textValue()
      case _ => "HDFS"
    }

    if (dataSourceType != "HDFS" && dataSourceType != "PASSTHROUGH" && dataSourceType != "KAFKA" && dataSourceType != "SNOWFLAKE") {
      throw new FeathrConfigException(ErrorLabel.FEATHR_USER_ERROR, s"Unknown source type parameter $dataSourceType is used")
    }

    val timePartitionPattern = Option(node.get("timePartitionPattern")).map(_.textValue())
    val postfixPath = Option(node.get("postfixPath")).map(_.textValue())

    // Check for time-stamped features (hasTimeSnapshot) or time-window features (isTimeSeries)
    val sourceFormatType =
      if (timePartitionPattern.isDefined || ConfigLoaderUtils.getBoolean(node, "hasTimeSnapshot")
        || ConfigLoaderUtils.getBoolean(node, "isTimeSeries")) {
        SourceFormatType.TIME_SERIES_PATH
      } else {
        SourceFormatType.FIXED_PATH
      }
    /*
     * path here can be:
     *
     * 1. a HDFS path
     * 2. a placeholder with reserved string "PASSTHROUGH" for anchor defined pass-through features,
     *    since anchor defined pass-through features do not have path
     */
    val path: DataLocation = dataSourceType match {
      case "KAFKA" =>
        Option(node.get("config")) match {
          case Some(field: ObjectNode) =>
            LocationUtils.getMapper().treeToValue(field, classOf[KafkaEndpoint])
          case None => throw new FeathrConfigException(ErrorLabel.FEATHR_USER_ERROR,
                                s"Kafka config is not defined for Kafka source ${node.toPrettyString()}")
          case _ => throw new FeathrConfigException(ErrorLabel.FEATHR_USER_ERROR,
                                s"Illegal setting for Kafka source ${node.toPrettyString()}, expected map")
        }
      case "PASSTHROUGH" => SimplePath("PASSTHROUGH")
      case "SNOWFLAKE" =>
        Option(node.get("location")) match {
          case Some(field: ObjectNode) =>
            LocationUtils.getMapper().treeToValue(field, classOf[Snowflake])
          case None => throw new FeathrConfigException(ErrorLabel.FEATHR_USER_ERROR,
            s"Snowflake config is not defined for Snowflake source ${node.toPrettyString()}")
          case _ => throw new FeathrConfigException(ErrorLabel.FEATHR_USER_ERROR,
            s"Illegal setting for Snowflake source ${node.toPrettyString()}, expected map")
        }
      case _ => Option(node.get("location")) match {
        case Some(field: ObjectNode) =>
          LocationUtils.getMapper().treeToValue(field, classOf[DataLocation])
        case None => throw new FeathrConfigException(ErrorLabel.FEATHR_USER_ERROR,
          s"Data location is not defined for data source ${node.toPrettyString()}")
        case _ => throw new FeathrConfigException(ErrorLabel.FEATHR_USER_ERROR,
          s"Illegal setting for location for data source ${node.toPrettyString()}, expected map")
      }
    }
    println(s"Source location is: $path")

    // time window parameters for data aggregation
    val timeWindowParameterNode = Option(node.get("timeWindowParameters")) match {
      case Some(field: ObjectNode) => Option(field)
      case None => None
      case _ => throw new FeathrConfigException(ErrorLabel.FEATHR_USER_ERROR,
                                s"Illegal setting for timeWindowParameters ${node.toPrettyString()}, expected map")
    }

    val timeWindowParameters = timeWindowParameterNode match {
      case Some(node: ObjectNode) =>
        if (node.has("timestamp")) { // legacy configurations
          TimeWindowParams(node.get("timestamp") match {
            case field: TextNode => field.textValue()
            case _ => throw new FeathrConfigException(ErrorLabel.FEATHR_USER_ERROR, "field 'timestamp is not correctly set")
          }, node.get("timestamp_format") match {
            case field: TextNode => field.textValue()
            case _ => throw new FeathrConfigException(ErrorLabel.FEATHR_USER_ERROR, "field 'timestamp_format' is not correctly set")
          })
        } else {
          TimeWindowParams(node.get("timestampColumn") match {
            case field: TextNode => field.textValue()
            case _ => throw new FeathrConfigException(ErrorLabel.FEATHR_USER_ERROR, "field 'timestampColumn is not correctly set")
          }, node.get("timestampColumnFormat") match {
            case field: TextNode => field.textValue()
            case _ => throw new FeathrConfigException(ErrorLabel.FEATHR_USER_ERROR, "field 'timestampColumnFormat' is not correctly set")
          })
        }
      case None => null
    }
    if (path.isInstanceOf[KafkaEndpoint]) {
      DataSource(path, sourceFormatType)
    } else {
      DataSource(path, sourceFormatType, Option(timeWindowParameters), timePartitionPattern, postfixPath)
    }
  }
}
