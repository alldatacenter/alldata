/*
 * Copyright 2019 ABSA Group Limited
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

package za.co.absa.spline.producer.service.repo

import za.co.absa.spline.producer.model.v1_1.{ExecutionEvent, ExecutionPlan}

import scala.concurrent.{ExecutionContext, Future}

trait ExecutionProducerRepository {
  def insertExecutionPlan(executionPlan: ExecutionPlan)(implicit ec: ExecutionContext): Future[Unit]
  def insertExecutionEvents(executionEvents: Array[ExecutionEvent])(implicit ec: ExecutionContext): Future[Unit]
  def isDatabaseOk()(implicit ec: ExecutionContext): Future[Boolean]
}
