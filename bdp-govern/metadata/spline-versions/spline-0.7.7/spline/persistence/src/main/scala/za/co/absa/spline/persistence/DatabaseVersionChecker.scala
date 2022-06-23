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

package za.co.absa.spline.persistence

import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import za.co.absa.spline.common.SplineBuildInfo
import za.co.absa.spline.persistence.migration.MigrationScriptRepository

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class DatabaseVersionChecker @Autowired()(dbVersionManager: DatabaseVersionManager)
  extends InitializingBean {

  override def afterPropertiesSet(): Unit = {
    val requiredDBVersion = MigrationScriptRepository.latestToVersion
    val currentDBVersion = Await.result(dbVersionManager.currentVersion, Duration.Inf)

    if (requiredDBVersion != currentDBVersion)
      sys.error("" +
        s"Database version ${currentDBVersion.asString} is out of date, version ${requiredDBVersion.asString} is required. " +
        s"Please execute 'java -jar admin-${SplineBuildInfo.Version}.jar db-upgrade' to upgrade the database.")
  }
}
