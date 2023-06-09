/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.qlangtech.tis.realtime.transfer.hudi

import java.util
import org.apache.flink.streaming.api.functions.sink.{PrintSinkFunction, SinkFunction}
import com.qlangtech.tis.realtime.{HoodieFlinkSourceHandle}
import scala.collection.JavaConverters._
import org.apache.hudi.streamer.FlinkStreamerConfig
import org.apache.hudi.common.model.WriteOperationType
import com.qlangtech.tis.extension.TISExtension

import org.slf4j.LoggerFactory

@TISExtension()
class HudiSourceHandle extends HoodieFlinkSourceHandle {
  lazy val logger = LoggerFactory.getLogger( classOf[HudiSourceHandle])
  val _currVersion : Long = 2
override protected def createTabStreamerCfg(): java.util.Map[String , FlinkStreamerConfig] = {
  var cfgs: Map[String , FlinkStreamerConfig] = Map()
  cfgs.asJava
}

  def getVer() : Long = {
    _currVersion
  }

}
