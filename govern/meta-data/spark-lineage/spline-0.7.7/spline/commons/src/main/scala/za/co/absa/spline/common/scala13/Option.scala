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

package za.co.absa.spline.common.scala13

object Option {

  import scala.language.implicitConversions

  /** An Option factory which creates Some(x) if the argument is not null,
   * and None if it is null.
   *
   * @param x the value
   * @return Some(value) if value != null, None if value == null
   */
  def apply[A](x: A): Option[A] = if (x == null) None else Some(x)

  /** An Option factory which returns `None` in a manner consistent with
   * the collections hierarchy.
   */
  def empty[A]: Option[A] = None

  /** When a given condition is true, evaluates the `a` argument and returns
   * Some(a). When the condition is false, `a` is not evaluated and None is
   * returned.
   */
  def when[A](cond: Boolean)(a: => A): Option[A] =
    if (cond) Some(a) else None

  /** Unless a given condition is true, this will evaluate the `a` argument and
   * return Some(a). Otherwise, `a` is not evaluated and None is returned.
   */
  @inline def unless[A](cond: Boolean)(a: => A): Option[A] =
    when(!cond)(a)
}
