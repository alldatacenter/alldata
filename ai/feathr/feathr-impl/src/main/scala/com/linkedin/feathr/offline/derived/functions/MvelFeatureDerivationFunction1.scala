package com.linkedin.feathr.offline.derived.functions

import com.linkedin.feathr.common
import com.linkedin.feathr.common.{FeatureDerivationFunction, FeatureTypeConfig}
import com.linkedin.feathr.offline.FeatureValue
import com.linkedin.feathr.offline.mvel.plugins.FeathrExpressionExecutionContext
import com.linkedin.feathr.offline.mvel.{FeatureVariableResolverFactory, MvelContext, MvelUtils}
import org.mvel2.MVEL

/**
 * A derivation function defined via an MVEL expression.
 * Unlike SimpleMvelDerivationFunction, this class is not for one-liners, and is useful for situations where
 * the feature names aren't (or can't be) given directly in a single expression. For example, see the example
 * config below:
 *
 *   example_derived_feature: {
 *     key: [viewerId, vieweeId]
 *     input: {
 *       x: { keyTag: viewerId, feature: member_connectionCount }
 *       y: { keyTag: vieweeId, feature: member_connectionCount }
 *     }
 *     definition: "x - y"
 *   }
 */
private[offline] class MvelFeatureDerivationFunction1(
  inputFeatures: Seq[String],
  expression: String,
  featureName: String,
  featureTypeConfigOpt: Option[FeatureTypeConfig] = None)
  extends FeatureDerivationFunction {
  var mvelContext: Option[FeathrExpressionExecutionContext] = None

  val parameterNames: Seq[String] = inputFeatures

  private val compiledExpression = {
    val parserContext = MvelContext.newParserContext()
    MVEL.compileExpression(expression, parserContext)
  }

  override def getFeatures(inputs: Seq[Option[common.FeatureValue]]): Seq[Option[common.FeatureValue]] = {
    val argMap = (parameterNames zip inputs).toMap
    val variableResolverFactory = new FeatureVariableResolverFactory(argMap)

    MvelUtils.executeExpression(compiledExpression, null, variableResolverFactory, featureName, mvelContext) match {
      case Some(value) =>
        val featureTypeConfig = featureTypeConfigOpt.getOrElse(FeatureTypeConfig.UNDEFINED_TYPE_CONFIG)
        if (value.isInstanceOf[common.FeatureValue]) {
          // The dependent feature values could have been converted to FeatureValue already, e.g. using MVEL
          // to rename an anchored feature where MVEL is just returning the original feature value
          Seq(Some(value.asInstanceOf[common.FeatureValue]))
        } else {
          // If mvel returns some 'raw' value, use feature value to build FeatureValue object
          Seq(Some(FeatureValue.fromTypeConfig(value, featureTypeConfig)))
        }
      case None => Seq(None) // undefined
    }
  }
}

