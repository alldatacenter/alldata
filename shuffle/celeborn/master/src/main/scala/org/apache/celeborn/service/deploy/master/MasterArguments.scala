/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.celeborn.service.deploy.master

import scala.annotation.tailrec

import org.apache.celeborn.common.CelebornConf
import org.apache.celeborn.common.CelebornConf._
import org.apache.celeborn.common.util.{IntParam, Utils}
import org.apache.celeborn.service.deploy.master.clustermeta.ha.MasterClusterInfo

class MasterArguments(args: Array[String], conf: CelebornConf) {

  private var _host: Option[String] = None
  private var _port: Option[Int] = None
  private var _propertiesFile: Option[String] = None
  private var _masterClusterInfo: Option[MasterClusterInfo] = None

  // 1st parse from cli args
  parse(args.toList)

  // 2nd parse from environment variables
  _host = _host.orElse(sys.env.get("CELEBORN_MASTER_HOST"))
  _port = _port.orElse(sys.env.get("CELEBORN_MASTER_PORT").map(_.toInt))

  // 3rd read from configuration file
  _propertiesFile = Some(Utils.loadDefaultRssProperties(conf, _propertiesFile.orNull))
  if (conf.haEnabled) {
    val clusterInfo = MasterClusterInfo.loadHAConfig(conf)
    val localNode = clusterInfo.localNode
    _host = _host.orElse(Some(conf.haMasterNodeHost(localNode.nodeId)))
    _port = _port.orElse(Some(conf.haMasterNodePort(localNode.nodeId)))
    _masterClusterInfo = Some(clusterInfo)
  } else {
    _host = _host.orElse(Some(conf.masterHost))
    _port = _port.orElse(Some(conf.masterPort))
  }

  def host: String = _host.get

  def port: Int = _port.get

  def masterClusterInfo: Option[MasterClusterInfo] = _masterClusterInfo

  @tailrec
  private def parse(args: List[String]): Unit = args match {
    case ("--host" | "-h") :: value :: tail =>
      Utils.checkHost(value)
      _host = Some(value)
      parse(tail)

    case ("--port" | "-p") :: IntParam(value) :: tail =>
      _port = Some(value)
      parse(tail)

    case "--properties-file" :: value :: tail =>
      _propertiesFile = Some(value)
      parse(tail)

    case "--help" :: _ =>
      printUsageAndExit(0)

    case Nil => // No-op

    case _ =>
      printUsageAndExit(1)
  }

  /**
   * Print usage and exit JVM with the given exit code.
   */
  private def printUsageAndExit(exitCode: Int): Unit = {
    // scalastyle:off println
    System.err.println(
      """Usage: Master [options]
        |
        |Options:
        |  -h HOST, --host HOST   Hostname to listen on
        |  -p PORT, --port PORT   Port to listen on (default: 9097)
        |  --properties-file FILE Path to a custom Celeborn properties file,
        |                         default is conf/celeborn-defaults.conf.
        |""".stripMargin)
    // scalastyle:on println
    sys.exit(exitCode)
  }
}
