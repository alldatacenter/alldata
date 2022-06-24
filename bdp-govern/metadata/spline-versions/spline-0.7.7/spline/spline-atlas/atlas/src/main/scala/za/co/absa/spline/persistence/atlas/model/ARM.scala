/*
 * Copyright 2017 ABSA Group Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package za.co.absa.spline.persistence.atlas.model

import scala.language.reflectiveCalls

object ARM {

  type Closeable = {def close(): Unit}

  def managed[T <: Closeable, U](resource: => T): ResourceWrapper[T] = new ResourceWrapper(resource)

  def managed[T <: Closeable, U](fn: T => U): T => U = using(_)(fn)

  def using[T <: Closeable, U](resource: => T)(body: T => U): U = {
    val res = resource
    try body(res)
    finally res.close()
  }

  /**
    * Implements a for-comprehension contract
    */
  class ResourceWrapper[ResourceType <: Closeable](resource: => ResourceType) {
    def foreach(f: ResourceType => Unit): Unit = using(resource)(f)

    def map[ResultType](body: ResourceType => ResultType): ResultType = using(resource)(body)

    def flatMap[ResultType](body: ResourceType => ResultType): ResultType = using(resource)(body)

    def withFilter(f: ResourceType => Boolean): ResourceWrapper[ResourceType] = this
  }

}
