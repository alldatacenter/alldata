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

import java.io.File

import org.apache.commons.io.FileUtils
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import za.co.absa.commons.io.TempDirectory
import za.co.absa.commons.version.Version._

class ResourceMigrationScriptLoaderSpec extends AnyFlatSpec with Matchers {

  behavior of "loadAll()"

  it should "load scripts from a given resource" in {
    val dir = TempDirectory(getClass.getName).deleteOnExit().path.toFile

    FileUtils.writeStringToFile(new File(dir, "1.0.0-1.1.0.js"), "aaa\n111", "UTF-8")
    FileUtils.writeStringToFile(new File(dir, "1.1.0-1.2.0.js"), "bbb\n222", "UTF-8")
    FileUtils.writeStringToFile(new File(dir, "1.2.0-1.3.0.js"), "ccc\n333", "UTF-8")

    new ResourceMigrationScriptLoader(dir.toURI.toString).loadAll() should equal(Seq(
      MigrationScript(semver"1.0.0", semver"1.1.0", "aaa\n111"),
      MigrationScript(semver"1.1.0", semver"1.2.0", "bbb\n222"),
      MigrationScript(semver"1.2.0", semver"1.3.0", "ccc\n333"),
    ))
  }
}
