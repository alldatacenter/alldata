/*
 * Copyright 2019 ABSA Group Limited
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

package za.co.absa.spline.persistence

import com.arangodb.async.ArangoDatabaseAsync
import org.slf4s.Logging
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.annotation.{Bean, Configuration}
import za.co.absa.commons.config.ConfTyped
import za.co.absa.spline.common.config.DefaultConfigurationStack
import za.co.absa.spline.common.scala13.Option
import za.co.absa.spline.common.security.TLSUtils

@Configuration
class ArangoRepoConfig extends InitializingBean with Logging {

  import za.co.absa.spline.persistence.ArangoRepoConfig._
  import scala.concurrent.ExecutionContext.Implicits._

  override def afterPropertiesSet(): Unit = {
    log.info(s"Spline database URL: ${Database.ConnectionURL.asString}")
  }

  @Bean def arangoDatabaseFacade: ArangoDatabaseFacade = {
    val sslCtxOpt = Option.when(Database.DisableSSLValidation)(TLSUtils.TrustingAllSSLContext)
    new ArangoDatabaseFacade(Database.ConnectionURL, sslCtxOpt)
  }

  @Bean def arangoDatabase: ArangoDatabaseAsync = arangoDatabaseFacade.db

  @Bean def databaseVersionManager: DatabaseVersionManager = new DatabaseVersionManager(arangoDatabase)

  @Bean def databaseVersionChecker: DatabaseVersionChecker = new DatabaseVersionChecker(databaseVersionManager)
}

object ArangoRepoConfig extends DefaultConfigurationStack with ConfTyped {

  import za.co.absa.commons.config.ConfigurationImplicits._

  override val rootPrefix: String = "spline"

  object Database extends Conf("database") {
    private val conf = ArangoRepoConfig.this

    val ConnectionURL: ArangoConnectionURL = ArangoConnectionURL(
      conf.getRequiredStringArray(Prop("connectionUrl")).mkString(","))

    val LogFullQueryOnError: Boolean =
      conf.getBoolean(Prop("logFullQueryOnError"), false)

    val DisableSSLValidation: Boolean =
      conf.getBoolean(Prop("disableSslValidation"), false)
  }

}
