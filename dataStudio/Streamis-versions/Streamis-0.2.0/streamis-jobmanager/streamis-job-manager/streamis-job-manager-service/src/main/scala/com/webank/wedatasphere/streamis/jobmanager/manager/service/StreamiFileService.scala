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

import com.webank.wedatasphere.streamis.jobmanager.manager.entity.StreamisFile

import java.util

/**
  * Created by enjoyyin on 2021/9/23.
  */
trait StreamiFileService {

  def getFile(projectName: String, fileName: String, version: String): StreamisFile

  def listFileVersions(projectName: String, fileName: String): util.List[_ <: StreamisFile]

}
