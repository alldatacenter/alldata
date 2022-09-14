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

package com.webank.wedatasphere.streamis.jobmanager.common.protocol

import org.apache.linkis.protocol.Protocol

/**
 * created by yangzhiyue on 2021/4/26
 * Description:
 */
trait StreamJobManagerProtocol extends Protocol


case class ImportJobManagerRequest(streamJobName:String,
                                   `type`:String,
                                   executionCode:String,
                                   createBy:String,
                                   updateBy:String,
                                   description:String,
                                   tags:java.util.List[String],
                                   publishUser:String,
                                   workspaceName:String,
                                   version:String,
                                   projectName:String,
                                   workflowId: Long,
                                   workflowName: String) extends StreamJobManagerProtocol


case class ImportJobManagerResponse(status:Int,
                                    streamJobId:Long,
                                    errorMessage:String) extends StreamJobManagerProtocol