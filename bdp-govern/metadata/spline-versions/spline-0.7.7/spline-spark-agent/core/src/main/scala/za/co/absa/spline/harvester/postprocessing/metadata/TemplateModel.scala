/*
 * Copyright 2022 ABSA Group Limited
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

import org.apache.spark.internal.Logging

import javax.script.ScriptEngine
import scala.util.{Failure, Success, Try}

class DataTemplate(val extra: Map[String, Any], val labels: Map[String, Any]) extends Logging {

  def eval(bindings: Map[String, Any]): EvaluatedTemplate =
    Try(new EvaluatedTemplate(evalExtra(bindings), evalLabels(bindings))) match {
      case Success(evaluatedTemplate) => evaluatedTemplate
      case Failure(e) =>
        logWarning("Template evaluation failed, filter will not be applied.", e)
        new EvaluatedTemplate(Map.empty, Map.empty)
    }

  private def evalExtra(bindings: Map[String, Any]): Map[String, Any] =
    extra.transform((k, v) => evalValue(v, bindings))

  private def evalLabels(bindings: Map[String, Any]): Map[String, Seq[String]] =
    labels
      .transform((k, v) => toStringSeq(evalValue(v, bindings)))
      .filter { case (k, v) => v.nonEmpty }

  private def toStringSeq(v: Any): Seq[String] = v match {
    case null => Seq.empty
    case xs: Traversable[_] => xs.toSeq.flatMap(toStringSeq)
    case x => Seq(x.toString)
  }

  private def evalValue(value: Any, bindings: Map[String, Any]): Any = value match {
    case m: Map[String, _] => m.transform((k, v) => evalValue(v, bindings))
    case s: Seq[_] => s.map(evalValue(_, bindings))
    case e: Evaluable => e.eval(bindings)
    case v => v
  }
}

class EvaluatedTemplate(val extra: Map[String, Any], val labels: Map[String, Seq[String]]) {

  def merge(other: EvaluatedTemplate): EvaluatedTemplate =
    new EvaluatedTemplate(
      deepMergeMaps(extra, other.extra),
      deepMergeMaps(labels, other.labels).asInstanceOf[Map[String, Seq[String]]]
    )

  private def deepMergeMaps(m1: Map[String, Any], m2: Map[String, Any]): Map[String, Any] =
    (m1.keySet ++ m2.keySet)
      .map(k => k -> mergeOptionalValues(m1.get(k), m2.get(k)))
      .toMap

  private def mergeOptionalValues(mv1: Option[Any], mv2: Option[Any]): Any = (mv1, mv2) match {
    case (Some(v1), Some(v2)) => mergeValues(v1, v2)
    case (None, Some(v2)) => v2
    case (Some(v1), None) => v1
  }

  private def mergeValues(v1: Any, v2: Any): Any = (v1, v2) match {
    case (v1: Map[String, _], v2: Map[String, _]) => deepMergeMaps(v1, v2)
    case (v1: Seq[Any], v2: Seq[Any]) => (v1 ++ v2).distinct
    case (_, v2) => v2
  }
}

object EvaluatedTemplate{
  val empty = new EvaluatedTemplate(Map.empty, Map.empty)
}


sealed trait Evaluable {
  def eval(bindings: Map[String, Any]): AnyRef
}

case class JVMProp(propName: String) extends Evaluable {
  override def eval(bindings: Map[String, Any]): AnyRef = System.getProperty(propName)
}

case class EnvVar(envName: String) extends Evaluable {
  override def eval(bindings: Map[String, Any]): AnyRef = System.getenv(envName)
}

case class JsEval(jsEngine: ScriptEngine, js: String) extends Evaluable {
  override def eval(bindings: Map[String, Any]): AnyRef = {

    val jsBindings = jsEngine.createBindings
    bindings.foreach { case (k, v) => jsBindings.put(k, v) }

    jsEngine.eval(js, jsBindings)
  }
}

object EvaluableNames {
  val JVMProp = "$jvm"
  val EnvVar = "$env"
  val JsEval = "$js"
}
