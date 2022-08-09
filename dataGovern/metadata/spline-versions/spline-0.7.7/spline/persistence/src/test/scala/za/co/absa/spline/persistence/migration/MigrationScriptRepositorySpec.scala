/*
 * Copyright 2020 ABSA Group Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package za.co.absa.spline.persistence.migration

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import za.co.absa.commons.version.Version._

class MigrationScriptRepositorySpec extends AnyFlatSpec with Matchers {

  behavior of "findMigrationChain()"

  /*
   *
   *    (1.0.0)
   *       |
   *    (1.1.0)
   *     /   |
   * (1.2.0) |
   *     \   |
   *    (2.0.0)
   *     /   |
   * (2.5.0) |
   *     \   |
   *    (3.0.0)
   */
  it should "find a correct migration path" in {
    val repo = new MigrationScriptRepository(() => Seq(
      MigrationScript(semver"1.0.0", semver"1.1.0", "aaa"),
      MigrationScript(semver"1.1.0", semver"1.2.0", "bbb"),
      MigrationScript(semver"1.1.0", semver"2.0.0", "bbb"),
      MigrationScript(semver"2.0.0", semver"2.5.0", "ccc"),
      MigrationScript(semver"2.5.0", semver"3.0.0", "ccc"),
      MigrationScript(semver"2.0.0", semver"3.0.0", "ccc"),
    ))

    val migrationPath = repo.findMigrationChain(semver"1.0.0", semver"3.0.0")

    migrationPath should equal(Seq(
      MigrationScript(semver"1.0.0", semver"1.1.0", "aaa"),
      MigrationScript(semver"1.1.0", semver"2.0.0", "bbb"),
      MigrationScript(semver"2.0.0", semver"3.0.0", "ccc"),
    ))
  }

  it should "return empty path in 'from' and 'to' nodes are the same " in {
    val repo = new MigrationScriptRepository(() => Seq(
      MigrationScript(semver"1.0.0", semver"2.0.0", "aaa")
    ))

    val path = repo.findMigrationChain(semver"1.0.0", semver"1.0.0")
    path should have length 0
  }

  it should "throw if no such vertex exists" in {
    val repo = new MigrationScriptRepository(() => Seq(
      MigrationScript(semver"1.0.0", semver"2.0.0", "aaa")
    ))

    intercept[RuntimeException](
      repo.findMigrationChain(semver"1.5.0", semver"2.0.0")
    ).getMessage should include("1.5.0")

    intercept[RuntimeException](
      repo.findMigrationChain(semver"1.0.0", semver"1.5.0")
    ).getMessage should include("1.5.0")
  }

  it should "throw if no path exists" in {
    val repo = new MigrationScriptRepository(() => Seq(
      MigrationScript(semver"1.0.0", semver"2.0.0", "aaa"),
      MigrationScript(semver"2.0.0", semver"3.0.0", "bbb"),
    ))

    val ex = intercept[RuntimeException](
      repo.findMigrationChain(semver"3.0.0", semver"1.0.0")
    )
    ex.getMessage should include("Cannot")
    ex.getMessage should include("3.0.0")
    ex.getMessage should include("1.0.0")
  }

  behavior of "latestToVersion"

  it should "return latest 'to' version" in {
    val repo = new MigrationScriptRepository(() => Seq(
      MigrationScript(semver"1.0.0", semver"1.1.0", "aaa"),
      MigrationScript(semver"1.1.0", semver"2.0.0", "bbb"),
      MigrationScript(semver"2.0.0", semver"3.0.0", "ccc"),
    ))
    repo.latestToVersion should equal(semver"3.0.0")
  }
}
