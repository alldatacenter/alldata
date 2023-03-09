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

import scala.collection.mutable

import org.apache.spark.sql.DataFrame

import org.apache.griffin.measure.Loggable

/**
 * cache and unpersist dataframes
 */
case class DataFrameCache() extends Loggable {

  val dataFrames: mutable.Map[String, DataFrame] = mutable.Map()
  val trashDataFrames: mutable.MutableList[DataFrame] = mutable.MutableList()

  private def trashDataFrame(df: DataFrame): Unit = {
    trashDataFrames += df
  }
  private def trashDataFrames(dfs: Seq[DataFrame]): Unit = {
    trashDataFrames ++= dfs
  }

  def cacheDataFrame(name: String, df: DataFrame): Unit = {
    info(s"try to cache data frame $name")
    dataFrames.get(name) match {
      case Some(odf) =>
        trashDataFrame(odf)
        dataFrames += (name -> df)
        df.cache
        info("cache after replace old df")
      case _ =>
        dataFrames += (name -> df)
        df.cache
        info("cache after replace no old df")
    }
  }

  def uncacheDataFrame(name: String): Unit = {
    dataFrames.get(name).foreach(df => trashDataFrame(df))
    dataFrames -= name
  }
  def uncacheAllDataFrames(): Unit = {
    trashDataFrames(dataFrames.values.toSeq)
    dataFrames.clear
  }

  def clearAllTrashDataFrames(): Unit = {
    trashDataFrames.foreach(_.unpersist)
    trashDataFrames.clear
  }

}
