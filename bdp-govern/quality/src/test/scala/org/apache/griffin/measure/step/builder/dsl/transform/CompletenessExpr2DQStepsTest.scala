/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.griffin.measure.step.builder.dsl.transform

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should._
import org.scalatestplus.mockito.MockitoSugar

import org.apache.griffin.measure.configuration.dqdefinition.{RuleErrorConfParam, RuleParam}
import org.apache.griffin.measure.context.DQContext
import org.apache.griffin.measure.step.builder.dsl.expr.Expr

class CompletenessExpr2DQStepsTest extends AnyFlatSpec with Matchers with MockitoSugar {

  "CompletenessExpr2DQSteps" should "get correct where clause" in {
    val completeness = CompletenessExpr2DQSteps(mock[DQContext], mock[Expr], mock[RuleParam])

    val regexClause =
      completeness.getEachErrorWhereClause(RuleErrorConfParam("id", "regex", List(raw"\d+")))
    regexClause shouldBe raw"(`id` REGEXP '\\d+')"

    val enumerationClause = completeness.getEachErrorWhereClause(
      RuleErrorConfParam("id", "enumeration", List("1", "2", "3")))
    enumerationClause shouldBe "(`id` IN ('1', '2', '3'))"

    val noneClause = completeness.getEachErrorWhereClause(
      RuleErrorConfParam("id", "enumeration", List("hive_none")))
    noneClause shouldBe "(`id` IS NULL)"

    val fullClause = completeness.getEachErrorWhereClause(
      RuleErrorConfParam("id", "enumeration", List("1", "hive_none", "3", "foo,bar")))
    fullClause shouldBe "(`id` IN ('1', '3', 'foo,bar') OR `id` IS NULL)"
  }
}
