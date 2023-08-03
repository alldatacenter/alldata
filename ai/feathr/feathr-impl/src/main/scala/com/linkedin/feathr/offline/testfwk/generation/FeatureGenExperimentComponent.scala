package com.linkedin.feathr.offline.testfwk.generation


import com.linkedin.feathr.offline.testfwk.TestFwkUtils

import java.io.File
import java.util.Optional

/**
 * A simple component to collect pretty-print result for local feature gen job.
 */
class FeatureGenExperimentComponent() {
  def prettyPrintFeatureGenResult(mockDataDir: String, featureNames: String, featureDefDir: String): String = {
    val genConf = s"""
                 |operational: {
                 |  name: generateWithDefaultParams
                 |  endTime: 2021-01-02
                 |  endTimeFormat: "yyyy-MM-dd"
                 |  resolution: DAILY
                 |  output:[]
                 |}
                 |features: [${featureNames}]""".stripMargin

    // enable debugger logging for local testing mode
    TestFwkUtils.IS_DEBUGGER_ENABLED = true
    TestFwkUtils.DERIVED_FEATURE_COUNTER = 10
    println()
    println()
    println(f"${Console.GREEN}===========================================${Console.RESET}")
    println(f"${Console.GREEN}Printing debug info:${Console.RESET}")
    println(f"${Console.GREEN}  - Only show 10 rows for the data table.${Console.RESET}")
    println(f"${Console.GREEN}  - Debug is printed in the execution order.${Console.RESET}")


    val featureDefFiles = getListOfFiles(featureDefDir).map(file => file.toString)
    val resourceLocations = Map(
      FeathrGenTestComponent.LocalGenConfString -> List(genConf),
      FeathrGenTestComponent.LocalConfPaths -> featureDefFiles)
    println(f"${Console.GREEN}Testing features in ${featureDefFiles.mkString(",")}${Console.RESET}")
    val feathrGenTestComponent = new FeathrGenTestComponent(resourceLocations, List()) //TODO: investigate FeatureExperimentEntryPoint instantation issue and re-add data path handlers
    val (validationRes, validationErrorMsg) = feathrGenTestComponent.validate(featureNames, mockDataDir)

    if (!validationRes) {
      validationErrorMsg
    } else {
      val feathrGenTestComponentResult = feathrGenTestComponent.run(mockDataDir, Optional.empty())
      var prettyPrintResult = ""
      // in test mode, we always only compute one anchor
      val featurePair = feathrGenTestComponentResult.toSeq.head
      val schemaTreeString = featurePair._2.data.schema.treeString
      val producedFeatureNames = feathrGenTestComponentResult.toSeq.map(x => x._1.getFeatureName).mkString(", ")
      prettyPrintResult = prettyPrintResult + "\nThe following features are calculated: \n"
      prettyPrintResult = prettyPrintResult + "\033[92m" + producedFeatureNames + "\033[0m \n"

      prettyPrintResult = prettyPrintResult + "\nYour feature schema is: \n"
      prettyPrintResult = prettyPrintResult + "\033[92m" + schemaTreeString + "\033[0m \n"

      prettyPrintResult = prettyPrintResult + "Your feature value is: \n"
      val features = featurePair._2.data.collect()
      features.foreach(feature => {prettyPrintResult = prettyPrintResult + "\033[92m" + feature.toString + "\033[0m \n"})
      prettyPrintResult
    }
  }

  def getListOfFiles(dir: String):List[File] = {
    val d = new File(dir)
    if (d.exists && d.isDirectory) {
      d.listFiles.filter(_.isFile).toList
    } else {
      List[File]()
    }
  }
}
