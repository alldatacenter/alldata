package com.linkedin.feathr.offline

import com.linkedin.feathr.cli.FeatureExperimentEntryPoint
import org.testng.annotations.{Ignore, Test}

/**
 * Execute FeatureExperimentEntryPoint.main in the context of test environment
 * that has all the `provided` jars, and can be run from the IDE
 */
object GatewayTest {
  def main(args: Array[String]): Unit = {
    FeatureExperimentEntryPoint.main(Array())
    Thread.sleep(Long.MaxValue)
  }
}