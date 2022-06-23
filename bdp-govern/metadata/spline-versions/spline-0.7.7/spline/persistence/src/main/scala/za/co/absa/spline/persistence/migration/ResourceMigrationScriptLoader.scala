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

import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import za.co.absa.commons.lang.ARM
import za.co.absa.commons.version.Version

import scala.io.Source

class ResourceMigrationScriptLoader(location: String) extends MigrationScriptLoader {
  override def loadAll(): Seq[MigrationScript] = {
    new PathMatchingResourcePatternResolver(getClass.getClassLoader)
      .getResources(s"$location/${MigrationScript.FileNamePattern}").toSeq
      .map(res => {
        val MigrationScript.NameRegexp(verFrom, verTo) = res.getFilename
        val scriptBody = ARM.using(Source.fromURL(res.getURL))(_.getLines.mkString("\n"))
        MigrationScript(
          Version.asSemVer(verFrom),
          Version.asSemVer(verTo),
          scriptBody)
      })
  }
}


