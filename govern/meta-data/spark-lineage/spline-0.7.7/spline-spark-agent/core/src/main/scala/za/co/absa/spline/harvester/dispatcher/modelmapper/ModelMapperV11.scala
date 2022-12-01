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

package za.co.absa.spline.harvester.dispatcher.modelmapper

import io.bfil.automapper._
import za.co.absa.spline.producer.dto.v1_1
import za.co.absa.spline.producer.model._

import scala.language.implicitConversions

object ModelMapperV11 extends ModelMapper[v1_1.ExecutionPlan, v1_1.ExecutionEvent] {

  override def toDTO(plan: ExecutionPlan): Option[v1_1.ExecutionPlan] = Some(automap(plan).to[v1_1.ExecutionPlan])

  override def toDTO(event: ExecutionEvent): Option[v1_1.ExecutionEvent] = PartialFunction.condOpt(event) {
    case e if e.error.isEmpty => automap(e).to[v1_1.ExecutionEvent]
  }

  implicit def map1(o: Option[Seq[ReadOperation]]): Option[Seq[v1_1.ReadOperation]] = o.map(_.map(automap(_).to[v1_1.ReadOperation]))

  implicit def map2(o: Option[Seq[DataOperation]]): Option[Seq[v1_1.DataOperation]] = o.map(_.map(automap(_).to[v1_1.DataOperation]))

  implicit def map3(o: Option[Seq[AttrOrExprRef]]): Option[Seq[v1_1.AttrOrExprRef]] = o.map(_.map(automap(_).to[v1_1.AttrOrExprRef]))

  implicit def map4(o: Option[Seq[Attribute]]): Option[Seq[v1_1.Attribute]] = o.map(_.map(automap(_).to[v1_1.Attribute]))

  implicit def map5(o: Option[Seq[Literal]]): Option[Seq[v1_1.Literal]] = o.map(_.map(automap(_).to[v1_1.Literal]))

  implicit def map6(o: Option[Seq[FunctionalExpression]]): Option[Seq[v1_1.FunctionalExpression]] = o.map(_.map(automap(_).to[v1_1.FunctionalExpression]))
}
