/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.celeborn

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths, StandardOpenOption}

import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer

import org.scalatest.funsuite.AnyFunSuite

import org.apache.celeborn.common.CelebornConf
import org.apache.celeborn.common.internal.config.ConfigEntry
import org.apache.celeborn.common.util.Utils

/**
 * End-to-end test cases for configuration documentation.
 *
 * The golden result file is "docs/configuration/".
 *
 * To run the entire test suite:
 * {{{
 *   build/mvn clean test -pl common -am -Dtest=none -DwildcardSuites=org.apache.celeborn.ConfigurationSuite
 * }}}
 *
 * To re-generate golden files for entire suite, run:
 * {{{
 *   UPDATE=1 build/mvn clean test -pl common -am -Dtest=none -DwildcardSuites=org.apache.celeborn.ConfigurationSuite
 * }}}
 */
class ConfigurationSuite extends AnyFunSuite {

  val confEntries: Seq[ConfigEntry[_]] = CelebornConf.confEntries.values.asScala.toSeq

  val confMarkdownDir: Path = Paths
    .get(Utils.getCodeSourceLocation(getClass).split("common/target").head)
    .resolve("docs")
    .resolve("configuration")
    .normalize

  test("docs - configuration/client.md") {
    generateConfigurationMarkdown("client")
  }

  test("docs - configuration/columnar-shuffle.md") {
    generateConfigurationMarkdown("columnar-shuffle")
  }

  test("docs - configuration/master.md") {
    generateConfigurationMarkdown("master")
  }

  test("docs - configuration/worker.md") {
    generateConfigurationMarkdown("worker")
  }

  test("docs - configuration/quota.md") {
    generateConfigurationMarkdown("quota")
  }

  test("docs - configuration/network.md") {
    generateConfigurationMarkdown("network")
  }

  test("docs - configuration/metrics.md") {
    generateConfigurationMarkdown("metrics")
  }

  test("docs - configuration/ha.md") {
    generateConfigurationMarkdown("ha")
  }

  def generateConfigurationMarkdown(category: String): Unit = {
    val markdown = confMarkdownDir.resolve(s"$category.md")

    val output = new ArrayBuffer[String]
    appendLicenseHeader(output)
    appendBeginInclude(output)
    appendConfigurationTableHeader(output)

    val categoryConfEntries = confEntries.filter(_.categories contains category)

    categoryConfEntries
      .filter(_.isPublic)
      .sortBy(_.key)
      .foreach { entry =>
        val seq = Seq(
          s"${escape(entry.key)}",
          s"${escape(entry.defaultValueString)}",
          s"${entry.doc}",
          s"${entry.version}")
        output += seq.mkString("| ", " | ", " | ")
      }
    appendEndInclude(output)

    verifyOutput(markdown, output)
  }

  def escape(s: String): String = s
    .replace("<", "&lt;")
    .replace(">", "&gt;")

  def appendLicenseHeader(output: ArrayBuffer[String]): Unit = {
    output += "---"
    output += "license: |"
    output += "  Licensed to the Apache Software Foundation (ASF) under one or more"
    output += "  contributor license agreements.  See the NOTICE file distributed with"
    output += "  this work for additional information regarding copyright ownership."
    output += "  The ASF licenses this file to You under the Apache License, Version 2.0"
    output += "  (the \"License\"); you may not use this file except in compliance with"
    output += "  the License.  You may obtain a copy of the License at"
    output += "  "
    output += "      https://www.apache.org/licenses/LICENSE-2.0"
    output += "  "
    output += "  Unless required by applicable law or agreed to in writing, software"
    output += "  distributed under the License is distributed on an \"AS IS\" BASIS,"
    output += "  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied."
    output += "  See the License for the specific language governing permissions and"
    output += "  limitations under the License."
    output += "---"
    output += ""
  }

  def appendConfigurationTableHeader(output: ArrayBuffer[String]): Unit = {
    output += "| Key | Default | Description | Since |"
    output += "| --- | ------- | ----------- | ----- |"
  }

  def appendBeginInclude(output: ArrayBuffer[String]): Unit = {
    output += "<!--begin-include-->"
  }

  def appendEndInclude(output: ArrayBuffer[String]): Unit = {
    output += "<!--end-include-->"
  }

  def verifyOutput(goldenFile: Path, output: ArrayBuffer[String]): Unit =
    if (System.getenv("UPDATE") == "1") {
      val writer = Files.newBufferedWriter(
        goldenFile,
        StandardCharsets.UTF_8,
        StandardOpenOption.TRUNCATE_EXISTING,
        StandardOpenOption.CREATE)
      try output.foreach { line =>
        writer.write(line)
        writer.newLine()
      } finally writer.close()
    } else {
      val expected = Files.readAllLines(goldenFile).asScala
      val updateCmd = "UPDATE=1 build/mvn clean test -pl common -am " +
        "-Dtest=none -DwildcardSuites=org.apache.celeborn.ConfigurationSuite"

      val hint =
        s"""
           |$goldenFile is out of date, please update the golden file with
           |
           |  $updateCmd
           |
           |>
           |""".stripMargin
      assert(output.size === expected.size, hint)

      output.zip(expected).foreach { case (out, in) => assert(out === in, hint) }
    }
}
