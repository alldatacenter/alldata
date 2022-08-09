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

package za.co.absa.spline.test

import za.co.absa.spline.producer.model._

object ProducerModelImplicits {

  implicit class DataOperationOps(val dataOperation: DataOperation) extends AnyVal {

    def outputAttributes(implicit walker: LineageWalker): Seq[Attribute] = {
      dataOperation.output
        .getOrElse(Seq.empty)
        .map(walker.attributeById)
    }

    def precedingOp(implicit walker: LineageWalker): DataOperation =
      walker.precedingOp(dataOperation)

    def precedingOps(implicit walker: LineageWalker): Seq[DataOperation] =
      walker.precedingOps(dataOperation)
  }

  implicit class WriteOperationOps(val write: WriteOperation) extends AnyVal {

    def precedingOp(implicit walker: LineageWalker): DataOperation =
      walker.precedingOp(write)

    def precedingOps(implicit walker: LineageWalker): Seq[DataOperation] =
      walker.precedingOps(write)
  }

}
