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

package za.co.absa.spline.persistence.foxx

import java.io.File

import org.apache.commons.io.FileUtils
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import za.co.absa.commons.io.TempDirectory

class FoxxSourceResolverSpec extends AnyFlatSpec with Matchers {

  behavior of "lookupSources()"

  it should "search a given location and return a list of Foxx service" in {
    val tmpDir = TempDirectory(getClass.getName).deleteOnExit().path.toFile

    val fooServiceDir = new File(tmpDir, "foo")
    FileUtils.writeStringToFile(new File(fooServiceDir, "manifest.json"), """{"main": "hello.js"}""", "UTF-8")
    FileUtils.writeStringToFile(new File(fooServiceDir, "hello.js"), "say('Hello Foo');", "UTF-8")

    val barServiceDir = new File(tmpDir, "bar")
    FileUtils.writeStringToFile(new File(barServiceDir, "manifest.json"), """{"main": "bbb.js"}""", "UTF-8")
    FileUtils.writeStringToFile(new File(barServiceDir, "bbb.js"), "say('Hello Bar');", "UTF-8")
    FileUtils.writeStringToFile(new File(barServiceDir, "baz/zzz.js"), "say('zzz');", "UTF-8")

    (FoxxSourceResolver
      .lookupSources(tmpDir.toURI.toString)
      .map({ case (sn, assets) => sn -> assets.toSet })

      should contain theSameElementsAs Set(

      ("foo", Set(
        "manifest.json" -> """{"main": "hello.js"}""",
        "hello.js" -> "say('Hello Foo');")),
      ("bar", Set(
        "manifest.json" -> """{"main": "bbb.js"}""",
        "bbb.js" -> "say('Hello Bar');",
        "baz/zzz.js" -> "say('zzz');")),
    ))
  }

}
