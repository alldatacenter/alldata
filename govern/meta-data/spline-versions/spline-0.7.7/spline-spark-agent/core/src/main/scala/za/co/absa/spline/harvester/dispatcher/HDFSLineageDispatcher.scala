/*
 * Copyright 2021 ABSA Group Limited
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

package za.co.absa.spline.harvester.dispatcher

import java.net.URI

import org.apache.commons.configuration.Configuration
import org.apache.hadoop.fs.permission.FsPermission
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.spark.SparkContext
import org.apache.spark.internal.Logging
import za.co.absa.commons.annotation.Experimental
import za.co.absa.commons.config.ConfigurationImplicits._
import za.co.absa.commons.lang.ARM._
import za.co.absa.spline.harvester.dispatcher.HDFSLineageDispatcher._
import za.co.absa.spline.producer.model.{ExecutionEvent, ExecutionPlan}
import za.co.absa.spline.harvester.json.HarvesterJsonSerDe

import scala.concurrent.blocking
import za.co.absa.commons.s3.SimpleS3Location.SimpleS3LocationExt

/**
 * A port of https://github.com/AbsaOSS/spline/tree/release/0.3.9/persistence/hdfs/src/main/scala/za/co/absa/spline/persistence/hdfs
 *
 * Note:
 * This class is unstable, experimental, is mostly used for debugging, with no guarantee to work properly
 * for every generic use case in a real production application.
 *
 * It is NOT thread-safe, strictly synchronous assuming a predefined order of method calls: `send(plan)` and then `send(event)`
 */
@Experimental
class HDFSLineageDispatcher(filename: String, permission: FsPermission, bufferSize: Int)
  extends LineageDispatcher
    with Logging {

  def this(conf: Configuration) = this(
    filename = conf.getRequiredString(FileNameKey),
    permission = new FsPermission(conf.getRequiredString(FilePermissionsKey)),
    bufferSize = conf.getRequiredInt(BufferSizeKey)
  )

  @volatile
  private var _lastSeenPlan: ExecutionPlan = _

  override def name = "HDFS"

  override def send(plan: ExecutionPlan): Unit = {
    this._lastSeenPlan = plan
  }

  override def send(event: ExecutionEvent): Unit = {
    // check state
    if (this._lastSeenPlan == null || this._lastSeenPlan.id.get != event.planId)
      throw new IllegalStateException("send(event) must be called strictly after send(plan) method with matching plan ID")

    try {
      val path = s"${this._lastSeenPlan.operations.write.outputSource.stripSuffix("/")}/$filename"
      val planWithEvent = Map(
        "executionPlan" -> this._lastSeenPlan,
        "executionEvent" -> event
      )

      import HarvesterJsonSerDe.impl._
      persistToHadoopFs(planWithEvent.toJson, path)
    } finally {
      this._lastSeenPlan = null
    }
  }

  private def persistToHadoopFs(content: String, fullLineagePath: String): Unit = blocking {
    val (fs, path) = pathStringToFsWithPath(fullLineagePath)
    logDebug(s"Opening HadoopFs output stream to $path")

    val replication = fs.getDefaultReplication(path)
    val blockSize = fs.getDefaultBlockSize(path)
    val outputStream = fs.create(path, permission, true, bufferSize, replication, blockSize, null)

    val umask = FsPermission.getUMask(fs.getConf)
    FsPermission.getFileDefault.applyUMask(umask)

    logDebug(s"Writing lineage to $path")
    using(outputStream) {
      _.write(content.getBytes("UTF-8"))
    }
  }
}

object HDFSLineageDispatcher {
  private val HadoopConfiguration = SparkContext.getOrCreate().hadoopConfiguration

  private val FileNameKey = "fileName"
  private val FilePermissionsKey = "filePermissions"
  private val BufferSizeKey = "fileBufferSize"

  /**
   * Converts string full path to Hadoop FS and Path, e.g.
   * `s3://mybucket1/path/to/file` -> S3 FS + `path/to/file`
   * `/path/on/hdfs/to/file` -> local HDFS + `/path/on/hdfs/to/file`
   *
   * Note, that non-local HDFS paths are not supported in this method, e.g. hdfs://nameservice123:8020/path/on/hdfs/too.
   *
   * @param pathString path to convert to FS and relative path
   * @return FS + relative path
   */
  def pathStringToFsWithPath(pathString: String): (FileSystem, Path) = {
    pathString.toSimpleS3Location match {
      case Some(s3Location) =>
        val s3Uri = new URI(s3Location.asSimpleS3LocationString) // s3://<bucket>
        val s3Path = new Path(s"/${s3Location.path}") // /<text-file-object-path>

        val fs = FileSystem.get(s3Uri, HadoopConfiguration)
        (fs, s3Path)

      case None => // local hdfs location
        val fs = FileSystem.get(HadoopConfiguration)
        (fs, new Path(pathString))
    }
  }
}
