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

package org.apache.griffin.measure.datasource

import scala.collection.mutable.{SortedSet => MutableSortedSet}

import org.apache.griffin.measure.Loggable

/**
 * tmst cache, CRUD of timestamps
 */
case class TimestampStorage() extends Loggable {

  private val tmstGroup: MutableSortedSet[Long] = MutableSortedSet.empty[Long]

  // -- insert tmst into tmst group --
  def insert(tmst: Long): MutableSortedSet[Long] = tmstGroup += tmst
  def insert(tmsts: Iterable[Long]): MutableSortedSet[Long] = tmstGroup ++= tmsts

  // -- remove tmst from tmst group --
  def remove(tmst: Long): MutableSortedSet[Long] = tmstGroup -= tmst
  def remove(tmsts: Iterable[Long]): MutableSortedSet[Long] = tmstGroup --= tmsts

  // -- get subset of tmst group --
  def fromUntil(from: Long, until: Long): Set[Long] = tmstGroup.range(from, until).toSet
  def afterTil(after: Long, til: Long): Set[Long] = tmstGroup.range(after + 1, til + 1).toSet
  def until(until: Long): Set[Long] = tmstGroup.until(until).toSet
  def from(from: Long): Set[Long] = tmstGroup.from(from).toSet
  def all: Set[Long] = tmstGroup.toSet

}
