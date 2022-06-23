/*
 * Copyright 2021 ABSA Group Limited
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

package za.co.absa.spline.common.validation

import za.co.absa.spline.common.validation.UniqueConstraint.MaxItemsToPrint

case class UniqueConstraint[A, B](xs: Seq[A], projection: A => B, name: String) extends Constraint {
  private val ys: Seq[B] = xs.map(projection)

  def by[C](projectionFn: A => C): UniqueConstraint[A, C] = copy(projection = projectionFn)

  def as(itemName: String): UniqueConstraint[A, B] = copy(name = itemName)

  override def isValid: Boolean = {
    ys.length == ys.distinct.length
  }

  override def message: String = {
    val dups = for {
      (id, cnt) <- ys.groupBy(identity).mapValues(_.length)
      if cnt > 1
    } yield id

    val firstFewItemsStr = dups.take(MaxItemsToPrint).mkString(", ")
    val restItemsCount = math.max(0, dups.size - MaxItemsToPrint)

    s"Duplicated ${name}s: $firstFewItemsStr ${if (restItemsCount > 0) s"and $restItemsCount others" else ""}".trim
  }
}

object UniqueConstraint {
  val MaxItemsToPrint = 3
}
