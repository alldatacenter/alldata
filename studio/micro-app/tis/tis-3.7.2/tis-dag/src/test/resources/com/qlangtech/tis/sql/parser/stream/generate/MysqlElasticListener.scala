
package com.qlangtech.tis.realtime

import java.util

import com.qlangtech.tis.realtime.transfer.DTO
import org.apache.flink.streaming.api.datastream.DataStream
import org.apache.flink.streaming.api.functions.sink.{PrintSinkFunction, SinkFunction}

class TISFlinkSourceHandle extends DefaultFlinkSourceHandle {
    override protected def processTableStream(streamMap: util.Map[String, DataStream[DTO]], sinkFunction: SinkFunction[DTO]): Unit = {

    val instancedetailStream = streamMap.get("instancedetail")
    instancedetailStream.addSink(sinkFunction)

    }
}
