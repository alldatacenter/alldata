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

import za.co.absa.spline.producer.model.{DataOperation, Operations, ReadOperation}

import scala.language.reflectiveCalls

object ProducerApiAdapters {

  type OperationLike = {
    def id: String
    def childIds: Any
    def params: Option[Map[String, Any]]
    def extra: Option[Map[String, Any]]
  }

  implicit class OperationsAdapter(val op: Operations) extends AnyVal {
    def all: Seq[OperationLike] =
      Seq(
        Seq(op.write),
        op.reads getOrElse Nil,
        op.other getOrElse Nil
      ).flatten.map(_.asInstanceOf[OperationLike])
  }

  implicit class OperationLikeAdapter(val op: OperationLike) extends AnyVal {
    def childIdList: Seq[_] = op.childIds match {
      case ids: Seq[_] => ids
      case Some(ids: Seq[_]) => ids
      case _ => Nil
    }

    def output: Seq[String] = op match {
      case rop: ReadOperation => rop.output.getOrElse(Nil)
      case dop: DataOperation => dop.output.getOrElse(Nil)
      case _ => Nil
    }
  }
}
