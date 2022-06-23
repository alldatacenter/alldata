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

package za.co.absa.spline.harvester.postprocessing.metadata

import org.apache.spark.SparkConf
import org.apache.spark.internal.Logging
import org.apache.spark.sql.RuntimeConfig
import za.co.absa.commons.reflect.ReflectionUtils
import za.co.absa.spline.harvester.postprocessing.metadata.ComparisonSymbol._

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

case class Predicate(expr: Expr) extends Logging {
  def eval(bindings: Map[String, Any]): Boolean = {
    Try(expr.eval(bindings).asInstanceOf[Boolean]) match {
      case Success(value) => value
      case Failure(exception) =>
        logWarning("predicate evaluation failed, " +
          "filter will not be applied.", exception)
        false
    }
  }
}

sealed trait Expr {
  def eval(bindings: Map[String, Any]): Any
}

case class Bool(value: Boolean) extends Expr {
  override def eval(bindings: Map[String, Any]): Any = value
}

case class IntNum(value: Int) extends Expr {
  override def eval(bindings: Map[String, Any]): Any = value
}

case class LongNum(value: Long) extends Expr {
  override def eval(bindings: Map[String, Any]): Any = value
}

case class Text(value: String) extends Expr {
  override def eval(bindings: Map[String, Any]): Any = value
}

case class Not(expr: Expr) extends Expr {
  override def eval(bindings: Map[String, Any]): Any = expr.eval(bindings) match {
    case b: Boolean => !b
    case _ => throw new UnsupportedOperationException()
  }
}

case class Or(exprs: Expr*) extends Expr {
  override def eval(bindings: Map[String, Any]): Any =
    exprs.exists(_.eval(bindings) == true)
}

case class And(exprs: Expr*) extends Expr {
  override def eval(bindings: Map[String, Any]): Any =
    exprs.forall(_.eval(bindings) == true)
}

case class Path(segments: PathSegment*) extends Expr {
  override def eval(bindings: Map[String, Any]): Any = resolvePath(segments, bindings)

  @tailrec
  private def resolvePath(segments: Seq[PathSegment], obj: Any): Any =
    if (segments.isEmpty) obj
    else resolvePath(segments.tail, resolvePath(segments.head, obj))

  @tailrec
  private def resolvePath(segment: PathSegment, obj: Any): Any = obj match {
    case Some(x) => resolvePath(segment, x)
    case x => segment.resolve(x)
  }
}

sealed trait PathSegment {
  def resolve(obj: Any): Any
}

case class PropertyPS(name: String) extends PathSegment {
  override def resolve(obj: Any): Any = obj match {
    case m: Map[String, _] => m(name)
    case rc: RuntimeConfig => rc.get(name)
    case sc: SparkConf => sc.get(name)
    case ar: AnyRef => ReflectionUtils.extractFieldValue[Any](ar, name)
  }
}

case class ArrayPS(index: Int) extends PathSegment {
  override def resolve(obj: Any): Any = obj match {
    case a: Array[_] => a(index)
    case s: Seq[_] => s(index)
  }
}

case class MapPS(key: String) extends PathSegment {
  override def resolve(obj: Any): Any = obj match {
    case m: Map[String, _] => m(key)
    case rc: RuntimeConfig => rc.get(key)
    case sc: SparkConf => sc.get(key)
  }
}


case class Eq(l: Expr, r: Expr) extends Expr {
  override def eval(bindings: Map[String, Any]): Any = {
    l.eval(bindings) == r.eval(bindings)
  }
}

case class Neq(l: Expr, r: Expr) extends Expr {
  override def eval(bindings: Map[String, Any]): Any = {
    l.eval(bindings) != r.eval(bindings)
  }
}

case class Comparison(l: Expr, r: Expr, symbol: String) extends Expr {

  import scala.math.Ordering
  import Ordering.Implicits._

  override def eval(bindings: Map[String, Any]): Any = {
    val left = l.eval(bindings)
    val right = r.eval(bindings)

    (left, right) match {
      case (n1: Int, n2: Int) => compare(n1, symbol, n2)
      case (n1: Int, n2: Long) => compare(n1, symbol, n2)
      case (n1: Long, n2: Int) => compare(n1, symbol, n2)
      case (n1: Long, n2: Long) => compare(n1, symbol, n2)
    }
  }

  private def compare[T: Ordering](a: T, op: String, b: T): Boolean = op match {
    case LT => a < b
    case LTE => a <= b
    case GT => a > b
    case GTE => a >= b
  }
}

object ComparisonSymbol {
  val LT = "<"
  val LTE = "<="
  val GT = ">"
  val GTE = ">="
}

object BaseNodeName {
  type Type = String

  val ExecutionPlan = "executionPlan"
  val ExecutionEvent = "executionEvent"
  val Operation = "operation"
  val Read = "read"
  val Write = "write"
}
