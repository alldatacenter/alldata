package com.linkedin.feathr.offline.mvel

import com.linkedin.feathr.offline.mvel.plugins.FeathrExpressionExecutionContext
import org.apache.commons.lang.exception.ExceptionUtils
import org.apache.logging.log4j.LogManager
import org.mvel2.PropertyAccessException
import org.mvel2.integration.VariableResolverFactory

private[offline] object MvelUtils {
  @transient private lazy val log = LogManager.getLogger(getClass)

  val warningCountThreshold = 50000
  var warningCount = 0

  // Allow references to null fields to result in the whole result being "undefined"
  // This approach has pros and cons and will likely be controversial
  // But it should allow for much simpler expressions for extracting features from data sets whose values may often be null
  // (We might not want to check for null explicitly everywhere)
  def executeExpression(compiledExpression: Any, input: Any, resolverFactory: VariableResolverFactory, featureName: String = "", mvelContext: Option[FeathrExpressionExecutionContext]): Option[AnyRef] = {
    try {
      Option(MvelContext.executeExpressionWithPluginSupportWithFactory(compiledExpression, input, resolverFactory, mvelContext.orNull))
    } catch {
      case e: RuntimeException =>
        log.debug(s"Expression $compiledExpression on input record $input threw exception", e)
        if (e.isInstanceOf[NullPointerException]
            || ExceptionUtils.getRootCause(e).isInstanceOf[NullPointerException]
            // Reading Map field would return a IllegalArgumentException from MVEL if the map key can not be found
            // in some data records, catching the exception here and force it to return null.
            || ExceptionUtils.getRootCause(e).isInstanceOf[IllegalArgumentException]
            // Reading Map field in new version of MVEL library would return a PropertyAccessException from MVEL if the
            // map key can not be found, catching the exception here and force it to return null. See https://github.com/mvel/mvel/issues/226
            || e.isInstanceOf[PropertyAccessException]) {
          if (warningCount == 0) {
            log.warn(s"Handling exception from MVEL expression $compiledExpression for feature $featureName gracefully by returning empty result", e)
          }
          warningCount = (warningCount + 1) % warningCountThreshold
          None
        } else {
          log.error(s"Unhandled exception evaluating expression $compiledExpression on record $input", e)
          // Try to fail fast in case of any other exceptions not related to the NPE-flexibility described above
          throw e
        }
    }
  }
}
