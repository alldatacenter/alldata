/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.celeborn.common.identity

import org.apache.celeborn.common.CelebornConf
import org.apache.celeborn.common.internal.Logging

abstract class IdentityProvider {
  def provide(): UserIdentifier
}

object IdentityProvider extends Logging {
  val DEFAULT_TENANT_ID = "default"
  val DEFAULT_USERNAME = "default"

  def instantiate(conf: CelebornConf): IdentityProvider = {
    val className = conf.quotaIdentityProviderClass
    logDebug(s"Creating identity provider $className")
    val clazz = Class.forName(
      className,
      true,
      Thread.currentThread().getContextClassLoader).asInstanceOf[Class[IdentityProvider]]
    try {
      val ctor = clazz.getDeclaredConstructor()
      logDebug("Using (String, String, Boolean) constructor")
      ctor.newInstance()
    } catch {
      case e: NoSuchMethodException =>
        logError(s"Falling to instantiate identity provider $className", e)
        throw e
    }
  }
}
