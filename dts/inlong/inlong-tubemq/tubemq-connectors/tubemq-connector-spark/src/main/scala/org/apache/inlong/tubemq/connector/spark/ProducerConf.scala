/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.tubemq.connector.spark

// Hold sender common parameters
abstract class ProducerConf extends Serializable {
}

class TubeMQProducerConf extends ProducerConf {
  private var _topic: String = _
  def topic: String = _topic
  def setTopic(value: String): this.type = {
    _topic = value
    this
  }

  private var _masterHostAndPort: String = _
  def masterHostAndPort: String = _masterHostAndPort
  def setMasterHostAndPort(value: String): this.type = {
    _masterHostAndPort = value
    this
  }

  private var _timeout: Long = 20000 // 20s
  def timeout: Long = _timeout
  def setTimeout(value: Long): this.type = {
    _timeout = value
    this
  }

  private var _maxRetryTimes: Int = 3
  def maxRetryTimes: Int = _maxRetryTimes
  def setMaxRetryTimes(value: Int): this.type = {
    _maxRetryTimes = value
    this
  }


  private var _exitOnException: Boolean = true
  def exitOnException: Boolean = _exitOnException
  def setExitOnException(value: Boolean): this.type = {
    _exitOnException = value
    this
  }

  // for python api
  def buildFrom(
      topic: String,
      masterHostAndPort: String,
      timeout: Int,
      maxRetryTimes: Int,
      exitOnException: Boolean): this.type = {
    _topic = topic
    _masterHostAndPort = masterHostAndPort
    _timeout = timeout
    _maxRetryTimes = maxRetryTimes
    _exitOnException = exitOnException
    this
  }
}
