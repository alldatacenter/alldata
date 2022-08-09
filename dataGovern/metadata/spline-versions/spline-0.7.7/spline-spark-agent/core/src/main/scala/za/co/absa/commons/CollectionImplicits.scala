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

package za.co.absa.commons

import scala.util.{Success, Try}

object CollectionImplicits {

  implicit class MapOps[A, B](val m1: Map[A, B]) extends AnyVal {

    /**
     * Mappend - a simpler equivalent of Scalaz's `|+|` for maps.
     * Merges two maps summing values for keys existing in both maps (`m1` and `m2`).
     *
     * @param m2 another map
     * @return `m1 ++ m2` where values of intersecting keys are summed up
     */
    def |+|[C >: B : Numeric](m2: Map[A, C]): Map[A, C] = {
      (m1.toSeq ++ m2.toSeq)
        .groupBy { case (k, _) => k }
        .map { case (k, pairs) =>
          k -> pairs.map { case (_, v) => v }.sum
        }
    }
  }

}
