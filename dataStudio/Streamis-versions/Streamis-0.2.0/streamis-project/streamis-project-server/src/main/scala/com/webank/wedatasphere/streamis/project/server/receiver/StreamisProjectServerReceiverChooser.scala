package com.webank.wedatasphere.streamis.project.server.receiver

import org.apache.linkis.rpc.{RPCMessageEvent, Receiver, ReceiverChooser}
import com.webank.wedatasphere.streamis.project.common.StreamisProjectProtocol
import com.webank.wedatasphere.streamis.project.server.service.StreamisProjectService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import javax.annotation.PostConstruct

@Component
class StreamisProjectServerReceiverChooser extends ReceiverChooser {

  @Autowired
  var streamisProjectService:StreamisProjectService = _


  private var receiver: Option[StreamisProjectServerReceiver] = _

  @PostConstruct
  def init():Unit = {
    receiver = Some(new StreamisProjectServerReceiver(streamisProjectService))
  }

  override def chooseReceiver(event: RPCMessageEvent): Option[Receiver] = event.message match {
    case _: StreamisProjectProtocol => receiver
    case _ => None
  }
}
