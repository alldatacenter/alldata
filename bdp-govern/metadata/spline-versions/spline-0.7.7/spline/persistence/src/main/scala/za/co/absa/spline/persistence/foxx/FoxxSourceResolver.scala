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

import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import za.co.absa.commons.lang.ARM

import scala.io.Source

object FoxxSourceResolver {
  type ServiceName = String
  type AssetContent = String
  type AssetPath = String
  type Assets = (AssetPath, AssetContent)
  type Service = (ServiceName, Array[Assets])

  private final val ManifestFilename = "manifest.json"

  def lookupSources(baseResourceLocation: String): Array[Service] = {
    // Although the code below might look a bit redundant it's actually not.
    // This has to work in both inside and outside a JAR, but:
    //   - ResourcePatternResolver.getResources("classpath:foo/*") doesn't behave consistently
    //   - manifestResource.createRelative(". or ..") doesn't behave consistently
    //   - Resource.getFile doesn't work within a JAR
    //
    new PathMatchingResourcePatternResolver(getClass.getClassLoader)
      .getResources(s"$baseResourceLocation/*/$ManifestFilename")
      .map(manifestResource => {
        val serviceBase = new File(manifestResource.getURL.getPath).getParentFile
        val serviceName = serviceBase.getName
        val assets = new PathMatchingResourcePatternResolver(getClass.getClassLoader)
          .getResources(s"$baseResourceLocation/$serviceName/**/*")
          .filter(_.isReadable)
          .map(r => {
            val assetUrl = r.getURL
            val assetPath = assetUrl.getPath.substring(serviceBase.getPath.length + 1)
            val content = ARM.using(Source.fromURL(assetUrl))(_.getLines.mkString("\n"))
            assetPath -> content
          })
        serviceName -> assets
      })
  }

}
