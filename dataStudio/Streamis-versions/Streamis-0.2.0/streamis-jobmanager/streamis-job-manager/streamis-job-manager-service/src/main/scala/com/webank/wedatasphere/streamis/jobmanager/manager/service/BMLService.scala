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

package com.webank.wedatasphere.streamis.jobmanager.manager.service

import java.io.{ByteArrayInputStream, File, FileInputStream, InputStream}
import java.util

import org.apache.linkis.bml.client.{BmlClient, BmlClientFactory}
import org.apache.linkis.bml.protocol.{BmlUpdateResponse, BmlUploadResponse}
import org.apache.linkis.common.exception.ErrorException
import org.apache.linkis.common.utils.{Logging, Utils}
import com.webank.wedatasphere.streamis.jobmanager.manager.exception.JobCreateErrorException
import javax.annotation.PreDestroy
import org.apache.commons.lang.StringUtils
import org.springframework.stereotype.Component

import scala.collection.JavaConversions._

/**
 * created by cooperyang on 2021/7/12
 * Description:
 */
@Component("projectServerBMLService")
class BMLService extends Logging{

  private val defaultBmlUser = "hadoop"
  private val bmlClient: BmlClient = BmlClientFactory.createBmlClient()

  def upload(userName: String, content: String, fileName: String): util.Map[String, Object] = {
    val inputStream = new ByteArrayInputStream(content.getBytes("utf-8"))
    val realUploadUser = if (StringUtils.isEmpty(userName)) defaultBmlUser else userName
    val resource: BmlUploadResponse = bmlClient.uploadResource(realUploadUser, fileName, inputStream)
    if (!resource.isSuccess) {
      error(s"Failed to upload ${content} to bml")
      throw new ErrorException(911113, "上传失败")
    }
    Utils.tryQuietly(inputStream.close())
    val map = new util.HashMap[String, Object]
    map += "resourceId" -> resource.resourceId
    map += "version" -> resource.version
  }

  def upload(userName: String, fileName: String): util.Map[String, Object] = {
    val resource = bmlClient.uploadResource(userName,fileName,new FileInputStream(new File(fileName)))
    if (!resource.isSuccess) {
      error(s"Failed to upload $fileName to bml")
      throw new ErrorException(911113, "上传失败")
    }
    val map = new util.HashMap[String, Object]
    map += "resourceId" -> resource.resourceId
    map += "version" -> resource.version
  }

  def update(userName: String, resourceId: String, content: String, fileName: String): util.Map[String, Object] = {
    val inputStream = new ByteArrayInputStream(content.getBytes("utf-8"))
    val realUploadUser = if (StringUtils.isEmpty(userName)) defaultBmlUser else userName
    val resource: BmlUpdateResponse = bmlClient.updateResource(realUploadUser, resourceId, fileName, inputStream)
    if (!resource.isSuccess) {
      error(s"Failed to upload ${content} to bml")
      throw new ErrorException(911114, "更新失败")
    }
    Utils.tryQuietly(inputStream.close())
    val map = new util.HashMap[String, Object]
    map += "resourceId" -> resource.resourceId
    map += "version" -> resource.version
  }

  def get(userName: String, resourceId: String, version: String): InputStream = {
    val realDownloadUser = if (StringUtils.isEmpty(userName)) defaultBmlUser else userName
    val resource =
    if (version == null) {
      bmlClient.downloadShareResource(realDownloadUser, resourceId)
    } else {
      bmlClient.downloadShareResource(realDownloadUser, resourceId, version)
    }
    if (!resource.isSuccess) {
      error(s"failed to download resourceId $resourceId version $version.")
      throw new JobCreateErrorException(91115,"下载失败")
    }
    resource.inputStream
  }

  @PreDestroy
  def destroy(): Unit = bmlClient.close()

}
