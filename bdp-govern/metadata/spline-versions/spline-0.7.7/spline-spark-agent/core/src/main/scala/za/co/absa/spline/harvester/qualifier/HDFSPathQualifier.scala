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

package za.co.absa.spline.harvester.qualifier

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path}

class HDFSPathQualifier(hadoopConfiguration: Configuration) extends PathQualifier {
  private val fs = FileSystem.get(hadoopConfiguration)
  private val fsUri = fs.getUri
  private val fsWorkingDirectory = fs.getWorkingDirectory

  override def qualify(path: String): String = new Path(path)
    .makeQualified(fsUri, fsWorkingDirectory)
    .toString
}
