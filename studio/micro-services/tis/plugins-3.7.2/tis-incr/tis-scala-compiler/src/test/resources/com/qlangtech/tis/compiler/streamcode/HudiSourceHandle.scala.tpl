package tis

import com.qlangtech.tis.extension.TISExtension
import com.qlangtech.tis.realtime.{BasicFlinkSourceHandle, DTOStream, SinkFuncs}
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment
import java.util
import java.util.Map


@TISExtension
class HudiSourceHandle extends BasicFlinkSourceHandle[DTO] {
  override protected def processTableStream(env: StreamExecutionEnvironment, tab2OutputTag: util.Map[String, DTOStream], sinkFunction: SinkFuncs): Unit = {
  }
}
