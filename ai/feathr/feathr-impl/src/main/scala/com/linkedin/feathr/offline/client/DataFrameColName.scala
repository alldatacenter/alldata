package com.linkedin.feathr.offline.client

import com.google.common.annotations.VisibleForTesting
import com.linkedin.feathr.common._
import com.linkedin.feathr.common.exception.{ErrorLabel, FeathrFeatureTransformationException}
import com.linkedin.feathr.offline.anchored.feature.FeatureAnchorWithSource
import com.linkedin.feathr.offline.derived.DerivedFeature
import com.linkedin.feathr.offline.job.FeatureTransformation.{FEATURE_NAME_PREFIX, FEATURE_TAGS_PREFIX}
import com.typesafe.config.ConfigFactory
import org.apache.commons.codec.binary.Base32
import org.apache.spark.sql.DataFrame

import scala.collection.convert.wrapAll._

/**
 * Utility class to create compliant column names for Spark DataFrames
 */
object DataFrameColName {
  // column name for a unique ID column in the observation data
  val UidColumnName: String = "__feathr_internal_uid__"

  /**
   * Delimiter between parts of a Feature Ref as used in a DataFrame column name.
   */
  @Experimental
  val FEATURE_REF_DELIM_IN_COLNAME: String = "__"

  /**
   * Delimiter between key tags as used in a DataFrame column name.
   */
  @Experimental
  val KEYTAG_DELIM_IN_COLNAME: String = "_"

  @Experimental
  val FEATURE_REF_KEYTAG_DELIM_IN_COLNAME: String = "__"

  private[offline] val EMPTY_STRING = ""

  // delimiter between the readable feature column name part and the encoded feature column part
  private val ENCODED_FEATURE_COLUMN_NAME_DELIM: String = "__"
  // magic string that denotes the feature column name is base32-encoded if it ends with this string
  private val ENCODED_FEATURE_REF_ENDING = "__base32_encoded"

  private val MAJOR_VERSION = "major"
  private val MINOR_VERSION = "minor"
  private val FEATURE_NAME = "n"
  private val FEATURE_NAMESPACE = "ns"
  // base32 encoding may end with padding chars '='
  private val BASE32_PADDING = "="

  private val JOIN_KEY_COLUMN_EXPR_DELIMITER = "__"
  private val JOIN_KEY_COLUMN_RAW_EXPRESSION_DELIMITER = "_"


  /**
   * get column name in the output dataframe of feathr join output
   * This function should be only used when a feature is requested with multiple key tags in a feathr join,
   * i.e. caller should be aware that this function will always preppend the key tags passed in,
   * the current output feature column should skip the key tag if a feature is only requested
   * using one set of key tags, so they should not use this function to create feature column name in this case,
   * or should pass in empty key tags
   * @param featureRefStr feature ref
   * @param tags  string tags of the feature
   * @return
   */
  def getFeatureOutputColumnName(featureRefStr: String, tags: Seq[String]): String = {
    // if key tag has non-alphanumeric characters, use encoded version for now

    val safeFeatureRefStr = featureRefStr.replaceAll(TypedRef.DELIM, FEATURE_REF_DELIM_IN_COLNAME)
    if (tags.isEmpty) {
      safeFeatureRefStr
    } else {
      val tagStr = if (tags.count(_.matches("[\\w]+")) != tags.size) {
        // replace all special chars so that the key tags are more user friendly
        tags.map(_.replaceAll("[^\\w]", KEYTAG_DELIM_IN_COLNAME)).mkString(KEYTAG_DELIM_IN_COLNAME)
      } else {
        tags.mkString(KEYTAG_DELIM_IN_COLNAME)
      }
      tagStr + FEATURE_REF_KEYTAG_DELIM_IN_COLNAME + safeFeatureRefStr
    }
  }

  /**
   * Gets the column name representation(without encoding) of the specified FeatureRef string
   * @param featureRefStr [[FeatureRef]] string
   * @return Representation of [[FeatureRef]] string as used in DataFrame column name.
   */
  @Experimental
  def getFeatureRefStrInColName(featureRefStr: String): String = {
    featureRefStr.replaceAll(TypedRef.DELIM, FEATURE_REF_DELIM_IN_COLNAME)
  }

  /**
   * generate the standardized feature column name to be used in DataFrame
   * return tagged feature name, such as __feathr_feature_xxx__feathr_tags_yyy_zzz
   * @param tags keytags to be appended
   * @param featureRefStr feature name to be standardized, may already standardized without tags before
   * @return
   */
  private[offline] def genFeatureColumnName(featureRefStr: String, tags: Option[Seq[String]] = None): String = {
    val namePrefix = if (featureRefStr.startsWith(FEATURE_NAME_PREFIX)) {
      EMPTY_STRING // already standardized name prefix, do not append multiple time
    } else {
      FEATURE_NAME_PREFIX
    }
    val tagStr = if (featureRefStr.contains(FEATURE_TAGS_PREFIX) && tags.isDefined) {
      throw new FeathrFeatureTransformationException(ErrorLabel.FEATHR_USER_ERROR, s"Should never add tags to feature column name $featureRefStr twice")
    } else if (tags.isDefined) {
      FEATURE_TAGS_PREFIX + tags.get.map(encodeString).mkString("_")
    } else {
      EMPTY_STRING
    }
    namePrefix + featureRefStr + tagStr
  }

  /**
   * get feature tags out of an column
   * @param columnName
   * @param encoded request for decoded tags or encoded tags
   * @return
   */
  private[offline] def getFeatureTagListFromColumn(columnName: String, encoded: Boolean): Seq[String] = {
    if (columnName.contains(FEATURE_TAGS_PREFIX)) {
      val begin = columnName.indexOf(FEATURE_TAGS_PREFIX) + FEATURE_TAGS_PREFIX.length
      val encodedTagsStr = columnName.substring(begin)
      if (!encoded) {
        encodedTagsStr
          .split(KEYTAG_DELIM_IN_COLNAME)
          .map(
            tag =>
              // use base32 instead of base64 which may have special chars such as '+' and '/"
              new String(new Base32().decode(tag)))
      } else {
        encodedTagsStr.split(KEYTAG_DELIM_IN_COLNAME)
      }
    } else {
      Seq(EMPTY_STRING)
    }
  }

  /**
   * Helper method to create a DataFrame column name from keyTags and string representation of [[FeatureRef]].
   * The syntax in (extended) BNF is featureRefStr__keyTags where
   * featureRefStr := (namespace__)?name(__major__minor)?
   * keyTags := (key_)*key
   *
   * @param keyTags list of keys
   * @param featureRefStr string representation of FeatureRef
   * @return column name
   */
  @Experimental
  private[offline] def getDFColName(keyTags: Seq[String], featureRefStr: String): String = {
    getFeatureOutputColumnName(featureRefStr, keyTags)
  }

  /**
   * get the encoded FeatureRef as a string which will be used as part of the feature column name internally,
   * the reason to encode the string is that we will be able to decode the string to reconstruct the featureRef from
   * the encoded string in the future.
   * We use base32 encoding so that the encoded string does not have special chars and can be used in the dataframe column name
   * e.g. if the input featureRefStr is feathr-f1-1-0, this function will return
   * feathr__f1__1__0__PMFCACRANZZTUIDGOJQW2ZJMBIQCAIBAEAQCACRANY5CAZRRBIQAUIDNMFVG64R2GEWAUIDNNFXG64R2GAFCAIBAEAQCACT5BIQCAIBAEAQA__base32_encoded
   * which consists of the following components:
   * 1. replace '-' in the input featureRef string with '__', e.g. feathr__f1__1__0
   * 2. create the a HOCON string which has all the featureRef components explicitly defined, e.g.
   * {
   *   ns: feathr,
   *   n: f1
   *   major:1,
   *   minor:0
   * }
   * 3. encode the HOCON string using base32 e.g. PMFCACRANZZTUIDGOJQW2ZJMBIQCAIBAEAQCACRANY5CAZRRBIQAUIDNMFVG64R2GEWAUIDNNFXG64R2GAFCAIBAEAQCACT5BIQCAIBAEAQA
   * 4. append a magic string "__base32_encoded"
   *
   * @param featureRefStr featureRef string, e.g. feathr-f1-1-0, or simply feature name f1
   * @return  encoded featureRef string, which can be used in the dataframe column name, and used to reconstruct the featureRef later
   */
  private[offline] def getEncodedFeatureRefStrForColName(featureRefStr: String): String = {
    if (featureRefStr.contains(TypedRef.DELIM)) {
      // replace all '-' with '__' to be able to used in dataframe column name, but since feature name or feature namespace
      // themselves could have '__', after the replacement, we will not be able to recover the original featureRef
      // to achieve this, we will encode the featureRef using base32 in this function as well
      val readableFeatureRefStr = featureRefStr.replaceAll(TypedRef.DELIM, FEATURE_REF_DELIM_IN_COLNAME)
      val featureRef = new FeatureRef(featureRefStr)
      val versionStr = EMPTY_STRING
      val namespace = EMPTY_STRING
      val encodedFeatureRefStr =
        s"""{
           | ${namespace}
           | ${FEATURE_NAME} : ${featureRef.getName}
           | ${versionStr}
           |}
      """.stripMargin
      readableFeatureRefStr + ENCODED_FEATURE_COLUMN_NAME_DELIM + encodeString(encodedFeatureRefStr) + ENCODED_FEATURE_REF_ENDING
    } else featureRefStr
  }

  /**
   * Returns the FeatureRef string from the its representation in DataFrame column name.
   * @param featureRefStrInColName Representation of [[FeatureRef]] string as used in DataFrame column name.
   * @return String representation of FeatureRef. It conforms to the syntax (namespace-)?name(-major-minor)?
   */
  private[offline] def getFeatureRefStrFromColumnName(featureRefStrInColName: String): String = {
    // if it is encoded, which means user is using featureRef string such as feathr-f1-1-0 to request feature, otherwise,
    // user is just using simple feature name to request feature
    if (featureRefStrInColName.endsWith(ENCODED_FEATURE_REF_ENDING)) {
      val firstPart = featureRefStrInColName.substring(0, featureRefStrInColName.indexOf(ENCODED_FEATURE_REF_ENDING))
      val encodedFeatureRefStart = firstPart.lastIndexOf(ENCODED_FEATURE_COLUMN_NAME_DELIM)
      val encodedFeatureRefStr = firstPart.substring(encodedFeatureRefStart + ENCODED_FEATURE_COLUMN_NAME_DELIM.length)
      val decodedFeatureRefInHOCON = new String(new Base32().decode(encodedFeatureRefStr))
      val cfg = ConfigFactory.parseString(decodedFeatureRefInHOCON)
      val namespace = if (cfg.hasPath(FEATURE_NAMESPACE)) {
        cfg.getString(FEATURE_NAMESPACE) + TypedRef.DELIM
      } else EMPTY_STRING
      val fn = cfg.getString(FEATURE_NAME)
      val version = if (cfg.hasPath(MAJOR_VERSION)) {
        TypedRef.DELIM + cfg.getString(MAJOR_VERSION) + TypedRef.DELIM + cfg.getString(MINOR_VERSION)
      } else EMPTY_STRING
      namespace + fn + version
    } else featureRefStrInColName
  }

  /**
   * Generate a map of a keyTagged feature (feature name, keyTags), to the final
   * output column name based on the following rules.
   * 1. if a feature is requested with only one set of key tags, the column name in the output dataframe
   *   for this string tagged feature would be feature name itself
   *
   * 2. if a feature is requested with multiple set of key tags, the column name in the output dataframe
   *     for each string tagged feature with same feature name would be [conatenated string
   *     tags using '_' and all non-word characters in the string tag will be replaced by underscores]__
   *    [featureNamespace]__?[featureName]__[major]?__[minor]?
   *
   * @param contextDF The dataframe with all the feathr generated features
   * @return A map of the taggedFeatureName to a combination of the old column name and new column name.
   */
  private[offline] def getTaggedFeatureToNewColumnName(contextDF: DataFrame): Map[TaggedFeatureName, (String, String)] = {
    // find the feature name to column name in the input dataframe
    val featureRefStrToColumnNameMap = contextDF.columns.collect {
      case columnName if getFeatureRefStrFromColumnNameOpt(columnName).isDefined =>
        val featureName = getFeatureRefStrFromColumnNameOpt(columnName).get
        (featureName, columnName)
    }

    // generate the tagged feature to (original column name, new output column name)
    featureRefStrToColumnNameMap
      .groupBy(_._1) // group by featureName
      .map {
        case (featureRefStr, featureNameColumnNamePairs) =>
          if (featureNameColumnNamePairs.length > 1) { // if has more than 1 tags for one feature, keep the tags in the output
            featureNameColumnNamePairs
              .map(featureNameColumnNamePair => {
                // same feature name are requested with different key tags
                // in such cases, we will name the stringTaggedFeature column as
                // base64 encoded
                val columnName = featureNameColumnNamePair._2
                // use decoded tags so that we can create correct stringTaggedFeature (use to do look up later)
                val tagList = DataFrameColName.getFeatureTagListFromColumn(columnName, false)

                // use encoded tags to construct new output column name, so that there's no special character in the
                // output column name
                val taggedFeature = new TaggedFeatureName(tagList, featureRefStr)

                val outputColumnName = DataFrameColName.getFeatureOutputColumnName(featureRefStr, tagList)
                (taggedFeature, (columnName, outputColumnName))
              })
              .toSeq
          } else {
            val columnName = featureNameColumnNamePairs.head._2
            val tags = DataFrameColName.getFeatureTagListFromColumn(columnName, false)
            val taggedFeature = new TaggedFeatureName(tags, featureRefStr)

            val outputColumnName = DataFrameColName.getFeatureOutputColumnName(featureRefStr, Seq())
            Seq((taggedFeature, (columnName, outputColumnName)))
          }
      }
      .flatten
      .toMap
  }

  /**
   * Helper method to generate the headers and the final dataframe after renaming the columns with output name
   * @param contextDF The dataframe which is to be renamed
   * @param taggedFeatureToColumnName Map from [[TaggedFeatureName]] to the column name.
   * @param allAnchoredFeatures   Map of all Anchored features to the [[FeatureAnchorWithSource]] object
   * @param allDerivedFeatures  Map of all Derived features to the [[FeatureAnchorWithSource]] object
   * @param inferredFeatureTypes  Map of feature name to [[FeatureTypeConfig]]
   * @return  DataFrame with renamed columns and [[header]] for every column
   */
  private[offline] def adjustFeatureColNamesAndGetHeader(contextDF: DataFrame,
    taggedFeatureToColumnName: Map[TaggedFeatureName, (String, String)],
    allAnchoredFeatures: Map[String, FeatureAnchorWithSource],
    allDerivedFeatures: Map[String, DerivedFeature],
    inferredFeatureTypes: Map[String, FeatureTypeConfig]): (DataFrame, Header) = {
    // rename feature column to output format
    val renamedDF = taggedFeatureToColumnName.foldLeft(contextDF)((baseDF, renamePair) => baseDF.withColumnRenamed(renamePair._2._1, renamePair._2._2))

    val taggedFeatureToOutputColumnName = taggedFeatureToColumnName.map(pr => (pr._1, pr._2._2))
    val header = generateHeader(taggedFeatureToOutputColumnName, allAnchoredFeatures, allDerivedFeatures, inferredFeatureTypes)
    (renamedDF, header)
  }

  /**
   * get featureRef string out from an encoded column name, if return empty, it means that the input column name is not for a feature
   * e.g.
   * __feathr_feature_feathr__f1__1__0__PMFCACRANZZTUIDGOJQW2ZJMBIQCAIBAEAQCACRANY5CAZRRBIQAUIDNMFVG64R2GEWAUIDNNFXG64R2GAFCAIBAEAQCACT5BIQCAIBAEAQA__
   * base32_encoded__feathr_tags_NVSW2YTFOJEWIKZQ
   *
   * @return feature name extracted from the standardized feature column name,
   *         return None if input is not a valid standardized feature column name
   */
  private[offline] def getFeatureRefStrFromColumnNameOpt(columnName: String): Option[String] = {
    if (columnName.contains(FEATURE_NAME_PREFIX)) {
      val begin = FEATURE_NAME_PREFIX.length
      val end = if (columnName.contains(FEATURE_TAGS_PREFIX)) {
        columnName.indexOf(FEATURE_TAGS_PREFIX)
      } else {
        columnName.length
      }
      val featureRefStrInColumnName = columnName.substring(begin, end)
      Some(getFeatureRefStrFromColumnName(featureRefStrInColumnName))
    } else {
      None
    }
  }

  /**
   * Generate a column name for join key expressions.
   * @param columnNameExprs join key expression
   * @return column name
   */
  private[offline] def generateJoinKeyColumnName(columnNameExprs: Seq[String]): String = {
    val rawExprs = columnNameExprs.map(_.replaceAll("[^\\w]", "")).mkString(JOIN_KEY_COLUMN_EXPR_DELIMITER)
    rawExprs + JOIN_KEY_COLUMN_RAW_EXPRESSION_DELIMITER + encodeString(columnNameExprs.mkString(JOIN_KEY_COLUMN_EXPR_DELIMITER))
  }

  /**
   * Given a list of [[JoiningFeatureParams]] and a corresponding featureRef, keyTags, timeDelay and dateParams,
   * get the featureAlias string associated with it, if any.
   * @param joiningFeatureParams Seq of joining feature params object
   * @param featureRef  Feature ref string of the matching object
   * @param keyTags Seq of keyTags
   * @param dateParam option of [[DateParam]] object
   * @param timeDelay option of override time delay parameter.
   * @return feature alias string associated with the above parameters, if any.
   */
  private[offline] def getFeatureAlias(joiningFeatureParams: Seq[JoiningFeatureParams], featureRef: String, keyTags: Seq[String],
    dateParam: Option[DateParam], timeDelay: Option[String]): Option[String] = {
    joiningFeatureParams.filter(y => y.featureAlias.isDefined).map (x =>
      if (x.featureName == featureRef && x.keyTags.equals(keyTags) && x.dateParam == dateParam
        && x.timeDelay == timeDelay) {
        x.featureAlias
      } else {
        None
      }).find(_.isDefined).flatten
  }

  /**
   * generate header info (e.g, feature type, feature column name map) for output dataframe of
   * feature join or feature generation
   *
   * @param featureToColumnNameMap map of feature to its column name in the dataframe
   * @param inferredFeatureTypeConfigs feature name to inferred feature types
   * @return header info for a dataframe that contains the features in featureToColumnNameMap
   */
  @VisibleForTesting
  def generateHeader(
      featureToColumnNameMap: Map[TaggedFeatureName, String],
      allAnchoredFeatures: Map[String, FeatureAnchorWithSource],
      allDerivedFeatures: Map[String, DerivedFeature],
      inferredFeatureTypeConfigs: Map[String, FeatureTypeConfig]): Header = {
    // generate a map of feature name to its feature type
    // if the feature type is unspecified in the anchor config, we will use FeatureTypes.UNSPECIFIED
    val anchoredFeatureTypes: Map[String, FeatureTypeConfig] = allAnchoredFeatures.map {
      case (featureName, anchorWithSource) =>
        val featureTypeOpt = anchorWithSource.featureAnchor.featureTypeConfigs.get(featureName)
        // Get the actual type in the output dataframe, the type is inferred and stored previously, if not specified by users
        val inferredType = inferredFeatureTypeConfigs.getOrElse(featureName, FeatureTypeConfig.UNDEFINED_TYPE_CONFIG)
        val featureType = featureTypeOpt.getOrElse(inferredType)
        featureName -> featureType
    }

    val derivedFeatureTypes: Map[String, FeatureTypeConfig] = allDerivedFeatures.flatMap {
      case (_, derivedFeature) =>
        derivedFeature.featureTypeConfigs
    }
    val allFeatureTypes = inferredFeatureTypeConfigs.map(x => (x._1, x._2)) ++ derivedFeatureTypes ++ anchoredFeatureTypes
    val featuresInfo = featureToColumnNameMap.map {
      case (taggedFeatureName, columnName) =>
        val featureInfo = new FeatureInfo(columnName, allFeatureTypes.getOrElse(taggedFeatureName.getFeatureName,
          FeatureTypeConfig.UNDEFINED_TYPE_CONFIG))
        taggedFeatureName -> featureInfo
    }
    new Header(featuresInfo)
  }

  /*
   * Encode the tags with no padding (i.e. '='), so that there will be no special characters in the generated
   * column names, otherwise, will result in unexpected sql parser error when used in derivation
   */
  private def encodeString(tag: String): String = {
    // use base32 instead of base64 which may have special chars such as '+' and '/"
    // we also don't need to padding chars in the end
    new Base32().encodeAsString(tag.getBytes()).replaceAll(BASE32_PADDING, "")
  }
}
