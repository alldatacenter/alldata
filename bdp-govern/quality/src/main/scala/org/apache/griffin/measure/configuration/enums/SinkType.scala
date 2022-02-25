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

import org.apache.griffin.measure.configuration.enums

/**
 * Supported Sink types
 *  <li>{@link #Console #Log} -  console sink, will sink metric in console (alias log)</li>
 *  <li>{@link #Hdfs} - hdfs sink, will sink metric and record in hdfs</li>
 *  <li>{@link #Es #Elasticsearch #Http} - elasticsearch sink, will sink metric
 *  in elasticsearch (alias Es and Http)</li>
 *  <li>{@link #Mongo #MongoDB} - mongo sink, will sink metric in mongo db (alias MongoDb)</li>
 *  <li>{@link #Custom} - custom sink (needs using extra jar-file-extension)</li>
 *  <li>{@link #Unknown} - </li>
 */
object SinkType extends GriffinEnum {
  type SinkType = Value

  val Console, Log, Hdfs, Es, Http, ElasticSearch, MongoDB, Mongo, Custom =
    Value

  def validSinkTypes(sinkTypeSeq: Seq[String]): Seq[SinkType] = {
    sinkTypeSeq
      .map(s => SinkType.withNameWithDefault(s))
      .filter(_ != SinkType.Unknown)
      .distinct
  }

  override def withNameWithDefault(name: String): enums.SinkType.Value = {
    val sinkType = super.withNameWithDefault(name)
    sinkType match {
      case Console | Log => Console
      case Es | ElasticSearch | Http => ElasticSearch
      case MongoDB | Mongo => MongoDB
      case _ => sinkType
    }
  }
}
