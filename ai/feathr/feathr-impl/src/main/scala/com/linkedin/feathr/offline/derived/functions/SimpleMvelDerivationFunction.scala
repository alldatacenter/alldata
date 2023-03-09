package com.linkedin.feathr.offline.derived.functions

import com.linkedin.feathr.common
import com.linkedin.feathr.common.{FeatureDerivationFunction, FeatureTypeConfig}
import com.linkedin.feathr.offline.FeatureValue
import com.linkedin.feathr.offline.mvel.plugins.FeathrExpressionExecutionContext
import com.linkedin.feathr.offline.mvel.{FeatureVariableResolverFactory, MvelContext, MvelUtils}
import com.linkedin.feathr.offline.testfwk.TestFwkUtils
import org.apache.logging.log4j.LogManager
import org.mvel2.MVEL

import scala.collection.convert.wrapAll._

/**
 * A FeatureDeriver defined via an MVEL expression
 *
 * @param expression the MVEL expression
 */
private[offline] class SimpleMvelDerivationFunction(expression: String, featureName: String, featureTypeConfigOpt: Option[FeatureTypeConfig] = None)
    extends FeatureDerivationFunction {
  @transient private lazy val log = LogManager.getLogger(getClass)

  var mvelContext: Option[FeathrExpressionExecutionContext] = None
  // strictMode should only be modified by FeathrConfigLoader when loading config, default value to be false
  var strictMode = false

  // IN ORDER:
  val dependencyFeatureNames: Seq[String] = {
    val parserContext = MvelContext.newParserContext()
    MVEL.analysisCompile(expression, parserContext) // SIDE EFFECT: fills in parserContext with the list of input variables
    // (we will interpret this as the list of dependency features)
    // MVEL Hack: remove '$' from the inputs, since it's a "special" input used for fold/projection statements
    parserContext.getInputs.keys.filterNot(_.equals("$")).toSeq.distinct
  }

  private val compiledExpression = {
    val parserContext = MvelContext.newParserContext()
    MVEL.compileExpression(expression, parserContext)
  }

  override def getFeatures(inputFeatureValues: Seq[Option[common.FeatureValue]]): Seq[Option[common.FeatureValue]] = {
    val args = (dependencyFeatureNames zip inputFeatureValues).toMap

    MvelContext.ensureInitialized()

    // In order to prevent MVEL from barfing if a feature is null, we use a custom variable resolver that understands `Option`
    val variableResolverFactory = new FeatureVariableResolverFactory(args)

    if (TestFwkUtils.IS_DEBUGGER_ENABLED) {
      while(TestFwkUtils.DERIVED_FEATURE_COUNTER > 0) {
        TestFwkUtils.DERIVED_FEATURE_COUNTER = TestFwkUtils.DERIVED_FEATURE_COUNTER - 1
        println(f"${Console.GREEN}Your inputs to the derived feature({$expression}) with detailed type info: {$args}${Console.RESET}")
      }
    }

    MvelUtils.executeExpression(compiledExpression, null, variableResolverFactory, featureName, mvelContext) match {
      case Some(value) =>
        val featureTypeConfig = featureTypeConfigOpt.getOrElse(FeatureTypeConfig.UNDEFINED_TYPE_CONFIG)
        val featureValue = FeatureValue.fromTypeConfig(value, featureTypeConfig)
        Seq(Some(featureValue))
      case None => Seq(None) // undefined
    }
  }
}
