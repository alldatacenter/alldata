/*
 * Copyright 2021 WeBank
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.webank.wedatasphere.streamis.jobmanager.rpc

import com.webank.wedatasphere.streamis.jobmanager.common.protocol.StreamJobManagerProtocol
import com.webank.wedatasphere.streamis.jobmanager.manager.service.StreamJobService
import org.apache.linkis.rpc.{RPCMessageEvent, Receiver, ReceiverChooser}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import javax.annotation.PostConstruct

/**
 * created by cooperyang on 2021/7/19
 * Description:
 */
@Component
class StreamisJobManagerReceiverChooser extends ReceiverChooser{

  @Autowired
  var jobService: StreamJobService = _


  private var receiver: Option[StreamisJobManagerReceiver] = _

  @PostConstruct
  def init():Unit = {
    receiver = Some(new StreamisJobManagerReceiver(jobService))
  }

  override def chooseReceiver(event: RPCMessageEvent): Option[Receiver] = event.message match {
    case streamFlowProtocol: StreamJobManagerProtocol => receiver
    case _ => None
  }

}
