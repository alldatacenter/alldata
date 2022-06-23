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

import java.io.ByteArrayOutputStream
import java.util.zip.{ZipEntry, ZipOutputStream}

import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods.parse
import org.slf4s.Logging
import za.co.absa.commons.lang.ARM
import za.co.absa.spline.common.rest.RESTClient

import scala.concurrent.{ExecutionContext, Future}

class FoxxManagerImpl(restClient: RESTClient)(implicit ec: ExecutionContext)
  extends FoxxManager
    with Logging {

  override def install(mountPrefix: String, assets: Array[(String, String)]): Future[Unit] = {
    log.debug(s"Prepare Foxx service.zip: $mountPrefix")

    val baos = new ByteArrayOutputStream()
    ARM.using(new ZipOutputStream(baos)) {
      zip => {
        for ((path, content) <- assets) {
          log.debug(s"Next zip entry: $path")
          log.trace(s"Entry content:\n$content")
          zip.putNextEntry(new ZipEntry(path))
          zip.write(content.getBytes("UTF-8"))
          zip.closeEntry()
        }
      }
    }

    restClient.post(s"_api/foxx?mount=$mountPrefix", baos.toByteArray)
  }

  override def uninstall(mountPrefix: String): Future[Unit] = {
    log.debug(s"Delete Foxx service: $mountPrefix")
    restClient.delete(s"_api/foxx/service?mount=$mountPrefix")
  }

  override def list(): Future[Seq[Map[String, Any]]] = {
    log.debug(s"List Foxx services")
    restClient.get(s"_api/foxx").map(str => {
      val srvDefs = parse(str).extract(DefaultFormats, manifest[Seq[Map[String, Any]]])
      log.debug(s"Found Foxx service definitions: $srvDefs")
      srvDefs
    })
  }
}
