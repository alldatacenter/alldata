package com.webank.wedatasphere.streamis.project.common

import org.apache.linkis.protocol.Protocol

/**
 * created by yangzhiyue on 2021/4/25
 * Description:
 */
trait StreamisProjectProtocol extends Protocol




case class CreateStreamProjectRequest(projectName:String,
                                      description:String,
                                      createBy:String) extends StreamisProjectProtocol


case class CreateStreamProjectResponse(status:Int,
                                       projectName:String,
                                       streamisProjectId:Long,
                                       errorMessage:String) extends StreamisProjectProtocol




case class UpdateStreamProjectRequest(streamisProjectId:Long,
                                      projectName:String,
                                      description:String,
                                      updateBy:String) extends StreamisProjectProtocol


case class UpdateStreamProjectResponse(status:Int,
                                       streamisProjectId:Long,
                                       errorMessage:String)extends StreamisProjectProtocol


case class DeleteStreamProjectRequest(streamisProjectId:Long,
                                      projectName:String) extends StreamisProjectProtocol


case class DeleteStreamProjectResponse(status:Long,
                                       projectName:String,
                                       errorMessage:String) extends StreamisProjectProtocol



