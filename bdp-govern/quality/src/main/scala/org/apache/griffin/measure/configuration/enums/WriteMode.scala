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

package org.apache.griffin.measure.configuration.enums

/**
 * write mode when write metrics and records
 */
sealed trait WriteMode {}

object WriteMode {
  def defaultMode(procType: ProcessType.ProcessType): WriteMode = {
    procType match {
      case ProcessType.BatchProcessType => SimpleMode
      case ProcessType.StreamingProcessType => TimestampMode
    }
  }
}

/**
 * simple mode: write metrics and records directly
 */
case object SimpleMode extends WriteMode {}

/**
 * timestamp mode: write metrics and records with timestamp information
 */
case object TimestampMode extends WriteMode {}
