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

package com.webank.wedatasphere.streamis.jobmanager.manager.transform

import com.webank.wedatasphere.streamis.jobmanager.launcher.job.LaunchJob
import org.apache.linkis.common.utils.ClassUtils
import com.webank.wedatasphere.streamis.jobmanager.manager.transform.entity.StreamisTransformJob


trait Transform {

  def transform(streamisTransformJob: StreamisTransformJob, job: LaunchJob): LaunchJob

}

import scala.collection.convert.WrapAsScala._
object Transform {

  private val transforms = ClassUtils.reflections.getSubTypesOf(classOf[Transform]).filterNot(ClassUtils.isInterfaceOrAbstract)
    .map(_.newInstance).toArray

  def getTransforms: Array[Transform] = transforms

}