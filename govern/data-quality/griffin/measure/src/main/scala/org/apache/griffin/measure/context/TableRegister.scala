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

package org.apache.griffin.measure.context

import scala.collection.mutable.{Set => MutableSet}

import org.apache.spark.sql._

import org.apache.griffin.measure.Loggable

/**
 * register table name
 */
trait TableRegister extends Loggable with Serializable {

  protected val tables: MutableSet[String] = MutableSet()

  def registerTable(name: String): Unit = {
    tables += name
  }

  def existsTable(name: String): Boolean = {
    tables.exists(_.equals(name))
  }

  def unregisterTable(name: String): Unit = {
    if (existsTable(name)) tables -= name
  }
  def unregisterAllTables(): Unit = {
    tables.clear
  }

  def getTables: Set[String] = {
    tables.toSet
  }

}

/**
 * register table name when building dq job
 */
case class CompileTableRegister() extends TableRegister {}

/**
 * register table name and create temp view during calculation
 */
case class RunTimeTableRegister(@transient sparkSession: SparkSession) extends TableRegister {

  def registerTable(name: String, df: DataFrame): Unit = {
    registerTable(name)
    df.createOrReplaceTempView(name)
  }

  override def unregisterTable(name: String): Unit = {
    if (existsTable(name)) {
      sparkSession.catalog.dropTempView(name)
      tables -= name
    }
  }
  override def unregisterAllTables(): Unit = {
    val uts = getTables
    uts.foreach(t => sparkSession.catalog.dropTempView(t))
    tables.clear
  }

}
