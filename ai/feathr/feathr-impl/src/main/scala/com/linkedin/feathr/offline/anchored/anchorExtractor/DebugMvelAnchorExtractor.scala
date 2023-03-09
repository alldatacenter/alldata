package com.linkedin.feathr.offline.anchored.anchorExtractor

import com.linkedin.feathr.offline.config.MVELFeatureDefinition
import com.linkedin.feathr.offline.mvel.{MvelContext, MvelUtils}
import org.mvel2.MVEL

import java.io.Serializable
import scala.collection.convert.wrapAll._

private[offline] class DebugMvelAnchorExtractor(keyExprs: Seq[String], features: Map[String, MVELFeatureDefinition])
    extends SimpleConfigurableAnchorExtractor(keyExprs, features) {
  private val debugExpressions = features.mapValues(value => findDebugExpressions(value.featureExpr)).map(identity)
  private val debugCompiledExpressions = debugExpressions.mapValues(_.map(x => (x, compile(x)))).map(identity)

  def evaluateDebugExpressions(input: Any): Map[String, Seq[(String, Any)]] = {
    debugCompiledExpressions
      .mapValues(_.map {
        case (expr, compiled) =>
          (expr, MvelUtils.executeExpression(compiled, input, null, "", None).orNull)
      })
      .map(identity)
  }

  private def compile(expression: String): Serializable = {
    MVEL.compileExpression(expression, MvelContext.newParserContext)
  }

  private def findDebugExpressions(expression: String): Seq[String] = {
    val parserContext = MvelContext.newParserContext
    MVEL.analysisCompile(expression, parserContext)
    parserContext.getInputs.keys.filterNot(_.equals("$")).toList ++ List(expression)
  }
}
