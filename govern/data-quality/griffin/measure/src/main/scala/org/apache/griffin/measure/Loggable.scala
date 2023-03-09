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

package org.apache.griffin.measure

import org.apache.log4j.{Level, Logger}

trait Loggable {

  @transient private lazy val logger = Logger.getLogger(getClass)

  @transient protected lazy val griffinLogger: Logger = Logger.getLogger("org.apache.griffin")

  def getGriffinLogLevel: Level = {
    var logger = griffinLogger
    while (logger != null && logger.getLevel == null) {
      logger = logger.getParent.asInstanceOf[Logger]
    }
    logger.getLevel
  }

  protected def info(msg: => String): Unit = {
    logger.info(msg)
  }

  protected def debug(msg: => String): Unit = {
    logger.debug(msg)
  }

  protected def warn(msg: => String): Unit = {
    logger.warn(msg)
  }

  protected def warn(msg: => String, e: Throwable): Unit = {
    logger.warn(msg, e)
  }

  protected def error(msg: => String): Unit = {
    logger.error(msg)
  }

  protected def error(msg: => String, e: Throwable): Unit = {
    logger.error(msg, e)
  }

}
