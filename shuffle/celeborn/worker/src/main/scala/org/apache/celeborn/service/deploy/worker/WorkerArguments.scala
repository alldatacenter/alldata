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

package org.apache.celeborn.service.deploy.worker

import scala.annotation.tailrec

import org.apache.celeborn.common.CelebornConf
import org.apache.celeborn.common.util.{IntParam, Utils}

class WorkerArguments(args: Array[String], conf: CelebornConf) {

  private var _host: Option[String] = None
  private var _port: Option[Int] = None
  // for local testing.
  private var _master: Option[String] = None
  private var _propertiesFile: Option[String] = None

  // 1st parse from cli args
  parse(args.toList)
  // 2nd read from configuration file
  _propertiesFile = Some(Utils.loadDefaultCelebornProperties(conf, _propertiesFile.orNull))
  _host = _host.orElse(Some(Utils.localHostName(conf)))
  _port = _port.orElse(Some(conf.workerRpcPort))

  def host: String = _host.get

  def port: Int = _port.get

  def master: Option[String] = _master

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

    case value :: tail =>
      _master = Some(value)
      parse(tail)

    case Nil =>

    case _ =>
      printUsageAndExit(1)
  }

  /**
   * Print usage and exit JVM with the given exit code.
   */
  def printUsageAndExit(exitCode: Int): Unit = {
    // scalastyle:off println
    System.err.println(
      """Usage: Worker [options] <master>
        |
        |<master> must be a URL of the form celeborn://hostname:port
        |Options:
        |  -h HOST, --host HOST     Hostname to listen on
        |  -p PORT, --port PORT     Port to listen on (default: random)
        |  --properties-file FILE   Path to a custom Celeborn properties file.
        |                           Default is conf/celeborn-defaults.conf.
        |""".stripMargin)
    // scalastyle:on println
    sys.exit(exitCode)
  }
}
