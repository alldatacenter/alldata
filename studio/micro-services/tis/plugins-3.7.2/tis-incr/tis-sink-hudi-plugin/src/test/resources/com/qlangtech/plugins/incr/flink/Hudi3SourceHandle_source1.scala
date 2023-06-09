package com.qlangtech.tis.realtime.transfer.hudi

import com.qlangtech.tis.extension.TISExtension
import com.qlangtech.tis.realtime.HoodieFlinkSourceHandle
import org.apache.hudi.streamer.FlinkStreamerConfig
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._

@TISExtension()
class HudiSourceHandle extends HoodieFlinkSourceHandle {
  lazy val logger = LoggerFactory.getLogger(classOf[HudiSourceHandle])
  val _currVersion: Long = 1

  override protected def createTabStreamerCfg(): java.util.Map[String, FlinkStreamerConfig] = {

    var cfgs: Map[String, FlinkStreamerConfig] = Map()
    cfgs.asJava
  }

  def getVer(): Long = {
    _currVersion
  }

}
